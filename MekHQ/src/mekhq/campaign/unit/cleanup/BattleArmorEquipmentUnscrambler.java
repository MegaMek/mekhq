package mekhq.campaign.unit.cleanup;

import java.util.List;
import java.util.stream.Collectors;

import megamek.common.BattleArmor;
import megamek.common.Mounted;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.BattleArmorEquipmentPart;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;

public class BattleArmorEquipmentUnscrambler extends EquipmentUnscrambler {

    public BattleArmorEquipmentUnscrambler(Unit unit) {
        super(unit);
        
        if (!(unit.getEntity() instanceof BattleArmor)) {
            throw new IllegalArgumentException("Attempting to assign trooper values to parts for non-BA unit");
        }
	}

	@Override
    public EquipmentUnscramblerResult unscramble(boolean isRefit) {
        EquipmentUnscramblerResult result = new EquipmentUnscramblerResult(unit);

        // Create a list that we can remove parts from as we match them
        List<EquipmentPart> tempParts = unit.getParts().stream()
                .filter(p -> p instanceof EquipmentPart)
                .map(p -> (EquipmentPart)p)
                .collect(Collectors.toList());

        for (Mounted m : unit.getEntity().getEquipment()) {
            final int eqNum = unit.getEntity().getEquipmentNum(m);
            //Look for parts of the same type with the equipment number already set correctly
            List<EquipmentPart> parts = tempParts.stream()
                    .filter(p -> p.getType().getInternalName().equals(m.getType().getInternalName())
                            && p.getEquipmentNum() == eqNum)
                    .collect(Collectors.toList());
            //If we don't find any, just match the internal name and set the equipment number.
            if (parts.isEmpty()) {
                parts = tempParts.stream()
                        .filter(p -> p.getType().getInternalName().equals(m.getType().getInternalName()))
                        .collect(Collectors.toList());
                parts.forEach(p -> p.setEquipmentNum(eqNum));
            }
            if (parts.stream().allMatch(p -> p instanceof BattleArmorEquipmentPart)) {
                //Try to find one for each trooper; if the Entity has multiple pieces of equipment of this
                //type this will make sure we're only setting one group to this eq number.
                Part[] perTrooper = new Part[unit.getEntity().locations() - 1];
                for (EquipmentPart p : parts) {
                    int trooper = ((BattleArmorEquipmentPart)p).getTrooper();
                    if (trooper > 0) {
                        perTrooper[trooper - 1] = p;
                    }
                }
                //Assign a part to any empty position and set the trooper field
                for (int t = 0; t < perTrooper.length; t++) {
                    if (null == perTrooper[t]) {
                        for (Part p : parts) {
                            if (((BattleArmorEquipmentPart)p).getTrooper() < 1) {
                                ((BattleArmorEquipmentPart)p).setTrooper(t + 1);
                                perTrooper[t] = p;
                                break;
                            }
                        }
                    }
                }
                //Normally there should be a part in each position, but we will leave open the possibility
                //of equipment missing equipment for some troopers in the case of modular/AP mounts or DWPs
                for (Part p : perTrooper) {
                    if (null != p) {
                        tempParts.remove(p);
                    }
                }
            } else {
                //Ammo Bin
                tempParts.removeAll(parts);
            }
        }
        //TODO: Is it necessary to update armor?

        result.setSucceeded(true);
        return result;
    }
    
}
