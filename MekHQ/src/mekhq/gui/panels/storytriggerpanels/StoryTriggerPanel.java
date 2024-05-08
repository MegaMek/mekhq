package mekhq.gui.panels.storytriggerpanels;

import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;

import javax.swing.*;
import java.awt.*;

public abstract class StoryTriggerPanel extends AbstractMHQScrollablePanel {

    private StoryTrigger storyTrigger;
    protected JPanel panMain;


    public StoryTriggerPanel(JFrame frame, String name, StoryTrigger trigger) {
        super(frame, name);
        this.storyTrigger = trigger;
        initialize();
    }

    public StoryTrigger getStoryTrigger() {
        return storyTrigger;
    }

    protected JPanel getMainPanel() {
        return panMain;
    }

    @Override
    protected void initialize() {
        setLayout(new BorderLayout());
        panMain = new JPanel();
        add(new JLabel(storyTrigger.getClass().getSimpleName()), BorderLayout.PAGE_START);
        createMainPanel();
        add(panMain, BorderLayout.CENTER);
    }

    protected abstract void createMainPanel();

    public abstract void updateStoryStrigger();
}
