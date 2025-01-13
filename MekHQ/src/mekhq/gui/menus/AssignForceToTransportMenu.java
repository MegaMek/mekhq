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

import megamek.common.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.utilities.MHQInternationalization;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Generic menu for displaying transports for the units in the force selected (or an individual unit).
 *
 * @see CampaignTransportType
 * @see mekhq.campaign.unit.AbstractTransportedUnitsSummary
 * @see mekhq.campaign.unit.ITransportAssignment
 */
public abstract class AssignForceToTransportMenu extends JScrollableMenu {

    final Campaign campaign;
    final CampaignTransportType campaignTransportType;

    // region Constructors

    /**
     * Constructor for a new Transport Menu
     * @param campaignTransportType type (Enum) of transport type for this menu
     * @param campaign current campaign
     * @param units selected units to try and assign
     * @see CampaignTransportType
     */
    public AssignForceToTransportMenu(CampaignTransportType campaignTransportType, final Campaign campaign, final Unit... units) {
        super(campaignTransportType.getName());
        this.campaign = campaign;
        this.campaignTransportType = campaignTransportType;
        initialize(units);
    }
    // endregion Constructors

    private void initialize(final Unit... units) {
        /*
         * Immediate Return for Illegal Assignments:
         * 1) No units to assign
         * 2) Any units are currently unavailable
         * 3) No transports available
         */
        if ((units.length == 0) || (Stream.of(units).anyMatch(unit -> !unit.isAvailable()))
            || (!campaign.hasTransports(campaignTransportType))) {
            return;
        }

        Set<JScrollableMenu> transporterTypeMenus = createTransporterTypeMenus(units);
        if(transporterTypeMenus.isEmpty()) {
            return;
        }

        //Assign Unit to {campaignTransportTypeName}
        setText(MHQInternationalization.getTextAt("mekhq.resources.AssignForceToTransport", "AssignForceToTransportMenu." + campaignTransportType.getName() + ".text"));
        for (JScrollableMenu transporterTypeMenu : transporterTypeMenus) {
            add(transporterTypeMenu);
        }
    }


    /**
     * Create the menus for selecting a transporter type
     * to try and load these units into
     * @param units units being assigned a transport
     * @return menu of transporter types
     */
    protected Set<JScrollableMenu> createTransporterTypeMenus(final Unit... units) {
        Set<JScrollableMenu> transporterTypeMenus = new HashSet<>();
        /* Let's get the transport types our campaign has
         and remove the ones that can't be used by these units.
         While we're at it, let's get the total capacity we'll
         need to transport all these units. If they use different
         calculation methods (an infantry and tank were both selected)
         then they shouldn't have any compatible transport types
         */
        for (Class<? extends Transporter> transporterType : filterTransporterTypeMenus(units)) {
            double requiredTransportCapacity = 0.0;
            for (Unit unit : units) {
                requiredTransportCapacity += CampaignTransportType.transportCapacityUsage(transporterType, unit.getEntity());
            }

            Set<Unit> transports = campaign.getTransportsByType(campaignTransportType, transporterType, requiredTransportCapacity);

            if (!transports.isEmpty()) {
                JScrollableMenu transporterTypeMenu = new JScrollableMenu(transporterType.getName(), transporterType.getName());
                Set<JMenuItem> transportMenus = createTransportMenus(transporterType, transports, units);
                for (JMenuItem transportMenu : transportMenus) {
                    transporterTypeMenu.add(transportMenu);
                }

                // {name of the bay}
                transporterTypeMenu.setText(MHQInternationalization.getTextAt("mekhq.resources.AssignForceToTransport",
                    "AssignForceToTransportMenu." + transporterType.getSimpleName() + ".text"));

                transporterTypeMenus.add(transporterTypeMenu);
            }
        }

        return transporterTypeMenus;
    }

    private Set<JMenuItem> createTransportMenus(Class<? extends Transporter> transporterType, Set<Unit> transports, Unit... units) {
        Set<JMenuItem> transportMenus = new HashSet<>();
        for (Unit transport : transports) {
            JMenuItem transportMenu = new JMenuItem(transport.getId().toString());

            // {Transport Name} | Space Remaining: {Current Transport Capacity}
            transportMenu.setText(MHQInternationalization.getFormattedTextAt("mekhq.resources.AssignForceToTransport",
                "AssignForceToTransportMenu.transportSpaceRemaining.text",
                transport.getName(), transport.getCurrentTransportCapacity(campaignTransportType, transporterType)));

            transportMenu.addActionListener(evt -> transportMenuAction(evt, transporterType, transport, units));
            transportMenus.add(transportMenu);
        }
        return transportMenus;
    }

    /**
     * Different transporter type menus return different transporters
     * @param units filter the transporter list based on what these units could use
     * @return transporters that can be used by all these units
     * @see CampaignTransportType
     */
    protected abstract Set<Class<? extends Transporter>> filterTransporterTypeMenus(final Unit... units);

    /**
     * Different transporter type menus do different things when selected
     * @param evt ActionEvent from the selection happening
     * @param transporterType transporter type selected in an earlier menu
     * @param transport transport (Unit) that will load these units
     * @param units units being assigned to the transport
     */
    protected abstract void transportMenuAction(ActionEvent evt, Class<? extends Transporter> transporterType, Unit transport, Unit... units);

}
