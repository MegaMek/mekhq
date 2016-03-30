/*
 * ReportDialog.java
 *
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JDialog;

import mekhq.gui.CampaignGUI;
import mekhq.gui.DailyReportLogPanel;
import mekhq.gui.ReportHyperlinkListener;

/**
 *
 * @author Jay Lawson
 */
public class DailyReportLogDialog extends JDialog {
    
    private static final long serialVersionUID = 3624327778807359294L;

    private DailyReportLogPanel panLog;
    
    private CampaignGUI gui;
 
    public DailyReportLogDialog(Frame parent, CampaignGUI gui, ReportHyperlinkListener listener) {
        super(parent, false);
        this.gui = gui;
        panLog = new DailyReportLogPanel(listener);
        initComponents();   
        setMinimumSize(new Dimension(400, 500));
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                gui.hideDailyReportDialog();
            }
        });
        setTitle("Daily Report Log");
        setLayout(new BorderLayout());
        add(panLog, BorderLayout.CENTER);
    }
    
    public void refreshLog() {
        panLog.refreshLog(gui.getCampaign().getCurrentReportHTML());
    }
    
    public void appendLog(List<String> newReports) {
    	panLog.appendLog(newReports);
    }
}
