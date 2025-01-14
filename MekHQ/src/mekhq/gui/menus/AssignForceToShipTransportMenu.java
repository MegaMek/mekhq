/**
 * Copyright (c) 2025-2025 The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.menus;

import mekhq.campaign.unit.enums.TransporterType;
import mekhq.campaign.utilities.CampaignTransportUtilities;
import mekhq.utilities.MHQInternationalization;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.unit.Unit;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import static mekhq.campaign.enums.CampaignTransportType.SHIP_TRANSPORT;

/**
 * Menu for assigning a unit to a specific Ship Transport
 * @see CampaignTransportType#SHIP_TRANSPORT
 * @see mekhq.campaign.unit.ShipTransportedUnitsSummary
 * @see mekhq.campaign.unit.TransportShipAssignment
 */
public class AssignForceToShipTransportMenu extends AssignForceToTransportMenu {

    /**
     * Constructor for a new ship transport Menu
     * @param campaign current campaign
     * @param units selected units to try and assign
     * @see CampaignTransportType#SHIP_TRANSPORT
     */
    public AssignForceToShipTransportMenu(Campaign campaign, Unit... units) {
        super(SHIP_TRANSPORT, campaign, units);
    }

    /**
     * Returns a Set of Transporters that the provided units could all be loaded into
     * for Ship Transport.
     * @param units filter the transporter list based on what these units could use
     * @return Transporters suitable for long-term or space travel.
     */
    @Override
    protected Set<TransporterType> filterTransporterTypeMenus(final Unit... units) {
        Set<TransporterType> transporterTypes = new HashSet<>(campaign.getTransports(campaignTransportType).keySet());

        for (Unit unit : units) {
            Set<TransporterType> unitTransporterTypes = CampaignTransportUtilities.mapEntityToTransporters(SHIP_TRANSPORT, unit.getEntity());
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
     * Assigns the units to the Ship Transport.
     * @param evt             ActionEvent from the selection happening
     * @param transporterType transporter type selected in an earlier menu
     * @param transport       transport (Unit) that will load these units
     * @param units           units being assigned to the transport
     */
    @Override
    protected void transportMenuAction(ActionEvent evt, TransporterType transporterType, Unit transport, Unit... units) {
        for (Unit unit : units) {
            if (!transport.getEntity().canLoad(unit.getEntity(), false)) {
                JOptionPane.showMessageDialog(null,MHQInternationalization.getFormattedTextAt(
                    "mekhq.resources.AssignForceToTransport", "AssignForceToTransportMenu.warningCouldNotLoadUnit.text",
                    unit.getName(), transport.getName()), "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

        }
        Set<Unit> oldTransports = transport.loadShipTransport(transporterType, units);
        if (!oldTransports.isEmpty()) {
            oldTransports.forEach(oldTransport -> campaign.updateTransportInTransports(campaignTransportType, oldTransport));
            oldTransports.forEach(oldTransport -> MekHQ.triggerEvent(new UnitChangedEvent(transport)));
        }
        for (Unit unit : units) {
            MekHQ.triggerEvent(new UnitChangedEvent(unit));
        }
        campaign.updateTransportInTransports(campaignTransportType, transport);
        MekHQ.triggerEvent(new UnitChangedEvent(transport));
    }

}
