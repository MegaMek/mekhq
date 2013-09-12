/*
 * ReportDialog.java
 *
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import mekhq.campaign.report.Report;

/**
 *
 * @author Jay Lawson
 */
public class ReportDialog extends JDialog {
    
    private static final long serialVersionUID = 3624327778807359294L;

    private JTextPane txtReport;
    private JScrollPane scrReport;
 
    public ReportDialog(Frame parent, Report report) {
        super(parent, false);
        setTitle(report.getTitle());
        txtReport = report.getReport();
        initComponents();   
        setMinimumSize(new Dimension(400, 500));
        //setPreferredSize(new Dimension(400, 500));
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        
        setLayout(new BorderLayout());
        
        scrReport = new JScrollPane(txtReport);
        txtReport.setEditable(false);

        getContentPane().add(scrReport, BorderLayout.CENTER);
    
    }
    
}
