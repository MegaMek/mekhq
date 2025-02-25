package mekhq.gui.panels.storytriggerpanels;

import mekhq.campaign.storyarc.StoryTrigger;

import javax.swing.*;

public class EndArcStoryTriggerPanel extends StoryTriggerPanel {
    public EndArcStoryTriggerPanel(JFrame frame, String name, StoryTrigger trigger) {
        super(frame, name, trigger);
    }

    @Override
    protected void createMainPanel() {
        getMainPanel().add(new JLabel("<html>This Story Trigger will finish the story arc.</html>"));
    }

    @Override
    public void updateStoryStrigger() {
        // nothing to update
    }
}
