/*
 * ReportDialog.java
 *
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

import mekhq.gui.CampaignGUI;
import mekhq.gui.ReportHyperlinkListener;
import mekhq.gui.DailyReportLogPanel;

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
    
}
