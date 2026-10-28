/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.digitalGM.stratCon.sectorGeneration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import jakarta.annotation.Nullable;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;

/**
 * Least-cost paths across a StratCon sector: a multi-source Dijkstra over the hex grid, with the cost of each step
 * left to the caller.
 *
 * <p>Roads are laid with it (see {@link StratConRoadPlacer}), and supply routes follow it too, so the two read the
 * ground the same way.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConPathfinder {
    /** What a {@link StepCost} returns for a step that cannot be taken. */
    public static final int IMPASSABLE = -1;

    private StratConPathfinder() {
    }

    /**
     * The cost of a single step between adjacent hexes.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @FunctionalInterface
    public interface StepCost {
        /**
         * @param from the hex the step leaves
         * @param to   the adjacent hex the step enters
         *
         * @return the cost of the step, or {@link #IMPASSABLE} if it cannot be taken
         */
        int cost(StratConCoords from, StratConCoords to);
    }

    /**
     * The result of a sweep: the least cost to each hex reached, and the hex each was reached from.
     *
     * @param distance the least cost to each hex reached; a source costs nothing
     * @param previous the hex each hex was reached from; a source has none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record Pathing(Map<StratConCoords, Integer> distance, Map<StratConCoords, StratConCoords> previous) {
        /**
         * @param target a hex
         *
         * @return {@code true} if the sweep reached the hex
         */
        public boolean reaches(StratConCoords target) {
            return distance.containsKey(target);
        }

        /**
         * @param candidates the hexes to choose between
         *
         * @return the candidate cheapest to reach, the first one met winning a tie; {@code null} if none was reached
         */
        public @Nullable StratConCoords nearest(Iterable<StratConCoords> candidates) {
            StratConCoords nearest = null;
            int nearestCost = Integer.MAX_VALUE;
            for (StratConCoords candidate : candidates) {
                Integer cost = distance.get(candidate);
                if ((cost != null) && (cost < nearestCost)) {
                    nearestCost = cost;
                    nearest = candidate;
                }
            }
            return nearest;
        }

        /**
         * Rebuilds the hex path between {@code target} and the sweep's source by following the previous-hex links;
         * includes both endpoints.
         *
         * <p>The list runs <em>from {@code target} back to the source</em>, because that is the direction the links
         * point.</p>
         *
         * @param target a hex
         *
         * @return the path, or an empty list if the sweep did not reach the hex
         */
        public List<StratConCoords> pathTo(StratConCoords target) {
            List<StratConCoords> path = new ArrayList<>();
            if (!reaches(target)) {
                return path;
            }

            StratConCoords current = target;
            while (current != null) {
                path.add(current);
                current = previous.get(current);
            }
            return path;
        }
    }

    private record Node(StratConCoords coords, int distance) {}

    /**
     * Multi-source Dijkstra from every hex in {@code sources}, each costing nothing to reach. Sources are taken in the
     * collection's order, so a caller that needs the result to be repeatable should pass an ordered collection.
     *
     * @param track    the sector
     * @param sources  the hexes to sweep out from
     * @param stepCost the cost of each step
     *
     * @return the least cost to, and the way back from, every hex reached
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Pathing pathfind(StratConTrackState track, Collection<StratConCoords> sources,
          StepCost stepCost) {
        Map<StratConCoords, Integer> distance = new HashMap<>();
        Map<StratConCoords, StratConCoords> previous = new HashMap<>();
        Set<StratConCoords> settled = new HashSet<>();
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(Node::distance));

        for (StratConCoords source : sources) {
            distance.put(source, 0);
            frontier.add(new Node(source, 0));
        }

        while (!frontier.isEmpty()) {
            Node node = frontier.poll();
            if (!settled.add(node.coords())) {
                continue;
            }

            for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, node.coords())) {
                int step = stepCost.cost(node.coords(), neighbor);
                if (step == IMPASSABLE) {
                    continue;
                }

                int candidate = node.distance() + step;
                if (candidate < distance.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    distance.put(neighbor, candidate);
                    previous.put(neighbor, node.coords());
                    frontier.add(new Node(neighbor, candidate));
                }
            }
        }

        return new Pathing(distance, previous);
    }
}
