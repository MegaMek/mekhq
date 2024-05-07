/*
 * Copyright (C) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.*;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * This class handles randomly generating SPAs for bot-controlled entities
 * @author NickAragua
 */
public class CrewSkillUpgrader {
    // complex data structure
    // first key is the unit type as in Megamek.common.UnitType
    // second key is the XP cost of the SPA
    private Map<Integer, Map<Integer, List<SpecialAbility>>> specialAbilitiesByUnitType;
    private double maxAbilityXPCost = 0;
    private double twoThirdsXPCost;
    private double oneThirdXPCost;
    private double minAbilityCost = Double.MAX_VALUE;
    private int upgradeIntensity;

    /**
     * Constructor. Initializes updated SPA list, broken down by unit type.
     * @param upgradeIntensity how likely a pilot is to receive consideration for an SPA (<0 means none, 3+ means every time)
     */
    public CrewSkillUpgrader(int upgradeIntensity) {
        this.upgradeIntensity = upgradeIntensity;

        specialAbilitiesByUnitType = new HashMap<>();

        for (SpecialAbility spa : SpecialAbility.getWeightedSpecialAbilities()) {
            if (spa.getCost() > maxAbilityXPCost) {
                maxAbilityXPCost = spa.getCost();
            }

            if (spa.getCost() < minAbilityCost) {
                minAbilityCost = spa.getCost();
            }

            for (int unitType = 0; unitType < UnitType.SIZE; unitType++) {
                specialAbilitiesByUnitType.putIfAbsent(unitType, new HashMap<>());
                if (spa.isEligible(unitType)) {
                    specialAbilitiesByUnitType.get(unitType).putIfAbsent(spa.getCost(), new ArrayList<>());
                    specialAbilitiesByUnitType.get(unitType).get(spa.getCost()).add(spa);
                }
            }
        }

        twoThirdsXPCost = maxAbilityXPCost / 3.0 * 2.0;
        oneThirdXPCost = maxAbilityXPCost / 3.0;
    }

    /**
     * Upgrades an entity's crew as per Campaign Ops rules.
     * @param entity The entity to potentially upgrade.
     */
    public void upgradeCrew(Entity entity) {
        // roll 1d4, and get SPAs with configurable odds
        // determine veterancy level
        // this sets the weight limit and how many SPAs we can assign
        // complete scrubs don't get SPAs.
        // this is described in some detail in CamOps page 70 (Special Pilot Abilities)
        int upgradeRoll = Compute.randomInt(4);
        if (upgradeRoll > upgradeIntensity) {
            return;
        }

        double skillAvg = (entity.getCrew().getGunnery() + entity.getCrew().getPiloting()) / 2.0;
        double xpCap = 0;
        int spaCap = 0;

        // elite
        if (skillAvg < 3) {
            xpCap = maxAbilityXPCost;
            spaCap = 3;
        // veteran
        } else if (skillAvg < 4) {
            xpCap = twoThirdsXPCost;
            spaCap = 2;
        // regular
        } else if (skillAvg < 5) {
            xpCap = oneThirdXPCost;
            spaCap = 1;
        }

        // algorithm:
        // we want a maximum # of SPAs, capped, in total at a max XP cost
        // every time we generate an SPA, we reduce the max available XP
        // this logic also prevents attempting to assign an SPA when there are no SPAs
        // that cost less XP than the remaining cap
        for (int x = 0; (x < spaCap) && (xpCap > minAbilityCost); x++) {
            int spaCost = addSingleSPA(entity, xpCap);
            if (spaCost == 0) {
                break;
            }
            xpCap -= spaCost;
        }
    }

    /**
     * Upgrade an entity with a single SPA
     * @param entity
     * @return the xp cost of the added SPA
     */
    private int addSingleSPA(Entity entity, double xpCap) {
        int unitType = entity.getUnitType();

        List<SpecialAbility> choices = coalescedSPAList(unitType, xpCap);
        if (choices.isEmpty()) {
            return 0;
        }

        while (!choices.isEmpty()) {
            int spaIndex = Compute.randomInt(choices.size());
            SpecialAbility spa = choices.get(spaIndex);

            // if the ability is disqualified by some weird circumstances,
            // because the entity already has it
            // or because it exceeds the weight limit
            // then try to generate another one
            if (entity.hasAbility(spa.getName()) || !extraEligibilityCheck(spa, entity)) {
                choices.remove(spaIndex);
            } else {
                String spaValue;

                switch (spa.getName()) {
                    case OptionsConstants.MISC_ENV_SPECIALIST:
                        spaValue = pickRandomEnvSpec();
                        break;
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
                        spaValue = pickRandomWeapon(entity, false);
                        break;
                    case OptionsConstants.GUNNERY_SANDBLASTER:
                        spaValue = pickRandomWeapon(entity, true);
                        break;
                    default:
                        // If there's no crew, stop looking for SPAs
                        if (entity.getCrew() == null) {
                            return 0;
                            // If we can't access the option, try a different one
                        } else if ((entity.getCrew().getOptions() == null) ||
                                (entity.getCrew().getOptions(spa.getName()) == null)) {
                            // Make sure to remove choices we can't use
                            choices.remove(spaIndex);
                            continue;
                        }

                        // If the option has a name but isn't defined, try another one
                        try {
                            entity.getCrew().getOptions().getOption(spa.getName()).setValue(true);
                            return spa.getCost();
                        } catch (NullPointerException e) {
                            LogManager.getLogger().warn("Attempted to assign SPA '" + spa.getName() + "' but SPA not found.");
                            // Make sure to remove choices we can't use
                            choices.remove(spaIndex);
                            continue;
                        }
                }

                // Assign the SPA, unless it was unable to pick a random weapon/specialization for whatever reason
                if (!spaValue.equals(Crew.SPECIAL_NONE)) {
                    entity.getCrew().getOptions().getOption(spa.getName()).setValue(spaValue);
                    return spa.getCost();
                }
            }
        }

        return 0;
    }

    /**
     * Utility function that returns all the SPAs for the given unit type at or below the given cap
     * @param unitType Unit type
     * @param xpCap maximum xp cost
     * @return coalesced list
     */
    private List<SpecialAbility> coalescedSPAList(int unitType, double xpCap) {
        List<SpecialAbility> coalescedList = new ArrayList<>();

        for (int cost : specialAbilitiesByUnitType.get(unitType).keySet()) {
            if (cost <= xpCap) {
                coalescedList.addAll(specialAbilitiesByUnitType.get(unitType).get(cost));
            }
        }

        return coalescedList;
    }

    /**
     * Contains "special" logic to ensure SPA is appropriate for the entity, beyond the simple unit type check.
     * @param spa
     * @param entity
     * @return
     */
    private boolean extraEligibilityCheck(SpecialAbility spa, Entity entity) {
        switch (spa.getName()) {
            case OptionsConstants.PILOT_ANIMAL_MIMIC:
                return entity.entityIsQuad() || entity.hasQuirk(OptionsConstants.QUIRK_POS_ANIMALISTIC);
            default:
                return true;
        }
    }

    /**
     * Picks a random weapon specialization for a weapon that the entity has mounted,
     * and returns the weapon's name so that a weapon specialist value may be set.
     * @param entity The entity being manipulated
     * @return Weapon name
     */
    private String pickRandomWeapon(Entity entity, boolean clusterOnly) {
        List<WeaponMounted> weapons = entity.getIndividualWeaponList();
        List<WeaponMounted> eligibleWeapons = new ArrayList<>();

        for (WeaponMounted weapon : weapons) {
            if (SpecialAbility.isWeaponEligibleForSPA(weapon.getType(), PersonnelRole.NONE, clusterOnly)) {
                eligibleWeapons.add(weapon);
            }
        }

        if (eligibleWeapons.isEmpty()) {
            return Crew.SPECIAL_NONE;
        }

        int weaponIndex = Compute.randomInt(eligibleWeapons.size());
        return eligibleWeapons.get(weaponIndex).getName();
    }

    /**
     * Picks a random gunnery specialization for the given entity. Naturally weighted
     * towards weapon categories contained in the entity.
     * @param entity The entity being examined.
     * @return Gunnery specialization name
     */
    private String pickRandomGunnerySpecialization(Entity entity) {
        // if you've got no weapons, tough
        if (entity.getIndividualWeaponList().size() <= 0) {
            return Crew.SPECIAL_NONE;
        }

        int weaponIndex = Compute.randomInt(entity.getIndividualWeaponList().size());
        WeaponType weaponType = (WeaponType) entity.getIndividualWeaponList().get(weaponIndex).getType();

        if (weaponType.hasFlag(WeaponType.F_BALLISTIC)) {
            return Crew.SPECIAL_BALLISTIC;
        } else if (weaponType.hasFlag(WeaponType.F_ENERGY)) {
            return Crew.SPECIAL_ENERGY;
        } else if (weaponType.hasFlag(WeaponType.F_MISSILE)) {
            return Crew.SPECIAL_MISSILE;
        } else {
            return Crew.SPECIAL_NONE;
        }
    }

    private String pickRandomRangeMaster() {
        return ObjectUtility.getRandomItem(
                Arrays.asList(Crew.RANGEMASTER_MEDIUM, Crew.RANGEMASTER_LONG, Crew.RANGEMASTER_EXTREME));
    }

    private String pickRandomHumanTRO() {
        return ObjectUtility.getRandomItem(
                Arrays.asList(Crew.HUMANTRO_MECH, Crew.HUMANTRO_AERO, Crew.HUMANTRO_VEE, Crew.HUMANTRO_BA));
    }

    private String pickRandomEnvSpec() {
        return ObjectUtility.getRandomItem(
                Arrays.asList(Crew.ENVSPC_FOG, Crew.ENVSPC_LIGHT, Crew.ENVSPC_RAIN, Crew.ENVSPC_SNOW, Crew.ENVSPC_WIND));
    }
}
