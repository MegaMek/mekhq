/*
 * Copyright (c) 2013-2022 - The MegaMek Team. All Rights Reserved.
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
        switch (column) {
            case COL_NAME:
                return "Name";
            case COL_TYPE:
                return "Type";
            case COL_WCLASS:
                return "Class";
            case COL_TECH:
                return "Tech";
            case COL_WEIGHT:
                return "Weight";
            case COL_COST:
                return "Value";
            case COL_STATUS:
                return "Status";
            case COL_CONDITION:
                return "Condition";
            case COL_CREW_STATE:
                return "Crew State";
            case COL_QUALITY:
                return "Quality";
            case COL_PILOT:
                return "Assigned to";
            case COL_FORCE:
                return "Force";
            case COL_CREW:
                return "Crew";
            case COL_TECH_CRW:
                return "Tech Crew";
            case COL_MAINTAIN:
                return "Maintenance Costs";
            case COL_BV:
                return "BV";
            case COL_REPAIR:
                return "# Repairs";
            case COL_PARTS:
                return "# Parts";
            case COL_SITE:
                return "Site";
            case COL_QUIRKS:
                return "Quirks";
            case COL_RSTATUS:
                return "Repair Status";
            default:
                return "?";
        }
    }

    public int getColumnWidth(final int columnId) {
        switch (columnId) {
            case COL_NAME:
            case COL_TECH:
            case COL_PILOT:
            case COL_FORCE:
            case COL_TECH_CRW:
                return 150;
            case COL_TYPE:
            case COL_WCLASS:
            case COL_SITE:
                return 50;
            case COL_COST:
            case COL_STATUS:
            case COL_RSTATUS:
                return 80;
            default:
                return 20;
        }
    }

    public int getAlignment(int col) {
        switch (col) {
            case COL_WEIGHT:
            case COL_COST:
            case COL_MAINTAIN:
            case COL_BV:
            case COL_REPAIR:
            case COL_PARTS:
                return SwingConstants.RIGHT;
            case COL_QUALITY:
            case COL_CREW:
            case COL_QUIRKS:
            case COL_RSTATUS:
                return SwingConstants.CENTER;
            default:
                return SwingConstants.LEFT;
        }
    }

    public @Nullable String getTooltip(int row, int col) {
        Unit u = getUnit(row);
        switch (col) {
            case COL_STATUS:
                return u.isRefitting() ? u.getRefit().getDesc() : null;
            case COL_CREW_STATE:
                return u.getCrewState().getToolTipText();
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

        switch (col) {
            case COL_NAME:
                return u.getName();
            case COL_TYPE:
                return UnitType.getTypeDisplayableName(e.getUnitType());
            case COL_WCLASS:
                return e.getWeightClassName();
            case COL_TECH:
                return TechConstants.getLevelDisplayableName(e.getTechLevel());
            case COL_WEIGHT:
                return e.getWeight();
            case COL_COST:
                return u.getSellValue().toAmountAndSymbolString();
            case COL_STATUS:
                return u.getStatus();
            case COL_CONDITION:
                return u.getCondition();
            case COL_CREW_STATE:
                return u.getCrewState();
            case COL_QUALITY:
                return u.getQualityName();
            case COL_PILOT:
                return (u.getCommander() != null) ? u.getCommander().getHTMLTitle() : "-";
            case COL_FORCE:
                Force force = u.getCampaign().getForce(u.getForceId());
                return (force != null) ? force.getFullName() : "-";
            case COL_CREW:
                return u.getActiveCrew().size() + "/" + u.getFullCrewSize();
            case COL_TECH_CRW:
                return (u.getTech() != null) ? u.getTech().getHTMLTitle() : "-";
            case COL_MAINTAIN:
                return u.getMaintenanceCost();
            case COL_BV:
                return e.calculateBattleValue(true, u.getEntity().getCrew() == null);
            case COL_REPAIR:
                return u.getPartsNeedingFixing().size();
            case COL_PARTS:
                return u.getPartsNeeded().size();
            case COL_SITE:
                return Unit.getSiteName(u.getSite());
            case COL_QUIRKS:
                return e.countQuirks();
            case COL_RSTATUS:
                return u.isSalvage() ? "Salvage" : "Repair";
            default:
                return "?";
        }
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public TableCellRenderer getRenderer(boolean graphic) {
        return (graphic) ? new UnitTableModel.VisualRenderer() : new UnitTableModel.Renderer();
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
