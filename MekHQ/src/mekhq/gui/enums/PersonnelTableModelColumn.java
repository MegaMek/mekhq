/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;

import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.StringUtility;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.Mek;
import megamek.common.units.SmallCraft;
import megamek.common.units.Tank;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.base.AbstractBase;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.location.AcademyCampusLocation;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.personnel.education.EducationController;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.skills.InfantryGunnerySkills;
import mekhq.campaign.personnel.skills.ScoutingSkills;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.randomEvents.personalities.enums.Aggression;
import mekhq.campaign.randomEvents.personalities.enums.Ambition;
import mekhq.campaign.randomEvents.personalities.enums.Greed;
import mekhq.campaign.randomEvents.personalities.enums.Social;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.sorter.AttributeScoreSorter;
import mekhq.gui.sorter.BonusSorter;
import mekhq.gui.sorter.DateStringComparator;
import mekhq.gui.sorter.EducationLevelSorter;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.IntegerStringSorter;
import mekhq.gui.sorter.LevelSorter;
import mekhq.gui.sorter.PersonRankStringSorter;
import mekhq.gui.sorter.ReasoningSorter;
import mekhq.utilities.ReportingUtilities;
import org.jspecify.annotations.NonNull;

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
    PROTOMEK("PersonnelTableModelColumn.PROTOMEK.text"),
    BATTLE_ARMOUR("PersonnelTableModelColumn.BATTLE_ARMOUR.text"),
    SMALL_ARMS("PersonnelTableModelColumn.SMALL_ARMS.text"),
    ANTI_MEK("PersonnelTableModelColumn.ANTI_MEK.text"),
    ARTILLERY("PersonnelTableModelColumn.ARTILLERY.text"),
    NAVIGATION("PersonnelTableModelColumn.NAVIGATION.text"),
    TACTICS("PersonnelTableModelColumn.TACTICS.text"),
    STRATEGY("PersonnelTableModelColumn.STRATEGY.text"),
    LEADERSHIP("PersonnelTableModelColumn.LEADERSHIP.text"),
    SCOUTING("PersonnelTableModelColumn.SCOUTING.text"),
    ASTECH("PersonnelTableModelColumn.ASTECH.text"),
    TECH_MEK("PersonnelTableModelColumn.TECH_MEK.text"),
    TECH_AERO("PersonnelTableModelColumn.TECH_AERO.text"),
    TECH_MECHANIC("PersonnelTableModelColumn.TECH_MECHANIC.text"),
    TECH_BA("PersonnelTableModelColumn.TECH_BA.text"),
    TECH_VESSEL("PersonnelTableModelColumn.TECH_VESSEL.text"),
    ZERO_G("PersonnelTableModelColumn.ZERO_G.text"),
    MEDTECH("PersonnelTableModelColumn.MEDTECH.text"),
    MEDICAL("PersonnelTableModelColumn.MEDICAL.text"),
    WORK_MINUTES("PersonnelTableModelColumn.WORK_MINUTES.text"),
    TECH_MINUTES("PersonnelTableModelColumn.TECH_MINUTES.text"),
    MEDICAL_CAPACITY("PersonnelTableModelColumn.MEDICAL_CAPACITY.text"),
    APPRAISAL("PersonnelTableModelColumn.APPRAISAL.text"),
    TRAINING("PersonnelTableModelColumn.TRAINING.text"),
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
    CLAN_PERSONNEL("PersonnelTableModelColumn.CLAN_PERSONNEL.text"),
    COMMANDER("PersonnelTableModelColumn.COMMANDER.text"),
    DIVORCEABLE("PersonnelTableModelColumn.DIVORCEABLE.text"),
    EMPLOYED("PersonnelTableModelColumn.EMPLOYED.text"),
    FOUNDER("PersonnelTableModelColumn.FOUNDER.text"),
    HIDE_PERSONALITY("PersonnelTableModelColumn.HIDE_PERSONALITY.text"),
    IMMORTAL("PersonnelTableModelColumn.IMMORTAL.text"),
    MARRIAGEABLE("PersonnelTableModelColumn.MARRIAGEABLE.text"),
    NEVER_ASSIGN_AUTO_MAINTENANCE("PersonnelTableModelColumn.NEVER_ASSIGN_AUTO_MAINTENANCE.text"),
    PREFERS_MEN("PersonnelTableModelColumn.PREFERS_MEN.text"),
    PREFERS_WOMEN("PersonnelTableModelColumn.PREFERS_WOMEN.text"),
    QUICK_TRAIN_IGNORE("PersonnelTableModelColumn.QUICK_TRAIN_IGNORE.text"),
    SALVAGE_SUPERVISOR("PersonnelTableModelColumn.SALVAGE_SUPERVISOR.text"),
    SECOND_IN_COMMAND("PersonnelTableModelColumn.SECOND_IN_COMMAND.text"),
    TRYING_TO_CONCEIVE("PersonnelTableModelColumn.TRYING_TO_CONCEIVE.text"),
    UNDER_PROTECTION("PersonnelTableModelColumn.UNDER_PROTECTION.text"),
    TOUGHNESS("PersonnelTableModelColumn.TOUGHNESS.text"),
    CONNECTIONS("PersonnelTableModelColumn.CONNECTIONS.text"),
    WEALTH("PersonnelTableModelColumn.WEALTH.text"),
    EXTRA_INCOME("PersonnelTableModelColumn.EXTRA_INCOME.text"),
    REPUTATION("PersonnelTableModelColumn.REPUTATION.text"),
    UNLUCKY("PersonnelTableModelColumn.UNLUCKY.text"),
    BLOODMARK("PersonnelTableModelColumn.BLOODMARK.text"),
    FATIGUE("PersonnelTableModelColumn.FATIGUE.text"),
    SPA_COUNT("PersonnelTableModelColumn.SPA_COUNT.text"),
    IMPLANT_COUNT("PersonnelTableModelColumn.IMPLANT_COUNT.text"),
    LOYALTY("PersonnelTableModelColumn.LOYALTY.text"),
    HIGHEST_EDUCATION("PersonnelTableModelColumn.HIGHEST_EDUCATION.text"),
    CURRENT_EDUCATION("PersonnelTableModelColumn.CURRENT_EDUCATION.text"),
    ACADEMY("PersonnelTableModelColumn.ACADEMY.text"),
    COURSE("PersonnelTableModelColumn.COURSE.text"),
    ACADEMY_DURATION("PersonnelTableModelColumn.ACADEMY_DURATION.text"),
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
    CHARISMA("PersonnelTableModelColumn.CHARISMA.text"),
    EDGE("PersonnelTableModelColumn.EDGE.text"),
    SHIP_TRANSPORT("PersonnelTableModelColumn.SHIP_TRANSPORT.text"),
    TACTICAL_TRANSPORT("PersonnelTableModelColumn.TACTICAL_TRANSPORT.text"),
    LOCATION_SYSTEM("PersonnelTableModelColumn.LOCATION_SYSTEM.text"),
    LOCATION_PLANET("PersonnelTableModelColumn.LOCATION_PLANET.text"),
    LOCATION_NAME("PersonnelTableModelColumn.LOCATION_NAME.text"),
    DESTINATION_SYSTEM("PersonnelTableModelColumn.DESTINATION_SYSTEM.text"),
    DESTINATION_PLANET("PersonnelTableModelColumn.DESTINATION_PLANET.text"),
    DESTINATION_NAME("PersonnelTableModelColumn.DESTINATION_NAME.text");

    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
          MekHQ.getMHQOptions().getLocale());
    private static final MMLogger LOGGER = MMLogger.create(PersonnelTableModelColumn.class);
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

    public boolean isProtoMek() {
        return this == PROTOMEK;
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

    public boolean isNavigation() {
        return this == NAVIGATION;
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

    public boolean isScouting() {
        return this == SCOUTING;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isAsTech() {
        return this == ASTECH;
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

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isZeroG() {
        return this == ZERO_G;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isMedTech() {
        return this == MEDTECH;
    }

    public boolean isMedical() {
        return this == MEDICAL;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isTechMinutes() {
        return this == TECH_MINUTES;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isWorkMinutes() {
        return this == WORK_MINUTES;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isMedicalCapacity() {
        return this == MEDICAL_CAPACITY;
    }

    public boolean isAppraisal() {
        return this == APPRAISAL;
    }

    public boolean isTraining() {
        return this == TRAINING;
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

    public boolean isExtraIncome() {
        return this == EXTRA_INCOME;
    }

    public boolean isReputation() {
        return this == REPUTATION;
    }

    public boolean isUnlucky() {
        return this == UNLUCKY;
    }

    public boolean isBloodmark() {
        return this == BLOODMARK;
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

    public boolean isShipTransport() {return this == SHIP_TRANSPORT;}

    public boolean isTacticalTransport() {return this == TACTICAL_TRANSPORT;}

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isATOWAttribute() {
        return isStrength() ||
                     isBody() ||
                     isReflexes() ||
                     isDexterity() ||
                     isATOWIntelligence() ||
                     isWillpower() ||
                     isCharisma() ||
                     isEdge();
    }


    public boolean isPersonality() {
        return isAggression() || isAmbition() || isGreed() || isSocial() || isReasoning();
    }
    // endregion Boolean Comparison Methods

    public String getCellValue(final Campaign campaign, final PersonnelMarket personnelMarket, final Person person,
          final boolean loadAssignmentFromMarket, final boolean groupByUnit) {
        return getDisplayString(campaign, personnelMarket, person, loadAssignmentFromMarket, groupByUnit);
    }

    private String getDisplayString(Campaign campaign, PersonnelMarket personnelMarket, Person person,
          boolean loadAssignmentFromMarket, boolean groupByUnit) {

        final boolean isClanCampaign = campaign.isClanCampaign();
        final LocalDate today = campaign.getLocalDate();
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final boolean isUseAgeEffects = campaignOptions.isUseAgeEffects();
        final int adjustedReputation = person.getAdjustedReputation(isUseAgeEffects, isClanCampaign, today,
              person.getRankNumeric());

        final boolean isUseTechAdmin = campaignOptions.isTechsUseAdministration();
        final int baseBedCapacity = campaignOptions.getMaximumPatients();
        final boolean isUseMedicalAdmin = campaignOptions.isDoctorsUseAdministration();

        final Academy currentAcademy = EducationController.getAcademy(person.getEduAcademySet(),
              person.getEduAcademyNameInSet());

        final SkillModifierData skillModifierData = person.getSkillModifierData(isUseAgeEffects,
              isClanCampaign,
              today,
              true);

        final Function<String, String> skillValue = getSkillValue(person, skillModifierData);
        final BiFunction<String, String, String> gunneryPilotingValue = getGunneryPilotingValue(skillValue);

        return switch (this) {
            case ACADEMY -> currentAcademy == null ? "" : currentAcademy.getName();
            case ACADEMY_DURATION -> currentAcademy == null ? "" : String.valueOf(person.getEduEducationTime());
            case ADMINISTRATION -> skillValue.apply(SkillType.S_ADMIN);
            case AGE, BIRTHDAY -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getDateOfBirth());
            case AGGRESSION -> {
                Aggression trait = person.getAggression();
                String sign = trait.isTraitPositive() ? "+" : "-";
                yield trait + " (" + (trait.isTraitMajor() ? sign + sign : sign) + ')';
            }
            case AMBITION -> {
                Ambition trait = person.getAmbition();
                String sign = trait.isTraitPositive() ? "+" : "-";
                yield trait + " (" + (trait.isTraitMajor() ? sign + sign : sign) + ')';
            }
            case ANTI_MEK -> skillValue.apply(SkillType.S_ANTI_MEK);
            case APPRAISAL -> skillValue.apply(SkillType.S_APPRAISAL);
            case AEROSPACE -> gunneryPilotingValue.apply(SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO);
            case ARTILLERY -> skillValue.apply(SkillType.S_ARTILLERY);
            case ASTECH -> skillValue.apply(SkillType.S_ASTECH);
            case BATTLE_ARMOUR -> skillValue.apply(SkillType.S_GUN_BA);
            case BLOODMARK -> Integer.toString(person.getBloodmark());
            case BLOODNAME -> person.getBloodname();
            case BODY -> getAttributeScoreDisplay(person, SkillAttribute.BODY);
            case CALLSIGN -> person.getCallsign();
            case CHARISMA -> getAttributeScoreDisplay(person, SkillAttribute.CHARISMA);
            case CLAN_PERSONNEL -> resources.getString(person.isClanPersonnel() ? "Yes.text" : "No.text");
            case COMMANDER -> resources.getString(person.isCommander() ? "Yes.text" : "No.text");
            case CONNECTIONS -> person.getBurnedConnectionsEndDate() != null
                                      ?
                                      "<html><b><font color='gray'>" +
                                            person.getAdjustedConnections(true) +
                                      "</font></b></html>"
                                      :
                                      Integer.toString(person.getAdjustedConnections(true));
            case CONVENTIONAL_AIRCRAFT -> gunneryPilotingValue.apply(SkillType.S_GUN_JET, SkillType.S_PILOT_JET);
            case COURSE ->
                  currentAcademy == null ? "" : currentAcademy.getQualifications().get(person.getEduCourseIndex());
            case CURRENT_EDUCATION -> currentAcademy == null ? "" :
                                            EducationLevel.fromString(String.valueOf(currentAcademy.getEducationLevel(
                                                  person))).toString();
            case DEATH_DATE -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getDateOfDeath());
            case DEPLOYED -> {
                Unit unit = person.getUnit();
                if (unit == null || !unit.isDeployed()) {
                    yield "-";
                }
                Scenario scenario = campaign.getScenario(unit.getScenarioId());
                if (scenario == null) {
                    LOGGER.warn("Unable to retrieve scenario for unit {} (Removing scenario assignment).",
                          unit.getName());
                    unit.setScenarioId(Scenario.S_DEFAULT_ID);
                    yield "-";
                }
                yield scenario.getName();
            }
            case DESTINATION_NAME -> {
                LocationNode node = person.getLocationNode();
                AbstractLocation loc = node != null ? node.getNearestAbstractLocation() : null;
                if (loc instanceof CurrentLocation cl
                          && cl.getJumpPath() != null && cl.getJumpPath().size() > 0) {
                    var destination = cl.getJumpPath().getLastSystem();
                    LocationNode clNode = cl.getLocationNode();
                    if (clNode != null) {
                        LocationNode parent = clNode.getParent();
                        while (parent != null) {
                            if (parent.getLocatable() instanceof AbstractBase base) {
                                yield base.getDisplayName();
                            }
                            if (parent.getLocatable() instanceof AcademyCampusLocation campus) {
                                LocationNode fixedLocNode = parent.getParent();
                                if (fixedLocNode != null
                                          && fixedLocNode.getLocatable() instanceof AbstractLocation campusLoc
                                          && campusLoc.getCurrentSystem() == destination) {
                                    yield campus.getAcademyName();
                                }
                                yield campaign.getName();
                            }
                            parent = parent.getParent();
                        }
                    }
                }
                yield "-";
            }
            case DESTINATION_PLANET -> {
                LocationNode node = person.getLocationNode();
                AbstractLocation loc = node != null ? node.getNearestAbstractLocation() : null;
                if (loc instanceof CurrentLocation cl
                          && cl.getJumpPath() != null && cl.getJumpPath().size() > 0) {
                    var dest = cl.getJumpPath().getLastSystem();
                    if (dest != null) {
                        Planet planet = dest.getPrimaryPlanet();
                        yield planet != null ? planet.getPrintableName(today) : "-";
                    }
                }
                yield "-";
            }
            case DESTINATION_SYSTEM -> {
                LocationNode node = person.getLocationNode();
                AbstractLocation loc = node != null ? node.getNearestAbstractLocation() : null;
                if (loc instanceof CurrentLocation cl
                          && cl.getJumpPath() != null && cl.getJumpPath().size() > 0) {
                    var dest = cl.getJumpPath().getLastSystem();
                    yield dest != null ? dest.getPrintableName(today) : "-";
                }
                yield "-";
            }
            case DEXTERITY -> getAttributeScoreDisplay(person, SkillAttribute.DEXTERITY);
            case DIVORCEABLE -> resources.getString(person.getGenealogy().hasSpouse()
                                                          ?
                                                          (person.isDivorceable() ? "Yes.text" : "No.text") :
                                                          "NA.text");
            case DUE_DATE -> person.getDueDateAsString(campaign);
            case EDGE -> {
                int currentAttributeValue = person.getAttributeScore(SkillAttribute.EDGE);
                int attributeCap = person.getAttributeCap(SkillAttribute.EDGE);
                yield currentAttributeValue + " / " + attributeCap;
            }
            case EMPLOYED -> resources.getString(person.isEmployed() ? "Yes.text" : "No.text");
            case EXTRA_INCOME -> Integer.toString(person.getExtraIncomeTraitLevel());
            case FATIGUE -> Integer.toString(getEffectiveFatigue(person.getAdjustedFatigue(),
                  person.getPermanentFatigue(), person.isClanPersonnel(),
                  person.getSkillLevel(campaign, false, true)));
            case FIRST_NAME -> person.getFirstName();
            case FORCE -> {
                final Formation formation = campaign.getFormationFor(person);
                yield (formation == null) ? "-" : formation.getName();
            }
            case FOUNDER -> resources.getString(person.isFounder() ? "Yes.text" : "No.text");
            case GENDER -> GenderDescriptors.MALE_FEMALE_OTHER.getDescriptorCapitalized(person.getGender());
            case GIVEN_NAME -> person.getGivenName();
            case GREED -> {
                Greed trait = person.getGreed();
                String sign = trait.isTraitPositive() ? "+" : "-";
                yield trait + " (" + (trait.isTraitMajor() ? sign + sign : sign) + ')';
            }
            case GROUND_VEHICLE -> gunneryPilotingValue.apply(SkillType.S_GUN_VEE, SkillType.S_PILOT_GVEE);
            case HIDE_PERSONALITY -> resources.getString(person.isHidePersonality() ? "Yes.text" : "No.text");
            case HIGHEST_EDUCATION -> person.getEduHighestEducation().toString();
            case IMMORTAL -> resources.getString(person.getStatus().isDead() ? "NA.text"
                                                       : (person.isImmortal() ? "Yes.text" : "No.text"));
            case IMPLANT_COUNT -> Integer.toString(person.countOptions(PersonnelOptions.MD_ADVANTAGES));
            case INJURIES -> campaign.getCampaignOptions().isUseAdvancedMedical()
                                   ? Integer.toString(person.getInjuries().size())
                                   : Integer.toString(person.getHits());
            case INTELLIGENCE -> getAttributeScoreDisplay(person, SkillAttribute.INTELLIGENCE);
            case KILLS -> Integer.toString(campaign.getKillsFor(person.getId()).size());
            case LAST_NAME -> person.getLastName();
            case LAST_RANK_CHANGE_DATE -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getLastRankChangeDate());
            case LEADERSHIP -> skillValue.apply(SkillType.S_LEADER);
            case LOCATION_NAME -> {
                LocationNode node = person.getLocationNode();
                AbstractLocation loc = node != null ? node.getNearestAbstractLocation() : null;
                boolean isTraveling = loc instanceof CurrentLocation cl
                                            && cl.getJumpPath() != null && cl.getJumpPath().size() > 0;
                if (node != null) {
                    LocationNode parent = node.getParent();
                    while (parent != null) {
                        if (parent.getLocatable() == campaign.getMainForcePersonnel()) {
                            yield campaign.getName();
                        }
                        if (!isTraveling && parent.getLocatable() instanceof AbstractBase base) {
                            yield base.getDisplayName();
                        }
                        if (!isTraveling && parent.getLocatable() instanceof AcademyCampusLocation campus) {
                            yield campus.getAcademyName();
                        }
                        parent = parent.getParent();
                    }
                }
                if (isTraveling) {
                    CurrentLocation currentLoc = (CurrentLocation) loc;
                    JumpPath path = currentLoc.getJumpPath();
                    PlanetarySystem sys = currentLoc.getCurrentSystem();
                    if (path.size() > 1 && currentLoc.isAtJumpPoint()) {
                        double neededHours = sys.getRechargeTime(today, currentLoc.computeIsUseCommandCircuit(campaign));
                        double remainingHours = neededHours - currentLoc.getRechargeTime();
                        if (remainingHours > 0) {
                            int days = (int) Math.ceil(remainingHours / 24.0);
                            yield String.format(
                                  resources.getString(
                                        "PersonnelTableModelColumn.LOCATION_NAME.inTransit.recharging.text"),
                                  days);
                        }
                        yield resources.getString("PersonnelTableModelColumn.LOCATION_NAME.inTransit.readyToJump.text");
                    } else if (path.size() == 1) {
                        int days = (int) Math.ceil(currentLoc.getTransitTime());
                        yield String.format(
                              resources.getString("PersonnelTableModelColumn.LOCATION_NAME.inTransit.toPlanet.text"),
                              days);
                    } else {
                        double daysToJP = sys.getTimeToJumpPoint(1.0) - currentLoc.getTransitTime();
                        int days = (int) Math.ceil(daysToJP);
                        yield String.format(
                              resources.getString("PersonnelTableModelColumn.LOCATION_NAME.inTransit.toJumpPoint.text"),
                              days);
                    }
                }
                yield "-";
            }
            case LOCATION_PLANET -> {
                LocationNode node = person.getLocationNode();
                AbstractLocation loc = node != null ? node.getNearestAbstractLocation() : null;
                if (loc != null) {
                    Planet planet = loc.getPlanet();
                    yield planet != null ? planet.getPrintableName(today) : "-";
                }
                yield "-";
            }
            case LOCATION_SYSTEM -> {
                LocationNode node = person.getLocationNode();
                AbstractLocation loc = node != null ? node.getNearestAbstractLocation() : null;
                if (loc != null) {
                    var system = loc.getCurrentSystem();
                    yield system != null ? system.getPrintableName(today) : "-";
                }
                yield "-";
            }
            case LOYALTY -> String.valueOf(person.getAdjustedLoyalty(campaign.getFaction(),
                  campaignOptions.isUseAlternativeAdvancedMedical()));
            case MARRIAGEABLE -> resources.getString(person.getGenealogy().hasSpouse() ? "NA.text"
                                                           : (person.isMarriageable() ? "Yes.text" : "No.text"));
            case MEDICAL -> skillValue.apply(SkillType.S_SURGERY);
            case MEDICAL_CAPACITY -> person.isDoctor()
                                           ?
                                           String.valueOf(person.getDoctorMedicalCapacity(isUseMedicalAdmin,
                                                 baseBedCapacity))
                                           :
                                           "0";
            case MEDTECH -> skillValue.apply(SkillType.S_MEDTECH);
            case MEK -> gunneryPilotingValue.apply(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK);
            case NAVAL_VEHICLE -> gunneryPilotingValue.apply(SkillType.S_GUN_VEE, SkillType.S_PILOT_NVEE);
            case NAVIGATION -> skillValue.apply(SkillType.S_NAVIGATION);
            case NEGOTIATION -> skillValue.apply(SkillType.S_NEGOTIATION);
            case NEVER_ASSIGN_AUTO_MAINTENANCE ->
                  resources.getString(person.isNeverAssignMaintenanceAutomatically() ? "Yes.text" : "No.text");
            case ORIGIN_FACTION -> person.getOriginFaction().getFullName(campaign.getGameYear());
            case ORIGIN_PLANET -> {
                final Planet originPlanet = person.getOriginPlanet();
                yield (originPlanet == null) ? "" : originPlanet.getName(campaign.getLocalDate());
            }
            case PERSON -> "";
            case PERSONNEL_ROLE -> person.getFormatedRoleDescriptions(today);
            case PERSONNEL_STATUS -> person.getStatus().toString();
            case POST_NOMINAL -> person.getPostNominal();
            case PRE_NOMINAL -> person.getPreNominal();
            case PREFERS_MEN -> resources.getString(person.isPrefersMen() ? "Yes.text" : "No.text");
            case PREFERS_WOMEN -> resources.getString(person.isPrefersWomen() ? "Yes.text" : "No.text");
            case PROTOMEK -> skillValue.apply(SkillType.S_GUN_PROTO);
            case QUICK_TRAIN_IGNORE -> resources.getString(person.isQuickTrainIgnore() ? "Yes.text" : "No.text");
            case RANK -> person.makeHTMLRank();
            case REASONING -> person.getReasoning().getLabel();
            case RECRUITMENT_DATE -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getRecruitment());
            case REFLEXES -> getAttributeScoreDisplay(person, SkillAttribute.REFLEXES);
            case REPUTATION -> Integer.toString(adjustedReputation);
            case RETIREMENT_DATE -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getRetirement());
            case SALARY -> person.getSalary(campaign).toAmountAndSymbolString();
            case SALVAGE_SUPERVISOR -> resources.getString(person.isSalvageSupervisor() ? "Yes.text" : "No.text");
            case SCOUTING -> {
                String sName = ScoutingSkills.getBestScoutingSkill(person);
                yield sName == null ?
                            "-" :
                            Integer.toString(person.getSkill(sName).getFinalSkillValue(skillModifierData));
            }
            case SECOND_IN_COMMAND -> resources.getString(person.isSecondInCommand() ? "Yes.text" : "No.text");
            case SHIP_TRANSPORT -> person.getUnit() != null && person.getUnit().getTransportShipAssignment() != null
                                         ? person.getUnit().getTransportShipAssignment().getTransportShip().getName()
                                         : "-";
            case SKILL_LEVEL -> "<html>" + SkillType.getColoredExperienceLevelName(
                  person.getExperienceLevel(campaign, false, true)) + "</html>";
            case SMALL_ARMS -> {
                String sName = InfantryGunnerySkills.getBestInfantryGunnerySkill(person,
                      campaignOptions.isUseSmallArmsOnly());
                yield sName == null ?
                            "-" :
                            Integer.toString(person.getSkill(sName).getFinalSkillValue(skillModifierData));
            }
            case SOCIAL -> {
                Social trait = person.getSocial();
                String sign = trait.isTraitPositive() ? "+" : "-";
                yield trait + " (" + (trait.isTraitMajor() ? sign + sign : sign) + ')';
            }
            case SPA_COUNT -> Integer.toString(person.countOptions(PersonnelOptions.LVL3_ADVANTAGES));
            case STRATEGY -> skillValue.apply(SkillType.S_STRATEGY);
            case STRENGTH -> getAttributeScoreDisplay(person, SkillAttribute.STRENGTH);
            case SURNAME -> {
                final String surname = person.getSurname();
                if (StringUtility.isNullOrBlank(surname)) {
                    yield "";
                } else if (!groupByUnit) {
                    yield surname;
                } else {
                    final Unit unit = person.getUnit();
                    if (unit == null) {
                        yield surname;
                    }
                    final int crewSize = unit.getCrew().size() - 1;
                    if (crewSize <= 0) {
                        yield surname;
                    }
                    yield surname + " (+" + crewSize +
                                resources.getString(unit.usesSoldiers()
                                                          ? "PersonnelTableModelColumn.SURNAME.Soldiers.text"
                                                          : "PersonnelTableModelColumn.SURNAME.Crew.text");
                }
            }
            case TACTICAL_TRANSPORT ->
                  person.getUnit() != null && person.getUnit().getTacticalTransportAssignment() != null
                        ? person.getUnit().getTacticalTransportAssignment().getTransport().getName()
                        : "-";
            case TACTICS -> skillValue.apply(SkillType.S_TACTICS);
            case TECH_AERO -> skillValue.apply(SkillType.S_TECH_AERO);
            case TECH_BA -> skillValue.apply(SkillType.S_TECH_BA);
            case TECH_MEK -> skillValue.apply(SkillType.S_TECH_MEK);
            case TECH_MECHANIC -> skillValue.apply(SkillType.S_TECH_MECHANIC);
            case TECH_MINUTES ->
                  person.isTechExpanded() ? String.valueOf(person.getDailyAvailableTechTime(isUseTechAdmin)) : "0";
            case TECH_VESSEL -> skillValue.apply(SkillType.S_TECH_VESSEL);
            case TOUGHNESS -> Integer.toString(person.getAdjustedToughness());
            case TRAINING -> skillValue.apply(SkillType.S_TRAINING);
            case TRYING_TO_CONCEIVE -> resources.getString(person.getGender().isFemale()
                                                                 ?
                                                                 (person.isTryingToConceive() ?
                                                                  "Yes.text" :
                                                                  "No.text") :
                                                                 "NA.text");
            case UNDER_PROTECTION -> resources.getString(person.isUnderProtection() ? "Yes.text" : "No.text");
            case UNIT_ASSIGNMENT -> {
                if (loadAssignmentFromMarket) {
                    final Entity entity = personnelMarket.getAttachedEntity(person);
                    yield (entity == null) ? "-" : entity.getDisplayName();
                } else {
                    Unit unit = person.getUnit();
                    if (unit != null) {
                        String name = unit.getName();
                        Entity entity = unit.getEntity();
                        String role = null;

                        if (entity instanceof SmallCraft || entity instanceof Jumpship || entity instanceof Tank) {
                            if (unit.isNavigator(person)) {
                                role = "Navigator";
                            } else if (unit.isDriver(person)) {
                                role = (entity instanceof Tank) ? "Driver" : "Pilot";
                            } else if (unit.isGunner(person)) {
                                role = "Gunner";
                            } else if (unit.isCommander(person)
                                             || ((entity instanceof Tank) && unit.isTechOfficer(person))) {
                                role = "Commander";
                            } else {
                                role = "Crew";
                            }
                        } else if (entity instanceof Mek && unit.getFullCrewSize() > 1) {
                            if (unit.isDriver(person)) {
                                role = "Pilot";
                            } else if (unit.isGunner(person)) {
                                role = "Gunner";
                            } else if (unit.isTechOfficer(person)) {
                                role = "Tech Officer";
                            } else if (unit.isCommander(person)) {
                                role = "Commander";
                            }
                        }

                        if (role != null) {
                            name += " [" + role + "]";
                        }

                        yield name;
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
                            unit = person.getTechUnits().getFirst();
                            if (unit != null) {
                                yield "<html>" +
                                            ReportingUtilities.separateIf(refitString,
                                                  ", ",
                                                  unit.getName() + " (" + person.getMaintenanceTimeUsing() + "m)") +
                                            "</html>";
                            }
                        } else {
                            yield "<html>" +
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
                yield "-";
            }
            case UNLUCKY -> Integer.toString(person.getUnlucky());
            case VESSEL -> gunneryPilotingValue.apply(SkillType.S_GUN_SPACE, SkillType.S_PILOT_SPACE);
            case VTOL -> gunneryPilotingValue.apply(SkillType.S_GUN_VEE, SkillType.S_PILOT_VTOL);
            case WEALTH -> Integer.toString(person.getWealth());
            case WILLPOWER -> getAttributeScoreDisplay(person, SkillAttribute.WILLPOWER);
            case WORK_MINUTES -> person.isTechExpanded() ? String.valueOf(person.getMinutesLeft()) : "0";
            case XP -> Integer.toString(person.getXP());
            case ZERO_G -> skillValue.apply(SkillType.S_ZERO_G_OPERATIONS);
        };
    }

    private static @NonNull BiFunction<String, String, String> getGunneryPilotingValue(
          Function<String, String> skillValue) {
        return (gunSkill, pilotSkill) -> skillValue.apply(gunSkill) + '/' + skillValue.apply(pilotSkill);
    }

    private static @NonNull Function<String, String> getSkillValue(Person person,
          SkillModifierData skillModifierData) {
        return skillName -> person.hasSkill(skillName) ?
                                  Integer.toString(person.getSkill(skillName).getFinalSkillValue(skillModifierData)) :
                                  "-";
    }

    /**
     * Constructs a displayable string representation of a person's skill attribute, including their current score,
     * maximum possible score (cap), and attribute modifier.
     *
     * @param person    the person whose attribute scores are being represented
     * @param attribute the specific skill attribute being evaluated
     *
     * @return a string in the format "currentScore / attributeCap (+/- modifier)"
     *
     * @author Illiani
     * @since 0.51.00
     */
    private static @NonNull String getAttributeScoreDisplay(Person person, SkillAttribute attribute) {
        int currentAttributeValue = person.getAttributeScore(attribute);
        int attributeCap = person.getAttributeCap(attribute);

        int attributeModifier = person.getAttributeModifier(attribute);
        String sign = attributeModifier >= 0 ? "+" : "";

        return currentAttributeValue + " / " + attributeCap + " (" + sign + attributeModifier + ")";
    }

    public @Nullable String getDisplayText(final Campaign campaign, final Person person) {
        if (this == PersonnelTableModelColumn.AGE) {
            return Integer.toString(person.getAge(campaign.getLocalDate()));
        }
        return null;
    }

    public @Nullable String getToolTipText(final Person person, final boolean loadAssignmentFromMarket) {
        return getToolTipText(person, loadAssignmentFromMarket, null);
    }

    /**
     * Returns the tooltip text for this column, optionally including color reason explanations.
     *
     * @param person                   the person for this row
     * @param loadAssignmentFromMarket whether to load assignment from market
     * @param colorReasonKeys          list of i18n keys for color reasons, or null/empty if no special coloring
     *
     * @return the tooltip text, or null if no tooltip
     */
    public @Nullable String getToolTipText(final Person person, final boolean loadAssignmentFromMarket,
          final @Nullable java.util.List<String> colorReasonKeys) {
        String baseTooltip = getBaseToolTipText(person, loadAssignmentFromMarket);

        // For name, rank, and status columns, append color reasons if present
        if (colorReasonKeys != null && !colorReasonKeys.isEmpty() && isNameRankOrStatusColumn()) {
            StringBuilder colorReasons = new StringBuilder();
            for (String key : colorReasonKeys) {
                if (!colorReasons.isEmpty()) {
                    colorReasons.append("<br>");
                }
                colorReasons.append(resources.getString(key));
            }

            if (baseTooltip != null) {
                // Combine existing tooltip with color reasons
                return "<html>" +
                             ReportingUtilities.stripHtmlTags(baseTooltip) +
                             "<br><i>" +
                             colorReasons +
                             "</i></html>";
            } else {
                return "<html><i>" + colorReasons + "</i></html>";
            }
        }

        return baseTooltip;
    }

    private @Nullable String getBaseToolTipText(final Person person, final boolean loadAssignmentFromMarket) {
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

    /**
     * Returns true if this column should display color reason tooltips. Applies to name columns, rank column, and
     * status column.
     */
    private boolean isNameRankOrStatusColumn() {
        return this == PERSON || this == FIRST_NAME || this == LAST_NAME ||
                     this == GIVEN_NAME || this == SURNAME || this == BLOODNAME ||
                     this == RANK || this == PERSONNEL_STATUS;
    }

    public int getWidth() {
        return switch (this) {
            case PERSON, UNIT_ASSIGNMENT -> 125;
            case RANK, FIRST_NAME, GIVEN_NAME, DEPLOYED -> 70;
            case LAST_NAME, SURNAME, BLOODNAME, CALLSIGN, SKILL_LEVEL, SALARY -> 50;
            case PERSONNEL_ROLE -> 150;
            case FORCE -> 100;
            case LOCATION_SYSTEM, LOCATION_PLANET, DESTINATION_SYSTEM, DESTINATION_PLANET -> 100;
            case LOCATION_NAME, DESTINATION_NAME -> 150;
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
                 DEPLOYED,
                 LOCATION_SYSTEM,
                 LOCATION_PLANET,
                 LOCATION_NAME,
                 DESTINATION_SYSTEM,
                 DESTINATION_PLANET,
                 DESTINATION_NAME -> SwingConstants.LEFT;
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
            case GUNNERY_PILOT_SKILLS -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     PERSONNEL_ROLE,
                     MEK,
                     GROUND_VEHICLE,
                     NAVAL_VEHICLE,
                     VTOL -> true;
                default -> false;
            };
            case GUNNERY_PILOT_SKILLS_II -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     PERSONNEL_ROLE,
                     AEROSPACE,
                     CONVENTIONAL_AIRCRAFT,
                     VESSEL,
                     ARTILLERY -> true;
                default -> false;
            };
            case INFANTRY_SKILLS -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, PROTOMEK, BATTLE_ARMOUR, SMALL_ARMS, ANTI_MEK -> true;
                default -> false;
            };
            case TACTICAL_SKILLS -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, TACTICS, STRATEGY, LEADERSHIP, NAVIGATION, SCOUTING ->
                      true;
                default -> false;
            };
            case TECHNICAL_SKILLS -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     PERSONNEL_ROLE,
                     ASTECH,
                     TECH_MEK,
                     TECH_AERO,
                     TECH_MECHANIC,
                     TECH_BA,
                     TECH_VESSEL,
                     ZERO_G,
                     WORK_MINUTES,
                     TECH_MINUTES -> true;
                default -> false;
            };
            case MEDICAL_SKILLS -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     PERSONNEL_ROLE,
                     MEDTECH,
                     MEDICAL,
                     MEDICAL_CAPACITY -> true;
                default -> false;
            };
            case ADMINISTRATIVE_SKILLS -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, ADMINISTRATION, NEGOTIATION, TRAINING, APPRAISAL ->
                      true;
                default -> false;
            };
            case TRANSPORT -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     SKILL_LEVEL,
                     PERSONNEL_ROLE,
                     UNIT_ASSIGNMENT,
                     SHIP_TRANSPORT,
                     TACTICAL_TRANSPORT -> true;
                default -> false;
            };
            case BIOGRAPHICAL -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME, AGE, PERSONNEL_STATUS, PERSONNEL_ROLE, HIGHEST_EDUCATION -> true;
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
            case FLAGS_A -> switch (this) {
                // Max 7 flags
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
            case FLAGS_B -> switch (this) {
                // Max 7 flags
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     MARRIAGEABLE,
                     NEVER_ASSIGN_AUTO_MAINTENANCE,
                     PREFERS_MEN,
                     PREFERS_WOMEN,
                     QUICK_TRAIN_IGNORE,
                     SALVAGE_SUPERVISOR,
                     SECOND_IN_COMMAND -> true;
                default -> false;
            };
            case FLAGS_C -> switch (this) {
                // Max 7 flags
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     TRYING_TO_CONCEIVE,
                     UNDER_PROTECTION -> true;
                default -> false;
            };
            case PERSONALITY -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME -> true;
                case AGGRESSION, AMBITION, GREED, SOCIAL, REASONING ->
                      campaign.getCampaignOptions().isUseRandomPersonalities();
                default -> false;
            };
            case TRAITS -> switch (this) {
                case RANK, FIRST_NAME, LAST_NAME, CONNECTIONS, WEALTH, EXTRA_INCOME, REPUTATION, UNLUCKY, BLOODMARK ->
                      true;
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
                case EDGE -> campaign.getCampaignOptions().isUseEdge();
                default -> false;
            };
            case EDUCATION -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     HIGHEST_EDUCATION,
                     CURRENT_EDUCATION,
                     ACADEMY,
                     COURSE,
                     ACADEMY_DURATION -> true;
                default -> false;
            };
            case LOCATION -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     PERSONNEL_ROLE,
                     LOCATION_SYSTEM,
                     LOCATION_PLANET,
                     LOCATION_NAME,
                     DESTINATION_SYSTEM,
                     DESTINATION_PLANET,
                     DESTINATION_NAME -> true;
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
            case HIGHEST_EDUCATION, CURRENT_EDUCATION -> new EducationLevelSorter();
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
                 PROTOMEK,
                 BATTLE_ARMOUR,
                 SMALL_ARMS,
                 ANTI_MEK,
                 ARTILLERY,
                 NAVIGATION,
                 TACTICS,
                 STRATEGY,
                 LEADERSHIP,
                 SCOUTING,
                 ASTECH,
                 TECH_MEK,
                 TECH_AERO,
                 TECH_MECHANIC,
                 TECH_BA,
                 TECH_VESSEL,
                 ZERO_G,
                 MEDTECH,
                 MEDICAL,
                 APPRAISAL,
                 TRAINING,
                 ADMINISTRATION,
                 NEGOTIATION -> new BonusSorter();
            case INJURIES,
                 KILLS,
                 XP,
                 TOUGHNESS,
                 CONNECTIONS,
                 WEALTH,
                 EXTRA_INCOME,
                 REPUTATION,
                 UNLUCKY,
                 BLOODMARK,
                 SPA_COUNT,
                 IMPLANT_COUNT,
                 LOYALTY -> new IntegerStringSorter();
            case STRENGTH, BODY, REFLEXES, DEXTERITY, INTELLIGENCE, WILLPOWER, CHARISMA, EDGE ->
                  new AttributeScoreSorter();
            case REASONING -> new ReasoningSorter();
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
