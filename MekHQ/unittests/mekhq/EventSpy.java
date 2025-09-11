/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import megamek.common.annotations.Nullable;
import megamek.common.event.MMEvent;
import megamek.common.event.Subscribe;
import mekhq.campaign.events.parts.PartArrivedEvent;
import mekhq.campaign.events.parts.PartChangedEvent;
import mekhq.campaign.events.parts.PartNewEvent;
import mekhq.campaign.events.parts.PartRemovedEvent;

/**
 * Provides a list of events captured during its lifetime. Use this as part of a try-with-resources block.
 * <p>
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
    @Override
    public void close() {
        MekHQ.unregisterHandler(this);
    }

    /**
     * Gets the list of events which occurred.
     *
     * @return The list of events which occurred, in the order received.
     */
    public List<MMEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Finds an event of a certain type matching a given predicate.
     *
     * @param clazz     The event class in question.
     * @param predicate The predicate to apply when searching for an event.
     *
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
     *
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
