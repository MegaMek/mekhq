package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter;

import megamek.common.Entity;
import megamek.common.IEntityRemovalConditions;
import megamek.common.IGame;

import java.util.Map;
import java.util.function.Consumer;

public class EndPhaseReporter {

    private final IGame game;
    private final Consumer<AcsReportEntry> reportConsumer;
    private static final Map<Integer, Integer> unitDestroyedMessageMap = Map.of(
        IEntityRemovalConditions.REMOVE_DEVASTATED, 3337,
        IEntityRemovalConditions.REMOVE_EJECTED, 3338,
        IEntityRemovalConditions.REMOVE_PUSHED, 3339,
        IEntityRemovalConditions.REMOVE_CAPTURED, 3340,
        IEntityRemovalConditions.REMOVE_IN_RETREAT, 3341,
        IEntityRemovalConditions.REMOVE_NEVER_JOINED, 3342,
        IEntityRemovalConditions.REMOVE_SALVAGEABLE, 3343);

    private static final int MSG_ID_UNIT_DESTROYED_UNKNOWINGLY = 3344;

    public EndPhaseReporter(IGame game, Consumer<AcsReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public void endPhaseHeader() {
        reportConsumer.accept(new AcsPublicReportEntry(999));
        reportConsumer.accept(new AcsPublicReportEntry(3299));
    }

    public void reportUnitDestroyed(Entity entity) {
        var crewMessageId = entity.getCrew().isDead() ? 3335 : 3336;
        var removalCondition = entity.getRemovalCondition();
        var messageId = unitDestroyedMessageMap.getOrDefault(removalCondition, MSG_ID_UNIT_DESTROYED_UNKNOWINGLY);

        reportConsumer.accept(new AcsPublicReportEntry(messageId)
                .add(new AcsEntityNameReportEntry(entity).reportText())
                .add(new AcsPublicReportEntry(crewMessageId)
                    .add(entity.getCrew().getName())
                    .add(entity.getCrew().getHits())
                    .reportText())
        );
    }
}
