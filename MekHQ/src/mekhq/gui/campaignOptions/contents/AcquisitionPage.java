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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;

/**
 * The {@code AcquisitionPage} class builds and manages the Acquisition leaf page of the Campaign Options dialog. It owns
 * the widgets for acquisition settings, the auto-logistics stock grid, and delivery/transit options, and synchronises
 * them with a shared {@link EquipmentAndSuppliesOptionsModel}.
 *
 * <p>This view is a sub-component of {@link EquipmentAndSuppliesPages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code EquipmentAndSuppliesPages}, while this class is responsible only for constructing the
 * Acquisition panel and copying acquisition values to and from the model. The page is built lazily; until
 * {@link #createPanel(EquipmentAndSuppliesOptionsModel)} is called,
 * {@link #readFromModel(EquipmentAndSuppliesOptionsModel)} and {@link #writeToModel(EquipmentAndSuppliesOptionsModel)}
 * are no-ops.</p>
 */
class AcquisitionPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int AUTO_LOGISTICS_LABEL_COLUMN_WIDTH = 190;
    private static final int AUTO_LOGISTICS_CONTROL_COLUMN_WIDTH = 90;
    private static final int AUTO_LOGISTICS_PAIRS_PER_ROW = 2;

    /**
     * Label-column width for the Acquisition page's single-control sections
     * (Acquisitions, Deliveries). It is computed
     * at runtime from the AutoLogistics grid (see
     * {@link #createAutoLogisticsPanel()}) so those sections' control
     * column lines up with the grid's second label column. Until then it falls back
     * to the standard label width.
     */
    private int acquisitionSectionLabelWidth = LABEL_COLUMN_WIDTH;

    private CampaignOptionsHeaderPanel acquisitionHeader;
    private JPanel pnlAcquisitions;
    private JLabel lblChoiceAcquireSkill;
    private MMComboBox<AcquisitionsType> choiceAcquireSkill;
    private JCheckBox chkUseFunctionalAppraisal;
    private JLabel lblAcquireClanPenalty;
    private JLabel lblProcurementPersonnelPick;
    private MMComboBox<ProcurementPersonnelPick> cboProcurementPersonnelPick;
    private JSpinner spnAcquireClanPenalty;
    private JLabel lblAcquireIsPenalty;
    private JSpinner spnAcquireIsPenalty;
    private JLabel lblAcquireWaitingPeriod;
    private JSpinner spnAcquireWaitingPeriod;
    private JLabel lblMaxAcquisitions;
    private JSpinner spnMaxAcquisitions;

    private JPanel pnlAutoLogistics;
    private JLabel lblAutoLogisticsHeatSink;
    private JSpinner spnAutoLogisticsHeatSink;
    private JLabel lblAutoLogisticsWeapons;
    private JSpinner spnAutoLogisticsWeapons;
    private JLabel lblAutoLogisticsMekHead;
    private JSpinner spnAutoLogisticsMekHead;
    private JLabel lblAutoLogisticsMekLocation;
    private JSpinner spnAutoLogisticsMekLocation;
    private JLabel lblAutoLogisticsNonRepairableLocation;
    private JSpinner spnAutoLogisticsNonRepairableLocation;
    private JLabel lblAutoLogisticsArmor;
    private JSpinner spnAutoLogisticsArmor;
    private JLabel lblAutoLogisticsAmmunition;
    private JSpinner spnAutoLogisticsAmmunition;
    private JLabel lblAutoLogisticsActuators;
    private JSpinner spnAutoLogisticsActuators;
    private JLabel lblAutoLogisticsJumpJets;
    private JSpinner spnAutoLogisticsJumpJets;
    private JLabel lblAutoLogisticsEngines;
    private JSpinner spnAutoLogisticsEngines;
    private JLabel lblAutoLogisticsHeadComponents;
    private JSpinner spnAutoLogisticsHeadComponents;
    private JLabel lblAutoLogisticsGyros;
    private JSpinner spnAutoLogisticsGyros;
    private JLabel lblAutoLogisticsOther;
    private JSpinner spnAutoLogisticsOther;

    private JLabel lblTransitTimeUnits;
    private MMComboBox<String> choiceTransitTimeUnits;
    private static final int TRANSIT_UNIT_DAY = 0;
    private static final int TRANSIT_UNIT_WEEK = 1;
    private static final int TRANSIT_UNIT_MONTH = 2;
    private static final int TRANSIT_UNIT_NUM = 3;
    private JCheckBox chkNoDeliveriesInTransit;

    private boolean created;

    /**
     * Creates and configures the acquisition page panel for the user interface. This method initializes and organizes
     * the components such as the header, acquisition panel, and delivery panel, and then returns the fully constructed
     * acquisition page panel.
     *
     * @param model the shared equipment and supplies options model to populate the freshly built controls from
     *
     * @return A {@code JPanel} instance representing the complete acquisition page.
     */
    @Nonnull JPanel createPanel(@Nullable EquipmentAndSuppliesOptionsModel model) {
        // Combo boxes built in the page's former initialize step
        choiceAcquireSkill = new MMComboBox<>("choiceAcquireSkill", AcquisitionsType.values());
        cboProcurementPersonnelPick = new MMComboBox<>("procurementPersonnelPick",
              buildProcurementPersonnelPickComboOptions());
        choiceTransitTimeUnits = new MMComboBox<>("choiceTransitTimeUnits", getTransitUnitOptions());

        // Header
        String imageAddress = getImageDirectory() + "logo_clan_cloud_cobra.png";
        acquisitionHeader = new CampaignOptionsHeaderPanel("AcquisitionPage", imageAddress);

        // Build AutoLogistics first: it measures its grid and sets
        // acquisitionSectionLabelWidth, which the
        // Acquisitions and Deliveries sections then use so their control column aligns
        // with the grid's second column.
        pnlAutoLogistics = createAutoLogisticsPanel();
        pnlAcquisitions = createAcquisitionPanel();
        JPanel pnlDelivery = createDeliveryPanel();

        JPanel panel = CampaignOptionsPagePanel.builder("AcquisitionPage", "AcquisitionPage", imageAddress)
                .header(acquisitionHeader)
                .quote("acquisitionPage")
                .section("lblAcquisitionPanel.text",
                        "lblAcquisitionPanel.summary",
                        pnlAcquisitions)
                .section("lblDeliveryPanel.text",
                        "lblDeliveryPanel.summary",
                        pnlDelivery)
                .section("lblAutoLogisticsPanel.text",
                        "lblAutoLogisticsPanel.summary",
                        pnlAutoLogistics)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates and returns a {@code JPanel} for configuring acquisition-related options. This panel includes various
     * components such as labels, checkboxes, and spinners to allow users to set values for acquisition settings,
     * including penalties, waiting periods, maximum acquisitions, and stock percentages.
     *
     * @return A {@code JPanel} populated with acquisition configuration components and their layout.
     */
    private @Nonnull JPanel createAcquisitionPanel() {
        // Content
        lblChoiceAcquireSkill = new CampaignOptionsLabel("ChoiceAcquireSkill");
        lblChoiceAcquireSkill.addMouseListener(createTipPanelUpdater("ChoiceAcquireSkill"));

        chkUseFunctionalAppraisal = new CampaignOptionsCheckBox("UseFunctionalAppraisal",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseFunctionalAppraisal.addMouseListener(createTipPanelUpdater("UseFunctionalAppraisal"));

        lblProcurementPersonnelPick = new CampaignOptionsLabel("ProcurementPersonnelPick");
        lblProcurementPersonnelPick.addMouseListener(createTipPanelUpdater("ProcurementPersonnelPick"));

        lblAcquireClanPenalty = new CampaignOptionsLabel("AcquireClanPenalty");
        lblAcquireClanPenalty.addMouseListener(createTipPanelUpdater("AcquireClanPenalty"));
        spnAcquireClanPenalty = new CampaignOptionsSpinner("AcquireClanPenalty", 0, 0, 13, 1);
        spnAcquireClanPenalty.addMouseListener(createTipPanelUpdater("AcquireClanPenalty"));

        lblAcquireIsPenalty = new CampaignOptionsLabel("AcquireISPenalty");
        lblAcquireIsPenalty.addMouseListener(createTipPanelUpdater("AcquireISPenalty"));
        spnAcquireIsPenalty = new CampaignOptionsSpinner("AcquireISPenalty", 0, 0, 13, 1);
        spnAcquireIsPenalty.addMouseListener(createTipPanelUpdater("AcquireISPenalty"));

        lblAcquireWaitingPeriod = new CampaignOptionsLabel("AcquireWaitingPeriod");
        lblAcquireWaitingPeriod.addMouseListener(createTipPanelUpdater("AcquireWaitingPeriod"));
        spnAcquireWaitingPeriod = new CampaignOptionsSpinner("AcquireWaitingPeriod", 1, 1, 365, 1);
        spnAcquireWaitingPeriod.addMouseListener(createTipPanelUpdater("AcquireWaitingPeriod"));

        lblMaxAcquisitions = new CampaignOptionsLabel("MaxAcquisitions");
        lblMaxAcquisitions.addMouseListener(createTipPanelUpdater("MaxAcquisitions"));
        spnMaxAcquisitions = new CampaignOptionsSpinner("MaxAcquisitions", 0, 0, 100, 1);
        spnMaxAcquisitions.addMouseListener(createTipPanelUpdater("MaxAcquisitions"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AcquisitionPanel",
                acquisitionSectionLabelWidth,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblChoiceAcquireSkill, choiceAcquireSkill);
        panel.addCheckBox(chkUseFunctionalAppraisal);
        panel.addRow(lblProcurementPersonnelPick, cboProcurementPersonnelPick);
        panel.addRow(lblAcquireClanPenalty, spnAcquireClanPenalty);
        panel.addRow(lblAcquireIsPenalty, spnAcquireIsPenalty);
        panel.addRow(lblAcquireWaitingPeriod, spnAcquireWaitingPeriod);
        panel.addRow(lblMaxAcquisitions, spnMaxAcquisitions);

        return panel;
    }

    private @Nonnull JPanel createDeliveryPanel() {
        lblTransitTimeUnits = new CampaignOptionsLabel("TransitTimeUnits");
        lblTransitTimeUnits.addMouseListener(createTipPanelUpdater("TransitTimeUnits"));
        choiceTransitTimeUnits.addMouseListener(createTipPanelUpdater("TransitTimeUnits"));

        chkNoDeliveriesInTransit = new CampaignOptionsCheckBox("NoDeliveriesInTransit",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkNoDeliveriesInTransit.addMouseListener(createTipPanelUpdater("NoDeliveriesInTransit"));

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("DeliveryPanel",
                acquisitionSectionLabelWidth,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblTransitTimeUnits, choiceTransitTimeUnits);
        panel.addCheckBox(chkNoDeliveriesInTransit);

        return panel;
    }

    /**
     * Creates and returns a {@code JPanel} for configuring autoLogistics-related options. This panel includes various
     * components such as labels, checkboxes, and spinners to allow users to set values for acquisition settings,
     * including penalties, waiting periods, maximum acquisitions, and stock percentages.
     *
     * @return A {@code JPanel} populated with autoLogistics configuration components and their layout.
     */
    private @Nonnull JPanel createAutoLogisticsPanel() {
        // Content
        lblAutoLogisticsMekHead = new CampaignOptionsLabel("AutoLogisticsMekHead");
        lblAutoLogisticsMekHead.addMouseListener(createTipPanelUpdater("AutoLogisticsMekHead"));
        spnAutoLogisticsMekHead = new CampaignOptionsSpinner("AutoLogisticsMekHead", 200, 0, 10000, 1);
        spnAutoLogisticsMekHead.addMouseListener(createTipPanelUpdater("AutoLogisticsMekHead"));

        lblAutoLogisticsMekLocation = new CampaignOptionsLabel("AutoLogisticsMekLocation");
        lblAutoLogisticsMekLocation.addMouseListener(createTipPanelUpdater("AutoLogisticsMekLocation"));
        spnAutoLogisticsMekLocation = new CampaignOptionsSpinner("AutoLogisticsMekLocation", 100, 0, 10000, 1);
        spnAutoLogisticsMekLocation.addMouseListener(createTipPanelUpdater("AutoLogisticsMekLocation"));

        lblAutoLogisticsNonRepairableLocation = new CampaignOptionsLabel("AutoLogisticsNonRepairableLocation");
        lblAutoLogisticsNonRepairableLocation.addMouseListener(createTipPanelUpdater("AutoLogisticsNonRepairableLocation"));
        spnAutoLogisticsNonRepairableLocation = new CampaignOptionsSpinner("AutoLogisticsNonRepairableLocation",
              0,
              0,
              10000,
              1);
        spnAutoLogisticsNonRepairableLocation.addMouseListener(createTipPanelUpdater("AutoLogisticsNonRepairableLocation"));

        lblAutoLogisticsArmor = new CampaignOptionsLabel("AutoLogisticsArmor");
        lblAutoLogisticsArmor.addMouseListener(createTipPanelUpdater("AutoLogisticsArmor"));
        spnAutoLogisticsArmor = new CampaignOptionsSpinner("AutoLogisticsArmor", 500, 0, 10000, 1);
        spnAutoLogisticsArmor.addMouseListener(createTipPanelUpdater("AutoLogisticsArmor"));

        lblAutoLogisticsAmmunition = new CampaignOptionsLabel("AutoLogisticsAmmunition");
        lblAutoLogisticsAmmunition.addMouseListener(createTipPanelUpdater("AutoLogisticsAmmunition"));
        spnAutoLogisticsAmmunition = new CampaignOptionsSpinner("AutoLogisticsAmmunition", 500, 0, 10000, 1);
        spnAutoLogisticsAmmunition.addMouseListener(createTipPanelUpdater("AutoLogisticsAmmunition"));

        lblAutoLogisticsHeatSink = new CampaignOptionsLabel("AutoLogisticsHeatSink");
        lblAutoLogisticsHeatSink.addMouseListener(createTipPanelUpdater("AutoLogisticsHeatSink"));
        spnAutoLogisticsHeatSink = new CampaignOptionsSpinner("AutoLogisticsHeatSink", 250, 0, 10000, 1);
        spnAutoLogisticsHeatSink.addMouseListener(createTipPanelUpdater("AutoLogisticsHeatSink"));

        lblAutoLogisticsWeapons = new CampaignOptionsLabel("AutoLogisticsWeapons");
        lblAutoLogisticsWeapons.addMouseListener(createTipPanelUpdater("AutoLogisticsWeapons"));
        spnAutoLogisticsWeapons = new CampaignOptionsSpinner("AutoLogisticsWeapons", 50, 0, 10000, 1);
        spnAutoLogisticsWeapons.addMouseListener(createTipPanelUpdater("AutoLogisticsWeapons"));

        lblAutoLogisticsActuators = new CampaignOptionsLabel("AutoLogisticsActuators");
        lblAutoLogisticsActuators.addMouseListener(createTipPanelUpdater("AutoLogisticsActuators"));
        spnAutoLogisticsActuators = new CampaignOptionsSpinner("AutoLogisticsActuators", 250, 0, 10000, 1);
        spnAutoLogisticsActuators.addMouseListener(createTipPanelUpdater("AutoLogisticsActuators"));

        lblAutoLogisticsJumpJets = new CampaignOptionsLabel("AutoLogisticsJumpJets");
        lblAutoLogisticsJumpJets.addMouseListener(createTipPanelUpdater("AutoLogisticsJumpJets"));
        spnAutoLogisticsJumpJets = new CampaignOptionsSpinner("AutoLogisticsJumpJets", 250, 0, 10000, 1);
        spnAutoLogisticsJumpJets.addMouseListener(createTipPanelUpdater("AutoLogisticsJumpJets"));

        lblAutoLogisticsHeadComponents = new CampaignOptionsLabel("AutoLogisticsHeadComponents",
              getMetadata(new Version(0, 51, 1)));
        lblAutoLogisticsHeadComponents.addMouseListener(createTipPanelUpdater("AutoLogisticsHeadComponents"));
        spnAutoLogisticsHeadComponents = new CampaignOptionsSpinner("AutoLogisticsHeadComponents", 15, 0, 10000, 1);
        spnAutoLogisticsHeadComponents.addMouseListener(createTipPanelUpdater("AutoLogisticsHeadComponents"));

        lblAutoLogisticsEngines = new CampaignOptionsLabel("AutoLogisticsEngines");
        lblAutoLogisticsEngines.addMouseListener(createTipPanelUpdater("AutoLogisticsEngines"));
        spnAutoLogisticsEngines = new CampaignOptionsSpinner("AutoLogisticsEngines", 250, 0, 10000, 1);
        spnAutoLogisticsEngines.addMouseListener(createTipPanelUpdater("AutoLogisticsEngines"));

        lblAutoLogisticsGyros = new CampaignOptionsLabel("AutoLogisticsGyros", getMetadata(new Version(0, 51, 1)));
        lblAutoLogisticsGyros.addMouseListener(createTipPanelUpdater("AutoLogisticsGyros"));
        spnAutoLogisticsGyros = new CampaignOptionsSpinner("AutoLogisticsGyros", 0, 0, 10000, 1);
        spnAutoLogisticsGyros.addMouseListener(createTipPanelUpdater("AutoLogisticsGyros"));

        lblAutoLogisticsOther = new CampaignOptionsLabel("AutoLogisticsOther");
        lblAutoLogisticsOther.addMouseListener(createTipPanelUpdater("AutoLogisticsOther"));
        spnAutoLogisticsOther = new CampaignOptionsSpinner("AutoLogisticsOther", 50, 0, 10000, 1);
        spnAutoLogisticsOther.addMouseListener(createTipPanelUpdater("AutoLogisticsOther"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AutoLogisticsPanel",
              AUTO_LOGISTICS_LABEL_COLUMN_WIDTH,
              AUTO_LOGISTICS_CONTROL_COLUMN_WIDTH);
        panel.addRowGrid(AUTO_LOGISTICS_PAIRS_PER_ROW,
              lblAutoLogisticsMekHead, spnAutoLogisticsMekHead,
              lblAutoLogisticsMekLocation, spnAutoLogisticsMekLocation,
              lblAutoLogisticsNonRepairableLocation, spnAutoLogisticsNonRepairableLocation,
              lblAutoLogisticsHeatSink, spnAutoLogisticsHeatSink,
              lblAutoLogisticsArmor, spnAutoLogisticsArmor,
              lblAutoLogisticsAmmunition, spnAutoLogisticsAmmunition,
              lblAutoLogisticsActuators, spnAutoLogisticsActuators,
              lblAutoLogisticsJumpJets, spnAutoLogisticsJumpJets,
              lblAutoLogisticsHeadComponents, spnAutoLogisticsHeadComponents,
              lblAutoLogisticsEngines, spnAutoLogisticsEngines,
              lblAutoLogisticsGyros, spnAutoLogisticsGyros,
              lblAutoLogisticsWeapons, spnAutoLogisticsWeapons,
              lblAutoLogisticsOther, spnAutoLogisticsOther);

        // Compute where this grid's second label column (the "third column") begins, so
        // the single-control sections
        // can size their label column to match and line their control column up with
        // it. With two pairs per row the
        // left pair occupies columns 0 (label) and 1 (spinner); the second pair's label
        // begins after both, plus the
        // inter-pair gap. addRowGrid floors each component at the configured widths, so
        // we measure the realized
        // (font-scaled) preferred widths of the left pair here, which keeps the
        // alignment correct at any GUI scale.
        int firstColumnLabelWidth = widestPreferredWidth(AUTO_LOGISTICS_LABEL_COLUMN_WIDTH,
                lblAutoLogisticsMekHead,
                lblAutoLogisticsNonRepairableLocation,
                lblAutoLogisticsArmor,
                lblAutoLogisticsActuators,
                lblAutoLogisticsEngines,
                lblAutoLogisticsOther);
        int firstColumnControlWidth = widestPreferredWidth(AUTO_LOGISTICS_CONTROL_COLUMN_WIDTH,
                spnAutoLogisticsMekHead,
                spnAutoLogisticsNonRepairableLocation,
                spnAutoLogisticsArmor,
                spnAutoLogisticsActuators,
                spnAutoLogisticsEngines,
                spnAutoLogisticsOther);
        // The label's own right padding and the grid label's right padding are equal,
        // so they cancel; what remains is
        // the first label column, the first control column, and the inter-pair gap
        // between them.
        acquisitionSectionLabelWidth = firstColumnLabelWidth + firstColumnControlWidth
                + CampaignOptionsFormPanel.GRID_COLUMN_GAP;

        return panel;
    }

    /**
     * Returns the widest preferred width among the given components, but never less
     * than {@code floor}.
     *
     * @param floor      the minimum width to return
     * @param components the components to measure
     *
     * @return the largest of {@code floor} and the components' preferred widths
     */
    private static int widestPreferredWidth(int floor, JComponent... components) {
        int width = floor;
        for (JComponent component : components) {
            width = Math.max(width, component.getPreferredSize().width);
        }
        return width;
    }

    /**
     * Creates and returns a DefaultComboBoxModel containing the transit unit options.
     *
     * @return a DefaultComboBoxModel<String> populated with transit unit names based on TRANSIT_UNIT_NUM.
     */
    private static DefaultComboBoxModel<String> getTransitUnitOptions() {
        DefaultComboBoxModel<String> transitUnitModel = new DefaultComboBoxModel<>();

        for (int i = 0; i < TRANSIT_UNIT_NUM; i++) {
            transitUnitModel.addElement(getTransitUnitName(i));
        }
        return transitUnitModel;
    }

    /**
     * Retrieves the name of the transit unit based on the provided unit value.
     *
     * @param unit the integer value representing the transit unit (e.g., day, week, month)
     *
     * @return the name of the transit unit as a string, or "ERROR" if the unit is not recognized
     */
    private static String getTransitUnitName(final int unit) {
        return switch (unit) {
            case TRANSIT_UNIT_DAY -> getTextAt(getCampaignOptionsResourceBundle(), "transitUnitNamesDays.text");
            case TRANSIT_UNIT_WEEK -> getTextAt(getCampaignOptionsResourceBundle(), "transitUnitNamesWeeks.text");
            case TRANSIT_UNIT_MONTH -> getTextAt(getCampaignOptionsResourceBundle(), "transitUnitNamesMonths.text");
            default -> "ERROR";
        };
    }

    /**
     * Builds a {@link DefaultComboBoxModel} containing options for all available {@link ProcurementPersonnelPick}
     * values.
     *
     * <p>This method iterates through all the values of the {@link ProcurementPersonnelPick}
     * enumeration and adds them as elements to the combo box model. The resulting model can be used to
     * populate a combo box in the user interface, allowing users to select a personnel category for procurement
     * purposes.</p>
     *
     * @return A {@link DefaultComboBoxModel} populated with all {@link ProcurementPersonnelPick} values.
     *
     * @see ProcurementPersonnelPick#values() Retrieves all defined personnel pick options.
     */
    private static DefaultComboBoxModel<ProcurementPersonnelPick> buildProcurementPersonnelPickComboOptions() {
        DefaultComboBoxModel<ProcurementPersonnelPick> procurementPersonnelPick = new DefaultComboBoxModel<>();

        for (ProcurementPersonnelPick pick : ProcurementPersonnelPick.values()) {
            procurementPersonnelPick.addElement(pick);
        }

        return procurementPersonnelPick;
    }

    /**
     * Copies acquisition values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared equipment and supplies options model to read values from
     */
    void readFromModel(@Nullable EquipmentAndSuppliesOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        choiceAcquireSkill.setSelectedItem(model.acquisitionType);
        chkUseFunctionalAppraisal.setSelected(model.useFunctionalAppraisal);
        cboProcurementPersonnelPick.setSelectedItem(model.acquisitionPersonnelCategory);
        spnAcquireClanPenalty.setValue(model.clanAcquisitionPenalty);
        spnAcquireIsPenalty.setValue(model.isAcquisitionPenalty);
        spnAcquireWaitingPeriod.setValue(model.waitingPeriod);
        spnMaxAcquisitions.setValue(model.maxAcquisitions);
        spnAutoLogisticsMekHead.setValue(model.autoLogisticsMekHead);
        spnAutoLogisticsMekLocation.setValue(model.autoLogisticsMekLocation);
        spnAutoLogisticsNonRepairableLocation.setValue(model.autoLogisticsNonRepairableLocation);
        spnAutoLogisticsArmor.setValue(model.autoLogisticsArmor);
        spnAutoLogisticsAmmunition.setValue(model.autoLogisticsAmmunition);
        spnAutoLogisticsActuators.setValue(model.autoLogisticsActuators);
        spnAutoLogisticsJumpJets.setValue(model.autoLogisticsJumpJets);
        spnAutoLogisticsHeadComponents.setValue(model.autoLogisticsHeadComponents);
        spnAutoLogisticsEngines.setValue(model.autoLogisticsEngines);
        spnAutoLogisticsGyros.setValue(model.autoLogisticsGyros);
        spnAutoLogisticsHeatSink.setValue(model.autoLogisticsHeatSink);
        spnAutoLogisticsWeapons.setValue(model.autoLogisticsWeapons);
        spnAutoLogisticsOther.setValue(model.autoLogisticsOther);
        choiceTransitTimeUnits.setSelectedIndex(model.unitTransitTime);
        chkNoDeliveriesInTransit.setSelected(model.noDeliveriesInTransit);
    }

    /**
     * Copies acquisition values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared equipment and supplies options model to write values into
     */
    void writeToModel(@Nullable EquipmentAndSuppliesOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.acquisitionType = choiceAcquireSkill.getSelectedItem();
        model.useFunctionalAppraisal = chkUseFunctionalAppraisal.isSelected();
        model.acquisitionPersonnelCategory = cboProcurementPersonnelPick.getSelectedItem();
        model.clanAcquisitionPenalty = (int) spnAcquireClanPenalty.getValue();
        model.isAcquisitionPenalty = (int) spnAcquireIsPenalty.getValue();
        model.waitingPeriod = (int) spnAcquireWaitingPeriod.getValue();
        model.maxAcquisitions = (int) spnMaxAcquisitions.getValue();
        model.autoLogisticsMekHead = (int) spnAutoLogisticsMekHead.getValue();
        model.autoLogisticsMekLocation = (int) spnAutoLogisticsMekLocation.getValue();
        model.autoLogisticsNonRepairableLocation = (int) spnAutoLogisticsNonRepairableLocation.getValue();
        model.autoLogisticsArmor = (int) spnAutoLogisticsArmor.getValue();
        model.autoLogisticsAmmunition = (int) spnAutoLogisticsAmmunition.getValue();
        model.autoLogisticsActuators = (int) spnAutoLogisticsActuators.getValue();
        model.autoLogisticsJumpJets = (int) spnAutoLogisticsJumpJets.getValue();
        model.autoLogisticsHeadComponents = (int) spnAutoLogisticsHeadComponents.getValue();
        model.autoLogisticsEngines = (int) spnAutoLogisticsEngines.getValue();
        model.autoLogisticsGyros = (int) spnAutoLogisticsGyros.getValue();
        model.autoLogisticsHeatSink = (int) spnAutoLogisticsHeatSink.getValue();
        model.autoLogisticsWeapons = (int) spnAutoLogisticsWeapons.getValue();
        model.autoLogisticsOther = (int) spnAutoLogisticsOther.getValue();
        model.unitTransitTime = choiceTransitTimeUnits.getSelectedIndex();
        model.noDeliveriesInTransit = chkNoDeliveriesInTransit.isSelected();
    }
}
