package mekhq.gui.panels.storytriggerpanels;

import megamek.client.ui.baseComponents.MMComboBox;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.storyarc.storytrigger.SwitchTabStoryTrigger;
import mekhq.gui.enums.MHQTabType;

import javax.swing.*;
import java.awt.*;

public class SwitchTabStoryTriggerPanel extends StoryTriggerPanel {

    MMComboBox<MHQTabType> comboTab;

    public SwitchTabStoryTriggerPanel(JFrame frame, String name, StoryTrigger trigger) {
        super(frame, name, trigger);
    }

    @Override
    protected void createMainPanel() {
        SwitchTabStoryTrigger switchTrigger;
        if(!(getStoryTrigger() instanceof SwitchTabStoryTrigger)) {
            getMainPanel().add(new JLabel("Incorrect story trigger type!"));
            return;
        } else {
            switchTrigger = (SwitchTabStoryTrigger) getStoryTrigger();
        }

        comboTab = new MMComboBox<>("comboTab", MHQTabType.values());
        comboTab.setSelectedItem(switchTrigger.getTab());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        getMainPanel().add(new JLabel("Selected Tab:"), gbc);
        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        getMainPanel().add(comboTab, gbc);

    }

    @Override
    public void updateStoryStrigger() {
        SwitchTabStoryTrigger switchTrigger;
        if(!(getStoryTrigger() instanceof SwitchTabStoryTrigger)) {
            return;
        } else {
            switchTrigger = (SwitchTabStoryTrigger) getStoryTrigger();
        }
        switchTrigger.setTab(comboTab.getSelectedItem());
    }
}
