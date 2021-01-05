package mekhq.campaign.unit.cleanup;

import java.util.List;
import java.util.Objects;

import megamek.common.BattleArmor;
import megamek.common.Mounted;
import megamek.common.annotations.Nullable;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;

public abstract class EquipmentUnscrambler {

    protected final Unit unit;

    protected EquipmentUnscrambler(Unit unit) {
        this.unit = Objects.requireNonNull(unit);
    }

    public EquipmentUnscramblerResult unscramble() {
        EquipmentProposal proposal = createProposal();
        for (UnscrambleStep step : createSteps()) {
            if (proposal.isReduced()) {
                break;
            }

            for (Part part : proposal.getParts()) {
                if (proposal.hasProposal(part)) {
                    continue;
                }

                if (part instanceof EquipmentPart) {
                    step.visit(proposal, (EquipmentPart) part);
                } else if (part instanceof MissingEquipmentPart) {
                    step.visit(proposal, (MissingEquipmentPart) part);
                }
            }
        }

        // Apply any changes to the equipment numbers
        proposal.apply();

        EquipmentUnscramblerResult result = new EquipmentUnscramblerResult(unit);
        result.setSucceeded(proposal.isReduced());
        result.setMessage(createReport(proposal));

        return result;
    }

    protected EquipmentProposal createProposal() {
        EquipmentProposal proposal = new EquipmentProposal();
        for (Part part : unit.getParts()) {
            proposal.consider(part);
        }

        for (Mounted m : unit.getEntity().getEquipment()) {
            proposal.includeEquipment(unit.getEntity().getEquipmentNum(m), m);
        }

        return proposal;
    }

    protected abstract List<UnscrambleStep> createSteps();

    protected abstract @Nullable String createReport(EquipmentProposal proposal);

    public static EquipmentUnscrambler create(Unit unit) {
        Objects.requireNonNull(unit, "Unit must not be null");
        if (unit.getEntity() instanceof BattleArmor) {
            return new BattleArmorEquipmentUnscrambler(unit);
        } else {
            return new DefaultEquipmentUnscrambler(unit);
        }
    }
}
