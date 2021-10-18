/*
 * Money.java
 *
 * Copyright (c) 2019 - Vicente Cartas Espinel <vicente.cartas at outlook.com>. All Rights Reserved.
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package mekhq.campaign.finances;

import org.joda.money.BigMoney;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * This class represents an quantity of money and its associated
 * currency.
 *
 * @author Vicente Cartas Espinel <vicente.cartas at outlook.com>
 */
public class Money implements Comparable<Money>, Serializable {
    private static final long serialVersionUID = 2018272535276369842L;
    private BigMoney wrapped;

    private Money(BigMoney money) {
        assert money != null;
        this.wrapped = money;
    }

    private BigMoney getWrapped() {
        return wrapped;
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
        return getWrapped().isZero();
    }

    public boolean isPositive() {
        return getWrapped().isPositive();
    }

    public boolean isPositiveOrZero() {
        return getWrapped().isPositive() || getWrapped().isZero();
    }

    public boolean isNegative() {
        return getWrapped().isNegative();
    }

    public boolean isGreaterThan(Money other) {
        return getWrapped().isGreaterThan(other.getWrapped());
    }

    public boolean isGreaterOrEqualThan(Money other) {
        return getWrapped().isGreaterThan(other.getWrapped()) || getWrapped().isEqual(other.getWrapped());
    }

    public boolean isLessThan(Money other) {
        return getWrapped().isLessThan(other.getWrapped());
    }

    public BigDecimal getAmount() {
        return getWrapped().getAmount();
    }

    public Money absolute() {
        return isPositiveOrZero() ? this : this.multipliedBy(-1);
    }

    public Money plus(Money amount) {
        if (amount == null) {
            return plus(0L);
        }

        return new Money(getWrapped().plus(amount.getWrapped()));
    }

    public Money plus(double amount) {
        return new Money(getWrapped().plus(amount));
    }

    public Money plus(List<Money> amounts) {
        return new Money(getWrapped().plus((Iterable<BigMoney>) (amounts.stream().map(Money::getWrapped)::iterator)));
    }

    public Money minus(Money amount) {
        if (amount == null) {
            return minus(0L);
        }

        return new Money(getWrapped().minus(amount.getWrapped()));
    }

    public Money minus(long amount) {
        return new Money(getWrapped().minus(amount));
    }

    public Money minus(double amount) {
        return new Money(getWrapped().minus(amount));
    }

    public Money multipliedBy(long amount) {
        return new Money(getWrapped().multipliedBy(amount));
    }

    public Money multipliedBy(double amount) {
        return new Money(getWrapped().multipliedBy(amount));
    }

    public Money dividedBy(double amount) {
        return new Money(getWrapped().dividedBy(amount, RoundingMode.HALF_EVEN));
    }

    public Money dividedBy(Money money) {
        return new Money(getWrapped().dividedBy(money.getWrapped().getAmount(), RoundingMode.HALF_EVEN));
    }

    public String toAmountString() {
        return CurrencyManager.getInstance().getUiAmountPrinter().print(getWrapped().toMoney(RoundingMode.HALF_EVEN));
    }

    public String toAmountAndSymbolString() {
        return CurrencyManager.getInstance().getUiAmountAndSymbolPrinter().print(getWrapped().toMoney(RoundingMode.HALF_EVEN));
    }

    public String toAmountAndNameString() {
        return CurrencyManager.getInstance().getUiAmountAndNamePrinter().print(getWrapped().toMoney(RoundingMode.HALF_EVEN));
    }

    //region File I/O
    public String toXmlString() {
        return CurrencyManager.getInstance().getXmlMoneyFormatter().print(getWrapped().toMoney(RoundingMode.HALF_EVEN));
    }

    public static Money fromXmlString(String xmlData) {
        return new Money(CurrencyManager.getInstance().getXmlMoneyFormatter().parseBigMoney(xmlData));
    }
    //endregion File I/O

    @Override
    public String toString() {
        return getWrapped().toString();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Money) && getWrapped().isEqual(((Money) obj).getWrapped());
    }

    @Override
    public int hashCode() {
        return this.wrapped.hashCode();
    }

    @Override
    public int compareTo(Money o) {
        return (o != null) ? getWrapped().compareTo(o.getWrapped()) : -1;
    }
}
