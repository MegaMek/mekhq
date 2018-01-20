/*
 * UnitCostReportDialog.java
 *
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import mekhq.campaign.unit.Unit;

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
        setMinimumSize(new Dimension(500, 200));
        setPreferredSize(new Dimension(700, 500));
        setLocationRelativeTo(parent);
    }

    private void initComponents() {

        txtReport = new JTextPane();
        txtReport.setContentType("text/html");

        setLayout(new java.awt.BorderLayout());

        txtReport.setEditable(false);

        getContentPane().add(new JScrollPane(txtReport), BorderLayout.CENTER);
    }
}
