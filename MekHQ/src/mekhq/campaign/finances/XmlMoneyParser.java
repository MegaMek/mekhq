/*
 * Copyright (c) 2019 Vicente Cartas Espinel (vicente.cartas at outlook.com). All rights reserved.
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

import org.joda.money.CurrencyUnit;
import org.joda.money.format.MoneyParseContext;
import org.joda.money.format.MoneyParser;

/**
 * This is the parser used to read money amounts from strings that came from XML data files.
 *
 * @author Vicente Cartas Espinel (vicente.cartas at outlook.com)
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
