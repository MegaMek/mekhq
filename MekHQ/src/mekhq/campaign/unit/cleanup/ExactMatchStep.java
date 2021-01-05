package mekhq.campaign.unit.cleanup;

import megamek.common.Mounted;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;

public class ExactMatchStep extends UnscrambleStep {
    @Override
    public void visit(EquipmentProposal proposal, EquipmentPart part) {
        final Mounted mount = proposal.getEquipment(part.getEquipmentNum());
        if ((mount != null) && part.getType().equals(mount.getType())) {
            proposal.proposeMapping(part, part.getEquipmentNum());
        }
    }

    @Override
    public void visit(EquipmentProposal proposal, MissingEquipmentPart part) {
        final Mounted mount = proposal.getEquipment(part.getEquipmentNum());
        if ((mount != null) && part.getType().equals(mount.getType())) {
            proposal.proposeMapping(part, part.getEquipmentNum());
        }
    }
}
