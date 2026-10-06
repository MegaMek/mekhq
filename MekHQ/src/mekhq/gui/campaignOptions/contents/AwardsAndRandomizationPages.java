/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.contents;

import javax.swing.JPanel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;

/**
 * The {@code AwardsAndRandomizationPages} class is responsible for rendering and managing
 * two primary pages in the campaign options
 * interface: the Experience Awards (XP Awards) page and the Skill Randomization
 * page. These pages allow for customization
 * of experience point distribution, randomization preferences, and skill
 * probabilities in the campaign.
 *
 * <p>
 * This class provides methods to initialize the UI components, load current
 * settings
 * from the campaign options, and update the options based on user input.
 * </p>
 */
public class AwardsAndRandomizationPages {
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final RandomSkillPreferences randomSkillPreferences;
    private AwardsAndRandomizationOptionsModel model;
    private final XpAwardsPage xpAwardsPage = new XpAwardsPage();
    private final RandomizationPage randomizationPage = new RandomizationPage();
    private final RecruitmentBonusesPage recruitmentBonusesPage = new RecruitmentBonusesPage();

    /**
     * Constructs an {@code AwardsAndRandomizationPages} object for rendering and managing
     * campaign advancement configurations.
     *
     * @param campaign The {@code Campaign} instance from which the campaign options
     *                 and random skill preferences are
     *                 retrieved.
     */
    public AwardsAndRandomizationPages(@Nonnull Campaign campaign) {
        this.campaign = campaign;
        this.randomSkillPreferences = campaign.getRandomSkillPreferences();
        this.campaignOptions = campaign.getCampaignOptions();

        loadValuesFromCampaignOptions();
    }

    /**
     * Creates and returns the Experience Awards (XP Awards) page panel. This page
     * allows users to configure experience
     * awards for tasks, scenarios, missions, and administrators, as well as set the
     * overall XP cost multiplier.
     *
     * @return A {@code JPanel} containing the configuration options for XP Awards
     *         in the campaign.
     */
    public @Nonnull JPanel xpAwardsPage() {
        return xpAwardsPage.createPanel(model);
    }

    /**
     * Creates and returns the Skill Randomization page panel. This page allows users
     * to configure settings related to
     * skill randomization, including phenotype probabilities and skill bonuses for
     * different experience levels and
     * skill groups.
     *
     * @return A {@code JPanel} containing the configuration options for skill
     *         randomization.
     */
    public @Nonnull JPanel skillRandomizationPage() {
        return randomizationPage.createPanel(model);
    }

    /**
     * Constructs and returns the panel containing recruitment bonus controls
     * grouped by combat and support roles.
     *
     * <p>
     * Includes the header and separately laid-out subpanels for combat and support
     * personnel roles.
     * </p>
     *
     * @return the fully configured {@link JPanel} for recruitment bonus settings
     */
    public @Nonnull JPanel recruitmentBonusesPage() {
        return recruitmentBonusesPage.createPanel(model);
    }

    /**
     * Loads the current values for XP Awards and Skill Randomization settings into
     * the UI components from the campaign
     * options and random skill preferences.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null);
    }

    /**
     * Loads the current values for XP Awards and Skill Randomization settings into
     * the UI components from the given
     * {@code CampaignOptions} and {@code RandomSkillPreferences} objects.
     *
     * @param presetCampaignOptions        Optional {@code CampaignOptions} object
     *                                     to load values from; if {@code null},
     *                                     values are loaded from the current
     *                                     campaign options.
     * @param presetRandomSkillPreferences Optional {@code RandomSkillPreferences}
     *                                     object to load values from; if
     *                                     {@code null}, values are loaded from the
     *                                     current skill preferences.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions,
            @Nullable RandomSkillPreferences presetRandomSkillPreferences) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        RandomSkillPreferences skillPreferences = presetRandomSkillPreferences;
        if (skillPreferences == null) {
            skillPreferences = this.randomSkillPreferences;
        }

        model = new AwardsAndRandomizationOptionsModel(options, skillPreferences);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the current values from the XP Awards and Skill Randomization pages to
     * the specified
     * {@code CampaignOptions} and {@code RandomSkillPreferences}.
     *
     * @param presetCampaignOptions        Optional {@code CampaignOptions} object
     *                                     to set values to; if {@code null},
     *                                     values are applied to the current
     *                                     campaign options.
     * @param presetRandomSkillPreferences Optional {@code RandomSkillPreferences}
     *                                     object to set values to; if
     *                                     {@code null}, values are applied to the
     *                                     current skill preferences.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions,
            @Nullable RandomSkillPreferences presetRandomSkillPreferences) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        RandomSkillPreferences skillPreferences = presetRandomSkillPreferences;
        if (skillPreferences == null) {
            skillPreferences = this.randomSkillPreferences;
        }

        updateModelFromCreatedControls();
        model.applyTo(options, skillPreferences);

        // Finishing Touches
        // This must be the last item, after all other pages, no matter what.
        if (presetRandomSkillPreferences == null) {
            campaign.setRandomSkillPreferences(randomSkillPreferences);
        }
    }

    private void updateCreatedControlsFromModel() {
        xpAwardsPage.readFromModel(model);
        randomizationPage.readFromModel(model);
        recruitmentBonusesPage.readFromModel(model);
    }

    private void updateModelFromCreatedControls() {
        xpAwardsPage.writeToModel(model);
        randomizationPage.writeToModel(model);
        recruitmentBonusesPage.writeToModel(model);
    }
}
