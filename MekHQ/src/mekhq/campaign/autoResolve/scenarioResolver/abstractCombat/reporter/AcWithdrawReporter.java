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

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.IGame;
import megamek.common.Roll;
import megamek.common.TargetRoll;
import megamek.common.strategicBattleSystems.*;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;

import java.util.function.Consumer;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class AcWithdrawReporter {

    private final IGame game;
    private final Consumer<AcReportEntry> reportConsumer;

    public AcWithdrawReporter(IGame game, Consumer<AcReportEntry> reportConsumer) {
        this.game = game;
        this.reportConsumer = reportConsumer;
    }

    public void reportStartWithdraw(AcFormation withdrawingFormation, TargetRoll toHitData) {
        // Formation trying to withdraw
        var report = new AcReportEntry(3330).noNL()
            .add(new SBFFormationReportEntry(
                withdrawingFormation.generalName(),
                UIUtil.hexColor(ownerColor(withdrawingFormation, game))
            ).text())
            .add(withdrawingFormation.moraleStatus().name().toLowerCase()).indent();
        reportConsumer.accept(report);

        // To-Hit Value
        reportConsumer.accept(new AcReportEntry(3331).add(toHitData.getValue()).add(toHitData.toString()).indent());
    }

    public void reportWithdrawRoll(AcFormation withdrawingFormation, Roll withdrawRoll) {
        var report = new AcReportEntry(3332).noNL();
        report.add(new SBFPlayerNameReportEntry(game.getPlayer(withdrawingFormation.getOwnerId())).text());
        report.add(new AcRollReportEntry(withdrawRoll).reportText()).indent();
        reportConsumer.accept(report);
    }

    public void reportSuccessfulWithdraw() {
        reportConsumer.accept(new AcPublicReportEntry(3333).indent());
    }

    public void reportFailedWithdraw() {
        reportConsumer.accept(new AcPublicReportEntry(3334).indent());
    }
}
