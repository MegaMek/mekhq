package mekhq.campaign.unit.cleanup;

import java.util.*;
import java.util.stream.Collectors;

import megamek.common.*;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.*;
import mekhq.campaign.unit.Unit;

public class BattleArmorEquipmentProposal extends EquipmentProposal {
    public BattleArmorEquipmentProposal(Unit unit) {
        super(unit);
    }

    @Override
    public void proposeMapping(Part part, int equipmentNum, Mounted mount) {
        // We do not clear the equipment because multiple battle armor parts
        // may use the same equipment.
        proposed.put(equipmentNum, part);
        mapped.put(part, mount);
    }

    @Override
    public void apply() {
        super.apply();

        // Clear out the used equipment from the equipment list
        for (int equipmentNum : proposed.keySet()) {
            equipment.remove(equipmentNum);
        }

        // Assign troopers per mount
        Map<Integer, List<BattleArmorEquipmentPart>> baPartMap =
                proposed.values().stream().filter(p -> p instanceof BattleArmorEquipmentPart)
                        .map(p -> (BattleArmorEquipmentPart) p)
                        .collect(Collectors.groupingBy(p -> p.getEquipmentNum()));
        for (List<BattleArmorEquipmentPart> parts : baPartMap.values()) {
            // Try to find one for each trooper; if the Entity has multiple pieces of equipment of this
            // type this will make sure we're only setting one group to this equipment number.
            Part[] perTrooper = new Part[unit.getEntity().locations() - 1];
            for (EquipmentPart p : parts) {
                int trooper = ((BattleArmorEquipmentPart)p).getTrooper();
                if (trooper > 0) {
                    perTrooper[trooper - 1] = p;
                }
            }

            // Assign a part to any empty position and set the trooper field
            for (int t = 0; t < perTrooper.length; t++) {
                if (null == perTrooper[t]) {
                    for (Part p : parts) {
                        if (((BattleArmorEquipmentPart)p).getTrooper() < 1) {
                            ((BattleArmorEquipmentPart)p).setTrooper(t + 1);
                            break;
                        }
                    }
                }
            }
        }
    }
}
