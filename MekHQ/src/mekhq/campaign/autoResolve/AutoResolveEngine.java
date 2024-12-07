/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve;

import megamek.common.options.BasicGameOptions;
import megamek.common.options.GameOptions;
import megamek.common.options.IGameOptions;
import megamek.common.options.StaticGameOptions;
import mekhq.MekHQ;
import mekhq.campaign.autoResolve.helper.SetupForces;
import mekhq.campaign.autoResolve.scenarioResolver.components.AutoResolveConcludedEvent;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.unit.Unit;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Luana Coppio
 */
public class AutoResolveEngine {

    private final AutoResolveMethod autoResolveMethod;

    public AutoResolveEngine(AutoResolveMethod method) {
        this.autoResolveMethod = method;
    }

    /**
     * Resolve the battle using the selected method
     * @param app MekHQ instance
     * @param units List of units from the player
     * @param scenario Scenario to resolve
     * @param autoResolveConcludedEvent Consumer to handle the result of the resolution, usually its a lambda to the method that will
     *                                  handle the result event
     */
    public void resolveBattle(
        MekHQ app, List<Unit> units, AtBScenario scenario, IGameOptions gameOptions,
        Consumer<AutoResolveConcludedEvent> autoResolveConcludedEvent)
    {
        var scenarioSpecificResolutionResolver = autoResolveMethod.of(scenario);

        var game = new AutoResolveGame(scenario, gameOptions);
        var setupForces = new SetupForces(app.getCampaign(), units, scenario);
        var result = scenarioSpecificResolutionResolver.resolveScenario(game, setupForces);

        autoResolveConcludedEvent.accept(result);
    }

}
