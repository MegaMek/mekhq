/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.TechConstants.getSimpleLevel;
import static megamek.common.options.OptionsConstants.ADVANCED_STRATOPS_QUIRKS;
import static megamek.common.options.OptionsConstants.ALLOWED_CANON_ONLY;
import static megamek.common.options.OptionsConstants.ALLOWED_TECH_LEVEL;
import static megamek.common.options.OptionsConstants.EDGE;
import static megamek.common.options.OptionsConstants.RPG_ARTILLERY_SKILL;
import static megamek.common.options.OptionsConstants.RPG_COMMAND_INIT;
import static megamek.common.options.OptionsConstants.RPG_MANEI_DOMINI;
import static megamek.common.options.OptionsConstants.RPG_PILOT_ADVANTAGES;
import static megamek.common.options.OptionsConstants.RPG_TOUGHNESS;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.annotation.Nonnull;
import megamek.Version;
import megamek.common.TechConstants;
import megamek.common.enums.SkillLevel;
import megamek.common.options.GameOptions;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.digitalGM.stratCon.StratConPlayType;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.randomEvents.prisoners.PrisonerCaptureStyle;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;
import mekhq.service.mrms.MRMSOption;
import org.w3c.dom.Node;

/**
 * @author natit
 */
public class CampaignOptions {
    private static final MMLogger LOGGER = MMLogger.create(CampaignOptions.class);
    private static final ClientPreferences CLIENT_PREFERENCES = PreferenceManager.getClientPreferences();
    // region Magic Numbers
    public static final int TECH_INTRO = 0;
    public static final int TECH_STANDARD = 1;
    public static final int TECH_ADVANCED = 2;
    public static final int TECH_EXPERIMENTAL = 3;
    public static final int TECH_UNOFFICIAL = 4;
    // This must always be the highest tech level to hide parts
    // that haven't been invented yet, or that are completely extinct
    public static final int TECH_UNKNOWN = 5;

    public static final int TRANSIT_UNIT_WEEK = 1;
    public static final int TRANSIT_UNIT_MONTH = 2;

    public static final double MAXIMUM_COMBAT_EQUIPMENT_PERCENT = 5.0;
    public static final double MAXIMUM_DROPSHIP_EQUIPMENT_PERCENT = 1.0;
    public static final double MAXIMUM_JUMPSHIP_EQUIPMENT_PERCENT = 1.0;
    public static final double MAXIMUM_WARSHIP_EQUIPMENT_PERCENT = 1.0;

    public static final int REPUTATION_PERFORMANCE_CUT_OFF_YEARS = 10;

    public static final int EDGE_AWARD_REPLACEMENT_XP = 10;

    public static String getTechLevelName(final int techLevel) {
        return switch (techLevel) {
            case TECH_INTRO -> TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_INTRO];
            case TECH_STANDARD -> TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_STANDARD];
            case TECH_ADVANCED -> TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_ADVANCED];
            case TECH_EXPERIMENTAL -> TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_EXPERIMENTAL];
            case TECH_UNOFFICIAL -> TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_UNOFFICIAL];
            default -> "Unknown";
        };
    }
    // endregion Magic Numbers

    // region Variable Declarations
    // Typed-key store backing options that have been migrated off dedicated fields (see CampaignOption).
    // Initialized here so it is ready before any migrated setter runs in the constructor body.
    private final CampaignOptionsStore options = new CampaignOptionsStore(CampaignOption.values());
    // endregion Variable Declarations

    // region Constructors
    public CampaignOptions() {
        // Initialize any reused variables
        final PersonnelRole[] personnelRoles = PersonnelRole.values();

        for (final PartRepairType type : PartRepairType.values()) {
            getMRMSOptions().add(new MRMSOption(type));
        }
        getAllPlanetTechAcquisitionBonuses().put(PlanetarySophistication.ADVANCED, -2); // TODO: needs to be verified
        getAllPlanetTechAcquisitionBonuses().put(PlanetarySophistication.A, -1);
        getAllPlanetTechAcquisitionBonuses().put(PlanetarySophistication.B, 0);
        getAllPlanetTechAcquisitionBonuses().put(PlanetarySophistication.C, 1);
        getAllPlanetTechAcquisitionBonuses().put(PlanetarySophistication.D, 2);
        getAllPlanetTechAcquisitionBonuses().put(PlanetarySophistication.F, 8);
        getAllPlanetTechAcquisitionBonuses().put(PlanetarySophistication.REGRESSED, 16); // TODO: needs to be verified
        getAllPlanetIndustryAcquisitionBonuses().put(PlanetaryRating.A, 0);
        getAllPlanetIndustryAcquisitionBonuses().put(PlanetaryRating.B, 0);
        getAllPlanetIndustryAcquisitionBonuses().put(PlanetaryRating.C, 0);
        getAllPlanetIndustryAcquisitionBonuses().put(PlanetaryRating.D, 0);
        getAllPlanetIndustryAcquisitionBonuses().put(PlanetaryRating.F, 0);
        getAllPlanetOutputAcquisitionBonuses().put(PlanetaryRating.A, -1);
        getAllPlanetOutputAcquisitionBonuses().put(PlanetaryRating.B, 0);
        getAllPlanetOutputAcquisitionBonuses().put(PlanetaryRating.C, 1);
        getAllPlanetOutputAcquisitionBonuses().put(PlanetaryRating.D, 2);
        getAllPlanetOutputAcquisitionBonuses().put(PlanetaryRating.F, 8);
        setUseAmmoByType(false);
        setUseAdvancedMedical(false);
        getSalaryXPMultipliers().put(SkillLevel.NONE, 0.5);
        getSalaryXPMultipliers().put(SkillLevel.ULTRA_GREEN, 0.6);
        getSalaryXPMultipliers().put(SkillLevel.GREEN, 0.6);
        getSalaryXPMultipliers().put(SkillLevel.REGULAR, 1.0);
        getSalaryXPMultipliers().put(SkillLevel.VETERAN, 1.6);
        getSalaryXPMultipliers().put(SkillLevel.ELITE, 3.2);
        getSalaryXPMultipliers().put(SkillLevel.HEROIC, 6.4);
        getSalaryXPMultipliers().put(SkillLevel.LEGENDARY, 12.8);
        setRoleBaseSalaries(new Money[personnelRoles.length]);
        for (PersonnelRole role : personnelRoles) {
            setRoleBaseSalary(role, 250);
        }
        setRoleBaseSalary(PersonnelRole.MEKWARRIOR, 1500);
        setRoleBaseSalary(PersonnelRole.LAM_PILOT, 2250);
        setRoleBaseSalary(PersonnelRole.VEHICLE_CREW_GROUND, 900);
        setRoleBaseSalary(PersonnelRole.VEHICLE_CREW_NAVAL, 900);
        setRoleBaseSalary(PersonnelRole.VEHICLE_CREW_VTOL, 900);
        setRoleBaseSalary(PersonnelRole.AEROSPACE_PILOT, 1500);
        setRoleBaseSalary(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT, 900);
        setRoleBaseSalary(PersonnelRole.PROTOMEK_PILOT, 960);
        setRoleBaseSalary(PersonnelRole.BATTLE_ARMOUR, 960);
        setRoleBaseSalary(PersonnelRole.SOLDIER, 750);
        setRoleBaseSalary(PersonnelRole.VESSEL_PILOT, 1000);
        setRoleBaseSalary(PersonnelRole.VESSEL_GUNNER, 1000);
        setRoleBaseSalary(PersonnelRole.VESSEL_CREW, 1000);
        setRoleBaseSalary(PersonnelRole.VESSEL_NAVIGATOR, 1000);
        setRoleBaseSalary(PersonnelRole.MEK_TECH, 800);
        setRoleBaseSalary(PersonnelRole.MECHANIC, 800);
        setRoleBaseSalary(PersonnelRole.AERO_TEK, 800);
        setRoleBaseSalary(PersonnelRole.BA_TECH, 800);
        setRoleBaseSalary(PersonnelRole.ASTECH, 400);
        setRoleBaseSalary(PersonnelRole.DOCTOR, 1500);
        setRoleBaseSalary(PersonnelRole.MEDIC, 400);
        setRoleBaseSalary(PersonnelRole.ADMINISTRATOR_COMMAND, 500);
        setRoleBaseSalary(PersonnelRole.ADMINISTRATOR_LOGISTICS, 500);
        setRoleBaseSalary(PersonnelRole.ADMINISTRATOR_TRANSPORT, 500);
        setRoleBaseSalary(PersonnelRole.ADMINISTRATOR_HR, 500);
        setRoleBaseSalary(PersonnelRole.NOBLE, 2500);
        setRoleBaseSalary(PersonnelRole.DEPENDENT, 50);
        setRoleBaseSalary(PersonnelRole.NONE, 0);
        setUseDylansRandomXP(false);

        getMarriageSurnameWeights().put(MergingSurnameStyle.NO_CHANGE, 100);
        getMarriageSurnameWeights().put(MergingSurnameStyle.YOURS, 55);
        getMarriageSurnameWeights().put(MergingSurnameStyle.SPOUSE, 55);
        getMarriageSurnameWeights().put(MergingSurnameStyle.SPACE_YOURS, 10);
        getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_SPACE_YOURS, 5);
        getMarriageSurnameWeights().put(MergingSurnameStyle.HYPHEN_YOURS, 30);
        getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_HYPHEN_YOURS, 20);
        getMarriageSurnameWeights().put(MergingSurnameStyle.SPACE_SPOUSE, 10);
        getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_SPACE_SPOUSE, 5);
        getMarriageSurnameWeights().put(MergingSurnameStyle.HYPHEN_SPOUSE, 30);
        getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_HYPHEN_SPOUSE, 20);
        getMarriageSurnameWeights().put(MergingSurnameStyle.MALE, 500);
        getMarriageSurnameWeights().put(MergingSurnameStyle.FEMALE, 160);

        getDivorceSurnameWeights().put(SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME, 10);
        getDivorceSurnameWeights().put(SplittingSurnameStyle.SPOUSE_CHANGES_SURNAME, 10);
        getDivorceSurnameWeights().put(SplittingSurnameStyle.BOTH_CHANGE_SURNAME, 30);
        getDivorceSurnameWeights().put(SplittingSurnameStyle.BOTH_KEEP_SURNAME, 50);

        getEnabledRandomDeathAgeGroups().put(AgeGroup.ELDER, true);
        getEnabledRandomDeathAgeGroups().put(AgeGroup.ADULT, true);
        getEnabledRandomDeathAgeGroups().put(AgeGroup.TEENAGER, true);
        getEnabledRandomDeathAgeGroups().put(AgeGroup.PRETEEN, false);
        getEnabledRandomDeathAgeGroups().put(AgeGroup.CHILD, false);
        getEnabledRandomDeathAgeGroups().put(AgeGroup.TODDLER, false);
        getEnabledRandomDeathAgeGroups().put(AgeGroup.BABY, false);
        setUsePercentageMaintenance(false);
        setUseInfantryDontCount(false);

        setUsedPartPriceMultipliers(0.1, 0.2, 0.3, 0.5, 0.7, 0.9);

        setEquipmentContractPercent(5.0);
        setDropShipContractPercent(1.0);
        setJumpShipContractPercent(0.0);
        setWarShipContractPercent(0.0);

        set(CampaignOption.PHENOTYPE_PROBABILITIES, new int[Phenotype.getExternalPhenotypes().size()]);
        getPhenotypeProbabilities()[Phenotype.MEKWARRIOR.ordinal()] = 95;
        getPhenotypeProbabilities()[Phenotype.ELEMENTAL.ordinal()] = 100;
        getPhenotypeProbabilities()[Phenotype.AEROSPACE.ordinal()] = 95;
        getPhenotypeProbabilities()[Phenotype.VEHICLE.ordinal()] = 0;
        getPhenotypeProbabilities()[Phenotype.PROTOMEK.ordinal()] = 95;
        getPhenotypeProbabilities()[Phenotype.NAVAL.ordinal()] = 25;

        set(CampaignOption.USE_PORTRAIT_FOR_ROLE, new boolean[personnelRoles.length]);
        Arrays.fill(isUsePortraitForRoles(), false);
        isUsePortraitForRoles()[PersonnelRole.MEKWARRIOR.ordinal()] = true;

        setPersonnelMarketName(PersonnelMarket.getTypeName(PersonnelMarket.TYPE_NONE));
        setPersonnelMarketRandomRemovalTargets(new HashMap<>());
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.NONE, 3);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.ULTRA_GREEN, 4);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.GREEN, 4);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.REGULAR, 6);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.VETERAN, 8);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.ELITE, 10);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.HEROIC, 11);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.LEGENDARY, 11);

        set(CampaignOption.STRATEGIC_VIEW_MINIMAP_THEME, "gbc green.theme");

        set(CampaignOption.ATB_BATTLE_CHANCE, new int[CombatRole.values().length - 1]);
        getAllAtBBattleChances()[CombatRole.MANEUVER.ordinal()] = 40;
        getAllAtBBattleChances()[CombatRole.FRONTLINE.ordinal()] = 20;
        getAllAtBBattleChances()[CombatRole.PATROL.ordinal()] = 60;
        getAllAtBBattleChances()[CombatRole.TRAINING.ordinal()] = 10;
        getAllAtBBattleChances()[CombatRole.CADRE.ordinal()] = 10;

        setUseFactionStandingOutlawed(true);
    }

    /**
     * Returns the current value of the given campaign option.
     *
     * @param option the option to read
     * @param <T>    the option's value type
     *
     * @return the option's current value
     */
    public @Nonnull <T> T get(final @Nonnull CampaignOption<T> option) {
        Objects.requireNonNull(option);
        return options.get(option);
    }

    /**
     * Sets the value of the given campaign option.
     *
     * @param option the option to write
     * @param value  the new value
     * @param <T>    the option's value type
     */
    public <T> void set(final @Nonnull CampaignOption<T> option, final @Nonnull T value) {
        Objects.requireNonNull(option);
        Objects.requireNonNull(value);
        options.set(option, value);
    }
    // endregion Constructors

    // region General Tab

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getManualUnitRatingModifier() {
        return get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MANUAL_UNIT_RATING_MODIFIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setManualUnitRatingModifier(final int manualUnitRatingModifier) {
        set(CampaignOption.MANUAL_UNIT_RATING_MODIFIER, manualUnitRatingModifier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.REQUIRE_SUPPORT_FORCE_TRANSPORTATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isRequireSupportForceTransportation() {
        return get(CampaignOption.REQUIRE_SUPPORT_FORCE_TRANSPORTATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.REQUIRE_SUPPORT_FORCE_TRANSPORTATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRequireSupportForceTransportation(final boolean requireSupportForceTransportation) {
        set(CampaignOption.REQUIRE_SUPPORT_FORCE_TRANSPORTATION, requireSupportForceTransportation);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.CLAMP_REPUTATION_PAY_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isClampReputationPayMultiplier() {
        return get(CampaignOption.CLAMP_REPUTATION_PAY_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CLAMP_REPUTATION_PAY_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setClampReputationPayMultiplier(final boolean clampReputationPayMultiplier) {
        set(CampaignOption.CLAMP_REPUTATION_PAY_MULTIPLIER, clampReputationPayMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.REDUCE_REPUTATION_PERFORMANCE_MODIFIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isReduceReputationPerformanceModifier() {
        return get(CampaignOption.REDUCE_REPUTATION_PERFORMANCE_MODIFIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.REDUCE_REPUTATION_PERFORMANCE_MODIFIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setReduceReputationPerformanceModifier(final boolean reduceReputationPerformanceModifier) {
        set(CampaignOption.REDUCE_REPUTATION_PERFORMANCE_MODIFIER, reduceReputationPerformanceModifier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.REPUTATION_PERFORMANCE_MODIFIER_CUT_OFF)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isReputationPerformanceModifierCutOff() {
        return get(CampaignOption.REPUTATION_PERFORMANCE_MODIFIER_CUT_OFF);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.REPUTATION_PERFORMANCE_MODIFIER_CUT_OFF, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setReputationPerformanceModifierCutOff(final boolean reputationPerformanceModifierCutOff) {
        set(CampaignOption.REPUTATION_PERFORMANCE_MODIFIER_CUT_OFF, reputationPerformanceModifierCutOff);
    }
    // endregion General Tab

    // region Repair and Maintenance Tab
    // region Repair
    // endregion Repair

    // region Maintenance

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.CHECK_MAINTENANCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isCheckMaintenance() {
        return get(CampaignOption.CHECK_MAINTENANCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.CHECK_MAINTENANCE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setCheckMaintenance(final boolean checkMaintenance) {
        set(CampaignOption.CHECK_MAINTENANCE, checkMaintenance);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MAINTENANCE_CYCLE_DAYS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMaintenanceCycleDays() {
        return get(CampaignOption.MAINTENANCE_CYCLE_DAYS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MAINTENANCE_CYCLE_DAYS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMaintenanceCycleDays(final int maintenanceCycleDays) {
        set(CampaignOption.MAINTENANCE_CYCLE_DAYS, maintenanceCycleDays);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MAINTENANCE_BONUS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMaintenanceBonus() {
        return get(CampaignOption.MAINTENANCE_BONUS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MAINTENANCE_BONUS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMaintenanceBonus(final int maintenanceBonus) {
        set(CampaignOption.MAINTENANCE_BONUS, maintenanceBonus);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_QUALITY_MAINTENANCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseQualityMaintenance() {
        return get(CampaignOption.USE_QUALITY_MAINTENANCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_QUALITY_MAINTENANCE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseQualityMaintenance(final boolean useQualityMaintenance) {
        set(CampaignOption.USE_QUALITY_MAINTENANCE, useQualityMaintenance);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.REVERSE_QUALITY_NAMES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isReverseQualityNames() {
        return get(CampaignOption.REVERSE_QUALITY_NAMES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.REVERSE_QUALITY_NAMES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setReverseQualityNames(final boolean reverseQualityNames) {
        set(CampaignOption.REVERSE_QUALITY_NAMES, reverseQualityNames);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_RANDOM_UNIT_QUALITIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomUnitQualities() {
        return get(CampaignOption.USE_RANDOM_UNIT_QUALITIES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_UNIT_QUALITIES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomUnitQualities(final boolean useRandomUnitQualities) {
        set(CampaignOption.USE_RANDOM_UNIT_QUALITIES, useRandomUnitQualities);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_PLANETARY_MODIFIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUsePlanetaryModifiers() {
        return get(CampaignOption.USE_PLANETARY_MODIFIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_PLANETARY_MODIFIERS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUsePlanetaryModifiers(final boolean usePlanetaryModifiers) {
        set(CampaignOption.USE_PLANETARY_MODIFIERS, usePlanetaryModifiers);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_UNOFFICIAL_MAINTENANCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseUnofficialMaintenance() {
        return get(CampaignOption.USE_UNOFFICIAL_MAINTENANCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_UNOFFICIAL_MAINTENANCE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseUnofficialMaintenance(final boolean useUnofficialMaintenance) {
        set(CampaignOption.USE_UNOFFICIAL_MAINTENANCE, useUnofficialMaintenance);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.LOG_MAINTENANCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isLogMaintenance() {
        return get(CampaignOption.LOG_MAINTENANCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.LOG_MAINTENANCE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setLogMaintenance(final boolean logMaintenance) {
        set(CampaignOption.LOG_MAINTENANCE, logMaintenance);
    }

    /**
     * @return the default maintenance time in minutes
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DEFAULT_MAINTENANCE_TIME)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getDefaultMaintenanceTime() {
        return get(CampaignOption.DEFAULT_MAINTENANCE_TIME);
    }

    /**
     * Sets the default maintenance time.
     *
     * @param defaultMaintenanceTime the default maintenance time multiplier
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DEFAULT_MAINTENANCE_TIME, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDefaultMaintenanceTime(final int defaultMaintenanceTime) {
        set(CampaignOption.DEFAULT_MAINTENANCE_TIME, defaultMaintenanceTime);
    }
    // endregion Maintenance

    // region Mass Repair/ Mass Salvage

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MRMS_USE_REPAIR)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isMRMSUseRepair() {
        return get(CampaignOption.MRMS_USE_REPAIR);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MRMS_USE_REPAIR,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMRMSUseRepair(final boolean mrmsUseRepair) {
        set(CampaignOption.MRMS_USE_REPAIR, mrmsUseRepair);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MRMS_USE_SALVAGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isMRMSUseSalvage() {
        return get(CampaignOption.MRMS_USE_SALVAGE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MRMS_USE_SALVAGE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMRMSUseSalvage(final boolean mrmsUseSalvage) {
        set(CampaignOption.MRMS_USE_SALVAGE, mrmsUseSalvage);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MRMS_USE_EXTRA_TIME)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isMRMSUseExtraTime() {
        return get(CampaignOption.MRMS_USE_EXTRA_TIME);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MRMS_USE_EXTRA_TIME,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMRMSUseExtraTime(final boolean mrmsUseExtraTime) {
        set(CampaignOption.MRMS_USE_EXTRA_TIME, mrmsUseExtraTime);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MRMS_USE_RUSH_JOB)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isMRMSUseRushJob() {
        return get(CampaignOption.MRMS_USE_RUSH_JOB);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MRMS_USE_RUSH_JOB,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMRMSUseRushJob(final boolean mrmsUseRushJob) {
        set(CampaignOption.MRMS_USE_RUSH_JOB, mrmsUseRushJob);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MRMS_ALLOW_CARRYOVER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isMRMSAllowCarryover() {
        return get(CampaignOption.MRMS_ALLOW_CARRYOVER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MRMS_ALLOW_CARRYOVER,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMRMSAllowCarryover(final boolean mrmsAllowCarryover) {
        set(CampaignOption.MRMS_ALLOW_CARRYOVER, mrmsAllowCarryover);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.MRMS_OPTIMIZE_TO_COMPLETE_TODAY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isMRMSOptimizeToCompleteToday() {
        return get(CampaignOption.MRMS_OPTIMIZE_TO_COMPLETE_TODAY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MRMS_OPTIMIZE_TO_COMPLETE_TODAY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMRMSOptimizeToCompleteToday(final boolean mrmsOptimizeToCompleteToday) {
        set(CampaignOption.MRMS_OPTIMIZE_TO_COMPLETE_TODAY, mrmsOptimizeToCompleteToday);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MRMS_SCRAP_IMPOSSIBLE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isMRMSScrapImpossible() {
        return get(CampaignOption.MRMS_SCRAP_IMPOSSIBLE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MRMS_SCRAP_IMPOSSIBLE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMRMSScrapImpossible(final boolean mrmsScrapImpossible) {
        set(CampaignOption.MRMS_SCRAP_IMPOSSIBLE, mrmsScrapImpossible);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.MRMS_USE_ASSIGNED_TECHS_FIRST)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isMRMSUseAssignedTechsFirst() {
        return get(CampaignOption.MRMS_USE_ASSIGNED_TECHS_FIRST);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MRMS_USE_ASSIGNED_TECHS_FIRST, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMRMSUseAssignedTechsFirst(final boolean mrmsUseAssignedTechsFirst) {
        set(CampaignOption.MRMS_USE_ASSIGNED_TECHS_FIRST, mrmsUseAssignedTechsFirst);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MRMS_REPLACE_POD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isMRMSReplacePod() {
        return get(CampaignOption.MRMS_REPLACE_POD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MRMS_REPLACE_POD,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMRMSReplacePod(final boolean mrmsReplacePod) {
        set(CampaignOption.MRMS_REPLACE_POD, mrmsReplacePod);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MRMS_OPTIONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public List<MRMSOption> getMRMSOptions() {
        return get(CampaignOption.MRMS_OPTIONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MRMS_OPTIONS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMRMSOptions(final List<MRMSOption> mrmsOptions) {
        set(CampaignOption.MRMS_OPTIONS, mrmsOptions);
    }

    public void addMRMSOption(final MRMSOption mrmsOption) {
        if (mrmsOption.getType().isUnknownLocation()) {
            return;
        }

        getMRMSOptions().removeIf(option -> option.getType() == mrmsOption.getType());
        getMRMSOptions().add(mrmsOption);
    }
    // endregion Mass Repair/ Mass Salvage
    // endregion Repair and Maintenance Tab

    // region Supplies and Acquisitions Tab
    // endregion Supplies and Acquisitions Tab

    // region Personnel Tab
    // region General Personnel

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_TACTICS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseTactics() {
        return get(CampaignOption.USE_TACTICS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_TACTICS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseTactics(final boolean useTactics) {
        set(CampaignOption.USE_TACTICS, useTactics);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_INITIATIVE_BONUS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseInitiativeBonus() {
        return get(CampaignOption.USE_INITIATIVE_BONUS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_INITIATIVE_BONUS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseInitiativeBonus(final boolean useInitiativeBonus) {
        set(CampaignOption.USE_INITIATIVE_BONUS, useInitiativeBonus);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_SENSIBLE_TACTICS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseSensibleTactics() {
        return get(CampaignOption.USE_SENSIBLE_TACTICS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_SENSIBLE_TACTICS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseSensibleTactics(final boolean useSensibleTactics) {
        set(CampaignOption.USE_SENSIBLE_TACTICS, useSensibleTactics);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_TOUGHNESS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseToughness() {
        return get(CampaignOption.USE_TOUGHNESS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_TOUGHNESS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseToughness(final boolean useToughness) {
        set(CampaignOption.USE_TOUGHNESS, useToughness);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_RANDOM_TOUGHNESS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomToughness() {
        return get(CampaignOption.USE_RANDOM_TOUGHNESS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_RANDOM_TOUGHNESS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomToughness(final boolean useRandomToughness) {
        set(CampaignOption.USE_RANDOM_TOUGHNESS, useRandomToughness);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_ARTILLERY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseArtillery() {
        return get(CampaignOption.USE_ARTILLERY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_ARTILLERY,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseArtillery(final boolean useArtillery) {
        set(CampaignOption.USE_ARTILLERY, useArtillery);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_ABILITIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseAbilities() {
        return get(CampaignOption.USE_ABILITIES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_ABILITIES,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseAbilities(final boolean useAbilities) {
        set(CampaignOption.USE_ABILITIES, useAbilities);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ONLY_COMMANDERS_MATTER_VEHICLES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isOnlyCommandersMatterVehicles() {
        return get(CampaignOption.ONLY_COMMANDERS_MATTER_VEHICLES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ONLY_COMMANDERS_MATTER_VEHICLES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setOnlyCommandersMatterVehicles(final boolean onlyCommandersMatterVehicles) {
        set(CampaignOption.ONLY_COMMANDERS_MATTER_VEHICLES, onlyCommandersMatterVehicles);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ONLY_COMMANDERS_MATTER_INFANTRY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isOnlyCommandersMatterInfantry() {
        return get(CampaignOption.ONLY_COMMANDERS_MATTER_INFANTRY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ONLY_COMMANDERS_MATTER_INFANTRY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setOnlyCommandersMatterInfantry(final boolean onlyCommandersMatterInfantry) {
        set(CampaignOption.ONLY_COMMANDERS_MATTER_INFANTRY, onlyCommandersMatterInfantry);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ONLY_COMMANDERS_MATTER_BATTLE_ARMOR)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isOnlyCommandersMatterBattleArmor() {
        return get(CampaignOption.ONLY_COMMANDERS_MATTER_BATTLE_ARMOR);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ONLY_COMMANDERS_MATTER_BATTLE_ARMOR, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setOnlyCommandersMatterBattleArmor(final boolean onlyCommandersMatterBattleArmor) {
        set(CampaignOption.ONLY_COMMANDERS_MATTER_BATTLE_ARMOR, onlyCommandersMatterBattleArmor);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.EDGE_REFRESH_PERIOD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public EdgeRefreshPeriod getEdgeRefreshPeriod() {
        return get(CampaignOption.EDGE_REFRESH_PERIOD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.EDGE_REFRESH_PERIOD,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEdgeRefreshPeriod(final EdgeRefreshPeriod edgeRefreshPeriod) {
        set(CampaignOption.EDGE_REFRESH_PERIOD, edgeRefreshPeriod);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_EDGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseEdge() {
        return get(CampaignOption.USE_EDGE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_EDGE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseEdge(final boolean useEdge) {
        set(CampaignOption.USE_EDGE, useEdge);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_IMPLANTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseImplants() {
        return get(CampaignOption.USE_IMPLANTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_IMPLANTS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseImplants(final boolean useImplants) {
        set(CampaignOption.USE_IMPLANTS, useImplants);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ALTERNATIVE_QUALITY_AVERAGING)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAlternativeQualityAveraging() {
        return get(CampaignOption.ALTERNATIVE_QUALITY_AVERAGING);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ALTERNATIVE_QUALITY_AVERAGING, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAlternativeQualityAveraging(final boolean alternativeQualityAveraging) {
        set(CampaignOption.ALTERNATIVE_QUALITY_AVERAGING, alternativeQualityAveraging);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_AGE_EFFECTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseAgeEffects() {
        return get(CampaignOption.USE_AGE_EFFECTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_AGE_EFFECTS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseAgeEffects(final boolean useAgeEffects) {
        set(CampaignOption.USE_AGE_EFFECTS, useAgeEffects);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_TRANSFERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseTransfers() {
        return get(CampaignOption.USE_TRANSFERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_TRANSFERS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseTransfers(final boolean useTransfers) {
        set(CampaignOption.USE_TRANSFERS, useTransfers);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_EXTENDED_TOE_FORCE_NAME)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseExtendedTOEForceName() {
        return get(CampaignOption.USE_EXTENDED_TOE_FORCE_NAME);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_EXTENDED_TOE_FORCE_NAME, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseExtendedTOEForceName(final boolean useExtendedTOEForceName) {
        set(CampaignOption.USE_EXTENDED_TOE_FORCE_NAME, useExtendedTOEForceName);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PERSONNEL_LOG_SKILL_GAIN)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPersonnelLogSkillGain() {
        return get(CampaignOption.PERSONNEL_LOG_SKILL_GAIN);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PERSONNEL_LOG_SKILL_GAIN, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPersonnelLogSkillGain(final boolean personnelLogSkillGain) {
        set(CampaignOption.PERSONNEL_LOG_SKILL_GAIN, personnelLogSkillGain);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PERSONNEL_LOG_ABILITY_GAIN)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPersonnelLogAbilityGain() {
        return get(CampaignOption.PERSONNEL_LOG_ABILITY_GAIN);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PERSONNEL_LOG_ABILITY_GAIN, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPersonnelLogAbilityGain(final boolean personnelLogAbilityGain) {
        set(CampaignOption.PERSONNEL_LOG_ABILITY_GAIN, personnelLogAbilityGain);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PERSONNEL_LOG_EDGE_GAIN)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPersonnelLogEdgeGain() {
        return get(CampaignOption.PERSONNEL_LOG_EDGE_GAIN);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PERSONNEL_LOG_EDGE_GAIN, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPersonnelLogEdgeGain(final boolean personnelLogEdgeGain) {
        set(CampaignOption.PERSONNEL_LOG_EDGE_GAIN, personnelLogEdgeGain);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DISPLAY_PERSONNEL_LOG)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDisplayPersonnelLog() {
        return get(CampaignOption.DISPLAY_PERSONNEL_LOG);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DISPLAY_PERSONNEL_LOG, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisplayPersonnelLog(final boolean displayPersonnelLog) {
        set(CampaignOption.DISPLAY_PERSONNEL_LOG, displayPersonnelLog);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DISPLAY_SCENARIO_LOG)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDisplayScenarioLog() {
        return get(CampaignOption.DISPLAY_SCENARIO_LOG);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.DISPLAY_SCENARIO_LOG,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisplayScenarioLog(final boolean displayScenarioLog) {
        set(CampaignOption.DISPLAY_SCENARIO_LOG, displayScenarioLog);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DISPLAY_KILL_RECORD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDisplayKillRecord() {
        return get(CampaignOption.DISPLAY_KILL_RECORD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.DISPLAY_KILL_RECORD,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisplayKillRecord(final boolean displayKillRecord) {
        set(CampaignOption.DISPLAY_KILL_RECORD, displayKillRecord);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DISPLAY_MEDICAL_RECORD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDisplayMedicalRecord() {
        return get(CampaignOption.DISPLAY_MEDICAL_RECORD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DISPLAY_MEDICAL_RECORD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisplayMedicalRecord(final boolean displayMedicalRecord) {
        set(CampaignOption.DISPLAY_MEDICAL_RECORD, displayMedicalRecord);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DISPLAY_PATIENT_RECORD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDisplayPatientRecord() {
        return get(CampaignOption.DISPLAY_PATIENT_RECORD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DISPLAY_PATIENT_RECORD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisplayPatientRecord(final boolean displayPatientRecord) {
        set(CampaignOption.DISPLAY_PATIENT_RECORD, displayPatientRecord);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DISPLAY_ASSIGNMENT_RECORD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDisplayAssignmentRecord() {
        return get(CampaignOption.DISPLAY_ASSIGNMENT_RECORD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DISPLAY_ASSIGNMENT_RECORD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisplayAssignmentRecord(final boolean displayAssignmentRecord) {
        set(CampaignOption.DISPLAY_ASSIGNMENT_RECORD, displayAssignmentRecord);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DISPLAY_PERFORMANCE_RECORD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDisplayPerformanceRecord() {
        return get(CampaignOption.DISPLAY_PERFORMANCE_RECORD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DISPLAY_PERFORMANCE_RECORD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisplayPerformanceRecord(final boolean displayPerformanceRecord) {
        set(CampaignOption.DISPLAY_PERFORMANCE_RECORD, displayPerformanceRecord);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AWARD_VETERANCY_SP_AS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAwardVeterancySPAs() {
        return get(CampaignOption.AWARD_VETERANCY_SP_AS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AWARD_VETERANCY_SP_AS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAwardVeterancySPAs(final boolean awardVeterancySPAs) {
        set(CampaignOption.AWARD_VETERANCY_SP_AS, awardVeterancySPAs);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.AWARD_RELEVANT_VETERANCY_SP_AS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAwardRelevantVeterancySPAs() {
        return get(CampaignOption.AWARD_RELEVANT_VETERANCY_SP_AS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AWARD_RELEVANT_VETERANCY_SP_AS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAwardRelevantVeterancySPAs(final boolean awardRelevantVeterancySPAs) {
        set(CampaignOption.AWARD_RELEVANT_VETERANCY_SP_AS, awardRelevantVeterancySPAs);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.REWARD_COMING_OF_AGE_ABILITIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isRewardComingOfAgeAbilities() {
        return get(CampaignOption.REWARD_COMING_OF_AGE_ABILITIES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.REWARD_COMING_OF_AGE_ABILITIES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRewardComingOfAgeAbilities(final boolean rewardComingOfAgeAbilities) {
        set(CampaignOption.REWARD_COMING_OF_AGE_ABILITIES, rewardComingOfAgeAbilities);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.REWARD_COMING_OF_AGE_RP_SKILLS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isRewardComingOfAgeRPSkills() {
        return get(CampaignOption.REWARD_COMING_OF_AGE_RP_SKILLS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.REWARD_COMING_OF_AGE_RP_SKILLS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRewardComingOfAgeRPSkills(final boolean rewardComingOfAgeRPSkills) {
        set(CampaignOption.REWARD_COMING_OF_AGE_RP_SKILLS, rewardComingOfAgeRPSkills);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_FATIGUE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFatigue() {
        return get(CampaignOption.USE_FATIGUE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_FATIGUE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFatigue(final boolean useFatigue) {
        set(CampaignOption.USE_FATIGUE, useFatigue);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.FATIGUE_RATE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getFatigueRate() {
        return get(CampaignOption.FATIGUE_RATE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.FATIGUE_RATE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setFatigueRate(final Integer fatigueRate) {
        set(CampaignOption.FATIGUE_RATE, fatigueRate);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_INJURY_FATIGUE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseInjuryFatigue() {
        return get(CampaignOption.USE_INJURY_FATIGUE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_INJURY_FATIGUE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseInjuryFatigue(final boolean useInjuryFatigue) {
        set(CampaignOption.USE_INJURY_FATIGUE, useInjuryFatigue);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.FIELD_KITCHEN_CAPACITY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getFieldKitchenCapacity() {
        return get(CampaignOption.FIELD_KITCHEN_CAPACITY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.FIELD_KITCHEN_CAPACITY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setFieldKitchenCapacity(final Integer fieldKitchenCapacity) {
        set(CampaignOption.FIELD_KITCHEN_CAPACITY, fieldKitchenCapacity);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.FIELD_KITCHEN_IGNORE_NON_COMBATANTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFieldKitchenIgnoreNonCombatants() {
        return get(CampaignOption.FIELD_KITCHEN_IGNORE_NON_COMBATANTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.FIELD_KITCHEN_IGNORE_NON_COMBATANTS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setFieldKitchenIgnoreNonCombatants(final boolean fieldKitchenIgnoreNonCombatants) {
        set(CampaignOption.FIELD_KITCHEN_IGNORE_NON_COMBATANTS, fieldKitchenIgnoreNonCombatants);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.FATIGUE_UNDEPLOYMENT_THRESHOLD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getFatigueUndeploymentThreshold() {
        return get(CampaignOption.FATIGUE_UNDEPLOYMENT_THRESHOLD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.FATIGUE_UNDEPLOYMENT_THRESHOLD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setFatigueUndeploymentThreshold(final Integer fatigueUndeploymentThreshold) {
        set(CampaignOption.FATIGUE_UNDEPLOYMENT_THRESHOLD, fatigueUndeploymentThreshold);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getFatigueLeaveThreshold() {
        return get(CampaignOption.FATIGUE_LEAVE_THRESHOLD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.FATIGUE_LEAVE_THRESHOLD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setFatigueLeaveThreshold(final Integer fatigueLeaveThreshold) {
        set(CampaignOption.FATIGUE_LEAVE_THRESHOLD, fatigueLeaveThreshold);
    }

    // endregion General Personnel

    // region Expanded Personnel Information

    /**
     * @return whether to use time in service
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_TIME_IN_SERVICE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseTimeInService() {
        return get(CampaignOption.USE_TIME_IN_SERVICE);
    }

    /**
     * @param useTimeInService the new value for whether to use time in service or not
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_TIME_IN_SERVICE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseTimeInService(final boolean useTimeInService) {
        set(CampaignOption.USE_TIME_IN_SERVICE, useTimeInService);
    }

    /**
     * @return the format to display the Time in Service in
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.TIME_IN_SERVICE_DISPLAY_FORMAT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public TimeInDisplayFormat getTimeInServiceDisplayFormat() {
        return get(CampaignOption.TIME_IN_SERVICE_DISPLAY_FORMAT);
    }

    /**
     * @param timeInServiceDisplayFormat the new display format for Time in Service
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.TIME_IN_SERVICE_DISPLAY_FORMAT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTimeInServiceDisplayFormat(final TimeInDisplayFormat timeInServiceDisplayFormat) {
        set(CampaignOption.TIME_IN_SERVICE_DISPLAY_FORMAT, timeInServiceDisplayFormat);
    }

    /**
     * @return whether to use time in rank
     *
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_TIME_IN_RANK)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseTimeInRank() {
        return get(CampaignOption.USE_TIME_IN_RANK);
    }

    /**
     * @param useTimeInRank the new value for whether to use time in rank
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_TIME_IN_RANK,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseTimeInRank(final boolean useTimeInRank) {
        set(CampaignOption.USE_TIME_IN_RANK, useTimeInRank);
    }

    /**
     * @return the format to display the Time in Rank in
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.TIME_IN_RANK_DISPLAY_FORMAT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public TimeInDisplayFormat getTimeInRankDisplayFormat() {
        return get(CampaignOption.TIME_IN_RANK_DISPLAY_FORMAT);
    }

    /**
     * @param timeInRankDisplayFormat the new display format for Time in Rank
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.TIME_IN_RANK_DISPLAY_FORMAT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTimeInRankDisplayFormat(final TimeInDisplayFormat timeInRankDisplayFormat) {
        set(CampaignOption.TIME_IN_RANK_DISPLAY_FORMAT, timeInRankDisplayFormat);
    }

    /**
     * @return whether to track the total earnings of personnel
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.TRACK_TOTAL_EARNINGS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isTrackTotalEarnings() {
        return get(CampaignOption.TRACK_TOTAL_EARNINGS);
    }

    /**
     * @param trackTotalEarnings the new value for whether to track total earnings for personnel
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.TRACK_TOTAL_EARNINGS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTrackTotalEarnings(final boolean trackTotalEarnings) {
        set(CampaignOption.TRACK_TOTAL_EARNINGS, trackTotalEarnings);
    }

    /**
     * @return whether to track the total experience earnings of personnel
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.TRACK_TOTAL_XP_EARNINGS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isTrackTotalXPEarnings() {
        return get(CampaignOption.TRACK_TOTAL_XP_EARNINGS);
    }

    /**
     * @param trackTotalXPEarnings the new value for whether to track total experience earnings for personnel
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.TRACK_TOTAL_XP_EARNINGS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTrackTotalXPEarnings(final boolean trackTotalXPEarnings) {
        set(CampaignOption.TRACK_TOTAL_XP_EARNINGS, trackTotalXPEarnings);
    }

    /**
     * Gets a value indicating whether to show a person's origin faction when displaying their details.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SHOW_ORIGIN_FACTION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isShowOriginFaction() {
        return get(CampaignOption.SHOW_ORIGIN_FACTION);
    }

    /**
     * Sets a value indicating whether to show a person's origin faction when displaying their details.
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.SHOW_ORIGIN_FACTION,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setShowOriginFaction(final boolean showOriginFaction) {
        set(CampaignOption.SHOW_ORIGIN_FACTION, showOriginFaction);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ADMINS_HAVE_NEGOTIATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAdminsHaveNegotiation() {
        return get(CampaignOption.ADMINS_HAVE_NEGOTIATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ADMINS_HAVE_NEGOTIATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAdminsHaveNegotiation(final boolean useAdminsHaveNegotiation) {
        set(CampaignOption.ADMINS_HAVE_NEGOTIATION, useAdminsHaveNegotiation);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ADMIN_EXPERIENCE_LEVEL_INCLUDE_NEGOTIATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAdminExperienceLevelIncludeNegotiation() {
        return get(CampaignOption.ADMIN_EXPERIENCE_LEVEL_INCLUDE_NEGOTIATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ADMIN_EXPERIENCE_LEVEL_INCLUDE_NEGOTIATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAdminExperienceLevelIncludeNegotiation(final boolean useAdminExperienceLevelIncludeNegotiation) {
        set(CampaignOption.ADMIN_EXPERIENCE_LEVEL_INCLUDE_NEGOTIATION,
              useAdminExperienceLevelIncludeNegotiation);
    }

    // endregion Expanded Personnel Information

    // region Medical

    /**
     * Checks if any form of advanced medical system is enabled.
     *
     * <p>This method returns {@code true} if either the standard advanced medical system or the alternative advanced
     * medical system is enabled.</p>
     *
     * @return {@code true} if either advanced medical system is in use, {@code false} otherwise
     *
     * @see #isUseAdvancedMedicalDirect()
     */
    public boolean isUseAdvancedMedical() {
        return get(CampaignOption.USE_ADVANCED_MEDICAL) || get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_ADVANCED_MEDICAL,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseAdvancedMedical(final boolean useAdvancedMedical) {
        set(CampaignOption.USE_ADVANCED_MEDICAL, useAdvancedMedical);
    }

    /**
     * Checks if the standard advanced medical system is enabled.
     *
     * <p>This method specifically checks only the standard advanced medical system, ignoring the alternative
     * advanced medical system setting.</p>
     *
     * @return {@code true} if the standard advanced medical system is enabled, {@code false} otherwise
     *
     * @see #isUseAdvancedMedical()
     *
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_ADVANCED_MEDICAL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseAdvancedMedicalDirect() {
        return get(CampaignOption.USE_ADVANCED_MEDICAL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.HEAL_WAITING_PERIOD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getHealingWaitingPeriod() {
        return get(CampaignOption.HEAL_WAITING_PERIOD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.HEAL_WAITING_PERIOD,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setHealingWaitingPeriod(final int healWaitingPeriod) {
        set(CampaignOption.HEAL_WAITING_PERIOD, healWaitingPeriod);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.NATURAL_HEALING_WAITING_PERIOD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getNaturalHealingWaitingPeriod() {
        return get(CampaignOption.NATURAL_HEALING_WAITING_PERIOD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.NATURAL_HEALING_WAITING_PERIOD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setNaturalHealingWaitingPeriod(final int naturalHealingWaitingPeriod) {
        set(CampaignOption.NATURAL_HEALING_WAITING_PERIOD, naturalHealingWaitingPeriod);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MINIMUM_HITS_FOR_VEHICLES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMinimumHitsForVehicles() {
        return get(CampaignOption.MINIMUM_HITS_FOR_VEHICLES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MINIMUM_HITS_FOR_VEHICLES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMinimumHitsForVehicles(final int minimumHitsForVehicles) {
        set(CampaignOption.MINIMUM_HITS_FOR_VEHICLES, minimumHitsForVehicles);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_HITS_FOR_VEHICLES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomHitsForVehicles() {
        return get(CampaignOption.USE_RANDOM_HITS_FOR_VEHICLES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_HITS_FOR_VEHICLES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomHitsForVehicles(final boolean useRandomHitsForVehicles) {
        set(CampaignOption.USE_RANDOM_HITS_FOR_VEHICLES, useRandomHitsForVehicles);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.TOUGHER_HEALING)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isTougherHealing() {
        return get(CampaignOption.TOUGHER_HEALING);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.TOUGHER_HEALING,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTougherHealing(final boolean tougherHealing) {
        set(CampaignOption.TOUGHER_HEALING, tougherHealing);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseAlternativeAdvancedMedical() {
        return get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseAlternativeAdvancedMedical(final boolean useAlternativeAdvancedMedical) {
        set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, useAlternativeAdvancedMedical);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL_FEWER_PERMANENT_INJURIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseAlternativeAdvancedMedicalFewerPermanentInjuries() {
        return get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL_FEWER_PERMANENT_INJURIES);
    }

    /**
     * @deprecated Use {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL_FEWER_PERMANENT_INJURIES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseAlternativeAdvancedMedicalFewerPermanentInjuries(
          final boolean useAlternativeAdvancedMedicalFewerPermanentInjuries) {
        set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL_FEWER_PERMANENT_INJURIES,
              useAlternativeAdvancedMedicalFewerPermanentInjuries);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ALTERNATIVE_ADVANCED_MEDICAL_HEALING_TIME_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getAlternativeAdvancedMedicalHealingTimeMultiplier() {
        return get(CampaignOption.ALTERNATIVE_ADVANCED_MEDICAL_HEALING_TIME_MULTIPLIER);
    }

    /**
     * @deprecated Use {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ALTERNATIVE_ADVANCED_MEDICAL_HEALING_TIME_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAlternativeAdvancedMedicalHealingTimeMultiplier(
          final double alternativeAdvancedMedicalHealingTimeMultiplier) {
        set(CampaignOption.ALTERNATIVE_ADVANCED_MEDICAL_HEALING_TIME_MULTIPLIER,
              alternativeAdvancedMedicalHealingTimeMultiplier);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.LIMIT_CLAN_TECH)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isLimitClanTech() {
        return get(CampaignOption.LIMIT_CLAN_TECH);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.LIMIT_CLAN_TECH, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setLimitClanTech(final boolean limitClanTech) {
        set(CampaignOption.LIMIT_CLAN_TECH, limitClanTech);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_TWIST_OF_FATE_SURVIVAL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseTwistOfFateSurvival() {
        return get(CampaignOption.USE_TWIST_OF_FATE_SURVIVAL);
    }

    /**
     * @deprecated Use {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_TWIST_OF_FATE_SURVIVAL, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseTwistOfFateSurvival(final boolean useTwistOfFateSurvival) {
        set(CampaignOption.USE_TWIST_OF_FATE_SURVIVAL, useTwistOfFateSurvival);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_REPLACE_EDGE_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseReplaceEdgeAwards() {
        return get(CampaignOption.USE_REPLACE_EDGE_AWARDS);
    }

    /**
     * @deprecated Use {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_REPLACE_EDGE_AWARDS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseReplaceEdgeAwards(final boolean useReplaceEdgeAwards) {
        set(CampaignOption.USE_REPLACE_EDGE_AWARDS, useReplaceEdgeAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_RANDOM_DISEASES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomDiseases() {
        return get(CampaignOption.USE_RANDOM_DISEASES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_RANDOM_DISEASES,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomDiseases(final boolean useRandomDiseases) {
        set(CampaignOption.USE_RANDOM_DISEASES, useRandomDiseases);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MAXIMUM_PATIENTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMaximumPatients() {
        return get(CampaignOption.MAXIMUM_PATIENTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MAXIMUM_PATIENTS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMaximumPatients(final int maximumPatients) {
        set(CampaignOption.MAXIMUM_PATIENTS, maximumPatients);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DOCTORS_USE_ADMINISTRATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDoctorsUseAdministration() {
        return get(CampaignOption.DOCTORS_USE_ADMINISTRATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DOCTORS_USE_ADMINISTRATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDoctorsUseAdministration(final boolean doctorsUseAdministration) {
        set(CampaignOption.DOCTORS_USE_ADMINISTRATION, doctorsUseAdministration);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_USEFUL_MEDICS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseUsefulMedics() {
        return get(CampaignOption.USE_USEFUL_MEDICS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_USEFUL_MEDICS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setIsUseUsefulMedics(final boolean useUsefulMedics) {
        set(CampaignOption.USE_USEFUL_MEDICS, useUsefulMedics);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_BLOB_INFANTRY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseBlobInfantry() {
        return get(CampaignOption.USE_BLOB_INFANTRY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_BLOB_INFANTRY,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseBlobInfantry(final boolean useBlobInfantry) {
        set(CampaignOption.USE_BLOB_INFANTRY, useBlobInfantry);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_BLOB_BATTLE_ARMOR)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseBlobBattleArmor() {
        return get(CampaignOption.USE_BLOB_BATTLE_ARMOR);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_BLOB_BATTLE_ARMOR, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseBlobBattleArmor(final boolean useBlobBattleArmor) {
        set(CampaignOption.USE_BLOB_BATTLE_ARMOR, useBlobBattleArmor);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseBlobVehicleCrewGround() {
        return get(CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseBlobVehicleCrewGround(final boolean useBlobVehicleCrewGround) {
        set(CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND, useBlobVehicleCrewGround);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_BLOB_VEHICLE_CREW_VTOL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseBlobVehicleCrewVTOL() {
        return get(CampaignOption.USE_BLOB_VEHICLE_CREW_VTOL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_BLOB_VEHICLE_CREW_VTOL, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseBlobVehicleCrewVTOL(final boolean useBlobVehicleCrewVTOL) {
        set(CampaignOption.USE_BLOB_VEHICLE_CREW_VTOL, useBlobVehicleCrewVTOL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_BLOB_VEHICLE_CREW_NAVAL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseBlobVehicleCrewNaval() {
        return get(CampaignOption.USE_BLOB_VEHICLE_CREW_NAVAL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_BLOB_VEHICLE_CREW_NAVAL, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseBlobVehicleCrewNaval(final boolean useBlobVehicleCrewNaval) {
        set(CampaignOption.USE_BLOB_VEHICLE_CREW_NAVAL, useBlobVehicleCrewNaval);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_BLOB_VESSEL_PILOT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseBlobVesselPilot() {
        return get(CampaignOption.USE_BLOB_VESSEL_PILOT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_BLOB_VESSEL_PILOT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseBlobVesselPilot(final boolean useBlobVesselPilot) {
        set(CampaignOption.USE_BLOB_VESSEL_PILOT, useBlobVesselPilot);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_BLOB_VESSEL_GUNNER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseBlobVesselGunner() {
        return get(CampaignOption.USE_BLOB_VESSEL_GUNNER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_BLOB_VESSEL_GUNNER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseBlobVesselGunner(final boolean useBlobVesselGunner) {
        set(CampaignOption.USE_BLOB_VESSEL_GUNNER, useBlobVesselGunner);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_BLOB_VESSEL_CREW)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseBlobVesselCrew() {
        return get(CampaignOption.USE_BLOB_VESSEL_CREW);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_BLOB_VESSEL_CREW,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseBlobVesselCrew(final boolean useBlobVesselCrew) {
        set(CampaignOption.USE_BLOB_VESSEL_CREW, useBlobVesselCrew);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_MASH_THEATRES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseMASHTheatres() {
        return get(CampaignOption.USE_MASH_THEATRES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_MASH_THEATRES,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setIsUseMASHTheatres(final boolean useMASHTheatres) {
        set(CampaignOption.USE_MASH_THEATRES, useMASHTheatres);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MASH_THEATRE_CAPACITY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMASHTheatreCapacity() {
        return get(CampaignOption.MASH_THEATRE_CAPACITY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MASH_THEATRE_CAPACITY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMASHTheatreCapacity(final int mashTheatreCapacity) {
        set(CampaignOption.MASH_THEATRE_CAPACITY, mashTheatreCapacity);
    }

    // endregion Medical

    // region Prisoners

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PRISONER_CAPTURE_STYLE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public PrisonerCaptureStyle getPrisonerCaptureStyle() {
        return get(CampaignOption.PRISONER_CAPTURE_STYLE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PRISONER_CAPTURE_STYLE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPrisonerCaptureStyle(final PrisonerCaptureStyle prisonerCaptureStyle) {
        set(CampaignOption.PRISONER_CAPTURE_STYLE, prisonerCaptureStyle);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_FUNCTIONAL_ESCAPE_ARTIST)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFunctionalEscapeArtist() {
        return get(CampaignOption.USE_FUNCTIONAL_ESCAPE_ARTIST);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FUNCTIONAL_ESCAPE_ARTIST, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFunctionalEscapeArtist(final boolean useFunctionalEscapeArtist) {
        set(CampaignOption.USE_FUNCTIONAL_ESCAPE_ARTIST, useFunctionalEscapeArtist);
    }
    // endregion Prisoners

    // region Personnel Randomization

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_DYLANS_RANDOM_XP)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseDylansRandomXP() {
        return get(CampaignOption.USE_DYLANS_RANDOM_XP);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_DYLANS_RANDOM_XP,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseDylansRandomXP(final boolean useDylansRandomXP) {
        set(CampaignOption.USE_DYLANS_RANDOM_XP, useDylansRandomXP);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.NON_BINARY_DICE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getNonBinaryDiceSize() {
        return get(CampaignOption.NON_BINARY_DICE_SIZE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.NON_BINARY_DICE_SIZE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setNonBinaryDiceSize(final int nonBinaryDiceSize) {
        set(CampaignOption.NON_BINARY_DICE_SIZE, nonBinaryDiceSize);
    }
    // endregion Personnel Randomization

    // region Random Histories

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.RANDOM_ORIGIN_OPTIONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public RandomOriginOptions getRandomOriginOptions() {
        return get(CampaignOption.RANDOM_ORIGIN_OPTIONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RANDOM_ORIGIN_OPTIONS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRandomOriginOptions(final RandomOriginOptions randomOriginOptions) {
        set(CampaignOption.RANDOM_ORIGIN_OPTIONS, randomOriginOptions);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_RANDOM_PERSONALITIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomPersonalities() {
        return get(CampaignOption.USE_RANDOM_PERSONALITIES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_PERSONALITIES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomPersonalities(final boolean useRandomPersonalities) {
        set(CampaignOption.USE_RANDOM_PERSONALITIES, useRandomPersonalities);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_PERSONALITY_LABELS_ONLY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUsePersonalityLabelsOnly() {
        return get(CampaignOption.USE_PERSONALITY_LABELS_ONLY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_PERSONALITY_LABELS_ONLY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUsePersonalityLabelsOnly(final boolean usePersonalityLabelsOnly) {
        set(CampaignOption.USE_PERSONALITY_LABELS_ONLY, usePersonalityLabelsOnly);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_PERSONALITY_REPUTATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomPersonalityReputation() {
        return get(CampaignOption.USE_RANDOM_PERSONALITY_REPUTATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_PERSONALITY_REPUTATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomPersonalityReputation(final boolean useRandomPersonalityReputation) {
        set(CampaignOption.USE_RANDOM_PERSONALITY_REPUTATION, useRandomPersonalityReputation);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_REASONING_XP_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseReasoningXpMultiplier() {
        return get(CampaignOption.USE_REASONING_XP_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_REASONING_XP_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseReasoningXpMultiplier(final boolean useReasoningXpMultiplier) {
        set(CampaignOption.USE_REASONING_XP_MULTIPLIER, useReasoningXpMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_SIMULATED_RELATIONSHIPS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseSimulatedRelationships() {
        return get(CampaignOption.USE_SIMULATED_RELATIONSHIPS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_SIMULATED_RELATIONSHIPS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseSimulatedRelationships(final boolean useSimulatedRelationships) {
        set(CampaignOption.USE_SIMULATED_RELATIONSHIPS, useSimulatedRelationships);
    }
    // endregion Random Histories

    // region Retirement

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_RANDOM_RETIREMENT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomRetirement() {
        return get(CampaignOption.USE_RANDOM_RETIREMENT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_RETIREMENT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomRetirement(final boolean useRandomRetirement) {
        set(CampaignOption.USE_RANDOM_RETIREMENT, useRandomRetirement);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.TURNOVER_FREQUENCY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public TurnoverFrequency getTurnoverFrequency() {
        return get(CampaignOption.TURNOVER_FREQUENCY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.TURNOVER_FREQUENCY,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTurnoverFrequency(final TurnoverFrequency turnoverFrequency) {
        set(CampaignOption.TURNOVER_FREQUENCY, turnoverFrequency);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_CONTRACT_COMPLETION_RANDOM_RETIREMENT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseContractCompletionRandomRetirement() {
        return get(CampaignOption.USE_CONTRACT_COMPLETION_RANDOM_RETIREMENT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_CONTRACT_COMPLETION_RANDOM_RETIREMENT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseContractCompletionRandomRetirement(final boolean useContractCompletionRandomRetirement) {
        set(CampaignOption.USE_CONTRACT_COMPLETION_RANDOM_RETIREMENT, useContractCompletionRandomRetirement);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_CUSTOM_RETIREMENT_MODIFIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseCustomRetirementModifiers() {
        return get(CampaignOption.USE_CUSTOM_RETIREMENT_MODIFIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_CUSTOM_RETIREMENT_MODIFIERS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseCustomRetirementModifiers(final boolean useCustomRetirementModifiers) {
        set(CampaignOption.USE_CUSTOM_RETIREMENT_MODIFIERS, useCustomRetirementModifiers);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_FATIGUE_MODIFIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFatigueModifiers() {
        return get(CampaignOption.USE_FATIGUE_MODIFIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FATIGUE_MODIFIERS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFatigueModifiers(final boolean useFatigueModifiers) {
        set(CampaignOption.USE_FATIGUE_MODIFIERS, useFatigueModifiers);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_LOYALTY_MODIFIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseLoyaltyModifiers() {
        return get(CampaignOption.USE_LOYALTY_MODIFIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_LOYALTY_MODIFIERS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseLoyaltyModifiers(final boolean useLoyaltyModifiers) {
        set(CampaignOption.USE_LOYALTY_MODIFIERS, useLoyaltyModifiers);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_HIDE_LOYALTY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseHideLoyalty() {
        return get(CampaignOption.USE_HIDE_LOYALTY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_HIDE_LOYALTY,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseHideLoyalty(final boolean useHideLoyalty) {
        set(CampaignOption.USE_HIDE_LOYALTY, useHideLoyalty);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_FOUNDER_TURNOVER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomFounderTurnover() {
        return get(CampaignOption.USE_RANDOM_FOUNDER_TURNOVER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_FOUNDER_TURNOVER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomFounderTurnover(final boolean useRandomFounderTurnover) {
        set(CampaignOption.USE_RANDOM_FOUNDER_TURNOVER, useRandomFounderTurnover);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_FOUNDER_RETIREMENT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFounderRetirement() {
        return get(CampaignOption.USE_FOUNDER_RETIREMENT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FOUNDER_RETIREMENT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFounderRetirement(final boolean useFounderRetirement) {
        set(CampaignOption.USE_FOUNDER_RETIREMENT, useFounderRetirement);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_SUB_CONTRACT_SOLDIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseSubContractSoldiers() {
        return get(CampaignOption.USE_SUB_CONTRACT_SOLDIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_SUB_CONTRACT_SOLDIERS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseSubContractSoldiers(final boolean useSubContractSoldiers) {
        set(CampaignOption.USE_SUB_CONTRACT_SOLDIERS, useSubContractSoldiers);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.TURNOVER_FIXED_TARGET_NUMBER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getTurnoverFixedTargetNumber() {
        return get(CampaignOption.TURNOVER_FIXED_TARGET_NUMBER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.TURNOVER_FIXED_TARGET_NUMBER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTurnoverFixedTargetNumber(final Integer turnoverFixedTargetNumber) {
        set(CampaignOption.TURNOVER_FIXED_TARGET_NUMBER, turnoverFixedTargetNumber);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAYOUT_RATE_OFFICER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getPayoutRateOfficer() {
        return get(CampaignOption.PAYOUT_RATE_OFFICER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAYOUT_RATE_OFFICER,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayoutRateOfficer(final Integer payoutRateOfficer) {
        set(CampaignOption.PAYOUT_RATE_OFFICER, payoutRateOfficer);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAYOUT_RATE_ENLISTED)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getPayoutRateEnlisted() {
        return get(CampaignOption.PAYOUT_RATE_ENLISTED);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAYOUT_RATE_ENLISTED,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayoutRateEnlisted(final Integer payoutRateEnlisted) {
        set(CampaignOption.PAYOUT_RATE_ENLISTED, payoutRateEnlisted);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.PAYOUT_RETIREMENT_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getPayoutRetirementMultiplier() {
        return get(CampaignOption.PAYOUT_RETIREMENT_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PAYOUT_RETIREMENT_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayoutRetirementMultiplier(final Integer payoutRetirementMultiplier) {
        set(CampaignOption.PAYOUT_RETIREMENT_MULTIPLIER, payoutRetirementMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_PAYOUT_SERVICE_BONUS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUsePayoutServiceBonus() {
        return get(CampaignOption.USE_PAYOUT_SERVICE_BONUS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_PAYOUT_SERVICE_BONUS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUsePayoutServiceBonus(final boolean usePayoutServiceBonus) {
        set(CampaignOption.USE_PAYOUT_SERVICE_BONUS, usePayoutServiceBonus);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAYOUT_SERVICE_BONUS_RATE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getPayoutServiceBonusRate() {
        return get(CampaignOption.PAYOUT_SERVICE_BONUS_RATE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PAYOUT_SERVICE_BONUS_RATE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayoutServiceBonusRate(final Integer payoutServiceBonusRate) {
        set(CampaignOption.PAYOUT_SERVICE_BONUS_RATE, payoutServiceBonusRate);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_SKILL_MODIFIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseSkillModifiers() {
        return get(CampaignOption.USE_SKILL_MODIFIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_SKILL_MODIFIERS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseSkillModifiers(final boolean useSkillModifiers) {
        set(CampaignOption.USE_SKILL_MODIFIERS, useSkillModifiers);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_AGE_MODIFIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseAgeModifiers() {
        return get(CampaignOption.USE_AGE_MODIFIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_AGE_MODIFIERS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseAgeModifiers(final boolean useAgeModifiers) {
        set(CampaignOption.USE_AGE_MODIFIERS, useAgeModifiers);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_UNIT_RATING_MODIFIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseUnitRatingModifiers() {
        return get(CampaignOption.USE_UNIT_RATING_MODIFIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_UNIT_RATING_MODIFIERS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseUnitRatingModifiers(final boolean useUnitRatingModifiers) {
        set(CampaignOption.USE_UNIT_RATING_MODIFIERS, useUnitRatingModifiers);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_FACTION_MODIFIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFactionModifiers() {
        return get(CampaignOption.USE_FACTION_MODIFIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FACTION_MODIFIERS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFactionModifiers(final boolean useFactionModifiers) {
        set(CampaignOption.USE_FACTION_MODIFIERS, useFactionModifiers);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_MISSION_STATUS_MODIFIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseMissionStatusModifiers() {
        return get(CampaignOption.USE_MISSION_STATUS_MODIFIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_MISSION_STATUS_MODIFIERS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseMissionStatusModifiers(final boolean useMissionStatusModifiers) {
        set(CampaignOption.USE_MISSION_STATUS_MODIFIERS, useMissionStatusModifiers);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_HOSTILE_TERRITORY_MODIFIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseHostileTerritoryModifiers() {
        return get(CampaignOption.USE_HOSTILE_TERRITORY_MODIFIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_HOSTILE_TERRITORY_MODIFIERS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseHostileTerritoryModifiers(final boolean useHostileTerritoryModifiers) {
        set(CampaignOption.USE_HOSTILE_TERRITORY_MODIFIERS, useHostileTerritoryModifiers);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_FAMILY_MODIFIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFamilyModifiers() {
        return get(CampaignOption.USE_FAMILY_MODIFIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_FAMILY_MODIFIERS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFamilyModifiers(final boolean useFamilyModifiers) {
        set(CampaignOption.USE_FAMILY_MODIFIERS, useFamilyModifiers);
    }

    /**
     * Use {@link #isUseHRStrain()} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public boolean isUseAdministrativeStrain() {
        return get(CampaignOption.USE_HR_STRAIN);
    }

    /**
     * Use {@link #setUseHRStrain(boolean)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void setUseAdministrativeStrain(final boolean UseHRStrain) {
        set(CampaignOption.USE_HR_STRAIN, UseHRStrain);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_HR_STRAIN)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseHRStrain() {
        return get(CampaignOption.USE_HR_STRAIN);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_HR_STRAIN,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseHRStrain(final boolean UseHRStrain) {
        set(CampaignOption.USE_HR_STRAIN, UseHRStrain);
    }

    /**
     * Use {@link #getHRCapacity()} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public Integer getAdministrativeCapacity() {
        return get(CampaignOption.HR_CAPACITY);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.HR_CAPACITY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getHRCapacity() {
        return get(CampaignOption.HR_CAPACITY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.HR_CAPACITY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setHRCapacity(final Integer hrCapacity) {
        set(CampaignOption.HR_CAPACITY, hrCapacity);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_MANAGEMENT_SKILL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseManagementSkill() {
        return get(CampaignOption.USE_MANAGEMENT_SKILL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_MANAGEMENT_SKILL,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseManagementSkill(final boolean useManagementSkill) {
        set(CampaignOption.USE_MANAGEMENT_SKILL, useManagementSkill);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_COMMANDER_LEADERSHIP_ONLY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseCommanderLeadershipOnly() {
        return get(CampaignOption.USE_COMMANDER_LEADERSHIP_ONLY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_COMMANDER_LEADERSHIP_ONLY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseCommanderLeadershipOnly(final boolean useCommanderLeadershipOnly) {
        set(CampaignOption.USE_COMMANDER_LEADERSHIP_ONLY, useCommanderLeadershipOnly);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MANAGEMENT_SKILL_PENALTY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getManagementSkillPenalty() {
        return get(CampaignOption.MANAGEMENT_SKILL_PENALTY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MANAGEMENT_SKILL_PENALTY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setManagementSkillPenalty(final Integer managementSkillPenalty) {
        set(CampaignOption.MANAGEMENT_SKILL_PENALTY, managementSkillPenalty);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SERVICE_CONTRACT_DURATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getServiceContractDuration() {
        return get(CampaignOption.SERVICE_CONTRACT_DURATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.SERVICE_CONTRACT_DURATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setServiceContractDuration(final Integer serviceContractDuration) {
        set(CampaignOption.SERVICE_CONTRACT_DURATION, serviceContractDuration);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SERVICE_CONTRACT_MODIFIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getServiceContractModifier() {
        return get(CampaignOption.SERVICE_CONTRACT_MODIFIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.SERVICE_CONTRACT_MODIFIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setServiceContractModifier(final Integer serviceContractModifier) {
        set(CampaignOption.SERVICE_CONTRACT_MODIFIER, serviceContractModifier);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAY_BONUS_DEFAULT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPayBonusDefault() {
        return get(CampaignOption.PAY_BONUS_DEFAULT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAY_BONUS_DEFAULT,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayBonusDefault(final boolean payBonusDefault) {
        set(CampaignOption.PAY_BONUS_DEFAULT, payBonusDefault);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.PAY_BONUS_DEFAULT_THRESHOLD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getPayBonusDefaultThreshold() {
        return get(CampaignOption.PAY_BONUS_DEFAULT_THRESHOLD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PAY_BONUS_DEFAULT_THRESHOLD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayBonusDefaultThreshold(final int payBonusDefaultThreshold) {
        set(CampaignOption.PAY_BONUS_DEFAULT_THRESHOLD, payBonusDefaultThreshold);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.INCLUDE_CIVILIANS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isIncludeCivilians() {
        return get(CampaignOption.INCLUDE_CIVILIANS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.INCLUDE_CIVILIANS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setIncludeCivilians(final boolean includeCivilians) {
        set(CampaignOption.INCLUDE_CIVILIANS, includeCivilians);
    }
    // endregion Retirement

    // region Family

    /**
     * @return the level of familial relation to display
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.FAMILY_DISPLAY_LEVEL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public FamilialRelationshipDisplayLevel getFamilyDisplayLevel() {
        return get(CampaignOption.FAMILY_DISPLAY_LEVEL);
    }

    /**
     * @param familyDisplayLevel the level of familial relation to display
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.FAMILY_DISPLAY_LEVEL,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setFamilyDisplayLevel(final FamilialRelationshipDisplayLevel familyDisplayLevel) {
        set(CampaignOption.FAMILY_DISPLAY_LEVEL, familyDisplayLevel);
    }
    // endregion Family

    // region anniversaries

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ANNOUNCE_BIRTHDAYS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAnnounceBirthdays() {
        return get(CampaignOption.ANNOUNCE_BIRTHDAYS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ANNOUNCE_BIRTHDAYS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAnnounceBirthdays(final boolean announceBirthdays) {
        set(CampaignOption.ANNOUNCE_BIRTHDAYS, announceBirthdays);
    }

    /**
     * Checks if recruitment anniversaries should be announced.
     *
     * @return {@code true} if recruitment anniversaries should be announced, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ANNOUNCE_RECRUITMENT_ANNIVERSARIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAnnounceRecruitmentAnniversaries() {
        return get(CampaignOption.ANNOUNCE_RECRUITMENT_ANNIVERSARIES);
    }

    /**
     * Set whether to announce recruitment anniversaries.
     *
     * @param announceRecruitmentAnniversaries {@code true} to announce recruitment anniversaries, {@code false}
     *                                         otherwise
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ANNOUNCE_RECRUITMENT_ANNIVERSARIES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAnnounceRecruitmentAnniversaries(final boolean announceRecruitmentAnniversaries) {
        set(CampaignOption.ANNOUNCE_RECRUITMENT_ANNIVERSARIES, announceRecruitmentAnniversaries);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ANNOUNCE_OFFICERS_ONLY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAnnounceOfficersOnly() {
        return get(CampaignOption.ANNOUNCE_OFFICERS_ONLY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ANNOUNCE_OFFICERS_ONLY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAnnounceOfficersOnly(final boolean announceOfficersOnly) {
        set(CampaignOption.ANNOUNCE_OFFICERS_ONLY, announceOfficersOnly);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ANNOUNCE_CHILD_BIRTHDAYS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAnnounceChildBirthdays() {
        return get(CampaignOption.ANNOUNCE_CHILD_BIRTHDAYS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ANNOUNCE_CHILD_BIRTHDAYS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAnnounceChildBirthdays(final boolean announceChildBirthdays) {
        set(CampaignOption.ANNOUNCE_CHILD_BIRTHDAYS, announceChildBirthdays);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ANNOUNCE_RETIREE_DEATH)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAnnounceRetireeDeath() {
        return get(CampaignOption.ANNOUNCE_RETIREE_DEATH);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ANNOUNCE_RETIREE_DEATH, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAnnounceRetireeDeath(final boolean announceRetireeDeath) {
        set(CampaignOption.ANNOUNCE_RETIREE_DEATH, announceRetireeDeath);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ANNOUNCE_RETIREE_DEATH_EXPANDED)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAnnounceRetireeDeathExpanded() {
        return get(CampaignOption.ANNOUNCE_RETIREE_DEATH_EXPANDED);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ANNOUNCE_RETIREE_DEATH_EXPANDED, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAnnounceRetireeDeathExpanded(final boolean announceRetireeDeathExpanded) {
        set(CampaignOption.ANNOUNCE_RETIREE_DEATH_EXPANDED, announceRetireeDeathExpanded);
    }
    // endregion anniversaries

    //startregion Life Events

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.SHOW_LIFE_EVENT_DIALOG_BIRTHS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isShowLifeEventDialogBirths() {
        return get(CampaignOption.SHOW_LIFE_EVENT_DIALOG_BIRTHS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.SHOW_LIFE_EVENT_DIALOG_BIRTHS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setShowLifeEventDialogBirths(final boolean showLifeEventDialogBirths) {
        set(CampaignOption.SHOW_LIFE_EVENT_DIALOG_BIRTHS, showLifeEventDialogBirths);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.SHOW_LIFE_EVENT_DIALOG_COMING_OF_AGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isShowLifeEventDialogComingOfAge() {
        return get(CampaignOption.SHOW_LIFE_EVENT_DIALOG_COMING_OF_AGE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.SHOW_LIFE_EVENT_DIALOG_COMING_OF_AGE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setShowLifeEventDialogComingOfAge(final boolean showLifeEventDialogComingOfAge) {
        set(CampaignOption.SHOW_LIFE_EVENT_DIALOG_COMING_OF_AGE, showLifeEventDialogComingOfAge);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.SHOW_LIFE_EVENT_DIALOG_CELEBRATIONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isShowLifeEventDialogCelebrations() {
        return get(CampaignOption.SHOW_LIFE_EVENT_DIALOG_CELEBRATIONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.SHOW_LIFE_EVENT_DIALOG_CELEBRATIONS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setShowLifeEventDialogCelebrations(final boolean showLifeEventDialogCelebrations) {
        set(CampaignOption.SHOW_LIFE_EVENT_DIALOG_CELEBRATIONS, showLifeEventDialogCelebrations);
    }
    //endregion Life Events

    // region Dependents

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_DEPENDENT_ADDITION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomDependentAddition() {
        return get(CampaignOption.USE_RANDOM_DEPENDENT_ADDITION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_DEPENDENT_ADDITION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomDependentAddition(final boolean useRandomDependentAddition) {
        set(CampaignOption.USE_RANDOM_DEPENDENT_ADDITION, useRandomDependentAddition);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_DEPENDENT_REMOVAL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomDependentRemoval() {
        return get(CampaignOption.USE_RANDOM_DEPENDENT_REMOVAL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_DEPENDENT_REMOVAL, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomDependentRemoval(final boolean useRandomDependentRemoval) {
        set(CampaignOption.USE_RANDOM_DEPENDENT_REMOVAL, useRandomDependentRemoval);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.DEPENDENT_PROFESSION_DIE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getDependentProfessionDieSize() {
        return get(CampaignOption.DEPENDENT_PROFESSION_DIE_SIZE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DEPENDENT_PROFESSION_DIE_SIZE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDependentProfessionDieSize(final int dependentProfessionDieSize) {
        set(CampaignOption.DEPENDENT_PROFESSION_DIE_SIZE, dependentProfessionDieSize);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.CIVILIAN_PROFESSION_DIE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getCivilianProfessionDieSize() {
        return get(CampaignOption.CIVILIAN_PROFESSION_DIE_SIZE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CIVILIAN_PROFESSION_DIE_SIZE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setCivilianProfessionDieSize(final int civilianProfessionDieSize) {
        set(CampaignOption.CIVILIAN_PROFESSION_DIE_SIZE, civilianProfessionDieSize);
    }
    // endregion Dependent

    // region Personnel Removal

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_PERSONNEL_REMOVAL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUsePersonnelRemoval() {
        return get(CampaignOption.USE_PERSONNEL_REMOVAL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_PERSONNEL_REMOVAL, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUsePersonnelRemoval(final boolean usePersonnelRemoval) {
        set(CampaignOption.USE_PERSONNEL_REMOVAL, usePersonnelRemoval);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_REMOVAL_EXEMPT_CEMETERY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRemovalExemptCemetery() {
        return get(CampaignOption.USE_REMOVAL_EXEMPT_CEMETERY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_REMOVAL_EXEMPT_CEMETERY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRemovalExemptCemetery(final boolean useRemovalExemptCemetery) {
        set(CampaignOption.USE_REMOVAL_EXEMPT_CEMETERY, useRemovalExemptCemetery);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_REMOVAL_EXEMPT_RETIREES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRemovalExemptRetirees() {
        return get(CampaignOption.USE_REMOVAL_EXEMPT_RETIREES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_REMOVAL_EXEMPT_RETIREES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRemovalExemptRetirees(final boolean useRemovalExemptRetirees) {
        set(CampaignOption.USE_REMOVAL_EXEMPT_RETIREES, useRemovalExemptRetirees);
    }
    // endregion Personnel Removal

    // region Salary

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.DISABLE_SECONDARY_ROLE_SALARY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDisableSecondaryRoleSalary() {
        return get(CampaignOption.DISABLE_SECONDARY_ROLE_SALARY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DISABLE_SECONDARY_ROLE_SALARY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisableSecondaryRoleSalary(final boolean disableSecondaryRoleSalary) {
        set(CampaignOption.DISABLE_SECONDARY_ROLE_SALARY, disableSecondaryRoleSalary);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SALARY_ANTI_MEK_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getSalaryAntiMekMultiplier() {
        return get(CampaignOption.SALARY_ANTI_MEK_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.SALARY_ANTI_MEK_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setSalaryAntiMekMultiplier(final double salaryAntiMekMultiplier) {
        set(CampaignOption.SALARY_ANTI_MEK_MULTIPLIER, salaryAntiMekMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.SALARY_SPECIALIST_INFANTRY_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getSalarySpecialistInfantryMultiplier() {
        return get(CampaignOption.SALARY_SPECIALIST_INFANTRY_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.SALARY_SPECIALIST_INFANTRY_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setSalarySpecialistInfantryMultiplier(final double salarySpecialistInfantryMultiplier) {
        set(CampaignOption.SALARY_SPECIALIST_INFANTRY_MULTIPLIER, salarySpecialistInfantryMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SALARY_XP_MULTIPLIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Map<SkillLevel, Double> getSalaryXPMultipliers() {
        return get(CampaignOption.SALARY_XP_MULTIPLIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.SALARY_XP_MULTIPLIERS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setSalaryXPMultipliers(final Map<SkillLevel, Double> salaryXPMultipliers) {
        set(CampaignOption.SALARY_XP_MULTIPLIERS, salaryXPMultipliers);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ROLE_BASE_SALARIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Money[] getRoleBaseSalaries() {
        return get(CampaignOption.ROLE_BASE_SALARIES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ROLE_BASE_SALARIES,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRoleBaseSalaries(final Money... roleBaseSalaries) {
        set(CampaignOption.ROLE_BASE_SALARIES, roleBaseSalaries);
    }

    public void setRoleBaseSalary(final PersonnelRole role, final double baseSalary) {
        setRoleBaseSalary(role, Money.of(baseSalary));
    }

    public void setRoleBaseSalary(final PersonnelRole role, final Money baseSalary) {
        getRoleBaseSalaries()[role.ordinal()] = baseSalary;
    }
    // endregion Salary

    // region Marriage

    /**
     * @return whether to use manual marriages
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_MANUAL_MARRIAGES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseManualMarriages() {
        return get(CampaignOption.USE_MANUAL_MARRIAGES);
    }

    /**
     * @param useManualMarriages whether to use manual marriages
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_MANUAL_MARRIAGES,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseManualMarriages(final boolean useManualMarriages) {
        set(CampaignOption.USE_MANUAL_MARRIAGES, useManualMarriages);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_CLAN_PERSONNEL_MARRIAGES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseClanPersonnelMarriages() {
        return get(CampaignOption.USE_CLAN_PERSONNEL_MARRIAGES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_CLAN_PERSONNEL_MARRIAGES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseClanPersonnelMarriages(final boolean useClanPersonnelMarriages) {
        set(CampaignOption.USE_CLAN_PERSONNEL_MARRIAGES, useClanPersonnelMarriages);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_PRISONER_MARRIAGES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUsePrisonerMarriages() {
        return get(CampaignOption.USE_PRISONER_MARRIAGES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_PRISONER_MARRIAGES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUsePrisonerMarriages(final boolean usePrisonerMarriages) {
        set(CampaignOption.USE_PRISONER_MARRIAGES, usePrisonerMarriages);
    }

    /**
     * This gets the number of recursions to use when checking mutual ancestors between two personnel
     *
     * @return the number of recursions to use
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.CHECK_MUTUAL_ANCESTORS_DEPTH)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getCheckMutualAncestorsDepth() {
        return get(CampaignOption.CHECK_MUTUAL_ANCESTORS_DEPTH);
    }

    /**
     * This sets the number of recursions to use when checking mutual ancestors between two personnel
     *
     * @param checkMutualAncestorsDepth the number of recursions
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CHECK_MUTUAL_ANCESTORS_DEPTH, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setCheckMutualAncestorsDepth(final int checkMutualAncestorsDepth) {
        set(CampaignOption.CHECK_MUTUAL_ANCESTORS_DEPTH, checkMutualAncestorsDepth);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.NO_INTEREST_IN_RELATIONSHIPS_DICE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getNoInterestInRelationshipsDiceSize() {
        return get(CampaignOption.NO_INTEREST_IN_RELATIONSHIPS_DICE_SIZE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.NO_INTEREST_IN_RELATIONSHIPS_DICE_SIZE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setNoInterestInRelationshipsDiceSize(final int noInterestInRelationshipsDiceSize) {
        set(CampaignOption.NO_INTEREST_IN_RELATIONSHIPS_DICE_SIZE, noInterestInRelationshipsDiceSize);
    }

    /**
     * @return whether to log a name change in a marriage
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.LOG_MARRIAGE_NAME_CHANGES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isLogMarriageNameChanges() {
        return get(CampaignOption.LOG_MARRIAGE_NAME_CHANGES);
    }

    /**
     * @param logMarriageNameChanges whether to log marriage name changes or not
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.LOG_MARRIAGE_NAME_CHANGES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setLogMarriageNameChanges(final boolean logMarriageNameChanges) {
        set(CampaignOption.LOG_MARRIAGE_NAME_CHANGES, logMarriageNameChanges);
    }

    /**
     * @return the weight map of potential surname changes for weighted marriage surname generation
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Map<MergingSurnameStyle, Integer> getMarriageSurnameWeights() {
        return get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS);
    }

    /**
     * @param marriageSurnameWeights the new marriage surname weight map
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MARRIAGE_SURNAME_WEIGHTS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMarriageSurnameWeights(final Map<MergingSurnameStyle, Integer> marriageSurnameWeights) {
        set(CampaignOption.MARRIAGE_SURNAME_WEIGHTS, marriageSurnameWeights);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.RANDOM_MARRIAGE_METHOD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public RandomMarriageMethod getRandomMarriageMethod() {
        return get(CampaignOption.RANDOM_MARRIAGE_METHOD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RANDOM_MARRIAGE_METHOD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRandomMarriageMethod(final RandomMarriageMethod randomMarriageMethod) {
        set(CampaignOption.RANDOM_MARRIAGE_METHOD, randomMarriageMethod);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_MARRIAGES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomClanPersonnelMarriages() {
        return get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_MARRIAGES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_MARRIAGES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomClanPersonnelMarriages(final boolean useRandomClanPersonnelMarriages) {
        set(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_MARRIAGES, useRandomClanPersonnelMarriages);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_PRISONER_MARRIAGES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomPrisonerMarriages() {
        return get(CampaignOption.USE_RANDOM_PRISONER_MARRIAGES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_PRISONER_MARRIAGES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomPrisonerMarriages(final boolean useRandomPrisonerMarriages) {
        set(CampaignOption.USE_RANDOM_PRISONER_MARRIAGES, useRandomPrisonerMarriages);
    }

    /**
     * A random marriage can only happen between two people whose ages differ (+/-) by the returned value
     *
     * @return the age range ages can differ (+/-)
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.RANDOM_MARRIAGE_AGE_RANGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getRandomMarriageAgeRange() {
        return get(CampaignOption.RANDOM_MARRIAGE_AGE_RANGE);
    }

    /**
     * A random marriage can only happen between two people whose ages differ (+/-) by this value
     *
     * @param randomMarriageAgeRange the new maximum age range
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RANDOM_MARRIAGE_AGE_RANGE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRandomMarriageAgeRange(final int randomMarriageAgeRange) {
        set(CampaignOption.RANDOM_MARRIAGE_AGE_RANGE, randomMarriageAgeRange);
    }

    /**
     * @return the number of sides on the die used to determine random marriage
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.RANDOM_MARRIAGE_DICE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getRandomMarriageDiceSize() {
        return get(CampaignOption.RANDOM_MARRIAGE_DICE_SIZE);
    }

    /**
     * Sets the size of the random marriage die.
     *
     * @param randomMarriageDiceSize the size of the random marriage die
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RANDOM_MARRIAGE_DICE_SIZE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRandomMarriageDiceSize(final int randomMarriageDiceSize) {
        set(CampaignOption.RANDOM_MARRIAGE_DICE_SIZE, randomMarriageDiceSize);
    }

    /**
     * @return the number of sides on the die used to determine random same-sex marriage
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.INTERESTED_IN_SAME_SEX_DICE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getInterestedInSameSexDiceSize() {
        return get(CampaignOption.INTERESTED_IN_SAME_SEX_DICE_SIZE);
    }

    /**
     * Sets the size of the random same-sex marriage die.
     *
     * @param interestedInSameSexDiceSize the size of the random same-sex marriage die
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.INTERESTED_IN_SAME_SEX_DICE_SIZE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setInterestedInSameSexDiceSize(final int interestedInSameSexDiceSize) {
        set(CampaignOption.INTERESTED_IN_SAME_SEX_DICE_SIZE, interestedInSameSexDiceSize);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.INTERESTED_IN_BOTH_SEXES_DICE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getInterestedInBothSexesDiceSize() {
        return get(CampaignOption.INTERESTED_IN_BOTH_SEXES_DICE_SIZE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.INTERESTED_IN_BOTH_SEXES_DICE_SIZE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setInterestedInBothSexesDiceSize(final int interestedInBothSexesDiceSize) {
        set(CampaignOption.INTERESTED_IN_BOTH_SEXES_DICE_SIZE, interestedInBothSexesDiceSize);
    }

    /**
     * @return the number of sides on the die used to determine whether marriage occurs outside of current personnel
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.RANDOM_NEW_DEPENDENT_MARRIAGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getRandomNewDependentMarriage() {
        return get(CampaignOption.RANDOM_NEW_DEPENDENT_MARRIAGE);
    }

    /**
     * Sets the size of the die used to determine whether marriage occurs outside of current personnel
     *
     * @param randomNewDependentMarriage the size of the die used to determine whether marriage occurs outside of
     *                                   current personnel
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RANDOM_NEW_DEPENDENT_MARRIAGE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRandomNewDependentMarriage(final int randomNewDependentMarriage) {
        set(CampaignOption.RANDOM_NEW_DEPENDENT_MARRIAGE, randomNewDependentMarriage);
    }
    // endregion Marriage

    // region Divorce

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_MANUAL_DIVORCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseManualDivorce() {
        return get(CampaignOption.USE_MANUAL_DIVORCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_MANUAL_DIVORCE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseManualDivorce(final boolean useManualDivorce) {
        set(CampaignOption.USE_MANUAL_DIVORCE, useManualDivorce);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_CLAN_PERSONNEL_DIVORCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseClanPersonnelDivorce() {
        return get(CampaignOption.USE_CLAN_PERSONNEL_DIVORCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_CLAN_PERSONNEL_DIVORCE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseClanPersonnelDivorce(final boolean useClanPersonnelDivorce) {
        set(CampaignOption.USE_CLAN_PERSONNEL_DIVORCE, useClanPersonnelDivorce);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_PRISONER_DIVORCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUsePrisonerDivorce() {
        return get(CampaignOption.USE_PRISONER_DIVORCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_PRISONER_DIVORCE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUsePrisonerDivorce(final boolean usePrisonerDivorce) {
        set(CampaignOption.USE_PRISONER_DIVORCE, usePrisonerDivorce);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DIVORCE_SURNAME_WEIGHTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Map<SplittingSurnameStyle, Integer> getDivorceSurnameWeights() {
        return get(CampaignOption.DIVORCE_SURNAME_WEIGHTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DIVORCE_SURNAME_WEIGHTS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDivorceSurnameWeights(final Map<SplittingSurnameStyle, Integer> divorceSurnameWeights) {
        set(CampaignOption.DIVORCE_SURNAME_WEIGHTS, divorceSurnameWeights);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.RANDOM_DIVORCE_METHOD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public RandomDivorceMethod getRandomDivorceMethod() {
        return get(CampaignOption.RANDOM_DIVORCE_METHOD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RANDOM_DIVORCE_METHOD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRandomDivorceMethod(final RandomDivorceMethod randomDivorceMethod) {
        set(CampaignOption.RANDOM_DIVORCE_METHOD, randomDivorceMethod);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_OPPOSITE_SEX_DIVORCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomOppositeSexDivorce() {
        return get(CampaignOption.USE_RANDOM_OPPOSITE_SEX_DIVORCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_OPPOSITE_SEX_DIVORCE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomOppositeSexDivorce(final boolean useRandomOppositeSexDivorce) {
        set(CampaignOption.USE_RANDOM_OPPOSITE_SEX_DIVORCE, useRandomOppositeSexDivorce);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_SAME_SEX_DIVORCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomSameSexDivorce() {
        return get(CampaignOption.USE_RANDOM_SAME_SEX_DIVORCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_SAME_SEX_DIVORCE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomSameSexDivorce(final boolean useRandomSameSexDivorce) {
        set(CampaignOption.USE_RANDOM_SAME_SEX_DIVORCE, useRandomSameSexDivorce);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_DIVORCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomClanPersonnelDivorce() {
        return get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_DIVORCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_DIVORCE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomClanPersonnelDivorce(final boolean useRandomClanPersonnelDivorce) {
        set(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_DIVORCE, useRandomClanPersonnelDivorce);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_PRISONER_DIVORCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomPrisonerDivorce() {
        return get(CampaignOption.USE_RANDOM_PRISONER_DIVORCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_PRISONER_DIVORCE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomPrisonerDivorce(final boolean useRandomPrisonerDivorce) {
        set(CampaignOption.USE_RANDOM_PRISONER_DIVORCE, useRandomPrisonerDivorce);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.RANDOM_DIVORCE_DICE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getRandomDivorceDiceSize() {
        return get(CampaignOption.RANDOM_DIVORCE_DICE_SIZE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RANDOM_DIVORCE_DICE_SIZE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRandomDivorceDiceSize(final int randomDivorceDiceSize) {
        set(CampaignOption.RANDOM_DIVORCE_DICE_SIZE, randomDivorceDiceSize);
    }
    // endregion Divorce

    // region Procreation

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_MANUAL_PROCREATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseManualProcreation() {
        return get(CampaignOption.USE_MANUAL_PROCREATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_MANUAL_PROCREATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseManualProcreation(final boolean useManualProcreation) {
        set(CampaignOption.USE_MANUAL_PROCREATION, useManualProcreation);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseClanPersonnelProcreation() {
        return get(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseClanPersonnelProcreation(final boolean useClanPersonnelProcreation) {
        set(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION, useClanPersonnelProcreation);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_PRISONER_PROCREATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUsePrisonerProcreation() {
        return get(CampaignOption.USE_PRISONER_PROCREATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_PRISONER_PROCREATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUsePrisonerProcreation(final boolean usePrisonerProcreation) {
        set(CampaignOption.USE_PRISONER_PROCREATION, usePrisonerProcreation);
    }

    /**
     * @return the X occurrences for there to be a single multiple child occurrence (i.e., 1 in X)
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.MULTIPLE_PREGNANCY_OCCURRENCES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMultiplePregnancyOccurrences() {
        return get(CampaignOption.MULTIPLE_PREGNANCY_OCCURRENCES);
    }

    /**
     * @param multiplePregnancyOccurrences the number of occurrences for there to be a single occurrence of a multiple
     *                                     child pregnancy (i.e., 1 in X)
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MULTIPLE_PREGNANCY_OCCURRENCES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMultiplePregnancyOccurrences(final int multiplePregnancyOccurrences) {
        set(CampaignOption.MULTIPLE_PREGNANCY_OCCURRENCES, multiplePregnancyOccurrences);
    }

    /**
     * @return what style of surname to use for a baby
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.BABY_SURNAME_STYLE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public BabySurnameStyle getBabySurnameStyle() {
        return get(CampaignOption.BABY_SURNAME_STYLE);
    }

    /**
     * @param babySurnameStyle the style of surname to use for a baby
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.BABY_SURNAME_STYLE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setBabySurnameStyle(final BabySurnameStyle babySurnameStyle) {
        set(CampaignOption.BABY_SURNAME_STYLE, babySurnameStyle);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ASSIGN_NON_PRISONER_BABIES_FOUNDER_TAG)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAssignNonPrisonerBabiesFounderTag() {
        return get(CampaignOption.ASSIGN_NON_PRISONER_BABIES_FOUNDER_TAG);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ASSIGN_NON_PRISONER_BABIES_FOUNDER_TAG, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAssignNonPrisonerBabiesFounderTag(final boolean assignNonPrisonerBabiesFounderTag) {
        set(CampaignOption.ASSIGN_NON_PRISONER_BABIES_FOUNDER_TAG, assignNonPrisonerBabiesFounderTag);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ASSIGN_CHILDREN_OF_FOUNDERS_FOUNDER_TAG)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAssignChildrenOfFoundersFounderTag() {
        return get(CampaignOption.ASSIGN_CHILDREN_OF_FOUNDERS_FOUNDER_TAG);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ASSIGN_CHILDREN_OF_FOUNDERS_FOUNDER_TAG, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAssignChildrenOfFoundersFounderTag(final boolean assignChildrenOfFoundersFounderTag) {
        set(CampaignOption.ASSIGN_CHILDREN_OF_FOUNDERS_FOUNDER_TAG, assignChildrenOfFoundersFounderTag);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_MATERNITY_LEAVE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseMaternityLeave() {
        return get(CampaignOption.USE_MATERNITY_LEAVE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_MATERNITY_LEAVE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseMaternityLeave(final boolean useMaternityLeave) {
        set(CampaignOption.USE_MATERNITY_LEAVE, useMaternityLeave);
    }

    /**
     * @return whether to determine the father at birth instead of at conception
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DETERMINE_FATHER_AT_BIRTH)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDetermineFatherAtBirth() {
        return get(CampaignOption.DETERMINE_FATHER_AT_BIRTH);
    }

    /**
     * @param determineFatherAtBirth whether to determine the father at birth instead of at conception
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DETERMINE_FATHER_AT_BIRTH, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDetermineFatherAtBirth(final boolean determineFatherAtBirth) {
        set(CampaignOption.DETERMINE_FATHER_AT_BIRTH, determineFatherAtBirth);
    }

    /**
     * @return whether to show the expected or actual due date for personnel
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DISPLAY_TRUE_DUE_DATE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDisplayTrueDueDate() {
        return get(CampaignOption.DISPLAY_TRUE_DUE_DATE);
    }

    /**
     * @param displayTrueDueDate whether to show the expected or actual due date for personnel
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DISPLAY_TRUE_DUE_DATE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisplayTrueDueDate(final boolean displayTrueDueDate) {
        set(CampaignOption.DISPLAY_TRUE_DUE_DATE, displayTrueDueDate);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.NO_INTEREST_IN_CHILDREN_DICE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getNoInterestInChildrenDiceSize() {
        return get(CampaignOption.NO_INTEREST_IN_CHILDREN_DICE_SIZE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.NO_INTEREST_IN_CHILDREN_DICE_SIZE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setNoInterestInChildrenDiceSize(final int noInterestInChildrenDiceSize) {
        set(CampaignOption.NO_INTEREST_IN_CHILDREN_DICE_SIZE, noInterestInChildrenDiceSize);
    }

    /**
     * @return whether to log procreation
     *
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.LOG_PROCREATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isLogProcreation() {
        return get(CampaignOption.LOG_PROCREATION);
    }

    /**
     * @param logProcreation whether to log procreation
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.LOG_PROCREATION,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setLogProcreation(final boolean logProcreation) {
        set(CampaignOption.LOG_PROCREATION, logProcreation);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.RANDOM_PROCREATION_METHOD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public RandomProcreationMethod getRandomProcreationMethod() {
        return get(CampaignOption.RANDOM_PROCREATION_METHOD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RANDOM_PROCREATION_METHOD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRandomProcreationMethod(final RandomProcreationMethod randomProcreationMethod) {
        set(CampaignOption.RANDOM_PROCREATION_METHOD, randomProcreationMethod);
    }

    /**
     * @return whether to use random procreation for personnel without a spouse
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRelationshiplessRandomProcreation() {
        return get(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION);
    }

    /**
     * @param useRelationshiplessRandomProcreation whether to use random procreation without a spouse
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRelationshiplessRandomProcreation(final boolean useRelationshiplessRandomProcreation) {
        set(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION, useRelationshiplessRandomProcreation);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomClanPersonnelProcreation() {
        return get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomClanPersonnelProcreation(final boolean useRandomClanPersonnelProcreation) {
        set(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION, useRandomClanPersonnelProcreation);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_PRISONER_PROCREATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomPrisonerProcreation() {
        return get(CampaignOption.USE_RANDOM_PRISONER_PROCREATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_PRISONER_PROCREATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomPrisonerProcreation(final boolean useRandomPrisonerProcreation) {
        set(CampaignOption.USE_RANDOM_PRISONER_PROCREATION, useRandomPrisonerProcreation);
    }

    /**
     * This gets the decimal chance (between 0 and 1) of random procreation occurring
     *
     * @return the chance, with a value between 0 and 1
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIP_DICE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getRandomProcreationRelationshipDiceSize() {
        return get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIP_DICE_SIZE);
    }

    /**
     * This sets the dice size for random procreation
     *
     * @param randomProcreationRelationshipDiceSize the chance, with a value between 0 and 1
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RANDOM_PROCREATION_RELATIONSHIP_DICE_SIZE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRandomProcreationRelationshipDiceSize(final int randomProcreationRelationshipDiceSize) {
        set(CampaignOption.RANDOM_PROCREATION_RELATIONSHIP_DICE_SIZE, randomProcreationRelationshipDiceSize);
    }

    /**
     * @return the dice size for random procreation
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIPLESS_DICE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getRandomProcreationRelationshiplessDiceSize() {
        return get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIPLESS_DICE_SIZE);
    }

    /**
     * This sets the decimal chance (between 0 and 1) of random procreation occurring without a relationship
     *
     * @param randomProcreationRelationshiplessDiceSize the chance, with a value between 0 and 1
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RANDOM_PROCREATION_RELATIONSHIPLESS_DICE_SIZE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRandomProcreationRelationshiplessDiceSize(final int randomProcreationRelationshiplessDiceSize) {
        set(CampaignOption.RANDOM_PROCREATION_RELATIONSHIPLESS_DICE_SIZE,
              randomProcreationRelationshiplessDiceSize);
    }
    // endregion Procreation

    // region Death

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_EDUCATION_MODULE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseEducationModule() {
        return get(CampaignOption.USE_EDUCATION_MODULE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_EDUCATION_MODULE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseEducationModule(boolean useEducationModule) {
        set(CampaignOption.USE_EDUCATION_MODULE, useEducationModule);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.CURRICULUM_XP_RATE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getCurriculumXpRate() {
        return get(CampaignOption.CURRICULUM_XP_RATE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.CURRICULUM_XP_RATE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setCurriculumXpRate(final int curriculumXpRate) {
        set(CampaignOption.CURRICULUM_XP_RATE, curriculumXpRate);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MAXIMUM_JUMP_COUNT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getMaximumJumpCount() {
        return get(CampaignOption.MAXIMUM_JUMP_COUNT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MAXIMUM_JUMP_COUNT,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMaximumJumpCount(Integer maximumJumpCount) {
        set(CampaignOption.MAXIMUM_JUMP_COUNT, maximumJumpCount);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_REEDUCATION_CAMPS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseReeducationCamps() {
        return get(CampaignOption.USE_REEDUCATION_CAMPS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_REEDUCATION_CAMPS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseReeducationCamps(boolean useReeducationCamps) {
        set(CampaignOption.USE_REEDUCATION_CAMPS, useReeducationCamps);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ENABLE_LOCAL_ACADEMIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableLocalAcademies() {
        return get(CampaignOption.ENABLE_LOCAL_ACADEMIES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLE_LOCAL_ACADEMIES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableLocalAcademies(boolean enableLocalAcademies) {
        set(CampaignOption.ENABLE_LOCAL_ACADEMIES, enableLocalAcademies);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ENABLE_PRESTIGIOUS_ACADEMIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnablePrestigiousAcademies() {
        return get(CampaignOption.ENABLE_PRESTIGIOUS_ACADEMIES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLE_PRESTIGIOUS_ACADEMIES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnablePrestigiousAcademies(boolean enablePrestigiousAcademies) {
        set(CampaignOption.ENABLE_PRESTIGIOUS_ACADEMIES, enablePrestigiousAcademies);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ENABLE_UNIT_EDUCATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableUnitEducation() {
        return get(CampaignOption.ENABLE_UNIT_EDUCATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLE_UNIT_EDUCATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableUnitEducation(boolean enableUnitEducation) {
        set(CampaignOption.ENABLE_UNIT_EDUCATION, enableUnitEducation);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ENABLE_OVERRIDE_REQUIREMENTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableOverrideRequirements() {
        return get(CampaignOption.ENABLE_OVERRIDE_REQUIREMENTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLE_OVERRIDE_REQUIREMENTS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableOverrideRequirements(boolean enableOverrideRequirements) {
        set(CampaignOption.ENABLE_OVERRIDE_REQUIREMENTS, enableOverrideRequirements);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ENABLE_SHOW_INELIGIBLE_ACADEMIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableShowIneligibleAcademies() {
        return get(CampaignOption.ENABLE_SHOW_INELIGIBLE_ACADEMIES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLE_SHOW_INELIGIBLE_ACADEMIES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableShowIneligibleAcademies(boolean enableShowIneligibleAcademies) {
        set(CampaignOption.ENABLE_SHOW_INELIGIBLE_ACADEMIES, enableShowIneligibleAcademies);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ENTRANCE_EXAM_BASE_TARGET_NUMBER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getEntranceExamBaseTargetNumber() {
        return get(CampaignOption.ENTRANCE_EXAM_BASE_TARGET_NUMBER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENTRANCE_EXAM_BASE_TARGET_NUMBER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEntranceExamBaseTargetNumber(int entranceExamBaseTargetNumber) {
        set(CampaignOption.ENTRANCE_EXAM_BASE_TARGET_NUMBER, entranceExamBaseTargetNumber);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.FACULTY_XP_RATE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Double getFacultyXpRate() {
        return get(CampaignOption.FACULTY_XP_RATE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.FACULTY_XP_RATE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setFacultyXpRate(Double facultyXpRate) {
        set(CampaignOption.FACULTY_XP_RATE, facultyXpRate);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ENABLE_BONUSES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableBonuses() {
        return get(CampaignOption.ENABLE_BONUSES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ENABLE_BONUSES,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableBonuses(boolean enableBonuses) {
        set(CampaignOption.ENABLE_BONUSES, enableBonuses);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ADULT_DROPOUT_CHANCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getAdultDropoutChance() {
        return get(CampaignOption.ADULT_DROPOUT_CHANCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ADULT_DROPOUT_CHANCE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAdultDropoutChance(Integer adultDropoutChance) {
        set(CampaignOption.ADULT_DROPOUT_CHANCE, adultDropoutChance);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.CHILDREN_DROPOUT_CHANCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getChildrenDropoutChance() {
        return get(CampaignOption.CHILDREN_DROPOUT_CHANCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CHILDREN_DROPOUT_CHANCE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setChildrenDropoutChance(Integer childrenDropoutChance) {
        set(CampaignOption.CHILDREN_DROPOUT_CHANCE, childrenDropoutChance);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ALL_AGES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAllAges() {
        return get(CampaignOption.ALL_AGES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ALL_AGES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAllAges(boolean allAges) {
        set(CampaignOption.ALL_AGES, allAges);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MILITARY_ACADEMY_ACCIDENTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getMilitaryAcademyAccidents() {
        return get(CampaignOption.MILITARY_ACADEMY_ACCIDENTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MILITARY_ACADEMY_ACCIDENTS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMilitaryAcademyAccidents(Integer militaryAcademyAccidents) {
        set(CampaignOption.MILITARY_ACADEMY_ACCIDENTS, militaryAcademyAccidents);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Map<AgeGroup, Boolean> getEnabledRandomDeathAgeGroups() {
        return get(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnabledRandomDeathAgeGroups(final Map<AgeGroup, Boolean> enabledRandomDeathAgeGroups) {
        set(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS, enabledRandomDeathAgeGroups);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_RANDOM_DEATH_SUICIDE_CAUSE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRandomDeathSuicideCause() {
        return get(CampaignOption.USE_RANDOM_DEATH_SUICIDE_CAUSE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_RANDOM_DEATH_SUICIDE_CAUSE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRandomDeathSuicideCause(final boolean useRandomDeathSuicideCause) {
        set(CampaignOption.USE_RANDOM_DEATH_SUICIDE_CAUSE, useRandomDeathSuicideCause);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.RANDOM_DEATH_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getRandomDeathMultiplier() {
        return get(CampaignOption.RANDOM_DEATH_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RANDOM_DEATH_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRandomDeathMultiplier(final double randomDeathMultiplier) {
        set(CampaignOption.RANDOM_DEATH_MULTIPLIER, randomDeathMultiplier);
    }
    // endregion Death

    // region Awards

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ISSUE_POSTHUMOUS_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isIssuePosthumousAwards() {
        return get(CampaignOption.ISSUE_POSTHUMOUS_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ISSUE_POSTHUMOUS_AWARDS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setIssuePosthumousAwards(final boolean issuePosthumousAwards) {
        set(CampaignOption.ISSUE_POSTHUMOUS_AWARDS, issuePosthumousAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ISSUE_BEST_AWARD_ONLY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isIssueBestAwardOnly() {
        return get(CampaignOption.ISSUE_BEST_AWARD_ONLY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ISSUE_BEST_AWARD_ONLY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setIssueBestAwardOnly(final boolean issueBestAwardOnly) {
        set(CampaignOption.ISSUE_BEST_AWARD_ONLY, issueBestAwardOnly);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.IGNORE_STANDARD_SET)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isIgnoreStandardSet() {
        return get(CampaignOption.IGNORE_STANDARD_SET);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.IGNORE_STANDARD_SET,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setIgnoreStandardSet(final boolean ignoreStandardSet) {
        set(CampaignOption.IGNORE_STANDARD_SET, ignoreStandardSet);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AWARD_TIER_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAwardTierSize() {
        return get(CampaignOption.AWARD_TIER_SIZE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.AWARD_TIER_SIZE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAwardTierSize(final int awardTierSize) {
        set(CampaignOption.AWARD_TIER_SIZE, awardTierSize);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AWARD_BONUS_STYLE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public AwardBonus getAwardBonusStyle() {
        return get(CampaignOption.AWARD_BONUS_STYLE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.AWARD_BONUS_STYLE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAwardBonusStyle(final AwardBonus awardBonusStyle) {
        set(CampaignOption.AWARD_BONUS_STYLE, awardBonusStyle);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ENABLE_AUTO_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableAutoAwards() {
        return get(CampaignOption.ENABLE_AUTO_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ENABLE_AUTO_AWARDS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableAutoAwards(final boolean enableAutoAwards) {
        set(CampaignOption.ENABLE_AUTO_AWARDS, enableAutoAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ENABLE_CONTRACT_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableContractAwards() {
        return get(CampaignOption.ENABLE_CONTRACT_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLE_CONTRACT_AWARDS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableContractAwards(final boolean enableContractAwards) {
        set(CampaignOption.ENABLE_CONTRACT_AWARDS, enableContractAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ENABLE_FACTION_HUNTER_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableFactionHunterAwards() {
        return get(CampaignOption.ENABLE_FACTION_HUNTER_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLE_FACTION_HUNTER_AWARDS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableFactionHunterAwards(final boolean enableFactionHunterAwards) {
        set(CampaignOption.ENABLE_FACTION_HUNTER_AWARDS, enableFactionHunterAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ENABLE_INJURY_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableInjuryAwards() {
        return get(CampaignOption.ENABLE_INJURY_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ENABLE_INJURY_AWARDS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableInjuryAwards(final boolean enableInjuryAwards) {
        set(CampaignOption.ENABLE_INJURY_AWARDS, enableInjuryAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ENABLE_INDIVIDUAL_KILL_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableIndividualKillAwards() {
        return get(CampaignOption.ENABLE_INDIVIDUAL_KILL_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLE_INDIVIDUAL_KILL_AWARDS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableIndividualKillAwards(final boolean enableIndividualKillAwards) {
        set(CampaignOption.ENABLE_INDIVIDUAL_KILL_AWARDS, enableIndividualKillAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ENABLE_FORMATION_KILL_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableFormationKillAwards() {
        return get(CampaignOption.ENABLE_FORMATION_KILL_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLE_FORMATION_KILL_AWARDS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableFormationKillAwards(final boolean enableFormationKillAwards) {
        set(CampaignOption.ENABLE_FORMATION_KILL_AWARDS, enableFormationKillAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ENABLE_RANK_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableRankAwards() {
        return get(CampaignOption.ENABLE_RANK_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ENABLE_RANK_AWARDS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableRankAwards(final boolean enableRankAwards) {
        set(CampaignOption.ENABLE_RANK_AWARDS, enableRankAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ENABLE_SCENARIO_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableScenarioAwards() {
        return get(CampaignOption.ENABLE_SCENARIO_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLE_SCENARIO_AWARDS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableScenarioAwards(final boolean enableScenarioAwards) {
        set(CampaignOption.ENABLE_SCENARIO_AWARDS, enableScenarioAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ENABLE_SKILL_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableSkillAwards() {
        return get(CampaignOption.ENABLE_SKILL_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ENABLE_SKILL_AWARDS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableSkillAwards(final boolean enableSkillAwards) {
        set(CampaignOption.ENABLE_SKILL_AWARDS, enableSkillAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ENABLE_THEATRE_OF_WAR_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableTheatreOfWarAwards() {
        return get(CampaignOption.ENABLE_THEATRE_OF_WAR_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLE_THEATRE_OF_WAR_AWARDS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableTheatreOfWarAwards(final boolean enableTheatreOfWarAwards) {
        set(CampaignOption.ENABLE_THEATRE_OF_WAR_AWARDS, enableTheatreOfWarAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ENABLE_TIME_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableTimeAwards() {
        return get(CampaignOption.ENABLE_TIME_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ENABLE_TIME_AWARDS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableTimeAwards(final boolean enableTimeAwards) {
        set(CampaignOption.ENABLE_TIME_AWARDS, enableTimeAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ENABLE_TRAINING_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableTrainingAwards() {
        return get(CampaignOption.ENABLE_TRAINING_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENABLE_TRAINING_AWARDS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableTrainingAwards(final boolean enableTrainingAwards) {
        set(CampaignOption.ENABLE_TRAINING_AWARDS, enableTrainingAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ENABLE_MISC_AWARDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableMiscAwards() {
        return get(CampaignOption.ENABLE_MISC_AWARDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ENABLE_MISC_AWARDS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableMiscAwards(final boolean enableMiscAwards) {
        set(CampaignOption.ENABLE_MISC_AWARDS, enableMiscAwards);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AWARD_SET_FILTER_LIST)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public String getAwardSetFilterList() {
        return get(CampaignOption.AWARD_SET_FILTER_LIST);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AWARD_SET_FILTER_LIST, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAwardSetFilterList(final String awardSetFilterList) {
        set(CampaignOption.AWARD_SET_FILTER_LIST, awardSetFilterList);
    }
    // endregion Awards
    // endregion Personnel Tab

    // region Finances Tab

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAY_FOR_PARTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPayForParts() {
        return get(CampaignOption.PAY_FOR_PARTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAY_FOR_PARTS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayForParts(final boolean payForParts) {
        set(CampaignOption.PAY_FOR_PARTS, payForParts);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAY_FOR_REPAIRS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPayForRepairs() {
        return get(CampaignOption.PAY_FOR_REPAIRS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAY_FOR_REPAIRS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayForRepairs(final boolean payForRepairs) {
        set(CampaignOption.PAY_FOR_REPAIRS, payForRepairs);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAY_FOR_UNITS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPayForUnits() {
        return get(CampaignOption.PAY_FOR_UNITS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAY_FOR_UNITS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayForUnits(final boolean payForUnits) {
        set(CampaignOption.PAY_FOR_UNITS, payForUnits);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAY_FOR_SALARIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPayForSalaries() {
        return get(CampaignOption.PAY_FOR_SALARIES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAY_FOR_SALARIES,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayForSalaries(final boolean payForSalaries) {
        set(CampaignOption.PAY_FOR_SALARIES, payForSalaries);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAY_FOR_OVERHEAD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPayForOverhead() {
        return get(CampaignOption.PAY_FOR_OVERHEAD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAY_FOR_OVERHEAD,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayForOverhead(final boolean payForOverhead) {
        set(CampaignOption.PAY_FOR_OVERHEAD, payForOverhead);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAY_FOR_MAINTAIN)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPayForMaintain() {
        return get(CampaignOption.PAY_FOR_MAINTAIN);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayForMaintain(final boolean payForMaintain) {
        set(CampaignOption.PAY_FOR_MAINTAIN, payForMaintain);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAY_FOR_TRANSPORT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPayForTransport() {
        return get(CampaignOption.PAY_FOR_TRANSPORT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAY_FOR_TRANSPORT,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayForTransport(final boolean payForTransport) {
        set(CampaignOption.PAY_FOR_TRANSPORT, payForTransport);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SELL_UNITS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isSellUnits() {
        return get(CampaignOption.SELL_UNITS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.SELL_UNITS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setSellUnits(final boolean sellUnits) {
        set(CampaignOption.SELL_UNITS, sellUnits);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SELL_PARTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isSellParts() {
        return get(CampaignOption.SELL_PARTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.SELL_PARTS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setSellParts(final boolean sellParts) {
        set(CampaignOption.SELL_PARTS, sellParts);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAY_FOR_RECRUITMENT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPayForRecruitment() {
        return get(CampaignOption.PAY_FOR_RECRUITMENT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAY_FOR_RECRUITMENT,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayForRecruitment(final boolean payForRecruitment) {
        set(CampaignOption.PAY_FOR_RECRUITMENT, payForRecruitment);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAY_FOR_FOOD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPayForFood() {
        return get(CampaignOption.PAY_FOR_FOOD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAY_FOR_FOOD,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayForFood(final boolean payForFood) {
        set(CampaignOption.PAY_FOR_FOOD, payForFood);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PAY_FOR_HOUSING)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPayForHousing() {
        return get(CampaignOption.PAY_FOR_HOUSING);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PAY_FOR_HOUSING,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPayForHousing(final boolean payForHousing) {
        set(CampaignOption.PAY_FOR_HOUSING, payForHousing);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.RENTED_FACILITIES_COST_HOSPITAL_BEDS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getRentedFacilitiesCostHospitalBeds() {
        return get(CampaignOption.RENTED_FACILITIES_COST_HOSPITAL_BEDS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RENTED_FACILITIES_COST_HOSPITAL_BEDS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRentedFacilitiesCostHospitalBeds(final int rentedFacilitiesCostHospitalBeds) {
        set(CampaignOption.RENTED_FACILITIES_COST_HOSPITAL_BEDS, rentedFacilitiesCostHospitalBeds);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.RENTED_FACILITIES_COST_KITCHENS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getRentedFacilitiesCostKitchens() {
        return get(CampaignOption.RENTED_FACILITIES_COST_KITCHENS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RENTED_FACILITIES_COST_KITCHENS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRentedFacilitiesCostKitchens(final int rentedFacilitiesCostKitchens) {
        set(CampaignOption.RENTED_FACILITIES_COST_KITCHENS, rentedFacilitiesCostKitchens);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.RENTED_FACILITIES_COST_HOLDING_CELLS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getRentedFacilitiesCostHoldingCells() {
        return get(CampaignOption.RENTED_FACILITIES_COST_HOLDING_CELLS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RENTED_FACILITIES_COST_HOLDING_CELLS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRentedFacilitiesCostHoldingCells(final int rentedFacilitiesCostHoldingCells) {
        set(CampaignOption.RENTED_FACILITIES_COST_HOLDING_CELLS, rentedFacilitiesCostHoldingCells);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.RENTED_FACILITIES_COST_REPAIR_BAYS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getRentedFacilitiesCostRepairBays() {
        return get(CampaignOption.RENTED_FACILITIES_COST_REPAIR_BAYS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RENTED_FACILITIES_COST_REPAIR_BAYS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRentedFacilitiesCostRepairBays(final int rentedFacilitiesCostRepairBays) {
        set(CampaignOption.RENTED_FACILITIES_COST_REPAIR_BAYS, rentedFacilitiesCostRepairBays);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_LOAN_LIMITS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseLoanLimits() {
        return get(CampaignOption.USE_LOAN_LIMITS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_LOAN_LIMITS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setLoanLimits(final boolean useLoanLimits) {
        set(CampaignOption.USE_LOAN_LIMITS, useLoanLimits);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_PERCENTAGE_MAINTENANCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUsePercentageMaintenance() {
        return get(CampaignOption.USE_PERCENTAGE_MAINTENANCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_PERCENTAGE_MAINTENANCE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUsePercentageMaintenance(final boolean usePercentageMaintenance) {
        set(CampaignOption.USE_PERCENTAGE_MAINTENANCE, usePercentageMaintenance);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.INFANTRY_DONT_COUNT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isInfantryDontCount() {
        return get(CampaignOption.INFANTRY_DONT_COUNT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.INFANTRY_DONT_COUNT,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseInfantryDontCount(final boolean infantryDontCount) {
        set(CampaignOption.INFANTRY_DONT_COUNT, infantryDontCount);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_PEACETIME_COST)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUsePeacetimeCost() {
        return get(CampaignOption.USE_PEACETIME_COST);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_PEACETIME_COST,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUsePeacetimeCost(final boolean usePeacetimeCost) {
        set(CampaignOption.USE_PEACETIME_COST, usePeacetimeCost);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_EXTENDED_PARTS_MODIFIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseExtendedPartsModifier() {
        return get(CampaignOption.USE_EXTENDED_PARTS_MODIFIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_EXTENDED_PARTS_MODIFIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseExtendedPartsModifier(final boolean useExtendedPartsModifier) {
        set(CampaignOption.USE_EXTENDED_PARTS_MODIFIER, useExtendedPartsModifier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SHOW_PEACETIME_COST)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isShowPeacetimeCost() {
        return get(CampaignOption.SHOW_PEACETIME_COST);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.SHOW_PEACETIME_COST,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setShowPeacetimeCost(final boolean showPeacetimeCost) {
        set(CampaignOption.SHOW_PEACETIME_COST, showPeacetimeCost);
    }

    /**
     * @return the duration of a financial year
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.FINANCIAL_YEAR_DURATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public FinancialYearDuration getFinancialYearDuration() {
        return get(CampaignOption.FINANCIAL_YEAR_DURATION);
    }

    /**
     * @param financialYearDuration the financial year duration to set
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.FINANCIAL_YEAR_DURATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setFinancialYearDuration(final FinancialYearDuration financialYearDuration) {
        set(CampaignOption.FINANCIAL_YEAR_DURATION, financialYearDuration);
    }

    /**
     * @return whether to export finances to CSV at the end of a financial year
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.NEW_FINANCIAL_YEAR_FINANCES_TO_CSV_EXPORT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isNewFinancialYearFinancesToCSVExport() {
        return get(CampaignOption.NEW_FINANCIAL_YEAR_FINANCES_TO_CSV_EXPORT);
    }

    /**
     * @param newFinancialYearFinancesToCSVExport whether to export finances to CSV at the end of a financial year
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.NEW_FINANCIAL_YEAR_FINANCES_TO_CSV_EXPORT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setNewFinancialYearFinancesToCSVExport(final boolean newFinancialYearFinancesToCSVExport) {
        set(CampaignOption.NEW_FINANCIAL_YEAR_FINANCES_TO_CSV_EXPORT, newFinancialYearFinancesToCSVExport);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SIMULATE_GRAY_MONDAY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isSimulateGrayMonday() {
        return get(CampaignOption.SIMULATE_GRAY_MONDAY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.SIMULATE_GRAY_MONDAY,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setSimulateGrayMonday(final boolean simulateGrayMonday) {
        set(CampaignOption.SIMULATE_GRAY_MONDAY, simulateGrayMonday);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ALLOW_MONTHLY_REINVESTMENT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAllowMonthlyReinvestment() {
        return get(CampaignOption.ALLOW_MONTHLY_REINVESTMENT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ALLOW_MONTHLY_REINVESTMENT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAllowMonthlyReinvestment(final boolean allowMonthlyReinvestment) {
        set(CampaignOption.ALLOW_MONTHLY_REINVESTMENT, allowMonthlyReinvestment);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DISPLAY_ALL_ATTRIBUTES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDisplayAllAttributes() {
        return get(CampaignOption.DISPLAY_ALL_ATTRIBUTES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DISPLAY_ALL_ATTRIBUTES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisplayAllAttributes(final boolean displayAllAttributes) {
        set(CampaignOption.DISPLAY_ALL_ATTRIBUTES, displayAllAttributes);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ALLOW_MONTHLY_CONNECTIONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAllowMonthlyConnections() {
        return get(CampaignOption.ALLOW_MONTHLY_CONNECTIONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ALLOW_MONTHLY_CONNECTIONS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAllowMonthlyConnections(final boolean allowMonthlyConnections) {
        set(CampaignOption.ALLOW_MONTHLY_CONNECTIONS, allowMonthlyConnections);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_BETTER_EXTRA_INCOME)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseBetterExtraIncome() {
        return get(CampaignOption.USE_BETTER_EXTRA_INCOME);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_BETTER_EXTRA_INCOME, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseBetterExtraIncome(final boolean useBetterExtraIncome) {
        set(CampaignOption.USE_BETTER_EXTRA_INCOME, useBetterExtraIncome);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_SMALL_ARMS_ONLY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseSmallArmsOnly() {
        return get(CampaignOption.USE_SMALL_ARMS_ONLY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_SMALL_ARMS_ONLY,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseSmallArmsOnly(final boolean useSmallArmsOnly) {
        set(CampaignOption.USE_SMALL_ARMS_ONLY, useSmallArmsOnly);
    }

    // region Price Multipliers

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.COMMON_PART_PRICE_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getCommonPartPriceMultiplier() {
        return get(CampaignOption.COMMON_PART_PRICE_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.COMMON_PART_PRICE_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setCommonPartPriceMultiplier(final double commonPartPriceMultiplier) {
        set(CampaignOption.COMMON_PART_PRICE_MULTIPLIER, commonPartPriceMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getInnerSphereUnitPriceMultiplier() {
        return get(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setInnerSphereUnitPriceMultiplier(final double innerSphereUnitPriceMultiplier) {
        set(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER, innerSphereUnitPriceMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.INNER_SPHERE_PART_PRICE_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getInnerSpherePartPriceMultiplier() {
        return get(CampaignOption.INNER_SPHERE_PART_PRICE_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.INNER_SPHERE_PART_PRICE_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setInnerSpherePartPriceMultiplier(final double innerSpherePartPriceMultiplier) {
        set(CampaignOption.INNER_SPHERE_PART_PRICE_MULTIPLIER, innerSpherePartPriceMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.CLAN_UNIT_PRICE_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getClanUnitPriceMultiplier() {
        return get(CampaignOption.CLAN_UNIT_PRICE_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CLAN_UNIT_PRICE_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setClanUnitPriceMultiplier(final double clanUnitPriceMultiplier) {
        set(CampaignOption.CLAN_UNIT_PRICE_MULTIPLIER, clanUnitPriceMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.CLAN_PART_PRICE_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getClanPartPriceMultiplier() {
        return get(CampaignOption.CLAN_PART_PRICE_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CLAN_PART_PRICE_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setClanPartPriceMultiplier(final double clanPartPriceMultiplier) {
        set(CampaignOption.CLAN_PART_PRICE_MULTIPLIER, clanPartPriceMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.MIXED_TECH_UNIT_PRICE_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getMixedTechUnitPriceMultiplier() {
        return get(CampaignOption.MIXED_TECH_UNIT_PRICE_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MIXED_TECH_UNIT_PRICE_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMixedTechUnitPriceMultiplier(final double mixedTechUnitPriceMultiplier) {
        set(CampaignOption.MIXED_TECH_UNIT_PRICE_MULTIPLIER, mixedTechUnitPriceMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USED_PART_PRICE_MULTIPLIERS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double[] getUsedPartPriceMultipliers() {
        return get(CampaignOption.USED_PART_PRICE_MULTIPLIERS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USED_PART_PRICE_MULTIPLIERS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUsedPartPriceMultipliers(final double... usedPartPriceMultipliers) {
        set(CampaignOption.USED_PART_PRICE_MULTIPLIERS, usedPartPriceMultipliers);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.DAMAGED_PARTS_VALUE_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getDamagedPartsValueMultiplier() {
        return get(CampaignOption.DAMAGED_PARTS_VALUE_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DAMAGED_PARTS_VALUE_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDamagedPartsValueMultiplier(final double damagedPartsValueMultiplier) {
        set(CampaignOption.DAMAGED_PARTS_VALUE_MULTIPLIER, damagedPartsValueMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.UNREPAIRABLE_PARTS_VALUE_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getUnrepairablePartsValueMultiplier() {
        return get(CampaignOption.UNREPAIRABLE_PARTS_VALUE_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.UNREPAIRABLE_PARTS_VALUE_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUnrepairablePartsValueMultiplier(final double unrepairablePartsValueMultiplier) {
        set(CampaignOption.UNREPAIRABLE_PARTS_VALUE_MULTIPLIER, unrepairablePartsValueMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.CANCELLED_ORDER_REFUND_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getCancelledOrderRefundMultiplier() {
        return get(CampaignOption.CANCELLED_ORDER_REFUND_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CANCELLED_ORDER_REFUND_MULTIPLIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setCancelledOrderRefundMultiplier(final double cancelledOrderRefundMultiplier) {
        set(CampaignOption.CANCELLED_ORDER_REFUND_MULTIPLIER, cancelledOrderRefundMultiplier);
    }
    // endregion Price Multipliers

    // region Taxes

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_TAXES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseTaxes() {
        return get(CampaignOption.USE_TAXES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_TAXES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseTaxes(final boolean useTaxes) {
        set(CampaignOption.USE_TAXES, useTaxes);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.TAXES_PERCENTAGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Integer getTaxesPercentage() {
        return get(CampaignOption.TAXES_PERCENTAGE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.TAXES_PERCENTAGE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTaxesPercentage(final int taxesPercentage) {
        set(CampaignOption.TAXES_PERCENTAGE, taxesPercentage);
    }
    // endregion Taxes
    // endregion Finances Tab

    // region Markets Tab
    // region Personnel Market

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PERSONNEL_MARKET_STYLE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public PersonnelMarketStyle getPersonnelMarketStyle() {
        return get(CampaignOption.PERSONNEL_MARKET_STYLE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PERSONNEL_MARKET_STYLE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPersonnelMarketStyle(final PersonnelMarketStyle personnelMarketStyle) {
        set(CampaignOption.PERSONNEL_MARKET_STYLE, personnelMarketStyle);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PERSONNEL_MARKET_NAME)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public String getPersonnelMarketName() {
        return get(CampaignOption.PERSONNEL_MARKET_NAME);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PERSONNEL_MARKET_NAME, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPersonnelMarketName(final String personnelMarketName) {
        set(CampaignOption.PERSONNEL_MARKET_NAME, personnelMarketName);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.PERSONNEL_MARKET_REPORT_REFRESH)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPersonnelMarketReportRefresh() {
        return get(CampaignOption.PERSONNEL_MARKET_REPORT_REFRESH);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PERSONNEL_MARKET_REPORT_REFRESH, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPersonnelMarketReportRefresh(final boolean personnelMarketReportRefresh) {
        set(CampaignOption.PERSONNEL_MARKET_REPORT_REFRESH, personnelMarketReportRefresh);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public Map<SkillLevel, Integer> getPersonnelMarketRandomRemovalTargets() {
        return get(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPersonnelMarketRandomRemovalTargets(
          final Map<SkillLevel, Integer> personnelMarketRandomRemovalTargets) {
        set(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS, personnelMarketRandomRemovalTargets);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.PERSONNEL_MARKET_DYLANS_WEIGHT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getPersonnelMarketDylansWeight() {
        return get(CampaignOption.PERSONNEL_MARKET_DYLANS_WEIGHT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PERSONNEL_MARKET_DYLANS_WEIGHT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPersonnelMarketDylansWeight(final double personnelMarketDylansWeight) {
        set(CampaignOption.PERSONNEL_MARKET_DYLANS_WEIGHT, personnelMarketDylansWeight);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_PERSONNEL_HIRE_HIRING_HALL_ONLY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUsePersonnelHireHiringHallOnly() {
        return get(CampaignOption.USE_PERSONNEL_HIRE_HIRING_HALL_ONLY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_PERSONNEL_HIRE_HIRING_HALL_ONLY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUsePersonnelHireHiringHallOnly(final boolean usePersonnelHireHiringHallOnly) {
        set(CampaignOption.USE_PERSONNEL_HIRE_HIRING_HALL_ONLY, usePersonnelHireHiringHallOnly);
    }
    // endregion Personnel Market

    // region Unit Market

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.UNIT_MARKET_METHOD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public UnitMarketMethod getUnitMarketMethod() {
        return get(CampaignOption.UNIT_MARKET_METHOD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.UNIT_MARKET_METHOD,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUnitMarketMethod(final UnitMarketMethod unitMarketMethod) {
        set(CampaignOption.UNIT_MARKET_METHOD, unitMarketMethod);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.UNIT_MARKET_REGIONAL_MEK_VARIATIONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUnitMarketRegionalMekVariations() {
        return get(CampaignOption.UNIT_MARKET_REGIONAL_MEK_VARIATIONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.UNIT_MARKET_REGIONAL_MEK_VARIATIONS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUnitMarketRegionalMekVariations(final boolean unitMarketRegionalMekVariations) {
        set(CampaignOption.UNIT_MARKET_REGIONAL_MEK_VARIATIONS, unitMarketRegionalMekVariations);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.UNIT_MARKET_ARTILLERY_UNIT_CHANCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getUnitMarketArtilleryUnitChance() {
        return get(CampaignOption.UNIT_MARKET_ARTILLERY_UNIT_CHANCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.UNIT_MARKET_ARTILLERY_UNIT_CHANCE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUnitMarketArtilleryUnitChance(final int unitMarketSpecialUnitChance) {
        set(CampaignOption.UNIT_MARKET_ARTILLERY_UNIT_CHANCE, unitMarketSpecialUnitChance);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.UNIT_MARKET_RARITY_MODIFIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getUnitMarketRarityModifier() {
        return get(CampaignOption.UNIT_MARKET_RARITY_MODIFIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.UNIT_MARKET_RARITY_MODIFIER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUnitMarketRarityModifier(final int unitMarketRarityModifier) {
        set(CampaignOption.UNIT_MARKET_RARITY_MODIFIER, unitMarketRarityModifier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.INSTANT_UNIT_MARKET_DELIVERY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isInstantUnitMarketDelivery() {
        return get(CampaignOption.INSTANT_UNIT_MARKET_DELIVERY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.INSTANT_UNIT_MARKET_DELIVERY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setInstantUnitMarketDelivery(final boolean instantUnitMarketDelivery) {
        set(CampaignOption.INSTANT_UNIT_MARKET_DELIVERY, instantUnitMarketDelivery);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.MOTHBALL_UNIT_MARKET_DELIVERIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isMothballUnitMarketDeliveries() {
        return get(CampaignOption.MOTHBALL_UNIT_MARKET_DELIVERIES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MOTHBALL_UNIT_MARKET_DELIVERIES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMothballUnitMarketDeliveries(final boolean mothballUnitMarketDeliveries) {
        set(CampaignOption.MOTHBALL_UNIT_MARKET_DELIVERIES, mothballUnitMarketDeliveries);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.UNIT_MARKET_REPORT_REFRESH)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUnitMarketReportRefresh() {
        return get(CampaignOption.UNIT_MARKET_REPORT_REFRESH);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.UNIT_MARKET_REPORT_REFRESH, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUnitMarketReportRefresh(final boolean unitMarketReportRefresh) {
        set(CampaignOption.UNIT_MARKET_REPORT_REFRESH, unitMarketReportRefresh);
    }
    // endregion Unit Market

    // region Contract Market

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.CONTRACT_MARKET_METHOD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public ContractMarketMethod getContractMarketMethod() {
        return get(CampaignOption.CONTRACT_MARKET_METHOD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CONTRACT_MARKET_METHOD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setContractMarketMethod(final ContractMarketMethod contractMarketMethod) {
        set(CampaignOption.CONTRACT_MARKET_METHOD, contractMarketMethod);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.CONTRACT_SEARCH_RADIUS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getContractSearchRadius() {
        return get(CampaignOption.CONTRACT_SEARCH_RADIUS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CONTRACT_SEARCH_RADIUS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setContractSearchRadius(final int contractSearchRadius) {
        set(CampaignOption.CONTRACT_SEARCH_RADIUS, contractSearchRadius);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.VARIABLE_CONTRACT_LENGTH)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isVariableContractLength() {
        return get(CampaignOption.VARIABLE_CONTRACT_LENGTH);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.VARIABLE_CONTRACT_LENGTH, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setVariableContractLength(final boolean variableContractLength) {
        set(CampaignOption.VARIABLE_CONTRACT_LENGTH, variableContractLength);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_DYNAMIC_DIFFICULTY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseDynamicDifficulty() {
        return get(CampaignOption.USE_DYNAMIC_DIFFICULTY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_DYNAMIC_DIFFICULTY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseDynamicDifficulty(final boolean useDynamicDifficulty) {
        set(CampaignOption.USE_DYNAMIC_DIFFICULTY, useDynamicDifficulty);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_BOLSTER_CONTRACT_SKILL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseBolsterContractSkill() {
        return get(CampaignOption.USE_BOLSTER_CONTRACT_SKILL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_BOLSTER_CONTRACT_SKILL, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseBolsterContractSkill(final boolean useBolsterContractSkill) {
        set(CampaignOption.USE_BOLSTER_CONTRACT_SKILL, useBolsterContractSkill);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.CONTRACT_MARKET_REPORT_REFRESH)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isContractMarketReportRefresh() {
        return get(CampaignOption.CONTRACT_MARKET_REPORT_REFRESH);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CONTRACT_MARKET_REPORT_REFRESH, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setContractMarketReportRefresh(final boolean contractMarketReportRefresh) {
        set(CampaignOption.CONTRACT_MARKET_REPORT_REFRESH, contractMarketReportRefresh);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.CONTRACT_MAX_SALVAGE_PERCENTAGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getContractMaxSalvagePercentage() {
        return get(CampaignOption.CONTRACT_MAX_SALVAGE_PERCENTAGE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CONTRACT_MAX_SALVAGE_PERCENTAGE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setContractMaxSalvagePercentage(final int contractMaxSalvagePercentage) {
        set(CampaignOption.CONTRACT_MAX_SALVAGE_PERCENTAGE, contractMaxSalvagePercentage);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DROP_SHIP_BONUS_PERCENTAGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getDropShipBonusPercentage() {
        return get(CampaignOption.DROP_SHIP_BONUS_PERCENTAGE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DROP_SHIP_BONUS_PERCENTAGE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDropShipBonusPercentage(final int dropShipBonusPercentage) {
        set(CampaignOption.DROP_SHIP_BONUS_PERCENTAGE, dropShipBonusPercentage);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PITY_CONTRACTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getPityContracts() {
        return get(CampaignOption.PITY_CONTRACTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.PITY_CONTRACTS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPityContracts(final int pityContracts) {
        set(CampaignOption.PITY_CONTRACTS, pityContracts);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.IS_USE_TWO_WAY_PAY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseTwoWayPay() {
        return get(CampaignOption.IS_USE_TWO_WAY_PAY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.IS_USE_TWO_WAY_PAY,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseTwoWayPay(final boolean isUseTwoWayPay) {
        set(CampaignOption.IS_USE_TWO_WAY_PAY, isUseTwoWayPay);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.IS_USE_CAM_OPS_SALVAGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseCamOpsSalvage() {
        return get(CampaignOption.IS_USE_CAM_OPS_SALVAGE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.IS_USE_CAM_OPS_SALVAGE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseCamOpsSalvage(final boolean isUseCamOpsSalvage) {
        set(CampaignOption.IS_USE_CAM_OPS_SALVAGE, isUseCamOpsSalvage);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.IS_USE_RISKY_SALVAGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseRiskySalvage() {
        return get(CampaignOption.IS_USE_RISKY_SALVAGE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.IS_USE_RISKY_SALVAGE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseRiskySalvage(final boolean isUseRiskySalvage) {
        set(CampaignOption.IS_USE_RISKY_SALVAGE, isUseRiskySalvage);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.IS_ENABLE_SALVAGE_FLAG_BY_DEFAULT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEnableSalvageFlagByDefault() {
        return get(CampaignOption.IS_ENABLE_SALVAGE_FLAG_BY_DEFAULT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.IS_ENABLE_SALVAGE_FLAG_BY_DEFAULT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnableSalvageFlagByDefault(final boolean isEnableSalvageFlagByDefault) {
        set(CampaignOption.IS_ENABLE_SALVAGE_FLAG_BY_DEFAULT, isEnableSalvageFlagByDefault);
    }
    // endregion Contract Market
    // endregion Markets Tab

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_ERA_MODS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseEraMods() {
        return get(CampaignOption.USE_ERA_MODS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_ERA_MODS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEraMods(final boolean useEraMods) {
        set(CampaignOption.USE_ERA_MODS, useEraMods);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ASSIGNED_TECH_FIRST)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAssignedTechFirst() {
        return get(CampaignOption.ASSIGNED_TECH_FIRST);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ASSIGNED_TECH_FIRST,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAssignedTechFirst(final boolean assignedTechFirst) {
        set(CampaignOption.ASSIGNED_TECH_FIRST, assignedTechFirst);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.RESET_TO_FIRST_TECH)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isResetToFirstTech() {
        return get(CampaignOption.RESET_TO_FIRST_TECH);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.RESET_TO_FIRST_TECH,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setResetToFirstTech(final boolean resetToFirstTech) {
        set(CampaignOption.RESET_TO_FIRST_TECH, resetToFirstTech);
    }

    /**
     * Checks whether administrative adjustments are applied for technician time calculations.
     *
     * <p>This configuration determines if technicians' daily available time should be adjusted
     * using administrative multipliers in relevant calculations.</p>
     *
     * @return {@code true} if administrative adjustments are enabled for technicians, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.TECHS_USE_ADMINISTRATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isTechsUseAdministration() {
        return get(CampaignOption.TECHS_USE_ADMINISTRATION);
    }

    /**
     * Sets whether administrative adjustments should be applied to technician time calculations.
     *
     * <p>Enabling this setting applies administrative multipliers to modify technicians' daily available time
     * in relevant calculations.</p>
     *
     * @param techsUseAdministration {@code true} to enable administrative adjustments for technicians, {@code false} to
     *                               disable them.
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.TECHS_USE_ADMINISTRATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTechsUseAdministration(final boolean techsUseAdministration) {
        set(CampaignOption.TECHS_USE_ADMINISTRATION, techsUseAdministration);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_USEFUL_AS_TECHS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseUsefulAsTechs() {
        return get(CampaignOption.USE_USEFUL_AS_TECHS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_USEFUL_AS_TECHS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setIsUseUsefulAsTechs(final boolean useUsefulAsTechs) {
        set(CampaignOption.USE_USEFUL_AS_TECHS, useUsefulAsTechs);
    }

    /**
     * @return true to use the origin faction for personnel names instead of a set faction
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_ORIGIN_FACTION_FOR_NAMES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseOriginFactionForNames() {
        return get(CampaignOption.USE_ORIGIN_FACTION_FOR_NAMES);
    }

    /**
     * @param useOriginFactionForNames whether to use personnel names or a set faction
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_ORIGIN_FACTION_FOR_NAMES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseOriginFactionForNames(final boolean useOriginFactionForNames) {
        set(CampaignOption.USE_ORIGIN_FACTION_FOR_NAMES, useOriginFactionForNames);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_QUIRKS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseQuirks() {
        return get(CampaignOption.USE_QUIRKS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_QUIRKS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setQuirks(final boolean useQuirks) {
        set(CampaignOption.USE_QUIRKS, useQuirks);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.XP_COST_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getXpCostMultiplier() {
        return get(CampaignOption.XP_COST_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.XP_COST_MULTIPLIER,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setXpCostMultiplier(final double xpCostMultiplier) {
        set(CampaignOption.XP_COST_MULTIPLIER, xpCostMultiplier);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SCENARIO_XP)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getScenarioXP() {
        return get(CampaignOption.SCENARIO_XP);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.SCENARIO_XP, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setScenarioXP(final int scenarioXP) {
        set(CampaignOption.SCENARIO_XP, scenarioXP);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.KILLS_FOR_XP)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getKillsForXP() {
        return get(CampaignOption.KILLS_FOR_XP);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.KILLS_FOR_XP,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setKillsForXP(final int killsForXP) {
        set(CampaignOption.KILLS_FOR_XP, killsForXP);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.KILL_XP_AWARD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getKillXPAward() {
        return get(CampaignOption.KILL_XP_AWARD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.KILL_XP_AWARD,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setKillXPAward(final int killXPAward) {
        set(CampaignOption.KILL_XP_AWARD, killXPAward);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.N_TASKS_XP)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getNTasksXP() {
        return get(CampaignOption.N_TASKS_XP);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.N_TASKS_XP, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setNTasksXP(final int nTasksXP) {
        set(CampaignOption.N_TASKS_XP, nTasksXP);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.TASKS_XP)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getTaskXP() {
        return get(CampaignOption.TASKS_XP);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.TASKS_XP, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTaskXP(final int tasksXP) {
        set(CampaignOption.TASKS_XP, tasksXP);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MISTAKE_XP)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMistakeXP() {
        return get(CampaignOption.MISTAKE_XP);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MISTAKE_XP, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMistakeXP(final int mistakeXP) {
        set(CampaignOption.MISTAKE_XP, mistakeXP);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SUCCESS_XP)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getSuccessXP() {
        return get(CampaignOption.SUCCESS_XP);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.SUCCESS_XP, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setSuccessXP(final int successXP) {
        set(CampaignOption.SUCCESS_XP, successXP);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.LIMIT_BY_YEAR)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isLimitByYear() {
        return get(CampaignOption.LIMIT_BY_YEAR);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.LIMIT_BY_YEAR,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setLimitByYear(final boolean limitByYear) {
        set(CampaignOption.LIMIT_BY_YEAR, limitByYear);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DISALLOW_EXTINCT_STUFF)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDisallowExtinctStuff() {
        return get(CampaignOption.DISALLOW_EXTINCT_STUFF);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.DISALLOW_EXTINCT_STUFF, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisallowExtinctStuff(final boolean disallowExtinctStuff) {
        set(CampaignOption.DISALLOW_EXTINCT_STUFF, disallowExtinctStuff);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ALLOW_CLAN_PURCHASES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAllowClanPurchases() {
        return get(CampaignOption.ALLOW_CLAN_PURCHASES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ALLOW_CLAN_PURCHASES,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAllowClanPurchases(final boolean allowClanPurchases) {
        set(CampaignOption.ALLOW_CLAN_PURCHASES, allowClanPurchases);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ALLOW_IS_PURCHASES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAllowISPurchases() {
        return get(CampaignOption.ALLOW_IS_PURCHASES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ALLOW_IS_PURCHASES,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAllowISPurchases(final boolean allowISPurchases) {
        set(CampaignOption.ALLOW_IS_PURCHASES, allowISPurchases);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ALLOW_CANON_ONLY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAllowCanonOnly() {
        return get(CampaignOption.ALLOW_CANON_ONLY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ALLOW_CANON_ONLY,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAllowCanonOnly(final boolean allowCanonOnly) {
        set(CampaignOption.ALLOW_CANON_ONLY, allowCanonOnly);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ALLOW_CANON_REFIT_ONLY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAllowCanonRefitOnly() {
        return get(CampaignOption.ALLOW_CANON_REFIT_ONLY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ALLOW_CANON_REFIT_ONLY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAllowCanonRefitOnly(final boolean allowCanonRefitOnly) {
        set(CampaignOption.ALLOW_CANON_REFIT_ONLY, allowCanonRefitOnly);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.VARIABLE_TECH_LEVEL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isVariableTechLevel() {
        return get(CampaignOption.VARIABLE_TECH_LEVEL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.VARIABLE_TECH_LEVEL,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setVariableTechLevel(final boolean variableTechLevel) {
        set(CampaignOption.VARIABLE_TECH_LEVEL, variableTechLevel);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.FACTION_INTRO_DATE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isFactionIntroDate() {
        return get(CampaignOption.FACTION_INTRO_DATE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.FACTION_INTRO_DATE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setIsUseFactionIntroDate(final boolean factionIntroDate) {
        set(CampaignOption.FACTION_INTRO_DATE, factionIntroDate);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_AMMO_BY_TYPE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseAmmoByType() {
        return get(CampaignOption.USE_AMMO_BY_TYPE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_AMMO_BY_TYPE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseAmmoByType(final boolean useAmmoByType) {
        set(CampaignOption.USE_AMMO_BY_TYPE, useAmmoByType);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.TECH_LEVEL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getTechLevel() {
        return get(CampaignOption.TECH_LEVEL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.TECH_LEVEL, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTechLevel(final int techLevel) {
        set(CampaignOption.TECH_LEVEL, techLevel);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PHENOTYPE_PROBABILITIES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int[] getPhenotypeProbabilities() {
        return get(CampaignOption.PHENOTYPE_PROBABILITIES);
    }

    public int getPhenotypeProbability(final Phenotype phenotype) {
        return getPhenotypeProbabilities()[phenotype.ordinal()];
    }

    public void setPhenotypeProbability(final int index, final int percentage) {
        getPhenotypeProbabilities()[index] = percentage;
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_PORTRAIT_FOR_ROLE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean[] isUsePortraitForRoles() {
        return get(CampaignOption.USE_PORTRAIT_FOR_ROLE);
    }

    public boolean isUsePortraitForRole(final PersonnelRole role) {
        return isUsePortraitForRoles()[role.ordinal()];
    }

    public void setUsePortraitForRole(final int index, final boolean use) {
        isUsePortraitForRoles()[index] = use;
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ASSIGN_PORTRAIT_ON_ROLE_CHANGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAssignPortraitOnRoleChange() {
        return get(CampaignOption.ASSIGN_PORTRAIT_ON_ROLE_CHANGE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ASSIGN_PORTRAIT_ON_ROLE_CHANGE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAssignPortraitOnRoleChange(final boolean assignPortraitOnRoleChange) {
        set(CampaignOption.ASSIGN_PORTRAIT_ON_ROLE_CHANGE, assignPortraitOnRoleChange);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ALLOW_DUPLICATE_PORTRAITS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAllowDuplicatePortraits() {
        return get(CampaignOption.ALLOW_DUPLICATE_PORTRAITS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ALLOW_DUPLICATE_PORTRAITS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAllowDuplicatePortraits(final boolean allowDuplicatePortraits) {
        set(CampaignOption.ALLOW_DUPLICATE_PORTRAITS, allowDuplicatePortraits);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_GENDERED_PORTRAITS_ONLY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseGenderedPortraitsOnly() {
        return get(CampaignOption.USE_GENDERED_PORTRAITS_ONLY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_GENDERED_PORTRAITS_ONLY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseGenderedPortraitsOnly(final boolean useGenderedPortraitsOnly) {
        set(CampaignOption.USE_GENDERED_PORTRAITS_ONLY, useGenderedPortraitsOnly);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.NO_RANDOM_PORTRAITS_FOR_CHILDREN)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isNoRandomPortraitsForChildren() {
        return get(CampaignOption.NO_RANDOM_PORTRAITS_FOR_CHILDREN);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.NO_RANDOM_PORTRAITS_FOR_CHILDREN, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setNoRandomPortraitsForChildren(final boolean noRandomPortraitsForChildren) {
        set(CampaignOption.NO_RANDOM_PORTRAITS_FOR_CHILDREN, noRandomPortraitsForChildren);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.CHILD_PORTRAITS_WHEN_COMING_OF_AGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isChildPortraitsWhenComingOfAge() {
        return get(CampaignOption.CHILD_PORTRAITS_WHEN_COMING_OF_AGE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CHILD_PORTRAITS_WHEN_COMING_OF_AGE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setChildPortraitsWhenComingOfAge(final boolean childPortraitsWhenComingOfAge) {
        set(CampaignOption.CHILD_PORTRAITS_WHEN_COMING_OF_AGE, childPortraitsWhenComingOfAge);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.VOCATIONAL_XP)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getVocationalXP() {
        return get(CampaignOption.VOCATIONAL_XP);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.VOCATIONAL_XP,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setVocationalXP(final int vocationalXP) {
        set(CampaignOption.VOCATIONAL_XP, vocationalXP);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.VOCATIONAL_XP_TARGET_NUMBER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getVocationalXPTargetNumber() {
        return get(CampaignOption.VOCATIONAL_XP_TARGET_NUMBER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.VOCATIONAL_XP_TARGET_NUMBER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setVocationalXPTargetNumber(final int vocationalXPTargetNumber) {
        set(CampaignOption.VOCATIONAL_XP_TARGET_NUMBER, vocationalXPTargetNumber);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.VOCATIONAL_XP_CHECK_FREQUENCY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getVocationalXPCheckFrequency() {
        return get(CampaignOption.VOCATIONAL_XP_CHECK_FREQUENCY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.VOCATIONAL_XP_CHECK_FREQUENCY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setVocationalXPCheckFrequency(final int vocationalXPCheckFrequency) {
        set(CampaignOption.VOCATIONAL_XP_CHECK_FREQUENCY, vocationalXPCheckFrequency);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.CONTRACT_NEGOTIATION_XP)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getContractNegotiationXP() {
        return get(CampaignOption.CONTRACT_NEGOTIATION_XP);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CONTRACT_NEGOTIATION_XP, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setContractNegotiationXP(final int contractNegotiationXP) {
        set(CampaignOption.CONTRACT_NEGOTIATION_XP, contractNegotiationXP);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ADMIN_XP)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAdminXP() {
        return get(CampaignOption.ADMIN_XP);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ADMIN_XP, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAdminXP(final int adminXP) {
        set(CampaignOption.ADMIN_XP, adminXP);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ADMIN_XP_PERIOD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAdminXPPeriod() {
        return get(CampaignOption.ADMIN_XP_PERIOD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ADMIN_XP_PERIOD,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAdminXPPeriod(final int adminXPPeriod) {
        set(CampaignOption.ADMIN_XP_PERIOD, adminXPPeriod);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MISSION_XP_FAIL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMissionXpFail() {
        return get(CampaignOption.MISSION_XP_FAIL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MISSION_XP_FAIL,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMissionXpFail(final int missionXpFail) {
        set(CampaignOption.MISSION_XP_FAIL, missionXpFail);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MISSION_XP_SUCCESS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMissionXpSuccess() {
        return get(CampaignOption.MISSION_XP_SUCCESS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MISSION_XP_SUCCESS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMissionXpSuccess(final int missionXpSuccess) {
        set(CampaignOption.MISSION_XP_SUCCESS, missionXpSuccess);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.MISSION_XP_OUTSTANDING_SUCCESS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMissionXpOutstandingSuccess() {
        return get(CampaignOption.MISSION_XP_OUTSTANDING_SUCCESS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MISSION_XP_OUTSTANDING_SUCCESS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMissionXpOutstandingSuccess(final int missionXpOutstandingSuccess) {
        set(CampaignOption.MISSION_XP_OUTSTANDING_SUCCESS, missionXpOutstandingSuccess);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.EDGE_COST)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getEdgeCost() {
        return get(CampaignOption.EDGE_COST);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.EDGE_COST, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEdgeCost(final int edgeCost) {
        set(CampaignOption.EDGE_COST, edgeCost);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.EDGE_REFRESH_COST)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getEdgeRefreshCost() {
        return get(CampaignOption.EDGE_REFRESH_COST);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.EDGE_REFRESH_COST,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEdgeRefreshCost(final int edgeRefreshCost) {
        set(CampaignOption.EDGE_REFRESH_COST, edgeRefreshCost);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ATTRIBUTE_COST)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAttributeCost() {
        return get(CampaignOption.ATTRIBUTE_COST);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ATTRIBUTE_COST,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAttributeCost(final int attributeCost) {
        set(CampaignOption.ATTRIBUTE_COST, attributeCost);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.WAITING_PERIOD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getWaitingPeriod() {
        return get(CampaignOption.WAITING_PERIOD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.WAITING_PERIOD,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setWaitingPeriod(final int acquisitionSkill) {
        set(CampaignOption.WAITING_PERIOD, acquisitionSkill);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ACQUISITIONS_TYPE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public AcquisitionsType getAcquisitionType() {
        return get(CampaignOption.ACQUISITIONS_TYPE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.ACQUISITIONS_TYPE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAcquisitionType(final AcquisitionsType acquisitionsType) {
        set(CampaignOption.ACQUISITIONS_TYPE, acquisitionsType);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_FUNCTIONAL_APPRAISAL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFunctionalAppraisal() {
        return get(CampaignOption.USE_FUNCTIONAL_APPRAISAL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FUNCTIONAL_APPRAISAL, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFunctionalAppraisal(final boolean useFunctionalAppraisal) {
        set(CampaignOption.USE_FUNCTIONAL_APPRAISAL, useFunctionalAppraisal);
    }

    /**
     * Checks if the acquisition personnel category matches a specified category.
     *
     * @param category The {@link ProcurementPersonnelPick} category to check against.
     *
     * @return {@code true} if the current acquisition personnel category matches the specified category, {@code false}
     *       otherwise.
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isAcquisitionPersonnelCategory(ProcurementPersonnelPick category) {
        return get(CampaignOption.ACQUISITION_PERSONNEL_CATEGORY) == category;
    }

    /**
     * Retrieves the current acquisition personnel category.
     *
     * <p>This method returns the {@link ProcurementPersonnelPick} value assigned to indicate what
     * personnel category can make acquisition checks.</p>
     *
     * <p><b>Usage:</b> Generally, for most use-cases, you'll want to use the shortcut method
     * {@link #isAcquisitionPersonnelCategory(ProcurementPersonnelPick)} instead.</p>
     *
     * @return The current {@link ProcurementPersonnelPick} that represents the acquisition's personnel category.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ACQUISITION_PERSONNEL_CATEGORY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public ProcurementPersonnelPick getAcquisitionPersonnelCategory() {
        return get(CampaignOption.ACQUISITION_PERSONNEL_CATEGORY);
    }

    /**
     * Sets the acquisition personnel category.
     *
     * <p>This method defines what personnel category (represented as a {@link ProcurementPersonnelPick})
     * is eligible to make acquisition checks in the campaign system.</p>
     *
     * @param acquisitionPersonnelCategory The {@link ProcurementPersonnelPick} value to assign.
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ACQUISITION_PERSONNEL_CATEGORY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAcquisitionPersonnelCategory(final ProcurementPersonnelPick acquisitionPersonnelCategory) {
        set(CampaignOption.ACQUISITION_PERSONNEL_CATEGORY, acquisitionPersonnelCategory);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.UNIT_TRANSIT_TIME)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getUnitTransitTime() {
        return get(CampaignOption.UNIT_TRANSIT_TIME);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.UNIT_TRANSIT_TIME,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUnitTransitTime(final int unitTransitTime) {
        set(CampaignOption.UNIT_TRANSIT_TIME, unitTransitTime);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.NO_DELIVERIES_IN_TRANSIT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isNoDeliveriesInTransit() {
        return get(CampaignOption.NO_DELIVERIES_IN_TRANSIT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.NO_DELIVERIES_IN_TRANSIT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setNoDeliveriesInTransit(final boolean noDeliveriesInTransit) {
        set(CampaignOption.NO_DELIVERIES_IN_TRANSIT, noDeliveriesInTransit);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_PLANETARY_ACQUISITION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUsePlanetaryAcquisition() {
        return get(CampaignOption.USE_PLANETARY_ACQUISITION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_PLANETARY_ACQUISITION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPlanetaryAcquisition(final boolean usePlanetaryAcquisition) {
        set(CampaignOption.USE_PLANETARY_ACQUISITION, usePlanetaryAcquisition);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.PLANET_ACQUISITION_FACTION_LIMIT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public PlanetaryAcquisitionFactionLimit getPlanetAcquisitionFactionLimit() {
        return get(CampaignOption.PLANET_ACQUISITION_FACTION_LIMIT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PLANET_ACQUISITION_FACTION_LIMIT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPlanetAcquisitionFactionLimit(final PlanetaryAcquisitionFactionLimit planetAcquisitionFactionLimit) {
        set(CampaignOption.PLANET_ACQUISITION_FACTION_LIMIT, planetAcquisitionFactionLimit);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.PLANET_ACQUISITION_NO_CLAN_CROSSOVER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPlanetAcquisitionNoClanCrossover() {
        return get(CampaignOption.PLANET_ACQUISITION_NO_CLAN_CROSSOVER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PLANET_ACQUISITION_NO_CLAN_CROSSOVER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisallowPlanetAcquisitionClanCrossover(final boolean planetAcquisitionNoClanCrossover) {
        set(CampaignOption.PLANET_ACQUISITION_NO_CLAN_CROSSOVER, planetAcquisitionNoClanCrossover);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.MAX_JUMPS_PLANETARY_ACQUISITION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMaxJumpsPlanetaryAcquisition() {
        return get(CampaignOption.MAX_JUMPS_PLANETARY_ACQUISITION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MAX_JUMPS_PLANETARY_ACQUISITION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMaxJumpsPlanetaryAcquisition(final int maxJumpsPlanetaryAcquisition) {
        set(CampaignOption.MAX_JUMPS_PLANETARY_ACQUISITION, maxJumpsPlanetaryAcquisition);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PENALTY_CLAN_PARTS_FROM_IS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getPenaltyClanPartsFromIS() {
        return get(CampaignOption.PENALTY_CLAN_PARTS_FROM_IS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PENALTY_CLAN_PARTS_FROM_IS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPenaltyClanPartsFromIS(final int penaltyClanPartsFromIS) {
        set(CampaignOption.PENALTY_CLAN_PARTS_FROM_IS, penaltyClanPartsFromIS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.NO_CLAN_PARTS_FROM_IS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isNoClanPartsFromIS() {
        return get(CampaignOption.NO_CLAN_PARTS_FROM_IS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.NO_CLAN_PARTS_FROM_IS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDisallowClanPartsFromIS(final boolean noClanPartsFromIS) {
        set(CampaignOption.NO_CLAN_PARTS_FROM_IS, noClanPartsFromIS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.PLANET_ACQUISITION_VERBOSE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPlanetAcquisitionVerbose() {
        return get(CampaignOption.PLANET_ACQUISITION_VERBOSE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PLANET_ACQUISITION_VERBOSE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPlanetAcquisitionVerboseReporting(final boolean planetAcquisitionVerbose) {
        set(CampaignOption.PLANET_ACQUISITION_VERBOSE, planetAcquisitionVerbose);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.EQUIPMENT_CONTRACT_PERCENT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getEquipmentContractPercent() {
        return get(CampaignOption.EQUIPMENT_CONTRACT_PERCENT);
    }

    public void setEquipmentContractPercent(final double equipmentContractPercent) {
        set(CampaignOption.EQUIPMENT_CONTRACT_PERCENT,
              Math.min(equipmentContractPercent, MAXIMUM_COMBAT_EQUIPMENT_PERCENT));
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_ALTERNATE_PAYMENT_MODE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseAlternatePaymentMode() {
        return get(CampaignOption.USE_ALTERNATE_PAYMENT_MODE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_ALTERNATE_PAYMENT_MODE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseAlternatePaymentMode(final boolean useAlternatePaymentMode) {
        set(CampaignOption.USE_ALTERNATE_PAYMENT_MODE, useAlternatePaymentMode);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_DIMINISHING_CONTRACT_PAY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseDiminishingContractPay() {
        return get(CampaignOption.USE_DIMINISHING_CONTRACT_PAY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_DIMINISHING_CONTRACT_PAY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseDiminishingContractPay(final boolean useDiminishingContractPay) {
        set(CampaignOption.USE_DIMINISHING_CONTRACT_PAY, useDiminishingContractPay);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.EQUIPMENT_CONTRACT_BASE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEquipmentContractBase() {
        return get(CampaignOption.EQUIPMENT_CONTRACT_BASE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.EQUIPMENT_CONTRACT_BASE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEquipmentContractBase(final boolean equipmentContractBase) {
        set(CampaignOption.EQUIPMENT_CONTRACT_BASE, equipmentContractBase);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.EQUIPMENT_CONTRACT_SALE_VALUE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isEquipmentContractSaleValue() {
        return get(CampaignOption.EQUIPMENT_CONTRACT_SALE_VALUE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.EQUIPMENT_CONTRACT_SALE_VALUE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEquipmentContractSaleValue(final boolean equipmentContractSaleValue) {
        set(CampaignOption.EQUIPMENT_CONTRACT_SALE_VALUE, equipmentContractSaleValue);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DROP_SHIP_CONTRACT_PERCENT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getDropShipContractPercent() {
        return get(CampaignOption.DROP_SHIP_CONTRACT_PERCENT);
    }

    public void setDropShipContractPercent(final double dropShipContractPercent) {
        set(CampaignOption.DROP_SHIP_CONTRACT_PERCENT,
              Math.min(dropShipContractPercent, MAXIMUM_DROPSHIP_EQUIPMENT_PERCENT));
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.JUMP_SHIP_CONTRACT_PERCENT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getJumpShipContractPercent() {
        return get(CampaignOption.JUMP_SHIP_CONTRACT_PERCENT);
    }

    public void setJumpShipContractPercent(final double jumpShipContractPercent) {
        set(CampaignOption.JUMP_SHIP_CONTRACT_PERCENT,
              Math.min(jumpShipContractPercent, MAXIMUM_JUMPSHIP_EQUIPMENT_PERCENT));
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.WAR_SHIP_CONTRACT_PERCENT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getWarShipContractPercent() {
        return get(CampaignOption.WAR_SHIP_CONTRACT_PERCENT);
    }

    public void setWarShipContractPercent(final double warShipContractPercent) {
        set(CampaignOption.WAR_SHIP_CONTRACT_PERCENT,
              Math.min(warShipContractPercent, MAXIMUM_WARSHIP_EQUIPMENT_PERCENT));
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.BLC_SALE_VALUE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isBLCSaleValue() {
        return get(CampaignOption.BLC_SALE_VALUE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.BLC_SALE_VALUE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setBLCSaleValue(final boolean blcSaleValue) {
        set(CampaignOption.BLC_SALE_VALUE, blcSaleValue);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.OVERAGE_REPAYMENT_IN_FINAL_PAYMENT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isOverageRepaymentInFinalPayment() {
        return get(CampaignOption.OVERAGE_REPAYMENT_IN_FINAL_PAYMENT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.OVERAGE_REPAYMENT_IN_FINAL_PAYMENT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setOverageRepaymentInFinalPayment(final boolean overageRepaymentInFinalPayment) {
        set(CampaignOption.OVERAGE_REPAYMENT_IN_FINAL_PAYMENT, overageRepaymentInFinalPayment);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.CLAN_ACQUISITION_PENALTY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getClanAcquisitionPenalty() {
        return get(CampaignOption.CLAN_ACQUISITION_PENALTY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CLAN_ACQUISITION_PENALTY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setClanAcquisitionPenalty(final int clanAcquisitionPenalty) {
        set(CampaignOption.CLAN_ACQUISITION_PENALTY, clanAcquisitionPenalty);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.IS_ACQUISITION_PENALTY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getIsAcquisitionPenalty() {
        return get(CampaignOption.IS_ACQUISITION_PENALTY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.IS_ACQUISITION_PENALTY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setIsAcquisitionPenalty(final int isAcquisitionPenalty) {
        set(CampaignOption.IS_ACQUISITION_PENALTY, isAcquisitionPenalty);
    }

    public int getPlanetTechAcquisitionBonus(final PlanetarySophistication sophistication) {
        return getAllPlanetTechAcquisitionBonuses().getOrDefault(sophistication, 0);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.PLANET_TECH_ACQUISITION_BONUS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public EnumMap<PlanetarySophistication, Integer> getAllPlanetTechAcquisitionBonuses() {
        return get(CampaignOption.PLANET_TECH_ACQUISITION_BONUS);
    }

    public void setPlanetTechAcquisitionBonus(final int base, final PlanetarySophistication sophistication) {
        getAllPlanetTechAcquisitionBonuses().put(sophistication, base);
    }

    public int getPlanetIndustryAcquisitionBonus(final PlanetaryRating rating) {
        return getAllPlanetIndustryAcquisitionBonuses().getOrDefault(rating, 0);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.PLANET_INDUSTRY_ACQUISITION_BONUS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public EnumMap<PlanetaryRating, Integer> getAllPlanetIndustryAcquisitionBonuses() {
        return get(CampaignOption.PLANET_INDUSTRY_ACQUISITION_BONUS);
    }

    public void setPlanetIndustryAcquisitionBonus(final int base, final PlanetaryRating rating) {
        getAllPlanetIndustryAcquisitionBonuses().put(rating, base);
    }

    public int getPlanetOutputAcquisitionBonus(final PlanetaryRating rating) {
        return getAllPlanetOutputAcquisitionBonuses().getOrDefault(rating, 0);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.PLANET_OUTPUT_ACQUISITION_BONUS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public EnumMap<PlanetaryRating, Integer> getAllPlanetOutputAcquisitionBonuses() {
        return get(CampaignOption.PLANET_OUTPUT_ACQUISITION_BONUS);
    }

    public void setPlanetOutputAcquisitionBonus(final int base, final PlanetaryRating rating) {
        getAllPlanetOutputAcquisitionBonuses().put(rating, base);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DESTROY_BY_MARGIN)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isDestroyByMargin() {
        return get(CampaignOption.DESTROY_BY_MARGIN);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.DESTROY_BY_MARGIN,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDestroyByMargin(final boolean destroyByMargin) {
        set(CampaignOption.DESTROY_BY_MARGIN, destroyByMargin);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DESTROY_MARGIN)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getDestroyMargin() {
        return get(CampaignOption.DESTROY_MARGIN);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.DESTROY_MARGIN,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDestroyMargin(final int destroyMargin) {
        set(CampaignOption.DESTROY_MARGIN, destroyMargin);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.DESTROY_PART_TARGET)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getDestroyPartTarget() {
        return get(CampaignOption.DESTROY_PART_TARGET);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.DESTROY_PART_TARGET,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setDestroyPartTarget(final int destroyPartTarget) {
        set(CampaignOption.DESTROY_PART_TARGET, destroyPartTarget);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_AERO_SYSTEM_HITS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseAeroSystemHits() {
        return get(CampaignOption.USE_AERO_SYSTEM_HITS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_AERO_SYSTEM_HITS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseAeroSystemHits(final boolean useAeroSystemHits) {
        set(CampaignOption.USE_AERO_SYSTEM_HITS, useAeroSystemHits);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MAX_ACQUISITIONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMaxAcquisitions() {
        return get(CampaignOption.MAX_ACQUISITIONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MAX_ACQUISITIONS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMaxAcquisitions(final int maxAcquisitions) {
        set(CampaignOption.MAX_ACQUISITIONS, maxAcquisitions);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_HEAT_SINK)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsHeatSink() {
        return get(CampaignOption.AUTO_LOGISTICS_HEAT_SINK);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_HEAT_SINK, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsHeatSink(int autoLogisticsHeatSink) {
        set(CampaignOption.AUTO_LOGISTICS_HEAT_SINK, autoLogisticsHeatSink);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_MEK_HEAD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsMekHead() {
        return get(CampaignOption.AUTO_LOGISTICS_MEK_HEAD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_MEK_HEAD, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsMekHead(int autoLogisticsMekHead) {
        set(CampaignOption.AUTO_LOGISTICS_MEK_HEAD, autoLogisticsMekHead);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_MEK_LOCATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsMekLocation() {
        return get(CampaignOption.AUTO_LOGISTICS_MEK_LOCATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_MEK_LOCATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsMekLocation(int autoLogisticsMekLocation) {
        set(CampaignOption.AUTO_LOGISTICS_MEK_LOCATION, autoLogisticsMekLocation);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_NON_REPAIRABLE_LOCATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsNonRepairableLocation() {
        return get(CampaignOption.AUTO_LOGISTICS_NON_REPAIRABLE_LOCATION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_NON_REPAIRABLE_LOCATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsNonRepairableLocation(int autoLogisticsNonRepairableLocation) {
        set(CampaignOption.AUTO_LOGISTICS_NON_REPAIRABLE_LOCATION, autoLogisticsNonRepairableLocation);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_ARMOR)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsArmor() {
        return get(CampaignOption.AUTO_LOGISTICS_ARMOR);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_ARMOR,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsArmor(int autoLogisticsArmor) {
        set(CampaignOption.AUTO_LOGISTICS_ARMOR, autoLogisticsArmor);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_AMMUNITION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsAmmunition() {
        return get(CampaignOption.AUTO_LOGISTICS_AMMUNITION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_AMMUNITION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsAmmunition(int autoLogisticsAmmunition) {
        set(CampaignOption.AUTO_LOGISTICS_AMMUNITION, autoLogisticsAmmunition);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_ACTUATORS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsActuators() {
        return get(CampaignOption.AUTO_LOGISTICS_ACTUATORS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_ACTUATORS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsActuators(int autoLogisticsActuators) {
        set(CampaignOption.AUTO_LOGISTICS_ACTUATORS, autoLogisticsActuators);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_JUMP_JETS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsJumpJets() {
        return get(CampaignOption.AUTO_LOGISTICS_JUMP_JETS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_JUMP_JETS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsJumpJets(int autoLogisticsJumpJets) {
        set(CampaignOption.AUTO_LOGISTICS_JUMP_JETS, autoLogisticsJumpJets);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_ENGINES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsEngines() {
        return get(CampaignOption.AUTO_LOGISTICS_ENGINES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_ENGINES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsEngines(int autoLogisticsEngines) {
        set(CampaignOption.AUTO_LOGISTICS_ENGINES, autoLogisticsEngines);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_WEAPONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsWeapons() {
        return get(CampaignOption.AUTO_LOGISTICS_WEAPONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_WEAPONS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsWeapons(int autoLogisticsWeapons) {
        set(CampaignOption.AUTO_LOGISTICS_WEAPONS, autoLogisticsWeapons);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_GYROS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsGyros() {
        return get(CampaignOption.AUTO_LOGISTICS_GYROS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_GYROS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsGyros(int autoLogisticsGyros) {
        set(CampaignOption.AUTO_LOGISTICS_GYROS, autoLogisticsGyros);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_HEAD_COMPONENTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsHeadComponents() {
        return get(CampaignOption.AUTO_LOGISTICS_HEAD_COMPONENTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_HEAD_COMPONENTS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsHeadComponents(int autoLogisticsHeadComponents) {
        set(CampaignOption.AUTO_LOGISTICS_HEAD_COMPONENTS, autoLogisticsHeadComponents);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AUTO_LOGISTICS_OTHER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoLogisticsOther() {
        return get(CampaignOption.AUTO_LOGISTICS_OTHER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.AUTO_LOGISTICS_OTHER,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoLogisticsOther(int autoLogisticsOther) {
        set(CampaignOption.AUTO_LOGISTICS_OTHER, autoLogisticsOther);
    }

    public boolean isUseStratCon() {
        return getStratConPlayType() != StratConPlayType.DISABLED;
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.STRAT_CON_PLAY_TYPE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public StratConPlayType getStratConPlayType() {
        return get(CampaignOption.STRAT_CON_PLAY_TYPE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.STRAT_CON_PLAY_TYPE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setStratConPlayType(final StratConPlayType stratConPlayType) {
        set(CampaignOption.STRAT_CON_PLAY_TYPE, stratConPlayType);
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isHadAtBEnabledMarker() {
        return get(CampaignOption.HAD_AT_B_ENABLED_MARKER);
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public void setHadAtBEnabledMarker(boolean hadAtBEnabledMarker) {
        set(CampaignOption.HAD_AT_B_ENABLED_MARKER, hadAtBEnabledMarker);
    }

    public boolean isUseStratConMaplessMode() {
        return getStratConPlayType() == StratConPlayType.MAPLESS ||
                     // Singles is a type of mapless mode, so all rules that apply to Mapless also apply to Singles
                     getStratConPlayType() == StratConPlayType.SINGLES;
    }

    public boolean isUseStratConSinglesMode() {
        return getStratConPlayType() == StratConPlayType.SINGLES;
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_ADVANCED_SCOUTING)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseAdvancedScouting() {
        return get(CampaignOption.USE_ADVANCED_SCOUTING);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_ADVANCED_SCOUTING, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseAdvancedScouting(final boolean useAdvancedScouting) {
        set(CampaignOption.USE_ADVANCED_SCOUTING, useAdvancedScouting);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.NO_SEED_FORCES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isNoSeedForces() {
        return get(CampaignOption.NO_SEED_FORCES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.NO_SEED_FORCES,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setNoSeedForces(final boolean noSeedForces) {
        set(CampaignOption.NO_SEED_FORCES, noSeedForces);
    }

    /**
     * Returns whether Generic BV is being used.
     *
     * @return {@code true} if Generic BV is enabled, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_GENERIC_BATTLE_VALUE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseGenericBattleValue() {
        return get(CampaignOption.USE_GENERIC_BATTLE_VALUE);
    }

    /**
     * Sets the flag indicating whether BV Balanced bot forces should use Generic BV.
     *
     * @param useGenericBattleValue flag indicating whether to use Generic BV
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_GENERIC_BATTLE_VALUE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseGenericBattleValue(final boolean useGenericBattleValue) {
        set(CampaignOption.USE_GENERIC_BATTLE_VALUE, useGenericBattleValue);
    }

    /**
     * Returns whether the verbose bidding mode is enabled.
     *
     * @return {@code true} if verbose bidding is enabled, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_VERBOSE_BIDDING)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseVerboseBidding() {
        return get(CampaignOption.USE_VERBOSE_BIDDING);
    }

    /**
     * Sets the flag indicating whether verbose bidding should be used.
     *
     * @param useVerboseBidding flag indicating whether to use verbose bidding
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_VERBOSE_BIDDING,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseVerboseBidding(final boolean useVerboseBidding) {
        set(CampaignOption.USE_VERBOSE_BIDDING, useVerboseBidding);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.OP_FOR_LANCE_TYPE_MEKS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getOpForLanceTypeMeks() {
        return get(CampaignOption.OP_FOR_LANCE_TYPE_MEKS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.OP_FOR_LANCE_TYPE_MEKS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setOpForLanceTypeMeks(final int opForLanceTypeMeks) {
        set(CampaignOption.OP_FOR_LANCE_TYPE_MEKS, opForLanceTypeMeks);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.OP_FOR_LANCE_TYPE_MIXED)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getOpForLanceTypeMixed() {
        return get(CampaignOption.OP_FOR_LANCE_TYPE_MIXED);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.OP_FOR_LANCE_TYPE_MIXED, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setOpForLanceTypeMixed(final int opForLanceTypeMixed) {
        set(CampaignOption.OP_FOR_LANCE_TYPE_MIXED, opForLanceTypeMixed);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.OP_FOR_LANCE_TYPE_VEHICLES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getOpForLanceTypeVehicles() {
        return get(CampaignOption.OP_FOR_LANCE_TYPE_VEHICLES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.OP_FOR_LANCE_TYPE_VEHICLES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setOpForLanceTypeVehicles(final int opForLanceTypeVehicles) {
        set(CampaignOption.OP_FOR_LANCE_TYPE_VEHICLES, opForLanceTypeVehicles);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_DROP_SHIPS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseDropShips() {
        return get(CampaignOption.USE_DROP_SHIPS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_DROP_SHIPS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseDropShips(final boolean useDropShips) {
        set(CampaignOption.USE_DROP_SHIPS, useDropShips);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SKILL_LEVEL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public SkillLevel getSkillLevel() {
        return get(CampaignOption.SKILL_LEVEL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.SKILL_LEVEL, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setSkillLevel(final SkillLevel skillLevel) {
        set(CampaignOption.SKILL_LEVEL, skillLevel);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.BOARD_SCALING_TYPE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public BoardScalingType getBoardScalingType() {
        return get(CampaignOption.BOARD_SCALING_TYPE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.BOARD_SCALING_TYPE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setBoardScalingType(final BoardScalingType boardScalingType) {
        set(CampaignOption.BOARD_SCALING_TYPE, boardScalingType);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AERO_RECRUITS_HAVE_UNITS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAeroRecruitsHaveUnits() {
        return get(CampaignOption.AERO_RECRUITS_HAVE_UNITS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AERO_RECRUITS_HAVE_UNITS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAeroRecruitsHaveUnits(final boolean aeroRecruitsHaveUnits) {
        set(CampaignOption.AERO_RECRUITS_HAVE_UNITS, aeroRecruitsHaveUnits);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_SHARE_SYSTEM)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseShareSystem() {
        return get(CampaignOption.USE_SHARE_SYSTEM);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_SHARE_SYSTEM,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseShareSystem(final boolean useShareSystem) {
        set(CampaignOption.USE_SHARE_SYSTEM, useShareSystem);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SHARES_FOR_ALL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isSharesForAll() {
        return get(CampaignOption.SHARES_FOR_ALL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.SHARES_FOR_ALL,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setSharesForAll(final boolean sharesForAll) {
        set(CampaignOption.SHARES_FOR_ALL, sharesForAll);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.TRACK_ORIGINAL_UNIT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isTrackOriginalUnit() {
        return get(CampaignOption.TRACK_ORIGINAL_UNIT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.TRACK_ORIGINAL_UNIT,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTrackOriginalUnit(final boolean trackOriginalUnit) {
        set(CampaignOption.TRACK_ORIGINAL_UNIT, trackOriginalUnit);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MORALE_VICTORY_EFFECT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMoraleVictoryEffect() {
        return get(CampaignOption.MORALE_VICTORY_EFFECT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MORALE_VICTORY_EFFECT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMoraleVictoryEffect(final int moraleVictoryEffect) {
        set(CampaignOption.MORALE_VICTORY_EFFECT, moraleVictoryEffect);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.MORALE_DECISIVE_VICTORY_EFFECT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMoraleDecisiveVictoryEffect() {
        return get(CampaignOption.MORALE_DECISIVE_VICTORY_EFFECT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MORALE_DECISIVE_VICTORY_EFFECT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMoraleDecisiveVictoryEffect(final int moraleDecisiveVictoryEffect) {
        set(CampaignOption.MORALE_DECISIVE_VICTORY_EFFECT, moraleDecisiveVictoryEffect);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.MORALE_DEFEAT_EFFECT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMoraleDefeatEffect() {
        return get(CampaignOption.MORALE_DEFEAT_EFFECT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.MORALE_DEFEAT_EFFECT,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMoraleDefeatEffect(final int moraleDefeatEffect) {
        set(CampaignOption.MORALE_DEFEAT_EFFECT, moraleDefeatEffect);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.MORALE_DECISIVE_DEFEAT_EFFECT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getMoraleDecisiveDefeatEffect() {
        return get(CampaignOption.MORALE_DECISIVE_DEFEAT_EFFECT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MORALE_DECISIVE_DEFEAT_EFFECT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMoraleDecisiveDefeatEffect(final int moraleDecisiveDefeatEffect) {
        set(CampaignOption.MORALE_DECISIVE_DEFEAT_EFFECT, moraleDecisiveDefeatEffect);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.REGIONAL_MEK_VARIATIONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isRegionalMekVariations() {
        return get(CampaignOption.REGIONAL_MEK_VARIATIONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.REGIONAL_MEK_VARIATIONS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRegionalMekVariations(final boolean regionalMekVariations) {
        set(CampaignOption.REGIONAL_MEK_VARIATIONS, regionalMekVariations);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ATTACHED_PLAYER_CAMOUFLAGE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAttachedPlayerCamouflage() {
        return get(CampaignOption.ATTACHED_PLAYER_CAMOUFLAGE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ATTACHED_PLAYER_CAMOUFLAGE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAttachedPlayerCamouflage(final boolean attachedPlayerCamouflage) {
        set(CampaignOption.ATTACHED_PLAYER_CAMOUFLAGE, attachedPlayerCamouflage);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.PLAYER_CONTROLS_ATTACHED_UNITS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isPlayerControlsAttachedUnits() {
        return get(CampaignOption.PLAYER_CONTROLS_ATTACHED_UNITS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.PLAYER_CONTROLS_ATTACHED_UNITS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setPlayerControlsAttachedUnits(final boolean playerControlsAttachedUnits) {
        set(CampaignOption.PLAYER_CONTROLS_ATTACHED_UNITS, playerControlsAttachedUnits);
    }

    /**
     * Retrieves the chance of having a battle for the specified {@link CombatRole}.
     * <p>
     * This is a convenience method that calls {@link #getAtBBattleChance(CombatRole, boolean)} with
     * {@code useStratConBypass} set to {@code false}. As a result, if StratCon is enabled, the method will return
     * {@code 0} regardless of other conditions.
     * </p>
     *
     * @param role the {@link CombatRole} to evaluate the battle chance for.
     *
     * @return the chance of having a battle for the specified role.
     *
     * @see #getAtBBattleChance(CombatRole, boolean)
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public int getAtBBattleChance(CombatRole role) {
        return getAtBBattleChance(role, false);
    }

    /**
     * Retrieves the chance of having a battle for the specified {@link CombatRole}.
     * <p>
     * This method calculates the battle chance percentage for the provided combat role based on its ordinal position in
     * the {@code atbBattleChance} array. If StratCon is enabled and the {@code useStratConBypass} parameter is set to
     * {@code true}, the method immediately returns {@code 0}.
     * <p>
     * Combat roles marked as {@link CombatRole#RESERVE} or {@link CombatRole#AUXILIARY} are not eligible for battles
     * and also return {@code 0}.
     *
     * @param role              the {@link CombatRole} to evaluate the battle chance for.
     * @param useStratConBypass a {@code boolean} indicating whether to bypass the StratCon-check logic. If
     *                          {@code false}, this allows the method to ignore StratCon-enabled status.
     */
    public int getAtBBattleChance(CombatRole role, boolean useStratConBypass) {
        if (isUseStratCon() && useStratConBypass) {
            return 0;
        }

        if (role.isReserve() || role.isAuxiliary()) {
            return 0;
        }

        return getAllAtBBattleChances()[role.ordinal()];
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.ATB_BATTLE_CHANCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int[] getAllAtBBattleChances() {
        return get(CampaignOption.ATB_BATTLE_CHANCE);
    }

    /**
     * @param role      the {@link CombatRole} ordinal value
     * @param frequency the frequency to set the generation to (percent chance from 0 to 100)
     */
    public void setAtBBattleChance(final int role, final int frequency) {
        getAllAtBBattleChances()[role] = Math.clamp(frequency, 0, 100);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.GENERATE_CHASES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isGenerateChases() {
        return get(CampaignOption.GENERATE_CHASES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.GENERATE_CHASES,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setGenerateChases(final boolean generateChases) {
        set(CampaignOption.GENERATE_CHASES, generateChases);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_WEATHER_CONDITIONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseWeatherConditions() {
        return get(CampaignOption.USE_WEATHER_CONDITIONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_WEATHER_CONDITIONS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseWeatherConditions(final boolean useWeatherConditions) {
        set(CampaignOption.USE_WEATHER_CONDITIONS, useWeatherConditions);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_LIGHT_CONDITIONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseLightConditions() {
        return get(CampaignOption.USE_LIGHT_CONDITIONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_LIGHT_CONDITIONS,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseLightConditions(final boolean useLightConditions) {
        set(CampaignOption.USE_LIGHT_CONDITIONS, useLightConditions);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_PLANETARY_CONDITIONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUsePlanetaryConditions() {
        return get(CampaignOption.USE_PLANETARY_CONDITIONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_PLANETARY_CONDITIONS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUsePlanetaryConditions(final boolean usePlanetaryConditions) {
        set(CampaignOption.USE_PLANETARY_CONDITIONS, usePlanetaryConditions);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.USE_NO_TORNADOES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseNoTornadoes() {
        return get(CampaignOption.USE_NO_TORNADOES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.USE_NO_TORNADOES,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseNoTornadoes(final boolean useNoTornadoes) {
        set(CampaignOption.USE_NO_TORNADOES, useNoTornadoes);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.RESTRICT_PARTS_BY_MISSION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isRestrictPartsByMission() {
        return get(CampaignOption.RESTRICT_PARTS_BY_MISSION);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.RESTRICT_PARTS_BY_MISSION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRestrictPartsByMission(final boolean restrictPartsByMission) {
        set(CampaignOption.RESTRICT_PARTS_BY_MISSION, restrictPartsByMission);
    }

    @Deprecated(since = "0.50.06", forRemoval = true)
    public boolean isLimitLanceWeight() {
        return false;
    }

    public boolean isLimitLanceNumUnits() {
        return false;
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.FIXED_MAP_CHANCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getFixedMapChance() {
        return get(CampaignOption.FIXED_MAP_CHANCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.FIXED_MAP_CHANCE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setFixedMapChance(final int fixedMapChance) {
        set(CampaignOption.FIXED_MAP_CHANCE, fixedMapChance);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_ADVANCED_BUILDING_GUN_EMPLACEMENTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseAdvancedBuildingGunEmplacements() {
        return get(CampaignOption.USE_ADVANCED_BUILDING_GUN_EMPLACEMENTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_ADVANCED_BUILDING_GUN_EMPLACEMENTS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseAdvancedBuildingGunEmplacements(final boolean useAdvancedBuildingGunEmplacements) {
        set(CampaignOption.USE_ADVANCED_BUILDING_GUN_EMPLACEMENTS, useAdvancedBuildingGunEmplacements);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SPA_UPGRADE_INTENSITY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getSpaUpgradeIntensity() {
        return get(CampaignOption.SPA_UPGRADE_INTENSITY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.SPA_UPGRADE_INTENSITY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setSpaUpgradeIntensity(final int spaUpgradeIntensity) {
        set(CampaignOption.SPA_UPGRADE_INTENSITY, spaUpgradeIntensity);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.REINFORCEMENT_BASE_TARGET_NUMBER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getReinforcementBaseTargetNumber() {
        return get(CampaignOption.REINFORCEMENT_BASE_TARGET_NUMBER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.REINFORCEMENT_BASE_TARGET_NUMBER, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setReinforcementBaseTargetNumber(final int reinforcementBaseTargetNumber) {
        set(CampaignOption.REINFORCEMENT_BASE_TARGET_NUMBER, reinforcementBaseTargetNumber);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.CLANS_OBEY_BIDDING_RULES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isClansObeyBiddingRules() {
        return get(CampaignOption.CLANS_OBEY_BIDDING_RULES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.CLANS_OBEY_BIDDING_RULES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setClansObeyBiddingRules(final boolean clansObeyBiddingRules) {
        set(CampaignOption.CLANS_OBEY_BIDDING_RULES, clansObeyBiddingRules);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ALLIED_FACILITY_MODIFIER_DIE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAlliedFacilityModifierDieSize() {
        return get(CampaignOption.ALLIED_FACILITY_MODIFIER_DIE_SIZE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ALLIED_FACILITY_MODIFIER_DIE_SIZE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAlliedFacilityModifierDieSize(final int alliedFacilityModifierDieSize) {
        set(CampaignOption.ALLIED_FACILITY_MODIFIER_DIE_SIZE, alliedFacilityModifierDieSize);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.ENEMY_FACILITY_MODIFIER_DIE_SIZE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getEnemyFacilityModifierDieSize() {
        return get(CampaignOption.ENEMY_FACILITY_MODIFIER_DIE_SIZE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.ENEMY_FACILITY_MODIFIER_DIE_SIZE, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setEnemyFacilityModifierDieSize(final int enemyFacilityModifierDieSize) {
        set(CampaignOption.ENEMY_FACILITY_MODIFIER_DIE_SIZE, enemyFacilityModifierDieSize);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SCENARIO_MOD_MAX)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getScenarioModMax() {
        return get(CampaignOption.SCENARIO_MOD_MAX);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.SCENARIO_MOD_MAX,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setScenarioModMax(final int scenarioModMax) {
        set(CampaignOption.SCENARIO_MOD_MAX, scenarioModMax);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SCENARIO_MOD_CHANCE)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getScenarioModChance() {
        return get(CampaignOption.SCENARIO_MOD_CHANCE);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.SCENARIO_MOD_CHANCE,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setScenarioModChance(final int scenarioModChance) {
        set(CampaignOption.SCENARIO_MOD_CHANCE, scenarioModChance);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.SCENARIO_MOD_BV)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getScenarioModBV() {
        return get(CampaignOption.SCENARIO_MOD_BV);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.SCENARIO_MOD_BV,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setScenarioModBV(final int scenarioModBV) {
        set(CampaignOption.SCENARIO_MOD_BV, scenarioModBV);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AUTO_CONFIG_MUNITIONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAutoConfigMunitions() {
        return get(CampaignOption.AUTO_CONFIG_MUNITIONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_CONFIG_MUNITIONS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoConfigMunitions(final boolean autoConfigMunitions) {
        set(CampaignOption.AUTO_CONFIG_MUNITIONS, autoConfigMunitions);
    }

    // region File IO

    /** Use {@link CampaignOptionsMarshaller#writeCampaignOptionsToXML(CampaignOptions, PrintWriter, int)} instead. */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void writeToXml(final PrintWriter pw, int indent) {
        CampaignOptionsMarshaller.writeCampaignOptionsToXML(this, pw, indent);
    }

    /** Use {@link CampaignOptionsUnmarshaller#generateCampaignOptionsFromXml(Node, Version)} instead */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static CampaignOptions generateCampaignOptionsFromXml(Node parentNod, Version version) {
        return CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(parentNod, version);
    }

    /**
     * This is annoyingly required for the case of anyone having changed the surname weights. The code is not nice, but
     * will nicely handle the cases where anyone has made changes
     *
     * @param values the values to migrate
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void migrateMarriageSurnameWeights(final String... values) {
        int[] weights = new int[values.length];

        for (int i = 0; i < weights.length; i++) {
            try {
                weights[i] = Integer.parseInt(values[i]);
            } catch (Exception ex) {
                LOGGER.error(ex, "Unknown Exception: migrateMarriageSurnameWeights47");
                weights[i] = 0;
            }
        }

        // Now we need to test it to figure out the weights have changed. If not, we
        // will keep the
        // new default values. If they have, we save their changes and add the new
        // surname weights
        if ((weights[0] != getMarriageSurnameWeights().get(MergingSurnameStyle.NO_CHANGE)) ||
                  (weights[1] != getMarriageSurnameWeights().get(MergingSurnameStyle.YOURS) + 5) ||
                  (weights[2] != getMarriageSurnameWeights().get(MergingSurnameStyle.SPOUSE) + 5) ||
                  (weights[3] != getMarriageSurnameWeights().get(MergingSurnameStyle.HYPHEN_SPOUSE) + 5) ||
                  (weights[4] != getMarriageSurnameWeights().get(MergingSurnameStyle.BOTH_HYPHEN_SPOUSE) + 5) ||
                  (weights[5] != getMarriageSurnameWeights().get(MergingSurnameStyle.HYPHEN_YOURS) + 5) ||
                  (weights[6] != getMarriageSurnameWeights().get(MergingSurnameStyle.BOTH_HYPHEN_YOURS) + 5) ||
                  (weights[7] != getMarriageSurnameWeights().get(MergingSurnameStyle.MALE)) ||
                  (weights[8] != getMarriageSurnameWeights().get(MergingSurnameStyle.FEMALE))) {
            getMarriageSurnameWeights().put(MergingSurnameStyle.NO_CHANGE, weights[0]);
            getMarriageSurnameWeights().put(MergingSurnameStyle.YOURS, weights[1]);
            getMarriageSurnameWeights().put(MergingSurnameStyle.SPOUSE, weights[2]);
            // SPACE_YOURS is newly added
            // BOTH_SPACE_YOURS is newly added
            getMarriageSurnameWeights().put(MergingSurnameStyle.HYPHEN_YOURS, weights[3]);
            getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_HYPHEN_YOURS, weights[4]);
            // SPACE_SPOUSE is newly added
            // BOTH_SPACE_SPOUSE is newly added
            getMarriageSurnameWeights().put(MergingSurnameStyle.HYPHEN_SPOUSE, weights[5]);
            getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_HYPHEN_SPOUSE, weights[6]);
            getMarriageSurnameWeights().put(MergingSurnameStyle.MALE, weights[7]);
            getMarriageSurnameWeights().put(MergingSurnameStyle.FEMALE, weights[8]);
        }
    }

    // endregion File IO

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.AUTO_RESOLVE_METHOD)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public AutoResolveMethod getAutoResolveMethod() {
        return get(CampaignOption.AUTO_RESOLVE_METHOD);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.AUTO_RESOLVE_METHOD,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoResolveMethod(final AutoResolveMethod autoResolveMethod) {
        set(CampaignOption.AUTO_RESOLVE_METHOD, autoResolveMethod);
    }

    public File getStrategicViewTheme() {
        CLIENT_PREFERENCES.setStrategicViewTheme(get(CampaignOption.STRATEGIC_VIEW_MINIMAP_THEME));
        return CLIENT_PREFERENCES.getStrategicViewTheme();
    }

    public void setStrategicViewTheme(String minimapStyle) {
        // it is persisted here to have something in the campaign options persisted that
        // will change the GUI preference for the theme
        set(CampaignOption.STRATEGIC_VIEW_MINIMAP_THEME, minimapStyle);
        CLIENT_PREFERENCES.setStrategicViewTheme(minimapStyle);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.AUTO_RESOLVE_VICTORY_CHANCE_ENABLED)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAutoResolveVictoryChanceEnabled() {
        return get(CampaignOption.AUTO_RESOLVE_VICTORY_CHANCE_ENABLED);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_RESOLVE_VICTORY_CHANCE_ENABLED, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoResolveVictoryChanceEnabled(final boolean autoResolveVictoryChanceEnabled) {
        set(CampaignOption.AUTO_RESOLVE_VICTORY_CHANCE_ENABLED, autoResolveVictoryChanceEnabled);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.AUTO_RESOLVE_NUMBER_OF_SCENARIOS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public int getAutoResolveNumberOfScenarios() {
        return get(CampaignOption.AUTO_RESOLVE_NUMBER_OF_SCENARIOS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_RESOLVE_NUMBER_OF_SCENARIOS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoResolveNumberOfScenarios(int autoResolveNumberOfScenarios) {
        set(CampaignOption.AUTO_RESOLVE_NUMBER_OF_SCENARIOS, autoResolveNumberOfScenarios);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.AUTO_RESOLVE_EXPERIMENTAL_PACAR_GUI_ENABLED)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAutoResolveExperimentalPacarGuiEnabled() {
        return get(CampaignOption.AUTO_RESOLVE_EXPERIMENTAL_PACAR_GUI_ENABLED);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_RESOLVE_EXPERIMENTAL_PACAR_GUI_ENABLED, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoResolveExperimentalPacarGuiEnabled(boolean autoResolveExperimentalPacarGuiEnabled) {
        set(CampaignOption.AUTO_RESOLVE_EXPERIMENTAL_PACAR_GUI_ENABLED, autoResolveExperimentalPacarGuiEnabled);
    }

    /**
     * Determines if faction standing negotiation is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingNegotiationSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing negotiation is enabled, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_FACTION_STANDING_NEGOTIATION)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFactionStandingNegotiation() {
        return get(CampaignOption.USE_FACTION_STANDING_NEGOTIATION);
    }

    /**
     * Sets whether the system should use faction standing negotiation.
     *
     * @param useFactionStandingNegotiation a boolean indicating if faction standing negotiation should be enabled
     *                                      (true) or disabled (false)
     *
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FACTION_STANDING_NEGOTIATION, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFactionStandingNegotiation(boolean useFactionStandingNegotiation) {
        set(CampaignOption.USE_FACTION_STANDING_NEGOTIATION, useFactionStandingNegotiation);
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing negotiation is active.
     *
     * @return {@code true} if both faction standing tracking and faction standing negotiation usage are enabled;
     *       {@code false} otherwise.
     */
    public boolean isUseFactionStandingNegotiationSafe() {
        return get(CampaignOption.TRACK_FACTION_STANDING) && get(CampaignOption.USE_FACTION_STANDING_NEGOTIATION);
    }

    /**
     * Determines if faction standing resupply modifiers is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingResupplySafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing resupply modifiers is enabled, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_FACTION_STANDING_RESUPPLY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFactionStandingResupply() {
        return get(CampaignOption.USE_FACTION_STANDING_RESUPPLY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FACTION_STANDING_RESUPPLY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFactionStandingResupply(boolean useFactionStandingResupply) {
        set(CampaignOption.USE_FACTION_STANDING_RESUPPLY, useFactionStandingResupply);
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing resupply modifiers is
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing resupply modifier usage are enabled;
     *       {@code false} otherwise.
     */
    public boolean isUseFactionStandingResupplySafe() {
        return get(CampaignOption.TRACK_FACTION_STANDING) && get(CampaignOption.USE_FACTION_STANDING_RESUPPLY);
    }

    /**
     * Determines if faction standing command circuit access is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingCommandCircuitSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing command circuit access is enabled, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_FACTION_STANDING_COMMAND_CIRCUIT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFactionStandingCommandCircuit() {
        return get(CampaignOption.USE_FACTION_STANDING_COMMAND_CIRCUIT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FACTION_STANDING_COMMAND_CIRCUIT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFactionStandingCommandCircuit(boolean useFactionStandingCommandCircuit) {
        set(CampaignOption.USE_FACTION_STANDING_COMMAND_CIRCUIT, useFactionStandingCommandCircuit);
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing command circuits are
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing command circuit usage are enabled;
     *       {@code false} otherwise.
     */
    public boolean isUseFactionStandingCommandCircuitSafe() {
        return get(CampaignOption.TRACK_FACTION_STANDING) && get(CampaignOption.USE_FACTION_STANDING_COMMAND_CIRCUIT);
    }

    /**
     * Determines if faction standing outlawing is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingOutlawedSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing outlawing is enabled, {@code false} otherwise.
     *
     * @deprecated Use {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_FACTION_STANDING_OUTLAWED)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFactionStandingOutlawed() {
        return get(CampaignOption.USE_FACTION_STANDING_OUTLAWED);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FACTION_STANDING_OUTLAWED, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFactionStandingOutlawed(boolean useFactionStandingOutlawed) {
        set(CampaignOption.USE_FACTION_STANDING_OUTLAWED, useFactionStandingOutlawed);
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing outlawing is active.
     *
     * @return {@code true} if both faction standing tracking and faction standing outlaw usage are enabled;
     *       {@code false} otherwise.
     */
    public boolean isUseFactionStandingOutlawedSafe() {
        return get(CampaignOption.TRACK_FACTION_STANDING) && get(CampaignOption.USE_FACTION_STANDING_OUTLAWED);
    }

    /**
     * Determines if faction standing batchall restriction is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingBatchallRestrictionsSafe()}
     * as that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing batchall restriction is enabled, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_FACTION_STANDING_BATCHALL_RESTRICTIONS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFactionStandingBatchallRestrictions() {
        return get(CampaignOption.USE_FACTION_STANDING_BATCHALL_RESTRICTIONS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FACTION_STANDING_BATCHALL_RESTRICTIONS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFactionStandingBatchallRestrictions(boolean useFactionStandingBatchallRestrictions) {
        set(CampaignOption.USE_FACTION_STANDING_BATCHALL_RESTRICTIONS, useFactionStandingBatchallRestrictions);
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing batchall restrictions are
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing batchall restrictions usage are
     *       enabled; {@code false} otherwise.
     */
    public boolean isUseFactionStandingBatchallRestrictionsSafe() {
        return get(CampaignOption.TRACK_FACTION_STANDING) &&
                     get(CampaignOption.USE_FACTION_STANDING_BATCHALL_RESTRICTIONS);
    }

    /**
     * Determines if faction standing recruitment modifiers is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingRecruitmentSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing recruitment modifiers is enabled, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_FACTION_STANDING_RECRUITMENT)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFactionStandingRecruitment() {
        return get(CampaignOption.USE_FACTION_STANDING_RECRUITMENT);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FACTION_STANDING_RECRUITMENT, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFactionStandingRecruitment(boolean useFactionStandingRecruitment) {
        set(CampaignOption.USE_FACTION_STANDING_RECRUITMENT, useFactionStandingRecruitment);
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing recruitment modifiers is
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing recruitment modifier usage are
     *       enabled; {@code false} otherwise.
     */
    public boolean isUseFactionStandingRecruitmentSafe() {
        return get(CampaignOption.TRACK_FACTION_STANDING) && get(CampaignOption.USE_FACTION_STANDING_RECRUITMENT);
    }

    /**
     * Determines if faction standing barrack costs is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingBarracksCostsSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing barrack costs is enabled, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_FACTION_STANDING_BARRACKS_COSTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFactionStandingBarracksCosts() {
        return get(CampaignOption.USE_FACTION_STANDING_BARRACKS_COSTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FACTION_STANDING_BARRACKS_COSTS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFactionStandingBarracksCosts(boolean useFactionStandingBarracksCosts) {
        set(CampaignOption.USE_FACTION_STANDING_BARRACKS_COSTS, useFactionStandingBarracksCosts);
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing barrack cost modifiers is
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing barrack cost modifier usage are
     *       enabled; {@code false} otherwise.
     */
    public boolean isUseFactionStandingBarracksCostsSafe() {
        return get(CampaignOption.TRACK_FACTION_STANDING) && get(CampaignOption.USE_FACTION_STANDING_BARRACKS_COSTS);
    }

    /**
     * Determines if faction standing unit market modifiers is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingUnitMarketSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing unit market modifiers is enabled, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_FACTION_STANDING_UNIT_MARKET)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFactionStandingUnitMarket() {
        return get(CampaignOption.USE_FACTION_STANDING_UNIT_MARKET);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FACTION_STANDING_UNIT_MARKET, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFactionStandingUnitMarket(boolean useFactionStandingUnitMarket) {
        set(CampaignOption.USE_FACTION_STANDING_UNIT_MARKET, useFactionStandingUnitMarket);
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing unit market modifiers is
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing unit market modifier usage are
     *       enabled; {@code false} otherwise.
     */
    public boolean isUseFactionStandingUnitMarketSafe() {
        return get(CampaignOption.TRACK_FACTION_STANDING) && get(CampaignOption.USE_FACTION_STANDING_UNIT_MARKET);
    }

    /**
     * Determines if faction standing contract pay is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingContractPaySafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing contract pay is enabled, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_FACTION_STANDING_CONTRACT_PAY)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFactionStandingContractPay() {
        return get(CampaignOption.USE_FACTION_STANDING_CONTRACT_PAY);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FACTION_STANDING_CONTRACT_PAY, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFactionStandingContractPay(boolean useFactionStandingContractPay) {
        set(CampaignOption.USE_FACTION_STANDING_CONTRACT_PAY, useFactionStandingContractPay);
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing contract payment modifiers
     * is active.
     *
     * @return {@code true} if both faction standing tracking and faction standing contract pay modifier usage are
     *       enabled; {@code false} otherwise.
     */
    public boolean isUseFactionStandingContractPaySafe() {
        return get(CampaignOption.TRACK_FACTION_STANDING) && get(CampaignOption.USE_FACTION_STANDING_CONTRACT_PAY);
    }

    /**
     * Determines if faction standing support points is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingSupportPointsSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing support points is enabled, {@code false} otherwise.
     *
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.USE_FACTION_STANDING_SUPPORT_POINTS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isUseFactionStandingSupportPoints() {
        return get(CampaignOption.USE_FACTION_STANDING_SUPPORT_POINTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.USE_FACTION_STANDING_SUPPORT_POINTS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setUseFactionStandingSupportPoints(boolean useFactionStandingSupportPoints) {
        set(CampaignOption.USE_FACTION_STANDING_SUPPORT_POINTS, useFactionStandingSupportPoints);
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing support point modifiers is
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing resupply modifier usage are enabled;
     *       {@code false} otherwise.
     */
    public boolean isUseFactionStandingSupportPointsSafe() {
        return get(CampaignOption.TRACK_FACTION_STANDING) && get(CampaignOption.USE_FACTION_STANDING_SUPPORT_POINTS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.TRACK_FACTION_STANDING)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isTrackFactionStanding() {
        return get(CampaignOption.TRACK_FACTION_STANDING);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.TRACK_FACTION_STANDING, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTrackFactionStanding(boolean trackFactionStanding) {
        set(CampaignOption.TRACK_FACTION_STANDING, trackFactionStanding);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.TRACK_CLIMATE_REGARD_CHANGES)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isTrackClimateRegardChanges() {
        return get(CampaignOption.TRACK_CLIMATE_REGARD_CHANGES);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.TRACK_CLIMATE_REGARD_CHANGES, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setTrackClimateRegardChanges(boolean trackClimateRegardChanges) {
        set(CampaignOption.TRACK_CLIMATE_REGARD_CHANGES, trackClimateRegardChanges);
    }

    /**
     * @deprecated Use {@link CampaignOptions#get(CampaignOption) CampaignOptions.get(CampaignOption.REGARD_MULTIPLIER)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public double getRegardMultiplier() {
        return get(CampaignOption.REGARD_MULTIPLIER);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object) CampaignOptions.set(CampaignOption.REGARD_MULTIPLIER,
     *       value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setRegardMultiplier(double regardMultiplier) {
        set(CampaignOption.REGARD_MULTIPLIER, regardMultiplier);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.AUTO_GENERATE_OP_FOR_CALL_SIGNS)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public boolean isAutoGenerateOpForCallSigns() {
        return get(CampaignOption.AUTO_GENERATE_OP_FOR_CALL_SIGNS);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.AUTO_GENERATE_OP_FOR_CALL_SIGNS, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setAutoGenerateOpForCallSigns(boolean autoGenerateOpForCallSigns) {
        set(CampaignOption.AUTO_GENERATE_OP_FOR_CALL_SIGNS, autoGenerateOpForCallSigns);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#get(CampaignOption)
     *       CampaignOptions.get(CampaignOption.MINIMUM_CALLSIGN_SKILL_LEVEL)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public SkillLevel getMinimumCallsignSkillLevel() {
        return get(CampaignOption.MINIMUM_CALLSIGN_SKILL_LEVEL);
    }

    /**
     * @deprecated Use
     *       {@link CampaignOptions#set(CampaignOption, Object)
     *       CampaignOptions.set(CampaignOption.MINIMUM_CALLSIGN_SKILL_LEVEL, value)}
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public void setMinimumCallsignSkillLevel(SkillLevel skillLevel) {
        set(CampaignOption.MINIMUM_CALLSIGN_SKILL_LEVEL, skillLevel);
    }

    /**
     * Updates the campaign options to reflect the current game options settings.
     *
     * <p>
     * This method retrieves the {@link GameOptions} and updates the corresponding campaign-specific settings, such as
     * the use of tactics, initiative bonuses, toughness, artillery, pilot abilities, edge, implants, quirks, canon
     * restrictions, and allowed tech level. This synchronization ensures that the campaign options match the current
     * state of the game options.
     * </p>
     *
     * @param gameOptions the {@link GameOptions} whose values will be used to update the campaign options.
     */
    public void updateCampaignOptionsFromGameOptions(GameOptions gameOptions) {
        set(CampaignOption.USE_TACTICS, gameOptions.getOption(RPG_COMMAND_INIT).booleanValue());
        set(CampaignOption.USE_TOUGHNESS, gameOptions.getOption(RPG_TOUGHNESS).booleanValue());
        set(CampaignOption.USE_ARTILLERY, gameOptions.getOption(RPG_ARTILLERY_SKILL).booleanValue());
        set(CampaignOption.USE_ABILITIES, gameOptions.getOption(RPG_PILOT_ADVANTAGES).booleanValue());
        set(CampaignOption.USE_EDGE, gameOptions.getOption(EDGE).booleanValue());
        set(CampaignOption.USE_IMPLANTS, gameOptions.getOption(RPG_MANEI_DOMINI).booleanValue());
        set(CampaignOption.USE_QUIRKS, gameOptions.getOption(ADVANCED_STRATOPS_QUIRKS).booleanValue());
        set(CampaignOption.ALLOW_CANON_ONLY, gameOptions.getOption(ALLOWED_CANON_ONLY).booleanValue());
        set(CampaignOption.TECH_LEVEL, getSimpleLevel(gameOptions.getOption(ALLOWED_TECH_LEVEL).stringValue()));
    }

    /**
     * Updates the game options to reflect the current campaign options settings.
     *
     * <p>
     * This method synchronizes the values of the given {@link GameOptions} with the current campaign-specific options,
     * such as the use of tactics, initiative bonuses, toughness, artillery, pilot abilities, edge, implants, quirks,
     * canon restrictions, and allowed tech level. These updates ensure parity between the campaign options and the game
     * options.
     * </p>
     *
     * @param gameOptions the {@link GameOptions} to update based on the current campaign options.
     */
    public void updateGameOptionsFromCampaignOptions(GameOptions gameOptions) {
        gameOptions.getOption(RPG_COMMAND_INIT)
              .setValue(get(CampaignOption.USE_TACTICS) || get(CampaignOption.USE_INITIATIVE_BONUS));
        gameOptions.getOption(RPG_TOUGHNESS).setValue(get(CampaignOption.USE_TOUGHNESS));
        gameOptions.getOption(RPG_ARTILLERY_SKILL).setValue(get(CampaignOption.USE_ARTILLERY));
        gameOptions.getOption(RPG_PILOT_ADVANTAGES).setValue(get(CampaignOption.USE_ABILITIES));
        gameOptions.getOption(EDGE).setValue(get(CampaignOption.USE_EDGE));
        gameOptions.getOption(RPG_MANEI_DOMINI).setValue(get(CampaignOption.USE_IMPLANTS));
        gameOptions.getOption(ADVANCED_STRATOPS_QUIRKS).setValue(get(CampaignOption.USE_QUIRKS));
        gameOptions.getOption(ALLOWED_CANON_ONLY).setValue(get(CampaignOption.ALLOW_CANON_ONLY));
        gameOptions.getOption(ALLOWED_CANON_ONLY).setValue(get(CampaignOption.ALLOW_CANON_ONLY));

        gameOptions.getOption(ALLOWED_TECH_LEVEL)
              .setValue(TechConstants.T_SIMPLE_NAMES[get(CampaignOption.TECH_LEVEL)]);
    }
}
