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
package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.universe.Faction.BANDIT_CASTE_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.EnumSet;

import megamek.common.universe.FactionTag;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * The Bandit Caste has no judgment text of its own: it speaks with the pirates' voice, including when it falls back to
 * generic text.
 */
class FactionStandingUtilitiesBanditCasteTest {
    private static Faction faction(String code, FactionTag... tags) {
        Faction faction = new Faction(code, code);
        EnumSet<FactionTag> tagSet = EnumSet.noneOf(FactionTag.class);
        tagSet.addAll(Arrays.asList(tags));
        faction.setTags(tagSet);
        return faction;
    }

    private static Faction pirates() {
        return faction(PIRATE_FACTION_CODE, FactionTag.PIRATE);
    }

    private static Faction banditCaste() {
        return faction(BANDIT_CASTE_FACTION_CODE, FactionTag.CLAN, FactionTag.PIRATE);
    }

    @Test
    void banditCasteJudgesWithThePiratesText() {
        Faction pirates = pirates();
        Factions factions = mock(Factions.class);
        when(factions.getFaction(PIRATE_FACTION_CODE)).thenReturn(pirates);

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class)) {
            factionsStatic.when(Factions::getInstance).thenReturn(factions);

            assertSame(pirates, FactionStandingUtilities.getJudgmentTextFaction(banditCaste()));
        }
    }

    @Test
    void everyOtherFactionJudgesWithItsOwnText() {
        Faction clanWolf = faction("CW", FactionTag.CLAN);
        Faction pirates = pirates();

        assertSame(clanWolf, FactionStandingUtilities.getJudgmentTextFaction(clanWolf));
        assertSame(pirates, FactionStandingUtilities.getJudgmentTextFaction(pirates));
    }

    @Test
    void banditCasteFallsBackExactlyLikeThePirates() {
        Faction pirates = pirates();
        Factions factions = mock(Factions.class);
        when(factions.getFaction(PIRATE_FACTION_CODE)).thenReturn(pirates);

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class)) {
            factionsStatic.when(Factions::getInstance).thenReturn(factions);

            // The key a Bandit Caste judgment looks up is built from the pirates' code
            String key = "FactionJudgmentDialog.message.WELCOME.HELLO." + PIRATE_FACTION_CODE;

            assertEquals(FactionStandingUtilities.getFallbackFactionKey(key, pirates),
                  FactionStandingUtilities.getFallbackFactionKey(key, banditCaste()));
        }
    }
}
