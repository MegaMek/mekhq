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
import megamek.common.strategicBattleSystems.SBFUnit;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsToHitData;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;

import java.util.function.Consumer;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class AcAttackReporter {

    private final IGame game;
    private final Consumer<AcReportEntry> reportConsumer;

    public AcAttackReporter(IGame game, Consumer<AcReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }
    public void reportAttackStart(AcFormation attacker, int unitNumber, AcFormation target) {
        var report = new AcReportEntry(2001).noNL();
        report.add(new AcUnitReportEntry(attacker, unitNumber, ownerColor(attacker, game)).text());
        report.add(new AcFormationReportEntry(target, game).text());
        reportConsumer.accept(report);
    }

    public void reportCannotSucceed(String toHitDesc) {
        reportConsumer.accept(new AcReportEntry(2010).add(toHitDesc));
    }

    public void reportToHitValue(AcsToHitData toHitValue) {
        // e.g. "Needed X to hit"
        reportConsumer.accept(new AcReportEntry(2003).indent().add(toHitValue.getValue())
            .add(toHitValue.toString()));
    }

    public void reportAttackRoll(Roll roll, AcFormation attacker) {
        var report = new AcReportEntry(2020).indent();
        report.add(new AcPlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text());
        report.add(new AcRollReportEntry(roll).reportText());
        reportConsumer.accept(report);
    }

    public void reportAttackMiss() {
        reportConsumer.accept(new AcPublicReportEntry(2012).indent(2));
    }

    public void reportAttackHit() {
        reportConsumer.accept(new AcPublicReportEntry(2013).indent(2));
    }

    public void reportDamageDealt(SBFUnit targetUnit, int damage, int newArmor) {
        reportConsumer.accept(new AcPublicReportEntry(3100)
            .add(targetUnit.getName())
            .add(damage)
            .add(newArmor)
            .indent(2));
    }

    public void reportStressEpisode() {
        reportConsumer.accept(new AcPublicReportEntry(3090).indent(3));
    }

    public void reportUnitDestroyed() {
        reportConsumer.accept(new AcPublicReportEntry(3092).indent(3));
    }

    public void reportCriticalCheck() {
        // Called before rolling criticals
        reportConsumer.accept(new AcPublicReportEntry(3095).indent(3));
    }

    public void reportNoCrit() {
        reportConsumer.accept(new AcPublicReportEntry(3097).indent(3));
    }

    public void reportTargetingCrit(SBFUnit targetUnit) {
        reportConsumer.accept(new AcPublicReportEntry(3094)
            .add(targetUnit.getName())
            .add(targetUnit.getTargetingCrits())
            .indent(3));
    }

    public void reportDamageCrit(SBFUnit targetUnit) {
        reportConsumer.accept(new AcPublicReportEntry(3096)
            .add(targetUnit.getName())
            .add(targetUnit.getDamageCrits())
            .indent(3));
    }

    public void reportUnitCrippled() {
        reportConsumer.accept(new AcPublicReportEntry(3091).indent(3));
    }
}
