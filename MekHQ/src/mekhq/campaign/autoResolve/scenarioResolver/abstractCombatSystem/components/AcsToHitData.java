package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.InGameObject;
import megamek.common.TargetRoll;
import megamek.common.TargetRollModifier;
import megamek.common.actions.sbf.SBFAttackAction;
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.helper.AutoResolveGame;

import java.util.List;

public class AcsToHitData extends TargetRoll {

    public AcsToHitData(int value, String desc) {
        super(value, desc);
    }

    public AcsToHitData() { }

    public static AcsToHitData compileToHit(AutoResolveGame game, AcsStandardUnitAttack attack) {
        if (!attack.isDataValid(game)) {
            return new AcsToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation attackingFormation = game.getFormation(attack.getEntityId()).get();
        AcsToHitData toHit = new AcsToHitData(attackingFormation.getSkill(), "Skill");
        if (!processRange(toHit, game, attack)) {
            return toHit;
        }
        processTMM(toHit, game, attack);
        processJUMP(toHit, game, attack);
        processMorale(toHit, game, attack);
        processSecondaryTarget(toHit, game, attack);
        return toHit;
    }

    private static boolean processRange(AcsToHitData toHit, AutoResolveGame game, AcsStandardUnitAttack attack) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation attackingFormation = game.getFormation(attack.getEntityId()).get();
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation target = game.getFormation(attack.getTargetId()).get();
        int range = attackingFormation.getPosition().coords().distance(target.getPosition().coords());
        if (range > 1) {
            toHit.addModifier(new TargetRollModifier(TargetRoll.IMPOSSIBLE, "out of range"));
            return false;
        } else if (range == 1) {
            toHit.addModifier(new TargetRollModifier(3, "long range"));
        }
        return true;
    }

    private static void processTMM(AcsToHitData toHit, AutoResolveGame game, AcsStandardUnitAttack attack) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation target = game.getFormation(attack.getTargetId()).get();
        if (target.getTmm() > 0) {
            toHit.addModifier(target.getTmm(), "TMM");
        }
    }

    private static void processJUMP(AcsToHitData toHit, AutoResolveGame game, AcsStandardUnitAttack attack) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation attacker = game.getFormation(attack.getEntityId()).get();
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation target = game.getFormation(attack.getTargetId()).get();
        if (attacker.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), "attacker JUMP");
        }
        if (target.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), "target JUMP");
        }
    }

    private static void processMorale(AcsToHitData toHit, AutoResolveGame game, AcsStandardUnitAttack attack) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation target = game.getFormation(attack.getTargetId()).get();
        switch (target.moraleStatus()) {
            case SHAKEN -> toHit.addModifier(-1, "shaken");
            case BROKEN -> toHit.addModifier(-2, "broken");
            case ROUTED -> toHit.addModifier(-3, "routed");
        }
    }

    private static void processSecondaryTarget(AcsToHitData toHit, AutoResolveGame game, AcsStandardUnitAttack attack) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation attacker = game.getFormation(attack.getEntityId()).get();
        if (targetsOfFormation(attacker, game).size() > 2) {
            toHit.addModifier(TargetRoll.IMPOSSIBLE, "too many targets");
        } else if (targetsOfFormation(attacker, game).size() == 2) {
            toHit.addModifier(1, "two targets");
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
            .filter(a -> a instanceof SBFAttackAction)
            .map(a -> ((SBFAttackAction) a).getTargetId())
            .distinct()
            .toList();
    }
}
