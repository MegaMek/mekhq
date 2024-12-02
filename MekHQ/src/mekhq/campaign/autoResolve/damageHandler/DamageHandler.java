package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.*;
import mekhq.campaign.autoResolve.helper.RandomUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static megamek.common.Compute.rollD6;

public interface DamageHandler<E extends Entity> {

    enum PilotEjected {
        YES, NO
    }

    E entity();

    CrewMustSurvive crewMustSurvive();
    EntityMustSurvive entityMustSurvive();

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
        tryToDamageCrew(hitDetails.hitCrew());
    }

    default void
    destroyLocationAfterEjection() {
        // default implementation does nothing
    }

    default void damageInternals(HitDetails hitDetails) {
        HitData hit = hitDetails.hit();
        var entity = entity();
        int currentInternalValue = entity.getInternal(hit);
        int newInternalValue = Math.max(currentInternalValue + hitDetails.setArmorValueTo(), 0);
        entity.setArmor(0, hit);
        if (entityMustSurvive() == EntityMustSurvive.YES) {
            newInternalValue = Math.max(newInternalValue, Compute.d6());
        }
        entity.setInternal(newInternalValue, hit);
        applyDamageToEquipments(hit);
        if (newInternalValue == 0) {
            destroyLocation(hit);
        }
    }

    default void destroyLocation(HitData hit) {
        var entity = entity();
        entity.destroyLocation(hit.getLocation());
        System.out.println("Location destroyed: " + hit.getLocation());
        entity.setDestroyed(true);
        tryToDamageCrew(Crew.DEATH);
        if (entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_DEVASTATED) {
            entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
            entity.setSalvage(true);
        }
    }

    default void tryToDamageCrew(int hitCrew) {
        if (hitCrew == 0 || (entity().getRemovalCondition() == IEntityRemovalConditions.REMOVE_EJECTED)) {
            return;
        }

        var entity = entity();
        Crew crew = entity.getCrew();
        if (crew == null || crew.isEjected()) {
            return;
        }
        var hits = Math.min(crew.getHits() + hitCrew, Crew.DEATH);

        if ((crewMustSurvive() == CrewMustSurvive.YES) && (hits == Crew.DEATH)) {
            var ejectionResult = tryToEjectCrew();
            if (ejectionResult == PilotEjected.YES) {
                destroyLocationAfterEjection();
                return;
            }
            hits = Crew.DEATH - 1;
        }

        crew.setHits(hits, 0);
        if (crew.isDead()) {
            entity.setDestroyed(true);
            if (entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_DEVASTATED) {
                entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
                entity.setSalvage(true);
            }
        }
    }

    default PilotEjected tryToEjectCrew() {
        var entity = entity();
        var crew = entity.getCrew();
        if (crew == null || crew.isEjected() || !entity().isEjectionPossible()) {
            return PilotEjected.NO;
        }
        crew.setEjected(true);
        entity.setDestroyed(true);
        if (entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_DEVASTATED) {
            entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
            entity.setSalvage(true);
        }
        return PilotEjected.YES;
    }

    default void applyDamageToEquipments(HitData hit) {
        var entity = entity();
        var criticalSlots = entity.getCriticalSlots(hit.getLocation()).stream().collect(RandomUtils.toShuffledList());
        for (CriticalSlot slot : criticalSlots) {
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
