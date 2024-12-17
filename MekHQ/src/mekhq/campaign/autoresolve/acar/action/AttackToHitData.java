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

package mekhq.campaign.autoresolve.acar.action;

import megamek.common.InGameObject;
import megamek.common.TargetRoll;
import megamek.common.strategicBattleSystems.SBFUnit;
import mekhq.campaign.autoresolve.acar.SimulationContext;
import mekhq.campaign.autoresolve.component.Formation;
import mekhq.utilities.I18n;

import java.util.List;

public class AttackToHitData extends TargetRoll {

    public AttackToHitData(int value, String desc) {
        super(value, desc);
    }

    public static AttackToHitData compileToHit(SimulationContext game, StandardUnitAttack attack) {
        if (!attack.isDataValid(game)) {
            return new AttackToHitData(TargetRoll.IMPOSSIBLE, I18n.t("acar.invalid_attack"));
        }

        var attackingFormation = game.getFormation(attack.getEntityId()).orElseThrow();
        var unit = attackingFormation.getUnits().get(attack.getUnitNumber());
        var toHit = new AttackToHitData(attackingFormation.getSkill(), I18n.t("acar.skill"));

        processCriticalDamage(toHit, attackingFormation, attack);
        processRange(toHit, attack);
        processCombatUnit(toHit, unit);
        processTMM(toHit, game, attack);
        processJUMP(toHit, game, attack);
        processMorale(toHit, game, attack);
        processSecondaryTarget(toHit, game, attack);
        return toHit;
    }

    private static void processCriticalDamage(AttackToHitData toHit, Formation formation, StandardUnitAttack attack) {
        SBFUnit combatUnit = formation.getUnits().get(attack.getUnitNumber());
        if (combatUnit.getTargetingCrits() > 0) {
            toHit.addModifier(combatUnit.getTargetingCrits(), I18n.t("acar.critical_target_damage"));
        }
    }

    private static void processCombatUnit(AttackToHitData toHit, SBFUnit unit) {
        switch (unit.getSkill()) {
            case 7 -> toHit.addModifier(+4, I18n.t("acar.skill_7"));
            case 6 -> toHit.addModifier(+3, I18n.t("acar.skill_6"));
            case 5 -> toHit.addModifier(+2, I18n.t("acar.skill_5"));
            case 4 -> toHit.addModifier(+1, I18n.t("acar.skill_4"));
            case 3 -> toHit.addModifier(0, I18n.t("acar.skill_3"));
            case 2 -> toHit.addModifier(-1, I18n.t("acar.skill_2"));
            case 1 -> toHit.addModifier(-2, I18n.t("acar.skill_1"));
            case 0 -> toHit.addModifier(-3, I18n.t("acar.skill_0"));
            default -> toHit.addModifier(TargetRoll.IMPOSSIBLE, I18n.t("acar.invalid_skill"));
        }
    }

    private static void processRange(AttackToHitData toHit, StandardUnitAttack attack) {
        var range = attack.getRange();
        switch (range) {
            case SHORT -> toHit.addModifier(-1, I18n.t( "acar.short_range"));
            case MEDIUM -> toHit.addModifier(+2, I18n.t("acar.medium_range"));
            case LONG -> toHit.addModifier(+4, I18n.t("acar.long_range"));
            case EXTREME -> toHit.addModifier(TargetRoll.IMPOSSIBLE, I18n.t( "acar.extreme_range"));
        }
    }

    private static void processTMM(AttackToHitData toHit, SimulationContext game, StandardUnitAttack attack) {
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        if (target.getTmm() > 0) {
            toHit.addModifier(target.getTmm(), I18n.t( "acar.TMM"));
        }
    }

    private static void processJUMP(AttackToHitData toHit, SimulationContext game, StandardUnitAttack attack) {
        var attacker = game.getFormation(attack.getEntityId()).orElseThrow();
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        if (attacker.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), I18n.t("acar.attacker_JUMP"));
        }
        if (target.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), I18n.t("acar.target_JUMP"));
        }
    }

    private static void processMorale(AttackToHitData toHit, SimulationContext game, StandardUnitAttack attack) {
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        switch (target.moraleStatus()) {
            case SHAKEN -> toHit.addModifier(1, I18n.t("acar.shaken"));
            case UNSTEADY -> toHit.addModifier(2, I18n.t("acar.unsteady"));
            case BROKEN -> toHit.addModifier(3, I18n.t("acar.broken"));
            case ROUTED -> toHit.addModifier(4, I18n.t("acar.routed"));
        }
    }

    private static void processSecondaryTarget(AttackToHitData toHit, SimulationContext game, StandardUnitAttack attack) {
        var attacker = game.getFormation(attack.getEntityId()).orElseThrow();
        if (targetsOfFormation(attacker, game).size() > 2) {
            toHit.addModifier(TargetRoll.IMPOSSIBLE, I18n.t("acar.more_than_two_targets"));
        } else if (targetsOfFormation(attacker, game).size() == 2) {
            toHit.addModifier(+1, I18n.t("acar.two_targets"));
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
    public static List<Integer> targetsOfFormation(InGameObject unit, SimulationContext game) {
        return game.getActionsVector().stream()
            .filter(a -> a.getEntityId() == unit.getId())
            .filter(AttackAction.class::isInstance)
            .map(AttackAction.class::cast)
            .map(AttackAction::getTargetId)
            .distinct()
            .toList();
    }
}
