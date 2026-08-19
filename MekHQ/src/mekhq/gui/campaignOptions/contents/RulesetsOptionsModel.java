/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.contents;

import jakarta.annotation.Nonnull;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.campaignOptions.BoardScalingType;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.gm.StratConPlayType;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorCountMethod;

class RulesetsOptionsModel {
    SkillLevel skillLevel;
    BoardScalingType boardScalingType;
    int opForLanceTypeMeks;
    int opForLanceTypeMixed;
    int opForLanceTypeVehicles;
    boolean autoGenerateOpForCallSigns;
    SkillLevel minimumCallsignSkillLevel;
    boolean useDropShips;
    boolean regionalMekVariations;
    boolean attachedPlayerCamouflage;
    boolean playerControlsAttachedUnits;
    boolean useAdvancedBuildingGunEmplacements;
    int spaUpgradeIntensity;
    int reinforcementBaseTargetNumber;
    boolean autoConfigMunitions;
    boolean clansObeyBiddingRules;
    int enemyFacilityModifierDieSize;
    int alliedFacilityModifierDieSize;
    int scenarioModMax;
    int scenarioModChance;
    int scenarioModBV;
    boolean useWeatherConditions;
    boolean useLightConditions;
    boolean usePlanetaryConditions;
    boolean useNoTornadoes;
    int fixedMapChance;
    boolean restrictPartsByMission;
    int moraleVictoryEffect;
    int moraleDecisiveVictoryEffect;
    int moraleDefeatEffect;
    int moraleDecisiveDefeatEffect;
    AutoResolveMethod autoResolveMethod;
    String strategicViewTheme;
    boolean autoResolveVictoryChanceEnabled;
    int autoResolveNumberOfScenarios;
    boolean autoResolveExperimentalPacarGuiEnabled;
    StratConPlayType stratConPlayType;
    boolean useAdvancedScouting;
    boolean noSeedForces;
    boolean useGenericBattleValue;
    boolean useVerboseBidding;
    StratConSectorCountMethod stratConSectorCountMethod;
    boolean useStratConAlternateSectorTerrain;
    double stratConSectorSizeMultiplier;

    RulesetsOptionsModel(@Nonnull CampaignOptions options) {
        skillLevel = options.get(CampaignOption.SKILL_LEVEL);
        boardScalingType = options.get(CampaignOption.BOARD_SCALING_TYPE);
        opForLanceTypeMeks = options.get(CampaignOption.OP_FOR_LANCE_TYPE_MEKS);
        opForLanceTypeMixed = options.get(CampaignOption.OP_FOR_LANCE_TYPE_MIXED);
        opForLanceTypeVehicles = options.get(CampaignOption.OP_FOR_LANCE_TYPE_VEHICLES);
        autoGenerateOpForCallSigns = options.get(CampaignOption.AUTO_GENERATE_OP_FOR_CALL_SIGNS);
        minimumCallsignSkillLevel = options.get(CampaignOption.MINIMUM_CALLSIGN_SKILL_LEVEL);
        useDropShips = options.get(CampaignOption.USE_DROP_SHIPS);
        regionalMekVariations = options.get(CampaignOption.REGIONAL_MEK_VARIATIONS);
        attachedPlayerCamouflage = options.get(CampaignOption.ATTACHED_PLAYER_CAMOUFLAGE);
        playerControlsAttachedUnits = options.get(CampaignOption.PLAYER_CONTROLS_ATTACHED_UNITS);
        useAdvancedBuildingGunEmplacements = options.get(CampaignOption.USE_ADVANCED_BUILDING_GUN_EMPLACEMENTS);
        spaUpgradeIntensity = options.get(CampaignOption.SPA_UPGRADE_INTENSITY);
        reinforcementBaseTargetNumber = options.get(CampaignOption.REINFORCEMENT_BASE_TARGET_NUMBER);
        autoConfigMunitions = options.get(CampaignOption.AUTO_CONFIG_MUNITIONS);
        clansObeyBiddingRules = options.get(CampaignOption.CLANS_OBEY_BIDDING_RULES);
        enemyFacilityModifierDieSize = options.get(CampaignOption.ENEMY_FACILITY_MODIFIER_DIE_SIZE);
        alliedFacilityModifierDieSize = options.get(CampaignOption.ALLIED_FACILITY_MODIFIER_DIE_SIZE);
        scenarioModMax = options.get(CampaignOption.SCENARIO_MOD_MAX);
        scenarioModChance = options.get(CampaignOption.SCENARIO_MOD_CHANCE);
        scenarioModBV = options.get(CampaignOption.SCENARIO_MOD_BV);
        useWeatherConditions = options.get(CampaignOption.USE_WEATHER_CONDITIONS);
        useLightConditions = options.get(CampaignOption.USE_LIGHT_CONDITIONS);
        usePlanetaryConditions = options.get(CampaignOption.USE_PLANETARY_CONDITIONS);
        useNoTornadoes = options.get(CampaignOption.USE_NO_TORNADOES);
        fixedMapChance = options.get(CampaignOption.FIXED_MAP_CHANCE);
        restrictPartsByMission = options.get(CampaignOption.RESTRICT_PARTS_BY_MISSION);
        moraleVictoryEffect = options.get(CampaignOption.MORALE_VICTORY_EFFECT);
        moraleDecisiveVictoryEffect = options.get(CampaignOption.MORALE_DECISIVE_VICTORY_EFFECT);
        moraleDefeatEffect = options.get(CampaignOption.MORALE_DEFEAT_EFFECT);
        moraleDecisiveDefeatEffect = options.get(CampaignOption.MORALE_DECISIVE_DEFEAT_EFFECT);
        autoResolveMethod = options.get(CampaignOption.AUTO_RESOLVE_METHOD);
        strategicViewTheme = options.getStrategicViewTheme().getName();
        autoResolveVictoryChanceEnabled = options.get(CampaignOption.AUTO_RESOLVE_VICTORY_CHANCE_ENABLED);
        autoResolveNumberOfScenarios = options.get(CampaignOption.AUTO_RESOLVE_NUMBER_OF_SCENARIOS);
        autoResolveExperimentalPacarGuiEnabled = options.get(CampaignOption.AUTO_RESOLVE_EXPERIMENTAL_PACAR_GUI_ENABLED);
        stratConPlayType = options.get(CampaignOption.STRAT_CON_PLAY_TYPE);
        useAdvancedScouting = options.get(CampaignOption.USE_ADVANCED_SCOUTING);
        noSeedForces = options.get(CampaignOption.NO_SEED_FORCES);
        useGenericBattleValue = options.get(CampaignOption.USE_GENERIC_BATTLE_VALUE);
        useVerboseBidding = options.get(CampaignOption.USE_VERBOSE_BIDDING);
        stratConSectorCountMethod = options.get(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD);
        useStratConAlternateSectorTerrain = options.get(CampaignOption.USE_STRAT_CON_ALTERNATE_SECTOR_TERRAIN);
        stratConSectorSizeMultiplier = options.get(CampaignOption.STRAT_CON_SECTOR_SIZE_MULTIPLIER);
    }

    void applyTo(@Nonnull CampaignOptions options) {
        options.set(CampaignOption.SKILL_LEVEL, skillLevel);
        options.set(CampaignOption.BOARD_SCALING_TYPE, boardScalingType);
        options.set(CampaignOption.OP_FOR_LANCE_TYPE_MEKS, opForLanceTypeMeks);
        options.set(CampaignOption.OP_FOR_LANCE_TYPE_MIXED, opForLanceTypeMixed);
        options.set(CampaignOption.OP_FOR_LANCE_TYPE_VEHICLES, opForLanceTypeVehicles);
        options.set(CampaignOption.AUTO_GENERATE_OP_FOR_CALL_SIGNS, autoGenerateOpForCallSigns);
        options.set(CampaignOption.MINIMUM_CALLSIGN_SKILL_LEVEL, minimumCallsignSkillLevel);
        options.set(CampaignOption.USE_DROP_SHIPS, useDropShips);
        options.set(CampaignOption.REGIONAL_MEK_VARIATIONS, regionalMekVariations);
        options.set(CampaignOption.ATTACHED_PLAYER_CAMOUFLAGE, attachedPlayerCamouflage);
        options.set(CampaignOption.PLAYER_CONTROLS_ATTACHED_UNITS, playerControlsAttachedUnits);
        options.set(CampaignOption.USE_ADVANCED_BUILDING_GUN_EMPLACEMENTS, useAdvancedBuildingGunEmplacements);
        options.set(CampaignOption.SPA_UPGRADE_INTENSITY, spaUpgradeIntensity);
        options.set(CampaignOption.REINFORCEMENT_BASE_TARGET_NUMBER, reinforcementBaseTargetNumber);
        options.set(CampaignOption.AUTO_CONFIG_MUNITIONS, autoConfigMunitions);
        options.set(CampaignOption.CLANS_OBEY_BIDDING_RULES, clansObeyBiddingRules);
        options.set(CampaignOption.ENEMY_FACILITY_MODIFIER_DIE_SIZE, enemyFacilityModifierDieSize);
        options.set(CampaignOption.ALLIED_FACILITY_MODIFIER_DIE_SIZE, alliedFacilityModifierDieSize);
        options.set(CampaignOption.SCENARIO_MOD_MAX, scenarioModMax);
        options.set(CampaignOption.SCENARIO_MOD_CHANCE, scenarioModChance);
        options.set(CampaignOption.SCENARIO_MOD_BV, scenarioModBV);
        options.set(CampaignOption.USE_WEATHER_CONDITIONS, useWeatherConditions);
        options.set(CampaignOption.USE_LIGHT_CONDITIONS, useLightConditions);
        options.set(CampaignOption.USE_PLANETARY_CONDITIONS, usePlanetaryConditions);
        options.set(CampaignOption.USE_NO_TORNADOES, useNoTornadoes);
        options.set(CampaignOption.FIXED_MAP_CHANCE, fixedMapChance);
        options.set(CampaignOption.RESTRICT_PARTS_BY_MISSION, restrictPartsByMission);
        options.set(CampaignOption.MORALE_VICTORY_EFFECT, moraleVictoryEffect);
        options.set(CampaignOption.MORALE_DECISIVE_VICTORY_EFFECT, moraleDecisiveVictoryEffect);
        options.set(CampaignOption.MORALE_DEFEAT_EFFECT, moraleDefeatEffect);
        options.set(CampaignOption.MORALE_DECISIVE_DEFEAT_EFFECT, moraleDecisiveDefeatEffect);
        options.set(CampaignOption.AUTO_RESOLVE_METHOD, autoResolveMethod);
        options.setStrategicViewTheme(strategicViewTheme);
        options.set(CampaignOption.AUTO_RESOLVE_VICTORY_CHANCE_ENABLED, autoResolveVictoryChanceEnabled);
        options.set(CampaignOption.AUTO_RESOLVE_NUMBER_OF_SCENARIOS, autoResolveNumberOfScenarios);
        options.set(CampaignOption.AUTO_RESOLVE_EXPERIMENTAL_PACAR_GUI_ENABLED, autoResolveExperimentalPacarGuiEnabled);
        options.set(CampaignOption.STRAT_CON_PLAY_TYPE, stratConPlayType);
        options.set(CampaignOption.USE_ADVANCED_SCOUTING, useAdvancedScouting);
        options.set(CampaignOption.NO_SEED_FORCES, noSeedForces);
        options.set(CampaignOption.USE_GENERIC_BATTLE_VALUE, useGenericBattleValue);
        options.set(CampaignOption.USE_VERBOSE_BIDDING, useVerboseBidding);
        options.set(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD, stratConSectorCountMethod);
        options.set(CampaignOption.USE_STRAT_CON_ALTERNATE_SECTOR_TERRAIN, useStratConAlternateSectorTerrain);
        options.set(CampaignOption.STRAT_CON_SECTOR_SIZE_MULTIPLIER, stratConSectorSizeMultiplier);
    }
}
