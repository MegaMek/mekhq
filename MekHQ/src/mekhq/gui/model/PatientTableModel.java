package mekhq.gui.model;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.BasicInfo;

/**
 * A table model for displaying personnel in the infirmary
 */
public class PatientTableModel extends AbstractListModel<Person> {
    private static final long serialVersionUID = -1615929049408417297L;

    private ArrayList<Person> patients;
    private final Campaign campaign;

    public PatientTableModel(Campaign c) {
        patients = new ArrayList<Person>();
        campaign = c;
    }

    //fill table with values
    public void setData(ArrayList<Person> data) {
        patients = data;
        this.fireContentsChanged(this, 0, patients.size());
        //fireTableDataChanged();
    }

    @Override
    public Person getElementAt(int index) {
        if (index < 0 || index >= patients.size()) {
            return null;
        }
        return patients.get(index);
    }

    @Override
    public int getSize() {
        return patients.size();
    }

    private Campaign getCampaign() {
        return campaign;
    }

    public PatientTableModel.Renderer getRenderer(IconPackage icons) {
        return new PatientTableModel.Renderer(icons);
    }

    public class Renderer extends BasicInfo implements ListCellRenderer<Object> {
        public Renderer(IconPackage icons) {
            super(icons);
        }

        private static final long serialVersionUID = -406535109900807837L;

        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            Person p = (Person)getElementAt(index);
            setPortrait(p);
            if (getCampaign().getCampaignOptions().useAdvancedMedical()) {
                setHtmlText(p.getInjuriesDesc());
            } else {
                setHtmlText(p.getPatientDesc());
            }
            if (isSelected) {
                highlightBorder();
            } else {
                unhighlightBorder();
            }
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            return this;
        }
    }
}
