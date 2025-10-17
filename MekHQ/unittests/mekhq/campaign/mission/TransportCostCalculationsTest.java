package mekhq.campaign.mission;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;

public class TransportCostCalculationsTest {
    private static final LocalDate today = LocalDate.of(3151, 1, 1);

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
