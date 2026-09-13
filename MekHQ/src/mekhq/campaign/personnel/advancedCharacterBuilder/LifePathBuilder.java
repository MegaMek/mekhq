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
package mekhq.campaign.personnel.advancedCharacterBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.Version;
import mekhq.MHQConstants;
import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;

/**
 * Assembles a {@link LifePath} by name rather than by position.
 *
 * <p>{@code LifePath} has 55 components, and many neighbours share a type, so building one by listing every
 * argument in order is a standing hazard: inserting a component anywhere but the end silently shifts the arguments
 * after it past the compiler. This builder removes that, and gives the Life Path wizard somewhere to hold a
 * half-finished Life Path, which the record itself cannot do because its constructor rejects the very values the
 * wizard needs to report back to the author.</p>
 *
 * <p>Every collection starts empty rather than null, so a caller only sets what it actually has.</p>
 *
 * <p>Example: taking an existing Life Path and stamping it with the current version is
 * {@code LifePathBuilder.from(existing).version(MHQConstants.VERSION).build()}, with no need to name the other 54
 * components at all.</p>
 *
 * @author Illiani
 * @since 0.50.11
 */
public class LifePathBuilder {

    // Dynamic
    private UUID id;
    private Version version;
    private Integer xpCost;

    // Basic Info
    private String source;
    private String name;
    private String flavorText;
    private Integer age;
    private Integer xpDiscount;
    private Integer minimumYear;
    private Integer maximumYear;
    private Double randomWeight;
    private Set<ATOWLifeStage> lifeStages = new HashSet<>();
    private Set<LifePathCategory> categories = new HashSet<>();
    private Boolean isPlayerRestricted;

    // Requirements
    private Map<Integer, Set<String>> requirementsFactions = new HashMap<>();
    private Map<Integer, Set<String>> requirementsSystems = new HashMap<>();
    private Map<Integer, Set<UUID>> requirementsLifePath = new HashMap<>();
    private Map<Integer, Map<LifePathCategory, Integer>> requirementsCategories = new HashMap<>();
    private Map<Integer, Map<SkillAttribute, Integer>> requirementsAttributes = new HashMap<>();
    private Map<Integer, Integer> requirementsEdge = new HashMap<>();
    private Map<Integer, Integer> requirementsFlexibleAttribute = new HashMap<>();
    private Map<Integer, Map<ATOWTraits, Integer>> requirementsTraits = new HashMap<>();
    private Map<Integer, Map<String, Integer>> requirementsSkills = new HashMap<>();
    private Map<Integer, Map<SkillSubType, Integer>> requirementsMetaSkills = new HashMap<>();
    private Map<Integer, Map<String, Integer>> requirementsAbilities = new HashMap<>();

    // Exclusions
    private Map<Integer, Set<String>> exclusionsFactions = new HashMap<>();
    private Map<Integer, Set<String>> exclusionsSystems = new HashMap<>();
    private Map<Integer, Set<UUID>> exclusionsLifePath = new HashMap<>();
    private Map<Integer, Map<LifePathCategory, Integer>> exclusionsCategories = new HashMap<>();
    private Map<Integer, Map<SkillAttribute, Integer>> exclusionsAttributes = new HashMap<>();
    private Map<Integer, Integer> exclusionsEdge = new HashMap<>();
    private Map<Integer, Integer> exclusionsFlexibleAttribute = new HashMap<>();
    private Map<Integer, Map<ATOWTraits, Integer>> exclusionsTraits = new HashMap<>();
    private Map<Integer, Map<String, Integer>> exclusionsSkills = new HashMap<>();
    private Map<Integer, Map<SkillSubType, Integer>> exclusionsMetaSkills = new HashMap<>();
    private Map<Integer, Map<String, Integer>> exclusionsAbilities = new HashMap<>();

    // Fixed XP
    private Map<Integer, Map<SkillAttribute, Integer>> fixedXPAttributes = new HashMap<>();
    private Map<Integer, Integer> fixedXPEdge = new HashMap<>();
    private Map<Integer, Integer> fixedXPFlexibleAttribute = new HashMap<>();
    private Map<Integer, Map<ATOWTraits, Integer>> fixedXPTraits = new HashMap<>();
    private Map<Integer, Map<String, Integer>> fixedXPSkills = new HashMap<>();
    private Map<Integer, Map<SkillSubType, Integer>> fixedXPMetaSkills = new HashMap<>();
    private Map<Integer, Map<String, Integer>> fixedXPNaturalAptitudes = new HashMap<>();
    private Map<Integer, Map<SkillSubType, Integer>> fixedXPNaturalAptitudesMetaSkills = new HashMap<>();
    private Map<Integer, Map<String, Integer>> fixedXPAbilities = new HashMap<>();

    // Flexible XP
    private Map<Integer, Map<SkillAttribute, Integer>> flexibleXPAttributes = new HashMap<>();
    private Map<Integer, Integer> flexibleXPEdge = new HashMap<>();
    private Map<Integer, Integer> flexibleXPFlexibleAttribute = new HashMap<>();
    private Map<Integer, Map<ATOWTraits, Integer>> flexibleXPTraits = new HashMap<>();
    private Map<Integer, Map<String, Integer>> flexibleXPSkills = new HashMap<>();
    private Map<Integer, Map<SkillSubType, Integer>> flexibleXPMetaSkills = new HashMap<>();
    private Map<Integer, Map<String, Integer>> flexibleXPNaturalAptitudes = new HashMap<>();
    private Map<Integer, Map<SkillSubType, Integer>> flexibleXPNaturalAptitudesMetaSkills = new HashMap<>();
    private Map<Integer, Map<String, Integer>> flexibleXPAbilities = new HashMap<>();
    private Map<Integer, Integer> flexibleXPPickCounts = new HashMap<>();

    /**
     * Creates a builder with empty collections, the current version, and no other values set.
     *
     * @since 0.50.11
     */
    public LifePathBuilder() {
        this.version = MHQConstants.VERSION;
    }

    /**
     * Creates a builder holding everything from an existing Life Path.
     *
     * <p>The collections are shared with the source rather than copied. A {@link LifePath} is immutable, so this is
     * safe for reading; a caller that means to edit one should replace it outright through its setter.</p>
     *
     * @param source the Life Path to copy
     *
     * @return a builder primed with the source's values
     *
     * @since 0.50.11
     */
    public static LifePathBuilder from(LifePath source) {
        LifePathBuilder builder = new LifePathBuilder();

        builder.id = source.id();
        builder.version = source.version();
        builder.xpCost = source.xpCost();
        builder.source = source.source();
        builder.name = source.name();
        builder.flavorText = source.flavorText();
        builder.age = source.age();
        builder.xpDiscount = source.xpDiscount();
        builder.minimumYear = source.minimumYear();
        builder.maximumYear = source.maximumYear();
        builder.randomWeight = source.randomWeight();
        builder.lifeStages = source.lifeStages();
        builder.categories = source.categories();
        builder.isPlayerRestricted = source.isPlayerRestricted();
        builder.requirementsFactions = source.requirementsFactions();
        builder.requirementsSystems = source.requirementsSystems();
        builder.requirementsLifePath = source.requirementsLifePath();
        builder.requirementsCategories = source.requirementsCategories();
        builder.requirementsAttributes = source.requirementsAttributes();
        builder.requirementsEdge = source.requirementsEdge();
        builder.requirementsFlexibleAttribute = source.requirementsFlexibleAttribute();
        builder.requirementsTraits = source.requirementsTraits();
        builder.requirementsSkills = source.requirementsSkills();
        builder.requirementsMetaSkills = source.requirementsMetaSkills();
        builder.requirementsAbilities = source.requirementsAbilities();
        builder.exclusionsFactions = source.exclusionsFactions();
        builder.exclusionsSystems = source.exclusionsSystems();
        builder.exclusionsLifePath = source.exclusionsLifePath();
        builder.exclusionsCategories = source.exclusionsCategories();
        builder.exclusionsAttributes = source.exclusionsAttributes();
        builder.exclusionsEdge = source.exclusionsEdge();
        builder.exclusionsFlexibleAttribute = source.exclusionsFlexibleAttribute();
        builder.exclusionsTraits = source.exclusionsTraits();
        builder.exclusionsSkills = source.exclusionsSkills();
        builder.exclusionsMetaSkills = source.exclusionsMetaSkills();
        builder.exclusionsAbilities = source.exclusionsAbilities();
        builder.fixedXPAttributes = source.fixedXPAttributes();
        builder.fixedXPEdge = source.fixedXPEdge();
        builder.fixedXPFlexibleAttribute = source.fixedXPFlexibleAttribute();
        builder.fixedXPTraits = source.fixedXPTraits();
        builder.fixedXPSkills = source.fixedXPSkills();
        builder.fixedXPMetaSkills = source.fixedXPMetaSkills();
        builder.fixedXPNaturalAptitudes = source.fixedXPNaturalAptitudes();
        builder.fixedXPNaturalAptitudesMetaSkills = source.fixedXPNaturalAptitudesMetaSkills();
        builder.fixedXPAbilities = source.fixedXPAbilities();
        builder.flexibleXPAttributes = source.flexibleXPAttributes();
        builder.flexibleXPEdge = source.flexibleXPEdge();
        builder.flexibleXPFlexibleAttribute = source.flexibleXPFlexibleAttribute();
        builder.flexibleXPTraits = source.flexibleXPTraits();
        builder.flexibleXPSkills = source.flexibleXPSkills();
        builder.flexibleXPMetaSkills = source.flexibleXPMetaSkills();
        builder.flexibleXPNaturalAptitudes = source.flexibleXPNaturalAptitudes();
        builder.flexibleXPNaturalAptitudesMetaSkills = source.flexibleXPNaturalAptitudesMetaSkills();
        builder.flexibleXPAbilities = source.flexibleXPAbilities();
        builder.flexibleXPPickCounts = source.flexibleXPPickCounts();

        return builder;
    }

    /**
     * Builds the {@link LifePath}.
     *
     * <p>The record's own constructor does the validating, so this throws {@link IllegalArgumentException} when
     * anything required is missing or out of range. Call
     * {@link LifePathValidator#validate(LifePathBuilder)} first to report problems to an author instead.</p>
     *
     * @return the finished Life Path
     *
     * @since 0.50.11
     */
    public LifePath build() {
        return new LifePath(id,
              version,
              xpCost,
              source,
              name,
              flavorText,
              age,
              xpDiscount,
              minimumYear,
              maximumYear,
              randomWeight,
              lifeStages,
              categories,
              isPlayerRestricted,
              requirementsFactions,
              requirementsSystems,
              requirementsLifePath,
              requirementsCategories,
              requirementsAttributes,
              requirementsEdge,
              requirementsFlexibleAttribute,
              requirementsTraits,
              requirementsSkills,
              requirementsMetaSkills,
              requirementsAbilities,
              exclusionsFactions,
              exclusionsSystems,
              exclusionsLifePath,
              exclusionsCategories,
              exclusionsAttributes,
              exclusionsEdge,
              exclusionsFlexibleAttribute,
              exclusionsTraits,
              exclusionsSkills,
              exclusionsMetaSkills,
              exclusionsAbilities,
              fixedXPAttributes,
              fixedXPEdge,
              fixedXPFlexibleAttribute,
              fixedXPTraits,
              fixedXPSkills,
              fixedXPMetaSkills,
              fixedXPNaturalAptitudes,
              fixedXPNaturalAptitudesMetaSkills,
              fixedXPAbilities,
              flexibleXPAttributes,
              flexibleXPEdge,
              flexibleXPFlexibleAttribute,
              flexibleXPTraits,
              flexibleXPSkills,
              flexibleXPMetaSkills,
              flexibleXPNaturalAptitudes,
              flexibleXPNaturalAptitudesMetaSkills,
              flexibleXPAbilities,
              flexibleXPPickCounts);
    }

    /**
     * Sets {@code id}.
     *
     * @param id the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder id(UUID id) {
        this.id = id;
        return this;
    }
    /**
     * Sets {@code version}.
     *
     * @param version the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder version(Version version) {
        this.version = version;
        return this;
    }
    /**
     * Sets {@code xpCost}.
     *
     * @param xpCost the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder xpCost(Integer xpCost) {
        this.xpCost = xpCost;
        return this;
    }
    /**
     * Sets {@code source}.
     *
     * @param source the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder source(String source) {
        this.source = source;
        return this;
    }
    /**
     * Sets {@code name}.
     *
     * @param name the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder name(String name) {
        this.name = name;
        return this;
    }
    /**
     * Sets {@code flavorText}.
     *
     * @param flavorText the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder flavorText(String flavorText) {
        this.flavorText = flavorText;
        return this;
    }
    /**
     * Sets {@code age}.
     *
     * @param age the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder age(Integer age) {
        this.age = age;
        return this;
    }
    /**
     * Sets {@code xpDiscount}.
     *
     * @param xpDiscount the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder xpDiscount(Integer xpDiscount) {
        this.xpDiscount = xpDiscount;
        return this;
    }
    /**
     * Sets {@code minimumYear}.
     *
     * @param minimumYear the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder minimumYear(Integer minimumYear) {
        this.minimumYear = minimumYear;
        return this;
    }
    /**
     * Sets {@code maximumYear}.
     *
     * @param maximumYear the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder maximumYear(Integer maximumYear) {
        this.maximumYear = maximumYear;
        return this;
    }
    /**
     * Sets {@code randomWeight}.
     *
     * @param randomWeight the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder randomWeight(Double randomWeight) {
        this.randomWeight = randomWeight;
        return this;
    }
    /**
     * Sets {@code lifeStages}.
     *
     * @param lifeStages the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder lifeStages(Set<ATOWLifeStage> lifeStages) {
        this.lifeStages = lifeStages;
        return this;
    }
    /**
     * Sets {@code categories}.
     *
     * @param categories the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder categories(Set<LifePathCategory> categories) {
        this.categories = categories;
        return this;
    }
    /**
     * Sets {@code isPlayerRestricted}.
     *
     * @param isPlayerRestricted the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder isPlayerRestricted(Boolean isPlayerRestricted) {
        this.isPlayerRestricted = isPlayerRestricted;
        return this;
    }
    /**
     * Sets {@code requirementsFactions}.
     *
     * @param requirementsFactions the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder requirementsFactions(Map<Integer, Set<String>> requirementsFactions) {
        this.requirementsFactions = requirementsFactions;
        return this;
    }
    /**
     * Sets {@code requirementsSystems}.
     *
     * @param requirementsSystems the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder requirementsSystems(Map<Integer, Set<String>> requirementsSystems) {
        this.requirementsSystems = requirementsSystems;
        return this;
    }
    /**
     * Sets {@code requirementsLifePath}.
     *
     * @param requirementsLifePath the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder requirementsLifePath(Map<Integer, Set<UUID>> requirementsLifePath) {
        this.requirementsLifePath = requirementsLifePath;
        return this;
    }
    /**
     * Sets {@code requirementsCategories}.
     *
     * @param requirementsCategories the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder requirementsCategories(Map<Integer, Map<LifePathCategory, Integer>> requirementsCategories) {
        this.requirementsCategories = requirementsCategories;
        return this;
    }
    /**
     * Sets {@code requirementsAttributes}.
     *
     * @param requirementsAttributes the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder requirementsAttributes(Map<Integer, Map<SkillAttribute, Integer>> requirementsAttributes) {
        this.requirementsAttributes = requirementsAttributes;
        return this;
    }
    /**
     * Sets {@code requirementsEdge}.
     *
     * @param requirementsEdge the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder requirementsEdge(Map<Integer, Integer> requirementsEdge) {
        this.requirementsEdge = requirementsEdge;
        return this;
    }
    /**
     * Sets {@code requirementsFlexibleAttribute}.
     *
     * @param requirementsFlexibleAttribute the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder requirementsFlexibleAttribute(Map<Integer, Integer> requirementsFlexibleAttribute) {
        this.requirementsFlexibleAttribute = requirementsFlexibleAttribute;
        return this;
    }
    /**
     * Sets {@code requirementsTraits}.
     *
     * @param requirementsTraits the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder requirementsTraits(Map<Integer, Map<ATOWTraits, Integer>> requirementsTraits) {
        this.requirementsTraits = requirementsTraits;
        return this;
    }
    /**
     * Sets {@code requirementsSkills}.
     *
     * @param requirementsSkills the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder requirementsSkills(Map<Integer, Map<String, Integer>> requirementsSkills) {
        this.requirementsSkills = requirementsSkills;
        return this;
    }
    /**
     * Sets {@code requirementsMetaSkills}.
     *
     * @param requirementsMetaSkills the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder requirementsMetaSkills(Map<Integer, Map<SkillSubType, Integer>> requirementsMetaSkills) {
        this.requirementsMetaSkills = requirementsMetaSkills;
        return this;
    }
    /**
     * Sets {@code requirementsAbilities}.
     *
     * @param requirementsAbilities the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder requirementsAbilities(Map<Integer, Map<String, Integer>> requirementsAbilities) {
        this.requirementsAbilities = requirementsAbilities;
        return this;
    }
    /**
     * Sets {@code exclusionsFactions}.
     *
     * @param exclusionsFactions the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder exclusionsFactions(Map<Integer, Set<String>> exclusionsFactions) {
        this.exclusionsFactions = exclusionsFactions;
        return this;
    }
    /**
     * Sets {@code exclusionsSystems}.
     *
     * @param exclusionsSystems the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder exclusionsSystems(Map<Integer, Set<String>> exclusionsSystems) {
        this.exclusionsSystems = exclusionsSystems;
        return this;
    }
    /**
     * Sets {@code exclusionsLifePath}.
     *
     * @param exclusionsLifePath the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder exclusionsLifePath(Map<Integer, Set<UUID>> exclusionsLifePath) {
        this.exclusionsLifePath = exclusionsLifePath;
        return this;
    }
    /**
     * Sets {@code exclusionsCategories}.
     *
     * @param exclusionsCategories the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder exclusionsCategories(Map<Integer, Map<LifePathCategory, Integer>> exclusionsCategories) {
        this.exclusionsCategories = exclusionsCategories;
        return this;
    }
    /**
     * Sets {@code exclusionsAttributes}.
     *
     * @param exclusionsAttributes the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder exclusionsAttributes(Map<Integer, Map<SkillAttribute, Integer>> exclusionsAttributes) {
        this.exclusionsAttributes = exclusionsAttributes;
        return this;
    }
    /**
     * Sets {@code exclusionsEdge}.
     *
     * @param exclusionsEdge the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder exclusionsEdge(Map<Integer, Integer> exclusionsEdge) {
        this.exclusionsEdge = exclusionsEdge;
        return this;
    }
    /**
     * Sets {@code exclusionsFlexibleAttribute}.
     *
     * @param exclusionsFlexibleAttribute the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder exclusionsFlexibleAttribute(Map<Integer, Integer> exclusionsFlexibleAttribute) {
        this.exclusionsFlexibleAttribute = exclusionsFlexibleAttribute;
        return this;
    }
    /**
     * Sets {@code exclusionsTraits}.
     *
     * @param exclusionsTraits the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder exclusionsTraits(Map<Integer, Map<ATOWTraits, Integer>> exclusionsTraits) {
        this.exclusionsTraits = exclusionsTraits;
        return this;
    }
    /**
     * Sets {@code exclusionsSkills}.
     *
     * @param exclusionsSkills the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder exclusionsSkills(Map<Integer, Map<String, Integer>> exclusionsSkills) {
        this.exclusionsSkills = exclusionsSkills;
        return this;
    }
    /**
     * Sets {@code exclusionsMetaSkills}.
     *
     * @param exclusionsMetaSkills the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder exclusionsMetaSkills(Map<Integer, Map<SkillSubType, Integer>> exclusionsMetaSkills) {
        this.exclusionsMetaSkills = exclusionsMetaSkills;
        return this;
    }
    /**
     * Sets {@code exclusionsAbilities}.
     *
     * @param exclusionsAbilities the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder exclusionsAbilities(Map<Integer, Map<String, Integer>> exclusionsAbilities) {
        this.exclusionsAbilities = exclusionsAbilities;
        return this;
    }
    /**
     * Sets {@code fixedXPAttributes}.
     *
     * @param fixedXPAttributes the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder fixedXPAttributes(Map<Integer, Map<SkillAttribute, Integer>> fixedXPAttributes) {
        this.fixedXPAttributes = fixedXPAttributes;
        return this;
    }
    /**
     * Sets {@code fixedXPEdge}.
     *
     * @param fixedXPEdge the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder fixedXPEdge(Map<Integer, Integer> fixedXPEdge) {
        this.fixedXPEdge = fixedXPEdge;
        return this;
    }
    /**
     * Sets {@code fixedXPFlexibleAttribute}.
     *
     * @param fixedXPFlexibleAttribute the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder fixedXPFlexibleAttribute(Map<Integer, Integer> fixedXPFlexibleAttribute) {
        this.fixedXPFlexibleAttribute = fixedXPFlexibleAttribute;
        return this;
    }
    /**
     * Sets {@code fixedXPTraits}.
     *
     * @param fixedXPTraits the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder fixedXPTraits(Map<Integer, Map<ATOWTraits, Integer>> fixedXPTraits) {
        this.fixedXPTraits = fixedXPTraits;
        return this;
    }
    /**
     * Sets {@code fixedXPSkills}.
     *
     * @param fixedXPSkills the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder fixedXPSkills(Map<Integer, Map<String, Integer>> fixedXPSkills) {
        this.fixedXPSkills = fixedXPSkills;
        return this;
    }
    /**
     * Sets {@code fixedXPMetaSkills}.
     *
     * @param fixedXPMetaSkills the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder fixedXPMetaSkills(Map<Integer, Map<SkillSubType, Integer>> fixedXPMetaSkills) {
        this.fixedXPMetaSkills = fixedXPMetaSkills;
        return this;
    }
    /**
     * Sets {@code fixedXPNaturalAptitudes}.
     *
     * @param fixedXPNaturalAptitudes the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder fixedXPNaturalAptitudes(Map<Integer, Map<String, Integer>> fixedXPNaturalAptitudes) {
        this.fixedXPNaturalAptitudes = fixedXPNaturalAptitudes;
        return this;
    }
    /**
     * Sets {@code fixedXPNaturalAptitudesMetaSkills}.
     *
     * @param fixedXPNaturalAptitudesMetaSkills the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder fixedXPNaturalAptitudesMetaSkills(Map<Integer, Map<SkillSubType, Integer>> fixedXPNaturalAptitudesMetaSkills) {
        this.fixedXPNaturalAptitudesMetaSkills = fixedXPNaturalAptitudesMetaSkills;
        return this;
    }
    /**
     * Sets {@code fixedXPAbilities}.
     *
     * @param fixedXPAbilities the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder fixedXPAbilities(Map<Integer, Map<String, Integer>> fixedXPAbilities) {
        this.fixedXPAbilities = fixedXPAbilities;
        return this;
    }
    /**
     * Sets {@code flexibleXPAttributes}.
     *
     * @param flexibleXPAttributes the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder flexibleXPAttributes(Map<Integer, Map<SkillAttribute, Integer>> flexibleXPAttributes) {
        this.flexibleXPAttributes = flexibleXPAttributes;
        return this;
    }
    /**
     * Sets {@code flexibleXPEdge}.
     *
     * @param flexibleXPEdge the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder flexibleXPEdge(Map<Integer, Integer> flexibleXPEdge) {
        this.flexibleXPEdge = flexibleXPEdge;
        return this;
    }
    /**
     * Sets {@code flexibleXPFlexibleAttribute}.
     *
     * @param flexibleXPFlexibleAttribute the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder flexibleXPFlexibleAttribute(Map<Integer, Integer> flexibleXPFlexibleAttribute) {
        this.flexibleXPFlexibleAttribute = flexibleXPFlexibleAttribute;
        return this;
    }
    /**
     * Sets {@code flexibleXPTraits}.
     *
     * @param flexibleXPTraits the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder flexibleXPTraits(Map<Integer, Map<ATOWTraits, Integer>> flexibleXPTraits) {
        this.flexibleXPTraits = flexibleXPTraits;
        return this;
    }
    /**
     * Sets {@code flexibleXPSkills}.
     *
     * @param flexibleXPSkills the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder flexibleXPSkills(Map<Integer, Map<String, Integer>> flexibleXPSkills) {
        this.flexibleXPSkills = flexibleXPSkills;
        return this;
    }
    /**
     * Sets {@code flexibleXPMetaSkills}.
     *
     * @param flexibleXPMetaSkills the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder flexibleXPMetaSkills(Map<Integer, Map<SkillSubType, Integer>> flexibleXPMetaSkills) {
        this.flexibleXPMetaSkills = flexibleXPMetaSkills;
        return this;
    }
    /**
     * Sets {@code flexibleXPNaturalAptitudes}.
     *
     * @param flexibleXPNaturalAptitudes the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder flexibleXPNaturalAptitudes(Map<Integer, Map<String, Integer>> flexibleXPNaturalAptitudes) {
        this.flexibleXPNaturalAptitudes = flexibleXPNaturalAptitudes;
        return this;
    }
    /**
     * Sets {@code flexibleXPNaturalAptitudesMetaSkills}.
     *
     * @param flexibleXPNaturalAptitudesMetaSkills the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder flexibleXPNaturalAptitudesMetaSkills(Map<Integer, Map<SkillSubType, Integer>> flexibleXPNaturalAptitudesMetaSkills) {
        this.flexibleXPNaturalAptitudesMetaSkills = flexibleXPNaturalAptitudesMetaSkills;
        return this;
    }
    /**
     * Sets {@code flexibleXPAbilities}.
     *
     * @param flexibleXPAbilities the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder flexibleXPAbilities(Map<Integer, Map<String, Integer>> flexibleXPAbilities) {
        this.flexibleXPAbilities = flexibleXPAbilities;
        return this;
    }
    /**
     * Sets {@code flexibleXPPickCounts}, how many items the player takes from each flexible XP set, keyed by set
     * index.
     *
     * @param flexibleXPPickCounts the value to use
     *
     * @return this builder
     *
     * @since 0.50.11
     */
    public LifePathBuilder flexibleXPPickCounts(Map<Integer, Integer> flexibleXPPickCounts) {
        this.flexibleXPPickCounts = flexibleXPPickCounts;
        return this;
    }

    /**
     * @return the current value of {@code id}
     */
    public UUID id() {
        return id;
    }
    /**
     * @return the current value of {@code version}
     */
    public Version version() {
        return version;
    }
    /**
     * @return the current value of {@code xpCost}
     */
    public Integer xpCost() {
        return xpCost;
    }
    /**
     * @return the current value of {@code source}
     */
    public String source() {
        return source;
    }
    /**
     * @return the current value of {@code name}
     */
    public String name() {
        return name;
    }
    /**
     * @return the current value of {@code flavorText}
     */
    public String flavorText() {
        return flavorText;
    }
    /**
     * @return the current value of {@code age}
     */
    public Integer age() {
        return age;
    }
    /**
     * @return the current value of {@code xpDiscount}
     */
    public Integer xpDiscount() {
        return xpDiscount;
    }
    /**
     * @return the current value of {@code minimumYear}
     */
    public Integer minimumYear() {
        return minimumYear;
    }
    /**
     * @return the current value of {@code maximumYear}
     */
    public Integer maximumYear() {
        return maximumYear;
    }
    /**
     * @return the current value of {@code randomWeight}
     */
    public Double randomWeight() {
        return randomWeight;
    }
    /**
     * @return the current value of {@code lifeStages}
     */
    public Set<ATOWLifeStage> lifeStages() {
        return lifeStages;
    }
    /**
     * @return the current value of {@code categories}
     */
    public Set<LifePathCategory> categories() {
        return categories;
    }
    /**
     * @return the current value of {@code isPlayerRestricted}
     */
    public Boolean isPlayerRestricted() {
        return isPlayerRestricted;
    }
    /**
     * @return the current value of {@code requirementsFactions}
     */
    public Map<Integer, Set<String>> requirementsFactions() {
        return requirementsFactions;
    }
    /**
     * @return the current value of {@code requirementsSystems}
     */
    public Map<Integer, Set<String>> requirementsSystems() {
        return requirementsSystems;
    }
    /**
     * @return the current value of {@code requirementsLifePath}
     */
    public Map<Integer, Set<UUID>> requirementsLifePath() {
        return requirementsLifePath;
    }
    /**
     * @return the current value of {@code requirementsCategories}
     */
    public Map<Integer, Map<LifePathCategory, Integer>> requirementsCategories() {
        return requirementsCategories;
    }
    /**
     * @return the current value of {@code requirementsAttributes}
     */
    public Map<Integer, Map<SkillAttribute, Integer>> requirementsAttributes() {
        return requirementsAttributes;
    }
    /**
     * @return the current value of {@code requirementsEdge}
     */
    public Map<Integer, Integer> requirementsEdge() {
        return requirementsEdge;
    }
    /**
     * @return the current value of {@code requirementsFlexibleAttribute}
     */
    public Map<Integer, Integer> requirementsFlexibleAttribute() {
        return requirementsFlexibleAttribute;
    }
    /**
     * @return the current value of {@code requirementsTraits}
     */
    public Map<Integer, Map<ATOWTraits, Integer>> requirementsTraits() {
        return requirementsTraits;
    }
    /**
     * @return the current value of {@code requirementsSkills}
     */
    public Map<Integer, Map<String, Integer>> requirementsSkills() {
        return requirementsSkills;
    }
    /**
     * @return the current value of {@code requirementsMetaSkills}
     */
    public Map<Integer, Map<SkillSubType, Integer>> requirementsMetaSkills() {
        return requirementsMetaSkills;
    }
    /**
     * @return the current value of {@code requirementsAbilities}
     */
    public Map<Integer, Map<String, Integer>> requirementsAbilities() {
        return requirementsAbilities;
    }
    /**
     * @return the current value of {@code exclusionsFactions}
     */
    public Map<Integer, Set<String>> exclusionsFactions() {
        return exclusionsFactions;
    }
    /**
     * @return the current value of {@code exclusionsSystems}
     */
    public Map<Integer, Set<String>> exclusionsSystems() {
        return exclusionsSystems;
    }
    /**
     * @return the current value of {@code exclusionsLifePath}
     */
    public Map<Integer, Set<UUID>> exclusionsLifePath() {
        return exclusionsLifePath;
    }
    /**
     * @return the current value of {@code exclusionsCategories}
     */
    public Map<Integer, Map<LifePathCategory, Integer>> exclusionsCategories() {
        return exclusionsCategories;
    }
    /**
     * @return the current value of {@code exclusionsAttributes}
     */
    public Map<Integer, Map<SkillAttribute, Integer>> exclusionsAttributes() {
        return exclusionsAttributes;
    }
    /**
     * @return the current value of {@code exclusionsEdge}
     */
    public Map<Integer, Integer> exclusionsEdge() {
        return exclusionsEdge;
    }
    /**
     * @return the current value of {@code exclusionsFlexibleAttribute}
     */
    public Map<Integer, Integer> exclusionsFlexibleAttribute() {
        return exclusionsFlexibleAttribute;
    }
    /**
     * @return the current value of {@code exclusionsTraits}
     */
    public Map<Integer, Map<ATOWTraits, Integer>> exclusionsTraits() {
        return exclusionsTraits;
    }
    /**
     * @return the current value of {@code exclusionsSkills}
     */
    public Map<Integer, Map<String, Integer>> exclusionsSkills() {
        return exclusionsSkills;
    }
    /**
     * @return the current value of {@code exclusionsMetaSkills}
     */
    public Map<Integer, Map<SkillSubType, Integer>> exclusionsMetaSkills() {
        return exclusionsMetaSkills;
    }
    /**
     * @return the current value of {@code exclusionsAbilities}
     */
    public Map<Integer, Map<String, Integer>> exclusionsAbilities() {
        return exclusionsAbilities;
    }
    /**
     * @return the current value of {@code fixedXPAttributes}
     */
    public Map<Integer, Map<SkillAttribute, Integer>> fixedXPAttributes() {
        return fixedXPAttributes;
    }
    /**
     * @return the current value of {@code fixedXPEdge}
     */
    public Map<Integer, Integer> fixedXPEdge() {
        return fixedXPEdge;
    }
    /**
     * @return the current value of {@code fixedXPFlexibleAttribute}
     */
    public Map<Integer, Integer> fixedXPFlexibleAttribute() {
        return fixedXPFlexibleAttribute;
    }
    /**
     * @return the current value of {@code fixedXPTraits}
     */
    public Map<Integer, Map<ATOWTraits, Integer>> fixedXPTraits() {
        return fixedXPTraits;
    }
    /**
     * @return the current value of {@code fixedXPSkills}
     */
    public Map<Integer, Map<String, Integer>> fixedXPSkills() {
        return fixedXPSkills;
    }
    /**
     * @return the current value of {@code fixedXPMetaSkills}
     */
    public Map<Integer, Map<SkillSubType, Integer>> fixedXPMetaSkills() {
        return fixedXPMetaSkills;
    }
    /**
     * @return the current value of {@code fixedXPNaturalAptitudes}
     */
    public Map<Integer, Map<String, Integer>> fixedXPNaturalAptitudes() {
        return fixedXPNaturalAptitudes;
    }
    /**
     * @return the current value of {@code fixedXPNaturalAptitudesMetaSkills}
     */
    public Map<Integer, Map<SkillSubType, Integer>> fixedXPNaturalAptitudesMetaSkills() {
        return fixedXPNaturalAptitudesMetaSkills;
    }
    /**
     * @return the current value of {@code fixedXPAbilities}
     */
    public Map<Integer, Map<String, Integer>> fixedXPAbilities() {
        return fixedXPAbilities;
    }
    /**
     * @return the current value of {@code flexibleXPAttributes}
     */
    public Map<Integer, Map<SkillAttribute, Integer>> flexibleXPAttributes() {
        return flexibleXPAttributes;
    }
    /**
     * @return the current value of {@code flexibleXPEdge}
     */
    public Map<Integer, Integer> flexibleXPEdge() {
        return flexibleXPEdge;
    }
    /**
     * @return the current value of {@code flexibleXPFlexibleAttribute}
     */
    public Map<Integer, Integer> flexibleXPFlexibleAttribute() {
        return flexibleXPFlexibleAttribute;
    }
    /**
     * @return the current value of {@code flexibleXPTraits}
     */
    public Map<Integer, Map<ATOWTraits, Integer>> flexibleXPTraits() {
        return flexibleXPTraits;
    }
    /**
     * @return the current value of {@code flexibleXPSkills}
     */
    public Map<Integer, Map<String, Integer>> flexibleXPSkills() {
        return flexibleXPSkills;
    }
    /**
     * @return the current value of {@code flexibleXPMetaSkills}
     */
    public Map<Integer, Map<SkillSubType, Integer>> flexibleXPMetaSkills() {
        return flexibleXPMetaSkills;
    }
    /**
     * @return the current value of {@code flexibleXPNaturalAptitudes}
     */
    public Map<Integer, Map<String, Integer>> flexibleXPNaturalAptitudes() {
        return flexibleXPNaturalAptitudes;
    }
    /**
     * @return the current value of {@code flexibleXPNaturalAptitudesMetaSkills}
     */
    public Map<Integer, Map<SkillSubType, Integer>> flexibleXPNaturalAptitudesMetaSkills() {
        return flexibleXPNaturalAptitudesMetaSkills;
    }
    /**
     * @return the current value of {@code flexibleXPAbilities}
     */
    public Map<Integer, Map<String, Integer>> flexibleXPAbilities() {
        return flexibleXPAbilities;
    }
    /**
     * @return the current value of {@code flexibleXPPickCounts}
     */
    public Map<Integer, Integer> flexibleXPPickCounts() {
        return flexibleXPPickCounts;
    }
}
