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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * One line of the travel log: a party departing, arriving, first being seen somewhere, or a base being set up or
 * closed. Contract rows shown alongside are built from the campaign's contracts and are never saved.
 */
public class TravelRecord {
    private static final MMLogger LOGGER = MMLogger.create(TravelRecord.class);

    /** What happened. */
    public enum Kind {
        /** Set off towards another system. */
        DEPARTED,
        /** Reached the end of a journey. */
        ARRIVED,
        /** Already here when the log first saw this party. */
        PRESENT,
        /** A base was set up. */
        BASE_ESTABLISHED,
        /** A base was closed. */
        BASE_CLOSED,
        /** A contract started in this system. Built from the campaign's contracts, never saved. */
        CONTRACT_STARTED,
        /** A contract ended in this system. Built from the campaign's contracts, never saved. */
        CONTRACT_ENDED
    }

    /** Who travelled. */
    public enum Party {
        MAIN_FORCE,
        BASE,
        CAST
    }

    private final LocalDate date;
    private final Kind kind;
    private final Party party;
    private final UUID partyId;
    private final String partyName;
    private final String systemId;
    private final String systemName;
    private final String otherSystemId;
    private final String otherSystemName;
    private final List<UUID> characters;
    private final boolean reconstructed;

    /**
     * @param date            the in-game date
     * @param kind            what happened
     * @param party           who it happened to
     * @param partyId         the base's or cast member's id; {@code null} for the main force or a contract
     * @param partyName       the party's display name, or the contract's name
     * @param systemId        the system it happened in: the origin of a departure, the destination of an arrival
     * @param systemName      that system's display name
     * @param otherSystemId   a departure's destination or an arrival's origin, if known
     * @param otherSystemName that system's display name
     * @param characters      the cast members involved
     * @param reconstructed   {@code true} if rebuilt from the campaign's history rather than seen as it happened
     */
    public TravelRecord(final LocalDate date, final Kind kind, final Party party, final @Nullable UUID partyId,
          final String partyName, final @Nullable String systemId, final String systemName,
          final @Nullable String otherSystemId, final @Nullable String otherSystemName, final List<UUID> characters,
          final boolean reconstructed) {
        this.date = Objects.requireNonNull(date);
        this.kind = Objects.requireNonNull(kind);
        this.party = Objects.requireNonNull(party);
        this.partyId = partyId;
        this.partyName = (partyName == null) ? "" : partyName;
        this.systemId = systemId;
        this.systemName = (systemName == null) ? "" : systemName;
        this.otherSystemId = otherSystemId;
        this.otherSystemName = otherSystemName;
        this.characters = List.copyOf(characters);
        this.reconstructed = reconstructed;
    }

    public LocalDate getDate() {
        return date;
    }

    public Kind getKind() {
        return kind;
    }

    public Party getParty() {
        return party;
    }

    public @Nullable UUID getPartyId() {
        return partyId;
    }

    public String getPartyName() {
        return partyName;
    }

    public @Nullable String getSystemId() {
        return systemId;
    }

    public String getSystemName() {
        return systemName;
    }

    public @Nullable String getOtherSystemId() {
        return otherSystemId;
    }

    public @Nullable String getOtherSystemName() {
        return otherSystemName;
    }

    /**
     * @return the cast members involved, in the order they were recorded
     */
    public List<UUID> getCharacters() {
        return characters;
    }

    public boolean isReconstructed() {
        return reconstructed;
    }

    /**
     * @return {@code true} if this record moves or places a party, rather than describing a contract
     */
    public boolean isMovement() {
        return kind != Kind.CONTRACT_STARTED && kind != Kind.CONTRACT_ENDED;
    }

    /**
     * @param party a party
     * @param id    the party's id, or {@code null} for the main force
     *
     * @return {@code true} if this record is about that party
     */
    public boolean concerns(final Party party, final @Nullable UUID id) {
        if (this.party != party) {
            return false;
        }
        if (party == Party.CAST) {
            return id != null && characters.contains(id);
        }
        return Objects.equals(partyId, id);
    }

    // region XML

    static void writeListToXML(final PrintWriter writer, int indent, final List<TravelRecord> records) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "records");
        for (TravelRecord record : records) {
            MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "record");
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "date", record.date);
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "kind", record.kind.name());
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "party", record.party.name());
            if (record.partyId != null) {
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "partyId", record.partyId);
            }
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "partyName", record.partyName);
            if (record.systemId != null) {
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "systemId", record.systemId);
            }
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "systemName", record.systemName);
            if (record.otherSystemId != null) {
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "otherSystemId", record.otherSystemId);
            }
            if (record.otherSystemName != null) {
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "otherSystemName", record.otherSystemName);
            }
            for (UUID character : record.characters) {
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "cast", character);
            }
            if (record.reconstructed) {
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "reconstructed", true);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "record");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "records");
    }

    /**
     * Reads a {@code <records>} element. A record that cannot be read is logged and skipped, keeping the rest.
     */
    static List<TravelRecord> parseList(final Node node) {
        final List<TravelRecord> records = new ArrayList<>();
        final NodeList children = node.getChildNodes();
        int failures = 0;
        Exception firstFailure = null;
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeName().equalsIgnoreCase("record")) {
                try {
                    records.add(parse(child));
                } catch (Exception exception) {
                    failures++;
                    firstFailure = (firstFailure == null) ? exception : firstFailure;
                }
            }
        }
        if (firstFailure != null) {
            LOGGER.error(firstFailure, "Skipped {} unreadable travel log records; the first failure follows",
                  failures);
        }
        return records;
    }

    private static TravelRecord parse(final Node node) {
        LocalDate date = null;
        Kind kind = null;
        Party party = null;
        UUID partyId = null;
        String partyName = "";
        String systemId = null;
        String systemName = "";
        String otherSystemId = null;
        String otherSystemName = null;
        final List<UUID> characters = new ArrayList<>();
        boolean reconstructed = false;
        final NodeList fields = node.getChildNodes();
        for (int i = 0; i < fields.getLength(); i++) {
            final Node field = fields.item(i);
            if (field.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String text = field.getTextContent().trim();
            switch (field.getNodeName()) {
                case "date" -> date = MHQXMLUtility.parseDate(text);
                case "kind" -> kind = Kind.valueOf(text);
                case "party" -> party = Party.valueOf(text);
                case "partyId" -> partyId = UUID.fromString(text);
                case "partyName" -> partyName = text;
                case "systemId" -> systemId = text;
                case "systemName" -> systemName = text;
                case "otherSystemId" -> otherSystemId = text;
                case "otherSystemName" -> otherSystemName = text;
                case "cast" -> characters.add(UUID.fromString(text));
                case "reconstructed" -> reconstructed = MathUtility.parseBoolean(text);
                default -> { }
            }
        }
        if (date == null || kind == null || party == null
                  || kind == Kind.CONTRACT_STARTED || kind == Kind.CONTRACT_ENDED) {
            throw new IllegalArgumentException("Incomplete travel record");
        }
        return new TravelRecord(date, kind, party, partyId, partyName, systemId, systemName, otherSystemId,
              otherSystemName, Collections.unmodifiableList(characters), reconstructed);
    }

    // endregion XML
}
