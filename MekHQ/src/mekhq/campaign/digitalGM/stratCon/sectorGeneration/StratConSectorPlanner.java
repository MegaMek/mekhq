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

import static java.lang.Math.max;
import static java.lang.Math.round;
import static mekhq.campaign.digitalGM.stratCon.StratConContractInitializer.NUM_FORMATIONS_PER_TRACK;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides how many StratCon sectors a contract generates and how large each one is, producing a {@link SectorSpec} per
 * sector. This is the whole-contract "how many, how big" step; per-sector sizing and terrain generation consume the
 * resulting specs later.
 *
 * <p>Three regimes are supported:</p>
 * <ul>
 *     <li><b>Legacy</b> (alternate count off): reproduces the historical layout of one sector per three combat teams,
 *     with a final smaller sector for any remainder.</li>
 *     <li><b>Alternate count</b> (alternate count on, condense off): roughly one sector per nine combat teams.</li>
 *     <li><b>Condensed</b> (alternate count and condense on): the alternate count, but capped at ten sectors. A force
 *     that would generate more instead receives ten proportionally larger sectors, so the total mapped area is
 *     preserved.</li>
 * </ul>
 *
 * <p>Condensing is part of the alternate-count system: it has no effect when alternate count is off, since the legacy
 * layout is preserved verbatim in that case.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConSectorPlanner {
    /** Combat teams represented by one base sector under the alternate-count rule. */
    public static final int ALTERNATE_FORMATIONS_PER_SECTOR = 9;

    /** The hard cap on sector count when condensing is enabled. */
    public static final int MAXIMUM_SECTORS = 10;

    private StratConSectorPlanner() {}

    /**
     * Generates the list of sector specs for a contract.
     *
     * @param requiredCombatFormations the contract's required combat formation count
     * @param alternateCount           {@code true} to use the alternate one-sector-per-nine-teams count; {@code false}
     *                                 for the legacy one-sector-per-three-teams layout
     * @param condenseSectors          {@code true} to cap the sector count at {@link #MAXIMUM_SECTORS} by enlarging
     *                                 sectors; only honored when {@code alternateCount} is {@code true}
     *
     * @return a non-empty list of sector specs
     */
    public static List<SectorSpec> generateSectorSpecs(int requiredCombatFormations, boolean alternateCount,
          boolean condenseSectors) {
        return alternateCount ?
                     generateAlternateSpecs(requiredCombatFormations, condenseSectors) :
                     generateLegacySpecs(requiredCombatFormations);
    }

    /**
     * Reproduces the historical sector layout: one sector per three combat teams, plus a final sector holding any
     * remainder, and a guaranteed minimum of one sector.
     */
    private static List<SectorSpec> generateLegacySpecs(int requiredCombatFormations) {
        List<SectorSpec> specs = new ArrayList<>();

        int fullSectors = max(0, requiredCombatFormations / NUM_FORMATIONS_PER_TRACK);
        for (int index = 0; index < fullSectors; index++) {
            specs.add(new SectorSpec(1, NUM_FORMATIONS_PER_TRACK, LatitudeBand.random()));
        }

        int remainder = requiredCombatFormations % NUM_FORMATIONS_PER_TRACK;
        if (remainder > 0) {
            specs.add(new SectorSpec(1, remainder, LatitudeBand.random()));
        }

        // Never generate a contract with zero sectors.
        if (specs.isEmpty()) {
            specs.add(new SectorSpec(1, 1, LatitudeBand.random()));
        }

        return specs;
    }

    /**
     * Generates the alternate-count layout (roughly one sector per nine combat teams), optionally condensed to at most
     * {@link #MAXIMUM_SECTORS} sectors.
     */
    private static List<SectorSpec> generateAlternateSpecs(int requiredCombatFormations, boolean condenseSectors) {
        int desiredSectors = max(1, (int) round(requiredCombatFormations / (double) ALTERNATE_FORMATIONS_PER_SECTOR));

        if (condenseSectors && (desiredSectors > MAXIMUM_SECTORS)) {
            return generateCondensedSpecs(desiredSectors);
        }

        List<SectorSpec> specs = new ArrayList<>();
        for (int index = 0; index < desiredSectors; index++) {
            specs.add(new SectorSpec(1, ALTERNATE_FORMATIONS_PER_SECTOR, LatitudeBand.random()));
        }
        return specs;
    }

    /**
     * Distributes {@code desiredSectors} size units across exactly {@link #MAXIMUM_SECTORS} sectors as evenly as
     * possible, giving the leading ("older") sectors the extra unit when the split is uneven. For example, 15 desired
     * sectors become five double-sized and five single-sized; 25 become five triple-sized and five double-sized.
     */
    private static List<SectorSpec> generateCondensedSpecs(int desiredSectors) {
        int baseUnits = desiredSectors / MAXIMUM_SECTORS;
        int remainder = desiredSectors % MAXIMUM_SECTORS;

        List<SectorSpec> specs = new ArrayList<>();
        for (int index = 0; index < MAXIMUM_SECTORS; index++) {
            int units = baseUnits + ((index < remainder) ? 1 : 0);
            specs.add(new SectorSpec(units, units * ALTERNATE_FORMATIONS_PER_SECTOR, LatitudeBand.random()));
        }
        return specs;
    }
}
