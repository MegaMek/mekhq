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

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import mekhq.gui.CampaignGUI;
import mekhq.gui.DailyReportLogPanel;
import mekhq.gui.ReportHyperlinkListener;

public class AdvanceDaysDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;
    private JSpinner spnDays;
    private JButton btnStart;
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
        pnlNumDays.add(btnStart);
        getContentPane().add(pnlNumDays, BorderLayout.NORTH);

        logPanel = new DailyReportLogPanel(listener);
        getContentPane().add(logPanel, BorderLayout.CENTER);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource().equals(btnStart)) {
            for (int numDays = (int)spnDays.getValue(); numDays > 0; numDays--) {
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
                gui.getCampaign().newDay();
                String newLogString = logPanel.getLogText();
                newLogString = newLogString.concat(gui.getCampaign().getCurrentReportHTML());
                logPanel.refreshLog(newLogString);
            }
            gui.refreshScenarioList();
            gui.refreshMissions();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshTaskList();
            gui.refreshAcquireList();
            gui.refreshTechsList();
            gui.refreshPartsList();
            gui.refreshPatientList();
            gui.refreshDoctorsList();
            gui.refreshCalendar();
            gui.refreshLocation();
            gui.refreshOrganization();
            gui.refreshReport();
            gui.refreshFunds();
            gui.refreshFinancialTransactions();
            gui.refreshOverview();
            gui.getMapPanel().repaint();
        }
    }
}
