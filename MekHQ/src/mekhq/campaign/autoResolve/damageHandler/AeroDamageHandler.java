package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.Aero;
import megamek.common.HitData;
import megamek.common.IEntityRemovalConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static megamek.common.Compute.randomInt;

public class AeroDamageHandler implements DamageHandler<Aero> {

    private final Aero entity;
    private final CrewMustSurvive crewMustSurvive;
    private final EntityMustSurvive entityMustSurvive;

    public AeroDamageHandler(Aero entity, CrewMustSurvive crewMustSurvive, EntityMustSurvive entityMustSurvive) {
        this.entity = entity;
        this.crewMustSurvive = crewMustSurvive;
        this.entityMustSurvive = entityMustSurvive;
    }

    @Override
    public Aero entity() {
        return entity;
    }

    @Override
    public CrewMustSurvive crewMustSurvive() {
        return crewMustSurvive;
    }

    @Override
    public EntityMustSurvive entityMustSurvive() {
        return entityMustSurvive;
    }


    @Override
    public int getHitLocation() {
        var entity = entity();
        List<Integer> validLocations = new ArrayList<>();
        for (int i = 0; i < entity.locations(); i++) {
            if (entity.getOArmor(i) <= 0) {
                continue;
            }
            if (!entity.isLocationBlownOff(i) && entity.getSI() > 0) {
                validLocations.add(i);
            }
        }

        return validLocations.isEmpty() ? -1 : validLocations.get(randomInt(validLocations.size()));
    }

    @Override
    public void destroyLocationAfterEjection() {
        entity().setDestroyed(true);
        entity().setRemovalCondition(IEntityRemovalConditions.REMOVE_DEVASTATED);
    }

    @Override
    public void damageInternals(HitDetails hitDetails) {
        HitData hit = hitDetails.hit();
        var entity = entity();
        int currentInternalValue = entity.getSI();
        int newInternalValue = Math.max(currentInternalValue + hitDetails.setArmorValueTo(), 0);
        entity.setArmor(0, hit);
        entity.setSI(newInternalValue);
        applyDamageToEquipments(hit);
        if (newInternalValue == 0) {
            destroyLocation(hit);
        }
    }
}
