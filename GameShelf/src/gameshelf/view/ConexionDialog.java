package gameshelf.view;

import gameshelf.persistence.ConexionDB;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Configura la conexión al servidor MariaDB de GameShelf. Prueba la conexión antes de guardar.
 */
public class ConexionDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final transient ConexionDB conexion;
    private final JTextField txtHost = new JTextField(ConexionDB.HOST_POR_DEFECTO, 18);
    private final JTextField txtPuerto = new JTextField(String.valueOf(ConexionDB.PUERTO_POR_DEFECTO), 6);
    private final JTextField txtBase = new JTextField(ConexionDB.BASE_POR_DEFECTO, 18);
    private final JTextField txtUsuario = new JTextField(ConexionDB.USUARIO_POR_DEFECTO, 18);
    private final JPasswordField txtClave = new JPasswordField(ConexionDB.CLAVE_POR_DEFECTO, 18);
    private boolean guardado;

    /** @param mensaje texto informativo/de error a mostrar arriba (puede ser null). */
    public ConexionDialog(Window owner, ConexionDB conexion, String mensaje) {
        super(owner, "Conexi\u00f3n a la base de datos (MariaDB)", ModalityType.APPLICATION_MODAL);
        this.conexion = conexion;
        construir(mensaje);
        precargar();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /** @return true si se configuró (y se probó) una conexión nueva. */
    public boolean isGuardado() {
        return guardado;
    }

    private void construir(String mensaje) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 6, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;

        int fila = 0;
        if (mensaje != null) {
            c.gridx = 0; c.gridy = fila++; c.gridwidth = 2;
            form.add(new JLabel("<html><body style='width:380px'>" + mensaje.replace("\n", "<br>") + "</body></html>"), c);
        }
        c.gridwidth = 1;
        fila = campo(form, c, fila, "Direcci\u00f3n (IP o nombre)", txtHost);
        fila = campo(form, c, fila, "Puerto", txtPuerto);
        fila = campo(form, c, fila, "Base", txtBase);
        fila = campo(form, c, fila, "Usuario de la base", txtUsuario);
        campo(form, c, fila, "Contrase\u00f1a de la base", txtClave);

        JButton btnGuardar = new JButton("Probar y guardar");
        JButton btnCancelar = new JButton("Cancelar");
        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(botones, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnGuardar);
    }

    private static int campo(JPanel panel, GridBagConstraints c, int fila, String etiqueta, java.awt.Component campo) {
        c.gridy = fila;
        c.gridx = 0;
        c.weightx = 0;
        panel.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(campo, c);
        c.fill = GridBagConstraints.NONE;
        return fila + 1;
    }

    /** Muestra la configuración actual. */
    private void precargar() {
        String url = conexion.getUrl();
        int inicio = url.indexOf("//");
        if (inicio >= 0) {
            String resto = url.substring(inicio + 2);
            int barra = resto.indexOf('/');
            String hostPuerto = barra < 0 ? resto : resto.substring(0, barra);
            String base = barra < 0 ? ConexionDB.BASE_POR_DEFECTO : resto.substring(barra + 1);
            int dosPuntos = hostPuerto.lastIndexOf(':');
            txtHost.setText(dosPuntos < 0 ? hostPuerto : hostPuerto.substring(0, dosPuntos));
            txtPuerto.setText(dosPuntos < 0 ? String.valueOf(ConexionDB.PUERTO_POR_DEFECTO) : hostPuerto.substring(dosPuntos + 1));
            txtBase.setText(base);
        }
        txtUsuario.setText(conexion.getUsuario());
        txtClave.setText(conexion.getClave());
    }

    private void guardar() {
        String urlAnterior = conexion.getUrl();
        String usuarioAnterior = conexion.getUsuario();
        String claveAnterior = conexion.getClave();
        try {
            int puerto;
            try {
                puerto = Integer.parseInt(txtPuerto.getText().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El puerto debe ser un n\u00famero (por ejemplo, 3306).");
            }
            if (txtHost.getText().trim().isEmpty() || txtBase.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("Complet\u00e1 la direcci\u00f3n y el nombre de la base.");
            }
            conexion.setUrl(ConexionDB.urlMariaDB(txtHost.getText(), puerto, txtBase.getText()));
            conexion.setCredenciales(txtUsuario.getText(), new String(txtClave.getPassword()));
            conexion.conectar();
            conexion.guardarConfiguracion();
            guardado = true;
            dispose();
        } catch (IllegalArgumentException | SQLException | IOException e) {
            restaurar(urlAnterior, usuarioAnterior, claveAnterior);
            JOptionPane.showMessageDialog(this, "No se pudo conectar:\n" + e.getMessage(), "Conexi\u00f3n", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void restaurar(String url, String usuario, String clave) {
        try {
            conexion.setUrl(url);
            conexion.setCredenciales(usuario, clave);
        } catch (IllegalArgumentException ignorada) {
            // la configuración anterior ya era válida
        }
    }
}
