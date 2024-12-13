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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.phase;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.actions.EntityAction;
import megamek.common.alphaStrike.ASRange;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.util.weightedMaps.WeightedDoubleMap;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsManeuverToHitData;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsStandardUnitAttack;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormationTurn;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.EngagementControl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static mekhq.campaign.autoResolve.helper.RandomUtils.toShuffledList;

public class FiringPhase extends PhaseHandler {

    public FiringPhase(AcGameManager gameManager) {
        super(gameManager, GamePhase.FIRING);
    }

    @Override
    protected void executePhase() {
        while (getGameManager().getGame().hasMoreTurns()) {

            var optTurn = getGameManager().getGame().changeToNextTurn();
            if (optTurn.isEmpty()) {
                break;
            }
            var turn = optTurn.get();

            if (turn instanceof AcFormationTurn formationTurn) {
                var player = getGameManager().getGame().getPlayer(formationTurn.playerId());

                getGameManager().getGame().getActiveFormations(player)
                    .stream()
                    .filter(f -> f.isEligibleForPhase(getGameManager().getGame().getPhase())) // only eligible formations
                    .findAny()
                    .map(this::attack)
                    .stream()
                    .flatMap(Collection::stream)
                    .forEach(this::standardUnitAttacks); // add engage and control action
            }
        }
    }

    private void standardUnitAttacks(AttackRecord attackRecord) {
        AcFormation actingFormation = attackRecord.actingFormation;
        var target = attackRecord.target;

        var attacks = new ArrayList<EntityAction>();

        var actingFormationToHit = AcsManeuverToHitData.compileToHit(actingFormation);
        var targetFormationToHit = AcsManeuverToHitData.compileToHit(target);

        ASRange range = ASRange.LONG;
        AcsStandardUnitAttack.ManeuverResult manueverModifier = AcsStandardUnitAttack.ManeuverResult.DRAW;
        if (!actingFormation.isRangeSet(target.getId())) {
            var actingFormationMos = Compute.rollD6(2).getIntValue() - actingFormationToHit.getValue();
            var targetFormationMos = Compute.rollD6(2).getIntValue() - targetFormationToHit.getValue();

            if (actingFormationMos > targetFormationMos) {
                if (EngagementControl.OVERRUN.equals(actingFormation.getEngagementControl()) && !actingFormation.isEngagementControlFailed()) {
                    range = ASRange.SHORT;
                } else {
                    range = WeightedDoubleMap.of(
                        ASRange.LONG, actingFormation.getStdDamage().L.damage,
                        ASRange.MEDIUM, actingFormation.getStdDamage().M.damage,
                        ASRange.SHORT, actingFormation.getStdDamage().S.damage
                    ).randomItem();

                }
                manueverModifier = AcsStandardUnitAttack.ManeuverResult.SUCCESS;
                target.setRange(actingFormation.getId(), range);
            } else if (actingFormationMos < targetFormationMos) {
                range = WeightedDoubleMap.of(
                    ASRange.LONG, target.getStdDamage().L.damage,
                    ASRange.MEDIUM, target.getStdDamage().M.damage,
                    ASRange.SHORT, target.getStdDamage().S.damage
                ).randomItem();
                manueverModifier = AcsStandardUnitAttack.ManeuverResult.FAILURE;
                target.setRange(actingFormation.getId(), range);
            }

            actingFormation.setRange(target.getId(), range);
        }

        range = actingFormation.getRange(target.getId());

        var maxAttacksOnFailure = Math.max(
            Math.round(actingFormation.getUnits().size() / 2.0),
            Math.min(actingFormation.getUnits().size(), 1));
        var maxAttacksNormally = actingFormation.getUnits().size();

        var maxAttacks = manueverModifier == AcsStandardUnitAttack.ManeuverResult.FAILURE ? maxAttacksOnFailure : maxAttacksNormally;

        for (int i = 0; (i < maxAttacks) && (i < attackRecord.attackingUnits.size()); i++) {
            var unitIndex = attackRecord.attackingUnits.get(i);
            var attack = new AcsStandardUnitAttack(actingFormation.getId(), unitIndex, target.getId(), range, manueverModifier);
            attacks.add(attack);
        }

        getGameManager().addAttack(attacks, actingFormation);
    }

    private record AttackRecord(AcFormation actingFormation, AcFormation target, List<Integer> attackingUnits) { }

    private List<AttackRecord> attack(AcFormation actingFormation) {
        var target = this.selectTarget(actingFormation);

        List<Integer> unitIds = new ArrayList<>();
        for (int i = 0; i < actingFormation.getUnits().size(); i++) {
            unitIds.add(i);
        }

        var ret = new ArrayList<AttackRecord>();

        if (target.size() == 2 && unitIds.size() > 1) {
            var sizeOfGroupA = Compute.randomInt(unitIds.size());
            var sizeOfGroupB = unitIds.size() - sizeOfGroupA;

            List<Integer> shuffledList = unitIds.stream().collect(toShuffledList());

            var groupA = shuffledList.subList(0, sizeOfGroupA);
            var groupB = shuffledList.subList(sizeOfGroupA, unitIds.size());

            if (sizeOfGroupA > 0) {
                ret.add(new AttackRecord(actingFormation, target.get(0), groupA));
            }
            if (sizeOfGroupB > 0) {
                ret.add(new AttackRecord(actingFormation, target.get(1), groupB));
            }
        } else if (target.size() == 1) {
            ret.add(new AttackRecord(actingFormation, target.get(0), unitIds));
        }

        return ret;
    }

    private List<AcFormation> selectTarget(AcFormation actingFormation) {
        var game = getGameManager().getGame();
        var player = game.getPlayer(actingFormation.getOwnerId());
        var canBeTargets = getGameManager().getGame().getActiveFormations().stream()
            .filter(SBFFormation::isDeployed)
//            .filter(f -> f.isGround() == actingFormation.isGround())
            .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
            .filter(f -> f.getId() != actingFormation.getTargetFormationId())
            .filter( f -> !formationWasOverrunByFormation(actingFormation, f))
            .filter(f -> !formationWasEvadedByFormation(actingFormation, f))
            .collect(toShuffledList());

        var mandatoryTarget = game.getFormation(actingFormation.getTargetFormationId());
        mandatoryTarget.ifPresent(formation -> canBeTargets.add(0, formation));

        if (canBeTargets.isEmpty()) {
            return List.of();
        }

        if ((canBeTargets.size() == 1)
            || formationWasForcedByFormation(actingFormation, canBeTargets.get(0))) {
            return List.of(canBeTargets.get(0));
        }

        var targetCount = Compute.d6() < 4 ? 1 : 2;
        return canBeTargets.subList(0, targetCount);
    }

    private boolean formationWasOverrunByFormation(AcFormation actingFormation, AcFormation target) {
        return engagementAndControlResult(actingFormation, target, EngagementControl.OVERRUN);
    }

    private boolean formationWasEvadedByFormation(AcFormation actingFormation, AcFormation target) {
        return engagementAndControlResult(actingFormation, target, EngagementControl.EVADE);
    }

    private boolean formationWasForcedByFormation(AcFormation actingFormation, AcFormation target) {
        return engagementAndControlResult(actingFormation, target, EngagementControl.FORCED_ENGAGEMENT);
    }

    private boolean engagementAndControlResult(AcFormation actingFormation, AcFormation target, EngagementControl engagementControl) {
        var engagementControlMemories = actingFormation.getMemory().getMemories("engagementControl");
        var engagement = engagementControlMemories.stream().filter(f -> f.getOrDefault("targetFormationId", Entity.NONE).equals(target.getId()))
            .findFirst(); // there should be only one engagement control memory for this target
        if (engagement.isEmpty()) {
            return false;
        }

        var isAttacker = (boolean) engagement.get().getOrDefault("attacker", false);
        var lostEngagementControl = (boolean) engagement.get().getOrDefault("wonEngagementControl", false);
        var enc = (EngagementControl) engagement.get().get("engagementControl");
        return enc.equals(engagementControl) && lostEngagementControl && !isAttacker;
    }
}
