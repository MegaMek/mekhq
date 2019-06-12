/*
 * Money.java
 *
 * Copyright (c) 2019 Vicente Cartas Espinel <vicente.cartas at outlook.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.finances;

import org.joda.money.BigMoney;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class represents an quantity of money and its associated
 * currency.
 *
 * @author Vicente Cartas Espinel <vicente.cartas at outlook.com>
 *
 */
public class Money implements Comparable<Money> {
    private BigMoney wrapped;

    private Money(BigMoney money) {
        assert money != null;
        this.wrapped = money;
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

    public static Money fromXmlString(String xmlData) {
        return new Money(CurrencyManager.getInstance().getXmlMoneyFormatter().parseBigMoney(xmlData));
    }

    public boolean isZero() {
        return this.wrapped.isZero();
    }

    public boolean isPositive() {
        return this.wrapped.isPositive();
    }

    public boolean isPositiveOrZero() {
        return this.wrapped.isPositive() ||
                this.wrapped.isZero();
    }

    public boolean isNegative() {
        return this.wrapped.isNegative();
    }

    public boolean isGreaterThan(Money other) {
        return this.wrapped.isGreaterThan(other.wrapped);
    }

    public boolean isGreaterOrEqualThan(Money other) {
        return this.wrapped.isGreaterThan(other.wrapped) ||
                this.wrapped.isEqual(other.wrapped);
    }

    public boolean isLessThan(Money other) {
        return this.wrapped.isLessThan(other.wrapped);
    }

    public BigDecimal getAmount() {
        return this.wrapped.getAmount();
    }

    public Money absolute() {
        if (this.isPositiveOrZero()) {
            return this;
        } else {
            return this.multipliedBy(-1);
        }
    }

    public Money plus(Money amount) {
        return new Money(this.wrapped.plus(amount.wrapped));
    }

    public Money plus(double amount) {
        return new Money(this.wrapped.plus(amount));
    }

    public Money plus(List<Money> amounts) {
        return new Money(this.wrapped.plus((Iterable<BigMoney>)(amounts.stream().map(x -> x.wrapped)::iterator)));
    }

    public Money minus(Money amount) {
        return new Money(this.wrapped.minus(amount.wrapped));
    }

    public Money minus(long amount) {
        return new Money(this.wrapped.minus(amount));
    }

    public Money minus(double amount) {
        return new Money(this.wrapped.minus(amount));
    }

    public Money multipliedBy(long amount) {
        return new Money(this.wrapped.multipliedBy(amount));
    }

    public Money multipliedBy(double amount) {
        return new Money(this.wrapped.multipliedBy(amount));
    }

    public Money dividedBy(double amount) {
        return new Money(this.wrapped.dividedBy(amount, RoundingMode.HALF_EVEN));
    }

    public Money dividedBy(Money money) {
        return new Money(this.wrapped.dividedBy(
                money.wrapped.getAmount(),
                RoundingMode.HALF_EVEN));
    }

    public String toXmlString() {
        return CurrencyManager.getInstance().getXmlMoneyFormatter().print(this.wrapped.toMoney(RoundingMode.HALF_EVEN));
    }

    public String toAmountString() {
        return CurrencyManager.getInstance().getUiAmountPrinter().print(this.wrapped.toMoney(RoundingMode.HALF_EVEN));
    }

    public String toAmountAndSymbolString() {
        return CurrencyManager.getInstance().getUiAmountAndSymbolPrinter().print(this.wrapped.toMoney(RoundingMode.HALF_EVEN));
    }

    public String toAmountAndNameString() {
        return CurrencyManager.getInstance().getUiAmountAndNamePrinter().print(this.wrapped.toMoney(RoundingMode.HALF_EVEN));
    }

    @Override
    public String toString() {
        return this.wrapped.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Money) {
            return this.wrapped.isEqual(((Money)obj).wrapped);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.wrapped.hashCode();
    }

    @Override
    public int compareTo(Money o) {
        if (null == o) {
            return -1;
        }
        return wrapped.compareTo(o.wrapped);
    }
}
