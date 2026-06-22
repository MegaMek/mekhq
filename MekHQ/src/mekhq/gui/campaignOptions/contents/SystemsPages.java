/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
 * The {@code SystemsPages} class coordinates the Systems section of the MekHQ Campaign Options dialog. It owns the
 * shared {@link SystemsOptionsModel} snapshot and delegates the construction and value synchronisation of each leaf page
 * to a dedicated view class: {@link ReputationPage}, {@link FactionStandingPage}, and {@link ATimeOfWarPage}.
 *
 * <p>On load it builds the model from a {@link CampaignOptions} source (and a {@link RandomSkillPreferences} source) and
 * pushes it into any pages that have already been built; on apply it gathers the page values back into the model, runs
 * any criminal-record reset requested on the Reputation page, and writes the model to the target options. Each leaf page
 * is built lazily the first time its {@code create...Page()} method is called.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class SystemsPages {
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final RandomSkillPreferences randomSkillPreferences;
    private SystemsOptionsModel model;
    private final ReputationPage reputationPage = new ReputationPage();
    private final FactionStandingPage factionStandingPage = new FactionStandingPage();
    private final ATimeOfWarPage aTimeOfWarPage = new ATimeOfWarPage();

    /**
     * Constructs a new {@code SystemsPages} for the specified campaign.
     *
     * @param campaign the campaign associated with this page
     *
     * @author Illiani
     * @since 0.50.07
     */
    public SystemsPages(@Nonnull Campaign campaign) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();
        this.randomSkillPreferences = campaign.getRandomSkillPreferences();
        loadValuesFromCampaignOptions();
    }

    /**
     * Creates the Reputation page panel, containing grouped UI elements for reputation options and its header.
     *
     * @return a {@link JPanel} component representing the entire Reputation page UI
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nonnull JPanel createReputationPage() {
        return reputationPage.createPanel(model);
    }

    /**
     * Creates the Faction Standing page panel, containing grouped UI elements for Faction Standing options and its
     * header.
     *
     * @return a {@link JPanel} component representing the entire Faction Standing page UI
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nonnull JPanel createFactionStandingPage() {
        return factionStandingPage.createPanel(model);
    }

    /**
     * Creates the ATOW page panel, containing grouped UI elements for configuring ATOW-related options and its header.
     *
     * @return a {@link JPanel} component representing the entire ATOW page UI
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nonnull JPanel createATOWPage() {
        return aTimeOfWarPage.createPanel(model);
    }

    /**
     * Loads values from the current campaign or an optional preset campaign options into the UI components, updating
     * their states to match the data.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null);
    }

    /**
     * Loads values from the specified {@code presetCampaignOptions}, or the current campaign's options if {@code null},
     * into the UI form fields and controls.
     *
     * @param presetCampaignOptions        an alternative {@link CampaignOptions}, or {@code null} to use the current
     *                                     campaign's options
     * @param presetRandomSkillPreferences Optional {@code RandomSkillPreferences} object to load values from; if
     *                                     {@code null}, values are loaded from the current skill preferences.
     *
     * @author Illiani
     * @since 0.50.07
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

        model = new SystemsOptionsModel(options, skillPreferences);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the currently selected values in the UI controls to modify the campaign's options. If a preset is
     * provided, that preset is updated instead of the campaign's default options.
     *
     * @param presetCampaignOptions        an alternative {@link CampaignOptions} object to update, or {@code null} to
     *                                     update the campaign's own options
     * @param presetRandomSkillPreferences Optional {@code RandomSkillPreferences} object to set values to; if
     *                                     {@code null}, values are applied to the current skill preferences.
     *
     * @author Illiani
     * @since 0.50.07
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

        if (model.resetCriminalRecord) {
            campaign.setDateOfLastCrime(null);
            campaign.setCrimeRating(0);
            campaign.setCrimePirateModifier(0);
            model.resetCriminalRecord = false;
        }

        model.applyTo(options, skillPreferences);
        reputationPage.updateResetCriminalRecordButtonFromModel();
    }

    private void updateCreatedControlsFromModel() {
        reputationPage.readFromModel(model);
        factionStandingPage.readFromModel(model);
        aTimeOfWarPage.readFromModel(model);
    }

    private void updateModelFromCreatedControls() {
        reputationPage.writeToModel(model);
        factionStandingPage.writeToModel(model);
        aTimeOfWarPage.writeToModel(model);
    }
}
