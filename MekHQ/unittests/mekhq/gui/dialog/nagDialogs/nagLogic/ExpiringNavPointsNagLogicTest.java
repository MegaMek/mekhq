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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import static mekhq.gui.dialog.nagDialogs.nagLogic.ExpiringNavPointsNagLogic.hasExpiringNavPoints;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinition;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinitions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.gui.dialog.nagDialogs.ExpiringNavPointsNagDialog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExpiringNavPointsNagLogic}, which backs the {@link ExpiringNavPointsNagDialog}.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ExpiringNavPointsNagLogicTest {
    private static final String HIDDEN_TYPE_ID = "EXPIRING_NAV_POINTS_NAG_TEST_HIDDEN";

    private AbstractContract contract;
    private StratConTrackState track;
    private LocalDate today;
    private List<StratConPointOfInterest> pointsOfInterest;

    @BeforeEach
    void init() {
        StratConPointOfInterestDefinition hiddenDefinition = new StratConPointOfInterestDefinition();
        hiddenDefinition.setTypeId(HIDDEN_TYPE_ID);
        hiddenDefinition.setHiddenUntilScouted(true);
        StratConPointOfInterestDefinitions.registerDefinition(hiddenDefinition);

        today = LocalDate.of(3025, 1, 1);
        contract = mock(AbstractContract.class);
        StratConCampaignState stratConCampaignState = mock(StratConCampaignState.class);
        track = mock(StratConTrackState.class);
        pointsOfInterest = new ArrayList<>();

        when(contract.getStratConCampaignState()).thenReturn(stratConCampaignState);
        when(stratConCampaignState.getTracks()).thenReturn(List.of(track));
        when(track.getPointsOfInterest()).thenReturn(pointsOfInterest);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(HIDDEN_TYPE_ID);
    }

    private StratConPointOfInterest addVisibleNavPoint(LocalDate expiryDate) {
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(HIDDEN_TYPE_ID,
              new StratConCoords(1, 1));
        pointOfInterest.setDisplayNameOverride("Test Nav Point");
        pointOfInterest.setRevealed(true);
        pointOfInterest.setExpiryDate(expiryDate);
        pointsOfInterest.add(pointOfInterest);
        return pointOfInterest;
    }

    @Test
    void noNavPoints() {
        assertFalse(hasExpiringNavPoints(List.of(contract), today));
    }

    @Test
    void noStratConCampaignState() {
        when(contract.getStratConCampaignState()).thenReturn(null);

        assertFalse(hasExpiringNavPoints(List.of(contract), today));
    }

    @Test
    void navPointExpiresTomorrow() {
        addVisibleNavPoint(today.plusDays(1));

        assertTrue(hasExpiringNavPoints(List.of(contract), today));
    }

    @Test
    void navPointExpiresLater() {
        addVisibleNavPoint(today.plusDays(2));

        assertFalse(hasExpiringNavPoints(List.of(contract), today));
    }

    @Test
    void navPointNeverExpires() {
        addVisibleNavPoint(null);

        assertFalse(hasExpiringNavPoints(List.of(contract), today));
    }

    @Test
    void resolvedNavPointExpiringTomorrow() {
        addVisibleNavPoint(today.plusDays(1)).setStatus(PointOfInterestStatus.RESOLVED);

        assertFalse(hasExpiringNavPoints(List.of(contract), today));
    }

    @Test
    void navPointWithLinkedScenarioExpiringTomorrow() {
        addVisibleNavPoint(today.plusDays(1)).setLinkedScenarioId(1);

        assertFalse(hasExpiringNavPoints(List.of(contract), today));
    }

    @Test
    void hiddenNavPointExpiringTomorrow() {
        addVisibleNavPoint(today.plusDays(1)).setRevealed(false);

        assertFalse(hasExpiringNavPoints(List.of(contract), today));
    }
}
