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
import mekhq.campaign.randomEvents.other.RiotScenario;

/**
 * The behavior of a scheduled parade: a public show of the employer's strength, placed in place of every point of
 * interest on a Retainer contract. Every scheduled parade is a strategic objective.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made - even against a routed enemy, since
 * civil unrest does not answer to the enemy's morale. If no scenario breaks out, the parade is held: its objective is
 * met and the contract's combat bonus is paid, standing in for the Essential scenarios such a contract does not get. If
 * one does, a riot breaks out, and the deploying formation is caught up in a {@value #SCENARIO_TEMPLATE} scenario, set
 * up as a riot's is (see {@link RiotScenario}). A disrupted parade is called off whatever the scenario's result: it
 * leaves the map and its objective is removed, neither met nor failed. (See
 * {@link StratConAmbushPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * <p>Any formation can be caught up in a riot - an aerospace flyover included.</p>
 *
 * <p>A parade not held in time is missed: it expires and its objective fails (its lifespan comes from its definition),
 * except while a riot there is still to be dealt with.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConScheduledParadeBehavior extends StratConAmbushPointOfInterestBehavior {
    /** The behavior ID the scheduled parade definition names. */
    public static final String BEHAVIOR_ID = "scheduledParade";

    /** The type ID of the scheduled parade definition. */
    public static final String TYPE_ID = "ScheduledParade";

    /** The scenario template a disrupted parade is fought as. */
    static final String SCENARIO_TEMPLATE = StratConRiots.SCENARIO_TEMPLATE;

    /**
     * A disrupted parade is fought as every riot is.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConScheduledParadeBehavior() {
        super(BEHAVIOR_ID, SCENARIO_TEMPLATE);
    }

    /**
     * A riot can break out at a parade whatever state the enemy is in.
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
     * A disrupted parade's riot is not an ambush, though the parade shares the ambush rules: it is set up as Civil
     * Disobedience's riot is - not a Crisis, and free to roll as a Turning Point - so the same riot carries the same
     * stakes on either contract.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isScenarioAnAmbush() {
        return false;
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
    protected boolean announceScenario(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          int formationId, AbstractContract contract, Campaign campaign) {
        StratConRiots.announceRiot(pointOfInterest, track, contract, campaign);
        return true;
    }
}
