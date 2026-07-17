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
import javax.swing.JPanel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;

/**
 * The {@code AttributesAndTraitsPage} class builds and manages the Attributes &amp; Traits leaf page of the Campaign
 * Options dialog, found under Advancement &rarr; Skills. It owns the widgets for the <i>A Time of War</i>
 * character-generation and skill rules - the attribute, trait, age-effect, and infantry-skill toggles - and
 * synchronises them with an {@link AttributesAndTraitsOptionsModel}.
 *
 * <p>These options previously lived on the Operations &rarr; Systems &rarr; A Time of War page. They were relocated here
 * (issue #9560) so that the personnel- and skill-oriented settings sit beside their peers instead of under Operations.
 * The trait-driven income toggles that used to share that page moved to Operations &rarr; Finances instead.</p>
 *
 * <p>Unlike the leaf pages that are driven by a shared coordinator, this page is self-contained: it owns its model and
 * exposes the load/apply lifecycle directly to {@code CampaignOptionsPane}. The page is built lazily; until
 * {@link #createPage()} is called, {@code readFromModel} and {@code writeToModel} are no-ops.</p>
 */
public class AttributesAndTraitsPage {
    private static final int FORM_LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int FORM_CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int CHECKBOX_GRID_COLUMNS = 2;

    private final CampaignOptions campaignOptions;
    private final RandomSkillPreferences randomSkillPreferences;
    private AttributesAndTraitsOptionsModel model;

    private CampaignOptionsHeaderPanel header;

    private JCheckBox chkUseAttributes;
    private JCheckBox chkRandomizeAttributes;
    private JCheckBox chkDisplayAllAttributes;
    private JCheckBox chkUseAgeEffects;
    private JCheckBox chkRandomizeTraits;
    private JCheckBox chkUseSmallArmsOnly;

    private boolean created;

    /**
     * Constructs a new {@code AttributesAndTraitsPage} for the specified campaign.
     *
     * @param campaign the campaign whose options and random skill preferences back this page
     */
    public AttributesAndTraitsPage(@Nonnull Campaign campaign) {
        this.campaignOptions = campaign.getCampaignOptions();
        this.randomSkillPreferences = campaign.getRandomSkillPreferences();
        loadValuesFromCampaignOptions();
    }

    /**
     * Creates the Attributes &amp; Traits page panel, containing the grouped attribute, trait, age-effect, and
     * infantry-skill toggles and its header.
     *
     * @return a {@link JPanel} component representing the entire Attributes &amp; Traits page UI
     */
    public @Nonnull JPanel createPage() {
        // Header
        String imageAddress = getImageDirectory() + "logo_elysian_fields.png";
        header = new CampaignOptionsHeaderPanel("AttributesAndTraitsPage", imageAddress);

        // Contents
        JPanel pnlAttributesAndTraits = createAttributesAndTraitsPanel();

        // Layout the Panel
        final JPanel panel = CampaignOptionsPagePanel.builder("AttributesAndTraitsPage", "AttributesAndTraitsPage",
                        imageAddress)
                .header(header)
                .quote("attributesAndTraitsPage")
                .section("lblAttributesAndTraitsSection.text",
                        "lblAttributesAndTraitsSection.summary",
                        pnlAttributesAndTraits)
                .build();

        created = true;
        readFromModel();

        return panel;
    }

    /**
     * Creates and returns the panel holding the attribute, trait, age-effect, and infantry-skill toggles.
     *
     * @return a {@link JPanel} containing the configuration options
     */
    private @Nonnull JPanel createAttributesAndTraitsPanel() {
        chkUseAttributes = new CampaignOptionsCheckBox("UseAttributes",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkUseAttributes.addMouseListener(createTipPanelUpdater("UseAttributes"));
        chkRandomizeAttributes = new CampaignOptionsCheckBox("RandomizeAttributes");
        chkRandomizeAttributes.addMouseListener(createTipPanelUpdater("RandomizeAttributes"));
        chkDisplayAllAttributes = new CampaignOptionsCheckBox("DisplayAllAttributes",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkDisplayAllAttributes.addMouseListener(createTipPanelUpdater("DisplayAllAttributes"));
        chkUseAgeEffects = new CampaignOptionsCheckBox("UseAgeEffects",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseAgeEffects.addMouseListener(createTipPanelUpdater("UseAgeEffects"));
        chkRandomizeTraits = new CampaignOptionsCheckBox("RandomizeTraits");
        chkRandomizeTraits.addMouseListener(createTipPanelUpdater("RandomizeTraits"));
        chkUseSmallArmsOnly = new CampaignOptionsCheckBox("UseSmallArmsOnly",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseSmallArmsOnly.addMouseListener(createTipPanelUpdater("UseSmallArmsOnly"));

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AttributesAndTraitsPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkUseAttributes,
                chkRandomizeAttributes,
                chkDisplayAllAttributes,
                chkUseAgeEffects,
                chkRandomizeTraits,
                chkUseSmallArmsOnly);

        return panel;
    }

    /**
     * Loads values from the current campaign options and random skill preferences into this page's controls.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null);
    }

    /**
     * Loads values from the specified preset sources, or the current campaign's sources if {@code null}, into this
     * page's controls.
     *
     * @param presetCampaignOptions        an alternative {@link CampaignOptions}, or {@code null} to use the current
     *                                     campaign's options
     * @param presetRandomSkillPreferences an alternative {@link RandomSkillPreferences}, or {@code null} to use the
     *                                     current campaign's preferences
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions,
            @Nullable RandomSkillPreferences presetRandomSkillPreferences) {
        CampaignOptions options = presetCampaignOptions != null ? presetCampaignOptions : this.campaignOptions;
        RandomSkillPreferences skillPreferences = presetRandomSkillPreferences != null
                ? presetRandomSkillPreferences
                : this.randomSkillPreferences;

        model = new AttributesAndTraitsOptionsModel(options, skillPreferences);
        readFromModel();
    }

    /**
     * Applies the currently selected values in this page's controls to the given campaign options and random skill
     * preferences, or the current campaign's sources if {@code null}.
     *
     * @param presetCampaignOptions        an alternative {@link CampaignOptions} to update, or {@code null} to update
     *                                     the campaign's own options
     * @param presetRandomSkillPreferences an alternative {@link RandomSkillPreferences} to update, or {@code null} to
     *                                     update the campaign's own preferences
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions,
            @Nullable RandomSkillPreferences presetRandomSkillPreferences) {
        CampaignOptions options = presetCampaignOptions != null ? presetCampaignOptions : this.campaignOptions;
        RandomSkillPreferences skillPreferences = presetRandomSkillPreferences != null
                ? presetRandomSkillPreferences
                : this.randomSkillPreferences;

        writeToModel();
        model.applyTo(options, skillPreferences);
    }

    private void readFromModel() {
        if (!created || model == null) {
            return;
        }

        chkUseAttributes.setSelected(model.useAttributes);
        chkRandomizeAttributes.setSelected(model.randomizeAttributes);
        chkDisplayAllAttributes.setSelected(model.displayAllAttributes);
        chkUseAgeEffects.setSelected(model.useAgeEffects);
        chkRandomizeTraits.setSelected(model.randomizeTraits);
        chkUseSmallArmsOnly.setSelected(model.useSmallArmsOnly);
    }

    private void writeToModel() {
        if (!created || model == null) {
            return;
        }

        model.useAttributes = chkUseAttributes.isSelected();
        model.randomizeAttributes = chkRandomizeAttributes.isSelected();
        model.displayAllAttributes = chkDisplayAllAttributes.isSelected();
        model.useAgeEffects = chkUseAgeEffects.isSelected();
        model.randomizeTraits = chkRandomizeTraits.isSelected();
        model.useSmallArmsOnly = chkUseSmallArmsOnly.isSelected();
    }
}
