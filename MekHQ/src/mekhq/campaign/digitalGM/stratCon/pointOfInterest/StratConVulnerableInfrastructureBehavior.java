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

import static megamek.common.units.UnitType.MEK;
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
import mekhq.gui.dialog.StratConAmbushedDialog;

/**
 * The behavior of vulnerable infrastructure: an enemy installation the player can destroy, placed in place of every
 * point of interest on a Guerrilla Warfare contract. Every piece of vulnerable infrastructure is a strategic objective.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the
 * infrastructure is destroyed on the spot: its objective is met and the contract's combat bonus is paid, standing in for
 * the Essential scenarios such a contract does not get. If one does, the infrastructure was a trap, and the deploying
 * formation is ambushed. A trap is spent whatever the ambush's result: the infrastructure leaves the map and its
 * objective is removed, neither met nor failed.</p>
 *
 * <p>Vulnerable infrastructure never expires; it waits until it is hit.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConVulnerableInfrastructureBehavior implements IStratConPointOfInterestBehavior {
    private static final MMLogger LOGGER = MMLogger.create(StratConVulnerableInfrastructureBehavior.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConRulesManager";

    /** The behavior ID the vulnerable infrastructure definition names. */
    public static final String BEHAVIOR_ID = "vulnerableInfrastructure";

    /** The type ID of the vulnerable infrastructure definition. */
    public static final String TYPE_ID = "VulnerableInfrastructure";

    /**
     * Any formation deploying onto the infrastructure rolls for a scenario the usual way. With no scenario, the
     * infrastructure is destroyed at once; with one, it was a trap: an ambush is placed on its hex and linked to it, and
     * the deploying formation is then assigned to it like any scenario found on the hex. Either way, no other random
     * scenario is rolled.
     *
     * <p>A formation joining the ambush at a sprung trap does not roll again.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public PointOfInterestDeploymentOutcome onFormationDeployed(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, Campaign campaign) {
        if (pointOfInterest.hasLinkedScenario()) {
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        if (contract == null) {
            LOGGER.warn("No active contract holds the sector {} of vulnerable infrastructure {}.",
                  track.getDisplayableName(),
                  pointOfInterest);
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        boolean essentialScenariosOnly = campaign.getCampaignOptions().get(CampaignOption.ESSENTIAL_SCENARIOS_ONLY);
        int targetNumber = StratConRulesManager.calculateScenarioOdds(track, contract, true);
        boolean isTrap = StratConRulesManager.rollsRandomScenario(PointOfInterestDeploymentOutcome.NO_EFFECT,
              essentialScenariosOnly,
              false,
              false,
              targetNumber);

        if (!isTrap) {
            destroyInfrastructure(pointOfInterest, track, contract, campaign);
            return PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
        }

        StratConScenario ambush = placeAmbush(pointOfInterest, track, formationId, contract, campaign);
        if (ambush == null) {
            // Nothing to spring the trap with, so leave the infrastructure in place and let the deployment carry on.
            LOGGER.error("Could not create an ambush at vulnerable infrastructure {}.", pointOfInterest);
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE,
              "StratConVulnerableInfrastructureBehavior.trap.report",
              pointOfInterest.getDisplayableName(),
              track.getDisplayableName()));
        new StratConAmbushedDialog(campaign, formationId, false);
        return PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
    }

    /**
     * The ambush at a trap has ended - won, lost, or left unplayed. The trap is spent whatever the result: the
     * infrastructure leaves the map and its objective is removed.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
        StratConPointOfInterestRules.withdrawPointOfInterest(track, pointOfInterest);
        campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE,
              "StratConVulnerableInfrastructureBehavior.trapSpent.report",
              pointOfInterest.getDisplayableName(),
              track.getDisplayableName()));
    }

    @Override
    public @Nullable String getObjectiveDescription(StratConPointOfInterest pointOfInterest,
          StratConTrackState track) {
        return getTextAt(RESOURCE_BUNDLE, "StratConVulnerableInfrastructureBehavior.objective");
    }

    /**
     * Places the ambush that springs a trap on the infrastructure's hex and links the infrastructure to it. The ambush
     * is drawn from the templates suited to ambushing the deploying formation's unit type, and - like any ambush - is a
     * Crisis and never a Turning Point. It is not Essential.
     *
     * @return the ambush, or {@code null} if none could be generated
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static @Nullable StratConScenario placeAmbush(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, AbstractContract contract, Campaign campaign) {
        Formation formation = campaign.getPlayerForce().getFormation(formationId);
        int unitType = (formation == null) ? MEK : formation.getPrimaryUnitType(campaign);

        // With no ambush template for the unit type, a random scenario springs the trap instead.
        ScenarioTemplate template = StratConScenarioFactory.getRandomScenario(unitType, true, false);

        // Facilities are ignored: vulnerable infrastructure occupies its hex, so none can share it.
        StratConScenario ambush = StratConRulesManager.setupScenario(pointOfInterest.getCoords(),
              null,
              campaign,
              contract,
              track,
              template,
              true,
              null);
        if (ambush == null) {
            return null;
        }

        ambush.getBackingScenario().setIsCrisis(true);
        ambush.setTurningPoint(false);
        track.addScenario(ambush);
        StratConPointOfInterestRules.linkScenario(pointOfInterest, ambush);
        return ambush;
    }

    /**
     * Destroys the infrastructure: resolves it, which meets its objective, takes it off the map, and pays the
     * contract's combat bonus.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void destroyInfrastructure(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        StratConPointOfInterestRules.resolvePointOfInterest(track, pointOfInterest);
        track.removePointOfInterest(pointOfInterest.getId());

        campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE,
              "StratConVulnerableInfrastructureBehavior.destroyed.report",
              pointOfInterest.getDisplayableName(),
              track.getDisplayableName()));
        StratConRulesManager.awardCombatBonus(campaign, contract);
    }
}
