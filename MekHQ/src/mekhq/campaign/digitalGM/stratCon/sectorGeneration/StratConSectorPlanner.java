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
 * <p>Four regimes are supported, three of them scaling by a formation echelon:</p>
 * <ul>
 *     <li><b>Legacy</b>: one sector per company (three combat teams), with a final smaller sector for any remainder,
 *     and no cap.</li>
 *     <li><b>Alternate</b>: roughly one sector per battalion (nine combat teams).</li>
 *     <li><b>Condensed</b>: the alternate count capped at {@link #MAXIMUM_SECTORS}, unless holding to that would
 *     leave a sector fronting more than a regiment, in which case more sectors are generated.</li>
 *     <li><b>Regimental</b>: roughly one sector per regiment (forty-five combat teams), so anything up to a regiment
 *     fights over a single sector.</li>
 * </ul>
 *
 * <p>Whenever a method produces fewer sectors than the teams would naturally fill, every team is still shared out
 * across the sectors that remain rather than being dropped. Sector size follows from the teams assigned to it, so the
 * total mapped area stays roughly constant however the contract is divided - the pieces just get bigger. The one
 * exception is the area ceiling applied later in {@code StratConContractInitializer}, which a sector fronting a
 * regiment's worth of teams will reach.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConSectorPlanner {
    /** Combat teams represented by one base sector under the alternate-count rule: a battalion. */
    public static final int ALTERNATE_FORMATIONS_PER_SECTOR = 9;

    /**
     * Combat teams represented by one sector under the regimental rule. Forty-five lances is a five-battalion regiment,
     * the largest the Inner Sphere structure recognizes, so a sector covers at most one regiment's ground.
     */
    public static final int REGIMENTAL_FORMATIONS_PER_SECTOR = 45;

    /**
     * The cap on sector count when condensing is enabled. Yields to {@link #REGIMENTAL_FORMATIONS_PER_SECTOR}: a
     * contract too large for ten sectors to hold at a regiment apiece gets more than ten rather than sectors that
     * overrun the area ceiling.
     */
    public static final int MAXIMUM_SECTORS = 10;

    private StratConSectorPlanner() {}

    /**
     * Generates the list of sector specs for a contract.
     *
     * @param requiredCombatFormations the contract's required combat formation count
     * @param countMethod              how the teams are divided into sectors
     * @param maximumTeamsPerSector    the most teams one sector on this planet can front before its area would be
     *                                 clipped by the sector-size ceiling; every method except
     *                                 {@link StratConSectorCountMethod#LEGACY} splits further rather than exceed it
     *
     * @return a non-empty list of sector specs
     */
    public static List<SectorSpec> generateSectorSpecs(int requiredCombatFormations,
          StratConSectorCountMethod countMethod, int maximumTeamsPerSector) {
        return switch (countMethod) {
            // Legacy sizing is a flat allowance per team that never approaches the ceiling, so it has nothing to
            // overflow into and keeps its historical layout exactly.
            case LEGACY -> generateLegacySpecs(requiredCombatFormations);
            case ALTERNATE -> generateAlternateSpecs(requiredCombatFormations, false, maximumTeamsPerSector);
            case CONDENSED -> generateAlternateSpecs(requiredCombatFormations, true, maximumTeamsPerSector);
            case REGIMENTAL -> generateRegimentalSpecs(requiredCombatFormations, maximumTeamsPerSector);
        };
    }

    /**
     * @return the fewest sectors that can hold the given teams without any one of them fronting more than
     *       {@code teamsPerSector}. A ceiling division, so the last sector is the one left short rather than over.
     */
    private static int sectorsNeededToStayUnder(int requiredCombatFormations, int teamsPerSector) {
        int perSector = max(1, teamsPerSector);
        return max(1, ((requiredCombatFormations + perSector) - 1) / perSector);
    }

    /**
     * Generates the regimental layout: roughly one sector per {@link #REGIMENTAL_FORMATIONS_PER_SECTOR} combat teams,
     * so a contract fielding up to a regiment gets a single sector and larger ones get a sector per regiment.
     *
     * <p>Uncapped, like the alternate count it scales from. The cap that {@link StratConSectorCountMethod#CONDENSED}
     * applies would need 450 combat teams to bite here - ten regiments - so there is nothing to guard against.</p>
     */
    private static List<SectorSpec> generateRegimentalSpecs(int requiredCombatFormations, int maximumTeamsPerSector) {
        int desiredSectors = max(1,
              (int) round(requiredCombatFormations / (double) REGIMENTAL_FORMATIONS_PER_SECTOR));

        // A regiment is more ground than a wet or oversized world will grant one sector, so overflow into more of them
        // rather than let the ceiling clip the map.
        desiredSectors = max(desiredSectors, sectorsNeededToStayUnder(requiredCombatFormations, maximumTeamsPerSector));

        return shareTeamsEvenly(requiredCombatFormations, desiredSectors);
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
     *
     * <p>Condensing has a second limit that outranks the first: no sector may front more than a regiment. Ten sectors
     * absorb a contract up to 450 combat teams before that binds, and past it the ten-sector cap yields and more
     * sectors are generated. The order matters because the two caps fail differently - exceeding ten sectors costs the
     * player some tab clutter, while a sector fronting more than a regiment asks for more ground than the area ceiling
     * will grant and quietly loses the difference.</p>
     */
    private static List<SectorSpec> generateAlternateSpecs(int requiredCombatFormations, boolean condenseSectors,
          int maximumTeamsPerSector) {
        int desiredSectors = max(1,
              (int) round(requiredCombatFormations / (double) ALTERNATE_FORMATIONS_PER_SECTOR));

        if (condenseSectors) {
            desiredSectors = min(desiredSectors, MAXIMUM_SECTORS);
            desiredSectors = max(desiredSectors,
                  sectorsNeededToStayUnder(requiredCombatFormations, REGIMENTAL_FORMATIONS_PER_SECTOR));
        }

        // Applies to both: condensing can pack a sector past what the ceiling grants, and on a wet or oversized world
        // even an uncondensed battalion-sized sector can.
        desiredSectors = max(desiredSectors, sectorsNeededToStayUnder(requiredCombatFormations, maximumTeamsPerSector));

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
