/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.campaignOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import jakarta.annotation.Nonnull;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.digitalGM.stratCon.StratConPlayType;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.randomEvents.prisoners.PrisonerCaptureStyle;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;
import mekhq.service.mrms.MRMSOption;

/**
 * A typed key identifying a single campaign option.
 *
 * <p>Each constant carries the option's value type ({@link #type()}), its default value
 * ({@link #defaultValue()}), and the XML tag under which it is persisted ({@link #xmlTag()}). Because the key itself is
 * parameterized by {@code T}, a heterogeneous store keyed on {@code CampaignOption} can offer a single, fully
 * compile-time-checked accessor pair ({@code get}/{@code set}) without casts or type tokens at the call site (the
 * typesafe heterogeneous container pattern, <i>Effective Java</i> Item 33).</p>
 *
 * <p>A Java {@code enum} cannot be generic per-constant, so this is a final class of
 * {@code public static final} constants rather than an enum. Constants self-register into {@link #values()} on creation
 * so a store can seed every managed option from its declared default.</p>
 *
 * @param <T> the type of the option's value
 */
public final class CampaignOption<T> {
    private static final List<CampaignOption<?>> ALL = new ArrayList<>();

    // region Repair and Maintenance
    public static final CampaignOption<Boolean> CHECK_MAINTENANCE =
          of(Boolean.class, true, "checkMaintenance");
    public static final CampaignOption<Integer> MAINTENANCE_CYCLE_DAYS =
          of(Integer.class, 7, "maintenanceCycleDays");
    public static final CampaignOption<Integer> MAINTENANCE_BONUS =
          of(Integer.class, -1, "maintenanceBonus");
    public static final CampaignOption<Boolean> USE_QUALITY_MAINTENANCE =
          of(Boolean.class, true, "useQualityMaintenance");
    public static final CampaignOption<Boolean> REVERSE_QUALITY_NAMES =
          of(Boolean.class, false, "reverseQualityNames");
    public static final CampaignOption<Boolean> USE_RANDOM_UNIT_QUALITIES =
          of(Boolean.class, true, "useRandomUnitQualities");
    public static final CampaignOption<Boolean> USE_UNOFFICIAL_MAINTENANCE =
          of(Boolean.class, false, "useUnofficialMaintenance");
    public static final CampaignOption<Boolean> LOG_MAINTENANCE =
          of(Boolean.class, false, "logMaintenance");
    public static final CampaignOption<Integer> DEFAULT_MAINTENANCE_TIME =
          of(Integer.class, 4, "defaultMaintenanceTime");
    // endregion Repair and Maintenance

    // region Migrated Options (generated storage-only migration)
    public static final CampaignOption<Boolean> REQUIRE_SUPPORT_FORCE_TRANSPORTATION =
          of(Boolean.class, true, "requireSupportForceTransportation");
    public static final CampaignOption<Integer> MANUAL_UNIT_RATING_MODIFIER =
          of(Integer.class, 0, "manualUnitRatingModifier");
    public static final CampaignOption<Boolean> CLAMP_REPUTATION_PAY_MULTIPLIER =
          of(Boolean.class, false, "clampReputationPayMultiplier");
    public static final CampaignOption<Boolean> REDUCE_REPUTATION_PERFORMANCE_MODIFIER =
          of(Boolean.class, false, "reduceReputationPerformanceModifier");
    public static final CampaignOption<Boolean> REPUTATION_PERFORMANCE_MODIFIER_CUT_OFF =
          of(Boolean.class, false, "reputationPerformanceModifierCutOff");
    public static final CampaignOption<Boolean> USE_ERA_MODS =
          of(Boolean.class, false, "useEraMods");
    public static final CampaignOption<Boolean> ASSIGNED_TECH_FIRST =
          of(Boolean.class, false, "assignedTechFirst");
    public static final CampaignOption<Boolean> RESET_TO_FIRST_TECH =
          of(Boolean.class, false, "resetToFirstTech");
    public static final CampaignOption<Boolean> TECHS_USE_ADMINISTRATION =
          of(Boolean.class, false, "techsUseAdministration");
    public static final CampaignOption<Boolean> USE_USEFUL_AS_TECHS =
          of(Boolean.class, false, "useUsefulAsTechs");
    public static final CampaignOption<Boolean> USE_QUIRKS =
          of(Boolean.class, false, "useQuirks");
    public static final CampaignOption<Boolean> USE_AERO_SYSTEM_HITS =
          of(Boolean.class, false, "useAeroSystemHits");
    public static final CampaignOption<Boolean> DESTROY_BY_MARGIN =
          of(Boolean.class, false, "destroyByMargin");
    public static final CampaignOption<Integer> DESTROY_MARGIN =
          of(Integer.class, 4, "destroyMargin");
    public static final CampaignOption<Integer> DESTROY_PART_TARGET =
          of(Integer.class, 10, "destroyPartTarget");
    public static final CampaignOption<Boolean> USE_PLANETARY_MODIFIERS =
          of(Boolean.class, true, "usePlanetaryModifiers");
    public static final CampaignOption<Boolean> MRMS_USE_REPAIR =
          of(Boolean.class, true, "mrmsUseRepair");
    public static final CampaignOption<Boolean> MRMS_USE_SALVAGE =
          of(Boolean.class, true, "mrmsUseSalvage");
    public static final CampaignOption<Boolean> MRMS_USE_EXTRA_TIME =
          of(Boolean.class, true, "mrmsUseExtraTime");
    public static final CampaignOption<Boolean> MRMS_USE_RUSH_JOB =
          of(Boolean.class, true, "mrmsUseRushJob");
    public static final CampaignOption<Boolean> MRMS_ALLOW_CARRYOVER =
          of(Boolean.class, true, "mrmsAllowCarryover");
    public static final CampaignOption<Boolean> MRMS_OPTIMIZE_TO_COMPLETE_TODAY =
          of(Boolean.class, false, "mrmsOptimizeToCompleteToday");
    public static final CampaignOption<Boolean> MRMS_SCRAP_IMPOSSIBLE =
          of(Boolean.class, false, "mrmsScrapImpossible");
    public static final CampaignOption<Boolean> MRMS_USE_ASSIGNED_TECHS_FIRST =
          of(Boolean.class, false, "mrmsUseAssignedTechsFirst");
    public static final CampaignOption<Boolean> MRMS_REPLACE_POD =
          of(Boolean.class, true, "mrmsReplacePod");
    public static final CampaignOption<List<MRMSOption>> MRMS_OPTIONS =
          ofRaw(List.class, () -> new ArrayList<>(), "mrmsOptions");
    public static final CampaignOption<Integer> WAITING_PERIOD =
          of(Integer.class, 7, "waitingPeriod");
    public static final CampaignOption<AcquisitionsType> ACQUISITIONS_TYPE =
          of(AcquisitionsType.class, AcquisitionsType.ANY_TECH, "acquisitionsType");
    public static final CampaignOption<Boolean> USE_FUNCTIONAL_APPRAISAL =
          of(Boolean.class, false, "useFunctionalAppraisal");
    public static final CampaignOption<ProcurementPersonnelPick> ACQUISITION_PERSONNEL_CATEGORY =
          of(ProcurementPersonnelPick.class, ProcurementPersonnelPick.SUPPORT, "acquisitionPersonnelCategory");
    public static final CampaignOption<Integer> CLAN_ACQUISITION_PENALTY =
          of(Integer.class, 0, "clanAcquisitionPenalty");
    public static final CampaignOption<Integer> IS_ACQUISITION_PENALTY =
          of(Integer.class, 0, "isAcquisitionPenalty");
    public static final CampaignOption<Integer> MAX_ACQUISITIONS =
          of(Integer.class, 0, "maxAcquisitions");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_HEAT_SINK =
          of(Integer.class, 50, "autoLogisticsHeatSink");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_MEK_HEAD =
          of(Integer.class, 40, "autoLogisticsMekHead");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_MEK_LOCATION =
          of(Integer.class, 25, "autoLogisticsMekLocation");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_NON_REPAIRABLE_LOCATION =
          of(Integer.class, 0, "autoLogisticsNonRepairableLocation");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_ARMOR =
          of(Integer.class, 100, "autoLogisticsArmor");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_AMMUNITION =
          of(Integer.class, 100, "autoLogisticsAmmunition");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_ACTUATORS =
          of(Integer.class, 100, "autoLogisticsActuators");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_JUMP_JETS =
          of(Integer.class, 50, "autoLogisticsJumpJets");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_ENGINES =
          of(Integer.class, 0, "autoLogisticsEngines");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_WEAPONS =
          of(Integer.class, 50, "autoLogisticsWeapons");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_GYROS =
          of(Integer.class, 0, "autoLogisticsGyros");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_HEAD_COMPONENTS =
          of(Integer.class, 15, "autoLogisticsHeadComponents");
    public static final CampaignOption<Integer> AUTO_LOGISTICS_OTHER =
          of(Integer.class, 0, "autoLogisticsOther");
    public static final CampaignOption<Integer> UNIT_TRANSIT_TIME =
          of(Integer.class, CampaignOptions.TRANSIT_UNIT_MONTH, "unitTransitTime");
    public static final CampaignOption<Boolean> NO_DELIVERIES_IN_TRANSIT =
          of(Boolean.class, false, "noDeliveriesInTransit");
    public static final CampaignOption<Boolean> USE_PLANETARY_ACQUISITION =
          of(Boolean.class, false, "usePlanetaryAcquisition");
    public static final CampaignOption<Integer> MAX_JUMPS_PLANETARY_ACQUISITION =
          of(Integer.class, 2, "maxJumpsPlanetaryAcquisition");
    public static final CampaignOption<PlanetaryAcquisitionFactionLimit> PLANET_ACQUISITION_FACTION_LIMIT =
          of(PlanetaryAcquisitionFactionLimit.class,
                PlanetaryAcquisitionFactionLimit.NEUTRAL,
                "planetAcquisitionFactionLimit");
    public static final CampaignOption<Boolean> PLANET_ACQUISITION_NO_CLAN_CROSSOVER =
          of(Boolean.class, true, "planetAcquisitionNoClanCrossover");
    public static final CampaignOption<Boolean> NO_CLAN_PARTS_FROM_IS =
          of(Boolean.class, true, "noClanPartsFromIS");
    public static final CampaignOption<Integer> PENALTY_CLAN_PARTS_FROM_IS =
          of(Integer.class, 4, "penaltyClanPartsFromIS");
    public static final CampaignOption<Boolean> PLANET_ACQUISITION_VERBOSE =
          of(Boolean.class, false, "planetAcquisitionVerbose");
    public static final CampaignOption<Boolean> LIMIT_BY_YEAR =
          of(Boolean.class, true, "limitByYear");
    public static final CampaignOption<Boolean> DISALLOW_EXTINCT_STUFF =
          of(Boolean.class, false, "disallowExtinctStuff");
    public static final CampaignOption<Boolean> ALLOW_CLAN_PURCHASES =
          of(Boolean.class, true, "allowClanPurchases");
    public static final CampaignOption<Boolean> ALLOW_IS_PURCHASES =
          of(Boolean.class, true, "allowISPurchases");
    public static final CampaignOption<Boolean> ALLOW_CANON_ONLY =
          of(Boolean.class, false, "allowCanonOnly");
    public static final CampaignOption<Boolean> ALLOW_CANON_REFIT_ONLY =
          of(Boolean.class, false, "allowCanonRefitOnly");
    public static final CampaignOption<Integer> TECH_LEVEL =
          of(Integer.class, CampaignOptions.TECH_EXPERIMENTAL, "techLevel");
    public static final CampaignOption<Boolean> VARIABLE_TECH_LEVEL =
          of(Boolean.class, false, "variableTechLevel");
    public static final CampaignOption<Boolean> FACTION_INTRO_DATE =
          of(Boolean.class, false, "factionIntroDate");
    public static final CampaignOption<Boolean> USE_TACTICS =
          of(Boolean.class, false, "useTactics");
    public static final CampaignOption<Boolean> USE_INITIATIVE_BONUS =
          of(Boolean.class, false, "useInitiativeBonus");
    public static final CampaignOption<Boolean> USE_SENSIBLE_TACTICS =
          of(Boolean.class, false, "useSensibleTactics");
    public static final CampaignOption<Boolean> USE_TOUGHNESS =
          of(Boolean.class, false, "useToughness");
    public static final CampaignOption<Boolean> USE_RANDOM_TOUGHNESS =
          of(Boolean.class, false, "useRandomToughness");
    public static final CampaignOption<Boolean> USE_ARTILLERY =
          of(Boolean.class, false, "useArtillery");
    public static final CampaignOption<Boolean> USE_ABILITIES =
          of(Boolean.class, false, "useAbilities");
    public static final CampaignOption<Boolean> ONLY_COMMANDERS_MATTER_VEHICLES =
          of(Boolean.class, false, "onlyCommandersMatterVehicles");
    public static final CampaignOption<Boolean> ONLY_COMMANDERS_MATTER_INFANTRY =
          of(Boolean.class, false, "onlyCommandersMatterInfantry");
    public static final CampaignOption<Boolean> ONLY_COMMANDERS_MATTER_BATTLE_ARMOR =
          of(Boolean.class, false, "onlyCommandersMatterBattleArmor");
    public static final CampaignOption<EdgeRefreshPeriod> EDGE_REFRESH_PERIOD =
          of(EdgeRefreshPeriod.class, EdgeRefreshPeriod.WEEKLY, "edgeRefreshPeriod");
    public static final CampaignOption<Boolean> USE_EDGE =
          of(Boolean.class, false, "useEdge");
    public static final CampaignOption<Boolean> USE_IMPLANTS =
          of(Boolean.class, false, "useImplants");
    public static final CampaignOption<Boolean> ALTERNATIVE_QUALITY_AVERAGING =
          of(Boolean.class, false, "alternativeQualityAveraging");
    public static final CampaignOption<Boolean> USE_AGE_EFFECTS =
          of(Boolean.class, false, "useAgeEffects");
    public static final CampaignOption<Boolean> USE_TRANSFERS =
          of(Boolean.class, true, "useTransfers");
    public static final CampaignOption<Boolean> USE_EXTENDED_TOE_FORCE_NAME =
          of(Boolean.class, false, "useExtendedTOEForceName");
    public static final CampaignOption<Boolean> PERSONNEL_LOG_SKILL_GAIN =
          of(Boolean.class, false, "personnelLogSkillGain");
    public static final CampaignOption<Boolean> PERSONNEL_LOG_ABILITY_GAIN =
          of(Boolean.class, false, "personnelLogAbilityGain");
    public static final CampaignOption<Boolean> PERSONNEL_LOG_EDGE_GAIN =
          of(Boolean.class, false, "personnelLogEdgeGain");
    public static final CampaignOption<Boolean> DISPLAY_PERSONNEL_LOG =
          of(Boolean.class, false, "displayPersonnelLog");
    public static final CampaignOption<Boolean> DISPLAY_SCENARIO_LOG =
          of(Boolean.class, false, "displayScenarioLog");
    public static final CampaignOption<Boolean> DISPLAY_KILL_RECORD =
          of(Boolean.class, false, "displayKillRecord");
    public static final CampaignOption<Boolean> DISPLAY_MEDICAL_RECORD =
          of(Boolean.class, false, "displayMedicalRecord");
    public static final CampaignOption<Boolean> DISPLAY_PATIENT_RECORD =
          of(Boolean.class, false, "displayPatientRecord");
    public static final CampaignOption<Boolean> DISPLAY_ASSIGNMENT_RECORD =
          of(Boolean.class, false, "displayAssignmentRecord");
    public static final CampaignOption<Boolean> DISPLAY_PERFORMANCE_RECORD =
          of(Boolean.class, false, "displayPerformanceRecord");
    public static final CampaignOption<Boolean> AWARD_VETERANCY_SP_AS =
          of(Boolean.class, false, "awardVeterancySPAs");
    public static final CampaignOption<Boolean> AWARD_RELEVANT_VETERANCY_SP_AS =
          of(Boolean.class, false, "awardRelevantVeterancySPAs");
    public static final CampaignOption<Boolean> USE_TIME_IN_SERVICE =
          of(Boolean.class, false, "useTimeInService");
    public static final CampaignOption<TimeInDisplayFormat> TIME_IN_SERVICE_DISPLAY_FORMAT =
          of(TimeInDisplayFormat.class, TimeInDisplayFormat.YEARS, "timeInServiceDisplayFormat");
    public static final CampaignOption<Boolean> USE_TIME_IN_RANK =
          of(Boolean.class, false, "useTimeInRank");
    public static final CampaignOption<TimeInDisplayFormat> TIME_IN_RANK_DISPLAY_FORMAT =
          of(TimeInDisplayFormat.class, TimeInDisplayFormat.MONTHS_YEARS, "timeInRankDisplayFormat");
    public static final CampaignOption<Boolean> TRACK_TOTAL_EARNINGS =
          of(Boolean.class, false, "trackTotalEarnings");
    public static final CampaignOption<Boolean> TRACK_TOTAL_XP_EARNINGS =
          of(Boolean.class, false, "trackTotalXPEarnings");
    public static final CampaignOption<Boolean> SHOW_ORIGIN_FACTION =
          of(Boolean.class, true, "showOriginFaction");
    public static final CampaignOption<Boolean> ADMINS_HAVE_NEGOTIATION =
          of(Boolean.class, false, "adminsHaveNegotiation");
    public static final CampaignOption<Boolean> ADMIN_EXPERIENCE_LEVEL_INCLUDE_NEGOTIATION =
          of(Boolean.class, false, "adminExperienceLevelIncludeNegotiation");
    public static final CampaignOption<Integer> HEAL_WAITING_PERIOD =
          of(Integer.class, 1, "healWaitingPeriod");
    public static final CampaignOption<Integer> NATURAL_HEALING_WAITING_PERIOD =
          of(Integer.class, 15, "naturalHealingWaitingPeriod");
    public static final CampaignOption<Integer> MINIMUM_HITS_FOR_VEHICLES =
          of(Integer.class, 1, "minimumHitsForVehicles");
    public static final CampaignOption<Boolean> USE_RANDOM_HITS_FOR_VEHICLES =
          of(Boolean.class, false, "useRandomHitsForVehicles");
    public static final CampaignOption<Boolean> TOUGHER_HEALING =
          of(Boolean.class, false, "tougherHealing");
    public static final CampaignOption<Boolean> USE_ALTERNATIVE_ADVANCED_MEDICAL =
          of(Boolean.class, false, "useAlternativeAdvancedMedical");
    public static final CampaignOption<Boolean> USE_ALTERNATIVE_ADVANCED_MEDICAL_FEWER_PERMANENT_INJURIES =
          of(Boolean.class, false, "useAlternativeAdvancedMedicalFewerPermanentInjuries");
    public static final CampaignOption<Double> ALTERNATIVE_ADVANCED_MEDICAL_HEALING_TIME_MULTIPLIER =
          of(Double.class, 1.0, "alternativeAdvancedMedicalHealingTimeMultiplier");
    public static final CampaignOption<Boolean> USE_RANDOM_DISEASES =
          of(Boolean.class, false, "useRandomDiseases");
    public static final CampaignOption<Integer> MAXIMUM_PATIENTS =
          of(Integer.class, 25, "maximumPatients");
    public static final CampaignOption<Boolean> DOCTORS_USE_ADMINISTRATION =
          of(Boolean.class, false, "doctorsUseAdministration");
    public static final CampaignOption<Boolean> USE_USEFUL_MEDICS =
          of(Boolean.class, false, "useUsefulMedics");
    public static final CampaignOption<Boolean> USE_MASH_THEATRES =
          of(Boolean.class, false, "useMASHTheatres");
    public static final CampaignOption<Integer> MASH_THEATRE_CAPACITY =
          of(Integer.class, 25, "mashTheatreCapacity");
    public static final CampaignOption<Boolean> USE_BLOB_INFANTRY =
          of(Boolean.class, false, "useBlobInfantry");
    public static final CampaignOption<Boolean> USE_BLOB_BATTLE_ARMOR =
          of(Boolean.class, false, "useBlobBattleArmor");
    public static final CampaignOption<Boolean> USE_BLOB_VEHICLE_CREW_GROUND =
          of(Boolean.class, false, "useBlobVehicleCrewGround");
    public static final CampaignOption<Boolean> USE_BLOB_VEHICLE_CREW_VTOL =
          of(Boolean.class, false, "useBlobVehicleCrewVTOL");
    public static final CampaignOption<Boolean> USE_BLOB_VEHICLE_CREW_NAVAL =
          of(Boolean.class, false, "useBlobVehicleCrewNaval");
    public static final CampaignOption<Boolean> USE_BLOB_VESSEL_PILOT =
          of(Boolean.class, false, "useBlobVesselPilot");
    public static final CampaignOption<Boolean> USE_BLOB_VESSEL_GUNNER =
          of(Boolean.class, false, "useBlobVesselGunner");
    public static final CampaignOption<Boolean> USE_BLOB_VESSEL_CREW =
          of(Boolean.class, false, "useBlobVesselCrew");
    public static final CampaignOption<PrisonerCaptureStyle> PRISONER_CAPTURE_STYLE =
          of(PrisonerCaptureStyle.class, PrisonerCaptureStyle.NONE, "prisonerCaptureStyle");
    public static final CampaignOption<Boolean> USE_FUNCTIONAL_ESCAPE_ARTIST =
          of(Boolean.class, false, "useFunctionalEscapeArtist");
    public static final CampaignOption<Boolean> USE_RANDOM_DEPENDENT_ADDITION =
          of(Boolean.class, false, "useRandomDependentAddition");
    public static final CampaignOption<Boolean> USE_RANDOM_DEPENDENT_REMOVAL =
          of(Boolean.class, false, "useRandomDependentRemoval");
    public static final CampaignOption<Integer> DEPENDENT_PROFESSION_DIE_SIZE =
          of(Integer.class, 4, "dependentProfessionDieSize");
    public static final CampaignOption<Integer> CIVILIAN_PROFESSION_DIE_SIZE =
          of(Integer.class, 2, "civilianProfessionDieSize");
    public static final CampaignOption<Boolean> USE_PERSONNEL_REMOVAL =
          of(Boolean.class, false, "usePersonnelRemoval");
    public static final CampaignOption<Boolean> USE_REMOVAL_EXEMPT_CEMETERY =
          of(Boolean.class, false, "useRemovalExemptCemetery");
    public static final CampaignOption<Boolean> USE_REMOVAL_EXEMPT_RETIREES =
          of(Boolean.class, false, "useRemovalExemptRetirees");
    public static final CampaignOption<Boolean> DISABLE_SECONDARY_ROLE_SALARY =
          of(Boolean.class, false, "disableSecondaryRoleSalary");
    public static final CampaignOption<Double> SALARY_ANTI_MEK_MULTIPLIER =
          of(Double.class, 1.5, "salaryAntiMekMultiplier");
    public static final CampaignOption<Double> SALARY_SPECIALIST_INFANTRY_MULTIPLIER =
          of(Double.class, 1.28, "salarySpecialistInfantryMultiplier");
    public static final CampaignOption<Map<SkillLevel, Double>> SALARY_XP_MULTIPLIERS =
          ofRaw(Map.class, () -> new HashMap<>(), "salaryXPMultipliers");
    public static final CampaignOption<AwardBonus> AWARD_BONUS_STYLE =
          of(AwardBonus.class, AwardBonus.BOTH, "awardBonusStyle");
    public static final CampaignOption<Boolean> ENABLE_AUTO_AWARDS =
          of(Boolean.class, false, "enableAutoAwards");
    public static final CampaignOption<Boolean> ISSUE_POSTHUMOUS_AWARDS =
          of(Boolean.class, false, "issuePosthumousAwards");
    public static final CampaignOption<Boolean> ISSUE_BEST_AWARD_ONLY =
          of(Boolean.class, true, "issueBestAwardOnly");
    public static final CampaignOption<Boolean> IGNORE_STANDARD_SET =
          of(Boolean.class, false, "ignoreStandardSet");
    public static final CampaignOption<Integer> AWARD_TIER_SIZE =
          of(Integer.class, 5, "awardTierSize");
    public static final CampaignOption<Boolean> ENABLE_CONTRACT_AWARDS =
          of(Boolean.class, true, "enableContractAwards");
    public static final CampaignOption<Boolean> ENABLE_FACTION_HUNTER_AWARDS =
          of(Boolean.class, true, "enableFactionHunterAwards");
    public static final CampaignOption<Boolean> ENABLE_INJURY_AWARDS =
          of(Boolean.class, true, "enableInjuryAwards");
    public static final CampaignOption<Boolean> ENABLE_INDIVIDUAL_KILL_AWARDS =
          of(Boolean.class, true, "enableIndividualKillAwards");
    public static final CampaignOption<Boolean> ENABLE_FORMATION_KILL_AWARDS =
          of(Boolean.class, true, "enableFormationKillAwards");
    public static final CampaignOption<Boolean> ENABLE_RANK_AWARDS =
          of(Boolean.class, true, "enableRankAwards");
    public static final CampaignOption<Boolean> ENABLE_SCENARIO_AWARDS =
          of(Boolean.class, true, "enableScenarioAwards");
    public static final CampaignOption<Boolean> ENABLE_SKILL_AWARDS =
          of(Boolean.class, true, "enableSkillAwards");
    public static final CampaignOption<Boolean> ENABLE_THEATRE_OF_WAR_AWARDS =
          of(Boolean.class, true, "enableTheatreOfWarAwards");
    public static final CampaignOption<Boolean> ENABLE_TIME_AWARDS =
          of(Boolean.class, true, "enableTimeAwards");
    public static final CampaignOption<Boolean> ENABLE_TRAINING_AWARDS =
          of(Boolean.class, true, "enableTrainingAwards");
    public static final CampaignOption<Boolean> ENABLE_MISC_AWARDS =
          of(Boolean.class, true, "enableMiscAwards");
    public static final CampaignOption<String> AWARD_SET_FILTER_LIST =
          of(String.class, "", "awardSetFilterList");
    public static final CampaignOption<Integer> NON_BINARY_DICE_SIZE =
          of(Integer.class, 60, "nonBinaryDiceSize");
    public static final CampaignOption<RandomOriginOptions> RANDOM_ORIGIN_OPTIONS =
          ofMutable(RandomOriginOptions.class, () -> new RandomOriginOptions(true), "randomOriginOptions");
    public static final CampaignOption<Boolean> USE_RANDOM_PERSONALITIES =
          of(Boolean.class, false, "useRandomPersonalities");
    public static final CampaignOption<Boolean> USE_PERSONALITY_LABELS_ONLY =
          of(Boolean.class, false, "usePersonalityLabelsOnly");
    public static final CampaignOption<Boolean> USE_RANDOM_PERSONALITY_REPUTATION =
          of(Boolean.class, true, "useRandomPersonalityReputation");
    public static final CampaignOption<Boolean> USE_REASONING_XP_MULTIPLIER =
          of(Boolean.class, true, "useReasoningXpMultiplier");
    public static final CampaignOption<Boolean> USE_SIMULATED_RELATIONSHIPS =
          of(Boolean.class, false, "useSimulatedRelationships");
    public static final CampaignOption<FamilialRelationshipDisplayLevel> FAMILY_DISPLAY_LEVEL =
          of(FamilialRelationshipDisplayLevel.class, FamilialRelationshipDisplayLevel.SPOUSE, "familyDisplayLevel");
    public static final CampaignOption<Boolean> ANNOUNCE_BIRTHDAYS =
          of(Boolean.class, true, "announceBirthdays");
    public static final CampaignOption<Boolean> ANNOUNCE_RECRUITMENT_ANNIVERSARIES =
          of(Boolean.class, true, "announceRecruitmentAnniversaries");
    public static final CampaignOption<Boolean> ANNOUNCE_OFFICERS_ONLY =
          of(Boolean.class, true, "announceOfficersOnly");
    public static final CampaignOption<Boolean> ANNOUNCE_CHILD_BIRTHDAYS =
          of(Boolean.class, true, "announceChildBirthdays");
    public static final CampaignOption<Boolean> ANNOUNCE_RETIREE_DEATH =
          of(Boolean.class, true, "announceRetireeDeath");
    public static final CampaignOption<Boolean> ANNOUNCE_RETIREE_DEATH_EXPANDED =
          of(Boolean.class, false, "announceRetireeDeathExpanded");
    public static final CampaignOption<Boolean> SHOW_LIFE_EVENT_DIALOG_BIRTHS =
          of(Boolean.class, true, "showLifeEventDialogBirths");
    public static final CampaignOption<Boolean> SHOW_LIFE_EVENT_DIALOG_COMING_OF_AGE =
          of(Boolean.class, true, "showLifeEventDialogComingOfAge");
    public static final CampaignOption<Boolean> SHOW_LIFE_EVENT_DIALOG_CELEBRATIONS =
          of(Boolean.class, true, "showLifeEventDialogCelebrations");
    public static final CampaignOption<Boolean> REWARD_COMING_OF_AGE_ABILITIES =
          of(Boolean.class, false, "rewardComingOfAgeAbilities");
    public static final CampaignOption<Boolean> REWARD_COMING_OF_AGE_RP_SKILLS =
          of(Boolean.class, false, "rewardComingOfAgeRPSkills");
    public static final CampaignOption<Boolean> USE_MANUAL_MARRIAGES =
          of(Boolean.class, true, "useManualMarriages");
    public static final CampaignOption<Boolean> USE_CLAN_PERSONNEL_MARRIAGES =
          of(Boolean.class, false, "useClanPersonnelMarriages");
    public static final CampaignOption<Boolean> USE_PRISONER_MARRIAGES =
          of(Boolean.class, true, "usePrisonerMarriages");
    public static final CampaignOption<Integer> CHECK_MUTUAL_ANCESTORS_DEPTH =
          of(Integer.class, 4, "checkMutualAncestorsDepth");
    public static final CampaignOption<Boolean> LOG_MARRIAGE_NAME_CHANGES =
          of(Boolean.class, false, "logMarriageNameChanges");
    public static final CampaignOption<Map<MergingSurnameStyle, Integer>> MARRIAGE_SURNAME_WEIGHTS =
          ofRaw(Map.class, () -> new HashMap<>(), "marriageSurnameWeights");
    public static final CampaignOption<RandomMarriageMethod> RANDOM_MARRIAGE_METHOD =
          of(RandomMarriageMethod.class, RandomMarriageMethod.NONE, "randomMarriageMethod");
    public static final CampaignOption<Boolean> USE_RANDOM_CLAN_PERSONNEL_MARRIAGES =
          of(Boolean.class, false, "useRandomClanPersonnelMarriages");
    public static final CampaignOption<Boolean> USE_RANDOM_PRISONER_MARRIAGES =
          of(Boolean.class, false, "useRandomPrisonerMarriages");
    public static final CampaignOption<Integer> RANDOM_MARRIAGE_AGE_RANGE =
          of(Integer.class, 10, "randomMarriageAgeRange");
    public static final CampaignOption<Integer> RANDOM_MARRIAGE_DICE_SIZE =
          of(Integer.class, 5000, "randomMarriageDiceSize");
    public static final CampaignOption<Integer> RANDOM_NEW_DEPENDENT_MARRIAGE =
          of(Integer.class, 20, "randomNewDependentMarriage");
    public static final CampaignOption<Boolean> USE_MANUAL_DIVORCE =
          of(Boolean.class, true, "useManualDivorce");
    public static final CampaignOption<Boolean> USE_CLAN_PERSONNEL_DIVORCE =
          of(Boolean.class, true, "useClanPersonnelDivorce");
    public static final CampaignOption<Boolean> USE_PRISONER_DIVORCE =
          of(Boolean.class, false, "usePrisonerDivorce");
    public static final CampaignOption<Map<SplittingSurnameStyle, Integer>> DIVORCE_SURNAME_WEIGHTS =
          ofRaw(Map.class, () -> new HashMap<>(), "divorceSurnameWeights");
    public static final CampaignOption<RandomDivorceMethod> RANDOM_DIVORCE_METHOD =
          of(RandomDivorceMethod.class, RandomDivorceMethod.NONE, "randomDivorceMethod");
    public static final CampaignOption<Boolean> USE_RANDOM_OPPOSITE_SEX_DIVORCE =
          of(Boolean.class, true, "useRandomOppositeSexDivorce");
    public static final CampaignOption<Boolean> USE_RANDOM_SAME_SEX_DIVORCE =
          of(Boolean.class, true, "useRandomSameSexDivorce");
    public static final CampaignOption<Boolean> USE_RANDOM_CLAN_PERSONNEL_DIVORCE =
          of(Boolean.class, true, "useRandomClanPersonnelDivorce");
    public static final CampaignOption<Boolean> USE_RANDOM_PRISONER_DIVORCE =
          of(Boolean.class, false, "useRandomPrisonerDivorce");
    public static final CampaignOption<Integer> RANDOM_DIVORCE_DICE_SIZE =
          of(Integer.class, 900, "randomDivorceDiceSize");
    public static final CampaignOption<Boolean> USE_MANUAL_PROCREATION =
          of(Boolean.class, true, "useManualProcreation");
    public static final CampaignOption<Boolean> USE_CLAN_PERSONNEL_PROCREATION =
          of(Boolean.class, false, "useClanPersonnelProcreation");
    public static final CampaignOption<Boolean> USE_PRISONER_PROCREATION =
          of(Boolean.class, true, "usePrisonerProcreation");
    // Hellin's Law is 89, but we make it more common, so it shows up more
    public static final CampaignOption<Integer> MULTIPLE_PREGNANCY_OCCURRENCES =
          of(Integer.class, 50, "multiplePregnancyOccurrences");
    public static final CampaignOption<BabySurnameStyle> BABY_SURNAME_STYLE =
          of(BabySurnameStyle.class, BabySurnameStyle.MOTHERS, "babySurnameStyle");
    public static final CampaignOption<Boolean> ASSIGN_NON_PRISONER_BABIES_FOUNDER_TAG =
          of(Boolean.class, false, "assignNonPrisonerBabiesFounderTag");
    public static final CampaignOption<Boolean> ASSIGN_CHILDREN_OF_FOUNDERS_FOUNDER_TAG =
          of(Boolean.class, false, "assignChildrenOfFoundersFounderTag");
    public static final CampaignOption<Boolean> USE_MATERNITY_LEAVE =
          of(Boolean.class, true, "useMaternityLeave");
    public static final CampaignOption<Boolean> DETERMINE_FATHER_AT_BIRTH =
          of(Boolean.class, false, "determineFatherAtBirth");
    public static final CampaignOption<Boolean> DISPLAY_TRUE_DUE_DATE =
          of(Boolean.class, false, "displayTrueDueDate");
    public static final CampaignOption<Integer> NO_INTEREST_IN_CHILDREN_DICE_SIZE =
          of(Integer.class, 3, "noInterestInChildrenDiceSize");
    public static final CampaignOption<Boolean> LOG_PROCREATION =
          of(Boolean.class, false, "logProcreation");
    public static final CampaignOption<RandomProcreationMethod> RANDOM_PROCREATION_METHOD =
          of(RandomProcreationMethod.class, RandomProcreationMethod.NONE, "randomProcreationMethod");
    public static final CampaignOption<Boolean> USE_RELATIONSHIPLESS_RANDOM_PROCREATION =
          of(Boolean.class, false, "useRelationshiplessRandomProcreation");
    public static final CampaignOption<Boolean> USE_RANDOM_CLAN_PERSONNEL_PROCREATION =
          of(Boolean.class, false, "useRandomClanPersonnelProcreation");
    public static final CampaignOption<Boolean> USE_RANDOM_PRISONER_PROCREATION =
          of(Boolean.class, true, "useRandomPrisonerProcreation");
    public static final CampaignOption<Integer> RANDOM_PROCREATION_RELATIONSHIP_DICE_SIZE =
          of(Integer.class, 150, "randomProcreationRelationshipDiceSize");
    public static final CampaignOption<Integer> RANDOM_PROCREATION_RELATIONSHIPLESS_DICE_SIZE =
          of(Integer.class, 2000, "randomProcreationRelationshiplessDiceSize");
    public static final CampaignOption<Integer> NO_INTEREST_IN_RELATIONSHIPS_DICE_SIZE =
          of(Integer.class, 100, "noInterestInMarriageDiceSize");
    public static final CampaignOption<Integer> INTERESTED_IN_BOTH_SEXES_DICE_SIZE =
          of(Integer.class, 33, "interestedInBothSexesDiceSize");
    public static final CampaignOption<Integer> INTERESTED_IN_SAME_SEX_DICE_SIZE =
          of(Integer.class, 14, "randomSameSexMarriageDiceSize");
    public static final CampaignOption<Boolean> USE_EDUCATION_MODULE =
          of(Boolean.class, false, "useEducationModule");
    public static final CampaignOption<Integer> CURRICULUM_XP_RATE =
          of(Integer.class, 3, "curriculumXpRate");
    public static final CampaignOption<Integer> MAXIMUM_JUMP_COUNT =
          of(Integer.class, 5, "maximumJumpCount");
    public static final CampaignOption<Boolean> USE_REEDUCATION_CAMPS =
          of(Boolean.class, true, "useReeducationCamps");
    public static final CampaignOption<Boolean> ENABLE_LOCAL_ACADEMIES =
          of(Boolean.class, true, "enableLocalAcademies");
    public static final CampaignOption<Boolean> ENABLE_PRESTIGIOUS_ACADEMIES =
          of(Boolean.class, true, "enablePrestigiousAcademies");
    public static final CampaignOption<Boolean> ENABLE_UNIT_EDUCATION =
          of(Boolean.class, true, "enableUnitEducation");
    public static final CampaignOption<Boolean> ENABLE_OVERRIDE_REQUIREMENTS =
          of(Boolean.class, false, "enableOverrideRequirements");
    public static final CampaignOption<Boolean> ENABLE_SHOW_INELIGIBLE_ACADEMIES =
          of(Boolean.class, true, "enableShowIneligibleAcademies");
    public static final CampaignOption<Integer> ENTRANCE_EXAM_BASE_TARGET_NUMBER =
          of(Integer.class, 14, "entranceExamBaseTargetNumber");
    public static final CampaignOption<Double> FACULTY_XP_RATE =
          of(Double.class, 1.00, "facultyXpRate");
    public static final CampaignOption<Boolean> ENABLE_BONUSES =
          of(Boolean.class, true, "enableBonuses");
    public static final CampaignOption<Integer> ADULT_DROPOUT_CHANCE =
          of(Integer.class, 1000, "adultDropoutChance");
    public static final CampaignOption<Integer> CHILDREN_DROPOUT_CHANCE =
          of(Integer.class, 10000, "childrenDropoutChance");
    public static final CampaignOption<Boolean> ALL_AGES =
          of(Boolean.class, false, "allAges");
    public static final CampaignOption<Integer> MILITARY_ACADEMY_ACCIDENTS =
          of(Integer.class, 10000, "militaryAcademyAccidents");
    public static final CampaignOption<Map<AgeGroup, Boolean>> ENABLED_RANDOM_DEATH_AGE_GROUPS =
          ofRaw(Map.class, () -> new HashMap<>(), "enabledRandomDeathAgeGroups");
    public static final CampaignOption<Boolean> USE_RANDOM_DEATH_SUICIDE_CAUSE =
          of(Boolean.class, false, "useRandomDeathSuicideCause");
    public static final CampaignOption<Double> RANDOM_DEATH_MULTIPLIER =
          of(Double.class, 0.0, "randomDeathMultiplier");
    public static final CampaignOption<Boolean> USE_RANDOM_RETIREMENT =
          of(Boolean.class, false, "useRandomRetirement");
    public static final CampaignOption<Integer> TURNOVER_FIXED_TARGET_NUMBER =
          of(Integer.class, 3, "turnoverBaseTn");
    public static final CampaignOption<Boolean> AERO_RECRUITS_HAVE_UNITS =
          of(Boolean.class, false, "aeroRecruitsHaveUnits");
    public static final CampaignOption<Boolean> TRACK_ORIGINAL_UNIT =
          of(Boolean.class, false, "trackOriginalUnit");
    public static final CampaignOption<TurnoverFrequency> TURNOVER_FREQUENCY =
          of(TurnoverFrequency.class, TurnoverFrequency.MONTHLY, "turnoverFrequency");
    public static final CampaignOption<Boolean> USE_CONTRACT_COMPLETION_RANDOM_RETIREMENT =
          of(Boolean.class, true, "useContractCompletionRandomRetirement");
    public static final CampaignOption<Boolean> USE_RANDOM_FOUNDER_TURNOVER =
          of(Boolean.class, true, "useRandomFounderTurnover");
    public static final CampaignOption<Boolean> USE_FOUNDER_RETIREMENT =
          of(Boolean.class, true, "useFounderRetirement");
    public static final CampaignOption<Boolean> USE_SUB_CONTRACT_SOLDIERS =
          of(Boolean.class, false, "useSubContractSoldiers");
    public static final CampaignOption<Integer> SERVICE_CONTRACT_DURATION =
          of(Integer.class, 36, "serviceContractDuration");
    public static final CampaignOption<Integer> SERVICE_CONTRACT_MODIFIER =
          of(Integer.class, 3, "serviceContractModifier");
    public static final CampaignOption<Boolean> PAY_BONUS_DEFAULT =
          of(Boolean.class, false, "payBonusDefault");
    public static final CampaignOption<Integer> PAY_BONUS_DEFAULT_THRESHOLD =
          of(Integer.class, 3, "payBonusDefaultThreshold");
    public static final CampaignOption<Boolean> INCLUDE_CIVILIANS =
          of(Boolean.class, false, "includeCivilians");
    public static final CampaignOption<Boolean> USE_CUSTOM_RETIREMENT_MODIFIERS =
          of(Boolean.class, true, "useCustomRetirementModifiers");
    public static final CampaignOption<Boolean> USE_FATIGUE_MODIFIERS =
          of(Boolean.class, true, "useFatigueModifiers");
    public static final CampaignOption<Boolean> USE_SKILL_MODIFIERS =
          of(Boolean.class, true, "useSkillModifiers");
    public static final CampaignOption<Boolean> USE_AGE_MODIFIERS =
          of(Boolean.class, true, "useAgeModifiers");
    public static final CampaignOption<Boolean> USE_UNIT_RATING_MODIFIERS =
          of(Boolean.class, true, "useUnitRatingModifiers");
    public static final CampaignOption<Boolean> USE_FACTION_MODIFIERS =
          of(Boolean.class, true, "useFactionModifiers");
    public static final CampaignOption<Boolean> USE_HOSTILE_TERRITORY_MODIFIERS =
          of(Boolean.class, true, "useHostileTerritoryModifiers");
    public static final CampaignOption<Boolean> USE_MISSION_STATUS_MODIFIERS =
          of(Boolean.class, true, "useMissionStatusModifiers");
    public static final CampaignOption<Boolean> USE_FAMILY_MODIFIERS =
          of(Boolean.class, true, "useFamilyModifiers");
    public static final CampaignOption<Boolean> USE_LOYALTY_MODIFIERS =
          of(Boolean.class, true, "useLoyaltyModifiers");
    public static final CampaignOption<Boolean> USE_HIDE_LOYALTY =
          of(Boolean.class, false, "useHideLoyalty");
    public static final CampaignOption<Integer> PAYOUT_RATE_OFFICER =
          of(Integer.class, 3, "payoutRateOfficer");
    public static final CampaignOption<Integer> PAYOUT_RATE_ENLISTED =
          of(Integer.class, 3, "payoutRateEnlisted");
    public static final CampaignOption<Integer> PAYOUT_RETIREMENT_MULTIPLIER =
          of(Integer.class, 12, "payoutRetirementMultiplier");
    public static final CampaignOption<Boolean> USE_PAYOUT_SERVICE_BONUS =
          of(Boolean.class, true, "usePayoutServiceBonus");
    public static final CampaignOption<Integer> PAYOUT_SERVICE_BONUS_RATE =
          of(Integer.class, 10, "payoutServiceBonusRate");
    public static final CampaignOption<Boolean> USE_HR_STRAIN =
          of(Boolean.class, true, "UseHRStrain");
    public static final CampaignOption<Integer> HR_CAPACITY =
          of(Integer.class, 10, "hrStrain");
    public static final CampaignOption<Boolean> USE_MANAGEMENT_SKILL =
          of(Boolean.class, true, "useManagementSkill");
    public static final CampaignOption<Boolean> USE_COMMANDER_LEADERSHIP_ONLY =
          of(Boolean.class, false, "useCommanderLeadershipOnly");
    public static final CampaignOption<Integer> MANAGEMENT_SKILL_PENALTY =
          of(Integer.class, 0, "managementSkillPenalty");
    public static final CampaignOption<Boolean> USE_FATIGUE =
          of(Boolean.class, false, "useFatigue");
    public static final CampaignOption<Integer> FATIGUE_RATE =
          of(Integer.class, 1, "fatigueRate");
    public static final CampaignOption<Boolean> USE_INJURY_FATIGUE =
          of(Boolean.class, true, "useInjuryFatigue");
    public static final CampaignOption<Integer> FIELD_KITCHEN_CAPACITY =
          of(Integer.class, 150, "fieldKitchenCapacity");
    public static final CampaignOption<Boolean> FIELD_KITCHEN_IGNORE_NON_COMBATANTS =
          of(Boolean.class, true, "fieldKitchenIgnoreNonCombatants");
    public static final CampaignOption<Integer> FATIGUE_UNDEPLOYMENT_THRESHOLD =
          of(Integer.class, 9, "fatigueUndeploymentThreshold");
    public static final CampaignOption<Integer> FATIGUE_LEAVE_THRESHOLD =
          of(Integer.class, 13, "fatigueLeaveThreshold");
    public static final CampaignOption<Boolean> PAY_FOR_PARTS =
          of(Boolean.class, false, "payForParts");
    public static final CampaignOption<Boolean> PAY_FOR_REPAIRS =
          of(Boolean.class, false, "payForRepairs");
    public static final CampaignOption<Boolean> PAY_FOR_UNITS =
          of(Boolean.class, false, "payForUnits");
    public static final CampaignOption<Boolean> PAY_FOR_SALARIES =
          of(Boolean.class, false, "payForSalaries");
    public static final CampaignOption<Boolean> PAY_FOR_OVERHEAD =
          of(Boolean.class, false, "payForOverhead");
    public static final CampaignOption<Boolean> PAY_FOR_MAINTAIN =
          of(Boolean.class, false, "payForMaintain");
    public static final CampaignOption<Boolean> PAY_FOR_TRANSPORT =
          of(Boolean.class, false, "payForTransport");
    public static final CampaignOption<Boolean> SELL_UNITS =
          of(Boolean.class, false, "sellUnits");
    public static final CampaignOption<Boolean> SELL_PARTS =
          of(Boolean.class, false, "sellParts");
    public static final CampaignOption<Boolean> PAY_FOR_RECRUITMENT =
          of(Boolean.class, false, "payForRecruitment");
    public static final CampaignOption<Boolean> PAY_FOR_FOOD =
          of(Boolean.class, false, "payForFood");
    public static final CampaignOption<Boolean> PAY_FOR_HOUSING =
          of(Boolean.class, false, "payForHousing");
    public static final CampaignOption<Boolean> USE_LOAN_LIMITS =
          of(Boolean.class, false, "useLoanLimits");
    public static final CampaignOption<Boolean> USE_PEACETIME_COST =
          of(Boolean.class, false, "usePeacetimeCost");
    public static final CampaignOption<Boolean> USE_EXTENDED_PARTS_MODIFIER =
          of(Boolean.class, false, "useExtendedPartsModifier");
    public static final CampaignOption<Boolean> SHOW_PEACETIME_COST =
          of(Boolean.class, false, "showPeacetimeCost");
    public static final CampaignOption<FinancialYearDuration> FINANCIAL_YEAR_DURATION =
          of(FinancialYearDuration.class, FinancialYearDuration.ANNUAL, "financialYearDuration");
    public static final CampaignOption<Boolean> NEW_FINANCIAL_YEAR_FINANCES_TO_CSV_EXPORT =
          of(Boolean.class, false, "newFinancialYearFinancesToCSVExport");
    public static final CampaignOption<Boolean> SIMULATE_GRAY_MONDAY =
          of(Boolean.class, false, "simulateGrayMonday");
    public static final CampaignOption<Boolean> DISPLAY_ALL_ATTRIBUTES =
          of(Boolean.class, false, "displayAllAttributes");
    public static final CampaignOption<Boolean> ALLOW_MONTHLY_REINVESTMENT =
          of(Boolean.class, false, "allowMonthlyReinvestment");
    public static final CampaignOption<Boolean> ALLOW_MONTHLY_CONNECTIONS =
          of(Boolean.class, false, "allowMonthlyConnections");
    public static final CampaignOption<Boolean> USE_BETTER_EXTRA_INCOME =
          of(Boolean.class, false, "useBetterExtraIncome");
    public static final CampaignOption<Boolean> USE_SMALL_ARMS_ONLY =
          of(Boolean.class, false, "useSmallArmsOnly");
    public static final CampaignOption<Double> COMMON_PART_PRICE_MULTIPLIER =
          of(Double.class, 1.0, "commonPartPriceMultiplier");
    public static final CampaignOption<Double> INNER_SPHERE_UNIT_PRICE_MULTIPLIER =
          of(Double.class, 1.0, "innerSphereUnitPriceMultiplier");
    public static final CampaignOption<Double> INNER_SPHERE_PART_PRICE_MULTIPLIER =
          of(Double.class, 1.0, "innerSpherePartPriceMultiplier");
    public static final CampaignOption<Double> CLAN_UNIT_PRICE_MULTIPLIER =
          of(Double.class, 1.0, "clanUnitPriceMultiplier");
    public static final CampaignOption<Double> CLAN_PART_PRICE_MULTIPLIER =
          of(Double.class, 1.0, "clanPartPriceMultiplier");
    public static final CampaignOption<Double> MIXED_TECH_UNIT_PRICE_MULTIPLIER =
          of(Double.class, 1.0, "mixedTechUnitPriceMultiplier");
    public static final CampaignOption<Double> DAMAGED_PARTS_VALUE_MULTIPLIER =
          of(Double.class, 0.33, "damagedPartsValueMultiplier");
    public static final CampaignOption<Double> UNREPAIRABLE_PARTS_VALUE_MULTIPLIER =
          of(Double.class, 0.1, "unrepairablePartsValueMultiplier");
    public static final CampaignOption<Double> CANCELLED_ORDER_REFUND_MULTIPLIER =
          of(Double.class, 0.5, "cancelledOrderRefundMultiplier");
    public static final CampaignOption<Boolean> USE_TAXES =
          of(Boolean.class, false, "useTaxes");
    public static final CampaignOption<Integer> TAXES_PERCENTAGE =
          of(Integer.class, 30, "taxesPercentage");
    public static final CampaignOption<Boolean> USE_SHARE_SYSTEM =
          of(Boolean.class, false, "useShareSystem");
    public static final CampaignOption<Boolean> SHARES_FOR_ALL =
          of(Boolean.class, true, "sharesForAll");
    public static final CampaignOption<Integer> RENTED_FACILITIES_COST_HOSPITAL_BEDS =
          of(Integer.class, 0, "rentedFacilitiesCostHospitalBeds");
    public static final CampaignOption<Integer> RENTED_FACILITIES_COST_KITCHENS =
          of(Integer.class, 0, "rentedFacilitiesCostKitchens");
    public static final CampaignOption<Integer> RENTED_FACILITIES_COST_HOLDING_CELLS =
          of(Integer.class, 0, "rentedFacilitiesCostHoldingCells");
    public static final CampaignOption<Integer> RENTED_FACILITIES_COST_REPAIR_BAYS =
          of(Integer.class, 0, "rentedFacilitiesCostRepairBays");
    public static final CampaignOption<Boolean> USE_ALTERNATE_PAYMENT_MODE =
          of(Boolean.class, false, "useAlternatePaymentMode");
    public static final CampaignOption<Boolean> USE_DIMINISHING_CONTRACT_PAY =
          of(Boolean.class, false, "useDiminishingContractPay");
    public static final CampaignOption<Boolean> EQUIPMENT_CONTRACT_BASE =
          of(Boolean.class, false, "equipmentContractBase");
    public static final CampaignOption<Boolean> EQUIPMENT_CONTRACT_SALE_VALUE =
          of(Boolean.class, false, "equipmentContractSaleValue");
    public static final CampaignOption<Boolean> BLC_SALE_VALUE =
          of(Boolean.class, false, "blcSaleValue");
    public static final CampaignOption<Boolean> OVERAGE_REPAYMENT_IN_FINAL_PAYMENT =
          of(Boolean.class, false, "overageRepaymentInFinalPayment");
    public static final CampaignOption<Double> XP_COST_MULTIPLIER =
          of(Double.class, 1.00, "xpCostMultiplier");
    public static final CampaignOption<Integer> SCENARIO_XP =
          of(Integer.class, 1, "scenarioXP");
    public static final CampaignOption<Integer> KILL_XP_AWARD =
          of(Integer.class, 0, "killXPAward");
    public static final CampaignOption<Integer> KILLS_FOR_XP =
          of(Integer.class, 0, "killsForXP");
    public static final CampaignOption<Integer> TASKS_XP =
          of(Integer.class, 1, "tasksXP");
    public static final CampaignOption<Integer> N_TASKS_XP =
          of(Integer.class, 25, "nTasksXP");
    public static final CampaignOption<Integer> SUCCESS_XP =
          of(Integer.class, 0, "successXP");
    public static final CampaignOption<Integer> MISTAKE_XP =
          of(Integer.class, 0, "mistakeXP");
    public static final CampaignOption<Integer> VOCATIONAL_XP =
          of(Integer.class, 1, "vocationalXP");
    public static final CampaignOption<Integer> VOCATIONAL_XP_CHECK_FREQUENCY =
          of(Integer.class, 1, "vocationalXPCheckFrequency");
    public static final CampaignOption<Integer> VOCATIONAL_XP_TARGET_NUMBER =
          of(Integer.class, 7, "vocationalXPTargetNumber");
    public static final CampaignOption<Integer> CONTRACT_NEGOTIATION_XP =
          of(Integer.class, 0, "contractNegotiationXP");
    public static final CampaignOption<Integer> ADMIN_XP =
          of(Integer.class, 0, "adminWeeklyXP");
    public static final CampaignOption<Integer> ADMIN_XP_PERIOD =
          of(Integer.class, 1, "adminXPPeriod");
    public static final CampaignOption<Integer> MISSION_XP_FAIL =
          of(Integer.class, 1, "missionXpFail");
    public static final CampaignOption<Integer> MISSION_XP_SUCCESS =
          of(Integer.class, 3, "missionXpSuccess");
    public static final CampaignOption<Integer> MISSION_XP_OUTSTANDING_SUCCESS =
          of(Integer.class, 5, "missionXpOutstandingSuccess");
    public static final CampaignOption<Integer> ATTRIBUTE_COST =
          of(Integer.class, 100, "attributeCost");
    public static final CampaignOption<Integer> EDGE_COST =
          of(Integer.class, 10, "edgeCost");
    public static final CampaignOption<Integer> EDGE_REFRESH_COST =
          of(Integer.class, 20, "edgeRefreshCost");
    public static final CampaignOption<Boolean> USE_ORIGIN_FACTION_FOR_NAMES =
          of(Boolean.class, true, "useFactionForNames");
    public static final CampaignOption<Boolean> ASSIGN_PORTRAIT_ON_ROLE_CHANGE =
          of(Boolean.class, false, "assignPortraitOnRoleChange");
    public static final CampaignOption<Boolean> ALLOW_DUPLICATE_PORTRAITS =
          of(Boolean.class, true, "allowDuplicatePortraits");
    public static final CampaignOption<Boolean> USE_GENDERED_PORTRAITS_ONLY =
          of(Boolean.class, false, "useGenderedPortraitsOnly");
    public static final CampaignOption<Boolean> NO_RANDOM_PORTRAITS_FOR_CHILDREN =
          of(Boolean.class, true, "noRandomPortraitsForChildren");
    public static final CampaignOption<Boolean> CHILD_PORTRAITS_WHEN_COMING_OF_AGE =
          of(Boolean.class, true, "childPortraitsWhenComingOfAge");
    public static final CampaignOption<PersonnelMarketStyle> PERSONNEL_MARKET_STYLE =
          of(PersonnelMarketStyle.class, PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED, "personnelMarketStyle");
    public static final CampaignOption<Boolean> USE_PERSONNEL_HIRE_HIRING_HALL_ONLY =
          of(Boolean.class, false, "usePersonnelHireHiringHallOnly");
    public static final CampaignOption<Boolean> PERSONNEL_MARKET_REPORT_REFRESH =
          of(Boolean.class, true, "personnelMarketReportRefresh");
    public static final CampaignOption<Double> PERSONNEL_MARKET_DYLANS_WEIGHT =
          of(Double.class, 0.3, "personnelMarketDylansWeight");
    public static final CampaignOption<UnitMarketMethod> UNIT_MARKET_METHOD =
          of(UnitMarketMethod.class, UnitMarketMethod.NONE, "unitMarketMethod");
    public static final CampaignOption<Boolean> UNIT_MARKET_REGIONAL_MEK_VARIATIONS =
          of(Boolean.class, true, "unitMarketRegionalMekVariations");
    public static final CampaignOption<Integer> UNIT_MARKET_ARTILLERY_UNIT_CHANCE =
          of(Integer.class, 30, "unitMarketArtilleryUnitChance");
    public static final CampaignOption<Integer> UNIT_MARKET_RARITY_MODIFIER =
          of(Integer.class, 0, "unitMarketRarityModifier");
    public static final CampaignOption<Boolean> INSTANT_UNIT_MARKET_DELIVERY =
          of(Boolean.class, false, "instantUnitMarketDelivery");
    public static final CampaignOption<Boolean> MOTHBALL_UNIT_MARKET_DELIVERIES =
          of(Boolean.class, false, "mothballUnitMarketDeliveries");
    public static final CampaignOption<Boolean> UNIT_MARKET_REPORT_REFRESH =
          of(Boolean.class, true, "unitMarketReportRefresh");
    public static final CampaignOption<ContractMarketMethod> CONTRACT_MARKET_METHOD =
          of(ContractMarketMethod.class, ContractMarketMethod.NONE, "contractMarketMethod");
    public static final CampaignOption<Integer> CONTRACT_SEARCH_RADIUS =
          of(Integer.class, 800, "contractSearchRadius");
    public static final CampaignOption<Boolean> VARIABLE_CONTRACT_LENGTH =
          of(Boolean.class, true, "variableContractLength");
    public static final CampaignOption<Boolean> USE_DYNAMIC_DIFFICULTY =
          of(Boolean.class, false, "useDynamicDifficulty");
    public static final CampaignOption<Boolean> USE_BOLSTER_CONTRACT_SKILL =
          of(Boolean.class, false, "useBolsterContractSkill");
    public static final CampaignOption<Boolean> CONTRACT_MARKET_REPORT_REFRESH =
          of(Boolean.class, true, "contractMarketReportRefresh");
    public static final CampaignOption<Integer> CONTRACT_MAX_SALVAGE_PERCENTAGE =
          of(Integer.class, 100, "contractMaxSalvagePercentage");
    public static final CampaignOption<Integer> DROP_SHIP_BONUS_PERCENTAGE =
          of(Integer.class, 0, "dropShipBonusPercentage");
    public static final CampaignOption<Integer> PITY_CONTRACTS =
          of(Integer.class, 4, "pityContracts");
    public static final CampaignOption<Boolean> IS_USE_TWO_WAY_PAY =
          of(Boolean.class, true, "isUseTwoWayPay");
    public static final CampaignOption<Boolean> IS_USE_CAM_OPS_SALVAGE =
          of(Boolean.class, false, "isUseCamOpsSalvage");
    public static final CampaignOption<Boolean> IS_USE_RISKY_SALVAGE =
          of(Boolean.class, false, "isUseRiskySalvage");
    public static final CampaignOption<Boolean> IS_ENABLE_SALVAGE_FLAG_BY_DEFAULT =
          of(Boolean.class, true, "isEnableSalvageFlagByDefault");
    public static final CampaignOption<Boolean> HAD_AT_B_ENABLED_MARKER =
          of(Boolean.class, false, "useAtB");
    public static final CampaignOption<StratConPlayType> STRAT_CON_PLAY_TYPE =
          of(StratConPlayType.class, StratConPlayType.DISABLED, "stratConPlayType");
    public static final CampaignOption<Boolean> USE_ADVANCED_SCOUTING =
          of(Boolean.class, false, "useAdvancedScouting");
    public static final CampaignOption<Boolean> NO_SEED_FORCES =
          of(Boolean.class, false, "noSeedForces");
    public static final CampaignOption<SkillLevel> SKILL_LEVEL =
          of(SkillLevel.class, SkillLevel.REGULAR, "skillLevel");
    public static final CampaignOption<BoardScalingType> BOARD_SCALING_TYPE =
          of(BoardScalingType.class, BoardScalingType.NORMAL, "boardScalingType");
    public static final CampaignOption<Integer> MORALE_VICTORY_EFFECT =
          of(Integer.class, 1, "moraleVictoryEffect");
    public static final CampaignOption<Integer> MORALE_DECISIVE_VICTORY_EFFECT =
          of(Integer.class, 2, "moraleDecisiveVictoryEffect");
    public static final CampaignOption<Integer> MORALE_DEFEAT_EFFECT =
          of(Integer.class, -2, "moraleDefeatEffect");
    public static final CampaignOption<Integer> MORALE_DECISIVE_DEFEAT_EFFECT =
          of(Integer.class, -3, "moraleDecisiveDefeatEffect");
    public static final CampaignOption<Boolean> RESTRICT_PARTS_BY_MISSION =
          of(Boolean.class, true, "restrictPartsByMission");
    public static final CampaignOption<Boolean> GENERATE_CHASES =
          of(Boolean.class, true, "generateChases");
    public static final CampaignOption<Boolean> USE_GENERIC_BATTLE_VALUE =
          of(Boolean.class, true, "useGenericBattleValue");
    public static final CampaignOption<Boolean> USE_VERBOSE_BIDDING =
          of(Boolean.class, false, "useVerboseBidding");
    public static final CampaignOption<Integer> OP_FOR_LANCE_TYPE_MEKS =
          of(Integer.class, 1, "opForLanceTypeMeks");
    public static final CampaignOption<Integer> OP_FOR_LANCE_TYPE_MIXED =
          of(Integer.class, 2, "opForLanceTypeMixed");
    public static final CampaignOption<Integer> OP_FOR_LANCE_TYPE_VEHICLES =
          of(Integer.class, 3, "opForLanceTypeVehicles");
    public static final CampaignOption<Boolean> REGIONAL_MEK_VARIATIONS =
          of(Boolean.class, false, "regionalMekVariations");
    public static final CampaignOption<Boolean> ATTACHED_PLAYER_CAMOUFLAGE =
          of(Boolean.class, true, "attachedPlayerCamouflage");
    public static final CampaignOption<Boolean> PLAYER_CONTROLS_ATTACHED_UNITS =
          of(Boolean.class, false, "playerControlsAttachedUnits");
    public static final CampaignOption<Boolean> USE_DROP_SHIPS =
          of(Boolean.class, false, "useDropShips");
    public static final CampaignOption<Boolean> USE_WEATHER_CONDITIONS =
          of(Boolean.class, true, "useWeatherConditions");
    public static final CampaignOption<Boolean> USE_LIGHT_CONDITIONS =
          of(Boolean.class, true, "useLightConditions");
    public static final CampaignOption<Boolean> USE_PLANETARY_CONDITIONS =
          of(Boolean.class, false, "usePlanetaryConditions");
    public static final CampaignOption<Boolean> USE_NO_TORNADOES =
          of(Boolean.class, false, "useNoTornadoes");
    public static final CampaignOption<Integer> FIXED_MAP_CHANCE =
          of(Integer.class, 25, "fixedMapChance");
    public static final CampaignOption<Boolean> USE_ADVANCED_BUILDING_GUN_EMPLACEMENTS =
          of(Boolean.class, false, "useAdvancedBuildingGunEmplacements");
    public static final CampaignOption<Integer> SPA_UPGRADE_INTENSITY =
          of(Integer.class, 0, "spaUpgradeIntensity");
    public static final CampaignOption<Integer> REINFORCEMENT_BASE_TARGET_NUMBER =
          of(Integer.class, 7, "reinforcementBaseTargetNumber");
    public static final CampaignOption<Boolean> CLANS_OBEY_BIDDING_RULES =
          of(Boolean.class, true, "clansObeyBiddingRules");
    public static final CampaignOption<Integer> ALLIED_FACILITY_MODIFIER_DIE_SIZE =
          of(Integer.class, 2, "alliedFacilityModifierDieSize");
    public static final CampaignOption<Integer> ENEMY_FACILITY_MODIFIER_DIE_SIZE =
          of(Integer.class, 2, "enemyFacilityModifierDieSize");
    public static final CampaignOption<Integer> SCENARIO_MOD_MAX =
          of(Integer.class, 3, "scenarioModMax");
    public static final CampaignOption<Integer> SCENARIO_MOD_CHANCE =
          of(Integer.class, 25, "scenarioModChance");
    public static final CampaignOption<Integer> SCENARIO_MOD_BV =
          of(Integer.class, 50, "scenarioModBV");
    public static final CampaignOption<Boolean> AUTO_CONFIG_MUNITIONS =
          of(Boolean.class, true, "autoConfigMunitions");
    public static final CampaignOption<AutoResolveMethod> AUTO_RESOLVE_METHOD =
          of(AutoResolveMethod.class, AutoResolveMethod.PRINCESS, "autoResolveMethod");
    public static final CampaignOption<Boolean> AUTO_RESOLVE_VICTORY_CHANCE_ENABLED =
          of(Boolean.class, false, "autoResolveVictoryChanceEnabled");
    public static final CampaignOption<Integer> AUTO_RESOLVE_NUMBER_OF_SCENARIOS =
          of(Integer.class, 100, "autoResolveNumberOfScenarios");
    public static final CampaignOption<Boolean> AUTO_RESOLVE_EXPERIMENTAL_PACAR_GUI_ENABLED =
          of(Boolean.class, false, "autoResolveUseExperimentalPacarGui");
    public static final CampaignOption<Boolean> AUTO_GENERATE_OP_FOR_CALL_SIGNS =
          of(Boolean.class, true, "autoGenerateOpForCallSigns");
    public static final CampaignOption<SkillLevel> MINIMUM_CALLSIGN_SKILL_LEVEL =
          of(SkillLevel.class, SkillLevel.VETERAN, "minimumCallsignSkillLevel");
    public static final CampaignOption<Boolean> TRACK_FACTION_STANDING =
          of(Boolean.class, false, "trackFactionStanding");
    public static final CampaignOption<Boolean> TRACK_CLIMATE_REGARD_CHANGES =
          of(Boolean.class, false, "trackClimateRegardChanges");
    public static final CampaignOption<Boolean> USE_FACTION_STANDING_NEGOTIATION =
          of(Boolean.class, true, "useFactionStandingNegotiation");
    public static final CampaignOption<Boolean> USE_FACTION_STANDING_RESUPPLY =
          of(Boolean.class, true, "useFactionStandingResupply");
    public static final CampaignOption<Boolean> USE_FACTION_STANDING_COMMAND_CIRCUIT =
          of(Boolean.class, true, "useFactionStandingCommandCircuit");
    public static final CampaignOption<Boolean> USE_FACTION_STANDING_BATCHALL_RESTRICTIONS =
          of(Boolean.class, true, "useFactionStandingBatchallRestrictions");
    public static final CampaignOption<Boolean> USE_FACTION_STANDING_RECRUITMENT =
          of(Boolean.class, true, "useFactionStandingRecruitment");
    public static final CampaignOption<Boolean> USE_FACTION_STANDING_BARRACKS_COSTS =
          of(Boolean.class, true, "useFactionStandingBarracksCosts");
    public static final CampaignOption<Boolean> USE_FACTION_STANDING_UNIT_MARKET =
          of(Boolean.class, true, "useFactionStandingUnitMarket");
    public static final CampaignOption<Boolean> USE_FACTION_STANDING_CONTRACT_PAY =
          of(Boolean.class, true, "useFactionStandingContractPay");
    public static final CampaignOption<Boolean> USE_FACTION_STANDING_SUPPORT_POINTS =
          of(Boolean.class, true, "useFactionStandingSupportPoints");
    public static final CampaignOption<Double> REGARD_MULTIPLIER =
          of(Double.class, 1.0, "factionStandingGainMultiplier");
    public static final CampaignOption<Money[]> ROLE_BASE_SALARIES = ofMutable(Money[].class, () -> new Money[0],
          "salaryTypeBase");
    public static final CampaignOption<Boolean> USE_AMMO_BY_TYPE =
          of(Boolean.class, false, "useAmmoByType");
    public static final CampaignOption<Boolean> USE_ADVANCED_MEDICAL =
          of(Boolean.class, false, "useAdvancedMedical");
    public static final CampaignOption<Boolean> USE_DYLANS_RANDOM_XP =
          of(Boolean.class, false, "useDylansRandomXP");
    public static final CampaignOption<Boolean> USE_PERCENTAGE_MAINTENANCE =
          of(Boolean.class, false, "usePercentageMaint");
    public static final CampaignOption<Boolean> INFANTRY_DONT_COUNT =
          of(Boolean.class, false, "infantryDontCount");
    public static final CampaignOption<Boolean> USE_FACTION_STANDING_OUTLAWED =
          of(Boolean.class, true, "useFactionStandingOutlawed");
    public static final CampaignOption<Double> EQUIPMENT_CONTRACT_PERCENT =
          of(Double.class, 5.0, "equipmentContractPercent");
    public static final CampaignOption<Double> DROP_SHIP_CONTRACT_PERCENT =
          of(Double.class, 1.0, "dropShipContractPercent");
    public static final CampaignOption<Double> JUMP_SHIP_CONTRACT_PERCENT =
          of(Double.class, 0.0, "jumpShipContractPercent");
    public static final CampaignOption<Double> WAR_SHIP_CONTRACT_PERCENT =
          of(Double.class, 0.0, "warShipContractPercent");
    public static final CampaignOption<String> STRATEGIC_VIEW_MINIMAP_THEME =
          of(String.class, "gbc green.theme", "strategicViewTheme");
    public static final CampaignOption<String> PERSONNEL_MARKET_NAME =
          of(String.class, "", "personnelMarketName");
    public static final CampaignOption<double[]> USED_PART_PRICE_MULTIPLIERS =
          ofMutable(double[].class, () -> new double[0], "usedPartPriceMultipliers");
    public static final CampaignOption<int[]> PHENOTYPE_PROBABILITIES =
          ofMutable(int[].class, () -> new int[0], "phenotypeProbabilities");
    public static final CampaignOption<boolean[]> USE_PORTRAIT_FOR_ROLE =
          ofMutable(boolean[].class, () -> new boolean[0], "usePortraitForType");
    public static final CampaignOption<int[]> ATB_BATTLE_CHANCE =
          ofMutable(int[].class, () -> new int[0], "atbBattleChance");
    public static final CampaignOption<EnumMap<PlanetarySophistication, Integer>> PLANET_TECH_ACQUISITION_BONUS =
          ofRaw(EnumMap.class, () -> new EnumMap<>(PlanetarySophistication.class), "planetTechAcquisitionBonus");
    public static final CampaignOption<EnumMap<PlanetaryRating, Integer>> PLANET_INDUSTRY_ACQUISITION_BONUS =
          ofRaw(EnumMap.class, () -> new EnumMap<>(PlanetaryRating.class), "planetIndustryAcquisitionBonus");
    public static final CampaignOption<EnumMap<PlanetaryRating, Integer>> PLANET_OUTPUT_ACQUISITION_BONUS =
          ofRaw(EnumMap.class, () -> new EnumMap<>(PlanetaryRating.class), "planetOutputAcquisitionBonus");
    public static final CampaignOption<Map<SkillLevel, Integer>> PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS =
          ofRaw(Map.class, () -> new HashMap<>(), "personnelMarketRandomRemovalTargets");
    public static final CampaignOption<Boolean> LIMIT_CLAN_TECH =
          of(Boolean.class, true, "limitClanTech");
    public static final CampaignOption<Boolean> USE_TWIST_OF_FATE_SURVIVAL =
          of(Boolean.class, false, "useTwistOfFateSurvival");
    public static final CampaignOption<Boolean> USE_REPLACE_EDGE_AWARDS =
          of(Boolean.class, false, "useReplaceEdgeAwards");
    // endregion Migrated Options

    private final Class<T> type;
    private final Supplier<T> defaultSupplier;
    private final String xmlTag;

    private CampaignOption(final @Nonnull Class<T> type, final @Nonnull Supplier<T> defaultSupplier,
          final @Nonnull String xmlTag) {
        this.type = Objects.requireNonNull(type);
        this.defaultSupplier = Objects.requireNonNull(defaultSupplier);
        this.xmlTag = Objects.requireNonNull(xmlTag);
    }

    /**
     * Registers an option whose default is an immutable value safe to share across campaigns (primitives, enums,
     * strings).
     */
    private static <T> CampaignOption<T> of(final @Nonnull Class<T> type, final @Nonnull T defaultValue,
          final @Nonnull String xmlTag) {
        Objects.requireNonNull(defaultValue);
        return register(new CampaignOption<>(type, () -> defaultValue, xmlTag));
    }

    /**
     * Registers an option whose default is a mutable value (arrays, collections, {@code Money[]},
     * {@code RandomOriginOptions}). The supplier is invoked once per {@link CampaignOptionsStore} so each campaign gets
     * its own instance and no default is aliased between campaigns.
     */
    private static <T> CampaignOption<T> ofMutable(final @Nonnull Class<T> type,
          final @Nonnull Supplier<T> defaultSupplier, final @Nonnull String xmlTag) {
        return register(new CampaignOption<>(type, defaultSupplier, xmlTag));
    }

    /**
     * Registers an option whose value is a generic collection. The raw {@link Class} token is all the runtime cast
     * needs (generics are erased); the unchecked cast is confined here.
     */
    @SuppressWarnings("unchecked")
    private static <T> CampaignOption<T> ofRaw(final @Nonnull Class<?> rawType,
          final @Nonnull Supplier<T> defaultSupplier, final @Nonnull String xmlTag) {
        return register(new CampaignOption<>((Class<T>) rawType, defaultSupplier, xmlTag));
    }

    private static <T> CampaignOption<T> register(final @Nonnull CampaignOption<T> option) {
        ALL.add(option);
        return option;
    }

    /** @return an unmodifiable view of every registered option, in declaration order */
    public static @Nonnull List<CampaignOption<?>> values() {
        return Collections.unmodifiableList(ALL);
    }

    /** @return the type of this option's value */
    public @Nonnull Class<T> type() {
        return type;
    }

    /**
     * @return a fresh default value for this option; for mutable-valued options each call yields a new instance
     */
    public @Nonnull T defaultValue() {
        return defaultSupplier.get();
    }

    /** @return the XML tag under which this option is persisted */
    public @Nonnull String xmlTag() {
        return xmlTag;
    }
}
