package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem;

import megamek.common.actions.EntityAction;
import megamek.common.alphaStrike.ASRange;
import megamek.common.preference.PreferenceManager;
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.AutoResolveGame;
import mekhq.campaign.autoResolve.helper.RandomUtils;
import mekhq.campaign.autoResolve.helper.SetupForces;
import mekhq.campaign.autoResolve.scenarioResolver.ScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.*;
import mekhq.campaign.autoResolve.scenarioResolver.components.AutoResolveConcludedEvent;
import mekhq.campaign.autoResolve.scenarioResolver.components.HtmlGameLogger;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.ScenarioObjective;

import java.util.*;

import static megamek.common.enums.GamePhase.*;
import static mekhq.campaign.autoResolve.helper.RandomUtils.WeightedList;

public class AcsSimpleScenarioResolver extends ScenarioResolver {

    private final HtmlGameLogger gameLogger = HtmlGameLogger
        .create(PreferenceManager.getClientPreferences().getGameLogFilename())
        .printToConsole();

    private final List<String> forceMustBePreserved = new ArrayList<>();

    private final AcsGameManager gameManager = new AcsGameManager();

    private static final WeightedList<EngagementControl> normal = WeightedList.of(
            EngagementControl.FORCE_ENGAGEMENT, 1.0,
            EngagementControl.EVADE, 0.0,
            EngagementControl.STANDARD, 1.0,
            EngagementControl.OVERRUN, 1.0,
            EngagementControl.NONE, 0.2
    );

    private static final WeightedList<EngagementControl> unsteady =  WeightedList.of(
            EngagementControl.FORCE_ENGAGEMENT, 0.5,
            EngagementControl.EVADE, 0.02,
            EngagementControl.STANDARD, 1.0,
            EngagementControl.OVERRUN, 0.1,
            EngagementControl.NONE, 0.01
    );

    private static final WeightedList<EngagementControl> shaken =  WeightedList.of(
            EngagementControl.FORCE_ENGAGEMENT, 0.2,
            EngagementControl.EVADE, 0.1,
            EngagementControl.STANDARD, 0.8,
            EngagementControl.OVERRUN, 0.05,
            EngagementControl.NONE, 0.01
    );

    private static final WeightedList<EngagementControl> broken = WeightedList.of(
            EngagementControl.FORCE_ENGAGEMENT, 0.05,
            EngagementControl.EVADE, 1.0,
            EngagementControl.STANDARD, 0.5,
            EngagementControl.OVERRUN, 0.05,
            EngagementControl.NONE, 0.3
    );

    private static final WeightedList<EngagementControl> routed = WeightedList.of(
            EngagementControl.NONE, 1.0
    );

    private static final Map<SBFFormation.MoraleStatus, WeightedList<EngagementControl>> engagementControlOptions = Map.of(
            SBFFormation.MoraleStatus.NORMAL, normal,
            SBFFormation.MoraleStatus.UNSTEADY, unsteady,
            SBFFormation.MoraleStatus.SHAKEN, shaken,
            SBFFormation.MoraleStatus.BROKEN, broken,
            SBFFormation.MoraleStatus.ROUTED, routed
    );

    public AcsSimpleScenarioResolver(AtBScenario scenario) {
        super(scenario);
    }

    private void initializeState(AutoResolveGame game) {
        gameManager.setGame(game);
        new SetupForces(game.getCampaign(), game.getUnits(), game.getScenario(), game).createForcesOnGame();

        forceMustBePreserved.clear();
        scenario.getScenarioObjectives().forEach(objective -> {
            if (objective.getObjectiveCriterion().equals(ScenarioObjective.ObjectiveCriterion.Preserve)) {
                forceMustBePreserved.addAll(objective.getAssociatedForceNames());
            }
        });

        gameManager.addPhaseHandler(new PhaseHandler(gameManager, STARTING_SCENARIO) {
            @Override
            protected void executePhase() {
                getGameManager().resetPlayersDone();
                getGameManager().calculatePlayerInitialCounts();
                getGameManager().getGame().setupTeams();
                getGameManager().getGame().getPlanetaryConditions().determineWind();
                getGameManager().getGame().setupDeployment();
                getGameManager().getGame().setVictoryContext(new HashMap<>());
            }
        });

        gameManager.addPhaseHandler(new PhaseHandler(gameManager, DEPLOYMENT) {
            @Override
            protected void executePhase() {
                // Automatically deploy all formations that are set to deploy this round
                getGameManager().getGame().getActiveFormations().stream()
                    .filter( f-> !f.isDeployed())
                    .filter( f-> f.getDeployRound() == getGameManager().getGame().getCurrentRound())
                    .forEach( f-> f.setDeployed(true));
            }
        });
        gameManager.addPhaseHandler(new PhaseHandler(gameManager, SBF_DETECTION) {
            @Override
            protected void executePhase() {
                // There is nothing to detect
            }
        });
        gameManager.addPhaseHandler(new PhaseHandler(gameManager, MOVEMENT) {
            @Override
            protected void executePhase() {
                while (getGameManager().getGame().hasMoreTurns()) {
                    var allFormations = getGameManager().getGame().getActiveFormations();

                    while (getGameManager().getGame().hasMoreTurns()) {

                        var turn = getGameManager().getGame().getTurn();

                        if (turn instanceof AcsFormationTurn formationTurn) {
                            var player = getGameManager().getGame().getPlayer(formationTurn.playerId());
                            var actingFormationOptional = allFormations.stream()
                                    .filter(f -> f.getOwnerId() == player.getId())
                                    .filter(f -> !f.isDone())
                                    .findAny();

                            if (actingFormationOptional.isEmpty()) {
                                continue;
                            }
                            var actingFormation = actingFormationOptional.get();

                            var target = selectTarget(allFormations, actingFormation);
                            if (target.isEmpty()) {
                                continue;
                            }

                            var engagementControl = engagementControlOptions.get(actingFormation.moraleStatus()).sample();

                            getGameManager().addEngagementControl(
                                    new AcsEngagementControlAction(
                                            actingFormation.getId(),
                                            target.get().getId(),
                                            engagementControl.orElseThrow()),
                                    actingFormation);
                        }
                    }
                }
            }

            private Optional<SBFFormation> selectTarget(List<SBFFormation> allFormations, SBFFormation actingFormation) {
                return allFormations.stream()
                        .filter(f -> f.getOwnerId() != actingFormation.getOwnerId())
                        .filter(f -> f.isGround() == actingFormation.isGround())
                        .collect(RandomUtils.toShuffledList()).stream().findAny();
            }
        });
        gameManager.addPhaseHandler(new PhaseHandler(gameManager, FIRING) {
            @Override
            protected void executePhase() {
                var allFormations = getGameManager().getGame().getActiveFormations();

                while (getGameManager().getGame().hasMoreTurns()) {

                    var turn = getGameManager().getGame().getTurn();

                    if (turn instanceof AcsFormationTurn formationTurn) {
                        var player = getGameManager().getGame().getPlayer(formationTurn.playerId());
                        var actingFormationOptional = allFormations.stream()
                                .filter(f -> f.getOwnerId() == player.getId())
                                .filter(f -> !f.isDone())
                                .findAny();

                        if (actingFormationOptional.isEmpty()) {
                            continue;
                        }
                        var actingFormation = actingFormationOptional.get();
                        var target = selectTarget(allFormations, actingFormation);
                        if (target.isEmpty()) {
                            continue;
                        }
                        List<EntityAction> attacks = new ArrayList<>();
                        for (int i = 0; i < actingFormation.getUnits().size(); i++) {
                            var attack = new AcsStandardUnitAttack(actingFormation.getId(), i, target.get().getId(), ASRange.LONG);
                            attacks.add(attack);
                        }
                        getGameManager().addAttack(attacks, actingFormation);
                    }
                }
            }

            private Optional<SBFFormation> selectTarget(List<SBFFormation> allFormations, SBFFormation actingFormation) {
                return allFormations.stream()
                    .filter(f -> f.getOwnerId() != actingFormation.getOwnerId())
                    .filter(f -> f.isGround() == actingFormation.isGround())
                    .collect(RandomUtils.toShuffledList()).stream().findAny();
            }

        });
        gameManager.addPhaseHandler(new PhaseHandler(gameManager, END) {
            @Override
            protected void executePhase() {
                var recoveringNerves = getGameManager().getGame().getActiveFormations().stream()
                        .filter(f -> f.moraleStatus().ordinal() >= SBFFormation.MoraleStatus.SHAKEN.ordinal())
                        .toList();

                for (var formation : recoveringNerves) {
                    getGameManager().addNerveRecovery(new AcsRecoveringNerveAction(formation.getId()), formation);
                }
            }
        });
    }

    @Override
    public AutoResolveConcludedEvent resolveScenario(AutoResolveGame game) {
        initializeState(game);

        gameManager.runGame();
        return new AutoResolveConcludedEvent(gameManager.getGame().getVictoryPlayerId() == game.getCampaign().getPlayer().getId(),
                game.getGraveyardEntities(),
                game.inGameTWEntities(),
                game);
    }

}
