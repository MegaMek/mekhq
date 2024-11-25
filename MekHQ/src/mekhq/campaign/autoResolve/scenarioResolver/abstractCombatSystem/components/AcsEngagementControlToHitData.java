package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.TargetRoll;
import mekhq.campaign.autoResolve.AutoResolveGame;

public class AcsEngagementControlToHitData extends TargetRoll {

    public AcsEngagementControlToHitData(int value, String desc) {
        super(value, desc);
    }

    public static AcsEngagementControlToHitData compileToHit(AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        if (!engagementControl.isIllegal()) {
            return new AcsEngagementControlToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }
        //noinspection OptionalGetWithoutIsPresent
        var attackingFormation = game.getFormation(engagementControl.getEntityId()).get();
        var toHit = new AcsEngagementControlToHitData(attackingFormation.getTactics(), "Tactics");
        processForceAbilities(toHit, game, engagementControl);
        processFormationModifiers(toHit, game, engagementControl);
        processMorale(toHit, game, engagementControl);
        processSupply(toHit, game, engagementControl);
        processFatigue(toHit, game, engagementControl);
        processEngagementAndControlChosen(toHit, game, engagementControl);
        processSizeDifference(toHit, game, engagementControl);
        return toHit;
    }

    private static void processEngagementAndControlChosen(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        switch (engagementControl.getEngagementControl()) {
            case FORCED_ENGAGEMENT:
                toHit.addModifier(-3, "force engagement");
                break;
            case EVADE:
                toHit.addModifier(-3, "evade");
                break;
            case OVERRUN:
                processSizeDifference(toHit, game, engagementControl);
                break;
            default:
                break;
        }
    }

    private static void processFatigue(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        //noinspection OptionalGetWithoutIsPresent
//        var formation = game.getFormation(engagementControl.getEntityId()).get();
//        if (formation.getFatigue()) {
//            toHit.addModifier(formation.getFatigue(), "Fatigue");
//        }
    }

    private static void processSupply(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        //noinspection OptionalGetWithoutIsPresent
        // var formation = game.getFormation(engagementControl.getEntityId()).get(); // use the formation for something
        var formationHasSupply = true;
        if (!formationHasSupply) {
            toHit.addModifier(4, "No Supply");
        }
    }



    private static void processFormationModifiers(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        var formationIsInfantryOnly = false;
        var formationIsVehicleOnly = false;
        var formationIsMercenary = false;
        var formationHasSuperiorCombatDoctrine = false;
        var formationHasFlawedCombatDoctrine = false;
        if (formationIsInfantryOnly) {
            toHit.addModifier(2, "infantry only");
        }
        if (formationIsVehicleOnly) {
            toHit.addModifier(1, "formation is vehicle only");
        }
        if (formationIsMercenary) {
            toHit.addModifier(-1, "formation is mercenary");
        }
        if (formationHasSuperiorCombatDoctrine) {
            toHit.addModifier(-1, "superior combat doctrine");
        }
        if (formationHasFlawedCombatDoctrine) {
            toHit.addModifier(+1, "flawed combat doctrine");
        }
    }

    private static void processForceAbilities(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
//        //noinspection OptionalGetWithoutIsPresent
//        AcsFormation formation = game.getFormation(engagementControl.getEntityId()).get();
//        toHit.addModifier(-1, "Leadership Rating");
//        toHit.addModifier(-1, "Command Skills");
//        switch (formation.getLoayalty()) {
//            case QUESTIONABLE -> toHit.addModifier(-2, "Questionable Loyalty");
//            case RELIABLE -> toHit.addModifier(0, "Reliable Loyalty");
//            case FANATICAL -> toHit.addModifier(2, "Fanatical Loyalty");
//        }
    }

    private static void processSizeDifference(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        //noinspection OptionalGetWithoutIsPresent
        AcsFormation attacker = game.getFormation(engagementControl.getEntityId()).get();
        //noinspection OptionalGetWithoutIsPresent
        AcsFormation target = game.getFormation(engagementControl.getTargetFormationId()).get();
        int sizeDifference = attacker.getSize() - target.getSize();
        toHit.addModifier(sizeDifference, "Overrun - Size Difference");
    }

    private static void processMorale(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        //noinspection OptionalGetWithoutIsPresent
        AcsFormation target = game.getFormation(engagementControl.getTargetFormationId()).get();
        switch (target.moraleStatus()) {
            case SHAKEN -> toHit.addModifier(+1, "shaken");
            case UNSTEADY -> toHit.addModifier(+2, "unsteady");
            case BROKEN -> toHit.addModifier(+3, "broken");
            case ROUTED -> toHit.addModifier(TargetRoll.AUTOMATIC_FAIL, "routed");
        }
    }
}
