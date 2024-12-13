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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.reporter;

import megamek.common.IGame;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;

import java.util.function.Consumer;

/**
 * Reporter for morale checks.
 * Message IDs starting at 4500.
 */
public class AcMoraleReporter {

    private final IGame game;
    private final Consumer<AcReportEntry> reportConsumer;

    public AcMoraleReporter(IGame game, Consumer<AcReportEntry> reportConsumer) {
        this.game = game;
        this.reportConsumer = reportConsumer;
    }

    public void reportMoraleCheckStart(AcFormation formation, int toHitValue) {
        // 4500: Start of morale check
        var startReport = new AcReportEntry(4500)
            .add(new AcFormationReportEntry(formation, game).reportText())
            .add(toHitValue);
        reportConsumer.accept(startReport);
    }

    public void reportMoraleCheckRoll(AcFormation formation, Roll roll) {
        // 4501: Roll result
        var rollReport = new AcReportEntry(4501)
            .add(new AcFormationReportEntry(formation, game).reportText())
            .add(new AcRollReportEntry(roll).reportText());
        reportConsumer.accept(rollReport);
    }

    public void reportMoraleCheckSuccess(AcFormation formation) {
        // 4502: Success - morale does not worsen
        var successReport = new AcReportEntry(4502)
            .add(new AcFormationReportEntry(formation, game).text());
        reportConsumer.accept(successReport);
    }

    public void reportMoraleCheckFailure(AcFormation formation, SBFFormation.MoraleStatus oldStatus, SBFFormation.MoraleStatus newStatus) {
        // 4503: Failure - morale worsens
        var failReport = new AcReportEntry(4503)
            .add(new AcFormationReportEntry(formation, game).text())
            .add(oldStatus.name())
            .add(newStatus.name());

        reportConsumer.accept(failReport);
    }
}
