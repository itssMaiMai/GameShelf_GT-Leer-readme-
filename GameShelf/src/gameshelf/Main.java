package gameshelf;

import gameshelf.controller.VideojuegoController;
import gameshelf.persistence.AlmacenImagenes;
import gameshelf.persistence.CalificacionRepository;
import gameshelf.persistence.ConexionDB;
import gameshelf.persistence.MigradorJson;
import gameshelf.persistence.SeguimientoRepository;
import gameshelf.persistence.UsuarioRepository;
import gameshelf.persistence.VideojuegoRepository;
import gameshelf.service.CalificacionService;
import gameshelf.service.SeguimientoService;
import gameshelf.service.UsuarioService;
import gameshelf.service.VideojuegoService;
import gameshelf.view.ConexionDialog;
import gameshelf.view.LoginDialog;
import gameshelf.view.MainFrame;
import gameshelf.view.RegistroDialog;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Punto de entrada de GameShelf.
 * <ul>
 *   <li>Sin argumentos: abre la aplicación.</li>
 *   <li>{@code --servidor [--puerto N] [--carpeta ruta]}: publica la base de datos para otras PCs de la red.</li>
 *   <li>{@code --importar <carpeta>}: importa los archivos de la versión anterior (JSON + carpeta imagenes) a la base.</li>
 * </ul>
 */
public class Main {
    private static ConexionDB conexionActual;

    public static void main(String[] args) {
        if (args.length > 0 && "--importar".equals(args[0])) {
            importarDesdeConsola(args);
        } else {
            SwingUtilities.invokeLater(Main::iniciar);
        }
    }

    private static void iniciar() {
        aplicarLookAndFeel();
        conexionActual = new ConexionDB();
        arrancar();
    }

    /**
     * Conecta a la base de datos (si falla, deja elegir otra conexión), importa los datos de la versión anterior si
     * corresponde, arma las capas y abre la aplicación (primero el alta del administrador si es el primer uso).
     */
    private static void arrancar() {
        ConexionDB conexion = conexionActual;
        while (true) {
            try {
                conexion.conectar();
                break;
            } catch (Exception e) {
                ConexionDialog dialogo = new ConexionDialog(null, conexion,
                        "No se pudo conectar a la base de datos (" + conexion.descripcion() + "):\n" + e.getMessage());
                dialogo.setVisible(true);
                if (!dialogo.isGuardado()) {
                    System.exit(1);
                }
            }
        }
        try {
            importarSiCorresponde(conexion);

            VideojuegoRepository juegos = new VideojuegoRepository(conexion);
            UsuarioRepository cuentas = new UsuarioRepository(conexion);
            CalificacionRepository calificaciones = new CalificacionRepository(conexion);
            SeguimientoRepository seguimiento = new SeguimientoRepository(conexion);
            AlmacenImagenes almacen = new AlmacenImagenes(conexion);
            renombrarImagenes(almacen);
            VideojuegoService servicioJuegos = new VideojuegoService(juegos, almacen, calificaciones, seguimiento);
            VideojuegoController controller = new VideojuegoController(servicioJuegos, new UsuarioService(cuentas),
                    new CalificacionService(juegos, calificaciones), new SeguimientoService(juegos, seguimiento));

            if (!controller.hayUsuarios()) {
                // Primer uso: la primera cuenta creada es la del administrador.
                RegistroDialog alta = new RegistroDialog(null, controller, true);
                alta.setVisible(true);
                if (!alta.isCreado()) {
                    System.exit(0);
                }
            }
            abrirConLogin(controller);
        } catch (RuntimeException e) {
            String causa = e.getCause() != null && e.getCause().getMessage() != null ? "\n" + e.getCause().getMessage() : "";
            JOptionPane.showMessageDialog(null, "GameShelf no pudo iniciar:\n" + e.getMessage() + causa,
                    "Error al iniciar", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /** Renombra las imágenes ya guardadas cuyo nombre no coincide con el de su juego (ver AlmacenImagenes). */
    private static void renombrarImagenes(AlmacenImagenes almacen) {
        try {
            int n = almacen.sincronizarNombres();
            if (n > 0) {
                JOptionPane.showMessageDialog(null, "Se renombraron " + n + (n == 1 ? " imagen" : " im\u00e1genes")
                        + " para que coincidan con el nombre de su juego.", "Im\u00e1genes", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (RuntimeException e) {
            // no impide usar la aplicación; se reintenta en el próximo arranque
        }
    }

    /** Si la carpeta del programa tiene los archivos de la versión anterior, los importa a la base. */
    private static void importarSiCorresponde(ConexionDB conexion) {
        Path carpeta = Paths.get("").toAbsolutePath();
        if (!MigradorJson.hayArchivosAntiguos(carpeta)) {
            return;
        }
        try {
            MigradorJson.Resultado r = MigradorJson.migrar(conexion, carpeta);
            if (r.datosImportados || r.imagenesImportadas > 0) {
                JOptionPane.showMessageDialog(null, "Se import\u00f3 la informaci\u00f3n de la versi\u00f3n anterior a la base de datos.\n\n"
                        + r.resumen(), "Importaci\u00f3n", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "No se pudieron importar los archivos de la versi\u00f3n anterior:\n" + e.getMessage(),
                    "Importaci\u00f3n", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Pide inicio de sesión (si todavía no hay una sesión activa) y abre la ventana principal.
     * Si el usuario cierra el login sin ingresar, la aplicación termina. Si cambia la conexión a la base desde el
     * login, se vuelve a arrancar con la nueva.
     */
    public static void abrirConLogin(VideojuegoController controller) {
        if (controller.getUsuarioActual() == null) {
            LoginDialog login = new LoginDialog(controller, conexionActual);
            login.setVisible(true);
            if (login.isReconectar()) {
                arrancar();
                return;
            }
            if (!login.isAutenticado()) {
                System.exit(0);
            }
        }
        MainFrame ventana = new MainFrame(controller);
        ventana.setVisible(true);
    }

    private static void importarDesdeConsola(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java -jar GameShelf.jar --importar <carpeta con gameshelf.json, usuarios.json, ... e imagenes/>");
            System.exit(1);
        }
        ConexionDB conexion = new ConexionDB();
        try {
            MigradorJson.Resultado r = MigradorJson.migrar(conexion, Paths.get(args[1]).toAbsolutePath());
            System.out.println(r.resumen());
            int renombradas = new AlmacenImagenes(conexion).sincronizarNombres();
            System.out.println("Im\u00e1genes renombradas con el nombre de su juego: " + renombradas);
            System.out.println("Base de datos: " + conexion.getUrl());
        } catch (Exception e) {
            System.err.println("No se pudo importar: " + e.getMessage());
            System.exit(1);
        } finally {
            conexion.cerrar();
        }
    }

    private static void aplicarLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Si falla, se usa el aspecto por defecto de Swing.
        }
    }
}
