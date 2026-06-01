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

import megamek.common.enums.SkillLevel;
import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.campaignOptions.BoardScalingType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.stratCon.StratConPlayType;

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

    RulesetsOptionsModel(CampaignOptions options) {
        skillLevel = options.getSkillLevel();
        boardScalingType = options.getBoardScalingType();
        opForLanceTypeMeks = options.getOpForLanceTypeMeks();
        opForLanceTypeMixed = options.getOpForLanceTypeMixed();
        opForLanceTypeVehicles = options.getOpForLanceTypeVehicles();
        autoGenerateOpForCallSigns = options.isAutoGenerateOpForCallSigns();
        minimumCallsignSkillLevel = options.getMinimumCallsignSkillLevel();
        useDropShips = options.isUseDropShips();
        regionalMekVariations = options.isRegionalMekVariations();
        attachedPlayerCamouflage = options.isAttachedPlayerCamouflage();
        playerControlsAttachedUnits = options.isPlayerControlsAttachedUnits();
        useAdvancedBuildingGunEmplacements = options.isUseAdvancedBuildingGunEmplacements();
        spaUpgradeIntensity = options.getSpaUpgradeIntensity();
        reinforcementBaseTargetNumber = options.getReinforcementBaseTargetNumber();
        autoConfigMunitions = options.isAutoConfigMunitions();
        clansObeyBiddingRules = options.isClansObeyBiddingRules();
        enemyFacilityModifierDieSize = options.getEnemyFacilityModifierDieSize();
        alliedFacilityModifierDieSize = options.getAlliedFacilityModifierDieSize();
        scenarioModMax = options.getScenarioModMax();
        scenarioModChance = options.getScenarioModChance();
        scenarioModBV = options.getScenarioModBV();
        useWeatherConditions = options.isUseWeatherConditions();
        useLightConditions = options.isUseLightConditions();
        usePlanetaryConditions = options.isUsePlanetaryConditions();
        useNoTornadoes = options.isUseNoTornadoes();
        fixedMapChance = options.getFixedMapChance();
        restrictPartsByMission = options.isRestrictPartsByMission();
        moraleVictoryEffect = options.getMoraleVictoryEffect();
        moraleDecisiveVictoryEffect = options.getMoraleDecisiveVictoryEffect();
        moraleDefeatEffect = options.getMoraleDefeatEffect();
        moraleDecisiveDefeatEffect = options.getMoraleDecisiveDefeatEffect();
        autoResolveMethod = options.getAutoResolveMethod();
        strategicViewTheme = options.getStrategicViewTheme().getName();
        autoResolveVictoryChanceEnabled = options.isAutoResolveVictoryChanceEnabled();
        autoResolveNumberOfScenarios = options.getAutoResolveNumberOfScenarios();
        autoResolveExperimentalPacarGuiEnabled = options.isAutoResolveExperimentalPacarGuiEnabled();
        stratConPlayType = options.getStratConPlayType();
        useAdvancedScouting = options.isUseAdvancedScouting();
        noSeedForces = options.isNoSeedForces();
        useGenericBattleValue = options.isUseGenericBattleValue();
        useVerboseBidding = options.isUseVerboseBidding();
    }

    void applyTo(CampaignOptions options) {
        options.setSkillLevel(skillLevel);
        options.setBoardScalingType(boardScalingType);
        options.setOpForLanceTypeMeks(opForLanceTypeMeks);
        options.setOpForLanceTypeMixed(opForLanceTypeMixed);
        options.setOpForLanceTypeVehicles(opForLanceTypeVehicles);
        options.setAutoGenerateOpForCallSigns(autoGenerateOpForCallSigns);
        options.setMinimumCallsignSkillLevel(minimumCallsignSkillLevel);
        options.setUseDropShips(useDropShips);
        options.setRegionalMekVariations(regionalMekVariations);
        options.setAttachedPlayerCamouflage(attachedPlayerCamouflage);
        options.setPlayerControlsAttachedUnits(playerControlsAttachedUnits);
        options.setUseAdvancedBuildingGunEmplacements(useAdvancedBuildingGunEmplacements);
        options.setSpaUpgradeIntensity(spaUpgradeIntensity);
        options.setReinforcementBaseTargetNumber(reinforcementBaseTargetNumber);
        options.setAutoConfigMunitions(autoConfigMunitions);
        options.setClansObeyBiddingRules(clansObeyBiddingRules);
        options.setEnemyFacilityModifierDieSize(enemyFacilityModifierDieSize);
        options.setAlliedFacilityModifierDieSize(alliedFacilityModifierDieSize);
        options.setScenarioModMax(scenarioModMax);
        options.setScenarioModChance(scenarioModChance);
        options.setScenarioModBV(scenarioModBV);
        options.setUseWeatherConditions(useWeatherConditions);
        options.setUseLightConditions(useLightConditions);
        options.setUsePlanetaryConditions(usePlanetaryConditions);
        options.setUseNoTornadoes(useNoTornadoes);
        options.setFixedMapChance(fixedMapChance);
        options.setRestrictPartsByMission(restrictPartsByMission);
        options.setMoraleVictoryEffect(moraleVictoryEffect);
        options.setMoraleDecisiveVictoryEffect(moraleDecisiveVictoryEffect);
        options.setMoraleDefeatEffect(moraleDefeatEffect);
        options.setMoraleDecisiveDefeatEffect(moraleDecisiveDefeatEffect);
        options.setAutoResolveMethod(autoResolveMethod);
        options.setStrategicViewTheme(strategicViewTheme);
        options.setAutoResolveVictoryChanceEnabled(autoResolveVictoryChanceEnabled);
        options.setAutoResolveNumberOfScenarios(autoResolveNumberOfScenarios);
        options.setAutoResolveExperimentalPacarGuiEnabled(autoResolveExperimentalPacarGuiEnabled);
        options.setStratConPlayType(stratConPlayType);
        options.setUseAdvancedScouting(useAdvancedScouting);
        options.setNoSeedForces(noSeedForces);
        options.setUseGenericBattleValue(useGenericBattleValue);
        options.setUseVerboseBidding(useVerboseBidding);
    }
}