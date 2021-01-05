package mekhq.campaign.unit.cleanup;

import java.util.Arrays;
import java.util.List;

import megamek.common.BattleArmor;
import megamek.common.Mounted;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

public class DefaultEquipmentUnscrambler extends EquipmentUnscrambler {

    public DefaultEquipmentUnscrambler(Unit unit) {
        super(unit);
        
        if (unit.getEntity() instanceof BattleArmor) {
            throw new IllegalArgumentException("DefaultEquipmentUnscrambler cannot unscramble BattleArmorEquipmentParts");
        }
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
        return EquipmentProposalReport.createReport(unit, proposal);
    }
}
