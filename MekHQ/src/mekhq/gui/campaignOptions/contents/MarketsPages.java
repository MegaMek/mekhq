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
 * The {@code MarketsPages} class coordinates the Markets section of the MekHQ Campaign Options dialog. It owns the
 * shared {@link MarketsOptionsModel} snapshot and delegates the construction and value synchronisation of each leaf
 * page to a dedicated view class: {@link PersonnelMarketPage}, {@link UnitMarketPage}, and {@link ContractMarketPage}.
 *
 * <p>On load it builds the model from a {@link CampaignOptions} source and pushes it into any pages that have already
 * been built; on apply it gathers the page values back into the model and writes them to the target options. Each leaf
 * page is built lazily the first time its {@code create...Page()} method is called.</p>
 *
 * <p>This class interacts with {@link CampaignOptions} to retrieve or update the persistent campaign settings.</p>
 */
public class MarketsPages {
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private MarketsOptionsModel model;
    private final PersonnelMarketPage personnelMarketPage = new PersonnelMarketPage();
    private final UnitMarketPage unitMarketPage = new UnitMarketPage();
    private final ContractMarketPage contractMarketPage = new ContractMarketPage();

    /**
     * Constructs a {@code MarketsPages} with the provided campaign. Initializes the market configuration options based on
     * the settings of the given {@link Campaign}.
     *
     * @param campaign The {@link Campaign} associated with this market page. This campaign is used to retrieve and
     *                 modify {@link CampaignOptions}.
     */
    public MarketsPages(@Nonnull Campaign campaign) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();

        loadValuesFromCampaignOptions();
    }

    /**
     * Creates and returns the JPanel representing the Personnel Market configuration page.
     * <p>
     * This page includes general personnel market settings, as well as removal target configuration options for various
     * skill levels.
     *
     * @return A {@link JPanel} for the Personnel Market configuration page.
     */
    public @Nonnull JPanel createPersonnelMarketPage() {
        return personnelMarketPage.createPanel(model);
    }

    /**
     * Creates and returns the JPanel representing the Unit Market configuration page.
     * <p>
     * This page includes options such as unit market methods, rarity modifiers, special unit change settings, and more.
     *
     * @return A {@link JPanel} for the Unit Market configuration page.
     */
    public @Nonnull JPanel createUnitMarketPage() {
        return unitMarketPage.createPanel(model);
    }

    /**
     * Creates and returns the JPanel representing the Contract Market configuration page.
     * <p>
     * This page includes settings for configuring various aspects of contract acquisition, such as methods, search
     * radius, payment options, and variable contract length.
     *
     * @return A {@link JPanel} for the Contract Market configuration page.
     */
    public @Nonnull JPanel createContractMarketPage() {
        return contractMarketPage.createPanel(model);
    }

    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads the campaign options from the associated {@link Campaign} into the UI components of the market pages. This
     * includes personnel, unit, and contract market settings.
     * <p>
     * If no preset options are provided, the current campaign options are loaded.
     *
     * @param presetCampaignOptions A {@link CampaignOptions} object with previously configured settings, or
     *                              {@code null} to use the current campaign's options.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new MarketsOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        updateModelFromCreatedControls();
        model.applyTo(campaign, options);
    }

    private void updateCreatedControlsFromModel() {
        personnelMarketPage.readFromModel(model);
        unitMarketPage.readFromModel(model);
        contractMarketPage.readFromModel(model);
    }

    private void updateModelFromCreatedControls() {
        personnelMarketPage.writeToModel(model);
        unitMarketPage.writeToModel(model);
        contractMarketPage.writeToModel(model);
    }
}
