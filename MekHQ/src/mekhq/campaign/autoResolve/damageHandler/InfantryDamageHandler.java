package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.*;

public record InfantryDamageHandler(Infantry entity) implements DamageHandler<Infantry> {

    @Override
    public void applyDamage(int dmg) {
        applyDamageToInfantry(entity);
    }

    private void applyDamageToInfantry(Infantry infantry) {
        if (infantry instanceof BattleArmor te) {
            for (int i = 0; i < te.getTroopers(); i++) {
                var currentValue = te.getInternal(BattleArmor.LOC_SQUAD);
                var newValue = Math.max(currentValue - 1, 0);
                if (te.getInternal(BattleArmor.LOC_TROOPER_1 + i) > 0) {
                    te.setInternal(newValue, BattleArmor.LOC_TROOPER_1 + i);
                }
            }
        } else {
            var currentValue = infantry.getInternal(Infantry.LOC_INFANTRY);
            var newValue = Math.max(currentValue - 1, 0);
            infantry.setInternal(newValue, Infantry.LOC_INFANTRY);
        }

        if (infantry.isCrippled()) {
            infantry.setDestroyed(true);
        }
    }
}
