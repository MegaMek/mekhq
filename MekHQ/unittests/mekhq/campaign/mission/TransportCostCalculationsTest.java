package mekhq.campaign.mission;


import static java.lang.Math.ceil;
import static java.lang.Math.round;
import static mekhq.campaign.mission.TransportCostCalculations.BAYS_PER_DROPSHIP;
import static mekhq.campaign.mission.TransportCostCalculations.PASSENGERS_COST;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import megamek.common.units.Entity;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TransportCostCalculationsTest {
    private static final LocalDate today = LocalDate.of(3151, 1, 1);
    private static final CargoStatistics mockCargoStatistics = mock(CargoStatistics.class);
    private static final HangarStatistics mockHangarStatistics = mock(HangarStatistics.class);

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_dropShips(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getDropShipCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_smallCraft(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getSmallCraftCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_mek(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getMekCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_aerospaceFighter(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(false);
        when(mockEntity.isAerospaceFighter()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getAsfCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_conventionalFighter(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(false);
        when(mockEntity.isAerospaceFighter()).thenReturn(false);
        when(mockEntity.isConventionalFighter()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getAsfCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_protoMek(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(false);
        when(mockEntity.isAerospaceFighter()).thenReturn(false);
        when(mockEntity.isConventionalFighter()).thenReturn(false);
        when(mockEntity.isProtoMek()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getProtoMekCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_battleArmor(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(false);
        when(mockEntity.isAerospaceFighter()).thenReturn(false);
        when(mockEntity.isConventionalFighter()).thenReturn(false);
        when(mockEntity.isProtoMek()).thenReturn(false);
        when(mockEntity.isBattleArmor()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getBattleArmorCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_infantry(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(false);
        when(mockEntity.isAerospaceFighter()).thenReturn(false);
        when(mockEntity.isConventionalFighter()).thenReturn(false);
        when(mockEntity.isProtoMek()).thenReturn(false);
        when(mockEntity.isBattleArmor()).thenReturn(false);
        when(mockEntity.isInfantry()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getInfantryCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_otherUnit(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(false);
        when(mockEntity.isAerospaceFighter()).thenReturn(false);
        when(mockEntity.isConventionalFighter()).thenReturn(false);
        when(mockEntity.isProtoMek()).thenReturn(false);
        when(mockEntity.isBattleArmor()).thenReturn(false);
        when(mockEntity.isInfantry()).thenReturn(false);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getOtherUnitCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_superHeavyVehicles(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(true);
        when(mockEntity.getWeight()).thenReturn(10000.0);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedSuperHeavyVehicles = transportCostCalculations.getSuperHeavyVehicleCount();
        assertEquals(unitCount, countedSuperHeavyVehicles,
              "Expected " + unitCount + " units but was " + countedSuperHeavyVehicles);

        int countedHeavyVehicles = transportCostCalculations.getHeavyVehicleCount();
        assertEquals(0, countedHeavyVehicles,
              "Expected " + unitCount + " units but was " + countedHeavyVehicles);

        int countedLightVehicles = transportCostCalculations.getLightVehicleCount();
        assertEquals(0, countedLightVehicles,
              "Expected " + unitCount + " units but was " + countedLightVehicles);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_heavyVehicles(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(true);
        when(mockEntity.getWeight()).thenReturn(75.0);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedSuperHeavyVehicles = transportCostCalculations.getSuperHeavyVehicleCount();
        assertEquals(0, countedSuperHeavyVehicles,
              "Expected " + unitCount + " units but was " + countedSuperHeavyVehicles);

        int countedHeavyVehicles = transportCostCalculations.getHeavyVehicleCount();
        assertEquals(unitCount, countedHeavyVehicles,
              "Expected " + unitCount + " units but was " + countedHeavyVehicles);

        int countedLightVehicles = transportCostCalculations.getLightVehicleCount();
        assertEquals(0, countedLightVehicles,
              "Expected " + unitCount + " units but was " + countedLightVehicles);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_lightVehicles(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(true);
        when(mockEntity.getWeight()).thenReturn(25.0);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedSuperHeavyVehicles = transportCostCalculations.getSuperHeavyVehicleCount();
        assertEquals(0, countedSuperHeavyVehicles,
              "Expected " + unitCount + " units but was " + countedSuperHeavyVehicles);

        int countedHeavyVehicles = transportCostCalculations.getHeavyVehicleCount();
        assertEquals(0, countedHeavyVehicles,
              "Expected " + unitCount + " units but was " + countedHeavyVehicles);

        int countedLightVehicles = transportCostCalculations.getLightVehicleCount();
        assertEquals(unitCount, countedLightVehicles,
              "Expected " + unitCount + " units but was " + countedLightVehicles);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 10, 30, 40, 50 })
    public void testCalculateAdditionalBayRequirementsFromPassengers(int passengerCount) {
        Person person = new Person(UUID.randomUUID());
        person.setStatus(PersonnelStatus.ACTIVE);
        Collection<Person> passengers = new ArrayList<>();
        for (int i = 0; i < passengerCount; i++) {
            passengers.add(person);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(new ArrayList<>(),
              passengers,
              mockCargoStatistics,
              mockHangarStatistics,
              EXP_REGULAR);

        double additionalPassengerBaysCost = transportCostCalculations.getAdditionalPassengerBaysCost();
        double expectedCost = round(additionalPassengerBaysCost * PASSENGERS_COST);
        assertEquals(additionalPassengerBaysCost, expectedCost,
              "Expected additional passenger bays cost to be " +
                    expectedCost +
                    " but was " +
                    additionalPassengerBaysCost);

        double totalAdditionalBaysRequired = transportCostCalculations.getAdditionalPassengerBaysRequired();
        int expectedAdditionalBays = (int) ceil(totalAdditionalBaysRequired / BAYS_PER_DROPSHIP);
        assertEquals(expectedAdditionalBays, totalAdditionalBaysRequired,
              "Expected total additional bays required to be " +
                    expectedAdditionalBays +
                    " but was " +
                    totalAdditionalBaysRequired);

    }

    @Test
    public void testPerformJumpTransaction_unableToAffordJourney() {
        Finances finances = new Finances();

        PlanetarySystem mockCurrentPlanetarySystem = mock(PlanetarySystem.class);
        when(mockCurrentPlanetarySystem.getName(any(LocalDate.class))).thenReturn("Test");

        PlanetarySystem mockDestinationPlanetarySystem = mock(PlanetarySystem.class);
        when(mockDestinationPlanetarySystem.getName(any(LocalDate.class))).thenReturn("Test");

        JumpPath mockJumpPath = mock(JumpPath.class);
        when(mockJumpPath.getLastSystem()).thenReturn(mockDestinationPlanetarySystem);

        String report = TransportCostCalculations.performJumpTransaction(finances,
              mockJumpPath,
              any(LocalDate.class),
              Money.of(100),
              mockCurrentPlanetarySystem);

        assertFalse(report.isBlank(), "Journey cost doesn't exceeds available funds");
    }

    @Test
    public void testPerformJumpTransaction_ableToAffordJourney() {
        Finances finances = new Finances();
        finances.credit(TransactionType.BONUS_EXCHANGE, today, Money.of(100), "");

        PlanetarySystem mockCurrentPlanetarySystem = mock(PlanetarySystem.class);
        when(mockCurrentPlanetarySystem.getName(any(LocalDate.class))).thenReturn("Test");

        PlanetarySystem mockDestinationPlanetarySystem = mock(PlanetarySystem.class);
        when(mockDestinationPlanetarySystem.getName(any(LocalDate.class))).thenReturn("Test");

        JumpPath mockJumpPath = mock(JumpPath.class);
        when(mockJumpPath.getLastSystem()).thenReturn(mockDestinationPlanetarySystem);

        String report = TransportCostCalculations.performJumpTransaction(finances,
              mockJumpPath,
              today,
              Money.of(1),
              mockCurrentPlanetarySystem);

        assertTrue(report.isBlank(), "Journey cost exceeds available funds");
    }
}
