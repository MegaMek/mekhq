package mekhq.gui.dialog;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.MekView;
import mekhq.MekHQ;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 * @since July 15, 2009, 9:30 PM
 */
public class MekViewDialog extends JDialog {
    private MekView mview;
    private JButton btnOkay;
    private JScrollPane jScrollPane2;
    private JTextPane txtMek;

    /** Creates new form MekViewDialog */
    public MekViewDialog(JFrame parent, boolean modal, MekView mv) {
        super(parent, modal);
        this.mview = mv;
        initComponents();
        setUserPreferences();
    }

    private void initComponents() {

        jScrollPane2 = new JScrollPane();
        txtMek = new JTextPane();
        btnOkay = new JButton();

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekViewDialog",
                MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Unit View");

        jScrollPane2.setName("jScrollPane2");

        txtMek.setContentType(resourceMap.getString("txtMek.contentType"));
        txtMek.setEditable(false);
        txtMek.setFont(Font.decode(resourceMap.getString("txtMek.font")));
        txtMek.setName("txtMek");
        txtMek.setText(mview.getMekReadout());
        jScrollPane2.setViewportView(txtMek);

        btnOkay.setText(resourceMap.getString("btnOkay.text"));
        btnOkay.setName("btnOkay");
        btnOkay.addActionListener(this::btnOkayActionPerformed);

        getContentPane().add(jScrollPane2, BorderLayout.CENTER);
        getContentPane().add(btnOkay, BorderLayout.PAGE_END);

        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MekViewDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set user preferences", ex);
        }
    }

    private void btnOkayActionPerformed(ActionEvent evt) {
        this.setVisible(false);
    }
}
