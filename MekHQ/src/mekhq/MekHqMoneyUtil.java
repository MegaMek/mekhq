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
package mekhq;

import org.joda.money.BigMoney;
import org.joda.money.BigMoneyProvider;
import org.joda.money.Money;
import org.joda.money.MoneyUtils;

/**
 * Class with helper methods to operate on money classes from joda.money.
 */
public class MekHqMoneyUtil {
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
