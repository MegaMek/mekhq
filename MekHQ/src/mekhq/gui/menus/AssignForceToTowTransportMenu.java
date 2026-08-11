/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.campaign.utilities.CampaignTransportUtilities;
import mekhq.utilities.MHQInternationalization;

/**
 * Menu for assigning a force to a specific Tow transport
 *
 * @see CampaignTransportType#TOW_TRANSPORT
 * @see mekhq.campaign.unit.TowTransportedUnitsSummary
 * @see mekhq.campaign.unit.TransportAssignment
 */
public class AssignForceToTowTransportMenu extends AssignForceToTransportMenu {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.AssignForceToTransport";

    /**
     * Constructor for a new tow transport Menu
     *
     * @param campaign current campaign
     * @param units    selected units to try and assign
     *
     * @see CampaignTransportType#TOW_TRANSPORT
     */
    public AssignForceToTowTransportMenu(Campaign campaign, Set<Unit> units) {
        super(TOW_TRANSPORT, campaign, units);
    }

    /**
     * Returns a Set of Transporters that the provided units could all be loaded into for Tow Transport.
     *
     * @param units filter the Transporter list based on what these units could use
     *
     * @return most Transporter types except cargo and hitches
     */
    @Override
    protected Set<TransporterType> filterTransporterTypeMenus(final Set<Unit> units) {
        Set<TransporterType> transporterTypes = new HashSet<>(campaign.getTransports(TOW_TRANSPORT).keySet());

        for (Unit unit : units) {
            Set<TransporterType> unitTransporterTypes = CampaignTransportUtilities.mapICarryableToTransporters(TOW_TRANSPORT,
                  unit.getEntity());
            if (!unitTransporterTypes.isEmpty()) {
                transporterTypes.retainAll(unitTransporterTypes);
            } else {
                return new HashSet<>();
            }
        }
        if (transporterTypes.isEmpty()) {
            return new HashSet<>();
        }

        return transporterTypes;
    }

    /**
     * Drops tractors whose train a selected unit is already part of. A trailer cannot be hitched
     * into its own train: the hitch goes onto the last unit of the train, which would be the
     * trailer itself, leaving it towing itself. This is also why the menu disappears for a trailer
     * once it is hitched to the only tractor that could pull it.
     *
     * @param transports tractors the selection would otherwise be offered
     * @param units      units being assigned a tractor
     *
     * @return tractors that can still take these units
     */
    @Override
    protected Set<Unit> filterTransports(final Set<Unit> transports, final Set<Unit> units) {
        Set<Unit> availableTransports = new HashSet<>();
        for (Unit transport : transports) {
            boolean alreadyInTrain = false;
            for (Unit unit : units) {
                if (TransportAssignmentMenus.isInSameTrain(transport, unit)) {
                    alreadyInTrain = true;
                    break;
                }
            }
            if (!alreadyInTrain) {
                availableTransports.add(transport);
            }
        }

        return availableTransports;
    }

    /**
     * Adds the tractor's current train to the tooltip, so the hitch point is visible before the
     * click: a trailer goes on the back of whatever this tractor is already pulling, not onto the
     * tractor itself.
     *
     * @param transport tractor being offered
     *
     * @return tooltip naming the tractor's formation and the train it heads
     */
    @Override
    protected @Nullable String transportMenuTooltip(Unit transport) {
        String formationPath = formationFullName(transport);
        if (formationPath == null) {
            return null;
        }

        List<Unit> trailers = TransportAssignmentMenus.trailersBehind(transport);
        if (trailers.isEmpty()) {
            return MHQInternationalization.getFormattedTextAt(RESOURCE_BUNDLE,
                  "AssignForceToTransportMenu.towTooltipEmpty.text", formationPath);
        }

        StringBuilder trainDescription = new StringBuilder(transport.getName());
        for (Unit trailer : trailers) {
            trainDescription.append(" -> ").append(trailer.getName());
        }
        return MHQInternationalization.getFormattedTextAt(RESOURCE_BUNDLE,
              "AssignForceToTransportMenu.towTooltipTrain.text", formationPath,
              trailers.size(), trainDescription.toString());
    }

    /**
     * Assign a unit to a Tow Transport.
     *
     * @param evt             ActionEvent from the selection happening
     * @param transporterType transporter type selected in an earlier menu
     * @param transport       transport (Unit) that will load these units
     * @param units           units being assigned to the transport
     */
    @Override
    protected void transportMenuAction(ActionEvent evt, TransporterType transporterType, Unit transport,
          Set<Unit> units) {
        for (Unit unit : units) {
            // A unit already in this train would be hitched to itself, which loops every later
            // walk along the train. The menu filters these out; a menu built before an earlier
            // click in the same selection can still get here.
            if (TransportAssignmentMenus.isInSameTrain(transport, unit)) {
                JOptionPane.showMessageDialog(null, MHQInternationalization.getFormattedTextAt(
                      "mekhq.resources.AssignForceToTransport",
                      "AssignForceToTransportMenu.warningUnitAlreadyInTrain.text",
                      unit.getName(),
                      transport.getName()), "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!transport.getEntity().canTow(unit.getEntity().getId())) {
                JOptionPane.showMessageDialog(null, MHQInternationalization.getFormattedTextAt(
                      "mekhq.resources.AssignForceToTransport",
                      "AssignForceToTransportMenu.warningCouldNotLoadUnit.text",
                      unit.getName(),
                      transport.getName()), "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

        }
        Unit towingEnt = transport;

        for (Unit unit : units) {

            // This unit is actually going be towed by the unit at the end of the train - let's find it.
            // We shouldn't actually set towingEnt to null unless "hasTransportedUnits" is lying
            Set<Unit> walkedTrain = new HashSet<>();
            walkedTrain.add(towingEnt);
            while (towingEnt != null && towingEnt.hasTransportedUnits(TOW_TRANSPORT)) {
                towingEnt = towingEnt.getTransportedUnits(TOW_TRANSPORT).stream().findAny().orElse(null);
                if ((towingEnt != null) && !walkedTrain.add(towingEnt)) {
                    // A looped hitch would keep this walk going forever
                    break;
                }
            }

            // Intentionally letting this throw an NPE if towingEnt is null, it
            // shouldn't happen and is more clear that something's wrong than doing nothing.
            if (towingEnt != null) {
                Unit oldTransport = towingEnt.towTrailer(unit, null, transporterType);

                if (oldTransport != null) {
                    campaign.updateTransportInTransports(TOW_TRANSPORT, oldTransport);
                    MekHQ.triggerEvent(new UnitChangedEvent(oldTransport));
                }

                if (!towingEnt.equals(transport)) {
                    transport.getTransportedUnitsSummary(TOW_TRANSPORT)
                          .recalculateTransportCapacity(transport.getEntity().getTransports());
                    campaign.updateTransportInTransports(TOW_TRANSPORT, towingEnt);
                    MekHQ.triggerEvent(new UnitChangedEvent(towingEnt));
                }

                // A trailer hitched from the Hangar tab follows its tractor into the TO&E
                TransportAssignmentMenus.moveTrainIntoTractorFormation(campaign, transport);
            }
            MekHQ.triggerEvent(new UnitChangedEvent(unit));

            campaign.updateTransportInTransports(TOW_TRANSPORT, transport);
            MekHQ.triggerEvent(new UnitChangedEvent(transport));
        }
    }
}
