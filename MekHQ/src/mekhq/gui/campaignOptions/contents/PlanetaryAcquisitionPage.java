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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsModifierTablePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code PlanetaryAcquisitionPage} class builds and manages the Planetary Acquisition leaf page of the Campaign
 * Options dialog. It owns the widgets for planetary acquisition options - faction limits, clan/Inner Sphere crossover
 * rules - and the technology, industry, and output modifier table, and synchronises them with a shared
 * {@link EquipmentAndSuppliesOptionsModel}.
 *
 * <p>This view is a sub-component of {@link EquipmentAndSuppliesPages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code EquipmentAndSuppliesPages}, while this class is responsible only for constructing the
 * Planetary Acquisition panel and copying planetary acquisition values to and from the model. The page is built lazily;
 * until {@link #createPanel(EquipmentAndSuppliesOptionsModel)} is called,
 * {@link #readFromModel(EquipmentAndSuppliesOptionsModel)} and {@link #writeToModel(EquipmentAndSuppliesOptionsModel)}
 * are no-ops.</p>
 */
class PlanetaryAcquisitionPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int MODIFIER_ROW_LABEL_COLUMN_WIDTH = 120;
    private static final int MODIFIER_CONTROL_COLUMN_WIDTH = 104;

    private CampaignOptionsHeaderPanel planetaryAcquisitionHeader;
    private JCheckBox usePlanetaryAcquisitions;
    private JLabel lblMaxJumpPlanetaryAcquisitions;
    private JSpinner spnMaxJumpPlanetaryAcquisitions;
    private JLabel lblPlanetaryAcquisitionsFactionLimits;
    private MMComboBox<PlanetaryAcquisitionFactionLimit> comboPlanetaryAcquisitionsFactionLimits;
    private JCheckBox disallowClanPartsFromIS;
    private JCheckBox disallowPlanetaryAcquisitionClanCrossover;
    private JLabel lblPenaltyClanPartsFromIS;
    private JSpinner spnPenaltyClanPartsFromIS;
    private JCheckBox usePlanetaryAcquisitionsVerbose;

    private JSpinner[] spnPlanetAcquireTechBonus;
    private JSpinner[] spnPlanetAcquireIndustryBonus;
    private JSpinner[] spnPlanetAcquireOutputBonus;

    private boolean created;

    /**
     * Creates and configures the planetary acquisition page panel in a campaign options interface. The panel includes a
     * header, options, and modifiers section, arranged using layout constraints. Once configured, it is wrapped within
     * a parent panel and returned.
     *
     * @param model the shared equipment and supplies options model to populate the freshly built controls from
     *
     * @return a {@code JPanel} object representing the planetary acquisition page with its configured components and
     *       layout.
     */
    @Nonnull JPanel createPanel(@Nullable EquipmentAndSuppliesOptionsModel model) {
        // Controls built in the page's former initialize step
        comboPlanetaryAcquisitionsFactionLimits = new MMComboBox<>("comboPlanetaryAcquisitionsFactionLimits",
              PlanetaryAcquisitionFactionLimit.values());
        spnPlanetAcquireTechBonus = new JSpinner[PlanetarySophistication.values().length];
        spnPlanetAcquireIndustryBonus = new JSpinner[PlanetaryRating.values().length];
        spnPlanetAcquireOutputBonus = new JSpinner[PlanetaryRating.values().length];

        // Header
        String imageAddress = getImageDirectory() + "logo_rim_worlds_republic.png";
        planetaryAcquisitionHeader = new CampaignOptionsHeaderPanel("PlanetaryAcquisitionPage", imageAddress);

        // Sub-Panels
        JPanel options = createOptionsPanel();
        JPanel modifiers = createModifiersPanel();

        JPanel panel = CampaignOptionsPagePanel.builder("PlanetaryAcquisitionPage", "PlanetaryAcquisitionPage",
                imageAddress)
                .header(planetaryAcquisitionHeader)
                .quote("planetaryAcquisitionPage")
                .section("lblPlanetaryAcquisitionPage.text",
                        "lblPlanetaryAcquisitionPage.summary",
                        options)
                .section("lblModifiersPanel.text",
                        "lblModifiersPanel.summary",
                        modifiers)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates and returns a {@code JPanel} containing the components necessary for configuring campaign options related
     * to planetary acquisitions. This panel includes various labels, checkboxes, and spinners for setting and adjusting
     * relevant options.
     *
     * @return a {@code JPanel} containing the campaign options panel for planetary acquisitions.
     */
    private @Nonnull JPanel createOptionsPanel() {
        usePlanetaryAcquisitions = new CampaignOptionsCheckBox("UsePlanetaryAcquisitions",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED));
        usePlanetaryAcquisitions.addMouseListener(createTipPanelUpdater("UsePlanetaryAcquisitions"));

        lblMaxJumpPlanetaryAcquisitions = new CampaignOptionsLabel("MaxJumpPlanetaryAcquisitions");
        lblMaxJumpPlanetaryAcquisitions.addMouseListener(createTipPanelUpdater("MaxJumpPlanetaryAcquisitions"));
        spnMaxJumpPlanetaryAcquisitions = new CampaignOptionsSpinner("MaxJumpPlanetaryAcquisitions", 2, 0, 5, 1);
        spnMaxJumpPlanetaryAcquisitions.addMouseListener(createTipPanelUpdater("MaxJumpPlanetaryAcquisitions"));

        lblPlanetaryAcquisitionsFactionLimits = new CampaignOptionsLabel("PlanetaryAcquisitionsFactionLimits");
        lblPlanetaryAcquisitionsFactionLimits.addMouseListener(createTipPanelUpdater("PlanetaryAcquisitionsFactionLimits"));

        disallowPlanetaryAcquisitionClanCrossover = new CampaignOptionsCheckBox(
              "DisallowPlanetaryAcquisitionClanCrossover");
        disallowPlanetaryAcquisitionClanCrossover.addMouseListener(createTipPanelUpdater("DisallowPlanetaryAcquisitionClanCrossover"));

        disallowClanPartsFromIS = new CampaignOptionsCheckBox("DisallowClanPartsFromIS");
        disallowClanPartsFromIS.addMouseListener(createTipPanelUpdater("DisallowClanPartsFromIS"));

        lblPenaltyClanPartsFromIS = new CampaignOptionsLabel("PenaltyClanPartsFromIS");
        lblPenaltyClanPartsFromIS.addMouseListener(createTipPanelUpdater("PenaltyClanPartsFromIS"));
        spnPenaltyClanPartsFromIS = new CampaignOptionsSpinner("PenaltyClanPartsFromIS", 0, 0, 12, 1);
        spnPenaltyClanPartsFromIS.addMouseListener(createTipPanelUpdater("PenaltyClanPartsFromIS"));

        usePlanetaryAcquisitionsVerbose = new CampaignOptionsCheckBox("UsePlanetaryAcquisitionsVerbose",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        usePlanetaryAcquisitionsVerbose.addMouseListener(createTipPanelUpdater("UsePlanetaryAcquisitionsVerbose"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PlanetaryAcquisitionOptionsPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(usePlanetaryAcquisitions);
        panel.addCheckBox(usePlanetaryAcquisitionsVerbose);
        panel.addRow(lblMaxJumpPlanetaryAcquisitions, spnMaxJumpPlanetaryAcquisitions);
        panel.addRow(lblPlanetaryAcquisitionsFactionLimits, comboPlanetaryAcquisitionsFactionLimits);
        panel.addCheckBox(disallowPlanetaryAcquisitionClanCrossover);
        panel.addCheckBox(disallowClanPartsFromIS);
        panel.addRow(lblPenaltyClanPartsFromIS, spnPenaltyClanPartsFromIS);

        return panel;
    }

    private @Nonnull JPanel createModifiersPanel() {
        int i;
        for (i = 0; i < PlanetarySophistication.values().length; i++) {
            spnPlanetAcquireTechBonus[i] = createModifierSpinner("TechLabel");
        }

        for (i = 0; i < PlanetaryRating.values().length; i++) {
            spnPlanetAcquireIndustryBonus[i] = createModifierSpinner("IndustryLabel");
            spnPlanetAcquireOutputBonus[i] = createModifierSpinner("OutputLabel");
        }

        final CampaignOptionsModifierTablePanel tablePanel = new CampaignOptionsModifierTablePanel(
              "PlanetaryAcquisitionPageModifiers",
              MODIFIER_ROW_LABEL_COLUMN_WIDTH,
              MODIFIER_CONTROL_COLUMN_WIDTH,
              createModifierColumnHeader("TechLabel"),
              createModifierColumnHeader("IndustryLabel"),
              createModifierColumnHeader("OutputLabel"));

        i = 0;
        for (PlanetarySophistication sophistication : PlanetarySophistication.values()) {
            int ratingIndex = getPlanetaryRatingIndex(sophistication.getName());
            tablePanel.addRow(createModifierRowLabel(sophistication.getName()),
                  spnPlanetAcquireTechBonus[i],
                  ratingIndex >= 0 ? spnPlanetAcquireIndustryBonus[ratingIndex] : null,
                  ratingIndex >= 0 ? spnPlanetAcquireOutputBonus[ratingIndex] : null);
            i++;
        }

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PlanetaryAcquisitionPageModifiersPanel",
              MODIFIER_ROW_LABEL_COLUMN_WIDTH,
              MODIFIER_CONTROL_COLUMN_WIDTH);
        panel.addFullWidthComponent(tablePanel);

        return panel;
    }

    private @Nonnull JLabel createModifierColumnHeader(String name) {
        JLabel label = new CampaignOptionsLabel(name);
        label.addMouseListener(createTipPanelUpdater(name));
        return label;
    }

    private @Nonnull JLabel createModifierRowLabel(String text) {
        JLabel label = new JLabel(String.format("<html>%s</html>", text));
        label.addMouseListener(createTipPanelUpdater("TechLabel"));
        return label;
    }

    private @Nonnull JSpinner createModifierSpinner(String tipKey) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
        spinner.addMouseListener(createTipPanelUpdater(tipKey));
        CampaignOptionsSpinner.installSelectAllOnFocus(spinner);
        return spinner;
    }

    private int getPlanetaryRatingIndex(String name) {
        for (PlanetaryRating rating : PlanetaryRating.values()) {
            if (rating.getName().equals(name)) {
                return rating.getIndex();
            }
        }

        return -1;
    }

    /**
     * Copies planetary acquisition values from the shared model into this page's controls. This is a no-op until the
     * page has been built.
     *
     * @param model the shared equipment and supplies options model to read values from
     */
    void readFromModel(@Nullable EquipmentAndSuppliesOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        usePlanetaryAcquisitions.setSelected(model.usePlanetaryAcquisition);
        spnMaxJumpPlanetaryAcquisitions.setValue(model.maxJumpsPlanetaryAcquisition);
        comboPlanetaryAcquisitionsFactionLimits.setSelectedItem(model.planetAcquisitionFactionLimit);
        disallowPlanetaryAcquisitionClanCrossover.setSelected(model.disallowPlanetAcquisitionClanCrossover);
        disallowClanPartsFromIS.setSelected(model.noClanPartsFromIS);
        spnPenaltyClanPartsFromIS.setValue(model.penaltyClanPartsFromIS);
        usePlanetaryAcquisitionsVerbose.setSelected(model.planetAcquisitionVerbose);

        for (int i = 0; i < Math.min(spnPlanetAcquireTechBonus.length, model.planetTechAcquisitionBonus.length); i++) {
            spnPlanetAcquireTechBonus[i].setValue(model.planetTechAcquisitionBonus[i]);
        }
        for (int i = 0; i < Math.min(spnPlanetAcquireIndustryBonus.length,
              model.planetIndustryAcquisitionBonus.length); i++) {
            spnPlanetAcquireIndustryBonus[i].setValue(model.planetIndustryAcquisitionBonus[i]);
        }
        for (int i = 0; i < Math.min(spnPlanetAcquireOutputBonus.length,
              model.planetOutputAcquisitionBonus.length); i++) {
            spnPlanetAcquireOutputBonus[i].setValue(model.planetOutputAcquisitionBonus[i]);
        }
    }

    /**
     * Copies planetary acquisition values from this page's controls into the shared model. This is a no-op until the
     * page has been built.
     *
     * @param model the shared equipment and supplies options model to write values into
     */
    void writeToModel(@Nullable EquipmentAndSuppliesOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.usePlanetaryAcquisition = usePlanetaryAcquisitions.isSelected();
        model.maxJumpsPlanetaryAcquisition = (int) spnMaxJumpPlanetaryAcquisitions.getValue();
        model.planetAcquisitionFactionLimit = comboPlanetaryAcquisitionsFactionLimits.getSelectedItem();
        model.disallowPlanetAcquisitionClanCrossover = disallowPlanetaryAcquisitionClanCrossover.isSelected();
        model.noClanPartsFromIS = disallowClanPartsFromIS.isSelected();
        model.penaltyClanPartsFromIS = (int) spnPenaltyClanPartsFromIS.getValue();
        model.planetAcquisitionVerbose = usePlanetaryAcquisitionsVerbose.isSelected();

        for (int i = 0; i < Math.min(spnPlanetAcquireTechBonus.length, model.planetTechAcquisitionBonus.length); i++) {
            model.planetTechAcquisitionBonus[i] = (int) spnPlanetAcquireTechBonus[i].getValue();
        }
        for (int i = 0; i < Math.min(spnPlanetAcquireIndustryBonus.length,
              model.planetIndustryAcquisitionBonus.length); i++) {
            model.planetIndustryAcquisitionBonus[i] = (int) spnPlanetAcquireIndustryBonus[i].getValue();
        }
        for (int i = 0; i < Math.min(spnPlanetAcquireOutputBonus.length,
              model.planetOutputAcquisitionBonus.length); i++) {
            model.planetOutputAcquisitionBonus[i] = (int) spnPlanetAcquireOutputBonus[i].getValue();
        }
    }
}
