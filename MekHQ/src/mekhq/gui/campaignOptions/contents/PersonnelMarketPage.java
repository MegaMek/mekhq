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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;

/**
 * The {@code PersonnelMarketPage} class builds and manages the Personnel Market leaf page of the Campaign Options
 * dialog. It owns the widgets for personnel market configuration - the market style and the report-refresh and
 * hiring-hall-only toggles - and synchronises them with a shared {@link MarketsOptionsModel}.
 *
 * <p>This view is a sub-component of {@link MarketsPages}: the model snapshot and the overall load/apply lifecycle still
 * live on {@code MarketsPages}, while this class is responsible only for constructing the Personnel Market panel and
 * copying personnel market values to and from the model. The page is built lazily; until
 * {@link #createPanel(MarketsOptionsModel)} is called, {@link #readFromModel(MarketsOptionsModel)} and
 * {@link #writeToModel(MarketsOptionsModel)} are no-ops.</p>
 */
class PersonnelMarketPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int CHECKBOX_GRID_COLUMNS = 2;

    private CampaignOptionsHeaderPanel personnelMarketHeader;
    private JPanel pnlPersonnelMarketGeneralOptions;
    private JLabel lblPersonnelMarketStyle;
    private MMComboBox<PersonnelMarketStyle> comboPersonnelMarketStyle;
    private JCheckBox chkPersonnelMarketReportRefresh;
    private JCheckBox chkUsePersonnelHireHiringHallOnly;

    private boolean created;

    /**
     * Creates and returns the JPanel representing the Personnel Market configuration page.
     * <p>
     * This page includes general personnel market settings, as well as removal target configuration options for various
     * skill levels.
     *
     * @param model the shared markets options model to populate the freshly built controls from
     *
     * @return A {@link JPanel} for the Personnel Market configuration page.
     */
    @Nonnull JPanel createPanel(@Nullable MarketsOptionsModel model) {
        comboPersonnelMarketStyle = new MMComboBox<>("comboPersonnelMarketStyle", PersonnelMarketStyle.values());

        // Header
        String imageAddress = getImageDirectory() + "logo_st_ives_compact.png";
        personnelMarketHeader = new CampaignOptionsHeaderPanel("PersonnelMarketPage", imageAddress);

        // Contents
        pnlPersonnelMarketGeneralOptions = createPersonnelMarketGeneralOptionsPanel();

        final JPanel panel = CampaignOptionsPagePanel.builder("PersonnelMarketPage", "PersonnelMarketPage",
                imageAddress)
                .header(personnelMarketHeader)
                .quote("personnelMarketPage")
                .section("lblPersonnelMarketGeneralOptionsPanel.text",
                        "lblPersonnelMarketGeneralOptionsPanel.summary",
                        pnlPersonnelMarketGeneralOptions)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Builds the general options panel for the Personnel Market page, which includes the market style selector and the
     * report-refresh and hiring-hall-only toggles.
     * <p>
     * These components are laid out into a panel and returned for use in the UI.
     *
     * @return A {@link JPanel} representing the general options within the Personnel Market page.
     */
    private @Nonnull JPanel createPersonnelMarketGeneralOptionsPanel() {
        // Contents
        lblPersonnelMarketStyle = new CampaignOptionsLabel("PersonnelMarketStyle",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.RECOMMENDED));
        lblPersonnelMarketStyle
                .addMouseListener(createTipPanelUpdater("PersonnelMarketStyle"));
        comboPersonnelMarketStyle.addMouseListener(createTipPanelUpdater("PersonnelMarketStyle"));

        chkPersonnelMarketReportRefresh = new CampaignOptionsCheckBox("PersonnelMarketReportRefresh");
        chkPersonnelMarketReportRefresh.addMouseListener(createTipPanelUpdater("PersonnelMarketReportRefresh"));

        chkUsePersonnelHireHiringHallOnly = new CampaignOptionsCheckBox("UsePersonnelHireHiringHallOnly",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUsePersonnelHireHiringHallOnly.addMouseListener(createTipPanelUpdater("UsePersonnelHireHiringHallOnly"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PersonnelMarketGeneralOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblPersonnelMarketStyle, comboPersonnelMarketStyle);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkPersonnelMarketReportRefresh,
                chkUsePersonnelHireHiringHallOnly);

        return panel;
    }

    /**
     * Copies personnel market values from the shared model into this page's controls. This is a no-op until the page has
     * been built.
     *
     * @param model the shared markets options model to read values from
     */
    void readFromModel(@Nullable MarketsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        comboPersonnelMarketStyle.setSelectedItem(model.personnelMarketStyle);
        chkPersonnelMarketReportRefresh.setSelected(model.personnelMarketReportRefresh);
        chkUsePersonnelHireHiringHallOnly.setSelected(model.usePersonnelHireHiringHallOnly);
    }

    /**
     * Copies personnel market values from this page's controls into the shared model. This is a no-op until the page has
     * been built.
     *
     * @param model the shared markets options model to write values into
     */
    void writeToModel(@Nullable MarketsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.personnelMarketStyle = comboPersonnelMarketStyle.getSelectedItem();
        model.personnelMarketReportRefresh = chkPersonnelMarketReportRefresh.isSelected();
        model.usePersonnelHireHiringHallOnly = chkUsePersonnelHireHiringHallOnly.isSelected();
    }
}
