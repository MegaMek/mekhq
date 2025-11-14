/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.model;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Jumpship;
import megamek.common.units.SmallCraft;
import megamek.common.units.SpaceStation;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;
import mekhq.gui.utilities.MekHqTableCellRenderer;

/**
 * A table Model for displaying information about units
 *
 * @author Jay lawson
 */
public class UnitTableModel extends DataTableModel<Unit> {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.UnitTableModel";

    //region Variable Declarations
    public static final int COL_NAME = 0;
    public static final int COL_TYPE = 1;
    public static final int COL_WEIGHT_CLASS = 2;
    public static final int COL_TECH = 3;
    public static final int COL_WEIGHT = 4;
    public static final int COL_COST = 5;
    public static final int COL_STATUS = 6;
    public static final int COL_CONDITION = 7;
    public static final int COL_CREW_STATE = 8;
    public static final int COL_QUALITY = 9;
    public static final int COL_PILOT = 10;
    public static final int COL_FORCE = 11;
    public static final int COL_CREW = 12;
    public static final int COL_TECH_CRW = 13;
    public static final int COL_MAINTAIN = 14;
    public static final int COL_MAINTAIN_CYCLE = 15;
    public static final int COL_BV = 16;
    public static final int COL_REPAIR = 17;
    public static final int COL_PARTS = 18;
    public static final int COL_SITE = 19;
    public static final int COL_QUIRKS = 20;
    public static final int COL_MODE = 21;
    public static final int COL_SHIP_TRANSPORT = 22;
    public static final int COL_TAC_TRANSPORT = 23;
    public static final int N_COL = 24;

    private final Campaign campaign;
    //endregion Variable Declarations

    public UnitTableModel(Campaign c) {
        data = new ArrayList<>();
        campaign = c;
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
            case COL_WEIGHT_CLASS -> "Class";
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
            case COL_MAINTAIN -> "Maint. Cost";
            case COL_MAINTAIN_CYCLE -> "Next Maint.";
            case COL_BV -> "BV";
            case COL_REPAIR -> "# Repairs";
            case COL_PARTS -> "# Parts";
            case COL_SITE -> "Site";
            case COL_QUIRKS -> "Quirks";
            case COL_MODE -> "Mode";
            case COL_SHIP_TRANSPORT -> "Ship Transport";
            case COL_TAC_TRANSPORT -> "Tactical Transport";
            default -> "?";
        };
    }

    public int getColumnWidth(final int columnId) {
        return switch (columnId) {
            case COL_NAME, COL_TECH, COL_PILOT, COL_FORCE, COL_TECH_CRW -> 150;
            case COL_TYPE, COL_WEIGHT_CLASS, COL_SITE -> 50;
            case COL_COST, COL_STATUS, COL_MODE, COL_CREW -> 40;
            case COL_PARTS -> 10;
            default -> 20;
        };
    }

    public int getAlignment(int col) {
        return switch (col) {
            case COL_WEIGHT, COL_COST, COL_MAINTAIN, COL_MAINTAIN_CYCLE, COL_BV, COL_REPAIR, COL_PARTS ->
                  SwingConstants.RIGHT;
            case COL_QUALITY, COL_CREW, COL_QUIRKS, COL_MODE -> SwingConstants.CENTER;
            default -> SwingConstants.LEFT;
        };
    }

    /**
     * Returns the tooltip for the specified row and column.
     *
     * @param rowIndex    the index of the row
     * @param columnIndex the index of the column
     *
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
     *
     * @return the crew tooltip as an HTML string
     */
    public static String getCrewTooltip(Unit unit) {
        int gunnersNeeded = unit.getTotalGunnerNeeds();
        int gunnersAssigned = unit.getGunners().size();

        int driversNeeded = unit.getTotalDriverNeeds();
        int driversAssigned = unit.getDrivers().size();

        Entity entity = unit.getEntity();
        int soldiersNeeded = entity instanceof Infantry ? gunnersNeeded : 0;
        int soldiersAssigned = entity instanceof Infantry ? gunnersAssigned : 0;

        int navigatorsNeeded = entity instanceof Jumpship && !(entity instanceof SpaceStation) ? 1 : 0;
        int navigatorsAssigned = unit.getNavigator() == null ? 0 : 1;

        int genericCrewNeeded = unit.getTotalGenericCrewNeeds();
        int genericCrewAssigned = unit.getGenericCrew().size();

        int communicationsCrewNeeded = unit.getTotalCommunicationCrewNeeds();
        int communicationsCrewAssigned = unit.getCommunicationsCrew().size();

        List<String> reports = new ArrayList<>();

        Campaign campaign = unit.getCampaign();
        boolean isClanCampaign = campaign != null && campaign.isClanCampaign();
        if (driversNeeded > 0 && soldiersNeeded == 0) {
            PersonnelRole driverRole = unit.getDriverRole();
            String driverDisplay = driverRole == null ? getTextAt(RESOURCE_BUNDLE,
                  "UnitTableModel.crewNeeds.unknown") : driverRole.getLabel(isClanCampaign);
            appendReport(reports,
                  getFormattedTextAt(RESOURCE_BUNDLE, "UnitTableModel.crewNeeds.drivers", driverDisplay),
                  driversAssigned,
                  driversNeeded);
        }

        if (gunnersNeeded > 0 && soldiersNeeded == 0) {
            PersonnelRole gunnerRole = unit.getGunnerRole();
            String gunnerDisplay = gunnerRole == null ? getTextAt(RESOURCE_BUNDLE,
                  "UnitTableModel.crewNeeds.unknown") : gunnerRole.getLabel(isClanCampaign);

            appendReport(reports,
                  getFormattedTextAt(RESOURCE_BUNDLE, "UnitTableModel.crewNeeds.gunners", gunnerDisplay),
                  gunnersAssigned,
                  gunnersNeeded);
        }

        if (soldiersNeeded > 0) {
            appendReport(reports, getTextAt(RESOURCE_BUNDLE, "UnitTableModel.crewNeeds.soldiers"), soldiersAssigned,
                  soldiersNeeded);
        }

        if (communicationsCrewNeeded > 0) {
            String key = "UnitTableModel.crewNeeds.communications";
            appendReport(reports,
                  getTextAt(RESOURCE_BUNDLE, key),
                  communicationsCrewAssigned,
                  communicationsCrewNeeded);
        }

        if (genericCrewNeeded > 0) {
            String key = entity.isLargeCraft() ? "UnitTableModel.crewNeeds.crew" : "UnitTableModel.crewNeeds.other";
            appendReport(reports, getTextAt(RESOURCE_BUNDLE, key), genericCrewAssigned, genericCrewNeeded);
        }

        if (navigatorsNeeded > 0) {
            appendReport(reports, getTextAt(RESOURCE_BUNDLE, "UnitTableModel.crewNeeds.navigator"), navigatorsAssigned,
                  navigatorsNeeded);
        }

        String finalReport = reports.isEmpty() ?
                                   getTextAt(RESOURCE_BUNDLE, "UnitTableModel.crewNeeds.none") :
                                   String.join("<br>", reports);
        return "<html>" + finalReport + "</html>";
    }

    /**
     * Appends a crew report line to the provided StringBuilder.
     *
     * @param report   the {@link List} to add to
     * @param title    the title of the crew role (e.g., "Driver", "Gunner")
     * @param assigned the number of crew members assigned to the role
     * @param needed   the number of crew members needed for the role
     */
    private static void appendReport(List<String> report, String title, int assigned, int needed) {
        report.add(String.format("<b>%s: </b>%d/%d", title, assigned, needed));
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public Unit getUnit(int i) {
        return (i < data.size()) ? data.get(i) : null;
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (data.isEmpty() || (row < 0) || (row >= data.size())) {
            return "";
        }

        Unit unit = getUnit(row);
        Entity entity = unit.getEntity();
        if (entity == null) {
            return "?";
        }

        return switch (col) {
            case COL_NAME -> unit.getName();
            case COL_TYPE -> unit.getTypeDisplayableNameWithOmni();
            case COL_WEIGHT_CLASS -> entity.getWeightClassName();
            case COL_TECH -> TechConstants.getLevelDisplayableName(entity.getTechLevel());
            case COL_WEIGHT -> entity.getWeight();
            case COL_COST -> unit.getSellValue().toAmountAndSymbolString();
            case COL_STATUS -> unit.getStatus();
            case COL_CONDITION -> unit.getCondition();
            case COL_CREW_STATE -> unit.getCrewState();
            case COL_QUALITY -> unit.getQualityName();
            case COL_PILOT -> (unit.getCommander() != null) ? unit.getCommander().getHTMLTitle() : "-";
            case COL_FORCE -> {
                Force force = unit.getCampaign().getForce(unit.getForceId());
                yield (force != null) ? force.getFullName() : "-";
            }
            case COL_CREW -> unit.getActiveCrew().size() + "/" + unit.getFullCrewSize();
            case COL_TECH_CRW -> (unit.getTech() != null) ? unit.getTech().getHTMLTitle() : "-";
            case COL_MAINTAIN -> unit.getMaintenanceCost().toAmountAndSymbolString();
            case COL_MAINTAIN_CYCLE -> {
                if (!campaign.getCampaignOptions().isCheckMaintenance()) {
                    yield "-"; // Do not convert this into a character, it will break sorting
                }

                boolean needsMaintenance = unit.requiresMaintenance();
                if (!needsMaintenance) {
                    yield "-"; // Do not convert this into a character, it will break sorting
                }

                double daysSinceLastMaintenance = unit.getDaysSinceMaintenance();
                int cycleLength = campaign.getCampaignOptions().getMaintenanceCycleDays();
                yield (unit.getMaintenanceCycleDuration(cycleLength) - daysSinceLastMaintenance) + " days";
            }
            case COL_BV -> entity.calculateBattleValue(true, unit.getEntity().getCrew() == null);
            case COL_REPAIR -> unit.getPartsNeedingFixing().size();
            case COL_PARTS -> unit.getPartsNeeded().size();
            case COL_SITE -> Unit.getSiteName(unit.getSite());
            case COL_QUIRKS -> entity.countQuirks();
            case COL_MODE -> unit.isSalvage() ? "Strip" : "Repair";
            case COL_SHIP_TRANSPORT -> (unit.getTransportShipAssignment() != null) ?
                                             unit.getTransportShipAssignment().getTransportShip().getName() : "-";
            case COL_TAC_TRANSPORT -> (unit.getTacticalTransportAssignment() != null) ?
                                            unit.getTacticalTransportAssignment().getTransport().getName() : "-";
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
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
              int row, int column) {
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
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
              int row, int column) {
            Component c = this;
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);

            setText(getValueAt(actualRow, actualCol).toString());

            Unit u = getUnit(actualRow);
            switch (actualCol) {
                case COL_WEIGHT_CLASS: {
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
                        StringBuilder desc = new StringBuilder("<html><b>").append(force.getName()).append("</b>");
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
