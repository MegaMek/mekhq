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
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.ranks.RankSystem;

/**
 * The {@code BiographyPages} class coordinates the Biography section of the MekHQ Campaign Options dialog. It owns the
 * shared {@link BiographyOptionsModel} snapshot and delegates the construction and value synchronisation of each leaf
 * page to a dedicated view class: {@link BiographyGeneralPage}, {@link BackgroundsPage}, {@link DeathPage},
 * {@link EducationPage}, {@link NameAndPortraitGenerationPage}, and {@link RankPage}.
 *
 * <p>On load it builds the model from a {@link CampaignOptions} source and pushes it into any pages that have already
 * been built; on apply it gathers the page values back into the model and writes them to the target options. Each
 * model-backed leaf page is built lazily the first time its {@code create...Page()} method is called. The Rank page is
 * an exception: it manages its rank systems through {@link RankPage} directly rather than through the shared model, so
 * the coordinator drives its rank selection and apply steps explicitly.</p>
 */
public class BiographyPages {
    private final Campaign campaign;
    private final GeneralPage generalPage;
    private final CampaignOptions campaignOptions;
    private final RandomOriginOptions randomOriginOptions;
    private BiographyOptionsModel model;

    private final BiographyGeneralPage biographyGeneralPage = new BiographyGeneralPage();
    private final BackgroundsPage backgroundsPage = new BackgroundsPage();
    private final DeathPage deathPage = new DeathPage();
    private final EducationPage educationPage = new EducationPage();
    private final NameAndPortraitGenerationPage nameAndPortraitGenerationPage = new NameAndPortraitGenerationPage();
    private final RankPage rankPage;

    /**
     * Constructs the `BiographyPage` and initializes the campaign and its dependent
     * options.
     *
     * @param campaign   The current `Campaign` object to which the BiographyPage is
     *                   linked. The campaign options and
     *                   origin options are derived from this object.
     * @param generalPage The currently active General Page.
     */
    public BiographyPages(@Nonnull Campaign campaign, GeneralPage generalPage) {
        this.campaign = campaign;
        this.generalPage = generalPage;
        this.campaignOptions = campaign.getCampaignOptions();
        this.randomOriginOptions = campaignOptions.getRandomOriginOptions();
        this.rankPage = new RankPage(campaign);

        loadValuesFromCampaignOptions();
    }

    /**
     * Creates and lays out the General page, including its components like:
     * <ul>
     * <li>Checkboxes for random XP distribution.</li>
     * <li>Sliders for gender representation customization.</li>
     * <li>Combo boxes for family display level settings within the GUI.</li>
     * </ul>
     *
     * @return A `JPanel` representing the General page in the campaign options
     *         dialog.
     */
    public @Nonnull JPanel createGeneralPage() {
        return biographyGeneralPage.createPanel(model);
    }

    /**
     * Creates and lays out the Backgrounds page, which includes:
     * <ul>
     * <li>Settings for enabling randomized personalities and relationships.</li>
     * <li>Random origin configurations such as faction specificity and distance
     * scaling.</li>
     * </ul>
     *
     * @return A `JPanel` representing the Backgrounds page in the campaign options
     *         dialog.
     */
    public @Nonnull JPanel createBackgroundsPage() {
        return backgroundsPage.createPanel(model, campaign, generalPage);
    }

    /**
     * Configures and creates the Death page. This includes options like:
     * <ul>
     * <li>Methods for random death.</li>
     * <li>Percentage-based chances for random death events.</li>
     * <li>Check boxes to enable or disable age-specific death events.</li>
     * </ul>
     *
     * @return A `JPanel` representing the Death page.
     */
    public @Nonnull JPanel createDeathPage() {
        return deathPage.createPanel(model);
    }

    /**
     * Creates the Education page, which allows managing educational settings within
     * the campaign.
     * <p>
     * This includes:
     * <ul>
     * <li>Setting curriculum XP rates.</li>
     * <li>Configuring academy requirements and override options.</li>
     * <li>Managing dropout chances for adults and children.</li>
     * <li>Enabling or disabling the use of reeducation camps, accidents, and
     * events.</li>
     * </ul>
     *
     * @return A {@code JPanel} representing the Education page in the campaign UI.
     */
    public @Nonnull JPanel createEducationPage() {
        return educationPage.createPanel(model);
    }

    /**
     * Creates the Name and Portrait Generation page for the campaign options.
     * <p>
     * This page allows users to:
     * </p>
     * <ul>
     * <li>Enable or disable the use of origin factions for name generation.</li>
     * <li>Assign portraits to personnel upon role changes.</li>
     * <li>Customize which portraits are randomly used for specific roles.</li>
     * </ul>
     *
     * @return A {@code JPanel} representing the Name and Portrait Generation page.
     */
    public @Nonnull JPanel createNameAndPortraitGenerationPage() {
        return nameAndPortraitGenerationPage.createPanel(model);
    }

    /**
     * Creates the Rank page for configuring rank systems within the campaign.
     * <p>
     * This page provides options for:
     * <ul>
     * <li>Managing rank systems for personnel in the campaign.</li>
     * <li>Displaying rank-related UI components for user configuration.</li>
     * </ul>
     *
     * @return A {@code JPanel} representing the Rank page in the campaign
     *         configuration.
     */
    public @Nonnull JPanel createRankPage() {
        return rankPage.createPanel();
    }

    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null, null);
    }

    /**
     * Loads values from campaign options, optionally integrating with presets for
     * default settings.
     *
     * @param presetCampaignOptions     Optional preset campaign options, or `null`
     *                                  to use the campaign's active
     *                                  settings.
     * @param presetRandomOriginOptions Optional random origin options, or `null` to
     *                                  use the default origin settings.
     * @param presetRankSystem          Optional rank system, or `null` to use the
     *                                  default system.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions,
            @Nullable RandomOriginOptions presetRandomOriginOptions, @Nullable RankSystem presetRankSystem) {
        CampaignOptions options = presetCampaignOptions;
        if (options == null) {
            options = this.campaignOptions;
        }

        RandomOriginOptions originOptions = presetRandomOriginOptions;
        if (originOptions == null) {
            originOptions = this.randomOriginOptions;
        }

        RankSystem rankSystem = presetRankSystem;
        if (rankSystem == null) {
            rankSystem = campaign.getRankSystem();
        }

        model = new BiographyOptionsModel(options, originOptions);
        updateCreatedControlsFromModel();

        // Ranks
        rankPage.setSelectedRankSystem(rankSystem);
    }

    /**
     * Applies the current settings from the market UI components back into the
     * {@link CampaignOptions} of the
     * associated campaign.
     * <p>
     * If a preset options object is provided, the changes are applied there.
     * Otherwise, they are applied to the current
     * campaign's options.
     *
     * @param presetCampaignOptions A {@link CampaignOptions} object to update with
     *                              the current UI settings, or
     *                              {@code null} to apply changes to the campaign's
     *                              options directly.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        RandomOriginOptions originOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
            originOptions = this.randomOriginOptions;
        } else {
            originOptions = options.getRandomOriginOptions();
        }

        updateModelFromCreatedControls();
        model.applyTo(options, originOptions);

        // Ranks
        rankPage.applyToCampaign();
    }

    private void updateCreatedControlsFromModel() {
        biographyGeneralPage.readFromModel(model);
        backgroundsPage.readFromModel(model);
        deathPage.readFromModel(model);
        educationPage.readFromModel(model);
        nameAndPortraitGenerationPage.readFromModel(model);
    }

    private void updateModelFromCreatedControls() {
        biographyGeneralPage.writeToModel(model);
        backgroundsPage.writeToModel(model);
        deathPage.writeToModel(model);
        educationPage.writeToModel(model);
        nameAndPortraitGenerationPage.writeToModel(model);
    }
}
