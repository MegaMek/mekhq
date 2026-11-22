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
package mekhq.campaign.personnel.autoAwards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.MMConstants;
import megamek.common.universe.Faction2;
import megamek.common.universe.Factions2;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.AwardsFactory;
import org.junit.jupiter.api.Test;

/**
 * Checks the Standard award set's Theatre of War awards name real factions. autoAwards matches belligerents by
 * faction code, so a full faction name never matches anything.
 */
class StandardAwardSetTest {
    private static final List<String> SUPER_FACTIONS = List.of("majorpowers", "innersphere", "clans", "periphery",
          "pirate", "mercenary", "independent", "deepperiphery", "comstar", "wob", "comstarorwob");

    @Test
    void testTheatreOfWarBelligerentsAreFactionCodes() {
        // The shipped factions, as the test faction data holds only a handful. Loaded apart from the shared instance so
        // other tests are unaffected.
        Factions2 factions = new Factions2(MMConstants.FACTIONS_DIR);
        assertFalse(factions.getFactions().isEmpty(), "No faction data found in " + MMConstants.FACTIONS_DIR);

        List<Award> theatreOfWarAwards = AwardsFactory.getInstance().getAllAwardsForSet("standard").stream()
                                               .filter(award -> award.getItem()
                                                                      .replaceAll("\\s", "")
                                                                      .equalsIgnoreCase("theatreofwar"))
                                               .toList();
        assertFalse(theatreOfWarAwards.isEmpty());

        for (Award award : theatreOfWarAwards) {
            for (String belligerent : award.getRange().split(",")) {
                String name = belligerent.replaceAll("\\([12]\\)", "").trim();

                if (SUPER_FACTIONS.contains(name.toLowerCase().replaceAll("\\s", ""))) {
                    continue;
                }

                assertEquals(name, factions.getFaction(name).map(Faction2::getKey).orElse(null),
                      award.getName() + " lists '" + name + "', which is not a faction code");
            }

            assertTrue(award.getSize().matches("\\d{4},\\s*\\d{4}"), award.getName() + " needs a start and end year");
        }
    }
}
