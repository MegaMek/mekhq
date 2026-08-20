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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import jakarta.annotation.Nonnull;
import megamek.common.TechConstants;
import megamek.common.enums.SkillLevel;
import megamek.common.options.GameOptions;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import mekhq.campaign.digitalGM.stratCon.gm.StratConPlayType;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.mission.utilities.CombatRole;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.campaign.personnel.enums.MergingSurnameStyle;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.enums.SplittingSurnameStyle;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.service.mrms.MRMSOption;

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
            get(CampaignOption.MRMS_OPTIONS).add(new MRMSOption(type));
        }
        get(CampaignOption.PLANET_TECH_ACQUISITION_BONUS).put(PlanetarySophistication.ADVANCED, -2); // TODO: needs to be verified
        get(CampaignOption.PLANET_TECH_ACQUISITION_BONUS).put(PlanetarySophistication.A, -1);
        get(CampaignOption.PLANET_TECH_ACQUISITION_BONUS).put(PlanetarySophistication.B, 0);
        get(CampaignOption.PLANET_TECH_ACQUISITION_BONUS).put(PlanetarySophistication.C, 1);
        get(CampaignOption.PLANET_TECH_ACQUISITION_BONUS).put(PlanetarySophistication.D, 2);
        get(CampaignOption.PLANET_TECH_ACQUISITION_BONUS).put(PlanetarySophistication.F, 8);
        get(CampaignOption.PLANET_TECH_ACQUISITION_BONUS).put(PlanetarySophistication.REGRESSED, 16); // TODO: needs to be verified
        get(CampaignOption.PLANET_INDUSTRY_ACQUISITION_BONUS).put(PlanetaryRating.A, 0);
        get(CampaignOption.PLANET_INDUSTRY_ACQUISITION_BONUS).put(PlanetaryRating.B, 0);
        get(CampaignOption.PLANET_INDUSTRY_ACQUISITION_BONUS).put(PlanetaryRating.C, 0);
        get(CampaignOption.PLANET_INDUSTRY_ACQUISITION_BONUS).put(PlanetaryRating.D, 0);
        get(CampaignOption.PLANET_INDUSTRY_ACQUISITION_BONUS).put(PlanetaryRating.F, 0);
        get(CampaignOption.PLANET_OUTPUT_ACQUISITION_BONUS).put(PlanetaryRating.A, -1);
        get(CampaignOption.PLANET_OUTPUT_ACQUISITION_BONUS).put(PlanetaryRating.B, 0);
        get(CampaignOption.PLANET_OUTPUT_ACQUISITION_BONUS).put(PlanetaryRating.C, 1);
        get(CampaignOption.PLANET_OUTPUT_ACQUISITION_BONUS).put(PlanetaryRating.D, 2);
        get(CampaignOption.PLANET_OUTPUT_ACQUISITION_BONUS).put(PlanetaryRating.F, 8);
        set(CampaignOption.USE_AMMO_BY_TYPE, false);
        set(CampaignOption.USE_ADVANCED_MEDICAL, false);
        get(CampaignOption.SALARY_XP_MULTIPLIERS).put(SkillLevel.NONE, 0.5);
        get(CampaignOption.SALARY_XP_MULTIPLIERS).put(SkillLevel.ULTRA_GREEN, 0.6);
        get(CampaignOption.SALARY_XP_MULTIPLIERS).put(SkillLevel.GREEN, 0.6);
        get(CampaignOption.SALARY_XP_MULTIPLIERS).put(SkillLevel.REGULAR, 1.0);
        get(CampaignOption.SALARY_XP_MULTIPLIERS).put(SkillLevel.VETERAN, 1.6);
        get(CampaignOption.SALARY_XP_MULTIPLIERS).put(SkillLevel.ELITE, 3.2);
        get(CampaignOption.SALARY_XP_MULTIPLIERS).put(SkillLevel.HEROIC, 6.4);
        get(CampaignOption.SALARY_XP_MULTIPLIERS).put(SkillLevel.LEGENDARY, 12.8);
        set(CampaignOption.ROLE_BASE_SALARIES, new Money[personnelRoles.length]);
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
        set(CampaignOption.USE_DYLANS_RANDOM_XP, false);

        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.NO_CHANGE, 100);
        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.YOURS, 55);
        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.SPOUSE, 55);
        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.SPACE_YOURS, 10);
        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.BOTH_SPACE_YOURS, 5);
        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.HYPHEN_YOURS, 30);
        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.BOTH_HYPHEN_YOURS, 20);
        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.SPACE_SPOUSE, 10);
        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.BOTH_SPACE_SPOUSE, 5);
        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.HYPHEN_SPOUSE, 30);
        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.BOTH_HYPHEN_SPOUSE, 20);
        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.MALE, 500);
        get(CampaignOption.MARRIAGE_SURNAME_WEIGHTS).put(MergingSurnameStyle.FEMALE, 160);

        get(CampaignOption.DIVORCE_SURNAME_WEIGHTS).put(SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME, 10);
        get(CampaignOption.DIVORCE_SURNAME_WEIGHTS).put(SplittingSurnameStyle.SPOUSE_CHANGES_SURNAME, 10);
        get(CampaignOption.DIVORCE_SURNAME_WEIGHTS).put(SplittingSurnameStyle.BOTH_CHANGE_SURNAME, 30);
        get(CampaignOption.DIVORCE_SURNAME_WEIGHTS).put(SplittingSurnameStyle.BOTH_KEEP_SURNAME, 50);

        get(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS).put(AgeGroup.ELDER, true);
        get(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS).put(AgeGroup.ADULT, true);
        get(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS).put(AgeGroup.TEENAGER, true);
        get(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS).put(AgeGroup.PRETEEN, false);
        get(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS).put(AgeGroup.CHILD, false);
        get(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS).put(AgeGroup.TODDLER, false);
        get(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS).put(AgeGroup.BABY, false);
        set(CampaignOption.USE_PERCENTAGE_MAINTENANCE, false);
        set(CampaignOption.INFANTRY_DONT_COUNT, false);

        set(CampaignOption.USED_PART_PRICE_MULTIPLIERS, new double[] { 0.1, 0.2, 0.3, 0.5, 0.7, 0.9 });

        setEquipmentContractPercent(5.0);
        setDropShipContractPercent(1.0);
        setJumpShipContractPercent(0.0);
        setWarShipContractPercent(0.0);

        set(CampaignOption.PHENOTYPE_PROBABILITIES, new int[Phenotype.getExternalPhenotypes().size()]);
        get(CampaignOption.PHENOTYPE_PROBABILITIES)[Phenotype.MEKWARRIOR.ordinal()] = 95;
        get(CampaignOption.PHENOTYPE_PROBABILITIES)[Phenotype.ELEMENTAL.ordinal()] = 100;
        get(CampaignOption.PHENOTYPE_PROBABILITIES)[Phenotype.AEROSPACE.ordinal()] = 95;
        get(CampaignOption.PHENOTYPE_PROBABILITIES)[Phenotype.VEHICLE.ordinal()] = 0;
        get(CampaignOption.PHENOTYPE_PROBABILITIES)[Phenotype.PROTOMEK.ordinal()] = 95;
        get(CampaignOption.PHENOTYPE_PROBABILITIES)[Phenotype.NAVAL.ordinal()] = 25;

        set(CampaignOption.USE_PORTRAIT_FOR_ROLE, new boolean[personnelRoles.length]);
        Arrays.fill(get(CampaignOption.USE_PORTRAIT_FOR_ROLE), false);
        get(CampaignOption.USE_PORTRAIT_FOR_ROLE)[PersonnelRole.MEKWARRIOR.ordinal()] = true;

        set(CampaignOption.PERSONNEL_MARKET_NAME, PersonnelMarket.getTypeName(PersonnelMarket.TYPE_NONE));
        set(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS, new HashMap<>());
        get(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS).put(SkillLevel.NONE, 3);
        get(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS).put(SkillLevel.ULTRA_GREEN, 4);
        get(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS).put(SkillLevel.GREEN, 4);
        get(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS).put(SkillLevel.REGULAR, 6);
        get(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS).put(SkillLevel.VETERAN, 8);
        get(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS).put(SkillLevel.ELITE, 10);
        get(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS).put(SkillLevel.HEROIC, 11);
        get(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS).put(SkillLevel.LEGENDARY, 11);

        set(CampaignOption.STRATEGIC_VIEW_MINIMAP_THEME, "gbc green.theme");

        set(CampaignOption.ATB_BATTLE_CHANCE, new int[CombatRole.values().length - 1]);
        get(CampaignOption.ATB_BATTLE_CHANCE)[CombatRole.MANEUVER.ordinal()] = 40;
        get(CampaignOption.ATB_BATTLE_CHANCE)[CombatRole.FRONTLINE.ordinal()] = 20;
        get(CampaignOption.ATB_BATTLE_CHANCE)[CombatRole.PATROL.ordinal()] = 60;
        get(CampaignOption.ATB_BATTLE_CHANCE)[CombatRole.TRAINING.ordinal()] = 10;
        get(CampaignOption.ATB_BATTLE_CHANCE)[CombatRole.CADRE.ordinal()] = 10;

        set(CampaignOption.USE_FACTION_STANDING_OUTLAWED, true);
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

    // endregion General Tab

    // region Repair and Maintenance Tab
    // region Repair
    // endregion Repair

    // region Maintenance

    // endregion Maintenance

    // region Mass Repair/ Mass Salvage

    public void addMRMSOption(final MRMSOption mrmsOption) {
        if (mrmsOption.getType().isUnknownLocation()) {
            return;
        }

        get(CampaignOption.MRMS_OPTIONS).removeIf(option -> option.getType() == mrmsOption.getType());
        get(CampaignOption.MRMS_OPTIONS).add(mrmsOption);
    }
    // endregion Mass Repair/ Mass Salvage
    // endregion Repair and Maintenance Tab

    // region Supplies and Acquisitions Tab
    // endregion Supplies and Acquisitions Tab

    // region Personnel Tab
    // region General Personnel

    // endregion General Personnel

    // region Expanded Personnel Information

    // endregion Expanded Personnel Information

    // region Medical

    /**
     * Checks if any form of advanced medical system is enabled.
     *
     * <p>This method returns {@code true} if either the standard advanced medical system or the alternative advanced
     * medical system is enabled.</p>
     *
     * @return {@code true} if either advanced medical system is in use, {@code false} otherwise
     */
    public boolean isUseAdvancedMedical() {
        return get(CampaignOption.USE_ADVANCED_MEDICAL) || get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL);
    }

    // endregion Medical

    // region Prisoners

    // endregion Prisoners

    // region Personnel Randomization

    // endregion Personnel Randomization

    // region Random Histories

    // endregion Random Histories

    // region Retirement

    // endregion Retirement

    // region Family

    // endregion Family

    // region anniversaries

    // endregion anniversaries

    //startregion Life Events

    //endregion Life Events

    // region Dependents

    // endregion Dependent

    // region Personnel Removal

    // endregion Personnel Removal

    // region Salary

    public void setRoleBaseSalary(final PersonnelRole role, final double baseSalary) {
        setRoleBaseSalary(role, Money.of(baseSalary));
    }

    public void setRoleBaseSalary(final PersonnelRole role, final Money baseSalary) {
        get(CampaignOption.ROLE_BASE_SALARIES)[role.ordinal()] = baseSalary;
    }
    // endregion Salary

    // region Marriage

    // endregion Marriage

    // region Divorce

    // endregion Divorce

    // region Procreation

    // endregion Procreation

    // region Death

    // endregion Death

    // region Awards

    // endregion Awards
    // endregion Personnel Tab

    // region Finances Tab

    // region Price Multipliers

    // endregion Price Multipliers

    // region Taxes

    // endregion Taxes
    // endregion Finances Tab

    // region Markets Tab
    // region Personnel Market

    // endregion Personnel Market

    // region Unit Market

    // endregion Unit Market

    // region Contract Market

    // endregion Contract Market
    // endregion Markets Tab

    public int getPhenotypeProbability(final Phenotype phenotype) {
        return get(CampaignOption.PHENOTYPE_PROBABILITIES)[phenotype.ordinal()];
    }

    public void setPhenotypeProbability(final int index, final int percentage) {
        get(CampaignOption.PHENOTYPE_PROBABILITIES)[index] = percentage;
    }

    public boolean isUsePortraitForRole(final PersonnelRole role) {
        return get(CampaignOption.USE_PORTRAIT_FOR_ROLE)[role.ordinal()];
    }

    public void setUsePortraitForRole(final int index, final boolean use) {
        get(CampaignOption.USE_PORTRAIT_FOR_ROLE)[index] = use;
    }

    public void setEquipmentContractPercent(final double equipmentContractPercent) {
        set(CampaignOption.EQUIPMENT_CONTRACT_PERCENT,
              Math.min(equipmentContractPercent, MAXIMUM_COMBAT_EQUIPMENT_PERCENT));
    }

    public void setDropShipContractPercent(final double dropShipContractPercent) {
        set(CampaignOption.DROP_SHIP_CONTRACT_PERCENT,
              Math.min(dropShipContractPercent, MAXIMUM_DROPSHIP_EQUIPMENT_PERCENT));
    }

    public void setJumpShipContractPercent(final double jumpShipContractPercent) {
        set(CampaignOption.JUMP_SHIP_CONTRACT_PERCENT,
              Math.min(jumpShipContractPercent, MAXIMUM_JUMPSHIP_EQUIPMENT_PERCENT));
    }

    public void setWarShipContractPercent(final double warShipContractPercent) {
        set(CampaignOption.WAR_SHIP_CONTRACT_PERCENT,
              Math.min(warShipContractPercent, MAXIMUM_WARSHIP_EQUIPMENT_PERCENT));
    }

    public int getPlanetTechAcquisitionBonus(final PlanetarySophistication sophistication) {
        return get(CampaignOption.PLANET_TECH_ACQUISITION_BONUS).getOrDefault(sophistication, 0);
    }

    public void setPlanetTechAcquisitionBonus(final int base, final PlanetarySophistication sophistication) {
        get(CampaignOption.PLANET_TECH_ACQUISITION_BONUS).put(sophistication, base);
    }

    public int getPlanetIndustryAcquisitionBonus(final PlanetaryRating rating) {
        return get(CampaignOption.PLANET_INDUSTRY_ACQUISITION_BONUS).getOrDefault(rating, 0);
    }

    public void setPlanetIndustryAcquisitionBonus(final int base, final PlanetaryRating rating) {
        get(CampaignOption.PLANET_INDUSTRY_ACQUISITION_BONUS).put(rating, base);
    }

    public int getPlanetOutputAcquisitionBonus(final PlanetaryRating rating) {
        return get(CampaignOption.PLANET_OUTPUT_ACQUISITION_BONUS).getOrDefault(rating, 0);
    }

    public void setPlanetOutputAcquisitionBonus(final int base, final PlanetaryRating rating) {
        get(CampaignOption.PLANET_OUTPUT_ACQUISITION_BONUS).put(rating, base);
    }

    public boolean isUseStratCon() {
        return get(CampaignOption.STRAT_CON_PLAY_TYPE) != StratConPlayType.DISABLED;
    }

    public boolean isUseStratConMaplessMode() {
        return get(CampaignOption.STRAT_CON_PLAY_TYPE) == StratConPlayType.MAPLESS ||
                     // Singles is a type of mapless mode, so all rules that apply to Mapless also apply to Singles
                     get(CampaignOption.STRAT_CON_PLAY_TYPE) == StratConPlayType.SINGLES;
    }

    public boolean isUseStratConSinglesMode() {
        return get(CampaignOption.STRAT_CON_PLAY_TYPE) == StratConPlayType.SINGLES;
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

        return get(CampaignOption.ATB_BATTLE_CHANCE)[role.ordinal()];
    }

    /**
     * @param role      the {@link CombatRole} ordinal value
     * @param frequency the frequency to set the generation to (percent chance from 0 to 100)
     */
    public void setAtBBattleChance(final int role, final int frequency) {
        get(CampaignOption.ATB_BATTLE_CHANCE)[role] = Math.clamp(frequency, 0, 100);
    }

    public boolean isLimitLanceNumUnits() {
        return false;
    }

    // region File IO

    // endregion File IO

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
     * Checks whether tracking faction standing is enabled and if the use of faction standing negotiation is active.
     *
     * @return {@code true} if both faction standing tracking and faction standing negotiation usage are enabled;
     *       {@code false} otherwise.
     */
    public boolean isUseFactionStandingNegotiationSafe() {
        return get(CampaignOption.TRACK_FACTION_STANDING) && get(CampaignOption.USE_FACTION_STANDING_NEGOTIATION);
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
     * Checks whether tracking faction standing is enabled and if the use of faction standing outlawing is active.
     *
     * @return {@code true} if both faction standing tracking and faction standing outlaw usage are enabled;
     *       {@code false} otherwise.
     */
    public boolean isUseFactionStandingOutlawedSafe() {
        return get(CampaignOption.TRACK_FACTION_STANDING) && get(CampaignOption.USE_FACTION_STANDING_OUTLAWED);
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
