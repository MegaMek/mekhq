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
package mekhq.utilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planet.PlanetaryEvent;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.PlanetarySystemYamlIO;
import mekhq.campaign.universe.SourceableValue;
import org.junit.jupiter.api.Test;

class PlanetarySystemChangeSummaryTest {
    private static final LocalDate EVENT_DATE = LocalDate.of(3000, 1, 1);
    private static final LocalDate EDIT_DATE = LocalDate.of(3050, 1, 1);

    private static final String SYSTEM = """
          id: Summary Test
          xcood: 1.25
          ycood: -2.5
          spectralType: G2V
          primarySlot: 1
          planet:
            - name: Summary Test Prime
              type: TERRESTRIAL
              orbitalDist: 1.0
              sysPos: 1
              pressure: STANDARD
              atmosphere: BREATHABLE
              gravity: 1.0
              diameter: 12000
              density: 5.5
              dayLength: 24
              yearLength: 1.0
              temperature: 20
              water: 70
              event:
                - date: '3000-01-01'
                  faction:
                    value:
                      - FS
                  socioIndustrial: C-C-C-C-C
          """;

    @Test
    void summarizeAddedEvent() throws Exception {
        PlanetarySystem baseline = readSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);

        PlanetaryEvent event = new PlanetaryEvent();
        event.date = EDIT_DATE;
        event.population = SourceableValue.of(1234567L);
        edited.getPrimaryPlanet().putEvent(event);

        List<String> changes = PlanetarySystemChangeSummary.summarize(baseline, edited, EVENT_DATE);

        assertTrue(changes.contains("Summary Test Prime: Added event on 3050-01-01"));
    }

    @Test
    void summarizeRemovedEvent() throws Exception {
        PlanetarySystem baseline = readSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);

        edited.getPrimaryPlanet().removeEvent(EVENT_DATE);

        List<String> changes = PlanetarySystemChangeSummary.summarize(baseline, edited, EVENT_DATE);

        assertTrue(changes.contains("Summary Test Prime: Removed event on 3000-01-01"));
    }

    @Test
    void summarizeChangedEventFields() throws Exception {
        PlanetarySystem baseline = readSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);
        Planet editedPlanet = edited.getPrimaryPlanet();
        PlanetaryEvent event = editedPlanet.getEvent(EVENT_DATE);
        event.faction = SourceableValue.of(List.of("LC"));
        event.population = SourceableValue.of(1234567L);

        List<String> changes = PlanetarySystemChangeSummary.summarize(baseline, edited, EVENT_DATE);

        assertTrue(changes.contains("Summary Test Prime: 3000-01-01 factions changed from FS to LC"));
        assertTrue(changes.contains("Summary Test Prime: 3000-01-01 population changed from none to 1,234,567"));
    }

    @Test
    void detectsChangedPlanet() throws Exception {
        PlanetarySystem baseline = readSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);
        Planet editedPlanet = edited.getPrimaryPlanet();
        PlanetaryEvent event = editedPlanet.getEvent(EVENT_DATE);
        event.population = SourceableValue.of(1234567L);

        assertTrue(PlanetarySystemChangeSummary.hasChangesForPlanet(baseline, edited, editedPlanet, EVENT_DATE));
    }

    @Test
    void ignoresUnchangedPlanet() throws Exception {
        PlanetarySystem baseline = readSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);

        assertFalse(PlanetarySystemChangeSummary.hasChangesForPlanet(baseline, edited, edited.getPrimaryPlanet(),
              EVENT_DATE));
    }

    private static PlanetarySystem readSystem() throws Exception {
        return PlanetarySystemYamlIO.read(new ByteArrayInputStream(SYSTEM.getBytes(StandardCharsets.UTF_8)));
    }
}