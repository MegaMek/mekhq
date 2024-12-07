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
package mekhq.campaign.autoResolve.scenarioResolver;

import mekhq.campaign.autoResolve.AutoResolveGame;
import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.autoResolve.helper.SetupForces;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.AcsSimpleScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.components.AutoResolveConcludedEvent;
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
            case PRINCESS -> throw new UnsupportedOperationException("Princess method is not run here!");
            case ABSTRACT_COMBAT -> new AcsSimpleScenarioResolver(scenario);
        };
    }

    public abstract AutoResolveConcludedEvent resolveScenario(AutoResolveGame game, SetupForces setupForces);

}
