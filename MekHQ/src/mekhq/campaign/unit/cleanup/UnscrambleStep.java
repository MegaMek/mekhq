package mekhq.campaign.unit.cleanup;

import mekhq.campaign.parts.equipment.*;

public abstract class UnscrambleStep {
    public abstract void visit(EquipmentProposal proposal, EquipmentPart part);

    public void visit(EquipmentProposal proposal, MissingEquipmentPart part) {
    }
}
