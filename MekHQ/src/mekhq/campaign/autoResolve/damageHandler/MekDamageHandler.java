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

import java.util.List;
import java.util.Objects;

import static megamek.common.Compute.rollD6;
import static megamek.common.CriticalSlot.TYPE_SYSTEM;

/**
 * @author Luana Coppio
 */
public record MekDamageHandler(Mek entity, boolean crewMustSurvive, boolean entityMustSurvive) implements DamageHandler<Mek> {

    @Override
    public HitDetails setupHitDetails(HitData hit, int dmg) {
        int originalArmor = entity.getOArmor(hit);
        int damageToApply = Math.max((int) Math.floor((double) originalArmor / 10), dmg);
        int currentArmorValue = entity.getArmor(hit);
        int setArmorValueTo = currentArmorValue - damageToApply;
        boolean hitInternal = setArmorValueTo < 0;
        boolean isHeadHit = (entity.getCockpitType() != Mek.COCKPIT_TORSO_MOUNTED)
            && (hit.getLocation() == Mek.LOC_HEAD);
        int hitCrew = isHeadHit || hitInternal ? 1 : 0;

        return new HitDetails(hit, damageToApply, setArmorValueTo, hitInternal, hitCrew);
    }

    @Override
    public HitData getHitData(int hitLocation) {
        boolean hasRead = hitLocation == Mek.LOC_CT || hitLocation == Mek.LOC_LT || hitLocation == Mek.LOC_RT;
        boolean isRearHit = hasRead && entity.getArmor(hitLocation, true) > 0 && Compute.d6(2) > 10;
        return new HitData(hitLocation, isRearHit, HitData.EFFECT_NONE);
    }

    @Override
    public void destroyLocation(HitData hit) {
        var entity = entity();
        entity.destroyLocation(hit.getLocation());
        if (hit.getLocation() == Mek.LOC_CT || hit.getLocation() == Mek.LOC_HEAD) {
            entity.setDestroyed(true);
            if (hit.getLocation() == Mek.LOC_HEAD) {
                var toHit = new ToHitData();
                toHit.addModifier(4, "Ejection");
                if (rollD6(2).isTargetRollSuccess(toHit)) {
                    tryToEjectCrew();
                } else {
                    tryToDamageCrew(Crew.DEATH);
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

    @Override
    public void applyDamageToEquipments(HitData hit) {
        final var criticalSystems = List.of(Mek.SYSTEM_ENGINE, Mek.SYSTEM_GYRO, Mek.SYSTEM_LIFE_SUPPORT, Mek.SYSTEM_SENSORS, Mek.SYSTEM_COCKPIT);
        final var criticalLocations = List.of(Mek.LOC_CT, Mek.LOC_HEAD, Mek.LOC_LT, Mek.LOC_RT);

        var entity = entity();
        var criticalSlots = entity.getCriticalSlots(hit.getLocation()).stream().collect(RandomUtils.toShuffledList());
        if (entityMustSurvive() && criticalLocations.contains(hit.getLocation())) {
            criticalSlots = criticalSlots.stream().filter(Objects::nonNull)
                .filter(slot -> !(slot.getType() == TYPE_SYSTEM && criticalSystems.contains(slot.getIndex())))
                .toList();
        }
        for (CriticalSlot slot : criticalSlots) {
            if (slot != null && slot.isHittable() && !slot.isHit() && !slot.isDestroyed()) {
                slot.setHit(true);
                slot.setDestroyed(true);
                System.out.println("Equipment destroyed: " + slot);
                break;
            }
        }
    }

    @Override
    public void destroyLocationAfterEjection(){
        var entity = entity();
        entity.destroyLocation(Mek.LOC_HEAD);
    }

    @Override
    public  void damageInternals(HitDetails hitDetails) {
        HitData hit = hitDetails.hit();
        var entity = entity();
        int currentInternalValue = entity.getInternal(hit);
        int newInternalValue = Math.max(currentInternalValue + hitDetails.setArmorValueTo(), 0);
        entity.setArmor(0, hit);
        if (entityMustSurvive() && !canLoseLocation(hit)) {
            newInternalValue = Math.max(newInternalValue, Compute.d6());
        }
        entity.setInternal(newInternalValue, hit);
        applyDamageToEquipments(hit);
        if (newInternalValue == 0) {
            destroyLocation(hit);
        }
    }

    private boolean canLoseLocation(HitData hitData) {
        var location = hitData.getLocation();
        if (!entityMustSurvive()) {
            return true;
        }

        if (location == Mek.LOC_CT || location == Mek.LOC_HEAD) {
            return false;
        }
        if (location == Mek.LOC_LT || location == Mek.LOC_RT) {
            if (entity.getCritical(location, Mek.SYSTEM_ENGINE) == null) {
                return true;
            }
        }
        if (location == Mek.LOC_LLEG || location == Mek.LOC_RLEG) {
            return !entity.isLocationBlownOff(Mek.LOC_LLEG) && !entity.isLocationBlownOff(Mek.LOC_RLEG);
        }

        return true;
    }
}
