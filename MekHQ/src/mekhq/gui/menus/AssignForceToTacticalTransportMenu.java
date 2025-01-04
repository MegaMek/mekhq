package mekhq.gui.menus;

import megamek.common.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class AssignForceToTacticalTransportMenu extends JScrollableMenu {

    Campaign campaign;

    // region Constructors
    public AssignForceToTacticalTransportMenu(final Campaign campaign, final Unit... units) {
        super("assignforce");
        initialize(campaign, units);
    }
    // endregion Constructors

    private void initialize(final Campaign campaign, final Unit... units) {
        this.campaign = campaign;

        /*
         * Immediate Return for Illegal Assignments:
         * 1) No units to assign
         * 2) Any units are currently unavailable
         * 3) No transports available
         */
        if ((units.length == 0) || (Stream.of(units).anyMatch(unit -> !unit.isAvailable()))
            || (!campaign.hasTacticalTransports())) {
            return;
        }

        Set<JScrollableMenu> transporterTypeMenus = createtransporterTypeMenus(units);
        if(transporterTypeMenus.isEmpty()) {
            return;
        }

        //TODO
        //setText(resources.getString(""));
        setText("Assign Unit to Tactical Transport");
        for (JScrollableMenu transporterTypeMenu : transporterTypeMenus) {
            add(transporterTypeMenu);
        }
    }


    private Set<JScrollableMenu> createtransporterTypeMenus(final Unit... units) {
        Set<JScrollableMenu> transporterTypeMenus = new HashSet<>();
        /* Let's get the transport types our campaign has
         and remove the ones that can't be used by these units.
         While we're at it, let's get the total capacity we'll
         need to transport all these units. If they use different
         calculation methods (an infantry and tank were both selected)
         then they shouldn't have any compatible transport types
         */
        Set<Class<? extends Transporter>> transporterTypes = new HashSet<>(campaign.getTacticalTransports().keySet());

        for (Unit unit : units) {
            Set<Class<? extends Transporter>> unitTransporterTypes = mapUnitToTransporters(unit);
            if (!unitTransporterTypes.isEmpty()) {
                transporterTypes.retainAll(unitTransporterTypes);
            } else {
                return transporterTypeMenus;
            }
        }
        if (transporterTypes.isEmpty()) {
            return transporterTypeMenus;
        }


        for (Class<? extends Transporter> transporterType : transporterTypes) {
            double requiredTransportCapacity = 0.0;
            for (Unit unit : units) {
                requiredTransportCapacity += unit.transportCapacityUsage(transporterType);
            }

            Set<Unit> transports = campaign.getTacticalTransportsByType(transporterType, requiredTransportCapacity);

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
                + " | Space Remaining: " + transport.getCurrentTacticalTransportCapacity(transporterType)); //TODO
            //TODO hacky implementation
            transportMenu.addActionListener(evt -> {
                for (Unit unit : units) {
                    if (!transport.getEntity().canLoad(unit.getEntity())) {
                        return; //TODO error popup
                    }

                }
                Set<Unit> oldTransports = transport.loadTacticalTransport(transporterType, units);
                if (!oldTransports.isEmpty()) {
                    oldTransports.forEach(oldTransport -> campaign.updateTransportInTacticalTransports(oldTransport));
                }
                campaign.updateTransportInTacticalTransports(transport);
            });

            transportMenus.add(transportMenu);
        }
        return transportMenus;
    }


    /**
     *
     * @param unit
     * @return the transporter types that could potentially transport this unit
     */
    private Set<Class<? extends Transporter>> mapUnitToTransporters(Unit unit) { //TODO This shouldn't be here, help me find a new home for this!
        Set<Class<? extends Transporter>> transporters = new HashSet<>();
        Class<? extends Entity> entityType = unit.getEntity().getClass();
        if (ProtoMek.class.isAssignableFrom(entityType)) {
            transporters.add(ProtoMekBay.class);
            transporters.add(ProtoMekClampMount.class);
        }
        else if (Aero.class.isAssignableFrom(entityType)) {
            //TODO

        }
        else if (Tank.class.isAssignableFrom(entityType)) {
            //TODO

        }
        else if (Mek.class.isAssignableFrom(entityType)) {
            //TODO

        }
        else if (Infantry.class.isAssignableFrom(entityType)) {
            transporters.add(InfantryBay.class);
            transporters.add(InfantryCompartment.class);

            if (BattleArmor.class.isAssignableFrom(entityType)) {
                transporters.add(BattleArmorBay.class);
                BattleArmor baUnit = (BattleArmor) unit.getEntity();

                if (baUnit.canDoMechanizedBA()) {
                    transporters.add(BattleArmorHandles.class);
                    transporters.add(BattleArmorHandlesTank.class);

                    if (baUnit.hasMagneticClamps()) {
                        transporters.add(ClampMountMek.class);
                        transporters.add(ClampMountTank.class);
                    }
                }

            }

        }
        return transporters;
    }
}
