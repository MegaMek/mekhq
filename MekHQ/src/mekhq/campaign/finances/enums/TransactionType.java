/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.finances.enums;

import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum TransactionType {
    // region Enum Declarations
    BATTLE_LOSS_COMPENSATION("TransactionType.BATTLE_LOSS_COMPENSATION.text",
          "TransactionType.BATTLE_LOSS_COMPENSATION.toolTipText"),
    CONSTRUCTION("TransactionType.CONSTRUCTION.text", "TransactionType.CONSTRUCTION.toolTipText"),
    CONTRACT_PAYMENT("TransactionType.CONTRACT_PAYMENT.text", "TransactionType.CONTRACT_PAYMENT.toolTipText"),
    EDUCATION("TransactionType.EDUCATION.text", "TransactionType.EDUCATION.toolTipText"),
    EQUIPMENT_PURCHASE("TransactionType.EQUIPMENT_PURCHASE.text", "TransactionType.EQUIPMENT_PURCHASE.toolTipText"),
    EQUIPMENT_SALE("TransactionType.EQUIPMENT_SALE.text", "TransactionType.EQUIPMENT_SALE.toolTipText"),
    FINANCIAL_TERM_END_CARRYOVER("TransactionType.FINANCIAL_TERM_END_CARRYOVER.text",
          "TransactionType.FINANCIAL_TERM_END_CARRYOVER.toolTipText"),
    FINE("TransactionType.FINE.text", "TransactionType.FINE.toolTipText"),
    LOAN_PAYMENT("TransactionType.LOAN_PAYMENT.text", "TransactionType.LOAN_PAYMENT.toolTipText"),
    LOAN_PRINCIPAL("TransactionType.LOAN_PRINCIPAL.text", "TransactionType.LOAN_PRINCIPAL.toolTipText"),
    MAINTENANCE("TransactionType.MAINTENANCE.text", "TransactionType.MAINTENANCE.toolTipText"),
    MEDICAL_EXPENSES("TransactionType.MEDICAL_EXPENSES.text", "TransactionType.MEDICAL_EXPENSES.toolTipText"),
    MISCELLANEOUS("TransactionType.MISCELLANEOUS.text", "TransactionType.MISCELLANEOUS.toolTipText"),
    OVERHEAD("TransactionType.OVERHEAD.text", "TransactionType.OVERHEAD.toolTipText"),
    RANSOM("TransactionType.RANSOM.text", "TransactionType.RANSOM.toolTipText"),
    RECRUITMENT("TransactionType.RECRUITMENT.text", "TransactionType.RECRUITMENT.toolTipText"),
    RENT("TransactionType.RENT.text", "TransactionType.RENT.toolTipText"),
    REPAIRS("TransactionType.REPAIRS.text", "TransactionType.REPAIRS.toolTipText"),
    PAYOUT("TransactionType.PAYOUT.text", "TransactionType.PAYOUT.toolTipText"),
    SALARIES("TransactionType.SALARIES.text", "TransactionType.SALARIES.toolTipText"),
    SALVAGE("TransactionType.SALVAGE.text", "TransactionType.SALVAGE.toolTipText"),
    SALVAGE_EXCHANGE("TransactionType.SALVAGE_EXCHANGE.text", "TransactionType.SALVAGE_EXCHANGE.toolTipText"),
    STARTING_CAPITAL("TransactionType.STARTING_CAPITAL.text", "TransactionType.STARTING_CAPITAL.toolTipText"),
    TAXES("TransactionType.TAXES.text", "TransactionType.TAXES.toolTipText"),
    THEFT("TransactionType.THEFT.text", "TransactionType.THEFT.toolTipText"),
    TRANSPORTATION("TransactionType.TRANSPORTATION.text", "TransactionType.TRANSPORTATION.toolTipText"),
    UNIT_PURCHASE("TransactionType.UNIT_PURCHASE.text", "TransactionType.UNIT_PURCHASE.toolTipText"),
    UNIT_SALE("TransactionType.UNIT_SALE.text", "TransactionType.UNIT_SALE.toolTipText"),
    BONUS_EXCHANGE("TransactionType.BONUS_EXCHANGE.text", "TransactionType.BONUS_EXCHANGE.toolTipText"),
    WEALTH("TransactionType.WEALTH.text", "TransactionType.WEALTH.toolTipText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    // region Constructors
    TransactionType(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isBattleLossCompensation() {
        return this == BATTLE_LOSS_COMPENSATION;
    }

    public boolean isConstruction() {
        return this == CONSTRUCTION;
    }

    public boolean isContractPayment() {
        return this == CONTRACT_PAYMENT;
    }

    public boolean isEducation() {
        return this == EDUCATION;
    }

    public boolean isEquipmentPurchase() {
        return this == EQUIPMENT_PURCHASE;
    }

    public boolean isEquipmentSale() {
        return this == EQUIPMENT_SALE;
    }

    public boolean isFinancialTermEndCarryover() {
        return this == FINANCIAL_TERM_END_CARRYOVER;
    }

    public boolean isFine() {
        return this == FINE;
    }

    public boolean isLoanPayment() {
        return this == LOAN_PAYMENT;
    }

    public boolean isLoanPrincipal() {
        return this == LOAN_PRINCIPAL;
    }

    public boolean isMaintenance() {
        return this == MAINTENANCE;
    }

    public boolean isMedicalExpenses() {
        return this == MEDICAL_EXPENSES;
    }

    public boolean isMiscellaneous() {
        return this == MISCELLANEOUS;
    }

    public boolean isOverhead() {
        return this == OVERHEAD;
    }

    public boolean isRansom() {
        return this == RANSOM;
    }

    public boolean isRecruitment() {
        return this == RECRUITMENT;
    }

    public boolean isRent() {
        return this == RENT;
    }

    public boolean isRepairs() {
        return this == REPAIRS;
    }

    public boolean isPayout() {
        return this == PAYOUT;
    }

    public boolean isSalaries() {
        return this == SALARIES;
    }

    public boolean isSalvage() {
        return this == SALVAGE;
    }

    public boolean isSalvageExchange() {
        return this == SALVAGE_EXCHANGE;
    }

    public boolean isStartingCapital() {
        return this == STARTING_CAPITAL;
    }

    public boolean isTaxes() {
        return this == TAXES;
    }

    public boolean isTheft() {
        return this == THEFT;
    }

    public boolean isTransportation() {
        return this == TRANSPORTATION;
    }

    public boolean isUnitPurchase() {
        return this == UNIT_PURCHASE;
    }

    public boolean isUnitSale() {
        return this == UNIT_SALE;
    }

    public boolean isBonusExchange() {
        return this == BONUS_EXCHANGE;
    }

    public boolean isWealth() {
        return this == WEALTH;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    public static TransactionType parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return MISCELLANEOUS;
                case 1:
                    return EQUIPMENT_PURCHASE;
                case 2:
                    return UNIT_PURCHASE;
                case 3:
                    return SALARIES;
                case 4:
                    return OVERHEAD;
                case 5:
                    return MAINTENANCE;
                case 6:
                    return UNIT_SALE;
                case 7:
                    return EQUIPMENT_SALE;
                case 8:
                    return STARTING_CAPITAL;
                case 9:
                    return TRANSPORTATION;
                case 10:
                    return CONTRACT_PAYMENT;
                case 11:
                    return BATTLE_LOSS_COMPENSATION;
                case 12:
                    return SALVAGE_EXCHANGE;
                case 13:
                    return LOAN_PRINCIPAL;
                case 14:
                    return LOAN_PAYMENT;
                case 15:
                    return REPAIRS;
                case 16:
                    return RANSOM;
                case 17:
                    return EDUCATION;
                case 18:
                    return THEFT;
                case 19:
                    return PAYOUT;
                case 20:
                    return TAXES;
                case 21:
                    return BONUS_EXCHANGE;
                case 22:
                    return WEALTH;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(TransactionType.class)
              .error("Unable to parse " + text + " into a TransactionType. Returning MISCELLANEOUS.");
        return MISCELLANEOUS;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
