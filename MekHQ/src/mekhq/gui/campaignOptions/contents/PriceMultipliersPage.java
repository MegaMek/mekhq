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

import static mekhq.campaign.parts.enums.PartQuality.QUALITY_F;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPairedFieldGridPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code PriceMultipliersPage} class builds and manages the Price Multipliers leaf page of the Campaign Options
 * dialog. It owns the widgets for price-multiplier configuration - general unit and part multipliers, the used-part
 * multipliers by quality level, and the damaged/unrepairable/refund multipliers - and synchronises them with a shared
 * {@link FinancesOptionsModel}.
 *
 * <p>This view is a sub-component of {@link FinancesPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code FinancesPages}, while this class is responsible only for constructing the Price Multipliers
 * panel and copying multiplier values to and from the model. The page is built lazily; until
 * {@link #createPanel(FinancesOptionsModel)} is called, {@link #readFromModel(FinancesOptionsModel)} and
 * {@link #writeToModel(FinancesOptionsModel)} are no-ops.</p>
 */
class PriceMultipliersPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int LABEL_CONTROL_GAP = 12;
    // Paired (4-column) grid widths for the Price Multipliers sections. The first
    // pair column is the label column plus
    // the form's label/control gap, so a two-column grid's column 3 sits where the
    // control column of 2-column form
    // sections does. The following pair is sized so the two-column grid's content
    // lands on the shared page-width floor
    // (measured: 312 + 303 -> 640px section). The control width keeps the spinners
    // uniform.
    private static final int GRID_FIRST_PAIR_COLUMN_WIDTH = LABEL_COLUMN_WIDTH + LABEL_CONTROL_GAP;
    private static final int GRID_FOLLOWING_PAIR_COLUMN_WIDTH = 303;
    private static final int GRID_CONTROL_COLUMN_WIDTH = 100;

    private CampaignOptionsHeaderPanel priceMultipliersHeader;
    private JPanel pnlGeneralMultipliers;
    private JLabel lblCommonPartPriceMultiplier;
    private JSpinner spnCommonPartPriceMultiplier;
    private JLabel lblInnerSphereUnitPriceMultiplier;
    private JSpinner spnInnerSphereUnitPriceMultiplier;
    private JLabel lblInnerSpherePartPriceMultiplier;
    private JSpinner spnInnerSpherePartPriceMultiplier;
    private JLabel lblClanUnitPriceMultiplier;
    private JSpinner spnClanUnitPriceMultiplier;
    private JLabel lblClanPartPriceMultiplier;
    private JSpinner spnClanPartPriceMultiplier;
    private JLabel lblMixedTechUnitPriceMultiplier;
    private JSpinner spnMixedTechUnitPriceMultiplier;

    private JPanel pnlUsedPartsMultipliers;
    private JLabel[] lblUsedPartPriceMultipliers;
    private JSpinner[] spnUsedPartPriceMultipliers;

    private JPanel pnlOtherMultipliers;
    private JLabel lblDamagedPartsValueMultiplier;
    private JSpinner spnDamagedPartsValueMultiplier;
    private JLabel lblUnrepairablePartsValueMultiplier;
    private JSpinner spnUnrepairablePartsValueMultiplier;
    private JLabel lblCancelledOrderRefundMultiplier;
    private JSpinner spnCancelledOrderRefundMultiplier;

    private boolean created;

    /**
     * Builds the Price Multipliers page.
     *
     * @param model the shared finances options model to populate the freshly built controls from
     *
     * @return a JPanel representing the Price Multipliers page
     */
    @Nonnull JPanel createPanel(@Nullable FinancesOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_stone_lion.png";
        priceMultipliersHeader = new CampaignOptionsHeaderPanel("PriceMultipliersPage",
                imageAddress);

        // Contents
        pnlGeneralMultipliers = createGeneralMultipliersPanel();
        pnlUsedPartsMultipliers = createUsedPartsMultiplierPanel();
        pnlOtherMultipliers = createOtherMultipliersPanel();

        JPanel panel = CampaignOptionsPagePanel.builder("PriceMultipliersPage", "PriceMultipliersPage", imageAddress)
                .header(priceMultipliersHeader)
                .intro("lblPriceMultipliersPageBody.text")
                .quote("priceMultipliersPage")
                .section("lblGeneralMultipliersPanel.text",
                        "lblGeneralMultipliersPanel.summary",
                        pnlGeneralMultipliers)
                .section("lblUsedPartsMultiplierPanel.text",
                        "lblUsedPartsMultiplierPanel.summary",
                        pnlUsedPartsMultipliers)
                .section("lblOtherMultipliersPanel.text",
                        "lblOtherMultipliersPanel.summary",
                        pnlOtherMultipliers)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates and configures the general multipliers panel, which includes labels
     * and spinners for various pricing
     * multipliers such as common parts, Inner Sphere units, Inner Sphere parts,
     * Clan units, Clan parts, and mixed tech
     * units. The panel is structured using a grid layout for organized placement of
     * components.
     *
     * @return a JPanel containing the components for setting general multipliers.
     */
    private @Nonnull JPanel createGeneralMultipliersPanel() {
        // Contents
        lblCommonPartPriceMultiplier = new CampaignOptionsLabel("CommonPartPriceMultiplier");
        lblCommonPartPriceMultiplier.addMouseListener(createTipPanelUpdater("CommonPartPriceMultiplier"));
        spnCommonPartPriceMultiplier = new CampaignOptionsSpinner("CommonPartPriceMultiplier", 1.0, 0.1, 100, 0.1);
        spnCommonPartPriceMultiplier.addMouseListener(createTipPanelUpdater("CommonPartPriceMultiplier"));

        lblInnerSphereUnitPriceMultiplier = new CampaignOptionsLabel("InnerSphereUnitPriceMultiplier");
        lblInnerSphereUnitPriceMultiplier.addMouseListener(createTipPanelUpdater("InnerSphereUnitPriceMultiplier"));
        spnInnerSphereUnitPriceMultiplier = new CampaignOptionsSpinner("InnerSphereUnitPriceMultiplier",
                1.0,
                0.1,
                100,
                0.1);
        spnInnerSphereUnitPriceMultiplier.addMouseListener(createTipPanelUpdater("InnerSphereUnitPriceMultiplier"));

        lblInnerSpherePartPriceMultiplier = new CampaignOptionsLabel("InnerSpherePartPriceMultiplier");
        lblInnerSpherePartPriceMultiplier.addMouseListener(createTipPanelUpdater("InnerSpherePartPriceMultiplier"));
        spnInnerSpherePartPriceMultiplier = new CampaignOptionsSpinner("InnerSpherePartPriceMultiplier",
                1.0,
                0.1,
                100,
                0.1);
        spnInnerSpherePartPriceMultiplier.addMouseListener(createTipPanelUpdater("InnerSpherePartPriceMultiplier"));

        lblClanUnitPriceMultiplier = new CampaignOptionsLabel("ClanUnitPriceMultiplier");
        lblClanUnitPriceMultiplier.addMouseListener(createTipPanelUpdater("ClanUnitPriceMultiplier"));
        spnClanUnitPriceMultiplier = new CampaignOptionsSpinner("ClanUnitPriceMultiplier", 1.0, 0.1, 100, 0.1);
        spnClanUnitPriceMultiplier.addMouseListener(createTipPanelUpdater("ClanUnitPriceMultiplier"));

        lblClanPartPriceMultiplier = new CampaignOptionsLabel("ClanPartPriceMultiplier");
        lblClanPartPriceMultiplier.addMouseListener(createTipPanelUpdater("ClanPartPriceMultiplier"));
        spnClanPartPriceMultiplier = new CampaignOptionsSpinner("ClanPartPriceMultiplier", 1.0, 0.1, 100, 0.1);
        spnClanPartPriceMultiplier.addMouseListener(createTipPanelUpdater("ClanPartPriceMultiplier"));

        lblMixedTechUnitPriceMultiplier = new CampaignOptionsLabel("MixedTechUnitPriceMultiplier");
        lblMixedTechUnitPriceMultiplier.addMouseListener(createTipPanelUpdater("MixedTechUnitPriceMultiplier"));
        spnMixedTechUnitPriceMultiplier = new CampaignOptionsSpinner("MixedTechUnitPriceMultiplier",
                1.0,
                0.1,
                100,
                0.1);
        spnMixedTechUnitPriceMultiplier.addMouseListener(createTipPanelUpdater("MixedTechUnitPriceMultiplier"));

        // Layout the Panel
        JComponent[] labels = { lblCommonPartPriceMultiplier, lblMixedTechUnitPriceMultiplier,
                lblInnerSphereUnitPriceMultiplier, lblInnerSpherePartPriceMultiplier,
                lblClanUnitPriceMultiplier, lblClanPartPriceMultiplier };
        JComponent[] controls = { spnCommonPartPriceMultiplier, spnMixedTechUnitPriceMultiplier,
                spnInnerSphereUnitPriceMultiplier, spnInnerSpherePartPriceMultiplier,
                spnClanUnitPriceMultiplier, spnClanPartPriceMultiplier };

        return createPriceMultiplierGridPanel("GeneralMultipliersPanel", labels, controls);
    }

    /**
     * Creates and returns a JPanel for configuring used parts price multipliers
     * based on part quality. Each part
     * quality level is represented with a label and a spinner for adjusting the
     * multiplier value.
     * <p>
     * The spinners are initialized with a range of values from 0.00 to 1.00,
     * incrementing by 0.05, and include
     * formatting for two decimal places. Additionally, the alignment of the spinner
     * text fields is set to left.
     * <p>
     * The panel is arranged using GridBagLayout to ensure proper alignment between
     * labels and spinners for each quality
     * level.
     *
     * @return A JPanel containing labels and spinners for used parts price
     *         multipliers.
     */
    private @Nonnull JPanel createUsedPartsMultiplierPanel() {
        // Contents
        lblUsedPartPriceMultipliers = new JLabel[QUALITY_F.ordinal() + 1];
        spnUsedPartPriceMultipliers = new JSpinner[QUALITY_F.ordinal() + 1];

        for (PartQuality partQuality : PartQuality.values()) {
            final String qualityLevel = partQuality.toName(false);
            int ordinal = partQuality.ordinal();

            lblUsedPartPriceMultipliers[ordinal] = new JLabel(qualityLevel);
            lblUsedPartPriceMultipliers[ordinal].setName("lbl" + qualityLevel);

            spnUsedPartPriceMultipliers[ordinal] = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 1.00, 0.05));
            spnUsedPartPriceMultipliers[ordinal].setName("spn" + qualityLevel);
            spnUsedPartPriceMultipliers[ordinal].setEditor(new NumberEditor(spnUsedPartPriceMultipliers[ordinal],
                    "0.00"));

            DefaultEditor editor = (DefaultEditor) spnUsedPartPriceMultipliers[ordinal].getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
            CampaignOptionsSpinner.installSelectAllOnFocus(spnUsedPartPriceMultipliers[ordinal]);
        }

        // Layout the Panel
        JComponent[] labels = new JComponent[spnUsedPartPriceMultipliers.length];
        JComponent[] controls = new JComponent[spnUsedPartPriceMultipliers.length];
        for (int index = 0; index < spnUsedPartPriceMultipliers.length; index++) {
            labels[index] = lblUsedPartPriceMultipliers[index];
            controls[index] = spnUsedPartPriceMultipliers[index];
        }

        return createPriceMultiplierGridPanel("UsedPartsMultiplierPanel", labels, controls);
    }

    /**
     * Creates and returns a JPanel configured with components for adjusting
     * multipliers related to damaged parts value,
     * unrepairable parts value, and cancelled order refunds. Each multiplier is
     * represented with a label and an
     * associated configurable spinner control.
     *
     * @return a JPanel instance containing the components for configuring the
     *         multipliers.
     */
    private @Nonnull JPanel createOtherMultipliersPanel() {
        // Contents
        lblDamagedPartsValueMultiplier = new CampaignOptionsLabel("DamagedPartsValueMultiplier");
        lblDamagedPartsValueMultiplier.addMouseListener(createTipPanelUpdater("DamagedPartsValueMultiplier"));
        spnDamagedPartsValueMultiplier = new CampaignOptionsSpinner("DamagedPartsValueMultiplier",
                0.33,
                0.00,
                1.00,
                0.05);
        spnDamagedPartsValueMultiplier.addMouseListener(createTipPanelUpdater("DamagedPartsValueMultiplier"));

        lblUnrepairablePartsValueMultiplier = new CampaignOptionsLabel("UnrepairablePartsValueMultiplier");
        lblUnrepairablePartsValueMultiplier.addMouseListener(createTipPanelUpdater("UnrepairablePartsValueMultiplier"));
        spnUnrepairablePartsValueMultiplier = new CampaignOptionsSpinner("UnrepairablePartsValueMultiplier",
                0.10,
                0.00,
                1.00,
                0.05);
        spnUnrepairablePartsValueMultiplier.addMouseListener(createTipPanelUpdater("UnrepairablePartsValueMultiplier"));

        lblCancelledOrderRefundMultiplier = new CampaignOptionsLabel("CancelledOrderRefundMultiplier");
        lblCancelledOrderRefundMultiplier.addMouseListener(createTipPanelUpdater("CancelledOrderRefundMultiplier"));
        spnCancelledOrderRefundMultiplier = new CampaignOptionsSpinner("CancelledOrderRefundMultiplier",
                0.50,
                0.00,
                1.00,
                0.05);
        spnCancelledOrderRefundMultiplier.addMouseListener(createTipPanelUpdater("CancelledOrderRefundMultiplier"));

        // Layout the Panel
        JComponent[] labels = { lblDamagedPartsValueMultiplier, lblUnrepairablePartsValueMultiplier,
                lblCancelledOrderRefundMultiplier };
        JComponent[] controls = { spnDamagedPartsValueMultiplier, spnUnrepairablePartsValueMultiplier,
                spnCancelledOrderRefundMultiplier };

        return createPriceMultiplierGridPanel("OtherMultipliersPanel", labels, controls);
    }

    /**
     * Builds a Price Multipliers section as a two-column
     * ({@code label/control, label/control}) aligned grid. The pair
     * widths are shared by every Price Multipliers section so their columns line
     * up, and are sized so the section stays
     * within the dialog's common page width.
     *
     * @param name     the section's base name; the Swing component name becomes
     *                 {@code "pnl" + name}
     * @param labels   the label components, one per field, in row-major order
     * @param controls the control components, matching {@code labels} by index
     *
     * @return the assembled paired-field grid panel
     */
    private @Nonnull CampaignOptionsPairedFieldGridPanel createPriceMultiplierGridPanel(String name, JComponent[] labels,
            JComponent[] controls) {
        final CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel(name,
                GRID_FIRST_PAIR_COLUMN_WIDTH,
                GRID_FOLLOWING_PAIR_COLUMN_WIDTH,
                GRID_CONTROL_COLUMN_WIDTH,
                2);
        panel.addPairs(labels, controls);

        return panel;
    }

    /**
     * Copies price-multiplier values from the shared model into this page's controls. This is a no-op until the page has
     * been built.
     *
     * @param model the shared finances options model to read values from
     */
    void readFromModel(@Nullable FinancesOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        spnCommonPartPriceMultiplier.setValue(model.commonPartPriceMultiplier);
        spnInnerSphereUnitPriceMultiplier.setValue(model.innerSphereUnitPriceMultiplier);
        spnInnerSpherePartPriceMultiplier.setValue(model.innerSpherePartPriceMultiplier);
        spnClanUnitPriceMultiplier.setValue(model.clanUnitPriceMultiplier);
        spnClanPartPriceMultiplier.setValue(model.clanPartPriceMultiplier);
        spnMixedTechUnitPriceMultiplier.setValue(model.mixedTechUnitPriceMultiplier);
        for (int i = 0; i < Math.min(spnUsedPartPriceMultipliers.length,
                model.usedPartPriceMultipliers.length); i++) {
            spnUsedPartPriceMultipliers[i].setValue(model.usedPartPriceMultipliers[i]);
        }
        spnDamagedPartsValueMultiplier.setValue(model.damagedPartsValueMultiplier);
        spnUnrepairablePartsValueMultiplier.setValue(model.unrepairablePartsValueMultiplier);
        spnCancelledOrderRefundMultiplier.setValue(model.cancelledOrderRefundMultiplier);
    }

    /**
     * Copies price-multiplier values from this page's controls into the shared model. This is a no-op until the page has
     * been built.
     *
     * @param model the shared finances options model to write values into
     */
    void writeToModel(@Nullable FinancesOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.commonPartPriceMultiplier = (double) spnCommonPartPriceMultiplier.getValue();
        model.innerSphereUnitPriceMultiplier = (double) spnInnerSphereUnitPriceMultiplier.getValue();
        model.innerSpherePartPriceMultiplier = (double) spnInnerSpherePartPriceMultiplier.getValue();
        model.clanUnitPriceMultiplier = (double) spnClanUnitPriceMultiplier.getValue();
        model.clanPartPriceMultiplier = (double) spnClanPartPriceMultiplier.getValue();
        model.mixedTechUnitPriceMultiplier = (double) spnMixedTechUnitPriceMultiplier.getValue();
        for (int i = 0; i < Math.min(spnUsedPartPriceMultipliers.length,
                model.usedPartPriceMultipliers.length); i++) {
            model.usedPartPriceMultipliers[i] = (Double) spnUsedPartPriceMultipliers[i].getValue();
        }
        model.damagedPartsValueMultiplier = (double) spnDamagedPartsValueMultiplier.getValue();
        model.unrepairablePartsValueMultiplier = (double) spnUnrepairablePartsValueMultiplier.getValue();
        model.cancelledOrderRefundMultiplier = (double) spnCancelledOrderRefundMultiplier.getValue();
    }
}
