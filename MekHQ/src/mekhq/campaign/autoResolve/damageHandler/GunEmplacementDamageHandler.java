package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.GunEmplacement;

public record GunEmplacementDamageHandler(GunEmplacement entity, CrewMustSurvive crewMustSurvive, EntityMustSurvive entityMustSurvive) implements DamageHandler<GunEmplacement> {

    @Override
    public int getHitLocation() {
        if (entity.isDestroyed()) {
            return -1;
        }
        return 0;
    }

}
