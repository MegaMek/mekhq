/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;
import mekhq.gui.enums.PersonnelTabView;
import mekhq.gui.enums.PersonnelTableModelColumn;
import mekhq.gui.utilities.MekHqTableCellRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A table Model for displaying information about personnel
 * @author Jay lawson
 */
public class PersonnelTableModel extends DataTableModel {
    //region Variable Declarations
    public static final PersonnelTableModelColumn[] PERSONNEL_COLUMNS = PersonnelTableModelColumn.values();

    private Campaign campaign;
    private PersonnelMarket personnelMarket;
    private boolean loadAssignmentFromMarket;
    private boolean groupByUnit;
    //endregion Variable Declarations

    public PersonnelTableModel(Campaign c) {
        data = new ArrayList<Person>();
        campaign = c;
    }

    /**
     * Gets a value indicating whether the table model should
     * group personnel by their unit.
     * @return A value indicating whether the table model groups users by their unit.
     */
    public boolean isGroupByUnit() {
        return groupByUnit;
    }

    /**
     * Determines whether to group personnel by their unit (if they have one).
     * If enabled, a commanding officer's crew (or soldiers) will not be displayed
     * in the table. Instead, an indicator will appear by the commander's name.
     * @param groupByUnit true if personnel should be grouped under
     *                    their commanding officer and not be displayed.
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

    public @Nullable Person getPerson(final int row) {
        return (row < getRowCount()) ? (Person) getData().get(row) : null;
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        return getValueAt(getPerson(row), PERSONNEL_COLUMNS[column]);
    }

    public String getValueAt(final @Nullable Person person,
                             final PersonnelTableModelColumn column) {
        if (getData().isEmpty()) {
            return "";
        } else if (person == null) {
            return "?";
        } else {
            return column.getCellValue(getCampaign(), personnelMarket, person,
                    loadAssignmentFromMarket, isGroupByUnit());
        }
    }

    private Campaign getCampaign() {
        return campaign;
    }

    public void refreshData() {
        if (!isGroupByUnit()) {
            setData(new ArrayList<>(getCampaign().getPersonnel()));
        } else {
            Campaign c = getCampaign();
            List<Person> commanders = new ArrayList<>();
            for (Person p : c.getPersonnel()) {
                if ((p.getUnit() != null) && !p.equals(p.getUnit().getCommander())) {
                    // this person is NOT the commander of their unit,
                    // skip them.
                    continue;
                }

                // 1. If they don't have a unit, add them.
                // 2. If their unit doesn't have a commander, add them.
                // 3. If their unit doesn't exist (error?), add them.
                commanders.add(p);
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

            // Display Text
            final String displayText = personnelColumn.getDisplayText(getCampaign(), person);
            if (displayText != null) {
                setText(displayText);
            }

            // Tool Tips
            setToolTipText(personnelColumn.getToolTipText(person, loadAssignmentFromMarket));

            // Colouring
            boolean personIsDamaged = false;
            if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
                personIsDamaged = person.hasInjuries(true);
            } else {
                personIsDamaged = person.getHits() > 0;
            }
            boolean personIsFatigued = (campaign.getCampaignOptions().isUseFatigue()
                    && (person.getEffectiveFatigue(campaign) >= 5));

            if (!isSelected) {
                if (person.getStatus().isAbsent()) {
                    setBackground(MekHQ.getMHQOptions().getAbsentBackground());
                    setForeground(MekHQ.getMHQOptions().getAbsentForeground());
                } else if (person.getStatus().isDepartedUnit()) {
                    setBackground(MekHQ.getMHQOptions().getGoneBackground());
                    setForeground(MekHQ.getMHQOptions().getGoneForeground());
                } else if (person.isDeployed()) {
                    setForeground(MekHQ.getMHQOptions().getDeployedForeground());
                    setBackground(MekHQ.getMHQOptions().getDeployedBackground());
                } else if (personIsDamaged) {
                    setForeground(MekHQ.getMHQOptions().getInjuredForeground());
                    setBackground(MekHQ.getMHQOptions().getInjuredBackground());
                } else if (person.isPregnant()) {
                    setForeground(MekHQ.getMHQOptions().getPregnantForeground());
                    setBackground(MekHQ.getMHQOptions().getPregnantBackground());
                } else if (personIsFatigued) {
                    setForeground(MekHQ.getMHQOptions().getFatiguedForeground());
                    setBackground(MekHQ.getMHQOptions().getFatiguedBackground());
                } else if (person.hasOnlyHealedPermanentInjuries()) {
                    setForeground(MekHQ.getMHQOptions().getHealedInjuriesForeground());
                    setBackground(MekHQ.getMHQOptions().getHealedInjuriesBackground());
                } else {
                    setBackground(UIManager.getColor("Table.background"));
                    setForeground(UIManager.getColor("Table.foreground"));
                }
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
            final int modelRow = table.convertRowIndexToModel(row);
            final PersonnelTableModelColumn personnelColumn = PERSONNEL_COLUMNS[table.convertColumnIndexToModel(column)];
            final Person person = getPerson(modelRow);

            setText(getValueAt(person, personnelColumn));

            switch (personnelColumn) {
                case PERSON:
                    setText(person.getFullDesc(getCampaign()));
                    setImage(person.getPortrait().getImage(54));
                    break;
                case UNIT_ASSIGNMENT:
                    if (loadAssignmentFromMarket) {
                        final Entity en = personnelMarket.getAttachedEntity(person);
                        setText((en != null) ? en.getDisplayName() : "-");
                    } else {
                        Unit u = person.getUnit();
                        if ((u == null) && !person.getTechUnits().isEmpty()) {
                            u = person.getTechUnits().get(0);
                        }

                        if (u != null) {
                            String desc = "<b>" + u.getName() + "</b><br>";
                            desc += u.getEntity().getWeightClassName();
                            if ((!(u.getEntity() instanceof SmallCraft) || !(u.getEntity() instanceof Jumpship))) {
                                desc += " " + UnitType.getTypeDisplayableName(u.getEntity().getUnitType());
                            }
                            desc += "<br>" + u.getStatus() + "";
                            setText(desc);
                            Image mekImage = u.getImage(this);
                            if (mekImage != null) {
                                setImage(mekImage);
                            } else {
                                clearImage();
                            }
                        } else {
                            clearImage();
                        }
                    }
                    break;
                case FORCE:
                    Force force = getCampaign().getForceFor(person);
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
                case INJURIES:
                    Image hitImage = getHitsImage(person.getHits());
                    if (hitImage != null) {
                        setImage(hitImage);
                    } else {
                        clearImage();
                    }
                    setHtmlText("");
                    break;
                default:
                    break;
            }

            MekHqTableCellRenderer.setupTableColors(this, table, isSelected, hasFocus, row);
            return this;
        }

        private @Nullable Image getHitsImage(int hits) {
            switch (hits) {
                case 1:
                    return Toolkit.getDefaultToolkit().getImage("data/images/misc/hits/onehit.png");
                case 2:
                    return Toolkit.getDefaultToolkit().getImage("data/images/misc/hits/twohits.png");
                case 3:
                    return Toolkit.getDefaultToolkit().getImage("data/images/misc/hits/threehits.png");
                case 4:
                    return Toolkit.getDefaultToolkit().getImage("data/images/misc/hits/fourhits.png");
                case 5:
                    return Toolkit.getDefaultToolkit().getImage("data/images/misc/hits/fivehits.png");
                case 6:
                    return Toolkit.getDefaultToolkit().getImage("data/images/misc/hits/sixhits.png");
                default:
                    return null;
            }
        }
    }

    public void loadAssignmentFromMarket(PersonnelMarket personnelMarket) {
        this.personnelMarket = personnelMarket;
        this.loadAssignmentFromMarket = (null != personnelMarket);
    }
}
