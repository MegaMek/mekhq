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
package mekhq.gui.utilities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;

/**
 * A short coloured tag marking which C3 network a unit belongs to, for the force tree.
 *
 * <p>The tree already names a unit's network underneath it, but reading a formation meant comparing
 * network ids line by line. Every unit on one network is given the same colour, so a Level II that has
 * formed a single network reads as one at a glance, and a unit sitting on a different network - or on
 * none - is visible without reading anything.</p>
 *
 * <p>The colour carries no meaning by itself and never replaces the text: which network a unit is on is
 * still written out, so nothing is lost to a reader who cannot separate the colours.</p>
 */
public final class C3NetworkBadge {

    /**
     * Colours for distinguishing one network from another, from Okabe and Ito's palette, which is
     * chosen so that the colours stay distinct for the common forms of colour blindness.
     */
    private static final String[] NETWORK_COLOURS = {
          "#0072B2", // blue
          "#D55E00", // vermillion
          "#009E73", // bluish green
          "#CC79A7", // reddish purple
          "#E69F00", // orange
          "#56B4E9", // sky blue
          "#8C6D1F"  // dark gold, in place of the palette's yellow, which is unreadable on white
    };

    /** Networked hardware that has not been connected to anything. */
    private static final String UNCONNECTED_COLOUR = "#808080";

    /** A C3i or naval C3 network holds six; this many free means nobody else has joined. */
    private static final int FREE_NODES_WHEN_ALONE = 5;

    /**
     * Colour assigned to each network, in the order the networks were first seen.
     *
     * <p>Assigned in order rather than hashed from the network id, so that the first
     * {@link #NETWORK_COLOURS}-many networks are guaranteed to look different from one another. Hashing
     * was simpler and stable between sessions, but let two networks collide onto one colour, which
     * defeats the point of the badge. A campaign with more networks than there are colours reuses them
     * from the start, so the text under the unit remains what settles it.</p>
     */
    private static final Map<String, String> COLOUR_BY_NETWORK = new ConcurrentHashMap<>();

    private C3NetworkBadge() {
    }

    /**
     * Forgets which colour each network was given. For tests; a campaign has no reason to call it, the
     * assignment being stable for as long as the networks are.
     */
    static void resetColourAssignments() {
        COLOUR_BY_NETWORK.clear();
    }

    /**
     * @param networkIdentity what identifies the network two units share
     *
     * @return the colour for that network, assigning the next unused one if it is new
     */
    private static String colourFor(String networkIdentity) {
        return COLOUR_BY_NETWORK.computeIfAbsent(networkIdentity,
              identity -> NETWORK_COLOURS[COLOUR_BY_NETWORK.size() % NETWORK_COLOURS.length]);
    }

    /**
     * The badge for one unit.
     *
     * @param entity the unit to describe, or {@code null}
     *
     * @return an HTML tag such as a coloured {@code [C3i]}, or an empty string when the unit carries no
     *       network equipment at all
     */
    public static String forEntity(@Nullable Entity entity) {
        if (entity == null) {
            return "";
        }
        String label = labelFor(entity);
        if (label == null) {
            return "";
        }
        String networkIdentity = networkIdentityOf(entity);
        String colour = (networkIdentity == null) ? UNCONNECTED_COLOUR : colourFor(networkIdentity);
        return String.format("<font color='%s'><b>[%s]</b></font> ", colour, label);
    }

    /**
     * @return the short name of the unit's network type, or {@code null} if it has none
     */
    private static @Nullable String labelFor(Entity entity) {
        if (entity.hasNavalC3()) {
            return "NC3";
        }
        if (entity.hasC3i()) {
            return "C3i";
        }
        if (entity.hasC3()) {
            return "C3";
        }
        return null;
    }

    /**
     * What decides two units share a colour.
     *
     * <p>For the non-hierarchic networks that is the network id they hold in common. For hierarchic C3
     * it is the master, so a master and its slaves colour alike and a second lance under a different
     * master does not.</p>
     *
     * @return the identity of the network this unit is on, or {@code null} if it is on none
     */
    private static @Nullable String networkIdentityOf(Entity entity) {
        if (entity.hasNavalC3() || entity.hasC3i()) {
            // Alone on its own network is the same as unconnected as far as a reader is concerned.
            if (entity.calculateFreeC3Nodes() >= FREE_NODES_WHEN_ALONE) {
                return null;
            }
            String networkId = entity.getC3NetId();
            return ((networkId == null) || networkId.isBlank()) ? null : networkId;
        }
        if (entity.hasC3()) {
            Entity master = entity.getC3Master();
            if (master == null) {
                return null;
            }
            return "C3-" + master.getC3UUIDAsString();
        }
        return null;
    }
}
