package mekhq.gui.menus;

import megamek.common.Transporter;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.TacticalTransportedUnitsSummary;
import mekhq.campaign.unit.Unit;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

public class AssignForceToTacticalTransportMenu extends AssignForceToTransportMenu {

    public AssignForceToTacticalTransportMenu(Campaign campaign, Unit... units) {
        super(TacticalTransportedUnitsSummary.class, campaign, units);
    }

    @Override
    protected Set<Class<? extends Transporter>> filterTransporterTypeMenus(final Unit... units) {
        Set<Class<? extends Transporter>> transporterTypes = new HashSet<>(campaign.getTransports(transportDetailType).keySet());

        for (Unit unit : units) {
            Set<Class<? extends Transporter>> unitTransporterTypes = TacticalTransportedUnitsSummary.mapEntityToTransporters(unit.getEntity());
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

    @Override
    protected void transportMenuAction(ActionEvent evt, Class<? extends Transporter> transporterType, Unit transport, Unit... units) {
        for (Unit unit : units) {
            if (!transport.getEntity().canLoad(unit.getEntity(), false)) {
                return; //TODO error popup
            }

        }
        Set<Unit> oldTransports = transport.loadTacticalTransport(transporterType, units);
        if (!oldTransports.isEmpty()) {
            oldTransports.forEach(oldTransport -> campaign.updateTransportInTransports(transportDetailType, oldTransport));
        }
        campaign.updateTransportInTransports(transportDetailType, transport);
    }
}
