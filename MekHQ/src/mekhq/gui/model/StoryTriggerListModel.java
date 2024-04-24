package mekhq.gui.model;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.storyarc.StoryTrigger;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class StoryTriggerListModel extends AbstractListModel<StoryTrigger> {

    List<StoryTrigger> triggers;

    @Override
    public int getSize() {
        return triggers.size();
    }

    @Override
    public StoryTrigger getElementAt(int index) {
        return triggers.get(index);
    }

    public void setData(List<StoryTrigger> triggers) {
        this.triggers = triggers;
    }

    public DefaultListCellRenderer getRenderer() {
        return new StoryTriggerListModel.StoryTriggerRenderer();
    }

    public class StoryTriggerRenderer extends DefaultListCellRenderer {
        public StoryTriggerRenderer() {
            super();
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            StoryTrigger trigger = getElementAt(index);
            setText(trigger.getDescription());
            return this;
        }
    }
}
