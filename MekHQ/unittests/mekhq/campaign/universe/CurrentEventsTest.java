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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.campaign.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.SortedMap;
import java.util.TreeMap;

import mekhq.campaign.universe.PlanetarySystem.PlanetarySystemEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link CurrentEvents}, the cache shared by {@link Planet} and {@link PlanetarySystem} for resolving their
 * event timelines at a given date.
 */
public class CurrentEventsTest {
    private static final LocalDate EARLY_DATE = LocalDate.of(3025, 1, 1);
    private static final LocalDate LATE_DATE = LocalDate.of(3050, 1, 1);

    private static PlanetarySystemEvent createChargeEvent(LocalDate date, Boolean nadirCharge, Boolean zenithCharge) {
        PlanetarySystemEvent event = new PlanetarySystemEvent();
        event.date = date;
        event.nadirCharge = (nadirCharge == null) ? null : SourceableValue.of(nadirCharge);
        event.zenithCharge = (zenithCharge == null) ? null : SourceableValue.of(zenithCharge);
        return event;
    }

    private static CurrentEvents<PlanetarySystemEvent> createCurrentEvents(
          SortedMap<LocalDate, PlanetarySystemEvent> events) {
        return new CurrentEvents<>(() -> events, PlanetarySystemEvent::new, PlanetarySystemEvent::copyDataFrom);
    }

    @Test
    @DisplayName("A timeline with no events returns the default value")
    void noEventsReturnsDefault() {
        CurrentEvents<PlanetarySystemEvent> currentEvents = createCurrentEvents(null);

        assertEquals("default", currentEvents.getEventData(EARLY_DATE, "default", e -> null));
        assertNull(currentEvents.getEventData(EARLY_DATE, null, e -> e.nadirCharge));
    }

    @Test
    @DisplayName("Values set by earlier events carry forward until a later event overrides them")
    void valuesCarryForwardUntilOverridden() {
        TreeMap<LocalDate, PlanetarySystemEvent> events = new TreeMap<>();
        events.put(EARLY_DATE, createChargeEvent(EARLY_DATE, true, true));
        events.put(LATE_DATE, createChargeEvent(LATE_DATE, false, null));
        CurrentEvents<PlanetarySystemEvent> currentEvents = createCurrentEvents(events);

        assertNull(currentEvents.getEventData(EARLY_DATE.minusDays(1), null, e -> e.nadirCharge));
        assertTrue(currentEvents.getEventData(EARLY_DATE, null, e -> e.nadirCharge).getValue());
        assertFalse(currentEvents.getEventData(LATE_DATE, null, e -> e.nadirCharge).getValue());
        assertTrue(currentEvents.getEventData(LATE_DATE, null, e -> e.zenithCharge).getValue(),
              "A value the later event leaves unset must carry forward from the earlier event");
    }

    @Test
    @DisplayName("Querying an earlier date after a later one does not return the later state")
    void outOfOrderQueriesResolveCorrectly() {
        TreeMap<LocalDate, PlanetarySystemEvent> events = new TreeMap<>();
        events.put(EARLY_DATE, createChargeEvent(EARLY_DATE, true, null));
        events.put(LATE_DATE, createChargeEvent(LATE_DATE, false, null));
        CurrentEvents<PlanetarySystemEvent> currentEvents = createCurrentEvents(events);

        assertTrue(currentEvents.getEventData(EARLY_DATE, null, e -> e.nadirCharge).getValue());
        assertFalse(currentEvents.getEventData(LATE_DATE, null, e -> e.nadirCharge).getValue());
        assertTrue(currentEvents.getEventData(EARLY_DATE, null, e -> e.nadirCharge).getValue());
    }

    @Test
    @DisplayName("Invalidating the cache picks up changes to the timeline")
    void invalidatePicksUpTimelineChanges() {
        TreeMap<LocalDate, PlanetarySystemEvent> events = new TreeMap<>();
        events.put(EARLY_DATE, createChargeEvent(EARLY_DATE, true, null));
        CurrentEvents<PlanetarySystemEvent> currentEvents = createCurrentEvents(events);

        assertTrue(currentEvents.getEventData(LATE_DATE, null, e -> e.nadirCharge).getValue());

        events.put(LATE_DATE, createChargeEvent(LATE_DATE, false, null));
        currentEvents.invalidate();

        assertFalse(currentEvents.getEventData(LATE_DATE, null, e -> e.nadirCharge).getValue());
    }

    @Test
    @DisplayName("A planetary system's charge stations reflect events added after an earlier lookup")
    void planetarySystemReflectsEventEdits() {
        PlanetarySystem system = new PlanetarySystem("Test System");
        system.putEvent(createChargeEvent(EARLY_DATE, true, false));

        assertTrue(system.isNadirCharge(LATE_DATE));
        assertFalse(system.isZenithCharge(LATE_DATE));

        system.putEvent(createChargeEvent(LATE_DATE, false, true));

        assertFalse(system.isNadirCharge(LATE_DATE));
        assertTrue(system.isZenithCharge(LATE_DATE));
        assertTrue(system.isNadirCharge(EARLY_DATE));
        assertFalse(system.isZenithCharge(EARLY_DATE));
    }
}
