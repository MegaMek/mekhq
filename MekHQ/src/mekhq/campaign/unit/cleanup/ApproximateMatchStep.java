package mekhq.campaign.unit.cleanup;

import megamek.common.AmmoType;
import megamek.common.Mounted;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;

public class ApproximateMatchStep extends UnscrambleStep {
    @Override
    public void visit(EquipmentProposal proposal, EquipmentPart part) {
        if (part instanceof AmmoBin) {
            visit(proposal, (AmmoBin) part);
        }
    }

    public void visit(EquipmentProposal proposal, AmmoBin ammoBin) {
        final Mounted mount = proposal.getEquipment(ammoBin.getEquipmentNum());
        if ((mount != null) && (mount.getType() instanceof AmmoType)
                && ammoBin.canChangeMunitions((AmmoType) mount.getType())) {
            proposal.proposeMapping(ammoBin, ammoBin.getEquipmentNum(), mount);
        }
    }
}
