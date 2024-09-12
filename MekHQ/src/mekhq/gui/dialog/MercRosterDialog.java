package mekhq.gui.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import javax.swing.*;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.MercRosterAccess;

/**
 * A dialog that sets up a connection with a mysql MercRoster database to upload
 * campaign data
 * 
 * @author Jay Lawson
 * @since Jan 6, 2010, 10:46:02 PM
 */
public class MercRosterDialog extends JDialog implements PropertyChangeListener {
    private static final MMLogger logger = MMLogger.create(MercRosterDialog.class);

    private Campaign campaign;
    private JFrame frame;

    private JTextField txtAddress;
    private JTextField txtPort;
    private JTextField txtTable;
    private JTextField txtUser;
    private JPasswordField txtPasswd;
    private JButton btnUpload;
    private JButton btnCancel;

    private ProgressMonitor progressMonitor;
    private MercRosterAccess access;

    public MercRosterDialog(JFrame parent, boolean modal, Campaign c) {
        super(parent, modal);
        frame = parent;
        this.campaign = c;
        initComponents();
        this.setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MercRosterDialog",
                MekHQ.getMHQOptions().getLocale());

        txtAddress = new JTextField("localhost");
        txtPort = new JTextField("3306");
        txtTable = new JTextField("mercroster");
        txtUser = new JTextField("mysqluser");
        txtPasswd = new JPasswordField("");
        btnUpload = new JButton(resourceMap.getString("btnUpload.text"));
        btnCancel = new JButton(resourceMap.getString("btnCancel.text"));

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setTitle(resourceMap.getString("Form.title"));

        getContentPane().setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(5, 5, 5, 5);
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

        JPanel panButtons = new JPanel(new GridLayout(0, 2));
        panButtons.add(btnUpload);
        panButtons.add(btnCancel);

        btnUpload.addActionListener(evt -> upload());

        btnCancel.addActionListener(evt -> setVisible(false));

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

    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MercRosterDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
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
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                    "Could not connect to the mysql database. Check your entries and confirm\n that you can connect to the database remotely.",
                    "Could not connect", JOptionPane.ERROR_MESSAGE);
            logger.error("", ex);
            return;
        }
        access.addPropertyChangeListener(this);
        access.execute();
        setVisible(false);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        progressMonitor.setProgress(access.getProgress());
        progressMonitor.setNote(access.getProgressNote());
        if (progressMonitor.isCanceled()) {
            access.cancel(true);
            access.close();
        }
    }
}
