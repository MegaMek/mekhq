/*
 * UnitCostReportDialog.java
 *
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import mekhq.MekHQ;
import mekhq.campaign.unit.Unit;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 * @author pheonixstorm
 * Copied from MaintenanceReportDialog.java
 */

public class UnitCostReportDialog extends javax.swing.JDialog {

    private static final long serialVersionUID = -1423208314297284122L;
    
    private JTextPane txtReport;

    public UnitCostReportDialog(java.awt.Frame parent, Unit unit) {
        super(parent, false);
        setTitle("Monthly Cost Report for " + unit.getName());
        initComponents();     
        txtReport.setText(unit.displayMonthlyCost());
        txtReport.setCaretPosition(0);
        setPreferredSize(new Dimension(700, 500));
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {

        txtReport = new JTextPane();
        txtReport.setContentType("text/html");
        
        setLayout(new java.awt.BorderLayout());
        
        txtReport.setEditable(false);
        
        getContentPane().add(new JScrollPane(txtReport), BorderLayout.CENTER);
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(UnitCostReportDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }
}
