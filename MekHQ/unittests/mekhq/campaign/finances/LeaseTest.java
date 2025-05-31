package mekhq.campaign.finances;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import megamek.common.Dropship;
import megamek.common.Mek;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Nested
public class LeaseTest {

    Unit mockUnit;

    //Arrange
    @BeforeEach
    void beforeEach() {
        Unit unit = new Unit();
        mockUnit = Mockito.spy(unit);
        Mockito.doReturn(Money.of(600000.00)).when(mockUnit).getSellValue();

        Lease testLease = new Lease(LocalDate.parse("3025-03-31"), mockUnit);
        mockUnit.addLease(testLease);
    }

    @Test
    void getLeaseCost_WhenSetUp_hasValidBaseLeaseCost() {
        //Act
        Money testCost = mockUnit.getUnitLease().getLeaseCost();

        //Assert
        assertEquals(Money.of(3000), testCost);
    }

    @Test
    void getLeaseCostNow_WhenFullMonthCost_CalculatesBaseLeaseCostCorrectly() {
        //Arrange
        LocalDate testDate = LocalDate.parse("3025-05-01");

        //Act
        Money testCost = mockUnit.getUnitLease().getLeaseCostNow(testDate);

        //Assert
        assertEquals(Money.of(3000), testCost);
    }

    @Test
    void getLeaseCostNow_WhenLeaseHasNotStarted_EqualsZeroCost() {
        //Arrange
        LocalDate testDate = LocalDate.parse("3025-03-01");

        //Act
        Money testCost = mockUnit.getUnitLease().getLeaseCostNow(testDate);

        //Assert
        assertEquals(Money.zero(), testCost);
    }

    @Test
    void getFirstLeaseCost_WhenInFirstMonth_ShouldGivePartialLeaseAmount() {
        LocalDate date = LocalDate.parse("3025-04-01");

        //Act
        Money testCost = mockUnit.getUnitLease().getFirstLeaseCost(date);

        //Assert
        assertEquals(Math.round(Money.of(3000.00 / 31).getAmount().doubleValue()),
              Math.round(testCost.getAmount().doubleValue())); // This should be a single day's lease
    }

    @Test
    void getLeaseStart_WhenInstantiated_ShouldReturnStartDate() {
        //Arrange
        LocalDate testDate = LocalDate.parse("3025-03-31");

        //Act
        LocalDate date = mockUnit.getUnitLease().getLeaseStart();

        //Assert
        assertEquals(date, testDate);
    }

    @Test
    void getFinalLeaseCost_WhenCalled_ShouldProrateLeaseCostUsedInMonth() {
        //Arrange
        LocalDate testDate = LocalDate.parse("3025-06-05");

        //Act
        Money testLeaseCost = mockUnit.getUnitLease().getFinalLeaseCost(testDate);

        // 5 days of lease cost in June - rounded, because doubles.
        double testCost = Math.round(testLeaseCost.getAmount().doubleValue());
        double cost = Math.round(Money.of(5 * 3000.00 / 30).getAmount().doubleValue());

        //Assert
        assertEquals(cost, testCost);
    }

    @Test
    void isLeaseable_WhenUnitIsDropship_shouldReturnTrue() {
        //Arrange
        Dropship mockDropship = mock(Dropship.class);
        Mockito.doReturn(true).when(mockDropship).isDropShip();

        //Act
        boolean returnValue = Lease.isLeasable(mockDropship);

        //Assert
        assertTrue(returnValue);
    }

    @Test
    void isLeaseable_WhenUnitIsMek_shouldReturnFalse() {
        //Arrange
        Mek mockMek = mock(Mek.class);
        Mockito.doReturn(false).when(mockMek).isDropShip();

        //Act
        boolean returnValue = Lease.isLeasable(mockMek);

        //Assert
        assertFalse(returnValue);
    }
}
