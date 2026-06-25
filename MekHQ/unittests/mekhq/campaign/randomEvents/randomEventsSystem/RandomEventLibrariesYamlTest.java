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
 * MechWarrior, BattleMech, `Mech and AeroTek are registered trademarks
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
package mekhq.campaign.randomEvents.randomEventsSystem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Loads the shipped prisoner event YAML data files ({@code data/randomEvents/PrisonerMajorEventData.yml} and
 * {@code data/randomEvents/PrisonerMinorEventData.yml}) through the real production loader
 * {@link RandomEventLibraries}.
 *
 * <p>These tests guard the data-file -> record deserialization contract. {@link RandomEventLibraries} reads the YAML
 * with Jackson straight into the {@link RandomEventData} / {@link RandomEventResponseEntry} / {@link RandomEventResult}
 * records. If the record component names (or their {@code @JsonProperty} keys) drift away from the keys used in the
 * shipped YAML, the events either fail to deserialize or load with null/empty values - which silently disables the
 * prisoner event feature at runtime.</p>
 */
class RandomEventLibrariesYamlTest {

    @Test
    void majorPrisonerEventsLoadFromYaml() {
        RandomEventLibraries libraries = new RandomEventLibraries(true);

        assertFalse(libraries.getPrisonerEvents(true).isEmpty(),
              "Major prisoner events must load from PrisonerMajorEventData.yml.");
    }

    @Test
    void minorPrisonerEventsLoadFromYaml() {
        RandomEventLibraries libraries = new RandomEventLibraries(true);

        assertFalse(libraries.getPrisonerEvents(false).isEmpty(),
              "Minor prisoner events must load from PrisonerMinorEventData.yml.");
    }

    @Test
    void everyLoadedEventHasANonNullType() {
        RandomEventLibraries libraries = new RandomEventLibraries(true);

        for (RandomEventData event : allEvents(libraries)) {
            assertNotNull(event.randomEventType(),
                  "Every loaded event must map to a RandomEventType; a null type means the YAML 'randomEventType' "
                        + "key did not bind to the record component.");
        }
    }

    @Test
    void everyLoadedEventHasResponseEntries() {
        RandomEventLibraries libraries = new RandomEventLibraries(true);

        for (RandomEventData event : allEvents(libraries)) {
            assertFalse(event.responseEntries().isEmpty(),
                  "Every loaded event must carry at least one response entry.");
        }
    }

    private static List<RandomEventData> allEvents(RandomEventLibraries libraries) {
        List<RandomEventData> events = new java.util.ArrayList<>();
        events.addAll(libraries.getPrisonerEvents(true));
        events.addAll(libraries.getPrisonerEvents(false));
        return events;
    }
}
