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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

import java.awt.GridBagConstraints;
import javax.swing.JPanel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * Represents a page in the campaign options UI for managing ruleset
 * configurations in campaigns.
 * <p>
 * This class organizes and manages options related to universal rules, legacy
 * AtB rules (Against the Bot), and StratCon
 * (Strategic Context) settings. It provides a UI to customize configurations
 * such as opponent force generation,
 * scenario rules, equipment behavior, and campaign-specific variations.
 * </p>
 *
 * <strong>Page Sections:</strong>
 * <ul>
 * <li><b>Universal Options:</b> Handles features applicable to all campaigns,
 * such as skill levels, unit ratios, map conditions, and auto-resolve
 * settings.</li>
 * <li><b>Legacy AtB:</b> Legacy-specific rules for opponent force generation,
 * scenario generation probabilities, and battle intensity configurations.</li>
 * <li><b>StratCon:</b> Settings for Strategic Context campaigns, including BV
 * usage
 * (Battle Values) and verbose bidding options.</li>
 * </ul>
 */
public class RulesetsPages {
    private final CampaignOptions campaignOptions;
    private RulesetsOptionsModel model;

    // start Legacy Options
    private CampaignOptionsHeaderPanel legacyHeader;
    // end Legacy Options

    private final StratConPage stratConPage = new StratConPage();

    /**
     * Constructs a {@code RulesetsPages} instance for managing ruleset options.
     *
     * @param campaignOptions the {@link CampaignOptions} object to manage repair,
     *                        maintenance, and other ruleset
     *                        options.
     */
    public RulesetsPages(@Nonnull CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        loadValuesFromCampaignOptions();
    }

    /**
     * Creates the UI panel for the StratCon configuration.
     * <p>
     * This section includes settings for using generic battle values, enabling
     * verbose bidding, and other Strategic
     * Conquest-specific rules.
     * </p>
     *
     * @return a {@link JPanel} containing all StratCon settings.
     */
    public @Nonnull JPanel createStratConPage() {
        return stratConPage.createPanel(model);
    }

    /**
     * Creates the UI panel for the Legacy AtB configuration.
     * <p>
     * This section configures opponent force generation, scenario generation
     * probabilities, and customization of battle
     * intensities for "Against the Bot" campaigns.
     * </p>
     *
     * @return a {@link JPanel} containing all Legacy AtB settings.
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    @SuppressWarnings("removal")
    public @Nonnull JPanel createLegacyPage() {
        // Header
        legacyHeader = new CampaignOptionsHeaderPanel("LegacyPage",
                getImageDirectory() + "logo_free_rasalhague_republic.png",
                true);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("LegacyPage", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(legacyHeader, layout);

        // Create panel and return
        return createParentPanel(panel, "LegacyPage");
    }

    /**
     * A convenience method to load values from the default {@link CampaignOptions}
     * instance.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads the ruleset values from a {@link CampaignOptions} object into the UI
     * components.
     * <p>
     * If no custom {@link CampaignOptions} is provided, it will fetch values from
     * the default {@link CampaignOptions}
     * instance.
     * </p>
     *
     * @param presetCampaignOptions an optional custom {@link CampaignOptions}
     *                              object to load values from; if
     *                              {@code null}, the default options are used.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new RulesetsOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the current values configured in the page back to the provided
     * {@link CampaignOptions}.
     * <p>
     * If no custom {@link CampaignOptions} is provided, it uses the default
     * {@link CampaignOptions} associated with the
     * page.
     * </p>
     *
     * @param presetCampaignOptions an optional custom {@link CampaignOptions}
     *                              object to apply the values to; if
     *                              {@code null}, the default options are used.
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
        stratConPage.readFromModel(model);
    }

    private void updateModelFromCreatedControls() {
        stratConPage.writeToModel(model);
    }

}
