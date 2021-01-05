package mekhq.campaign.unit.cleanup;

import java.util.Map.Entry;

import megamek.common.AmmoType;
import megamek.common.Mounted;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;

public class MovedAmmoBinStep extends UnscrambleStep {
    @Override
    public void visit(EquipmentProposal proposal, EquipmentPart part) {
        if (part instanceof AmmoBin) {
            visit(proposal, (AmmoBin) part);
        }
    }

    public void visit(EquipmentProposal proposal, AmmoBin ammoBin) {
        for (Entry<Integer, Mounted> equipment : proposal.getEquipment()) {
            final Mounted m = equipment.getValue();
            if (m.isDestroyed() || !(m.getType() instanceof AmmoType)) { 
                continue;
            }

            if (ammoBin.canChangeMunitions((AmmoType) m.getType())) {
                proposal.proposeMapping(ammoBin, equipment.getKey());
                return;
            }
        }
    }
}