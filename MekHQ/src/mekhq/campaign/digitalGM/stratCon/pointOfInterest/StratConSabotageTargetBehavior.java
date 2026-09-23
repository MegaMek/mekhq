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

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConEscalation;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a sabotage target: an enemy asset - a fuel dump, a supply depot, a communications relay - for the
 * player to sabotage, placed in place of every point of interest on a Sabotage contract. Every sabotage target is a
 * strategic objective, and together they replace the contract's Essential scenarios.
 *
 * <p>When a ground formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the
 * target is sabotaged quietly: its objective is met, the contract's combat bonus is paid, and the contract's Escalation
 * rises by 3d6. If one does, the saboteurs are caught in the act and must fight a {@value #SCENARIO_TEMPLATE}
 * scenario: an overall victory still sabotages the target, meeting its objective and paying the bonus, and anything
 * else - a defeat, a draw, or leaving the scenario unplayed - fails it. Being caught raises Escalation by 2d6 whatever
 * the result, so a won fight - with the usual +1d6 for winning - escalates things as much as a quiet success. (See
 * {@link StratConContestedPointOfInterestBehavior} for the rules it shares, and {@link StratConEscalation}.)</p>
 *
 * <p>Sabotage targets are hidden until scouted. One not sabotaged in time is gone: it expires and its objective fails
 * (its lifespan comes from its definition), except while a fight there is still to be had.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConSabotageTargetBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the sabotage target definition names. */
    public static final String BEHAVIOR_ID = "sabotageTarget";

    /** The type ID of the sabotage target definition. */
    public static final String TYPE_ID = "SabotageTarget";

    /** The scenario template fought when saboteurs are caught. */
    static final String SCENARIO_TEMPLATE = "Covert Strike.json";

    @Override
    protected String getScenarioTemplateName() {
        return SCENARIO_TEMPLATE;
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConSabotageTargetBehavior";
    }

    /**
     * With no scenario breaking out, the target is sabotaged quietly, raising Escalation by 3d6.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onNoScenario(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        securePointOfInterest(pointOfInterest, track, contract, campaign);
        StratConEscalation.onTargetSabotaged(campaign, contract);
    }

    /**
     * The fight after the saboteurs were caught has ended: a win sabotages the target, anything else fails it, and
     * either way Escalation rises by 2d6.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
        super.onLinkedScenarioEnded(pointOfInterest, track, isVictory, campaign);

        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        if (contract != null) {
            StratConEscalation.onSaboteursCaught(campaign, contract);
        }
    }
}
