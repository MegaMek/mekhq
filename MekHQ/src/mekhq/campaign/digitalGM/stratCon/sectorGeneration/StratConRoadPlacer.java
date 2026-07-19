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

import java.util.*;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;

/**
 * Builds the road network for a StratCon track: connects the sector's cities with roads and branches each network off
 * the map toward its neighbors.
 *
 * <p>Cities are connected by a minimum spanning tree over shortest paths (Prim's). Paths are found with Dijkstra where
 * mountains are passable but expensive, so roads route around relief when a cheaper way exists. A road may bridge a
 * single hex of ocean at a high cost, but never two in a row, so cities across a narrow strait can be joined while wide
 * water still separates networks. Every network that can reach the sector edge also gets a branch to the nearest border
 * land hex, implying the road continues into the neighboring sector; networks boxed in by wide water stay
 * internal.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConRoadPlacer {
    private StratConRoadPlacer() {}

    /** Movement cost to enter an ordinary hex. */
    private static final int NORMAL_COST = 1;
    /** Movement cost to enter a mountainous or volcanic hex - passable, but roads avoid it when they can. */
    private static final int RELIEF_COST = 5;
    /**
     * Movement cost to bridge a single ocean hex - expensive, so roads only span water to reach land they can't
     * otherwise get to, or when the land detour would be very long. Two ocean hexes can never be crossed in a row, so a
     * bridge is always exactly one hex.
     */
    private static final int BRIDGE_COST = 12;

    /** The result of a Dijkstra sweep: least cost to each hex, and the hex each was reached from. */
    private record Pathing(Map<StratConCoords, Integer> distance, Map<StratConCoords, StratConCoords> previous) {}

    private record Node(StratConCoords coords, int distance) {}

    /**
     * Clears and rebuilds the track's road network from its current cities. Safe to call whenever the cities change
     * (e.g. when a GM adds or removes one).
     *
     * @param track the track whose roads to (re)compute
     */
    public static void recalculateRoads(StratConTrackState track) {
        recalculateRoads(track, Collections.emptyList());
    }

    /**
     * Clears and rebuilds the track's road network from its current cities plus the given extra endpoints, connecting
     * them all into one network. The extra endpoints are typically the hexes of facilities belonging to the faction
     * that owns the planet, so those bases read as sitting on the road grid alongside the cities.
     *
     * @param track          the track whose roads to (re)compute
     * @param extraEndpoints additional hexes to fold into the network (impassable ones are ignored)
     */
    public static void recalculateRoads(StratConTrackState track, Collection<StratConCoords> extraEndpoints) {
        Set<StratConCoords> roads = new HashSet<>();
        Set<StratConCoords> roadExits = new HashSet<>();

        Set<StratConCoords> unconnected = new HashSet<>();
        for (StratConCoords city : track.getCities()) {
            if (!isImpassable(track, city)) {
                unconnected.add(city);
            }
        }
        for (StratConCoords endpoint : extraEndpoints) {
            if (!isImpassable(track, endpoint)) {
                unconnected.add(endpoint);
            }
        }

        while (!unconnected.isEmpty()) {
            StratConCoords seed = unconnected.iterator().next();
            unconnected.remove(seed);

            Set<StratConCoords> network = new HashSet<>();
            network.add(seed);
            roads.add(seed);

            // Prim's MST: repeatedly connect the nearest still-unconnected city on this landmass.
            boolean grew = true;
            while (grew) {
                grew = false;
                Pathing pathing = pathfind(track, network);

                StratConCoords nearest = null;
                int nearestCost = Integer.MAX_VALUE;
                for (StratConCoords city : unconnected) {
                    Integer cost = pathing.distance().get(city);
                    if ((cost != null) && (cost < nearestCost)) {
                        nearestCost = cost;
                        nearest = city;
                    }
                }

                if (nearest != null) {
                    List<StratConCoords> path = reconstruct(pathing, nearest);
                    roads.addAll(path);
                    network.addAll(path);
                    unconnected.remove(nearest);
                    grew = true;
                }
            }

            addOffMapBranch(track, network, roads, roadExits);
        }

        track.setRoads(roads);
        track.setRoadExits(roadExits);
    }

    /**
     * Reports the hex directions in which a road leaves the given hex, for a scenario spawned on a road hex. A
     * direction counts when the neighbor in that direction is itself a road hex, or - when the hex is an off-map road
     * exit - when that direction leaves the sector. This is the geometry a launched battle map uses to trace roads onto
     * the board so they line up with the sector road network.
     *
     * @param track  the track whose road network to read
     * @param coords the hex to examine
     *
     * @return the road-carrying directions (0-5), or an empty list if the hex carries no road
     */
    public static List<Integer> roadEntryEdges(StratConTrackState track, StratConCoords coords) {
        List<Integer> edges = new ArrayList<>();
        if (!track.isRoad(coords)) {
            return edges;
        }

        boolean isExit = track.getRoadExits().contains(coords);
        for (int direction = 0; direction < StratConHexGeometry.HEX_DIRECTIONS; direction++) {
            StratConCoords neighbor = coords.translate(direction);
            if (StratConHexGeometry.inBounds(track, neighbor)) {
                if (track.isRoad(neighbor)) {
                    edges.add(direction);
                }
            } else if (isExit) {
                // the road continues off the sector edge in this direction
                edges.add(direction);
            }
        }
        return edges;
    }

    /**
     * Extends the network with a road to the nearest border land hex it can reach, marking that hex as an off-map exit.
     * Does nothing if the network is boxed in by water.
     */
    private static void addOffMapBranch(StratConTrackState track, Set<StratConCoords> network,
          Set<StratConCoords> roads, Set<StratConCoords> roadExits) {
        Pathing pathing = pathfind(track, network);

        StratConCoords nearestBorder = null;
        int nearestCost = Integer.MAX_VALUE;
        for (StratConCoords border : borderLandHexes(track)) {
            Integer cost = pathing.distance().get(border);
            if ((cost != null) && (cost < nearestCost)) {
                nearestCost = cost;
                nearestBorder = border;
            }
        }

        if (nearestBorder != null) {
            roads.addAll(reconstruct(pathing, nearestBorder));
            roadExits.add(nearestBorder);
        }
    }

    /**
     * Multi-source Dijkstra over the passable land of the track, from every hex in {@code sources}.
     */
    private static Pathing pathfind(StratConTrackState track, Set<StratConCoords> sources) {
        Map<StratConCoords, Integer> distance = new HashMap<>();
        Map<StratConCoords, StratConCoords> previous = new HashMap<>();
        Set<StratConCoords> settled = new HashSet<>();
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(Node::distance));

        for (StratConCoords source : sources) {
            if (!isImpassable(track, source)) {
                distance.put(source, 0);
                frontier.add(new Node(source, 0));
            }
        }

        while (!frontier.isEmpty()) {
            Node node = frontier.poll();
            if (!settled.add(node.coords())) {
                continue;
            }

            boolean currentIsOcean = isImpassable(track, node.coords());

            for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, node.coords())) {
                // Roads may bridge a single ocean hex, but never two in a row, so an ocean neighbor is only reachable
                // from land.
                if (isImpassable(track, neighbor) && currentIsOcean) {
                    continue;
                }

                int candidate = node.distance() + terrainCost(track, neighbor);
                if (candidate < distance.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    distance.put(neighbor, candidate);
                    previous.put(neighbor, node.coords());
                    frontier.add(new Node(neighbor, candidate));
                }
            }
        }

        return new Pathing(distance, previous);
    }

    /**
     * Rebuilds the hex path from a source to {@code target} by following the previous-hex links; includes both
     * endpoints.
     */
    private static List<StratConCoords> reconstruct(Pathing pathing, StratConCoords target) {
        List<StratConCoords> path = new ArrayList<>();
        StratConCoords current = target;
        while (current != null) {
            path.add(current);
            current = pathing.previous().get(current);
        }
        return path;
    }

    private static List<StratConCoords> borderLandHexes(StratConTrackState track) {
        int width = track.getWidth();
        int height = track.getHeight();
        List<StratConCoords> border = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean onBorder = (x == 0) || (x == (width - 1)) || (y == 0) || (y == (height - 1));
                if (onBorder) {
                    StratConCoords coords = new StratConCoords(x, y);
                    if (!isImpassable(track, coords)) {
                        border.add(coords);
                    }
                }
            }
        }
        return border;
    }

    private static boolean isImpassable(StratConTrackState track, StratConCoords coords) {
        return StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords));
    }

    private static int terrainCost(StratConTrackState track, StratConCoords coords) {
        String terrain = track.getTerrainTile(coords);
        if (StratConBiomeManifest.isOceanTerrain(terrain)) {
            return BRIDGE_COST;
        }
        boolean relief = StratConBiomeManifest.isMountainTerrain(terrain) ||
                               StratConBiomeManifest.isVolcanicTerrain(terrain);
        return relief ? RELIEF_COST : NORMAL_COST;
    }
}
