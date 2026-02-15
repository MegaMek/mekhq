/*
 * Copyright (C) 2015-2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.Payout.isBreakingContract;

import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Jumpship;
import megamek.common.units.SmallCraft;
import megamek.common.units.Tank;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;
import mekhq.gui.dialog.RetirementDefectionDialog;
import mekhq.gui.utilities.MekHqTableCellRenderer;

public class RetirementTableModel extends AbstractTableModel {
    private static final MMLogger LOGGER = MMLogger.create(RetirementTableModel.class);

    public static final int COL_PERSON = 0;
    public static final int COL_ASSIGN = 1;
    public static final int COL_FORCE = 2;
    public static final int COL_TARGET = 3;
    public static final int COL_SHARES = 4;
    public static final int COL_BONUS_COST = 5;
    public static final int COL_PAY_BONUS = 6;
    public static final int COL_MISC_MOD = 7;
    public static final int COL_PAYOUT = 8;
    public static final int COL_UNIT = 9;
    public static final int N_COL = 10;

    private static final String[] colNames = {
          "Person", "Assignment", "Force", "Target Number",
          "Shares", "Retention Bonus", "Pay Bonus", "Custom Modifier",
          "Payout", "Unit"
    };

    private final Campaign campaign;
    private List<UUID> data;
    private Map<UUID, TargetRoll> targets;
    private final Map<UUID, Boolean> payBonus;
    private final Map<UUID, Integer> miscMods;
    private int generalMod;
    private Map<UUID, UUID> unitAssignments;
    private final Map<UUID, Money> altPayout;
    boolean editPayout;

    public RetirementTableModel(Campaign c) {
        this.campaign = c;
        data = new ArrayList<>();
        payBonus = new HashMap<>();
        miscMods = new HashMap<>();
        generalMod = 0;
        unitAssignments = new HashMap<>();
        altPayout = new HashMap<>();
        editPayout = false;
    }

    public void setData(List<UUID> list, Map<UUID, UUID> unitAssignments) {
        this.unitAssignments = ObjectUtility.nonNull(unitAssignments, new HashMap<>());
        data = list;
        fireTableDataChanged();
    }

    public void setData(Map<UUID, TargetRoll> targets) {
        this.targets = targets;
        data.clear();

        for (UUID id : targets.keySet()) {
            data.add(id);
            payBonus.put(id,
                  ((campaign.getCampaignOptions().isPayBonusDefault())
                         && (targets.get(id).getValue() >= campaign.getCampaignOptions()
                                                                 .getPayBonusDefaultThreshold())));
            miscMods.put(id, 0);
        }
        fireTableDataChanged();
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
        return colNames[column];
    }

    public int getColumnWidth(int c) {
        return switch (c) {
            case COL_PERSON, COL_ASSIGN, COL_FORCE, COL_UNIT -> 70;
            case COL_BONUS_COST, COL_PAYOUT, COL_TARGET, COL_SHARES, COL_MISC_MOD -> 50;
            default -> 20;
        };
    }

    public int getAlignment(int col) {
        if (col == COL_PERSON) {
            return SwingConstants.LEFT;
        } else {
            return SwingConstants.CENTER;
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return switch (col) {
            case COL_PAYOUT -> editPayout;
            case COL_PAY_BONUS, COL_MISC_MOD -> true;
            default -> false;
        };
    }

    @Override
    public Class<?> getColumnClass(int col) {
        Class<?> retVal = Object.class;
        try {
            retVal = getValueAt(0, col).getClass();
        } catch (NullPointerException e) {
            LOGGER.error("", e);
        }
        return retVal;
    }

    @Override
    public Object getValueAt(int row, int col) {
        Person person;
        if (data.isEmpty()) {
            return "";
        } else {
            person = campaign.getPerson(data.get(row));
        }
        switch (col) {
            case COL_PERSON:
                return person.makeHTMLRank();
            case COL_ASSIGN:
                Unit u = person.getUnit();
                if (null != u) {
                    String name = u.getName();
                    if (u.getEntity() instanceof Tank) {
                        if (u.isDriver(person)) {
                            name = name + " [Driver]";
                        } else {
                            name = name + " [Gunner]";
                        }
                    }
                    if ((u.getEntity() instanceof SmallCraft) || (u.getEntity() instanceof Jumpship)) {
                        if (u.isNavigator(person)) {
                            name = name + " [Navigator]";
                        } else if (u.isDriver(person)) {
                            name = name + " [Pilot]";
                        } else if (u.isGunner(person)) {
                            name = name + " [Gunner]";
                        } else {
                            name = name + " [Crew]";
                        }
                    }
                    return name;
                }
                // check for tech
                if (!person.getTechUnits().isEmpty()) {
                    if (person.getTechUnits().size() == 1) {
                        u = person.getTechUnits().get(0);
                        if (null != u) {
                            return u.getName() + " (" + person.getMaintenanceTimeUsing() + "m)";
                        }
                    } else {
                        return person.getTechUnits().size() + " units (" + person.getMaintenanceTimeUsing() + "m)";
                    }
                }
                return "-";
            case COL_FORCE:
                Formation formation = campaign.getFormationFor(person);
                if (null != formation) {
                    return formation.getName();
                } else {
                    return "None";
                }
            case COL_TARGET:
                if (null == targets) {
                    return 0;
                }
                return targets.get(person.getId()).getValue() - (payBonus.get(person.getId()) ? 2 : 0) +
                             miscMods.get(person.getId()) +
                             generalMod;
            case COL_BONUS_COST:
                Money bonusCost = RetirementDefectionTracker.getPayoutOrBonusValue(campaign, person);

                if (campaign.getCampaignOptions().getTurnoverFrequency().isQuarterly()) {
                    return bonusCost.dividedBy(3).toAmountAndSymbolString();
                } else if (campaign.getCampaignOptions().getTurnoverFrequency().isMonthly()) {
                    return bonusCost.dividedBy(12).toAmountAndSymbolString();
                } else if (campaign.getCampaignOptions().getTurnoverFrequency().isWeekly()) {
                    return bonusCost.dividedBy(52).toAmountAndSymbolString();
                } else {
                    return bonusCost;
                }
            case COL_PAY_BONUS:
                return payBonus.getOrDefault(person.getId(), false);
            case COL_MISC_MOD:
                return miscMods.getOrDefault(person.getId(), 0);
            case COL_SHARES:
                return person.getNumShares(campaign, campaign.getCampaignOptions().isSharesForAll());
            case COL_PAYOUT:
                if (null == campaign.getRetirementDefectionTracker().getPayout(person.getId())) {
                    return "";
                }
                if (altPayout.containsKey(person.getId())) {
                    return altPayout.get(person.getId()).toAmountAndSymbolString();
                }
                Money payout = campaign.getRetirementDefectionTracker().getPayout(person.getId()).getPayoutAmount();
                /*
                 * If no unit is required as part of the payout, the unit is part or all of the
                 * final payout. If using the share system and tracking the original unit,
                 * the payout is also reduced by the value of the unit.
                 */
                if ((campaign.getRetirementDefectionTracker().getPayout(person.getId()).getWeightClass() == 0 &&
                           null != unitAssignments.get(person.getId())) ||
                          (campaign.getCampaignOptions().isUseShareSystem() &&
                                 campaign.getCampaignOptions().isTrackOriginalUnit() &&
                                 Objects.equals(person.getOriginalUnitId(), unitAssignments.get(person.getId())) &&
                                 null != campaign.getUnit(unitAssignments.get(person.getId())))) {
                    payout = payout.minus(campaign.getUnit(unitAssignments.get(person.getId())).getBuyCost());
                }

                if (isBreakingContract(person,
                      campaign.getLocalDate(),
                      campaign.getCampaignOptions().getServiceContractDuration())) {
                    // if the person requires a unit, check to ensure there isn't a shortfall
                    if (null != unitAssignments.get(person.getId())) {
                        payout = payout.plus(RetirementDefectionDialog.getShortfallAdjustment(campaign.getRetirementDefectionTracker()
                                                                                                    .getPayout(person.getId())
                                                                                                    .getWeightClass(),
                              RetirementDefectionDialog.weightClassIndex(campaign.getUnit(unitAssignments.get(person.getId())))));
                    }

                    // if the person requires a unit, but doesn't have one...
                    if ((unitAssignments.get(person.getId()) == null) &&
                              (campaign.getRetirementDefectionTracker().getPayout(person.getId()).getWeightClass() >
                                     0)) {
                        payout = payout.plus(RetirementDefectionDialog.getShortfallAdjustment(campaign.getRetirementDefectionTracker()
                                                                                                    .getPayout(person.getId())
                                                                                                    .getWeightClass(),
                              0));
                    }
                }

                // If payout is negative then make it zero
                if (payout.isNegative()) {
                    payout = Money.zero();
                }
                return payout.toAmountAndSymbolString();
            case COL_UNIT:
                if (null == campaign.getRetirementDefectionTracker().getPayout(person.getId())) {
                    return "";
                }
                if (null != unitAssignments.get(person.getId())) {
                    return campaign.getUnit(unitAssignments.get(person.getId())).getName();
                } else if (campaign.getRetirementDefectionTracker().getPayout(person.getId())
                                 .getWeightClass() < EntityWeightClass.WEIGHT_LIGHT) {
                    return "";
                } else {
                    return "Class " +
                                 campaign.getRetirementDefectionTracker().getPayout(person.getId()).getWeightClass();
                }
            default:
                return "?";
        }
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (col == COL_PAYOUT) {
            try {
                if (value == null) {
                    return;
                }

                Money payout = Money.of(Double.parseDouble(value.toString()));
                altPayout.put(data.get(row), payout);
            } catch (Exception e1) {
                return;
            }
        } else if (col == COL_PAY_BONUS) {
            payBonus.put(data.get(row), (Boolean) value);
        } else if (col == COL_MISC_MOD) {
            miscMods.put(data.get(row), (Integer) value);
        } else if (col == COL_UNIT) {
            if (null != value) {
                unitAssignments.put(getPerson(row).getId(), (UUID) value);
            }
        }
        fireTableDataChanged();
    }

    public Person getPerson(int row) {
        return campaign.getPerson(data.get(row));
    }

    public boolean getPayBonus(UUID id) {
        return payBonus.get(id);
    }

    public int getMiscModifier(UUID id) {
        return miscMods.get(id);
    }

    public void setGeneralMod(int mod) {
        generalMod = mod;
        fireTableDataChanged();
    }

    public Map<UUID, Money> getAltPayout() {
        return altPayout;
    }

    public void setEditPayout(boolean edit) {
        editPayout = edit;
    }

    public TableCellRenderer getRenderer(int col) {
        if (col < COL_TARGET) {
            return new VisualRenderer();
        } else {
            return new TextRenderer();
        }
    }

    public class TextRenderer extends MekHqTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected,
                  hasFocus, row, column);
            int actualCol = table.convertColumnIndexToModel(column);
            setHorizontalAlignment(getAlignment(actualCol));

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
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            Person p = getPerson(actualRow);
            setText(getValueAt(actualRow, actualCol).toString());
            if (actualCol == COL_PERSON) {
                setText(p.getFullDesc(campaign));
                setImage(p.getPortrait().getImage(40));
            } else if (actualCol == COL_ASSIGN) {
                Unit u = p.getUnit();
                if (!p.getTechUnits().isEmpty()) {
                    u = p.getTechUnits().get(0);
                }

                if (null != u) {
                    String desc = "<b>" + u.getName() + "</b><br>";
                    desc += u.getEntity().getWeightClassName();
                    if (!((u.getEntity() instanceof SmallCraft) || (u.getEntity() instanceof Jumpship))) {
                        desc += ' ' + UnitType.getTypeDisplayableName(u.getEntity().getUnitType());
                    }
                    desc += "<br>" + u.getStatus();
                    setText(desc);
                    Image mekImage = u.getImage(this);
                    if (null != mekImage) {
                        setImage(mekImage);
                    } else {
                        clearImage();
                    }
                } else {
                    clearImage();
                }
            } else if (actualCol == COL_FORCE) {
                Formation formation = campaign.getFormationFor(p);
                if (null != formation) {
                    StringBuilder desc = new StringBuilder("<html><b>" + formation.getName() + "</b>");
                    Formation parent = formation.getParentFormation();
                    // cut off after three lines and don't include the top level
                    int lines = 1;
                    while ((parent != null) && (null != parent.getParentFormation()) && (lines < 4)) {
                        desc.append("<br>").append(parent.getName());
                        lines++;
                        parent = parent.getParentFormation();
                    }
                    desc.append("</html>");
                    setHtmlText(desc.toString());
                    final Image forceImage = formation.getFormationIcon().getImage(40);
                    if (null != forceImage) {
                        setImage(forceImage);
                    } else {
                        clearImage();
                    }
                } else {
                    clearImage();
                }
            }

            MekHqTableCellRenderer.setupTableColors(this, table, isSelected, hasFocus, row);

            return this;
        }
    }
}
