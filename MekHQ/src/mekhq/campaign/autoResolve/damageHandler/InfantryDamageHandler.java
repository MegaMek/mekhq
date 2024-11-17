package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.*;

public class InfantryDamageHandler implements DamageHandler<Infantry> {

    private final Infantry entity;

    public InfantryDamageHandler(Infantry entity) {
        this.entity = entity;
    }

    @Override
    public Infantry getEntity() {
        return entity;
    }

    @Override
    public void applyDamage(int dmg) {
        applyDamageToInfantry(entity);
    }

    private void applyDamageToInfantry(Infantry infantry) {
        var currentValue = infantry.getInternal(Infantry.LOC_INFANTRY);
        var newValue = Math.max(currentValue - 1, 0);
        infantry.setInternal(newValue, Infantry.LOC_INFANTRY);
        if (infantry.isCrippled()) {
            infantry.setDestroyed(true);
        }
    }
}
