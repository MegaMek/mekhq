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
package mekhq.campaign.universe.commandGeneration;

import java.util.List;
import java.util.function.IntFunction;

import jakarta.annotation.Nullable;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;

/**
 * How a command groups its support vehicles, echelon by echelon: vehicles into lances, lances into companies,
 * companies into battalions - or Stars, Trinaries and Clusters, or Level IIs, IIIs and IVs - the same ladder its
 * combat formations climb.
 *
 * <p>Echelon {@code 1} is the smallest formation. {@code sizes.get(0)} is the vehicles in one, and
 * {@code sizes.get(n)} is how many echelon-{@code n} formations make one echelon-{@code n + 1} formation.</p>
 *
 * @param sizes        what fills one formation at each echelon, smallest first; never empty
 * @param namers       the name of the formation at a given position, counting from one, for each echelon
 * @param levelAtDepth the formation level of a given echelon, counting the smallest as {@code 1}; {@code null} leaves
 *                     formation levels alone
 */
public record SupportEchelons(List<Integer> sizes, List<IntFunction<String>> namers,
      @Nullable IntFunction<FormationLevel> levelAtDepth) {

    /** The most formations of one echelon a support formation is looked through for, past what any command fields. */
    static final int MAXIMUM_POSITIONS = 26;

    public SupportEchelons {
        if (sizes.isEmpty() || (sizes.size() != namers.size())) {
            throw new IllegalArgumentException("each echelon needs a size and a namer");
        }
        sizes = List.copyOf(sizes);
        namers = List.copyOf(namers);
    }

    /**
     * Lances only, with nothing grouped above them.
     *
     * @param lanceSize vehicles in one lance
     * @param namer     the name of the lance at a given position, counting from one
     *
     * @return the single-echelon ladder
     */
    public static SupportEchelons lancesOnly(int lanceSize, IntFunction<String> namer) {
        return new SupportEchelons(List.of(lanceSize), List.of(namer), null);
    }

    /** @return how many echelons the ladder climbs */
    public int depth() {
        return sizes.size();
    }

    /**
     * @param echelon the echelon, counting the smallest as {@code 1}
     *
     * @return what fills one formation of that echelon
     */
    public int sizeOf(int echelon) {
        return sizes.get(echelon - 1);
    }

    /**
     * @param echelon  the echelon, counting the smallest as {@code 1}
     * @param position the formation's position, counting from one
     *
     * @return the formation's name
     */
    public String nameOf(int echelon, int position) {
        return namers.get(echelon - 1).apply(position);
    }

    /**
     * The echelon of a formation this ladder made, which is told from a formation the player made by its name.
     *
     * @param formation the formation
     *
     * @return its echelon counting the smallest as {@code 1}, or {@code 0} when it is not one of the ladder's
     */
    public int echelonOf(@Nullable Formation formation) {
        if ((formation == null) || (formation.getName() == null)) {
            return 0;
        }
        String name = formation.getName();
        for (int echelon = 1; echelon <= depth(); echelon++) {
            for (int position = 1; position <= MAXIMUM_POSITIONS; position++) {
                if (name.equalsIgnoreCase(nameOf(echelon, position))) {
                    return echelon;
                }
            }
        }
        return 0;
    }

    /**
     * @param echelon the echelon, counting the smallest as {@code 1}; may be past the top of the ladder
     *
     * @return the formation level to show for it, or {@code null} when this ladder sets no levels
     */
    public @Nullable FormationLevel levelOf(int echelon) {
        return (levelAtDepth == null) ? null : levelAtDepth.apply(echelon);
    }
}
