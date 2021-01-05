package mekhq.campaign.unit.cleanup;

import java.util.*;
import java.util.stream.Collectors;

import megamek.common.BattleArmor;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.*;
import mekhq.campaign.unit.Unit;

public class BattleArmorEquipmentProposal extends EquipmentProposal {
    private final int squadSize;
    
    public BattleArmorEquipmentProposal(Unit unit) {
        super(unit);

        squadSize = ((BattleArmor) unit.getEntity()).getSquadSize();
    }

    @Override
    public void proposeMapping(Part part, int equipmentNum) {
        // We do not clear the equipment because multiple battle armor parts
        // may use the same equipment.
        mapped.put(part, equipmentNum);
    }

    @Override
    public void apply() {
        super.apply();

        // Clear used equipment from our list
        for (int equipmentNum : mapped.values()) {
            equipment.remove(equipmentNum);
        }

        // Assign troopers per mount
        Map<Integer, List<BattleArmorEquipmentPart>> baPartMap =
                mapped.keySet().stream()
                        .filter(p -> p instanceof BattleArmorEquipmentPart)
                        .map(p -> (BattleArmorEquipmentPart) p)
                        .collect(Collectors.groupingBy(p -> p.getEquipmentNum()));
        for (List<BattleArmorEquipmentPart> parts : baPartMap.values()) {
            // Try to find one for each trooper; if the Entity has multiple pieces of equipment of this
            // type this will make sure we're only setting one group to this equipment number.
            Part[] perTrooper = new Part[squadSize];
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
