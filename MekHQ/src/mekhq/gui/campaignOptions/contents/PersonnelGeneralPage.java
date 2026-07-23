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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.personnel.enums.EdgeRefreshPeriod;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code PersonnelGeneralPage} class builds and manages the General leaf page of the Personnel section of the
 * Campaign Options dialog. It owns the widgets for general personnel options (tactics, edge, implants, quality
 * averaging), personnel cleanup, administrator negotiation, and blob-crew settings, and synchronises them with a shared
 * {@link PersonnelOptionsModel}.
 *
 * <p>This view is a sub-component of {@link PersonnelPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code PersonnelPages}, while this class is responsible only for constructing the General panel and
 * copying its values to and from the model. The page is built lazily; until {@link #createPanel(PersonnelOptionsModel)}
 * is called, {@link #readFromModel(PersonnelOptionsModel)} and {@link #writeToModel(PersonnelOptionsModel)} are
 * no-ops.</p>
 */
class PersonnelGeneralPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

    private CampaignOptionsHeaderPanel generalHeader;
    private JCheckBox chkUseTactics;
    private JCheckBox chkUseInitiativeBonus;
    private JCheckBox chkUseSensibleTactics;
    private JCheckBox chkUseToughness;
    private JCheckBox chkUseRandomToughness;
    private JCheckBox chkUseArtillery;
    private JCheckBox chkUseAbilities;
    private JCheckBox chkOnlyCommandersMatterVehicles;
    private JCheckBox chkOnlyCommandersMatterInfantry;
    private JCheckBox chkOnlyCommandersMatterBattleArmor;
    private JCheckBox chkUseEdge;
    private JCheckBox chkUseTwistOfFateSurvival;
    private JCheckBox chkUseFoundersHavePlotArmor;
    private MMComboBox<EdgeRefreshPeriod> comboEdgeRefreshPeriod;
    private JSpinner spnEdgeRefreshCost;
    private JCheckBox chkUseImplants;
    private JCheckBox chkUseAlternativeQualityAveraging;

    private JCheckBox chkUsePersonnelRemoval;
    private JCheckBox chkUseRemovalExemptCemetery;
    private JCheckBox chkUseRemovalExemptRetirees;

    private JCheckBox chkAdminsHaveNegotiation;
    private JCheckBox chkAdminExperienceLevelIncludeNegotiation;

    private JCheckBox chkUseBlobInfantry;
    private JCheckBox chkUseBlobBattleArmor;
    private JCheckBox chkUseBlobVehicleCrewGround;
    private JCheckBox chkUseBlobVehicleCrewVTOL;
    private JCheckBox chkUseBlobVehicleCrewNaval;
    private JCheckBox chkUseBlobVesselPilot;
    private JCheckBox chkUseBlobVesselGunner;
    private JCheckBox chkUseBlobVesselCrew;

    private boolean created;

    /**
     * Builds the General page, populates its controls from the supplied model, and returns the assembled panel.
     *
     * @param model the shared personnel options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the General Page
     */
    @Nonnull
    JPanel createPanel(@Nullable PersonnelOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_wolverine.png";
        generalHeader = new CampaignOptionsHeaderPanel("PersonnelGeneralPage", imageAddress);

        // Contents
        comboEdgeRefreshPeriod = new MMComboBox<>("comboEdgeRefreshPeriod", EdgeRefreshPeriod.values());
        JPanel pnlPersonnelGeneralOptions = createGeneralOptionsPanel();
        JPanel pnlPersonnelCleanup = createPersonnelCleanUpPanel();
        JPanel pnlAdministrators = createAdministratorsPanel();
        JPanel pnlBlobCrew = createBlobCrewPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("PersonnelGeneralPage", "PersonnelGeneralPage", imageAddress)
                             .header(generalHeader)
                             .quote("personnelGeneralPage")
                             .section("lblPersonnelGeneralPage.text",
                                   "lblPersonnelGeneralPage.summary",
                                   pnlPersonnelGeneralOptions)
                             .section("lblPersonnelCleanUpPanel.text",
                                   "lblPersonnelCleanUpPanel.summary",
                                   pnlPersonnelCleanup,
                                   getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                             .section("lblAdministratorsPanel.text",
                                   "lblAdministratorsPanel.summary",
                                   pnlAdministrators,
                                   getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                             .section("lblBlobCrewPanel.text",
                                   "lblBlobCrewPanel.summary",
                                   pnlBlobCrew,
                                   getMetadata(new Version(0, 50, 12)))
                             .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates the panel for general personnel options in the General Page.
     *
     * @return a {@link JPanel} containing checkboxes for various personnel management settings
     */
    private @Nonnull JPanel createGeneralOptionsPanel() {
        // Contents
        chkUseTactics = new CampaignOptionsCheckBox("UseTactics",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkUseTactics.addMouseListener(createTipPanelUpdater("UseTactics"));
        chkUseInitiativeBonus = new CampaignOptionsCheckBox("UseInitiativeBonus",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkUseInitiativeBonus.addMouseListener(createTipPanelUpdater("UseInitiativeBonus"));
        chkUseSensibleTactics = new CampaignOptionsCheckBox("UseSensibleTactics",
              getMetadata(new Version(0, 51, 1), CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseSensibleTactics.addMouseListener(createTipPanelUpdater("UseSensibleTactics"));
        chkUseToughness = new CampaignOptionsCheckBox("UseToughness",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseToughness.addMouseListener(createTipPanelUpdater("UseToughness"));
        chkUseRandomToughness = new CampaignOptionsCheckBox("UseRandomToughness",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseRandomToughness.addMouseListener(createTipPanelUpdater("UseRandomToughness"));
        chkUseArtillery = new CampaignOptionsCheckBox("UseArtillery");
        chkUseArtillery.addMouseListener(createTipPanelUpdater("UseArtillery"));
        chkUseAbilities = new CampaignOptionsCheckBox("UseAbilities");
        chkUseAbilities.addMouseListener(createTipPanelUpdater("UseAbilities"));
        chkOnlyCommandersMatterVehicles = new CampaignOptionsCheckBox("OnlyCommandersMatterVehicles",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkOnlyCommandersMatterVehicles.addMouseListener(createTipPanelUpdater("OnlyCommandersMatterVehicles"));
        chkOnlyCommandersMatterInfantry = new CampaignOptionsCheckBox("OnlyCommandersMatterInfantry",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkOnlyCommandersMatterInfantry.addMouseListener(createTipPanelUpdater("OnlyCommandersMatterInfantry"));
        chkOnlyCommandersMatterBattleArmor = new CampaignOptionsCheckBox("OnlyCommandersMatterBattleArmor",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkOnlyCommandersMatterBattleArmor.addMouseListener(createTipPanelUpdater("OnlyCommandersMatterBattleArmor"));
        chkUseEdge = new CampaignOptionsCheckBox("UseEdge");
        chkUseEdge.addMouseListener(createTipPanelUpdater("UseEdge"));
        chkUseTwistOfFateSurvival = new CampaignOptionsCheckBox("UseTwistOfFateSurvival");
        chkUseTwistOfFateSurvival.addMouseListener(createTipPanelUpdater("UseTwistOfFateSurvival"));
        chkUseFoundersHavePlotArmor = new CampaignOptionsCheckBox("UseFoundersHavePlotArmor",
              getMetadata(new Version(0, 51, 1)));
        chkUseFoundersHavePlotArmor.addMouseListener(createTipPanelUpdater("UseFoundersHavePlotArmor"));

        JLabel lblEdgeRefreshPeriod = new CampaignOptionsLabel("EdgeRefreshPeriod", getMetadata(new Version(0, 51, 0)));
        lblEdgeRefreshPeriod.addMouseListener(createTipPanelUpdater("EdgeRefreshPeriod"));
        comboEdgeRefreshPeriod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof EdgeRefreshPeriod) {
                    list.setToolTipText(wordWrap(((EdgeRefreshPeriod) value).getTooltip()));
                }
                return this;
            }
        });
        comboEdgeRefreshPeriod.addMouseListener(createTipPanelUpdater("EdgeRefreshPeriod"));

        JLabel lblEdgeRefreshCost = new CampaignOptionsLabel("EdgeRefreshCost", getMetadata(new Version(0, 51, 0)));
        lblEdgeRefreshCost.addMouseListener(createTipPanelUpdater("EdgeRefreshCost"));
        spnEdgeRefreshCost = new CampaignOptionsSpinner("EdgeRefreshCost", 20, 0, 100, 1);
        spnEdgeRefreshCost.addMouseListener(createTipPanelUpdater("EdgeRefreshCost"));
        chkUseImplants = new CampaignOptionsCheckBox("UseImplants");
        chkUseImplants.addMouseListener(createTipPanelUpdater("UseImplants"));
        chkUseAlternativeQualityAveraging = new CampaignOptionsCheckBox("UseAlternativeQualityAveraging",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseAlternativeQualityAveraging.addMouseListener(createTipPanelUpdater("UseAlternativeQualityAveraging"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PersonnelGeneralPage",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
              chkUseTactics,
              chkUseInitiativeBonus,
              chkUseSensibleTactics,
              chkUseToughness,
              chkUseRandomToughness,
              chkUseArtillery,
              chkUseAbilities,
              chkOnlyCommandersMatterVehicles,
              chkOnlyCommandersMatterInfantry,
              chkOnlyCommandersMatterBattleArmor,
              chkUseEdge,
              chkUseTwistOfFateSurvival,
              chkUseFoundersHavePlotArmor,
              chkUseImplants,
              chkUseAlternativeQualityAveraging);
        panel.addRow(lblEdgeRefreshCost, spnEdgeRefreshCost);
        panel.addRow(lblEdgeRefreshPeriod, comboEdgeRefreshPeriod);

        return panel;
    }

    /**
     * Creates the panel for personnel cleanup options in the General Page.
     *
     * @return a {@link JPanel} containing options for personnel cleanup, such as removal exemptions
     */
    private @Nonnull JPanel createPersonnelCleanUpPanel() {
        // Contents
        chkUsePersonnelRemoval = new CampaignOptionsCheckBox("UsePersonnelRemoval");
        chkUsePersonnelRemoval.addMouseListener(createTipPanelUpdater("UsePersonnelRemoval"));
        chkUseRemovalExemptCemetery = new CampaignOptionsCheckBox("UseRemovalExemptCemetery");
        chkUseRemovalExemptCemetery
              .addMouseListener(createTipPanelUpdater("UseRemovalExemptCemetery"));
        chkUseRemovalExemptRetirees = new CampaignOptionsCheckBox("UseRemovalExemptRetirees");
        chkUseRemovalExemptRetirees
              .addMouseListener(createTipPanelUpdater("UseRemovalExemptRetirees"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PersonnelCleanUpPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
              chkUsePersonnelRemoval,
              chkUseRemovalExemptCemetery,
              chkUseRemovalExemptRetirees);

        return panel;
    }

    /**
     * Creates the panel for administrative settings in the General Page.
     *
     * @return a {@link JPanel} containing settings related to administrators, such as negotiation options
     */
    private @Nonnull JPanel createAdministratorsPanel() {
        // Contents
        chkAdminsHaveNegotiation = new CampaignOptionsCheckBox("AdminsHaveNegotiation");
        chkAdminsHaveNegotiation.addMouseListener(createTipPanelUpdater("AdminsHaveNegotiation"));
        chkAdminExperienceLevelIncludeNegotiation = new CampaignOptionsCheckBox(
              "AdminExperienceLevelIncludeNegotiation");
        chkAdminExperienceLevelIncludeNegotiation.addMouseListener(createTipPanelUpdater(
              "AdminExperienceLevelIncludeNegotiation"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AdministratorsPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
              chkAdminsHaveNegotiation,
              chkAdminExperienceLevelIncludeNegotiation);

        return panel;
    }

    /**
     * Creates the panel for blob crew settings in the General Page.
     *
     * @return a {@link JPanel} containing settings related to blob crews (temporary personnel pools)
     */
    private @Nonnull JPanel createBlobCrewPanel() {
        // Contents
        chkUseBlobInfantry = new CampaignOptionsCheckBox("UseBlobInfantry", getMetadata(new Version(0, 50, 12)));
        chkUseBlobInfantry.addMouseListener(createTipPanelUpdater("UseBlobInfantry"));
        chkUseBlobBattleArmor = new CampaignOptionsCheckBox("UseBlobBattleArmor",
              getMetadata(new Version(0, 50, 12)));
        chkUseBlobBattleArmor.addMouseListener(createTipPanelUpdater("UseBlobBattleArmor"));
        chkUseBlobVehicleCrewGround = new CampaignOptionsCheckBox("UseBlobVehicleCrewGround",
              getMetadata(new Version(0, 50, 12)));
        chkUseBlobVehicleCrewGround
              .addMouseListener(createTipPanelUpdater("UseBlobVehicleCrewGround"));
        chkUseBlobVehicleCrewVTOL = new CampaignOptionsCheckBox("UseBlobVehicleCrewVTOL",
              getMetadata(new Version(0, 50, 12)));
        chkUseBlobVehicleCrewVTOL.addMouseListener(createTipPanelUpdater("UseBlobVehicleCrewVTOL"));
        chkUseBlobVehicleCrewNaval = new CampaignOptionsCheckBox("UseBlobVehicleCrewNaval",
              getMetadata(new Version(0, 50, 12)));
        chkUseBlobVehicleCrewNaval
              .addMouseListener(createTipPanelUpdater("UseBlobVehicleCrewNaval"));
        chkUseBlobVesselPilot = new CampaignOptionsCheckBox("UseBlobVesselPilot",
              getMetadata(new Version(0, 50, 12)));
        chkUseBlobVesselPilot.addMouseListener(createTipPanelUpdater("UseBlobVesselPilot"));
        chkUseBlobVesselGunner = new CampaignOptionsCheckBox("UseBlobVesselGunner",
              getMetadata(new Version(0, 50, 12)));
        chkUseBlobVesselGunner.addMouseListener(createTipPanelUpdater("UseBlobVesselGunner"));
        chkUseBlobVesselCrew = new CampaignOptionsCheckBox("UseBlobVesselCrew",
              getMetadata(new Version(0, 50, 12)));
        chkUseBlobVesselCrew.addMouseListener(createTipPanelUpdater("UseBlobVesselCrew"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("BlobCrewPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
              chkUseBlobInfantry,
              chkUseBlobBattleArmor,
              chkUseBlobVehicleCrewGround,
              chkUseBlobVehicleCrewVTOL,
              chkUseBlobVehicleCrewNaval,
              chkUseBlobVesselPilot,
              chkUseBlobVesselGunner,
              chkUseBlobVesselCrew);

        return panel;
    }

    /**
     * Copies general personnel values from the shared model into this page's controls. This is a no-op until the page
     * has been built.
     *
     * @param model the shared personnel options model to read values from
     */
    void readFromModel(@Nullable PersonnelOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseTactics.setSelected(model.useTactics);
        chkUseInitiativeBonus.setSelected(model.useInitiativeBonus);
        chkUseSensibleTactics.setSelected(model.useSensibleTactics);
        chkUseToughness.setSelected(model.useToughness);
        chkUseRandomToughness.setSelected(model.useRandomToughness);
        chkUseArtillery.setSelected(model.useArtillery);
        chkUseAbilities.setSelected(model.useAbilities);
        chkOnlyCommandersMatterVehicles.setSelected(model.onlyCommandersMatterVehicles);
        chkOnlyCommandersMatterInfantry.setSelected(model.onlyCommandersMatterInfantry);
        chkOnlyCommandersMatterBattleArmor.setSelected(model.onlyCommandersMatterBattleArmor);
        chkUseEdge.setSelected(model.useEdge);
        chkUseTwistOfFateSurvival.setSelected(model.useTwistOfFateSurvival);
        chkUseFoundersHavePlotArmor.setSelected(model.useFoundersHavePlotArmor);
        comboEdgeRefreshPeriod.setSelectedItem(model.edgeRefreshPeriod);
        spnEdgeRefreshCost.setValue(model.edgeRefreshCost);
        chkUseImplants.setSelected(model.useImplants);
        chkUseAlternativeQualityAveraging.setSelected(model.alternativeQualityAveraging);
        chkUsePersonnelRemoval.setSelected(model.usePersonnelRemoval);
        chkUseRemovalExemptCemetery.setSelected(model.useRemovalExemptCemetery);
        chkUseRemovalExemptRetirees.setSelected(model.useRemovalExemptRetirees);
        chkAdminsHaveNegotiation.setSelected(model.adminsHaveNegotiation);
        chkAdminExperienceLevelIncludeNegotiation.setSelected(model.adminExperienceLevelIncludeNegotiation);
        chkUseBlobInfantry.setSelected(model.useBlobInfantry);
        chkUseBlobBattleArmor.setSelected(model.useBlobBattleArmor);
        chkUseBlobVehicleCrewGround.setSelected(model.useBlobVehicleCrewGround);
        chkUseBlobVehicleCrewVTOL.setSelected(model.useBlobVehicleCrewVTOL);
        chkUseBlobVehicleCrewNaval.setSelected(model.useBlobVehicleCrewNaval);
        chkUseBlobVesselPilot.setSelected(model.useBlobVesselPilot);
        chkUseBlobVesselGunner.setSelected(model.useBlobVesselGunner);
        chkUseBlobVesselCrew.setSelected(model.useBlobVesselCrew);
    }

    /**
     * Copies general personnel values from this page's controls into the shared model. This is a no-op until the page
     * has been built.
     *
     * @param model the shared personnel options model to write values into
     */
    void writeToModel(@Nullable PersonnelOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useTactics = chkUseTactics.isSelected();
        model.useInitiativeBonus = chkUseInitiativeBonus.isSelected();
        model.useSensibleTactics = chkUseSensibleTactics.isSelected();
        model.useToughness = chkUseToughness.isSelected();
        model.useRandomToughness = chkUseRandomToughness.isSelected();
        model.useArtillery = chkUseArtillery.isSelected();
        model.useAbilities = chkUseAbilities.isSelected();
        model.onlyCommandersMatterVehicles = chkOnlyCommandersMatterVehicles.isSelected();
        model.onlyCommandersMatterInfantry = chkOnlyCommandersMatterInfantry.isSelected();
        model.onlyCommandersMatterBattleArmor = chkOnlyCommandersMatterBattleArmor.isSelected();
        model.useEdge = chkUseEdge.isSelected();
        model.useTwistOfFateSurvival = chkUseTwistOfFateSurvival.isSelected();
        model.useFoundersHavePlotArmor = chkUseFoundersHavePlotArmor.isSelected();
        model.edgeRefreshPeriod = comboEdgeRefreshPeriod.getSelectedItem();
        model.edgeRefreshCost = (int) spnEdgeRefreshCost.getValue();
        model.useImplants = chkUseImplants.isSelected();
        model.alternativeQualityAveraging = chkUseAlternativeQualityAveraging.isSelected();
        model.usePersonnelRemoval = chkUsePersonnelRemoval.isSelected();
        model.useRemovalExemptCemetery = chkUseRemovalExemptCemetery.isSelected();
        model.useRemovalExemptRetirees = chkUseRemovalExemptRetirees.isSelected();
        model.adminsHaveNegotiation = chkAdminsHaveNegotiation.isSelected();
        model.adminExperienceLevelIncludeNegotiation = chkAdminExperienceLevelIncludeNegotiation.isSelected();
        model.useBlobInfantry = chkUseBlobInfantry.isSelected();
        model.useBlobBattleArmor = chkUseBlobBattleArmor.isSelected();
        model.useBlobVehicleCrewGround = chkUseBlobVehicleCrewGround.isSelected();
        model.useBlobVehicleCrewVTOL = chkUseBlobVehicleCrewVTOL.isSelected();
        model.useBlobVehicleCrewNaval = chkUseBlobVehicleCrewNaval.isSelected();
        model.useBlobVesselPilot = chkUseBlobVesselPilot.isSelected();
        model.useBlobVesselGunner = chkUseBlobVesselGunner.isSelected();
        model.useBlobVesselCrew = chkUseBlobVesselCrew.isSelected();
    }
}
