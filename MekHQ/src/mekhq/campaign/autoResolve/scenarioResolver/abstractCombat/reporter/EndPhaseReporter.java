/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.reporter;

import megamek.common.Entity;
import megamek.common.IEntityRemovalConditions;
import megamek.common.IGame;

import java.util.Map;
import java.util.function.Consumer;

public class EndPhaseReporter {

    private final IGame game;
    private final Consumer<AcReportEntry> reportConsumer;
    private static final Map<Integer, Integer> unitDestroyedMessageMap = Map.of(
        IEntityRemovalConditions.REMOVE_DEVASTATED, 3337,
        IEntityRemovalConditions.REMOVE_EJECTED, 3338,
        IEntityRemovalConditions.REMOVE_PUSHED, 3339,
        IEntityRemovalConditions.REMOVE_CAPTURED, 3340,
        IEntityRemovalConditions.REMOVE_IN_RETREAT, 3341,
        IEntityRemovalConditions.REMOVE_NEVER_JOINED, 3342,
        IEntityRemovalConditions.REMOVE_SALVAGEABLE, 3343);

    private static final int MSG_ID_UNIT_DESTROYED_UNKNOWINGLY = 3344;

    public EndPhaseReporter(IGame game, Consumer<AcReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public void endPhaseHeader() {
        reportConsumer.accept(new AcPublicReportEntry(999));
        reportConsumer.accept(new AcPublicReportEntry(3299));
    }

    public void reportUnitDestroyed(Entity entity) {
        var crewMessageId = entity.getCrew().isDead() ? 3335 : 3336;
        var removalCondition = entity.getRemovalCondition();
        var messageId = unitDestroyedMessageMap.getOrDefault(removalCondition, MSG_ID_UNIT_DESTROYED_UNKNOWINGLY);

        reportConsumer.accept(new AcPublicReportEntry(messageId)
                .add(new AcEntityNameReportEntry(entity).reportText())
                .add(new AcPublicReportEntry(crewMessageId)
                    .add(entity.getCrew().getName())
                    .add(entity.getCrew().getHits())
                    .reportText())
        );
    }
}
