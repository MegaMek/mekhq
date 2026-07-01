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
 * The {@code EquipmentAndSuppliesPages} class represents a graphical user interface (GUI) page containing various options
 * and settings related to equipment and supplies in a campaign simulation. This class is responsible for building and
 * managing multiple sub-pages and panels for customization purposes, including acquisition settings, delivery settings,
 * planetary acquisition settings, and more. It also provides methods to initialize, create, and manage these different
 * components.
 * <p>
 * Fields in this class include labels, spinners, combo boxes, checkboxes, and panels used for displaying and managing
 * options in the page. They allow the user to configure various parameters like transit times, penalties, acquisition
 * limits, faction-specific settings, and modifiers to influence game mechanics. Multiple constants are defined for
 * units of time, representing days, weeks, and months, among others.
 * <p>
 * The class includes initialization methods for different sections of the page, as well as methods to create panels for
 * specific functionality. Utility methods are also provided for configuring spinners and combo boxes or formatting
 * options and labels.
 */
public class EquipmentAndSuppliesPages {
    private final CampaignOptions campaignOptions;
    private EquipmentAndSuppliesOptionsModel model;
    private final AcquisitionPage acquisitionPage = new AcquisitionPage();
    private final PlanetaryAcquisitionPage planetaryAcquisitionPage = new PlanetaryAcquisitionPage();
    private final TechLimitsPage techLimitsPage = new TechLimitsPage();

    /**
     * Constructs the EquipmentAndSuppliesPage with the given campaign options.
     *
     * @param campaignOptions the {@link CampaignOptions} object containing configuration settings for the campaign
     */
    public EquipmentAndSuppliesPages(@Nonnull CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        loadValuesFromCampaignOptions();
    }

    /**
     * Creates and configures the acquisition page panel for the user interface. This method initializes and organizes
     * the components such as the header, acquisition panel, and delivery panel, and then returns the fully constructed
     * acquisition page panel.
     *
     * @return A {@code JPanel} instance representing the complete acquisition page.
     */
    public @Nonnull JPanel createAcquisitionPage() {
        return acquisitionPage.createPanel(model);
    }

    /**
     * Creates and configures the planetary acquisition page panel in a campaign options interface. The panel includes a
     * header, options, and modifiers section, arranged using layout constraints. Once configured, it is wrapped within
     * a parent panel and returned.
     *
     * @return a {@code JPanel} object representing the planetary acquisition page with its configured components and
     *       layout.
     */
    public @Nonnull JPanel createPlanetaryAcquisitionPage() {
        return planetaryAcquisitionPage.createPanel(model);
    }

    /**
     * Creates and initializes the "Tech Limits" page panel within a user interface. The page includes various settings
     * and options related to technical limitations, such as limiting by year, disallowing extinct technologies,
     * allowing faction-specific purchases, enabling canon-only restrictions, setting maximum tech levels, and more. The
     * method arranges the components in a structured layout and constructs the required parent panel.
     *
     * @return the {@code JPanel} representing the "Tech Limits" page, fully configured with its components and layout.
     */
    public @Nonnull JPanel createTechLimitsPage() {
        return techLimitsPage.createPanel(model);
    }

    /**
     * Loads values from the campaign options. This method serves as a convenience method that calls the overloaded
     * version of {@code loadValuesFromCampaignOptions} with a {@code null} parameter.
     * <p>
     * This method is typically used to initialize or update certain settings or configurations based on the campaign
     * options when no specific options are provided.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads values from the provided CampaignOptions instance into the UI components. If the provided CampaignOptions
     * instance is null, it defaults to using the internal campaignOptions instance.
     *
     * @param presetCampaignOptions the CampaignOptions instance containing the preset values to load, or null to use
     *                              the default internal campaignOptions.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new EquipmentAndSuppliesOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the given campaign options to the campaign or uses default options if none are provided. This method
     * updates the campaign settings for acquisitions, deliveries, planetary acquisitions, and technological limits to
     * customize campaign behavior.
     *
     * @param presetCampaignOptions the campaign options to apply; if null, default campaign options are used instead
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
        acquisitionPage.readFromModel(model);
        planetaryAcquisitionPage.readFromModel(model);
        techLimitsPage.readFromModel(model);
    }

    private void updateModelFromCreatedControls() {
        acquisitionPage.writeToModel(model);
        planetaryAcquisitionPage.writeToModel(model);
        techLimitsPage.writeToModel(model);
    }
}
