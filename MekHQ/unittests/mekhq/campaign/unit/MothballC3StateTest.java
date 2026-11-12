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

import static mekhq.campaign.force.Formation.FORMATION_NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.util.C3Util;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.NetworkChangedEvent;
import mekhq.campaign.force.PlayerForce;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.MockedStatic;
import org.w3c.dom.Document;

/**
 * Tests for {@link MothballC3State}: what it captures when a unit is mothballed, that the capture survives a save, and
 * that activation only re-applies links that are still legal.
 *
 * <p>MegaMek's {@link C3Util} is mocked so each test controls whether MegaMek's own equipment and capacity rules
 * accept a link. What is under test is MekHQ's decisions: which links it attempts, which it refuses to attempt, and
 * how it reports the ones that were dropped.</p>
 */
class MothballC3StateTest {
    private static final int IN_FORMATION = 3;
    private static final String NETWORK_ID = "C3i.100";
    private static final String DETACHED_NETWORK_ID = "C3i.1";

    private Campaign campaign;
    private Game game;
    private PlayerForce force;
    private MockedStatic<C3Util> c3Util;
    private MockedStatic<MekHQ> mekHQ;
    private final List<Unit> allUnits = new ArrayList<>();
    private int nextEntityId = 1;

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class);
        game = mock(Game.class);
        force = mock(PlayerForce.class);
        when(campaign.getPlayerForce()).thenReturn(force);
        when(campaign.getGame()).thenReturn(game);
        when(force.allUnits()).thenReturn(allUnits);

        c3Util = mockStatic(C3Util.class);
        c3Util.when(() -> C3Util.sameNhC3System(any(), any())).thenReturn(true);
        mekHQ = mockStatic(MekHQ.class);
    }

    @AfterEach
    void tearDown() {
        c3Util.close();
        mekHQ.close();
    }

    // region Capture

    @Test
    void unitOutsideTheTableOfOrganizationCapturesNothing() {
        Unit slave = slave(master());
        when(slave.getFormationId()).thenReturn(FORMATION_NONE);

        assertTrue(new MothballC3State(slave, campaign).isEmpty());
    }

    @Test
    void unitWithoutACampaignCapturesNothing() {
        Unit slave = slave(master());

        assertTrue(new MothballC3State(slave, null).isEmpty());
    }

    @Test
    void unitWithoutAnyC3LinksCapturesNothing() {
        Unit loner = unitWithoutC3();

        assertTrue(new MothballC3State(loner, campaign).isEmpty());
    }

    @Test
    void slaveCapturesItsMaster() {
        assertFalse(new MothballC3State(slave(master()), campaign).isEmpty());
    }

    @Test
    void companyMasterIsCaptured() {
        Unit companyMaster = master();
        when(companyMaster.getEntity().getC3MasterIsUUIDAsString()).thenReturn(c3Uuid(companyMaster));

        assertFalse(new MothballC3State(companyMaster, campaign).isEmpty());
    }

    @Test
    void masterCapturesItsNodes() {
        Unit master = master();
        Unit node = slave(master);
        when(force.getC3Nodes(master)).thenReturn(List.of(node));

        assertFalse(new MothballC3State(master, campaign).isEmpty());
    }

    @Test
    void masterWithNoNodesAndNoMasterCapturesNothing() {
        assertTrue(new MothballC3State(master(), campaign).isEmpty());
    }

    @Test
    void networkMembersAreCaptured() {
        Unit unit = networkMember(NETWORK_ID);
        networkMember(NETWORK_ID);

        assertFalse(new MothballC3State(unit, campaign).isEmpty());
    }

    @Test
    void unitsOnAnotherNetworkAreNotCaptured() {
        Unit unit = networkMember(NETWORK_ID);
        networkMember("C3i.200");

        assertTrue(new MothballC3State(unit, campaign).isEmpty());
    }

    // endregion Capture

    // region Save and load

    @Test
    void emptyStateWritesNothing() {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        new MothballC3State().writeToXML(printWriter, 0);
        printWriter.flush();

        assertTrue(writer.toString().isBlank());
    }

    @Test
    void savedStateIsStillRestorableAfterLoading() throws Exception {
        Unit master = master();
        Unit node = slave(master);
        when(force.getC3Nodes(master)).thenReturn(List.of(node));
        MothballC3State loaded = roundTrip(new MothballC3State(master, campaign));
        detach(node);

        int dropped = loaded.restore(master, campaign);

        assertFalse(loaded.isEmpty());
        assertEquals(0, dropped);
        verifyConnected(node, master);
    }

    @Test
    void savedStatePreservesEveryKindOfLink() throws Exception {
        Unit master = master();
        Unit upstream = master();
        when(master.getEntity().getC3MasterIsUUIDAsString()).thenReturn(c3Uuid(upstream));
        Unit node = slave(master);
        when(force.getC3Nodes(master)).thenReturn(List.of(node));
        MothballC3State loaded = roundTrip(new MothballC3State(master, campaign));
        detach(master);
        detach(node);

        int dropped = loaded.restore(master, campaign);

        assertEquals(0, dropped);
        verifyConnected(master, upstream);
        verifyConnected(node, master);
    }

    // endregion Save and load

    // region Restoring hierarchical C3

    @Test
    void slaveIsReconnectedToItsMaster() {
        Unit master = master();
        Unit slave = slave(master);
        MothballC3State state = new MothballC3State(slave, campaign);
        detach(slave);

        int dropped = state.restore(slave, campaign);

        assertEquals(0, dropped);
        verifyConnected(slave, master);
        mekHQ.verify(() -> MekHQ.triggerEvent(any(NetworkChangedEvent.class)));
    }

    @Test
    void slaveThatIsStillLinkedIsLeftAlone() {
        Unit master = master();
        Unit slave = slave(master);
        MothballC3State state = new MothballC3State(slave, campaign);
        // Plain slaves are not detached by mothballing, so the master link is still in place

        int dropped = state.restore(slave, campaign);

        assertEquals(0, dropped);
        verifyNoConnection();
    }

    @Test
    void nodesAreReconnectedToTheirMaster() {
        Unit master = master();
        Unit nodeOne = slave(master);
        Unit nodeTwo = slave(master);
        when(force.getC3Nodes(master)).thenReturn(List.of(nodeOne, nodeTwo));
        MothballC3State state = new MothballC3State(master, campaign);
        detach(nodeOne);
        detach(nodeTwo);

        int dropped = state.restore(master, campaign);

        assertEquals(0, dropped);
        verifyConnected(nodeOne, master);
        verifyConnected(nodeTwo, master);
    }

    @Test
    void connectionsMegaMekRefusesAreDroppedAndCounted() throws Exception {
        Unit master = master();
        Unit nodeOne = slave(master);
        Unit nodeTwo = slave(master);
        when(force.getC3Nodes(master)).thenReturn(List.of(nodeOne, nodeTwo));
        MothballC3State state = new MothballC3State(master, campaign);
        detach(nodeOne);
        detach(nodeTwo);
        // Another unit took the last free node while the master was mothballed
        Entity nodeTwoEntity = nodeTwo.getEntity();
        c3Util.when(() -> C3Util.connect(any(), argThat(containing(nodeTwoEntity)), anyInt(), anyBoolean()))
              .thenThrow(new C3Util.C3CapacityException());

        int dropped = state.restore(master, campaign);

        assertEquals(1, dropped);
        verifyConnected(nodeOne, master);
        verify(nodeTwo.getEntity(), never()).setC3MasterIsUUIDAsString(any());
    }

    @Test
    void nodeThatLostItsEquipmentIsDropped() throws Exception {
        Unit master = master();
        Unit node = slave(master);
        when(force.getC3Nodes(master)).thenReturn(List.of(node));
        MothballC3State state = new MothballC3State(master, campaign);
        detach(node);
        c3Util.when(() -> C3Util.connect(any(), any(), anyInt(), anyBoolean()))
              .thenThrow(new C3Util.MismatchingC3MException());

        assertEquals(1, state.restore(master, campaign));
        verify(node.getEntity(), never()).setC3MasterIsUUIDAsString(any());
    }

    @Test
    void nodeReassignedToAnotherMasterIsNotTakenBack() {
        Unit master = master();
        Unit node = slave(master);
        when(force.getC3Nodes(master)).thenReturn(List.of(node));
        MothballC3State state = new MothballC3State(master, campaign);
        when(node.getEntity().getC3MasterId()).thenReturn(999);

        assertEquals(1, state.restore(master, campaign));
        verifyNoConnection();
    }

    @Test
    void mothballedPartnerIsDropped() {
        Unit master = master();
        Unit slave = slave(master);
        MothballC3State state = new MothballC3State(slave, campaign);
        detach(slave);
        when(master.isMothballed()).thenReturn(true);

        assertEquals(1, state.restore(slave, campaign));
        verifyNoConnection();
    }

    @Test
    void partnerBeingMothballedIsDropped() {
        Unit master = master();
        Unit slave = slave(master);
        MothballC3State state = new MothballC3State(slave, campaign);
        detach(slave);
        when(master.isMothballing()).thenReturn(true);

        assertEquals(1, state.restore(slave, campaign));
        verifyNoConnection();
    }

    @Test
    void partnerOutsideTheTableOfOrganizationIsDropped() {
        Unit master = master();
        Unit slave = slave(master);
        MothballC3State state = new MothballC3State(slave, campaign);
        detach(slave);
        when(master.getFormationId()).thenReturn(FORMATION_NONE);

        assertEquals(1, state.restore(slave, campaign));
        verifyNoConnection();
    }

    @Test
    void partnerThatNoLongerExistsIsDropped() {
        Unit master = master();
        Unit slave = slave(master);
        MothballC3State state = new MothballC3State(slave, campaign);
        detach(slave);
        when(campaign.getUnit(any(UUID.class))).thenReturn(null);

        assertEquals(1, state.restore(slave, campaign));
        verifyNoConnection();
    }

    @Test
    void unitThatDidNotReturnToTheTableOfOrganizationIsNotRewired() {
        Unit master = master();
        Unit slave = slave(master);
        MothballC3State state = new MothballC3State(slave, campaign);
        detach(slave);
        when(slave.getFormationId()).thenReturn(FORMATION_NONE);

        assertEquals(0, state.restore(slave, campaign));
        verifyNoConnection();
        mekHQ.verifyNoInteractions();
    }

    @Test
    void nothingIsAnnouncedWhenNothingChanged() {
        Unit master = master();
        Unit slave = slave(master);
        MothballC3State state = new MothballC3State(slave, campaign);
        detach(slave);
        when(master.isMothballed()).thenReturn(true);

        state.restore(slave, campaign);

        verify(force, never()).refreshNetworks(any());
        mekHQ.verifyNoInteractions();
    }

    // endregion Restoring hierarchical C3

    // region Restoring company masters

    @Test
    void companyMasterIsRestoredBeforeItsNodesAreAttached() {
        Unit companyMaster = master();
        when(companyMaster.getEntity().getC3MasterIsUUIDAsString()).thenReturn(c3Uuid(companyMaster));
        Unit node = slave(companyMaster);
        when(force.getC3Nodes(companyMaster)).thenReturn(List.of(node));
        MothballC3State state = new MothballC3State(companyMaster, campaign);
        detach(companyMaster);
        detach(node);

        int dropped = state.restore(companyMaster, campaign);

        assertEquals(0, dropped);
        Entity companyMasterEntity = companyMaster.getEntity();
        c3Util.verify(() -> C3Util.setCompanyMaster(argThat(containing(companyMasterEntity))));
        verify(companyMaster.getEntity()).setC3MasterIsUUIDAsString(c3Uuid(companyMaster));
        verifyConnected(node, companyMaster);
    }

    @Test
    void nodesAreNotAttachedAsALanceWhenCompanyRoleCannotBeRestored() throws Exception {
        Unit companyMaster = master();
        when(companyMaster.getEntity().getC3MasterIsUUIDAsString()).thenReturn(c3Uuid(companyMaster));
        Unit node = slave(companyMaster);
        when(force.getC3Nodes(companyMaster)).thenReturn(List.of(node));
        MothballC3State state = new MothballC3State(companyMaster, campaign);
        detach(companyMaster);
        detach(node);
        // The master computer was destroyed while the unit sat mothballed
        c3Util.when(() -> C3Util.setCompanyMaster(any())).thenThrow(new C3Util.MissingC3MException());

        int dropped = state.restore(companyMaster, campaign);

        assertEquals(2, dropped, "The company role and the node should both be counted as dropped");
        verifyNoConnection();
        verify(companyMaster.getEntity(), never()).setC3MasterIsUUIDAsString(any());
    }

    // endregion Restoring company masters

    // region Restoring non-hierarchical networks

    @Test
    void unitRejoinsItsFormerNetworkWhenThereIsRoom() {
        Unit unit = networkMember(NETWORK_ID);
        networkMember(NETWORK_ID);
        MothballC3State state = new MothballC3State(unit, campaign);
        when(unit.getEntity().getC3NetId()).thenReturn(DETACHED_NETWORK_ID);

        int dropped = state.restore(unit, campaign);

        assertEquals(0, dropped);
        verify(force).addUnitsToNetwork(argThat(vectorContaining(unit)), eq(NETWORK_ID), eq(game));
        mekHQ.verify(() -> MekHQ.triggerEvent(any(NetworkChangedEvent.class)));
    }

    @Test
    void unitDoesNotRejoinANetworkThatIsFull() {
        Unit unit = networkMember(NETWORK_ID);
        for (int i = 0; i < Entity.MAX_C3i_NODES; i++) {
            networkMember(NETWORK_ID);
        }
        MothballC3State state = new MothballC3State(unit, campaign);
        when(unit.getEntity().getC3NetId()).thenReturn(DETACHED_NETWORK_ID);

        int dropped = state.restore(unit, campaign);

        assertEquals(1, dropped);
        verify(force, never()).addUnitsToNetwork(any(), any(), any());
    }

    @Test
    void novaCewsNetworkHoldsOnlyThreeUnits() {
        Unit unit = networkMember(NETWORK_ID);
        for (int i = 0; i < Entity.MAX_NOVA_CEWS_NODES; i++) {
            Unit member = networkMember(NETWORK_ID);
            when(member.getEntity().hasNovaCEWS()).thenReturn(true);
        }
        MothballC3State state = new MothballC3State(unit, campaign);
        when(unit.getEntity().getC3NetId()).thenReturn(DETACHED_NETWORK_ID);

        int dropped = state.restore(unit, campaign);

        assertEquals(1, dropped);
        verify(force, never()).addUnitsToNetwork(any(), any(), any());
    }

    @Test
    void unitJoinsALaterPartnerWhenTheFirstIsNoLongerUsable() {
        Unit unit = networkMember(NETWORK_ID);
        Unit mothballedPartner = networkMember(NETWORK_ID);
        networkMember(NETWORK_ID);
        MothballC3State state = new MothballC3State(unit, campaign);
        when(unit.getEntity().getC3NetId()).thenReturn(DETACHED_NETWORK_ID);
        when(mothballedPartner.isMothballed()).thenReturn(true);

        int dropped = state.restore(unit, campaign);

        assertEquals(0, dropped);
        verify(force).addUnitsToNetwork(argThat(vectorContaining(unit)), eq(NETWORK_ID), eq(game));
    }

    @Test
    void unitDoesNotJoinANetworkRunningADifferentSystem() {
        Unit unit = networkMember(NETWORK_ID);
        networkMember(NETWORK_ID);
        MothballC3State state = new MothballC3State(unit, campaign);
        when(unit.getEntity().getC3NetId()).thenReturn(DETACHED_NETWORK_ID);
        // The partner was refitted from C3i to Naval C3 while the unit was mothballed
        c3Util.when(() -> C3Util.sameNhC3System(any(), any())).thenReturn(false);

        int dropped = state.restore(unit, campaign);

        assertEquals(1, dropped);
        verify(force, never()).addUnitsToNetwork(any(), any(), any());
    }

    @Test
    void unitWithoutANonHierarchicalSystemIsNotPutOnANetwork() {
        Unit unit = networkMember(NETWORK_ID);
        networkMember(NETWORK_ID);
        MothballC3State state = new MothballC3State(unit, campaign);
        // The C3i computer was removed in a refit
        when(unit.getEntity().hasNhC3()).thenReturn(false);

        state.restore(unit, campaign);

        verify(force, never()).addUnitsToNetwork(any(), any(), any());
    }

    // endregion Restoring non-hierarchical networks

    // region Helpers

    // Plain lookups, so no mock is ever invoked while another mock is mid-stubbing or mid-verification
    private final Map<Unit, String> c3Uuids = new IdentityHashMap<>();
    private final Map<Unit, Integer> entityIds = new IdentityHashMap<>();

    private Entity entity(String c3Uuid, int entityId, boolean isMaster, boolean isSlave) {
        Entity entity = mock(Entity.class);
        when(entity.getId()).thenReturn(entityId);
        when(entity.getShortName()).thenReturn("Entity " + entityId);
        when(entity.getC3UUIDAsString()).thenReturn(c3Uuid);
        when(entity.getGame()).thenReturn(game);
        when(entity.getC3MasterId()).thenReturn(Entity.NONE);
        when(entity.hasC3M()).thenReturn(isMaster);
        when(entity.hasC3S()).thenReturn(isSlave);
        when(entity.hasC3()).thenReturn(isMaster || isSlave);
        return entity;
    }

    private Unit unit(boolean isMaster, boolean isSlave) {
        int entityId = nextEntityId++;
        String c3Uuid = "uuid-" + entityId;
        Unit unit = mock(Unit.class);
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);
        when(unit.getEntity()).thenReturn(entity(c3Uuid, entityId, isMaster, isSlave));
        when(unit.getFormationId()).thenReturn(IN_FORMATION);
        when(campaign.getUnit(unitId)).thenReturn(unit);
        allUnits.add(unit);
        c3Uuids.put(unit, c3Uuid);
        entityIds.put(unit, entityId);
        return unit;
    }

    private Unit unitWithoutC3() {
        return unit(false, false);
    }

    private Unit master() {
        return unit(true, false);
    }

    /** A unit slaved to {@code master}. */
    private Unit slave(Unit master) {
        Unit slave = unit(false, true);
        Entity entity = slave.getEntity();
        when(entity.getC3MasterIsUUIDAsString()).thenReturn(c3Uuid(master));
        when(entity.getC3MasterId()).thenReturn(entityIds.get(master));
        return slave;
    }

    /** A unit with a non-hierarchical C3 system, currently on the network {@code networkId}. */
    private Unit networkMember(String networkId) {
        Unit unit = unit(false, false);
        Entity entity = unit.getEntity();
        when(entity.hasNhC3()).thenReturn(true);
        when(entity.getC3NetId()).thenReturn(networkId);
        return unit;
    }

    private String c3Uuid(Unit unit) {
        return c3Uuids.get(unit);
    }

    /** Puts the unit in the state mothballing leaves it in: its own master link has been cleared. */
    private static void detach(Unit unit) {
        Entity entity = unit.getEntity();
        when(entity.getC3MasterIsUUIDAsString()).thenReturn(null);
        when(entity.getC3MasterId()).thenReturn(Entity.NONE);
    }

    private static ArgumentMatcher<Collection<Entity>> containing(Entity entity) {
        return entities -> (entities != null) && entities.contains(entity);
    }

    private static ArgumentMatcher<Vector<Unit>> vectorContaining(Unit unit) {
        return units -> (units != null) && units.contains(unit);
    }

    private void verifyConnected(Unit node, Unit master) {
        Entity nodeEntity = node.getEntity();
        int masterEntityId = entityIds.get(master);
        String masterUuid = c3Uuid(master);
        c3Util.verify(() -> C3Util.connect(eq(game), argThat(containing(nodeEntity)), eq(masterEntityId), eq(false)));
        verify(nodeEntity).setC3MasterIsUUIDAsString(masterUuid);
    }

    private void verifyNoConnection() {
        c3Util.verify(() -> C3Util.connect(any(), any(), anyInt(), anyBoolean()), never());
    }

    private MothballC3State roundTrip(MothballC3State state) throws Exception {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        state.writeToXML(printWriter, 0);
        printWriter.flush();

        Document document = MHQXMLUtility.newSafeDocumentBuilder()
                                  .parse(new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8)));
        return MothballC3State.generateInstanceFromXML(document.getDocumentElement());
    }

    // endregion Helpers
}
