package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.phase;

import megamek.common.Entity;
import megamek.common.alphaStrike.ASRange;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.helper.RandomUtils;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsEngagementControlAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsFormationTurn;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.EngagementControl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static mekhq.campaign.autoResolve.helper.RandomUtils.toShuffledList;

public class MovementPhase extends PhaseHandler {

    private static final Map<EngagementControl, Double> normal = Map.of(
        EngagementControl.FORCED_ENGAGEMENT, 1.0,
        EngagementControl.EVADE, 0.0,
        EngagementControl.STANDARD, 1.0,
        EngagementControl.OVERRUN, 0.5,
        EngagementControl.NONE, 0.0
    );

    private static final Map<EngagementControl, Double> unsteady =  Map.of(
        EngagementControl.FORCED_ENGAGEMENT, 0.5,
        EngagementControl.EVADE, 0.02,
        EngagementControl.STANDARD, 1.0,
        EngagementControl.OVERRUN, 0.1,
        EngagementControl.NONE, 0.01
    );

    private static final Map<EngagementControl, Double> shaken =  Map.of(
        EngagementControl.FORCED_ENGAGEMENT, 0.2,
        EngagementControl.EVADE, 0.1,
        EngagementControl.STANDARD, 0.8,
        EngagementControl.OVERRUN, 0.05,
        EngagementControl.NONE, 0.01
    );

    private static final Map<EngagementControl, Double> broken = Map.of(
        EngagementControl.FORCED_ENGAGEMENT, 0.05,
        EngagementControl.EVADE, 1.0,
        EngagementControl.STANDARD, 0.5,
        EngagementControl.OVERRUN, 0.05,
        EngagementControl.NONE, 0.3
    );

    private static final Map<EngagementControl, Double> routed = Map.of(
        EngagementControl.NONE, 1.0
    );

    private static final Map<AcsFormation.MoraleStatus, Map<EngagementControl, Double>> engagementControlOptions = Map.of(
        AcsFormation.MoraleStatus.NORMAL, normal,
        AcsFormation.MoraleStatus.UNSTEADY, unsteady,
        AcsFormation.MoraleStatus.SHAKEN, shaken,
        AcsFormation.MoraleStatus.BROKEN, broken,
        AcsFormation.MoraleStatus.ROUTED, routed
    );

    public MovementPhase(AcsGameManager gameManager) {
        super(gameManager, GamePhase.MOVEMENT);
    }

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
                    .map(this::engage)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .ifPresent(this::engagementAndControl);
            }
        }
    }

    private Optional<EngagementControlRecord> engage(AcsFormation activeFormation) {
        var target = this.selectTarget(activeFormation);
        return target.map(sbfFormation -> new EngagementControlRecord(activeFormation, sbfFormation));
    }

    private record EngagementControlRecord(AcsFormation actingFormation, AcsFormation target) { }

    private void engagementAndControl(EngagementControlRecord engagement) {
        var eco = engagementControlOptions.get(engagement.target.moraleStatus());

        var ecc = new HashMap<>(eco);
        if (engagement.actingFormation.getStdDamage().usesDamage(ASRange.LONG)) {
            ecc.remove(EngagementControl.FORCED_ENGAGEMENT);
        }
        if (engagement.actingFormation.getStdDamage().getDamage(ASRange.SHORT).damage < 2) {
            ecc.remove(EngagementControl.OVERRUN);
        }

        var engagementControlToSelect = RandomUtils.WeightedList.of(ecc);
        var engagementControl = engagementControlToSelect.sample();

        getGameManager().addEngagementControl(
            new AcsEngagementControlAction(
                engagement.actingFormation.getId(),
                engagement.target.getId(),
                engagementControl.orElseThrow()),
            engagement.actingFormation);
    }

    private Optional<AcsFormation> selectTarget(AcsFormation actingFormation) {
        var game = getGameManager().getGame();
        var player = game.getPlayer(actingFormation.getOwnerId());
        var canBeTargets = getGameManager().getGame().getActiveFormations().stream()
            .filter(f -> actingFormation.getTargetFormationId() == Entity.NONE || f.getId() == actingFormation.getTargetFormationId())
            .filter(SBFFormation::isDeployed)
            .filter(f -> f.isGround() == actingFormation.isGround())
            .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
            .collect(toShuffledList());

        if (canBeTargets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(canBeTargets.get(0));
    }
}
