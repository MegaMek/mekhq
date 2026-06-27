/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.getEffectiveFatigue;

import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import megamek.common.annotations.Nullable;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;
import mekhq.gui.enums.PersonnelTabView;
import mekhq.gui.enums.PersonnelTableModelColumn;
import mekhq.gui.utilities.ComponentColors;
import mekhq.gui.utilities.MekHqTableCellRenderer;

/**
 * A table Model for displaying information about personnel
 *
 * @author Jay lawson
 */
public class PersonnelTableModel extends DataTableModel<Person> {

    public static final PersonnelTableModelColumn[] PERSONNEL_COLUMNS = PersonnelTableModelColumn.values();

    private final Campaign campaign;
    private PersonnelMarket personnelMarket;
    private boolean groupByUnit;

    public PersonnelTableModel(Campaign c) {
        data = new ArrayList<>();
        campaign = c;
    }

    /**
     * Gets a value indicating whether the table model should group personnel by their unit.
     *
     * @return A value indicating whether the table model groups users by their unit.
     */
    public boolean isGroupByUnit() {
        return groupByUnit;
    }

    /**
     * Determines whether to group personnel by their unit (if they have one). If enabled, a commanding officer's crew
     * (or soldiers) will not be displayed in the table. Instead, an indicator will appear by the commander's name.
     *
     * @param groupByUnit true if personnel should be grouped under their commanding officer and not be displayed.
     */
    public void setGroupByUnit(boolean groupByUnit) {
        this.groupByUnit = groupByUnit;
    }

    @Override
    public int getColumnCount() {
        return PERSONNEL_COLUMNS.length;
    }

    @Override
    public String getColumnName(final int column) {
        return PERSONNEL_COLUMNS[column].toString();
    }

    @Override
    public Class<?> getColumnClass(int column) {
        if (PERSONNEL_COLUMNS[column] == PersonnelTableModelColumn.RANK) {
            return Person.class;
        }
        return String.class;
    }

    public @Nullable Person getPerson(final int row) {
        return (row < getRowCount()) ? (Person) getData().get(row) : null;
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        return getValueAt(getPerson(row), PERSONNEL_COLUMNS[column]);
    }

    public Object getValueAt(@Nullable Person person, PersonnelTableModelColumn column) {
        if (getData().isEmpty()) {
            return "";
        } else if (person == null) {
            return "?";
        } else {
            return column.getCellValue(campaign, person);
        }
    }

    public void refreshData() {
        if (!isGroupByUnit()) {
            setData(new ArrayList<>(campaign.getAllPersonnel()));
        } else {
            List<Person> commanders = new ArrayList<>();
            for (Person person : campaign.getAllPersonnel()) {
                if ((person.getUnit() != null) && !person.equals(person.getUnit().getCommander())) {
                    // this person is NOT the commander of their unit,
                    // skip them.
                    continue;
                }

                // 1. If they don't have a unit, add them.
                // 2. If their unit doesn't have a commander, add them.
                // 3. If their unit doesn't exist (error?), add them.
                commanders.add(person);
            }

            setData(commanders);
        }
    }

    public TableCellRenderer getRenderer(final @Nullable PersonnelTabView view) {
        return ((view != null) && view.isGraphic()) ? new VisualRenderer() : new Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            final int modelRow = table.convertRowIndexToModel(row);
            final PersonnelTableModelColumn personnelColumn = PERSONNEL_COLUMNS[table.convertColumnIndexToModel(column)];
            final Person person = getPerson(modelRow);

            setOpaque(true);
            setHorizontalAlignment(personnelColumn.getAlignment());

            setText(personnelColumn.getText(value));

            // Collect all applicable color reasons for tooltip
            List<String> personalStateFlags = new ArrayList<>();
            if (!isSelected) {
                ComponentColors cellColors = populatePersonalStateFlags(person, personalStateFlags);
                setForeground(cellColors.foreground());
                setBackground(cellColors.background());
            }
            // Tool Tips - includes all applicable color reasons for name/rank/status columns
            setToolTipText(personnelColumn.getToolTipText(person, personalStateFlags));

            return this;
        }

        /**
         * Populates a list with personal state flags. Selects a color for the most important state.
         */
        private ComponentColors populatePersonalStateFlags(Person person, List<String> colorReasonKeys) {
            // Set color based on priority (first match wins for display color)
            // But collect ALL applicable reasons for tooltip
            MHQOptions mhqOptions = MekHQ.getMHQOptions();
            CampaignOptions campaignOptions = campaign.getCampaignOptions();

            ComponentColors cellColors = null;
            if (person.getStatus().isAbsent()) {
                colorReasonKeys.add("colorReason.personnel.absent");
                cellColors = mhqOptions.getAbsentColors();
            }
            if (person.getStatus().isDepartedUnit()) {
                colorReasonKeys.add("colorReason.personnel.departed");
                cellColors = (cellColors == null) ? mhqOptions.getGoneColors() : cellColors;
            }
            if (person.isDeployed()) {
                colorReasonKeys.add("colorReason.personnel.deployed");
                cellColors = (cellColors == null) ? mhqOptions.getDeployedColors() : cellColors;
            }
            if (PersonnelStatus.computeIsAwayFromMainForce(campaign, person)) {
                cellColors = (cellColors == null) ? mhqOptions.getAwayFromMainForceColors() : cellColors;
            }
            if (campaignOptions.isUseAdvancedMedical() ? person.hasInjuries(true) : (person.getHits() > 0)) {
                colorReasonKeys.add("colorReason.personnel.injured");
                cellColors = (cellColors == null) ? mhqOptions.getInjuredColors() : cellColors;
            }
            if (person.isPregnant()) {
                colorReasonKeys.add("colorReason.personnel.pregnant");
                cellColors = (cellColors == null) ? mhqOptions.getPregnantColors() : cellColors;
            }
            if (campaignOptions.isUseFatigue() && (getEffectiveFatigue(person, campaign) >= 5)) {
                colorReasonKeys.add("colorReason.personnel.fatigued");
                cellColors = (cellColors == null) ? mhqOptions.getFatiguedColors() : cellColors;
            }
            if (person.hasNonProstheticPermanentInjuries(campaignOptions.isUseAlternativeAdvancedMedical())) {
                colorReasonKeys.add("colorReason.personnel.healedInjuries");
                cellColors = (cellColors == null) ? mhqOptions.getHealedInjuriesColors() : cellColors;
            }
            return (cellColors == null) ? new ComponentColors(UIManager.getColor("Table.foreground"),
                  UIManager.getColor("Table.background")) : cellColors;
        }
    }

    public class VisualRenderer extends BasicInfo implements TableCellRenderer {
        public VisualRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            final int modelRow = table.convertRowIndexToModel(row);
            final PersonnelTableModelColumn personnelColumn = PERSONNEL_COLUMNS[table.convertColumnIndexToModel(column)];
            final Person person = getPerson(modelRow);

            setText(personnelColumn.getText(value));
            Image image = getImage(person, personnelColumn);
            if (image != null) {
                setImage(image);
            } else {
                clearImage();
            }

            MekHqTableCellRenderer.setupTableColors(this, table, isSelected, hasFocus, row);
            return this;
        }

        private Image getImage(Person person, PersonnelTableModelColumn personnelColumn) {
            return switch (personnelColumn) {
                case PERSON_GRAPHICAL -> person.getPortraitImageIconWithFallback(true, 54).getImage();
                case UNIT_ASSIGNMENT_GRAPHICAL -> {
                    Unit unit = person.getUnit();
                    if ((unit == null) && !person.getTechUnits().isEmpty()) {
                        unit = person.getTechUnits().getFirst();
                    }
                    yield (unit == null) ? null : unit.getImage(this);
                }
                case FORCE_GRAPHICAL -> {
                    Formation formation = campaign.getFormationFor(person);
                    yield  (formation == null) ? null : formation.getFormationIcon().getImage(54);
                }
                default -> null;
            };
        }
    }

    public void loadAssignmentFromMarket(PersonnelMarket personnelMarket) {
        this.personnelMarket = personnelMarket;
    }

}
