package mekhq.campaign.unit.cleanup;

import java.util.*;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.*;

public class EquipmentProposal {
    private final Set<Part> considered = new HashSet<>();
    private final Set<Part> remaining = new HashSet<>();
    private final Map<Integer, Mounted> equipment = new HashMap<>();
    private final Map<Integer, Part> proposed = new HashMap<>();
    private final Map<Part, Mounted> mapped = new HashMap<>();

    public void consider(Part part) {
        if ((part instanceof EquipmentPart) || (part instanceof MissingEquipmentPart)) {
            considered.add(part);
            remaining.add(part);
        }
    }

    public void include(int equipmentNum, Mounted mount) {
        equipment.put(equipmentNum, mount);
    }

    public void propose(Part part, int equipmentNum, Mounted mount) {
        remaining.remove(part);
        equipment.remove(equipmentNum);
        proposed.put(equipmentNum, part);
        mapped.put(part, mount);
    }

    public Set<Part> getParts() {
        return Collections.unmodifiableSet(considered);
    }

    public @Nullable Mounted getEquipment(int equipmentNum) {
        return equipment.get(equipmentNum);
    }

    public Set<Map.Entry<Integer, Mounted>> getEquipment() {
        return Collections.unmodifiableSet(equipment.entrySet());
    }

    public boolean isReduced() {
        return remaining.isEmpty();
    }
    
    public boolean hasProposal(Part part) {
        return mapped.containsKey(part);
    }

    public EquipmentProposal reduce() {
        EquipmentProposal reduced = new EquipmentProposal();
        for (Part part : remaining) {
            reduced.consider(part);
        }

        for (Map.Entry<Integer, Mounted> entry : equipment.entrySet()) {
            reduced.equipment.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Integer, Part> entry : proposed.entrySet()) {
            reduced.proposed.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Part, Mounted> entry : mapped.entrySet()) {
            reduced.mapped.put(entry.getKey(), entry.getValue());
        }

        return reduced;
    }

    public void apply() {
        for (Map.Entry<Integer, Part> entry : proposed.entrySet()) {
            final int equipmentNum = entry.getKey();
            final Part part = entry.getValue();
            if (part instanceof EquipmentPart) {
                ((EquipmentPart) part).setEquipmentNum(equipmentNum);
            } else if (part instanceof MissingEquipmentPart) {
                ((MissingEquipmentPart) part).setEquipmentNum(equipmentNum);
            }
        }
    }

    public void cleanUp() {
        for (Part part : remaining) {
            if (part instanceof EquipmentPart) {
                ((EquipmentPart) part).setEquipmentNum(-1);
            } else if (part instanceof MissingEquipmentPart) {
                ((MissingEquipmentPart) part).setEquipmentNum(-1);
            }
        }
    }
}
