package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.*;

public record InfantryDamageHandler(Infantry entity) implements DamageHandler<Infantry> {

    @Override
    public CrewMustSurvive crewMustSurvive() {
        return null;
    }

    @Override
    public EntityMustSurvive entityMustSurvive() {
        return null;
    }

    @Override
    public void damageArmor(HitDetails hitDetails) {
        if (entity() instanceof BattleArmor te) {
            for (int i = 0; i < te.getTroopers(); i++) {
                var currentValueArmor = te.getArmor(BattleArmor.LOC_SQUAD);
                var newArmorValue = Math.max(currentValueArmor - 1, 0);
                if (te.getArmor(BattleArmor.LOC_TROOPER_1 + i) > 0) {
                    te.setArmor(newArmorValue, BattleArmor.LOC_TROOPER_1 + i);
                }
            }
        }
        if (entity().isCrippled()) {
            entity().setDestroyed(true);
        }
    }

    @Override
    public void damageInternals(HitDetails hitDetails) {
        if (entity() instanceof BattleArmor te) {
            for (int i = 0; i < te.getTroopers(); i++) {
                var currentValue = te.getInternal(BattleArmor.LOC_SQUAD);
                var newValue = Math.max(currentValue - 1, 0);
                if (te.getInternal(BattleArmor.LOC_TROOPER_1 + i) > 0) {
                    te.setInternal(newValue, BattleArmor.LOC_TROOPER_1 + i);
                }
            }
        } else {
            var currentValue = entity().getInternal(Infantry.LOC_INFANTRY);
            var newValue = Math.max(currentValue - 1, 0);
            entity().setInternal(newValue, Infantry.LOC_INFANTRY);
        }

        if (entity().isCrippled()) {
            entity().setDestroyed(true);
        }
    }
}
