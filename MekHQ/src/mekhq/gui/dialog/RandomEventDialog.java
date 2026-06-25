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
package mekhq.gui.dialog;

import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.personnel.PersonnelOptions.EDGE_RANDOM_EVENTS;
import static mekhq.campaign.randomEvents.personalities.PersonalityController.getPersonalityValue;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import megamek.codeUtilities.StringUtility;
import megamek.common.TargetRollModifier;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.AttributeCheck;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.randomEvents.randomEventsSystem.RandomEventData;
import mekhq.campaign.randomEvents.randomEventsSystem.RandomEventResponseEntry;
import mekhq.campaign.randomEvents.randomEventsSystem.RandomEventResponseQuality;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import org.jspecify.annotations.NonNull;

public class RandomEventDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.RandomEventDialog";

    private final static String CHECK_REASON_KEY = "skillOrAttributeCheck.reason";

    private final static String RESPONSE_PREFIX = "response";
    private final static String BUTTON_SUFFIX = ".button";

    private final static String RESULT_OOC = "result.ooc";

    private final static String EVENT_PREFIX = "event.";
    private final static String MESSAGE_SUFFIX = ".message";

    private final Campaign campaign;
    private final Person eventParticipant;
    private final boolean isUseAgingEffects;
    private final boolean isUseRandomPersonalities;
    private final boolean isClanCampaign;
    private final LocalDate today;
    private final boolean useEdge;
    private final int personalityModifier;

    private final Map<Integer, SkillCheck> skillCheckMap = new HashMap<>();
    private final Map<Integer, AttributeCheck> attributeCheckMap = new HashMap<>();
    private final Map<Integer, Integer> difficultyMap = new HashMap<>();

    private int choiceIndex;
    private String skillCheckResultsText = "";
    private String attributeCheckResultsText = "";
    private boolean wasSuccessful;

    public int getDialogChoice() {
        return choiceIndex;
    }

    public boolean wasSuccessful() {
        return wasSuccessful;
    }

    public RandomEventDialog(Campaign campaign, Person eventParticipant, @Nullable Person otherEventParticipant,
          RandomEventData eventData, String externalResourceBundle) {
        this.campaign = campaign;
        this.eventParticipant = eventParticipant;

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        isUseAgingEffects = campaignOptions.isUseAgeEffects();
        isUseRandomPersonalities = campaignOptions.isUseRandomPersonalities();

        isClanCampaign = campaign.isClanCampaign();
        today = campaign.getLocalDate();

        useEdge = campaignOptions.isUseEdge() && eventParticipant.getOptions().booleanOption(EDGE_RANDOM_EVENTS);
        personalityModifier = getPersonalityModifier(eventParticipant);
        populateHashMaps(eventData);

        resolveDialog(campaign,
              eventParticipant,
              otherEventParticipant,
              eventData,
              externalResourceBundle);
    }

    private void resolveDialog(Campaign campaign, Person eventParticipant, @Nullable Person otherEventParticipant,
          RandomEventData eventData, String externalResourceBundle) {
        triggerDialog(campaign, eventParticipant, otherEventParticipant, eventData, externalResourceBundle);

        resolveEvent(useEdge);
        reportSkillCheckResults();
        reportAttributeCheckResults();
    }

    private void triggerDialog(Campaign campaign, Person eventParticipant, @Nullable Person otherEventParticipant,
          RandomEventData eventData, String externalResourceBundle) {
        String eventName = eventData.randomEventType().name();
        String commanderAddress = getCommanderAddress();

        String inCharacterMessage = getInCharacterMessage(eventName, externalResourceBundle, commanderAddress);
        String outOfCharacterMessage = getOutOfCharacterMessage(externalResourceBundle);
        List<String> options = getOptions(eventData, eventName, externalResourceBundle);

        ImmersiveDialogSimple eventDialog = new ImmersiveDialogSimple(campaign,
              eventParticipant,
              otherEventParticipant,
              inCharacterMessage,
              options,
              outOfCharacterMessage,
              null,
              true);

        choiceIndex = eventDialog.getDialogChoice();
    }

    private int getPersonalityModifier(Person eventParticipant) {
        // This polarity inversion is intentional. Positive is a penalty, negative is a bonus
        return -getPersonalityValue(isUseRandomPersonalities,
              eventParticipant.getAggression(),
              eventParticipant.getAmbition(),
              eventParticipant.getGreed(),
              eventParticipant.getSocial());
    }

    private void populateHashMaps(RandomEventData event) {
        List<RandomEventResponseEntry> responseEntries = event.responseEntries();
        for (int responseIndex = 0; responseIndex < event.responseEntries().size(); responseIndex++) {
            RandomEventResponseEntry response = responseEntries.get(responseIndex);

            List<TargetRollModifier> externalModifiers = applyExternalModifiers(personalityModifier,
                  response, responseIndex);

            addSkillCheck(response.skillCheckSkill(), externalModifiers, responseIndex);
            addAttributeCheck(response.abilityCheckType(), externalModifiers, responseIndex);
        }
    }

    private int getDifficultyModifier(RandomEventResponseEntry response, int responseIndex) {
        RandomEventResponseQuality quality = response.quality();
        int difficultyModifier = quality.getTargetNumberModifier();

        difficultyMap.put(responseIndex, difficultyModifier);

        return difficultyModifier;
    }

    private void addAttributeCheck(SkillAttribute attribute, List<TargetRollModifier> externalModifiers,
          int responseIndex) {
        if (attribute != SkillAttribute.NO_ATTRIBUTE) {
            AttributeCheck attributeCheck = eventParticipant.checkAttribute(attribute)
                                                  .withExternalModifiers(externalModifiers);

            attributeCheckMap.put(responseIndex, attributeCheck);
        }
    }

    private void addSkillCheck(String skillName, List<TargetRollModifier> externalModifiers,
          int responseIndex) {
        if (!StringUtility.isNullOrBlank(skillName)) {
            SkillCheck skillCheck = eventParticipant.checkSkill(skillName, isUseAgingEffects, isClanCampaign, today)
                                          .withExternalModifiers(externalModifiers);

            skillCheckMap.put(responseIndex, skillCheck);
        }
    }

    private List<TargetRollModifier> applyExternalModifiers(int personalityModifier, RandomEventResponseEntry response,
          int responseIndex) {
        List<TargetRollModifier> externalModifiers = new ArrayList<>();
        if (personalityModifier != 0) {
            addModifier("RandomEventDialog.modifier.personality", externalModifiers, personalityModifier);
        }

        int difficultyModifier = getDifficultyModifier(response, responseIndex);
        if (difficultyModifier != 0) {
            addModifier("RandomEventDialog.modifier.difficulty", externalModifiers, difficultyModifier);
        }

        return externalModifiers;
    }

    private static void addModifier(String resourceKey, List<TargetRollModifier> modifierArray, int modifier) {
        String modifierLabel = getTextAt(RESOURCE_BUNDLE, resourceKey);
        modifierArray.add(new TargetRollModifier(modifier, modifierLabel));
    }

    private static @NonNull String getOutOfCharacterMessage(String externalResourceBundle) {
        return getFormattedTextAt(externalResourceBundle, RESULT_OOC);
    }

    private List<String> getOptions(RandomEventData event, String eventName, String externalResourceBundle) {
        List<String> options = new ArrayList<>();

        List<RandomEventResponseEntry> responseEntries = event.responseEntries();
        for (int responseIndex = 0; responseIndex < responseEntries.size(); responseIndex++) {
            String optionText = getOptionText(eventName, externalResourceBundle, responseIndex);
            options.add(optionText);
        }

        return options;
    }

    private @NonNull String getOptionText(String eventName, String externalResourceBundle, int responseIndex) {
        String resourceKey = RESPONSE_PREFIX + "." + responseIndex + "." + eventName + BUTTON_SUFFIX;
        String optionText = getFormattedTextAt(externalResourceBundle, resourceKey);

        SkillCheck skillCheck = skillCheckMap.get(responseIndex);
        AttributeCheck attributeCheck = attributeCheckMap.get(responseIndex);
        if (skillCheck != null) {
            optionText = getSkillAddendum(skillCheck, optionText);
        } else if (attributeCheck != null) {
            optionText = getAttributeAddendum(attributeCheck, optionText);
        }

        return optionText;
    }

    private @NonNull String getAttributeAddendum(AttributeCheck attributeCheck, String optionText) {
        SkillAttribute attribute = attributeCheck.getFirstAttribute();
        String attributeName = attribute.getLabel();
        String attributeLevel = Attributes.getAttributeLevel(eventParticipant, attribute);

        optionText = attributeName + " (" + attributeLevel + ")<br>" + optionText;

        return optionText;
    }

    private @NonNull String getSkillAddendum(SkillCheck skillCheck, String optionText) {
        SkillType skillType = skillCheck.getSkillType();
        String skillName = skillType.getName();

        Skill skill = eventParticipant.getSkill(skillName);
        String skillLevelLabel = getTextAt(RESOURCE_BUNDLE, "RandomEventDialog.skill.unskilled");
        if (skill != null) {
            SkillModifierData skillModifierData = eventParticipant.getSkillModifierData(isUseAgingEffects,
                  isClanCampaign,
                  today);
            SkillLevel skillLevel = skill.getSkillLevel(skillModifierData);
            skillLevelLabel = skillLevel.toString();
        }

        optionText = skillName + " (" + skillLevelLabel + ")<br>" + optionText;
        return optionText;
    }

    private static @NonNull String getInCharacterMessage(String eventName, String externalResourceBundle,
          String commanderAddress) {
        String resourceKey = EVENT_PREFIX + eventName + MESSAGE_SUFFIX;
        return getFormattedTextAt(externalResourceBundle, resourceKey, commanderAddress);
    }

    private String getCommanderAddress() {
        return campaign.getCommanderAddress();
    }

    public void resolveEvent(boolean useEdge) {
        SkillCheck skillCheck = skillCheckMap.get(choiceIndex);
        AttributeCheck attributeCheck = attributeCheckMap.get(choiceIndex);

        String reason = getTextAt(RESOURCE_BUNDLE, CHECK_REASON_KEY);

        // Skill checks take precedence over attribute checks
        if (skillCheck != null) {
            resolveSkillCheck(useEdge, skillCheck, reason);
            return;
        }

        // Attribute checks
        if (attributeCheck != null) {
            resolveAttributeCheck(useEdge, attributeCheck, reason);
            return;
        }

        // Fallback
        int externalModifiers = difficultyMap.get(choiceIndex) + personalityModifier;
        final int RESPONSE_TARGET_NUMBER = 7 + externalModifiers;

        int roll = d6(2);
        wasSuccessful = roll <= RESPONSE_TARGET_NUMBER;
    }

    private void resolveAttributeCheck(boolean useEdge, AttributeCheck attributeCheck, String reason) {
        ActionCheckResult result = attributeCheck.resolve(useEdge, reason, true);
        attributeCheckResultsText = result.resultsText();
        wasSuccessful = result.isSuccess();
    }

    private void resolveSkillCheck(boolean useEdge, SkillCheck skillCheck, String reason) {
        ActionCheckResult result = skillCheck.resolve(useEdge, reason, true);
        skillCheckResultsText = result.resultsText();
        wasSuccessful = result.isSuccess();
    }

    public void reportSkillCheckResults() {
        if (!skillCheckResultsText.isBlank()) {
            campaign.addReport(DailyReportType.SKILL_CHECKS, skillCheckResultsText);
        }
    }

    public void reportAttributeCheckResults() {
        if (!attributeCheckResultsText.isBlank()) {
            campaign.addReport(DailyReportType.SKILL_CHECKS, attributeCheckResultsText);
        }
    }
}
