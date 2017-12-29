/**
 * @author Dylan Myers <ralgith@gmail.com>
 *
 */
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.event.ReportEvent;
import mekhq.gui.CampaignGUI;
import mekhq.gui.DailyReportLogPanel;
import mekhq.gui.ReportHyperlinkListener;

public class AdvanceDaysDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;
    private JSpinner spnDays;
    private JButton btnStart;
    private JButton btnNextMonth;
    private JLabel lblDays = new JLabel("Days");
    private JPanel pnlNumDays;
    private DailyReportLogPanel logPanel;
    private CampaignGUI gui;
    private ReportHyperlinkListener listener;

    public AdvanceDaysDialog(Frame owner, CampaignGUI gui, ReportHyperlinkListener listener) {
        super(owner, true);
        this.gui = gui;
        this.listener = listener;
        setName("formADD"); // NOI18N
        setTitle("Advanced Days Dialog");
        getContentPane().setLayout(new java.awt.GridBagLayout());
        this.setPreferredSize(new Dimension(500,500));
        this.setMinimumSize(new Dimension(500,500));
        initComponents();
        setLocationRelativeTo(owner);
    }

    public void initComponents() {
        setLayout(new BorderLayout());

        pnlNumDays = new JPanel();
        spnDays = new JSpinner(new SpinnerNumberModel(7, 1, 365, 1));
        ((JSpinner.DefaultEditor)spnDays.getEditor()).getTextField().setEditable(true);
        pnlNumDays.add(spnDays);
        pnlNumDays.add(lblDays);
        btnStart = new JButton("Start Advancement");
        btnStart.addActionListener(this);
        btnNextMonth = new JButton("Advance to Next Month");
        btnNextMonth.addActionListener(this);
        pnlNumDays.add(btnStart);
        pnlNumDays.add(btnNextMonth);
        getContentPane().add(pnlNumDays, BorderLayout.NORTH);

        logPanel = new DailyReportLogPanel(listener);
        getContentPane().add(logPanel, BorderLayout.CENTER);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource().equals(btnStart) || event.getSource().equals(btnNextMonth)) {
            int days = (int)spnDays.getValue();
            boolean firstDay = true;
            MekHQ.registerHandler(this);
            if (event.getSource().equals(btnNextMonth)) {
                //Use java.time to get the number of days to next month.
                //We already need Java 8 anyway, and this is much easier and more accurate.
                GregorianCalendar cal = gui.getCampaign().getCalendar();
                Duration duration = Duration.between(cal.getTime().toInstant(),
                        new java.util.GregorianCalendar(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, 1).getTime().toInstant());
                days = Math.abs((int)duration.toDays());
            }
            for (int numDays = days; numDays > 0; numDays--) {
                spnDays.setValue(numDays);
                if (gui.getCampaign().checkOverDueLoans()
                        || gui.nagShortMaintenance()
                        || (gui.getCampaign().getCampaignOptions().getUseAtB())
                        && (gui.nagShortDeployments() || gui.nagOutstandingScenarios())) {
                    break;
                }
                if (gui.getCampaign().checkRetirementDefections()
                        || gui.getCampaign().checkYearlyRetirements()) {
                    gui.showRetirementDefectionDialog();
                    break;
                }
               if(!gui.getCampaign().newDay()) {
                   break;
               }
                //String newLogString = logPanel.getLogText();
                //newLogString = newLogString.concat(gui.getCampaign().getCurrentReportHTML());
                if(firstDay) {
                    logPanel.refreshLog(gui.getCampaign().getCurrentReportHTML());
                    firstDay = false;
                } else {
                    logPanel.appendLog(Collections.singletonList("<hr/>")); //$NON-NLS-1$
                    logPanel.appendLog(gui.getCampaign().fetchAndClearNewReports());
                }
            }
            MekHQ.unregisterHandler(this);
            
            gui.refreshCalendar();
            gui.refreshLocation();
            gui.initReport();

            gui.refreshAllTabs();
        }
    }
    
    @Subscribe(priority = 1)
    public void reportOverride(ReportEvent ev) {
        ev.cancel();
    }
}
