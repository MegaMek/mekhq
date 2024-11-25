package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.MapSettings;
import megamek.common.Roll;
import megamek.common.actions.EntityAction;
import megamek.common.alphaStrike.ASRange;
import megamek.common.preference.PreferenceManager;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.server.ServerBoardHelper;
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
            EngagementControl.FORCED_ENGAGEMENT, 1.0,
            EngagementControl.EVADE, 0.0,
            EngagementControl.STANDARD, 1.0,
            EngagementControl.OVERRUN, 1.0,
            EngagementControl.NONE, 0.2
    );

    private static final WeightedList<EngagementControl> unsteady =  WeightedList.of(
            EngagementControl.FORCED_ENGAGEMENT, 0.5,
            EngagementControl.EVADE, 0.02,
            EngagementControl.STANDARD, 1.0,
            EngagementControl.OVERRUN, 0.1,
            EngagementControl.NONE, 0.01
    );

    private static final WeightedList<EngagementControl> shaken =  WeightedList.of(
            EngagementControl.FORCED_ENGAGEMENT, 0.2,
            EngagementControl.EVADE, 0.1,
            EngagementControl.STANDARD, 0.8,
            EngagementControl.OVERRUN, 0.05,
            EngagementControl.NONE, 0.01
    );

    private static final WeightedList<EngagementControl> broken = WeightedList.of(
            EngagementControl.FORCED_ENGAGEMENT, 0.05,
            EngagementControl.EVADE, 1.0,
            EngagementControl.STANDARD, 0.5,
            EngagementControl.OVERRUN, 0.05,
            EngagementControl.NONE, 0.3
    );

    private static final WeightedList<EngagementControl> routed = WeightedList.of(
            EngagementControl.NONE, 1.0
    );

    private static final Map<AcsFormation.MoraleStatus, WeightedList<EngagementControl>> engagementControlOptions = Map.of(
        AcsFormation.MoraleStatus.NORMAL, normal,
        AcsFormation.MoraleStatus.UNSTEADY, unsteady,
        AcsFormation.MoraleStatus.SHAKEN, shaken,
        AcsFormation.MoraleStatus.BROKEN, broken,
        AcsFormation.MoraleStatus.ROUTED, routed
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
                MapSettings mapSettings = getGameManager().getGame().getMapSettings();
                mapSettings.setBoardsAvailableVector(ServerBoardHelper.scanForBoards(mapSettings));

            }
        });

        gameManager.addPhaseHandler(new PhaseHandler(gameManager, INITIATIVE) {
            @Override
            protected void executePhase() {
                getGameManager().calculatePlayerInitialCounts();
                getGameManager().resetPlayersDone();
                getGameManager().rollInitiative();
                getGameManager().incrementAndSendGameRound();
                getGameManager().getInitiativeHelper().writeInitiativeReport();
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

                    var optTurn = getGameManager().getGame().changeToNextTurn();
                    if (optTurn.isEmpty()) {
                        break;
                    }
                    var turn = optTurn.get();

                    if (turn instanceof AcsFormationTurn formationTurn) {
                        var player = getGameManager().getGame().getPlayer(formationTurn.playerId());
                        getGameManager().getGame().getActiveFormations(player)
                            .stream()
                            .filter(f -> f.isEligibleForPhase(getGameManager().getGame().getPhase())) // only eligible formations
                            .findAny()
                            .map(this::engage) // engage with target
                            .filter(Optional::isPresent) // if it will engage and has a target
                            .map(Optional::get)
                            .ifPresent(this::engagementAndControl); // add engage and control action
                    }
                }
            }

            private Optional<EngagementControlRecord> engage(AcsFormation activeFormation) {
                var target = selectTarget(activeFormation);
                return target.map(sbfFormation -> new EngagementControlRecord(activeFormation, sbfFormation));
            }

            private record EngagementControlRecord(AcsFormation actingFormation, AcsFormation target) { }

            private void engagementAndControl(EngagementControlRecord engagement) {
                var engagementControl = engagementControlOptions.get(engagement.actingFormation.moraleStatus()).sample();
                getGameManager().addEngagementControl(
                    new AcsEngagementControlAction(
                        engagement.actingFormation.getId(),
                        engagement.target.getId(),
                        engagementControl.orElseThrow()),
                    engagement.actingFormation);
            }
        });

        gameManager.addPhaseHandler(new PhaseHandler(gameManager, FIRING) {
            @Override
            protected void executePhase() {
                while (getGameManager().getGame().hasMoreTurns()) {

                    var optTurn = getGameManager().getGame().changeToNextTurn();
                    if (optTurn.isEmpty()) {
                        break;
                    }
                    var turn = optTurn.get();

                    if (turn instanceof AcsFormationTurn formationTurn) {
                        var player = getGameManager().getGame().getPlayer(formationTurn.playerId());
                        getGameManager().getGame().getActiveFormations(player)
                            .stream()
                            .filter(f -> f.isEligibleForPhase(getGameManager().getGame().getPhase())) // only eligible formations
                            .findAny()
                            .map(this::attack) // engage with target
                            .filter(Optional::isPresent) // if it will engage and has a target
                            .map(Optional::get)
                            .ifPresent(this::standardUnitAttacks); // add engage and control action
                    }
                }
            }

            private void standardUnitAttacks(AttackRecord attackRecord) {
                AcsFormation actingFormation = attackRecord.actingFormation;
                var target = attackRecord.target;

                var attacks = new ArrayList<EntityAction>();

                var actingFormationToHit = AcsManueverToHitData.compileToHit(gameManager.getGame(), actingFormation);
                var targetFormationToHit = AcsManueverToHitData.compileToHit(gameManager.getGame(), target);

                ASRange range = actingFormation.getRange(target.getId());

                if (!actingFormation.isRangeSet(target.getId())) {
                    var actingFormationMos = Compute.rollD6(2).getIntValue() - actingFormationToHit.getValue();
                    var targetFormationMos = Compute.rollD6(2).getIntValue() - targetFormationToHit.getValue();

                    if (actingFormationMos > targetFormationMos) {
                        range = WeightedList.of(
                            ASRange.LONG, actingFormation.getStdDamage().L.damage,
                            ASRange.MEDIUM, actingFormation.getStdDamage().M.damage,
                            ASRange.SHORT, actingFormation.getStdDamage().S.damage
                        ).sampleGet();

                    } else if (actingFormationMos < targetFormationMos) {
                        range = WeightedList.of(
                            ASRange.LONG, target.getStdDamage().L.damage,
                            ASRange.MEDIUM, target.getStdDamage().M.damage,
                            ASRange.SHORT, target.getStdDamage().S.damage
                        ).sampleGet();
                    }

                    target.setRange(actingFormation.getId(), range);
                    actingFormation.setRange(target.getId(), range);
                }
                for (int i = 0; i < actingFormation.getUnits().size(); i++) {
                    var attack = new AcsStandardUnitAttack(actingFormation.getId(), i, target.getId(), range);
                    attacks.add(attack);
                }

                getGameManager().addAttack(attacks, actingFormation);
            }

            private record AttackRecord(AcsFormation actingFormation, AcsFormation target) { }

            private Optional<AttackRecord> attack(AcsFormation actingFormation) {
                var target = selectTarget(actingFormation);
                return target.map(sbfFormation -> new AttackRecord(actingFormation, sbfFormation));
            }

        });
        gameManager.addPhaseHandler(new PhaseHandler(gameManager, END) {
            @Override
            protected void executePhase() {
                var recoveringNerves = getGameManager().getGame().getActiveFormations().stream()
                        .filter(f -> f.moraleStatus().ordinal() >= AcsFormation.MoraleStatus.SHAKEN.ordinal())
                        .toList();

                for (var formation : recoveringNerves) {
                    getGameManager().addNerveRecovery(new AcsRecoveringNerveAction(formation.getId()), formation);
                }
            }
        });
    }


    private Optional<AcsFormation> selectTarget(AcsFormation actingFormation) {
        var game = gameManager.getGame();
        var player = game.getPlayer(actingFormation.getOwnerId());
        var targetables = gameManager.getGame().getActiveFormations().stream()
            .filter(f -> actingFormation.getTargetFormationId() == Entity.NONE || f.getId() == actingFormation.getTargetFormationId())
            .filter(SBFFormation::isDeployed)
            .filter(f -> f.isGround() == actingFormation.isGround())
            .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
            .collect(RandomUtils.toShuffledList());
        return targetables.stream().findFirst();
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
