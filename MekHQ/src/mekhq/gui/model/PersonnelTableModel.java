/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.util.EncodeControl;
import megamek.common.util.StringUtil;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;
import mekhq.gui.BasicInfo;
import mekhq.gui.enums.PersonnelTabView;
import mekhq.gui.utilities.MekHqTableCellRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * A table Model for displaying information about personnel
 * @author Jay lawson
 */
public class PersonnelTableModel extends DataTableModel {
    //region Variable Declarations
    private static final long serialVersionUID = -5207167419079014157L;

    private Campaign campaign;
    private PersonnelMarket personnelMarket;
    private boolean loadAssignmentFromMarket;
    private boolean groupByUnit;


    public static final int COL_RANK            = 0;
    public static final int COL_FIRST_NAME      = 1;
    public static final int COL_LAST_NAME       = 2;
    public static final int COL_PRENOMINAL      = 3;
    public static final int COL_GIVEN_NAME      = 4;
    public static final int COL_SURNAME         = 5;
    public static final int COL_BLOODNAME       = 6;
    public static final int COL_POSTNOMINAL     = 7;
    public static final int COL_CALL            = 8;
    public static final int COL_AGE             = 9;
    public static final int COL_STATUS          = 10;
    public static final int COL_GENDER          = 11;
    public static final int COL_SKILL           = 12;
    public static final int COL_TYPE            = 13;
    public static final int COL_ASSIGN          = 14;
    public static final int COL_FORCE           = 15;
    public static final int COL_DEPLOY          = 16;
    public static final int COL_MECH            = 17;
    public static final int COL_AERO            = 18;
    public static final int COL_JET             = 19;
    public static final int COL_VEE             = 20;
    public static final int COL_VTOL            = 21;
    public static final int COL_NVEE            = 22;
    public static final int COL_SPACE           = 23;
    public static final int COL_ARTY            = 24;
    public static final int COL_GUN_BA          = 25;
    public static final int COL_SMALL_ARMS      = 26;
    public static final int COL_ANTI_MECH       = 27;
    public static final int COL_TACTICS         = 28;
    public static final int COL_STRATEGY        = 29;
    public static final int COL_LEADERSHIP      = 30;
    public static final int COL_TECH_MECH       = 31;
    public static final int COL_TECH_AERO       = 32;
    public static final int COL_TECH_VEE        = 33;
    public static final int COL_TECH_BA         = 34;
    public static final int COL_MEDICAL         = 35;
    public static final int COL_ADMIN           = 36;
    public static final int COL_NEG             = 37;
    public static final int COL_SCROUNGE        = 38;
    public static final int COL_HITS            = 39;
    public static final int COL_KILLS           = 40;
    public static final int COL_SALARY          = 41;
    public static final int COL_XP              = 42;
    public static final int COL_ORIGIN_FACTION  = 43;
    public static final int COL_ORIGIN_PLANET   = 44;
    public static final int COL_RECRUIT_DATE    = 45;
    public static final int COL_DEATH_DATE      = 46;
    //Export Only Columns
    public static final int COL_TOUGH           = 47;
    public static final int COL_EDGE            = 48;
    public static final int COL_NABIL           = 49;
    public static final int COL_NIMP            = 50;
    public static final int COL_PORTRAIT        = 51;
    public static final int N_COL               = 52;

    private ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.PersonnelTableModel", new EncodeControl());
    //endregion Variable Declarations

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
        switch (column) {
            case COL_RANK:
                return resources.getString("col_rank.text");
            case COL_FIRST_NAME:
                return resources.getString("col_first_name.text");
            case COL_LAST_NAME:
                return resources.getString("col_last_name.text");
            case COL_PRENOMINAL:
                return resources.getString("col_prenominal.text");
            case COL_GIVEN_NAME:
                return resources.getString("col_given_name.text");
            case COL_SURNAME:
                return resources.getString("col_surname.text");
            case COL_POSTNOMINAL:
                return resources.getString("col_postnominal.text");
            case COL_CALL:
                return resources.getString("col_call.text");
            case COL_BLOODNAME:
                return resources.getString("col_bloodname.text");
            case COL_AGE:
                return resources.getString("col_age.text");
            case COL_STATUS:
                return resources.getString("col_status.text");
            case COL_GENDER:
                return resources.getString("col_gender.text");
            case COL_TYPE:
                return resources.getString("col_type.text");
            case COL_MECH:
                return resources.getString("col_mech.text");
            case COL_AERO:
                return resources.getString("col_aero.text");
            case COL_JET:
                return resources.getString("col_jet.text");
            case COL_VEE:
                return resources.getString("col_vee.text");
            case COL_VTOL:
                return resources.getString("col_vtol.text");
            case COL_NVEE:
                return resources.getString("col_nvee.text");
            case COL_SPACE:
                return resources.getString("col_space.text");
            case COL_ARTY:
                return resources.getString("col_arty.text");
            case COL_GUN_BA:
                return resources.getString("col_gun_ba.text");
            case COL_SMALL_ARMS:
                return resources.getString("col_small_arms.text");
            case COL_ANTI_MECH:
                return resources.getString("col_anti_mech.text");
            case COL_TACTICS:
                return resources.getString("col_tactics.text");
            case COL_STRATEGY:
                return resources.getString("col_strategy.text");
            case COL_LEADERSHIP:
                return resources.getString("col_leadership.text");
            case COL_TECH_MECH:
                return resources.getString("col_tech_mech.text");
            case COL_TECH_AERO:
                return resources.getString("col_tech_aero.text");
            case COL_TECH_VEE:
                return resources.getString("col_tech_vee.text");
            case COL_TECH_BA:
                return resources.getString("col_tech_ba.text");
            case COL_MEDICAL:
                return resources.getString("col_medical.text");
            case COL_ADMIN:
                return resources.getString("col_admin.text");
            case COL_NEG:
                return resources.getString("col_neg.text");
            case COL_SCROUNGE:
                return resources.getString("col_scrounge.text");
            case COL_SKILL:
                return resources.getString("col_skill.text");
            case COL_ASSIGN:
                return resources.getString("col_assign.text");
            case COL_HITS:
                return resources.getString("col_hits.text");
            case COL_XP:
                return resources.getString("col_xp.text");
            case COL_DEPLOY:
                return resources.getString("col_deploy.text");
            case COL_FORCE:
                return resources.getString("col_force.text");
            case COL_SALARY:
                return resources.getString("col_salary.text");
            case COL_KILLS:
                return resources.getString("col_kills.text");
            case COL_ORIGIN_FACTION:
                return resources.getString("col_origin_faction.text");
            case COL_ORIGIN_PLANET:
                return resources.getString("col_origin_planet.text");
            case COL_RECRUIT_DATE:
                return resources.getString("col_recruit_date.text");
            case COL_DEATH_DATE:
                return resources.getString("col_death_date.text");
            case COL_TOUGH:
                return resources.getString("col_tough.text");
            case COL_EDGE:
                return resources.getString("col_edge.text");
            case COL_NABIL:
                return resources.getString("col_nabil.text");
            case COL_NIMP:
                return resources.getString("col_nimp.text");
            case COL_PORTRAIT:
                return resources.getString("col_portrait.text");
            default:
                return resources.getString("col_unknown.text");
        }
    }

    public int getColumnWidth(int c) {
        switch (c) {
            case COL_GIVEN_NAME:
            case COL_FIRST_NAME:
            case COL_RANK:
            case COL_DEPLOY:
                return 70;
            case COL_CALL:
            case COL_SURNAME:
            case COL_LAST_NAME:
            case COL_BLOODNAME:
            case COL_SALARY:
            case COL_SKILL:
                return 50;
            case COL_TYPE:
            case COL_FORCE:
                return 100;
            case COL_ASSIGN:
                return 125;
            default:
                return 20;
        }
    }

    public int getAlignment(int col) {
        switch (col) {
            case COL_SALARY:
                return SwingConstants.RIGHT;
            case COL_RANK:
            case COL_FIRST_NAME:
            case COL_LAST_NAME:
            case COL_PRENOMINAL:
            case COL_GIVEN_NAME:
            case COL_SURNAME:
            case COL_BLOODNAME:
            case COL_POSTNOMINAL:
            case COL_CALL:
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
        switch (col) {
            case COL_STATUS:
                return p.getStatus().getToolTipText();
            case COL_ASSIGN: {
                if ((p.getTechUnits().size() > 1) && !loadAssignmentFromMarket) {
                    StringBuilder toReturn = new StringBuilder("<html>");
                    for (Unit u : p.getTechUnits()) {
                        toReturn.append(u.getName()).append("<br>");
                    }
                    toReturn.append("</html>");
                    return toReturn.toString();
                } else {
                    return null;
                }
            }
            case COL_NABIL:
                return p.getAbilityListAsString(PersonnelOptions.LVL3_ADVANTAGES);
            case COL_NIMP:
                return p.getAbilityListAsString(PersonnelOptions.MD_ADVANTAGES);
            default:
                return null;
        }
    }

    public Person getPerson(int i) {
        return (i < getRowCount()) ? (Person) data.get(i) : null;
    }

    public boolean isDeployed(int row) {
        return getPerson(row).isDeployed();
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (data.isEmpty()) {
            return "";
        }
        Person p = getPerson(row);
        if (p == null) {
            return "?";
        }
        String toReturn = "";

        switch (col) {
            case COL_RANK:
                return p.makeHTMLRank();
            case COL_FIRST_NAME:
                return p.getFirstName();
            case COL_LAST_NAME:
                return p.getLastName();
            case COL_PRENOMINAL:
                return p.getPreNominal();
            case COL_GIVEN_NAME:
                return p.getGivenName();
            case COL_SURNAME: {
                toReturn = p.getSurname();
                if (StringUtil.isNullOrEmpty(toReturn)) {
                    return "";
                } else if (!getGroupByUnit()) {
                    return toReturn;
                } else {
                    // If we're grouping by unit, determine the number of persons under
                    // their command.
                    Unit u = p.getUnit();

                    // If the personnel does not have a unit, return their name
                    if (u == null) {
                        return toReturn;
                    }

                    // The crew count is the number of personnel under their charge,
                    // excepting themselves.
                    int crewCount = u.getCrew().size() - 1;
                    if (crewCount <= 0) {
                        // If there is only one crew member, just return their name
                        return toReturn;
                    }

                    return toReturn + " (+" + crewCount +
                            (u.usesSoldiers()
                                    ? resources.getString("surname_soldiers.text")
                                    : resources.getString("surname_crew.text"));
                }
            }
            case COL_POSTNOMINAL:
                return p.getPostNominal();
            case COL_CALL:
                return p.getCallsign();
            case COL_BLOODNAME:
                return p.getBloodname();
            case COL_GENDER:
                return GenderDescriptors.MALE_FEMALE.getDescriptorCapitalized(p.getGender());
            case COL_AGE:
                return Integer.toString(p.getAge(getCampaign().getLocalDate()));
            case COL_STATUS:
                return p.getStatus();
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
            case COL_LEADERSHIP:
                if (p.hasSkill(SkillType.S_LEADER)) {
                    return Integer.toString(p.getSkill(SkillType.S_LEADER).getFinalSkillValue());
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
                return Integer.toString(p.countOptions(PersonnelOptions.LVL3_ADVANTAGES));
            case COL_NIMP:
                return Integer.toString(p.countOptions(PersonnelOptions.MD_ADVANTAGES));
            case COL_HITS:
                return Integer.toString(p.getHits());
            case COL_SKILL:
                return p.getSkillSummary();
            case COL_ASSIGN: {
                if (loadAssignmentFromMarket) {
                    Entity en = personnelMarket.getAttachedEntity(p);
                    return ((en != null) ? en.getDisplayName() : "-");
                } else {
                    Unit u = p.getUnit();
                    if (u != null) {
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
                    if (!p.getTechUnits().isEmpty()) {
                        if (p.getTechUnits().size() == 1) {
                            u = p.getTechUnits().get(0);
                            if (u != null) {
                                return u.getName() + " (" + p.getMaintenanceTimeUsing() + "m)";
                            }
                        } else {
                            return "" + p.getTechUnits().size() + " units (" + p.getMaintenanceTimeUsing() + "m)";
                        }
                    }
                }
                break;
            }
            case COL_XP:
                return Integer.toString(p.getXP());
            case COL_DEPLOY:
                Unit u = p.getUnit();
                if ((null != u) && u.isDeployed()) {
                    return getCampaign().getScenario(u.getScenarioId()).getName();
                }
                break;
            case COL_FORCE:
                Force force = getCampaign().getForceFor(p);
                return (force != null) ? force.getName() : resources.getString("force_none.text");
            case COL_SALARY:
                return p.getSalary().toAmountAndSymbolString();
            case COL_KILLS:
                return Integer.toString(getCampaign().getKillsFor(p.getId()).size());
            case COL_ORIGIN_FACTION:
                return p.getOriginFaction().getFullName(getCampaign().getGameYear());
            case COL_ORIGIN_PLANET:
                Planet originPlanet = p.getOriginPlanet();
                if (originPlanet != null) {
                    return originPlanet.getName(getCampaign().getLocalDate());
                }
                break;
            case COL_RECRUIT_DATE:
                return p.getRecruitmentAsString();
            case COL_DEATH_DATE:
                return p.getDeathDateAsString();
            case COL_TOUGH:
                return p.getToughness();
            case COL_PORTRAIT:
                return p.getPortrait();
            default:
               return resources.getString("col_unknown.text");
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

            if (!isSelected) {
                if (isDeployed(actualRow)) {
                    setForeground(MekHQ.getMekHQOptions().getDeployedForeground());
                    setBackground(MekHQ.getMekHQOptions().getDeployedBackground());
                } else if ((Integer.parseInt((String) getValueAt(actualRow, COL_HITS)) > 0)
                        || getPerson(actualRow).hasInjuries(true)) {
                    setForeground(MekHQ.getMekHQOptions().getInjuredForeground());
                    setBackground(MekHQ.getMekHQOptions().getInjuredBackground());
                } else if (getPerson(actualRow).hasOnlyHealedPermanentInjuries()) {
                    setForeground(MekHQ.getMekHQOptions().getHealedInjuriesForeground());
                    setBackground(MekHQ.getMekHQOptions().getHealedInjuriesBackground());
                } else {
                    setBackground(UIManager.getColor("Table.background"));
                    setForeground(UIManager.getColor("Table.foreground"));
                }
            }
            return this;
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
            Person p = getPerson(actualRow);

            setText(getValueAt(actualRow, actualCol).toString());

            switch (actualCol) {
                case COL_RANK:
                    setPortrait(p);
                    setText(p.getFullDesc());
                    break;
                case COL_ASSIGN:
                    if (loadAssignmentFromMarket) {
                        Entity en = personnelMarket.getAttachedEntity(p);
                        setText((en != null) ? en.getDisplayName() : "-");
                    } else {
                        Unit u = p.getUnit();
                        if ((u == null) && !p.getTechUnits().isEmpty()) {
                            u = p.getTechUnits().get(0);
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
                case COL_FORCE:
                    Force force = getCampaign().getForceFor(p);
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
                case COL_HITS:
                    Image hitImage = getHitsImage(p.getHits());
                    if (hitImage != null) {
                        setImage(hitImage);
                    } else {
                        clearImage();
                    }
                    setHtmlText("");
                    break;
            }

            MekHqTableCellRenderer.setupTableColors(c, table, isSelected, hasFocus, row);
            return c;
        }

        private Image getHitsImage(int hits) {
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
