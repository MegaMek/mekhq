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

import mekhq.campaign.Hangar;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TransportCostCalculationsTest {
    private static final LocalDate today = LocalDate.of(3151, 1, 1);

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 10, 30, 40, 50 })
    public void testCalculateAdditionalBayRequirementsFromPassengers(int passengerCount) {
        Hangar hangar = new Hangar();
        CargoStatistics mockCargoStatistics = mock(CargoStatistics.class);
        HangarStatistics mockHangarStatistics = mock(HangarStatistics.class);

        Person person = new Person(UUID.randomUUID());
        person.setStatus(PersonnelStatus.ACTIVE);
        Collection<Person> passengers = new ArrayList<>();
        for (int i = 0; i < passengerCount; i++) {
            passengers.add(person);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(hangar, passengers,
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);

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
