package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.phase;

import megamek.common.enums.GamePhase;
import mekhq.campaign.ai.utility.Memory;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsMoraleCheckAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsRecoveringNerveAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsWithdrawAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;

public class EndPhase extends PhaseHandler {

    public EndPhase(AcsGameManager gameManager) {
        super(gameManager, GamePhase.END);
    }

    @Override
    protected void executePhase() {
        checkUnitDestruction();
        checkWithdrawingForces();
        checkMorale();
        checkRecoveringNerves();
        forgetEverything();
    }

    private void checkUnitDestruction() {
        var allFormations = getGameManager().getGame().getActiveFormations();
        for (var formation : allFormations) {
            var destroyedUnits = formation.getUnits().stream()
                .filter(u -> u.getCurrentArmor() <= 0)
                .toList();
            if (!destroyedUnits.isEmpty()) {
                getGameManager().getGame().destroyUnits(formation, destroyedUnits);
            }
        }
    }

    private void checkWithdrawingForces() {
        var forcedWithdrawingUnits = getGameManager().getGame().getActiveFormations().stream()
            .filter(f -> f.moraleStatus() == AcsFormation.MoraleStatus.ROUTED || f.isCrippled())
            .toList();

        for (var formation : forcedWithdrawingUnits) {
            getGameManager().addWithdraw(new AcsWithdrawAction(formation.getId()), formation);
        }
    }

    private void checkMorale() {
        var formationNeedsMoraleCheck = getGameManager().getGame().getActiveFormations().stream()
            .filter(AcsFormation::hadHighStressEpisode)
            .filter(f -> f.moraleStatus() != AcsFormation.MoraleStatus.ROUTED)
            .toList();

        for (var formation : formationNeedsMoraleCheck) {
            getGameManager().addMoraleCheck(new AcsMoraleCheckAction(formation.getId()), formation);
        }
    }

    private void checkRecoveringNerves() {
        var recoveringNerves = getGameManager().getGame().getActiveFormations().stream()
            .filter(f -> f.moraleStatus().ordinal() >= AcsFormation.MoraleStatus.SHAKEN.ordinal())
            .toList();

        for (var formation : recoveringNerves) {
            getGameManager().addNerveRecovery(new AcsRecoveringNerveAction(formation.getId()), formation);
        }
    }

    private void forgetEverything() {
        getGameManager().getGame().getActiveFormations().stream().map(AcsFormation::getMemory).forEach(Memory::clear);
    }
}
