package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.phase;

import megamek.common.enums.GamePhase;
import megamek.server.victory.VictoryResult;
import mekhq.campaign.autoResolve.damageHandler.DamageHandlerChooser;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter.VictoryPhaseReporter;

public class VictoryPhase extends PhaseHandler {

    private final VictoryPhaseReporter victoryPhaseReporter;

    public VictoryPhase(AcsGameManager gameManager) {
        super(gameManager, GamePhase.VICTORY);
        victoryPhaseReporter = new VictoryPhaseReporter(gameManager.getGame(), gameManager::addReport);
    }

    @Override
    protected void executePhase() {
        checkVictory();
    }

    private void checkVictory() {
        // Nobody is left unscathed
        applyDamageToRemainingUnits(getGameManager());

        victoryPhaseReporter.victoryHeader();
        var game = getGameManager().getGame();
        VictoryResult vr = game.getVictoryResult();
        vr.setVictory(true);
        game.setVictoryTeam(vr.getWinningTeam());
        victoryPhaseReporter.victoryResult(getGameManager());
    }

    private static void applyDamageToRemainingUnits(AcsGameManager gameManager) {
        for (AcsFormation formation : gameManager.getGame().getActiveFormations()) {
            for ( var unit : formation.getUnits()) {
                if (unit.getCurrentArmor() < unit.getArmor()) {
                    for (var element : unit.getElements()) {
                        var entityOpt = gameManager.getGame().getEntity(element.getId());
                        if (entityOpt.isPresent()) {
                            var entity = entityOpt.get();
                            var percent = (double) unit.getCurrentArmor() / unit.getArmor();
                            var crits = Math.min(9, unit.getTargetingCrits() + unit.getMpCrits() + unit.getDamageCrits());
                            percent -= percent * (crits / 11.0);
                            percent = Math.min(0.95, percent);
                            var totalDamage = (int) ((entity.getTotalArmor() + entity.getTotalInternal()) * (1 - percent));
                            DamageHandlerChooser.chooseHandler(entity, DamageHandlerChooser.EntityFinalState.CREW_AND_ENTITY_MUST_SURVIVE)
                                .applyDamageInClusters(totalDamage, 5);
                        }
                    }
                }
            }
        }
    }
}
