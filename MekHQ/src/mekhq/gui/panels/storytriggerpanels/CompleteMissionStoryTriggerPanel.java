package mekhq.gui.panels.storytriggerpanels;

import megamek.client.ui.baseComponents.MMComboBox;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.storyarc.storypoint.MissionStoryPoint;
import mekhq.campaign.storyarc.storytrigger.CompleteMissionStoryTrigger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CompleteMissionStoryTriggerPanel extends StoryTriggerPanel {

    private List<UUID> missions;
    private JComboBox<String> comboMission;
    private MMComboBox<MissionStatus> comboStatus;
    public CompleteMissionStoryTriggerPanel(JFrame frame, String name, CompleteMissionStoryTrigger trigger) {
        super(frame, name, trigger);
    }

    @Override
    protected void createMainPanel() {
        comboMission = new JComboBox<>();
        // need to track a list of UUIDs so we can get the right one
        missions = new ArrayList<>();

        for(StoryPoint storyPoint : getStoryTrigger().getStoryArc().getStoryPoints()) {
            if(storyPoint instanceof MissionStoryPoint) {
                comboMission.addItem(storyPoint.getName());
                missions.add(storyPoint.getId());
            }
        }
        UUID currentStoryPointId = ((CompleteMissionStoryTrigger) getStoryTrigger()).getMissionStoryPointId();
        StoryPoint currentMission = null;
        if(currentStoryPointId != null) {
            currentMission = getStoryTrigger().getStoryArc().getStoryPoint(currentStoryPointId);
        }
        if(currentMission != null) {
            comboMission.setSelectedItem(currentMission.getName());
        }

        comboStatus = new MMComboBox<>("comboStatus", MissionStatus.values());
        comboStatus.setSelectedItem(((CompleteMissionStoryTrigger) getStoryTrigger()).getMissionStatus());

        getMainPanel().setLayout(new GridLayout(0, 2));
        getMainPanel().add(comboMission);
        getMainPanel().add(comboStatus);

    }

    @Override
    public void updateStoryStrigger() {
        ((CompleteMissionStoryTrigger) getStoryTrigger()).setMissionStoryPointId(missions.get(comboMission.getSelectedIndex()));
        ((CompleteMissionStoryTrigger) getStoryTrigger()).setMissionStatus(comboStatus.getSelectedItem());
    }
}
