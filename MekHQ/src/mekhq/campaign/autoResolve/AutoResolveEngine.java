/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package mekhq.campaign.autoResolve;

import mekhq.MekHQ;
import mekhq.campaign.autoResolve.scenarioResolver.components.AutoResolveConcludedEvent;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.unit.Unit;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Luana Coppio
 */
public class AutoResolveEngine {

    private final AutoResolveMethod autoResolveMethod;

    public AutoResolveEngine(AutoResolveMethod method) {
        this.autoResolveMethod = method;
    }

    public void resolveBattle(MekHQ app, List<Unit> units, AtBScenario scenario, Consumer<AutoResolveConcludedEvent> autoResolveConcludedEvent) {
        var scenarioSpecificResolutionResolver = autoResolveMethod.of(scenario);
        var game = new AutoResolveGame(app, units, scenario);
        var result = scenarioSpecificResolutionResolver.resolveScenario(game);
        autoResolveConcludedEvent.accept(result);
    }

}
