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
package mekhq.campaign.universe.garrison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.PlanetarySystemYamlIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link GarrisonModifierResolver} reads capital status and industrialization from planetary system data,
 * detects Clan control through the faction registry, and combines all of it with the era modifier. The faction registry
 * is mocked so the Clan check runs without loading real faction data.
 */
class GarrisonModifierResolverTest {

    // All test dates fall in the Third/Fourth Succession Wars band (era modifier 0) unless stated, so the assertions
    // isolate the world modifiers.
    private static final LocalDate WHEN = LocalDate.of(3025, 1, 1);

    @BeforeEach
    void mockFactionRegistry() {
        Faction fedSuns = mock(Faction.class);
        when(fedSuns.getShortName()).thenReturn("FS");
        when(fedSuns.isClan()).thenReturn(false);

        Faction clanWolf = mock(Faction.class);
        when(clanWolf.getShortName()).thenReturn("CW");
        when(clanWolf.isClan()).thenReturn(true);

        Factions mockedFactions = mock(Factions.class);
        when(mockedFactions.getFaction(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            if ("FS".equals(code)) {
                return fedSuns;
            }
            if ("CW".equals(code)) {
                return clanWolf;
            }
            return null;
        });
        Factions.setInstance(mockedFactions);
    }

    @AfterEach
    void resetFactionRegistry() {
        Factions.setInstance(null);
    }

    private static String system(String industry, String capitalType, String faction) {
        return """
              id: Modifier Test
              sucsId: 1
              xcood: 0.0
              ycood: 0.0
              primarySlot: 1
              planet:
                - name: Modifier Prime
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
                        - %FACTION%
                      socioIndustrial: C-%INDUSTRY%-C-C-C
                      capitalType: %CAPITAL%
              """
                     .replace("%FACTION%", faction)
                     .replace("%INDUSTRY%", industry)
                     .replace("%CAPITAL%", capitalType);
    }

    private static int modifier(String industry, String capitalType, String faction) throws Exception {
        PlanetarySystem planetarySystem = PlanetarySystemYamlIO.read(system(industry, capitalType, faction));
        return GarrisonModifierResolver.resolveModifier(planetarySystem, WHEN);
    }

    @Test
    void plainWorldContributesNothingBeyondEra() throws Exception {
        // Industry D (0), no capital (0), Federated Suns owner (not Clan, 0), era 0 -> total 0.
        assertEquals(0, modifier("D", "NONE", "FS"));
    }

    @Test
    void industrializationScales() throws Exception {
        assertEquals(4, modifier("A", "NONE", "FS")); // hyper industrial
        assertEquals(2, modifier("B", "NONE", "FS")); // major industrial
        assertEquals(1, modifier("C", "NONE", "FS")); // minor industrial
        assertEquals(0, modifier("F", "NONE", "FS")); // none
    }

    @Test
    void capitalStatusScales() throws Exception {
        assertEquals(4, modifier("D", "National Capital", "FS"));
        assertEquals(2, modifier("D", "Region Capital", "FS"));
        assertEquals(2, modifier("D", "District Capital", "FS"));
    }

    @Test
    void clanControlSubtractsOne() throws Exception {
        assertEquals(-1, modifier("D", "NONE", "CW")); // Clan Wolf controls the system
    }

    @Test
    void modifiersAndEraStack() throws Exception {
        // National capital (+4), hyper industrial (+4), Clan Wolf owner (-1); Third/Fourth SW era (0) -> +7.
        assertEquals(7, modifier("A", "National Capital", "CW"));
    }

    @Test
    void eraModifierIsIncluded() throws Exception {
        // A plain Star League-era world: no world modifiers, but the Star League era modifier is -2.
        PlanetarySystem planetarySystem = PlanetarySystemYamlIO.read(system("D", "NONE", "FS"));
        assertEquals(-2, GarrisonModifierResolver.resolveModifier(planetarySystem, LocalDate.of(2700, 1, 1)));
    }
}
