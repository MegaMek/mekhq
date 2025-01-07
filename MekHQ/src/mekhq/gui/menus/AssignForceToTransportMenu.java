package mekhq.gui.menus;

import megamek.common.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.AbstractTransportedUnitsSummary;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public abstract class AssignForceToTransportMenu extends JScrollableMenu {

    final Campaign campaign;
    final CampaignTransportType campaignTransportType;

    // region Constructors
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

        //TODO
        //setText(resources.getString(""));
        setText(String.format("Assign Unit to %s Transport", campaignTransportType.getName()));
        for (JScrollableMenu transporterTypeMenu : transporterTypeMenus) {
            add(transporterTypeMenu);
        }
    }


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
                //TODO
                JScrollableMenu transporterTypeMenu = new JScrollableMenu(transporterType.getName(), transporterType.getName());
                Set<JMenuItem> transportMenus = createTransportMenus(transporterType, transports, units);
                for (JMenuItem transportMenu : transportMenus) {
                    transporterTypeMenu.add(transportMenu);
                }
                transporterTypeMenu.setText(transporterType.getName());
                transporterTypeMenus.add(transporterTypeMenu);
            }
        }

        return transporterTypeMenus;
    }

    private Set<JMenuItem> createTransportMenus(Class<? extends Transporter> transporterType, Set<Unit> transports, Unit... units) {
        Set<JMenuItem> transportMenus = new HashSet<>();
        for (Unit transport : transports) {
            JMenuItem transportMenu = new JMenuItem(transport.getId().toString());
            transportMenu.setText(transport.getName()
                + " | Space Remaining: " + transport.getCurrentTransportCapacity(campaignTransportType, transporterType)); //TODO
            //TODO hacky implementation
            transportMenu.addActionListener(evt -> { transportMenuAction(evt, transporterType, transport, units); });

            transportMenus.add(transportMenu);
        }
        return transportMenus;
    }

    protected abstract Set<Class<? extends Transporter>> filterTransporterTypeMenus(final Unit... units);

    protected abstract void transportMenuAction(ActionEvent evt, Class<? extends Transporter> transporterType, Unit transport, Unit... units);

}
