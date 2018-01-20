/*
 * MaintenanceReportDialog.java
 *
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import mekhq.campaign.unit.Unit;

/**
 *
 * @author Jay Lawson
 */
public class MaintenanceReportDialog extends javax.swing.JDialog {

    private static final long serialVersionUID = 3624327778807359294L;

    private JTextPane txtReport;

    public MaintenanceReportDialog(java.awt.Frame parent, Unit unit) {
        super(parent, false);
        setTitle("Maintenance Report for " + unit.getName());
        initComponents();
        txtReport.setText(unit.getLastMaintenanceReport());
        txtReport.setCaretPosition(0);
        setMinimumSize(new Dimension(700, 500));
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
