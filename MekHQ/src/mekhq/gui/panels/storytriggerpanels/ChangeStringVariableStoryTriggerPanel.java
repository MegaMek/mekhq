package mekhq.gui.panels.storytriggerpanels;

import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.storyarc.storytrigger.ChangeStringVariableStoryTrigger;

import javax.swing.*;
import java.awt.*;

public class ChangeStringVariableStoryTriggerPanel extends StoryTriggerPanel {

    private JComboBox<String> comboKey;
    private JTextField txtValue;

    public ChangeStringVariableStoryTriggerPanel(JFrame frame, String name, ChangeStringVariableStoryTrigger trigger) {
        super(frame, name, trigger);
    }


    @Override
    protected void createMainPanel() {
        getMainPanel().setLayout(new GridBagLayout());

        comboKey = new JComboBox<>();
        for(String key : getStoryTrigger().getStoryArc().getCustomStringVariables().keySet()) {
            comboKey.addItem(key);
        }
        comboKey.setSelectedItem(((ChangeStringVariableStoryTrigger) getStoryTrigger()).getKey());

        txtValue = new JTextField(((ChangeStringVariableStoryTrigger) getStoryTrigger()).getValue());

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
        ((ChangeStringVariableStoryTrigger) getStoryTrigger()).setKey((String) comboKey.getSelectedItem());
        ((ChangeStringVariableStoryTrigger) getStoryTrigger()).setValue(txtValue.getText());
    }
}
