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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.handler;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Roll;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.strategicBattleSystems.SBFUnit;
import mekhq.campaign.autoResolve.helper.RandomUtils;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsStandardUnitAttack;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsToHitData;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.EngagementControl;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.reporter.AcAttackReporter;

import java.util.Optional;

/**
 * @author Luana Coppio
 */
public class AcStandardUnitAttackHandler extends AbstractAcActionHandler {

    private final AcAttackReporter reporter;

    public AcStandardUnitAttackHandler(AcsStandardUnitAttack action, AcGameManager gameManager) {
        super(action, gameManager);
        this.reporter = new AcAttackReporter(game(), this::addReport);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isFiring();
    }

    private void execute() {
        AcsStandardUnitAttack attack = (AcsStandardUnitAttack) getAction();
        if (!attack.isDataValid(game())) {
            return;
        }

        var attackerOpt = game().getFormation(attack.getEntityId());
        var targetOpt = game().getFormation(attack.getTargetId());
        if (attackerOpt.isEmpty() || targetOpt.isEmpty()) {
            return;
        }
        var attacker = attackerOpt.get();
        var target = targetOpt.get();

        // Using simplified damage as of Interstellar Operations (BETA) page 241
        var targetUnitOpt = RandomUtils.fastSample(target.getUnits());
        if (targetUnitOpt.isEmpty()) {
            return;
        }
        var targetUnit = targetUnitOpt.get();
        resolveAttack(attacker, attack, target, targetUnit);
    }

    private void resolveAttack(AcFormation attacker, AcsStandardUnitAttack attack, AcFormation target, SBFUnit targetUnit) {
        var attackingUnit = attacker.getUnits().get(attack.getUnitNumber());
        var toHit = AcsToHitData.compileToHit(game(), attack);

        // Start of attack report
        reporter.reportAttackStart(attacker, attack.getUnitNumber(), target);

        if (toHit.cannotSucceed()) {
            reporter.reportCannotSucceed(toHit.getDesc());
        } else {
            reporter.reportToHitValue(toHit);
            Roll roll = Compute.rollD6(2);
            reporter.reportAttackRoll(roll, attacker);

            if (roll.getIntValue() < toHit.getValue()) {
                reporter.reportAttackMiss();
            } else {
                reporter.reportAttackHit();
                int damage = calculateDamage(attacker, attack, attackingUnit, target);

                int newArmor = Math.max(0, targetUnit.getCurrentArmor() - damage);
                reporter.reportDamageDealt(targetUnit, damage, newArmor);

                if (newArmor * 2 <= targetUnit.getCurrentArmor()) {
                    target.setHighStressEpisode(true);
                    reporter.reportStressEpisode();
                }

                targetUnit.setCurrentArmor(newArmor);

                if (target.isCrippled() && newArmor > 0) {
                    reporter.reportStressEpisode();
                    target.setHighStressEpisode(true);
                    reporter.reportUnitCrippled();
                }

                if (newArmor == 0) {
                    // Destroyed
                    reporter.reportUnitDestroyed();
                    target.setHighStressEpisode(true);
                    countKill(attackingUnit, targetUnit);
                } else {
                    // Check for critical hits if armor is now less than half original
                    if (newArmor * 2 < targetUnit.getArmor()) {
                        reporter.reportCriticalCheck();
                        var critRoll = Compute.rollD6(2);
                        var criticalRollResult = critRoll.getIntValue();
                        handleCrits(target, targetUnit, criticalRollResult, attackingUnit);
                    }
                }
            }
        }
    }

    private int calculateDamage(AcFormation attacker, AcsStandardUnitAttack attack,
                                SBFUnit attackingUnit, AcFormation target) {
        int damage = 0;
        if (attack.getManueverResult().equals(AcsStandardUnitAttack.ManeuverResult.SUCCESS)) {
            damage += 1;
        }
        damage += attackingUnit.getCurrentDamage().getDamage(attack.getRange()).damage;

        damage = Math.max(1, processDamageByEngagementControl(attacker, target, damage));
        return damage;
    }

    private void handleCrits(AcFormation target, SBFUnit targetUnit, int criticalRollResult, SBFUnit attackingUnit) {
        if (criticalRollResult <= 4) {
            // No crit
            reporter.reportNoCrit();
        } else if (criticalRollResult <= 7) {
            // targeting critical
            targetUnit.addTargetingCrit();
            reporter.reportTargetingCrit(targetUnit);
        } else if (criticalRollResult <= 9) {
            // Damage crit
            targetUnit.addDamageCrit();
            reporter.reportDamageCrit(targetUnit);
        } else if (criticalRollResult <= 11) {
            // Both targeting and damage crit
            targetUnit.addTargetingCrit();
            targetUnit.addDamageCrit();
            reporter.reportTargetingCrit(targetUnit);
            reporter.reportDamageCrit(targetUnit);
        } else {
            // Destroyed
            countKill(attackingUnit, targetUnit);
            targetUnit.setCurrentArmor(0);
            target.setHighStressEpisode(true);
            reporter.reportUnitDestroyed();
        }
    }

    private void countKill(SBFUnit attackingUnit, SBFUnit targetUnit) {
        var killers = attackingUnit.getElements().stream().map(AlphaStrikeElement::getId)
            .map(e -> gameManager().getGame().getEntity(e)).filter(Optional::isPresent).map(Optional::get).toList();
        var targets = targetUnit.getElements().stream().map(AlphaStrikeElement::getId)
            .map(e -> gameManager().getGame().getEntity(e)).filter(Optional::isPresent).map(Optional::get).toList();
        for (var target : targets) {
            RandomUtils.fastSample(killers).ifPresent(e -> e.addKill(target));
        }
    }

    private int processDamageByEngagementControl(AcFormation attacker, AcFormation target, int damage) {
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
