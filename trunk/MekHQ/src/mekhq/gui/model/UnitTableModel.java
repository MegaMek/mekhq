package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;

/**
 * A table Model for displaying information about units
 * @author Jay lawson
 */
public class UnitTableModel extends DataTableModel {

    private static final long serialVersionUID = -5207167419079014157L;

    private Campaign campaign;
    
    public final static int COL_NAME    =    0;
    public final static int COL_TYPE    =    1;
    public final static int COL_WCLASS    =  2;
    public final static int COL_TECH     =   3;
    public final static int COL_WEIGHT =     4;
    public final static int COL_COST    =    5;
    public final static int COL_STATUS   =   6;
    public final static int COL_QUALITY  =   7;
    public final static int COL_PILOT    =   8;
    public final static int COL_CREW     =   9;
    public final static int COL_TECH_CRW =   10;
    public final static int COL_MAINTAIN  =  11;
    public final static int COL_BV        =  12;
    public final static int COL_REPAIR  =    13;
    public final static int COL_PARTS    =   14;
    public final static int COL_QUIRKS   =   15;
    public final static int N_COL =          16;

    public UnitTableModel(Campaign c) {
        data = new ArrayList<Unit>();
        campaign = c;
    }
    
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    @Override
    public String getColumnName(int column) {
        switch(column) {
        case COL_NAME:
            return "Name";
        case COL_TYPE:
            return "Type";
        case COL_WEIGHT:
            return "Weight";
        case COL_WCLASS:
            return "Class";
        case COL_COST:
            return "Value";
        case COL_TECH:
            return "Tech";
        case COL_QUALITY:
            return "Quality";
        case COL_STATUS:
            return "Status";
        case COL_PILOT:
            return "Assigned to";
        case COL_TECH_CRW:
            return "Tech";
        case COL_CREW:
            return "Crew";
        case COL_BV:
            return "BV";
        case COL_REPAIR:
            return "# Repairs";
        case COL_PARTS:
            return "# Parts";
        case COL_QUIRKS:
            return "Quirks";
        case COL_MAINTAIN:
            return "Maintenance Costs";
        default:
            return "?";
        }
    }

    public int getColumnWidth(int c) {
        switch(c) {
        case COL_WCLASS:
        case COL_TYPE:
            return 50;
        case COL_COST:
        case COL_STATUS:
            return 80;
        case COL_PILOT:
        case COL_TECH:
        case COL_NAME:
        case COL_TECH_CRW:
            return 150;
        default:
            return 20;
        }
    }

    public int getAlignment(int col) {
        switch(col) {
        case COL_QUALITY:
        case COL_QUIRKS:
        case COL_CREW:
            return SwingConstants.CENTER;
        case COL_WEIGHT:
        case COL_COST:
        case COL_MAINTAIN:
        case COL_REPAIR:
        case COL_PARTS:
        case COL_BV:
            return SwingConstants.RIGHT;
        default:
            return SwingConstants.LEFT;
        }
    }

    public String getTooltip(int row, int col) {
        Unit u = getUnit(row);
        switch(col) {
        case COL_STATUS:
            if(u.isRefitting()) {
                return u.getRefit().getDesc();
            }
            return null;
        case COL_QUIRKS:
            return u.getQuirksList();
        default:
            return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public Unit getUnit(int i) {
        if(i >= data.size()) {
            return null;
        }
        return (Unit)data.get(i);
    }

    public Object getValueAt(int row, int col) {
        Unit u;
        if(data.isEmpty()) {
            return "";
        } else {
            u = getUnit(row);
        }
        Entity e = u.getEntity();
        //PilotPerson pp = u.getPilot();
        DecimalFormat format = new DecimalFormat();
        if(null == e) {
            return "?";
        }
        if(col == COL_NAME) {
            return u.getName();
        }
        if(col == COL_TYPE) {
            return UnitType.getTypeDisplayableName(UnitType.determineUnitTypeCode(e));
        }
        if(col == COL_WEIGHT) {
            return e.getWeight();
        }
        if(col == COL_WCLASS) {
            return e.getWeightClassName();
        }
        if(col == COL_COST) {
            return format.format(u.getSellValue());
        }
        if(col == COL_MAINTAIN) {
            return u.getMaintenanceCost();
        }
        if(col == COL_TECH) {
            return TechConstants.getLevelDisplayableName(e.getTechLevel());
        }
        if(col == COL_QUALITY) {
            return u.getQualityName();
        }
        if(col == COL_STATUS) {
            return u.getStatus();
        }
        if(col == COL_TECH_CRW) {
            if(null != u.getTech()) {
                return u.getTech().getFullTitle();
            } else {
                return "-";
            }
        }
        if(col == COL_PILOT) {
            if(null == u.getCommander()) {
                return "-";
            } else {
                return u.getCommander().getFullTitle();
            }
        }
        if(col == COL_BV) {
            if(null == u.getEntity().getCrew()) {
                return e.calculateBattleValue(true, true);
            } else {
                return e.calculateBattleValue(true, false);
            }
        }
        if(col == COL_REPAIR) {
            return u.getPartsNeedingFixing().size();
        }
        if(col == COL_PARTS) {
            return u.getPartsNeeded().size();
        }
        if(col == COL_QUIRKS) {
            return e.countQuirks();
        }
        if(col == COL_CREW) {
            return u.getActiveCrew().size() + "/" + u.getFullCrewSize();
        }
        return "?";
    }
    
    public Campaign getCampaign() {
        return campaign;
    }
    
    public void refreshData() {
        setData(getCampaign().getUnits());
    }

    public TableCellRenderer getRenderer(boolean graphic, IconPackage icons) {
        if(graphic) {
            return new UnitTableModel.VisualRenderer(icons);
        }
        return new UnitTableModel.Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 9054581142945717303L;

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);
            setOpaque(true);
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            setHorizontalAlignment(getAlignment(actualCol));
            setToolTipText(getTooltip(actualRow, actualCol));
            Unit u = getUnit(actualRow);

            setForeground(Color.BLACK);
            if (isSelected) {
                setBackground(Color.DARK_GRAY);
                setForeground(Color.WHITE);
            } else {

                if (u.isDeployed()) {
                    setBackground(Color.LIGHT_GRAY);
                }
                else if(!u.isPresent()) {
                    setBackground(Color.ORANGE);
                }
                else if(u.isRefitting()) {
                    setBackground(Color.CYAN);
                }
                else if ((null != u)
                        && (u.isMothballing())) {
                    setBackground(new Color(153,153,255));
                } 
                else if ((null != u)
                        && (u.isMothballed())) {
                    setBackground(new Color(204, 204, 255));
                } 
                else if (null != u && !u.isRepairable()) {
                    setBackground(new Color(190, 150, 55));
                } else if ((null != u) && !u.isFunctional()) {
                    setBackground(new Color(205, 92, 92));
                } else if ((null != u)
                        && (u.getPartsNeedingFixing().size() > 0)) {
                    setBackground(new Color(238, 238, 0));
                } else if (u.getEntity() instanceof Infantry
                        && u.getActiveCrew().size() < u.getFullCrewSize()) {
                    setBackground(Color.RED);
                }
                else {
                    setBackground(Color.WHITE);
                }
            }
            return this;
        }

    }

    public class VisualRenderer extends BasicInfo implements TableCellRenderer {

        private static final long serialVersionUID = -9154596036677641620L;

        public VisualRenderer(IconPackage icons) {
            super(icons);
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = this;
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            String color = "black";
            if(isSelected) {
                color = "white";
            }
            setText(getValueAt(actualRow, actualCol).toString(), color);
            Unit u = getUnit(actualRow);
            if (actualCol == COL_PILOT) {
                Person p = u.getCommander();
                if(null != p) {
                    setPortrait(p);
                    setText(p.getFullDesc(), color);
                } else {
                    clearImage();
                }
            }
            if (actualCol == COL_TECH_CRW) {
                Person p = u.getTech();
                if(null != p) {
                    setPortrait(p);
                    setText(p.getFullDesc(), color);
                } else {
                    clearImage();
                }
            }
            if(actualCol == COL_WCLASS) {
                if(null != u) {
                    String desc = "<html><b>" + u.getName() + "</b><br>";
                    desc += u.getEntity().getWeightClassName();
                    if(!(u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship)) {
                        desc += " " + UnitType.getTypeDisplayableName(UnitType.determineUnitTypeCode(u.getEntity()));
                    }
                    desc += "<br>" + u.getStatus() + "</html>";
                    setText(desc, color);
                    Image mekImage = getImageFor(u);
                    if(null != mekImage) {
                        setImage(mekImage);
                    } else {
                        clearImage();
                    }
                } else {
                    clearImage();
                }
            }

            if (isSelected) {
                c.setBackground(Color.DARK_GRAY);
            } else {
                // tiger stripes
                if ((row % 2) == 0) {
                    c.setBackground(new Color(220, 220, 220));
                } else {
                    c.setBackground(Color.WHITE);

                }
            }
            return c;
        }
    }

}