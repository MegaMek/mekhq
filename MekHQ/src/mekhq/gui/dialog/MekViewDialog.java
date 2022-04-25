package mekhq.gui.dialog;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.MechView;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 * @since July 15, 2009, 9:30 PM
 */
public class MekViewDialog extends JDialog {
    private MechView mview;

    /** Creates new form MekViewDialog */
    public MekViewDialog(JFrame parent, boolean modal, MechView mv) {
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
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Unit View");

        jScrollPane2.setName("jScrollPane2");

        txtMek.setContentType(resourceMap.getString("txtMek.contentType"));
        txtMek.setEditable(false);
        txtMek.setFont(Font.decode(resourceMap.getString("txtMek.font")));
        txtMek.setName("txtMek");
        txtMek.setText(mview.getMechReadout());
        jScrollPane2.setViewportView(txtMek);

        btnOkay.setText(resourceMap.getString("btnOkay.text"));
        btnOkay.setName("btnOkay");
        btnOkay.addActionListener(this::btnOkayActionPerformed);

        getContentPane().add(jScrollPane2, BorderLayout.CENTER);
        getContentPane().add(btnOkay, BorderLayout.PAGE_END);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MekViewDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnOkayActionPerformed(java.awt.event.ActionEvent evt) {
        this.setVisible(false);
    }
    private JButton btnOkay;
    private JScrollPane jScrollPane2;
    private JTextPane txtMek;
}
