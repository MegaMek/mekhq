/*
 * XmlMoneyParser.java
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

import java.math.BigDecimal;

import org.joda.money.CurrencyUnit;
import org.joda.money.format.MoneyParseContext;
import org.joda.money.format.MoneyParser;

/**
 * This is the parser used to read money amounts from strings that came from
 * XML data files.
 *
 * @author Vicente Cartas Espinel <vicente.cartas at outlook.com>
 */
class XmlMoneyParser implements MoneyParser {
    @Override
    public void parse(MoneyParseContext context) {
        String moneyAsString = context.getText().toString();
        int separator = moneyAsString.indexOf(" ");

        BigDecimal moneyAmount;
        CurrencyUnit currency;

        // Check if this is an old save value with no currency data
        if (separator == -1) {
            moneyAmount = new BigDecimal(moneyAsString);
            currency = CurrencyManager.getInstance().getDefaultCurrency().getCurrencyUnit();
        } else {
            moneyAmount = new BigDecimal(moneyAsString.subSequence(0, separator).toString());
            currency = CurrencyUnit.of(moneyAsString.substring(separator + 1));
        }

        context.setAmount(moneyAmount);
        context.setCurrency(currency);
        context.setIndex(moneyAsString.length());
    }
}
