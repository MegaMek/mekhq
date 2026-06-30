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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import megamek.common.units.UnitType;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
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
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.campaign.personnel.skills.InfantryGunnerySkills;
import mekhq.campaign.personnel.skills.ScoutingSkills;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import mekhq.campaign.randomEvents.personalities.PersonalityTrait;
import mekhq.campaign.randomEvents.personalities.Reasoning;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;
import mekhq.gui.model.LocationDisplay;
import mekhq.gui.sorter.PersonRankSorter;
import mekhq.gui.sorter.PersonalityTraitSorter;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.ReportingUtilities;
import org.jspecify.annotations.NonNull;

public enum PersonnelTableModelColumn {

    PERSON_GRAPHICAL("Column.PERSON.title", Comparators.STRING_COMPARATOR,
          (person, campaign) -> "<html>" + person.getFullDesc(campaign) + "</html>"),
    RANK("Column.RANK.title", PersonRankSorter.INSTANCE,
          person -> person, Person::getRankName),
    FIRST_NAME("Column.FIRST_NAME.title", Comparators.STRING_COMPARATOR,
          Person::getFirstName),
    LAST_NAME("Column.LAST_NAME.title", Comparators.STRING_COMPARATOR,
          Person::getLastName),
    PRE_NOMINAL("Column.PRE_NOMINAL.title", Comparators.STRING_COMPARATOR,
          Person::getPreNominal),
    GIVEN_NAME("Column.GIVEN_NAME.title", Comparators.STRING_COMPARATOR,
          Person::getGivenName),
    SURNAME("Column.SURNAME.title", Comparators.STRING_COMPARATOR,
          person -> StringUtility.isNullOrBlank(person.getSurname()) ? "" : person.getSurname()),
    SURNAME_GROUPED_BY_UNIT("Column.SURNAME.title", Comparators.STRING_COMPARATOR,
          PersonnelTableModelColumn::getSurnameGroupedByUnit),
    BLOODNAME("Column.BLOODNAME.title", Comparators.STRING_COMPARATOR,
          Person::getBloodname),
    POST_NOMINAL("Column.POST_NOMINAL.title", Comparators.STRING_COMPARATOR,
          Person::getPostNominal),
    CALLSIGN("Column.CALLSIGN.title", Comparators.STRING_COMPARATOR,
          Person::getCallsign),
    AGE("Column.AGE.title", Comparators.INT_COMPARATOR,
          (person, campaign) -> person.getAge(campaign.getLocalDate()), Object::toString),
    PERSONNEL_STATUS("Column.PERSONNEL_STATUS.title", fieldBasedSorter(PersonnelStatus::getLabel),
          Person::getStatus, PersonnelStatus::getLabel),
    GENDER("Column.GENDER.title", Comparators.STRING_COMPARATOR,
          person -> GenderDescriptors.MALE_FEMALE_OTHER.getDescriptorCapitalized(person.getGender())),
    SKILL_LEVEL("Column.SKILL_LEVEL.title", Comparators.INT_COMPARATOR,
          (person, campaign) -> person.getExperienceLevel(campaign, false, true),
          level -> "<html>" + SkillType.getColoredExperienceLevelName(level) + "</html>"),
    PERSONNEL_ROLE("Column.PERSONNEL_ROLE.title", Comparators.STRING_COMPARATOR,
          (person, campaign) -> person.getFormatedRoleDescriptions(campaign.getLocalDate())),
    UNIT_ASSIGNMENT("Column.UNIT_ASSIGNMENT.title", Comparators.STRING_COMPARATOR,
          PersonnelTableModelColumn::getUnitAssignment),
    UNIT_ASSIGNMENT_GRAPHICAL("Column.UNIT_ASSIGNMENT.title", Comparators.STRING_COMPARATOR,
          PersonnelTableModelColumn::getUnitAssignmentGraphical),
    TECH_UNIT_ASSIGNMENT("Column.TECH_UNIT_ASSIGNMENT.title", Comparators.STRING_COMPARATOR,
          PersonnelTableModelColumn::getTechUnitAssignment),
    MARKET_UNIT_ASSIGNMENT("Column.UNIT_ASSIGNMENT.title", Comparators.STRING_COMPARATOR,
          (person, campaign) -> {
              PersonnelMarket market = campaign.getPersonnelMarket();
              Entity entity = (market == null) ? null : market.getAttachedEntity(person);
              return (entity == null) ? "-" : entity.getDisplayName();
          }),
    FORCE("Column.FORCE.title", Comparators.STRING_COMPARATOR,
          (person, campaign) -> {
              Formation formation = campaign.getFormationFor(person);
              return (formation == null) ? "-" : formation.getName();
          }),
    FORCE_GRAPHICAL("Column.FORCE.title", Comparators.STRING_COMPARATOR,
          PersonnelTableModelColumn::getForceTextGraphical),
    DEPLOYED("Column.DEPLOYED.title", Comparators.STRING_COMPARATOR,
          PersonnelTableModelColumn::getDeployedScenarioName),
    MEK("Column.MEK.title", SkillPair.COMPARATOR,
          skillPairModelExtractor(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK), SkillPair::toString),
    GROUND_VEHICLE("Column.GROUND_VEHICLE.title", SkillPair.COMPARATOR,
          skillPairModelExtractor(SkillType.S_GUN_VEE, SkillType.S_PILOT_GVEE), SkillPair::toString),
    NAVAL_VEHICLE("Column.NAVAL_VEHICLE.title", SkillPair.COMPARATOR,
          skillPairModelExtractor(SkillType.S_GUN_VEE, SkillType.S_PILOT_NVEE), SkillPair::toString),
    VTOL("Column.VTOL.title", SkillPair.COMPARATOR,
          skillPairModelExtractor(SkillType.S_GUN_VEE, SkillType.S_PILOT_VTOL), SkillPair::toString),
    AEROSPACE("Column.AEROSPACE.title", SkillPair.COMPARATOR,
          skillPairModelExtractor(SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO), SkillPair::toString),
    CONVENTIONAL_AIRCRAFT("Column.CONVENTIONAL_AIRCRAFT.title", SkillPair.COMPARATOR,
          skillPairModelExtractor(SkillType.S_GUN_JET, SkillType.S_PILOT_JET), SkillPair::toString),
    VESSEL("Column.VESSEL.title", SkillPair.COMPARATOR,
          skillPairModelExtractor(SkillType.S_GUN_SPACE, SkillType.S_PILOT_SPACE), SkillPair::toString),
    PROTOMEK("Column.PROTOMEK.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_GUN_PROTO), PersonnelTableModelColumn::skillToText),
    BATTLE_ARMOUR("Column.BATTLE_ARMOUR.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_GUN_BA), PersonnelTableModelColumn::skillToText),
    AGGREGATE_COMBAT("Column.AGGREGATE_COMBAT.title", NaturalOrderComparator.INSTANCE,
          PersonnelTableModelColumn::getAggregateCombatSkillValue),
    AGGREGATE_TECH("Column.AGGREGATE_TECH.title", SkillPair.COMPARATOR,
          PersonnelTableModelColumn::getAggregateTechSkillValue, SkillPair::toString),
    SMALL_ARMS("Column.SMALL_ARMS.title", Comparators.SKILL_COMPARATOR,
          (person, campaign) -> getSkillValue(person, campaign).apply(InfantryGunnerySkills.getBestInfantryGunnerySkill(
                person,
                campaign.getCampaignOptions().isUseSmallArmsOnly())), PersonnelTableModelColumn::skillToText),
    ANTI_MEK("Column.ANTI_MEK.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_ANTI_MEK), PersonnelTableModelColumn::skillToText),
    ARTILLERY("Column.ARTILLERY.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_ARTILLERY), PersonnelTableModelColumn::skillToText),
    NAVIGATION("Column.NAVIGATION.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_NAVIGATION), PersonnelTableModelColumn::skillToText),
    TACTICS("Column.TACTICS.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_TACTICS), PersonnelTableModelColumn::skillToText),
    STRATEGY("Column.STRATEGY.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_STRATEGY), PersonnelTableModelColumn::skillToText),
    LEADERSHIP("Column.LEADERSHIP.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_LEADER), PersonnelTableModelColumn::skillToText),
    SCOUTING("Column.SCOUTING.title", Comparators.SKILL_COMPARATOR,
          (person, campaign) -> getSkillValue(person, campaign).apply(ScoutingSkills.getBestScoutingSkill(person)),
          PersonnelTableModelColumn::skillToText),
    ASTECH("Column.ASTECH.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_ASTECH), PersonnelTableModelColumn::skillToText),
    TECH_MEK("Column.TECH_MEK.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_TECH_MEK), PersonnelTableModelColumn::skillToText),
    TECH_AERO("Column.TECH_AERO.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_TECH_AERO), PersonnelTableModelColumn::skillToText),
    TECH_MECHANIC("Column.TECH_MECHANIC.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_TECH_MECHANIC), PersonnelTableModelColumn::skillToText),
    TECH_BA("Column.TECH_BA.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_TECH_BA), PersonnelTableModelColumn::skillToText),
    TECH_VESSEL("Column.TECH_VESSEL.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_TECH_VESSEL), PersonnelTableModelColumn::skillToText),
    ZERO_G("Column.ZERO_G.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_ZERO_G_OPERATIONS), PersonnelTableModelColumn::skillToText),
    MEDTECH("Column.MEDTECH.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_MEDTECH), PersonnelTableModelColumn::skillToText),
    MEDICAL("Column.MEDICAL.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_SURGERY), PersonnelTableModelColumn::skillToText),
    APPRAISAL("Column.APPRAISAL.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_APPRAISAL), PersonnelTableModelColumn::skillToText),
    TRAINING("Column.TRAINING.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_TRAINING), PersonnelTableModelColumn::skillToText),
    ADMINISTRATION("Column.ADMINISTRATION.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_ADMIN), PersonnelTableModelColumn::skillToText),
    NEGOTIATION("Column.NEGOTIATION.title", Comparators.SKILL_COMPARATOR,
          skillModelExtractor(SkillType.S_NEGOTIATION), PersonnelTableModelColumn::skillToText),
    REMAINING_TECH_MINUTES("Column.REMAINING_TECH_MINUTES.title", Comparators.INT_COMPARATOR,
          person -> person.isTechExpanded() ? person.getMinutesLeft() : 0, Object::toString),
    MAINTENANCE_TECH_MINUTES("Column.MAINTENANCE_TECH_MINUTES.title", Comparators.INT_COMPARATOR,
          person -> person.isTechExpanded() ? person.getMaintenanceTimeUsing() : 0, Object::toString),
    MAX_TECH_MINUTES("Column.MAX_TECH_MINUTES.title", Comparators.INT_COMPARATOR,
          (person, campaign) -> {
              boolean isUseTechAdmin = campaign.getCampaignOptions().isTechsUseAdministration();
              return person.isTechExpanded() ? person.getDailyAvailableTechTime(isUseTechAdmin) : 0;
          }, Object::toString),
    MEDICAL_CAPACITY("Column.MEDICAL_CAPACITY.title", Comparators.INT_COMPARATOR,
          (person, campaign) -> {
              int baseBedCapacity = campaign.getCampaignOptions().getMaximumPatients();
              return person.isDoctor() ? person.getDoctorMedicalCapacity(
                    campaign.getCampaignOptions().isDoctorsUseAdministration(), baseBedCapacity) : 0;
          }, Object::toString),
    INJURIES("Column.INJURIES.title", Comparators.INT_COMPARATOR,
          (person, campaign) ->
                campaign.getCampaignOptions().isUseAdvancedMedical() ? person.getInjuries().size() : person.getHits(),
          Object::toString),
    KILLS("Column.KILLS.title", Comparators.INT_COMPARATOR,
          (person, campaign) -> campaign.getKillsFor(person.getId()).size(), Object::toString),
    SALARY("Column.SALARY.title", fieldBasedSorter(Money::getAmount),
          Person::getSalary, Money::toAmountAndSymbolString),
    XP("Column.XP.title", Comparators.INT_COMPARATOR,
          Person::getXP, Object::toString),
    ORIGIN("Column.ORIGIN.title", Comparators.STRING_COMPARATOR,
          PersonnelTableModelColumn::getOrigin),
    RECRUITMENT_DATE("Column.RECRUITMENT_DATE.title", Comparators.DATE_COMPARATOR,
          Person::getRecruitment, date -> MekHQ.getMHQOptions().getDisplayFormattedDate(date)),
    LAST_RANK_CHANGE_DATE("Column.LAST_RANK_CHANGE_DATE.title", Comparators.DATE_COMPARATOR,
          Person::getLastRankChangeDate, date -> MekHQ.getMHQOptions().getDisplayFormattedDate(date)),
    DUE_DATE("Column.DUE_DATE.title", Comparators.DATE_COMPARATOR,
          Person::getDueDate, date -> MekHQ.getMHQOptions().getDisplayFormattedDate(date)),
    RETIREMENT_DATE("Column.RETIREMENT_DATE.title", Comparators.DATE_COMPARATOR,
          Person::getRetirement, date -> MekHQ.getMHQOptions().getDisplayFormattedDate(date)),
    DEATH_DATE("Column.DEATH_DATE.title", Comparators.DATE_COMPARATOR,
          Person::getDateOfDeath, date -> MekHQ.getMHQOptions().getDisplayFormattedDate(date)),
    CLAN_PERSONNEL("Column.CLAN_PERSONNEL.title", Comparators.YES_NO_NA_COMPARATOR,
          Person::isClanPersonnel, PersonnelTableModelColumn::convertBooleanToYesNoNA),
    COMMAND_STATUS("Column.COMMAND_STATUS.title", Comparators.COMMAND_STATUS_COMPARATOR,
          person -> person, PersonnelTableModelColumn::getCommandStatus),
    FOUNDER("Column.FOUNDER.title", Comparators.YES_NO_NA_COMPARATOR,
          Person::isFounder, PersonnelTableModelColumn::convertBooleanToYesNoNA),
    HIDE_PERSONALITY("Column.HIDE_PERSONALITY.title", Comparators.YES_NO_NA_COMPARATOR,
          Person::isHidePersonality, PersonnelTableModelColumn::convertBooleanToYesNoNA),
    IMMORTAL("Column.IMMORTAL.title", Comparators.YES_NO_NA_COMPARATOR,
          person -> person.getStatus().isDead() ? null : person.isImmortal(),
          PersonnelTableModelColumn::convertBooleanToYesNoNA),
    NEVER_ASSIGN_AUTO_MAINTENANCE("Column.NEVER_ASSIGN_AUTO_MAINTENANCE.title", Comparators.YES_NO_NA_COMPARATOR,
          Person::isNeverAssignMaintenanceAutomatically, PersonnelTableModelColumn::convertBooleanToYesNoNA),
    PREFERENCE("Column.PREFERENCE.title", Comparators.PREFERENCE_COMPARATOR,
          (person, campaign) -> person.isChild(campaign.getLocalDate()) ? null : person,
          PersonnelTableModelColumn::getPreference),
    QUICK_TRAIN_IGNORE("Column.QUICK_TRAIN_IGNORE.title", Comparators.YES_NO_NA_COMPARATOR,
          Person::isQuickTrainIgnore, PersonnelTableModelColumn::convertBooleanToYesNoNA),
    SALVAGE_SUPERVISOR("Column.SALVAGE_SUPERVISOR.title", Comparators.YES_NO_NA_COMPARATOR,
          Person::isSalvageSupervisor, PersonnelTableModelColumn::convertBooleanToYesNoNA),
    WANTS_CHILDREN("Column.WANTS_CHILDREN.title", Comparators.WANTS_CHILDREN_COMPARATOR,
          (person, campaign) -> person.isChild(campaign.getLocalDate()) ? null : person,
          PersonnelTableModelColumn::getProcreationStatus),
    UNDER_PROTECTION("Column.UNDER_PROTECTION.title", Comparators.YES_NO_NA_COMPARATOR,
          Person::isUnderProtection, PersonnelTableModelColumn::convertBooleanToYesNoNA),
    COVER_MEDICAL_EXPENSES("Column.COVER_MEDICAL_EXPENSES.title", Comparators.YES_NO_NA_COMPARATOR,
          Person::isCoverIllicitMedicalExpenses, PersonnelTableModelColumn::convertBooleanToYesNoNA),
    BLOCK_MATERNITY_LEAVE("Column.BLOCK_MATERNITY_LEAVE.title", Comparators.YES_NO_NA_COMPARATOR,
          Person::isBlockMaternityLeave, PersonnelTableModelColumn::convertBooleanToYesNoNA),
    TOUGHNESS("Column.TOUGHNESS.title", Comparators.INT_COMPARATOR,
          Person::getAdjustedToughness, Object::toString),
    CONNECTIONS("Column.CONNECTIONS.title", fieldBasedSorter(person -> person.getAdjustedConnections(true)),
          person -> person, person -> {
        if (person.getBurnedConnectionsEndDate() != null) {
            return "<html><b><font color='gray'>" + person.getAdjustedConnections(true) + "</font></b></html>";
        }
        return Integer.toString(person.getAdjustedConnections(true));
    }),
    WEALTH("Column.WEALTH.title", Comparators.INT_COMPARATOR,
          Person::getWealth, Object::toString),
    EXTRA_INCOME("Column.EXTRA_INCOME.title", Comparators.INT_COMPARATOR,
          Person::getExtraIncomeTraitLevel, Object::toString),
    REPUTATION("Column.REPUTATION.title", Comparators.INT_COMPARATOR,
          (person, campaign) -> person.getAdjustedReputation(campaign.getCampaignOptions().isUseAgeEffects(),
                campaign.isClanCampaign(), campaign.getLocalDate(), person.getRankNumeric()), Object::toString),
    UNLUCKY("Column.UNLUCKY.title", Comparators.INT_COMPARATOR,
          Person::getUnlucky, Object::toString),
    BLOODMARK("Column.BLOODMARK.title", Comparators.INT_COMPARATOR,
          Person::getBloodmark, Object::toString),
    FATIGUE("Column.FATIGUE.title", Comparators.INT_COMPARATOR,
          Fatigue::getEffectiveFatigue, Object::toString),
    SPA_COUNT("Column.SPA_COUNT.title", Comparators.INT_COMPARATOR,
          person -> person.countOptions(PersonnelOptions.LVL3_ADVANTAGES), Object::toString),
    MODIFICATION_COUNT("Column.MODIFICATION_COUNT.title", Comparators.INT_COMPARATOR,
          person -> person.getProstheticInjuries().size(), Object::toString),
    IMPLANT_COUNT("Column.IMPLANT_COUNT.title", Comparators.INT_COMPARATOR,
          person -> person.countOptions(PersonnelOptions.MD_ADVANTAGES), Object::toString),
    LOYALTY("Column.LOYALTY.title", Comparators.INT_COMPARATOR,
          (person, campaign) -> person.getAdjustedLoyalty(campaign.getFaction(),
                campaign.getCampaignOptions().isUseAlternativeAdvancedMedical()), Object::toString),
    HIGHEST_EDUCATION("Column.HIGHEST_EDUCATION.title", fieldBasedSorter(EducationLevel::getLevel),
          Person::getEduHighestEducation, Object::toString),
    CURRENT_EDUCATION("Column.CURRENT_EDUCATION.title", fieldBasedSorter(EducationLevel::getLevel),
          person -> {
              Academy academy = EducationController.getAcademy(person.getEduAcademySet(),
                    person.getEduAcademyNameInSet());
              return (academy == null) ? null : EducationLevel.fromLevel(academy.getEducationLevel(person));
          },
          level -> (level == null) ? "" : level.toString()),
    ACADEMY("Column.ACADEMY.title", Comparators.STRING_COMPARATOR,
          person -> {
              Academy currentAcademy = EducationController.getAcademy(person.getEduAcademySet(),
                    person.getEduAcademyNameInSet());
              return currentAcademy == null ? "" : currentAcademy.getName();
          }),
    COURSE("Column.COURSE.title", Comparators.STRING_COMPARATOR,
          person -> {
              Academy currentAcademy = EducationController.getAcademy(person.getEduAcademySet(),
                    person.getEduAcademyNameInSet());
              return currentAcademy == null ? "" : currentAcademy.getQualifications().get(person.getEduCourseIndex());
          }),
    ACADEMY_DURATION("Column.ACADEMY_DURATION.title", Comparators.INT_COMPARATOR,
          person -> {
              Academy currentAcademy = EducationController.getAcademy(person.getEduAcademySet(),
                    person.getEduAcademyNameInSet());
              return currentAcademy == null ? null : person.getEduEducationTime();
          }, time -> (time == null) ? "" : time.toString()),
    AGGRESSION("Column.AGGRESSION.title", PersonalityTraitSorter.INSTANCE,
          Person::getAggression, PersonnelTableModelColumn::traitToText),
    AMBITION("Column.AMBITION.title", PersonalityTraitSorter.INSTANCE,
          Person::getAmbition, PersonnelTableModelColumn::traitToText),
    GREED("Column.GREED.title", PersonalityTraitSorter.INSTANCE,
          Person::getGreed, PersonnelTableModelColumn::traitToText),
    SOCIAL("Column.SOCIAL.title", PersonalityTraitSorter.INSTANCE,
          Person::getSocial, PersonnelTableModelColumn::traitToText),
    REASONING("Column.REASONING.title", fieldBasedSorter(Reasoning::getLevel),
          Person::getReasoning, Reasoning::getLabel),
    STRENGTH("Column.STRENGTH.title", SkillAttributeCell.COMPARATOR,
          attributeExtractor(SkillAttribute.STRENGTH), Object::toString),
    BODY("Column.BODY.title", SkillAttributeCell.COMPARATOR,
          attributeExtractor(SkillAttribute.BODY), Object::toString),
    REFLEXES("Column.REFLEXES.title", SkillAttributeCell.COMPARATOR,
          attributeExtractor(SkillAttribute.REFLEXES), Object::toString),
    DEXTERITY("Column.DEXTERITY.title", SkillAttributeCell.COMPARATOR,
          attributeExtractor(SkillAttribute.DEXTERITY), Object::toString),
    INTELLIGENCE("Column.INTELLIGENCE.title", SkillAttributeCell.COMPARATOR,
          attributeExtractor(SkillAttribute.INTELLIGENCE), Object::toString),
    WILLPOWER("Column.WILLPOWER.title", SkillAttributeCell.COMPARATOR,
          attributeExtractor(SkillAttribute.WILLPOWER), Object::toString),
    CHARISMA("Column.CHARISMA.title", SkillAttributeCell.COMPARATOR,
          attributeExtractor(SkillAttribute.CHARISMA), Object::toString),
    EDGE("Column.EDGE.title", SkillAttributeCell.COMPARATOR,
          attributeExtractor(SkillAttribute.EDGE), Object::toString),
    SHIP_TRANSPORT("Column.SHIP_TRANSPORT.title", Comparators.STRING_COMPARATOR,
          person -> {
              if (person.getUnit() == null || person.getUnit().getTransportShipAssignment() == null) {
                  return "-";
              }
              return person.getUnit().getTransportShipAssignment().getTransportShip().getName();
          }),
    TACTICAL_TRANSPORT("Column.TACTICAL_TRANSPORT.title", Comparators.STRING_COMPARATOR,
          person -> {
              if (person.getUnit() == null || person.getUnit().getTacticalTransportAssignment() == null) {
                  return "-";
              }
              return person.getUnit().getTacticalTransportAssignment().getTransport().getName();
          }),
    LOCATION_SYSTEM_AND_PLANET("Column.LOCATION_SYSTEM_AND_PLANET.title", Comparators.STRING_COMPARATOR,
          PersonnelTableModelColumn::getLocationSystemAndPlanet),
    LOCATION_NAME("Column.LOCATION_NAME.title", Comparators.STRING_COMPARATOR,
          (person, campaign) -> LocationDisplay.getLocationName(person, campaign, campaign.getLocalDate())),
    DESTINATION_SYSTEM_AND_PLANET("Column.DESTINATION_SYSTEM_AND_PLANET.title", Comparators.STRING_COMPARATOR,
          PersonnelTableModelColumn::getDestinationSystemAndPlanet),
    DESTINATION_NAME("Column.DESTINATION_NAME.title", Comparators.STRING_COMPARATOR,
          (person, campaign) -> LocationDisplay.getDestinationName(person, campaign, campaign.getLocalDate())),
    IS_MARRIED("Column.IS_MARRIED.title", Comparators.YES_NO_NA_COMPARATOR,
          person -> person.getGenealogy().hasSpouse(), PersonnelTableModelColumn::convertBooleanToYesNoNA),
    FORMER_SPOUSES("Column.FORMER_SPOUSES.title", Comparators.INT_COMPARATOR,
          person -> person.getGenealogy().getFormerSpouses().size(), Object::toString),
    IMMEDIATE_FAMILY("Column.IMMEDIATE_FAMILY.title", Comparators.INT_COMPARATOR,
          PersonnelTableModelColumn::getImmediateFamilySize, Object::toString),
    EXTENDED_FAMILY("Column.EXTENDED_FAMILY.title", Comparators.INT_COMPARATOR,
          person -> person.getGenealogy().getChildren().size(), Object::toString),
    TOTAL_RELATIVES("Column.TOTAL_RELATIVES.title", Comparators.INT_COMPARATOR,
          person -> getImmediateFamilySize(person) + getExtendedFamilySize(person), Object::toString);

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
     * Defines a personnel table column model, how it's sorted and visualised. This is a simplified column
     * implementation that only depends on {@link Person}. See
     * {@link #PersonnelTableModelColumn(String, Comparator, BiFunction)}.
     */
    <Model> PersonnelTableModelColumn(String name, Comparator<Model> modelComparator,
          Function<Person, Model> modelExtractor, Function<Model, String> modelToText) {
        this(name, modelComparator, (person, campaign) -> modelExtractor.apply(person), modelToText);
    }

    /**
     * Defines a personnel table column model, how it's sorted and visualised. This is a simplified column
     * implementation based on the String model. See
     * {@link #PersonnelTableModelColumn(String, Comparator, BiFunction)}.
     */
    PersonnelTableModelColumn(String name, Comparator<String> modelComparator,
          BiFunction<Person, Campaign, String> modelExtractor) {
        this(name, modelComparator, modelExtractor, string -> string);
    }

    /**
     * Defines a personnel table column model, how it's sorted and visualised. This is a simplified column
     * implementation based on the String model that only depends on {@link Person}. See
     * {@link #PersonnelTableModelColumn(String, Comparator, BiFunction)}.
     */
    PersonnelTableModelColumn(String name, Comparator<String> modelComparator,
          Function<Person, String> modelExtractor) {
        this(name, modelComparator, (person, campaign) -> modelExtractor.apply(person), string -> string);
    }

    public Comparator<?> getComparator() {
        return modelComparator;
    }

    private static String convertBooleanToYesNoNA(Boolean yesNoValue) {
        if (yesNoValue == null) {
            return MHQInternationalization.getText("NA.text");
        }
        return MHQInternationalization.getText(yesNoValue ? "Yes.text" : "No.text");
    }

    public Object getCellValue(Campaign campaign, Person person) {
        return modelExtractor.apply(person, campaign);
    }

    private static String getFormattedTextAt(String key, Object... args) {
        return MHQInternationalization.getFormattedTextAt(RESOURCE_BUNDLE, key, args);
    }

    private static String getTextAt(String key) {
        return MHQInternationalization.getTextAt(RESOURCE_BUNDLE, key);
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

    private static String getForceTextGraphical(Person person, Campaign campaign) {
        Formation formation = campaign.getFormationFor(person);
        if (formation != null) {
            StringBuilder desc = new StringBuilder("<html><b>").append(formation.getName()).append("</b>");
            Formation parent = formation.getParentFormation();
            // cut off after three lines and don't include the top level
            int lines = 1;
            while ((parent != null) && (parent.getParentFormation() != null) && (lines < 4)) {
                desc.append("<br>").append(parent.getName());
                lines++;
                parent = parent.getParentFormation();
            }
            desc.append("</html>");
            return desc.toString();
        }
        return "-";
    }

    private static String getDeployedScenarioName(Person person, Campaign campaign) {
        Unit unit = person.getUnit();
        if (unit == null || !unit.isDeployed()) {
            return "-";
        }
        Scenario scenario = campaign.getScenario(unit.getScenarioId());
        return scenario == null ? "-" : scenario.getName();
    }

    private static BiFunction<Person, Campaign, Integer> skillModelExtractor(String skillName) {
        return (person, campaign) -> getSkillValue(person, campaign).apply(skillName);
    }

    private static BiFunction<Person, Campaign, SkillPair> skillPairModelExtractor(
          String gunnerySkill, String pilotingSkill) {
        return (person, campaign) -> {
            Function<String, Integer> skillValue = getSkillValue(person, campaign);
            return new SkillPair(skillValue.apply(gunnerySkill), gunnerySkill,
                  skillValue.apply(pilotingSkill), pilotingSkill);
        };
    }

    private static String getCommandStatus(Person person) {
        if (person.isCommander()) {
            return getTextAt("Cell.COMMAND_STATUS.text.commander");
        }
        if (person.isSecondInCommand()) {
            return getTextAt("Cell.COMMAND_STATUS.text.secondInCommand");
        }
        return "";
    }

    private static int getImmediateFamilySize(Person person) {
        Genealogy genealogy = person.getGenealogy();
        return genealogy.getChildren().size() + genealogy.getSiblingCount() + genealogy.getParentsCount();
    }

    private static int getExtendedFamilySize(Person person) {
        Genealogy genealogy = person.getGenealogy();
        return genealogy.getGrandchildrenCount() + genealogy.getGrandparentsCount() +
                     genealogy.getAuntsAndUnclesCount() + genealogy.getCousinsCount();
    }

    private static String getProcreationStatus(Person person) {
        if (person == null) {
            return MHQInternationalization.getText("NA.text");
        } else if (person.getDueDate() != null) {
            String formattedDate = MekHQ.getMHQOptions().getDisplayFormattedDate(person.getDueDate());
            return getFormattedTextAt("Cell.WANTS_CHILDREN.dueText", formattedDate);
        }
        return convertBooleanToYesNoNA(person.isWantsChildren());
    }

    private static String getPreference(Person person) {
        if (person == null) {
            return MHQInternationalization.getText("NA.text");
        } else if (person.isPrefersMen() && person.isPrefersWomen()) {
            return getTextAt("Cell.PREFERS.text.any");
        } else if (person.isPrefersWomen()) {
            return getTextAt("Cell.PREFERS.text.women");
        } else if (person.isPrefersMen()) {
            return getTextAt("Cell.PREFERS.text.men");
        }
        return getTextAt("Cell.PREFERS.text.none");
    }

    private static String getOrigin(Person person, Campaign campaign) {
        StringBuilder result = new StringBuilder();
        if (person.getOriginFaction() == null) {
            result.append("-");
        } else {
            result.append(person.getOriginFaction().getFullName(campaign.getGameYear()));
        }
        Planet originPlanet = person.getOriginPlanet();
        if (originPlanet != null) {
            result.append(" (").append(originPlanet.getName(campaign.getLocalDate())).append(")");
        }
        return result.toString();
    }

    private static String getLocationSystemAndPlanet(Person person, Campaign campaign) {
        String locationPlanet = LocationDisplay.getLocationPlanet(person, campaign.getLocalDate(), campaign);
        String locationSystem = LocationDisplay.getLocationSystem(person, campaign.getLocalDate(), campaign);
        if (locationSystem.equals(locationPlanet)) {
            return locationPlanet;
        }
        return locationSystem + " - " + locationPlanet;
    }

    private static String getDestinationSystemAndPlanet(Person person, Campaign campaign) {
        String locationPlanet = LocationDisplay.getDestinationPlanet(person, campaign.getLocalDate());
        String locationSystem = LocationDisplay.getDestinationSystem(person, campaign.getLocalDate());
        if (locationSystem.equals(locationPlanet)) {
            return locationPlanet;
        }
        return locationSystem + " - " + locationPlanet;
    }

    private static String getAggregateCombatSkillValue(Person person, Campaign campaign) {
        Function<String, String> skillValue = getStringSkillValue(person, campaign);
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
            case PersonnelRole.BATTLE_ARMOUR -> skillValue.apply(SkillType.S_GUN_BA);
            case PersonnelRole.SOLDIER -> {
                String gunnerySkill = InfantryGunnerySkills.getBestInfantryGunnerySkill(person,
                      campaignOptions.isUseSmallArmsOnly());
                yield skillValue.apply(gunnerySkill) + '/' + skillValue.apply(SkillType.S_ANTI_MEK);
            }
            case PersonnelRole.PROTOMEK_PILOT -> skillValue.apply(SkillType.S_GUN_PROTO);
            default -> "-/-";
        };
    }

    private static SkillPair getAggregateTechSkillValue(Person person, Campaign campaign) {
        Function<String, Integer> skillValue = getSkillValue(person, campaign);
        PersonnelRole primaryProfession = person.getPrimaryRole();
        PersonnelRole secondaryProfession = person.getSecondaryRole();
        PersonnelRole profession = primaryProfession.isTech() ? primaryProfession : secondaryProfession;
        return switch (profession) {
            case PersonnelRole.MEK_TECH ->
                  new SkillPair(skillValue.apply(SkillType.S_TECH_MEK), SkillType.S_TECH_MEK,
                        skillValue.apply(SkillType.S_ZERO_G_OPERATIONS), SkillType.S_ZERO_G_OPERATIONS);
            case PersonnelRole.BA_TECH ->
                  new SkillPair(skillValue.apply(SkillType.S_TECH_BA), SkillType.S_TECH_BA,
                        skillValue.apply(SkillType.S_ZERO_G_OPERATIONS), SkillType.S_ZERO_G_OPERATIONS);
            case PersonnelRole.MECHANIC ->
                  new SkillPair(skillValue.apply(SkillType.S_TECH_MECHANIC), SkillType.S_TECH_MECHANIC,
                        skillValue.apply(SkillType.S_ZERO_G_OPERATIONS), SkillType.S_ZERO_G_OPERATIONS);
            case PersonnelRole.AERO_TEK ->
                  new SkillPair(skillValue.apply(SkillType.S_TECH_AERO), SkillType.S_TECH_AERO,
                        skillValue.apply(SkillType.S_ZERO_G_OPERATIONS), SkillType.S_ZERO_G_OPERATIONS);
            case PersonnelRole.VESSEL_CREW ->
                  new SkillPair(skillValue.apply(SkillType.S_TECH_VESSEL), SkillType.S_TECH_VESSEL,
                        skillValue.apply(SkillType.S_ZERO_G_OPERATIONS), SkillType.S_ZERO_G_OPERATIONS);
            default ->
                  new SkillPair(null, SkillType.S_TECH_MEK, null, SkillType.S_ZERO_G_OPERATIONS);
        };
    }

    private static String getUnitAssignmentGraphical(Person person) {
        Unit unit = person.getUnit();
        if ((unit == null) && !person.getTechUnits().isEmpty()) {
            unit = person.getTechUnits().getFirst();
        }

        if (unit != null) {
            String description = "<html><b>" + unit.getName() + "</b><br>";
            description += unit.getEntity().getWeightClassName();
            if ((!(unit.getEntity() instanceof SmallCraft) || !(unit.getEntity() instanceof Jumpship))) {
                description += " " + UnitType.getTypeDisplayableName(unit.getEntity().getUnitType());
            }
            description += "<br>" + unit.getStatus() + "</html>";
            return description;
        }
        return "-";
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

    private static String getTechUnitAssignment(Person person) {
        int maintainedUnitCount = person.getTechUnits().size();
        if (maintainedUnitCount > 0) {
            List<String> assignments = person.getTechUnits().stream().map(unit ->
                unit.getName() + ((unit.isRefitting() && unit.getRefit().getTech() == person) ?
                                        getTextAt("Cell.TECH_UNIT_ASSIGNMENT.text.refit") : "")
            ).toList();
            return "<html>" + String.join(", ", assignments) + "</html>";
        }
        return "";
    }

    private static @NonNull Function<String, Integer> getSkillValue(Person person, Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        SkillModifierData skillModifierData = person.getSkillModifierData(
              campaignOptions.isUseAgeEffects(), campaign.isClanCampaign(), campaign.getLocalDate(), true);
        return skillName -> (skillName == null) || !person.hasSkill(skillName) ? null :
                                  person.getSkill(skillName).getFinalSkillValue(skillModifierData);
    }

    private static @NonNull Function<String, String> getStringSkillValue(Person person, Campaign campaign) {
        return skillName -> skillToText(getSkillValue(person, campaign).apply(skillName));
    }

    private static Function<Person, SkillAttributeCell> attributeExtractor(SkillAttribute attribute) {
        return person -> new SkillAttributeCell(person.getAttributeScore(attribute), person.getAttributeCap(attribute),
              attribute != SkillAttribute.EDGE);
    }

    private static String traitToText(PersonalityTrait trait) {
        if (trait.isNone()) {
            return trait.toString();
        }
        String sign = trait.isTraitPositive() ? "+" : "-";
        return trait + " (" + (trait.isTraitMajor() ? sign + sign : sign) + ')';
    }

    private static String skillToText(Integer skill) {
        return (skill == null) ? "-" : skill.toString();
    }

    /**
     * Returns the tooltip text for this column, optionally including color reason explanations.
     *
     * @param person             the person for this row
     * @param personalStateFlags list of i18n keys for color reasons, or null/empty if no special coloring
     *
     * @return the tooltip text, or null if no tooltip
     */
    public @Nullable String getToolTipText(Person person, java.util.List<String> personalStateFlags) {
        String baseTooltip = getBaseToolTipText(person);

        // For name, rank, and status columns, append color reasons if present
        if (!personalStateFlags.isEmpty() && isNameRankOrStatusColumn()) {
            StringBuilder colorReasons = new StringBuilder();
            for (String key : personalStateFlags) {
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
        return (this == PERSON_GRAPHICAL) ||
                     (this == FIRST_NAME) ||
                     (this == LAST_NAME) ||
                     (this == GIVEN_NAME) ||
                     (this == SURNAME) ||
                     (this == SURNAME_GROUPED_BY_UNIT) ||
                     (this == BLOODNAME) ||
                     (this == RANK) ||
                     (this == PERSONNEL_STATUS);
    }

    /**
     * Returns optional preferred size.
     * @return null if the column has no size preference, a preferred width otherwise.
     */
    @Nullable
    public Integer getPreferredWidth() {
        Integer preferredWidth = switch (this) {
            case RANK -> 60;
            case FIRST_NAME -> 50;
            case LAST_NAME -> 70;
            case SKILL_LEVEL -> 45;
            case PERSONNEL_ROLE -> 120;
            case UNIT_ASSIGNMENT -> 140;
            case ORIGIN -> 160;
            case TECH_UNIT_ASSIGNMENT -> 280;
            default -> null;
        };
        return (preferredWidth == null) ? null : UIUtil.scaleForGUI(preferredWidth);
    }

    public int getAlignment() {
        return switch (this) {
            case RANK, SKILL_LEVEL -> SwingConstants.LEFT;
            case SALARY, MAX_TECH_MINUTES -> SwingConstants.RIGHT;
            default -> {
                if (modelComparator.equals(Comparators.STRING_COMPARATOR)) {
                    yield SwingConstants.LEFT;
                }
                yield SwingConstants.CENTER;
            }
        };
    }

    public @Nullable SortOrder getDefaultSortOrder() {
        return switch (this) {
            case RANK, FIRST_NAME, LAST_NAME, SKILL_LEVEL -> SortOrder.DESCENDING;
            default -> null;
        };
    }

    /**
     * Generates a {@link Comparator} that orders elements naturally based on a field.
     *
     * @param fieldExtractor the field to be used for ordering
     */
    private static <T, U extends Comparable<? super U>> Comparator<T> fieldBasedSorter(Function<T, U> fieldExtractor) {
        return Comparator.nullsLast(Comparator.comparing(fieldExtractor));
    }

    @Override
    public String toString() {
        return name;
    }

    private static class Comparators {
        private static final Comparator<Integer> SKILL_COMPARATOR = Comparator.reverseOrder();
        private static final Comparator<Integer> INT_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());
        private static final Comparator<String> STRING_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());
        private static final Comparator<LocalDate> DATE_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());
        private static final Comparator<Boolean> YES_NO_NA_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());
        private static final Comparator<Person> WANTS_CHILDREN_COMPARATOR =
              Comparator.nullsLast(
                    Comparator.comparing(Person::getDueDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                          .thenComparing(Person::isWantsChildren));
        private static final Comparator<Person> PREFERENCE_COMPARATOR =
              Comparator.nullsLast(Comparator.comparing(Person::isPrefersMen).thenComparing(Person::isPrefersWomen));
        private static final Comparator<Person> COMMAND_STATUS_COMPARATOR =
              Comparator.nullsLast(Comparator.comparing(Person::isCommander)
                                         .thenComparing(Person::isSecondInCommand));
    }

    /**
     * Models cells that display attributes.
     */
    private record SkillAttributeCell(int value, int cap, boolean showModifier) {
        // sort by value, then cap
        private static final Comparator<SkillAttributeCell> COMPARATOR =
              Comparator.comparing(SkillAttributeCell::value).thenComparing(SkillAttributeCell::cap);

        @Override
        @NonNull
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(value).append(" / ").append(cap);
            if (showModifier) {
                int attributeModifier = Skill.getIndividualAttributeModifier(value);
                result.append(" (").append(attributeModifier >= 0 ? "+" : "").append(attributeModifier).append(")");
            }
            return result.toString();
        }
    }

    /**
     * Models cells that display two skills simultaneously.
     */
    private record SkillPair(@Nullable Integer primaryValue, String primaryName,
          @Nullable Integer secondaryValue, String secondaryName) {

        // sort by sum, then primary skill value
        private static final Comparator<SkillPair> COMPARATOR =
              Comparator.comparing(SkillPair::getValueSum, Comparator.reverseOrder())
                    .thenComparing(SkillPair::primaryValue, Comparator.nullsFirst(Comparator.reverseOrder()));


        private int getValueSum() {
            int primary = primaryValue == null ? (SkillType.getType(primaryName).getTarget() + 1) : primaryValue;
            int secondary = secondaryValue == null ?
                                  (SkillType.getType(secondaryName).getTarget() + 1) :
                                  secondaryValue;
            return primary + secondary;
        }

        @Override
        @NonNull
        public String toString() {
            String primaryString = primaryValue == null ? "-" : primaryValue.toString();
            String secondaryString = secondaryValue == null ? "-" : secondaryValue.toString();
            return primaryString + '/' + secondaryString;
        }
    }
}
