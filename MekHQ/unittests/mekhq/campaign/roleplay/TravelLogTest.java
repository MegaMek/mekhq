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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.campaign.roleplay.TravelLog.ContractInfo;
import mekhq.campaign.roleplay.TravelLog.Observation;
import mekhq.campaign.roleplay.TravelLog.Snapshot;
import mekhq.campaign.roleplay.TravelLog.SystemSummary;
import mekhq.campaign.roleplay.TravelRecord.Kind;
import mekhq.campaign.roleplay.TravelRecord.Party;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

class TravelLogTest {
    private static final LocalDate DAY = LocalDate.of(3025, 1, 1);
    private static final UUID BASE = UUID.randomUUID();
    private static final UUID KAI = UUID.randomUUID();
    private static final UUID ANA = UUID.randomUUID();

    private static Observation at(final UUID id, final String name, final String system) {
        return new Observation(id, name, system, system, false, null, null);
    }

    private static Observation going(final UUID id, final String name, final String from, final String to) {
        return new Observation(id, name, from, from, true, to, to);
    }

    private static Snapshot force(final Observation force) {
        return new Snapshot(force, List.of(), List.of(), Set.of());
    }

    private static LocalDate day(final int n) {
        return DAY.plusDays(n);
    }

    @Test
    void firstLookNotesWhereEveryoneIs() {
        TravelLog log = new TravelLog();
        List<TravelRecord> added = log.observe(DAY, new Snapshot(at(null, "", "Galax"),
              List.of(at(BASE, "Home", "Tharkad")), List.of(), Set.of()));

        assertEquals(2, added.size());
        assertEquals(Kind.PRESENT, added.get(0).getKind());
        assertEquals(Party.MAIN_FORCE, added.get(0).getParty());
        assertEquals("Galax", added.get(0).getSystemId());
        assertEquals(Party.BASE, added.get(1).getParty());
        assertEquals("Home", added.get(1).getPartyName());

        // Loading again finds everyone where the log already places them.
        TravelLog reloaded = roundTrip(log);
        assertTrue(reloaded.observe(DAY, new Snapshot(at(null, "", "Galax"), List.of(at(BASE, "Home", "Tharkad")),
              List.of(), Set.of())).isEmpty());
    }

    @Test
    void onlyDepartureAndFinalArrivalAreLogged() {
        TravelLog log = new TravelLog();
        log.observe(day(0), force(at(null, "", "Galax")));
        List<TravelRecord> departed = log.observe(day(1), force(going(null, "", "Galax", "Tharkad")));
        // Jumping through a system on the way records nothing.
        assertTrue(log.observe(day(5), force(going(null, "", "Hesperus", "Tharkad"))).isEmpty());
        List<TravelRecord> arrived = log.observe(day(9), force(at(null, "", "Tharkad")));

        assertEquals(1, departed.size());
        assertEquals(Kind.DEPARTED, departed.get(0).getKind());
        assertEquals("Galax", departed.get(0).getSystemId());
        assertEquals("Tharkad", departed.get(0).getOtherSystemId());
        assertEquals(1, arrived.size());
        assertEquals(Kind.ARRIVED, arrived.get(0).getKind());
        assertEquals("Tharkad", arrived.get(0).getSystemId());
        assertEquals("Galax", arrived.get(0).getOtherSystemId());
        // Seeing the same place again adds nothing.
        assertTrue(log.observe(day(10), force(at(null, "", "Tharkad"))).isEmpty());
    }

    @Test
    void tripsWithinOneSystemAreNotLogged() {
        TravelLog log = new TravelLog();
        log.observe(day(0), force(at(null, "", "Galax")));
        assertTrue(log.observe(day(1), force(going(null, "", "Galax", "Galax"))).isEmpty());
        assertTrue(log.observe(day(2), force(at(null, "", "Galax"))).isEmpty());
    }

    @Test
    void anArrivalSeenWithoutItsDepartureIsStillLogged() {
        TravelLog log = new TravelLog();
        log.observe(day(0), force(at(null, "", "Galax")));
        // Loaded mid-journey: the departure happened before this session.
        TravelLog reloaded = roundTrip(log);
        reloaded.observe(day(3), force(going(null, "", "Hesperus", "Tharkad")));
        List<TravelRecord> arrived = reloaded.observe(day(6), force(at(null, "", "Tharkad")));

        assertEquals(1, arrived.size());
        assertEquals(Kind.ARRIVED, arrived.get(0).getKind());
        assertNull(arrived.get(0).getOtherSystemId());
    }

    @Test
    void basesAreLoggedWhenSetUpAndClosed() {
        TravelLog log = new TravelLog();
        log.observe(day(0), force(at(null, "", "Galax")));
        List<TravelRecord> opened = log.observe(day(1), new Snapshot(at(null, "", "Galax"),
              List.of(at(BASE, "Outpost", "Galax")), List.of(), Set.of()));
        List<TravelRecord> closed = log.observe(day(4), force(at(null, "", "Galax")));

        assertEquals(Kind.BASE_ESTABLISHED, opened.get(0).getKind());
        assertEquals(BASE, opened.get(0).getPartyId());
        assertEquals(Kind.BASE_CLOSED, closed.get(0).getKind());
        assertEquals("Galax", closed.get(0).getSystemId());
    }

    @Test
    void castTravellingTogetherShareOneRecordAndRejoinTheForce() {
        TravelLog log = new TravelLog();
        Observation force = at(null, "", "Galax");
        log.observe(day(0), new Snapshot(force, List.of(), List.of(), Set.of(KAI, ANA)));

        // Both leave the main force for a base elsewhere.
        List<TravelRecord> departed = log.observe(day(1), new Snapshot(force, List.of(),
              List.of(going(KAI, "Kai", "Galax", "Tharkad"), going(ANA, "Ana", "Galax", "Tharkad")), Set.of()));
        assertEquals(1, departed.size());
        assertEquals(Kind.DEPARTED, departed.get(0).getKind());
        assertEquals(List.of(KAI, ANA), departed.get(0).getCharacters());
        assertEquals("Galax", departed.get(0).getSystemId());

        List<TravelRecord> arrived = log.observe(day(7), new Snapshot(force, List.of(),
              List.of(at(KAI, "Kai", "Tharkad"), at(ANA, "Ana", "Tharkad")), Set.of()));
        assertEquals(1, arrived.size());
        assertEquals("Galax", arrived.get(0).getOtherSystemId());

        // Kai heads back and rejoins the main force; Ana stays.
        log.observe(day(8), new Snapshot(force, List.of(),
              List.of(going(KAI, "Kai", "Tharkad", "Galax"), at(ANA, "Ana", "Tharkad")), Set.of()));
        List<TravelRecord> rejoined = log.observe(day(14), new Snapshot(force, List.of(),
              List.of(at(ANA, "Ana", "Tharkad")), Set.of(KAI)));
        assertEquals(1, rejoined.size());
        assertEquals(Kind.ARRIVED, rejoined.get(0).getKind());
        assertEquals(List.of(KAI), rejoined.get(0).getCharacters());
        assertEquals("Galax", rejoined.get(0).getSystemId());
    }

    @Test
    void castWhoLeaveTheCompanyAreNotLoggedAsRejoining() {
        TravelLog log = new TravelLog();
        Observation force = at(null, "", "Galax");
        log.observe(day(0), new Snapshot(force, List.of(), List.of(going(KAI, "Kai", "Galax", "Tharkad")),
              Set.of()));
        assertTrue(log.observe(day(1), new Snapshot(force, List.of(), List.of(), Set.of())).isEmpty());
    }

    @Test
    void historyIsRebuiltFromContractsOnce() {
        TravelLog log = new TravelLog();
        List<ContractInfo> contracts = List.of(
              new ContractInfo(UUID.randomUUID(), "Raid", "Galax", "Galax", day(0), day(30)),
              new ContractInfo(UUID.randomUUID(), "Garrison", "Galax", "Galax", day(31), day(90)),
              new ContractInfo(UUID.randomUUID(), "Defence", "Tharkad", "Tharkad", day(100), null));
        log.fillHistory(contracts);
        log.fillHistory(contracts);

        // Back-to-back contracts in one system are one stay.
        assertEquals(2, log.getRecords().size());
        assertTrue(log.getRecords().stream().allMatch(TravelRecord::isReconstructed));
        assertEquals("Tharkad", log.getRecords().get(1).getSystemId());
        assertTrue(log.isHistoryFilled());
        assertTrue(roundTrip(log).isHistoryFilled());

        // The first live look agrees with the rebuilt history, so adds nothing.
        assertTrue(log.observe(day(120), force(at(null, "", "Tharkad"))).isEmpty());
    }

    @Test
    void summaryCountsVisitsDaysAndContracts() {
        TravelLog log = new TravelLog();
        log.observe(day(0), force(at(null, "", "Galax")));
        log.observe(day(10), force(going(null, "", "Galax", "Tharkad")));
        log.observe(day(20), force(at(null, "", "Tharkad")));
        log.observe(day(30), force(going(null, "", "Tharkad", "Galax")));
        log.observe(day(40), force(at(null, "", "Galax")));
        List<ContractInfo> contracts = List.of(
              new ContractInfo(UUID.randomUUID(), "Raid", "Tharkad", "Tharkad", day(21), day(29)));

        List<SystemSummary> summary = log.summarize(contracts, day(45));
        SystemSummary galax = summary.stream().filter(s -> s.systemId().equals("Galax")).findFirst().orElseThrow();
        SystemSummary tharkad = summary.stream().filter(s -> s.systemId().equals("Tharkad")).findFirst()
                                      .orElseThrow();

        assertEquals(2, galax.visits());
        assertEquals(10 + 5, galax.daysPresent());
        assertEquals(0, galax.contracts());
        assertEquals(1, tharkad.visits());
        assertEquals(10, tharkad.daysPresent());
        assertEquals(1, tharkad.contracts());
        assertEquals(day(20), tharkad.firstVisit());
        assertEquals(2, log.getStays("Galax", day(45)).size());

        List<TravelRecord> atTharkad = log.getTimelineFor("Tharkad", contracts);
        assertTrue(atTharkad.stream().anyMatch(record -> record.getKind() == Kind.CONTRACT_STARTED));
        assertFalse(atTharkad.stream().anyMatch(record -> "Galax".equals(record.getSystemId())));
    }

    @Test
    void recordsSurviveSaveAndLoad() {
        TravelLog log = new TravelLog();
        log.observe(day(0), new Snapshot(at(null, "", "Galax"), List.of(), List.of(), Set.of(KAI)));
        log.observe(day(1), new Snapshot(at(null, "", "Galax"), List.of(),
              List.of(going(KAI, "Kai <3 & co", "Galax", "Tharkad")), Set.of()));

        TravelLog loaded = roundTrip(log);
        assertEquals(log.getRecords().size(), loaded.getRecords().size());
        TravelRecord record = loaded.getRecords().get(1);
        assertEquals(Kind.DEPARTED, record.getKind());
        assertEquals("Kai <3 & co", record.getPartyName());
        assertEquals(List.of(KAI), record.getCharacters());
        assertEquals("Tharkad", record.getOtherSystemName());
    }

    private static TravelLog roundTrip(final TravelLog log) {
        try {
            StringWriter text = new StringWriter();
            try (PrintWriter writer = new PrintWriter(text)) {
                log.writeToXML(writer, 0);
            }
            Node node = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                              .parse(new ByteArrayInputStream(text.toString().getBytes(StandardCharsets.UTF_8)))
                              .getDocumentElement();
            return TravelLog.parse(node);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
