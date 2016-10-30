package mekhq.gui.model;

import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import megamek.common.EquipmentType;
import megamek.common.WeaponType;
import mekhq.IconPackage;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.gui.BasicInfo;

/**
 * A table model for displaying work items
 */
public class TaskTableModel extends DataTableModel {
    private static final long serialVersionUID = -6256038046416893994L;

    public TaskTableModel() {
        columnNames = new String[] { "Tasks" };
        data = new ArrayList<Part>();
    }

    public Object getValueAt(int row, int col) {
        return ((Part) data.get(row)).getDesc();
    }

    public Part getTaskAt(int row) {
        return (Part) data.get(row);
    }

    public Part[] getTasksAt(int[] rows) {
        Part[] tasks = new Part[rows.length];
        for (int i = 0; i < rows.length; i++) {
            int row = rows[i];
            tasks[i] = (Part) data.get(row);
        }
        return tasks;
    }

    public TaskTableModel.Renderer getRenderer(IconPackage icons) {
        return new TaskTableModel.Renderer(icons);
    }

    public class Renderer extends BasicInfo implements TableCellRenderer {

        public Renderer(IconPackage icons) {
            super(icons);
        }

        private static final long serialVersionUID = -3052618135259621130L;

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = this;
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            
            Part part = getTaskAt(actualRow);
            String imgPath = null;

            int repairType = Part.findCorrectRepairType(part);
            
            switch (repairType) {
	        	case Part.REPAIR_PART_TYPE.ARMOR:
	        		imgPath = "data/images/misc/repair/armor.png";
	        		break;
	        	case Part.REPAIR_PART_TYPE.AMMO:
	        		imgPath = "data/images/misc/repair/ammo.png";
	        		break;
	        	case Part.REPAIR_PART_TYPE.ACTUATOR:
	        		imgPath = "data/images/misc/repair/actuator.png";
	        		break;
	        	case Part.REPAIR_PART_TYPE.ENGINE:
	        		imgPath = "data/images/misc/repair/engine.png";
	        		break;
	        	case Part.REPAIR_PART_TYPE.ELECTRONICS:
	        		imgPath = "data/images/misc/repair/electronics.png";
	        		break;
	        	case Part.REPAIR_PART_TYPE.HEATSINK:
	        		imgPath = "data/images/misc/repair/heatsink.png";
	        		break;
	        	case Part.REPAIR_PART_TYPE.WEAPON:
	        		EquipmentType equipmentType = null;
	        		
	        		if (part instanceof EquipmentPart) {
	        			equipmentType = ((EquipmentPart)part).getType();
	        		} else if (part instanceof MissingEquipmentPart) {
	        			equipmentType = ((MissingEquipmentPart)part).getType();
	        		}

	        		if (null != equipmentType) {
		        		if (equipmentType.hasFlag(WeaponType.F_LASER)) {
		        			imgPath = "data/images/misc/repair/laser.png";	
		        		} else if (equipmentType.hasFlag(WeaponType.F_MISSILE)) {
		        			imgPath = "data/images/misc/repair/missile.png";	
		        		} else if (equipmentType.hasFlag(WeaponType.F_BALLISTIC)) {
		        			imgPath = "data/images/misc/repair/ballistic.png";	
		        		} else if (equipmentType.hasFlag(WeaponType.F_ARTILLERY)) {
		        			imgPath = "data/images/misc/repair/artillery.png";	
		        		}	        		
	        		}
	        		
	        		break;
	        	case Part.REPAIR_PART_TYPE.MEK_LOCATION:
	        		imgPath = "data/images/misc/repair/location_mek.png";
	        		break;
	        	case Part.REPAIR_PART_TYPE.PHYSICAL_WEAPON:
	        		imgPath = "data/images/misc/repair/melee.png";
	        		break;
            }

            if (null == imgPath) {
            	imgPath = "data/images/misc/repair/equipment.png";
            }
            
            Image imgTool = getToolkit().getImage(imgPath); //$NON-NLS-1$
            
            this.setImage(imgTool);
            setOpaque(true);
            setText("<html>" + getValueAt(actualRow, actualCol).toString() + "</html>", "black");
            if (isSelected) {
                highlightBorder();
            } else {
                unhighlightBorder();
            }
            
            return c;
        }

    }
}