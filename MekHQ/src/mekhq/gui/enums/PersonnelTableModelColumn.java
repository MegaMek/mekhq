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
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.Comparator;
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
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.personnel.education.EducationController;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.enums.PersonnelRole;
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
import mekhq.gui.model.LocationDisplay;
import mekhq.gui.sorter.AttributeScoreSorter;
import mekhq.gui.sorter.BonusSorter;
import mekhq.gui.sorter.DateStringComparator;
import mekhq.gui.sorter.EducationLevelSorter;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.IntegerStringSorter;
import mekhq.gui.sorter.LevelSorter;
import mekhq.gui.sorter.PersonRankStringSorter;
import mekhq.gui.sorter.ReasoningSorter;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.ReportingUtilities;
import org.jspecify.annotations.NonNull;

public enum PersonnelTableModelColumn {
    // region Enum Declarations
    PERSON("Column.PERSON.title"),
    RANK("Column.RANK.title"),
    FIRST_NAME("Column.FIRST_NAME.title"),
    LAST_NAME("Column.LAST_NAME.title"),
    PRE_NOMINAL("Column.PRE_NOMINAL.title"),
    GIVEN_NAME("Column.GIVEN_NAME.title"),
    SURNAME("Column.SURNAME.title"),
    BLOODNAME("Column.BLOODNAME.title"),
    POST_NOMINAL("Column.POST_NOMINAL.title"),
    CALLSIGN("Column.CALLSIGN.title"),
    AGE("Column.AGE.title"),
    PERSONNEL_STATUS("Column.PERSONNEL_STATUS.title"),
    GENDER("Column.GENDER.title"),
    SKILL_LEVEL("Column.SKILL_LEVEL.title"),
    PERSONNEL_ROLE("Column.PERSONNEL_ROLE.title"),
    UNIT_ASSIGNMENT("Column.UNIT_ASSIGNMENT.title"),
    FORCE("Column.FORCE.title"),
    DEPLOYED("Column.DEPLOYED.title"),
    MEK("Column.MEK.title"),
    GROUND_VEHICLE("Column.GROUND_VEHICLE.title"),
    NAVAL_VEHICLE("Column.NAVAL_VEHICLE.title"),
    VTOL("Column.VTOL.title"),
    AEROSPACE("Column.AEROSPACE.title"),
    CONVENTIONAL_AIRCRAFT("Column.CONVENTIONAL_AIRCRAFT.title"),
    VESSEL("Column.VESSEL.title"),
    PROTOMEK("Column.PROTOMEK.title"),
    BATTLE_ARMOUR("Column.BATTLE_ARMOUR.title"),
    AGGREGATE_COMBAT("Column.AGGREGATE_COMBAT.title"),
    SMALL_ARMS("Column.SMALL_ARMS.title"),
    ANTI_MEK("Column.ANTI_MEK.title"),
    ARTILLERY("Column.ARTILLERY.title"),
    NAVIGATION("Column.NAVIGATION.title"),
    TACTICS("Column.TACTICS.title"),
    STRATEGY("Column.STRATEGY.title"),
    LEADERSHIP("Column.LEADERSHIP.title"),
    SCOUTING("Column.SCOUTING.title"),
    ASTECH("Column.ASTECH.title"),
    TECH_MEK("Column.TECH_MEK.title"),
    TECH_AERO("Column.TECH_AERO.title"),
    TECH_MECHANIC("Column.TECH_MECHANIC.title"),
    TECH_BA("Column.TECH_BA.title"),
    TECH_VESSEL("Column.TECH_VESSEL.title"),
    ZERO_G("Column.ZERO_G.title"),
    MEDTECH("Column.MEDTECH.title"),
    MEDICAL("Column.MEDICAL.title"),
    WORK_MINUTES("Column.WORK_MINUTES.title"),
    TECH_MINUTES("Column.TECH_MINUTES.title"),
    MEDICAL_CAPACITY("Column.MEDICAL_CAPACITY.title"),
    APPRAISAL("Column.APPRAISAL.title"),
    TRAINING("Column.TRAINING.title"),
    ADMINISTRATION("Column.ADMINISTRATION.title"),
    NEGOTIATION("Column.NEGOTIATION.title"),
    INJURIES("Column.INJURIES.title"),
    KILLS("Column.KILLS.title"),
    SALARY("Column.SALARY.title"),
    XP("Column.XP.title"),
    ORIGIN_FACTION("Column.ORIGIN_FACTION.title"),
    ORIGIN_PLANET("Column.ORIGIN_PLANET.title"),
    BIRTHDAY("Column.BIRTHDAY.title"),
    RECRUITMENT_DATE("Column.RECRUITMENT_DATE.title"),
    LAST_RANK_CHANGE_DATE("Column.LAST_RANK_CHANGE_DATE.title"),
    DUE_DATE("Column.DUE_DATE.title"),
    RETIREMENT_DATE("Column.RETIREMENT_DATE.title"),
    DEATH_DATE("Column.DEATH_DATE.title"),
    CLAN_PERSONNEL("Column.CLAN_PERSONNEL.title"),
    COMMANDER("Column.COMMANDER.title"),
    DIVORCEABLE("Column.DIVORCEABLE.title"),
    EMPLOYED("Column.EMPLOYED.title"),
    FOUNDER("Column.FOUNDER.title"),
    HIDE_PERSONALITY("Column.HIDE_PERSONALITY.title"),
    IMMORTAL("Column.IMMORTAL.title"),
    MARRIAGEABLE("Column.MARRIAGEABLE.title"),
    NEVER_ASSIGN_AUTO_MAINTENANCE("Column.NEVER_ASSIGN_AUTO_MAINTENANCE.title"),
    PREFERS_MEN("Column.PREFERS_MEN.title"),
    PREFERS_WOMEN("Column.PREFERS_WOMEN.title"),
    QUICK_TRAIN_IGNORE("Column.QUICK_TRAIN_IGNORE.title"),
    SALVAGE_SUPERVISOR("Column.SALVAGE_SUPERVISOR.title"),
    SECOND_IN_COMMAND("Column.SECOND_IN_COMMAND.title"),
    WANTS_CHILDREN("Column.WANTS_CHILDREN.title"),
    UNDER_PROTECTION("Column.UNDER_PROTECTION.title"),
    COVER_MEDICAL_EXPENSES("Column.COVER_MEDICAL_EXPENSES.title"),
    BLOCK_MATERNITY_LEAVE("Column.BLOCK_MATERNITY_LEAVE.title"),
    TOUGHNESS("Column.TOUGHNESS.title"),
    CONNECTIONS("Column.CONNECTIONS.title"),
    WEALTH("Column.WEALTH.title"),
    EXTRA_INCOME("Column.EXTRA_INCOME.title"),
    REPUTATION("Column.REPUTATION.title"),
    UNLUCKY("Column.UNLUCKY.title"),
    BLOODMARK("Column.BLOODMARK.title"),
    FATIGUE("Column.FATIGUE.title"),
    SPA_COUNT("Column.SPA_COUNT.title"),
    MODIFICATION_COUNT("Column.MODIFICATION_COUNT.title"),
    IMPLANT_COUNT("Column.IMPLANT_COUNT.title"),
    LOYALTY("Column.LOYALTY.title"),
    HIGHEST_EDUCATION("Column.HIGHEST_EDUCATION.title"),
    CURRENT_EDUCATION("Column.CURRENT_EDUCATION.title"),
    ACADEMY("Column.ACADEMY.title"),
    COURSE("Column.COURSE.title"),
    ACADEMY_DURATION("Column.ACADEMY_DURATION.title"),
    AGGRESSION("Column.AGGRESSION.title"),
    AMBITION("Column.AMBITION.title"),
    GREED("Column.GREED.title"),
    SOCIAL("Column.SOCIAL.title"),
    REASONING("Column.REASONING.title"),
    STRENGTH("Column.STRENGTH.title"),
    BODY("Column.BODY.title"),
    REFLEXES("Column.REFLEXES.title"),
    DEXTERITY("Column.DEXTERITY.title"),
    INTELLIGENCE("Column.INTELLIGENCE.title"),
    WILLPOWER("Column.WILLPOWER.title"),
    CHARISMA("Column.CHARISMA.title"),
    EDGE("Column.EDGE.title"),
    SHIP_TRANSPORT("Column.SHIP_TRANSPORT.title"),
    TACTICAL_TRANSPORT("Column.TACTICAL_TRANSPORT.title"),
    LOCATION_SYSTEM("Column.LOCATION_SYSTEM.title"),
    LOCATION_PLANET("Column.LOCATION_PLANET.title"),
    LOCATION_NAME("Column.LOCATION_NAME.title"),
    DESTINATION_SYSTEM("Column.DESTINATION_SYSTEM.title"),
    DESTINATION_PLANET("Column.DESTINATION_PLANET.title"),
    DESTINATION_NAME("Column.DESTINATION_NAME.title");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.PersonnelTable";
    private static final MMLogger LOGGER = MMLogger.create(PersonnelTableModelColumn.class);

    private final String name;

    PersonnelTableModelColumn(final String name) {
        this.name = getTextAt(RESOURCE_BUNDLE, name);
    }

    private String convertBooleanToYesNo(boolean yesNoValue) {
        return MHQInternationalization.getText(yesNoValue ? "Yes.text" : "No.text");
    }

    private String getNAText() {
        return MHQInternationalization.getText("NA.text");
    }

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
            case AGGREGATE_COMBAT -> {
                Unit unit = person.getUnit();
                if (unit != null && unit.getEntity() != null) {
                    Entity entity = unit.getEntity();

                    yield skillValue.apply(SkillType.getGunnerySkillFor(entity)) + "/" +
                                skillValue.apply(SkillType.getDrivingSkillFor(entity));
                }

                PersonnelRole primaryProfession = person.getPrimaryRole();
                PersonnelRole secondaryProfession = person.getSecondaryRole();
                PersonnelRole profession = primaryProfession.isCombat() ? primaryProfession : secondaryProfession;

                yield getAggregateSkillDisplay(person,
                      profession,
                      gunneryPilotingValue,
                      skillValue,
                      campaignOptions,
                      skillModifierData);
            }
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
            case CLAN_PERSONNEL -> convertBooleanToYesNo(person.isClanPersonnel());
            case COMMANDER -> convertBooleanToYesNo(person.isCommander());
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
            case DESTINATION_NAME -> LocationDisplay.getDestinationName(person, campaign, today);
            case DESTINATION_PLANET -> LocationDisplay.getDestinationPlanet(person, today);
            case DESTINATION_SYSTEM -> LocationDisplay.getDestinationSystem(person, today);
            case DEXTERITY -> getAttributeScoreDisplay(person, SkillAttribute.DEXTERITY);
            case DIVORCEABLE -> person.getGenealogy().hasSpouse() ?
                                      convertBooleanToYesNo(person.isDivorceable()) : getNAText();
            case DUE_DATE -> person.getDueDateAsString(campaign);
            case EDGE -> {
                int currentAttributeValue = person.getAttributeScore(SkillAttribute.EDGE);
                int attributeCap = person.getAttributeCap(SkillAttribute.EDGE);
                yield currentAttributeValue + " / " + attributeCap;
            }
            case EMPLOYED -> convertBooleanToYesNo(person.isEmployed());
            case EXTRA_INCOME -> Integer.toString(person.getExtraIncomeTraitLevel());
            case FATIGUE -> Integer.toString(getEffectiveFatigue(person.getAdjustedFatigue(),
                  person.getPermanentFatigue(), person.isClanPersonnel(),
                  person.getSkillLevel(campaign, false, true)));
            case FIRST_NAME -> person.getFirstName();
            case FORCE -> {
                final Formation formation = campaign.getFormationFor(person);
                yield (formation == null) ? "-" : formation.getName();
            }
            case FOUNDER -> convertBooleanToYesNo(person.isFounder());
            case GENDER -> GenderDescriptors.MALE_FEMALE_OTHER.getDescriptorCapitalized(person.getGender());
            case GIVEN_NAME -> person.getGivenName();
            case GREED -> {
                Greed trait = person.getGreed();
                String sign = trait.isTraitPositive() ? "+" : "-";
                yield trait + " (" + (trait.isTraitMajor() ? sign + sign : sign) + ')';
            }
            case GROUND_VEHICLE -> gunneryPilotingValue.apply(SkillType.S_GUN_VEE, SkillType.S_PILOT_GVEE);
            case HIDE_PERSONALITY -> convertBooleanToYesNo(person.isHidePersonality());
            case HIGHEST_EDUCATION -> person.getEduHighestEducation().toString();
            case IMMORTAL -> person.getStatus().isDead() ? getNAText()
                                                       : convertBooleanToYesNo(person.isImmortal());
            case IMPLANT_COUNT -> Integer.toString(person.countOptions(PersonnelOptions.MD_ADVANTAGES));
            case MODIFICATION_COUNT -> Integer.toString(person.getProstheticInjuries().size());
            case INJURIES -> campaign.getCampaignOptions().isUseAdvancedMedical()
                                   ? Integer.toString(person.getInjuries().size())
                                   : Integer.toString(person.getHits());
            case INTELLIGENCE -> getAttributeScoreDisplay(person, SkillAttribute.INTELLIGENCE);
            case KILLS -> Integer.toString(campaign.getKillsFor(person.getId()).size());
            case LAST_NAME -> person.getLastName();
            case LAST_RANK_CHANGE_DATE -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getLastRankChangeDate());
            case LEADERSHIP -> skillValue.apply(SkillType.S_LEADER);
            case LOCATION_NAME -> LocationDisplay.getLocationName(person, campaign, today);
            case LOCATION_PLANET -> LocationDisplay.getLocationPlanet(person, today, campaign);
            case LOCATION_SYSTEM -> LocationDisplay.getLocationSystem(person, today, campaign);
            case LOYALTY -> String.valueOf(person.getAdjustedLoyalty(campaign.getFaction(),
                  campaignOptions.isUseAlternativeAdvancedMedical()));
            case MARRIAGEABLE -> person.getGenealogy().hasSpouse() ? getNAText()
                                                           : convertBooleanToYesNo(person.isMarriageable());
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
                  convertBooleanToYesNo(person.isNeverAssignMaintenanceAutomatically());
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
            case PREFERS_MEN -> person.isChild(campaign.getLocalDate()) ? getNAText() :
                                                          convertBooleanToYesNo(person.isPrefersMen());
            case PREFERS_WOMEN -> person.isChild(campaign.getLocalDate()) ? getNAText() :
                                                            convertBooleanToYesNo(person.isPrefersWomen());
            case PROTOMEK -> skillValue.apply(SkillType.S_GUN_PROTO);
            case QUICK_TRAIN_IGNORE -> convertBooleanToYesNo(person.isQuickTrainIgnore());
            case RANK -> person.makeHTMLRank();
            case REASONING -> person.getReasoning().getLabel();
            case RECRUITMENT_DATE -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getRecruitment());
            case REFLEXES -> getAttributeScoreDisplay(person, SkillAttribute.REFLEXES);
            case REPUTATION -> Integer.toString(adjustedReputation);
            case RETIREMENT_DATE -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getRetirement());
            case SALARY -> person.getSalary(campaign).toAmountAndSymbolString();
            case SALVAGE_SUPERVISOR -> convertBooleanToYesNo(person.isSalvageSupervisor());
            case SCOUTING -> getAggregateSmallArmsOrScouting(ScoutingSkills.getBestScoutingSkill(person),
                  person, skillModifierData);
            case SECOND_IN_COMMAND -> convertBooleanToYesNo(person.isSecondInCommand());
            case SHIP_TRANSPORT -> person.getUnit() != null && person.getUnit().getTransportShipAssignment() != null
                                         ? person.getUnit().getTransportShipAssignment().getTransportShip().getName()
                                         : "-";
            case SKILL_LEVEL -> "<html>" + SkillType.getColoredExperienceLevelName(
                  person.getExperienceLevel(campaign, false, true)) + "</html>";
            case SMALL_ARMS -> getAggregateSmallArmsOrScouting(InfantryGunnerySkills.getBestInfantryGunnerySkill(person,
                  campaignOptions.isUseSmallArmsOnly()), person, skillModifierData);
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
                    String key = unit.usesSoldiers() ? "Cell.SURNAME.Soldiers.suffix" : "Cell.SURNAME.Crew.suffix";
                    yield surname + " " + getFormattedTextAt(RESOURCE_BUNDLE, key, crewSize);
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
            case WANTS_CHILDREN -> person.isChild(campaign.getLocalDate()) ? getNAText() :
                                                             convertBooleanToYesNo(person.isWantsChildren());
            case UNDER_PROTECTION -> convertBooleanToYesNo(person.isUnderProtection());
            case COVER_MEDICAL_EXPENSES -> convertBooleanToYesNo(person.isCoverIllicitMedicalExpenses());
            case BLOCK_MATERNITY_LEAVE -> convertBooleanToYesNo(person.isBlockMaternityLeave());
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

    private static String getAggregateSkillDisplay(Person person, PersonnelRole primaryProfession,
          BiFunction<String, String, String> gunneryPilotingValue, Function<String, String> skillValue,
          CampaignOptions campaignOptions, SkillModifierData skillModifierData) {
        return switch (primaryProfession) {
            case PersonnelRole.LAM_PILOT -> {
                String mekSkills = gunneryPilotingValue.apply(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK);
                String aeroSkills = gunneryPilotingValue.apply(SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO);
                yield mekSkills + " / " + aeroSkills;
            }
            case PersonnelRole.MEKWARRIOR -> gunneryPilotingValue.apply(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK);
            case PersonnelRole.VEHICLE_CREW_VTOL ->
                  gunneryPilotingValue.apply(SkillType.S_GUN_VEE, SkillType.S_PILOT_VTOL);
            case PersonnelRole.VEHICLE_CREW_NAVAL ->
                  gunneryPilotingValue.apply(SkillType.S_GUN_VEE, SkillType.S_PILOT_NVEE);
            case PersonnelRole.VEHICLE_CREW_GROUND ->
                  gunneryPilotingValue.apply(SkillType.S_GUN_VEE, SkillType.S_PILOT_GVEE);
            case PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT ->
                  gunneryPilotingValue.apply(SkillType.S_GUN_JET, SkillType.S_PILOT_JET);
            case PersonnelRole.VESSEL_PILOT, PersonnelRole.VESSEL_GUNNER ->
                  gunneryPilotingValue.apply(SkillType.S_GUN_SPACE,
                        SkillType.S_PILOT_SPACE);
            case PersonnelRole.AEROSPACE_PILOT ->
                  gunneryPilotingValue.apply(SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO);
            case PersonnelRole.BATTLE_ARMOUR -> skillValue.apply(SkillType.S_GUN_BA);
            case PersonnelRole.SOLDIER -> {
                String smallArms = getAggregateSmallArmsOrScouting(InfantryGunnerySkills.getBestInfantryGunnerySkill(
                      person,
                      campaignOptions.isUseSmallArmsOnly()), person, skillModifierData);
                String antiMek = skillValue.apply(SkillType.S_ANTI_MEK);
                yield smallArms + "/" + antiMek;
            }
            case PersonnelRole.PROTOMEK_PILOT -> skillValue.apply(SkillType.S_GUN_PROTO);
            default -> "-/-";
        };
    }

    private static @NonNull String getAggregateSmallArmsOrScouting(String skillName, Person person,
          SkillModifierData skillModifierData) {
        return skillName == null ?
                     "-" :
                     Integer.toString(person.getSkill(skillName).getFinalSkillValue(skillModifierData));
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
                colorReasons.append(MHQInternationalization.getText(key));
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
            case MODIFICATION_COUNT:
                StringBuilder modificationCount = new StringBuilder("<html>");
                for (Injury injury : person.getProstheticInjuries()) {
                    modificationCount.append(injury.getName()).append("<br>");
                }
                modificationCount.append("</html>");
                return modificationCount.toString();
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
            case COMBAT -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     PERSONNEL_ROLE,
                     AGGREGATE_COMBAT,
                     ARTILLERY,
                     SCOUTING,
                     LEADERSHIP,
                     TACTICS,
                     STRATEGY -> true;
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
                     SECOND_IN_COMMAND,
                     FOUNDER,
                     CLAN_PERSONNEL,
                     UNDER_PROTECTION,
                     IMMORTAL -> true;
                default -> false;
            };
            case FLAGS_B -> switch (this) {
                // Max 7 flags
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     DIVORCEABLE,
                     PREFERS_MEN,
                     PREFERS_WOMEN,
                     COVER_MEDICAL_EXPENSES,
                     WANTS_CHILDREN,
                     BLOCK_MATERNITY_LEAVE -> true;
                default -> false;
            };
            case FLAGS_C -> switch (this) {
                // Max 7 flags
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     SALVAGE_SUPERVISOR,
                     QUICK_TRAIN_IGNORE,
                     NEVER_ASSIGN_AUTO_MAINTENANCE,
                     HIDE_PERSONALITY -> true;
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
                case MODIFICATION_COUNT -> campaign.getCampaignOptions().isUseAlternativeAdvancedMedical();
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
                 MODIFICATION_COUNT,
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
