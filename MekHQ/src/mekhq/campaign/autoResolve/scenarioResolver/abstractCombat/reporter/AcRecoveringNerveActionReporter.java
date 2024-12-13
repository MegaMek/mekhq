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
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;

import java.util.function.Consumer;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class AcRecoveringNerveActionReporter {

    private final IGame game;
    private final Consumer<AcReportEntry> reportConsumer;

    public AcRecoveringNerveActionReporter(IGame game, Consumer<AcReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public void reportRecoveringNerveStart(AcFormation formation) {
        reportConsumer.accept(new AcReportEntry(4000)
            .add(new AcFormationReportEntry(formation.generalName(), UIUtil.hexColor(ownerColor(formation, game))).text()));
    }

    public void reportToHitValue(int toHitValue) {
        reportConsumer.accept(new AcReportEntry(4001).add(toHitValue).noNL());
    }

    public void reportSuccessRoll(Roll roll) {
        var report = new AcReportEntry(4002).indent().noNL();
        report.add(new AcRollReportEntry(roll).reportText());
        reportConsumer.accept(report);
    }

    public void reportMoraleStatusChange(SBFFormation.MoraleStatus newMoraleStatus) {
        reportConsumer.accept(new AcReportEntry(4003).add(newMoraleStatus.name()));
    }

    public void reportFailureRoll(Roll roll) {
        reportConsumer.accept(new AcReportEntry(4004));
    }
}
