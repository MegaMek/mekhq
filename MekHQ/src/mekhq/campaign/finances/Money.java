/*
 * Copyright (c) 2019 - Vicente Cartas Espinel (vicente.cartas at outlook.com). All Rights Reserved.
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.Nonnull;
import org.joda.money.BigMoney;

/**
 * This class represents a quantity of money and its associated currency.
 *
 * @author Vicente Cartas Espinel (vicente.cartas at outlook.com)
 */
public record Money(BigMoney wrapped) implements Comparable<Money> {
    public Money {
        Objects.requireNonNull(wrapped);
    }

    public static Money of(double amount, Currency currency) {
        return new Money(BigMoney.of(currency.getCurrencyUnit(), amount));
    }

    public static Money of(double amount) {
        return Money.of(amount, CurrencyManager.getInstance().getDefaultCurrency());
    }

    public static Money zero(Currency currency) {
        return new Money(BigMoney.zero(currency.getCurrencyUnit()));
    }

    public static Money zero() {
        return zero(CurrencyManager.getInstance().getDefaultCurrency());
    }

    public boolean isZero() {
        return wrapped().isZero();
    }

    public boolean isPositive() {
        return wrapped().isPositive();
    }

    public boolean isPositiveOrZero() {
        return wrapped().isPositive() || wrapped().isZero();
    }

    public boolean isNegative() {
        return wrapped().isNegative();
    }

    public boolean isGreaterThan(Money other) {
        return wrapped().isGreaterThan(other.wrapped());
    }

    public boolean isGreaterOrEqualThan(Money other) {
        return wrapped().isGreaterThan(other.wrapped()) || wrapped().isEqual(other.wrapped());
    }

    public boolean isLessThan(Money other) {
        return wrapped().isLessThan(other.wrapped());
    }

    public BigDecimal getAmount() {
        return wrapped().getAmount();
    }

    public Money absolute() {
        return isPositiveOrZero() ? this : this.multipliedBy(-1);
    }

    public Money plus(Money amount) {
        if (amount == null) {
            return plus(0L);
        }

        return new Money(wrapped().plus(amount.wrapped()));
    }

    public Money plus(double amount) {
        return new Money(wrapped().plus(amount));
    }

    public Money plus(List<Money> amounts) {
        return new Money(wrapped().plus((Iterable<BigMoney>) (amounts.stream().map(Money::wrapped)::iterator)));
    }

    public Money minus(Money amount) {
        if (amount == null) {
            return minus(0L);
        }

        return new Money(wrapped().minus(amount.wrapped()));
    }

    public Money minus(long amount) {
        return new Money(wrapped().minus(amount));
    }

    public Money minus(double amount) {
        return new Money(wrapped().minus(amount));
    }

    public Money minus(List<Money> amounts) {
        return new Money(wrapped().minus((Iterable<BigMoney>) (amounts.stream().map(Money::wrapped)::iterator)));
    }

    public Money multipliedBy(long amount) {
        return new Money(wrapped().multipliedBy(amount));
    }

    public Money multipliedBy(double amount) {
        return new Money(wrapped().multipliedBy(amount));
    }

    public Money dividedBy(double amount) {
        return new Money(wrapped().dividedBy(amount, RoundingMode.HALF_EVEN));
    }

    public Money dividedBy(Money money) {
        return new Money(wrapped().dividedBy(money.wrapped().getAmount(), RoundingMode.HALF_EVEN));
    }

    public String toAmountString() {
        return CurrencyManager.getInstance().getUiAmountPrinter().print(wrapped().toMoney(RoundingMode.HALF_EVEN));
    }

    public String toAmountAndSymbolString() {
        return CurrencyManager.getInstance()
                     .getUiAmountAndSymbolPrinter()
                     .print(wrapped().toMoney(RoundingMode.HALF_EVEN));
    }

    /**
     * @return a new money object, rounded to use a scale of 0 with no trailing 0's
     */
    public Money round() {
        return new Money(wrapped().withScale(0, RoundingMode.HALF_UP));
    }

    // region File I/O
    public String toXmlString() {
        return CurrencyManager.getInstance().getXmlMoneyFormatter().print(wrapped().toMoney(RoundingMode.HALF_EVEN));
    }

    public static Money fromXmlString(String xmlData) {
        return new Money(CurrencyManager.getInstance().getXmlMoneyFormatter().parseBigMoney(xmlData));
    }
    // endregion File I/O

    @Override
    @Nonnull
    public String toString() {
        return wrapped().toString();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Money) && wrapped().isEqual(((Money) obj).wrapped());
    }

    @Override
    public int compareTo(@Nonnull Money o) {
        return wrapped().compareTo(o.wrapped());
    }
}
