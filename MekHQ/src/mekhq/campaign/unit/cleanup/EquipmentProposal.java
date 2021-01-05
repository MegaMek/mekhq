package mekhq.campaign.unit.cleanup;

import java.util.*;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.*;

public class EquipmentProposal {
    private final Map<Integer, Mounted> equipment = new HashMap<>();
    private final Map<Part, Integer> original = new HashMap<>();
    private final Map<Part, Mounted> mapped = new HashMap<>();
    private final Map<Integer, Part> proposed = new TreeMap<>();

    public void consider(Part part) {
        if (part instanceof EquipmentPart) {
            mapped.put(part, null);
            original.put(part, ((EquipmentPart) part).getEquipmentNum());
        } else if (part instanceof MissingEquipmentPart) {
            mapped.put(part, null);
            original.put(part, ((MissingEquipmentPart) part).getEquipmentNum());
        }
    }

    public void includeEquipment(int equipmentNum, Mounted mount) {
        equipment.put(equipmentNum, mount);
    }

    public void proposeMapping(Part part, int equipmentNum, Mounted mount) {
        equipment.remove(equipmentNum);
        proposed.put(equipmentNum, part);
        mapped.put(part, mount);
    }

    public Set<Part> getParts() {
        return Collections.unmodifiableSet(mapped.keySet());
    }

    public Set<Map.Entry<Integer, Mounted>> getEquipment() {
        return Collections.unmodifiableSet(equipment.entrySet());
    }

    public @Nullable Mounted getEquipment(int equipmentNum) {
        return equipment.get(equipmentNum);
    }

    public boolean hasProposal(Part part) {
        return mapped.get(part) != null;
    }

    public int getOriginalMapping(Part part) {
        return original.get(part);
    }

    public boolean isReduced() {
        for (Mounted mount : mapped.values()) {
            // Unmapped part
            if (mount == null) {
                return false;
            }
        }
        
        return true;
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

        for (Map.Entry<Part, Mounted> entry : mapped.entrySet()) {
            // Skip mapped parts
            if (entry.getValue() != null) {
                continue;
            }

            final Part part = entry.getKey();
            if (part instanceof EquipmentPart) {
                ((EquipmentPart) part).setEquipmentNum(-1);
            } else if (part instanceof MissingEquipmentPart) {
                ((MissingEquipmentPart) part).setEquipmentNum(-1);
            }
        }
    }
}
