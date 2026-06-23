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
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.AttributeCheck;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.randomEvents.randomEventSystem.RandomEventData;
import mekhq.campaign.randomEvents.randomEventSystem.RandomEventResponseEntry;
import mekhq.campaign.randomEvents.randomEventSystem.RandomEventResponseQuality;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import org.jspecify.annotations.NonNull;

public class RandomEventDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.RandomEventDialog";

    private final static String RESPONSE_PREFIX = "response";
    private final static String BUTTON_SUFFIX = ".button";

    private final static String RESULT_OOC = "result.ooc";

    private final static String EVENT_PREFIX = "event.";
    private final static String MESSAGE_SUFFIX = ".message";

    private final int choiceIndex;
    private final Map<Integer, SkillCheck> skillCheckMap = new HashMap<>();
    private final Map<Integer, AttributeCheck> attributeCheckMap = new HashMap<>();

    public int getDialogChoice() {
        return choiceIndex;
    }

    public RandomEventDialog(Campaign campaign, Person eventParticipant, @Nullable Person otherEventParticipant,
          RandomEventData eventData, String externalResourceBundle) {
        // Build check maps
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUseAgingEffects = campaignOptions.isUseAgeEffects();
        boolean isUseRandomPersonalities = campaignOptions.isUseRandomPersonalities();

        boolean isClanCampaign = campaign.isClanCampaign();
        LocalDate today = campaign.getLocalDate();

        int personalityModifier = -getPersonalityValue(isUseRandomPersonalities,
              eventParticipant.getAggression(),
              eventParticipant.getAmbition(),
              eventParticipant.getGreed(),
              eventParticipant.getSocial());

        buildCheckMaps(eventParticipant, eventData, isUseAgingEffects, isClanCampaign, today, personalityModifier);

        String eventName = eventData.randomEventType().name();
        String commanderAddress = getCommanderAddress(campaign);

        String inCharacterMessage = getInCharacterMessage(eventName, externalResourceBundle, commanderAddress);
        String outOfCharacterMessage = getOutOfCharacterMessage(externalResourceBundle);

        List<String> options = getOptions(eventParticipant, eventData, eventName, externalResourceBundle,
              isUseAgingEffects, isClanCampaign, today);

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

    private void buildCheckMaps(Person speaker, RandomEventData event, boolean isUseAgingEffects,
          boolean isClanCampaign, LocalDate today, int personalityModifier) {
        List<RandomEventResponseEntry> responseEntries = event.responseEntries();
        for (int i = 0; i < event.responseEntries().size(); i++) {
            RandomEventResponseEntry response = responseEntries.get(i);
            RandomEventResponseQuality quality = response.quality();
            int difficultyModifier = quality.getTargetNumberModifier();

            String skillName = response.skillCheckSkill();
            if (!StringUtility.isNullOrBlank(skillName)) {
                SkillCheck skillCheck = speaker.checkSkill(skillName,
                      isUseAgingEffects,
                      isClanCampaign,
                      today);

                applyExternalModifiers(personalityModifier, difficultyModifier, skillCheck);

                skillCheckMap.put(i, skillCheck);
            }

            SkillAttribute attributeChecked = response.abilityCheckType();
            if (attributeChecked != SkillAttribute.ATTRIBUTE_NONE) {
                AttributeCheck attributeCheck = speaker.checkAttribute(attributeChecked);
                attributeCheckMap.put(i, attributeCheck);
            }
        }
    }

    private static void applyExternalModifiers(int personalityModifier, int difficultyModifier, SkillCheck skillCheck) {
        List<TargetRollModifier> externalModifiers = new ArrayList<>();
        if (personalityModifier != 0) {
            String modifierLabel = getTextAt(RESOURCE_BUNDLE, "RandomEventDialog.modifier.personality");

            // Polarity change is intentional. Positive is a penalty, negative is a bonus
            externalModifiers.add(new TargetRollModifier(-personalityModifier, modifierLabel));
        }

        if (difficultyModifier != 0) {
            String modifierLabel = getTextAt(RESOURCE_BUNDLE, "RandomEventDialog.modifier.difficulty");
            externalModifiers.add(new TargetRollModifier(personalityModifier, modifierLabel));
        }

        if (!externalModifiers.isEmpty()) {
            skillCheck.withExternalModifiers(externalModifiers);
        }
    }

    private static @NonNull String getOutOfCharacterMessage(String RESOURCE_BUNDLE) {
        return getFormattedTextAt(RESOURCE_BUNDLE, RESULT_OOC);
    }

    private List<String> getOptions(Person speaker, RandomEventData event, String eventName, String RESOURCE_BUNDLE,
          boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today) {
        List<String> options = new ArrayList<>();

        List<RandomEventResponseEntry> responseEntries = event.responseEntries();
        for (int responseIndex = 0; responseIndex < responseEntries.size(); responseIndex++) {
            String optionText = getOptionText(speaker, eventName, RESOURCE_BUNDLE, responseIndex, isUseAgingEffects,
                  isClanCampaign, today);
            options.add(optionText);
        }

        return options;
    }

    private @NonNull String getOptionText(Person speaker, String eventName, String RESOURCE_BUNDLE, int responseIndex,
          boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today) {
        String resourceKey = RESPONSE_PREFIX + "." + responseIndex + "." + eventName + BUTTON_SUFFIX;
        String optionText = getFormattedTextAt(RESOURCE_BUNDLE, resourceKey);

        SkillCheck skillCheck = skillCheckMap.get(responseIndex);
        if (skillCheck != null) {
            SkillType skillType = skillCheck.getSkillType();
            String skillName = skillType.getName();

            Skill skill = speaker.getSkill(skillName);
            String skillLevelLabel = "Unskilled"; // TODO remove hardcoded value
            if (skill != null) {
                SkillModifierData skillModifierData = speaker.getSkillModifierData(isUseAgingEffects, isClanCampaign,
                      today);
                SkillLevel skillLevel = skill.getSkillLevel(skillModifierData);
                skillLevelLabel = skillLevel.getShortName();
            }
            optionText = "<b>(" + skillName + ", " + skillLevelLabel + ")</b> " + optionText;
        }

        AttributeCheck attributeCheck = attributeCheckMap.get(responseIndex);
        if (attributeCheck != null) {
            String attributeName = attributeCheck.getActionName();
            int targetNumber = attributeCheck.getTargetNumber().getValue();
            optionText = "<b>(" + attributeName + ", " + targetNumber + "+)</b> " + optionText;
        }
        return optionText;
    }

    private static @NonNull String getInCharacterMessage(String eventName, String RESOURCE_BUNDLE,
          String commanderAddress) {
        String resourceKey = EVENT_PREFIX + eventName + MESSAGE_SUFFIX;
        return getFormattedTextAt(RESOURCE_BUNDLE, resourceKey, commanderAddress);
    }

    private static String getCommanderAddress(Campaign campaign) {
        return campaign.getCommanderAddress();
    }
}
