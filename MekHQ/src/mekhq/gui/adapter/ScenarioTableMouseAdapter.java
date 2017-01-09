package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputAdapter;

import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.gui.BriefingTab;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.CustomizeScenarioDialog;

public class ScenarioTableMouseAdapter extends MouseInputAdapter implements
        ActionListener {
    private CampaignGUI gui;
    private BriefingTab briefingTab;

    public ScenarioTableMouseAdapter(CampaignGUI gui, BriefingTab briefingTab) {
        super();
        this.gui = gui;
        this.briefingTab = briefingTab;
    }

    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        Scenario scenario = briefingTab.getScenarioModel().getScenario(briefingTab.getScenarioTable()
                .getSelectedRow());
        Mission mission = briefingTab.getSelectedMission();
        if (command.equalsIgnoreCase("EDIT")) {
            if (null != mission && null != scenario) {
                CustomizeScenarioDialog csd = new CustomizeScenarioDialog(
                        gui.getFrame(), true, scenario, mission, gui.getCampaign());
                csd.setVisible(true);
                gui.refreshScenarioList();
            }
        } else if (command.equalsIgnoreCase("REMOVE")) {
            if (0 == JOptionPane.showConfirmDialog(null,
                    "Do you really want to delete the scenario?",
                    "Delete Scenario?", JOptionPane.YES_NO_OPTION)) {
                gui.getCampaign().removeScenario(scenario.getId());
                gui.refreshScenarioList();
                gui.refreshOrganization();
                gui.refreshPersonnelList();
                gui.refreshUnitList();
                gui.refreshOverview();
            }

        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        if (e.isPopupTrigger()) {
            int row = briefingTab.getScenarioTable().getSelectedRow();
            if (row < 0) {
                return;
            }
            @SuppressWarnings("unused")
            // FIXME
            Scenario scenario = briefingTab.getScenarioModel().getScenario(row);
            JMenuItem menuItem = null;
            JMenu menu = null;
            @SuppressWarnings("unused")
            // Placeholder for future expansion
            JCheckBoxMenuItem cbMenuItem = null;
            // **lets fill the pop up menu**//
            menuItem = new JMenuItem("Edit...");
            menuItem.setActionCommand("EDIT");
            menuItem.addActionListener(this);
            popup.add(menuItem);
            // GM mode
            menu = new JMenu("GM Mode");
            // remove scenario
            menuItem = new JMenuItem("Remove Scenario");
            menuItem.setActionCommand("REMOVE");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            // end
            popup.addSeparator();
            popup.add(menu);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}