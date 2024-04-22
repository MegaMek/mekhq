/*
 * Copyright (c) 2021-2023 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.enums;

import megamek.codeUtilities.StringUtility;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.annotations.Nullable;
import megamek.common.util.sorter.NaturalOrderComparator;
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
import mekhq.gui.sorter.*;

import javax.swing.*;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public enum PersonnelTableModelColumn {
    //region Enum Declarations
    PERSON("PersonnelTableModelColumn.PERSON.text"),
    RANK("PersonnelTableModelColumn.RANK.text"),
    FIRST_NAME("PersonnelTableModelColumn.FIRST_NAME.text"),
    LAST_NAME("PersonnelTableModelColumn.LAST_NAME.text"),
    PRE_NOMINAL("PersonnelTableModelColumn.PRE_NOMINAL.text"),
    GIVEN_NAME("PersonnelTableModelColumn.GIVEN_NAME.text"),
    SURNAME("PersonnelTableModelColumn.SURNAME.text"),
    BLOODNAME("PersonnelTableModelColumn.BLOODNAME.text"),
    POST_NOMINAL("PersonnelTableModelColumn.POST_NOMINAL.text"),
    CALLSIGN("PersonnelTableModelColumn.CALLSIGN.text"),
    AGE("PersonnelTableModelColumn.AGE.text"),
    PERSONNEL_STATUS("PersonnelTableModelColumn.PERSONNEL_STATUS.text"),
    GENDER("PersonnelTableModelColumn.GENDER.text"),
    SKILL_LEVEL("PersonnelTableModelColumn.SKILL_LEVEL.text"),
    PERSONNEL_ROLE("PersonnelTableModelColumn.PERSONNEL_ROLE.text"),
    UNIT_ASSIGNMENT("PersonnelTableModelColumn.UNIT_ASSIGNMENT.text"),
    FORCE("PersonnelTableModelColumn.FORCE.text"),
    DEPLOYED("PersonnelTableModelColumn.DEPLOYED.text"),
    MEK("PersonnelTableModelColumn.MEK.text"),
    GROUND_VEHICLE("PersonnelTableModelColumn.GROUND_VEHICLE.text"),
    NAVAL_VEHICLE("PersonnelTableModelColumn.NAVAL_VEHICLE.text"),
    VTOL("PersonnelTableModelColumn.VTOL.text"),
    AEROSPACE("PersonnelTableModelColumn.AEROSPACE.text"),
    CONVENTIONAL_AIRCRAFT("PersonnelTableModelColumn.CONVENTIONAL_AIRCRAFT.text"),
    VESSEL("PersonnelTableModelColumn.VESSEL.text"),
    BATTLE_ARMOUR("PersonnelTableModelColumn.BATTLE_ARMOUR.text"),
    SMALL_ARMS("PersonnelTableModelColumn.SMALL_ARMS.text"),
    ANTI_MEK("PersonnelTableModelColumn.ANTI_MEK.text"),
    ARTILLERY("PersonnelTableModelColumn.ARTILLERY.text"),
    TACTICS("PersonnelTableModelColumn.TACTICS.text"),
    STRATEGY("PersonnelTableModelColumn.STRATEGY.text"),
    LEADERSHIP("PersonnelTableModelColumn.LEADERSHIP.text"),
    TECH_MEK("PersonnelTableModelColumn.TECH_MEK.text"),
    TECH_AERO("PersonnelTableModelColumn.TECH_AERO.text"),
    TECH_MECHANIC("PersonnelTableModelColumn.TECH_MECHANIC.text"),
    TECH_BA("PersonnelTableModelColumn.TECH_BA.text"),
    TECH_VESSEL("PersonnelTableModelColumn.TECH_VESSEL.text"),
    MEDICAL("PersonnelTableModelColumn.MEDICAL.text"),
    ADMINISTRATION("PersonnelTableModelColumn.ADMINISTRATION.text"),
    NEGOTIATION("PersonnelTableModelColumn.NEGOTIATION.text"),
    SCROUNGE("PersonnelTableModelColumn.SCROUNGE.text"),
    INJURIES("PersonnelTableModelColumn.INJURIES.text"),
    KILLS("PersonnelTableModelColumn.KILLS.text"),
    SALARY("PersonnelTableModelColumn.SALARY.text"),
    XP("PersonnelTableModelColumn.XP.text"),
    ORIGIN_FACTION("PersonnelTableModelColumn.ORIGIN_FACTION.text"),
    ORIGIN_PLANET("PersonnelTableModelColumn.ORIGIN_PLANET.text"),
    BIRTHDAY("PersonnelTableModelColumn.BIRTHDAY.text"),
    RECRUITMENT_DATE("PersonnelTableModelColumn.RECRUITMENT_DATE.text"),
    LAST_RANK_CHANGE_DATE("PersonnelTableModelColumn.LAST_RANK_CHANGE_DATE.text"),
    DUE_DATE("PersonnelTableModelColumn.DUE_DATE.text"),
    RETIREMENT_DATE("PersonnelTableModelColumn.RETIREMENT_DATE.text"),
    DEATH_DATE("PersonnelTableModelColumn.DEATH_DATE.text"),
    COMMANDER("PersonnelTableModelColumn.COMMANDER.text"),
    FOUNDER("PersonnelTableModelColumn.FOUNDER.text"),
    CLAN_PERSONNEL("PersonnelTableModelColumn.CLAN_PERSONNEL.text"),
    MARRIAGEABLE("PersonnelTableModelColumn.MARRIAGEABLE.text"),
    DIVORCEABLE("PersonnelTableModelColumn.DIVORCEABLE.text"),
    TRYING_TO_CONCEIVE("PersonnelTableModelColumn.TRYING_TO_CONCEIVE.text"),
    IMMORTAL("PersonnelTableModelColumn.IMMORTAL.text"),
    TOUGHNESS("PersonnelTableModelColumn.TOUGHNESS.text"),
    EDGE("PersonnelTableModelColumn.EDGE.text"),
    SPA_COUNT("PersonnelTableModelColumn.SPA_COUNT.text"),
    IMPLANT_COUNT("PersonnelTableModelColumn.IMPLANT_COUNT.text"),
    PORTRAIT_PATH("PersonnelTableModelColumn.PORTRAIT_PATH.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Constructors
    PersonnelTableModelColumn(final String name) {
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparison Methods
    public boolean isPerson() {
        return this == PERSON;
    }

    public boolean isRank() {
        return this == RANK;
    }

    public boolean isFirstName() {
        return this == FIRST_NAME;
    }

    public boolean isLastName() {
        return this == LAST_NAME;
    }

    public boolean isPreNominal() {
        return this == PRE_NOMINAL;
    }

    public boolean isGivenName() {
        return this == GIVEN_NAME;
    }

    public boolean isSurname() {
        return this == SURNAME;
    }

    public boolean isBloodname() {
        return this == BLOODNAME;
    }

    public boolean isPostNominal() {
        return this == POST_NOMINAL;
    }

    public boolean isCallsign() {
        return this == CALLSIGN;
    }

    public boolean isAge() {
        return this == AGE;
    }

    public boolean isPersonnelStatus() {
        return this == PERSONNEL_STATUS;
    }

    public boolean isGender() {
        return this == GENDER;
    }

    public boolean isSkillLevel() {
        return this == SKILL_LEVEL;
    }

    public boolean isPersonnelRole() {
        return this == PERSONNEL_ROLE;
    }

    public boolean isUnitAssignment() {
        return this == UNIT_ASSIGNMENT;
    }

    public boolean isForce() {
        return this == FORCE;
    }

    public boolean isDeployed() {
        return this == DEPLOYED;
    }

    public boolean isMek() {
        return this == MEK;
    }

    public boolean isGroundVehicle() {
        return this == GROUND_VEHICLE;
    }

    public boolean isNavalVehicle() {
        return this == NAVAL_VEHICLE;
    }

    public boolean isVTOL() {
        return this == VTOL;
    }

    public boolean isAerospace() {
        return this == AEROSPACE;
    }

    public boolean isConventionalAircraft() {
        return this == CONVENTIONAL_AIRCRAFT;
    }

    public boolean isVessel() {
        return this == VESSEL;
    }

    public boolean isBattleArmour() {
        return this == BATTLE_ARMOUR;
    }

    public boolean isSmallArms() {
        return this == SMALL_ARMS;
    }

    public boolean isAntiMek() {
        return this == ANTI_MEK;
    }

    public boolean isArtillery() {
        return this == ARTILLERY;
    }

    public boolean isTactics() {
        return this == TACTICS;
    }

    public boolean isStrategy() {
        return this == STRATEGY;
    }

    public boolean isLeadership() {
        return this == LEADERSHIP;
    }

    public boolean isTechMek() {
        return this == TECH_MEK;
    }

    public boolean isTechAero() {
        return this == TECH_AERO;
    }

    public boolean isTechMechanic() {
        return this == TECH_MECHANIC;
    }

    public boolean isTechBA() {
        return this == TECH_BA;
    }
    public boolean isTechVessel() {
        return this == TECH_VESSEL;
    }

    public boolean isMedical() {
        return this == MEDICAL;
    }

    public boolean isAdministration() {
        return this == ADMINISTRATION;
    }

    public boolean isNegotiation() {
        return this == NEGOTIATION;
    }

    public boolean isScrounge() {
        return this == SCROUNGE;
    }

    public boolean isInjuries() {
        return this == INJURIES;
    }

    public boolean isKills() {
        return this == KILLS;
    }

    public boolean isSalary() {
        return this == SALARY;
    }

    public boolean isXP() {
        return this == XP;
    }

    public boolean isOriginFaction() {
        return this == ORIGIN_FACTION;
    }

    public boolean isOriginPlanet() {
        return this == ORIGIN_PLANET;
    }

    public boolean isBirthday() {
        return this == BIRTHDAY;
    }

    public boolean isRecruitmentDate() {
        return this == RECRUITMENT_DATE;
    }

    public boolean isLastRankChangeDate() {
        return this == LAST_RANK_CHANGE_DATE;
    }

    public boolean isDueDate() {
        return this == DUE_DATE;
    }

    public boolean isRetirementDate() {
        return this == RETIREMENT_DATE;
    }

    public boolean isDeathDate() {
        return this == DEATH_DATE;
    }

    public boolean isCommander() {
        return this == COMMANDER;
    }

    public boolean isFounder() {
        return this == FOUNDER;
    }

    public boolean isClanPersonnel() {
        return this == CLAN_PERSONNEL;
    }

    public boolean isMarriageable() {
        return this == MARRIAGEABLE;
    }

    public boolean isDivorceable() {
        return this == DIVORCEABLE;
    }

    public boolean isTryingToConceive() {
        return this == TRYING_TO_CONCEIVE;
    }

    public boolean isImmortal() {
        return this == IMMORTAL;
    }

    public boolean isToughness() {
        return this == TOUGHNESS;
    }

    public boolean isEdge() {
        return this == EDGE;
    }

    public boolean isSPACount() {
        return this == SPA_COUNT;
    }

    public boolean isImplantCount() {
        return this == IMPLANT_COUNT;
    }

    public boolean isPortraitPath() {
        return this == PORTRAIT_PATH;
    }
    //endregion Boolean Comparison Methods

    public String getCellValue(final Campaign campaign, final PersonnelMarket personnelMarket,
                               final Person person, final boolean loadAssignmentFromMarket,
                               final boolean groupByUnit) {
        switch (this) {
            case PERSON:
                return "";
            case RANK:
                return person.makeHTMLRank();
            case FIRST_NAME:
                return person.getFirstName();
            case LAST_NAME:
                return person.getLastName();
            case PRE_NOMINAL:
                return person.getPreNominal();
            case GIVEN_NAME:
                return person.getGivenName();
            case SURNAME: {
                final String surname = person.getSurname();
                if (StringUtility.isNullOrBlank(surname)) {
                    return "";
                } else if (!groupByUnit) {
                    return surname;
                } else {
                    // If we're grouping by unit, determine the number of persons under their command.
                    final Unit unit = person.getUnit();

                    // If the personnel does not have a unit, return their name
                    if (unit == null) {
                        return surname;
                    }

                    // The crew size is the number of personnel under their charge, excluding
                    // themselves of course
                    final int crewSize = unit.getCrew().size() - 1;
                    if (crewSize <= 0) {
                        // If there is only one crew member, just return their name
                        return surname;
                    }

                    return surname + " (+" + crewSize+ resources.getString(unit.usesSoldiers()
                            ? "PersonnelTableModelColumn.SURNAME.Soldiers.text"
                            : "PersonnelTableModelColumn.SURNAME.Crew.text");
                }
            }
            case BLOODNAME:
                return person.getBloodname();
            case POST_NOMINAL:
                return person.getPostNominal();
            case CALLSIGN:
                return person.getCallsign();
            case AGE:
            case BIRTHDAY:
                return MekHQ.getMHQOptions().getDisplayFormattedDate(person.getBirthday());
            case PERSONNEL_STATUS:
                return person.getStatus().toString();
            case GENDER:
                return GenderDescriptors.MALE_FEMALE_OTHER.getDescriptorCapitalized(person.getGender());
            case SKILL_LEVEL:
                return person.getSkillLevel(campaign, false).toString();
            case PERSONNEL_ROLE:
                return person.getRoleDesc();
            case UNIT_ASSIGNMENT: {
                if (loadAssignmentFromMarket) {
                    final Entity entity = personnelMarket.getAttachedEntity(person);
                    return (entity == null) ? "-" : entity.getDisplayName();
                } else {
                    Unit unit = person.getUnit();
                    if (unit != null) {
                        String name = unit.getName();
                        if (unit.getEntity() instanceof Tank) {
                            if (unit.isDriver(person)) {
                                name = name + " [Driver]";
                            } else {
                                name = name + " [Gunner]";
                            }
                        } else if ((unit.getEntity() instanceof SmallCraft)
                                || (unit.getEntity() instanceof Jumpship)) {
                            if (unit.isNavigator(person)) {
                                name = name + " [Navigator]";
                            } else if (unit.isDriver(person)) {
                                name = name + " [Pilot]";
                            } else if (unit.isGunner(person)) {
                                name = name + " [Gunner]";
                            } else {
                                name = name + " [Crew]";
                            }
                        }

                        return name;
                    }

                    // Check for tech units
                    if (!person.getTechUnits().isEmpty()) {
                        if (person.getTechUnits().size() == 1) {
                            unit = person.getTechUnits().get(0);
                            if (unit != null) {
                                return unit.getName() + " (" + person.getMaintenanceTimeUsing() + "m)";
                            }
                        } else {
                            return "" + person.getTechUnits().size() + " units (" + person.getMaintenanceTimeUsing() + "m)";
                        }
                    }
                }

                // Final fallback return of nothing
                return "-";
            }
            case FORCE:
                final Force force = campaign.getForceFor(person);
                return (force == null) ? "-" : force.getName();
            case DEPLOYED:
                final Unit unit = person.getUnit();
                return ((unit == null) || !unit.isDeployed()) ? "-"
                        : campaign.getScenario(unit.getScenarioId()).getName();
            case MEK:
                return (person.hasSkill(SkillType.S_GUN_MECH)
                                ? Integer.toString(person.getSkill(SkillType.S_GUN_MECH).getFinalSkillValue())
                                : "-")
                        + "/"
                        + (person.hasSkill(SkillType.S_PILOT_MECH)
                                ? Integer.toString(person.getSkill(SkillType.S_PILOT_MECH).getFinalSkillValue())
                                : "-");
            case GROUND_VEHICLE:
                return (person.hasSkill(SkillType.S_GUN_VEE)
                                ? Integer.toString(person.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue())
                                : "-")
                        + "/"
                        + (person.hasSkill(SkillType.S_PILOT_GVEE)
                                ? Integer.toString(person.getSkill(SkillType.S_PILOT_GVEE).getFinalSkillValue())
                                : "-");
            case NAVAL_VEHICLE:
                return (person.hasSkill(SkillType.S_GUN_VEE)
                                ? Integer.toString(person.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue())
                                : "-")
                        + "/"
                        + (person.hasSkill(SkillType.S_PILOT_NVEE)
                                ? Integer.toString(person.getSkill(SkillType.S_PILOT_NVEE).getFinalSkillValue())
                                : "-");
            case VTOL:
                return (person.hasSkill(SkillType.S_GUN_VEE)
                                ? Integer.toString(person.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue())
                                : "-")
                        + "/"
                        + (person.hasSkill(SkillType.S_PILOT_VTOL)
                                ? Integer.toString(person.getSkill(SkillType.S_PILOT_VTOL).getFinalSkillValue())
                                : "-");
            case AEROSPACE:
                return (person.hasSkill(SkillType.S_GUN_AERO)
                                ? Integer.toString(person.getSkill(SkillType.S_GUN_AERO).getFinalSkillValue())
                                : "-")
                        + "/"
                        + (person.hasSkill(SkillType.S_PILOT_AERO)
                                ? Integer.toString(person.getSkill(SkillType.S_PILOT_AERO).getFinalSkillValue())
                                : "-");
            case CONVENTIONAL_AIRCRAFT:
                return (person.hasSkill(SkillType.S_GUN_JET)
                                ? Integer.toString(person.getSkill(SkillType.S_GUN_JET).getFinalSkillValue())
                                : "-")
                        + "/"
                        + (person.hasSkill(SkillType.S_PILOT_JET)
                                ? Integer.toString(person.getSkill(SkillType.S_PILOT_JET).getFinalSkillValue())
                                : "-");
            case VESSEL:
                return (person.hasSkill(SkillType.S_GUN_SPACE)
                                ? Integer.toString(person.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue())
                                : "-")
                        + "/"
                        + (person.hasSkill(SkillType.S_PILOT_SPACE)
                                ? Integer.toString(person.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue())
                                : "-");
            case BATTLE_ARMOUR:
                return person.hasSkill(SkillType.S_GUN_BA)
                        ? Integer.toString(person.getSkill(SkillType.S_GUN_BA).getFinalSkillValue())
                        : "-";
            case ANTI_MEK:
                return person.hasSkill(SkillType.S_ANTI_MECH)
                        ? Integer.toString(person.getSkill(SkillType.S_ANTI_MECH).getFinalSkillValue())
                        : "-";
            case SMALL_ARMS:
                return person.hasSkill(SkillType.S_SMALL_ARMS)
                        ? Integer.toString(person.getSkill(SkillType.S_SMALL_ARMS).getFinalSkillValue())
                        : "-";
            case ARTILLERY:
                return person.hasSkill(SkillType.S_ARTILLERY)
                        ? Integer.toString(person.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue())
                        : "-";
            case TACTICS:
                return person.hasSkill(SkillType.S_TACTICS)
                        ? Integer.toString(person.getSkill(SkillType.S_TACTICS).getFinalSkillValue())
                        : "-";
            case STRATEGY:
                return person.hasSkill(SkillType.S_STRATEGY)
                        ? Integer.toString(person.getSkill(SkillType.S_STRATEGY).getFinalSkillValue())
                        : "-";
            case LEADERSHIP:
                return person.hasSkill(SkillType.S_LEADER)
                        ? Integer.toString(person.getSkill(SkillType.S_LEADER).getFinalSkillValue())
                        : "-";
            case TECH_MEK:
                return person.hasSkill(SkillType.S_TECH_MECH)
                        ? Integer.toString(person.getSkill(SkillType.S_TECH_MECH).getFinalSkillValue())
                        : "-";
            case TECH_AERO:
                return person.hasSkill(SkillType.S_TECH_AERO)
                        ? Integer.toString(person.getSkill(SkillType.S_TECH_AERO).getFinalSkillValue())
                        : "-";
            case TECH_MECHANIC:
                return person.hasSkill(SkillType.S_TECH_MECHANIC)
                        ? Integer.toString(person.getSkill(SkillType.S_TECH_MECHANIC).getFinalSkillValue())
                        : "-";
            case TECH_BA:
                return person.hasSkill(SkillType.S_TECH_BA)
                        ? Integer.toString(person.getSkill(SkillType.S_TECH_BA).getFinalSkillValue())
                        : "-";
            case TECH_VESSEL:
                return person.hasSkill(SkillType.S_TECH_VESSEL)
                        ? Integer.toString(person.getSkill(SkillType.S_TECH_VESSEL).getFinalSkillValue())
                        : "-";
            case MEDICAL:
                return person.hasSkill(SkillType.S_DOCTOR)
                        ? Integer.toString(person.getSkill(SkillType.S_DOCTOR).getFinalSkillValue())
                        : "-";
            case ADMINISTRATION:
                return person.hasSkill(SkillType.S_ADMIN)
                        ? Integer.toString(person.getSkill(SkillType.S_ADMIN).getFinalSkillValue())
                        : "-";
            case NEGOTIATION:
                return person.hasSkill(SkillType.S_NEG)
                        ? Integer.toString(person.getSkill(SkillType.S_NEG).getFinalSkillValue())
                        : "-";
            case SCROUNGE:
                return person.hasSkill(SkillType.S_SCROUNGE)
                        ? Integer.toString(person.getSkill(SkillType.S_SCROUNGE).getFinalSkillValue())
                        : "-";
            case INJURIES:
                return Integer.toString(person.getHits());
            case KILLS:
                return Integer.toString(campaign.getKillsFor(person.getId()).size());
            case SALARY:
                return person.getSalary(campaign).toAmountAndSymbolString();
            case XP:
                return Integer.toString(person.getXP());
            case ORIGIN_FACTION:
                return person.getOriginFaction().getFullName(campaign.getGameYear());
            case ORIGIN_PLANET:
                final Planet originPlanet = person.getOriginPlanet();
                return (originPlanet == null) ? "" : originPlanet.getName(campaign.getLocalDate());
            case RECRUITMENT_DATE:
                return MekHQ.getMHQOptions().getDisplayFormattedDate(person.getRecruitment());
            case LAST_RANK_CHANGE_DATE:
                return MekHQ.getMHQOptions().getDisplayFormattedDate(person.getLastRankChangeDate());
            case DUE_DATE:
                return person.getDueDateAsString(campaign);
            case RETIREMENT_DATE:
                return MekHQ.getMHQOptions().getDisplayFormattedDate(person.getRetirement());
            case DEATH_DATE:
                return MekHQ.getMHQOptions().getDisplayFormattedDate(person.getDateOfDeath());
            case COMMANDER:
                return resources.getString(person.isCommander() ? "Yes.text" : "No.text");
            case FOUNDER:
                return resources.getString(person.isFounder() ? "Yes.text" : "No.text");
            case CLAN_PERSONNEL:
                return resources.getString(person.isClanPersonnel() ? "Yes.text" : "No.text");
            case MARRIAGEABLE:
                return resources.getString(person.getGenealogy().hasSpouse() ? "NA.text" : (person.isMarriageable() ? "Yes.text" : "No.text"));
            case DIVORCEABLE:
                return resources.getString(person.getGenealogy().hasSpouse() ? (person.isDivorceable() ? "Yes.text" : "No.text") : "NA.text");
            case TRYING_TO_CONCEIVE:
                return resources.getString(person.getGender().isFemale() ? (person.isTryingToConceive() ? "Yes.text" : "No.text") : "NA.text");
            case IMMORTAL:
                return resources.getString(person.getStatus().isDead() ? "NA.text" : (person.isImmortal() ? "Yes.text" : "No.text"));
            case TOUGHNESS:
                return Integer.toString(person.getToughness());
            case EDGE:
                return Integer.toString(person.getEdge());
            case SPA_COUNT:
                return Integer.toString(person.countOptions(PersonnelOptions.LVL3_ADVANTAGES));
            case IMPLANT_COUNT:
                return Integer.toString(person.countOptions(PersonnelOptions.MD_ADVANTAGES));
            case PORTRAIT_PATH:
                return person.getPortrait().toString();
            default:
                return "UNIMPLEMENTED";
        }
    }

    public @Nullable String getDisplayText(final Campaign campaign, final Person person) {
        switch (this) {
            case AGE:
                return Integer.toString(person.getAge(campaign.getLocalDate()));
            default:
                return null;
        }
    }

    public @Nullable String getToolTipText(final Person person,
                                           final boolean loadAssignmentFromMarket) {
        switch (this) {
            case PERSONNEL_STATUS:
                return person.getStatus().getToolTipText();
            case UNIT_ASSIGNMENT: {
                if ((person.getTechUnits().size() > 1) && !loadAssignmentFromMarket) {
                    return person.getTechUnits().stream()
                            .map(u1 -> u1.getName() + "<br>")
                            .collect(Collectors.joining("", "<html>", "</html>"));
                } else {
                    return null;
                }
            }
            case SPA_COUNT:
                return person.getAbilityListAsString(PersonnelOptions.LVL3_ADVANTAGES);
            case IMPLANT_COUNT:
                return person.getAbilityListAsString(PersonnelOptions.MD_ADVANTAGES);
            default:
                return null;
        }
    }

    public int getWidth() {
        switch (this) {
            case PERSON:
            case UNIT_ASSIGNMENT:
                return 125;
            case RANK:
            case FIRST_NAME:
            case GIVEN_NAME:
            case DEPLOYED:
                return 70;
            case LAST_NAME:
            case SURNAME:
            case BLOODNAME:
            case CALLSIGN:
            case SKILL_LEVEL:
            case SALARY:
                return 50;
            case PERSONNEL_ROLE:
            case FORCE:
                return 100;
            default:
                return 20;
        }
    }

    public int getAlignment() {
        switch (this) {
            case PERSON:
            case RANK:
            case FIRST_NAME:
            case LAST_NAME:
            case PRE_NOMINAL:
            case GIVEN_NAME:
            case SURNAME:
            case BLOODNAME:
            case POST_NOMINAL:
            case CALLSIGN:
            case GENDER:
            case SKILL_LEVEL:
            case PERSONNEL_ROLE:
            case UNIT_ASSIGNMENT:
            case FORCE:
            case DEPLOYED:
                return SwingConstants.LEFT;
            case SALARY:
                return SwingConstants.RIGHT;
            default:
                return SwingConstants.CENTER;
        }
    }

    public boolean isVisible(final Campaign campaign, final PersonnelTabView view,
                             final JTable table) {
        switch (view) {
            case GRAPHIC: {
                table.setRowHeight(80);
                switch (this) {
                    case PERSON:
                    case UNIT_ASSIGNMENT:
                    case FORCE:
                        return true;
                    default:
                        return false;
                }
            }
            case GENERAL: {
                switch (this) {
                    case RANK:
                    case FIRST_NAME:
                    case LAST_NAME:
                    case SKILL_LEVEL:
                    case PERSONNEL_ROLE:
                    case UNIT_ASSIGNMENT:
                    case FORCE:
                    case DEPLOYED:
                    case INJURIES:
                    case XP:
                        return true;
                    case SALARY:
                        return campaign.getCampaignOptions().isPayForSalaries();
                    default:
                        return false;
                }
            }
            case PILOT_GUNNERY_SKILLS: {
                switch (this) {
                    case RANK:
                    case FIRST_NAME:
                    case LAST_NAME:
                    case PERSONNEL_ROLE:
                    case MEK:
                    case GROUND_VEHICLE:
                    case NAVAL_VEHICLE:
                    case VTOL:
                    case AEROSPACE:
                    case CONVENTIONAL_AIRCRAFT:
                    case VESSEL:
                    case ARTILLERY:
                        return true;
                    default:
                        return false;
                }
            }
            case INFANTRY_SKILLS: {
                switch (this) {
                    case RANK:
                    case FIRST_NAME:
                    case LAST_NAME:
                    case PERSONNEL_ROLE:
                    case BATTLE_ARMOUR:
                    case SMALL_ARMS:
                    case ANTI_MEK:
                        return true;
                    default:
                        return false;
                }
            }
            case TACTICAL_SKILLS: {
                switch (this) {
                    case RANK:
                    case FIRST_NAME:
                    case LAST_NAME:
                    case PERSONNEL_ROLE:
                    case TACTICS:
                    case STRATEGY:
                    case LEADERSHIP:
                        return true;
                    default:
                        return false;
                }
            }
            case TECHNICAL_SKILLS: {
                switch (this) {
                    case RANK:
                    case FIRST_NAME:
                    case LAST_NAME:
                    case PERSONNEL_ROLE:
                    case TECH_MEK:
                    case TECH_AERO:
                    case TECH_MECHANIC:
                    case TECH_BA:
                    case TECH_VESSEL:
                    case MEDICAL:
                        return true;
                    default:
                        return false;
                }
            }
            case ADMINISTRATIVE_SKILLS: {
                switch (this) {
                    case RANK:
                    case FIRST_NAME:
                    case LAST_NAME:
                    case PERSONNEL_ROLE:
                    case ADMINISTRATION:
                    case NEGOTIATION:
                    case SCROUNGE:
                        return true;
                    default:
                        return false;
                }
            }
            case BIOGRAPHICAL: {
                switch (this) {
                    case RANK:
                    case FIRST_NAME:
                    case LAST_NAME:
                    case AGE:
                    case PERSONNEL_STATUS:
                    case PERSONNEL_ROLE:
                    case FOUNDER:
                        return true;
                    case ORIGIN_FACTION:
                    case ORIGIN_PLANET:
                        return campaign.getCampaignOptions().isShowOriginFaction();
                    default:
                        return false;
                }
            }
            case FLUFF: {
                switch (this) {
                    case RANK:
                    case PRE_NOMINAL:
                    case GIVEN_NAME:
                    case SURNAME:
                    case BLOODNAME:
                    case POST_NOMINAL:
                    case CALLSIGN:
                    case GENDER:
                    case PERSONNEL_ROLE:
                    case KILLS:
                        return true;
                    default:
                        return false;
                }
            }
            case DATES: {
                switch (this) {
                    case RANK:
                    case FIRST_NAME:
                    case LAST_NAME:
                    case BIRTHDAY:
                    case DEATH_DATE:
                        return true;
                    case RECRUITMENT_DATE:
                        return campaign.getCampaignOptions().isUseTimeInService();
                    case LAST_RANK_CHANGE_DATE:
                        return campaign.getCampaignOptions().isUseTimeInRank();
                    case DUE_DATE:
                        return campaign.getCampaignOptions().isUseManualProcreation()
                                || !campaign.getCampaignOptions().getRandomProcreationMethod().isNone();
                    case RETIREMENT_DATE:
                        return campaign.getCampaignOptions().isUseRetirementDateTracking();
                    default:
                        return false;
                }
            }
            case FLAGS: {
                switch (this) {
                    case RANK:
                    case FIRST_NAME:
                    case LAST_NAME:
                    case COMMANDER:
                    case FOUNDER:
                    case CLAN_PERSONNEL:
                    case MARRIAGEABLE:
                    case DIVORCEABLE:
                    case TRYING_TO_CONCEIVE:
                    case IMMORTAL:
                        return true;
                    default:
                        return false;
                }
            }
            case OTHER: {
                switch (this) {
                    case RANK:
                    case FIRST_NAME:
                    case LAST_NAME:
                    case TOUGHNESS:
                    case EDGE:
                    case SPA_COUNT:
                    case IMPLANT_COUNT:
                    case PORTRAIT_PATH:
                        return true;
                    default:
                        return false;
                }
            }
            default: {
                return false;
            }
        }
    }

    public Comparator<?> getComparator(final Campaign campaign) {
        switch (this) {
            case RANK:
                return new PersonRankStringSorter(campaign);
            case AGE:
            case BIRTHDAY:
            case RECRUITMENT_DATE:
            case LAST_RANK_CHANGE_DATE:
            case DUE_DATE:
            case RETIREMENT_DATE:
            case DEATH_DATE:
                return new DateStringComparator();
            case SKILL_LEVEL:
                return new LevelSorter();
            case MEK:
            case GROUND_VEHICLE:
            case NAVAL_VEHICLE:
            case VTOL:
            case AEROSPACE:
            case CONVENTIONAL_AIRCRAFT:
            case VESSEL:
            case BATTLE_ARMOUR:
            case SMALL_ARMS:
            case ANTI_MEK:
            case ARTILLERY:
            case TACTICS:
            case STRATEGY:
            case LEADERSHIP:
            case TECH_MEK:
            case TECH_AERO:
            case TECH_MECHANIC:
            case TECH_BA:
            case TECH_VESSEL:
            case MEDICAL:
            case ADMINISTRATION:
            case NEGOTIATION:
            case SCROUNGE:
                return new BonusSorter();
            case INJURIES:
            case KILLS:
            case XP:
            case TOUGHNESS:
            case EDGE:
            case SPA_COUNT:
            case IMPLANT_COUNT:
                return new IntegerStringSorter();
            case SALARY:
                return new FormattedNumberSorter();
            default:
                return new NaturalOrderComparator();
        }
    }

    public @Nullable SortOrder getDefaultSortOrder() {
        switch (this) {
            case RANK:
            case FIRST_NAME:
            case LAST_NAME:
            case SKILL_LEVEL:
                return SortOrder.DESCENDING;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
