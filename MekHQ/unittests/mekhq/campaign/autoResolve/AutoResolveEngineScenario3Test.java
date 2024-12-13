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

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

/**
 * @author Luana Coppio
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AutoResolveEngineScenario3Test extends AbstractAutoResolveEngineScenarios {
    /**
     * Those values are used to determine the expected outcome of the auto resolve
     * Because I am running less than 1000 times, they may vary alot, this means that its not uncommon for them to fail
     * randomly
     */
    @Override
    double lowerBoundTeam1() {
        return 0.60;
    }

    @Override
    double upperBoundTeam1() {
        return 0.68;
    }

    @Override
    double lowerBoundTeam2() {
        return 0.24;
    }

    @Override
    double upperBoundTeam2() {
        return 0.30;
    }

    @Override
    double lowerBoundDraw() {
        return 0.07;
    }

    @Override
    double upperBoundDraw() {
        return 0.11;
    }

    @Override
    TeamArrangement getTeamArrangement() {
        return TeamArrangement.SAME_BV;
    }
}
