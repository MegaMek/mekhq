/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.finances.enums;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransactionTypeTest {
    //region Variable Declarations
    private static final TransactionType[] types = TransactionType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("TransactionType.RECRUITMENT.toolTipText"),
                TransactionType.RECRUITMENT.getToolTipText());
        assertEquals(resources.getString("TransactionType.UNIT_SALE.toolTipText"),
                TransactionType.UNIT_SALE.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsBattleLossCompensation() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.BATTLE_LOSS_COMPENSATION) {
                assertTrue(transactionType.isBattleLossCompensation());
            } else {
                assertFalse(transactionType.isBattleLossCompensation());
            }
        }
    }

    @Test
    public void testIsConstruction() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.CONSTRUCTION) {
                assertTrue(transactionType.isConstruction());
            } else {
                assertFalse(transactionType.isConstruction());
            }
        }
    }

    @Test
    public void testIsContractPayment() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.CONTRACT_PAYMENT) {
                assertTrue(transactionType.isContractPayment());
            } else {
                assertFalse(transactionType.isContractPayment());
            }
        }
    }

    @Test
    public void testIsEducation() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.EDUCATION) {
                assertTrue(transactionType.isEducation());
            } else {
                assertFalse(transactionType.isEducation());
            }
        }
    }

    @Test
    public void testIsEquipmentPurchase() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.EQUIPMENT_PURCHASE) {
                assertTrue(transactionType.isEquipmentPurchase());
            } else {
                assertFalse(transactionType.isEquipmentPurchase());
            }
        }
    }

    @Test
    public void testIsEquipmentSale() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.EQUIPMENT_SALE) {
                assertTrue(transactionType.isEquipmentSale());
            } else {
                assertFalse(transactionType.isEquipmentSale());
            }
        }
    }

    @Test
    public void testIsFinancialTermEndCarryover() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.FINANCIAL_TERM_END_CARRYOVER) {
                assertTrue(transactionType.isFinancialTermEndCarryover());
            } else {
                assertFalse(transactionType.isFinancialTermEndCarryover());
            }
        }
    }

    @Test
    public void testIsFine() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.FINE) {
                assertTrue(transactionType.isFine());
            } else {
                assertFalse(transactionType.isFine());
            }
        }
    }

    @Test
    public void testIsLoanPayment() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.LOAN_PAYMENT) {
                assertTrue(transactionType.isLoanPayment());
            } else {
                assertFalse(transactionType.isLoanPayment());
            }
        }
    }

    @Test
    public void testIsLoanPrincipal() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.LOAN_PRINCIPAL) {
                assertTrue(transactionType.isLoanPrincipal());
            } else {
                assertFalse(transactionType.isLoanPrincipal());
            }
        }
    }

    @Test
    public void testIsMaintenance() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.MAINTENANCE) {
                assertTrue(transactionType.isMaintenance());
            } else {
                assertFalse(transactionType.isMaintenance());
            }
        }
    }

    @Test
    public void testIsMedicalExpenses() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.MEDICAL_EXPENSES) {
                assertTrue(transactionType.isMedicalExpenses());
            } else {
                assertFalse(transactionType.isMedicalExpenses());
            }
        }
    }

    @Test
    public void testIsMiscellaneous() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.MISCELLANEOUS) {
                assertTrue(transactionType.isMiscellaneous());
            } else {
                assertFalse(transactionType.isMiscellaneous());
            }
        }
    }

    @Test
    public void testIsOverhead() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.OVERHEAD) {
                assertTrue(transactionType.isOverhead());
            } else {
                assertFalse(transactionType.isOverhead());
            }
        }
    }

    @Test
    public void testIsRansom() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.RANSOM) {
                assertTrue(transactionType.isRansom());
            } else {
                assertFalse(transactionType.isRansom());
            }
        }
    }

    @Test
    public void testIsRecruitment() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.RECRUITMENT) {
                assertTrue(transactionType.isRecruitment());
            } else {
                assertFalse(transactionType.isRecruitment());
            }
        }
    }

    @Test
    public void testIsRent() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.RENT) {
                assertTrue(transactionType.isRent());
            } else {
                assertFalse(transactionType.isRent());
            }
        }
    }

    @Test
    public void testIsRepairs() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.REPAIRS) {
                assertTrue(transactionType.isRepairs());
            } else {
                assertFalse(transactionType.isRepairs());
            }
        }
    }

    @Test
    public void testIsPayout() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.PAYOUT) {
                assertTrue(transactionType.isPayout());
            } else {
                assertFalse(transactionType.isPayout());
            }
        }
    }

    @Test
    public void testIsSalaries() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.SALARIES) {
                assertTrue(transactionType.isSalaries());
            } else {
                assertFalse(transactionType.isSalaries());
            }
        }
    }

    @Test
    public void testIsSalvage() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.SALVAGE) {
                assertTrue(transactionType.isSalvage());
            } else {
                assertFalse(transactionType.isSalvage());
            }
        }
    }

    @Test
    public void testIsSalvageExchange() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.SALVAGE_EXCHANGE) {
                assertTrue(transactionType.isSalvageExchange());
            } else {
                assertFalse(transactionType.isSalvageExchange());
            }
        }
    }

    @Test
    public void testIsStartingCapital() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.STARTING_CAPITAL) {
                assertTrue(transactionType.isStartingCapital());
            } else {
                assertFalse(transactionType.isStartingCapital());
            }
        }
    }

    @Test
    public void testIsTaxes() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.TAXES) {
                assertTrue(transactionType.isTaxes());
            } else {
                assertFalse(transactionType.isTaxes());
            }
        }
    }

    @Test
    public void testIsTransportation() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.TRANSPORTATION) {
                assertTrue(transactionType.isTransportation());
            } else {
                assertFalse(transactionType.isTransportation());
            }
        }
    }

    @Test
    public void testIsUnitPurchase() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.UNIT_PURCHASE) {
                assertTrue(transactionType.isUnitPurchase());
            } else {
                assertFalse(transactionType.isUnitPurchase());
            }
        }
    }

    @Test
    public void testIsUnitSale() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.UNIT_SALE) {
                assertTrue(transactionType.isUnitSale());
            } else {
                assertFalse(transactionType.isUnitSale());
            }
        }
    }

    @Test
    public void testIsTheft() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.THEFT) {
                assertTrue(transactionType.isTheft());
            } else {
                assertFalse(transactionType.isTheft());
            }
        }
    }

    @Test
    public void testIsBonusPartExchange() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.BONUS_EXCHANGE) {
                assertTrue(transactionType.isBonusExchange());
            } else {
                assertFalse(transactionType.isBonusExchange());
            }
        }
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    @Test
    public void testParseFromString() {
        // Enum.valueOf Testing
        assertEquals(TransactionType.CONSTRUCTION, TransactionType.parseFromString("CONSTRUCTION"));
        assertEquals(TransactionType.FINANCIAL_TERM_END_CARRYOVER, TransactionType.parseFromString("FINANCIAL_TERM_END_CARRYOVER"));
        assertEquals(TransactionType.MEDICAL_EXPENSES, TransactionType.parseFromString("MEDICAL_EXPENSES"));

        // Parsing Legacy Testing
        assertEquals(TransactionType.FINANCIAL_TERM_END_CARRYOVER, TransactionType.parseFromString("CARRYOVER"));
        assertEquals(TransactionType.MISCELLANEOUS, TransactionType.parseFromString("0"));
        assertEquals(TransactionType.EQUIPMENT_PURCHASE, TransactionType.parseFromString("1"));
        assertEquals(TransactionType.UNIT_PURCHASE, TransactionType.parseFromString("2"));
        assertEquals(TransactionType.SALARIES, TransactionType.parseFromString("3"));
        assertEquals(TransactionType.OVERHEAD, TransactionType.parseFromString("4"));
        assertEquals(TransactionType.MAINTENANCE, TransactionType.parseFromString("5"));
        assertEquals(TransactionType.UNIT_SALE, TransactionType.parseFromString("6"));
        assertEquals(TransactionType.EQUIPMENT_SALE, TransactionType.parseFromString("7"));
        assertEquals(TransactionType.STARTING_CAPITAL, TransactionType.parseFromString("8"));
        assertEquals(TransactionType.TRANSPORTATION, TransactionType.parseFromString("9"));
        assertEquals(TransactionType.CONTRACT_PAYMENT, TransactionType.parseFromString("10"));
        assertEquals(TransactionType.BATTLE_LOSS_COMPENSATION, TransactionType.parseFromString("11"));
        assertEquals(TransactionType.SALVAGE_EXCHANGE, TransactionType.parseFromString("12"));
        assertEquals(TransactionType.LOAN_PRINCIPAL, TransactionType.parseFromString("13"));
        assertEquals(TransactionType.LOAN_PAYMENT, TransactionType.parseFromString("14"));
        assertEquals(TransactionType.REPAIRS, TransactionType.parseFromString("15"));
        assertEquals(TransactionType.RANSOM, TransactionType.parseFromString("16"));
        assertEquals(TransactionType.EDUCATION, TransactionType.parseFromString("17"));
        assertEquals(TransactionType.THEFT, TransactionType.parseFromString("18"));
        assertEquals(TransactionType.PAYOUT, TransactionType.parseFromString("19"));
        assertEquals(TransactionType.BONUS_EXCHANGE, TransactionType.parseFromString("20"));
        assertEquals(TransactionType.MISCELLANEOUS, TransactionType.parseFromString("21"));

        // Failure Testing
        assertEquals(TransactionType.MISCELLANEOUS, TransactionType.parseFromString("failureFailsFake"));
    }
    //endregion File I/O

    /**
     * Testing to ensure the toString Override is working as intended
     */
    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("TransactionType.BATTLE_LOSS_COMPENSATION.text"),
                TransactionType.BATTLE_LOSS_COMPENSATION.toString());
        assertEquals(resources.getString("TransactionType.MISCELLANEOUS.text"),
                TransactionType.MISCELLANEOUS.toString());
        assertEquals(resources.getString("TransactionType.TRANSPORTATION.text"),
                TransactionType.TRANSPORTATION.toString());
    }
}
