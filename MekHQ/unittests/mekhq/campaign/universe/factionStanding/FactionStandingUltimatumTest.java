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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the lookup performed by {@link FactionStandingUltimatum}.
 *
 * <p>Presenting an ultimatum opens modal dialogs, so only the paths that return before any dialog is built are
 * covered here.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class FactionStandingUltimatumTest {
    private static final LocalDate ULTIMATUM_DATE = LocalDate.of(3057, 9, 18);

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static FactionStandingUltimatumsLibrary buildLibrary() {
        FactionStandingUltimatumSide lyranSide = new FactionStandingUltimatumSide("LA", "Challenger",
              PersonnelRole.NOBLE, "LA");
        FactionStandingUltimatumSide davionSide = new FactionStandingUltimatumSide("FS", "Incumbent",
              PersonnelRole.NOBLE, "FS");
        FactionStandingUltimatumData ultimatum = new FactionStandingUltimatumData("FED_COM_CIVIL_WAR",
              ULTIMATUM_DATE.toString(), null, List.of("FC"), List.of(lyranSide, davionSide), "FS", false, 0);
        return new FactionStandingUltimatumsLibrary(List.of(ultimatum));
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static Campaign buildCampaign(String factionCode) {
        Campaign campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        Faction faction = mock(Faction.class);

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getFaction()).thenReturn(faction);
        when(faction.getShortName()).thenReturn(factionCode);

        return campaign;
    }

    @Test
    @DisplayName("findUltimatum returns the ultimatum for a matching date and faction")
    void testFindUltimatumMatches() {
        FactionStandingUltimatumsLibrary library = buildLibrary();

        assertSame(library.getUltimatum(ULTIMATUM_DATE, "FC"),
              FactionStandingUltimatum.findUltimatum(ULTIMATUM_DATE, "FC", library));
    }

    @Test
    @DisplayName("findUltimatum returns null when the faction or date doesn't match")
    void testFindUltimatumNoMatch() {
        FactionStandingUltimatumsLibrary library = buildLibrary();

        assertNull(FactionStandingUltimatum.findUltimatum(ULTIMATUM_DATE, "LA", library));
        assertNull(FactionStandingUltimatum.findUltimatum(ULTIMATUM_DATE.plusDays(1), "FC", library));
    }

    @Test
    @DisplayName("findUltimatum tolerates a library that failed to load")
    void testFindUltimatumNullLibrary() {
        assertNull(FactionStandingUltimatum.findUltimatum(ULTIMATUM_DATE, "FC", null));
    }

    @Test
    @DisplayName("processUltimatum does nothing when the campaign's faction has no ultimatum")
    void testProcessUltimatumNoMatchDoesNothing() {
        Campaign campaign = buildCampaign("LA");
        PlayerForce playerForce = campaign.getPlayerForce();

        assertFalse(FactionStandingUltimatum.processUltimatum(ULTIMATUM_DATE, campaign, buildLibrary()));

        // Only the faction lookup happened; no agitators were generated
        verify(playerForce).getFaction();
        verifyNoMoreInteractions(playerForce);
    }

    @Test
    @DisplayName("processUltimatum does nothing when the library failed to load")
    void testProcessUltimatumNullLibraryDoesNothing() {
        assertFalse(FactionStandingUltimatum.processUltimatum(ULTIMATUM_DATE, buildCampaign("FC"), null));
    }
}
