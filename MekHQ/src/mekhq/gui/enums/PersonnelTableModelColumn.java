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

    PERSON("Column.PERSON.title", NaturalOrderComparator.INSTANCE,
          person -> ""),
    RANK("Column.RANK.title", PersonRankSorter.INSTANCE,
          (person, campaign) -> person, Person::getRankName),
    FIRST_NAME("Column.FIRST_NAME.title", NaturalOrderComparator.INSTANCE,
          Person::getFirstName),
    LAST_NAME("Column.LAST_NAME.title", NaturalOrderComparator.INSTANCE,
          Person::getLastName),
    PRE_NOMINAL("Column.PRE_NOMINAL.title", NaturalOrderComparator.INSTANCE,
          Person::getPreNominal),
    GIVEN_NAME("Column.GIVEN_NAME.title", NaturalOrderComparator.INSTANCE,
          Person::getGivenName),
    SURNAME("Column.SURNAME.title", NaturalOrderComparator.INSTANCE,
          person -> StringUtility.isNullOrBlank(person.getSurname()) ? "" : person.getSurname()),
    SURNAME_GROUPED_BY_UNIT("Column.SURNAME.title", NaturalOrderComparator.INSTANCE,
          PersonnelTableModelColumn::getSurnameGroupedByUnit),
    BLOODNAME("Column.BLOODNAME.title", NaturalOrderComparator.INSTANCE,
          Person::getBloodname),
    POST_NOMINAL("Column.POST_NOMINAL.title", NaturalOrderComparator.INSTANCE,
          Person::getPostNominal),
    CALLSIGN("Column.CALLSIGN.title", NaturalOrderComparator.INSTANCE,
          Person::getCallsign),
    AGE("Column.AGE.title", IntegerStringSorter.INSTANCE,
          (person, campaign) -> Integer.toString(person.getAge(campaign.getLocalDate()))),
    PERSONNEL_STATUS("Column.PERSONNEL_STATUS.title", NaturalOrderComparator.INSTANCE,
          person -> person.getStatus().toString()),
    GENDER("Column.GENDER.title", NaturalOrderComparator.INSTANCE,
          person -> GenderDescriptors.MALE_FEMALE_OTHER.getDescriptorCapitalized(person.getGender())),
    SKILL_LEVEL("Column.SKILL_LEVEL.title", LevelSorter.INSTANCE,
          (person, campaign) -> "<html>" + SkillType.getColoredExperienceLevelName(
                person.getExperienceLevel(campaign, false, true)) + "</html>"),
    PERSONNEL_ROLE("Column.PERSONNEL_ROLE.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> person.getFormatedRoleDescriptions(campaign.getLocalDate())),
    UNIT_ASSIGNMENT("Column.UNIT_ASSIGNMENT.title", NaturalOrderComparator.INSTANCE,
          PersonnelTableModelColumn::getUnitAssignment),
    MARKET_UNIT_ASSIGNMENT("Column.UNIT_ASSIGNMENT.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> {
              PersonnelMarket market = campaign.getPersonnelMarket();
              Entity entity = (market == null) ? null : market.getAttachedEntity(person);
              return (entity == null) ? "-" : entity.getDisplayName();
          }),
    FORCE("Column.FORCE.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> {
              Formation formation = campaign.getFormationFor(person);
              return (formation == null) ? "-" : formation.getName();
          }),
    DEPLOYED("Column.DEPLOYED.title", NaturalOrderComparator.INSTANCE,
          PersonnelTableModelColumn::getDeployedScenarioName),
    MEK("Column.MEK.title", BonusSorter.INSTANCE,
          skillPairModelExtractor(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK)),
    GROUND_VEHICLE("Column.GROUND_VEHICLE.title", BonusSorter.INSTANCE,
          skillPairModelExtractor(SkillType.S_GUN_VEE, SkillType.S_PILOT_GVEE)),
    NAVAL_VEHICLE("Column.NAVAL_VEHICLE.title", BonusSorter.INSTANCE,
          skillPairModelExtractor(SkillType.S_GUN_VEE, SkillType.S_PILOT_NVEE)),
    VTOL("Column.VTOL.title", BonusSorter.INSTANCE,
          skillPairModelExtractor(SkillType.S_GUN_VEE, SkillType.S_PILOT_VTOL)),
    AEROSPACE("Column.AEROSPACE.title", BonusSorter.INSTANCE,
          skillPairModelExtractor(SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO)),
    CONVENTIONAL_AIRCRAFT("Column.CONVENTIONAL_AIRCRAFT.title", BonusSorter.INSTANCE,
          skillPairModelExtractor(SkillType.S_GUN_JET, SkillType.S_PILOT_JET)),
    VESSEL("Column.VESSEL.title", BonusSorter.INSTANCE,
          skillPairModelExtractor(SkillType.S_GUN_SPACE, SkillType.S_PILOT_SPACE)),
    PROTOMEK("Column.PROTOMEK.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_GUN_PROTO)),
    BATTLE_ARMOUR("Column.BATTLE_ARMOUR.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_GUN_BA)),
    AGGREGATE_COMBAT("Column.AGGREGATE_COMBAT.title", NaturalOrderComparator.INSTANCE,
          PersonnelTableModelColumn::getAggregateSkillValue),
    SMALL_ARMS("Column.SMALL_ARMS.title", BonusSorter.INSTANCE,
          (person, campaign) -> getSkillValue(person, campaign).apply(InfantryGunnerySkills.getBestInfantryGunnerySkill(person,
                campaign.getCampaignOptions().isUseSmallArmsOnly()))),
    ANTI_MEK("Column.ANTI_MEK.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_ANTI_MEK)),
    ARTILLERY("Column.ARTILLERY.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_ARTILLERY)),
    NAVIGATION("Column.NAVIGATION.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_NAVIGATION)),
    TACTICS("Column.TACTICS.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_TACTICS)),
    STRATEGY("Column.STRATEGY.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_STRATEGY)),
    LEADERSHIP("Column.LEADERSHIP.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_LEADER)),
    SCOUTING("Column.SCOUTING.title", BonusSorter.INSTANCE,
          (person, campaign) -> getSkillValue(person, campaign).apply(ScoutingSkills.getBestScoutingSkill(person))),
    ASTECH("Column.ASTECH.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_ASTECH)),
    TECH_MEK("Column.TECH_MEK.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_TECH_MEK)),
    TECH_AERO("Column.TECH_AERO.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_TECH_AERO)),
    TECH_MECHANIC("Column.TECH_MECHANIC.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_TECH_MECHANIC)),
    TECH_BA("Column.TECH_BA.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_TECH_BA)),
    TECH_VESSEL("Column.TECH_VESSEL.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_TECH_VESSEL)),
    ZERO_G("Column.ZERO_G.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_ZERO_G_OPERATIONS)),
    MEDTECH("Column.MEDTECH.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_MEDTECH)),
    MEDICAL("Column.MEDICAL.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_SURGERY)),
    APPRAISAL("Column.APPRAISAL.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_APPRAISAL)),
    TRAINING("Column.TRAINING.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_TRAINING)),
    ADMINISTRATION("Column.ADMINISTRATION.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_ADMIN)),
    NEGOTIATION("Column.NEGOTIATION.title", BonusSorter.INSTANCE,
          skillModelExtractor(SkillType.S_NEGOTIATION)),
    WORK_MINUTES("Column.WORK_MINUTES.title", NaturalOrderComparator.INSTANCE,
          person -> person.isTechExpanded() ? String.valueOf(person.getMinutesLeft()) : "0"),
    TECH_MINUTES("Column.TECH_MINUTES.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> {
              boolean isUseTechAdmin = campaign.getCampaignOptions().isTechsUseAdministration();
              return person.isTechExpanded() ? String.valueOf(person.getDailyAvailableTechTime(isUseTechAdmin)) : "0";
          }),
    MEDICAL_CAPACITY("Column.MEDICAL_CAPACITY.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> {
              int baseBedCapacity = campaign.getCampaignOptions().getMaximumPatients();
              return person.isDoctor() ? String.valueOf(person.getDoctorMedicalCapacity(
                    campaign.getCampaignOptions().isDoctorsUseAdministration(), baseBedCapacity)) : "0";
          }),
    INJURIES("Column.INJURIES.title", IntegerStringSorter.INSTANCE,
          (person, campaign) -> Integer.toString(campaign.getCampaignOptions().isUseAdvancedMedical() ?
                                                       person.getInjuries().size() : person.getHits())),
    KILLS("Column.KILLS.title", IntegerStringSorter.INSTANCE,
          (person, campaign) -> Integer.toString(campaign.getKillsFor(person.getId()).size())),
    SALARY("Column.SALARY.title", FormattedNumberSorter.INSTANCE,
          (person, campaign) -> person.getSalary(campaign).toAmountAndSymbolString()),
    XP("Column.XP.title", IntegerStringSorter.INSTANCE,
          person -> Integer.toString(person.getXP())),
    ORIGIN_FACTION("Column.ORIGIN_FACTION.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) ->
                person.getOriginFaction() == null ? "-" :
                      person.getOriginFaction().getFullName(campaign.getGameYear())),
    ORIGIN_PLANET("Column.ORIGIN_PLANET.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> {
              Planet originPlanet = person.getOriginPlanet();
              return (originPlanet == null) ? "" : originPlanet.getName(campaign.getLocalDate());
          }),
    BIRTHDAY("Column.BIRTHDAY.title", DateStringComparator.INSTANCE,
          person -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getDateOfBirth())),
    RECRUITMENT_DATE("Column.RECRUITMENT_DATE.title", DateStringComparator.INSTANCE,
          person -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getRecruitment())),
    LAST_RANK_CHANGE_DATE("Column.LAST_RANK_CHANGE_DATE.title", DateStringComparator.INSTANCE,
          person -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getLastRankChangeDate())),
    DUE_DATE("Column.DUE_DATE.title", DateStringComparator.INSTANCE,
          Person::getDueDateAsString),
    RETIREMENT_DATE("Column.RETIREMENT_DATE.title", DateStringComparator.INSTANCE,
          person -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getRetirement())),
    DEATH_DATE("Column.DEATH_DATE.title", DateStringComparator.INSTANCE,
          person -> MekHQ.getMHQOptions().getDisplayFormattedDate(person.getDateOfDeath())),
    CLAN_PERSONNEL("Column.CLAN_PERSONNEL.title", NaturalOrderComparator.INSTANCE,
          person -> convertBooleanToYesNo(person.isClanPersonnel())),
    COMMANDER("Column.COMMANDER.title", NaturalOrderComparator.INSTANCE,
          person -> convertBooleanToYesNo(person.isCommander())),
    DIVORCEABLE("Column.DIVORCEABLE.title", NaturalOrderComparator.INSTANCE,
          person -> person.getGenealogy().hasSpouse() ? convertBooleanToYesNo(person.isDivorceable()) : getNAText()),
    EMPLOYED("Column.EMPLOYED.title", NaturalOrderComparator.INSTANCE,
          person -> convertBooleanToYesNo(person.isEmployed())),
    FOUNDER("Column.FOUNDER.title", NaturalOrderComparator.INSTANCE,
          person -> convertBooleanToYesNo(person.isFounder())),
    HIDE_PERSONALITY("Column.HIDE_PERSONALITY.title", NaturalOrderComparator.INSTANCE,
          person -> convertBooleanToYesNo(person.isHidePersonality())),
    IMMORTAL("Column.IMMORTAL.title", NaturalOrderComparator.INSTANCE,
          person -> person.getStatus().isDead() ? getNAText() : convertBooleanToYesNo(person.isImmortal())),
    MARRIAGEABLE("Column.MARRIAGEABLE.title", NaturalOrderComparator.INSTANCE,
          person -> person.getGenealogy().hasSpouse() ? getNAText() : convertBooleanToYesNo(person.isMarriageable())),
    NEVER_ASSIGN_AUTO_MAINTENANCE("Column.NEVER_ASSIGN_AUTO_MAINTENANCE.title", NaturalOrderComparator.INSTANCE,
          person -> convertBooleanToYesNo(person.isNeverAssignMaintenanceAutomatically())),
    PREFERS_MEN("Column.PREFERS_MEN.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> person.isChild(campaign.getLocalDate()) ? getNAText() :
                                      convertBooleanToYesNo(person.isPrefersMen())),
    PREFERS_WOMEN("Column.PREFERS_WOMEN.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> person.isChild(campaign.getLocalDate()) ? getNAText() :
                                      convertBooleanToYesNo(person.isPrefersWomen())),
    QUICK_TRAIN_IGNORE("Column.QUICK_TRAIN_IGNORE.title", NaturalOrderComparator.INSTANCE,
          person -> convertBooleanToYesNo(person.isQuickTrainIgnore())),
    SALVAGE_SUPERVISOR("Column.SALVAGE_SUPERVISOR.title", NaturalOrderComparator.INSTANCE,
          person -> convertBooleanToYesNo(person.isSalvageSupervisor())),
    SECOND_IN_COMMAND("Column.SECOND_IN_COMMAND.title", NaturalOrderComparator.INSTANCE,
          person -> convertBooleanToYesNo(person.isSecondInCommand())),
    WANTS_CHILDREN("Column.WANTS_CHILDREN.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> person.isChild(campaign.getLocalDate()) ? getNAText() :
                                      convertBooleanToYesNo(person.isWantsChildren())),
    UNDER_PROTECTION("Column.UNDER_PROTECTION.title", NaturalOrderComparator.INSTANCE,
          person -> convertBooleanToYesNo(person.isUnderProtection())),
    COVER_MEDICAL_EXPENSES("Column.COVER_MEDICAL_EXPENSES.title", NaturalOrderComparator.INSTANCE,
          person -> convertBooleanToYesNo(person.isCoverIllicitMedicalExpenses())),
    BLOCK_MATERNITY_LEAVE("Column.BLOCK_MATERNITY_LEAVE.title", NaturalOrderComparator.INSTANCE,
          person -> convertBooleanToYesNo(person.isBlockMaternityLeave())),
    TOUGHNESS("Column.TOUGHNESS.title", IntegerStringSorter.INSTANCE,
          person -> Integer.toString(person.getAdjustedToughness())),
    CONNECTIONS("Column.CONNECTIONS.title", IntegerStringSorter.INSTANCE,
          (person, campaign) -> {
              if (person.getBurnedConnectionsEndDate() != null) {
                  // FIXME: IntegerStringSorter will fail here
                  return "<html><b><font color='gray'>" + person.getAdjustedConnections(true) + "</font></b></html>";
              }
              return Integer.toString(person.getAdjustedConnections(true));
          }),
    WEALTH("Column.WEALTH.title", IntegerStringSorter.INSTANCE,
          person -> Integer.toString(person.getWealth())),
    EXTRA_INCOME("Column.EXTRA_INCOME.title", IntegerStringSorter.INSTANCE,
          person -> Integer.toString(person.getExtraIncomeTraitLevel())),
    REPUTATION("Column.REPUTATION.title", IntegerStringSorter.INSTANCE,
          (person, campaign) -> {
              int adjustedReputation = person.getAdjustedReputation(campaign.getCampaignOptions().isUseAgeEffects(),
                    campaign.isClanCampaign(), campaign.getLocalDate(), person.getRankNumeric());
              return Integer.toString(adjustedReputation);
          }),
    UNLUCKY("Column.UNLUCKY.title", IntegerStringSorter.INSTANCE,
          person -> Integer.toString(person.getUnlucky())),
    BLOODMARK("Column.BLOODMARK.title", IntegerStringSorter.INSTANCE,
          person -> Integer.toString(person.getBloodmark())),
    FATIGUE("Column.FATIGUE.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) ->
              Integer.toString(getEffectiveFatigue(person.getAdjustedFatigue(),
                    person.getPermanentFatigue(), person.isClanPersonnel(),
                    person.getSkillLevel(campaign, false, true)))),
    SPA_COUNT("Column.SPA_COUNT.title", IntegerStringSorter.INSTANCE,
          person -> Integer.toString(person.countOptions(PersonnelOptions.LVL3_ADVANTAGES))),
    MODIFICATION_COUNT("Column.MODIFICATION_COUNT.title", IntegerStringSorter.INSTANCE,
          person -> Integer.toString(person.getProstheticInjuries().size())),
    IMPLANT_COUNT("Column.IMPLANT_COUNT.title", IntegerStringSorter.INSTANCE,
          person -> Integer.toString(person.countOptions(PersonnelOptions.MD_ADVANTAGES))),
    LOYALTY("Column.LOYALTY.title", IntegerStringSorter.INSTANCE,
          (person, campaign) -> String.valueOf(person.getAdjustedLoyalty(campaign.getFaction(),
                campaign.getCampaignOptions().isUseAlternativeAdvancedMedical()))),
    HIGHEST_EDUCATION("Column.HIGHEST_EDUCATION.title", EducationLevelSorter.INSTANCE,
          person -> person.getEduHighestEducation().toString()),
    CURRENT_EDUCATION("Column.CURRENT_EDUCATION.title", EducationLevelSorter.INSTANCE,
          person -> {
              Academy currentAcademy = EducationController.getAcademy(person.getEduAcademySet(),
                    person.getEduAcademyNameInSet());
              return currentAcademy == null ? "" :
                           EducationLevel.fromString(String.valueOf(currentAcademy.getEducationLevel(person))).toString();
          }),
    ACADEMY("Column.ACADEMY.title", NaturalOrderComparator.INSTANCE,
          person -> {
              Academy currentAcademy = EducationController.getAcademy(person.getEduAcademySet(),
                    person.getEduAcademyNameInSet());
              return currentAcademy == null ? "" : currentAcademy.getName();
          }),
    COURSE("Column.COURSE.title", NaturalOrderComparator.INSTANCE,
          person -> {
              Academy currentAcademy = EducationController.getAcademy(person.getEduAcademySet(),
                    person.getEduAcademyNameInSet());
              return currentAcademy == null ? "" : currentAcademy.getQualifications().get(person.getEduCourseIndex());
          }),
    ACADEMY_DURATION("Column.ACADEMY_DURATION.title", NaturalOrderComparator.INSTANCE,
          person -> {
              Academy currentAcademy = EducationController.getAcademy(person.getEduAcademySet(),
                    person.getEduAcademyNameInSet());
              return currentAcademy == null ? "" : String.valueOf(person.getEduEducationTime());
          }),
    AGGRESSION("Column.AGGRESSION.title", NaturalOrderComparator.INSTANCE,
          person -> {
              Aggression trait = person.getAggression();
              String sign = trait.isTraitPositive() ? "+" : "-";
              return trait + " (" + (trait.isTraitMajor() ? sign + sign : sign) + ')';
          }),
    AMBITION("Column.AMBITION.title", NaturalOrderComparator.INSTANCE,
          person -> {
              Ambition trait = person.getAmbition();
              String sign = trait.isTraitPositive() ? "+" : "-";
              return trait + " (" + (trait.isTraitMajor() ? sign + sign : sign) + ')';
          }),
    GREED("Column.GREED.title", NaturalOrderComparator.INSTANCE,
          person -> {
              Greed trait = person.getGreed();
              String sign = trait.isTraitPositive() ? "+" : "-";
              return trait + " (" + (trait.isTraitMajor() ? sign + sign : sign) + ')';
          }),
    SOCIAL("Column.SOCIAL.title", NaturalOrderComparator.INSTANCE,
          person -> {
              Social trait = person.getSocial();
              String sign = trait.isTraitPositive() ? "+" : "-";
              return trait + " (" + (trait.isTraitMajor() ? sign + sign : sign) + ')';
          }),
    REASONING("Column.REASONING.title", ReasoningSorter.INSTANCE,
          person -> person.getReasoning().getLabel()),
    STRENGTH("Column.STRENGTH.title", AttributeScoreSorter.INSTANCE,
          attributeExtractor(SkillAttribute.STRENGTH)),
    BODY("Column.BODY.title", AttributeScoreSorter.INSTANCE,
          attributeExtractor(SkillAttribute.BODY)),
    REFLEXES("Column.REFLEXES.title", AttributeScoreSorter.INSTANCE,
          attributeExtractor(SkillAttribute.REFLEXES)),
    DEXTERITY("Column.DEXTERITY.title", AttributeScoreSorter.INSTANCE,
          attributeExtractor(SkillAttribute.DEXTERITY)),
    INTELLIGENCE("Column.INTELLIGENCE.title", AttributeScoreSorter.INSTANCE,
          attributeExtractor(SkillAttribute.INTELLIGENCE)),
    WILLPOWER("Column.WILLPOWER.title", AttributeScoreSorter.INSTANCE,
          attributeExtractor(SkillAttribute.WILLPOWER)),
    CHARISMA("Column.CHARISMA.title", AttributeScoreSorter.INSTANCE,
          attributeExtractor(SkillAttribute.CHARISMA)),
    EDGE("Column.EDGE.title", AttributeScoreSorter.INSTANCE,
          person -> {
              int currentAttributeValue = person.getAttributeScore(SkillAttribute.EDGE);
              int attributeCap = person.getAttributeCap(SkillAttribute.EDGE);
              return currentAttributeValue + " / " + attributeCap;
          }),
    SHIP_TRANSPORT("Column.SHIP_TRANSPORT.title", NaturalOrderComparator.INSTANCE,
          person -> {
              if (person.getUnit() == null || person.getUnit().getTransportShipAssignment() == null) {
                  return "-";
              }
              return person.getUnit().getTransportShipAssignment().getTransportShip().getName();
          }),
    TACTICAL_TRANSPORT("Column.TACTICAL_TRANSPORT.title", NaturalOrderComparator.INSTANCE,
          person -> {
              if (person.getUnit() == null || person.getUnit().getTacticalTransportAssignment() == null) {
                  return "-";
              }
              return person.getUnit().getTacticalTransportAssignment().getTransport().getName();
          }),
    LOCATION_SYSTEM("Column.LOCATION_SYSTEM.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> LocationDisplay.getLocationSystem(person, campaign.getLocalDate(), campaign)),
    LOCATION_PLANET("Column.LOCATION_PLANET.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> LocationDisplay.getLocationPlanet(person, campaign.getLocalDate(), campaign)),
    LOCATION_NAME("Column.LOCATION_NAME.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> LocationDisplay.getLocationName(person, campaign, campaign.getLocalDate())),
    DESTINATION_SYSTEM("Column.DESTINATION_SYSTEM.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> LocationDisplay.getDestinationSystem(person, campaign.getLocalDate())),
    DESTINATION_PLANET("Column.DESTINATION_PLANET.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> LocationDisplay.getDestinationPlanet(person, campaign.getLocalDate())),
    DESTINATION_NAME("Column.DESTINATION_NAME.title", NaturalOrderComparator.INSTANCE,
          (person, campaign) -> LocationDisplay.getDestinationName(person, campaign, campaign.getLocalDate()));

    private static final String RESOURCE_BUNDLE = "mekhq.resources.PersonnelTable";
    
    private final String name;
    private final Comparator<Object> modelComparator;
    private final BiFunction<Person, Campaign, Object> modelExtractor;
    private final Function<Object, String> modelToText;

    /**
     * Defines a personnel table column model, how it's sorted and visualised.
     *
     * @param name            column name resource key
     * @param modelComparator the {@link Comparator} used for row sorting, operates at the model level
     * @param modelExtractor  the {@link BiFunction} that extracts a model for each row based on {@link Person} and
     *                        {@link Campaign}
     * @param modelToText     the {@link Function} defining model's text representation
     * @param <Model>         model associated with every cell in this column
     */
    <Model> PersonnelTableModelColumn(String name, Comparator<Model> modelComparator,
          BiFunction<Person, Campaign, Model> modelExtractor, Function<Model, String> modelToText) {
        this.name = MHQInternationalization.getTextAt(RESOURCE_BUNDLE, name);
        // Enums can't be generic so we have to erase types here, ultimately it's
        // not critical because JTable API uses Objects anyway
        this.modelComparator = (Comparator<Object>) modelComparator;
        this.modelExtractor = (BiFunction<Person, Campaign, Object>) modelExtractor;
        this.modelToText = (Function<Object, String>) modelToText;
    }

    /**
     * Defines a personnel table column model, how it's sorted and visualised.
     * This is a simplified column implementation based on the String model.
     * See {@link #PersonnelTableModelColumn(String, Comparator, BiFunction)}.
     */
    PersonnelTableModelColumn(String name, Comparator<String> modelComparator,
          BiFunction<Person, Campaign, String> modelExtractor) {
        this(name, modelComparator, modelExtractor, string -> string);
    }

    /**
     * Defines a personnel table column model, how it's sorted and visualised.
     * This is a simplified column implementation based on the String model that only depends on {@link Person}.
     * See {@link #PersonnelTableModelColumn(String, Comparator, BiFunction)}.
     */
    PersonnelTableModelColumn(String name, Comparator<String> modelComparator,
          Function<Person, String> modelExtractor) {
        this(name, modelComparator, (person, campaign) -> modelExtractor.apply(person), string -> string);
    }

    public Comparator<?> getComparator() {
        return modelComparator;
    }

    private static String convertBooleanToYesNo(boolean yesNoValue) {
        return MHQInternationalization.getText(yesNoValue ? "Yes.text" : "No.text");
    }

    private static String getNAText() {
        return MHQInternationalization.getText("NA.text");
    }

    public Object getCellValue(Campaign campaign, Person person) {
        return modelExtractor.apply(person, campaign);
    }

    private static String getFormattedTextAt(String key, Object... args) {
        return MHQInternationalization.getFormattedTextAt(RESOURCE_BUNDLE, key, args);
    }

    public String getText(Object model) {
        return modelToText.apply(model);
    }

    private static String getSurnameGroupedByUnit(Person person) {
        final String surname = person.getSurname();
        if (StringUtility.isNullOrBlank(surname)) {
            return "";
        } else {
            final Unit unit = person.getUnit();
            if (unit == null) {
                return surname;
            }
            final int crewSize = unit.getCrew().size() - 1;
            if (crewSize <= 0) {
                return surname;
            }
            String key = unit.usesSoldiers() ? "Cell.SURNAME.Soldiers.suffix" : "Cell.SURNAME.Crew.suffix";
            return surname + " " + getFormattedTextAt(key, crewSize);
        }
    }

    private static String getDeployedScenarioName(Person person, Campaign campaign) {
        Unit unit = person.getUnit();
        if (unit == null || !unit.isDeployed()) {
            return "-";
        }
        Scenario scenario = campaign.getScenario(unit.getScenarioId());
        return scenario == null ? "-" : scenario.getName();
    }

    private static BiFunction<Person, Campaign, String> skillModelExtractor(String skillName) {
        return (person, campaign) -> getSkillValue(person, campaign).apply(skillName);
    }

    private static BiFunction<Person, Campaign, String> skillPairModelExtractor(String gunnerySkill, String pilotingSkill) {
        return (person, campaign) -> {
            Function<String, String> skillValue = getSkillValue(person, campaign);
            return skillValue.apply(gunnerySkill) + '/' + skillValue.apply(pilotingSkill);
        };
    }

    private static String getAggregateSkillValue(Person person, Campaign campaign) {
        Function<String, String> skillValue = getSkillValue(person, campaign);
        Unit unit = person.getUnit();
        if (unit != null && unit.getEntity() != null) {
            Entity entity = unit.getEntity();
            return skillValue.apply(SkillType.getGunnerySkillFor(entity)) + "/" +
                         skillValue.apply(SkillType.getDrivingSkillFor(entity));
        }
        PersonnelRole primaryProfession = person.getPrimaryRole();
        PersonnelRole secondaryProfession = person.getSecondaryRole();
        PersonnelRole profession = primaryProfession.isCombat() ? primaryProfession : secondaryProfession;
        return getAggregateSkillDisplay(person, profession, skillValue, campaign.getCampaignOptions());
    }

    private static String getAggregateSkillDisplay(Person person, PersonnelRole primaryProfession,
          Function<String, String> skillValue, CampaignOptions campaignOptions) {
        return switch (primaryProfession) {
            case PersonnelRole.LAM_PILOT ->
                  skillValue.apply(SkillType.S_GUN_MEK) + '/' + skillValue.apply(SkillType.S_PILOT_MEK) + " / " +
                              skillValue.apply(SkillType.S_GUN_AERO) + '/' + skillValue.apply(SkillType.S_PILOT_AERO);
            case PersonnelRole.MEKWARRIOR ->
                  skillValue.apply(SkillType.S_GUN_MEK) + '/' + skillValue.apply(SkillType.S_PILOT_MEK);
            case PersonnelRole.VEHICLE_CREW_VTOL ->
                  skillValue.apply(SkillType.S_GUN_VEE) + '/' + skillValue.apply(SkillType.S_PILOT_VTOL);
            case PersonnelRole.VEHICLE_CREW_NAVAL ->
                  skillValue.apply(SkillType.S_GUN_VEE) + '/' + skillValue.apply(SkillType.S_PILOT_NVEE);
            case PersonnelRole.VEHICLE_CREW_GROUND ->
                  skillValue.apply(SkillType.S_GUN_VEE) + '/' + skillValue.apply(SkillType.S_PILOT_GVEE);
            case PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT ->
                  skillValue.apply(SkillType.S_GUN_JET) + '/' + skillValue.apply(SkillType.S_PILOT_JET);
            case PersonnelRole.VESSEL_PILOT, PersonnelRole.VESSEL_GUNNER ->
                  skillValue.apply(SkillType.S_GUN_SPACE) + '/' + skillValue.apply(SkillType.S_PILOT_SPACE);
            case PersonnelRole.AEROSPACE_PILOT ->
                  skillValue.apply(SkillType.S_GUN_AERO) + '/' + skillValue.apply(SkillType.S_PILOT_AERO);
            case PersonnelRole.BATTLE_ARMOUR ->
                  skillValue.apply(SkillType.S_GUN_BA);
            case PersonnelRole.SOLDIER -> {
                String gunnerySkill = InfantryGunnerySkills.getBestInfantryGunnerySkill(person,
                      campaignOptions.isUseSmallArmsOnly());
                yield skillValue.apply(gunnerySkill) + '/' + skillValue.apply(SkillType.S_ANTI_MEK);
            }
            case PersonnelRole.PROTOMEK_PILOT ->
                  skillValue.apply(SkillType.S_GUN_PROTO);
            default -> "-/-";
        };
    }

    private static String getUnitAssignment(Person person) {
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
                unit = person.getTechUnits().getFirst();
                if (unit != null) {
                    return "<html>" + ReportingUtilities.separateIf(refitString, ", ",
                          unit.getName() + " (" + person.getMaintenanceTimeUsing() + "m)") + "</html>";
                }
            } else {
                return "<html>" + ReportingUtilities.separateIf(refitString, ", ",
                      person.getTechUnits().size() + " units (" + person.getMaintenanceTimeUsing() + "m)") +
                             "</html>";
            }
        }
        // Final fallback return of nothing
        return "-";
    }

    private static @NonNull Function<String, String> getSkillValue(Person person, Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        SkillModifierData skillModifierData = person.getSkillModifierData(
              campaignOptions.isUseAgeEffects(), campaign.isClanCampaign(), campaign.getLocalDate(), true);
        return skillName -> (skillName == null) || !person.hasSkill(skillName) ? "-" :
                                  Integer.toString(person.getSkill(skillName).getFinalSkillValue(skillModifierData));
    }

    /**
     * Constructs a visualisation function of a person's skill attribute, including their current score,
     * maximum possible score (cap), and attribute modifier.
     *
     * @param attribute the specific skill attribute being evaluated
     *
     * @return a function generating "currentScore / attributeCap (+/- modifier)" string for a person
     *
     * @author Illiani
     * @since 0.51.00
     */
    private static Function<Person, String> attributeExtractor(SkillAttribute attribute) {
        return person -> {
            int currentAttributeValue = person.getAttributeScore(attribute);
            int attributeCap = person.getAttributeCap(attribute);

            int attributeModifier = person.getAttributeModifier(attribute);
            String sign = attributeModifier >= 0 ? "+" : "";

            return currentAttributeValue + " / " + attributeCap + " (" + sign + attributeModifier + ")";
        };
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
