package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.TargetRoll;
import mekhq.campaign.autoResolve.AutoResolveGame;

public class AcsManueverToHitData extends TargetRoll {

    public AcsManueverToHitData(int value, String desc) {
        super(value, desc);
    }

    public static AcsManueverToHitData compileToHit(AutoResolveGame game, AcsFormation formation) {
        var toHit = new AcsManueverToHitData(formation.getTactics(), "Tactics");
        processFormationModifiers(toHit, game, formation);
        processCombatUnit(toHit, game, formation);
        processFatigue(toHit, game, formation);
        return toHit;
    }

    private static void processFatigue(AcsManueverToHitData toHit, AutoResolveGame game, AcsFormation formation) {
        if (formation.getFatigue() > 0) {
            toHit.addModifier(formation.getFatigue(), "Fatigue");
        }
    }


    private static void processFormationModifiers(AcsManueverToHitData toHit, AutoResolveGame game, AcsFormation formation) {
        if (formation.getEngagementControl() == EngagementControl.FORCED_ENGAGEMENT) {
            toHit.addModifier(1, "Forced Engagement");
        }
        if (formation.isAerospace()) {
            toHit.addModifier(2, "Aerospace Formation");
        }
    }

    private static void processCombatUnit(AcsManueverToHitData toHit, AutoResolveGame game, AcsFormation formation) {
        switch (formation.getSkill()) {
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

        switch (formation.moraleStatus()) {
            case SHAKEN -> toHit.addModifier(+0, "shaken");
            case UNSTEADY -> toHit.addModifier(+1, "unsteady");
            case BROKEN -> toHit.addModifier(+2, "broken");
            case ROUTED -> toHit.addModifier(+2, "routed");
        }
    }

}
