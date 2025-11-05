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

import static megamek.common.equipment.MiscType.F_CARGO;
import static megamek.common.equipment.MiscType.F_LIFT_HOIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.getEntityForUnitTesting;

import java.util.UUID;
import java.util.stream.Stream;

import megamek.common.bays.Bay;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.icons.Portrait;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.ForceType;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    private Campaign mockCampaign;
    private CampaignOptions mockCampaignOptions;
    private Game mockGame;
    private GameOptions mockGameOptions;

    private final String CARGO_MEK = "Buster BC XV-M-B HaulerMech MOD";
    private final CargoUnit cargoMek = new CargoUnit(CARGO_MEK, 0, 2, 50, 0);

    private final String LIFT_HOIST_MEK = "Quickdraw QKD-8X";
    private final CargoUnit liftHoistMek = new CargoUnit(LIFT_HOIST_MEK, 0, 0, 60, 30);

    private final String CARGO_DROP_SHIP = "Hoshiryokou Tug Boat";
    private final CargoUnit cargoDropShip = new CargoUnit(CARGO_DROP_SHIP, 7, 100, 0, 0);

    private final String CARGO_FIGHTER = "Caravan Heavy Transport";
    private final CargoUnit cargoFighter = new CargoUnit(CARGO_FIGHTER, 60, 0, 0, 0);

    private final String CARGO_TANK = "Prime Mover";
    private final CargoUnit cargoTank = new CargoUnit(CARGO_TANK, 0, 20, 60, 0);

    private static Stream<Arguments> getForceTypes() {
        return Stream.of(Arguments.of(ForceType.CONVOY), Arguments.of(ForceType.SALVAGE));
    }

    @BeforeEach
    public void setup() {
        mockCampaign = mock(Campaign.class);
        mockCampaignOptions = mock(CampaignOptions.class);
        mockGame = mock(Game.class);
        mockGameOptions = mock(GameOptions.class);

        when(mockCampaign.getGame()).thenReturn(mockGame);
        when(mockGame.getOptions()).thenReturn(mockGameOptions);
        when(mockGameOptions.booleanOption(OptionsConstants.ADVANCED_BA_GRAB_BARS)).thenReturn(false);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoMek(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoMek.name, false);
        testCargoTotal(entity, cargoMek.getTotalCargoCapacity(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoMekKilledLocations(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoMek.name, false);
        assertNotNull(entity);
        killCargoLocations(entity);
        testCargoTotal(entity, cargoMek.getCargoCapacityOtherDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoMekKilledBays(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoMek.name, false);
        assertNotNull(entity);
        killBays(entity);
        testCargoTotal(entity, cargoMek.getCargoCapacityBaysDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoMekKillEverything(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoMek.name, false);
        assertNotNull(entity);
        killCargoLocations(entity);
        killBays(entity);
        testCargoTotal(entity, cargoMek.getCargoCapacityAllDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfLiftHoistMek(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(liftHoistMek.name, false);
        testCargoTotal(entity, liftHoistMek.getTotalCargoCapacity(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfLiftHoistMekKilledLocations(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(liftHoistMek.name, false);
        assertNotNull(entity);
        killCargoLocations(entity);
        testCargoTotal(entity, liftHoistMek.getCargoCapacityOtherDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfLiftHoistMekKilledBays(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(liftHoistMek.name, false);
        assertNotNull(entity);
        killBays(entity);
        testCargoTotal(entity, liftHoistMek.getCargoCapacityBaysDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfLiftHoistMekKillEverything(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(liftHoistMek.name, false);
        assertNotNull(entity);
        killCargoLocations(entity);
        killBays(entity);
        testCargoTotal(entity, liftHoistMek.getCargoCapacityAllDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoDropShip(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoDropShip.name, true);
        testCargoTotal(entity, cargoDropShip.getTotalCargoCapacity(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoDropShipKilledLocations(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoDropShip.name, true);
        assertNotNull(entity);
        killCargoLocations(entity);
        testCargoTotal(entity, cargoDropShip.getCargoCapacityOtherDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoDropShipKilledBays(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoDropShip.name, true);
        assertNotNull(entity);
        killBays(entity);
        testCargoTotal(entity, cargoDropShip.getCargoCapacityBaysDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoDropShipKillEverything(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoDropShip.name, true);
        assertNotNull(entity);
        killCargoLocations(entity);
        killBays(entity);
        testCargoTotal(entity, cargoDropShip.getCargoCapacityAllDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoFighter(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoFighter.name, true);
        testCargoTotal(entity, cargoFighter.getTotalCargoCapacity(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoFighterKilledLocations(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoFighter.name, true);
        assertNotNull(entity);
        killCargoLocations(entity);
        testCargoTotal(entity, cargoFighter.getCargoCapacityOtherDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoFighterKilledBays(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoFighter.name, true);
        assertNotNull(entity);
        killBays(entity);
        testCargoTotal(entity, cargoFighter.getCargoCapacityBaysDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoFighterKillEverything(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoFighter.name, true);
        assertNotNull(entity);
        killCargoLocations(entity);
        killBays(entity);
        testCargoTotal(entity, cargoFighter.getCargoCapacityAllDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoTank(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoTank.name, true);
        testCargoTotal(entity, cargoTank.getTotalCargoCapacity(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoTankKilledLocations(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoTank.name, true);
        assertNotNull(entity);
        killCargoLocations(entity);
        testCargoTotal(entity, cargoTank.getCargoCapacityOtherDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoTankKilledBays(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoTank.name, true);
        assertNotNull(entity);
        killBays(entity);
        testCargoTotal(entity, cargoTank.getCargoCapacityBaysDestroyed(forceType), forceType);
    }

    @ParameterizedTest
    @MethodSource("getForceTypes")
    public void testCargoCapacityOfCargoTankKillEverything(ForceType forceType) {
        Entity entity = getEntityForUnitTestingCargoCapacity(cargoTank.name, true);
        assertNotNull(entity);
        killCargoLocations(entity);
        killBays(entity);
        testCargoTotal(entity, cargoTank.getCargoCapacityAllDestroyed(forceType), forceType);
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
        for (Bay bay : entity.getTransportBays()) {
            bay.setBayDamage(bay.getCapacity());
        }
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
                mounted.setDestroyed(true);
            } else if (mounted.getType().hasFlag(F_LIFT_HOIST)) {
                mounted.setDestroyed(true);
            }
        }
    }

    private Entity getEntityForUnitTestingCargoCapacity(String name, boolean isBLK) {
        Entity entity = getEntityForUnitTesting(name, isBLK);
        if (entity != null) {
            entity.setId(1);
            when(mockGame.getEntity(1)).thenReturn(entity);
            entity.setGame(mockGame);
            entity.addIntrinsicTransporters();
        }
        return entity;
    }

    /**
     * CargoRecord is an immutable data class representing information about the cargo capacity of an entity. It
     * contains the name of the entity, the cargo capacity from transport bays, the cargo capacity from other mounted
     * equipment, and a utility method to calculate the total cargo capacity.
     *
     * @param name               The name of the entity associated with the cargo.
     * @param bayCargoCapacity   The cargo capacity contributed by transport bays.
     * @param otherCargoCapacity The cargo capacity contributed by other mounted equipment.
     * @param roofCargoCapacity  The cargo capacity Meks and Vees recieve from their roof
     * @param liftHoistCapacity  The cargo capacity granted by lift hoists - not used for convoys
     */
    public record CargoUnit(String name, double bayCargoCapacity, double otherCargoCapacity,
          double roofCargoCapacity, double liftHoistCapacity) {

        /**
         * Calculates the total cargo capacity as the sum of {@code bayCargoCapacity} and {@code otherCargoCapacity}.
         *
         * @return The total cargo capacity of the entity.
         */
        public double getTotalCargoCapacity(ForceType forceType) {
            if (forceType == ForceType.CONVOY) {
                return bayCargoCapacity + otherCargoCapacity;
            } else {
                return bayCargoCapacity + otherCargoCapacity + roofCargoCapacity + liftHoistCapacity;
            }
        }

        public double getCargoCapacityBaysDestroyed(ForceType forceType) {
            if (forceType == ForceType.CONVOY) {
                return otherCargoCapacity;
            } else {
                return otherCargoCapacity + roofCargoCapacity + liftHoistCapacity;
            }
        }

        public double getCargoCapacityOtherDestroyed(ForceType forceType) {
            if (forceType == ForceType.CONVOY) {
                return bayCargoCapacity;
            } else {
                return bayCargoCapacity + roofCargoCapacity;
            }
        }

        public double getCargoCapacityAllDestroyed(ForceType forceType) {
            if (forceType == ForceType.CONVOY) {
                return 0;
            } else {
                return roofCargoCapacity;
            }
        }
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
    private void testCargoTotal(Entity entity, double expectedCargoTotal, ForceType forceType) {
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

        double cargoCapacity;

        if (forceType == ForceType.SALVAGE) {
            cargoCapacity = unit.getCargoCapacityForSalvage();
        } else if (forceType == ForceType.CONVOY) {
            cargoCapacity = unit.getCargoCapacityForConvoy();
        } else {
            cargoCapacity = unit.getCargoCapacity();
        }

        assertEquals(expectedCargoTotal, cargoCapacity);
    }
}
