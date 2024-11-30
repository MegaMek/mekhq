package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.phase;

import megamek.common.MapSettings;
import megamek.common.enums.GamePhase;
import megamek.server.ServerBoardHelper;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;

import java.util.HashMap;

public class StartingScenarioPhase extends PhaseHandler {

    public StartingScenarioPhase(AcsGameManager gameManager) {
        super(gameManager, GamePhase.STARTING_SCENARIO);
    }

    @Override
    protected void executePhase() {
        getGameManager().resetPlayersDone();
        getGameManager().resetFormations();
        getGameManager().calculatePlayerInitialCounts();
        getGameManager().getGame().setupTeams();
        getGameManager().getGame().getPlanetaryConditions().determineWind();
        getGameManager().getGame().setupDeployment();
        getGameManager().getGame().setVictoryContext(new HashMap<>());
        MapSettings mapSettings = getGameManager().getGame().getMapSettings();
        mapSettings.setBoardsAvailableVector(ServerBoardHelper.scanForBoards(mapSettings));
    }
}
