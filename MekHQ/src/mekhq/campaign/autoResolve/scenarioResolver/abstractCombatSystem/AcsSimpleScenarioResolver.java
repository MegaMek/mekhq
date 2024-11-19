package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem;

import megamek.common.BTObject;
import megamek.common.IGame;
import megamek.common.actions.sbf.SBFStandardUnitAttack;
import megamek.common.alphaStrike.ASRange;
import megamek.common.enums.GamePhase;
import megamek.common.preference.PreferenceManager;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFToHitData;
import mekhq.campaign.autoResolve.helper.AutoResolveGame;
import mekhq.campaign.autoResolve.helper.SetupForces;
import mekhq.campaign.autoResolve.scenarioResolver.ScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsStandardUnitAttack;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsToHitData;
import mekhq.campaign.autoResolve.scenarioResolver.components.AutoResolveConcludedEvent;
import mekhq.campaign.autoResolve.scenarioResolver.components.HtmlGameLogger;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.ScenarioObjective;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static megamek.common.enums.GamePhase.*;

public class AcsSimpleScenarioResolver extends ScenarioResolver {

    private final HtmlGameLogger gameLogger = HtmlGameLogger
        .create(PreferenceManager.getClientPreferences().getGameLogFilename())
        .printToConsole();

    private final List<String> forceMustBePreserved = new ArrayList<>();

    private final AcsGameManager gameManager = new AcsGameManager();
    private AutoResolveGame game;

    public AcsSimpleScenarioResolver(AtBScenario scenario) {
        super(scenario);
    }

    private void initializeState(AutoResolveGame game) {
        this.game = game;
        gameManager.setGame(this.game);

        new SetupForces(game.getCampaign(), game.getUnits(), game.getScenario(), game).createForcesOnGame();

        forceMustBePreserved.clear();
        scenario.getScenarioObjectives().forEach(objective -> {
            if (objective.getObjectiveCriterion().equals(ScenarioObjective.ObjectiveCriterion.Preserve)) {
                forceMustBePreserved.addAll(objective.getAssociatedForceNames());
            }
        });

        gameManager.addPhaseHandler(new PhaseHandler(gameManager, STARTING_SCENARIO, DEPLOYMENT) {
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

        gameManager.addPhaseHandler(new PhaseHandler(gameManager, DEPLOYMENT, SBF_DETECTION) {
            @Override
            protected void executePhase() {
                // Automatically deploy all formations that are set to deploy this round
                getGameManager().getGame().getActiveFormations().stream()
                    .filter( f-> !f.isDeployed())
                    .filter( f-> f.getDeployRound() == getGameManager().getGame().getCurrentRound())
                    .forEach( f-> f.setDeployed(true));
            }
        });
        gameManager.addPhaseHandler(new PhaseHandler(gameManager, SBF_DETECTION, MOVEMENT) {
            @Override
            protected void executePhase() {
                // There is nothing to detect
            }
        });
        gameManager.addPhaseHandler(new PhaseHandler(gameManager, MOVEMENT, FIRING) {
            @Override
            protected void executePhase() {
                getGameManager().getGame().getNextEligibleFormation()
                for (var formation : getGameManager().getGame().getActiveFormations()) {
                    // Determine if formation wants to:
                    //  standard (normal engagement, only if it was not overruned sucessfully, winner of the rolloff
                    //      decides if the formations are engaged in a combat or not)
                    //  Overrun (short range damage is 2 or more, engagement control roll using the
                    //      size differences between the formation and its target, causes 25%
                    //      short range damage value to target formation, target formation causes 50% medium range
                    //      damage to the acting formation, overrun formation cannot engage this turn, only evade,
                    //      if failing, target formation isnt damaged, but it still damages the overruning formation,
                    //      engagement follows normally after this).
                    //  force engagement (half damage, but opposing formation can't retreat)
                    //  evade (can take one free attack, modifiers agains it are at +1 to hit,
                    //      takes 0.75 damage until next turn, can't attack if won,
                    //      if fail, it receives +2 to hit and all damage it causes is 0.5)
                }
            }
        });
        gameManager.addPhaseHandler(new PhaseHandler(gameManager, FIRING, END) {
            @Override
            protected void executePhase() {
                // TODO do firing

                var allFormations = getGameManager().getGame().getActiveFormations();

                for (var actingFormation : getGameManager().getGame().getActiveFormations()) {
                    var target = selectTarget(allFormations, actingFormation);
                    if (target.isEmpty()) {
                        continue;
                    }

                    for (int i = 0; i < actingFormation.getUnits().size(); i++) {
                        var attack = new AcsStandardUnitAttack(actingFormation.getId(), i, target.get().getId(), ASRange.LONG);
                        var toHitData = AcsToHitData.compileToHit(getGameManager().getGame(), attack);
                        getGameManager().getGame().addAction(attack);
                    }
                }
            }

            private Optional<SBFFormation> selectTarget(List<SBFFormation> allFormations, SBFFormation actingFormation) {
                return allFormations.stream()
                    .filter(f -> f.getOwnerId() != actingFormation.getOwnerId())
                    .filter(f -> f.isGround() == actingFormation.isGround())
                    .collect(toShuffledList()).stream().findAny();
            }

        });
        gameManager.addPhaseHandler(new PhaseHandler(gameManager, END, INITIATIVE) {
            @Override
            protected void executePhase() {
                // TODO do end
                // if one side is victories:
                getGameManager().changePhase(VICTORY);
            }
        });
        gameManager.addPhaseHandler(new PhaseHandler(gameManager, VICTORY) {
            @Override
            protected void executePhase() {
                // setup the victory
            }
        });
    }

    @Override
    public AutoResolveConcludedEvent resolveScenario(AutoResolveGame game) {
        initializeState(game);

        gameManager.runGame();

        return new AutoResolveConcludedEvent(false, game.getGraveyardEntities(), game.inGameTWEntities(), game);
    }

    private static final Collector<?, ?, ?> SHUFFLER = Collectors.collectingAndThen(
        Collectors.toCollection(ArrayList::new),
        list -> {
            Collections.shuffle(list);
            return list;
        }
    );

    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ?, List<T>> toShuffledList() {
        return (Collector<T, ?, List<T>>) SHUFFLER;
    }


}
