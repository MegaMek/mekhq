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
package mekhq.campaign.autoResolve.scenarioResolver;

import mekhq.campaign.autoResolve.AutoResolveGame;
import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.AcsSimpleScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.components.AutoResolveConcludedEvent;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.UnitsMatterSimpleScenarioResolver;
import mekhq.campaign.mission.AtBScenario;

/**
 * @author Luana Coppio
 */
public abstract class ScenarioResolver {

    protected AtBScenario scenario;

    protected ScenarioResolver(AtBScenario scenario) {
        this.scenario = scenario;
    }

    public static ScenarioResolver of(AutoResolveMethod method, AtBScenario scenario) {
        return switch (method) {
            case UNITS_MATTER -> new UnitsMatterSimpleScenarioResolver(scenario);
            case ABSTRACT_COMBAT_SYSTEM -> new AcsSimpleScenarioResolver(scenario);
        };
    }

    public abstract AutoResolveConcludedEvent resolveScenario(AutoResolveGame game);

}
