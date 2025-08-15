/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit;

import static megamek.common.MiscType.F_CARGO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestConstants.TEST_BLK;
import static testUtilities.MHQTestConstants.TEST_MTF;
import static testUtilities.MHQTestConstants.TEST_UNIT_DATA_DIR;

import java.io.File;
import java.util.UUID;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.MekSummary;
import megamek.common.Mounted;
import megamek.common.icons.Portrait;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CargoCapacityTest is a test suite for verifying the cargo capacity calculations of different entity types. It ensures
 * that factoring accurately computes the cargo capacity in the state of transport bays, mounted equipment, and damage
 * to specific locations.
 *
 * <p>The test primarily validates conditions where:</p>
 * <ul>
 *     <li>All systems are operational.</li>
 *     <li>Damage is applied to specific locations or bays.</li>
 *     <li>All systems are destroyed.</li>
 * </ul>
 */

class CargoCapacityTest {
    MMLogger logger = MMLogger.create(CargoCapacityTest.class);

    private Campaign mockCampaign;
    private CampaignOptions mockCampaignOptions;

    private final String CARGO_MEK = "Buster BC XV-M-B HaulerMech MOD";
    private final CargoUnit cargoMek = new CargoUnit(CARGO_MEK, 0, 2);

    private final String CARGO_DROP_SHIP = "Hoshiryokou Tug Boat";
    private final CargoUnit cargoDropShip = new CargoUnit(CARGO_DROP_SHIP, 7, 100);

    private final String CARGO_FIGHTER = "Caravan Heavy Transport";
    private final CargoUnit cargoFighter = new CargoUnit(CARGO_FIGHTER, 60, 0);

    private final String CARGO_TANK = "Prime Mover";
    private final CargoUnit cargoTank = new CargoUnit(CARGO_TANK, 0, 20);

    @BeforeAll
    public static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    public void setup() {
        mockCampaign = mock(Campaign.class);
        mockCampaignOptions = mock(CampaignOptions.class);
    }

    @Test
    public void testCargoCapacityOfCargoMek() {
        Entity entity = createEntity(cargoMek.name, false);
        testCargoTotal(entity, cargoMek.getTotalCargoCapacity());
    }

    @Test
    public void testCargoCapacityOfCargoMekKilledLocations() {
        Entity entity = createEntity(cargoMek.name, false);
        assertNotNull(entity);
        killCargoLocations(entity);
        testCargoTotal(entity, cargoMek.bayCargoCapacity);
    }

    @Test
    public void testCargoCapacityOfCargoMekKilledBays() {
        Entity entity = createEntity(cargoMek.name, false);
        assertNotNull(entity);
        killBays(entity);
        testCargoTotal(entity, cargoMek.otherCargoCapacity);
    }

    @Test
    public void testCargoCapacityOfCargoMekKillEverything() {
        Entity entity = createEntity(cargoMek.name, false);
        assertNotNull(entity);
        killCargoLocations(entity);
        killBays(entity);
        testCargoTotal(entity, 0);
    }

    @Test
    public void testCargoCapacityOfCargoDropShip() {
        Entity entity = createEntity(cargoDropShip.name, true);
        testCargoTotal(entity, cargoDropShip.getTotalCargoCapacity());
    }

    @Test
    public void testCargoCapacityOfCargoDropShipKilledLocations() {
        Entity entity = createEntity(cargoMek.name, false);
        assertNotNull(entity);
        killCargoLocations(entity);
        testCargoTotal(entity, cargoMek.bayCargoCapacity);
    }

    @Test
    public void testCargoCapacityOfCargoDropShipKilledBays() {
        Entity entity = createEntity(cargoMek.name, false);
        assertNotNull(entity);
        killBays(entity);
        testCargoTotal(entity, cargoMek.otherCargoCapacity);
    }

    @Test
    public void testCargoCapacityOfCargoDropShipKillEverything() {
        Entity entity = createEntity(cargoMek.name, false);
        assertNotNull(entity);
        killCargoLocations(entity);
        killBays(entity);
        testCargoTotal(entity, 0);
    }

    @Test
    public void testCargoCapacityOfCargoFighter() {
        Entity entity = createEntity(cargoFighter.name, true);
        testCargoTotal(entity, cargoFighter.getTotalCargoCapacity());
    }

    @Test
    public void testCargoCapacityOfCargoFighterKilledLocations() {
        Entity entity = createEntity(cargoMek.name, false);
        assertNotNull(entity);
        killCargoLocations(entity);
        testCargoTotal(entity, cargoMek.bayCargoCapacity);
    }

    @Test
    public void testCargoCapacityOfCargoFighterKilledBays() {
        Entity entity = createEntity(cargoMek.name, false);
        assertNotNull(entity);
        killBays(entity);
        testCargoTotal(entity, cargoMek.otherCargoCapacity);
    }

    @Test
    public void testCargoCapacityOfCargoFighterKillEverything() {
        Entity entity = createEntity(cargoMek.name, false);
        assertNotNull(entity);
        killCargoLocations(entity);
        killBays(entity);
        testCargoTotal(entity, 0);
    }

    @Test
    public void testCargoCapacityOfCargoTank() {
        Entity entity = createEntity(cargoTank.name, true);
        testCargoTotal(entity, cargoTank.getTotalCargoCapacity());
    }

    @Test
    public void testCargoCapacityOfCargoTankKilledLocations() {
        Entity entity = createEntity(cargoMek.name, false);
        assertNotNull(entity);
        killCargoLocations(entity);
        testCargoTotal(entity, cargoMek.bayCargoCapacity);
    }

    @Test
    public void testCargoCapacityOfCargoTankKilledBays() {
        Entity entity = createEntity(cargoMek.name, false);
        assertNotNull(entity);
        killBays(entity);
        testCargoTotal(entity, cargoMek.otherCargoCapacity);
    }

    @Test
    public void testCargoCapacityOfCargoTankKillEverything() {
        Entity entity = createEntity(cargoMek.name, false);
        assertNotNull(entity);
        killCargoLocations(entity);
        killBays(entity);
        testCargoTotal(entity, 0);
    }

    /**
     * Creates an {@link Entity} from the given unit name by retrieving its information from the cache.
     *
     * @param unitName The name of the unit to retrieve and parse.
     *
     * @return The {@link Entity} representing the unit, or {@code null} if the unit cannot be loaded.
     */
    private Entity createEntity(String unitName, boolean isBLK) {
        File unitFile = new File(TEST_UNIT_DATA_DIR + unitName + (isBLK ? TEST_BLK : TEST_MTF));
        Entity entity = MekSummary.loadEntity(unitFile);
        assert entity != null;
        return entity;
    }

    /**
     * Verifies the calculated cargo capacity of a given entity against an expected value.
     *
     * <p>To ensure accurate calculations, this method creates and fully crews a {@link Unit} entity,
     * then compares the reported cargo capacity to the expected value.</p>
     *
     * <p>Mock {@link Person} crew members are added to satisfy crewing requirements.
     * Drivers, gunners, and vessel crew are set up as needed by the entity being tested.</p>
     *
     * @param entity             The {@link Entity} whose cargo capacity is to be tested.
     * @param expectedCargoTotal The expected total cargo capacity for the provided entity.
     */
    private void testCargoTotal(Entity entity, double expectedCargoTotal) {
        Unit unit = new Unit(entity, mockCampaign);

        while (!unit.isFullyCrewed()) {
            Person crewMember = mock(Person.class);
            when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
            when(crewMember.getPortrait()).thenReturn(mock(Portrait.class));
            when(crewMember.getId()).thenReturn(mock(UUID.class));

            if (unit.getTotalDriverNeeds() > 0) {
                unit.addDriver(crewMember);
                continue;
            }

            if (unit.getTotalGunnerNeeds() > 0) {
                unit.addGunner(crewMember);
                continue;
            }

            if (unit.getTotalCrewNeeds() > 0) {
                unit.addVesselCrew(crewMember);
            }
        }

        double cargoCapacity = unit.getCargoCapacity();
        assertEquals(expectedCargoTotal, cargoCapacity);
    }

    /**
     * Simulates the destruction of all transport location-based systems for the provided entity.
     *
     * <p>This method iterates through all mounted equipment on the entity, identifies those with
     * the {@code F_CARGO} flag, and marks their locations as destroyed.</p>
     *
     * @param entity The {@link Entity} whose transport systems are to be simulated as destroyed.
     */
    private void killCargoLocations(Entity entity) {
        for (Mounted<?> mounted : entity.getMisc()) {
            if (mounted.getType().hasFlag(F_CARGO)) {
                if (entity.getInternal(mounted.getLocation()) != IArmorState.ARMOR_NA) {
                    entity.setInternal(IArmorState.ARMOR_DESTROYED, mounted.getLocation());
                }
            }
        }
    }

    /**
     * Simulates the destruction of all transport bays for the provided entity.
     *
     * <p>This method identifies and marks all bay-related systems with the {@code F_CARGO} flag as
     * destroyed.</p>
     *
     * @param entity The {@link Entity} whose bays are to be simulated as destroyed.
     */
    private void killBays(Entity entity) {
        for (Mounted<?> mounted : entity.getMisc()) {
            if (mounted.getType().hasFlag(F_CARGO)) {
                if (entity.getInternal(mounted.getLocation()) == IArmorState.ARMOR_NA) {
                    mounted.setDestroyed(true);
                }
            }
        }
    }

    /**
     * CargoRecord is an immutable data class representing information about the cargo capacity of an entity. It
     * contains the name of the entity, the cargo capacity from transport bays, the cargo capacity from other mounted
     * equipment, and a utility method to calculate the total cargo capacity.
     *
     * @param name               The name of the entity associated with the cargo.
     * @param bayCargoCapacity   The cargo capacity contributed by transport bays.
     * @param otherCargoCapacity The cargo capacity contributed by other mounted equipment.
     */
    public record CargoUnit(String name, double bayCargoCapacity, double otherCargoCapacity) {

        /**
         * Calculates the total cargo capacity as the sum of {@code bayCargoCapacity} and {@code otherCargoCapacity}.
         *
         * @return The total cargo capacity of the entity.
         */
        public double getTotalCargoCapacity() {
            return bayCargoCapacity + otherCargoCapacity;
        }
    }
}
