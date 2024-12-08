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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions;

import megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Roll;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.strategicBattleSystems.*;
import mekhq.campaign.autoResolve.helper.RandomUtils;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.EngagementControl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

/**
 * @author Luana Coppio
 */
public class AcsStandardUnitAttackHandler extends AbstractAcsActionHandler {

    public AcsStandardUnitAttackHandler(AcsStandardUnitAttack action, AcsGameManager gameManager) {
        super(action, gameManager);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isFiring();
    }

    private void execute() {
        AcsStandardUnitAttack attack = (AcsStandardUnitAttack) getAction();
        if (attack.isDataValid(game())) {
            var attackerOpt = game().getFormation(attack.getEntityId());
            var targetOpt = game().getFormation(attack.getTargetId());
            if (attackerOpt.isEmpty() || targetOpt.isEmpty()) {
                return;
            }
            var attacker = attackerOpt.get();
            var target = targetOpt.get();

            var attackingUnit = attacker.getUnits().get(attack.getUnitNumber());
            var attackingElements = attackingUnit.getElements();

            // Using simplified damage as of Interstellar Operations (BETA) page 241
            var targetUnitOpt = RandomUtils.fastSample(target.getUnits());
            if (targetUnitOpt.isEmpty()) {
                return;
            }
            var targetUnit = targetUnitOpt.get();
            var toHit = AcsToHitData.compileToHit(game(), attack);

            var report = new SBFReportEntry(2001).noNL();
            report.add(new SBFUnitReportEntry(attacker, attack.getUnitNumber(), ownerColor(attacker, game())).text());
            report.add(new SBFFormationReportEntry(
                target.generalName(), UIUtil.hexColor(SBFInGameObjectTooltip.ownerColor(target, game()))).text());
            addReport(report);

            if (toHit.cannotSucceed()) {
                addReport(new SBFReportEntry(2010).add(toHit.getDesc()));
            } else {
                addReport(new SBFReportEntry(2003).add(toHit.getValue()).noNL());
                Roll roll = Compute.rollD6(2);
                report = new SBFReportEntry(2020).noNL();
                report.add(new SBFPlayerNameReportEntry(game().getPlayer(attacker.getOwnerId())).text());
                report.add(new SBFRollReportEntry(roll).noNL().text());
                addReport(report);

                if (roll.getIntValue() < toHit.getValue()) {
                    addReport(new SBFPublicReportEntry(2012));
                } else {
                    addReport(new SBFPublicReportEntry(2013));
                    int damage = 0;
                    if (attack.getManueverResult().equals(AcsStandardUnitAttack.ManueverResult.SUCCESS)) {
                        damage += 1;
                    }

                    for (var element : attackingElements) {
                        if (element.getStandardDamage().usesDamage(attack.getRange())) {
                            damage += element.getStandardDamage().getDamage(attack.getRange()).damage;
                        }
                    }

                    damage = Math.max(1, processDamageByEngagementControl(attacker, target, damage));

                    int newArmor = Math.max(0, targetUnit.getCurrentArmor() - damage);
                    addReport(new SBFPublicReportEntry(3100)
                        .add(targetUnit.getName())
                        .add(damage)
                        .add(newArmor));

                    if (newArmor * 2 <= targetUnit.getCurrentArmor()) {
                        target.setHighStressEpisode(true);
                    }

                    targetUnit.setCurrentArmor(newArmor);

                    if (target.isCrippled() && newArmor > 0) {
                        addReport(new SBFPublicReportEntry(3091));
                        target.setHighStressEpisode(true);
                    }

                    if (newArmor == 0) {
                        addReport(new SBFPublicReportEntry(3092));
                        target.setHighStressEpisode(true);
                        countKill(attackingUnit, targetUnit);
                    } else {
                        if (newArmor * 2 < targetUnit.getArmor()) {
                            var critRoll = Compute.rollD6(2);
                            addReport(new SBFPublicReportEntry(3092));
                            var criticalRollResult = critRoll.getIntValue();
                            if (criticalRollResult <= 4) {
                                // Nothing happens
                                addReport(new SBFPublicReportEntry(3097));
                            } else if (criticalRollResult <= 7) {
                                // targeting critical
                                targetUnit.addTargetingCrit();
                                addReport(new SBFPublicReportEntry(3094)
                                    .add(targetUnit.getName())
                                    .add(targetUnit.getTargetingCrits()));
                            } else if (criticalRollResult <= 9) {
                                targetUnit.addDamageCrit();
                                addReport(new SBFPublicReportEntry(3096)
                                    .add(targetUnit.getName())
                                    .add(targetUnit.getDamageCrits()));
                            } else if (criticalRollResult <= 11) {
                                targetUnit.addTargetingCrit();
                                targetUnit.addDamageCrit();
                                addReport(new SBFPublicReportEntry(3094)
                                    .add(targetUnit.getName())
                                    .add(targetUnit.getTargetingCrits()));
                                addReport(new SBFPublicReportEntry(3096)
                                    .add(targetUnit.getName())
                                    .add(targetUnit.getDamageCrits()));
                            } else {
                                countKill(attackingUnit, targetUnit);
                                targetUnit.setCurrentArmor(0);
                                target.setHighStressEpisode(true);
                                addReport(new SBFPublicReportEntry(3092));
                            }
                        }
                    }

                }
            }
        }
    }

    private void countKill(SBFUnit attackingUnit, SBFUnit targetUnit) {
        var killers = attackingUnit.getElements().stream().map(AlphaStrikeElement::getId).map(e -> gameManager().getGame().getEntity(e)).filter(Optional::isPresent).map(Optional::get).toList();
        var targets = targetUnit.getElements().stream().map(AlphaStrikeElement::getId).map(e -> gameManager().getGame().getEntity(e)).filter(Optional::isPresent).map(Optional::get).toList();
        for (var target : targets) {
            RandomUtils.fastSample(killers).ifPresent(e -> e.addKill(target));
        }
    }

    private int processDamageByEngagementControl(AcsFormation attacker, AcsFormation target, int damage) {
        var engagementControlMemories = attacker.getMemory().getMemories("engagementControl");
        var engagement = engagementControlMemories.stream().filter(f -> f.getOrDefault("targetFormationId", Entity.NONE).equals(target.getId()))
            .findFirst(); // there should be only one engagement control memory for this target

        if (engagement.isEmpty()) {
            return damage;
        }

        var isAttacker = (boolean) engagement.get().getOrDefault("attacker", false);

        if (!isAttacker) {
            return damage;
        }

        var wonEngagement = (boolean) engagement.get().getOrDefault("wonEngagementControl", false);
        var engagementControl = (EngagementControl) engagement.get().getOrDefault("engagementControl", EngagementControl.NONE);
        if (wonEngagement) {
            return processDamageEngagementControlVictory(engagementControl, damage);
        }
        return processDamageEngagementControlDefeat(engagementControl, damage);
    }

    private int processDamageEngagementControlDefeat(EngagementControl engagementControl, double damage) {
        switch(engagementControl) {
            case NONE:
                // nothing happens
                break;
            case OVERRUN:
                break;
            case STANDARD:
                break;
            case EVADE:
                damage = damage * 0.5;
                break;
            case FORCED_ENGAGEMENT:
                break;
        }
        return (int) damage;
    }

    private int processDamageEngagementControlVictory(EngagementControl engagementControl, double damage) {
        switch(engagementControl) {
            case NONE:
                // nothing happens
                break;
            case OVERRUN:
                damage = damage * 0.25;
                break;
            case STANDARD:
                break;
            case EVADE:
                break;
            case FORCED_ENGAGEMENT:
                damage = damage * 0.5;
                break;
        }
        return (int) damage;
    }

    @Override
    public void handle() {
        execute();
        setFinished();
    }
}
