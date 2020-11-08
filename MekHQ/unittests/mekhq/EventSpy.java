/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import megamek.common.annotations.Nullable;
import megamek.common.event.MMEvent;
import megamek.common.event.Subscribe;
import mekhq.campaign.event.*;

/**
 * Provides a list of events captured during its lifetime.
 * Use this as part of a try-with-resources block.
 *
 * If you need to listen to a new event, add a handler to this class.
 */
public class EventSpy implements AutoCloseable {
    private final List<MMEvent> events = new ArrayList<>();

    /**
     * Creates a new EventSpy and registers it with MekHQ's event bus.
     */
    public EventSpy() {
        MekHQ.registerHandler(this);
    }

    /**
     * Deregisters this instance from MekHQ's event bus.
     */
    public void close() {
        MekHQ.unregisterHandler(this);
    }

    /**
     * Gets the list of events which occurred.
     * @return The list of events which occurred, in the order received.
     */
    public List<MMEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Finds an event of a certain type matching a given predicate.
     * @param clazz The event class in question.
     * @param predicate The predicate to apply when searching for an event.
     * @return The first matching event, otherwise null.
     */
    public @Nullable <TEvent extends MMEvent> TEvent findEvent(Class<TEvent> clazz, Predicate<TEvent> predicate) {
        for (MMEvent e : events) {
            if (clazz.isInstance(e)) {
                TEvent instance = clazz.cast(e);
                if (predicate.test(instance)) {
                    return instance;
                }
            }
        }

        return null;
    }

    /**
     * Records an event.
     * @param e The event received.
     */
    private void record(MMEvent e) {
        events.add(e);
    }

    //
    // CAW: Add new event handlers below as needed for a unit test.
    //

    @Subscribe
    public void handle(PartNewEvent e) {
        record(e);
    }

    @Subscribe
    public void handle(PartChangedEvent e) {
        record(e);
    }

    @Subscribe
    public void handle(PartRemovedEvent e) {
        record(e);
    }

    @Subscribe
    public void handle(PartArrivedEvent e) {
        record(e);
    }
}
