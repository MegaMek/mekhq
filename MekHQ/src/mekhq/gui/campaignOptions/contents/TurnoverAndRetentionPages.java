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
import mekhq.campaign.campaignOptions.CampaignOptions;

/**
 * The {@code TurnoverAndRetentionPages} class represents a graphical user
 * interface (GUI) configuration page in the
 * campaign options for managing unit turnover, retention, and fatigue settings.
 * <p>
 * This class provides functionality to define and customize gameplay-related
 * options such as:
 * </p>
 * <ul>
 * <li>Unit turnover settings, including retirement, contract durations,
 * payouts, and modifiers.</li>
 * <li>HR strain and management skills impacting unit cohesion.</li>
 * <li>Fatigue mechanics such as fatigue rates, leave thresholds, and injury
 * fatigue.</li>
 * </ul>
 * <p>
 * The class interacts with a {@link CampaignOptions} object, allowing the user
 * to load and save
 * configurations. It consists of two main panels:
 * </p>
 * <ul>
 * <li><strong>Turnover Page:</strong> Controls unit turnover, payouts, and
 * related modifiers.</li>
 * <li><strong>Fatigue Page:</strong> Manages fatigue-related options like
 * kitchen capacity
 * and fatigue rates.</li>
 * </ul>
 */
public class TurnoverAndRetentionPages {
    private final CampaignOptions campaignOptions;
    private TurnoverAndRetentionOptionsModel model;
    private final TurnoverPage turnoverPage = new TurnoverPage();
    private final FatiguePage fatiguePage = new FatiguePage();

    /**
     * Constructs a {@code TurnoverAndRetentionPages} and initializes the page with the
     * given {@link CampaignOptions}. This
     * sets up necessary UI components and their default configurations.
     *
     * @param campaignOptions the {@code CampaignOptions} instance that holds the
     *                        settings to be modified or displayed
     *                        in this page.
     */
    public TurnoverAndRetentionPages(@Nonnull CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        loadValuesFromCampaignOptions();
    }

    /**
     * Creates and configures the "Fatigue" page with its relevant components. These
     * include options related to enabling
     * fatigue, fatigue rates, injury fatigue, kitchen capacities, and leave
     * thresholds.
     *
     * @return the {@link JPanel} representing the constructed Fatigue page.
     */
    public @Nonnull JPanel createFatiguePage() {
        return fatiguePage.createPanel(model);
    }

    /**
     * Creates and configures the "Turnover" page with its relevant components. These
     * include options for turnover
     * control, random retirement, payout settings, and modifiers for HR Strain and
     * cohesion.
     *
     * @return the {@link JPanel} representing the constructed Turnover page.
     */
    public @Nonnull JPanel createTurnoverPage() {
        return turnoverPage.createPanel(model);
    }

    /**
     * Overload of {@code loadValuesFromCampaignOptions} method. Loads values from
     * the current {@link CampaignOptions}
     * instance.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads the current configuration values from the provided
     * {@link CampaignOptions} object and updates the
     * associated UI components in both the Turnover and Fatigue pages. If no options
     * are provided, the existing campaign
     * options are used.
     *
     * @param presetCampaignOptions the {@link CampaignOptions} instance to load
     *                              settings from, or {@code null} to use
     *                              the current campaign options.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new TurnoverAndRetentionOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the current campaign options based on the configurations in the UI to
     * the given {@link CampaignOptions}.
     * If no options are provided, the current campaign options are updated.
     *
     * @param presetCampaignOptions the {@link CampaignOptions} instance to save
     *                              settings to, or {@code null} to update
     *                              the current campaign options.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        updateModelFromCreatedControls();
        model.applyTo(options);
    }

    private void updateCreatedControlsFromModel() {
        turnoverPage.readFromModel(model);
        fatiguePage.readFromModel(model);
    }

    private void updateModelFromCreatedControls() {
        turnoverPage.writeToModel(model);
        fatiguePage.writeToModel(model);
    }
}
