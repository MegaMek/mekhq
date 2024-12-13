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
package mekhq.campaign.autoResolve.helper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BalancedConsolidateForces is a helper class that redistribute entities and forces
 * in a way to consolidate then into valid forces to build AcFormations out of them.
 * @author Luana Coppio
 */
public class BalancedConsolidateForces {

    public static final int MAX_ENTITIES_IN_SUB_FORCE = 6;
    public static final int MAX_ENTITIES_IN_TOP_LEVEL_FORCE = 20;

    public record Container(int uid, int teamId, int[] entities, Container[] subs) {
        public boolean isLeaf() {
            return subs.length == 0 && entities.length > 0;
        }

        public boolean isTop() {
            return subs.length > 0 && entities.length == 0;
        }

        public String toString() {
            return "Container(uid=" + uid + ", team=" + teamId + ", ent=" + Arrays.toString(entities) + ", subs=" + Arrays.toString(subs) + ")";
        }
    }
    public record ForceRepresentation(int uid, int teamId, int[] entities, int[] subForces) {
        public boolean isLeaf() {
            return subForces.length == 0 && entities.length > 0;
        }

        public boolean isTop() {
            return subForces.length > 0 && entities.length == 0;
        }
    }

    /**
     * Balances the forces by team, tries to ensure that every team has the same number of top level forces, each within the ACS parameters
     * of a maximum of 20 entities and 4 sub forces. It also aggregates the entities by team instead of keeping segregated by player.
     * See the test cases for examples on how it works.
     * @param forces List of Forces to balance
     * @return List of Trees representing the balanced forces
     */
    public static List<Container> balancedLists(List<ForceRepresentation> forces) {
        Map<Integer, Set<Integer>> entitiesByTeam = new HashMap<>();
        for (ForceRepresentation c : forces) {
            entitiesByTeam.computeIfAbsent(c.teamId(), k -> new HashSet<>()).addAll(Arrays.stream(c.entities()).boxed().toList());
        }

        // Find the number of top-level containers for each team
        Map<Integer, Integer> numOfEntitiesByTeam = entitiesByTeam.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));

        int maxEntities = numOfEntitiesByTeam.values().stream().max(Integer::compareTo).orElse(0);
        int topCount = (int) Math.ceil((double) maxEntities / MAX_ENTITIES_IN_TOP_LEVEL_FORCE);

        Map<Integer, Container> balancedForces = new HashMap<>();

        for (int team : entitiesByTeam.keySet()) {
            createTopLevelForTeam(balancedForces, team, new ArrayList<>(entitiesByTeam.get(team)), topCount);
        }

        return new ArrayList<>(balancedForces.values());
    }

    private static void createTopLevelForTeam(Map<Integer, Container> cmap, int team, List<Integer> allEnt, int topCount) {
        int maxId = cmap.keySet().stream().max(Integer::compareTo).orElse(0) + 1;

        int perTop = (int) Math.min(Math.ceil((double) allEnt.size() / topCount), MAX_ENTITIES_IN_TOP_LEVEL_FORCE);

        int idx = 0;

        for (int i = 0; i < topCount; i++) {
            int end = Math.min(idx + perTop, allEnt.size());
            List<Integer> part = allEnt.subList(idx, end);
            idx = end;
            // split part into sub containers of up to 6 entities
            List<Container> subs = new ArrayList<>();
            int step = Math.min(part.size(), MAX_ENTITIES_IN_SUB_FORCE);
            for (int start = 0; start < part.size(); start += step) {
                var subForceSize = Math.min(part.size(), start + step);
                Container leaf = new Container(
                    maxId++,
                    team,
                    part.subList(start, subForceSize).stream().mapToInt(Integer::intValue).toArray(),
                    new Container[0]);
                subs.add(leaf);
            }

            if (subs.isEmpty()) {
                // no entities? skip creating top-level
                break;
            }

            var containers = new Container[subs.size()];
            for (int k = 0; k < containers.length; k++) {
                containers[k] = subs.get(k);
            }

            Container top = new Container(maxId++, team, new int[0], containers);
            cmap.put(top.uid(), top);
        }
    }

    public static boolean isBalanced(List<BalancedConsolidateForces.Container> postBalanceForces) {
        if (postBalanceForces.isEmpty()) {
            return false;
        }
        Map<Integer, List<BalancedConsolidateForces.Container>> resMap = new HashMap<>();
        for (BalancedConsolidateForces.Container c : postBalanceForces) {
            if (c.isTop()) resMap.computeIfAbsent(c.teamId(), k -> new ArrayList<>()).add(c);
        }

        List<Integer> counts = resMap.values().stream().map(List::size).toList();
        int min = Collections.min(counts), max = Collections.max(counts);
        return max - min <= 1;
    }
}
