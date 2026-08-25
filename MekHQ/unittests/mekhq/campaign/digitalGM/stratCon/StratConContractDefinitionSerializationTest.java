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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.ObjectiveParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Serialization guards for {@link StratConContractDefinition}: JSON is the read/write format, with the license notice
 * as a leading, read-ignored {@code #} block.
 */
class StratConContractDefinitionSerializationTest {

    @Test
    void jsonRoundTripPreservesFieldsAndCarriesLicense(@TempDir Path tempDir) throws IOException {
        StratConContractDefinition original = new StratConContractDefinition();
        original.setBriefing("Destroy designated targets.");
        original.setAlliedFacilityCount(-0.3);
        original.setHostileFacilityCount(-0.5);
        original.setAllowEarlyVictory(true);
        original.setScenarioOdds(List.of(22, 32, 42));
        original.setDeploymentTimes(List.of(3, 4, 5));

        ObjectiveParameters objective = new ObjectiveParameters();
        objective.objectiveType = StrategicObjectiveType.SpecificScenarioVictory;
        objective.objectiveCount = -0.5;
        objective.objectiveScenarios = List.of("Covert Strike.json", "Deep Raid.json");
        original.setObjectiveParameters(List.of(objective));

        File out = tempDir.resolve("ObjectiveRaid.json").toFile();
        original.Serialize(out);

        String content = Files.readString(out.toPath());
        assertTrue(content.startsWith("# MegaMek Data (C)"), "saved JSON should begin with the '#' license header");
        assertTrue(content.contains("CC BY-NC-SA 4.0"), "license text should be the MegaMek Data notice");
        assertTrue(content.contains("affiliated with Microsoft."), "the license header should be complete");
        assertTrue(content.substring(0, content.indexOf('{')).contains("# MegaMek Data"),
              "the license header must precede the JSON object");

        StratConContractDefinition reloaded = StratConContractDefinition.Deserialize(out);
        assertNotNull(reloaded, "a definition carrying a leading license header should still deserialize");
        assertEquals("Destroy designated targets.", reloaded.getBriefing());
        assertEquals(-0.3, reloaded.getAlliedFacilityCount());
        assertEquals(-0.5, reloaded.getHostileFacilityCount());
        assertTrue(reloaded.isAllowEarlyVictory());
        assertEquals(List.of(22, 32, 42), reloaded.getScenarioOdds());
        assertEquals(List.of(3, 4, 5), reloaded.getDeploymentTimes());
        assertEquals(1, reloaded.getObjectiveParameters().size());
        assertEquals(StrategicObjectiveType.SpecificScenarioVictory,
              reloaded.getObjectiveParameters().get(0).objectiveType);
        assertEquals(List.of("Covert Strike.json", "Deep Raid.json"),
              reloaded.getObjectiveParameters().get(0).objectiveScenarios);
    }

    @Test
    void manifestMappingRoundTripsAndCarriesLicense(@TempDir Path tempDir) throws IOException {
        Map<ContractObjectiveType, String> mapping = new EnumMap<>(ContractObjectiveType.class);
        mapping.put(ContractObjectiveType.GARRISON_DUTY, "GarrisonDuty.json");
        mapping.put(ContractObjectiveType.OBJECTIVE_RAID, "ObjectiveRaid.json");

        File out = tempDir.resolve("ContractDefinitionManifest.json").toFile();
        assertTrue(StratConContractDefinition.writeManifestMapping(out, mapping));

        String content = Files.readString(out.toPath());
        assertTrue(content.startsWith("# MegaMek Data (C)"), "manifest should begin with the '#' license header");

        Map<ContractObjectiveType, String> reloaded = StratConContractDefinition.readManifestMapping(out);
        assertEquals(2, reloaded.size());
        assertEquals("GarrisonDuty.json", reloaded.get(ContractObjectiveType.GARRISON_DUTY));
        assertEquals("ObjectiveRaid.json", reloaded.get(ContractObjectiveType.OBJECTIVE_RAID));
    }

    @Test
    void manifestMappingOverwritesExistingTypeMapping(@TempDir Path tempDir) {
        Map<ContractObjectiveType, String> mapping = new EnumMap<>(ContractObjectiveType.class);
        mapping.put(ContractObjectiveType.GARRISON_DUTY, "GarrisonDuty.json");
        File out = tempDir.resolve("ContractDefinitionManifest.json").toFile();
        assertTrue(StratConContractDefinition.writeManifestMapping(out, mapping));

        // re-mapping the same contract type replaces the file name rather than adding a duplicate
        Map<ContractObjectiveType, String> updated = StratConContractDefinition.readManifestMapping(out);
        updated.put(ContractObjectiveType.GARRISON_DUTY, "CustomGarrison.json");
        assertTrue(StratConContractDefinition.writeManifestMapping(out, updated));

        Map<ContractObjectiveType, String> reloaded = StratConContractDefinition.readManifestMapping(out);
        assertEquals(1, reloaded.size());
        assertEquals("CustomGarrison.json", reloaded.get(ContractObjectiveType.GARRISON_DUTY));
    }

    @Test
    void readManifestMappingReturnsEmptyForMissingFile(@TempDir Path tempDir) {
        // a checkout without a user manifest must yield an empty (never null) map so registration can start fresh
        Map<ContractObjectiveType, String> mapping =
              StratConContractDefinition.readManifestMapping(tempDir.resolve("absent.json").toFile());
        assertNotNull(mapping);
        assertTrue(mapping.isEmpty());
    }
}
