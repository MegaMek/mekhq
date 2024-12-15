/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.autoresolve.acar.report;

import megamek.common.IGame;
import mekhq.campaign.autoresolve.acar.SimulationContext;

import java.util.Date;
import java.util.function.Consumer;

public class StartingScenarioReporter {

    private final SimulationContext game;
    private final Consumer<PublicReportEntry> reportConsumer;

    public StartingScenarioReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = (SimulationContext) game;
    }

    public void logHeader() {
        reportConsumer.accept(new PublicReportEntry(1230).add(
                "# Starting scenario " + game.getScenario().getName() + " @ " + new Date()
            )
        );
    }
}
