/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.enums.CampaignTransportType.SHIP_TRANSPORT;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JOptionPane;

import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.campaign.utilities.CampaignTransportUtilities;
import mekhq.utilities.MHQInternationalization;

/**
 * Menu for assigning a unit to a specific Ship Transport
 *
 * @see CampaignTransportType#SHIP_TRANSPORT
 * @see mekhq.campaign.unit.ShipTransportedUnitsSummary
 * @see mekhq.campaign.unit.TransportShipAssignment
 */
public class AssignForceToShipTransportMenu extends AssignForceToTransportMenu {

    /**
     * Constructor for a new ship transport Menu
     *
     * @param campaign current campaign
     * @param units    selected units to try and assign
     *
     * @see CampaignTransportType#SHIP_TRANSPORT
     */
    public AssignForceToShipTransportMenu(Campaign campaign, Set<Unit> units) {
        super(SHIP_TRANSPORT, campaign, units);
    }

    /**
     * Returns a Set of Transporters that the provided units could all be loaded into for Ship Transport.
     *
     * @param units filter the transporter list based on what these units could use
     *
     * @return Transporters suitable for long-term or space travel.
     */
    @Override
    protected Set<TransporterType> filterTransporterTypeMenus(final Set<Unit> units) {
        Set<TransporterType> transporterTypes = new HashSet<>(campaign.getTransports(campaignTransportType).keySet());

        for (Unit unit : units) {
            Set<TransporterType> unitTransporterTypes = CampaignTransportUtilities.mapEntityToTransporters(
                  SHIP_TRANSPORT,
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
     * Assigns the units to the Ship Transport.
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
            if (!transport.getEntity().canLoad(unit.getEntity(), false)) {
                JOptionPane.showMessageDialog(null, MHQInternationalization.getFormattedTextAt(
                      "mekhq.resources.AssignForceToTransport",
                      "AssignForceToTransportMenu.warningCouldNotLoadUnit.text",
                      unit.getName(),
                      transport.getName()), "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

        }
        Set<Unit> oldTransports = transport.loadShipTransport(transporterType, units);
        updateTransportsForTransportMenuAction(SHIP_TRANSPORT, transport, units, oldTransports);
    }

}
