package mekhq.campaign.unit.cleanup;

import java.util.Map.Entry;

import megamek.common.Mounted;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;

public class MovedEquipmentStep extends UnscrambleStep {
    @Override
    public void visit(EquipmentProposal proposal, EquipmentPart part) {
        for (Entry<Integer, Mounted> equipment : proposal.getEquipment()) {
            final Mounted m = equipment.getValue();
            if (m.isDestroyed()) {
                continue;
            }

            if (m.getType().equals(part.getType())) {
                proposal.propose(part, equipment.getKey(), m);
                return;
            }
        }
    }

    @Override
    public void visit(EquipmentProposal proposal, MissingEquipmentPart part) {
        for (Entry<Integer, Mounted> equipment : proposal.getEquipment()) {
            final Mounted m = equipment.getValue();
            if (m.isDestroyed()) {
                continue;
            }

            if (m.getType().equals(part.getType())) {
                proposal.propose(part, equipment.getKey(), m);
                return;
            }
        }
    }
}
