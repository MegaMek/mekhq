package mekhq.campaign.unit.cleanup;

import java.util.Arrays;
import java.util.List;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Mounted;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;

public class DefaultEquipmentUnscrambler extends EquipmentUnscrambler {

    public DefaultEquipmentUnscrambler(Unit unit) {
        super(unit);
        
        if (unit.getEntity() instanceof BattleArmor) {
            throw new IllegalArgumentException("DefaultEquipmentUnscrambler cannot unscramble BattleArmorEquipmentParts");
        }
    }

    public EquipmentUnscramblerResult unscramble(boolean isRefit) {
        EquipmentProposal proposal = createProposal();
        for (UnscrambleStep step : getSteps()) {
            if (proposal.isReduced()) {
                break;
            }

            for (Part part : proposal.getParts()) {
                if (part instanceof EquipmentPart) {
                    step.visit(proposal, (EquipmentPart) part);
                } else if (part instanceof MissingEquipmentPart) {
                    step.visit(proposal, (MissingEquipmentPart) part);
                }
            }

            // Reduce the set of equipment needing to be unscrambled
            proposal = proposal.reduce();
        }

        // Apply any changes to the equipment numbers
        proposal.apply();

        EquipmentUnscramblerResult result = new EquipmentUnscramblerResult(unit);
        result.setSucceeded(proposal.isReduced());
        result.setMessage(EquipmentProposalReport.createReport(unit, proposal));

        proposal.cleanUp();

        if (isRefit) {
            unscrambleAmmoBinMunitions();
        }

        return result;
    }

    private static List<UnscrambleStep> getSteps() {
        return Arrays.asList(new UnscrambleStep[] {
            new ExactMatchStep(),
            new ApproximateMatchStep(),
            new MovedEquipmentStep(),
            new MovedAmmoBinStep(),
        });
    }

    private EquipmentProposal createProposal() {
        EquipmentProposal proposal = new EquipmentProposal();
        for (Part part : unit.getParts()) {
            proposal.consider(part);
        }

        for (Mounted m : unit.getEntity().getEquipment()) {
            proposal.include(unit.getEntity().getEquipmentNum(m), m);
        }

        return proposal;
    }
    
    private void unscrambleAmmoBinMunitions() {
        for (Part part : unit.getParts()) {
            if (part instanceof AmmoBin) {
                final AmmoBin ammoBin = (AmmoBin) part;
                final Mounted mounted = unit.getEntity().getEquipment(ammoBin.getEquipmentNum());
                if ((mounted != null) && (mounted.getType() instanceof AmmoType)
                        && !ammoBin.getType().equals(mounted.getType())
                        && ammoBin.canChangeMunitions((AmmoType) mounted.getType())) {
                    // AmmoBin changed munition type during a refit
                    ammoBin.updateConditionFromPart();
                    // Unload bin before munition change
                    ammoBin.unload();
                    ammoBin.changeMunition((AmmoType) mounted.getType());
                }
            }
        }
    }
}
