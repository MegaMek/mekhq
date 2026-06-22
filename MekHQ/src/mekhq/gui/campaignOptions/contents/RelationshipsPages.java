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
 * Represents a page in the campaign options UI for configuring
 * relationship-related options, such as marriage, divorce,
 * and procreation settings.
 * <p>
 * This page allows users to manage manual and random settings for the
 * relationships between personnel in a campaign,
 * applying user-defined rules and configurations. The class generates UI
 * components for the respective configurations
 * and interacts with {@link CampaignOptions} to store and apply these settings.
 * </p>
 * <p>
 * The page is divided into three main sections:
 * </p>
 * <ul>
 * <li>Marriage Page: Manages configurations for manual and random marriage
 * settings.</li>
 * <li>Divorce Page: Manages configurations for manual and random divorce
 * settings.</li>
 * <li>Procreation Page: Manages configurations for manual and random procreation
 * settings.</li>
 * </ul>
 */
public class RelationshipsPages {
    private final CampaignOptions campaignOptions;
    private RelationshipsOptionsModel model;
    private final MarriagePage marriagePage = new MarriagePage();
    private final DivorcePage divorcePage = new DivorcePage();
    private final ProcreationPage procreationPage = new ProcreationPage();

    /**
     * Constructs a {@code RelationshipsPages} instance for configuring
     * relationships-related campaign options.
     *
     * @param campaignOptions the {@link CampaignOptions} instance to be used for
     *                        managing relationship settings.
     */
    public RelationshipsPages(@Nonnull CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        loadValuesFromCampaignOptions();
    }

    /**
     * Creates the UI for the Marriage Page, including components for managing manual
     * and random marriage options.
     *
     * @return a {@link JPanel} representing the Marriage Page.
     */
    public @Nonnull JPanel createMarriagePage() {
        return marriagePage.createPanel(model);
    }

    /**
     * Creates the UI for the Divorce Page, including components for managing manual
     * and random divorce options.
     *
     * @return a {@link JPanel} representing the Divorce Page.
     */
    public @Nonnull JPanel createDivorcePage() {
        return divorcePage.createPanel(model);
    }

    /**
     * Creates the UI for the Procreation Page, including components for managing
     * manual and random procreation options.
     *
     * @return a {@link JPanel} representing the Procreation Page.
     */
    public @Nonnull JPanel createProcreationPage() {
        return procreationPage.createPanel(model);
    }

    /**
     * Loads the default {@link CampaignOptions} values into the RelationshipsPage
     * components. This is a shortcut for
     * calling {@link #loadValuesFromCampaignOptions(CampaignOptions)} with
     * {@code null}.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads values from the specified {@link CampaignOptions} instance into the
     * RelationshipsPage components. If no
     * custom options are provided, the current {@link CampaignOptions} instance is
     * used.
     *
     * @param presetCampaignOptions optional custom {@link CampaignOptions} to load.
     *                              If {@code null}, default options
     *                              are used.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new RelationshipsOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the current settings from the RelationshipsPage components to the
     * specified {@link CampaignOptions}. If no
     * custom options are provided, changes are applied to the current
     * {@link CampaignOptions} instance.
     *
     * @param presetCampaignOptions optional custom {@link CampaignOptions} to apply
     *                              changes to. If {@code null},
     *                              default options are used.
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
        marriagePage.readFromModel(model);
        divorcePage.readFromModel(model);
        procreationPage.readFromModel(model);
    }

    private void updateModelFromCreatedControls() {
        marriagePage.writeToModel(model);
        divorcePage.writeToModel(model);
        procreationPage.writeToModel(model);
    }
}
