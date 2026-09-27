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
package mekhq.campaign.roleplay;

import java.io.PrintWriter;

import megamek.common.annotations.Nullable;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The details of one Fate Chart question, kept with its Oracle log record so the answer can be shown again later.
 *
 * @param question the question asked, or an empty string if the player did not type one
 * @param odds     the odds chosen
 * @param chaos    the chaos factor at the time
 * @param roll     the d100 roll
 * @param answer   the answer
 */
public record OracleAnswer(String question, FateChartOdds odds, int chaos, int roll, FateChartAnswer answer) {
    void writeToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "answer");
        if (!question.isBlank()) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "question", question);
        }
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "odds", odds.name());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "chaos", chaos);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "roll", roll);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "result", answer.name());
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "answer");
    }

    static @Nullable OracleAnswer parse(final Node node) {
        String question = "";
        FateChartOdds odds = null;
        int chaos = Roleplay.DEFAULT_CHAOS_FACTOR;
        int roll = 0;
        FateChartAnswer answer = null;
        final NodeList fields = node.getChildNodes();
        for (int i = 0; i < fields.getLength(); i++) {
            final Node field = fields.item(i);
            final String text = field.getTextContent().trim();
            switch (field.getNodeName()) {
                case "question" -> question = field.getTextContent();
                case "odds" -> odds = FateChartOdds.valueOf(text);
                case "chaos" -> chaos = Integer.parseInt(text);
                case "roll" -> roll = Integer.parseInt(text);
                case "result" -> answer = FateChartAnswer.valueOf(text);
                default -> { }
            }
        }
        return (odds == null || answer == null) ? null : new OracleAnswer(question, odds, chaos, roll, answer);
    }
}
