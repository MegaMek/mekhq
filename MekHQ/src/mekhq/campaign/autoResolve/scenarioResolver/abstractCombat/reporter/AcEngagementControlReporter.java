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
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.EngagementControl;

import java.util.function.Consumer;


public class AcEngagementControlReporter {

    private final IGame game;
    private final Consumer<AcReportEntry> reportConsumer;

    public AcEngagementControlReporter(IGame game, Consumer<AcReportEntry> reportConsumer) {
        this.game = game;
        this.reportConsumer = reportConsumer;
    }

    public void reportEngagementStart(AcFormation attacker, AcFormation target, EngagementControl control) {
        AcReportEntry report = new AcReportEntry(2200)
            .add(new AcFormationReportEntry(attacker, game).text())
            .add(new AcFormationReportEntry(target, game).text())
            .add(control.name());
        reportConsumer.accept(report);
    }

    public void reportAttackerToHitValue(int toHitValue) {
        reportConsumer.accept(new AcReportEntry(2203).indent().add(toHitValue));
    }

    public void reportAttackerRoll(AcFormation attacker, Roll attackerRoll) {
        AcReportEntry report = new AcReportEntry(2202);
        report.add(new AcPlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text());
        report.add(new AcRollReportEntry(attackerRoll).reportText()).indent();
        reportConsumer.accept(report);
    }

    public void reportDefenderRoll(AcFormation target, Roll defenderRoll) {
        AcReportEntry report = new AcReportEntry(2202).indent();
        report.add(new AcPlayerNameReportEntry(game.getPlayer(target.getOwnerId())).text());
        report.add(new AcRollReportEntry(defenderRoll).reportText());
        reportConsumer.accept(report);
    }

    public void reportAttackerWin(AcFormation attacker) {
        AcReportEntry report = new AcReportEntry(2204).indent()
            .add(new AcPlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text());
        reportConsumer.accept(report);
    }

    public void reportAttackerLose(AcFormation attacker) {
        reportConsumer.accept(new AcPublicReportEntry(2205)
            .add(new AcPlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text())
            .indent());
    }
}
