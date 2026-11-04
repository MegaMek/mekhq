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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nullable;
import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.utilities.spaUtilities.SpaUtilities;

/**
 * A person as they will be once the training staged for them is applied: their skill levels, abilities, attributes,
 * traits and remaining XP. Nothing here touches the real person; the projection only records the differences.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class PersonProjection {
    private final Person person;
    /** Copies of every skill training has changed or added, at their staged levels. */
    private final Map<String, Skill> skills = new HashMap<>();
    private final Set<String> aptitudes = new HashSet<>();
    /** Abilities gained, with their chosen values ({@code ""} for an ability without one), in the order staged. */
    private final Map<String, String> gainedAbilities = new LinkedHashMap<>();
    private final Set<String> removedFlaws = new HashSet<>();
    private final Map<SkillAttribute, Integer> attributeChanges = new EnumMap<>(SkillAttribute.class);
    private final Map<ATOWTraits, Integer> traitChanges = new EnumMap<>(ATOWTraits.class);
    private boolean isEdgeRefilled;
    /** The skills at their staged levels, built when first asked for and rebuilt after a skill changes. */
    private @Nullable Skills projectedSkills;
    private int xpSpent;
    private int purchaseCount;

    PersonProjection(Person person) {
        this.person = person;
    }

    public Person person() {
        return person;
    }

    /**
     * @return the person's XP once everything staged is paid for; flaws and some trait steps give XP back
     */
    public int availableXp() {
        return person.getXP() - xpSpent;
    }

    /** @return the XP everything staged for this person costs, less any it gives back */
    public int xpSpent() {
        return xpSpent;
    }

    /** @return how many purchases are staged for this person */
    public int purchaseCount() {
        return purchaseCount;
    }

    /**
     * @param skillName a skill
     *
     * @return the skill at its staged level, or {@code null} if the person won't have it
     */
    public @Nullable Skill skill(String skillName) {
        Skill staged = skills.get(skillName);
        return (staged != null) ? staged : person.getSkill(skillName);
    }

    /**
     * @param skillName a skill
     *
     * @return the skill's staged level, or {@code -1} if the person won't have it
     */
    public int skillLevel(String skillName) {
        Skill skill = skill(skillName);
        return (skill == null) ? -1 : skill.getLevel();
    }

    /** @return {@code true} if training has changed or added this skill */
    public boolean isSkillChanged(String skillName) {
        return skills.containsKey(skillName);
    }

    /**
     * @return the person's skills at their staged levels, for checking ability prerequisites
     */
    public Skills projectedSkills() {
        if (projectedSkills != null) {
            return projectedSkills;
        }
        Skills projected = new Skills();
        for (Skill skill : person.getSkills().getSkills()) {
            String name = skill.getType().getName();
            projected.addSkill(name, skills.getOrDefault(name, skill));
        }
        for (Map.Entry<String, Skill> entry : skills.entrySet()) {
            if (!projected.hasSkill(entry.getKey())) {
                projected.addSkill(entry.getKey(), entry.getValue());
            }
        }
        projectedSkills = projected;
        return projected;
    }

    /** @return {@code true} if the person has, or will have, a Natural Aptitude in this skill */
    public boolean hasAptitude(String skillName) {
        if (aptitudes.contains(skillName)) {
            return true;
        }
        Skill skill = person.getSkill(skillName);
        return (skill != null) && skill.getHasNaturalAptitude();
    }

    /**
     * @param abilityName an ability's lookup name
     *
     * @return {@code true} if the person has the ability and won't lose it, or will gain it
     */
    public boolean hasAbility(String abilityName) {
        if (gainedAbilities.containsKey(abilityName)) {
            return true;
        }
        return !removedFlaws.contains(abilityName) && person.getOptions().booleanOption(abilityName);
    }

    /** @return {@code true} if the person has this flaw now and nothing staged removes it */
    public boolean hasFlawToBuyOff(String abilityName) {
        return !removedFlaws.contains(abilityName) && person.getOptions().booleanOption(abilityName);
    }

    /** @return {@code true} if training will add this ability */
    public boolean isAbilityGained(String abilityName) {
        return gainedAbilities.containsKey(abilityName);
    }

    /** @return the abilities training will add, with their chosen values, in the order staged */
    public Map<String, String> gainedAbilities() {
        return gainedAbilities;
    }

    /**
     * @return how many abilities that aren't flaws the person will have, which sets the next ability's training fee
     *       and is checked against the ability cap
     */
    public int nonFlawAbilityCount() {
        int count = SpaUtilities.countNonFlawSpas(person);
        for (String name : gainedAbilities.keySet()) {
            SpecialAbility ability = SpecialAbility.getAbility(name);
            if ((ability != null) && !SpaUtilities.isFlaw(ability)) {
                count++;
            }
        }
        return count;
    }

    /** @return how much training will raise this attribute */
    public int attributeChange(SkillAttribute attribute) {
        return attributeChanges.getOrDefault(attribute, 0);
    }

    /** @return how much training will move this trait */
    public int traitChange(ATOWTraits trait) {
        return traitChanges.getOrDefault(trait, 0);
    }

    /** @return {@code true} if a refill of the person's spent Edge is staged */
    public boolean isEdgeRefilled() {
        return isEdgeRefilled;
    }

    // region Recording staged purchases

    void improveSkill(String skillName, Skill improved, int xp) {
        skills.put(skillName, improved);
        projectedSkills = null;
        spend(xp);
    }

    void gainAptitude(String skillName, int xp) {
        aptitudes.add(skillName);
        spend(xp);
    }

    void gainAbility(String abilityName, @Nullable String choice, int xp) {
        gainedAbilities.put(abilityName, (choice == null) ? "" : choice);
        spend(xp);
    }

    void removeFlaw(String abilityName, int xp) {
        removedFlaws.add(abilityName);
        spend(xp);
    }

    void changeAttribute(SkillAttribute attribute, int xp) {
        attributeChanges.merge(attribute, 1, Integer::sum);
        spend(xp);
    }

    void changeTrait(ATOWTraits trait, int delta, int xp) {
        traitChanges.merge(trait, delta, Integer::sum);
        spend(xp);
    }

    void refillEdge(int xp) {
        isEdgeRefilled = true;
        spend(xp);
    }

    private void spend(int xp) {
        xpSpent += xp;
        purchaseCount++;
    }

    // endregion Recording staged purchases
}
