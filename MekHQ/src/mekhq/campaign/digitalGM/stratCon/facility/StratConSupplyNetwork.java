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
package mekhq.campaign.digitalGM.stratCon.facility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;

/**
 * One side's supply routes on one sector: the hexes its supply starts from (the roots), and a link from each hex on a
 * route to the next hex toward a root. Each hex leads on to at most one other, so the routes branch out from the roots
 * like a tree, and the route of a facility is found by following the links from its hex until they run out.
 *
 * <p>This only holds the routes. Laying them, and deciding what blocks them, is {@link StratConFacilitySupply}'s
 * work.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConSupplyNetwork {
    private List<StratConSupplyLink> links;
    private Set<StratConCoords> roots;

    // the next hex from each hex, built from the links on first use; see getNextHexes
    private Map<StratConCoords, StratConCoords> nextHexes;

    public StratConSupplyNetwork() {
        links = new ArrayList<>();
        roots = new HashSet<>();
    }

    /**
     * Used for serialization/deserialization. To change the routes, use {@link #addLink} and the methods that prune
     * them, which keep the lookup by hex correct.
     *
     * @return every link of the network
     *
     * @author Illiani
     * @since 0.51.01
     */
    @XmlElementWrapper(name = "links")
    @XmlElement(name = "link")
    public List<StratConSupplyLink> getLinks() {
        return links;
    }

    public void setLinks(List<StratConSupplyLink> links) {
        this.links = (links == null) ? new ArrayList<>() : links;
        nextHexes = null;
    }

    /**
     * Used for serialization/deserialization. To change the roots, use {@link #replaceRoots}.
     *
     * @return the hexes the side's supply starts from
     *
     * @author Illiani
     * @since 0.51.01
     */
    @XmlElementWrapper(name = "roots")
    @XmlElement(name = "root")
    public Set<StratConCoords> getRoots() {
        return roots;
    }

    public void setRoots(Set<StratConCoords> roots) {
        this.roots = (roots == null) ? new HashSet<>() : roots;
    }

    private Map<StratConCoords, StratConCoords> getNextHexes() {
        if (nextHexes == null) {
            nextHexes = new HashMap<>();
            for (StratConSupplyLink link : links) {
                nextHexes.put(link.getFrom(), link.getTo());
            }
        }
        return nextHexes;
    }

    /**
     * Drops every route and root.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void clear() {
        links.clear();
        roots.clear();
        nextHexes = null;
    }

    /**
     * @param coords a hex
     *
     * @return {@code true} if the side's supply starts from the hex
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isRoot(StratConCoords coords) {
        return roots.contains(coords);
    }

    /**
     * @param coords a hex
     *
     * @return {@code true} if the hex is a root, or a route runs through it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean contains(StratConCoords coords) {
        return roots.contains(coords) || getNextHexes().containsKey(coords) || getNextHexes().containsValue(coords);
    }

    /**
     * @return every hex on the network - roots first, then the hexes of each link in turn - in a repeatable order
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Set<StratConCoords> hexes() {
        Set<StratConCoords> hexes = new LinkedHashSet<>(roots);
        for (StratConSupplyLink link : links) {
            hexes.add(link.getFrom());
            hexes.add(link.getTo());
        }
        return hexes;
    }

    /**
     * @param coords a hex
     *
     * @return the next hex toward a root, or {@code null} if no link leaves the hex
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable StratConCoords nextHex(StratConCoords coords) {
        return getNextHexes().get(coords);
    }

    /**
     * Follows the links from a hex for as long as they go.
     *
     * @param coords a hex
     *
     * @return the hex, then each hex after it in turn; just the hex if no link leaves it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public List<StratConCoords> routeFrom(StratConCoords coords) {
        List<StratConCoords> route = new ArrayList<>();
        Set<StratConCoords> visited = new HashSet<>();
        StratConCoords current = coords;
        // A loop should never form, but a route that ran in circles would never reach a root, so it ends there.
        while ((current != null) && visited.add(current)) {
            route.add(current);
            current = nextHex(current);
        }
        return route;
    }

    /**
     * @param coords a hex
     *
     * @return {@code true} if the links from the hex lead to a root; a root is itself routed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isRouted(StratConCoords coords) {
        List<StratConCoords> route = routeFrom(coords);
        return isRoot(route.get(route.size() - 1));
    }

    /**
     * Links a hex to the next hex toward a root, in place of any link that already left it.
     *
     * @param from the hex
     * @param to   the next hex toward a root
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void addLink(StratConCoords from, StratConCoords to) {
        links.removeIf(link -> from.equals(link.getFrom()));
        links.add(new StratConSupplyLink(from, to));
        nextHexes = null;
    }

    /**
     * Sets the hexes the side's supply starts from. A root leads nowhere, so any link leaving one is dropped.
     *
     * @param newRoots the roots
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void replaceRoots(Collection<StratConCoords> newRoots) {
        roots = new HashSet<>(newRoots);
        links.removeIf(link -> roots.contains(link.getFrom()));
        nextHexes = null;
    }

    /**
     * Takes hexes off the network: drops them as roots, and drops every link into or out of them.
     *
     * @param isRemoved the hexes to take off
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void removeHexes(Predicate<StratConCoords> isRemoved) {
        roots.removeIf(isRemoved);
        links.removeIf(link -> isRemoved.test(link.getFrom()) || isRemoved.test(link.getTo()));
        nextHexes = null;
    }

    /**
     * Keeps only the links on the routes from the given hexes that still reach a root, dropping every other link. The
     * roots are kept.
     *
     * @param endpoints the hexes whose routes to keep, such as those of the side's facilities
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void retainRoutesFrom(Collection<StratConCoords> endpoints) {
        Set<StratConCoords> keptFrom = new HashSet<>();
        for (StratConCoords endpoint : endpoints) {
            List<StratConCoords> route = routeFrom(endpoint);
            if (isRoot(route.get(route.size() - 1))) {
                // Every hex on the route but the root has a link leaving it.
                keptFrom.addAll(route.subList(0, route.size() - 1));
            }
        }

        links.removeIf(link -> !keptFrom.contains(link.getFrom()));
        nextHexes = null;
    }
}
