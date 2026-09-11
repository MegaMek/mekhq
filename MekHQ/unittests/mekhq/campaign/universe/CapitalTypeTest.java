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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import mekhq.campaign.universe.enums.CapitalType;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@link CapitalType} enum and the parsing/aggregation of the {@code capitalType} planetary event field
 * that was activated across the canon planetary system data.
 */
class CapitalTypeTest {

    // A single-planet system whose capital status is authored as a plain scalar (the exact form used in the data
    // files), and which changes over time: national from 3025, downgraded to district from 3075.
    private static final String SINGLE_PLANET_SYSTEM = """
          id: Capital Test
          sucsId: 1
          xcood: 0.0
          ycood: 0.0
          primarySlot: 1
          planet:
            - name: Capital Test Prime
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
                    - FS
                - date: '3025-01-01'
                  capitalType: National Capital
                - date: '3075-01-01'
                  capitalType: District Capital
          """;

    // A two-planet system: the primary planet is a district capital, a secondary planet is a national capital. The
    // system aggregate should report the more significant of the two.
    private static final String TWO_PLANET_SYSTEM = """
          id: Aggregate Test
          sucsId: 2
          xcood: 0.0
          ycood: 0.0
          primarySlot: 1
          planet:
            - name: Aggregate Prime
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
                  capitalType: District Capital
            - name: Aggregate Second
              type: TERRESTRIAL
              orbitalDist: 2.0
              sysPos: 2
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
                  capitalType: National Capital
          """;

    @Test
    void fromLabelMapsAuthoredLabels() {
        assertEquals(CapitalType.NATIONAL, CapitalType.fromLabel("National Capital"));
        assertEquals(CapitalType.REGION, CapitalType.fromLabel("Region Capital"));
        assertEquals(CapitalType.DISTRICT, CapitalType.fromLabel("District Capital"));
    }

    @Test
    void fromLabelIsLenient() {
        assertEquals(CapitalType.REGION, CapitalType.fromLabel("  region capital  "));
        assertEquals(CapitalType.NATIONAL, CapitalType.fromLabel("NATIONAL"));
        assertEquals(CapitalType.NONE, CapitalType.fromLabel(null));
        assertEquals(CapitalType.NONE, CapitalType.fromLabel(""));
        assertEquals(CapitalType.NONE, CapitalType.fromLabel("Provincial Capital"));
    }

    @Test
    void significanceRanksNationalHighest() {
        assertTrue(CapitalType.NATIONAL.significance() > CapitalType.REGION.significance());
        assertTrue(CapitalType.REGION.significance() > CapitalType.DISTRICT.significance());
        assertTrue(CapitalType.DISTRICT.significance() > CapitalType.NONE.significance());
    }

    @Test
    void plainScalarCapitalTypeParsesAndIsDateScoped() throws Exception {
        PlanetarySystem system = PlanetarySystemYamlIO.read(SINGLE_PLANET_SYSTEM);
        Planet planet = system.getPrimaryPlanet();

        assertEquals(CapitalType.NONE, planet.getCapitalType(LocalDate.of(3010, 1, 1)));
        assertEquals(CapitalType.NATIONAL, planet.getCapitalType(LocalDate.of(3030, 1, 1)));
        assertEquals(CapitalType.DISTRICT, planet.getCapitalType(LocalDate.of(3080, 1, 1)));
    }

    @Test
    void capitalTypeSurvivesWriteReadRoundTrip() throws Exception {
        PlanetarySystem system = PlanetarySystemYamlIO.read(SINGLE_PLANET_SYSTEM);

        PlanetarySystem reloaded = PlanetarySystemYamlIO.read(PlanetarySystemYamlIO.writeToString(system));
        Planet reloadedPlanet = reloaded.getPrimaryPlanet();

        assertEquals(CapitalType.NATIONAL, reloadedPlanet.getCapitalType(LocalDate.of(3030, 1, 1)));
        assertEquals(CapitalType.DISTRICT, reloadedPlanet.getCapitalType(LocalDate.of(3080, 1, 1)));
    }

    @Test
    void systemReportsMostSignificantCapitalAmongPlanets() throws Exception {
        PlanetarySystem system = PlanetarySystemYamlIO.read(TWO_PLANET_SYSTEM);

        assertEquals(CapitalType.NATIONAL, system.getCapitalType(LocalDate.of(3000, 1, 1)));
    }
}
