/*
 * MekHqMoneyUtil.java
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

import org.joda.money.*;
import org.joda.money.format.MoneyFormatter;

/**
 * Class with helper methods to operate on money classes from joda.money.
 */
public class MekHqMoneyUtil {
    public static CurrencyUnit defaultCurrency() {
        return CurrencyManager.getInstance().getDefaultCurrency();
    }

    public static Money zero() {
        return Money.zero(defaultCurrency());
    }

    public static BigMoney bigZero() {
        return BigMoney.zero(defaultCurrency());
    }

    public static Money money(double amount) {
        return Money.of(defaultCurrency(), amount);
    }

    public static BigMoney bigMoney(double amount) {
        return BigMoney.of(defaultCurrency(), amount);
    }

    public static MoneyFormatter uiAmountMoneyFormatter() {
        return CurrencyManager.getInstance().getUiAmountMoneyFormatter();
    }

    public static MoneyFormatter xmlMoneyFormatter() {
        return CurrencyManager.getInstance().getXmlMoneyFormatter();
    }

    public static MoneyFormatter shortUiMoneyPrinter() {
        return CurrencyManager.getInstance().getShortUiMoneyPrinter();
    }

    public static MoneyFormatter longUiMoneyPrinter() {
        return CurrencyManager.getInstance().getLongUiMoneyPrinter();
    }

    public static boolean isGreaterOrEqual(Money left, Money right) {
        return left.isGreaterThan(right) || left.isEqual(right);
    }

    public static boolean isGreaterOrEqual(BigMoney left, BigMoney right) {
        return left.isGreaterThan(right) || left.isEqual(right);
    }

    public static boolean isGreaterOrEqual(BigMoneyProvider left, BigMoneyProvider right) {
        return MoneyUtils.isPositiveOrZero(left.toBigMoney().minus(right.toBigMoney()));
    }
}
