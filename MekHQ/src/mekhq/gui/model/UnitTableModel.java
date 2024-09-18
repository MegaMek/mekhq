/*
 * Copyright (c) 2013-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.model;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;
import mekhq.gui.utilities.MekHqTableCellRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;

/**
 * A table Model for displaying information about units
 * @author Jay lawson
 */
public class UnitTableModel extends DataTableModel {
    //region Variable Declarations
    public final static int COL_NAME    =    0;
    public final static int COL_TYPE    =    1;
    public final static int COL_WCLASS    =  2;
    public final static int COL_TECH     =   3;
    public final static int COL_WEIGHT =     4;
    public final static int COL_COST    =    5;
    public final static int COL_STATUS   =   6;
    public final static int COL_CONDITION  = 7;
    public final static int COL_CREW_STATE = 8;
    public final static int COL_QUALITY  =   9;
    public final static int COL_PILOT    =   10;
    public final static int COL_FORCE    =   11;
    public final static int COL_CREW     =   12;
    public final static int COL_TECH_CRW =   13;
    public final static int COL_MAINTAIN  =  14;
    public final static int COL_BV        =  15;
    public final static int COL_REPAIR  =    16;
    public final static int COL_PARTS    =   17;
    public final static int COL_SITE     =   18;
    public final static int COL_QUIRKS   =   19;
    public final static int COL_RSTATUS   =  20;
    public final static int N_COL =          21;

    private Campaign campaign;
    //endregion Variable Declarations

    public UnitTableModel(Campaign c) {
        data = new ArrayList<Unit>();
        campaign = c;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case COL_NAME -> "Name";
            case COL_TYPE -> "Type";
            case COL_WCLASS -> "Class";
            case COL_TECH -> "Tech";
            case COL_WEIGHT -> "Weight";
            case COL_COST -> "Value";
            case COL_STATUS -> "Status";
            case COL_CONDITION -> "Condition";
            case COL_CREW_STATE -> "Crew State";
            case COL_QUALITY -> "Quality";
            case COL_PILOT -> "Assigned to";
            case COL_FORCE -> "Force";
            case COL_CREW -> "Crew";
            case COL_TECH_CRW -> "Tech Crew";
            case COL_MAINTAIN -> "Maintenance";
            case COL_BV -> "BV";
            case COL_REPAIR -> "# Repairs";
            case COL_PARTS -> "# Parts";
            case COL_SITE -> "Site";
            case COL_QUIRKS -> "Quirks";
            case COL_RSTATUS -> "Mode";
            default -> "?";
        };
    }

    public int getColumnWidth(final int columnId) {
        return switch (columnId) {
            case COL_NAME, COL_TECH, COL_PILOT, COL_FORCE, COL_TECH_CRW -> 150;
            case COL_TYPE, COL_WCLASS, COL_SITE -> 50;
            case COL_COST, COL_STATUS, COL_RSTATUS, COL_CREW -> 40;
            case COL_PARTS -> 10;
            default -> 20;
        };
    }

    public int getAlignment(int col) {
        return switch (col) {
            case COL_WEIGHT, COL_COST, COL_MAINTAIN, COL_BV, COL_REPAIR, COL_PARTS -> SwingConstants.RIGHT;
            case COL_QUALITY, COL_CREW, COL_QUIRKS, COL_RSTATUS -> SwingConstants.CENTER;
            default -> SwingConstants.LEFT;
        };
    }

    /**
     * Returns the tooltip for the specified row and column.
     *
     * @param rowIndex    the index of the row
     * @param columnIndex the index of the column
     * @return the tooltip for the specified row and column, or {@code null} if no tooltip is available
     */
    public @Nullable String getTooltip(int rowIndex, int columnIndex) {
        Unit unit = getUnit(rowIndex);

        if (unit == null) {
            return null;
        }

        return switch (columnIndex) {
            case COL_STATUS -> unit.isRefitting() ? unit.getRefit().getDesc() : null;
            case COL_CREW_STATE -> unit.getCrewState().getToolTipText();
            case COL_CREW -> getCrewTooltip(unit);
            case COL_QUIRKS -> unit.getQuirksList();
            default -> null;
        };
    }

    /**
     * Returns the tooltip for the crew status of a given unit.
     *
     * @param unit the unit for which to get the crew tooltip
     * @return the crew tooltip as an HTML string
     */
    static String getCrewTooltip(Unit unit) {
        int gunnersNeeded = unit.getTotalGunnerNeeds();
        int gunnersAssigned = unit.getGunners().size();

        int driversNeeded = unit.getTotalDriverNeeds();
        int driversAssigned = unit.getDrivers().size();

        Entity entity = unit.getEntity();
        int navigatorsNeeded = entity instanceof Jumpship && !(entity instanceof SpaceStation) ? 1 : 0;
        int navigatorsAssigned = unit.getNavigator() == null ? 0 : 1;

        int crewNeeded = unit.getTotalCrewNeeds();
        int crewAssigned = unit.getCrew().size() - (gunnersAssigned + driversAssigned + navigatorsAssigned);

        StringBuilder report = new StringBuilder("<html>");

        if (driversNeeded > 0) {
            appendReport(report, "Drivers", driversAssigned, driversNeeded);
        }

        if (gunnersNeeded > 0) {
            report.append("<br>");
            appendReport(report, "Gunners", gunnersAssigned, gunnersNeeded);
        }

        if (crewNeeded > 0) {
            report.append("<br>");
            appendReport(report, "Crew", crewAssigned, crewNeeded);
        }

        if (navigatorsNeeded > 0) {
            report.append("<br>");
            appendReport(report, "Navigator", navigatorsAssigned, navigatorsNeeded);
        }

        report.append("</html>");

        return report.toString();
    }

    /**
     * Appends a crew report line to the provided StringBuilder.
     *
     * @param report the {@link StringBuilder} to append to
     * @param title the title of the crew role (e.g., "Driver", "Gunner")
     * @param assigned the number of crew members assigned to the role
     * @param needed the number of crew members needed for the role
     */
    private static void appendReport(StringBuilder report, String title, int assigned, int needed) {
        report.append(String.format("<b>%s: </b>%d/%d", title, assigned, needed));
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
        return (i < data.size()) ? (Unit) data.get(i) : null;
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (data.isEmpty() || (row < 0) || (row >= data.size())) {
            return "";
        }

        Unit u = getUnit(row);
        Entity e = u.getEntity();
        if (e == null) {
            return "?";
        }

        return switch (col) {
            case COL_NAME -> u.getName();
            case COL_TYPE -> UnitType.getTypeDisplayableName(e.getUnitType());
            case COL_WCLASS -> e.getWeightClassName();
            case COL_TECH -> TechConstants.getLevelDisplayableName(e.getTechLevel());
            case COL_WEIGHT -> e.getWeight();
            case COL_COST -> u.getSellValue().toAmountAndSymbolString();
            case COL_STATUS -> u.getStatus();
            case COL_CONDITION -> u.getCondition();
            case COL_CREW_STATE -> u.getCrewState();
            case COL_QUALITY -> u.getQualityName();
            case COL_PILOT -> (u.getCommander() != null) ? u.getCommander().getHTMLTitle() : "-";
            case COL_FORCE -> {
                Force force = u.getCampaign().getForce(u.getForceId());
                yield (force != null) ? force.getFullName() : "-";
            }
            case COL_CREW -> u.getActiveCrew().size() + "/" + u.getFullCrewSize();
            case COL_TECH_CRW -> (u.getTech() != null) ? u.getTech().getHTMLTitle() : "-";
            case COL_MAINTAIN -> u.getMaintenanceCost().toAmountAndSymbolString();
            case COL_BV -> e.calculateBattleValue(true, u.getEntity().getCrew() == null);
            case COL_REPAIR -> u.getPartsNeedingFixing().size();
            case COL_PARTS -> u.getPartsNeeded().size();
            case COL_SITE -> Unit.getSiteName(u.getSite());
            case COL_QUIRKS -> e.countQuirks();
            case COL_RSTATUS -> u.isSalvage() ? "Strip" : "Repair";
            default -> "?";
        };
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public TableCellRenderer getRenderer(boolean graphic) {
        return (graphic) ? new VisualRenderer() : new Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            setHorizontalAlignment(getAlignment(actualCol));
            setToolTipText(getTooltip(actualRow, actualCol));
            Unit u = getUnit(actualRow);

            if (!isSelected) {
                setForeground(u.determineForegroundColor("Table"));
                setBackground(u.determineBackgroundColor("Table"));
            }
            return this;
        }
    }

    public class VisualRenderer extends BasicInfo implements TableCellRenderer {
        public VisualRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = this;
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);

            setText(getValueAt(actualRow, actualCol).toString());

            Unit u = getUnit(actualRow);
            switch (actualCol) {
                case COL_WCLASS: {
                    String desc = "<html><b>" + u.getName() + "</b><br>";
                    desc += u.getEntity().getWeightClassName();
                    if (!((u.getEntity() instanceof SmallCraft) || (u.getEntity() instanceof Jumpship))) {
                        desc += ' ' + UnitType.getTypeDisplayableName(u.getEntity().getUnitType());
                    }
                    desc += "<br>" + u.getStatus() + "</html>";
                    setHtmlText(desc);
                    Image mekImage = u.getImage(this);
                    if (mekImage != null) {
                        setImage(mekImage);
                    } else {
                        clearImage();
                    }
                    break;
                }
                case COL_PILOT: {
                    final Person p = u.getCommander();
                    if (p != null) {
                        setText(p.getFullDesc(getCampaign()));
                        setImage(p.getPortrait().getImage(54));
                    } else {
                        clearImage();
                    }
                    break;
                }
                case COL_FORCE: {
                    Force force = getCampaign().getForceFor(u);
                    if (force != null) {
                        StringBuilder desc = new StringBuilder("<html><b>").append(force.getName())
                                .append("</b>");
                        Force parent = force.getParentForce();
                        // cut off after three lines and don't include the top level
                        int lines = 1;
                        while ((parent != null) && (parent.getParentForce() != null) && (lines < 4)) {
                            desc.append("<br>").append(parent.getName());
                            lines++;
                            parent = parent.getParentForce();
                        }
                        desc.append("</html>");
                        setHtmlText(desc.toString());
                        final Image forceImage = force.getForceIcon().getImage(54);
                        if (forceImage != null) {
                            setImage(forceImage);
                        } else {
                            clearImage();
                        }
                    } else {
                        clearImage();
                    }
                    break;
                }
                case COL_TECH_CRW: {
                    final Person p = u.getTech();
                    if (p != null) {
                        setText(p.getFullDesc(getCampaign()));
                        setImage(p.getPortrait().getImage(54));
                    } else {
                        clearImage();
                    }
                    break;
                }
                default:
                    break;
            }

            MekHqTableCellRenderer.setupTableColors(c, table, isSelected, hasFocus, row);
            return c;
        }
    }
}
