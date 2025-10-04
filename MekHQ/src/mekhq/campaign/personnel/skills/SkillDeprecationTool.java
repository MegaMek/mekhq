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
package mekhq.campaign.personnel.skills;

import static java.lang.Math.round;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.SkillTypeNew;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * The {@code SkillDeprecationTool} class checks and manages deprecated skills for a {@link Person}.
 *
 * <p>It identifies deprecated skills from the person's current skill set and allows the user to remove them while
 * refunding the appropriate amount of XP. This process involves calculating XP refunds using the skill's cost and
 * reasoning multiplier and providing a dialog for managing the refund.
 *
 * <p>The class is initialized with a {@link Campaign} and a {@link Person}, and directly modifies the person's
 * skills and XP as necessary.
 *
 * <p>Resources such as messages and button labels are loaded from a localized resource bundle.</p>
 */
public class SkillDeprecationTool {
    private final String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    @SuppressWarnings(value = "FieldCanBeLocal")
    private final int SKIP_ALL_DIALOG_OPTION_INDEX = 1;
    @SuppressWarnings(value = "FieldCanBeLocal")
    private final int REFUND_DIALOG_OPTION_INDEX = 2;
    @SuppressWarnings(value = "FieldCanBeLocal")
    private final int REFUND_ALL_DIALOG_OPTION_INDEX = 3;
    /**
     * A list of deprecated skills.
     *
     * <p>These are skills that have been scheduled for removal.</p>
     *
     * <p>Once the skill is removed from this list, players will no longer be able to benefit from skill refund.
     * This list should be updated following each Milestone release. If there are no skills in the list, an empty array
     * MUST be left; otherwise we will run into NPEs during campaign loading.</p>
     *
     * <p><b>Last Updated:</b> 50.07</p>
     */
    public static final List<SkillTypeNew> DEPRECATED_SKILLS = List.of();

    private final Campaign campaign;
    private final Person person;
    private boolean skipAll = false;
    private boolean refundAll;

    /**
     * Constructs a new {@code SkillDeprecationTool} for the specified campaign and person.
     *
     * <p>Upon initialization, this constructor immediately checks the person's skills for any deprecated skills and
     * handles them if necessary.
     *
     * @param campaign the {@link Campaign} instance that provides context for the operation
     * @param person   the {@link Person} whose skills will be checked for deprecation
     */
    public SkillDeprecationTool(Campaign campaign, Person person, boolean refundAll) {
        this.campaign = campaign;
        this.person = person;
        this.refundAll = refundAll;

        checkForDeprecatedSkills(person);
    }

    public boolean isSkipAll() {
        return skipAll;
    }

    public boolean isRefundAll() {
        return refundAll;
    }

    /**
     * Checks the specified person's skills for any deprecated skills.
     *
     * <p>If a deprecated skill is found, calculates the XP refund based on the skill's level and the person's
     * reasoning multiplier. It then triggers a dialog to allow the user to refund or retain the skill.
     *
     * @param person the {@link Person} to check for deprecated skills
     */
    private void checkForDeprecatedSkills(Person person) {
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();

        final double xpCostMultiplier = campaignOptions.getXpCostMultiplier();

        final Skills skills = person.getSkills();
        for (SkillTypeNew skillType : DEPRECATED_SKILLS) {
            final String skillName = skillType.getName();
            if (skills.hasSkill(skillName)) {
                int refundValue = getRefundValue(skills, skillType, skillName);

                refundValue = (int) round(refundValue * xpCostMultiplier);

                triggerDialog(skills, skillName, refundValue);
            }
        }
    }

    /**
     * Calculates the total XP refund value for a deprecated skill by summing the XP required to reach the current level
     * of the skill.
     *
     * @param skills    the {@link Skills} object containing details about the person's skills
     * @param skillType the {@link SkillTypeNew} of the deprecated skill
     * @param skillName the name of the deprecated skill
     *
     * @return the total XP refund value for the deprecated skill
     */
    public static int getRefundValue(Skills skills, SkillTypeNew skillType, String skillName) {
        Skill skill = skills.getSkill(skillName);
        int level = skill.getLevel();

        // Sum the XP cost to reach the current level
        int totalCost = 0;
        for (int i = 1; i <= level; i++) {
            totalCost += skillType.getCost(i);
        }

        return totalCost;
    }

    /**
     * Triggers a dialog to inform the user of a deprecated skill and provides options to either refund the skill or
     * retain it.
     *
     * <p>If the user chooses to refund the skill, it will be removed from the person's skill set, and the XP refund
     * will be added directly to the person's current XP.
     *
     * @param skills      the {@link Skills} object associated with the person
     * @param skillName   the name of the deprecated skill
     * @param refundValue the XP refund value for the skill
     */
    private void triggerDialog(final Skills skills, final String skillName, final int refundValue) {
        ImmersiveDialogSimple dialog = null;
        if (!refundAll) {
            dialog = new ImmersiveDialogSimple(campaign,
                  person,
                  null,
                  getInCharacterMessage(skillName),
                  getButtonLabels(),
                  getOutOfCharacterMessage(skillName, refundValue),
                  null,
                  false);

            if (dialog.getDialogChoice() == SKIP_ALL_DIALOG_OPTION_INDEX) {
                skipAll = true;
            } else if (dialog.getDialogChoice() == REFUND_ALL_DIALOG_OPTION_INDEX) {
                refundAll = true;
            }
        }

        if (isRefundAll() || (dialog != null && dialog.getDialogChoice() == REFUND_DIALOG_OPTION_INDEX)) {
            skills.removeSkill(skillName);
            int currentXp = person.getXP();
            // We use 'setXPDirect' here as the xp gain has already been factored into the tracking of xp gain, so we
            // don't want to double-dip.
            person.setXPDirect(currentXp + refundValue);
        }
    }

    /**
     * Retrieves the in-character message for the deprecated skill dialog.
     *
     * <p>The message is formatted using the localized resource bundle and includes skill-specific context, such as
     * the commander address.
     *
     * @param skillName the name of the deprecated skill
     *
     * @return the formatted in-character message
     */
    private String getInCharacterMessage(String skillName) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "message.ic", campaign.getCommanderAddress(), skillName);
    }

    /**
     * Retrieves the button labels for the deprecated skill dialog.
     *
     * <p>These labels are localized and include options such as "Continue" and "Refund".
     *
     * @return a {@link List} of button label strings
     */
    private List<String> getButtonLabels() {
        return List.of(getFormattedTextAt(RESOURCE_BUNDLE, "button.skip"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.skipAll"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.refund"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.refundAll"));
    }

    /**
     * Retrieves the out-of-character message for the deprecated skill dialog.
     *
     * <p>The message is formatted using the localized resource bundle and includes context about the skill being
     * refunded, the person's name, and the refund value.
     *
     * @param skillName   the name of the deprecated skill
     * @param refundValue the XP refund value for the skill
     *
     * @return the formatted out-of-character message
     */
    private String getOutOfCharacterMessage(String skillName, int refundValue) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "message.ooc", skillName, person.getFullTitle(), refundValue);
    }
}
