package gameshelf.persistence;

import gameshelf.model.Calificacion;
import gameshelf.model.ResumenCalificacion;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Calificaciones de los usuarios. Hay como máximo una por usuario y juego (si vuelve a calificar, reemplaza la anterior).
 * Por diseño esta clase NO ofrece ninguna operación que devuelva calificaciones de otros usuarios: solo las de un usuario
 * concreto y resúmenes anónimos (promedio y cantidad de votos).
 */
public class CalificacionRepository {
    private final ConexionDB db;

    public CalificacionRepository(ConexionDB db) {
        this.db = db;
    }

    /** @return mapa idJuego -> puntuación, solo con las calificaciones del usuario indicado. */
    public synchronized Map<Integer, Integer> puntuacionesDeUsuario(int usuarioId) {
        Map<Integer, Integer> mapa = new HashMap<>();
        try (PreparedStatement ps = db.conectar().prepareStatement("SELECT videojuego_id, puntuacion FROM calificacion WHERE usuario_id = ?")) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getInt(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudieron leer las calificaciones de la base de datos", e);
        }
        return mapa;
    }

    /**
     * Promedio y cantidad de votos de cada juego, sumando las calificaciones de TODOS los usuarios.
     * Es un resumen anónimo: no revela qué nota puso cada usuario.
     */
    public synchronized Map<Integer, ResumenCalificacion> resumenesPorJuego() {
        Map<Integer, ResumenCalificacion> resumenes = new HashMap<>();
        try (Statement st = db.conectar().createStatement();
             ResultSet rs = st.executeQuery("SELECT videojuego_id, AVG(puntuacion * 1.0), COUNT(*) FROM calificacion GROUP BY videojuego_id")) {
            while (rs.next()) {
                resumenes.put(rs.getInt(1), new ResumenCalificacion(rs.getDouble(2), rs.getInt(3)));
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo calcular el promedio de calificaciones", e);
        }
        return resumenes;
    }

    /**
     * Guarda la calificación del usuario para el juego; si ya había calificado ese juego, la reemplaza.
     * @return true si reemplazó una calificación anterior; false si es la primera vez que lo califica.
     */
    public synchronized boolean guardar(Calificacion nueva) {
        try {
            boolean existe;
            try (PreparedStatement ps = db.conectar().prepareStatement("SELECT 1 FROM calificacion WHERE usuario_id = ? AND videojuego_id = ?")) {
                ps.setInt(1, nueva.getUsuarioId());
                ps.setInt(2, nueva.getVideojuegoId());
                try (ResultSet rs = ps.executeQuery()) {
                    existe = rs.next();
                }
            }
            String sql = existe
                    ? "UPDATE calificacion SET puntuacion = ?, fecha = ? WHERE usuario_id = ? AND videojuego_id = ?"
                    : "INSERT INTO calificacion (puntuacion, fecha, usuario_id, videojuego_id) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = db.conectar().prepareStatement(sql)) {
                ps.setInt(1, nueva.getPuntuacion());
                ps.setTimestamp(2, Timestamp.valueOf(nueva.getFecha()));
                ps.setInt(3, nueva.getUsuarioId());
                ps.setInt(4, nueva.getVideojuegoId());
                ps.executeUpdate();
            }
            return existe;
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo guardar la calificación en la base de datos", e);
        }
    }

    /** Borra las calificaciones de un juego (la base ya lo hace en cascada al eliminar el juego). */
    public synchronized void eliminarDeJuego(int videojuegoId) {
        try (PreparedStatement ps = db.conectar().prepareStatement("DELETE FROM calificacion WHERE videojuego_id = ?")) {
            ps.setInt(1, videojuegoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("No se pudieron borrar las calificaciones del juego", e);
        }
    }
}
