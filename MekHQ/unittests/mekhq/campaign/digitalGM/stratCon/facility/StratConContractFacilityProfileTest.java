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
package mekhq.campaign.digitalGM.stratCon.facility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests contract facility profiles: weighted type picks, tier adjustment, and reading and writing them with a contract
 * definition.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConContractFacilityProfileTest {
    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static StratConContractFacilityProfile profile(Map<FacilityType, Integer> weights) {
        StratConContractFacilityProfile profile = new StratConContractFacilityProfile();
        profile.setTypeWeights(new LinkedHashMap<>(weights));
        return profile;
    }

    @Test
    void typesArePickedInProportionToTheirWeights() {
        Map<FacilityType, Integer> weights = new LinkedHashMap<>();
        weights.put(FacilityType.MekBase, 3);
        weights.put(FacilityType.TankBase, 1);
        StratConContractFacilityProfile profile = profile(weights);

        assertEquals(FacilityType.MekBase, profile.pickType(ForceAlignment.Opposing, bound -> 0));
        assertEquals(FacilityType.MekBase, profile.pickType(ForceAlignment.Opposing, bound -> 2));
        assertEquals(FacilityType.TankBase, profile.pickType(ForceAlignment.Opposing, bound -> 3));
        assertEquals(FacilityType.TankBase, profile.pickType(ForceAlignment.Opposing, bound -> bound - 1));
    }

    @Test
    void aProfileWeightingNothingPicksNothing() {
        assertNull(profile(Map.of()).pickType(ForceAlignment.Opposing, bound -> 0));
        assertNull(profile(Map.of(FacilityType.MekBase, 0)).pickType(ForceAlignment.Opposing, bound -> 0));
    }

    @Test
    void createdFacilitiesHaveThePickedTypeAndOwner() {
        StratConFacility facility = profile(Map.of(FacilityType.DataCenter, 1)).createFacility(ForceAlignment.Allied);

        assertNotNull(facility);
        assertEquals(FacilityType.DataCenter, facility.getFacilityType());
        assertEquals(ForceAlignment.Allied, facility.getOwner());
    }

    @Test
    void theTierModifierIsKeptWithinTheTiers() {
        StratConContractFacilityProfile profile = new StratConContractFacilityProfile();
        profile.setTierModifier(-1);
        assertEquals(FacilityTier.OUTPOST, profile.adjustTier(FacilityTier.BASE));
        assertEquals(FacilityTier.OUTPOST, profile.adjustTier(FacilityTier.OUTPOST));

        profile.setTierModifier(1);
        assertEquals(FacilityTier.STRONGHOLD, profile.adjustTier(FacilityTier.STRONGHOLD));
    }

    @Test
    void aContractDefinitionReadsAndWritesItsProfile(@TempDir Path tempDir) throws IOException {
        StratConContractFacilityProfile profile = profile(Map.of(FacilityType.SupplyDepot, 2));
        profile.setBriefing("Cut their supply.");
        profile.setAlliedShare(0.25);
        profile.setTierModifier(1);
        profile.setDensityMultiplier(1.5);
        profile.setAnchorType(FacilityType.BaseOfOperations);

        StratConContractDefinition definition = new StratConContractDefinition();
        definition.setFacilityProfile(profile);
        File file = tempDir.resolve("Profiled.json").toFile();
        definition.Serialize(file);
        StratConContractFacilityProfile loaded = StratConContractDefinition.Deserialize(file).getFacilityProfile();

        assertNotNull(loaded);
        assertEquals("Cut their supply.", loaded.getBriefing());
        assertEquals(2, loaded.getTypeWeights().get(FacilityType.SupplyDepot));
        assertEquals(0.25, loaded.getAlliedShare());
        assertEquals(1, loaded.getTierModifier());
        assertEquals(1.5, loaded.getDensityMultiplier());
        assertEquals(FacilityType.BaseOfOperations, loaded.getAnchorType());
        assertEquals(ForceAlignment.Opposing, loaded.getAnchorOwner());
    }

    @Test
    void aDefinitionWithoutAProfilePlacesFacilitiesAsBefore(@TempDir Path tempDir) throws IOException {
        StratConContractDefinition definition = new StratConContractDefinition();
        File file = tempDir.resolve("Plain.json").toFile();
        definition.Serialize(file);

        assertNull(StratConContractDefinition.Deserialize(file).getFacilityProfile());
    }
}
