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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.biome.StratConBiome;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityManifest;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Serialization guards for {@link StratConFacility} and {@link StratConFacilityManifest}: JSON is the write format, led
 * by the MegaMek Data license notice as a leading, read-ignored {@code #} block.
 */
class StratConFacilitySerializationTest {

    private static StratConBiome biome(String category, int lower, int upper, String... terrain) {
        StratConBiome biome = new StratConBiome();
        biome.biomeCategory = category;
        biome.allowedTemperatureLowerBound = lower;
        biome.allowedTemperatureUpperBound = upper;
        biome.allowedTerrainTypes = List.of(terrain);
        return biome;
    }

    @Test
    void facilityRoundTripPreservesFieldsAndCarriesLicense(@TempDir Path tempDir) throws IOException {
        StratConFacility original = new StratConFacility();
        original.setOwner(ForceAlignment.Opposing);
        original.setDisplayableName("Mek Base");
        original.setFacilityType(FacilityType.MekBase);
        original.setUserDescription("Hostile Meks defend this facility.");
        original.setVisible(false);
        original.setIsAvailable(false);
        original.setSharedModifiers(List.of("EnemyMekReinforcements.json"));
        original.setLocalModifiers(List.of("EnemyMekGarrison.json"));
        original.setCapturedDefinition("AlliedMekBase.json");
        original.setRevealTrack(true);
        original.setIncreaseScanRange(true);
        original.setScenarioOddsModifier(15);
        original.setMonthlySPModifier(-2);
        original.setPreventAerospace(true);
        original.setStrategicObjective(true);
        original.setBiomes(List.of(
              biome("TerranFacility", 0, 267, "FrozenFacility"),
              biome("TerranFacility", 268, 277, "ColdFacility")));

        File out = tempDir.resolve("HostileMekBase.json").toFile();
        original.Serialize(out);

        String content = Files.readString(out.toPath());
        assertTrue(content.startsWith("# MegaMek Data (C)"), "saved JSON should begin with the '#' license header");
        assertTrue(content.contains("CC BY-NC-SA 4.0"), "license text should be the MegaMek Data notice");
        assertTrue(content.substring(0, content.indexOf('{')).contains("# MegaMek Data"),
              "the license header must precede the JSON object");

        StratConFacility reloaded = StratConFacility.deserialize(out.getPath());
        assertNotNull(reloaded, "a facility carrying a leading license header should still deserialize");
        assertEquals(ForceAlignment.Opposing, reloaded.getOwner());
        assertEquals("Mek Base", reloaded.getDisplayableName());
        assertEquals(FacilityType.MekBase, reloaded.getFacilityType());
        assertEquals("Hostile Meks defend this facility.", reloaded.getUserDescription());
        assertFalse(reloaded.getVisible());
        assertFalse(reloaded.getIsAvailable());
        assertEquals(List.of("EnemyMekReinforcements.json"), reloaded.getSharedModifiers());
        assertEquals(List.of("EnemyMekGarrison.json"), reloaded.getLocalModifiers());
        assertEquals("AlliedMekBase.json", reloaded.getCapturedDefinition());
        assertTrue(reloaded.getRevealTrack());
        assertTrue(reloaded.getIncreaseScanRange());
        assertEquals(15, reloaded.getScenarioOddsModifier());
        assertEquals(-2, reloaded.getMonthlySPModifier());
        assertTrue(reloaded.preventAerospace());
        assertTrue(reloaded.isStrategicObjective());
    }

    @Test
    void facilityBiomesAndTransientMapSurviveLoad(@TempDir Path tempDir) {
        StratConFacility original = new StratConFacility();
        original.setOwner(ForceAlignment.Opposing);
        original.setDisplayableName("Mek Base");
        original.setFacilityType(FacilityType.MekBase);
        original.setBiomes(List.of(
              biome("TerranFacility", 0, 267, "FrozenFacility"),
              biome("TerranFacility", 268, 277, "ColdFacility"),
              biome("TerranFacility", 278, 297, "TemperateFacility")));

        File out = tempDir.resolve("HostileMekBase.json").toFile();
        original.Serialize(out);

        StratConFacility reloaded = StratConFacility.deserialize(out.getPath());
        assertNotNull(reloaded);
        assertEquals(3, reloaded.getBiomes().size());
        assertEquals("TerranFacility", reloaded.getBiomes().get(0).biomeCategory);
        assertEquals(0, reloaded.getBiomes().get(0).allowedTemperatureLowerBound);
        assertEquals(List.of("FrozenFacility"), reloaded.getBiomes().get(0).allowedTerrainTypes);
        // deserialize() reconstructs the transient temperature lookup keyed by each biome's lower bound
        assertEquals(3, reloaded.getBiomeTempMap().size());
        assertTrue(reloaded.getBiomeTempMap().containsKey(0));
        assertTrue(reloaded.getBiomeTempMap().containsKey(278));
    }

    @Test
    void facilityDefaultsApplyWhenFieldsAbsent(@TempDir Path tempDir) {
        // a minimal facility omits most fields; the model's initializers/defaults must survive a round trip
        StratConFacility original = new StratConFacility();
        original.setOwner(ForceAlignment.Allied);
        original.setDisplayableName("Air Base");
        original.setFacilityType(FacilityType.AirBase);

        File out = tempDir.resolve("AlliedAirBase.json").toFile();
        original.Serialize(out);

        StratConFacility reloaded = StratConFacility.deserialize(out.getPath());
        assertNotNull(reloaded);
        // isAvailable defaults to true; the collections default to empty (never null)
        assertTrue(reloaded.getIsAvailable());
        assertNotNull(reloaded.getSharedModifiers());
        assertTrue(reloaded.getSharedModifiers().isEmpty());
        assertNotNull(reloaded.getBiomes());
        assertTrue(reloaded.getBiomes().isEmpty());
    }

    @Test
    void facilityManifestRoundTripsAndCarriesLicense(@TempDir Path tempDir) throws IOException {
        StratConFacilityManifest original = new StratConFacilityManifest();
        original.facilityFileNames.add("AlliedMekBase.json");
        original.facilityFileNames.add("HostileMekBase.json");

        File out = tempDir.resolve("facilitymanifest.json").toFile();
        assertTrue(original.serialize(out));

        String content = Files.readString(out.toPath());
        assertTrue(content.startsWith("# MegaMek Data (C)"), "manifest should begin with the '#' license header");

        StratConFacilityManifest reloaded = StratConFacilityManifest.deserialize(out.getPath());
        assertNotNull(reloaded);
        assertEquals(List.of("AlliedMekBase.json", "HostileMekBase.json"), reloaded.facilityFileNames);
    }

    @Test
    void freshFacilityManifestHasWritableList(@TempDir Path tempDir) {
        // a freshly constructed manifest must expose a non-null list so registration can append to it
        StratConFacilityManifest fresh = new StratConFacilityManifest();
        assertNotNull(fresh.facilityFileNames);
        fresh.facilityFileNames.add("NewFacility.json");

        File out = tempDir.resolve("facilitymanifest.json").toFile();
        assertTrue(fresh.serialize(out));

        StratConFacilityManifest reloaded = StratConFacilityManifest.deserialize(out.getPath());
        assertNotNull(reloaded);
        assertEquals(List.of("NewFacility.json"), reloaded.facilityFileNames);
    }
}
