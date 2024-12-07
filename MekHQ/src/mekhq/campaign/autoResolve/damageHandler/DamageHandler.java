/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.*;
import mekhq.campaign.autoResolve.helper.RandomUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Luana Coppio
 */
public interface DamageHandler<E extends Entity> {

    enum PilotEjected {
        YES, NO
    }

    E entity();

    boolean crewMustSurvive();
    boolean entityMustSurvive();

    /**
     * Applies damage to the entity in clusters of a given size.
     * This is USUALLY the function you will want to use.
     *
     * @param dmg the total damage to apply
     * @param clusterSize the size of the clusters
     */
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

    /**
     * Applies damage to the entity.
     * @param dmg the total damage to apply
     */
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

    /**
     * Returns the location to hit.
     * @return returns a valid random location to be hit.
     */
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

    /**
     * Hits the entity with the given hit details.
     * @param hitDetails the hit details
     */
    default void hitEntity(HitDetails hitDetails) {
        damageArmor(hitDetails);
        if (hitDetails.hitInternal()) {
            hitDetails = hitDetails.withIncrementedHitCrew();
            damageInternals(hitDetails);
        }
        tryToDamageCrew(hitDetails.hitCrew());
    }

    /**
     * Destroys the location after the crew has been ejected.
     */
    default void destroyLocationAfterEjection() {
        // default implementation does nothing
    }

    /**
     * Applies damage to the internals of the entity.
     * @param hitDetails the hit details
     */
    default void damageInternals(HitDetails hitDetails) {
        HitData hit = hitDetails.hit();
        var entity = entity();
        int currentInternalValue = entity.getInternal(hit);
        int newInternalValue = Math.max(currentInternalValue + hitDetails.setArmorValueTo(), 0);
        entity.setArmor(0, hit);
        if (entityMustSurvive()) {
            newInternalValue = Math.max(newInternalValue, Compute.d6());
        }
        entity.setInternal(newInternalValue, hit);
        applyDamageToEquipments(hit);
        if (newInternalValue == 0) {
            destroyLocation(hit);
        }
    }

    /**
     * Destroys the location of the entity.
     * @param hit the hit data with information about the location
     */
    default void destroyLocation(HitData hit) {
        var entity = entity();
        entity.destroyLocation(hit.getLocation());
        entity.setDestroyed(true);
        tryToDamageCrew(Crew.DEATH);
        if (entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_DEVASTATED) {
            entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
            entity.setSalvage(true);
        }
    }

    /**
     * Tries to damage the crew of the entity.
     * @param hitCrew the amount of hits to apply to ALL crew
     */
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

        if ((crewMustSurvive()) && (hits == Crew.DEATH)) {
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

    /**
     * Tries to eject the crew of the entity if possible.
     * @return YES if the crew was ejected, NO otherwise
     */
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

    /**
     * Applies damage to the equipments of the entity.
     * @param hit the hit data with information about the location
     */
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

    /**
     * Damages the armor of the entity.
     * @param hitDetails the hit details
     */
    default void damageArmor(HitDetails hitDetails) {
        entity().setArmor(Math.max(hitDetails.setArmorValueTo(), 0), hitDetails.hit());
        System.out.println("Armor: " + Math.max(hitDetails.setArmorValueTo(), 0));
    }

    /**
     * Returns the hit data for the given hit location.
     * @param hitLocation the hit location
     * @return the hit data
     */
    default HitData getHitData(int hitLocation) {
        return new HitData(hitLocation, false, HitData.EFFECT_NONE);
    }

    /**
     * Sets up the hit details for the given hit and damage.
     * @param hit the hit data
     * @param dmg the damage to apply
     * @return the hit details
     */
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
