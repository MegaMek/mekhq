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
package mekhq.campaign.universe;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.codeUtilities.ObjectUtility;

/**
 * Tracks the accumulated state of a dated event timeline (such as the events of a {@link Planet} or a
 * {@link PlanetarySystem}) as of the most recently queried date.
 *
 * <p>Events are merged in chronological order into a single accumulator, so moving forward in time only has to merge
 * the events between the previous query and the new one. Moving backward in time re-initializes the accumulator and
 * replays the timeline from the start.</p>
 *
 * <p>The owner must call {@link #invalidate()} whenever its event timeline changes, as the cache holds an iterator
 * over it.</p>
 *
 * @param <E> the event type
 */
public class CurrentEvents<E> {
    private final Supplier<SortedMap<LocalDate, E>> eventsSupplier;
    private final Supplier<E> emptyEventFactory;
    private final BiConsumer<E, E> merger;

    private LocalDate lastUpdated;
    private E currentEvent;
    private Map.Entry<LocalDate, E> nextEvent;
    private Iterator<Map.Entry<LocalDate, E>> eventStream;

    /**
     * @param eventsSupplier    supplies the owner's current event timeline, sorted by date; may supply {@code null}
     *                          when there are no events
     * @param emptyEventFactory creates a new event carrying no data, used as the accumulator
     * @param merger            merges the data of the second event into the first, overwriting only the values the
     *                          second event actually sets
     */
    public CurrentEvents(Supplier<SortedMap<LocalDate, E>> eventsSupplier, Supplier<E> emptyEventFactory,
          BiConsumer<E, E> merger) {
        this.eventsSupplier = eventsSupplier;
        this.emptyEventFactory = emptyEventFactory;
        this.merger = merger;
    }

    /**
     * Discards the cached state. Must be called whenever the underlying event timeline changes.
     */
    public synchronized void invalidate() {
        lastUpdated = null;
        currentEvent = null;
        nextEvent = null;
        eventStream = null;
    }

    private void initialize(LocalDate now) {
        currentEvent = emptyEventFactory.get();
        lastUpdated = now;
        nextEvent = null;
        eventStream = null;
        SortedMap<LocalDate, E> events = eventsSupplier.get();
        if (events != null) {
            eventStream = events.entrySet().iterator();
            if (eventStream.hasNext()) {
                nextEvent = eventStream.next();
            }
        }
    }

    /**
     * Gets the current event for the time.
     *
     * <p>{@code currentEvent} is a single shared, mutable accumulator: fast-forwarding merges every event up to
     * {@code now} into it, and nothing ever unmerges them. {@code lastUpdated} must therefore track the date this
     * accumulator actually reflects (the most recent {@code now} resolved, not just the date passed to the original
     * {@link #initialize}), or a later out-of-order query (e.g., a caller checking a future date) would fast-forward
     * the accumulator past a subsequent, earlier-dated query without tripping the "went back in time" reset below,
     * silently returning already-future state for a query that asked about the present or past.</p>
     *
     * @param now The current time.
     *
     * @return The up-to-date event as of {@code now}.
     */
    public synchronized @Nonnull E getCurrentEvent(LocalDate now) {
        if ((lastUpdated == null) || lastUpdated.isAfter(now)) {
            // initialize ourselves if we're fresh or if we went back in time (which breaks how the event stream
            // works)
            initialize(now);
        }
        lastUpdated = now;

        // if we have no more events, or if our current date is before the next date, return our cached event
        if ((nextEvent == null) || now.isBefore(nextEvent.getKey())) {
            return currentEvent;
        }

        // fast-forward to the next event
        do {
            merger.accept(currentEvent, nextEvent.getValue());
            if (eventStream.hasNext()) {
                nextEvent = eventStream.next();
            } else {
                nextEvent = null;
            }

        } while ((nextEvent != null) && !now.isBefore(nextEvent.getKey()));

        return currentEvent;
    }

    /**
     * Looks up a single value from the current event.
     *
     * @param when         the date to look the value up for
     * @param defaultValue the value to return if no event up to {@code when} sets one
     * @param getter       extracts the value from an event
     *
     * @return the most recent value set on or before {@code when}, or {@code defaultValue} if there is none
     */
    public <T> T getEventData(@Nullable LocalDate when, T defaultValue, @Nullable Function<E, T> getter) {
        if ((null == when) || (null == getter)) {
            return defaultValue;
        }

        T result = getter.apply(getCurrentEvent(when));

        return ObjectUtility.nonNull(result, defaultValue);
    }
}
