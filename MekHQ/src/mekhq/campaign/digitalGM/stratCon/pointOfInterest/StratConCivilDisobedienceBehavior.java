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

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;

/**
 * The behavior of civil disobedience: unrest the player's forces must put down before it turns violent, placed in place
 * of every point of interest on a Riot Duty contract. Every outbreak of civil disobedience is a strategic objective.
 * Together they replace the contract's Essential scenarios, and the riots they can turn into replace the old weekly
 * riot event.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made - even against a routed enemy, since civil
 * unrest does not answer to the enemy's morale. If no scenario breaks out, the disobedience is defused: its objective is
 * met and the contract's combat bonus is paid (see {@link #isCombatBonusPaid}). If one does, it has turned into a
 * riot, which breaks out at once with its rioting mobs (see {@link StratConRiots}): an overall victory puts the riot
 * down, meeting the objective and paying the bonus, and anything else - a defeat, a draw, or leaving the riot unplayed -
 * fails it. Either way it then leaves the map. (See {@link StratConContestedPointOfInterestBehavior} for the rules it
 * shares.)</p>
 *
 * <p>Civil disobedience not responded to in time dies down: it expires and its objective fails (its lifespan comes from
 * its definition), except while a riot there is still to be put down.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConCivilDisobedienceBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the civil disobedience definition names. */
    public static final String BEHAVIOR_ID = "civilDisobedience";

    /** The type ID of the civil disobedience definition. */
    public static final String TYPE_ID = "CivilDisobedience";

    /**
     * Civil disobedience turns into a riot, fought as every riot is; with no riot, it is defused.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConCivilDisobedienceBehavior() {
        super(BEHAVIOR_ID, StratConRiots.SCENARIO_TEMPLATE, NoScenarioOutcome.SECURE);
    }

    /**
     * Civil disobedience can turn into a riot whatever state the enemy is in.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isScenarioPossibleWhileRouted() {
        return true;
    }

    /**
     * Riots are fought as "Crowd Control", which is not in the scenario manifest, so it is read from its file (see
     * {@link StratConRiots#loadScenarioTemplate}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected @Nullable ScenarioTemplate loadScenarioTemplate(String templateName) {
        return StratConRiots.loadScenarioTemplate();
    }

    /**
     * A riot breaks out at once.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected Integer getDaysUntilDeployment() {
        return StratConRiots.DAYS_UNTIL_DEPLOYMENT;
    }

    /**
     * Adds the rioting mobs, as every riot has (see {@link StratConRiots#addRiotingMobs}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onScenarioFinalized(StratConScenario scenario, AbstractContract contract, Campaign campaign) {
        StratConRiots.addRiotingMobs(scenario, contract, campaign);
    }

    /**
     * A riot is announced as every riot is (see {@link StratConRiots#announceRiot}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void announceScenario(StratConPointOfInterest pointOfInterest, StratConTrackState track, int formationId,
          AbstractContract contract, Campaign campaign) {
        StratConRiots.announceRiot(pointOfInterest, track, contract, campaign);
    }
}
