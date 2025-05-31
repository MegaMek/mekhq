/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.enums;

import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.getEffectiveFatigue;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;

import megamek.client.ui.util.UIUtil;
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
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.randomEvents.personalities.enums.Aggression;
import mekhq.campaign.randomEvents.personalities.enums.Ambition;
import mekhq.campaign.randomEvents.personalities.enums.Greed;
import mekhq.campaign.randomEvents.personalities.enums.Reasoning;
import mekhq.campaign.randomEvents.personalities.enums.Social;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;
import mekhq.gui.sorter.AttributeScoreSorter;
import mekhq.gui.sorter.BonusSorter;
import mekhq.gui.sorter.DateStringComparator;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.IntegerStringSorter;
import mekhq.gui.sorter.LevelSorter;
import mekhq.gui.sorter.PersonRankStringSorter;
import mekhq.utilities.ReportingUtilities;

public enum PersonnelTableModelColumn {
    // region Enum Declarations
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
    EMPLOYED("PersonnelTableModelColumn.EMPLOYED.text"),
    TOUGHNESS("PersonnelTableModelColumn.TOUGHNESS.text"),
    CONNECTIONS("PersonnelTableModelColumn.CONNECTIONS.text"),
    WEALTH("PersonnelTableModelColumn.WEALTH.text"),
    REPUTATION("PersonnelTableModelColumn.REPUTATION.text"),
    UNLUCKY("PersonnelTableModelColumn.UNLUCKY.text"),
    FATIGUE("PersonnelTableModelColumn.FATIGUE.text"),
    EDGE("PersonnelTableModelColumn.EDGE.text"),
    SPA_COUNT("PersonnelTableModelColumn.SPA_COUNT.text"),
    IMPLANT_COUNT("PersonnelTableModelColumn.IMPLANT_COUNT.text"),
    LOYALTY("PersonnelTableModelColumn.LOYALTY.text"),
    EDUCATION("PersonnelTableModelColumn.EDUCATION.text"),
    AGGRESSION("PersonnelTableModelColumn.AGGRESSION.text"),
    AMBITION("PersonnelTableModelColumn.AMBITION.text"),
    GREED("PersonnelTableModelColumn.GREED.text"),
    SOCIAL("PersonnelTableModelColumn.SOCIAL.text"),
    REASONING("PersonnelTableModelColumn.REASONING.text"),
    STRENGTH("PersonnelTableModelColumn.STRENGTH.text"),
    BODY("PersonnelTableModelColumn.BODY.text"),
    REFLEXES("PersonnelTableModelColumn.REFLEXES.text"),
    DEXTERITY("PersonnelTableModelColumn.DEXTERITY.text"),
    INTELLIGENCE("PersonnelTableModelColumn.INTELLIGENCE.text"),
    WILLPOWER("PersonnelTableModelColumn.WILLPOWER.text"),
    CHARISMA("PersonnelTableModelColumn.CHARISMA.text");

    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Constructors
    PersonnelTableModelColumn(final String name) {
        this.name = resources.getString(name);
    }
    // endregion Constructors

    // region Boolean Comparison Methods

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

    public boolean isEmployed() {
        return this == EMPLOYED;
    }

    public boolean isToughness() {
        return this == TOUGHNESS;
    }

    public boolean isConnections() {
        return this == CONNECTIONS;
    }

    public boolean isWealth() {
        return this == WEALTH;
    }

    public boolean isReputation() {
        return this == REPUTATION;
    }

    public boolean isUnlucky() {
        return this == UNLUCKY;
    }

    public boolean isFatigue() {
        return this == FATIGUE;
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

    public boolean isLoyalty() {
        return this == LOYALTY;
    }

    public boolean isEducation() {
        return this == EDUCATION;
    }

    public boolean isAggression() {
        return this == AGGRESSION;
    }

    public boolean isAmbition() {
        return this == AMBITION;
    }

    public boolean isGreed() {
        return this == GREED;
    }

    public boolean isSocial() {
        return this == SOCIAL;
    }

    public boolean isReasoning() {
        return this == REASONING;
    }

    public boolean isStrength() {
        return this == STRENGTH;
    }

    public boolean isBody() {
        return this == BODY;
    }

    public boolean isReflexes() {
        return this == REFLEXES;
    }

    public boolean isDexterity() {
        return this == DEXTERITY;
    }

    public boolean isATOWIntelligence() {
        return this == INTELLIGENCE;
    }

    public boolean isWillpower() {
        return this == WILLPOWER;
    }

    public boolean isCharisma() {
        return this == CHARISMA;
    }

    public boolean isATOWAttribute() {
        return isStrength() ||
                     isBody() ||
                     isReflexes() ||
                     isDexterity() ||
                     isATOWIntelligence() ||
                     isWillpower() ||
                     isCharisma();
    }


    public boolean isPersonality() {
        return isAggression() || isAmbition() || isGreed() || isSocial() || isReasoning();
    }
    // endregion Boolean Comparison Methods

    public String getCellValue(final Campaign campaign, final PersonnelMarket personnelMarket, final Person person,
          final boolean loadAssignmentFromMarket, final boolean groupByUnit) {
        final PersonnelOptions options = person.getOptions();
        final Attributes attributes = person.getATOWAttributes();

        // We define these here, as they're used in multiple cases
        int currentAttributeValue;
        int attributeCap;

        String sign;

        boolean isUseAgeEffects = campaign.getCampaignOptions().isUseAgeEffects();
        boolean isClanCampaign = campaign.isClanCampaign();
        LocalDate today = campaign.getLocalDate();

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
                    // If we're grouping by unit, determine the number of persons under their
                    // command.
                    final Unit unit = person.getUnit();

                    // If the personnel does not have a unit, return their name
                    if (unit == null) {
                        return surname;
                    }

                    // The crew size is the number of personnel under their charge, excluding
                    // themselves, of course
                    final int crewSize = unit.getCrew().size() - 1;
                    if (crewSize <= 0) {
                        // If there is only one crew member, just return their name
                        return surname;
                    }

                    return surname +
                                 " (+" +
                                 crewSize +
                                 resources.getString(unit.usesSoldiers() ?
                                                           "PersonnelTableModelColumn.SURNAME.Soldiers.text" :
                                                           "PersonnelTableModelColumn.SURNAME.Crew.text");
                }
            }
            case BLOODNAME:
                return person.getBloodname();
            case POST_NOMINAL:
                return person.getPostNominal();
            case CALLSIGN:
                return person.getCallsign();
            case AGE: //Age's cell value must return birthday to allow sorting
            case BIRTHDAY:
                return MekHQ.getMHQOptions().getDisplayFormattedDate(person.getDateOfBirth());
            case PERSONNEL_STATUS:
                return person.getStatus().toString();
            case GENDER:
                return GenderDescriptors.MALE_FEMALE_OTHER.getDescriptorCapitalized(person.getGender());
            case SKILL_LEVEL:
                return "<html>" +
                             SkillType.getColoredExperienceLevelName(person.getExperienceLevel(campaign, false)) +
                             "</html>";
            case PERSONNEL_ROLE:
                return person.getFormatedRoleDescriptions(today);
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
                        } else if ((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship)) {
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
                        Unit refitUnit = person.getTechUnits()
                                               .stream()
                                               .filter(u -> u.isRefitting() && u.getRefit().getTech() == person)
                                               .findFirst()
                                               .orElse(null);
                        String refitString = null != refitUnit ? "<b>Refitting</b> " + refitUnit.getName() : "";
                        if (person.getTechUnits().size() == 1) {
                            unit = person.getTechUnits().get(0);
                            if (unit != null) {
                                return "<html>" +
                                             ReportingUtilities.separateIf(refitString,
                                                   ", ",
                                                   unit.getName() + " (" + person.getMaintenanceTimeUsing() + "m)") +
                                             "</html>";
                            }
                        } else {
                            return "<html>" +
                                         ReportingUtilities.separateIf(refitString,
                                               ", ",
                                               person.getTechUnits().size() +
                                                     " units (" +
                                                     person.getMaintenanceTimeUsing() +
                                                     "m)") +
                                         "</html>";
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
                return ((unit == null) || !unit.isDeployed()) ?
                             "-" :
                             campaign.getScenario(unit.getScenarioId()).getName();
            case MEK:
                return (person.hasSkill(SkillType.S_GUN_MEK) ?
                              Integer.toString(person.getSkill(SkillType.S_GUN_MEK)
                                                     .getFinalSkillValue(options, attributes)) :
                              "-") +
                             '/' +
                             (person.hasSkill(SkillType.S_PILOT_MEK) ?
                                    Integer.toString(person.getSkill(SkillType.S_PILOT_MEK)
                                                           .getFinalSkillValue(options, attributes)) :
                                    "-");
            case GROUND_VEHICLE:
                return (person.hasSkill(SkillType.S_GUN_VEE) ?
                              Integer.toString(person.getSkill(SkillType.S_GUN_VEE)
                                                     .getFinalSkillValue(options, attributes)) :
                              "-") +
                             '/' +
                             (person.hasSkill(SkillType.S_PILOT_GVEE) ?
                                    Integer.toString(person.getSkill(SkillType.S_PILOT_GVEE)
                                                           .getFinalSkillValue(options, attributes)) :
                                    "-");
            case NAVAL_VEHICLE:
                return (person.hasSkill(SkillType.S_GUN_VEE) ?
                              Integer.toString(person.getSkill(SkillType.S_GUN_VEE)
                                                     .getFinalSkillValue(options, attributes)) :
                              "-") +
                             '/' +
                             (person.hasSkill(SkillType.S_PILOT_NVEE) ?
                                    Integer.toString(person.getSkill(SkillType.S_PILOT_NVEE)
                                                           .getFinalSkillValue(options, attributes)) :
                                    "-");
            case VTOL:
                return (person.hasSkill(SkillType.S_GUN_VEE) ?
                              Integer.toString(person.getSkill(SkillType.S_GUN_VEE)
                                                     .getFinalSkillValue(options, attributes)) :
                              "-") +
                             '/' +
                             (person.hasSkill(SkillType.S_PILOT_VTOL) ?
                                    Integer.toString(person.getSkill(SkillType.S_PILOT_VTOL)
                                                           .getFinalSkillValue(options, attributes)) :
                                    "-");
            case AEROSPACE:
                return (person.hasSkill(SkillType.S_GUN_AERO) ?
                              Integer.toString(person.getSkill(SkillType.S_GUN_AERO)
                                                     .getFinalSkillValue(options, attributes)) :
                              "-") +
                             '/' +
                             (person.hasSkill(SkillType.S_PILOT_AERO) ?
                                    Integer.toString(person.getSkill(SkillType.S_PILOT_AERO)
                                                           .getFinalSkillValue(options, attributes)) :
                                    "-");
            case CONVENTIONAL_AIRCRAFT:
                return (person.hasSkill(SkillType.S_GUN_JET) ?
                              Integer.toString(person.getSkill(SkillType.S_GUN_JET)
                                                     .getFinalSkillValue(options, attributes)) :
                              "-") +
                             '/' +
                             (person.hasSkill(SkillType.S_PILOT_JET) ?
                                    Integer.toString(person.getSkill(SkillType.S_PILOT_JET)
                                                           .getFinalSkillValue(options, attributes)) :
                                    "-");
            case VESSEL:
                return (person.hasSkill(SkillType.S_GUN_SPACE) ?
                              Integer.toString(person.getSkill(SkillType.S_GUN_SPACE)
                                                     .getFinalSkillValue(options, attributes)) :
                              "-") +
                             '/' +
                             (person.hasSkill(SkillType.S_PILOT_SPACE) ?
                                    Integer.toString(person.getSkill(SkillType.S_PILOT_SPACE)
                                                           .getFinalSkillValue(options, attributes)) :
                                    "-");
            case BATTLE_ARMOUR:
                return person.hasSkill(SkillType.S_GUN_BA) ?
                             Integer.toString(person.getSkill(SkillType.S_GUN_BA)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case ANTI_MEK:
                return person.hasSkill(SkillType.S_ANTI_MEK) ?
                             Integer.toString(person.getSkill(SkillType.S_ANTI_MEK)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case SMALL_ARMS:
                return person.hasSkill(SkillType.S_SMALL_ARMS) ?
                             Integer.toString(person.getSkill(SkillType.S_SMALL_ARMS)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case ARTILLERY:
                return person.hasSkill(SkillType.S_ARTILLERY) ?
                             Integer.toString(person.getSkill(SkillType.S_ARTILLERY)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case TACTICS:
                return person.hasSkill(SkillType.S_TACTICS) ?
                             Integer.toString(person.getSkill(SkillType.S_TACTICS)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case STRATEGY:
                return person.hasSkill(SkillType.S_STRATEGY) ?
                             Integer.toString(person.getSkill(SkillType.S_STRATEGY)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case LEADERSHIP:
                return person.hasSkill(SkillType.S_LEADER) ?
                             Integer.toString(person.getSkill(SkillType.S_LEADER)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case TECH_MEK:
                return person.hasSkill(SkillType.S_TECH_MEK) ?
                             Integer.toString(person.getSkill(SkillType.S_TECH_MEK)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case TECH_AERO:
                return person.hasSkill(SkillType.S_TECH_AERO) ?
                             Integer.toString(person.getSkill(SkillType.S_TECH_AERO)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case TECH_MECHANIC:
                return person.hasSkill(SkillType.S_TECH_MECHANIC) ?
                             Integer.toString(person.getSkill(SkillType.S_TECH_MECHANIC)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case TECH_BA:
                return person.hasSkill(SkillType.S_TECH_BA) ?
                             Integer.toString(person.getSkill(SkillType.S_TECH_BA)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case TECH_VESSEL:
                return person.hasSkill(SkillType.S_TECH_VESSEL) ?
                             Integer.toString(person.getSkill(SkillType.S_TECH_VESSEL)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case MEDICAL:
                return person.hasSkill(SkillType.S_SURGERY) ?
                             Integer.toString(person.getSkill(SkillType.S_SURGERY)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case ADMINISTRATION:
                return person.hasSkill(SkillType.S_ADMIN) ?
                             Integer.toString(person.getSkill(SkillType.S_ADMIN)
                                                    .getFinalSkillValue(options, attributes)) :
                             "-";
            case NEGOTIATION:
                return person.hasSkill(SkillType.S_NEGOTIATION) ?
                             Integer.toString(person.getSkill(SkillType.S_NEGOTIATION)
                                                    .getFinalSkillValue(options,
                                                          attributes,
                                                          person.getAdjustedReputation(isUseAgeEffects,
                                                                isClanCampaign,
                                                                today,
                                                                person.getRankLevel()))) :
                             "-";
            case INJURIES:
                if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
                    return Integer.toString(person.getInjuries().size());
                } else {
                    return Integer.toString(person.getHits());
                }
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
                return resources.getString(person.getGenealogy().hasSpouse() ?
                                                 "NA.text" :
                                                 (person.isMarriageable() ? "Yes.text" : "No.text"));
            case DIVORCEABLE:
                return resources.getString(person.getGenealogy().hasSpouse() ?
                                                 (person.isDivorceable() ? "Yes.text" : "No.text") :
                                                 "NA.text");
            case TRYING_TO_CONCEIVE:
                return resources.getString(person.getGender().isFemale() ?
                                                 (person.isTryingToConceive() ? "Yes.text" : "No.text") :
                                                 "NA.text");
            case IMMORTAL:
                return resources.getString(person.getStatus().isDead() ?
                                                 "NA.text" :
                                                 (person.isImmortal() ? "Yes.text" : "No.text"));
            case EMPLOYED:
                return resources.getString(person.isEmployed() ? "Yes.text" : "No.text");
            case TOUGHNESS:
                return Integer.toString(person.getToughness());
            case CONNECTIONS:
                return Integer.toString(person.getConnections());
            case WEALTH:
                return Integer.toString(person.getWealth());
            case REPUTATION:
                return Integer.toString(person.getAdjustedReputation(isUseAgeEffects,
                      isClanCampaign,
                      today,
                      person.getRankLevel()));
            case UNLUCKY:
                return Integer.toString(person.getUnlucky());
            case FATIGUE:
                return Integer.toString(getEffectiveFatigue(person.getFatigue(),
                      person.isClanPersonnel(),
                      person.getSkillLevel(campaign, false),
                      campaign.getFieldKitchenWithinCapacity()));
            case EDGE:
                return Integer.toString(person.getEdge());
            case SPA_COUNT:
                return Integer.toString(person.countOptions(PersonnelOptions.LVL3_ADVANTAGES));
            case IMPLANT_COUNT:
                return Integer.toString(person.countOptions(PersonnelOptions.MD_ADVANTAGES));
            case LOYALTY:
                return String.valueOf(person.getLoyalty());
            case EDUCATION:
                return person.getEduHighestEducation().toString();
            case AGGRESSION:
                Aggression aggression = person.getAggression();
                sign = aggression.isTraitPositive() ? "+" : "-";

                return aggression + " (" + (aggression.isTraitMajor() ? sign + sign : sign) + ')';
            case AMBITION:
                Ambition ambition = person.getAmbition();
                sign = ambition.isTraitPositive() ? "+" : "-";

                return ambition + " (" + (ambition.isTraitMajor() ? sign + sign : sign) + ')';
            case GREED:
                Greed greed = person.getGreed();
                sign = greed.isTraitPositive() ? "+" : "-";

                return greed + " (" + (greed.isTraitMajor() ? sign + sign : sign) + ')';
            case SOCIAL:
                Social social = person.getSocial();
                sign = social.isTraitPositive() ? "+" : "-";

                return social + " (" + (social.isTraitMajor() ? sign + sign : sign) + ')';
            case REASONING:
                Reasoning reasoning = person.getReasoning();

                return String.valueOf(reasoning.ordinal());
            case STRENGTH:
                currentAttributeValue = person.getAttributeScore(SkillAttribute.STRENGTH);
                attributeCap = person.getAttributeCap(SkillAttribute.STRENGTH);
                return currentAttributeValue + " / " + attributeCap;
            case BODY:
                currentAttributeValue = person.getAttributeScore(SkillAttribute.BODY);
                attributeCap = person.getAttributeCap(SkillAttribute.BODY);
                return currentAttributeValue + " / " + attributeCap;
            case REFLEXES:
                currentAttributeValue = person.getAttributeScore(SkillAttribute.REFLEXES);
                attributeCap = person.getAttributeCap(SkillAttribute.REFLEXES);
                return currentAttributeValue + " / " + attributeCap;
            case DEXTERITY:
                currentAttributeValue = person.getAttributeScore(SkillAttribute.DEXTERITY);
                attributeCap = person.getAttributeCap(SkillAttribute.DEXTERITY);
                return currentAttributeValue + " / " + attributeCap;
            case INTELLIGENCE:
                currentAttributeValue = person.getAttributeScore(SkillAttribute.INTELLIGENCE);
                attributeCap = person.getAttributeCap(SkillAttribute.INTELLIGENCE);
                return currentAttributeValue + " / " + attributeCap;
            case WILLPOWER:
                currentAttributeValue = person.getAttributeScore(SkillAttribute.WILLPOWER);
                attributeCap = person.getAttributeCap(SkillAttribute.WILLPOWER);
                return currentAttributeValue + " / " + attributeCap;
            case CHARISMA:
                currentAttributeValue = person.getAttributeScore(SkillAttribute.CHARISMA);
                attributeCap = person.getAttributeCap(SkillAttribute.CHARISMA);
                return currentAttributeValue + " / " + attributeCap;
            default:
                return "UNIMPLEMENTED";
        }
    }

    public @Nullable String getDisplayText(final Campaign campaign, final Person person) {
        if (this == PersonnelTableModelColumn.AGE) {
            return Integer.toString(person.getAge(campaign.getLocalDate()));
        }
        return null;
    }

    public @Nullable String getToolTipText(final Person person, final boolean loadAssignmentFromMarket) {
        switch (this) {
            case PERSONNEL_STATUS:
                return person.getStatus().getToolTipText();
            case UNIT_ASSIGNMENT: {
                if ((person.getTechUnits().size() > 1) && !loadAssignmentFromMarket) {
                    return person.getTechUnits()
                                 .stream()
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
        return switch (this) {
            case PERSON, UNIT_ASSIGNMENT -> 125;
            case RANK, FIRST_NAME, GIVEN_NAME, DEPLOYED -> 70;
            case LAST_NAME, SURNAME, BLOODNAME, CALLSIGN, SKILL_LEVEL, SALARY -> 50;
            case PERSONNEL_ROLE -> 150;
            case FORCE -> 100;
            default -> 20;
        };
    }

    public int getAlignment() {
        return switch (this) {
            case PERSON,
                 RANK,
                 FIRST_NAME,
                 LAST_NAME,
                 PRE_NOMINAL,
                 GIVEN_NAME,
                 SURNAME,
                 BLOODNAME,
                 POST_NOMINAL,
                 CALLSIGN,
                 GENDER,
                 SKILL_LEVEL,
                 PERSONNEL_ROLE,
                 UNIT_ASSIGNMENT,
                 FORCE,
                 DEPLOYED -> SwingConstants.LEFT;
            case SALARY -> SwingConstants.RIGHT;
            default -> SwingConstants.CENTER;
        };
    }

    public boolean isVisible(final Campaign campaign, final PersonnelTabView view, final JTable table) {
        return switch (view) {
            case GRAPHIC -> {
                table.setRowHeight(UIUtil.scaleForGUI(60));
                yield switch (this) {
                    case PERSON, UNIT_ASSIGNMENT, FORCE -> true;
                    default -> false;
                };
            }
            case GENERAL -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     SKILL_LEVEL,
                     PERSONNEL_ROLE,
                     UNIT_ASSIGNMENT,
                     FORCE,
                     DEPLOYED,
                     INJURIES,
                     XP -> true;
                default -> false;
            };
            case PILOT_GUNNERY_SKILLS -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     PERSONNEL_ROLE,
                     MEK,
                     GROUND_VEHICLE,
                     NAVAL_VEHICLE,
                     VTOL,
                     AEROSPACE,
                     CONVENTIONAL_AIRCRAFT,
                     VESSEL,
                     ARTILLERY -> true;
                default -> false;
            };
            case INFANTRY_SKILLS -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, BATTLE_ARMOUR, SMALL_ARMS, ANTI_MEK -> true;
                default -> false;
            };
            case TACTICAL_SKILLS -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, TACTICS, STRATEGY, LEADERSHIP -> true;
                default -> false;
            };
            case TECHNICAL_SKILLS -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     PERSONNEL_ROLE,
                     TECH_MEK,
                     TECH_AERO,
                     TECH_MECHANIC,
                     TECH_BA,
                     TECH_VESSEL,
                     MEDICAL -> true;
                default -> false;
            };
            case ADMINISTRATIVE_SKILLS -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, ADMINISTRATION, NEGOTIATION -> true;
                default -> false;
            };
            case BIOGRAPHICAL -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME, AGE, PERSONNEL_STATUS, PERSONNEL_ROLE, EDUCATION -> true;
                case ORIGIN_FACTION, ORIGIN_PLANET -> campaign.getCampaignOptions().isShowOriginFaction();
                case SALARY -> campaign.getCampaignOptions().isPayForSalaries();
                default -> false;
            };
            case FLUFF -> switch (this) {
                case RANK,
                     PRE_NOMINAL,
                     GIVEN_NAME,
                     SURNAME,
                     BLOODNAME,
                     POST_NOMINAL,
                     CALLSIGN,
                     GENDER,
                     PERSONNEL_ROLE,
                     KILLS -> true;
                default -> false;
            };
            case DATES -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME, BIRTHDAY, DEATH_DATE, RETIREMENT_DATE -> true;
                case RECRUITMENT_DATE -> campaign.getCampaignOptions().isUseTimeInService();
                case LAST_RANK_CHANGE_DATE -> campaign.getCampaignOptions().isUseTimeInRank();
                case DUE_DATE -> campaign.getCampaignOptions().isUseManualProcreation() ||
                                       !campaign.getCampaignOptions().getRandomProcreationMethod().isNone();
                default -> false;
            };
            case FLAGS -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     COMMANDER,
                     FOUNDER,
                     CLAN_PERSONNEL,
                     MARRIAGEABLE,
                     DIVORCEABLE,
                     TRYING_TO_CONCEIVE,
                     IMMORTAL -> true;
                default -> false;
            };
            case PERSONALITY -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME -> true;
                case AGGRESSION, AMBITION, GREED, SOCIAL, REASONING ->
                      campaign.getCampaignOptions().isUseRandomPersonalities();
                default -> false;
            };
            case TRAITS -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME, CONNECTIONS, WEALTH, REPUTATION, UNLUCKY -> true;
                case EDGE -> campaign.getCampaignOptions().isUseEdge();
                default -> false;
            };
            case ATTRIBUTES -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     STRENGTH,
                     BODY,
                     REFLEXES,
                     DEXTERITY,
                     INTELLIGENCE,
                     WILLPOWER,
                     CHARISMA -> true;
                default -> false;
            };
            case OTHER -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME -> true;
                case TOUGHNESS -> campaign.getCampaignOptions().isUseToughness();
                case FATIGUE -> campaign.getCampaignOptions().isUseFatigue();
                case SPA_COUNT -> campaign.getCampaignOptions().isUseAbilities();
                case IMPLANT_COUNT -> campaign.getCampaignOptions().isUseImplants();
                case LOYALTY -> campaign.getCampaignOptions().isUseLoyaltyModifiers() &&
                                      !campaign.getCampaignOptions().isUseHideLoyalty();
                default -> false;
            };
        };
    }

    public Comparator<?> getComparator(final Campaign campaign) {
        return switch (this) {
            case RANK -> new PersonRankStringSorter(campaign);
            case AGE, BIRTHDAY, RECRUITMENT_DATE, LAST_RANK_CHANGE_DATE, DUE_DATE, RETIREMENT_DATE, DEATH_DATE ->
                  new DateStringComparator();
            case SKILL_LEVEL -> new LevelSorter();
            case MEK,
                 GROUND_VEHICLE,
                 NAVAL_VEHICLE,
                 VTOL,
                 AEROSPACE,
                 CONVENTIONAL_AIRCRAFT,
                 VESSEL,
                 BATTLE_ARMOUR,
                 SMALL_ARMS,
                 ANTI_MEK,
                 ARTILLERY,
                 TACTICS,
                 STRATEGY,
                 LEADERSHIP,
                 TECH_MEK,
                 TECH_AERO,
                 TECH_MECHANIC,
                 TECH_BA,
                 TECH_VESSEL,
                 MEDICAL,
                 ADMINISTRATION,
                 NEGOTIATION -> new BonusSorter();
            case INJURIES,
                 KILLS,
                 XP,
                 TOUGHNESS,
                 CONNECTIONS,
                 WEALTH,
                 REPUTATION,
                 UNLUCKY,
                 EDGE,
                 SPA_COUNT,
                 IMPLANT_COUNT,
                 LOYALTY,
                 REASONING -> new IntegerStringSorter();
            case STRENGTH, BODY, REFLEXES, DEXTERITY, INTELLIGENCE, WILLPOWER, CHARISMA -> new AttributeScoreSorter();
            case SALARY -> new FormattedNumberSorter();
            default -> new NaturalOrderComparator();
        };
    }

    public @Nullable SortOrder getDefaultSortOrder() {
        return switch (this) {
            case RANK, FIRST_NAME, LAST_NAME, SKILL_LEVEL -> SortOrder.DESCENDING;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
