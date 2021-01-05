package mekhq.campaign.unit.cleanup;

import java.util.*;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.*;
import mekhq.campaign.unit.Unit;

public class EquipmentProposal {
    protected final Unit unit;
    protected final Map<Integer, Mounted> equipment = new HashMap<>();
    protected final Map<Part, Integer> original = new HashMap<>();
    protected final Map<Part, Integer> mapped = new HashMap<>();

    public EquipmentProposal(Unit unit) {
        this.unit = Objects.requireNonNull(unit);
    }

    public Unit getUnit() {
        return unit;
    }

    public void consider(Part part) {
        if (part instanceof EquipmentPart) {
            original.put(part, ((EquipmentPart) part).getEquipmentNum());
        } else if (part instanceof MissingEquipmentPart) {
            original.put(part, ((MissingEquipmentPart) part).getEquipmentNum());
        }
    }

    public void includeEquipment(int equipmentNum, Mounted mount) {
        equipment.put(equipmentNum, Objects.requireNonNull(mount));
    }

    public void proposeMapping(Part part, int equipmentNum) {
        equipment.remove(equipmentNum);
        mapped.put(part, equipmentNum);
    }

    public Set<Part> getParts() {
        return Collections.unmodifiableSet(original.keySet());
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
        Integer originalEquipmentNum = original.get(part);
        return (originalEquipmentNum != null) ? originalEquipmentNum : -1;
    }

    public boolean isReduced() {
        for (Part part : original.keySet()) {
            if (mapped.get(part) == null) {
                return false;
            }
        }
        
        return true;
    }

    public void apply() {
        for (Part part : original.keySet()) {
            int equipmentNum = -1;
            if (mapped.get(part) != null) {
                equipmentNum = mapped.get(part);
            }

            if (part instanceof EquipmentPart) {
                ((EquipmentPart) part).setEquipmentNum(equipmentNum);
            } else if (part instanceof MissingEquipmentPart) {
                ((MissingEquipmentPart) part).setEquipmentNum(equipmentNum);
            }
        }
    }
}
