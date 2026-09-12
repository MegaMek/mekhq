/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;

/**
 * Everything one group of a Life Path section holds.
 *
 * <p>A section of the Life Path wizard is made of numbered groups, and what the number means depends on the section.
 * In Requirements the groups are alternatives, so a character needs to satisfy one of them. In Flexible XP they are
 * the options a player picks between. Fixed XP and Exclusions have a single group.</p>
 *
 * <p>{@link LifePathTab} used to keep these thirteen things in thirteen parallel maps all keyed by the same group
 * index. Every operation on a group therefore had to be written thirteen times: removing one was 170 lines of
 * identical shift blocks, duplicating one was thirteen copy statements, and adding a fourteenth kind of entry meant
 * finding every one of those places. Holding a group in one object makes each of those operations a single
 * statement.</p>
 *
 * <p>{@code edge} and {@code flexibleAttribute} are single values and are {@code null} when the author has not set
 * one. That distinction matters: storing the "unset" number instead of null is what used to write an Edge rule into
 * every group nobody had touched.</p>
 *
 * @since 0.50.11
 */
class LifePathGroup {
    private Set<String> factions = new HashSet<>();
    private Set<String> systems = new HashSet<>();
    private Set<UUID> lifePaths = new HashSet<>();
    private Map<LifePathCategory, Integer> categories = new HashMap<>();
    private Map<SkillAttribute, Integer> attributes = new HashMap<>();
    private Integer edge;
    private Integer flexibleAttribute;
    private Map<ATOWTraits, Integer> traits = new HashMap<>();
    private Map<String, Integer> skills = new HashMap<>();
    private Map<SkillSubType, Integer> metaSkills = new HashMap<>();
    private Map<String, Integer> naturalAptitudes = new HashMap<>();
    private Map<SkillSubType, Integer> naturalAptitudesMetaSkills = new HashMap<>();
    private Map<String, Integer> abilities = new HashMap<>();

    /**
     * How many items the player takes from this set, or {@code null} when the section has no such notion.
     *
     * <p>Only Flexible XP sets carry one. It is the one thing a set holds that is not an award.</p>
     */
    private Integer pickCount;

    /**
     * Returns an independent copy of this group.
     *
     * <p>The collections are copied, not shared, so editing the copy cannot reach back into the original. This is what
     * the wizard's Duplicate Group button needs.</p>
     *
     * @return a copy holding the same contents
     *
     * @since 0.50.11
     */
    LifePathGroup copy() {
        LifePathGroup copy = new LifePathGroup();

        copy.factions = new HashSet<>(factions);
        copy.systems = new HashSet<>(systems);
        copy.lifePaths = new HashSet<>(lifePaths);
        copy.categories = new HashMap<>(categories);
        copy.attributes = new HashMap<>(attributes);
        copy.edge = edge;
        copy.flexibleAttribute = flexibleAttribute;
        copy.traits = new HashMap<>(traits);
        copy.skills = new HashMap<>(skills);
        copy.metaSkills = new HashMap<>(metaSkills);
        copy.naturalAptitudes = new HashMap<>(naturalAptitudes);
        copy.naturalAptitudesMetaSkills = new HashMap<>(naturalAptitudesMetaSkills);
        copy.abilities = new HashMap<>(abilities);
        copy.pickCount = pickCount;

        return copy;
    }

    /**
     * Counts the items a player could pick from this set.
     *
     * <p>Mirrors {@code LifePath.flexibleXPItemValues}: one per scored entry in every award map, plus one each for
     * Edge and the "any attribute" award when they are set. Factions, systems, Life Paths and categories are
     * requirements, not awards, and are not counted.</p>
     *
     * @return the number of pickable items
     *
     * @since 0.51.01
     */
    int countItems() {
        // Counted the same way the record counts them: an entry whose value is null is not an item. A hand-edited
        // file can leave one, and counting it here would let the spinner allow a pick the record then refuses.
        int itemCount = countAwards(attributes)
                              + countAwards(traits)
                              + countAwards(skills)
                              + countAwards(metaSkills)
                              + countAwards(naturalAptitudes)
                              + countAwards(naturalAptitudesMetaSkills)
                              + countAwards(abilities);

        if (edge != null) {
            itemCount++;
        }
        if (flexibleAttribute != null) {
            itemCount++;
        }

        return itemCount;
    }

    /**
     * Counts the entries of an award map that carry a value.
     *
     * @param awards awards keyed by whatever the map awards
     *
     * @return the number of entries whose value is not {@code null}
     *
     * @since 0.51.01
     */
    private static int countAwards(Map<?, Integer> awards) {
        int count = 0;

        for (Integer value : awards.values()) {
            if (value != null) {
                count++;
            }
        }

        return count;
    }

    /** @return how many items the player takes from this set, or {@code null} when not a flexible set */
    @Nullable
    Integer getPickCount() {
        return pickCount;
    }

    /** @param pickCount how many items the player takes from this set; {@code null} clears it */
    void setPickCount(@Nullable Integer pickCount) {
        this.pickCount = pickCount;
    }

    /** @return the faction codes this group names */
    Set<String> getFactions() {
        return factions;
    }

    /** @param factions the faction codes this group names */
    void setFactions(Set<String> factions) {
        this.factions = factions == null ? new HashSet<>() : factions;
    }

    /** @return the planetary system identifiers this group names */
    Set<String> getSystems() {
        return systems;
    }

    /** @param systems the planetary system identifiers this group names */
    void setSystems(Set<String> systems) {
        this.systems = systems == null ? new HashSet<>() : systems;
    }

    /** @return the identifiers of the other Life Paths this group names */
    Set<UUID> getLifePaths() {
        return lifePaths;
    }

    /** @param lifePaths the identifiers of the other Life Paths this group names */
    void setLifePaths(Set<UUID> lifePaths) {
        this.lifePaths = lifePaths == null ? new HashSet<>() : lifePaths;
    }

    /** @return how many Life Paths of each category this group calls for */
    Map<LifePathCategory, Integer> getCategories() {
        return categories;
    }

    /** @param categories how many Life Paths of each category this group calls for */
    void setCategories(Map<LifePathCategory, Integer> categories) {
        this.categories = categories == null ? new HashMap<>() : categories;
    }

    /** @return the per-attribute values this group holds */
    Map<SkillAttribute, Integer> getAttributes() {
        return attributes;
    }

    /** @param attributes the per-attribute values this group holds */
    void setAttributes(Map<SkillAttribute, Integer> attributes) {
        this.attributes = attributes == null ? new HashMap<>() : attributes;
    }

    /** @return this group's Edge value, or {@code null} when the author set none */
    @Nullable
    Integer getEdge() {
        return edge;
    }

    /** @param edge this group's Edge value, or {@code null} for none */
    void setEdge(@Nullable Integer edge) {
        this.edge = edge;
    }

    /** @return this group's "any attribute" value, or {@code null} when the author set none */
    @Nullable
    Integer getFlexibleAttribute() {
        return flexibleAttribute;
    }

    /** @param flexibleAttribute this group's "any attribute" value, or {@code null} for none */
    void setFlexibleAttribute(@Nullable Integer flexibleAttribute) {
        this.flexibleAttribute = flexibleAttribute;
    }

    /** @return the per-trait values this group holds */
    Map<ATOWTraits, Integer> getTraits() {
        return traits;
    }

    /** @param traits the per-trait values this group holds */
    void setTraits(Map<ATOWTraits, Integer> traits) {
        this.traits = traits == null ? new HashMap<>() : traits;
    }

    /** @return the per-skill values this group holds */
    Map<String, Integer> getSkills() {
        return skills;
    }

    /** @param skills the per-skill values this group holds */
    void setSkills(Map<String, Integer> skills) {
        this.skills = skills == null ? new HashMap<>() : skills;
    }

    /** @return the per-meta-skill values this group holds */
    Map<SkillSubType, Integer> getMetaSkills() {
        return metaSkills;
    }

    /** @param metaSkills the per-meta-skill values this group holds */
    void setMetaSkills(Map<SkillSubType, Integer> metaSkills) {
        this.metaSkills = metaSkills == null ? new HashMap<>() : metaSkills;
    }

    /** @return the per-skill natural aptitude values this group holds */
    Map<String, Integer> getNaturalAptitudes() {
        return naturalAptitudes;
    }

    /** @param naturalAptitudes the per-skill natural aptitude values this group holds */
    void setNaturalAptitudes(Map<String, Integer> naturalAptitudes) {
        this.naturalAptitudes = naturalAptitudes == null ? new HashMap<>() : naturalAptitudes;
    }

    /** @return the per-meta-skill natural aptitude values this group holds */
    Map<SkillSubType, Integer> getNaturalAptitudesMetaSkills() {
        return naturalAptitudesMetaSkills;
    }

    /** @param naturalAptitudesMetaSkills the per-meta-skill natural aptitude values this group holds */
    void setNaturalAptitudesMetaSkills(Map<SkillSubType, Integer> naturalAptitudesMetaSkills) {
        this.naturalAptitudesMetaSkills = naturalAptitudesMetaSkills == null
              ? new HashMap<>()
              : naturalAptitudesMetaSkills;
    }

    /** @return the per-ability values this group holds */
    Map<String, Integer> getAbilities() {
        return abilities;
    }

    /** @param abilities the per-ability values this group holds */
    void setAbilities(Map<String, Integer> abilities) {
        this.abilities = abilities == null ? new HashMap<>() : abilities;
    }
}
