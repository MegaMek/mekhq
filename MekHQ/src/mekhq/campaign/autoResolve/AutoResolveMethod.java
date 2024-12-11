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

import mekhq.MekHQ;
import mekhq.campaign.autoResolve.scenarioResolver.ScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.AcsSimpleScenarioResolver;
import mekhq.campaign.mission.AtBScenario;

import java.util.Optional;
import java.util.ResourceBundle;

/**
 * @author Luana Coppio
 */
public enum AutoResolveMethod {
    PRINCESS("AutoResolveMethod.PRINCESS.text", "AutoResolveMethod.PRINCESS.toolTipText") {
        @Override
        public ScenarioResolver of(AtBScenario scenario) {
            throw new UnsupportedOperationException("Princess method not implemented");
        }
    },
    ABSTRACT_COMBAT("AutoResolveMethod.ABSTRACT_COMBAT.text", "AutoResolveMethod.ABSTRACT_COMBAT.toolTipText") {
        @Override
        public ScenarioResolver of(AtBScenario scenario) {
            return new AcsSimpleScenarioResolver(scenario);
        }
    };

    private final String name;
    private final String toolTipText;

    AutoResolveMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AutoResolveMethod",
            MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public String getName() {
        return name;
    }

    public static Optional<AutoResolveMethod> fromInteger(int index) {
        if (index < 0 || index >= values().length) {
            return Optional.empty();
        }
        return Optional.of(values()[index]);
    }

    public static Optional<AutoResolveMethod> fromString(String method) {
        return switch (method) {
            case "PRINCESS" -> Optional.of(PRINCESS);
            case "ABSTRACT_COMBAT_SYSTEM" -> Optional.of(ABSTRACT_COMBAT);
            default -> Optional.empty();
        };
    }

    public abstract ScenarioResolver of(AtBScenario scenario);

    @Override
    public String toString() {
        return name;
    }
}

