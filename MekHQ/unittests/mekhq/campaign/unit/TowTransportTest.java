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
package mekhq.campaign.unit;

import static mekhq.campaign.enums.CampaignTransportType.SHIP_TRANSPORT;
import static mekhq.campaign.enums.CampaignTransportType.TACTICAL_TRANSPORT;
import static mekhq.campaign.enums.CampaignTransportType.TOW_TRANSPORT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.getEntityForUnitTesting;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.TankTrailerHitch;
import megamek.common.equipment.Transporter;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.gui.menus.TransportAssignmentMenus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Tests for TOW_TRANSPORT campaign assignments: train building, capacity math, the unassign
 * actions used by the TO&amp;E and Hangar popup menus, and the mothball bookkeeping.
 */
public class TowTransportTest {

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    private Unit buildTowUnit(Campaign campaign, double weight, boolean isTrailer) {
        Tank tank = mock(Tank.class);
        when(tank.getWeight()).thenReturn(weight);
        when(tank.isTrailer()).thenReturn(isTrailer);
        Vector<Transporter> transporters = new Vector<>();
        transporters.add(new TankTrailerHitch(true));
        when(tank.getTransports()).thenReturn(transporters);

        Unit unit = new Unit(tank, campaign);
        unit.setId(UUID.randomUUID());
        unit.initializeTransportSpace(TOW_TRANSPORT);
        return unit;
    }

    @Test
    public void towTrailerBuildsLinkedTrainAndTracksCapacity() {
        Campaign campaign = mockCampaign();
        Unit tractor = buildTowUnit(campaign, 50.0, false);
        Unit firstTrailer = buildTowUnit(campaign, 10.0, true);
        Unit secondTrailer = buildTowUnit(campaign, 20.0, true);

        // Hitch trailer 1 behind the tractor, trailer 2 behind trailer 1
        tractor.towTrailer(firstTrailer, null, TransporterType.TANK_TRAILER_HITCH);
        firstTrailer.towTrailer(secondTrailer, null, TransporterType.TANK_TRAILER_HITCH);

        // Each unit only records the trailer directly behind it - a linked list, not a flat set
        assertEquals(tractor, firstTrailer.getTransportAssignment(TOW_TRANSPORT).getTransport());
        assertEquals(firstTrailer, secondTrailer.getTransportAssignment(TOW_TRANSPORT).getTransport());
        assertEquals(1, tractor.getTransportedUnits(TOW_TRANSPORT).size());
        assertTrue(tractor.getTransportedUnits(TOW_TRANSPORT).contains(firstTrailer));
        assertEquals(1, firstTrailer.getTransportedUnits(TOW_TRANSPORT).size());
        assertTrue(firstTrailer.getTransportedUnits(TOW_TRANSPORT).contains(secondTrailer));

        // Towing capacity belongs to the lead tractor: its weight minus every trailer in the train
        assertEquals(20.0, tractor.getCurrentTransportCapacity(TOW_TRANSPORT, TransporterType.TANK_TRAILER_HITCH));
    }

    @Test
    public void unassignTransportedUnitsReleasesTrailersFromTractor() {
        Campaign campaign = mockCampaign();
        Unit tractor = buildTowUnit(campaign, 50.0, false);
        Unit trailer = buildTowUnit(campaign, 10.0, true);
        tractor.towTrailer(trailer, null, TransporterType.TANK_TRAILER_HITCH);

        // Tractor-side detach: the tractor has no tow assignment of its own, so this used to NPE
        assertDoesNotThrow(() -> TransportAssignmentMenus.unassignTransportedUnits(campaign,
              TOW_TRANSPORT, List.of(tractor)));

        assertNull(trailer.getTransportAssignment(TOW_TRANSPORT));
        assertFalse(tractor.hasTransportedUnits(TOW_TRANSPORT));
    }

    @Test
    public void unassignFromTransportReleasesTrailerAndLeavesOthersAlone() {
        Campaign campaign = mockCampaign();
        Unit tractor = buildTowUnit(campaign, 50.0, false);
        Unit firstTrailer = buildTowUnit(campaign, 10.0, true);
        Unit secondTrailer = buildTowUnit(campaign, 20.0, true);
        tractor.towTrailer(firstTrailer, null, TransporterType.TANK_TRAILER_HITCH);
        firstTrailer.towTrailer(secondTrailer, null, TransporterType.TANK_TRAILER_HITCH);

        // Trailer-side detach on the last trailer only
        TransportAssignmentMenus.unassignFromTransport(campaign, TOW_TRANSPORT, List.of(secondTrailer));

        assertNull(secondTrailer.getTransportAssignment(TOW_TRANSPORT));
        assertEquals(tractor, firstTrailer.getTransportAssignment(TOW_TRANSPORT).getTransport());
        assertTrue(tractor.getTransportedUnits(TOW_TRANSPORT).contains(firstTrailer));
    }

    @Test
    public void unassignFromTransportIgnoresUnitsWithoutAssignment() {
        Campaign campaign = mockCampaign();
        Unit tractor = buildTowUnit(campaign, 50.0, false);

        assertDoesNotThrow(() -> TransportAssignmentMenus.unassignFromTransport(campaign,
              TOW_TRANSPORT, List.of(tractor)));
    }

    @Test
    public void connectTrainHitchesTrailersInDialogOrder() {
        Campaign campaign = mockCampaign();
        Unit tractor = buildTowUnit(campaign, 50.0, false);
        when(tractor.getEntity().canTow(anyInt())).thenReturn(true);
        Unit firstTrailer = buildTowUnit(campaign, 10.0, true);
        Unit secondTrailer = buildTowUnit(campaign, 20.0, true);

        assertTrue(TransportAssignmentMenus.connectTrain(campaign, tractor,
              List.of(firstTrailer, secondTrailer)));

        // The train forms in the given order: tractor -> first -> second
        assertEquals(tractor, firstTrailer.getTransportAssignment(TOW_TRANSPORT).getTransport());
        assertEquals(firstTrailer, secondTrailer.getTransportAssignment(TOW_TRANSPORT).getTransport());
        assertEquals(20.0, tractor.getCurrentTransportCapacity(TOW_TRANSPORT, TransporterType.TANK_TRAILER_HITCH));
    }

    @Test
    public void connectTrainRefusesOverweightTrainAtomically() {
        Campaign campaign = mockCampaign();
        Unit tractor = buildTowUnit(campaign, 50.0, false);
        when(tractor.getEntity().canTow(anyInt())).thenReturn(true);
        Unit firstTrailer = buildTowUnit(campaign, 30.0, true);
        Unit secondTrailer = buildTowUnit(campaign, 30.0, true);

        // 60 tons of trailers behind a 50 ton tractor: refused, and nothing is hitched
        assertFalse(TransportAssignmentMenus.connectTrain(campaign, tractor,
              List.of(firstTrailer, secondTrailer)));

        assertNull(firstTrailer.getTransportAssignment(TOW_TRANSPORT));
        assertNull(secondTrailer.getTransportAssignment(TOW_TRANSPORT));
        assertFalse(tractor.hasTransportedUnits(TOW_TRANSPORT));
    }

    @Test
    public void disconnectTrainDissolvesWholeTrainFromAnyMember() {
        Campaign campaign = mockCampaign();
        Unit tractor = buildTowUnit(campaign, 50.0, false);
        Unit firstTrailer = buildTowUnit(campaign, 10.0, true);
        Unit secondTrailer = buildTowUnit(campaign, 20.0, true);
        tractor.towTrailer(firstTrailer, null, TransporterType.TANK_TRAILER_HITCH);
        firstTrailer.towTrailer(secondTrailer, null, TransporterType.TANK_TRAILER_HITCH);

        // Dissolve from a middle trailer - the whole train comes apart, not just one link
        TransportAssignmentMenus.disconnectTrain(campaign, firstTrailer);

        assertNull(firstTrailer.getTransportAssignment(TOW_TRANSPORT));
        assertNull(secondTrailer.getTransportAssignment(TOW_TRANSPORT));
        assertFalse(tractor.hasTransportedUnits(TOW_TRANSPORT));
        assertFalse(firstTrailer.hasTransportedUnits(TOW_TRANSPORT));

        // Calling it on a unit not in a train is a no-op
        assertDoesNotThrow(() -> TransportAssignmentMenus.disconnectTrain(campaign, tractor));
    }

    private Campaign xmlCampaign() {
        Campaign campaign = mockCampaign();
        when(campaign.getEntities()).thenReturn(new ArrayList<>());
        return campaign;
    }

    private String writeUnitToXml(Unit unit) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        unit.writeToXML(printWriter, 0);
        printWriter.flush();
        return stringWriter.toString();
    }

    private Unit loadUnitFromXml(String unitXml, Campaign campaign) throws Exception {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(new ByteArrayInputStream(
              unitXml.getBytes(StandardCharsets.UTF_8)));
        return Unit.generateInstanceFromXML(document.getDocumentElement(), new Version(), campaign);
    }

    @Test
    public void towAssignmentSurvivesXmlRoundTripAndStaysTow() throws Exception {
        Campaign campaign = xmlCampaign();
        Entity trailerEntity = getEntityForUnitTesting("Caravan Heavy Transport", true);
        assertNotNull(trailerEntity, "Caravan Heavy Transport.blk missing from test resources");

        Unit trailer = new Unit(trailerEntity, campaign);
        trailer.setId(UUID.randomUUID());
        UUID tractorId = UUID.randomUUID();
        Unit tractor = mock(Unit.class);
        when(tractor.getId()).thenReturn(tractorId);
        trailer.setTransportAssignment(TOW_TRANSPORT,
              new TransportAssignment(tractor, TransporterType.TANK_TRAILER_HITCH));

        Unit loadedTrailer = loadUnitFromXml(writeUnitToXml(trailer), campaign);

        assertNotNull(loadedTrailer);
        assertTrue(loadedTrailer.hasTransportAssignment(TOW_TRANSPORT));
        assertEquals(tractorId, loadedTrailer.getTransportAssignment(TOW_TRANSPORT).getTransport().getId());
        assertEquals(TransporterType.TANK_TRAILER_HITCH,
              loadedTrailer.getTransportAssignment(TOW_TRANSPORT).getTransporterType());
        // The tow entry must not be mistaken for the legacy attribute-less tactical format
        assertFalse(loadedTrailer.hasTacticalTransportAssignment());
    }

    @Test
    public void towTransportedUnitsSurviveXmlRoundTrip() throws Exception {
        Campaign campaign = xmlCampaign();
        Entity tractorEntity = getEntityForUnitTesting("Prime Mover", true);
        assertNotNull(tractorEntity, "Prime Mover.blk missing from test resources");

        Unit tractor = new Unit(tractorEntity, campaign);
        tractor.setId(UUID.randomUUID());
        UUID trailerId = UUID.randomUUID();
        Unit trailer = mock(Unit.class);
        when(trailer.getId()).thenReturn(trailerId);
        tractor.addTransportedUnit(TOW_TRANSPORT, trailer);

        Unit loadedTractor = loadUnitFromXml(writeUnitToXml(tractor), campaign);

        assertNotNull(loadedTractor);
        assertTrue(loadedTractor.hasTransportedUnits(TOW_TRANSPORT));
        assertEquals(1, loadedTractor.getTransportedUnits(TOW_TRANSPORT).size());
        assertEquals(trailerId, loadedTractor.getTransportedUnits(TOW_TRANSPORT).iterator().next().getId());
        assertFalse(loadedTractor.hasShipTransportedUnits());
        assertFalse(loadedTractor.hasTacticalTransportedUnits());
    }

    @Test
    public void legacyAssignmentWithoutTypeAttributeStillLoadsAsTactical() throws Exception {
        Campaign campaign = xmlCampaign();
        Entity trailerEntity = getEntityForUnitTesting("Caravan Heavy Transport", true);
        assertNotNull(trailerEntity, "Caravan Heavy Transport.blk missing from test resources");

        Unit unit = new Unit(trailerEntity, campaign);
        unit.setId(UUID.randomUUID());
        UUID transportId = UUID.randomUUID();
        Unit transport = mock(Unit.class);
        when(transport.getId()).thenReturn(transportId);
        unit.setTransportAssignment(CampaignTransportType.TACTICAL_TRANSPORT,
              new TransportAssignment(transport, TransporterType.MEK_BAY));

        // Rewrite the tactical entries to the pre-attribute legacy format a pre-50.07 save has
        String unitXml = writeUnitToXml(unit)
                               .replace(" campaignTransportType=\"TACTICAL_TRANSPORT\"", "");
        Unit loadedUnit = loadUnitFromXml(unitXml, campaign);

        assertNotNull(loadedUnit);
        assertTrue(loadedUnit.hasTacticalTransportAssignment());
        assertEquals(transportId, loadedUnit.getTacticalTransportAssignment().getTransport().getId());
        assertFalse(loadedUnit.hasTransportAssignment(TOW_TRANSPORT));
    }

    @Test
    public void completeMothballRemovesTowTransporterFromCampaignMap() {
        Campaign campaign = mockCampaign();
        Tank tank = mock(Tank.class);
        Unit tractor = spy(new Unit(tank, campaign));
        tractor.setId(UUID.randomUUID());
        doReturn(Set.of()).when(tractor).getTransportCapabilities(SHIP_TRANSPORT);
        doReturn(Set.of()).when(tractor).getTransportCapabilities(TACTICAL_TRANSPORT);
        doReturn(Set.of(TransporterType.TANK_TRAILER_HITCH)).when(tractor).getTransportCapabilities(TOW_TRANSPORT);
        // Crew handling is not under test here and needs a full campaign to run
        doNothing().when(tractor).resetPilotAndEntity();

        tractor.completeMothball();

        verify(campaign).removeCampaignTransporter(TOW_TRANSPORT, tractor);
        verify(campaign, never()).removeCampaignTransporter(SHIP_TRANSPORT, tractor);
        verify(campaign, never()).removeCampaignTransporter(TACTICAL_TRANSPORT, tractor);
    }

    @Test
    public void completeActivationAddsTowTransporterToCampaignMap() {
        Campaign campaign = mockCampaign();
        Tank tank = mock(Tank.class);
        Unit tractor = spy(new Unit(tank, campaign));
        tractor.setId(UUID.randomUUID());
        doReturn(Set.of()).when(tractor).getTransportCapabilities(SHIP_TRANSPORT);
        doReturn(Set.of()).when(tractor).getTransportCapabilities(TACTICAL_TRANSPORT);
        doReturn(Set.of(TransporterType.TANK_TRAILER_HITCH)).when(tractor).getTransportCapabilities(TOW_TRANSPORT);

        tractor.completeActivation();

        verify(campaign).addCampaignTransport(TOW_TRANSPORT, tractor);
        verify(campaign, never()).addCampaignTransport(SHIP_TRANSPORT, tractor);
        verify(campaign, never()).addCampaignTransport(TACTICAL_TRANSPORT, tractor);
    }
}
