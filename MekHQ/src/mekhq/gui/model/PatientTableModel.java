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
// TODO: Raw types should be fixed
@SuppressWarnings("rawtypes")
public class PatientTableModel extends AbstractListModel {
    private static final long serialVersionUID = -1615929049408417297L;

    ArrayList<Person> patients;
    Campaign campaign;
    
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
    public Object getElementAt(int index) {
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

    public class Renderer extends BasicInfo implements ListCellRenderer {
        public Renderer(IconPackage icons) {
            super(icons);
        }

        private static final long serialVersionUID = -406535109900807837L;

        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            Component c = this;
            setOpaque(true);
            Person p = (Person)getElementAt(index);
            if (getCampaign().getCampaignOptions().useAdvancedMedical()) {
                setText(p.getInjuriesDesc(), "black");
            } else {
                setText(p.getPatientDesc(), "black");
            }
            if (isSelected) {
                highlightBorder();
            } else {
                unhighlightBorder();
            }
            setPortrait(p);
            return c;
        }
    }


}
