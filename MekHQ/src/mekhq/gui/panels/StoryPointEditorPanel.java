package mekhq.gui.panels;

import mekhq.campaign.storyarc.StoryPoint;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.JScrollablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StoryPointEditorPanel extends AbstractMHQScrollablePanel {

    private StoryPoint storyPoint;

    // What do we need to track
    // name
    // previous story points
    // personality
    // splash image
    // StoryOutcomes and StoryTriggers
    private JTextField txtName;

    public StoryPointEditorPanel(JFrame frame, String name, StoryPoint sp) {
        super(frame, name);
        storyPoint = sp;
        initialize();
    }

    @Override
    protected void initialize() {
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("<html><b>Story Point Name:</b></html>"), gbc);

        txtName = new JTextField(storyPoint.getName());
        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(txtName, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("<html><b>Linking Story Points:</b></html>"), gbc);

        StringBuilder sb = new StringBuilder();
        List<StoryPoint> linkedStoryPoints = storyPoint.getLinkingStoryPoints();
        String SEPARATOR = "";
        for (int i = 0; i < linkedStoryPoints.size(); i++) {
            sb.append(SEPARATOR);
            sb.append(linkedStoryPoints.get(i).getName());
            SEPARATOR = ", ";
        }
        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(new JLabel(sb.toString()), gbc);
    }
}
