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
package mekhq.campaign.roleplay;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import megamek.common.annotations.Nullable;

/**
 * Rolls on up to {@value #MAXIMUM_TABLES} {@link OracleTable}s at once to give the player combined story suggestions.
 */
public final class Concepts {
    public static final int MAXIMUM_TABLES = 3;

    /**
     * One concept result.
     *
     * @param table   the table rolled on
     * @param meaning the meaning drawn, or {@code null} if the table had nothing to draw
     */
    public record Concept(OracleTable table, @Nullable String meaning) {}

    private Concepts() {}

    /**
     * Rolls once on each of the given tables. {@code null} entries and repeated tables are skipped, and only the first
     * {@value #MAXIMUM_TABLES} distinct tables are used.
     *
     * @param tables    the tables to roll on, in display order
     * @param generator the generator supplying the table contents
     *
     * @return one concept per table rolled, in the same order
     */
    public static List<Concept> roll(final List<OracleTable> tables, final RandomOracleGenerator generator) {
        final LinkedHashSet<OracleTable> distinct = new LinkedHashSet<>();
        for (OracleTable table : tables) {
            if (table != null && distinct.size() < MAXIMUM_TABLES) {
                distinct.add(table);
            }
        }

        final List<Concept> concepts = new ArrayList<>();
        for (OracleTable table : distinct) {
            concepts.add(new Concept(table, generator.generate(table)));
        }
        return concepts;
    }
}
