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

import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.ATOW_TRAIT;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.FACTION_CODE;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.LIFE_PATH;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.LIFE_PATH_CATEGORY;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.SKILL;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.SKILL_ATTRIBUTE;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathEntryData.fromRawEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderTabBasicInformation;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderTabExclusions;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderTabFixedXP;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderTabFlexibleXP;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderTabRequirements;

public record LifePathDataProcessor(LifePathBuilderTabBasicInformation basicInfoTab,
      LifePathBuilderTabRequirements requirementsTab, LifePathBuilderTabExclusions exclusionsTab,
      LifePathBuilderTabFixedXP fixedXPTab, LifePathBuilderTabFlexibleXP flexibleXPTab) {
    private static final MMLogger LOGGER = MMLogger.create(LifePathDataProcessor.class);

    public LifePathRecord buildLifePathFromLifePathBuilder(UUID lifePathID) {
        // Basic Info Tab
        String source = basicInfoTab.getSource();
        Version version = MHQConstants.VERSION;
        String name = basicInfoTab.getName();
        String flavorText = basicInfoTab.getFlavorText();
        int age = basicInfoTab.getAge();
        int xpDiscount = basicInfoTab.getDiscount();
        int xpCost = LifePathXPCostCalculator.calculateXPCost(0, fixedXPTab.getFixedXPTabStorage(),
              flexibleXPTab.getFlexibleXPTabStorageMap());
        List<ATOWLifeStage> lifeStages = basicInfoTab.getLifeStages();
        List<LifePathCategory> categories = basicInfoTab.getCategories();

        // Requirements Tab
        Map<Integer, LifePathTabStorage> requirementsStorage = requirementsTab.getRequirementsTabStorageMap();
        Map<Integer, List<LifePathEntryData>> requirements = new HashMap<>();
        for (Map.Entry<Integer, LifePathTabStorage> entry : requirementsStorage.entrySet()) {
            List<LifePathEntryData> fullyProcessedData = fullyProcessStoredTab(entry.getValue());
            requirements.put(entry.getKey(), fullyProcessedData);
        }

        // Exclusions Tab
        LifePathTabStorage exclusionsStorage = exclusionsTab.getExclusionsTabStorage();
        List<LifePathEntryData> exclusions = new ArrayList<>(fullyProcessStoredTab(exclusionsStorage));

        // Fixed XP Tab
        LifePathTabStorage fixedXPStorage = fixedXPTab.getFixedXPTabStorage();
        List<LifePathEntryData> fixedXPAwards = new ArrayList<>(fullyProcessStoredTab(fixedXPStorage));

        // Flexible XP Tab
        Map<Integer, LifePathTabStorage> flexibleXPStorage = flexibleXPTab.getFlexibleXPTabStorageMap();
        Map<Integer, List<LifePathEntryData>> flexibleXPAwards = new HashMap<>();
        for (Map.Entry<Integer, LifePathTabStorage> entry : flexibleXPStorage.entrySet()) {
            List<LifePathEntryData> fullyProcessedData = fullyProcessStoredTab(entry.getValue());
            flexibleXPAwards.put(entry.getKey(), fullyProcessedData);
        }

        int pickCount = flexibleXPTab.getPickCount();

        return new LifePathRecord(lifePathID, source, version, name, flavorText, age, xpDiscount, xpCost, lifeStages,
              categories, requirements, exclusions, fixedXPAwards, flexibleXPAwards, pickCount);
    }

    private static List<LifePathEntryData> fullyProcessStoredTab(LifePathTabStorage storage) {
        List<String> preProcessedData = new ArrayList<>();
        preProcessedData.addAll(preProcessFactionData(storage.factions()));
        preProcessedData.addAll(preProcessLifePathData(storage.lifePaths()));
        preProcessedData.addAll(preProcessCategoryData(storage.categories()));
        preProcessedData.addAll(preProcessAttributeData(storage.attributes()));
        preProcessedData.addAll(preProcessTraitData(storage.traits()));
        preProcessedData.addAll(preProcessSkillData(storage.skills()));
        preProcessedData.addAll(preProcessAbilityData(storage.abilities()));

        List<LifePathEntryData> fullyProcessedData = new ArrayList<>();
        for (String preProcessedEntry : preProcessedData) {
            fullyProcessedData.add(fromRawEntry(preProcessedEntry));
        }
        return fullyProcessedData;
    }

    private static List<String> preProcessFactionData(List<Faction> storage) {
        List<String> rawFactionData = new ArrayList<>();

        for (Faction faction : storage) {
            // We don't care about the 'value' entry, so use the placeholder '0'
            String rawData = FACTION_CODE.getLookupName() + "::" + faction.getShortName() + "::0";
            rawFactionData.add(rawData);
        }

        return rawFactionData;
    }

    private static List<String> preProcessLifePathData(List<LifePathRecord> storage) {
        List<String> processedLifePathData = new ArrayList<>();

        for (LifePathRecord lifePathRecord : storage) {
            // We don't care about the 'value' entry, so use the placeholder '0'
            String rawData = LIFE_PATH.getLookupName() + "::" + lifePathRecord.id().toString() + "::0";
            processedLifePathData.add(rawData);
        }

        return processedLifePathData;
    }

    private static List<String> preProcessCategoryData(Map<LifePathCategory, Integer> storage) {
        List<String> processedCategoryData = new ArrayList<>();

        for (Map.Entry<LifePathCategory, Integer> entry : storage.entrySet()) {
            String rawData = LIFE_PATH_CATEGORY.getLookupName() +
                                   "::" +
                                   entry.getKey().getLookupName() +
                                   "::" +
                                   entry.getValue();
            processedCategoryData.add(rawData);
        }

        return processedCategoryData;
    }

    private static List<String> preProcessAttributeData(Map<SkillAttribute, Integer> storage) {
        List<String> processedAttributeData = new ArrayList<>();

        for (Map.Entry<SkillAttribute, Integer> entry : storage.entrySet()) {
            String rawData = SKILL_ATTRIBUTE.getLookupName() +
                                   "::" +
                                   entry.getKey().getLookupName() +
                                   "::" +
                                   entry.getValue();
            processedAttributeData.add(rawData);
        }

        return processedAttributeData;
    }

    private static List<String> preProcessTraitData(Map<LifePathEntryDataTraitLookup, Integer> storage) {
        List<String> processedAttributeData = new ArrayList<>();

        for (Map.Entry<LifePathEntryDataTraitLookup, Integer> entry : storage.entrySet()) {
            String rawData = ATOW_TRAIT.getLookupName() +
                                   "::" +
                                   entry.getKey().getLookupName() +
                                   "::" +
                                   entry.getValue();
            processedAttributeData.add(rawData);
        }

        return processedAttributeData;
    }

    private static List<String> preProcessSkillData(Map<SkillType, Integer> storage) {
        List<String> processedAttributeData = new ArrayList<>();

        for (Map.Entry<SkillType, Integer> entry : storage.entrySet()) {
            String rawData = SKILL.getLookupName() + "::" + entry.getKey().getName() + "::" + entry.getValue();
            processedAttributeData.add(rawData);
        }

        return processedAttributeData;
    }

    private static List<String> preProcessAbilityData(Map<CampaignOptionsAbilityInfo, Integer> storage) {
        List<String> processedAttributeData = new ArrayList<>();

        for (Map.Entry<CampaignOptionsAbilityInfo, Integer> entry : storage.entrySet()) {
            String rawData =
                  SKILL.getLookupName() + "::" + entry.getKey().getAbility().getName() + "::" + entry.getValue();
            processedAttributeData.add(rawData);
        }

        return processedAttributeData;
    }

    public UUID updateExistingTabsFromNewLifePath(LifePathRecord record, int gameYear) {
        // Basic Info Tab
        basicInfoTab.setName(record.name());
        basicInfoTab.setFlavorText(record.flavorText());
        basicInfoTab.setSource(record.source());
        basicInfoTab.setAge(record.age());
        basicInfoTab.setDiscount(record.xpDiscount());
        basicInfoTab.setLifeStages(record.lifeStages());
        basicInfoTab.setCategories(record.categories());

        // Requirements Tab
        Map<Integer, LifePathTabStorage> unpackedRequirements = unpackRequirements(record.requirements(),
              gameYear);
        requirementsTab.setRequirementsTabStorageMap(record.requirements());

        LifePathBuilderTabExclusions exclusionsTab
        LifePathBuilderTabFixedXP fixedXPTab
        LifePathBuilderTabFlexibleXP flexibleXPTab

        return record.id();
    }

    private Map<Integer, LifePathTabStorage> unpackRequirements(Map<Integer, List<LifePathEntryData>> requirements,
          int gameYear) {
        final Factions factionsInstance = Factions.getInstance();

        Map<Integer, LifePathTabStorage> unpackedRequirements = new HashMap<>();

        for (Map.Entry<Integer, List<LifePathEntryData>> entry : requirements.entrySet()) {
            int stage = entry.getKey();
            List<LifePathTabStorage> unpackedData = new ArrayList<>();

            Map<LifePathEntryDataTraitLookup, Integer> traits = new HashMap<>();
            List<Faction> factions = new ArrayList<>();
            List<UUID> lifePaths = new ArrayList<>();
            Map<LifePathCategory, Integer> categories = new HashMap<>();
            Map<SkillAttribute, Integer> attributes = new HashMap<>();
            Map<SkillType, Integer> skills = new HashMap<>();
            Map<CampaignOptionsAbilityInfo, Integer> abilities = new HashMap<>();

            for (LifePathEntryData data : entry.getValue()) {
                LifePathDataClassLookup classLookup = LifePathDataClassLookup.fromLookupName(data.classLookupName());
                if (classLookup == null) {
                    LOGGER.error(new IllegalArgumentException("Unknown object lookup name: " + data.classLookupName()));
                    continue;
                }

                switch (classLookup) {
                    case ATOW_TRAIT -> traits.putAll(processTraitNode(data, true));
                    case FACTION_CODE -> {
                        Faction faction = processFactionNode(data.getFactionCode(), factionsInstance, factions);
                        if (faction != null) {
                            factions.add(faction);
                        }
                    }
                    case LIFE_PATH -> {
                        UUID lifePath = processLifePathNode(data.getLifePathUUID(), lifePaths);
                        if (lifePath != null) {
                            lifePaths.add(lifePath);
                        }
                    }
                    case LIFE_PATH_CATEGORY -> categories.putAll(processLifePathCategoryNode(data));
                    case SKILL -> skills.putAll(processSkillsNode(data));
                    case SKILL_ATTRIBUTE -> {}
                    case SPA -> {}
                    default -> LOGGER.error(new IllegalArgumentException("Unknown object lookup name: " +
                                                                               data.objectLookupName()));
                }


                String factionCode = data.getFactionCode();


            }

        }

        return unpackedRequirements;
    }

    private static Map<LifePathEntryDataTraitLookup, Integer> processTraitNode(LifePathEntryData data,
          boolean compareAgainstMinimum) {
        Map<LifePathEntryDataTraitLookup, Integer> traits = new HashMap<>();

        LifePathEntryDataTraitLookup lookup = LifePathEntryDataTraitLookup.fromLookupName(data.objectLookupName());
        if (lookup == null) {
            LOGGER.error(new IllegalArgumentException("Unknown trait lookup name: " + data.objectLookupName()));
            return new HashMap<>();
        }

        traits.put(lookup, data.getTraitValue(lookup, compareAgainstMinimum));

        return traits;
    }

    private static @Nullable Faction processFactionNode(String factionCode, Factions factionsInstance,
          List<Faction> factions) {
        if (factionCode == null || factionCode.isBlank()) {
            LOGGER.warn("Faction code is null or blank for life path: {}", factionCode);
            return null;
        }

        Faction faction = factionsInstance.getFaction(factionCode);
        if (faction == null) {
            LOGGER.warn("Faction code does not match existing faction: {}", factionCode);
            return null;
        }

        if (factions.contains(faction)) {
            LOGGER.warn("Duplicate faction found for life path: {}", factionCode);
            return null;
        }

        return faction;
    }

    private static @Nullable UUID processLifePathNode(UUID lifePathID, List<UUID> lifePaths) {
        if (lifePathID == null) {
            LOGGER.warn("Life Path UUID is null or blank for life path");
            return null;
        }

        if (lifePaths.contains(lifePathID)) {
            LOGGER.warn("Duplicate Life Path found for life path: {}", lifePathID);
            return null;
        }

        return lifePathID;
    }

    private static Map<LifePathCategory, Integer> processLifePathCategoryNode(LifePathEntryData data) {
        Map<LifePathCategory, Integer> lifePaths = new HashMap<>();

        LifePathCategory lookup = LifePathCategory.fromLookupName(data.objectLookupName());
        if (lookup == null) {
            LOGGER.error(new IllegalArgumentException("Unknown Life Path Category lookup name: " +
                                                            data.objectLookupName()));
            return new HashMap<>();
        }

        lifePaths.put(lookup, data.getLifePathCategory(lookup));

        return lifePaths;
    }

    private static Map<SkillType, Integer> processSkillsNode(LifePathEntryData data) {
        Map<SkillType, Integer> skills = new HashMap<>();

        String skillName = data.objectLookupName();
        SkillType lookup = SkillType.getType(skillName);
        if (lookup == null) {
            LOGGER.error(new IllegalArgumentException("Unknown Skill lookup name: " + data.objectLookupName()));
            return new HashMap<>();
        }

        skills.put(lookup, data.getSkill(skillName));

        return skills;
    }
}
