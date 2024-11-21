package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.InGameObject;
import megamek.common.TargetRoll;
import megamek.common.TargetRollModifier;
import megamek.common.actions.sbf.SBFAttackAction;
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.AutoResolveGame;

import java.util.List;

public class AcsEngagementControlToHitData extends TargetRoll {

    public AcsEngagementControlToHitData(int value, String desc) {
        super(value, desc);
    }

    public AcsEngagementControlToHitData() { }

    public static AcsEngagementControlToHitData compileToHit(AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        if (!engagementControl.isIllegal()) {
            return new AcsEngagementControlToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation attackingFormation = game.getFormation(engagementControl.getEntityId()).get();
        AcsEngagementControlToHitData toHit = new AcsEngagementControlToHitData(attackingFormation.getSkill(), "Skill");
        if (!processRange(toHit, game, engagementControl)) {
            return toHit;
        }
        processTMM(toHit, game, engagementControl);
        processJUMP(toHit, game, engagementControl);
        processMorale(toHit, game, engagementControl);
        return toHit;
    }

    private static boolean processRange(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation attackingFormation = game.getFormation(engagementControl.getEntityId()).get();
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation target = game.getFormation(engagementControl.getTargetFormationId()).get();
        int range = attackingFormation.getPosition().coords().distance(target.getPosition().coords());
        if (range > 1) {
            toHit.addModifier(new TargetRollModifier(TargetRoll.IMPOSSIBLE, "out of range"));
            return false;
        } else if (range == 1) {
            toHit.addModifier(new TargetRollModifier(3, "long range"));
        }
        return true;
    }

    private static void processTMM(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation target = game.getFormation(engagementControl.getTargetFormationId()).get();
        if (target.getTmm() > 0) {
            toHit.addModifier(target.getTmm(), "TMM");
        }
    }

    private static void processJUMP(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation attacker = game.getFormation(engagementControl.getEntityId()).get();
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation target = game.getFormation(engagementControl.getTargetFormationId()).get();
        if (attacker.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), "attacker JUMP");
        }
        if (target.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), "target JUMP");
        }
    }

    private static void processMorale(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation target = game.getFormation(engagementControl.getTargetFormationId()).get();
        switch (target.moraleStatus()) {
            case SHAKEN -> toHit.addModifier(+1, "shaken");
            case UNSTEADY -> toHit.addModifier(+2, "unsteady");
            case BROKEN -> toHit.addModifier(+3, "broken");
            case ROUTED -> toHit.addModifier(TargetRoll.AUTOMATIC_FAIL, "routed");
        }
    }

    private static void processSecondaryTarget(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation attacker = game.getFormation(engagementControl.getEntityId()).get();
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
            .filter(a -> a instanceof AcsEngagementControlAction)
            .map(a -> ((SBFAttackAction) a).getTargetId())
            .distinct()
            .toList();
    }
}
