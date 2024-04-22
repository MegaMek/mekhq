package mekhq.gui.panels;

import mekhq.campaign.storyarc.StoryPoint;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.JScrollablePanel;

import javax.swing.*;
import java.awt.*;

public class StoryPointEditorPanel extends AbstractMHQScrollablePanel {

    private StoryPoint storyPoint;

    public StoryPointEditorPanel(JFrame frame, String name, StoryPoint sp) {
        super(frame, name);
        storyPoint = sp;
        initialize();
    }

    @Override
    protected void initialize() {
        add(new JLabel(storyPoint.getName()));
    }
}
