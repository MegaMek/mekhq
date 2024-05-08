package mekhq.gui.panels.storytriggerpanels;

import mekhq.campaign.storyarc.StoryTrigger;

import javax.swing.*;

public class FakeStoryTriggerPanel extends StoryTriggerPanel {

    public FakeStoryTriggerPanel(JFrame frame, String name, StoryTrigger trigger) {
        super(frame, name, trigger);
    }

    @Override
    protected void createMainPanel() {
        getMainPanel().add(new JLabel("Test!"));
    }

    @Override
    public void updateStoryStrigger() {
        // nothing to update
    }

}
