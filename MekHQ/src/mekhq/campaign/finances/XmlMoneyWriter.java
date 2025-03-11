/*
 * Copyright (c) 2019 Vicente Cartas Espinel (vicente.cartas at outlook.com). All rights reserved.
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
 * @author Vicente Cartas Espinel (vicente.cartas at outlook.com)
 */
class XmlMoneyWriter implements MoneyPrinter {
    @Override
    public void print(MoneyPrintContext context, Appendable appendable, BigMoney money) throws IOException {
        appendable.append(money.getAmount().toString());
        appendable.append(" ");
        appendable.append(money.getCurrencyUnit().getCode());
    }
}
