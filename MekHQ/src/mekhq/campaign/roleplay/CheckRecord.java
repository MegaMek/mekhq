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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The result of a check made from the Oracle console, kept with its journal entry so the console can show it again.
 *
 * @param opposed {@code true} for an opposed check, where the first side acts and the second defends
 * @param reason  what the check was for, or an empty string
 * @param sides   one side per person rolling, in the order they were picked
 */
public record CheckRecord(boolean opposed, String reason, List<Side> sides) {
    /**
     * One person's roll.
     *
     * @param name     the person's name when the check was made
     * @param personId the person's id if they are in Personnel, otherwise {@code null}
     * @param action   what they rolled for, such as "Negotiation (Hard +2)" or "Veteran"
     * @param target   the target as players read it, such as "7+"
     * @param roll     the total rolled, after any Edge re-roll
     * @param dice     the dice of that roll, including any die dropped for natural aptitude
     * @param margin   how far the roll beat the target; negative for a failure
     * @param usedEdge {@code true} if they spent Edge to re-roll
     * @param won      {@code true} if the check succeeded or, in an opposed check, if this side won
     */
    public record Side(String name, @Nullable UUID personId, String action, String target, int roll,
          List<Integer> dice, int margin, boolean usedEdge, boolean won) {}

    /**
     * @return how many sides succeeded or won
     */
    public int countWon() {
        return (int) sides.stream().filter(Side::won).count();
    }

    /**
     * @return for an opposed check, the difference between the two margins; otherwise 0
     */
    public int winningDifference() {
        return (opposed && sides.size() == 2) ? Math.abs(sides.get(0).margin() - sides.get(1).margin()) : 0;
    }

    void writeToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "check");
        if (opposed) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "opposed", true);
        }
        if (!reason.isBlank()) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "reason", reason);
        }
        for (Side side : sides) {
            MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "side");
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "name", side.name());
            if (side.personId() != null) {
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "person", side.personId());
            }
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "action", side.action());
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "target", side.target());
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "roll", side.roll());
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "dice",
                  String.join(",", side.dice().stream().map(String::valueOf).toList()));
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "margin", side.margin());
            if (side.usedEdge()) {
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "edge", true);
            }
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "won", side.won());
            MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "side");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "check");
    }

    static @Nullable CheckRecord parse(final Node node) {
        boolean opposed = false;
        String reason = "";
        final List<Side> sides = new ArrayList<>();
        final NodeList fields = node.getChildNodes();
        for (int i = 0; i < fields.getLength(); i++) {
            final Node field = fields.item(i);
            switch (field.getNodeName()) {
                case "opposed" -> opposed = MathUtility.parseBoolean(field.getTextContent().trim());
                case "reason" -> reason = field.getTextContent();
                case "side" -> sides.add(parseSide(field));
                default -> { }
            }
        }
        return sides.isEmpty() ? null : new CheckRecord(opposed, reason, sides);
    }

    private static Side parseSide(final Node node) {
        String name = "";
        UUID personId = null;
        String action = "";
        String target = "";
        int roll = 0;
        final List<Integer> dice = new ArrayList<>();
        int margin = 0;
        boolean usedEdge = false;
        boolean won = false;
        final NodeList fields = node.getChildNodes();
        for (int i = 0; i < fields.getLength(); i++) {
            final Node field = fields.item(i);
            final String text = field.getTextContent().trim();
            switch (field.getNodeName()) {
                case "name" -> name = field.getTextContent();
                case "person" -> personId = UUID.fromString(text);
                case "action" -> action = field.getTextContent();
                case "target" -> target = text;
                case "roll" -> roll = MathUtility.parseInt(text);
                case "dice" -> {
                    for (String die : text.split(",")) {
                        if (!die.isBlank()) {
                            dice.add(MathUtility.parseInt(die.trim()));
                        }
                    }
                }
                case "margin" -> margin = MathUtility.parseInt(text);
                case "edge" -> usedEdge = MathUtility.parseBoolean(text);
                case "won" -> won = MathUtility.parseBoolean(text);
                default -> { }
            }
        }
        return new Side(name, personId, action, target, roll, List.copyOf(dice), margin, usedEdge, won);
    }
}
