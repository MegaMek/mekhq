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

import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsModifierTablePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPairedFieldGridPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code RandomizationPage} class builds and manages the Skill Randomization leaf page of the Campaign Options
 * dialog. It owns the widgets for skill randomization - phenotype probabilities, experience-level ability and skill
 * modifiers, and the special skill modifiers - and synchronises them with a shared
 * {@link AwardsAndRandomizationOptionsModel}.
 *
 * <p>This view is a sub-component of {@link AwardsAndRandomizationPages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code AwardsAndRandomizationPages}, while this class is responsible only for constructing the
 * Skill Randomization panel and copying randomization values to and from the model. The page is built lazily; until
 * {@link #createPanel(AwardsAndRandomizationOptionsModel)} is called,
 * {@link #readFromModel(AwardsAndRandomizationOptionsModel)} and {@link #writeToModel(AwardsAndRandomizationOptionsModel)}
 * are no-ops.</p>
 */
class RandomizationPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int ADVANCEMENT_GRID_CONTROL_COLUMN_WIDTH = 100;
    private static final int ADVANCEMENT_GRID_MEDIUM_PAIR_COLUMN_WIDTH = 290;
    private static final int MODIFIER_ROW_LABEL_COLUMN_WIDTH = 120;
    private static final int MODIFIER_CONTROL_COLUMN_WIDTH = 104;

    private CampaignOptionsHeaderPanel skillRandomizationHeader;
    private JCheckBox chkExtraRandomness;

    private JPanel pnlPhenotype;
    private JLabel[] phenotypeLabels;
    private JSpinner[] phenotypeSpinners;

    private JPanel pnlExperienceLevelModifiers;
    private JLabel lblAbilityUltraGreen;
    private JSpinner spnAbilityUltraGreen;
    private JLabel lblAbilityGreen;
    private JSpinner spnAbilityGreen;
    private JLabel lblAbilityReg;
    private JSpinner spnAbilityReg;
    private JLabel lblAbilityVet;
    private JSpinner spnAbilityVet;
    private JLabel lblAbilityElite;
    private JSpinner spnAbilityElite;
    private JLabel lblAbilityHeroic;
    private JSpinner spnAbilityHeroic;
    private JLabel lblAbilityLegendary;
    private JSpinner spnAbilityLegendary;

    private JPanel pnlSpecialSkillModifiers;

    private JSpinner spnCommandSkillsUltraGreen;
    private JSpinner spnCommandSkillsGreen;
    private JSpinner spnCommandSkillsReg;
    private JSpinner spnCommandSkillsVet;
    private JSpinner spnCommandSkillsElite;
    private JSpinner spnCommandSkillsHeroic;
    private JSpinner spnCommandSkillsLegendary;

    private JSpinner spnUtilitySkillsUltraGreen;
    private JSpinner spnUtilitySkillsGreen;
    private JSpinner spnUtilitySkillsReg;
    private JSpinner spnUtilitySkillsVet;
    private JSpinner spnUtilitySkillsElite;
    private JSpinner spnUtilitySkillsHeroic;
    private JSpinner spnUtilitySkillsLegendary;

    private JLabel lblCombatSA;
    private JSpinner spnCombatSA;
    private JLabel lblSupportSA;
    private JSpinner spnSupportSA;

    private JLabel lblArtyProb;
    private JSpinner spnArtyProb;
    private JLabel lblArtyBonus;
    private JSpinner spnArtyBonus;

    private JLabel lblAntiMekSkill;
    private JSpinner spnAntiMekSkill;
    private JLabel lblSecondProb;
    private JSpinner spnSecondProb;
    private JLabel lblSecondBonus;
    private JSpinner spnSecondBonus;
    private JLabel lblRoleplaySkillsModifier;
    private JSpinner spnRoleplaySkillsModifier;

    private boolean created;

    /**
     * Creates and returns the Skill Randomization page panel. This page allows users
     * to configure settings related to
     * skill randomization, including phenotype probabilities and skill bonuses for
     * different experience levels and
     * skill groups.
     *
     * @param model the shared awards and randomization options model to populate the freshly built controls from
     *
     * @return A {@code JPanel} containing the configuration options for skill
     *         randomization.
     */
    @Nonnull JPanel createPanel(@Nullable AwardsAndRandomizationOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_republic_of_the_sphere.png";
        skillRandomizationHeader = new CampaignOptionsHeaderPanel("SkillRandomizationPage", imageAddress);

        // Contents
        JPanel randomizationOptions = createSkillRandomizationOptionsPanel();
        pnlPhenotype = createPhenotypePanel();
        pnlExperienceLevelModifiers = createExperienceLevelModifiersPanel();
        pnlSpecialSkillModifiers = createSpecialSkillModifiersPanel();

        JPanel panel = CampaignOptionsPagePanel.builder("SkillRandomizationPage", "SkillRandomizationPage", imageAddress)
                .header(skillRandomizationHeader)
                .section("lblSkillRandomizationPage.text",
                        "lblSkillRandomizationPage.summary",
                        randomizationOptions)
                .section("lblPhenotypesPanel.text",
                        "lblPhenotypesPanel.summary",
                        pnlPhenotype)
                .section("lblExperienceLevelModifiersPanel.text",
                        "lblExperienceLevelModifiersPanel.summary",
                        pnlExperienceLevelModifiers,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT))
                .section("lblSpecialSkillModifiersPanel.text",
                        "lblSpecialSkillModifiersPanel.summary",
                        pnlSpecialSkillModifiers)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    private @Nonnull JPanel createSkillRandomizationOptionsPanel() {
        chkExtraRandomness = new CampaignOptionsCheckBox("ExtraRandomness",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkExtraRandomness.addMouseListener(createTipPanelUpdater("ExtraRandomness"));

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("SkillRandomizationOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkExtraRandomness);

        return panel;
    }

    /**
     * Creates and returns the Phenotype panel, which allows users to configure
     * settings for phenotype probabilities in
     * the campaign. Each phenotype is assigned a spinner to adjust its probability.
     *
     * @return A {@code JPanel} containing configuration options for phenotype
     *         probabilities.
     */
    private @Nonnull JPanel createPhenotypePanel() {
        // Contents
        List<Phenotype> phenotypes = Phenotype.getExternalPhenotypes();
        phenotypeLabels = new JLabel[phenotypes.size()];
        phenotypeSpinners = new JSpinner[phenotypes.size()];

        for (int i = 0; i < phenotypes.size(); i++) {
            phenotypeLabels[i] = new CampaignOptionsLabel(phenotypes.get(i).getLabel());
            phenotypeLabels[i].addMouseListener(createTipPanelUpdater(null,
                    phenotypes.get(i).getTooltip()));
            phenotypeSpinners[i] = new CampaignOptionsSpinner(phenotypes.get(i).getLabel(), 0, 0, 100, 1);
            phenotypeSpinners[i].addMouseListener(createTipPanelUpdater(null,
                    phenotypes.get(i).getTooltip()));
        }

        return createAdvancementPairedGrid("PhenotypesPanel", phenotypeLabels, phenotypeSpinners,
                ADVANCEMENT_GRID_MEDIUM_PAIR_COLUMN_WIDTH);
    }

    private @Nonnull JPanel createExperienceLevelModifiersPanel() {
        createAbilityModifierControls();
        createCommandSkillModifierControls();
        createUtilitySkillModifierControls();

        final CampaignOptionsModifierTablePanel panel = new CampaignOptionsModifierTablePanel(
                "ExperienceLevelModifiersPanel",
                MODIFIER_ROW_LABEL_COLUMN_WIDTH,
                MODIFIER_CONTROL_COLUMN_WIDTH,
                createModifierColumnHeader("AbilityPanel"),
                createModifierColumnHeader("CommandSkillsPanel"),
                createModifierColumnHeader("UtilitySkillsPanel"));

        panel.addRow(lblAbilityUltraGreen,
                spnAbilityUltraGreen,
                spnCommandSkillsUltraGreen,
                spnUtilitySkillsUltraGreen);
        panel.addRow(lblAbilityGreen,
                spnAbilityGreen,
                spnCommandSkillsGreen,
                spnUtilitySkillsGreen);
        panel.addRow(lblAbilityReg,
                spnAbilityReg,
                spnCommandSkillsReg,
                spnUtilitySkillsReg);
        panel.addRow(lblAbilityVet,
                spnAbilityVet,
                spnCommandSkillsVet,
                spnUtilitySkillsVet);
        panel.addRow(lblAbilityElite,
                spnAbilityElite,
                spnCommandSkillsElite,
                spnUtilitySkillsElite);
        panel.addRow(lblAbilityHeroic,
                spnAbilityHeroic,
                spnCommandSkillsHeroic,
                spnUtilitySkillsHeroic);
        panel.addRow(lblAbilityLegendary,
                spnAbilityLegendary,
                spnCommandSkillsLegendary,
                spnUtilitySkillsLegendary);

        final CampaignOptionsFormPanel wrapper = new CampaignOptionsFormPanel("ExperienceLevelModifiersWrapperPanel",
                MODIFIER_ROW_LABEL_COLUMN_WIDTH,
                MODIFIER_CONTROL_COLUMN_WIDTH);
        wrapper.addFullWidthComponent(panel);

        return wrapper;
    }

    private void createAbilityModifierControls() {
        lblAbilityUltraGreen = createExperienceLevelLabel(SkillType.EXP_ULTRA_GREEN);
        spnAbilityUltraGreen = createSkillModifierSpinner("AbilityUltraGreen");
        lblAbilityGreen = createExperienceLevelLabel(SkillType.EXP_GREEN);
        spnAbilityGreen = createSkillModifierSpinner("AbilityGreen");
        lblAbilityReg = createExperienceLevelLabel(SkillType.EXP_REGULAR);
        spnAbilityReg = createSkillModifierSpinner("AbilityRegular");
        lblAbilityVet = createExperienceLevelLabel(SkillType.EXP_VETERAN);
        spnAbilityVet = createSkillModifierSpinner("AbilityVeteran");
        lblAbilityElite = createExperienceLevelLabel(SkillType.EXP_ELITE);
        spnAbilityElite = createSkillModifierSpinner("AbilityElite");
        lblAbilityHeroic = createExperienceLevelLabel(SkillType.EXP_HEROIC);
        spnAbilityHeroic = createSkillModifierSpinner("AbilityHeroic");
        lblAbilityLegendary = createExperienceLevelLabel(SkillType.EXP_LEGENDARY);
        spnAbilityLegendary = createSkillModifierSpinner("AbilityLegendary");
    }

    private void createCommandSkillModifierControls() {
        spnCommandSkillsUltraGreen = createSkillModifierSpinner("CommandSkillsUltraGreen");
        spnCommandSkillsGreen = createSkillModifierSpinner("CommandSkillsGreen");
        spnCommandSkillsReg = createSkillModifierSpinner("CommandSkillsRegular");
        spnCommandSkillsVet = createSkillModifierSpinner("CommandSkillsVeteran");
        spnCommandSkillsElite = createSkillModifierSpinner("CommandSkillsElite");
        spnCommandSkillsHeroic = createSkillModifierSpinner("CommandSkillsHeroic");
        spnCommandSkillsLegendary = createSkillModifierSpinner("CommandSkillsLegendary");
    }

    private void createUtilitySkillModifierControls() {
        spnUtilitySkillsUltraGreen = createSkillModifierSpinner("UtilitySkillsUltraGreen");
        spnUtilitySkillsGreen = createSkillModifierSpinner("UtilitySkillsGreen");
        spnUtilitySkillsReg = createSkillModifierSpinner("UtilitySkillsRegular");
        spnUtilitySkillsVet = createSkillModifierSpinner("UtilitySkillsVeteran");
        spnUtilitySkillsElite = createSkillModifierSpinner("UtilitySkillsElite");
        spnUtilitySkillsHeroic = createSkillModifierSpinner("UtilitySkillsHeroic");
        spnUtilitySkillsLegendary = createSkillModifierSpinner("UtilitySkillsLegendary");
    }

    private @Nonnull JLabel createModifierColumnHeader(String name) {
        return new JLabel(getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".text"));
    }

    private @Nonnull JLabel createExperienceLevelLabel(int experienceLevel) {
        return new JLabel(SkillType.getExperienceLevelName(experienceLevel));
    }

    private @Nonnull JSpinner createSkillModifierSpinner(String name) {
        JSpinner spinner = new CampaignOptionsSpinner(name, 0, -12, 12, 1);
        spinner.addMouseListener(createTipPanelUpdater(name));
        return spinner;
    }

    private @Nonnull JPanel createSpecialSkillModifiersPanel() {
        createSecondarySkillControls();
        createArtilleryControls();
        createSmallArmsControls();

        JComponent[] labels = { lblRoleplaySkillsModifier, lblAntiMekSkill, lblSecondProb, lblSecondBonus,
                lblArtyProb, lblArtyBonus, lblCombatSA, lblSupportSA };
        JComponent[] controls = { spnRoleplaySkillsModifier, spnAntiMekSkill, spnSecondProb, spnSecondBonus,
                spnArtyProb, spnArtyBonus, spnCombatSA, spnSupportSA };

        return createAdvancementPairedGrid("SpecialSkillModifiersPanel", labels, controls,
                ADVANCEMENT_GRID_MEDIUM_PAIR_COLUMN_WIDTH);
    }

    private void createSecondarySkillControls() {
        lblRoleplaySkillsModifier = new CampaignOptionsLabel("RoleplaySkillsModifier",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblRoleplaySkillsModifier.addMouseListener(createTipPanelUpdater("RoleplaySkillsModifier"));
        spnRoleplaySkillsModifier = new CampaignOptionsSpinner("RoleplaySkillsModifier", 0, -12, 12, 1);
        spnRoleplaySkillsModifier.addMouseListener(createTipPanelUpdater("RoleplaySkillsModifier"));

        lblAntiMekSkill = new CampaignOptionsLabel("AntiMekChance");
        lblAntiMekSkill.addMouseListener(createTipPanelUpdater("AntiMekChance"));
        spnAntiMekSkill = new CampaignOptionsSpinner("AntiMekChance", 0, 0, 100, 1);
        spnAntiMekSkill.addMouseListener(createTipPanelUpdater("AntiMekChance"));

        lblSecondProb = new CampaignOptionsLabel("SecondarySkillChance");
        lblSecondProb.addMouseListener(createTipPanelUpdater("SecondarySkillChance"));
        spnSecondProb = new CampaignOptionsSpinner("SecondarySkillChance", 0, 0, 100, 1);
        spnSecondProb.addMouseListener(createTipPanelUpdater("SecondarySkillChance"));

        lblSecondBonus = new CampaignOptionsLabel("SecondarySkillBonus");
        lblSecondBonus.addMouseListener(createTipPanelUpdater("SecondarySkillBonus"));
        spnSecondBonus = new CampaignOptionsSpinner("SecondarySkillBonus", 0, -12, 12, 1);
        spnSecondBonus.addMouseListener(createTipPanelUpdater("SecondarySkillBonus"));
    }

    private void createArtilleryControls() {
        lblArtyProb = new CampaignOptionsLabel("ArtilleryChance");
        lblArtyProb.addMouseListener(createTipPanelUpdater("ArtilleryChance"));
        spnArtyProb = new CampaignOptionsSpinner("ArtilleryChance", 0, 0, 100, 1);
        spnArtyProb.addMouseListener(createTipPanelUpdater("ArtilleryChance"));

        lblArtyBonus = new CampaignOptionsLabel("ArtilleryBonus");
        lblArtyBonus.addMouseListener(createTipPanelUpdater("ArtilleryBonus"));
        spnArtyBonus = new CampaignOptionsSpinner("ArtilleryBonus", 0, -12, 12, 1);
        spnArtyBonus.addMouseListener(createTipPanelUpdater("ArtilleryBonus"));
    }

    private void createSmallArmsControls() {
        lblCombatSA = new CampaignOptionsLabel("CombatSmallArms");
        lblCombatSA.addMouseListener(createTipPanelUpdater("CombatSmallArms"));
        spnCombatSA = new CampaignOptionsSpinner("CombatSmallArms", 0, -12, 12, 1);
        spnCombatSA.addMouseListener(createTipPanelUpdater("CombatSmallArms"));

        lblSupportSA = new CampaignOptionsLabel("NonCombatSmallArms");
        lblSupportSA.addMouseListener(createTipPanelUpdater("NonCombatSmallArms"));
        spnSupportSA = new CampaignOptionsSpinner("NonCombatSmallArms", 0, -12, 12, 1);
        spnSupportSA.addMouseListener(createTipPanelUpdater("NonCombatSmallArms"));
    }

    private @Nonnull CampaignOptionsPairedFieldGridPanel createAdvancementPairedGrid(String name, JComponent[] labels,
            JComponent[] controls, int pairColumnWidth) {
        final CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel(name,
                pairColumnWidth,
                pairColumnWidth,
                ADVANCEMENT_GRID_CONTROL_COLUMN_WIDTH,
                2);
        panel.addPairs(labels, controls);

        return panel;
    }

    /**
     * Copies skill randomization values from the shared model into this page's controls. This is a no-op until the page
     * has been built.
     *
     * @param model the shared awards and randomization options model to read values from
     */
    void readFromModel(@Nullable AwardsAndRandomizationOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkExtraRandomness.setSelected(model.randomizeSkill);
        for (int i = 0; i < Math.min(phenotypeSpinners.length, model.phenotypeProbabilities.length); i++) {
            phenotypeSpinners[i].setValue(model.phenotypeProbabilities[i]);
        }

        spnAbilityUltraGreen.setValue(model.specialAbilityBonus[SkillType.EXP_ULTRA_GREEN]);
        spnAbilityGreen.setValue(model.specialAbilityBonus[SkillType.EXP_GREEN]);
        spnAbilityReg.setValue(model.specialAbilityBonus[SkillType.EXP_REGULAR]);
        spnAbilityVet.setValue(model.specialAbilityBonus[SkillType.EXP_VETERAN]);
        spnAbilityElite.setValue(model.specialAbilityBonus[SkillType.EXP_ELITE]);
        spnAbilityHeroic.setValue(model.specialAbilityBonus[SkillType.EXP_HEROIC]);
        spnAbilityLegendary.setValue(model.specialAbilityBonus[SkillType.EXP_LEGENDARY]);

        spnCommandSkillsUltraGreen.setValue(model.commandSkillsModifier[SkillType.EXP_ULTRA_GREEN]);
        spnCommandSkillsGreen.setValue(model.commandSkillsModifier[SkillType.EXP_GREEN]);
        spnCommandSkillsReg.setValue(model.commandSkillsModifier[SkillType.EXP_REGULAR]);
        spnCommandSkillsVet.setValue(model.commandSkillsModifier[SkillType.EXP_VETERAN]);
        spnCommandSkillsElite.setValue(model.commandSkillsModifier[SkillType.EXP_ELITE]);
        spnCommandSkillsHeroic.setValue(model.commandSkillsModifier[SkillType.EXP_HEROIC]);
        spnCommandSkillsLegendary.setValue(model.commandSkillsModifier[SkillType.EXP_LEGENDARY]);

        spnUtilitySkillsUltraGreen.setValue(model.utilitySkillsModifier[SkillType.EXP_ULTRA_GREEN]);
        spnUtilitySkillsGreen.setValue(model.utilitySkillsModifier[SkillType.EXP_GREEN]);
        spnUtilitySkillsReg.setValue(model.utilitySkillsModifier[SkillType.EXP_REGULAR]);
        spnUtilitySkillsVet.setValue(model.utilitySkillsModifier[SkillType.EXP_VETERAN]);
        spnUtilitySkillsElite.setValue(model.utilitySkillsModifier[SkillType.EXP_ELITE]);
        spnUtilitySkillsHeroic.setValue(model.utilitySkillsModifier[SkillType.EXP_HEROIC]);
        spnUtilitySkillsLegendary.setValue(model.utilitySkillsModifier[SkillType.EXP_LEGENDARY]);

        spnRoleplaySkillsModifier.setValue(model.roleplaySkillsModifier);
        spnCombatSA.setValue(model.combatSmallArmsBonus);
        spnSupportSA.setValue(model.supportSmallArmsBonus);
        spnArtyProb.setValue(model.artilleryProb);
        spnArtyBonus.setValue(model.artilleryBonus);
        spnAntiMekSkill.setValue(model.antiMekProb);
        spnSecondProb.setValue(model.secondSkillProb);
        spnSecondBonus.setValue(model.secondSkillBonus);
    }

    /**
     * Copies skill randomization values from this page's controls into the shared model. This is a no-op until the page
     * has been built.
     *
     * @param model the shared awards and randomization options model to write values into
     */
    void writeToModel(@Nullable AwardsAndRandomizationOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.randomizeSkill = chkExtraRandomness.isSelected();
        for (int i = 0; i < Math.min(phenotypeSpinners.length, model.phenotypeProbabilities.length); i++) {
            model.phenotypeProbabilities[i] = (int) phenotypeSpinners[i].getValue();
        }

        model.specialAbilityBonus[SkillType.EXP_ULTRA_GREEN] = (int) spnAbilityUltraGreen.getValue();
        model.specialAbilityBonus[SkillType.EXP_GREEN] = (int) spnAbilityGreen.getValue();
        model.specialAbilityBonus[SkillType.EXP_REGULAR] = (int) spnAbilityReg.getValue();
        model.specialAbilityBonus[SkillType.EXP_VETERAN] = (int) spnAbilityVet.getValue();
        model.specialAbilityBonus[SkillType.EXP_ELITE] = (int) spnAbilityElite.getValue();
        model.specialAbilityBonus[SkillType.EXP_HEROIC] = (int) spnAbilityHeroic.getValue();
        model.specialAbilityBonus[SkillType.EXP_LEGENDARY] = (int) spnAbilityLegendary.getValue();

        model.commandSkillsModifier[SkillType.EXP_ULTRA_GREEN] = (int) spnCommandSkillsUltraGreen.getValue();
        model.commandSkillsModifier[SkillType.EXP_GREEN] = (int) spnCommandSkillsGreen.getValue();
        model.commandSkillsModifier[SkillType.EXP_REGULAR] = (int) spnCommandSkillsReg.getValue();
        model.commandSkillsModifier[SkillType.EXP_VETERAN] = (int) spnCommandSkillsVet.getValue();
        model.commandSkillsModifier[SkillType.EXP_ELITE] = (int) spnCommandSkillsElite.getValue();
        model.commandSkillsModifier[SkillType.EXP_HEROIC] = (int) spnCommandSkillsHeroic.getValue();
        model.commandSkillsModifier[SkillType.EXP_LEGENDARY] = (int) spnCommandSkillsLegendary.getValue();

        model.utilitySkillsModifier[SkillType.EXP_ULTRA_GREEN] = (int) spnUtilitySkillsUltraGreen.getValue();
        model.utilitySkillsModifier[SkillType.EXP_GREEN] = (int) spnUtilitySkillsGreen.getValue();
        model.utilitySkillsModifier[SkillType.EXP_REGULAR] = (int) spnUtilitySkillsReg.getValue();
        model.utilitySkillsModifier[SkillType.EXP_VETERAN] = (int) spnUtilitySkillsVet.getValue();
        model.utilitySkillsModifier[SkillType.EXP_ELITE] = (int) spnUtilitySkillsElite.getValue();
        model.utilitySkillsModifier[SkillType.EXP_HEROIC] = (int) spnUtilitySkillsHeroic.getValue();
        model.utilitySkillsModifier[SkillType.EXP_LEGENDARY] = (int) spnUtilitySkillsLegendary.getValue();

        model.roleplaySkillsModifier = (int) spnRoleplaySkillsModifier.getValue();
        model.combatSmallArmsBonus = (int) spnCombatSA.getValue();
        model.supportSmallArmsBonus = (int) spnSupportSA.getValue();
        model.artilleryProb = (int) spnArtyProb.getValue();
        model.artilleryBonus = (int) spnArtyBonus.getValue();
        model.antiMekProb = (int) spnAntiMekSkill.getValue();
        model.secondSkillProb = (int) spnSecondProb.getValue();
        model.secondSkillBonus = (int) spnSecondBonus.getValue();
    }
}
