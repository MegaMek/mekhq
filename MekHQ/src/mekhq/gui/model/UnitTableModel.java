/*
 * Copyright (c) 2013-2020 - The MegaMek Team. All Rights Reserved.
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

import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;
import mekhq.gui.MekHqColors;
import mekhq.gui.preferences.ColorPreference;
import mekhq.gui.utilities.MekHqTableCellRenderer;

/**
 * A table Model for displaying information about units
 * @author Jay lawson
 */
public class UnitTableModel extends DataTableModel {
    //region Variable Declarations
    private static final long serialVersionUID = -5207167419079014157L;

    public final static int COL_NAME    =    0;
    public final static int COL_TYPE    =    1;
    public final static int COL_WCLASS    =  2;
    public final static int COL_TECH     =   3;
    public final static int COL_WEIGHT =     4;
    public final static int COL_COST    =    5;
    public final static int COL_STATUS   =   6;
    public final static int COL_QUALITY  =   7;
    public final static int COL_PILOT    =   8;
    public final static int COL_FORCE    =   9;
    public final static int COL_CREW     =   10;
    public final static int COL_TECH_CRW =   11;
    public final static int COL_MAINTAIN  =  12;
    public final static int COL_BV        =  13;
    public final static int COL_REPAIR  =    14;
    public final static int COL_PARTS    =   15;
    public final static int COL_SITE     =   16;
    public final static int COL_QUIRKS   =   17;
    public final static int COL_RSTATUS   =  18;
    public final static int N_COL =          19;

    private Campaign campaign;

    private final MekHqColors colors = new MekHqColors();
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
            case COL_FORCE:
                return "Force";
            case COL_TECH_CRW:
                return "Tech Crew";
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
            case COL_SITE:
                return "Site";
            case COL_RSTATUS:
                return "Repair Status";
            default:
                return "?";
        }
    }

    public int getColumnWidth(int c) {
        switch (c) {
            case COL_WCLASS:
            case COL_TYPE:
            case COL_SITE:
                return 50;
            case COL_COST:
            case COL_STATUS:
            case COL_RSTATUS:
                return 80;
            case COL_PILOT:
            case COL_FORCE:
            case COL_TECH:
            case COL_NAME:
            case COL_TECH_CRW:
                return 150;
            default:
                return 20;
        }
    }

    public int getAlignment(int col) {
        switch (col) {
            case COL_QUALITY:
            case COL_QUIRKS:
            case COL_CREW:
            case COL_RSTATUS:
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
        switch (col) {
            case COL_STATUS:
                return u.isRefitting() ? u.getRefit().getDesc() : null;
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
        Unit u;
        if (data.isEmpty() || (row < 0) || (row >= data.size())) {
            return "";
        } else {
            u = getUnit(row);
        }
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
            case COL_QUALITY:
                return u.getQualityName();
            case COL_PILOT:
                return (u.getCommander() != null) ? u.getCommander().getFullTitle() : "-";
            case COL_FORCE:
                Force force = u.getCampaign().getForce(u.getForceId());
                return (force != null) ? force.getFullName() : "-";
            case COL_CREW:
                return u.getActiveCrew().size() + "/" + u.getFullCrewSize();
            case COL_TECH_CRW:
                return (u.getTech() != null) ? u.getTech().getFullTitle() : "-";
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
        private static final long serialVersionUID = 9054581142945717303L;

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
                if (u.isDeployed()) {
                    applyColors(colors.getDeployed());
                } else if (!u.isPresent()) {
                    applyColors(colors.getInTransit());
                } else if (u.isRefitting()) {
                    applyColors(colors.getRefitting());
                } else if (u.isMothballing()) {
                    applyColors(colors.getMothballing());
                } else if (u.isMothballed()) {
                    applyColors(colors.getMothballed());
                } else if (!u.isRepairable()) {
                    applyColors(colors.getNotRepairable());
                } else if (!u.isFunctional()) {
                    applyColors(colors.getNonFunctional());
                } else if (u.hasPartsNeedingFixing()) {
                    applyColors(colors.getNeedsPartsFixed());
                } else if (u.isUnmaintained()) {
                    applyColors(colors.getUnmaintained());
                } else if (u.getActiveCrew().size() < u.getFullCrewSize()) {
                    applyColors(colors.getUncrewed());
                } else {
                    setForeground(UIManager.getColor("Table.foreground"));
                    setBackground(UIManager.getColor("Table.background"));
                }
            }
            return this;
        }

        private void applyColors(ColorPreference c) {
            setBackground(c.getColor().orElseGet(() -> UIManager.getColor("Table.background")));
            setForeground(c.getAlternateColor().orElseGet(() -> UIManager.getColor("Table.foreground")));
        }
    }

    public class VisualRenderer extends BasicInfo implements TableCellRenderer {
        private static final long serialVersionUID = -9154596036677641620L;

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
                        desc += " " + UnitType.getTypeDisplayableName(u.getEntity().getUnitType());
                    }
                    desc += "<br>" + u.getStatus() + "</html>";
                    setHtmlText(desc);
                    Image mekImage = getImageFor(u);
                    if (mekImage != null) {
                        setImage(mekImage);
                    } else {
                        clearImage();
                    }
                    break;
                }
                case COL_PILOT: {
                    Person p = u.getCommander();
                    if (p != null) {
                        setPortrait(p);
                        setText(p.getFullDesc());
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
                        //cut off after three lines and don't include the top level
                        int lines = 1;
                        while ((parent != null) && (parent.getParentForce() != null) && (lines < 4)) {
                            desc.append("<br>").append(parent.getName());
                            lines++;
                            parent = parent.getParentForce();
                        }
                        desc.append("</html>");
                        setHtmlText(desc.toString());
                        Image forceImage = getImageFor(force);
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
                    Person p = u.getTech();
                    if (p != null) {
                        setPortrait(p);
                        setText(p.getFullDesc());
                    } else {
                        clearImage();
                    }
                    break;
                }
            }

            MekHqTableCellRenderer.setupTableColors(c, table, isSelected, hasFocus, row);
            return c;
        }
    }
}
