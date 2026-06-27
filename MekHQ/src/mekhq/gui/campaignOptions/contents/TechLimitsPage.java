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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;

/**
 * The {@code TechLimitsPage} class builds and manages the Tech Limits leaf page of the Campaign Options dialog. It owns
 * the widgets for technology restrictions - year limits, extinct-equipment handling, faction-specific purchase rules,
 * canon-only restrictions, maximum tech level, and ammo-by-type - and synchronises them with a shared
 * {@link EquipmentAndSuppliesOptionsModel}.
 *
 * <p>This view is a sub-component of {@link EquipmentAndSuppliesPages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code EquipmentAndSuppliesPages}, while this class is responsible only for constructing the
 * Tech Limits panel and copying tech-limit values to and from the model. The page is built lazily; until
 * {@link #createPanel(EquipmentAndSuppliesOptionsModel)} is called,
 * {@link #readFromModel(EquipmentAndSuppliesOptionsModel)} and {@link #writeToModel(EquipmentAndSuppliesOptionsModel)}
 * are no-ops.</p>
 */
class TechLimitsPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

    private JCheckBox limitByYearBox;
    private JCheckBox disallowExtinctStuffBox;
    private JCheckBox allowClanPurchasesBox;
    private JCheckBox allowISPurchasesBox;
    private JCheckBox allowCanonOnlyBox;
    private JCheckBox allowCanonRefitOnlyBox;
    private JLabel lblChoiceTechLevel;
    private MMComboBox<String> choiceTechLevel;
    private JCheckBox variableTechLevelBox;
    private JCheckBox useAmmoByTypeBox;

    private boolean created;

    /**
     * Creates and initializes the "Tech Limits" page panel within a user interface. The page includes various settings
     * and options related to technical limitations, such as limiting by year, disallowing extinct technologies,
     * allowing faction-specific purchases, enabling canon-only restrictions, setting maximum tech levels, and more. The
     * method arranges the components in a structured layout and constructs the required parent panel.
     *
     * @param model the shared equipment and supplies options model to populate the freshly built controls from
     *
     * @return the {@code JPanel} representing the "Tech Limits" page, fully configured with its components and layout.
     */
    @Nonnull JPanel createPanel(@Nullable EquipmentAndSuppliesOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_ghost_bear.png";
        CampaignOptionsHeaderPanel techLimitsHeader = new CampaignOptionsHeaderPanel("TechLimitsPage", imageAddress);

        limitByYearBox = new CampaignOptionsCheckBox("LimitByYearBox");
        limitByYearBox.addMouseListener(createTipPanelUpdater("LimitByYearBox"));

        disallowExtinctStuffBox = new CampaignOptionsCheckBox("DisallowExtinctStuffBox");
        disallowExtinctStuffBox.addMouseListener(createTipPanelUpdater("DisallowExtinctStuffBox"));

        allowClanPurchasesBox = new CampaignOptionsCheckBox("AllowClanPurchasesBox");
        allowClanPurchasesBox.addMouseListener(createTipPanelUpdater("AllowClanPurchasesBox"));
        allowISPurchasesBox = new CampaignOptionsCheckBox("AllowISPurchasesBox");
        allowISPurchasesBox.addMouseListener(createTipPanelUpdater("AllowISPurchasesBox"));

        // Canon Purchases/Refits
        allowCanonOnlyBox = new CampaignOptionsCheckBox("AllowCanonOnlyBox");
        allowCanonOnlyBox.addMouseListener(createTipPanelUpdater("AllowCanonOnlyBox"));
        allowCanonRefitOnlyBox = new CampaignOptionsCheckBox("AllowCanonRefitOnlyBox");
        allowCanonRefitOnlyBox.addMouseListener(createTipPanelUpdater("AllowCanonRefitOnlyBox"));

        // Maximum Tech Level
        lblChoiceTechLevel = new CampaignOptionsLabel("ChoiceTechLevel");
        lblChoiceTechLevel.addMouseListener(createTipPanelUpdater("ChoiceTechLevel"));
        choiceTechLevel = new MMComboBox<>("choiceTechLevel", getMaximumTechLevelOptions());
        choiceTechLevel.addMouseListener(createTipPanelUpdater("ChoiceTechLevel"));
        choiceTechLevel.setToolTipText(String.format("<html>%s</html>",
              getTextAt(getCampaignOptionsResourceBundle(), "lblChoiceTechLevel.tooltip")));

        // Variable Tech Level
        variableTechLevelBox = new CampaignOptionsCheckBox("VariableTechLevelBox",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        variableTechLevelBox.addMouseListener(createTipPanelUpdater("VariableTechLevelBox"));

        // Ammo by Type
        useAmmoByTypeBox = new CampaignOptionsCheckBox("UseAmmoByTypeBox",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        useAmmoByTypeBox.addMouseListener(createTipPanelUpdater("UseAmmoByTypeBox"));

        JPanel techLevelPanel = createTechLevelPanel();
        JPanel purchaseRulesPanel = createPurchaseRulesPanel();

        JPanel panel = CampaignOptionsPagePanel.builder("TechLimitsPage", "TechLimitsPage", imageAddress)
                .header(techLimitsHeader)
                .quote("techLimitsPage")
                .section("lblTechLimitsPage.text",
                        "lblTechLimitsPage.summary",
                        techLevelPanel)
                .section("lblTechPurchaseRulesPanel.text",
                        "lblTechPurchaseRulesPanel.summary",
                        purchaseRulesPanel)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    private @Nonnull JPanel createTechLevelPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TechLevelPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addRow(lblChoiceTechLevel, choiceTechLevel);
        panel.addCheckBox(variableTechLevelBox);
        panel.addCheckBox(useAmmoByTypeBox);

        return panel;
    }

    private @Nonnull JPanel createPurchaseRulesPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TechPurchaseRulesPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
              limitByYearBox,
              disallowExtinctStuffBox,
              allowClanPurchasesBox,
              allowISPurchasesBox,
              allowCanonOnlyBox,
              allowCanonRefitOnlyBox);

        return panel;
    }

    /**
     * Creates and returns a DefaultComboBoxModel containing the available options for maximum technology levels.
     *
     * @return A DefaultComboBoxModel<String> populated with the list of technology level names corresponding to the
     *       defined constants in CampaignOptions (e.g., TECH_INTRO, TECH_STANDARD, etc.).
     */
    private static DefaultComboBoxModel<String> getMaximumTechLevelOptions() {
        DefaultComboBoxModel<String> maximumTechLevelModel = new DefaultComboBoxModel<>();

        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_INTRO));
        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_STANDARD));
        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_ADVANCED));
        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_EXPERIMENTAL));
        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_UNOFFICIAL));

        return maximumTechLevelModel;
    }

    /**
     * Copies tech-limit values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared equipment and supplies options model to read values from
     */
    void readFromModel(@Nullable EquipmentAndSuppliesOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        limitByYearBox.setSelected(model.limitByYear);
        disallowExtinctStuffBox.setSelected(model.disallowExtinctStuff);
        allowClanPurchasesBox.setSelected(model.allowClanPurchases);
        allowISPurchasesBox.setSelected(model.allowISPurchases);
        allowCanonOnlyBox.setSelected(model.allowCanonOnly);
        allowCanonRefitOnlyBox.setSelected(model.allowCanonRefitOnly);
        choiceTechLevel.setSelectedIndex(model.techLevel);
        variableTechLevelBox.setSelected(model.variableTechLevel);
        useAmmoByTypeBox.setSelected(model.useAmmoByType);
    }

    /**
     * Copies tech-limit values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared equipment and supplies options model to write values into
     */
    void writeToModel(@Nullable EquipmentAndSuppliesOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.limitByYear = limitByYearBox.isSelected();
        model.disallowExtinctStuff = disallowExtinctStuffBox.isSelected();
        model.allowClanPurchases = allowClanPurchasesBox.isSelected();
        model.allowISPurchases = allowISPurchasesBox.isSelected();
        model.allowCanonOnly = allowCanonOnlyBox.isSelected();
        model.allowCanonRefitOnly = allowCanonRefitOnlyBox.isSelected();
        model.techLevel = choiceTechLevel.getSelectedIndex();
        model.variableTechLevel = variableTechLevelBox.isSelected();
        model.useAmmoByType = useAmmoByTypeBox.isSelected();
    }
}
