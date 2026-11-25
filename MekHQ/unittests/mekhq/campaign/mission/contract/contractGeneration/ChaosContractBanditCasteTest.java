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
package mekhq.campaign.mission.contract.contractGeneration;

import static mekhq.campaign.universe.Faction.BANDIT_CASTE_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;

import megamek.common.compute.Compute;
import megamek.common.universe.FactionTag;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.contract.contractGeneration.AbstractContractDeterminationEmployer.EmployerFactions;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/**
 * The Bandit Caste is a Clan flavor of the pirates, so the contract market has to treat a Bandit Caste campaign exactly
 * like a pirate one, and a Clan campaign's pirate employers are the Bandit Caste rather than the generic pirates.
 */
class ChaosContractBanditCasteTest {
    private static final LocalDate DATE = LocalDate.of(3060, 1, 1);

    private static Faction faction(String code, FactionTag... tags) {
        Faction faction = new Faction(code, code);
        EnumSet<FactionTag> tagSet = EnumSet.noneOf(FactionTag.class);
        tagSet.addAll(Arrays.asList(tags));
        faction.setTags(tagSet);
        return faction;
    }

    @ParameterizedTest
    @ValueSource(strings = { PIRATE_FACTION_CODE, BANDIT_CASTE_FACTION_CODE })
    void pirateAndBanditCasteForcesAreNotGovernments(String code) {
        assertFalse(ChaosContractMarketAvailability.isGovernmentFaction(faction(code, FactionTag.PIRATE)));
    }

    @Test
    void mercenaryForcesAreNotGovernments() {
        assertFalse(ChaosContractMarketAvailability.isGovernmentFaction(faction("MERC", FactionTag.MERC)));
    }

    @Test
    void ordinaryFactionsAreGovernments() {
        assertTrue(ChaosContractMarketAvailability.isGovernmentFaction(faction("DC")));
        assertTrue(ChaosContractMarketAvailability.isGovernmentFaction(faction("CW", FactionTag.CLAN)));
    }

    @Test
    void aClanCampaignsPirateEmployerIsTheBanditCaste() {
        assertPirateEmployerFor(faction("CW", FactionTag.CLAN), BANDIT_CASTE_FACTION_CODE);
    }

    @Test
    void aBanditCasteCampaignsPirateEmployerIsTheBanditCaste() {
        assertPirateEmployerFor(faction(BANDIT_CASTE_FACTION_CODE, FactionTag.CLAN, FactionTag.PIRATE),
              BANDIT_CASTE_FACTION_CODE);
    }

    @Test
    void anInnerSphereCampaignsPirateEmployerIsThePirates() {
        assertPirateEmployerFor(faction("DC"), PIRATE_FACTION_CODE);
    }

    @Test
    void aPirateCampaignsPirateEmployerIsThePirates() {
        assertPirateEmployerFor(faction(PIRATE_FACTION_CODE, FactionTag.PIRATE), PIRATE_FACTION_CODE);
    }

    private static void assertPirateEmployerFor(Faction playerFaction, String expectedPirateCode) {
        Faction pirates = faction(PIRATE_FACTION_CODE, FactionTag.PIRATE);
        Faction banditCaste = faction(BANDIT_CASTE_FACTION_CODE, FactionTag.CLAN, FactionTag.PIRATE);
        Factions factions = mock(Factions.class);
        when(factions.getFaction(PIRATE_FACTION_CODE)).thenReturn(pirates);
        when(factions.getFaction(BANDIT_CASTE_FACTION_CODE)).thenReturn(banditCaste);

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class);
              MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            factionsStatic.when(Factions::getInstance).thenReturn(factions);
            // Anything other than 0 means no real power is secretly sponsoring the raid
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(1);

            EmployerFactions employer = new ChaosContractDeterminationEmployerPirate().determineEmployerFactions(
                  ChaosEmployerType.UNDERWORLD_CONTACT, DATE, mock(ILocation.class), false, playerFaction);

            Faction expected = expectedPirateCode.equals(BANDIT_CASTE_FACTION_CODE) ? banditCaste : pirates;
            assertSame(expected, employer.flavor());
            assertSame(expected, employer.anchor());
        }
    }
}
