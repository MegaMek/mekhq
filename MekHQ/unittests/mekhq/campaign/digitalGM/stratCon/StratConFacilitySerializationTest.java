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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.biome.StratConBiome;
import mekhq.campaign.digitalGM.stratCon.facility.IStratConFacilityEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityDefinition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.MonthlySupportPointsEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.PreventAerospaceEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.RevealTrackEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.ScanRangeEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.ScenarioOddsEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.SharedModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.UnknownEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityJson;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityJson.LoadedFacilityDefinition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityManifest;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Serialization guards for {@link StratConFacilityDefinition} and {@link StratConFacilityManifest}: JSON is the write
 * format, led by the MegaMek Data license notice as a leading, read-ignored {@code #} block. Files in the format used
 * before 0.51.01 must still load.
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

    private static LoadedFacilityDefinition roundTrip(StratConFacilityDefinition definition, File out)
          throws IOException {
        StratConFacilityJson.toFile(definition, out);
        return StratConFacilityJson.fromFile(out);
    }

    @Test
    void definitionRoundTripPreservesEveryEffectAndCarriesLicense(@TempDir Path tempDir) throws IOException {
        StratConFacilityProfile alliedProfile = new StratConFacilityProfile("Allied Meks defend this facility.",
              List.of(new LocalModifiersEffect(List.of("AlliedMekGarrison.json")),
                    new ScanRangeEffect(2),
                    new MonthlySupportPointsEffect(3),
                    new RevealTrackEffect()));
        StratConFacilityProfile hostileProfile = new StratConFacilityProfile("Hostile Meks defend this facility.",
              List.of(new SharedModifiersEffect(List.of("EnemyMekReinforcements.json")),
                    new ScenarioOddsEffect(15),
                    new PreventAerospaceEffect()));
        StratConFacilityDefinition original = new StratConFacilityDefinition("MekBase",
              "Mek Base",
              FacilityType.MekBase,
              alliedProfile,
              hostileProfile);

        File out = tempDir.resolve("MekBase.json").toFile();
        LoadedFacilityDefinition loaded = roundTrip(original, out);

        String content = Files.readString(out.toPath());
        assertTrue(content.startsWith("# MegaMek Data (C)"), "saved JSON should begin with the '#' license header");
        assertTrue(content.contains("CC BY-NC-SA 4.0"), "license text should be the MegaMek Data notice");
        assertTrue(content.substring(0, content.indexOf('{')).contains("# MegaMek Data"),
              "the license header must precede the JSON object");

        assertFalse(loaded.isLegacyFormat());
        StratConFacilityDefinition reloaded = loaded.definition();
        assertEquals("MekBase", reloaded.getId());
        assertEquals("Mek Base", reloaded.getDisplayableName());
        assertEquals(FacilityType.MekBase, reloaded.getFacilityType());

        StratConFacilityProfile reloadedAllied = reloaded.getAlliedProfile();
        assertNotNull(reloadedAllied);
        assertEquals("Allied Meks defend this facility.", reloadedAllied.getDescription());
        assertEquals(List.of("AlliedMekGarrison.json"), reloadedAllied.getLocalModifierIds());
        assertEquals(2, reloadedAllied.getScanRangeIncrease());
        assertEquals(3, reloadedAllied.getMonthlySupportPoints());
        assertTrue(reloadedAllied.isRevealingTrack());
        assertFalse(reloadedAllied.isPreventingAerospace());

        StratConFacilityProfile reloadedHostile = reloaded.getHostileProfile();
        assertNotNull(reloadedHostile);
        assertEquals(List.of("EnemyMekReinforcements.json"), reloadedHostile.getSharedModifierIds());
        assertEquals(15, reloadedHostile.getScenarioOddsModifier());
        assertTrue(reloadedHostile.isPreventingAerospace());
        assertEquals(0, reloadedHostile.getScanRangeIncrease());
    }

    @Test
    void definitionBiomesAndTransientMapSurviveLoad(@TempDir Path tempDir) throws IOException {
        StratConFacilityDefinition original = new StratConFacilityDefinition("MekBase",
              "Mek Base",
              FacilityType.MekBase,
              new StratConFacilityProfile(),
              new StratConFacilityProfile());
        original.setBiomes(List.of(
              biome("TerranFacility", 0, 267, "FrozenFacility"),
              biome("TerranFacility", 268, 277, "ColdFacility"),
              biome("TerranFacility", 278, 297, "TemperateFacility")));

        StratConFacilityDefinition reloaded = roundTrip(original, tempDir.resolve("MekBase.json").toFile())
                                                    .definition();

        assertEquals(3, reloaded.getBiomes().size());
        assertEquals("TerranFacility", reloaded.getBiomes().get(0).biomeCategory);
        assertEquals(0, reloaded.getBiomes().get(0).allowedTemperatureLowerBound);
        assertEquals(List.of("FrozenFacility"), reloaded.getBiomes().get(0).allowedTerrainTypes);
        // loading rebuilds the transient temperature lookup keyed by each biome's lower bound
        assertEquals(3, reloaded.getBiomeTempMap().size());
        assertTrue(reloaded.getBiomeTempMap().containsKey(0));
        assertTrue(reloaded.getBiomeTempMap().containsKey(278));
    }

    @Test
    void aMissingIdTakesTheFileNameAndAMissingProfileStaysAbsent(@TempDir Path tempDir) throws IOException {
        StratConFacilityDefinition original = new StratConFacilityDefinition(null,
              "Air Base",
              FacilityType.AirBase,
              new StratConFacilityProfile(),
              null);

        StratConFacilityDefinition reloaded = roundTrip(original, tempDir.resolve("AirBase.json").toFile())
                                                    .definition();

        assertEquals("AirBase", reloaded.getId());
        assertNotNull(reloaded.getAlliedProfile());
        assertTrue(reloaded.getAlliedProfile().getEffects().isEmpty());
        assertNull(reloaded.getHostileProfile());
        assertTrue(reloaded.getBiomes().isEmpty());
    }

    @Test
    void anOldFormatFileLoadsAsAOneSidedDefinition(@TempDir Path tempDir) throws IOException {
        File legacyFile = tempDir.resolve("HostileDataCenter.json").toFile();
        Files.writeString(legacyFile.toPath(), """
              # MegaMek Data (C) 2025 by The MegaMek Team is licensed under CC BY-NC-SA 4.0.
              {
                "owner": "Opposing",
                "displayableName": "Data Center",
                "facilityType": "DataCenter",
                "userDescription": "An extra hostile Mek patrol.",
                "visible": false,
                "sharedModifiers": ["EnemyMekPatrol.json"],
                "capturedDefinition": "AlliedDataCenter.json",
                "scenarioOddsModifier": 5,
                "preventAerospace": true
              }
              """, StandardCharsets.UTF_8);

        LoadedFacilityDefinition loaded = StratConFacilityJson.fromFile(legacyFile);

        assertTrue(loaded.isLegacyFormat());
        assertEquals("AlliedDataCenter.json", loaded.capturedDefinition());
        StratConFacilityDefinition definition = loaded.definition();
        assertEquals("HostileDataCenter", definition.getId());
        assertEquals("Data Center", definition.getDisplayableName());
        assertEquals(FacilityType.DataCenter, definition.getFacilityType());
        assertNull(definition.getAlliedProfile(), "an old file describes one side only");

        StratConFacilityProfile hostileProfile = definition.getHostileProfile();
        assertNotNull(hostileProfile);
        assertEquals("An extra hostile Mek patrol.", hostileProfile.getDescription());
        assertEquals(List.of("EnemyMekPatrol.json"), hostileProfile.getSharedModifierIds());
        assertEquals(5, hostileProfile.getScenarioOddsModifier());
        assertTrue(hostileProfile.isPreventingAerospace());
    }

    @Test
    void anUnknownEffectLoadsAsOneThatDoesNothing(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("Future.json").toFile();
        Files.writeString(file.toPath(), """
              {
                "id": "Future",
                "displayableName": "Future Base",
                "facilityType": "MekBase",
                "alliedProfile": {
                  "effects": [
                    { "effect": "somethingNew", "strength": 3 },
                    { "effect": "scanRange", "value": 1 }
                  ]
                }
              }
              """, StandardCharsets.UTF_8);

        StratConFacilityProfile profile = StratConFacilityJson.fromFile(file).definition().getAlliedProfile();

        assertNotNull(profile);
        List<IStratConFacilityEffect> effects = profile.getEffects();
        assertEquals(2, effects.size(), "the unknown effect must not fail the file or drop its neighbours");
        assertInstanceOf(UnknownEffect.class, effects.get(0));
        assertEquals(1, profile.getScanRangeIncrease());
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
