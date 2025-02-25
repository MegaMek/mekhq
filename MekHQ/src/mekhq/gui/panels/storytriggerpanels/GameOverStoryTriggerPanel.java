package mekhq.gui.panels.storytriggerpanels;

import mekhq.campaign.storyarc.StoryTrigger;

import javax.swing.*;

public class GameOverStoryTriggerPanel extends StoryTriggerPanel {

    public GameOverStoryTriggerPanel(JFrame frame, String name, StoryTrigger trigger) {
        super(frame, name, trigger);
    }

    @Override
    protected void createMainPanel() {
        getMainPanel().add(new JLabel("<html>This Story Trigger will quit the game.</html>"));
    }

    @Override
    public void updateStoryStrigger() {
        // nothing to update
    }
}
