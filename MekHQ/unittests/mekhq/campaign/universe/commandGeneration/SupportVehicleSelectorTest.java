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
package mekhq.campaign.universe.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.function.Function;

import megamek.client.ratgenerator.MissionRole;
import mekhq.campaign.universe.commandGeneration.SupportVehicleSelector.Candidate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers which vehicles a capability will accept when the force generator offers them.
 *
 * <p>The rolling itself needs the real generator data and is checked in a campaign rather than here. What is
 * pinned here is the judgement applied to whatever the roll returns, because that is where a wrong answer is
 * quiet: a convoy of runabouts still looks like a convoy in the TOE.</p>
 */
class SupportVehicleSelectorTest {
    /** Real capacities, so the thresholds are tested against the vehicles they were chosen for. */
    private static final double SKODA_GROWLER_CARGO = 0.13;
    private static final double FLATBED_TRUCK_CARGO = 6.0;
    private static final double BURRO_II_CARGO = 12.5;

    private static Candidate vehicle(double cargoTons, int mashTheatres, int fieldKitchens) {
        return new Candidate("test vehicle", null, cargoTons, mashTheatres, fieldKitchens, false);
    }

    /** The same vehicle, but as a trailer: no engine of its own, so it needs a tractor to go anywhere. */
    private static Candidate trailer(double cargoTons, int mashTheatres, int fieldKitchens) {
        return new Candidate("test trailer", null, cargoTons, mashTheatres, fieldKitchens, true);
    }

    /** The real role lookup, put back after each test that stands in for the force generator. */
    private Function<String, Set<MissionRole>> realRoleLookup;

    @BeforeEach
    void rememberRoleLookup() {
        realRoleLookup = SupportVehicleSelector.roleLookup;
    }

    @AfterEach
    void restoreRoleLookup() {
        SupportVehicleSelector.roleLookup = realRoleLookup;
    }

    /** Stands in for the force generator, which is not staged for tests: every vehicle carries these roles. */
    private static void everyVehicleHasRoles(MissionRole... roles) {
        Set<MissionRole> roleSet = Set.of(roles);
        SupportVehicleSelector.roleLookup = unitName -> roleSet;
    }

    @Test
    @DisplayName("A recovery vehicle with a cargo bay is counted as recovery, never as a convoy truck")
    void aRecoveryVehicleWithCargoIsSalvageNotLogistics() {
        // Issue 10375: the convoy roll accepted BattleMek Recovery Vehicles, which carry the cargo role too. Counted
        // afterwards they were recovery vehicles, so the convoy came up short and the recovery formation over.
        everyVehicleHasRoles(MissionRole.RECOVERY, MissionRole.CARGO);

        assertEquals(SupportCapability.SALVAGE, SupportVehicleSelector.classify(vehicle(FLATBED_TRUCK_CARGO, 0, 0)));
    }

    @Test
    @DisplayName("Equipment decides first: a MASH vehicle with a cargo bay is medical")
    void equipmentOutranksRoles() {
        everyVehicleHasRoles(MissionRole.SUPPORT, MissionRole.CARGO, MissionRole.RECOVERY);

        assertEquals(SupportCapability.MEDICAL, SupportVehicleSelector.classify(vehicle(BURRO_II_CARGO, 1, 0)));
        assertEquals(SupportCapability.COMMISSARY, SupportVehicleSelector.classify(vehicle(BURRO_II_CARGO, 0, 2)));
    }

    @Test
    @DisplayName("A cargo truck is logistics, and a vehicle with no support role or equipment is nothing")
    void cargoTrucksAndCombatVehicles() {
        everyVehicleHasRoles(MissionRole.CARGO);
        assertEquals(SupportCapability.LOGISTICS, SupportVehicleSelector.classify(vehicle(FLATBED_TRUCK_CARGO, 0, 0)));
        assertNull(SupportVehicleSelector.classify(vehicle(SKODA_GROWLER_CARGO, 0, 0)),
              "a runabout's bay is too small to make it a convoy truck");

        everyVehicleHasRoles();
        assertNull(SupportVehicleSelector.classify(vehicle(FLATBED_TRUCK_CARGO, 0, 0)),
              "a combat vehicle with a cargo bay but no support role is part of the fighting force");
    }

    @Test
    @DisplayName("A recovery vehicle needs no equipment, because the real ones carry none")
    void salvageAcceptsAnythingTheRoleReturned() {
        // The role has already filtered the table, and a BattleMek Recovery Vehicle is a plain Tank with a cargo
        // bay: requiring equipment here would reject the very vehicle the capability exists for.
        assertTrue(SupportVehicleSelector.suits(SupportCapability.SALVAGE, vehicle(0, 0, 0)));
    }

    @Test
    @DisplayName("A convoy needs a real cargo bay, not a runabout")
    void logisticsRejectsVehiclesTooSmallToHaulSupply() {
        assertFalse(SupportVehicleSelector.suits(SupportCapability.LOGISTICS,
                    vehicle(SKODA_GROWLER_CARGO, 0, 0)),
              "a 0.13 ton runabout is cargo tagged, but a convoy of them hauls nothing");
        assertTrue(SupportVehicleSelector.suits(SupportCapability.LOGISTICS, vehicle(FLATBED_TRUCK_CARGO, 0, 0)));
        assertTrue(SupportVehicleSelector.suits(SupportCapability.LOGISTICS, vehicle(BURRO_II_CARGO, 0, 0)));
    }

    @Test
    @DisplayName("Medical and commissary are recognised by the equipment they carry")
    void medicalAndCommissaryNeedTheirEquipment() {
        assertTrue(SupportVehicleSelector.suits(SupportCapability.MEDICAL, vehicle(0, 1, 0)));
        assertFalse(SupportVehicleSelector.suits(SupportCapability.MEDICAL, vehicle(FLATBED_TRUCK_CARGO, 0, 0)),
              "a cargo truck is not a field hospital");

        assertTrue(SupportVehicleSelector.suits(SupportCapability.COMMISSARY, vehicle(0, 0, 1)));
        assertFalse(SupportVehicleSelector.suits(SupportCapability.COMMISSARY, vehicle(0, 1, 0)),
              "a MASH truck is not a canteen");
    }

    @Test
    @DisplayName("A trailer is never fielded, whatever it carries")
    void trailersAreRejectedForEveryCapability() {
        // A Clan command was handed a Hector Road Train trailer module as its canteen: three field kitchens that
        // cannot leave the depot, because nothing was generated to tow them. Pairing trailers with tractors is
        // separate work; until then they are not fielded at all.
        assertFalse(SupportVehicleSelector.suits(SupportCapability.COMMISSARY, trailer(5.0, 0, 3)),
              "a trailer with three kitchens still cannot move itself");
        assertFalse(SupportVehicleSelector.suits(SupportCapability.LOGISTICS, trailer(BURRO_II_CARGO, 0, 0)));
        assertFalse(SupportVehicleSelector.suits(SupportCapability.MEDICAL, trailer(0, 2, 0)));
        assertFalse(SupportVehicleSelector.suits(SupportCapability.SALVAGE, trailer(0, 0, 0)),
              "even the salvage role, which accepts anything the role returned, stops at a trailer");
    }

    @Test
    @DisplayName("The security detail is infantry, so nothing is rolled for it")
    void securityRollsNoVehicle() {
        assertFalse(SupportVehicleSelector.suits(SupportCapability.SECURITY, vehicle(BURRO_II_CARGO, 1, 1)),
              "however capable the vehicle, the security detail is foot infantry");
    }

    @Test
    @DisplayName("The convoy threshold sits between the runabout and the smallest real truck")
    void theConvoyThresholdIsBetweenARunaboutAndATruck() {
        assertTrue(SKODA_GROWLER_CARGO < SupportVehicleSelector.MINIMUM_CONVOY_CARGO_TONS,
              "the threshold must exclude the runabout");
        assertTrue(SupportVehicleSelector.MINIMUM_CONVOY_CARGO_TONS < FLATBED_TRUCK_CARGO,
              "and must not exclude the Flatbed Truck, which is the convoy workhorse");
    }
}
