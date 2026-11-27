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
package mekhq.campaign.location;

import static mekhq.campaign.enums.CampaignTransportType.TOW_TRANSPORT;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.Vector;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.TankTrailerHitch;
import megamek.common.equipment.Transporter;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.unit.TransportAssignment;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.TransporterType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;

/**
 * A unit can only ride in, or be towed by, a carrier at the same place. Sending one of them elsewhere unloads the
 * cargo, and a load resolves the links of units kept at a base.
 */
class TransportLinksAcrossPlacesTest {
    private static final String CAMPAIGN_BUNDLE = "mekhq.resources.Campaign";

    private Campaign campaign;
    private PlayerBase base;
    private Unit tractor;
    private Unit trailer;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        campaign = PartsScenario.create().getCampaign();
        // The base shares the main force's system, so a dispatch there is an overland trip
        base = new PlayerBase(new FixedLocation(campaign.getCurrentSystem()));
        campaign.getCampaignLocationManager().addPlayerBase(base);

        tractor = towUnit(false);
        trailer = towUnit(true);
        campaign.getPlayerForce().getHangar().addUnit(tractor);
        campaign.getPlayerForce().getHangar().addUnit(trailer);
        tractor.towTrailer(trailer, null, TransporterType.TANK_TRAILER_HITCH);
    }

    private Unit towUnit(boolean isTrailer) {
        Tank tank = mock(Tank.class);
        when(tank.getWeight()).thenReturn(isTrailer ? 10.0 : 50.0);
        when(tank.isTrailer()).thenReturn(isTrailer);
        Vector<Transporter> transporters = new Vector<>();
        transporters.add(new TankTrailerHitch(true));
        when(tank.getTransports()).thenReturn(transporters);

        Unit unit = new Unit(tank, campaign);
        unit.setId(UUID.randomUUID());
        unit.initializeTransportSpace(TOW_TRANSPORT);
        return unit;
    }

    private void keepAtTheBase(Unit unit) {
        campaign.getPlayerForce().getHangar().removeUnit(unit.getId());
        base.getBaseHangar().addUnit(unit);
    }

    private void assertUnhitched() {
        assertNull(trailer.getTransportAssignment(TOW_TRANSPORT), "the trailer is no longer towed");
        assertFalse(tractor.hasTransportedUnits(TOW_TRANSPORT), "the tractor tows nothing");
    }

    @Test
    void aTrailerSentWithoutItsTractorIsUnhitchedWithAReport() {
        keepAtTheBase(trailer);

        TransportLinksAcrossPlaces.release(campaign, List.of(trailer));

        assertUnhitched();
        String report = getFormattedTextAt(CAMPAIGN_BUNDLE, "TransportLinksAcrossPlaces.unloaded.report",
              trailer.getHyperlinkedName(), tractor.getHyperlinkedName());
        assertTrue(campaign.getDailyReportLog().getLines(GENERAL).contains(report));
    }

    @Test
    void aTractorSentAwayLeavesItsTrailerBehind() {
        keepAtTheBase(tractor);

        TransportLinksAcrossPlaces.release(campaign, List.of(tractor));

        assertUnhitched();
    }

    @Test
    void aTractorAndTrailerSentTogetherStayHitched() {
        keepAtTheBase(tractor);
        keepAtTheBase(trailer);

        TransportLinksAcrossPlaces.release(campaign, List.of(tractor, trailer));

        assertSame(tractor, trailer.getTransportAssignment(TOW_TRANSPORT).getTransport());
        assertTrue(tractor.getTransportedUnits(TOW_TRANSPORT).contains(trailer));
    }

    @Test
    void dispatchingATrailerAloneUnhitchesIt() {
        LocationDispatch.dispatchTravelers(List.of(trailer), base, campaign);

        assertSame(trailer, base.getBaseHangar().getUnit(trailer.getId()), "the trailer went to the base");
        assertUnhitched();
    }

    @Test
    void dispatchingATractorWithItsTrailerKeepsThemHitched() {
        LocationDispatch.dispatchTravelers(List.of(tractor, trailer), base, campaign);

        assertSame(trailer, base.getBaseHangar().getUnit(trailer.getId()));
        assertSame(tractor, trailer.getTransportAssignment(TOW_TRANSPORT).getTransport());
    }

    @Test
    void aTrailerAtABaseFindsItsTractorThereOnLoad() {
        keepAtTheBase(tractor);
        keepAtTheBase(trailer);
        // A load leaves the trailer pointing at a placeholder for its tractor until references are fixed
        trailer.setTransportAssignment(TOW_TRANSPORT,
              new TransportAssignment(new Unit.UnitRef(tractor.getId()), TransporterType.TANK_TRAILER_HITCH));

        trailer.getTransportAssignment(TOW_TRANSPORT).fixReferences(campaign, trailer);

        assertSame(tractor, trailer.getTransportAssignment(TOW_TRANSPORT).getTransport());
    }
}
