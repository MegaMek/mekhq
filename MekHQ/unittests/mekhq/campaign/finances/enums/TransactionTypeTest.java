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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import mekhq.MekHQ;

class TransactionTypeTest {
    // region Variable Declarations
    private static final TransactionType[] types = TransactionType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
            MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Getters
    @Test
    void testGetToolTipText() {
        assertEquals(resources.getString("TransactionType.RECRUITMENT.toolTipText"),
                TransactionType.RECRUITMENT.getToolTipText());
        assertEquals(resources.getString("TransactionType.UNIT_SALE.toolTipText"),
                TransactionType.UNIT_SALE.getToolTipText());
    }
    // endregion Getters

    // region Boolean Comparison Methods
    @Test
    void testIsBattleLossCompensation() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.BATTLE_LOSS_COMPENSATION) {
                assertTrue(transactionType.isBattleLossCompensation());
            } else {
                assertFalse(transactionType.isBattleLossCompensation());
            }
        }
    }

    @Test
    void testIsConstruction() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.CONSTRUCTION) {
                assertTrue(transactionType.isConstruction());
            } else {
                assertFalse(transactionType.isConstruction());
            }
        }
    }

    @Test
    void testIsContractPayment() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.CONTRACT_PAYMENT) {
                assertTrue(transactionType.isContractPayment());
            } else {
                assertFalse(transactionType.isContractPayment());
            }
        }
    }

    @Test
    void testIsEducation() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.EDUCATION) {
                assertTrue(transactionType.isEducation());
            } else {
                assertFalse(transactionType.isEducation());
            }
        }
    }

    @Test
    void testIsEquipmentPurchase() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.EQUIPMENT_PURCHASE) {
                assertTrue(transactionType.isEquipmentPurchase());
            } else {
                assertFalse(transactionType.isEquipmentPurchase());
            }
        }
    }

    @Test
    void testIsEquipmentSale() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.EQUIPMENT_SALE) {
                assertTrue(transactionType.isEquipmentSale());
            } else {
                assertFalse(transactionType.isEquipmentSale());
            }
        }
    }

    @Test
    void testIsFinancialTermEndCarryover() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.FINANCIAL_TERM_END_CARRYOVER) {
                assertTrue(transactionType.isFinancialTermEndCarryover());
            } else {
                assertFalse(transactionType.isFinancialTermEndCarryover());
            }
        }
    }

    @Test
    void testIsFine() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.FINE) {
                assertTrue(transactionType.isFine());
            } else {
                assertFalse(transactionType.isFine());
            }
        }
    }

    @Test
    void testIsLoanPayment() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.LOAN_PAYMENT) {
                assertTrue(transactionType.isLoanPayment());
            } else {
                assertFalse(transactionType.isLoanPayment());
            }
        }
    }

    @Test
    void testIsLoanPrincipal() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.LOAN_PRINCIPAL) {
                assertTrue(transactionType.isLoanPrincipal());
            } else {
                assertFalse(transactionType.isLoanPrincipal());
            }
        }
    }

    @Test
    void testIsMaintenance() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.MAINTENANCE) {
                assertTrue(transactionType.isMaintenance());
            } else {
                assertFalse(transactionType.isMaintenance());
            }
        }
    }

    @Test
    void testIsMedicalExpenses() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.MEDICAL_EXPENSES) {
                assertTrue(transactionType.isMedicalExpenses());
            } else {
                assertFalse(transactionType.isMedicalExpenses());
            }
        }
    }

    @Test
    void testIsMiscellaneous() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.MISCELLANEOUS) {
                assertTrue(transactionType.isMiscellaneous());
            } else {
                assertFalse(transactionType.isMiscellaneous());
            }
        }
    }

    @Test
    void testIsOverhead() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.OVERHEAD) {
                assertTrue(transactionType.isOverhead());
            } else {
                assertFalse(transactionType.isOverhead());
            }
        }
    }

    @Test
    void testIsRansom() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.RANSOM) {
                assertTrue(transactionType.isRansom());
            } else {
                assertFalse(transactionType.isRansom());
            }
        }
    }

    @Test
    void testIsRecruitment() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.RECRUITMENT) {
                assertTrue(transactionType.isRecruitment());
            } else {
                assertFalse(transactionType.isRecruitment());
            }
        }
    }

    @Test
    void testIsRent() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.RENT) {
                assertTrue(transactionType.isRent());
            } else {
                assertFalse(transactionType.isRent());
            }
        }
    }

    @Test
    void testIsRepairs() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.REPAIRS) {
                assertTrue(transactionType.isRepairs());
            } else {
                assertFalse(transactionType.isRepairs());
            }
        }
    }

    @Test
    void testIsPayout() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.PAYOUT) {
                assertTrue(transactionType.isPayout());
            } else {
                assertFalse(transactionType.isPayout());
            }
        }
    }

    @Test
    void testIsSalaries() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.SALARIES) {
                assertTrue(transactionType.isSalaries());
            } else {
                assertFalse(transactionType.isSalaries());
            }
        }
    }

    @Test
    void testIsSalvage() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.SALVAGE) {
                assertTrue(transactionType.isSalvage());
            } else {
                assertFalse(transactionType.isSalvage());
            }
        }
    }

    @Test
    void testIsSalvageExchange() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.SALVAGE_EXCHANGE) {
                assertTrue(transactionType.isSalvageExchange());
            } else {
                assertFalse(transactionType.isSalvageExchange());
            }
        }
    }

    @Test
    void testIsStartingCapital() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.STARTING_CAPITAL) {
                assertTrue(transactionType.isStartingCapital());
            } else {
                assertFalse(transactionType.isStartingCapital());
            }
        }
    }

    @Test
    void testIsTaxes() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.TAXES) {
                assertTrue(transactionType.isTaxes());
            } else {
                assertFalse(transactionType.isTaxes());
            }
        }
    }

    @Test
    void testIsTransportation() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.TRANSPORTATION) {
                assertTrue(transactionType.isTransportation());
            } else {
                assertFalse(transactionType.isTransportation());
            }
        }
    }

    @Test
    void testIsUnitPurchase() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.UNIT_PURCHASE) {
                assertTrue(transactionType.isUnitPurchase());
            } else {
                assertFalse(transactionType.isUnitPurchase());
            }
        }
    }

    @Test
    void testIsUnitSale() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.UNIT_SALE) {
                assertTrue(transactionType.isUnitSale());
            } else {
                assertFalse(transactionType.isUnitSale());
            }
        }
    }

    @Test
    void testIsTheft() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.THEFT) {
                assertTrue(transactionType.isTheft());
            } else {
                assertFalse(transactionType.isTheft());
            }
        }
    }

    @Test
    void testIsBonusPartExchange() {
        for (final TransactionType transactionType : types) {
            if (transactionType == TransactionType.BONUS_EXCHANGE) {
                assertTrue(transactionType.isBonusExchange());
            } else {
                assertFalse(transactionType.isBonusExchange());
            }
        }
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    @Test
    void testParseFromString() {
        // Enum.valueOf Testing
        assertEquals(TransactionType.CONSTRUCTION, TransactionType.parseFromString("CONSTRUCTION"));
        assertEquals(TransactionType.FINANCIAL_TERM_END_CARRYOVER,
                TransactionType.parseFromString("FINANCIAL_TERM_END_CARRYOVER"));
        assertEquals(TransactionType.MEDICAL_EXPENSES, TransactionType.parseFromString("MEDICAL_EXPENSES"));

        // Failure Testing
        assertEquals(TransactionType.MISCELLANEOUS, TransactionType.parseFromString("failureFailsFake"));
    }
    // endregion File I/O

    /**
     * Testing to ensure the toString Override is working as intended
     */
    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("TransactionType.BATTLE_LOSS_COMPENSATION.text"),
                TransactionType.BATTLE_LOSS_COMPENSATION.toString());
        assertEquals(resources.getString("TransactionType.MISCELLANEOUS.text"),
                TransactionType.MISCELLANEOUS.toString());
        assertEquals(resources.getString("TransactionType.TRANSPORTATION.text"),
                TransactionType.TRANSPORTATION.toString());
    }
}
