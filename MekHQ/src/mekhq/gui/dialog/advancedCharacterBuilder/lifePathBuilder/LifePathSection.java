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
import java.util.Map;
import java.util.Set;

import mekhq.campaign.personnel.advancedCharacterBuilder.LifePath;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilder;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;

/**
 * Moves one section of a Life Path between the wizard's tabs and a {@link LifePathBuilder}.
 *
 * <p>A Life Path is stored as four parallel sections, Requirements, Exclusions, Fixed XP and Flexible XP, and each
 * section has its own copy of up to eleven group maps on the record. The wizard holds each section in a
 * {@link LifePathTab}, which names those maps the same way whichever section it is serving. This class is the one
 * place that knows which tab map corresponds to which record component for a given section, so the dialog no longer
 * needs four near-identical blocks to save and four more to load.</p>
 *
 * <p>Requirements and Exclusions can name factions, planetary systems, other Life Paths and categories. The two XP
 * sections cannot, because they award things rather than test for them, and they carry natural aptitudes, which the
 * other two do not.</p>
 *
 * @since 0.50.11
 */
public final class LifePathSection {
    private LifePathSection() {
        // Utility class: not instantiable.
    }

    /**
     * Copies everything the author entered on a section's tab into the builder.
     *
     * <p>Empty groups are dropped on the way through. A group holding nothing contributes nothing to the Life Path,
     * and writing it out only fills the saved file with entries like {@code "requirementsSkills": &#123;"0": &#123;&#125;&#125;}
     * for every section the author never touched.</p>
     *
     * @param tabType the section being saved
     * @param tab     the tab holding that section
     * @param builder the builder to write into
     *
     * @since 0.50.11
     */
    public static void readFromTab(LifePathBuilderTabType tabType, LifePathTab tab, LifePathBuilder builder) {
        switch (tabType) {
            case REQUIREMENTS -> builder.requirementsFactions(stripEmptyEntries(tab.getFactions()))
                                       .requirementsSystems(stripEmptyEntries(tab.getSystems()))
                                       .requirementsLifePath(stripEmptyEntries(tab.getLifePaths()))
                                       .requirementsCategories(stripEmptyEntries(tab.getCategories()))
                                       .requirementsAttributes(stripEmptyEntries(tab.getAttributes()))
                                       .requirementsEdge(stripNullValues(tab.getEdge()))
                                       .requirementsFlexibleAttribute(stripNullValues(tab.getFlexibleAttribute()))
                                       .requirementsTraits(stripEmptyEntries(tab.getTraits()))
                                       .requirementsSkills(stripEmptyEntries(tab.getSkills()))
                                       .requirementsMetaSkills(stripEmptyEntries(tab.getMetaSkills()))
                                       .requirementsAbilities(stripEmptyEntries(tab.getAbilities()));
            case EXCLUSIONS -> builder.exclusionsFactions(stripEmptyEntries(tab.getFactions()))
                                     .exclusionsSystems(stripEmptyEntries(tab.getSystems()))
                                     .exclusionsLifePath(stripEmptyEntries(tab.getLifePaths()))
                                     .exclusionsCategories(stripEmptyEntries(tab.getCategories()))
                                     .exclusionsAttributes(stripEmptyEntries(tab.getAttributes()))
                                     .exclusionsEdge(stripNullValues(tab.getEdge()))
                                     .exclusionsFlexibleAttribute(stripNullValues(tab.getFlexibleAttribute()))
                                     .exclusionsTraits(stripEmptyEntries(tab.getTraits()))
                                     .exclusionsSkills(stripEmptyEntries(tab.getSkills()))
                                     .exclusionsMetaSkills(stripEmptyEntries(tab.getMetaSkills()))
                                     .exclusionsAbilities(stripEmptyEntries(tab.getAbilities()));
            case FIXED_XP -> builder.fixedXPAttributes(stripEmptyEntries(tab.getAttributes()))
                                   .fixedXPEdge(stripNullValues(tab.getEdge()))
                                   .fixedXPFlexibleAttribute(stripNullValues(tab.getFlexibleAttribute()))
                                   .fixedXPTraits(stripEmptyEntries(tab.getTraits()))
                                   .fixedXPSkills(stripEmptyEntries(tab.getSkills()))
                                   .fixedXPMetaSkills(stripEmptyEntries(tab.getMetaSkills()))
                                   .fixedXPNaturalAptitudes(stripEmptyEntries(tab.getNaturalAptitudes()))
                                   .fixedXPNaturalAptitudesMetaSkills(
                                         stripEmptyEntries(tab.getNaturalAptitudesMetaSkills()))
                                   .fixedXPAbilities(stripEmptyEntries(tab.getAbilities()));
            case FLEXIBLE_XP -> builder.flexibleXPAttributes(stripEmptyEntries(tab.getAttributes()))
                                      .flexibleXPEdge(stripNullValues(tab.getEdge()))
                                      .flexibleXPFlexibleAttribute(stripNullValues(tab.getFlexibleAttribute()))
                                      .flexibleXPTraits(stripEmptyEntries(tab.getTraits()))
                                      .flexibleXPSkills(stripEmptyEntries(tab.getSkills()))
                                      .flexibleXPMetaSkills(stripEmptyEntries(tab.getMetaSkills()))
                                      .flexibleXPNaturalAptitudes(stripEmptyEntries(tab.getNaturalAptitudes()))
                                      .flexibleXPNaturalAptitudesMetaSkills(
                                            stripEmptyEntries(tab.getNaturalAptitudesMetaSkills()))
                                      .flexibleXPAbilities(stripEmptyEntries(tab.getAbilities()))
                                      .flexibleXPPickCount(tab.getPickCount());
        }
    }

    /**
     * Returns the highest group index a section uses in the given Life Path.
     *
     * <p>Every one of the section's maps is consulted, so a file whose only content sits in, say, the meta skills map
     * still reports the right number of groups.</p>
     *
     * @param tabType the section to measure
     * @param record  the Life Path to measure it in
     *
     * @return the highest group index used, or {@code -1} when the section is empty
     *
     * @since 0.50.11
     */
    public static int getMaximumGroupIndex(LifePathBuilderTabType tabType, LifePath record) {
        return switch (tabType) {
            case REQUIREMENTS -> maxKey(record.requirementsFactions(),
                  record.requirementsSystems(),
                  record.requirementsLifePath(),
                  record.requirementsCategories(),
                  record.requirementsAttributes(),
                  record.requirementsEdge(),
                  record.requirementsFlexibleAttribute(),
                  record.requirementsTraits(),
                  record.requirementsSkills(),
                  record.requirementsMetaSkills(),
                  record.requirementsAbilities());
            case EXCLUSIONS -> maxKey(record.exclusionsFactions(),
                  record.exclusionsSystems(),
                  record.exclusionsLifePath(),
                  record.exclusionsCategories(),
                  record.exclusionsAttributes(),
                  record.exclusionsEdge(),
                  record.exclusionsFlexibleAttribute(),
                  record.exclusionsTraits(),
                  record.exclusionsSkills(),
                  record.exclusionsMetaSkills(),
                  record.exclusionsAbilities());
            case FIXED_XP -> maxKey(record.fixedXPAttributes(),
                  record.fixedXPEdge(),
                  record.fixedXPFlexibleAttribute(),
                  record.fixedXPTraits(),
                  record.fixedXPSkills(),
                  record.fixedXPMetaSkills(),
                  record.fixedXPNaturalAptitudes(),
                  record.fixedXPNaturalAptitudesMetaSkills(),
                  record.fixedXPAbilities());
            case FLEXIBLE_XP -> maxKey(record.flexibleXPAttributes(),
                  record.flexibleXPEdge(),
                  record.flexibleXPFlexibleAttribute(),
                  record.flexibleXPTraits(),
                  record.flexibleXPSkills(),
                  record.flexibleXPMetaSkills(),
                  record.flexibleXPNaturalAptitudes(),
                  record.flexibleXPNaturalAptitudesMetaSkills(),
                  record.flexibleXPAbilities());
        };
    }

    /**
     * Loads a section of an existing Life Path onto its tab.
     *
     * @param tabType the section being loaded
     * @param record  the Life Path to read from
     * @param tab     the tab to write into
     *
     * @since 0.50.11
     */
    public static void writeToTab(LifePathBuilderTabType tabType, LifePath record, LifePathTab tab) {
        switch (tabType) {
            case REQUIREMENTS -> {
                tab.setFactions(record.requirementsFactions());
                tab.setSystems(record.requirementsSystems());
                tab.setLifePaths(record.requirementsLifePath());
                tab.setCategories(record.requirementsCategories());
                tab.setAttributes(record.requirementsAttributes());
                tab.setEdge(record.requirementsEdge());
                tab.setFlexibleAttribute(record.requirementsFlexibleAttribute());
                tab.setTraits(record.requirementsTraits());
                tab.setSkills(record.requirementsSkills());
                tab.setMetaSkills(record.requirementsMetaSkills());
                tab.setAbilities(record.requirementsAbilities());
            }
            case EXCLUSIONS -> {
                tab.setFactions(record.exclusionsFactions());
                tab.setSystems(record.exclusionsSystems());
                tab.setLifePaths(record.exclusionsLifePath());
                tab.setCategories(record.exclusionsCategories());
                tab.setAttributes(record.exclusionsAttributes());
                tab.setEdge(record.exclusionsEdge());
                tab.setFlexibleAttribute(record.exclusionsFlexibleAttribute());
                tab.setTraits(record.exclusionsTraits());
                tab.setSkills(record.exclusionsSkills());
                tab.setMetaSkills(record.exclusionsMetaSkills());
                tab.setAbilities(record.exclusionsAbilities());
            }
            case FIXED_XP -> {
                tab.setAttributes(record.fixedXPAttributes());
                tab.setEdge(record.fixedXPEdge());
                tab.setFlexibleAttribute(record.fixedXPFlexibleAttribute());
                tab.setTraits(record.fixedXPTraits());
                tab.setSkills(record.fixedXPSkills());
                tab.setMetaSkills(record.fixedXPMetaSkills());
                tab.setNaturalAptitudes(record.fixedXPNaturalAptitudes());
                tab.setNaturalAptitudesMetaSkills(record.fixedXPNaturalAptitudesMetaSkills());
                tab.setAbilities(record.fixedXPAbilities());
            }
            case FLEXIBLE_XP -> {
                tab.setAttributes(record.flexibleXPAttributes());
                tab.setEdge(record.flexibleXPEdge());
                tab.setFlexibleAttribute(record.flexibleXPFlexibleAttribute());
                tab.setTraits(record.flexibleXPTraits());
                tab.setSkills(record.flexibleXPSkills());
                tab.setMetaSkills(record.flexibleXPMetaSkills());
                tab.setNaturalAptitudes(record.flexibleXPNaturalAptitudes());
                tab.setNaturalAptitudesMetaSkills(record.flexibleXPNaturalAptitudesMetaSkills());
                tab.setAbilities(record.flexibleXPAbilities());
                tab.setPickCount(record.flexibleXPPickCount());
            }
        }
    }

    /**
     * Returns the supplied groups without the ones whose contents are empty.
     *
     * @param groups a group map whose values are collections
     * @param <T>    the collection type held per group
     *
     * @return a new map holding only the groups that contain something
     *
     * @since 0.50.11
     */
    private static <T> Map<Integer, T> stripEmptyEntries(Map<Integer, T> groups) {
        Map<Integer, T> populated = new HashMap<>();

        for (Map.Entry<Integer, T> entry : groups.entrySet()) {
            if (!isEmptyValue(entry.getValue())) {
                populated.put(entry.getKey(), entry.getValue());
            }
        }

        return populated;
    }

    /**
     * Returns the supplied groups without the ones holding no value.
     *
     * <p>Edge and the flexible attribute store a single number per group, and {@code null} means the author never
     * opened the attribute picker for that group. Keeping those entries is what used to write an unintended
     * "Edge 10 is banned" rule into exclusion groups the author never touched.</p>
     *
     * @param groups a group map of single values
     *
     * @return a new map holding only the groups with a value set
     *
     * @since 0.50.11
     */
    private static Map<Integer, Integer> stripNullValues(Map<Integer, Integer> groups) {
        Map<Integer, Integer> populated = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : groups.entrySet()) {
            if (entry.getValue() != null) {
                populated.put(entry.getKey(), entry.getValue());
            }
        }

        return populated;
    }

    /**
     * Reports whether a group's contents amount to nothing.
     *
     * @param value the group's contents
     *
     * @return {@code true} when the value is {@code null}, an empty map or an empty set
     *
     * @since 0.50.11
     */
    private static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }

        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }

        if (value instanceof Set<?> set) {
            return set.isEmpty();
        }

        return false;
    }

    /**
     * Returns the highest key across the supplied group maps.
     *
     * @param maps the maps to scan
     *
     * @return the highest key found, or {@code -1} when every map is empty
     *
     * @since 0.50.11
     */
    @SafeVarargs
    private static int maxKey(Map<Integer, ?>... maps) {
        int maximumKey = -1;

        for (Map<Integer, ?> map : maps) {
            if (map == null) {
                continue;
            }

            for (Integer key : map.keySet()) {
                maximumKey = Math.max(maximumKey, key);
            }
        }

        return maximumKey;
    }
}
