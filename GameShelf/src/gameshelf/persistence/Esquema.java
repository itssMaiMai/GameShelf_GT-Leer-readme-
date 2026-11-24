package gameshelf.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Esquema de la base de datos MariaDB. Todo vive en la misma base: las imágenes se guardan como LONGBLOB en la tabla "imagen".
 */
final class Esquema {

    private static final String[] SENTENCIAS = {
        "CREATE TABLE IF NOT EXISTS usuario ("
            + " id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
            + " nombre VARCHAR(30) COLLATE utf8mb4_general_ci NOT NULL UNIQUE,"
            + " rol VARCHAR(10) NOT NULL,"
            + " salt VARCHAR(64) NOT NULL,"
            + " hash VARCHAR(128) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",

        "CREATE TABLE IF NOT EXISTS imagen ("
            + " nombre VARCHAR(80) PRIMARY KEY,"
            + " tipo VARCHAR(10) NOT NULL,"
            + " datos LONGBLOB NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",

        "CREATE TABLE IF NOT EXISTS videojuego ("
            + " id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
            + " nombre VARCHAR(100) NOT NULL,"
            + " plataforma VARCHAR(700) NOT NULL DEFAULT '',"
            + " genero VARCHAR(700) NOT NULL DEFAULT '',"
            + " desarrollador VARCHAR(700) NOT NULL DEFAULT '',"
            + " anio INT NOT NULL DEFAULT 0,"
            + " fecha_creacion DATETIME(3) NOT NULL,"
            + " creado_por VARCHAR(30) NOT NULL DEFAULT '',"
            + " imagen VARCHAR(80) NOT NULL DEFAULT '') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",

        "CREATE TABLE IF NOT EXISTS seguimiento ("
            + " usuario_id INT NOT NULL,"
            + " videojuego_id INT NOT NULL,"
            + " estado VARCHAR(12) NULL,"
            + " horas_jugadas DOUBLE NULL,"
            + " favorito TINYINT(1) NOT NULL DEFAULT 0,"
            + " notas VARCHAR(2000) NOT NULL DEFAULT '',"
            + " PRIMARY KEY (usuario_id, videojuego_id),"
            + " FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,"
            + " FOREIGN KEY (videojuego_id) REFERENCES videojuego(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci",

        "CREATE TABLE IF NOT EXISTS calificacion ("
            + " usuario_id INT NOT NULL,"
            + " videojuego_id INT NOT NULL,"
            + " puntuacion INT NOT NULL CHECK (puntuacion BETWEEN 1 AND 10),"
            + " fecha DATETIME(3) NOT NULL,"
            + " PRIMARY KEY (usuario_id, videojuego_id),"
            + " FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,"
            + " FOREIGN KEY (videojuego_id) REFERENCES videojuego(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci"
    };

    private Esquema() {
    }

    static void crear(Connection conexion) throws SQLException {
        try (Statement st = conexion.createStatement()) {
            for (String sql : SENTENCIAS) {
                st.execute(sql);
            }
        }
    }
}
