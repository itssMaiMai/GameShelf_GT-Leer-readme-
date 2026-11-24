package gameshelf.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Conexión a la base de datos MariaDB.
 * Orden de configuración: propiedades del sistema ({@code -Dgameshelf.db.url=...}), luego el archivo
 * {@code gameshelf.properties} (claves db.url, db.usuario, db.clave) y por último los valores por defecto.
 */
public class ConexionDB {
    public static final String ARCHIVO_CONFIG = "gameshelf.properties";
    public static final String HOST_POR_DEFECTO = "167.126.30.65";
    public static final int PUERTO_POR_DEFECTO = 3306;
    public static final String BASE_POR_DEFECTO = "gameshelf";
    public static final String USUARIO_POR_DEFECTO = "gameshelf";
    public static final String CLAVE_POR_DEFECTO = "h6iyuygsfaOrufMPdUQV";

    private String url;
    private String usuario;
    private String clave;
    private Connection conexion;

    /** Carga la configuración (propiedades del sistema, gameshelf.properties o valores por defecto). */
    public ConexionDB() {
        Properties archivo = new Properties();
        Path ruta = Paths.get(ARCHIVO_CONFIG).toAbsolutePath();
        if (Files.isRegularFile(ruta)) {
            try (InputStream in = Files.newInputStream(ruta)) {
                archivo.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException e) {
                // Si no se puede leer, se usan los valores por defecto.
            }
        }
        String urlDefecto = urlMariaDB(HOST_POR_DEFECTO, PUERTO_POR_DEFECTO, BASE_POR_DEFECTO);
        this.url = valor("gameshelf.db.url", archivo.getProperty("db.url"), urlDefecto);
        this.usuario = valor("gameshelf.db.usuario", archivo.getProperty("db.usuario"), USUARIO_POR_DEFECTO);
        this.clave = valor("gameshelf.db.clave", archivo.getProperty("db.clave"), CLAVE_POR_DEFECTO);
    }

    public ConexionDB(String url, String usuario, String clave) {
        setUrl(url);
        this.usuario = usuario == null ? "" : usuario;
        this.clave = clave == null ? "" : clave;
    }

    public static String urlMariaDB(String host, int puerto, String base) {
        return "jdbc:mariadb://" + host.trim() + ":" + puerto + "/" + base.trim();
    }

    private static String valor(String propiedadSistema, String delArchivo, String porDefecto) {
        String s = System.getProperty(propiedadSistema);
        if (s != null && !s.trim().isEmpty()) return s.trim();
        if (delArchivo != null && !delArchivo.trim().isEmpty()) return delArchivo.trim();
        return porDefecto;
    }

    public String getUrl() {
        return url;
    }

    public synchronized void setUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("La dirección de la base de datos no puede estar vacía.");
        }
        String u = url.trim();
        if (!u.startsWith("jdbc:mariadb:") && !u.startsWith("jdbc:mysql:")) {
            throw new IllegalArgumentException("Solo se admiten bases MariaDB (jdbc:mariadb:...).");
        }
        cerrar();
        this.url = u;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getClave() {
        return clave;
    }

    public synchronized void setCredenciales(String usuario, String clave) {
        cerrar();
        this.usuario = usuario == null ? "" : usuario.trim();
        this.clave = clave == null ? "" : clave;
    }

    public boolean esRemota() {
        return true;
    }

    /** Texto corto para mostrar al usuario. */
    public String descripcion() {
        int inicio = url.indexOf("//");
        if (inicio >= 0) {
            String s = url.substring(inicio + 2);
            int corte = s.indexOf('/');
            return "MariaDB " + (corte < 0 ? s : s.substring(0, corte));
        }
        return "MariaDB";
    }

    /**
     * Devuelve una conexión abierta (la crea o la reabre si hace falta) con el esquema ya creado.
     * La misma conexión se comparte entre los repositorios.
     */
    public synchronized Connection conectar() throws SQLException {
        if (conexion == null || conexion.isClosed() || !conexion.isValid(2)) {
            cerrar();
            try {
                Class.forName("org.mariadb.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Falta el controlador de base de datos MariaDB.", e);
            }
            Connection nueva = DriverManager.getConnection(url, usuario, clave);
            try {
                Esquema.crear(nueva);
            } catch (SQLException e) {
                try {
                    nueva.close();
                } catch (SQLException ignorada) {
                    // se informa el error original
                }
                throw e;
            }
            conexion = nueva;
        }
        return conexion;
    }

    public synchronized void cerrar() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                // no hay nada más que hacer
            }
            conexion = null;
        }
    }

    /** Guarda la configuración en gameshelf.properties (junto al programa) para los próximos arranques. */
    public synchronized void guardarConfiguracion() throws IOException {
        Properties p = new Properties();
        p.setProperty("db.url", url);
        p.setProperty("db.usuario", usuario);
        p.setProperty("db.clave", clave);
        try (OutputStream out = Files.newOutputStream(Paths.get(ARCHIVO_CONFIG).toAbsolutePath())) {
            p.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), "Configuracion de GameShelf");
        }
    }
}
