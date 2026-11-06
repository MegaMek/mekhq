/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.training;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.annotation.Nullable;
import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

/**
 * One thing a person can spend XP on, for one person. A {@link TrainingPlan} stages purchases, prices them with
 * {@link TrainingPricer}, and {@link TrainingService} applies them.
 *
 * <p>Each purchase is one step: one skill level, one ability, one point of an attribute or trait. Training a skill up
 * three levels is three purchases, staged in order, so every step is priced from the level the one before it
 * reached.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public sealed interface TrainingPurchase {
    /**
     * @return the person the purchase is for
     */
    Person person();

    /**
     * @param purchases some purchases
     *
     * @return how many different people they are for
     */
    static int peopleCount(Collection<? extends TrainingPurchase> purchases) {
        Set<UUID> people = new HashSet<>();
        for (TrainingPurchase purchase : purchases) {
            people.add(purchase.person().getId());
        }
        return people.size();
    }

    /**
     * Raises a skill by one level, or learns it at level 0 if the person doesn't have it.
     *
     * @param person    the person
     * @param skillName the skill
     */
    record SkillImprovement(Person person, String skillName) implements TrainingPurchase {}

    /**
     * Buys a Natural Aptitude in a skill the person already has.
     *
     * @param person    the person
     * @param skillName the skill
     */
    record NaturalAptitude(Person person, String skillName) implements TrainingPurchase {}

    /**
     * Gains a special ability. Taking a flaw this way gives the person XP instead of costing it.
     *
     * @param person      the person
     * @param abilityName the ability's lookup name
     * @param choice      the chosen value for an ability that needs one, such as Weapon Specialist's weapon, or
     *                    {@code null} for an ability that doesn't
     */
    record AbilityPurchase(Person person, String abilityName, @Nullable String choice) implements TrainingPurchase {}

    /**
     * Pays XP to remove a flaw the person has.
     *
     * @param person      the person
     * @param abilityName the flaw's lookup name
     */
    record FlawBuyOff(Person person, String abilityName) implements TrainingPurchase {}

    /**
     * Raises an attribute, Edge included, by one.
     *
     * @param person    the person
     * @param attribute the attribute
     */
    record AttributeIncrease(Person person, SkillAttribute attribute) implements TrainingPurchase {}

    /**
     * Moves a trait one step up or down. Some steps cost XP and others give it back, as the Spend XP menu has always
     * priced them.
     *
     * @param person the person
     * @param trait  the trait
     * @param delta  {@code 1} or {@code -1}
     */
    record TraitChange(Person person, ATOWTraits trait, int delta) implements TrainingPurchase {}

    /**
     * Refills the Edge the person has spent.
     *
     * @param person the person
     */
    record EdgeRefill(Person person) implements TrainingPurchase {}
}
