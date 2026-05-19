/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WeeklyNetWorth {
    private static final MMLogger LOGGER = MMLogger.create(WeeklyNetWorth.class);

    private LocalDate date;
    private Money amount;

    public WeeklyNetWorth(LocalDate date, Money amount) {
        this.date = date;
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public Money getMoney() {
        return amount;
    }

    private void setDate(LocalDate localDate) {
        this.date = localDate;
    }

    private void setAmount(Money money) {
        this.amount = money;
    }

    public void writeToXML(PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "weeklyNetWorth");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "date", getDate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "amount", getMoney());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "weeklyNetWorth");
    }

    static List<WeeklyNetWorth> parseWeeklyNetWorthFromXML(final Node wn) {
        if (!wn.hasChildNodes()) {
            return new ArrayList<>();
        }

        final NodeList nl = wn.getChildNodes();
        return IntStream.range(0, nl.getLength())
                     .mapToObj(nl::item)
                     .filter(node -> "weeklyNetWorth".equals(node.getNodeName()))
                     .map(WeeklyNetWorth::generateInstanceFromXML)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
    }

    public static WeeklyNetWorth generateInstanceFromXML(final Node wn) {
        final WeeklyNetWorth weeklyNetWorth = new WeeklyNetWorth(null, null);
        final NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    weeklyNetWorth.setDate(MHQXMLUtility.parseDate(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("amount")) {
                    weeklyNetWorth.setAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                }
            } catch (Exception exception) {
                LOGGER.error(exception, "exception loading WeeklyNetWorth from save file");
            }
        }
        if (weeklyNetWorth.getDate() == null || weeklyNetWorth.getMoney() == null) {
            return null;
        } else {
            return weeklyNetWorth;
        }
    }

}
