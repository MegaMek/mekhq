package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.InGameObject;
import megamek.common.TargetRoll;
import megamek.common.TargetRollModifier;
import megamek.common.actions.sbf.SBFAttackAction;
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.AutoResolveGame;

import java.util.List;

public class AcsRecoveringNerveActionToHitData extends TargetRoll {

    public AcsRecoveringNerveActionToHitData(int value, String desc) {
        super(value, desc);
    }

    public AcsRecoveringNerveActionToHitData() { }

    public static AcsRecoveringNerveActionToHitData compileToHit(AutoResolveGame game, AcsRecoveringNerveAction recoveringNerveAction) {
        if (!recoveringNerveAction.isIllegal()) {
            return new AcsRecoveringNerveActionToHitData(TargetRoll.IMPOSSIBLE, "Invalid nerve recovering");
        }
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation formation = game.getFormation(recoveringNerveAction.getEntityId()).get();
        AcsRecoveringNerveActionToHitData toHit = new AcsRecoveringNerveActionToHitData(formation.getMorale(), "Morale");
        processSkill(toHit, game, formation);
        processCombatModifier(toHit, game, formation);
        return toHit;
    }

    private static void processSkill(AcsRecoveringNerveActionToHitData toHit, AutoResolveGame game, SBFFormation formation) {
        switch (formation.getSkill()) {
            case 7 -> toHit.addModifier(+2, "Wet behind the ears");
            case 6 -> toHit.addModifier(+1, "Really Green");
            case 5 -> toHit.addModifier(0, "Green");
            case 4 -> toHit.addModifier(-1, "Regular");
            case 3 -> toHit.addModifier(-2, "Veteran");
            case 2 -> toHit.addModifier(-3, "Elite");
            case 1 -> toHit.addModifier(-4, "Heroic");
            case 0 -> toHit.addModifier(-5, "Legendary");
            default -> toHit.addModifier(TargetRoll.IMPOSSIBLE, "Invalid skill");
        }
    }

    private static void processCombatModifier(AcsRecoveringNerveActionToHitData toHit, AutoResolveGame game, SBFFormation formation) {
        //
    }

    /**processCombatModifierprocessCombatModifierprocessCombatModifierprocessCombatModifierprocessCombatModifierprocessCombatModifier
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
