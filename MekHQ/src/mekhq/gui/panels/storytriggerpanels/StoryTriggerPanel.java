package mekhq.gui.panels.storytriggerpanels;

import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public abstract class StoryTriggerPanel extends AbstractMHQScrollablePanel {

    private StoryTrigger storyTrigger;
    protected JPanel panMain;
    private JButton btnDelete;


    public StoryTriggerPanel(JFrame frame, String name, StoryTrigger trigger) {
        super(frame, name);
        this.storyTrigger = trigger;
        initialize();
    }

    public JButton getDeleteButton() {
        return btnDelete;
    }
    public StoryTrigger getStoryTrigger() {
        return storyTrigger;
    }

    protected JPanel getMainPanel() {
        return panMain;
    }

    @Override
    protected void initialize() {
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(storyTrigger.getClass().getSimpleName()),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        setLayout(new BorderLayout());
        btnDelete = new JButton("Delete Trigger");
        JPanel panTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panTop.add(btnDelete);
        add(panTop, BorderLayout.PAGE_START);
        panMain = new JPanel();
        createMainPanel();
        add(panMain, BorderLayout.CENTER);
    }

    protected abstract void createMainPanel();

    public abstract void updateStoryStrigger();
}
