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

package mekhq.gui.menus;

import static mekhq.campaign.enums.CampaignTransportType.TOW_TRANSPORT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import megamek.client.ui.dialogs.lobby.TrainOrderDialog;
import megamek.common.equipment.TankTrailerHitch;
import megamek.common.units.Entity;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.gui.utilities.JMenuHelpers;
import mekhq.utilities.MHQInternationalization;

/**
 * Builds the transport assignment section of a unit right-click menu: for each campaign transport
 * type (ship, tactical, tow) an assign submenu plus the matching unassign items. Shared by the
 * TO&amp;E and Hangar tab popup menus so both tabs offer the same transport actions.
 *
 * @see CampaignTransportType
 * @see AssignForceToTransportMenu
 */
public final class TransportAssignmentMenus {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.AssignForceToTransport";

    private TransportAssignmentMenus() {
    }

    /**
     * Adds the assign-to-transport submenus and unassign menu items for every campaign transport
     * type to the given popup, plus the Connect Train item for a multi-selected tractor and its
     * trailers. Menus that do not apply to the selection are omitted.
     *
     * @param frame    parent frame for dialogs opened by menu actions
     * @param popup    popup menu being built
     * @param campaign current campaign
     * @param units    selected units
     */
    public static void addTransportMenus(JFrame frame, JPopupMenu popup, Campaign campaign, List<Unit> units) {
        Set<Unit> unitSet = new HashSet<>(units);
        for (CampaignTransportType campaignTransportType : CampaignTransportType.values()) {
            JMenu assignMenu = switch (campaignTransportType) {
                case SHIP_TRANSPORT -> new AssignForceToShipTransportMenu(campaign, unitSet);
                case TACTICAL_TRANSPORT -> new AssignForceToTacticalTransportMenu(campaign, unitSet);
                case TOW_TRANSPORT -> new AssignForceToTowTransportMenu(campaign, unitSet);
            };
            JMenuHelpers.addMenuIfNonEmpty(popup, assignMenu);
            addUnassignSelfMenuItem(popup, campaign, units, campaignTransportType);
            addUnassignTransportedMenuItem(popup, campaign, units, campaignTransportType);
        }
        addConnectTrainMenuItem(frame, popup, campaign, units);
        addDisconnectTrainMenuItem(popup, campaign, units);
    }

    /**
     * Menu item shown when every selected unit is part of a tow train: dissolves the whole train
     * each selected unit belongs to, from lead tractor to last trailer, like the lobby's
     * Disconnect Train. The single-link unassign items above only break one hitch; this one frees
     * everything.
     */
    private static void addDisconnectTrainMenuItem(JPopupMenu popup, Campaign campaign, List<Unit> units) {
        if (anyDeployed(units) ||
                  !units.stream().allMatch(unit -> unit.hasTransportAssignment(TOW_TRANSPORT) ||
                                                         unit.hasTransportedUnits(TOW_TRANSPORT))) {
            return;
        }
        JMenuItem menuItem = new JMenuItem(MHQInternationalization.getTextAt(RESOURCE_BUNDLE,
              "TransportAssignmentMenus.disconnectTrain.text"));
        menuItem.addActionListener(evt -> {
            for (Unit unit : units) {
                disconnectTrain(campaign, unit);
            }
        });
        popup.add(menuItem);
    }

    /**
     * Dissolves the whole tow train the given unit belongs to: walks forward to the lead tractor,
     * then releases every trailer behind it. Safe to call on any member of the train - tractor,
     * middle trailer, or tail - and on a unit that is not in a train at all (does nothing).
     *
     * @param campaign  current campaign
     * @param anyMember any unit of the train to dissolve
     */
    public static void disconnectTrain(Campaign campaign, Unit anyMember) {
        // Walk forward to the tractor heading the train
        Unit head = anyMember;
        Set<Unit> seenGoingForward = new HashSet<>();
        seenGoingForward.add(head);
        while (head.hasTransportAssignment(TOW_TRANSPORT)) {
            Unit unitInFront = head.getTransportAssignment(TOW_TRANSPORT).getTransport();
            if ((unitInFront == null) || !seenGoingForward.add(unitInFront)) {
                break;
            }
            head = unitInFront;
        }

        // Collect every towed unit behind the head; each unit only lists the one directly behind it
        List<Unit> towedMembers = new ArrayList<>();
        Set<Unit> seenGoingBack = new HashSet<>();
        seenGoingBack.add(head);
        Unit current = head;
        while (current.hasTransportedUnits(TOW_TRANSPORT)) {
            Unit unitBehind = current.getTransportedUnits(TOW_TRANSPORT).iterator().next();
            if ((unitBehind == null) || !seenGoingBack.add(unitBehind)) {
                break;
            }
            towedMembers.add(unitBehind);
            current = unitBehind;
        }

        if (!towedMembers.isEmpty()) {
            unassignFromTransport(campaign, TOW_TRANSPORT, towedMembers);
        }
    }

    /**
     * Menu item shown when the multi-selection parses the same way the MegaMek lobby's Connect
     * Train does: exactly one free tractor plus one or more free trailers, nothing else. Opens the
     * lobby's {@link TrainOrderDialog} to pick the hitch order, then connects the whole train.
     */
    private static void addConnectTrainMenuItem(JFrame frame, JPopupMenu popup, Campaign campaign, List<Unit> units) {
        if ((units.size() < 2) || anyDeployed(units)) {
            return;
        }

        Unit head = null;
        List<Unit> trailers = new ArrayList<>();
        for (Unit unit : units) {
            Entity entity = unit.getEntity();
            if ((entity == null) || !unit.isAvailable() || unit.hasTransportAssignment(TOW_TRANSPORT)) {
                return;
            }
            if (entity.isTrailer()) {
                trailers.add(unit);
            } else if (entity.isTractor()) {
                if ((head != null) || unit.hasTransportedUnits(TOW_TRANSPORT)) {
                    // The train is built from one unattached tractor; two heads or a tractor
                    // already towing means the selection is not a connectable train.
                    return;
                }
                head = unit;
            } else {
                // Selection contains a unit that is neither a tractor nor a trailer
                return;
            }
        }
        if ((head == null) || trailers.isEmpty()) {
            return;
        }

        Unit trainHead = head;
        JMenuItem menuItem = new JMenuItem(MHQInternationalization.getTextAt(RESOURCE_BUNDLE,
              "TransportAssignmentMenus.connectTrain.text"));
        menuItem.addActionListener(evt -> connectTrainAction(frame, campaign, trainHead, trailers));
        popup.add(menuItem);
    }

    private static void connectTrainAction(JFrame frame, Campaign campaign, Unit head, List<Unit> trailers) {
        List<Entity> trailerEntities = new ArrayList<>();
        for (Unit trailer : trailers) {
            trailerEntities.add(trailer.getEntity());
        }

        TrainOrderDialog dialog = new TrainOrderDialog(frame, head.getEntity(), trailerEntities);
        dialog.setVisible(true);
        if (!dialog.wasConfirmed()) {
            return;
        }

        List<Unit> orderedTrailers = new ArrayList<>();
        for (Entity orderedEntity : dialog.getOrderedTrailers()) {
            for (Unit trailer : trailers) {
                if (trailer.getEntity() == orderedEntity) {
                    orderedTrailers.add(trailer);
                    break;
                }
            }
        }

        if (!connectTrain(campaign, head, orderedTrailers)) {
            JOptionPane.showMessageDialog(frame,
                  MHQInternationalization.getTextAt(RESOURCE_BUNDLE,
                        "TransportAssignmentMenus.connectTrainFailed.text"),
                  MHQInternationalization.getTextAt(RESOURCE_BUNDLE,
                        "TransportAssignmentMenus.connectTrainFailed.title"),
                  JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Connects a tractor and its trailers into one campaign-side train, in the given order. The
     * whole train is checked before anything is hitched - all or nothing, like the server-side
     * train build at scenario launch: the first link must pass {@link Entity#canTow(int)}, every
     * later attach point must carry a trailer hitch, and the combined trailer weight must not
     * exceed the tractor's weight (TW: a tractor tows up to its own weight).
     *
     * @param campaign        current campaign
     * @param head            free tractor heading the train
     * @param orderedTrailers trailers in hitch order, front to back
     *
     * @return true when the train was connected, false when validation failed and nothing changed
     */
    public static boolean connectTrain(Campaign campaign, Unit head, List<Unit> orderedTrailers) {
        Entity headEntity = head.getEntity();
        if ((headEntity == null) || orderedTrailers.isEmpty()) {
            return false;
        }

        if (!headEntity.canTow(orderedTrailers.getFirst().getEntity().getId())) {
            return false;
        }

        double totalTrailerWeight = 0;
        for (int trailerIndex = 0; trailerIndex < orderedTrailers.size(); trailerIndex++) {
            Entity trailerEntity = orderedTrailers.get(trailerIndex).getEntity();
            if ((trailerEntity == null) || !trailerEntity.isTrailer()) {
                return false;
            }
            totalTrailerWeight += trailerEntity.getWeight();

            // Every trailer except the last one is an attach point and needs a hitch of its own
            boolean isLastTrailer = trailerIndex == (orderedTrailers.size() - 1);
            if (!isLastTrailer &&
                      trailerEntity.getTransports()
                            .stream()
                            .noneMatch(transporter -> transporter instanceof TankTrailerHitch)) {
                return false;
            }
        }
        if (totalTrailerWeight > headEntity.getWeight()) {
            return false;
        }

        Unit attachPoint = head;
        for (Unit trailer : orderedTrailers) {
            attachPoint.towTrailer(trailer, null, TransporterType.TANK_TRAILER_HITCH);
            attachPoint = trailer;
        }

        campaign.updateTransportInTransports(TOW_TRANSPORT, head);
        MekHQ.triggerEvent(new UnitChangedEvent(head));
        for (Unit trailer : orderedTrailers) {
            MekHQ.triggerEvent(new UnitChangedEvent(trailer));
        }
        return true;
    }

    /**
     * Menu item shown when every selected unit is itself assigned to a transport of this type:
     * unassigns the selected units from their transports.
     */
    private static void addUnassignSelfMenuItem(JPopupMenu popup, Campaign campaign, List<Unit> units,
          CampaignTransportType campaignTransportType) {
        if (anyDeployed(units) ||
                  !units.stream().allMatch(unit -> unit.hasTransportAssignment(campaignTransportType))) {
            return;
        }
        JMenuItem menuItem = new JMenuItem(MHQInternationalization.getTextAt(RESOURCE_BUNDLE,
              "TOEMouseAdapter.unassign." + campaignTransportType.name() + ".text"));
        menuItem.addActionListener(evt -> unassignFromTransport(campaign, campaignTransportType, units));
        popup.add(menuItem);
    }

    /**
     * Menu item shown when every selected unit is carrying or towing units of this type: releases
     * the selected transports' carried or towed units.
     */
    private static void addUnassignTransportedMenuItem(JPopupMenu popup, Campaign campaign, List<Unit> units,
          CampaignTransportType campaignTransportType) {
        if (anyDeployed(units) ||
                  !units.stream().allMatch(unit -> unit.hasTransportedUnits(campaignTransportType))) {
            return;
        }
        JMenuItem menuItem = new JMenuItem(MHQInternationalization.getTextAt(RESOURCE_BUNDLE,
              "TOEMouseAdapter.unassignFrom." + campaignTransportType.name() + ".text"));
        menuItem.addActionListener(evt -> unassignTransportedUnits(campaign, campaignTransportType, units));
        popup.add(menuItem);
    }

    private static boolean anyDeployed(List<Unit> units) {
        for (Unit unit : units) {
            if (unit.isDeployed()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unassigns the given units from their transports of the given type, then refreshes the
     * transports they were unloaded from.
     *
     * @param campaign              current campaign
     * @param campaignTransportType transport type being unassigned
     * @param units                 transported units to unassign
     */
    public static void unassignFromTransport(Campaign campaign, CampaignTransportType campaignTransportType,
          Collection<Unit> units) {
        Set<Unit> transportsToUpdate = new HashSet<>();
        for (Unit transportedUnit : units) {
            Unit oldTransport = transportedUnit.unloadFromTransport(campaignTransportType);
            if (oldTransport != null) {
                transportsToUpdate.add(oldTransport);
            }
            MekHQ.triggerEvent(new UnitChangedEvent(transportedUnit));
        }

        for (Unit transportToUpdate : transportsToUpdate) {
            transportToUpdate.initializeTransportSpace(campaignTransportType);
            campaign.updateTransportInTransports(campaignTransportType, transportToUpdate);
            MekHQ.triggerEvent(new UnitChangedEvent(transportToUpdate));
        }
    }

    /**
     * Releases every unit carried or towed by the given transports for the given type.
     *
     * @param campaign              current campaign
     * @param campaignTransportType transport type being unassigned
     * @param transports            transports whose carried or towed units are released
     */
    public static void unassignTransportedUnits(Campaign campaign, CampaignTransportType campaignTransportType,
          Collection<Unit> transports) {
        for (Unit transport : transports) {
            if (transport.hasTransportedUnits(campaignTransportType)) {
                unassignFromTransport(campaign, campaignTransportType,
                      new ArrayList<>(transport.getTransportedUnits(campaignTransportType)));
            }
        }
    }
}
