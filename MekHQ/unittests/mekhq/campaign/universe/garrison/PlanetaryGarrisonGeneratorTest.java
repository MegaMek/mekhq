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
package mekhq.campaign.universe.garrison;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.units.UnitType;
import mekhq.campaign.universe.commandGeneration.ratgen.ForceDescriptorSnapshot;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link PlanetaryGarrisonGenerator} turns a garrison composition into the right number of contingents,
 * at the right echelon, unit type, faction, and rating, all nested under one garrison force. The formation generator is
 * stubbed so the RAT generation engine is not required.
 */
class PlanetaryGarrisonGeneratorTest {

    /** Records each snapshot it is asked to build and returns a marker force so assembly can be inspected. */
    private static final class RecordingFormationGenerator
          implements Function<ForceDescriptorSnapshot, ForceDescriptor> {
        private final List<ForceDescriptorSnapshot> snapshots = new ArrayList<>();

        @Override
        public ForceDescriptor apply(ForceDescriptorSnapshot snapshot) {
            snapshots.add(snapshot);
            ForceDescriptor formation = new ForceDescriptor();
            formation.setUnitType(snapshot.getUnitType());
            formation.setEchelon(snapshot.getEchelon());
            return formation;
        }
    }

    private static long countByUnitType(List<ForceDescriptorSnapshot> snapshots, int unitType) {
        return snapshots.stream().filter(snapshot -> snapshot.getUnitType() == unitType).count();
    }

    @Test
    void assembleGeneratesOneFormationPerContingentMember() {
        RecordingFormationGenerator generator = new RecordingFormationGenerator();
        GarrisonComposition composition = new GarrisonComposition(4, 3, 1); // 4 inf regt, 3 armor bn, 1 Mek bn

        ForceDescriptor garrison = PlanetaryGarrisonGenerator.assemble("FS.pm", 3025, composition, generator);

        assertEquals(8, garrison.getSubForces().size()); // 4 + 3 + 1
        assertEquals(4, countByUnitType(generator.snapshots, UnitType.INFANTRY));
        assertEquals(3, countByUnitType(generator.snapshots, UnitType.TANK));
        assertEquals(1, countByUnitType(generator.snapshots, UnitType.MEK));
    }

    @Test
    void assembleSetsFactionYearRatingAndEchelonsOnSnapshots() {
        RecordingFormationGenerator generator = new RecordingFormationGenerator();
        GarrisonComposition composition = new GarrisonComposition(1, 1, 1);

        PlanetaryGarrisonGenerator.assemble("DC.pm", 3025, composition, generator);

        for (ForceDescriptorSnapshot snapshot : generator.snapshots) {
            assertEquals("DC.pm", snapshot.getFaction());
            assertEquals(3025, snapshot.getYear());
            assertEquals("F", snapshot.getRating());
        }
        // Infantry generate at regiment (6); armor and Mek at battalion (5).
        assertEquals(6, snapshotFor(generator, UnitType.INFANTRY).getEchelon());
        assertEquals(5, snapshotFor(generator, UnitType.TANK).getEchelon());
        assertEquals(5, snapshotFor(generator, UnitType.MEK).getEchelon());
    }

    @Test
    void assembleSetsGarrisonRootProperties() {
        RecordingFormationGenerator generator = new RecordingFormationGenerator();

        ForceDescriptor garrison = PlanetaryGarrisonGenerator.assemble("LA.pm", 3025,
              new GarrisonComposition(2, 1, 0), generator);

        assertEquals("Planetary Militia", garrison.getName());
        assertEquals("LA.pm", garrison.getFaction());
        assertEquals(7, garrison.getEchelon()); // brigade
        assertEquals(3, garrison.getSubForces().size()); // 2 + 1 + 0, no Mek
    }

    @Test
    void assembleSkipsFormationsTheGeneratorCouldNotBuild() {
        // A generator that fails (returns null) for Mek must not add a null subforce.
        Function<ForceDescriptorSnapshot, ForceDescriptor> pickyGenerator = snapshot -> {
            if (snapshot.getUnitType() == UnitType.MEK) {
                return null;
            }
            ForceDescriptor formation = new ForceDescriptor();
            formation.setUnitType(snapshot.getUnitType());
            return formation;
        };

        ForceDescriptor garrison = PlanetaryGarrisonGenerator.assemble("FS.pm", 3025,
              new GarrisonComposition(2, 2, 3), pickyGenerator);

        assertEquals(4, garrison.getSubForces().size()); // 2 inf + 2 armor, the 3 Mek dropped
    }

    private static ForceDescriptorSnapshot snapshotFor(RecordingFormationGenerator generator, int unitType) {
        return generator.snapshots.stream()
                     .filter(snapshot -> snapshot.getUnitType() == unitType)
                     .findFirst()
                     .orElseThrow();
    }
}
