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

package mekhq.campaign.finances;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void getLeaseCostNow_WhenNotThe1st_ReturnsNull() {
        //Arrange
        LocalDate testDate = LocalDate.parse("3025-03-02");

        //Act
        Money testCost = mockUnit.getUnitLease().getLeaseCostNow(testDate);

        //Assert
        assertNull(testCost);
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
    void getFirstLeaseCost_WhenNotThe1st_ReturnsNull() {
        LocalDate date = LocalDate.parse("3025-04-02");

        //Act
        Money testCost = mockUnit.getUnitLease().getFirstLeaseCost(date);

        //Assert
        assertNull(testCost);
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
