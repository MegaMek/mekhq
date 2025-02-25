package mekhq.gui.panels.storytriggerpanels;

import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.storyarc.storytrigger.AdvanceTimeStoryTrigger;

import javax.swing.*;
import java.awt.*;

public class AdvanceTimeStoryTriggerPanel extends StoryTriggerPanel {

    private JSpinner spinDays;

    public AdvanceTimeStoryTriggerPanel(JFrame frame, String name, AdvanceTimeStoryTrigger trigger) {
        super(frame, name, trigger);
    }

    @Override
    protected void createMainPanel() {
        getMainPanel().setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        getMainPanel().add(new JLabel("Number of days:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        spinDays = new JSpinner(new SpinnerNumberModel(((AdvanceTimeStoryTrigger) getStoryTrigger()).getDays(),
                1, null, 1));
        getMainPanel().add(spinDays, gbc);
    }

    @Override
    public void updateStoryStrigger() {
        ((AdvanceTimeStoryTrigger) getStoryTrigger()).setDays((int) spinDays.getModel().getValue());
    }
}
