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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static megamek.common.units.UnitType.CONV_FIGHTER;
import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConScenarioFactory;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;

/**
 * The behavior of a data cache: intelligence the player must recover, placed in place of every point of interest on an
 * Espionage contract. Every data cache is a strategic objective.
 *
 * <p>When a ground formation deploys onto the cache's hex, the usual scenario roll is made. If no scenario breaks out,
 * the cache is secured on the spot. If one does, it is a {@value #SCENARIO_TEMPLATE} scenario fought over the cache:
 * an overall victory secures it, and anything else - a defeat, a draw, or leaving the scenario unplayed - loses it.
 * Either way the cache then leaves the map, and its objective is met or failed. Securing a cache pays the contract's
 * combat bonus, standing in for the Essential scenarios such a contract does not get.</p>
 *
 * <p>A cache that is not recovered in time expires and is lost (its lifespan comes from its definition), except while
 * a scenario over it is still to be fought.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConDataCacheBehavior implements IStratConPointOfInterestBehavior {
    private static final MMLogger LOGGER = MMLogger.create(StratConDataCacheBehavior.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConRulesManager";

    /** The behavior ID the data cache definition names. */
    public static final String BEHAVIOR_ID = "dataCache";

    /** The type ID of the data cache definition. */
    public static final String TYPE_ID = "DataCache";

    /** The scenario template fought over a contested data cache. */
    static final String SCENARIO_TEMPLATE = "Recon Evasion.json";

    /**
     * A ground formation deploying onto the cache rolls for a scenario the usual way. With no scenario, the cache is
     * secured at once; with one, a scenario is placed on the cache's hex and linked to it, and the deploying formation
     * is then assigned to it like any scenario found on the hex. Either way, no other random scenario is rolled.
     *
     * <p>A formation that is not a ground formation cannot recover the cache, and deploys as it would onto any other
     * hex. Nor does a formation joining a scenario already fought over the cache roll again.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public PointOfInterestDeploymentOutcome onFormationDeployed(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, Campaign campaign) {
        if (pointOfInterest.hasLinkedScenario() || !isGroundFormation(formationId, campaign)) {
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        if (contract == null) {
            LOGGER.warn("No active contract holds the sector {} of data cache {}.",
                  track.getDisplayableName(),
                  pointOfInterest);
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        boolean essentialScenariosOnly = campaign.getCampaignOptions().get(CampaignOption.ESSENTIAL_SCENARIOS_ONLY);
        int targetNumber = StratConRulesManager.calculateScenarioOdds(track, contract, true);
        boolean isContested = StratConRulesManager.rollsRandomScenario(PointOfInterestDeploymentOutcome.NO_EFFECT,
              essentialScenariosOnly,
              false,
              false,
              targetNumber);

        if (!isContested) {
            secureDataCache(pointOfInterest, track, campaign);
            return PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
        }

        StratConScenario scenario = placeContestingScenario(pointOfInterest, track, contract, campaign);
        if (scenario == null) {
            // Nothing to fight over the cache, so leave it in place and let the deployment carry on as usual.
            LOGGER.error("Could not create a scenario over data cache {}.", pointOfInterest);
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE,
              "StratConDataCacheBehavior.contested.report",
              pointOfInterest.getDisplayableName(),
              track.getDisplayableName()));
        return PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
    }

    @Override
    public void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
        if (isVictory) {
            secureDataCache(pointOfInterest, track, campaign);
            return;
        }

        StratConPointOfInterestRules.losePointOfInterest(track, pointOfInterest);
        campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE,
              "StratConDataCacheBehavior.lost.report",
              pointOfInterest.getDisplayableName(),
              track.getDisplayableName()));
    }

    @Override
    public @Nullable String getObjectiveDescription(StratConPointOfInterest pointOfInterest,
          StratConTrackState track) {
        return getTextAt(RESOURCE_BUNDLE, "StratConDataCacheBehavior.objective");
    }

    /**
     * Places the scenario fought over a contested cache on its hex and links the cache to it.
     *
     * <p>The scenario is deliberately not Essential: Espionage contracts using special mechanics have no Essential
     * scenarios. Winning it still pays the combat bonus, through securing the cache (see
     * {@link #secureDataCache}).</p>
     *
     * @return the scenario, or {@code null} if none could be generated
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static @Nullable StratConScenario placeContestingScenario(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, AbstractContract contract, Campaign campaign) {
        // A missing template is logged by the factory; a random scenario is fought over the cache instead.
        ScenarioTemplate template = StratConScenarioFactory.getSpecificScenario(SCENARIO_TEMPLATE);

        // Facilities are ignored: a data cache occupies its hex, so none can share it.
        StratConScenario scenario = StratConRulesManager.setupScenario(pointOfInterest.getCoords(),
              null,
              campaign,
              contract,
              track,
              template,
              true,
              null);
        if (scenario == null) {
            return null;
        }

        track.addScenario(scenario);
        StratConPointOfInterestRules.linkScenario(pointOfInterest, scenario);
        return scenario;
    }

    /**
     * Secures a cache: resolves it, which meets its objective, takes it off the map, and pays the contract's combat
     * bonus - the reward an Essential scenario would otherwise have given.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void secureDataCache(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          Campaign campaign) {
        StratConPointOfInterestRules.resolvePointOfInterest(track, pointOfInterest);
        track.removePointOfInterest(pointOfInterest.getId());

        campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE,
              "StratConDataCacheBehavior.secured.report",
              pointOfInterest.getDisplayableName(),
              track.getDisplayableName()));

        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        if (contract == null) {
            LOGGER.warn("No active contract holds the sector {} of data cache {}, so no combat bonus is paid.",
                  track.getDisplayableName(),
                  pointOfInterest);
            return;
        }

        StratConRulesManager.awardCombatBonus(campaign, contract);
    }

    /**
     * @return {@code true} if the formation fights on the ground, so can recover a cache: its primary unit type is not
     *       an aerospace or larger craft
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isGroundFormation(int formationId, Campaign campaign) {
        Formation formation = campaign.getPlayerForce().getFormation(formationId);
        return (formation != null) && (formation.getPrimaryUnitType(campaign) < CONV_FIGHTER);
    }
}
