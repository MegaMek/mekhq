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
import mekhq.gui.sorter.PersonRankSorter;
import mekhq.gui.sorter.ReasoningSorter;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.ReportingUtilities;
import org.jspecify.annotations.NonNull;

public enum PersonnelTableModelColumn {

    PERSON("Column.PERSON.title", NaturalOrderComparator.INSTANCE),
    RANK("Column.RANK.title", PersonRankSorter.INSTANCE),
    FIRST_NAME("Column.FIRST_NAME.title", NaturalOrderComparator.INSTANCE),
    LAST_NAME("Column.LAST_NAME.title", NaturalOrderComparator.INSTANCE),
    PRE_NOMINAL("Column.PRE_NOMINAL.title", NaturalOrderComparator.INSTANCE),
    GIVEN_NAME("Column.GIVEN_NAME.title", NaturalOrderComparator.INSTANCE),
    SURNAME("Column.SURNAME.title", NaturalOrderComparator.INSTANCE),
    SURNAME_GROUPED_BY_UNIT("Column.SURNAME.title", NaturalOrderComparator.INSTANCE),
    BLOODNAME("Column.BLOODNAME.title", NaturalOrderComparator.INSTANCE),
    POST_NOMINAL("Column.POST_NOMINAL.title", NaturalOrderComparator.INSTANCE),
    CALLSIGN("Column.CALLSIGN.title", NaturalOrderComparator.INSTANCE),
    AGE("Column.AGE.title", DateStringComparator.INSTANCE),
    PERSONNEL_STATUS("Column.PERSONNEL_STATUS.title", NaturalOrderComparator.INSTANCE),
    GENDER("Column.GENDER.title", NaturalOrderComparator.INSTANCE),
    SKILL_LEVEL("Column.SKILL_LEVEL.title", LevelSorter.INSTANCE),
    PERSONNEL_ROLE("Column.PERSONNEL_ROLE.title", NaturalOrderComparator.INSTANCE),
    UNIT_ASSIGNMENT("Column.UNIT_ASSIGNMENT.title", NaturalOrderComparator.INSTANCE),
    MARKET_UNIT_ASSIGNMENT("Column.UNIT_ASSIGNMENT.title", NaturalOrderComparator.INSTANCE),
    FORCE("Column.FORCE.title", NaturalOrderComparator.INSTANCE),
    DEPLOYED("Column.DEPLOYED.title", NaturalOrderComparator.INSTANCE),
    MEK("Column.MEK.title", BonusSorter.INSTANCE),
    GROUND_VEHICLE("Column.GROUND_VEHICLE.title", BonusSorter.INSTANCE),
    NAVAL_VEHICLE("Column.NAVAL_VEHICLE.title", BonusSorter.INSTANCE),
    VTOL("Column.VTOL.title", BonusSorter.INSTANCE),
    AEROSPACE("Column.AEROSPACE.title", BonusSorter.INSTANCE),
    CONVENTIONAL_AIRCRAFT("Column.CONVENTIONAL_AIRCRAFT.title", BonusSorter.INSTANCE),
    VESSEL("Column.VESSEL.title", BonusSorter.INSTANCE),
    PROTOMEK("Column.PROTOMEK.title", BonusSorter.INSTANCE),
    BATTLE_ARMOUR("Column.BATTLE_ARMOUR.title", BonusSorter.INSTANCE),
    AGGREGATE_COMBAT("Column.AGGREGATE_COMBAT.title", NaturalOrderComparator.INSTANCE),
    SMALL_ARMS("Column.SMALL_ARMS.title", BonusSorter.INSTANCE),
    ANTI_MEK("Column.ANTI_MEK.title", BonusSorter.INSTANCE),
    ARTILLERY("Column.ARTILLERY.title", BonusSorter.INSTANCE),
    NAVIGATION("Column.NAVIGATION.title", BonusSorter.INSTANCE),
    TACTICS("Column.TACTICS.title", BonusSorter.INSTANCE),
    STRATEGY("Column.STRATEGY.title", BonusSorter.INSTANCE),
    LEADERSHIP("Column.LEADERSHIP.title", BonusSorter.INSTANCE),
    SCOUTING("Column.SCOUTING.title", BonusSorter.INSTANCE),
    ASTECH("Column.ASTECH.title", BonusSorter.INSTANCE),
    TECH_MEK("Column.TECH_MEK.title", BonusSorter.INSTANCE),
    TECH_AERO("Column.TECH_AERO.title", BonusSorter.INSTANCE),
    TECH_MECHANIC("Column.TECH_MECHANIC.title", BonusSorter.INSTANCE),
    TECH_BA("Column.TECH_BA.title", BonusSorter.INSTANCE),
    TECH_VESSEL("Column.TECH_VESSEL.title", BonusSorter.INSTANCE),
    ZERO_G("Column.ZERO_G.title", BonusSorter.INSTANCE),
    MEDTECH("Column.MEDTECH.title", BonusSorter.INSTANCE),
    MEDICAL("Column.MEDICAL.title", BonusSorter.INSTANCE),
    WORK_MINUTES("Column.WORK_MINUTES.title", NaturalOrderComparator.INSTANCE),
    TECH_MINUTES("Column.TECH_MINUTES.title", NaturalOrderComparator.INSTANCE),
    MEDICAL_CAPACITY("Column.MEDICAL_CAPACITY.title", NaturalOrderComparator.INSTANCE),
    APPRAISAL("Column.APPRAISAL.title", BonusSorter.INSTANCE),
    TRAINING("Column.TRAINING.title", BonusSorter.INSTANCE),
    ADMINISTRATION("Column.ADMINISTRATION.title", BonusSorter.INSTANCE),
    NEGOTIATION("Column.NEGOTIATION.title", BonusSorter.INSTANCE),
    INJURIES("Column.INJURIES.title", IntegerStringSorter.INSTANCE),
    KILLS("Column.KILLS.title", IntegerStringSorter.INSTANCE),
    SALARY("Column.SALARY.title", FormattedNumberSorter.INSTANCE),
    XP("Column.XP.title", IntegerStringSorter.INSTANCE),
    ORIGIN_FACTION("Column.ORIGIN_FACTION.title", NaturalOrderComparator.INSTANCE),
    ORIGIN_PLANET("Column.ORIGIN_PLANET.title", NaturalOrderComparator.INSTANCE),
    BIRTHDAY("Column.BIRTHDAY.title", DateStringComparator.INSTANCE),
    RECRUITMENT_DATE("Column.RECRUITMENT_DATE.title", DateStringComparator.INSTANCE),
    LAST_RANK_CHANGE_DATE("Column.LAST_RANK_CHANGE_DATE.title", DateStringComparator.INSTANCE),
    DUE_DATE("Column.DUE_DATE.title", DateStringComparator.INSTANCE),
    RETIREMENT_DATE("Column.RETIREMENT_DATE.title", DateStringComparator.INSTANCE),
    DEATH_DATE("Column.DEATH_DATE.title", DateStringComparator.INSTANCE),
    CLAN_PERSONNEL("Column.CLAN_PERSONNEL.title", NaturalOrderComparator.INSTANCE),
    COMMANDER("Column.COMMANDER.title", NaturalOrderComparator.INSTANCE),
    DIVORCEABLE("Column.DIVORCEABLE.title", NaturalOrderComparator.INSTANCE),
    EMPLOYED("Column.EMPLOYED.title", NaturalOrderComparator.INSTANCE),
    FOUNDER("Column.FOUNDER.title", NaturalOrderComparator.INSTANCE),
    HIDE_PERSONALITY("Column.HIDE_PERSONALITY.title", NaturalOrderComparator.INSTANCE),
    IMMORTAL("Column.IMMORTAL.title", NaturalOrderComparator.INSTANCE),
    MARRIAGEABLE("Column.MARRIAGEABLE.title", NaturalOrderComparator.INSTANCE),
    NEVER_ASSIGN_AUTO_MAINTENANCE("Column.NEVER_ASSIGN_AUTO_MAINTENANCE.title", NaturalOrderComparator.INSTANCE),
    PREFERS_MEN("Column.PREFERS_MEN.title", NaturalOrderComparator.INSTANCE),
    PREFERS_WOMEN("Column.PREFERS_WOMEN.title", NaturalOrderComparator.INSTANCE),
    QUICK_TRAIN_IGNORE("Column.QUICK_TRAIN_IGNORE.title", NaturalOrderComparator.INSTANCE),
    SALVAGE_SUPERVISOR("Column.SALVAGE_SUPERVISOR.title", NaturalOrderComparator.INSTANCE),
    SECOND_IN_COMMAND("Column.SECOND_IN_COMMAND.title", NaturalOrderComparator.INSTANCE),
    WANTS_CHILDREN("Column.WANTS_CHILDREN.title", NaturalOrderComparator.INSTANCE),
    UNDER_PROTECTION("Column.UNDER_PROTECTION.title", NaturalOrderComparator.INSTANCE),
    COVER_MEDICAL_EXPENSES("Column.COVER_MEDICAL_EXPENSES.title", NaturalOrderComparator.INSTANCE),
    BLOCK_MATERNITY_LEAVE("Column.BLOCK_MATERNITY_LEAVE.title", NaturalOrderComparator.INSTANCE),
    TOUGHNESS("Column.TOUGHNESS.title", IntegerStringSorter.INSTANCE),
    CONNECTIONS("Column.CONNECTIONS.title", IntegerStringSorter.INSTANCE),
    WEALTH("Column.WEALTH.title", IntegerStringSorter.INSTANCE),
    EXTRA_INCOME("Column.EXTRA_INCOME.title", IntegerStringSorter.INSTANCE),
    REPUTATION("Column.REPUTATION.title", IntegerStringSorter.INSTANCE),
    UNLUCKY("Column.UNLUCKY.title", IntegerStringSorter.INSTANCE),
    BLOODMARK("Column.BLOODMARK.title", IntegerStringSorter.INSTANCE),
    FATIGUE("Column.FATIGUE.title", NaturalOrderComparator.INSTANCE),
    SPA_COUNT("Column.SPA_COUNT.title", IntegerStringSorter.INSTANCE),
    MODIFICATION_COUNT("Column.MODIFICATION_COUNT.title", IntegerStringSorter.INSTANCE),
    IMPLANT_COUNT("Column.IMPLANT_COUNT.title", IntegerStringSorter.INSTANCE),
    LOYALTY("Column.LOYALTY.title", IntegerStringSorter.INSTANCE),
    HIGHEST_EDUCATION("Column.HIGHEST_EDUCATION.title", EducationLevelSorter.INSTANCE),
    CURRENT_EDUCATION("Column.CURRENT_EDUCATION.title", EducationLevelSorter.INSTANCE),
    ACADEMY("Column.ACADEMY.title", NaturalOrderComparator.INSTANCE),
    COURSE("Column.COURSE.title", NaturalOrderComparator.INSTANCE),
    ACADEMY_DURATION("Column.ACADEMY_DURATION.title", NaturalOrderComparator.INSTANCE),
    AGGRESSION("Column.AGGRESSION.title", NaturalOrderComparator.INSTANCE),
    AMBITION("Column.AMBITION.title", NaturalOrderComparator.INSTANCE),
    GREED("Column.GREED.title", NaturalOrderComparator.INSTANCE),
    SOCIAL("Column.SOCIAL.title", NaturalOrderComparator.INSTANCE),
    REASONING("Column.REASONING.title", ReasoningSorter.INSTANCE),
    STRENGTH("Column.STRENGTH.title", AttributeScoreSorter.INSTANCE),
    BODY("Column.BODY.title", AttributeScoreSorter.INSTANCE),
    REFLEXES("Column.REFLEXES.title", AttributeScoreSorter.INSTANCE),
    DEXTERITY("Column.DEXTERITY.title", AttributeScoreSorter.INSTANCE),
    INTELLIGENCE("Column.INTELLIGENCE.title", AttributeScoreSorter.INSTANCE),
    WILLPOWER("Column.WILLPOWER.title", AttributeScoreSorter.INSTANCE),
    CHARISMA("Column.CHARISMA.title", AttributeScoreSorter.INSTANCE),
    EDGE("Column.EDGE.title", AttributeScoreSorter.INSTANCE),
    SHIP_TRANSPORT("Column.SHIP_TRANSPORT.title", NaturalOrderComparator.INSTANCE),
    TACTICAL_TRANSPORT("Column.TACTICAL_TRANSPORT.title", NaturalOrderComparator.INSTANCE),
    LOCATION_SYSTEM("Column.LOCATION_SYSTEM.title", NaturalOrderComparator.INSTANCE),
    LOCATION_PLANET("Column.LOCATION_PLANET.title", NaturalOrderComparator.INSTANCE),
    LOCATION_NAME("Column.LOCATION_NAME.title", NaturalOrderComparator.INSTANCE),
    DESTINATION_SYSTEM("Column.DESTINATION_SYSTEM.title", NaturalOrderComparator.INSTANCE),
    DESTINATION_PLANET("Column.DESTINATION_PLANET.title", NaturalOrderComparator.INSTANCE),
    DESTINATION_NAME("Column.DESTINATION_NAME.title", NaturalOrderComparator.INSTANCE);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.PersonnelTable";
    private static final MMLogger LOGGER = MMLogger.create(PersonnelTableModelColumn.class);
    
    private final String name;
    private final Comparator<?> comparator;

    PersonnelTableModelColumn(String name, Comparator<?> comparator) {
        this.name = getTextAt(RESOURCE_BUNDLE, name);
        this.comparator = comparator;
    }

    public Comparator<?> getComparator() {
        return comparator;
    }

    private String convertBooleanToYesNo(boolean yesNoValue) {
        return MHQInternationalization.getText(yesNoValue ? "Yes.text" : "No.text");
    }

    private String getNAText() {
        return MHQInternationalization.getText("NA.text");
    }

    public Object getCellValue(Campaign campaign, Person person) {
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
            case RANK -> person;
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
                } else {
                    yield surname;
                }
            }
            case SURNAME_GROUPED_BY_UNIT -> {
                final String surname = person.getSurname();
                if (StringUtility.isNullOrBlank(surname)) {
                    yield "";
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
            case MARKET_UNIT_ASSIGNMENT -> {
                PersonnelMarket market = campaign.getPersonnelMarket();
                Entity entity = (market == null) ? null : market.getAttachedEntity(person);
                yield (entity == null) ? "-" : entity.getDisplayName();
            }
            case UNIT_ASSIGNMENT -> {
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
        } else if (this == PersonnelTableModelColumn.RANK) {
            return person.getRankName();
        }
        return null;
    }

    /**
     * Returns the tooltip text for this column, optionally including color reason explanations.
     *
     * @param person                   the person for this row
     * @param colorReasonKeys          list of i18n keys for color reasons, or null/empty if no special coloring
     *
     * @return the tooltip text, or null if no tooltip
     */
    public @Nullable String getToolTipText(Person person, @Nullable java.util.List<String> colorReasonKeys) {
        String baseTooltip = getBaseToolTipText(person);

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

    private @Nullable String getBaseToolTipText(Person person) {
        switch (this) {
            case PERSONNEL_STATUS:
                return person.getStatus().getToolTipText();
            case UNIT_ASSIGNMENT: {
                if (person.getTechUnits().size() > 1) {
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
        return (this == PERSON) ||
                     (this == FIRST_NAME) ||
                     (this == LAST_NAME) ||
                     (this == GIVEN_NAME) ||
                     (this == SURNAME) ||
                     (this == SURNAME_GROUPED_BY_UNIT) ||
                     (this == BLOODNAME) ||
                     (this == RANK) ||
                     (this == PERSONNEL_STATUS);
    }

    public int getWidth() {
        return switch (this) {
            case PERSON, UNIT_ASSIGNMENT, MARKET_UNIT_ASSIGNMENT -> 125;
            case RANK, FIRST_NAME, GIVEN_NAME, DEPLOYED -> 70;
            case LAST_NAME, SURNAME, SURNAME_GROUPED_BY_UNIT, BLOODNAME, CALLSIGN, SKILL_LEVEL, SALARY -> 50;
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
                 SURNAME_GROUPED_BY_UNIT,
                 BLOODNAME,
                 POST_NOMINAL,
                 CALLSIGN,
                 GENDER,
                 SKILL_LEVEL,
                 PERSONNEL_ROLE,
                 UNIT_ASSIGNMENT,
                 MARKET_UNIT_ASSIGNMENT,
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

    public boolean isVisible(Campaign campaign, PersonnelTabView view, JTable table,
          boolean loadAssignmentFromMarket, boolean groupByUnit) {
        return switch (view) {
            case GRAPHIC -> {
                table.setRowHeight(UIUtil.scaleForGUI(60));
                yield switch (this) {
                    case PERSON, FORCE -> true;
                    case UNIT_ASSIGNMENT -> !loadAssignmentFromMarket;
                    case MARKET_UNIT_ASSIGNMENT -> loadAssignmentFromMarket;
                    default -> false;
                };
            }
            case GENERAL -> switch (this) {
                case RANK,
                     FIRST_NAME,
                     LAST_NAME,
                     SKILL_LEVEL,
                     PERSONNEL_ROLE,
                     FORCE,
                     DEPLOYED,
                     INJURIES,
                     XP -> true;
                case UNIT_ASSIGNMENT -> !loadAssignmentFromMarket;
                case MARKET_UNIT_ASSIGNMENT -> loadAssignmentFromMarket;
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
                     SHIP_TRANSPORT,
                     TACTICAL_TRANSPORT -> true;
                case UNIT_ASSIGNMENT -> !loadAssignmentFromMarket;
                case MARKET_UNIT_ASSIGNMENT -> loadAssignmentFromMarket;
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
                     BLOODNAME,
                     POST_NOMINAL,
                     CALLSIGN,
                     GENDER,
                     PERSONNEL_ROLE,
                     KILLS -> true;
                case SURNAME -> !groupByUnit;
                case SURNAME_GROUPED_BY_UNIT -> groupByUnit;
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
