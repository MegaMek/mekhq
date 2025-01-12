/*
 * Copyright (c) 2024-2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package mekhq.campaign.autoresolve;

import megamek.common.Entity;
import megamek.common.OffBoardDirection;
import megamek.common.autoresolve.acar.order.Condition;
import megamek.common.autoresolve.acar.order.Order;
import megamek.common.autoresolve.acar.order.OrderType;
import megamek.common.autoresolve.acar.order.Orders;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.ScenarioObjective;

import java.util.Set;

/**
 * @author Luana Coppio
 */
public class OrderFactory {
    private static final MMLogger logger = MMLogger.create(OrderFactory.class);

    private final Campaign campaign;
    private final Scenario scenario;
    private Orders orders;

    public OrderFactory(Campaign campaign, Scenario scenario) {
        this.campaign = campaign;
        this.scenario = scenario;
    }

    public Orders getOrders() {
        this.orders = new Orders();
        var scenarioObjectives = scenario.getScenarioObjectives();
        for (var objective : scenarioObjectives) {
            createOrderFromObjective(campaign.getPlayer().getId(), objective);
        }
        return orders;
    }

    private void addOrder(Order order) {
        logger.info("Adding order {}", order);
        this.orders.add(order);
    }

    private void createOrderFromObjective(int ownerId, ScenarioObjective objective) {

        switch (objective.getObjectiveCriterion()) {
            case Capture: // no capture, only destroy!
            case Destroy:
                createDestroyOrder(ownerId, objective);
                break;
            case Preserve:
                createPreserveOrder(ownerId, objective);
                break;
            case ReachMapEdge:
                createReachMapEdgeOrder(ownerId, objective);
                break;
            case ForceWithdraw:
                createForceWithdrawOrder(ownerId, objective);
                break;
            case PreventReachMapEdge:
                createPreventReachMapEdgeOrder(ownerId, objective);
                break;
            case Custom:
            default:
                // do nothing for custom bc what can be done?
                break;
        };
    }
    // Make the enemy withdraw
    private void createForceWithdrawOrder(int ownerId, ScenarioObjective objective) {
        var orderBuilder = Order.OrderBuilder.anOrder(ownerId, OrderType.ATTACK_TARGET_NOT_WITHDRAWING)
            .withCondition(Condition.alwaysTrue());
        addOrder(orderBuilder.build());
    }

    private void createReachMapEdgeOrder(int ownerId, ScenarioObjective objective) {
        if (objective.getDestinationEdge() != null && objective.getDestinationEdge() != OffBoardDirection.NONE) {
            var northSide = Set.of(OffBoardDirection.NORTH, OffBoardDirection.EAST);
            var orderType = northSide.contains(objective.getDestinationEdge()) ? OrderType.FLEE_NORTH : OrderType.FLEE_SOUTH;
            var orderBuilder = Order.OrderBuilder.anOrder(ownerId, orderType)
                .withCondition(Condition.alwaysTrue());
            orders.add(orderBuilder.build());
        } else {
            var orderBuilder = Order.OrderBuilder.anOrder(ownerId, scenario.getStartingPos() > 4 ? OrderType.FLEE_NORTH : OrderType.FLEE_SOUTH)
                .withCondition(Condition.alwaysTrue());
            addOrder(orderBuilder.build());
        }
    }

    private void createPreventReachMapEdgeOrder(int ownerId, ScenarioObjective objective) {
        var orderBuilder = Order.OrderBuilder.anOrder(ownerId, OrderType.ATTACK_TARGET_WITHDRAWING)
            .withCondition(Condition.alwaysTrue());
        addOrder(orderBuilder.build());
    }

    private void createPreserveOrder(int ownerId, ScenarioObjective objective) {
        if (!objective.getAssociatedForceNames().isEmpty()) {
            var order = createOrderForPreserveAssociatedForces(ownerId, objective);
            addOrder(order);
        }
        if (!objective.getAssociatedUnitIDs().isEmpty()) {
            var order = createOrderForPreserveAssociatedUnits(ownerId, objective);
            addOrder(order);
        }
    }

    private Order createOrderForPreserveAssociatedForces(int ownerId, ScenarioObjective objective) {
        var orderBuilder = Order.OrderBuilder.anOrder(ownerId, OrderType.WITHDRAW_IF_CONDITION_IS_MET)
            .withCondition(context -> {
                var attackTypeOrders = Set.of(OrderType.ATTACK_TARGET, OrderType.ATTACK_TARGET_NOT_WITHDRAWING, OrderType.ATTACK_TARGET_WITHDRAWING);
                var hasOutstandingAttackOrders = context.getOrders().getOrders(ownerId).stream()
                    .filter(o -> attackTypeOrders.contains(o.getOrderType())).anyMatch(order -> order.isEligible(context));
                int totalUnits = 0;
                var currentUnits = 0;
                for (var forceName : objective.getAssociatedForceNames()) {
                    for (var force : context.getForces().getTopLevelForces()) {
                        if (force.getName().equals(forceName)) {
                            totalUnits += force.getEntities().size();
                            for (var entityId : force.getEntities()) {
                                currentUnits += context.getSelectedEntityCount(entity -> entity.getId() == entityId);
                            }
                        }
                    }
                }

                if (totalUnits == 0) {
                    return true;
                } else {

                    var timeLimit = false;
                    if (objective.getTimeLimitType() != ScenarioObjective.TimeLimitType.None && objective.isTimeLimitAtMost()) {
                        timeLimit = context.getCurrentRound() >= objective.getTimeLimit();
                    }
                    if (!hasOutstandingAttackOrders) {
                        if (objective.getAmountType().equals(ScenarioObjective.ObjectiveAmountType.Fixed)) {
                            return currentUnits < objective.getFixedAmount() || timeLimit;
                        } else {
                            int percent = (int) ((totalUnits / (double) currentUnits) * 100);
                            return percent < objective.getPercentage() || timeLimit;
                        }
                    } else {
                        if (timeLimit) {
                            return context.getCurrentRound() >= objective.getTimeLimit();
                        } else {
                            return true;
                        }
                    }
                }
            });

        return orderBuilder.build();
    }

    private Order createOrderForPreserveAssociatedUnits(int ownerId, ScenarioObjective objective) {
        var orderBuilder = Order.OrderBuilder.anOrder(ownerId, OrderType.WITHDRAW_IF_CONDITION_IS_MET)
            .withCondition(context -> {
                var inGameObjects = context.getInGameObjects();
                var unitIds = objective.getAssociatedUnitIDs();
                var entitiesOfInterest = inGameObjects.stream().filter(Entity.class::isInstance).map(Entity.class::cast)
                    .filter(e -> e.getOwnerId() == ownerId).toList();
                var currentUnits = 0;
                var totalUnits = unitIds.size();
                if (totalUnits == 0) {
                    return true;
                }
                for (var unit : entitiesOfInterest) {
                    if (unitIds.contains(unit.getExternalIdAsString())) {
                        currentUnits++;
                    }
                }
                var timeLimit = false;
                if (objective.getTimeLimitType() != ScenarioObjective.TimeLimitType.None && objective.isTimeLimitAtMost()) {
                    timeLimit = context.getCurrentRound() >= objective.getTimeLimit();
                }
                if (objective.getAmountType().equals(ScenarioObjective.ObjectiveAmountType.Fixed)) {
                    return currentUnits < objective.getFixedAmount() || timeLimit;
                } else {
                    int percent = (int) ((totalUnits / (double) currentUnits) * 100);
                    return percent < objective.getPercentage() || timeLimit;
                }
            });
        return orderBuilder.build();
    }

    private void createDestroyOrder(int ownerId, ScenarioObjective objective) {
        var orderBuilder = Order.OrderBuilder.anOrder(ownerId, OrderType.ATTACK_TARGET);
        if (objective.getPercentage() == 100) {
            orderBuilder.withCondition(Condition.alwaysTrue());
        } else {
            orderBuilder.withCondition(context -> {
                var enemyPlayers = context.getPlayersList().stream().filter(p -> p.isEnemyOf(campaign.getPlayer())).toList();
                var totalUnits = 0;
                var currentUnits = 0;
                for (var player: enemyPlayers) {
                    totalUnits += context.getStartingNumberOfUnits(player.getId());
                    currentUnits += context.getActiveFormations(player).size();
                }

                if (totalUnits == 0) {
                    return true;
                }
                if (objective.getAmountType().equals(ScenarioObjective.ObjectiveAmountType.Fixed)) {
                    return (totalUnits - currentUnits) < objective.getFixedAmount();
                } else {
                    var currentPercent = 100 - (int) (currentUnits / (double) totalUnits);
                    return (objective.getPercentage() - currentPercent) > 0;
                }
            });
        }
        addOrder(orderBuilder.build());
    }
}
