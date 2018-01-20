/*
 * MercRosterDialog.java
 *
 * Created on Jan 6, 2010, 10:46:02 PM
 */

package mekhq.gui.dialog;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.MercRosterAccess;

/**
 * A dialog that sets up a connection with a mysql MercRoster database to upload campaign data
 * @author Jay Lawson
 */
public class MercRosterDialog extends javax.swing.JDialog implements PropertyChangeListener {

    private Campaign campaign;
    private Frame frame;

    private JTextField txtAddress;
    private JTextField txtPort;
    private JTextField txtTable;
    private JTextField txtUser;
    private JPasswordField txtPasswd;
    private JButton btnUpload;
    private JButton btnCancel;

    private ProgressMonitor progressMonitor;
    private MercRosterAccess access;

    private static final long serialVersionUID = 8376874926997734492L;
    /** Creates new form */
    public MercRosterDialog(java.awt.Frame parent, boolean modal, Campaign c) {
        super(parent, modal);
        frame = parent;
        this.campaign = c;
        initComponents();
        this.setLocationRelativeTo(parent);
    }

    private void initComponents() {

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MercRosterDialog", new EncodeControl()); //$NON-NLS-1$

        txtAddress = new JTextField("localhost");
        txtPort = new JTextField("3306");
        txtTable = new JTextField("mercroster");
        txtUser = new JTextField("mysqluser");
        txtPasswd = new JPasswordField("");
        btnUpload = new JButton(resourceMap.getString("btnUpload.text"));
        btnCancel = new JButton(resourceMap.getString("btnCancel.text"));


        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));

        getContentPane().setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        getContentPane().add(new JLabel(resourceMap.getString("lblAddress.text")), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        getContentPane().add(txtAddress, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        getContentPane().add(new JLabel(resourceMap.getString("lblPort.text")), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        getContentPane().add(txtPort, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        getContentPane().add(new JLabel(resourceMap.getString("lblTable.text")), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        getContentPane().add(txtTable, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        getContentPane().add(new JLabel(resourceMap.getString("lblUser.text")), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        getContentPane().add(txtUser, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        getContentPane().add(new JLabel(resourceMap.getString("lblPasswd.text")), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        getContentPane().add(txtPasswd, gbc);

        JPanel panButtons = new JPanel(new GridLayout(0,2));
        panButtons.add(btnUpload);
        panButtons.add(btnCancel);

        btnUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upload();
            }
        });

        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setVisible(false);
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        getContentPane().add(panButtons, gbc);

        pack();
    }

    private void upload() {
        String address = txtAddress.getText();
        int port = Integer.parseInt(txtPort.getText());
        String table = txtTable.getText();
        String username = txtUser.getText();
        String passwd = new String(txtPasswd.getPassword());
        access = new MercRosterAccess(address, port, table, username, passwd, campaign);
        progressMonitor = new ProgressMonitor(frame,
                "Uploading data to MercRoster",
                access.getProgressNote(), 0, 100);
        try {
            access.connect();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame,
                    "Could not connect to the mysql database. Check your entries and confirm\n" +
                    "that you can connect to the database remotely.",
                    "Could not connect",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }
        access.addPropertyChangeListener(this);
        access.execute();
        setVisible(false);
    }

    @Override
    public void propertyChange(PropertyChangeEvent arg0) {
        progressMonitor.setProgress(access.getProgress());
        progressMonitor.setNote(access.getProgressNote());
        if (progressMonitor.isCanceled()) {
            access.cancel(true);
            access.close();
        }
        if (access.isDone()) {
            //nothing
        }
    }

}
