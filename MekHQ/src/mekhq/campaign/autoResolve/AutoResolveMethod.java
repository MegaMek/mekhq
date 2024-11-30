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

import mekhq.campaign.autoResolve.scenarioResolver.ScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.AcsSimpleScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.UnitsMatterSimpleScenarioResolver;
import mekhq.campaign.mission.AtBScenario;

/**
 * @author Luana Coppio
 */
public enum AutoResolveMethod {
    UNITS_MATTER(){
        @Override
        public ScenarioResolver of(AtBScenario scenario) {
            return new UnitsMatterSimpleScenarioResolver(scenario);
        }
    },
    ABSTRACT_COMBAT_SYSTEM() {
        @Override
        public ScenarioResolver of(AtBScenario scenario) {
            return new AcsSimpleScenarioResolver(scenario);
        }
    };

    public static AutoResolveMethod fromString(String method) {
        return switch (method) {
            case "UNITS_MATTER" -> UNITS_MATTER;
            case "ABSTRACT_COMBAT_SYSTEM" -> ABSTRACT_COMBAT_SYSTEM;
            default -> throw new IllegalArgumentException("Invalid method: " + method);
        };
    }

    public abstract ScenarioResolver of(AtBScenario scenario);
}
