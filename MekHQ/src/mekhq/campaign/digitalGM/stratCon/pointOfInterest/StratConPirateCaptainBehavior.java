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

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a pirate captain: a pirate band's leader, reported in the area, placed in place of every point of
 * interest on a Pirate Hunting contract. A pirate captain is not a strategic objective itself; the fight to bring one
 * down is.
 *
 * <p>When a ground formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the
 * captain has already slipped away: the point of interest leaves the map, with nothing gained or lost. If one does, the
 * captain stands and fights in a {@value #SCENARIO_TEMPLATE} scenario. That scenario is an Essential scenario - a
 * strategic objective of its own, won by winning it - so winning it pays the contract's combat bonus, as any Essential
 * scenario does; the captain itself pays nothing more. Winning brings the captain down; anything else lets the captain
 * escape, failing the scenario's objective. (See {@link StratConContestedPointOfInterestBehavior} for the rules it
 * shares.)</p>
 *
 * <p>A pirate captain not run down in time moves on: it expires (its lifespan comes from its definition), except while
 * a fight there is still to be had.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConPirateCaptainBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the pirate captain definition names. */
    public static final String BEHAVIOR_ID = "pirateCaptain";

    /** The type ID of the pirate captain definition. */
    public static final String TYPE_ID = "PirateCaptain";

    /** The scenario template a pirate captain is fought in. */
    static final String SCENARIO_TEMPLATE = "Decapitation Strike.json";

    @Override
    protected String getScenarioTemplateName() {
        return SCENARIO_TEMPLATE;
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConPirateCaptainBehavior";
    }

    /**
     * Bringing a captain down pays no combat bonus of its own: the Essential scenario it was fought in pays it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isCombatBonusPaid() {
        return false;
    }

    /**
     * With no scenario breaking out, the captain has already slipped away: the point of interest leaves the map.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onUncontested(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        StratConPointOfInterestRules.withdrawPointOfInterest(track, pointOfInterest);
        addReport(GENERAL, "slippedAway.report", pointOfInterest, track, campaign);
    }

    /**
     * Places the fight with the captain, as an Essential scenario (see {@link #makeEssential}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected @Nullable StratConScenario placeContestingScenario(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, AbstractContract contract, Campaign campaign) {
        StratConScenario scenario = super.placeContestingScenario(pointOfInterest, track, formationId, contract,
              campaign);
        if (scenario != null) {
            makeEssential(scenario, track);
        }
        return scenario;
    }

    /**
     * Makes the fight with a captain an Essential scenario, as a contract's scheduled Essential scenarios are: a
     * strategic objective, never a Turning Point, with a "win the scenario" objective on its hex. Winning it then meets
     * that objective and pays the combat bonus; anything else fails the objective.
     *
     * @param scenario the fight with the captain, already on the map
     * @param track    the sector it sits in
     *
     * @author Illiani
     * @since 0.51.01
     */
    // Package-private rather than private so the rule can be tested without building a scenario from a template.
    static void makeEssential(StratConScenario scenario, StratConTrackState track) {
        scenario.setStrategicObjective(true);
        scenario.setTurningPoint(false);

        StratConStrategicObjective objective = new StratConStrategicObjective();
        objective.setObjectiveCoords(scenario.getCoords());
        objective.setObjectiveType(StrategicObjectiveType.SpecificScenarioVictory);
        objective.setDesiredObjectiveCount(1);
        track.addStrategicObjective(objective);
    }
}
