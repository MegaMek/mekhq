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
 * Represents a page in the campaign options UI used to configure settings
 * related to repair and maintenance in a
 * campaign.
 * <p>
 * This page is divided into two sections:
 * </p>
 * <ul>
 * <li><b>Repair Page:</b> Manages options for era modifications, tech
 * assignments,
 * equipment quirks handling, destruction margins, and more.</li>
 * <li><b>Maintenance Page:</b> Handles maintenance settings such as cycle
 * frequency, quality standards,
 * planetary modifiers, and unofficial maintenance rules.</li>
 * </ul>
 * <p>
 * The class interacts with {@link CampaignOptions}, enabling the retrieval,
 * storage,
 * and application of repair and maintenance configuration settings.
 * </p>
 */
public class RepairAndMaintenancePages {
    private final CampaignOptions campaignOptions;
    private RepairAndMaintenanceOptionsModel model;
    private final RepairPage repairPage = new RepairPage();
    private final MaintenancePage maintenancePage = new MaintenancePage();

    /**
     * Constructs a {@code RepairAndMaintenancePages} instance for configuring repair
     * and maintenance-related settings.
     *
     * @param campaignOptions the {@link CampaignOptions} object to be used for
     *                        managing repair and maintenance
     *                        options.
     */
    public RepairAndMaintenancePages(@Nonnull CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        loadValuesFromCampaignOptions();
    }

    /**
     * Creates the panel for the Repair Page.
     * <p>
     * This page provides configurable options for managing repair rules, handling
     * quirks, setting margins for equipment
     * survival, and incorporating era modifications.
     * </p>
     *
     * @return a {@link JPanel} representing the Repair Page.
     */
    public @Nonnull JPanel createRepairPage() {
        return repairPage.createPanel(model);
    }

    /**
     * Creates the panel for the Maintenance Page.
     * <p>
     * This page provides configurable options for managing maintenance cycles,
     * quality standards, planetary effects, and
     * custom rules for units' upkeep.
     * </p>
     *
     * @return a {@link JPanel} representing the Maintenance Page.
     */
    public @Nonnull JPanel createMaintenancePage() {
        return maintenancePage.createPanel(model);
    }

    /**
     * Loads the repair and maintenance settings from the default
     * {@link CampaignOptions} into the page's UI components.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads the repair and maintenance settings from the {@link CampaignOptions}
     * object into the page's UI components.
     * <p>
     * If no custom {@link CampaignOptions} are provided, the default
     * {@link CampaignOptions} associated with this page
     * is used.
     * </p>
     *
     * @param presetCampaignOptions optional custom {@link CampaignOptions} object
     *                              to load settings from. If
     *                              {@code null}, the default options are used.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new RepairAndMaintenanceOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the current page's repair and maintenance settings from the UI
     * components to the provided
     * {@link CampaignOptions}.
     * <p>
     * If no custom {@link CampaignOptions} are provided, uses the default
     * {@link CampaignOptions} associated with this
     * page.
     * </p>
     *
     * @param presetCampaignOptions optional custom {@link CampaignOptions} object
     *                              to apply the current settings to. If
     *                              {@code null}, the default options are modified.
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
        repairPage.readFromModel(model);
        maintenancePage.readFromModel(model);
    }

    private void updateModelFromCreatedControls() {
        repairPage.writeToModel(model);
        maintenancePage.writeToModel(model);
    }
}
