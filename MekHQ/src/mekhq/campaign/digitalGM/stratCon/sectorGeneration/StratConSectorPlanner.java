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
import static java.lang.Math.min;
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
 *     <li><b>Legacy</b>: the historical layout of one sector per three combat teams, with a final smaller sector for
 *     any remainder, and no cap.</li>
 *     <li><b>Alternate</b>: roughly one sector per nine combat teams.</li>
 *     <li><b>Condensed</b>: the alternate count capped at {@link #MAXIMUM_SECTORS}.</li>
 *     <li><b>Single</b>: exactly one sector.</li>
 * </ul>
 *
 * <p>Whenever a method produces fewer sectors than the teams would naturally fill, every team is still shared out
 * across the sectors that remain rather than being dropped. Sector size follows from the teams assigned to it, so the
 * total mapped area stays roughly constant however the contract is divided - the pieces just get bigger. The one
 * exception is the area ceiling applied later in {@code StratConContractInitializer}, which a single sector holding a
 * very large contract will reach.</p>
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
     * @param countMethod              how the teams are divided into sectors
     *
     * @return a non-empty list of sector specs
     */
    public static List<SectorSpec> generateSectorSpecs(int requiredCombatFormations,
          StratConSectorCountMethod countMethod) {
        return switch (countMethod) {
            case LEGACY -> generateLegacySpecs(requiredCombatFormations);
            case ALTERNATE -> generateAlternateSpecs(requiredCombatFormations, false);
            case CONDENSED -> generateAlternateSpecs(requiredCombatFormations, true);
            // Every team lands in the one sector, so it is sized for the whole contract. Large contracts will reach
            // the area ceiling in StratConContractInitializer and stop growing there.
            case SINGLE -> shareTeamsEvenly(requiredCombatFormations, 1);
        };
    }

    /**
     * Reproduces the historical sector layout: one sector per three combat teams, plus a final sector holding any
     * remainder, and a guaranteed minimum of one sector.
     *
     * <p>Uncapped by design: this is the historical layout reproduced exactly, so a large contract really does get
     * dozens of sectors. A player who wants a ceiling picks {@link StratConSectorCountMethod#CONDENSED} instead, which
     * is a different count rather than this one with a limit bolted on.</p>
     */
    private static List<SectorSpec> generateLegacySpecs(int requiredCombatFormations) {
        int fullSectors = max(0, requiredCombatFormations / NUM_FORMATIONS_PER_TRACK);

        List<SectorSpec> specs = new ArrayList<>();

        for (int index = 0; index < fullSectors; index++) {
            specs.add(new SectorSpec(NUM_FORMATIONS_PER_TRACK, LatitudeBand.random()));
        }

        int remainder = requiredCombatFormations % NUM_FORMATIONS_PER_TRACK;
        if (remainder > 0) {
            specs.add(new SectorSpec(remainder, LatitudeBand.random()));
        }

        // Never generate a contract with zero sectors.
        if (specs.isEmpty()) {
            specs.add(new SectorSpec(1, LatitudeBand.random()));
        }

        return specs;
    }

    /**
     * Generates the alternate-count layout: roughly one sector per {@link #ALTERNATE_FORMATIONS_PER_SECTOR} combat
     * teams, capped at {@link #MAXIMUM_SECTORS} when condensing is enabled.
     *
     * <p>The contract's teams are then shared out evenly across however many sectors that leaves. Condensing therefore
     * makes every sector a little larger rather than making the first few enormous and the rest ordinary.</p>
     */
    private static List<SectorSpec> generateAlternateSpecs(int requiredCombatFormations, boolean condenseSectors) {
        int desiredSectors = max(1,
              (int) round(requiredCombatFormations / (double) ALTERNATE_FORMATIONS_PER_SECTOR));

        if (condenseSectors) {
            desiredSectors = min(desiredSectors, MAXIMUM_SECTORS);
        }

        return shareTeamsEvenly(requiredCombatFormations, desiredSectors);
    }

    /**
     * Splits the contract's combat teams as evenly as possible across the given number of sectors, so no sector ends up
     * markedly bigger than its neighbours. Any teams left over by the division are handed one each to the leading
     * sectors, which is the closest an integer split can get to even.
     *
     * @param requiredCombatFormations the contract's total required combat teams
     * @param sectorCount              how many sectors to split them across, at least one
     *
     * @return one spec per sector, each carrying its share of the teams
     */
    private static List<SectorSpec> shareTeamsEvenly(int requiredCombatFormations, int sectorCount) {
        int sectors = max(1, sectorCount);
        int baseTeams = requiredCombatFormations / sectors;
        int remainder = requiredCombatFormations % sectors;

        List<SectorSpec> specs = new ArrayList<>();
        for (int index = 0; index < sectors; index++) {
            // A sector always demands at least one team, however small the contract.
            int teams = max(1, baseTeams + ((index < remainder) ? 1 : 0));
            specs.add(new SectorSpec(teams, LatitudeBand.random()));
        }
        return specs;
    }
}
