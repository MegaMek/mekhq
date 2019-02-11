/*
 * XmlMoneyWriter.java
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

import java.io.IOException;

import org.joda.money.BigMoney;
import org.joda.money.format.MoneyPrintContext;
import org.joda.money.format.MoneyPrinter;

/**
 * This is the writer used to write money amounts to strings that will be
 * stored in XML data files.
 *
 * @author Vicente Cartas Espinel <vicente.cartas at outlook.com>
 */
class XmlMoneyWriter implements MoneyPrinter {
    @Override
    public void print(MoneyPrintContext context, Appendable appendable, BigMoney money) throws IOException {
        appendable.append(money.getAmount().toString());
        appendable.append(" ");
        appendable.append(money.getCurrencyUnit().getCode());
    }
}
