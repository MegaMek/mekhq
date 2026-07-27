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

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.personnel.enums.AwardBonus;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code AwardsView} class builds and manages the Awards leaf page of the Campaign Options dialog. It owns the
 * widgets for award configuration - bonus style, tier size, auto-awards issuance toggles, the auto-award type filters,
 * and the award-set filter list - and synchronises them with a shared {@link PersonnelOptionsModel}.
 *
 * <p>This view is a sub-component of {@link PersonnelPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code PersonnelPages}, while this class is responsible only for constructing the Awards panel and
 * copying award values to and from the model. The page is built lazily; until
 * {@link #createPanel(PersonnelOptionsModel)} is called, {@link #readFromModel(PersonnelOptionsModel)} and
 * {@link #writeToModel(PersonnelOptionsModel)} are no-ops.</p>
 */
class AwardsPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

    private CampaignOptionsHeaderPanel awardsHeader;
    private JPanel pnlAwardsGeneralOptions;
    private JLabel lblAwardBonusStyle;
    private MMComboBox<AwardBonus> comboAwardBonusStyle;
    private JCheckBox chkUseReplaceEdgeAwards;
    private JLabel lblAwardTierSize;
    private JSpinner spnAwardTierSize;
    private JCheckBox chkEnableAutoAwards;
    private JCheckBox chkIssuePosthumousAwards;
    private JCheckBox chkIssueBestAwardOnly;
    private JCheckBox chkIgnoreStandardSet;

    private JPanel pnlAutoAwardsFilter;
    private JCheckBox chkEnableContractAwards;
    private JCheckBox chkEnableFactionHunterAwards;
    private JCheckBox chkEnableInjuryAwards;
    private JCheckBox chkEnableIndividualKillAwards;
    private JCheckBox chkEnableFormationKillAwards;
    private JCheckBox chkEnableRankAwards;
    private JCheckBox chkEnableScenarioAwards;
    private JCheckBox chkEnableSkillAwards;
    private JCheckBox chkEnableTheatreOfWarAwards;
    private JCheckBox chkEnableTimeAwards;
    private JCheckBox chkEnableTrainingAwards;
    private JCheckBox chkEnableMiscAwards;
    private JTextArea txtAwardSetFilterList;

    private boolean created;

    /**
     * Builds the Awards page, populates its controls from the supplied model, and returns the assembled panel.
     *
     * @param model the shared personnel options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Awards Page
     */
    @Nonnull
    JPanel createPanel(@Nullable PersonnelOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_outworld_alliance.png";
        awardsHeader = new CampaignOptionsHeaderPanel("AwardsPage", imageAddress);

        // Contents
        comboAwardBonusStyle = new MMComboBox<>("comboAwardBonusStyle", AwardBonus.values());
        pnlAwardsGeneralOptions = createAwardsGeneralOptionsPanel();
        pnlAutoAwardsFilter = createAutoAwardsFilterPanel();

        txtAwardSetFilterList = new JTextArea(10, 1);
        txtAwardSetFilterList.setLineWrap(true);
        txtAwardSetFilterList.setWrapStyleWord(true);
        txtAwardSetFilterList.addMouseListener(createTipPanelUpdater("AwardSetFilterList"));
        txtAwardSetFilterList.setToolTipText(wordWrap(getTextAt(getCampaignOptionsResourceBundle(),
              "lblAwardSetFilterList.tooltip")));
        txtAwardSetFilterList.setName("txtAwardSetFilterList");
        txtAwardSetFilterList.setText("");
        JScrollPane scrollAwardSetFilterList = new FastJScrollPane(txtAwardSetFilterList);
        scrollAwardSetFilterList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollAwardSetFilterList.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        JPanel pnlAwardSetFilter = createAwardSetFilterPanel(scrollAwardSetFilterList);
        JPanel panel = CampaignOptionsPagePanel.builder("AwardsPage", "AwardsPage", imageAddress)
                             .header(awardsHeader)
                             .quote("awardsPage")
                             .section("lblAwardsPage.text", "lblAwardsPage.summary", pnlAwardsGeneralOptions)
                             .section("lblAutoAwardsFilterPanel.text",
                                   "lblAutoAwardsFilterPanel.summary",
                                   pnlAutoAwardsFilter)
                             .section("lblAwardsPageBottom.text",
                                   "lblAwardsPageBottom.summary",
                                   pnlAwardSetFilter,
                                   getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT))
                             .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates the panel for general award configuration settings in the Awards Page.
     *
     * @return a {@link JPanel} containing settings for awards, such as bonus style and auto awards
     */
    private JPanel createAwardsGeneralOptionsPanel() {
        // Contents
        lblAwardBonusStyle = new CampaignOptionsLabel("AwardBonusStyle");
        lblAwardBonusStyle.addMouseListener(createTipPanelUpdater("AwardBonusStyle"));
        comboAwardBonusStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                  final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AwardBonus) {
                    list.setToolTipText(((AwardBonus) value).getToolTipText());
                }
                return this;
            }
        });
        comboAwardBonusStyle.addMouseListener(createTipPanelUpdater("AwardBonusStyle"));

        lblAwardTierSize = new CampaignOptionsLabel("AwardTierSize");
        lblAwardTierSize.addMouseListener(createTipPanelUpdater("AwardTierSize"));
        spnAwardTierSize = new CampaignOptionsSpinner("AwardTierSize", 5, 1, 100, 1);
        spnAwardTierSize.addMouseListener(createTipPanelUpdater("AwardTierSize"));

        chkEnableAutoAwards = new CampaignOptionsCheckBox("EnableAutoAwards");
        chkEnableAutoAwards.addMouseListener(createTipPanelUpdater("EnableAutoAwards"));

        chkUseReplaceEdgeAwards = new CampaignOptionsCheckBox("UseReplaceEdgeAwards",
              getMetadata(new Version(0, 51, 1)));
        chkUseReplaceEdgeAwards.addMouseListener(createTipPanelUpdater("UseReplaceEdgeAwards"));

        chkIssuePosthumousAwards = new CampaignOptionsCheckBox("IssuePosthumousAwards");
        chkIssuePosthumousAwards.addMouseListener(createTipPanelUpdater("IssuePosthumousAwards"));

        chkIssueBestAwardOnly = new CampaignOptionsCheckBox("IssueBestAwardOnly");
        chkIssueBestAwardOnly.addMouseListener(createTipPanelUpdater("IssueBestAwardOnly"));

        chkIgnoreStandardSet = new CampaignOptionsCheckBox("IgnoreStandardSet");
        chkIgnoreStandardSet.addMouseListener(createTipPanelUpdater("IgnoreStandardSet"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AwardsGeneralOptionsPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addRow(lblAwardBonusStyle, comboAwardBonusStyle);
        panel.addRow(lblAwardTierSize, spnAwardTierSize);
        panel.addCheckBoxGrid(2,
              chkUseReplaceEdgeAwards,
              chkEnableAutoAwards,
              chkIssuePosthumousAwards,
              chkIssueBestAwardOnly,
              chkIgnoreStandardSet);

        return panel;
    }

    /**
     * Creates the panel for filtering auto-awards settings in the Awards Page.
     *
     * @return a {@link JPanel} containing checkboxes for various award filters
     */
    private @Nonnull JPanel createAutoAwardsFilterPanel() {
        // Contents
        chkEnableContractAwards = new CampaignOptionsCheckBox("EnableContractAwards");
        chkEnableContractAwards.addMouseListener(createTipPanelUpdater("EnableContractAwards"));
        chkEnableFactionHunterAwards = new CampaignOptionsCheckBox("EnableFactionHunterAwards",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkEnableFactionHunterAwards
              .addMouseListener(createTipPanelUpdater("EnableFactionHunterAwards"));
        chkEnableInjuryAwards = new CampaignOptionsCheckBox("EnableInjuryAwards",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkEnableInjuryAwards.addMouseListener(createTipPanelUpdater("EnableInjuryAwards"));
        chkEnableIndividualKillAwards = new CampaignOptionsCheckBox("EnableIndividualKillAwards");
        chkEnableIndividualKillAwards.addMouseListener(createTipPanelUpdater("EnableIndividualKillAwards"));
        chkEnableFormationKillAwards = new CampaignOptionsCheckBox("EnableFormationKillAwards");
        chkEnableFormationKillAwards
              .addMouseListener(createTipPanelUpdater("EnableFormationKillAwards"));
        chkEnableRankAwards = new CampaignOptionsCheckBox("EnableRankAwards");
        chkEnableRankAwards.addMouseListener(createTipPanelUpdater("EnableRankAwards"));
        chkEnableScenarioAwards = new CampaignOptionsCheckBox("EnableScenarioAwards");
        chkEnableScenarioAwards.addMouseListener(createTipPanelUpdater("EnableScenarioAwards"));
        chkEnableSkillAwards = new CampaignOptionsCheckBox("EnableSkillAwards");
        chkEnableSkillAwards.addMouseListener(createTipPanelUpdater("EnableSkillAwards"));
        chkEnableTheatreOfWarAwards = new CampaignOptionsCheckBox("EnableTheatreOfWarAwards");
        chkEnableTheatreOfWarAwards
              .addMouseListener(createTipPanelUpdater("EnableTheatreOfWarAwards"));
        chkEnableTimeAwards = new CampaignOptionsCheckBox("EnableTimeAwards",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkEnableTimeAwards.addMouseListener(createTipPanelUpdater("EnableTimeAwards"));
        chkEnableTrainingAwards = new CampaignOptionsCheckBox("EnableTrainingAwards",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkEnableTrainingAwards.addMouseListener(createTipPanelUpdater("EnableTrainingAwards"));
        chkEnableMiscAwards = new CampaignOptionsCheckBox("EnableMiscAwards");
        chkEnableMiscAwards.addMouseListener(createTipPanelUpdater("EnableMiscAwards"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AutoAwardsFilterPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
              chkEnableContractAwards,
              chkEnableFactionHunterAwards,
              chkEnableInjuryAwards,
              chkEnableIndividualKillAwards,
              chkEnableFormationKillAwards,
              chkEnableRankAwards,
              chkEnableScenarioAwards,
              chkEnableSkillAwards,
              chkEnableTheatreOfWarAwards,
              chkEnableTimeAwards,
              chkEnableTrainingAwards,
              chkEnableMiscAwards);

        return panel;
    }

    private @Nonnull JPanel createAwardSetFilterPanel(JScrollPane scrollAwardSetFilterList) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName("pnlAwardSetFilterPanel");
        panel.setOpaque(false);
        panel.add(scrollAwardSetFilterList, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Copies award values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared personnel options model to read values from
     */
    void readFromModel(@Nullable PersonnelOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        comboAwardBonusStyle.setSelectedItem(model.awardBonusStyle);
        spnAwardTierSize.setValue(model.awardTierSize);
        chkEnableAutoAwards.setSelected(model.enableAutoAwards);
        chkUseReplaceEdgeAwards.setSelected(model.useReplaceEdgeAwards);
        chkIssuePosthumousAwards.setSelected(model.issuePosthumousAwards);
        chkIssueBestAwardOnly.setSelected(model.issueBestAwardOnly);
        chkIgnoreStandardSet.setSelected(model.ignoreStandardSet);
        chkEnableContractAwards.setSelected(model.enableContractAwards);
        chkEnableFactionHunterAwards.setSelected(model.enableFactionHunterAwards);
        chkEnableInjuryAwards.setSelected(model.enableInjuryAwards);
        chkEnableIndividualKillAwards.setSelected(model.enableIndividualKillAwards);
        chkEnableFormationKillAwards.setSelected(model.enableFormationKillAwards);
        chkEnableRankAwards.setSelected(model.enableRankAwards);
        chkEnableScenarioAwards.setSelected(model.enableScenarioAwards);
        chkEnableSkillAwards.setSelected(model.enableSkillAwards);
        chkEnableTheatreOfWarAwards.setSelected(model.enableTheatreOfWarAwards);
        chkEnableTimeAwards.setSelected(model.enableTimeAwards);
        chkEnableTrainingAwards.setSelected(model.enableTrainingAwards);
        chkEnableMiscAwards.setSelected(model.enableMiscAwards);
        txtAwardSetFilterList.setText(model.awardSetFilterList);
    }

    /**
     * Copies award values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared personnel options model to write values into
     */
    void writeToModel(@Nullable PersonnelOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.awardBonusStyle = comboAwardBonusStyle.getSelectedItem();
        model.awardTierSize = (int) spnAwardTierSize.getValue();
        model.enableAutoAwards = chkEnableAutoAwards.isSelected();
        model.useReplaceEdgeAwards = chkUseReplaceEdgeAwards.isSelected();
        model.issuePosthumousAwards = chkIssuePosthumousAwards.isSelected();
        model.issueBestAwardOnly = chkIssueBestAwardOnly.isSelected();
        model.ignoreStandardSet = chkIgnoreStandardSet.isSelected();
        model.enableContractAwards = chkEnableContractAwards.isSelected();
        model.enableFactionHunterAwards = chkEnableFactionHunterAwards.isSelected();
        model.enableInjuryAwards = chkEnableInjuryAwards.isSelected();
        model.enableIndividualKillAwards = chkEnableIndividualKillAwards.isSelected();
        model.enableFormationKillAwards = chkEnableFormationKillAwards.isSelected();
        model.enableRankAwards = chkEnableRankAwards.isSelected();
        model.enableScenarioAwards = chkEnableScenarioAwards.isSelected();
        model.enableSkillAwards = chkEnableSkillAwards.isSelected();
        model.enableTheatreOfWarAwards = chkEnableTheatreOfWarAwards.isSelected();
        model.enableTimeAwards = chkEnableTimeAwards.isSelected();
        model.enableTrainingAwards = chkEnableTrainingAwards.isSelected();
        model.enableMiscAwards = chkEnableMiscAwards.isSelected();
        model.awardSetFilterList = txtAwardSetFilterList.getText();
    }
}
