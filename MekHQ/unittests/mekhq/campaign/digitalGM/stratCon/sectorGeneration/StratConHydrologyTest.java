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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConHydrology}: YAML loading, Gaussian weighting, and ocean-percentage rolls. Backed by the
 * authored {@code HydrologyProfiles.yaml}.
 */
class StratConHydrologyTest {

    private static StratConHydrology hydrology() {
        return StratConHydrology.getInstance();
    }

    private static HydrologyProfileType highestWeightType(int waterPercent) {
        StratConHydrology hydrology = hydrology();
        HydrologyProfile best = null;
        double bestWeight = -1.0;
        for (HydrologyProfile profile : hydrology.getProfiles()) {
            double weight = StratConHydrology.weight(profile, waterPercent, hydrology.getSigma());
            if (weight > bestWeight) {
                bestWeight = weight;
                best = profile;
            }
        }
        assertNotNull(best);
        return best.type();
    }

    @Test
    void yaml_loadsAllNineProfilesAndSigma() {
        StratConHydrology hydrology = hydrology();

        assertEquals(9, hydrology.getProfiles().size());
        assertEquals(18.0, hydrology.getSigma());
        // Every profile parsed its type.
        hydrology.getProfiles().forEach(profile -> assertNotNull(profile.type()));
    }

    @Test
    void weight_peaksAtGaussianCenter() {
        HydrologyProfile coastal = profileOf(HydrologyProfileType.COASTAL);
        int center = (int) Math.round(coastal.gaussianCenter());

        double atCenter = StratConHydrology.weight(coastal, center, 18.0);
        double below = StratConHydrology.weight(coastal, center - 15, 18.0);
        double above = StratConHydrology.weight(coastal, center + 15, 18.0);

        assertTrue(atCenter > below);
        assertTrue(atCenter > above);
    }

    @Test
    void selection_favorsTheProfileWhoseCenterMatchesWaterCoverage() {
        assertEquals(HydrologyProfileType.INLAND, highestWeightType(5));
        assertEquals(HydrologyProfileType.RIVERLANDS, highestWeightType(16));
        assertEquals(HydrologyProfileType.LAKELANDS, highestWeightType(22));
        assertEquals(HydrologyProfileType.MARSHLANDS, highestWeightType(32));
        assertEquals(HydrologyProfileType.COASTAL, highestWeightType(35));
        assertEquals(HydrologyProfileType.INLAND_SEA, highestWeightType(40));
        assertEquals(HydrologyProfileType.PENINSULA, highestWeightType(48));
        assertEquals(HydrologyProfileType.ISLAND, highestWeightType(60));
        assertEquals(HydrologyProfileType.ARCHIPELAGO, highestWeightType(70));
    }

    @Test
    void dryPlanet_weightsInlandOverArchipelago() {
        HydrologyProfile inland = profileOf(HydrologyProfileType.INLAND);
        HydrologyProfile archipelago = profileOf(HydrologyProfileType.ARCHIPELAGO);

        assertTrue(StratConHydrology.weight(inland, 5, 18.0) > StratConHydrology.weight(archipelago, 5, 18.0));
    }

    @Test
    void selectProfile_alwaysReturnsALoadedProfile() {
        StratConHydrology hydrology = hydrology();
        List<HydrologyProfile> profiles = hydrology.getProfiles();

        for (int water = 0; water <= 100; water += 5) {
            HydrologyProfile selected = hydrology.selectProfile(water);
            assertNotNull(selected);
            assertTrue(profiles.contains(selected));
        }
    }

    @Test
    void rollOceanPercent_staysWithinTheProfileBand() {
        StratConHydrology hydrology = hydrology();

        for (HydrologyProfile profile : hydrology.getProfiles()) {
            for (int attempt = 0; attempt < 200; attempt++) {
                int ocean = hydrology.rollOceanPercent(profile);
                assertTrue(ocean >= profile.minOceanPercent(),
                      profile.type() + " rolled " + ocean + " below its band");
                assertTrue(ocean <= profile.maxOceanPercent(),
                      profile.type() + " rolled " + ocean + " above its band");
            }
        }
    }

    private static HydrologyProfile profileOf(HydrologyProfileType type) {
        return hydrology().getProfiles()
                     .stream()
                     .filter(profile -> profile.type() == type)
                     .findFirst()
                     .orElseThrow();
    }
}
