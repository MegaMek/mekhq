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
import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;

/**
 * The {@code PersonnelPages} class coordinates the Personnel section of the MekHQ Campaign Options dialog. It owns the
 * shared {@link PersonnelOptionsModel} snapshot and delegates the construction and value synchronisation of each leaf
 * page to a dedicated view class: {@link PersonnelGeneralPage}, {@link AwardsPage}, {@link MedicalPage},
 * {@link PersonnelInformationPage}, and {@link PrisonersAndDependentsPage}.
 *
 * <p>On load it builds the model from a {@link CampaignOptions} source and pushes it into any pages that have already
 * been built; on apply it gathers the page values back into the model and writes them to the target options. Each leaf
 * page is built lazily the first time its {@code create...Page()} method is called.</p>
 */
public class PersonnelPages {
    private final CampaignOptions campaignOptions;
    private PersonnelOptionsModel model;
    private final AwardsPage awardsPage = new AwardsPage();
    private final MedicalPage medicalPage = new MedicalPage();
    private final PrisonersAndDependentsPage prisonersAndDependentsPage = new PrisonersAndDependentsPage();
    private final PersonnelGeneralPage personnelGeneralPage = new PersonnelGeneralPage();
    private final PersonnelInformationPage personnelInformationPage = new PersonnelInformationPage();

    /**
     * Constructs the {@code PersonnelPages} object with the given campaign options.
     *
     * @param campaignOptions the {@link CampaignOptions} instance to be used for
     *                        initializing and managing personnel
     *                        options.
     */
    public PersonnelPages(@Nonnull CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        loadValuesFromCampaignOptions(new Version());
    }

    /**
     * Creates the components and layout for the General Page, organizing personnel
     * management settings into specific
     * groups.
     *
     * @return a {@link JPanel} representing the General Page.
     */
    public @Nonnull JPanel createGeneralPage() {
        return personnelGeneralPage.createPanel(model);
    }

    /**
     * Creates the panels and layout for the Awards Page, including its general and
     * filter components.
     *
     * @return a {@link JPanel} representing the Awards Page.
     */
    public @Nonnull JPanel createAwardsPage() {
        return awardsPage.createPanel(model);
    }

    /**
     * Creates the layout for the Medical Page, combining components related to
     * medical settings.
     *
     * @return a {@link JPanel} containing medical-related settings.
     */
    public @Nonnull JPanel createMedicalPage() {
        return medicalPage.createPanel(model);
    }

    /**
     * Creates the layout for the Personnel Information Page, including its
     * components for displaying personnel
     * information and logs.
     *
     * @return a {@link JPanel} representing the Personnel Information Page.
     */
    public @Nonnull JPanel createPersonnelInformationPage() {
        return personnelInformationPage.createPanel(model);
    }

    /**
     * Creates the layout for the Prisoners and Dependents Page, organizing settings
     * for prisoner handling and dependent
     * management.
     *
     * @return a {@link JPanel} containing the Prisoners and Dependents Page
     *         components.
     */
    public @Nonnull JPanel createPrisonersAndDependentsPage() {
        return prisonersAndDependentsPage.createPanel(model);
    }

    /**
     * Shortcut method to load default {@link CampaignOptions} values into the page
     * components.
     */
    public void loadValuesFromCampaignOptions(Version version) {
        loadValuesFromCampaignOptions(null, version);
    }

    /**
     * Loads and applies configuration values from the provided
     * {@link CampaignOptions} object, or uses the default
     * campaign options if none are provided. The configuration includes general
     * settings, personnel logs, personnel
     * information, awards, medical settings, prisoner and dependent settings, and
     * salary-related options. It also
     * adjusts certain values based on the version of the application.
     *
     * @param presetCampaignOptions the {@link CampaignOptions} object to load
     *                              settings from. If null, default campaign
     *                              options will be used.
     * @param version               the version of the application, used to
     *                              determine adjustments for compatibility.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions, Version version) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new PersonnelOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the modified personnel page settings to the repository's campaign
     * options. If no preset
     * {@link CampaignOptions} is provided, the changes are applied to the current
     * options.
     *
     * @param campaign              the {@link Campaign} object, representing the
     *                              current campaign state.
     * @param presetCampaignOptions optional custom {@link CampaignOptions} to apply
     *                              changes to.
     */
    public void applyCampaignOptionsToCampaign(@Nonnull Campaign campaign, @Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        updateModelFromCreatedControls();
        model.applyTo(campaign, options);
    }

    private void updateCreatedControlsFromModel() {
        personnelGeneralPage.readFromModel(model);
        awardsPage.readFromModel(model);
        medicalPage.readFromModel(model);
        personnelInformationPage.readFromModel(model);
        prisonersAndDependentsPage.readFromModel(model);
    }

    private void updateModelFromCreatedControls() {
        personnelGeneralPage.writeToModel(model);
        awardsPage.writeToModel(model);
        medicalPage.writeToModel(model);
        personnelInformationPage.writeToModel(model);
        prisonersAndDependentsPage.writeToModel(model);
    }
}
