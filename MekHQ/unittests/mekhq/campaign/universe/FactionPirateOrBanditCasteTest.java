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

import static mekhq.campaign.universe.Faction.BANDIT_CASTE_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;

import megamek.common.universe.FactionTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The Bandit Caste is a Clan flavor of the pirates, so anything that special-cases "the pirates" by faction code has to
 * treat {@code BAN} the same as {@code PIR}. These tests pin the helpers that make that possible.
 */
class FactionPirateOrBanditCasteTest {
    private static Faction faction(String code, FactionTag... tags) {
        Faction faction = new Faction(code, code);
        EnumSet<FactionTag> tagSet = EnumSet.noneOf(FactionTag.class);
        tagSet.addAll(Arrays.asList(tags));
        faction.setTags(tagSet);
        return faction;
    }

    @ParameterizedTest
    @ValueSource(strings = { PIRATE_FACTION_CODE, BANDIT_CASTE_FACTION_CODE })
    void pirateAndBanditCasteCodesBothCountAsPirates(String code) {
        assertTrue(Faction.isPirateOrBanditCaste(code));
        assertTrue(faction(code).isPirateOrBanditCaste());
    }

    @ParameterizedTest
    @ValueSource(strings = { "MERC", "REB", "CLAN", "CW", "DC", "TD", "PSI", "BAN.IS", "" })
    void everyOtherCodeIsNotAPirate(String code) {
        assertFalse(Faction.isPirateOrBanditCaste(code));
        assertFalse(faction(code).isPirateOrBanditCaste());
    }

    @Test
    void aNullCodeIsNotAPirate() {
        assertFalse(Faction.isPirateOrBanditCaste(null));
    }

    @Test
    void clanCampaignsGetTheBanditCasteAsTheirPirates() {
        assertEquals(BANDIT_CASTE_FACTION_CODE, Faction.getPirateFactionCodeFor(faction("CW", FactionTag.CLAN)));
        assertEquals(BANDIT_CASTE_FACTION_CODE,
              Faction.getPirateFactionCodeFor(faction(BANDIT_CASTE_FACTION_CODE, FactionTag.CLAN)));
    }

    @Test
    void everyOtherCampaignGetsTheGenericPirates() {
        assertEquals(PIRATE_FACTION_CODE, Faction.getPirateFactionCodeFor(faction("DC")));
        assertEquals(PIRATE_FACTION_CODE, Faction.getPirateFactionCodeFor(faction(PIRATE_FACTION_CODE)));
        assertEquals(PIRATE_FACTION_CODE, Faction.getPirateFactionCodeFor(null));
    }
}
