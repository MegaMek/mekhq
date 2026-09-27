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

import static java.time.temporal.ChronoUnit.DAYS;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import mekhq.campaign.roleplay.TravelRecord.Kind;
import mekhq.campaign.roleplay.TravelRecord.Party;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Where the main force, the bases and the cast have been. It is kept apart from the journal so travel never crowds
 * out the story, and it has no size limit, since the whole history is the point.
 *
 * <p>The log works by comparing snapshots: it is shown where every party is (see {@link #observe}), and records the
 * differences from the last time. Only departures and final arrivals are recorded, not each jump on the way, and a
 * trip that starts and ends in the same system is not recorded. Cast members travelling with the main force are
 * covered by the main force's records.</p>
 */
public class TravelLog {
    /**
     * Where one party is now.
     *
     * @param id              the base's or cast member's id; {@code null} for the main force
     * @param name            the party's display name
     * @param systemId        the system it is in, or {@code null} if unknown
     * @param systemName      that system's display name
     * @param travelling      {@code true} if it is on its way somewhere
     * @param destinationId   where it is heading, if travelling and known
     * @param destinationName that system's display name
     */
    public record Observation(@Nullable UUID id, String name, @Nullable String systemId, String systemName,
          boolean travelling, @Nullable String destinationId, @Nullable String destinationName) {}

    /**
     * Where everyone is now.
     *
     * @param force    the main force, or {@code null} if it has no location
     * @param bases    every base
     * @param castAway       the linked cast members who are not with the main force
     * @param castWithForce  the ids of the linked cast members who are with the main force
     */
    public record Snapshot(@Nullable Observation force, List<Observation> bases, List<Observation> castAway,
          Set<UUID> castWithForce) {}

    /**
     * A contract, for rebuilding history and for showing contracts alongside travel.
     *
     * @param id         the contract's id
     * @param name       the contract's name
     * @param systemId   its target system, or {@code null} if unknown
     * @param systemName that system's display name
     * @param start      when it started, or {@code null} if unknown
     * @param end        when it ended, or {@code null} if it has not ended
     */
    public record ContractInfo(UUID id, String name, @Nullable String systemId, String systemName,
          @Nullable LocalDate start, @Nullable LocalDate end) {}

    /**
     * One row of the by-system view.
     *
     * @param systemId    the system's id
     * @param systemName  its display name
     * @param firstVisit  the first date anything happened there
     * @param lastVisit   the last date anything happened there
     * @param visits      how many separate stays the main force had there
     * @param daysPresent how many days the main force spent there
     * @param contracts   how many contracts were there
     */
    public record SystemSummary(String systemId, String systemName, LocalDate firstVisit, LocalDate lastVisit,
          int visits, long daysPresent, int contracts) {}

    private final List<TravelRecord> records = new ArrayList<>();
    private boolean historyFilled;
    /** The last snapshot seen, keyed by party; {@code null} until the first snapshot after loading. */
    private Map<String, Observation> last;
    private Observation lastForce;
    private Set<UUID> lastWithForce = Set.of();

    /**
     * @return every record, oldest first
     */
    public List<TravelRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    /**
     * @return {@code true} once the history has been rebuilt from the campaign's contracts
     */
    public boolean isHistoryFilled() {
        return historyFilled;
    }

    // region Recording

    /**
     * Rebuilds the main force's history from the campaign's contracts, once per campaign: an arrival in each
     * contract's system on the day it started, for every contract before the log's first record. The records are
     * marked as reconstructed, since the real travel dates can't be recovered.
     *
     * @param contracts the campaign's contracts
     */
    public void fillHistory(final List<ContractInfo> contracts) {
        if (historyFilled) {
            return;
        }
        historyFilled = true;
        final LocalDate firstRecord = records.stream()
                                            .filter(record -> record.getParty() == Party.MAIN_FORCE)
                                            .map(TravelRecord::getDate)
                                            .min(Comparator.naturalOrder())
                                            .orElse(LocalDate.MAX);
        final List<ContractInfo> earlier = contracts.stream()
                                                 .filter(contract -> contract.start() != null
                                                                           && contract.systemId() != null
                                                                           && contract.start().isBefore(firstRecord))
                                                 .sorted(Comparator.comparing(ContractInfo::start))
                                                 .toList();
        String previous = null;
        for (ContractInfo contract : earlier) {
            if (!contract.systemId().equals(previous)) {
                records.add(new TravelRecord(contract.start(), Kind.ARRIVED, Party.MAIN_FORCE, null, "",
                      contract.systemId(), contract.systemName(), null, null, List.of(), true));
                previous = contract.systemId();
            }
        }
        records.sort(Comparator.comparing(TravelRecord::getDate));
    }

    /**
     * Records what has changed since the last snapshot. The first snapshot after loading only notes where everyone
     * is, recording a party as present where the log does not already place it.
     *
     * @param date     today's date
     * @param snapshot where everyone is now
     *
     * @return the records added
     */
    public List<TravelRecord> observe(final LocalDate date, final Snapshot snapshot) {
        final int before = records.size();
        final Map<String, Observation> now = index(snapshot);
        if (last == null) {
            for (Map.Entry<String, Observation> entry : now.entrySet()) {
                final Observation observation = entry.getValue();
                final Party party = partyOf(entry.getKey());
                if (!observation.travelling() && observation.systemId() != null
                          && !observation.systemId().equals(lastPlace(party, observation.id()))) {
                    add(date, Kind.PRESENT, party, observation, observation.systemId(), observation.systemName(),
                          null, null);
                }
            }
        } else {
            final List<TravelRecord> cast = new ArrayList<>();
            for (String key : union(last, now)) {
                final Observation previous = last.get(key);
                final Observation current = now.get(key);
                switch (partyOf(key)) {
                    case MAIN_FORCE -> {
                        if (previous != null && current != null) {
                            compare(date, Party.MAIN_FORCE, previous, current, null);
                        }
                    }
                    case BASE -> compareBase(date, previous, current);
                    case CAST -> compareCast(date, previous, current, snapshot, cast);
                }
            }
            addGrouped(cast);
        }
        last = now;
        lastForce = snapshot.force();
        lastWithForce = Set.copyOf(snapshot.castWithForce());
        return List.copyOf(records.subList(before, records.size()));
    }

    private void compareBase(final LocalDate date, final @Nullable Observation previous,
          final @Nullable Observation current) {
        if (previous == null && current != null) {
            add(date, Kind.BASE_ESTABLISHED, Party.BASE, current, current.systemId(), current.systemName(), null,
                  null);
        } else if (previous != null && current == null) {
            add(date, Kind.BASE_CLOSED, Party.BASE, previous, previous.systemId(), previous.systemName(), null,
                  null);
        } else if (previous != null) {
            compare(date, Party.BASE, previous, current, null);
        }
    }

    private void compareCast(final LocalDate date, final @Nullable Observation previous,
          final @Nullable Observation current, final Snapshot snapshot, final List<TravelRecord> into) {
        final Observation force = snapshot.force();
        if (previous == null && current != null) {
            final Observation from = (lastForce != null) ? lastForce : force;
            if (from == null || !lastWithForce.contains(current.id())) {
                // Newly linked to the cast, so only note where they are.
                if (!current.travelling() && current.systemId() != null
                          && !current.systemId().equals(lastPlace(Party.CAST, current.id()))) {
                    into.add(record(date, Kind.PRESENT, Party.CAST, current, current.systemId(),
                          current.systemName(), null, null));
                }
                return;
            }
            // Left the main force.
            final Observation origin = new Observation(current.id(), current.name(), from.systemId(),
                  from.systemName(), false, null, null);
            compare(date, Party.CAST, origin, current, into);
        } else if (previous != null && current == null) {
            // Rejoined the main force, unless they simply left the cast or the company.
            if (force != null && snapshot.castWithForce().contains(previous.id())) {
                final Observation joined = new Observation(previous.id(), previous.name(), force.systemId(),
                      force.systemName(), force.travelling(), force.destinationId(), force.destinationName());
                compare(date, Party.CAST, previous, joined, into);
            }
        } else if (previous != null) {
            compare(date, Party.CAST, previous, current, into);
        }
    }

    /**
     * Records a departure or an arrival between two sightings of one party.
     *
     * @param into where cast records go to be grouped, or {@code null} to add records straight away
     */
    private void compare(final LocalDate date, final Party party, final Observation previous,
          final Observation current, final @Nullable List<TravelRecord> into) {
        if (!previous.travelling() && current.travelling()) {
            final String destination = current.destinationId();
            if (destination != null && !destination.equals(previous.systemId())) {
                emit(date, Kind.DEPARTED, party, current, previous.systemId(), previous.systemName(), destination,
                      current.destinationName(), into);
            }
        } else if (!current.travelling() && current.systemId() != null
                         && (previous.travelling() || !current.systemId().equals(previous.systemId()))) {
            final TravelRecord lastRecord = lastMovement(party, current.id());
            if (lastRecord != null && lastRecord.getKind() == Kind.DEPARTED) {
                emit(date, Kind.ARRIVED, party, current, current.systemId(), current.systemName(),
                      lastRecord.getSystemId(), lastRecord.getSystemName(), into);
            } else if (!current.systemId().equals(placeOf(lastRecord))) {
                final boolean moved = !previous.travelling() && previous.systemId() != null;
                emit(date, Kind.ARRIVED, party, current, current.systemId(), current.systemName(),
                      moved ? previous.systemId() : null, moved ? previous.systemName() : null, into);
            }
        }
    }

    private void emit(final LocalDate date, final Kind kind, final Party party, final Observation observation,
          final @Nullable String systemId, final String systemName, final @Nullable String otherId,
          final @Nullable String otherName, final @Nullable List<TravelRecord> into) {
        if (into == null) {
            add(date, kind, party, observation, systemId, systemName, otherId, otherName);
        } else {
            into.add(record(date, kind, party, observation, systemId, systemName, otherId, otherName));
        }
    }

    private void add(final LocalDate date, final Kind kind, final Party party, final Observation observation,
          final @Nullable String systemId, final String systemName, final @Nullable String otherId,
          final @Nullable String otherName) {
        records.add(record(date, kind, party, observation, systemId, systemName, otherId, otherName));
    }

    private static TravelRecord record(final LocalDate date, final Kind kind, final Party party,
          final Observation observation, final @Nullable String systemId, final String systemName,
          final @Nullable String otherId, final @Nullable String otherName) {
        final boolean cast = party == Party.CAST;
        return new TravelRecord(date, kind, party, cast ? null : observation.id(),
              party == Party.MAIN_FORCE ? "" : observation.name(), systemId, systemName, otherId, otherName,
              (cast && observation.id() != null) ? List.of(observation.id()) : List.of(), false);
    }

    /** Cast members making the same journey on the same day share one record. */
    private void addGrouped(final List<TravelRecord> cast) {
        final Map<List<Object>, List<TravelRecord>> groups = new LinkedHashMap<>();
        for (TravelRecord record : cast) {
            groups.computeIfAbsent(List.of(record.getKind(), String.valueOf(record.getSystemId()),
                  String.valueOf(record.getOtherSystemId())), key -> new ArrayList<>()).add(record);
        }
        for (List<TravelRecord> group : groups.values()) {
            final TravelRecord first = group.get(0);
            records.add(new TravelRecord(first.getDate(), first.getKind(), Party.CAST, null,
                  group.stream().map(TravelRecord::getPartyName).collect(Collectors.joining(", ")),
                  first.getSystemId(), first.getSystemName(), first.getOtherSystemId(), first.getOtherSystemName(),
                  group.stream().flatMap(record -> record.getCharacters().stream()).toList(), false));
        }
    }

    private TravelRecord lastMovement(final Party party, final @Nullable UUID id) {
        for (int i = records.size() - 1; i >= 0; i--) {
            final TravelRecord record = records.get(i);
            if (record.concerns(party, id)) {
                return record;
            }
        }
        return null;
    }

    /**
     * @return the system a party's last record leaves it in, or {@code null} if unknown
     */
    private String lastPlace(final Party party, final @Nullable UUID id) {
        return placeOf(lastMovement(party, id));
    }

    private static @Nullable String placeOf(final @Nullable TravelRecord record) {
        if (record == null || record.getKind() == Kind.DEPARTED || record.getKind() == Kind.BASE_CLOSED) {
            return null;
        }
        return record.getSystemId();
    }

    private static Map<String, Observation> index(final Snapshot snapshot) {
        final Map<String, Observation> index = new LinkedHashMap<>();
        if (snapshot.force() != null) {
            index.put("F", snapshot.force());
        }
        for (Observation base : snapshot.bases()) {
            index.put("B" + base.id(), base);
        }
        for (Observation member : snapshot.castAway()) {
            index.put("C" + member.id(), member);
        }
        return index;
    }

    private static Party partyOf(final String key) {
        return switch (key.charAt(0)) {
            case 'F' -> Party.MAIN_FORCE;
            case 'B' -> Party.BASE;
            default -> Party.CAST;
        };
    }

    private static List<String> union(final Map<String, Observation> first, final Map<String, Observation> second) {
        final List<String> keys = new ArrayList<>(first.keySet());
        for (String key : second.keySet()) {
            if (!first.containsKey(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    // endregion Recording

    // region Views

    /**
     * Builds the combined timeline: the travel records and, for each contract, its start and end.
     *
     * @param contracts the campaign's contracts
     *
     * @return every row, oldest first
     */
    public List<TravelRecord> getTimeline(final List<ContractInfo> contracts) {
        final List<TravelRecord> timeline = new ArrayList<>(records);
        for (ContractInfo contract : contracts) {
            if (contract.start() != null) {
                timeline.add(contractRow(contract, Kind.CONTRACT_STARTED, contract.start()));
            }
            if (contract.end() != null) {
                timeline.add(contractRow(contract, Kind.CONTRACT_ENDED, contract.end()));
            }
        }
        // A stable sort keeps travel before contracts on the same day, in the order they happened.
        timeline.sort(Comparator.comparing(TravelRecord::getDate));
        return timeline;
    }

    private static TravelRecord contractRow(final ContractInfo contract, final Kind kind, final LocalDate date) {
        return new TravelRecord(date, kind, Party.MAIN_FORCE, contract.id(), contract.name(), contract.systemId(),
              contract.systemName(), null, null, List.of(), false);
    }

    /**
     * Summarises every system the log mentions.
     *
     * @param contracts the campaign's contracts
     * @param today     today's date, closing the main force's current stay
     *
     * @return one row per system, in the order they were first visited
     */
    public List<SystemSummary> summarize(final List<ContractInfo> contracts, final LocalDate today) {
        final Map<String, String> names = new LinkedHashMap<>();
        final Map<String, LocalDate> first = new HashMap<>();
        final Map<String, LocalDate> latest = new HashMap<>();
        final Map<String, Integer> visits = new HashMap<>();
        final Map<String, Long> days = new HashMap<>();
        final Map<String, Integer> contractCounts = new HashMap<>();

        final List<TravelRecord> timeline = getTimeline(contracts);
        for (TravelRecord record : timeline) {
            final String id = record.getSystemId();
            if (id == null) {
                continue;
            }
            names.putIfAbsent(id, record.getSystemName());
            first.merge(id, record.getDate(), (a, b) -> a.isBefore(b) ? a : b);
            latest.merge(id, record.getDate(), (a, b) -> a.isAfter(b) ? a : b);
            if (record.getKind() == Kind.CONTRACT_STARTED) {
                contractCounts.merge(id, 1, Integer::sum);
            }
        }

        // The main force's stays: from each arrival to the next departure or arrival elsewhere.
        String stayAt = null;
        LocalDate stayFrom = null;
        for (TravelRecord record : records) {
            if (record.getParty() != Party.MAIN_FORCE) {
                continue;
            }
            final boolean arrives = record.getKind() == Kind.ARRIVED || record.getKind() == Kind.PRESENT;
            if (arrives && Objects.equals(record.getSystemId(), stayAt)) {
                continue;
            }
            if (stayAt != null) {
                days.merge(stayAt, Math.max(0, DAYS.between(stayFrom, record.getDate())), Long::sum);
                stayAt = null;
            }
            if (arrives && record.getSystemId() != null) {
                stayAt = record.getSystemId();
                stayFrom = record.getDate();
                visits.merge(stayAt, 1, Integer::sum);
            }
        }
        if (stayAt != null) {
            days.merge(stayAt, Math.max(0, DAYS.between(stayFrom, today)), Long::sum);
        }

        final List<SystemSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            final String id = entry.getKey();
            summaries.add(new SystemSummary(id, entry.getValue(), first.get(id), latest.get(id),
                  visits.getOrDefault(id, 0), days.getOrDefault(id, 0L), contractCounts.getOrDefault(id, 0)));
        }
        return summaries;
    }

    /**
     * @param systemId a system's id
     * @param contracts the campaign's contracts
     *
     * @return everything that happened in that system, oldest first, including departures from it
     */
    public List<TravelRecord> getTimelineFor(final String systemId, final List<ContractInfo> contracts) {
        return getTimeline(contracts).stream()
                     .filter(record -> systemId.equals(record.getSystemId()))
                     .toList();
    }

    /**
     * @param systemId a system's id
     * @param today    today's date
     *
     * @return the periods the main force spent in that system, each as {start, end}, with the current stay ending
     *       today
     */
    public List<LocalDate[]> getStays(final String systemId, final LocalDate today) {
        final List<LocalDate[]> stays = new ArrayList<>();
        LocalDate from = null;
        for (TravelRecord record : records) {
            if (record.getParty() != Party.MAIN_FORCE) {
                continue;
            }
            final boolean here = systemId.equals(record.getSystemId());
            final boolean arrives = record.getKind() == Kind.ARRIVED || record.getKind() == Kind.PRESENT;
            if (from == null && here && arrives) {
                from = record.getDate();
            } else if (from != null && !(here && arrives)) {
                stays.add(new LocalDate[] { from, record.getDate() });
                from = null;
            }
        }
        if (from != null) {
            stays.add(new LocalDate[] { from, today });
        }
        return stays;
    }

    // endregion Views

    // region XML

    void writeToXML(final PrintWriter writer, int indent) {
        if (records.isEmpty() && !historyFilled) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "travelLog");
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "historyFilled", historyFilled);
        TravelRecord.writeListToXML(writer, indent, records);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "travelLog");
    }

    static TravelLog parse(final Node node) {
        final TravelLog log = new TravelLog();
        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeName().equalsIgnoreCase("historyFilled")) {
                log.historyFilled = MathUtility.parseBoolean(child.getTextContent().trim());
            } else if (child.getNodeName().equalsIgnoreCase("records")) {
                log.records.addAll(TravelRecord.parseList(child));
            }
        }
        log.records.sort(Comparator.comparing(TravelRecord::getDate));
        return log;
    }

    // endregion XML
}
