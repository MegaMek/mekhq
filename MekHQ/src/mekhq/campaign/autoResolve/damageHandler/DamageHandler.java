package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static megamek.common.Compute.randomInt;
import static megamek.common.Compute.rollD6;

public interface DamageHandler<E extends Entity> {

    E entity();

    default void applyDamageInClusters(int dmg, int clusterSize) {
        int totalDamage = dmg;
        while (totalDamage > 0) {
            if (entity().getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED) {
                // devastated units don't need to take any damage
                break;
            }
            var clusterDamage = Math.min(totalDamage, clusterSize);
            applyDamage(clusterDamage);
            totalDamage -= clusterDamage;

        }
    }

    default void applyDamage(int dmg) {
        int hitLocation = getHitLocation();
        if (hitLocation == -1) {
            entity().setDestroyed(true);
            return;
        }
        HitData hit = getHitData(hitLocation);
        HitDetails hitDetails = setupHitDetails(hit, dmg);
        hitEntity(hitDetails);
    }

    default int getHitLocation() {
        var entity = entity();
        List<Integer> validLocations = new ArrayList<>();
        for (int i = 0; i < entity.locations(); i++) {
            if (entity.getOArmor(i) <= 0) {
                continue;
            }
            var locationIsNotBlownOff = !entity.isLocationBlownOff(i);
            var locationIsNotDestroyed = entity.getInternal(i) > 0;
            if (locationIsNotBlownOff && locationIsNotDestroyed) {
                validLocations.add(i);
            }
        }
        Collections.shuffle(validLocations);
        return validLocations.isEmpty() ? -1 : validLocations.get(0);
    }

    default void hitEntity(HitDetails hitDetails) {
        damageArmor(hitDetails);
        if (hitDetails.hitInternal()) {
            hitDetails = hitDetails.withIncrementedHitCrew();
            damageInternals(hitDetails);
        }
        damageCrew(hitDetails.hitCrew());
    }

    default void damageInternals(HitDetails hitDetails) {
        HitData hit = hitDetails.hit();
        var entity = entity();
        int currentInternalValue = entity.getInternal(hit);
        int newInternalValue = Math.max(currentInternalValue + hitDetails.setArmorValueTo(), 0);
        entity.setArmor(0, hit);
        entity.setInternal(newInternalValue, hit);
        System.out.println("Internal: " + newInternalValue);
        applyDamageToEquipments(hit);
        if (newInternalValue == 0) {
            destroyLocation(hit);
        }
    }

    default void destroyLocation(HitData hit) {
        var entity = entity();
        entity.destroyLocation(hit.getLocation());
        System.out.println("Location destroyed: " + hit.getLocation());
        if (hit.getLocation() == Mek.LOC_CT || hit.getLocation() == Mek.LOC_HEAD) {
            entity.setDestroyed(true);
            if (hit.getLocation() == Mek.LOC_HEAD) {
                var toHit = new ToHitData();
                toHit.addModifier(8, "Ejection");
                if (rollD6(2).isTargetRollSuccess(toHit) && entity().isEjectionPossible()) {
                    entity.getCrew().setEjected(true);
                    entity.getCrew().setHits(5, 0);
                } else {
                    entity.getCrew().setHits(Crew.DEATH, 0);
                }
                if (entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_DEVASTATED) {
                    entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
                    entity.setSalvage(true);
                }
            } else {
                entity.setSalvage(false);
                entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_DEVASTATED);
            }
        }
    }

    private void damageCrew(int hitCrew) {
        if (hitCrew == 0) {
            return;
        }
        var entity = entity();
        Crew crew = entity.getCrew();
        crew.setHits(crew.getHits() + hitCrew, 0);
        System.out.println("Crew hits: " + crew.getHits());
        if (crew.isDead()) {
            entity.setDestroyed(true);
            entity.setSalvage(true);
            entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
            System.out.println("Crew dead");
        }
    }

    default void applyDamageToEquipments(HitData hit) {
        var entity = entity();
        for (CriticalSlot slot : entity.getCriticalSlots(hit.getLocation())) {
            if (slot != null && slot.isHittable() && !slot.isHit() && !slot.isDestroyed()) {
                slot.setHit(true);
                slot.setDestroyed(true);
                System.out.println("Equipment destroyed: " + slot);
                break;
            }
        }
    }

    default void damageArmor(HitDetails hitDetails) {
        entity().setArmor(Math.max(hitDetails.setArmorValueTo(), 0), hitDetails.hit());
        System.out.println("Armor: " + Math.max(hitDetails.setArmorValueTo(), 0));
    }

    default HitData getHitData(int hitLocation) {
        return new HitData(hitLocation, false, HitData.EFFECT_NONE);
    }

    default HitDetails setupHitDetails(HitData hit, int dmg) {
        int originalArmor = entity().getOArmor(hit);
        int damageToApply = Math.max((int) Math.floor((double) originalArmor / 10), dmg);
        int currentArmorValue = entity().getArmor(hit);
        int setArmorValueTo = currentArmorValue - damageToApply;
        boolean hitInternal = setArmorValueTo < 0;
        int hitCrew = hitInternal ? 1 : 0;

        return new HitDetails(hit, damageToApply, setArmorValueTo, hitInternal, hitCrew);
    }
}
