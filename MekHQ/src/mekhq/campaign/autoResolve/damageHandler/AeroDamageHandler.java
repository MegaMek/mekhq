package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.HitData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static megamek.common.Compute.randomInt;

public class AeroDamageHandler implements DamageHandler<Aero> {

    private final Aero entity;
    private final Random random = new Random();

    public AeroDamageHandler(Aero entity) {
        this.entity = entity;
    }

    @Override
    public Aero getEntity() {
        return entity;
    }

    @Override
    public int getHitLocation() {
        var entity = getEntity();
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
    public void damageInternals(HitDetails hitDetails) {
        HitData hit = hitDetails.hit();
        var entity = getEntity();
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
