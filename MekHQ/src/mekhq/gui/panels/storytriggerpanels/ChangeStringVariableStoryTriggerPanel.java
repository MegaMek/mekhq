package mekhq.gui.panels.storytriggerpanels;

import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.storyarc.storytrigger.ChangeStringVariableStoryTrigger;

import javax.swing.*;
import java.awt.*;

public class ChangeStringVariableStoryTriggerPanel extends StoryTriggerPanel {

    private JComboBox<String> comboKey;
    private JTextField txtValue;

    public ChangeStringVariableStoryTriggerPanel(JFrame frame, String name, StoryTrigger trigger) {
        super(frame, name, trigger);
    }

    @Override
    protected void createMainPanel() {
        ChangeStringVariableStoryTrigger changeTrigger;
        if(!(getStoryTrigger() instanceof ChangeStringVariableStoryTrigger)) {
            getMainPanel().add(new JLabel("Incorrect story trigger type!"));
            return;
        } else {
            changeTrigger = (ChangeStringVariableStoryTrigger) getStoryTrigger();
        }

        getMainPanel().setLayout(new GridBagLayout());
        comboKey = new JComboBox<>();

        for(String key : getStoryTrigger().getStoryArc().getCustomStringVariables().keySet()) {
            comboKey.addItem(key);
        }
        comboKey.setSelectedItem(changeTrigger.getKey());

        txtValue = new JTextField(changeTrigger.getValue());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        getMainPanel().add(comboKey, gbc);
        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        getMainPanel().add(txtValue, gbc);
    }

    @Override
    public void updateStoryStrigger() {
        ChangeStringVariableStoryTrigger changeTrigger;
        if(!(getStoryTrigger() instanceof ChangeStringVariableStoryTrigger)) {
            return;
        } else {
            changeTrigger = (ChangeStringVariableStoryTrigger) getStoryTrigger();
        }
        changeTrigger.setKey((String) comboKey.getSelectedItem());
        changeTrigger.setValue(txtValue.getText());
    }
}
