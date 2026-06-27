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

/**
 * The FinancesPage class represents a UI page within a larger financial
 * management system for a campaign. It provides
 * panels, checkboxes, spinners, combo boxes, and other controls to manage and
 * configure various financial options,
 * payments, sales, taxes, shares, and price multipliers for the campaign.
 * <p>
 * It is primarily composed of multiple `JPanel` sections organized inside the campaign options page shell for
 * modularity and clarity.
 */
public class FinancesPages {
    private final CampaignOptions campaignOptions;
    private FinancesOptionsModel model;
    private final FinancesGeneralPage financesGeneralPage = new FinancesGeneralPage();
    private final PriceMultipliersPage priceMultipliersPage = new PriceMultipliersPage();

    /**
     * Constructs a `FinancesPage` instance which manages the financial settings and
     * configurations for a specific
     * campaign.
     *
     * @param campaign The `Campaign` object that this `FinancesPage` will be
     *                 associated with. Provides access to
     *                 campaign-related options and data.
     */
    public FinancesPages(@Nonnull Campaign campaign) {
        this.campaignOptions = campaign.getCampaignOptions();

        loadValuesFromCampaignOptions();
    }

    /**
     * Creates and configures the Finances General Options page, assembling its
     * components, layout, and panels which
     * include general options, other systems, payments, and sales. This method
     * initializes required sub-panels and
     * arranges them within the overall structure to create a fully constructed page
     * for financial general options.
     *
     * @return A fully configured JPanel representing the Finances General Options
     *         page.
     */
    public @Nonnull JPanel createFinancesGeneralOptionsPage() {
        return financesGeneralPage.createPanel(model);
    }

    /**
     * Builds the Price Multipliers page.
     *
     * @return a JPanel representing the Price Multipliers page
     */
    public @Nonnull JPanel createPriceMultipliersPage() {
        return priceMultipliersPage.createPanel(model);
    }

    /**
     * Loads configuration values from the current campaign options to populate the
     * financial settings and related UI
     * components in the `FinancesPage`.
     * <p>
     * This method is a convenience overload that invokes the overloaded
     * {@link #loadValuesFromCampaignOptions(CampaignOptions)} method with a `null`
     * parameter, ensuring that default
     * campaign options will be loaded.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads and applies the values from the provided campaign options or the
     * default campaign options if the provided
     * options are null. Updates various UI components and internal variables based
     * on the configuration of the campaign
     * options.
     *
     * @param presetCampaignOptions the campaign options to load values from; if
     *                              null, the default campaign options will
     *                              be used
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new FinancesOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the specified campaign options to the corresponding campaign
     * settings. If no campaign options are
     * provided, default options are used instead.
     *
     * @param presetCampaignOptions The campaign options to be applied. If null,
     *                              default campaign options are applied.
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
        financesGeneralPage.readFromModel(model);
        priceMultipliersPage.readFromModel(model);
    }

    private void updateModelFromCreatedControls() {
        financesGeneralPage.writeToModel(model);
        priceMultipliersPage.writeToModel(model);
    }
}
