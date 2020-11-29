package mekhq.gui.model;

import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import megamek.common.EntityWeightClass;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.UnitType;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.RetirementDefectionTracker;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;
import mekhq.gui.MekHqColors;
import mekhq.gui.dialog.RetirementDefectionDialog;
import mekhq.gui.utilities.MekHqTableCellRenderer;

public class RetirementTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 7461821036790309952L;

    public final static int COL_PERSON = 0;
    public final static int COL_ASSIGN = 1;
    public final static int COL_FORCE = 2;
    public final static int COL_TARGET = 3;
    public final static int COL_SHARES = 4;
    public final static int COL_BONUS_COST = 5;
    public final static int COL_PAY_BONUS = 6;
    public final static int COL_MISC_MOD = 7;
    public final static int COL_PAYOUT = 8;
    public final static int COL_RECRUIT = 9;
    public final static int COL_UNIT = 10;
    public final static int N_COL = 11;

    private final static String[] colNames = {
        "Person", "Assignment", "Force", "Target",
        "Shares", "Bonus Cost", "Pay Bonus", "Misc Modifier",
        "Payout", "Recruit", "Unit"
    };

    private final Campaign campaign;
    private final MekHqColors colors = new MekHqColors();
    private ArrayList<UUID> data;
    private HashMap<UUID, TargetRoll> targets;
    private HashMap<UUID, Boolean> payBonus;
    private HashMap<UUID, Integer> miscMods;
    private int generalMod;
    private HashMap<UUID, UUID> unitAssignments;
    private HashMap<UUID, Money> altPayout;
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

    public void setData(ArrayList<UUID> list, HashMap<UUID, UUID> unitAssignments) {
        this.unitAssignments = Utilities.nonNull(unitAssignments, new HashMap<>());
        data = list;
        fireTableDataChanged();
    }

    public void setData(HashMap<UUID, TargetRoll> targets) {
        this.targets = targets;
        data.clear();
        for (UUID id : targets.keySet()) {
            data.add(id);
            payBonus.put(id, false);
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
        switch (c) {
            case COL_PERSON:
            case COL_ASSIGN:
            case COL_FORCE:
            case COL_UNIT:
            case COL_RECRUIT:
                return 125;
            case COL_BONUS_COST:
            case COL_PAYOUT:
                return 70;
            case COL_TARGET:
            case COL_SHARES:
            case COL_MISC_MOD:
                return 50;
            case COL_PAY_BONUS:
            default:
                return 20;
        }
    }

    public int getAlignment(int col) {
        switch (col) {
            case COL_PERSON:
            case COL_ASSIGN:
            case COL_FORCE:
            case COL_UNIT:
            case COL_RECRUIT:
                return SwingConstants.LEFT;
            case COL_BONUS_COST:
            case COL_PAYOUT:
                return SwingConstants.RIGHT;
            case COL_TARGET:
            case COL_PAY_BONUS:
            case COL_SHARES:
            case COL_MISC_MOD:
            default:
                return SwingConstants.CENTER;
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        switch (col) {
            case COL_PAYOUT:
                return editPayout;
            case COL_PAY_BONUS:
            case COL_MISC_MOD:
                return true;
            case COL_RECRUIT:
                return campaign.getRetirementDefectionTracker().getPayout(data.get(row)).hasRecruit();
            default:
                return false;
        }
    }

    @Override
    public Class<?> getColumnClass(int col) {
        Class<?> retVal = Object.class;
        try {
            retVal = getValueAt(0, col).getClass();
        } catch (NullPointerException e) {
            MekHQ.getLogger().error(e);
        }
        return retVal;
    }

    @Override
    public Object getValueAt(int row, int col) {
        Person p;
        if (data.isEmpty()) {
            return "";
        } else {
            p = campaign.getPerson(data.get(row));
        }
        switch (col) {
            case COL_PERSON:
                return p.makeHTMLRank();
            case COL_ASSIGN:
                Unit u = p.getUnit();
                if (null != u) {
                    String name = u.getName();
                    if (u.getEntity() instanceof Tank) {
                        if (u.isDriver(p)) {
                            name = name + " [Driver]";
                        } else {
                            name = name + " [Gunner]";
                        }
                    }
                    if ((u.getEntity() instanceof SmallCraft) || (u.getEntity() instanceof Jumpship)) {
                        if (u.isNavigator(p)) {
                            name = name + " [Navigator]";
                        } else if (u.isDriver(p)) {
                            name =  name + " [Pilot]";
                        } else if (u.isGunner(p)) {
                            name = name + " [Gunner]";
                        } else {
                            name = name + " [Crew]";
                        }
                    }
                    return name;
                }
                //check for tech
                if (!p.getTechUnits().isEmpty()) {
                    if (p.getTechUnits().size() == 1) {
                        u = p.getTechUnits().get(0);
                        if (null != u) {
                            return u.getName() + " (" + p.getMaintenanceTimeUsing() + "m)";
                        }
                    } else {
                        return "" + p.getTechUnits().size() + " units (" + p.getMaintenanceTimeUsing() + "m)";
                    }
                }
                return "-";
            case COL_FORCE:
                Force force = campaign.getForceFor(p);
                if (null != force) {
                    return force.getName();
                } else {
                    return "None";
                }
            case COL_TARGET:
                if (null == targets) {
                    return 0;
                }
                return targets.get(p.getId()).getValue() -
                        (payBonus.get(p.getId()) ? 1 : 0) +
                        miscMods.get(p.getId()) + generalMod;
            case COL_BONUS_COST:
                return RetirementDefectionTracker.getBonusCost(p).toAmountAndSymbolString();
            case COL_PAY_BONUS:
                if (null == payBonus.get(p.getId())) {
                    return false;
                }
                return payBonus.get(p.getId());
            case COL_MISC_MOD:
                if (null == miscMods.get(p.getId())) {
                    return false;
                }
                return miscMods.get(p.getId());
            case COL_SHARES:
                return p.getNumShares(campaign.getCampaignOptions().getSharesForAll());
            case COL_PAYOUT:
                if (null == campaign.getRetirementDefectionTracker().getPayout(p.getId())) {
                    return "";
                }
                if (altPayout.containsKey(p.getId())) {
                    return altPayout.get(p.getId()).toAmountAndSymbolString();
                }
                Money payout = campaign.getRetirementDefectionTracker().getPayout(p.getId()).getPayoutAmount();
                /* If no unit is required as part of the payout, the unit is part or all of the
                 * final payout. If using the share system and tracking the original unit,
                 * the payout is also reduced by the value of the unit.
                 */
                if ((campaign.getRetirementDefectionTracker().getPayout(p.getId()).getWeightClass() == 0 &&
                        null != unitAssignments.get(p.getId())) ||
                        (campaign.getCampaignOptions().getUseShareSystem() &&
                                campaign.getCampaignOptions().getTrackOriginalUnit() &&
                                p.getOriginalUnitId() == unitAssignments.get(p.getId()) &&
                                        null != campaign.getUnit(unitAssignments.get(p.getId())))) {
                    payout = payout.minus(campaign.getUnit(unitAssignments.get(p.getId())).getBuyCost());
                }
                if (null != unitAssignments.get(p.getId())) {
                    payout = payout.plus(RetirementDefectionDialog.getShortfallAdjustment(campaign.getRetirementDefectionTracker().getPayout(p.getId()).getWeightClass(),
                            RetirementDefectionDialog.weightClassIndex(campaign.getUnit(unitAssignments.get(p.getId())))));
                }
                /* No payout if the pilot stole a unit */
                if (campaign.getRetirementDefectionTracker().getPayout(p.getId()).hasStolenUnit() &&
                        null != unitAssignments.get(p.getId())) {
                    payout = Money.zero();
                }
                // If payout is negative then just make it zero
                if (payout.isNegative()) {
                    payout = Money.zero();
                }
                return payout.toAmountAndSymbolString();
            case COL_UNIT:
                if (null == campaign.getRetirementDefectionTracker().getPayout(p.getId())) {
                    return "";
                }
                if (null != unitAssignments.get(p.getId())) {
                    return campaign.getUnit(unitAssignments.get(p.getId())).getName();
                } else if (campaign.getRetirementDefectionTracker().getPayout(p.getId()).getWeightClass() < EntityWeightClass.WEIGHT_LIGHT) {
                    return "";
                } else {
                    return "Class " + campaign.getRetirementDefectionTracker().getPayout(p.getId()).getWeightClass();
                }
            case COL_RECRUIT:
                RetirementDefectionTracker.Payout pay =
                    campaign.getRetirementDefectionTracker().getPayout(data.get(row));
                if (null == pay) {
                    return "";
                }
                if (pay.getDependents() > 0) {
                    return pay.getDependents() + " Dependents";
                } else if (pay.hasRecruit()) {
                    return Person.getRoleDesc(pay.getRecruitType(),
                            campaign.getFaction().isClan());
                } else if (pay.hasHeir()) {
                    return "Heir";
                } else {
                    return "";
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
            payBonus.put(data.get(row), (Boolean)value);
        } else if (col == COL_MISC_MOD) {
            miscMods.put(data.get(row), (Integer)value);
        } else if (col == COL_UNIT) {
            if (null != value) {
                unitAssignments.put(getPerson(row).getId(), (UUID)value);
            }
        } else if (col == COL_RECRUIT) {
            for (int i = 0; i < Person.T_NUM; i++) {
                if (Person.getRoleDesc(i, campaign.getFaction().isClan()).equals(value)) {
                    campaign.getRetirementDefectionTracker().getPayout(data.get(row)).setRecruitType(i);
                    break;
                }
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

    public HashMap<UUID, Money> getAltPayout() {
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
        /**
         *
         */
         private static final long serialVersionUID = 770305943352316265L;

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);
            int actualRow = table.convertRowIndexToModel(row);
            int actualCol = table.convertColumnIndexToModel(column);
            Person p = getPerson(actualRow);
            setHorizontalAlignment(getAlignment(actualCol));
            if (!isSelected) {
                if (null != campaign.getRetirementDefectionTracker().getPayout(p.getId()) &&
                    campaign.getRetirementDefectionTracker().getPayout(p.getId()).getWeightClass() > 0) {
                    colors.getPaidRetirement().getColor().ifPresent(this::setBackground);
                    colors.getPaidRetirement().getAlternateColor().ifPresent(this::setForeground);
                }
            }
            return this;
        }
    }

    public class VisualRenderer extends BasicInfo implements TableCellRenderer {
        private static final long serialVersionUID = 7261885081786958754L;

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
                setPortrait(p);
                setText(p.getFullDesc());
            }
            if (actualCol == COL_ASSIGN) {
                Unit u = p.getUnit();
                if (!p.getTechUnits().isEmpty()) {
                    u = p.getTechUnits().get(0);
                }
                if (null != u) {
                    String desc = "<b>" + u.getName() + "</b><br>";
                    desc += u.getEntity().getWeightClassName();
                    if (!((u.getEntity() instanceof SmallCraft) || (u.getEntity() instanceof Jumpship))) {
                        desc += " " + UnitType.getTypeDisplayableName(u.getEntity().getUnitType());
                    }
                    desc += "<br>" + u.getStatus() + "";
                    setText(desc);
                    Image mekImage = getImageFor(u);
                    if (null != mekImage) {
                        setImage(mekImage);
                    } else {
                        clearImage();
                    }
                } else {
                    clearImage();
                }
            }
            if (actualCol == COL_FORCE) {
                Force force = campaign.getForceFor(p);
                if (null != force) {
                    String desc = "<html><b>" + force.getName() + "</b>";
                    Force parent = force.getParentForce();
                    //cut off after three lines and don't include the top level
                    int lines = 1;
                    while ((parent != null) && (null != parent.getParentForce()) && (lines < 4)) {
                        desc += "<br>" + parent.getName();
                        lines++;
                        parent = parent.getParentForce();
                    }
                    desc += "</html>";
                    setHtmlText(desc);
                    Image forceImage = getImageFor(force);
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
            if (!isSelected) {
                if (null != campaign.getRetirementDefectionTracker().getPayout(p.getId()) &&
                        campaign.getRetirementDefectionTracker().getPayout(p.getId()).getWeightClass() > 0) {
                    colors.getPaidRetirement().getColor().ifPresent(this::setBackground);
                    colors.getPaidRetirement().getAlternateColor().ifPresent(this::setForeground);
                }
            }

            return this;
        }
    }
}
