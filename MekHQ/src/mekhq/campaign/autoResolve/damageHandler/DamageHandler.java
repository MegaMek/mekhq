package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.*;

import java.util.ArrayList;
import java.util.List;

import static megamek.common.Compute.randomInt;

public interface DamageHandler<E extends Entity> {

    E getEntity();

    default void applyDamage(int dmg) {
        int hitLocation = getHitLocation();
        if (hitLocation == -1) {
            getEntity().setDestroyed(true);
            return;
        }
        HitData hit = getHitData(hitLocation);
        HitDetails hitDetails = setupHitDetails(hit, dmg);
        hitEntity(hitDetails);
    }

    default int getHitLocation() {
        var entity = getEntity();
        List<Integer> validLocations = new ArrayList<>();
        for (int i = 0; i < entity.locations(); i++) {
            if (entity.getOArmor(i) <= 0) {
                continue;
            }
            if (!entity.isLocationBlownOff(i) && entity.getInternal(i) > 0) {
                validLocations.add(i);
            }
        }

        return validLocations.isEmpty() ? -1 : validLocations.get(randomInt(validLocations.size()));
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
        var entity = getEntity();
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
        var entity = getEntity();
        entity.destroyLocation(hit.getLocation());
        System.out.println("Location destroyed: " + hit.getLocation());
        if (hit.getLocation() == Mek.LOC_CT || hit.getLocation() == Mek.LOC_HEAD) {
            entity.setDestroyed(true);
            entity.setSalvage(true);
            if (hit.getLocation() == Mek.LOC_HEAD) {
                entity.getCrew().setHits(Crew.DEATH, 0);
                System.out.println("Crew dead");
            }
        }
    }

    private void damageCrew(int hitCrew) {
        if (hitCrew == 0) {
            return;
        }
        var entity = getEntity();
        Crew crew = entity.getCrew();
        crew.setHits(crew.getHits() + hitCrew, 0);
        System.out.println("Crew hits: " + crew.getHits());
        if (crew.isDead()) {
            entity.setDestroyed(true);
            entity.setSalvage(true);
            System.out.println("Crew dead");
        }
    }

    default void applyDamageToEquipments(HitData hit) {
        var entity = getEntity();
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
        getEntity().setArmor(Math.max(hitDetails.setArmorValueTo(), 0), hitDetails.hit());
        System.out.println("Armor: " + Math.max(hitDetails.setArmorValueTo(), 0));
    }

    default HitData getHitData(int hitLocation) {
        return new HitData(hitLocation, false, HitData.EFFECT_NONE);
    }

    default HitDetails setupHitDetails(HitData hit, int dmg) {
        int originalArmor = getEntity().getOArmor(hit);
        int damageToApply = Math.max((int) Math.floor((double) originalArmor / 10), dmg);
        int currentArmorValue = getEntity().getArmor(hit);
        int setArmorValueTo = currentArmorValue - damageToApply;
        boolean hitInternal = setArmorValueTo < 0;
        int hitCrew = hitInternal ? 1 : 0;

        return new HitDetails(hit, damageToApply, setArmorValueTo, hitInternal, hitCrew);
    }
}
