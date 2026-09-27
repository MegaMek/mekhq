/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.settings.SettingsFormPanel;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The Roleplay page of the Campaign Options dialog: options for the solo-roleplay Oracle console and its journal.
 *
 * <p>Like {@link AttributesAndTraitsPage}, the page is self-contained: it keeps the values it loaded and exposes the
 * load/apply lifecycle directly to {@code CampaignOptionsPane}. It is built lazily; until {@link #createPage()} is
 * called, the loaded values are simply held and written back unchanged.</p>
 */
public class RoleplayPage {
    private static final int FORM_LABEL_COLUMN_WIDTH = SettingsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int FORM_CONTROL_COLUMN_WIDTH = SettingsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final Version INTRODUCED = new Version(0, 51, 1);

    private final CampaignOptions campaignOptions;
    private int maximumOracleLogEntries;
    private boolean useOracleChronicle;

    private JSpinner spnMaximumOracleLogEntries;
    private JCheckBox chkUseOracleChronicle;
    private boolean created;

    /**
     * @param campaignOptions the campaign's options, which back this page
     */
    public RoleplayPage(@Nonnull CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Creates the page: its header, then the journal options.
     *
     * @return the page
     */
    public @Nonnull JPanel createPage() {
        String imageAddress = getImageDirectory() + "logo_comstar.png";
        CampaignOptionsHeaderPanel header = new CampaignOptionsHeaderPanel("RoleplayPage", imageAddress);

        JPanel panel = CampaignOptionsPagePanel.builder("RoleplayPage", "RoleplayPage", imageAddress)
                             .header(header)
                             .quote("roleplayPage")
                             .section("lblOracleSection.text", "lblOracleSection.summary", createJournalPanel())
                             .build();

        created = true;
        readValues();
        return panel;
    }

    private @Nonnull JPanel createJournalPanel() {
        JLabel lblMaximumOracleLogEntries = new CampaignOptionsLabel("MaximumOracleLogEntries",
              getMetadata(INTRODUCED));
        lblMaximumOracleLogEntries.addMouseListener(createTipPanelUpdater("MaximumOracleLogEntries"));
        spnMaximumOracleLogEntries = new CampaignOptionsSpinner("MaximumOracleLogEntries", 500, 1, 100000, 50);
        spnMaximumOracleLogEntries.addMouseListener(createTipPanelUpdater("MaximumOracleLogEntries"));
        chkUseOracleChronicle = new CampaignOptionsCheckBox("UseOracleChronicle", getMetadata(INTRODUCED));
        chkUseOracleChronicle.addMouseListener(createTipPanelUpdater("UseOracleChronicle"));

        SettingsFormPanel panel = new SettingsFormPanel("OraclePanel", FORM_LABEL_COLUMN_WIDTH,
              FORM_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblMaximumOracleLogEntries, spnMaximumOracleLogEntries);
        panel.addCheckBox(chkUseOracleChronicle);
        return panel;
    }

    /**
     * Loads values from the given options, or the campaign's own if {@code null}.
     *
     * @param presetCampaignOptions options to load from, such as a preset, or {@code null}
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = (presetCampaignOptions != null) ? presetCampaignOptions : campaignOptions;
        maximumOracleLogEntries = options.get(CampaignOption.MAXIMUM_ORACLE_LOG_ENTRIES);
        useOracleChronicle = options.get(CampaignOption.USE_ORACLE_CHRONICLE);
        readValues();
    }

    /**
     * Writes this page's values to the given options, or the campaign's own if {@code null}.
     *
     * @param presetCampaignOptions options to write to, such as a preset, or {@code null}
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = (presetCampaignOptions != null) ? presetCampaignOptions : campaignOptions;
        if (created) {
            maximumOracleLogEntries = (int) spnMaximumOracleLogEntries.getValue();
            useOracleChronicle = chkUseOracleChronicle.isSelected();
        }
        options.set(CampaignOption.MAXIMUM_ORACLE_LOG_ENTRIES, maximumOracleLogEntries);
        options.set(CampaignOption.USE_ORACLE_CHRONICLE, useOracleChronicle);
    }

    private void readValues() {
        if (!created) {
            return;
        }
        spnMaximumOracleLogEntries.setValue(maximumOracleLogEntries);
        chkUseOracleChronicle.setSelected(useOracleChronicle);
    }
}
