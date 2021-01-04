package mekhq.campaign.unit.cleanup;

import java.util.Objects;

import megamek.common.BattleArmor;
import mekhq.campaign.unit.Unit;

public abstract class EquipmentUnscrambler {

    protected final Unit unit;

    protected EquipmentUnscrambler(Unit unit) {
        this.unit = Objects.requireNonNull(unit);
    }

    public abstract EquipmentUnscramblerResult unscramble(boolean isRefit);

    public static EquipmentUnscrambler create(Unit unit) {
        if (unit.getEntity() instanceof BattleArmor) {
            return new BattleArmorEquipmentUnscrambler(unit);
        } else {
            return new DefaultEquipmentUnscrambler(unit);
        }
    }
}
