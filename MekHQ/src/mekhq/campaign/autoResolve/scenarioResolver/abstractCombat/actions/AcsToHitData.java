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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions;

import megamek.common.InGameObject;
import megamek.common.TargetRoll;
import megamek.common.strategicBattleSystems.SBFUnit;
import mekhq.campaign.autoResolve.AutoResolveGame;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;

import java.util.List;

/**
 * @author Luana Coppio
 */
public class AcsToHitData extends TargetRoll {

    public AcsToHitData(int value, String desc) {
        super(value, desc);
    }

    public static AcsToHitData compileToHit(AutoResolveGame game, AcsStandardUnitAttack attack) {
        if (!attack.isDataValid(game)) {
            return new AcsToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }

        var attackingFormation = game.getFormation(attack.getEntityId()).orElseThrow();
        var unit = attackingFormation.getUnits().get(attack.getUnitNumber());
        var toHit = new AcsToHitData(attackingFormation.getSkill(), "Skill");

        processCriticalDamage(toHit, attackingFormation, attack);
        processRange(toHit, attack);
        processCombatUnit(toHit, unit);
        processTMM(toHit, game, attack);
        processJUMP(toHit, game, attack);
        processMorale(toHit, game, attack);
        processSecondaryTarget(toHit, game, attack);
        return toHit;
    }

    private static void processCriticalDamage(AcsToHitData toHit, AcFormation formation, AcsStandardUnitAttack attack) {
        SBFUnit combatUnit = formation.getUnits().get(attack.getUnitNumber());
        if (combatUnit.getTargetingCrits() > 0) {
            toHit.addModifier(combatUnit.getTargetingCrits(), "Critical Target Damage");
        }
    }

    private static void processCombatUnit(AcsToHitData toHit, SBFUnit unit) {
        switch (unit.getSkill()) {
            case 7 -> toHit.addModifier(+4, "Wet behind the ears");
            case 6 -> toHit.addModifier(+3, "Really Green");
            case 5 -> toHit.addModifier(+2, "Green");
            case 4 -> toHit.addModifier(+1, "Regular");
            case 3 -> toHit.addModifier(0, "Veteran");
            case 2 -> toHit.addModifier(-1, "Elite");
            case 1 -> toHit.addModifier(-2, "Heroic");
            case 0 -> toHit.addModifier(-3, "Legendary");
            default -> toHit.addModifier(TargetRoll.IMPOSSIBLE, "Invalid skill");
        }
    }

    private static void processRange(AcsToHitData toHit, AcsStandardUnitAttack attack) {
        var range = attack.getRange();
        switch (range) {
            case SHORT -> toHit.addModifier(-1, "short range");
            case MEDIUM -> toHit.addModifier(+2, "medium range");
            case LONG -> toHit.addModifier(+4, "long range");
            case EXTREME -> toHit.addModifier(TargetRoll.IMPOSSIBLE, "extreme range");
        }
    }

    private static void processTMM(AcsToHitData toHit, AutoResolveGame game, AcsStandardUnitAttack attack) {
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        if (target.getTmm() > 0) {
            toHit.addModifier(target.getTmm(), "TMM");
        }
    }

    private static void processJUMP(AcsToHitData toHit, AutoResolveGame game, AcsStandardUnitAttack attack) {
        var attacker = game.getFormation(attack.getEntityId()).orElseThrow();
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        if (attacker.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), "attacker JUMP");
        }
        if (target.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), "target JUMP");
        }
    }

    private static void processMorale(AcsToHitData toHit, AutoResolveGame game, AcsStandardUnitAttack attack) {
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        switch (target.moraleStatus()) {
            case SHAKEN -> toHit.addModifier(1, "shaken");
            case UNSTEADY -> toHit.addModifier(2, "unsteady");
            case BROKEN -> toHit.addModifier(3, "broken");
            case ROUTED -> toHit.addModifier(4, "routed");
        }
    }

    private static void processSecondaryTarget(AcsToHitData toHit, AutoResolveGame game, AcsStandardUnitAttack attack) {
        var attacker = game.getFormation(attack.getEntityId()).orElseThrow();
        if (targetsOfFormation(attacker, game).size() > 2) {
            toHit.addModifier(TargetRoll.IMPOSSIBLE, "too many targets");
        } else if (targetsOfFormation(attacker, game).size() == 2) {
            toHit.addModifier(+1, "two targets");
        }
    }

    /**
     * Returns a list of target IDs of all the targets of all attacks that the attacker of the given
     * attack is performing this round. The result can be empty (the unit isn't attacking anything or
     * it is not the firing phase), it can have one or two entries.
     *
     * @param unit The attacker to check attacks for
     * @param game The game
     * @return A list of all target IDs
     */
    public static List<Integer> targetsOfFormation(InGameObject unit, AutoResolveGame game) {
        return game.getActionsVector().stream()
            .filter(a -> a.getEntityId() == unit.getId())
            .filter(a -> a instanceof AcsAttackAction)
            .map(a -> ((AcsAttackAction) a).getTargetId())
            .distinct()
            .toList();
    }
}
