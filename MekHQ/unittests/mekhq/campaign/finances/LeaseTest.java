package mekhq.campaign.finances;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


public class LeaseTest {

    @Test
    void getLeaseCosts_WhenMonthIsCalculatedForScenarios_ShouldCalculateCorrectLeaseAmounts() {
        //Arrange
        Unit unit = new Unit();
        Unit mockUnit = Mockito.spy(unit);
        Mockito.doReturn(Money.of(600000.00)).when(mockUnit).getSellValue();

        Lease testLease = new Lease(LocalDate.parse("3025-03-31"), mockUnit);
        mockUnit.addLease(testLease);
        LocalDate dateForTest2 = LocalDate.parse("3025-05-01");
        LocalDate dateForTest3 = LocalDate.parse("3025-04-01");
        LocalDate dateForTest4 = LocalDate.parse("3025-03-25");

        //Act
        Money testCost = mockUnit.getUnitLease().getLeaseCost();
        Money testCost2 = mockUnit.getUnitLease().getLeaseCostNow(dateForTest2);
        Money testCost3 = mockUnit.getUnitLease().getFirstLeaseCost(dateForTest3);
        Money testCost4 = mockUnit.getUnitLease().getLeaseCostNow(dateForTest4);

        //Assert
        assertEquals(Money.of(3000.00), testCost); // this should be the base cost of the lease
        assertEquals(Money.of(3000.00), testCost2);// This should be a full month's lease
        assertEquals(Math.round(Money.of(3000.00 / 31).getAmount().doubleValue()),
              Math.round(testCost3.getAmount().doubleValue())); // This should be a single day's lease
        assertEquals(Money.of(0.00), testCost4); // This checks leases that have not started yet
    }
}
