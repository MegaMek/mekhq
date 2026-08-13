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

import jakarta.annotation.Nullable;
import megamek.client.ui.dialogs.lobby.TrainOrderDialog;
import megamek.client.ui.util.MenuScroller;
import megamek.common.equipment.TankTrailerHitch;
import megamek.common.equipment.Transporter;
import megamek.common.units.Entity;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.force.Formation;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.TransporterType;
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
            addMenuIfNonEmpty(popup, assignMenu);
            addUnassignSelfMenuItem(popup, campaign, units, campaignTransportType);
            addUnassignTransportedMenuItem(popup, campaign, units, campaignTransportType);
        }
        addConnectTrainMenuItem(frame, popup, campaign, units);
        addDisconnectTrainMenuItem(popup, campaign, units);
    }

    /**
     * Adds the child menu to the popup only when it has entries, with a scroller once it grows
     * past the standard threshold. Replaces the deprecated
     * {@code JMenuHelpers.addMenuIfNonEmpty}; the popups handed in by the TO&amp;E and Hangar
     * adapters are plain {@link JPopupMenu}s, so the {@code JScrollablePopupMenu} replacement
     * does not apply here.
     */
    private static void addMenuIfNonEmpty(JPopupMenu popup, JMenu childMenu) {
        if (childMenu.getItemCount() > 0) {
            popup.add(childMenu);
            if (childMenu.getItemCount() > MHQConstants.BASE_SCROLLER_THRESHOLD) {
                MenuScroller.setScrollerFor(childMenu, MHQConstants.BASE_SCROLLER_THRESHOLD);
            }
        }
    }

    /**
     * Menu item shown when every selected unit is part of a tow train: dissolves the whole train
     * each selected unit belongs to, from lead tractor to last trailer, like the lobby's
     * Disconnect Train. The single-link unassign items above only break one hitch; this one frees
     * everything.
     */
    private static void addDisconnectTrainMenuItem(JPopupMenu popup, Campaign campaign, List<Unit> units) {
        if (anyDeployed(units)) {
            return;
        }
        for (Unit unit : units) {
            if (!unit.hasTransportAssignment(TOW_TRANSPORT) && !unit.hasTransportedUnits(TOW_TRANSPORT)) {
                return;
            }
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
        List<Unit> members = trainMembers(anyMember);
        List<Unit> towedMembers = new ArrayList<>(members.subList(1, members.size()));

        if (!towedMembers.isEmpty()) {
            unassignFromTransport(campaign, TOW_TRANSPORT, towedMembers);
        }
    }

    /**
     * Lists the whole tow train the given unit belongs to, lead tractor first, then each trailer in
     * hitch order. A unit that is in no train is returned on its own.
     *
     * <p>A train is a singly-linked list - each unit records only the unit directly behind it - so
     * listing it from a member in the middle means finding the lead tractor first and then reading
     * back down from there. Callers that only need what is behind a unit should use
     * {@link #trailersBehind(Unit)}, which skips the walk to the head.</p>
     *
     * @param anyMember any unit of the train - tractor, middle trailer, or tail
     *
     * @return train members in order, lead tractor first; never empty
     */
    public static List<Unit> trainMembers(Unit anyMember) {
        Unit head = trainHead(anyMember);
        List<Unit> members = new ArrayList<>();
        members.add(head);
        members.addAll(trailersBehind(head));
        return members;
    }

    /**
     * Walks forward from any train member to the tractor heading it. Stops if a unit repeats, so a
     * campaign that somehow ended up with a looped hitch cannot spin here.
     *
     * @param anyMember any unit of the train
     *
     * @return the lead tractor, or the unit itself when it is hitched to nothing
     */
    private static Unit trainHead(Unit anyMember) {
        Unit head = anyMember;
        Set<Unit> unitsWalked = new HashSet<>();
        unitsWalked.add(head);
        while (head.hasTransportAssignment(TOW_TRANSPORT)) {
            Unit unitInFront = head.getTransportAssignment(TOW_TRANSPORT).getTransport();
            if ((unitInFront == null) || !unitsWalked.add(unitInFront)) {
                break;
            }
            head = unitInFront;
        }
        return head;
    }

    /**
     * True when the unit is already part of the train the given transport belongs to. Hitching a
     * unit into its own train leaves it towing itself, and every walk along that train then runs
     * forever, so these pairings are never offered and never applied.
     *
     * @param transport tractor the unit would be hitched into the train of
     * @param unit      unit being hitched
     *
     * @return true when the two are already in the same train
     */
    public static boolean isInSameTrain(Unit transport, Unit unit) {
        return trainMembers(transport).contains(unit);
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

        List<Unit> trailers = new ArrayList<>();
        Unit head = parseConnectableTrain(units, trailers);
        if (head == null) {
            return;
        }

        JMenuItem menuItem = new JMenuItem(MHQInternationalization.getTextAt(RESOURCE_BUNDLE,
              "TransportAssignmentMenus.connectTrain.text"));
        menuItem.addActionListener(evt -> connectTrainAction(frame, campaign, head, trailers));
        popup.add(menuItem);
    }

    /**
     * Reads a multi-selection the way the lobby's Connect Train does: exactly one free tractor plus
     * one or more free trailers, nothing else. Anything else in the selection makes it unusable.
     *
     * @param units    selected units
     * @param trailers filled with the free trailers found, in selection order
     *
     * @return the one free tractor to head the train, or null when the selection is not one
     */
    private static @Nullable Unit parseConnectableTrain(List<Unit> units, List<Unit> trailers) {
        Unit head = null;
        for (Unit unit : units) {
            Entity entity = unit.getEntity();
            if ((entity == null) || !unit.isAvailable() || unit.hasTransportAssignment(TOW_TRANSPORT)) {
                return null;
            }
            if (entity.isTrailer()) {
                trailers.add(unit);
            } else if (entity.isTractor()) {
                if ((head != null) || unit.hasTransportedUnits(TOW_TRANSPORT)) {
                    // The train is built from one unattached tractor; two heads or a tractor
                    // already towing means the selection is not a connectable train.
                    return null;
                }
                head = unit;
            } else {
                // Selection contains a unit that is neither a tractor nor a trailer
                return null;
            }
        }
        return trailers.isEmpty() ? null : head;
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
            if (!isLastTrailer && !hasTrailerHitch(trailerEntity)) {
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
        moveTrainIntoTractorFormation(campaign, head);
        for (Unit trailer : orderedTrailers) {
            MekHQ.triggerEvent(new UnitChangedEvent(trailer));
        }
        return true;
    }

    /**
     * True when the entity carries a trailer hitch, so another trailer can attach behind it.
     *
     * @param entity entity being checked as an attach point
     *
     * @return true when it has at least one hitch
     */
    private static boolean hasTrailerHitch(Entity entity) {
        for (Transporter transporter : entity.getTransports()) {
            if (transporter instanceof TankTrailerHitch) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves every trailer of the tractor's train into the tractor's TO&amp;E formation so the whole
     * train sits together on the org chart. A trailer that was in another formation is moved out of
     * it. Nothing happens when the tractor is in no formation, since there is nothing to move the
     * trailers into.
     *
     * @param campaign current campaign
     * @param tractor  unit the trailers were hitched onto
     */
    static void moveTrainIntoTractorFormation(Campaign campaign, Unit tractor) {
        int formationId = tractor.getFormationId();
        if (formationId == Formation.FORMATION_NONE) {
            return;
        }

        for (Unit member : trainMembers(tractor)) {
            if (member.getFormationId() != formationId) {
                campaign.getPlayerForce().addUnitToFormation(member, formationId, campaign);
            }
        }
    }

    /**
     * Menu item shown when every selected unit is itself assigned to a transport of this type:
     * unassigns the selected units from their transports.
     */
    private static void addUnassignSelfMenuItem(JPopupMenu popup, Campaign campaign, List<Unit> units,
          CampaignTransportType campaignTransportType) {
        if (anyDeployed(units)) {
            return;
        }
        for (Unit unit : units) {
            if (!unit.hasTransportAssignment(campaignTransportType)) {
                return;
            }
        }
        String menuTextKey = "TOEMouseAdapter.unassign." + campaignTransportType.name() + ".text";
        JMenuItem menuItem = new JMenuItem(MHQInternationalization.getTextAt(RESOURCE_BUNDLE, menuTextKey));
        menuItem.addActionListener(evt -> unassignFromTransport(campaign, campaignTransportType, units));
        popup.add(menuItem);
    }

    /**
     * Menu item shown when every selected unit is carrying or towing units of this type: releases
     * the selected transports' carried or towed units.
     */
    private static void addUnassignTransportedMenuItem(JPopupMenu popup, Campaign campaign, List<Unit> units,
          CampaignTransportType campaignTransportType) {
        if (anyDeployed(units)) {
            return;
        }
        for (Unit unit : units) {
            if (!unit.hasTransportedUnits(campaignTransportType)) {
                return;
            }
        }
        String menuTextKey = "TOEMouseAdapter.unassignFrom." + campaignTransportType.name() + ".text";
        JMenuItem menuItem = new JMenuItem(MHQInternationalization.getTextAt(RESOURCE_BUNDLE, menuTextKey));
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
        Set<Unit> changedUnits = new HashSet<>();
        for (Unit transportedUnit : units) {
            Unit oldTransport = transportedUnit.unloadFromTransport(campaignTransportType);
            if (oldTransport != null) {
                transportsToUpdate.add(oldTransport);
            }
            changedUnits.add(transportedUnit);
        }

        for (Unit transportToUpdate : transportsToUpdate) {
            transportToUpdate.initializeTransportSpace(campaignTransportType);
            campaign.updateTransportInTransports(campaignTransportType, transportToUpdate);
            changedUnits.add(transportToUpdate);
        }

        // Both sides of every broken hitch really did change, but releasing a whole train puts the
        // same middle trailer in both sets - it is a released unit and the next one's old transport.
        // Collecting first means each unit refreshes once instead of twice.
        for (Unit changedUnit : changedUnits) {
            MekHQ.triggerEvent(new UnitChangedEvent(changedUnit));
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
            if (!transport.hasTransportedUnits(campaignTransportType)) {
                continue;
            }
            // A tow train is a linked list, so a tractor only lists the trailer directly behind it.
            // Releasing that one alone would leave the rest of the train hitched to it and floating
            // loose, so the whole run of trailers behind the unit comes off together.
            Collection<Unit> unitsToRelease = campaignTransportType.isTowTransport() ?
                                                    trailersBehind(transport) :
                                                    new ArrayList<>(transport.getTransportedUnits(
                                                          campaignTransportType));
            unassignFromTransport(campaign, campaignTransportType, unitsToRelease);
        }
    }

    /**
     * Lists every trailer behind the given unit in its tow train, front to back. A tractor returns
     * its whole train; a middle trailer returns only what it is pulling, leaving its own hitch to
     * the unit in front alone.
     *
     * @param unit any member of a tow train
     *
     * @return the trailers behind it in hitch order, empty when it is pulling nothing
     */
    public static List<Unit> trailersBehind(Unit unit) {
        List<Unit> trailers = new ArrayList<>();
        Set<Unit> unitsWalked = new HashSet<>();
        unitsWalked.add(unit);

        Unit current = unit;
        while (current.hasTransportedUnits(TOW_TRANSPORT)) {
            Unit unitBehind = current.getTransportedUnits(TOW_TRANSPORT).iterator().next();
            if ((unitBehind == null) || !unitsWalked.add(unitBehind)) {
                // A looped hitch would keep this walk going forever
                break;
            }
            trailers.add(unitBehind);
            current = unitBehind;
        }

        return trailers;
    }
}
