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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.universe.Faction2;
import org.junit.jupiter.api.Test;

/**
 * A faction's era modifier list stops when the faction ends, but a campaign can carry on past that point. A year
 * beyond the list used to throw, so every repair, refit and maintenance roll failed (issue #5967). It now uses the last
 * era listed. The faction here lists three eras, from the Age of War to the Star League, as the Star League does.
 */
class FactionEraModifierTest {
    private static final String SHORT_LIST_FACTION = """
          key: SHORT
          name: Short List Faction
          eraMods:
            - 1
            - 2
            - 3
          """;

    private static final int AGE_OF_WAR_YEAR = 2500;
    private static final int STAR_LEAGUE_YEAR = 2700;
    private static final int CLAN_INVASION_YEAR = 3055;
    private static final int ILCLAN_YEAR = 3151;

    private static Faction shortListFaction() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return new Faction(mapper.readValue(SHORT_LIST_FACTION, Faction2.class));
    }

    @Test
    void aYearInsideTheListUsesItsOwnEra() throws Exception {
        Faction faction = shortListFaction();

        assertEquals(1, faction.getEraMod(AGE_OF_WAR_YEAR));
        assertEquals(3, faction.getEraMod(STAR_LEAGUE_YEAR));
    }

    @Test
    void aYearPastTheListUsesTheLastEraListed() throws Exception {
        Faction faction = shortListFaction();

        assertEquals(3, faction.getEraMod(CLAN_INVASION_YEAR));
        assertEquals(3, faction.getEraMod(ILCLAN_YEAR));
    }
}
