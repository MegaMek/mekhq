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
package mekhq.campaign.digitalGM.stratCon.facility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests which facilities pair up as synergies.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilitySynergiesTest {
    private static final StratConCoords FIRST = new StratConCoords(1, 1);
    private static final StratConCoords SECOND = new StratConCoords(4, 4);

    private StratConTrackState track;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        track = new StratConTrackState();
        track.setWidth(8);
        track.setHeight(8);
    }

    private StratConFacility place(StratConCoords coords, ForceAlignment owner, FacilityType facilityType) {
        StratConFacility facility = StratConTestData.facility(owner,
              facilityType,
              new LocalModifiersEffect(List.of("MekGarrison.json")));
        track.addFacility(coords, facility);
        return facility;
    }

    @Test
    void thePairingsWorkInEitherOrder() {
        assertTrue(StratConFacilitySynergies.isPairing(FacilityType.ArtilleryBase, FacilityType.CommandCenter));
        assertTrue(StratConFacilitySynergies.isPairing(FacilityType.EarlyWarningSystem, FacilityType.AirBase));
        assertTrue(StratConFacilitySynergies.isPairing(FacilityType.TankBase, FacilityType.DataCenter));
        assertFalse(StratConFacilitySynergies.isPairing(FacilityType.MekBase, FacilityType.TankBase));
    }

    @Test
    void partnersMustBeHeldByTheSameSide() {
        place(FIRST, ForceAlignment.Opposing, FacilityType.ArtilleryBase);
        StratConFacility commandCenter = place(SECOND, ForceAlignment.Opposing, FacilityType.CommandCenter);

        assertEquals(List.of(commandCenter), StratConFacilitySynergies.getPartners(track, FIRST));

        commandCenter.setOwner(ForceAlignment.Allied);
        assertTrue(StratConFacilitySynergies.getPartners(track, FIRST).isEmpty());
    }

    @Test
    void aCrippledOrCutOffFacilityIsNoPartner() {
        place(FIRST, ForceAlignment.Allied, FacilityType.DataCenter);
        StratConFacility base = place(SECOND, ForceAlignment.Player, FacilityType.MekBase);

        base.setCondition(FacilityCondition.CRIPPLED);
        assertTrue(StratConFacilitySynergies.getPartners(track, FIRST).isEmpty());

        base.setCondition(FacilityCondition.INTACT);
        track.getCutOffFacilities().add(SECOND);
        assertTrue(StratConFacilitySynergies.getPartners(track, FIRST).isEmpty());
    }

    @Test
    void thePlayerIsOnlyToldOfPartnersTheyCanSee() {
        StratConFacility artilleryBase = place(FIRST, ForceAlignment.Opposing, FacilityType.ArtilleryBase);
        StratConFacility commandCenter = place(SECOND, ForceAlignment.Opposing, FacilityType.CommandCenter);

        // Not yet scouted: whether it has partners would give away its condition and supply.
        artilleryBase.setIntel(StratConFacility.FacilityIntel.LOCATED);
        commandCenter.setIntel(StratConFacility.FacilityIntel.LOCATED);
        assertTrue(StratConFacilitySynergies.getKnownPartners(track, FIRST).isEmpty());

        // Scouted, but its partner not yet found.
        artilleryBase.setIntel(StratConFacility.FacilityIntel.SCOUTED);
        commandCenter.setIntel(StratConFacility.FacilityIntel.UNKNOWN);
        assertTrue(StratConFacilitySynergies.getKnownPartners(track, FIRST).isEmpty());

        commandCenter.setIntel(StratConFacility.FacilityIntel.LOCATED);
        assertEquals(List.of(commandCenter), StratConFacilitySynergies.getKnownPartners(track, FIRST));
    }
}
