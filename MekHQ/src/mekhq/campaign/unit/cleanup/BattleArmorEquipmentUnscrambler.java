package mekhq.campaign.unit.cleanup;

import java.util.Arrays;
import java.util.List;

import megamek.common.BattleArmor;
import megamek.common.Mounted;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

public class BattleArmorEquipmentUnscrambler extends EquipmentUnscrambler {

    public BattleArmorEquipmentUnscrambler(Unit unit) {
        super(unit);
        
        if (!(unit.getEntity() instanceof BattleArmor)) {
            throw new IllegalArgumentException("Attempting to assign trooper values to parts for non-BA unit");
        }
	}

    @Override
    protected EquipmentProposal createProposal() {
        EquipmentProposal proposal = new BattleArmorEquipmentProposal(unit);
        for (Part part : unit.getParts()) {
            proposal.consider(part);
        }

        for (Mounted m : unit.getEntity().getEquipment()) {
            proposal.includeEquipment(unit.getEntity().getEquipmentNum(m), m);
        }

        return proposal;
    }

    @Override
    protected List<UnscrambleStep> createSteps() {
        return Arrays.asList(new UnscrambleStep[] {
            new ExactMatchStep(),
            new ApproximateMatchStep(),
            new MovedEquipmentStep(),
            new MovedAmmoBinStep(),
        });
    }

    @Override
    protected String createReport(EquipmentProposal proposal) {
        return EquipmentProposalReport.createReport(proposal);
    }
}
