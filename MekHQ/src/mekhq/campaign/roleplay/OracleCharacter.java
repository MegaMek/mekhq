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
import java.util.UUID;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A person the Oracle can pick when a random event involves an NPC. A character may be linked to someone in the
 * campaign's personnel, in which case its name follows theirs.
 *
 * <p>Removing a character from the cast only marks it inactive, so journal entries tagged with it keep their name and
 * the character can be restored. The Oracle only picks active characters.</p>
 */
public class OracleCharacter {
    private UUID id = UUID.randomUUID();
    private String name;
    private UUID personId;
    private boolean active = true;
    private NpcRating rating;
    private String notes = "";

    /**
     * @param name     the character's name
     * @param personId the linked person's id, or {@code null} for a character the player typed in
     */
    public OracleCharacter(final String name, final @Nullable UUID personId) {
        this.name = name;
        this.personId = personId;
    }

    /**
     * @return the character's permanent id, used to tag journal entries
     */
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the id of the linked person, or {@code null} if the character is not linked to personnel
     */
    public @Nullable UUID getPersonId() {
        return personId;
    }

    /**
     * @return {@code true} if the character is linked to someone in the campaign's personnel
     */
    public boolean isLinked() {
        return personId != null;
    }

    /**
     * @return {@code true} if the character is in the cast; {@code false} if it has been removed
     */
    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    /**
     * @return the rating last used for this character in an opposed check, or {@code null} if none has been chosen
     */
    public @Nullable NpcRating getRating() {
        return rating;
    }

    public void setRating(final @Nullable NpcRating rating) {
        this.rating = rating;
    }

    /**
     * @return the player's notes on this character, such as a generated profile; plain text, possibly empty
     */
    public String getNotes() {
        return notes;
    }

    public void setNotes(final @Nullable String notes) {
        this.notes = (notes == null) ? "" : notes.strip();
    }

    @Override
    public String toString() {
        return name;
    }

    void writeToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "character");
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "id", id);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "name", name);
        if (personId != null) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "person", personId);
        }
        if (!active) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "active", false);
        }
        if (rating != null) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "rating", rating.name());
        }
        if (!notes.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "notes", notes);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "character");
    }

    /**
     * Reads a {@code <character>} element. Saves from before characters had ids stored just the name as the element's
     * text; those become a new, unlinked character.
     *
     * @param node the {@code <character>} element
     *
     * @return the character
     */
    static OracleCharacter parse(final Node node) {
        final NodeList fields = node.getChildNodes();
        boolean structured = false;
        for (int i = 0; i < fields.getLength(); i++) {
            structured |= fields.item(i).getNodeType() == Node.ELEMENT_NODE;
        }
        if (!structured) {
            return new OracleCharacter(node.getTextContent().trim(), null);
        }

        final OracleCharacter character = new OracleCharacter("", null);
        for (int i = 0; i < fields.getLength(); i++) {
            final Node field = fields.item(i);
            final String text = field.getTextContent();
            switch (field.getNodeName()) {
                case "id" -> character.id = UUID.fromString(text.trim());
                case "name" -> character.name = text;
                case "person" -> character.personId = UUID.fromString(text.trim());
                case "active" -> character.active = MathUtility.parseBoolean(text.trim());
                case "rating" -> character.rating = parseRating(text.trim());
                case "notes" -> character.notes = text.strip();
                default -> { }
            }
        }
        return character;
    }

    /** An unknown rating, perhaps from a newer version, is dropped rather than losing the character. */
    private static @Nullable NpcRating parseRating(final String text) {
        try {
            return NpcRating.valueOf(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
