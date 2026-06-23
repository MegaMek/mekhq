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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.AttributeCheck;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.randomEvents.randomEventSystem.RandomEventData;
import mekhq.campaign.randomEvents.randomEventSystem.RandomEventResponseEntry;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import org.jspecify.annotations.NonNull;

public class RandomEventDialog {
    private static final MMLogger LOGGER = MMLogger.create(RandomEventDialog.class);

    private final static String RESPONSE_PREFIX = "response.";
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
        boolean isUseAgingEffects = campaign.getCampaignOptions().isUseAgeEffects();
        boolean isClanCampaign = campaign.isClanCampaign();
        LocalDate today = campaign.getLocalDate();

        buildCheckMaps(eventParticipant, eventData, isUseAgingEffects, isClanCampaign, today);

        String eventName = eventData.randomEventType().name();
        String commanderAddress = getCommanderAddress(campaign);

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

    private void buildCheckMaps(Person speaker, RandomEventData event, boolean isUseAgingEffects,
          boolean isClanCampaign,
          LocalDate today) {
        List<RandomEventResponseEntry> responseEntries = event.responseEntries();
        for (int i = 0; i < event.responseEntries().size(); i++) {
            RandomEventResponseEntry response = responseEntries.get(i);

            String skillCheckSkill = response.skillCheckSkill();
            if (!StringUtility.isNullOrBlank(skillCheckSkill)) {
                SkillCheck skillCheck = speaker.checkSkill(skillCheckSkill,
                      isUseAgingEffects,
                      isClanCampaign,
                      today);
                skillCheckMap.put(i, skillCheck);
            }

            SkillAttribute attributeChecked = response.abilityCheckType();
            if (attributeChecked != SkillAttribute.ATTRIBUTE_NONE) {
                AttributeCheck attributeCheck = speaker.checkAttribute(attributeChecked);
                attributeCheckMap.put(i, attributeCheck);
            }
        }
    }

    private static @NonNull String getOutOfCharacterMessage(String RESOURCE_BUNDLE) {
        return getFormattedTextAt(RESOURCE_BUNDLE, RESULT_OOC);
    }

    private List<String> getOptions(RandomEventData event, String eventName, String RESOURCE_BUNDLE) {
        List<String> options = new ArrayList<>();

        List<RandomEventResponseEntry> responseEntries = event.responseEntries();
        for (int i = 0; i < responseEntries.size(); i++) {
            String optionText = getOptionText(eventName, RESOURCE_BUNDLE, i);
            options.add(optionText);
        }

        return options;
    }

    private @NonNull String getOptionText(String eventName, String RESOURCE_BUNDLE, int i) {
        String resourceKey = RESPONSE_PREFIX + eventName + i + BUTTON_SUFFIX;
        String optionText = getFormattedTextAt(RESOURCE_BUNDLE, resourceKey);

        SkillCheck skillCheck = skillCheckMap.get(i);
        if (skillCheck != null) {
            String skillName = skillCheck.getSkillType().getName();
            int targetNumber = skillCheck.getTargetNumber().getValue();
            optionText = "(" + skillName + ", " + targetNumber + "+) " + optionText;
        }

        AttributeCheck attributeCheck = attributeCheckMap.get(i);
        if (attributeCheck != null) {
            String attributeName = attributeCheck.getActionName();
            int targetNumber = attributeCheck.getTargetNumber().getValue();
            optionText = "(" + attributeName + ", " + targetNumber + "+) " + optionText;
        }
        return optionText;
    }

    private static String getSkillCheckString(Person speaker, RandomEventResponseEntry responseEntry, String optionText,
          boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today) {
        String skillCheckSkill = responseEntry.skillCheckSkill();
        if (!StringUtility.isNullOrBlank(skillCheckSkill)) {
            SkillType skillType = SkillType.getType(skillCheckSkill);
            if (skillType == null) {
                LOGGER.error("Unknown skill type {} in Random Event Dialog. Skipping", skillCheckSkill);
                return optionText;
            }

            SkillCheck skillCheck = speaker.checkSkill(skillCheckSkill, isUseAgingEffects, isClanCampaign, today);
            int targetNumber = skillCheck.getTargetNumber().getValue();
            return " (" + skillCheckSkill + ", " + targetNumber + "+) " + optionText;
        }

        return optionText;
    }

    private static String getAbilityCheckString(Person speaker, RandomEventResponseEntry responseEntry,
          String optionText) {
        SkillAttribute attributeChecked = responseEntry.abilityCheckType();
        if (attributeChecked != SkillAttribute.ATTRIBUTE_NONE) {
            AttributeCheck attributeCheck = speaker.checkAttribute(attributeChecked);
            int targetNumber = attributeCheck.getTargetNumber().getValue();
            optionText = " (" + attributeChecked.getLabel() + ", " + targetNumber + "+) " + optionText;
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
