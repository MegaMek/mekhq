/*
 * Copyright (c) 2020 The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.UUID;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.UnitType;
import megamek.common.options.PilotOptions;
import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;
import mekhq.gui.BasicInfo;

/**
 * A table Model for displaying information about personnel
 * @author Jay lawson
 */
public class PersonnelTableModel extends DataTableModel {

    private static final long serialVersionUID = -5207167419079014157L;

    private Campaign campaign;
    private PersonnelMarket personnelMarket;
    private boolean loadAssignmentFromMarket;
    private boolean groupByUnit;

    public static final int COL_RANK            = 0;
    public static final int COL_NAME            = 1;
    public static final int COL_CALL            = 2;
    public static final int COL_AGE             = 3;
    public static final int COL_GENDER          = 4;
    public static final int COL_SKILL           = 5;
    public static final int COL_TYPE            = 6;
    public static final int COL_ASSIGN          = 7;
    public static final int COL_FORCE           = 8;
    public static final int COL_DEPLOY          = 9;
    public static final int COL_MECH            = 10;
    public static final int COL_AERO            = 11;
    public static final int COL_JET             = 12;
    public static final int COL_VEE             = 13;
    public static final int COL_VTOL            = 14;
    public static final int COL_NVEE            = 15;
    public static final int COL_SPACE           = 16;
    public static final int COL_ARTY            = 17;
    public static final int COL_GUN_BA          = 18;
    public static final int COL_SMALL_ARMS      = 19;
    public static final int COL_ANTI_MECH       = 20;
    public static final int COL_TACTICS         = 21;
    public static final int COL_STRATEGY        = 22;
    public static final int COL_TECH_MECH       = 23;
    public static final int COL_TECH_AERO       = 24;
    public static final int COL_TECH_VEE        = 25;
    public static final int COL_TECH_BA         = 26;
    public static final int COL_MEDICAL         = 27;
    public static final int COL_ADMIN           = 28;
    public static final int COL_NEG             = 29;
    public static final int COL_SCROUNGE        = 30;
    public static final int COL_TOUGH           = 31;
    public static final int COL_EDGE            = 32;
    public static final int COL_NABIL           = 33;
    public static final int COL_NIMP            = 34;
    public static final int COL_HITS            = 35;
    public static final int COL_KILLS           = 36;
    public static final int COL_SALARY          = 37;
    public static final int COL_XP              = 38;
    public static final int COL_BLOODNAME       = 39;
    public static final int COL_ORIGIN_FACTION  = 40;
    public static final int COL_ORIGIN_PLANET   = 41;
    public static final int N_COL               = 42;

    public PersonnelTableModel(Campaign c) {
        data = new ArrayList<Person>();
        campaign = c;
    }

    /**
     * Gets a value indicating whether or not the table model should
     * group personnel by their unit.
     * @return A value indicating whether or not the table model groups
     *         users by their unit.
     */
    public boolean getGroupByUnit() {
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
        return N_COL;
    }

    @Override
    public String getColumnName(int column) {
        switch(column) {
            case COL_RANK:
                return "Rank";
            case COL_NAME:
                return "Name";
            case COL_CALL:
                return "Callsign";
            case COL_AGE:
                return "Age";
            case COL_GENDER:
                return "Gender";
            case COL_TYPE:
                return "Role";
            case COL_MECH:
                return "Mech";
            case COL_AERO:
                return "Aero";
            case COL_JET:
                return "Aircraft";
            case COL_VEE:
                return "Vehicle";
            case COL_VTOL:
                return "VTOL";
            case COL_NVEE:
                return "Naval";
            case COL_SPACE:
                return "Spacecraft";
            case COL_ARTY:
                return "Artillery";
            case COL_GUN_BA:
                return "G/Battlesuit";
            case COL_SMALL_ARMS:
                return "Small Arms";
            case COL_ANTI_MECH:
                return "Anti-Mech";
            case COL_TACTICS:
                return "Tactics";
            case COL_STRATEGY:
                return "Strategy";
            case COL_TECH_MECH:
                return "Tech/Mech";
            case COL_TECH_AERO:
                return "Tech/Aero";
            case COL_TECH_VEE:
                return "Mechanic";
            case COL_TECH_BA:
                return "Tech/BA";
            case COL_MEDICAL:
                return "Medical";
            case COL_ADMIN:
                return "Admin";
            case COL_NEG:
                return "Negotiation";
            case COL_SCROUNGE:
                return "Scrounge";
            case COL_TOUGH:
                return "Toughness";
            case COL_SKILL:
                return "Skill Level";
            case COL_ASSIGN:
                return "Unit Assignment";
            case COL_EDGE:
                return "Edge";
            case COL_NABIL:
                return "# Abilities";
            case COL_NIMP:
                return "# Implants";
            case COL_HITS:
                return "Hits";
            case COL_XP:
                return "XP";
            case COL_DEPLOY:
                return "Deployed";
            case COL_FORCE:
                return "Force";
            case COL_SALARY:
                return "Salary";
            case COL_KILLS:
                return "Kills";
            case COL_BLOODNAME:
                return "Bloodname";
            case COL_ORIGIN_FACTION:
                return "Origin Faction";
            case COL_ORIGIN_PLANET:
                return "Origin Planet";
            default:
                return "?";
        }
    }

    public int getColumnWidth(int c) {
        switch(c) {
        case COL_RANK:
        case COL_DEPLOY:
            return 70;
        case COL_CALL:
        case COL_BLOODNAME:
        case COL_SALARY:
        case COL_SKILL:
            return 50;
        case COL_TYPE:
        case COL_FORCE:
            return 100;
        case COL_NAME:
        case COL_ASSIGN:
            return 125;
        default:
            return 20;
        }
    }

    public int getAlignment(int col) {
        switch(col) {
            case COL_SALARY:
                return SwingConstants.RIGHT;
            case COL_RANK:
            case COL_NAME:
            case COL_GENDER:
            case COL_TYPE:
            case COL_DEPLOY:
            case COL_FORCE:
            case COL_ASSIGN:
            case COL_SKILL:
                return SwingConstants.LEFT;
            default:
                return SwingConstants.CENTER;
        }
    }

    public String getTooltip(int row, int col) {
        Person p = getPerson(row);
        switch(col) {
        case COL_NABIL:
            return p.getAbilityList(PilotOptions.LVL3_ADVANTAGES);
        case COL_NIMP:
            return p.getAbilityList(PilotOptions.MD_ADVANTAGES);
        case COL_ASSIGN:
            if ((p.getTechUnitIDs().size() > 1) && !loadAssignmentFromMarket) {
                String toReturn = "<html>";
                for (UUID id : p.getTechUnitIDs()) {
                    Unit u = getCampaign().getUnit(id);
                    if (null != u) {
                        toReturn += u.getName() + "<br>";
                    }
                }
                toReturn += "</html>";
                return toReturn;
            } else {
                return null;
            }
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

    public Person getPerson(int i) {
        if( i >= data.size()) {
            return null;
        }
        return (Person)data.get(i);
    }

    public boolean isDeployed(int row) {
        return getPerson(row).isDeployed();
    }

    @Override
    public Object getValueAt(int row, int col) {
        Person p;
        if (data.isEmpty()) {
            return "";
        } else {
            p = getPerson(row);
        }

        String toReturn = "";

        switch (col) {
            case COL_RANK:
                return p.makeHTMLRank();
            case COL_NAME:
                if (!getGroupByUnit()) {
                    return p.getName();
                } else {
                    // If we're grouping by unit, determine the number of persons under
                    // their command.
                    UUID unitId = p.getUnitId();

                    // If the personnel does not have a unit, return their name
                    if (unitId == null) {
                        return p.getName();
                    }

                    // Get the actual unit
                    Unit u = getCampaign().getUnit(unitId);
                    if (u == null) {
                        // This should not happen, but if it does, just return their name
                        return p.getName();
                    }

                    // Get the crew for the unit
                    ArrayList<Person> crew = u.getCrew();

                    // The crew count is the number of personnel under their charge,
                    // excepting themselves.
                    int crewCount = crew.size() - 1;
                    if (crewCount <= 0) {
                        // If there is only one crew member, just return their name
                        return p.getName();
                    }

                    StringBuilder builder = new StringBuilder(p.getName());
                    builder.append(" (+");
                    builder.append(crewCount);
                    if (u.usesSoldiers()) {
                        builder.append(" soldiers)");
                    } else {
                        builder.append(" crew)");
                    }
                    return builder.toString();
                }
            case COL_CALL:
                return p.getCallsign();
            case COL_GENDER:
                return p.getGenderName();
            case COL_AGE:
                return Integer.toString(p.getAge(getCampaign().getCalendar()));
            case COL_TYPE:
                return p.getRoleDesc();
            case COL_MECH:
                if (p.hasSkill(SkillType.S_GUN_MECH)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_MECH).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if (p.hasSkill(SkillType.S_PILOT_MECH)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_MECH).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;
            case COL_AERO:
                if (p.hasSkill(SkillType.S_GUN_AERO)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_AERO).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if (p.hasSkill(SkillType.S_PILOT_AERO)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_AERO).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;
            case COL_JET:
                if (p.hasSkill(SkillType.S_GUN_JET)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_JET).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if (p.hasSkill(SkillType.S_PILOT_JET)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_JET).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;
            case COL_SPACE:
                if (p.hasSkill(SkillType.S_GUN_SPACE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if (p.hasSkill(SkillType.S_PILOT_SPACE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;
            case COL_VEE:
                if (p.hasSkill(SkillType.S_GUN_VEE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if (p.hasSkill(SkillType.S_PILOT_GVEE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_GVEE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;
            case COL_NVEE:
                if (p.hasSkill(SkillType.S_GUN_VEE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if (p.hasSkill(SkillType.S_PILOT_NVEE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_NVEE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;
            case COL_VTOL:
                if (p.hasSkill(SkillType.S_GUN_VEE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if (p.hasSkill(SkillType.S_PILOT_VTOL)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_VTOL).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;
            case COL_GUN_BA:
                if (p.hasSkill(SkillType.S_GUN_BA)) {
                    return Integer.toString(p.getSkill(SkillType.S_GUN_BA).getFinalSkillValue());
                }
                break;
            case COL_ANTI_MECH:
                if (p.hasSkill(SkillType.S_ANTI_MECH)) {
                    return Integer.toString(p.getSkill(SkillType.S_ANTI_MECH).getFinalSkillValue());
                }
                break;
            case COL_SMALL_ARMS:
                if (p.hasSkill(SkillType.S_SMALL_ARMS)) {
                    return Integer.toString(p.getSkill(SkillType.S_SMALL_ARMS).getFinalSkillValue());
                }
                break;
            case COL_ARTY:
                if (p.hasSkill(SkillType.S_ARTILLERY)) {
                    return Integer.toString(p.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue());
                }
                break;
            case COL_TACTICS:
                if (p.hasSkill(SkillType.S_TACTICS)) {
                    return Integer.toString(p.getSkill(SkillType.S_TACTICS).getFinalSkillValue());
                }
                break;
            case COL_STRATEGY:
                if (p.hasSkill(SkillType.S_STRATEGY)) {
                    return Integer.toString(p.getSkill(SkillType.S_STRATEGY).getFinalSkillValue());
                }
                break;
            case COL_TECH_MECH:
                if (p.hasSkill(SkillType.S_TECH_MECH)) {
                    return Integer.toString(p.getSkill(SkillType.S_TECH_MECH).getFinalSkillValue());
                }
                break;
            case COL_TECH_AERO:
                if (p.hasSkill(SkillType.S_TECH_AERO)) {
                    return Integer.toString(p.getSkill(SkillType.S_TECH_AERO).getFinalSkillValue());
                }
                break;
            case COL_TECH_VEE:
                if (p.hasSkill(SkillType.S_TECH_MECHANIC)) {
                    return Integer.toString(p.getSkill(SkillType.S_TECH_MECHANIC).getFinalSkillValue());
                }
                break;
            case COL_TECH_BA:
                if (p.hasSkill(SkillType.S_TECH_BA)) {
                    return Integer.toString(p.getSkill(SkillType.S_TECH_BA).getFinalSkillValue());
                }
                break;
            case COL_MEDICAL:
                if (p.hasSkill(SkillType.S_DOCTOR)) {
                    return Integer.toString(p.getSkill(SkillType.S_DOCTOR).getFinalSkillValue());
                }
                break;
            case COL_ADMIN:
                if (p.hasSkill(SkillType.S_ADMIN)) {
                    return Integer.toString(p.getSkill(SkillType.S_ADMIN).getFinalSkillValue());
                }
                break;
            case COL_NEG:
                if (p.hasSkill(SkillType.S_NEG)) {
                    return Integer.toString(p.getSkill(SkillType.S_NEG).getFinalSkillValue());
                }
                break;
            case COL_SCROUNGE:
                if (p.hasSkill(SkillType.S_SCROUNGE)) {
                    return Integer.toString(p.getSkill(SkillType.S_SCROUNGE).getFinalSkillValue());
                }
                break;
            case COL_EDGE:
                return Integer.toString(p.getEdge());
            case COL_NABIL:
                return Integer.toString(p.countOptions(PilotOptions.LVL3_ADVANTAGES));
            case COL_NIMP:
                return Integer.toString(p.countOptions(PilotOptions.MD_ADVANTAGES));
            case COL_HITS:
                return Integer.toString(p.getHits());
            case COL_SKILL:
                return p.getSkillSummary();
            case COL_ASSIGN:
                if (loadAssignmentFromMarket) {
                    Entity en = personnelMarket.getAttachedEntity(p);

                    if (null == en) {
                        return "-";
                    }

                    return en.getDisplayName();
                } else {
                    Unit u = getCampaign().getUnit(p.getUnitId());
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
                                name = name + " [Pilot]";
                            } else if (u.isGunner(p)) {
                                name = name + " [Gunner]";
                            } else {
                                name = name + " [Crew]";
                            }
                        }
                        return name;
                    }
                    //check for tech
                    if (!p.getTechUnitIDs().isEmpty()) {
                        if (p.getTechUnitIDs().size() == 1) {
                            u = getCampaign().getUnit(p.getTechUnitIDs().get(0));
                            if (null != u) {
                                return u.getName() + " (" + p.getMaintenanceTimeUsing() + "m)";
                            }
                        } else {
                            return "" + p.getTechUnitIDs().size() + " units (" + p.getMaintenanceTimeUsing() + "m)";
                        }
                    }
                }
                break;
            case COL_XP:
                return Integer.toString(p.getXp());
            case COL_DEPLOY:
                Unit u = getCampaign().getUnit(p.getUnitId());
                if ((null != u) && u.isDeployed()) {
                    return getCampaign().getScenario(u.getScenarioId()).getName();
                }

                break;
            case COL_FORCE:
                Force force = getCampaign().getForceFor(p);
                if (null != force) {
                    return force.getName();
                } else {
                    return "None";
                }
            case COL_SALARY:
                return p.getSalary().toAmountAndSymbolString();
            case COL_KILLS:
                return Integer.toString(getCampaign().getKillsFor(p.getId()).size());
            case COL_BLOODNAME:
                return p.getBloodname();
            case COL_ORIGIN_FACTION:
                return p.getOriginFaction().getFullName(getCampaign().getGameYear());
            case COL_ORIGIN_PLANET:
                Planet originPlanet = p.getOriginPlanet();
                if (originPlanet != null) {
                    return originPlanet.getName(getCampaign().getDateTime());
                }
                break;
            case COL_TOUGH:
            default:
               return "?";
        }

        return "-";
    }

    private Campaign getCampaign() {
        return campaign;
    }

    public void refreshData() {
        if (!getGroupByUnit()) {
            setData(new ArrayList<>(getCampaign().getPersonnel()));
        } else {
            Campaign c = getCampaign();
            ArrayList<Person> commanders = new ArrayList<>();
            for (Person p : c.getPersonnel()) {
                if (p.getUnitId() != null) {
                    UUID unitId = p.getUnitId();
                    Unit u = c.getUnit(unitId);
                    if (u != null && u.getCommander() != p) {
                        // this person is NOT the commander of their unit,
                        // skip them.
                        continue;
                    }
                }

                // 1. If they don't have a unit, add them.
                // 2. If their unit doesn't have a commander, add them.
                // 3. If their unit doesn't exist (error?), add them.
                commanders.add(p);
            }

            setData(commanders);
        }
    }

    public TableCellRenderer getRenderer(boolean graphic, IconPackage icons) {
        if (graphic) {
            return new PersonnelTableModel.VisualRenderer(icons);
        }
        return new PersonnelTableModel.Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 9054581142945717303L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            setHorizontalAlignment(getAlignment(actualCol));
            setToolTipText(getTooltip(actualRow, actualCol));

            setForeground(UIManager.getColor("Table.foreground"));
            if (isSelected) {
                setBackground(UIManager.getColor("Table.selectionBackground"));
                setForeground(UIManager.getColor("Table.selectionForeground"));
            } else {
                // tiger stripes
                if (isDeployed(actualRow)) {
                    setBackground(Color.LIGHT_GRAY);
                } else if ((Integer.parseInt((String) getValueAt(actualRow,COL_HITS)) > 0) || getPerson(actualRow).hasInjuries(true)) {
                    setBackground(Color.RED);
                } else if (getPerson(actualRow).hasOnlyHealedPermanentInjuries()) {
                    setBackground(new Color(0xee9a00));
                } else {
                    setBackground(UIManager.getColor("Table.background"));
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

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = this;
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            Person p = getPerson(actualRow);
            String color = "black";
            if (isSelected) {
                color = "white";
            }
            setText(getValueAt(actualRow, actualCol).toString(), color);

            switch(actualCol) {
                case COL_RANK:
                    setPortrait(p);
                    setText(p.getFullDesc(false), color);
                    break;
                case COL_ASSIGN:
                    if (loadAssignmentFromMarket) {
                        Entity en = personnelMarket.getAttachedEntity(p);
                        setText(en != null ? en.getDisplayName() : "-", color);
                    } else {
                        Unit u = getCampaign().getUnit(p.getUnitId());
                        if (!p.getTechUnitIDs().isEmpty()) {
                            u = getCampaign().getUnit(p.getTechUnitIDs().get(0));
                        }
                        if (null != u) {
                            String desc = "<b>" + u.getName() + "</b><br>";
                            desc += u.getEntity().getWeightClassName();
                            if ((!(u.getEntity() instanceof SmallCraft) || !(u.getEntity() instanceof Jumpship))) {
                                desc += " " + UnitType.getTypeDisplayableName(u.getEntity().getUnitType());
                            }
                            desc += "<br>" + u.getStatus() + "";
                            setText(desc, color);
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
                    break;
                case COL_FORCE:
                    Force force = getCampaign().getForceFor(p);
                    if (null != force) {
                        String desc = "<html><b>" + force.getName() + "</b>";
                        Force parent = force.getParentForce();
                        //cut off after three lines and don't include the top level
                        int lines = 1;
                        while ((parent != null) && (parent.getParentForce() != null) && (lines < 4)) {
                            desc += "<br>" + parent.getName();
                            lines++;
                            parent = parent.getParentForce();
                        }
                        desc += "</html>";
                        setText(desc, color);
                        Image forceImage = getImageFor(force);
                        if (null != forceImage) {
                            setImage(forceImage);
                        } else {
                            clearImage();
                        }
                    } else {
                        clearImage();
                    }
                    break;
                case COL_HITS:
                    Image hitImage = getHitsImage(p.getHits());
                    if (null != hitImage) {
                        setImage(hitImage);
                    } else {
                        clearImage();
                    }
                    setText("", color);
                    break;
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

        private Image getHitsImage(int hits) {
            switch(hits) {
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
            }
            return null;
        }
    }

    public void loadAssignmentFromMarket(PersonnelMarket personnelMarket) {
        this.personnelMarket = personnelMarket;
        this.loadAssignmentFromMarket = (null != personnelMarket);
    }
}
