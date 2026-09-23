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

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.personnel.familiarity.Familiarity;
import mekhq.campaign.personnel.familiarity.FamiliarityGainType;

/**
 * The behavior of training maneuvers: an exercise the player's forces run with the troops they are training, placed in
 * place of every point of interest on a Cadre Duty contract. Every set of training maneuvers is a strategic objective,
 * alongside the contract's Essential scenarios rather than in place of them.
 *
 * <p>When any formation deploys onto their hex, the maneuvers are completed on the spot: their objective is met, and
 * the deploying formation earns chassis familiarity, as a patrol does. No scenario ever breaks out there - not even the
 * usual random one. Completing them pays no combat bonus: the contract's Essential scenarios still pay it.</p>
 *
 * <p>Training maneuvers not held in time lapse: they expire and their objective fails (their lifespan comes from their
 * definition).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConTrainingManeuversBehavior implements IStratConPointOfInterestBehavior {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConRulesManager";

    /** The behavior ID the training maneuvers definition names. */
    public static final String BEHAVIOR_ID = "trainingManeuvers";

    /** The type ID of the training maneuvers definition. */
    public static final String TYPE_ID = "TrainingManeuvers";

    /**
     * Any formation deploying onto the maneuvers completes them: they are resolved, which meets their objective, and
     * leave the map, and the formation earns chassis familiarity. No scenario is rolled.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public PointOfInterestDeploymentOutcome onFormationDeployed(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, Campaign campaign) {
        StratConPointOfInterestRules.resolvePointOfInterest(track, pointOfInterest);
        track.removePointOfInterest(pointOfInterest.getId());

        campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE,
              "StratConTrainingManeuversBehavior.completed.report",
              pointOfInterest.getDisplayableName(),
              track.getDisplayableName()));

        CombatTeam combatTeam = campaign.getPlayerForce().getCombatTeamsAsMap(campaign).get(formationId);
        if (combatTeam != null) {
            Familiarity.assignFamiliarityToCombatTeam(campaign, combatTeam, FamiliarityGainType.D3);
        }

        return PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
    }

    @Override
    public @Nullable String getObjectiveDescription(StratConPointOfInterest pointOfInterest,
          StratConTrackState track) {
        return getTextAt(RESOURCE_BUNDLE, "StratConTrainingManeuversBehavior.objective");
    }
}
