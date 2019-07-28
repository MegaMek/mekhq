/*
 * MegaMek - Copyright (C) 2019 Megamek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package mekhq.campaign.mission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.UnitType;
import megamek.common.WeaponType;
import megamek.common.options.OptionsConstants;
import mekhq.Utilities;
import mekhq.campaign.personnel.SpecialAbility;

/**
 * This class handles randomly generating SPAs for bot-controlled entities
 * @author NickAragua
 *
 */
public class CrewSkillUpgrader {
    private Map<Integer, List<SpecialAbility>> specialAbilitiesByUnitType;
    
    /**
     * Constructor. Initializes updated SPA list, broken down by unit type.
     */
    public CrewSkillUpgrader() {
        specialAbilitiesByUnitType = new HashMap<>();
        
        for(SpecialAbility spa : SpecialAbility.getWeightedSpecialAbilities()) {
            for(int unitType = 0; unitType < UnitType.SIZE; unitType++) {
                specialAbilitiesByUnitType.putIfAbsent(unitType, new ArrayList<>());
                if(spa.isEligible(unitType)) {
                    specialAbilitiesByUnitType.get(unitType).add(spa);
                }
            }
        }
        
    }
    
    /**
     * Upgrades an entity's crew as per Campaign Ops rules.
     * @param entity The entity to potentially upgrade.
     */
    public void upgradeCrew(Entity entity) {
        // roll 1d4, and get SPAs with 25% odds (as recommended by CamOps)
        // determine veterancy level
        // this sets the weight limit and how many SPAs we can assign
        // this is described in some detail in CamOps page 70 (Special Pilot Abilities)
        int upgradeRoll = Compute.randomInt(4);
        if(upgradeRoll != 3) {
            return;
        }
        
        double skillAvg = (entity.getCrew().getGunnery() + entity.getCrew().getPiloting()) / 2;
        int weightCap = 0;
        int spaCap = 0;
        
        // elite
        if(skillAvg < 3) {
            weightCap = 6;
            spaCap = 3;
        // veteran
        } else if(skillAvg < 4) {
            weightCap = 4;
            spaCap = 2;
        // regular
        } else if(skillAvg < 5) {
            weightCap = 2;
            spaCap = 1;
        }
        
        for(int x = 0; x < spaCap; x++) {
            addSingleSPA(entity, weightCap);
        }
    }
    
    /**
     * Upgrade an entity with a single SPA
     * @param entity
     */
    public void addSingleSPA(Entity entity, int weightLimit) {
        int unitType = entity.getUnitType();
        
        int spaIndex = Compute.randomInt(specialAbilitiesByUnitType.get(unitType).size());
        SpecialAbility spa = specialAbilitiesByUnitType.get(unitType).get(spaIndex);
        
        // if the ability is disqualified by some weird circumstances,
        // because the entity already has it
        // or because it exceeds the weight limit 
        // then try to generate another one
        while(!extraEligibilityCheck(spa, entity) ||
                entity.hasAbility(spa.getName()) ||
                spa.getWeight() > weightLimit) {
            spaIndex = Compute.randomInt(specialAbilitiesByUnitType.get(unitType).size());
            spa = specialAbilitiesByUnitType.get(unitType).get(spaIndex);
        }
        
        String spaValue;
        
        switch(spa.getName()) {
        case OptionsConstants.MISC_HUMAN_TRO:
            spaValue = pickRandomHumanTRO();
            break;
        case OptionsConstants.GUNNERY_RANGE_MASTER:
            spaValue = pickRandomRangeMaster();
            break;
        case OptionsConstants.GUNNERY_SPECIALIST:
            spaValue = pickRandomGunnerySpecialization(entity);
            break;
        case OptionsConstants.GUNNERY_WEAPON_SPECIALIST:
            spaValue = pickRandomWeapon(entity);
            break;
        default:
            entity.getCrew().getOptions().getOption(spa.getName()).setValue(true);
            return;
        }
        
        entity.getCrew().getOptions().getOption(spa.getName()).setValue(spaValue);
    }
    
    /**
     * Contains "special" logic to ensure SPA is appropriate for the entity, beyond the simple unit type check.
     * @param spa
     * @param entity
     * @return
     */
    private boolean extraEligibilityCheck(SpecialAbility spa, Entity entity) {
        switch(spa.getName()) {
        case OptionsConstants.PILOT_ANIMAL_MIMIC:
            return entity.entityIsQuad() || entity.hasQuirk(OptionsConstants.QUIRK_POS_ANIMALISTIC);
        }
        
        return true;
    }
    
    /**
     * Picks a random weapon specialization for a weapon that the entity has mounted,
     * and returns the weapon's name so that a weapon specialist value may be set.
     * @param entity The entity being manipulated
     * @return Weapon name
     */
    private String pickRandomWeapon(Entity entity) {
        int weaponIndex = Compute.randomInt(entity.getIndividualWeaponList().size());
        return entity.getIndividualWeaponList().get(weaponIndex).getName();
    }
    
    /**
     * Picks a random gunnery specialization for the given entity. Naturally weighted
     * towards weapon categories contained in the entity.
     * @param entity The entity being examined.
     * @return Gunnery specialization name
     */
    private String pickRandomGunnerySpecialization(Entity entity) {
        int weaponIndex = Compute.randomInt(entity.getIndividualWeaponList().size());
        WeaponType weaponType = (WeaponType) entity.getIndividualWeaponList().get(weaponIndex).getType();
        
        if(weaponType.hasFlag(WeaponType.F_BALLISTIC)) {
            return Crew.SPECIAL_BALLISTIC;
        } else if(weaponType.hasFlag(WeaponType.F_ENERGY)) {
            return Crew.SPECIAL_ENERGY;
        } else if(weaponType.hasFlag(WeaponType.F_MISSILE)) {
            return Crew.SPECIAL_MISSILE;
        } else {
            return Crew.SPECIAL_NONE;
        }
    }
    
    private String pickRandomRangeMaster() {
        return Utilities.getRandomItem(
                Arrays.asList(new String[]{Crew.RANGEMASTER_MEDIUM, Crew.RANGEMASTER_LONG, Crew.RANGEMASTER_EXTREME})).toString();
    }
    
    private String pickRandomHumanTRO() {
        return Utilities.getRandomItem(
                Arrays.asList(new String[]{Crew.HUMANTRO_MECH, Crew.HUMANTRO_AERO, Crew.HUMANTRO_VEE, Crew.HUMANTRO_BA})).toString();
    }
}
