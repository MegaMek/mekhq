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
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_ATTRIBUTE_SCORE;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
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
import megamek.client.ui.settings.SettingsFormPanel;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.enums.EdgeRefreshPeriod;
import mekhq.campaign.personnel.familiarity.Familiarity;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.Category;
import mekhq.campaign.personnel.quartermaster.RepairKitCatalog;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
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
    private static final int LABEL_COLUMN_WIDTH = SettingsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = SettingsFormPanel.DEFAULT_CONTROL_WIDTH;

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
    private JSpinner spnMaximumEdge;
    private MMComboBox<EdgeRefreshPeriod> comboEdgeRefreshPeriod;
    private JSpinner spnEdgeRefreshCost;
    private JCheckBox chkUseImplants;
    private JCheckBox chkUseAlternativeQualityAveraging;
    private JCheckBox chkAdminsHaveNegotiation;
    private JCheckBox chkAdminExperienceLevelIncludeNegotiation;

    private JCheckBox chkUsePersonnelRemoval;
    private JCheckBox chkUseRemovalExemptCemetery;
    private JCheckBox chkUseRemovalExemptRetirees;

    private JCheckBox chkUseSupportTeams;
    private JCheckBox chkUseBlobInfantry;
    private JCheckBox chkUseBlobBattleArmor;
    private JCheckBox chkUseBlobVehicleCrewGround;
    private JCheckBox chkUseBlobVehicleCrewVTOL;
    private JCheckBox chkUseBlobVehicleCrewNaval;
    private JCheckBox chkUseBlobVesselPilot;
    private JCheckBox chkUseBlobVesselGunner;
    private JCheckBox chkUseBlobVesselCrew;

    private JLabel lblChassisFamiliarityMode;
    private MMComboBox<Familiarity> comboChassisFamiliarityMode;
    private JLabel lblchassisFamiliaritySpeed;
    private JSpinner spnchassisFamiliaritySpeed;

    private JLabel lblMekWarriorDefaultKit;
    private MMComboBox<String> cboMekWarriorDefaultKit;
    private JLabel lblVehicleCrewDefaultKit;
    private MMComboBox<String> cboVehicleCrewDefaultKit;
    private JLabel lblAircraftDefaultKit;
    private MMComboBox<String> cboAircraftDefaultKit;
    private JCheckBox chkAddDefaultKitToProcurement;
    private JCheckBox chkNpcFactionArmorKits;
    private JCheckBox chkRequireMekWarriorKitToDeploy;

    private JLabel lblMekTechDefaultToolKit;
    private MMComboBox<String> cboMekTechDefaultToolKit;
    private JLabel lblMechanicDefaultToolKit;
    private MMComboBox<String> cboMechanicDefaultToolKit;
    private JLabel lblAeroTechDefaultToolKit;
    private MMComboBox<String> cboAeroTechDefaultToolKit;
    private JLabel lblBATechDefaultToolKit;
    private MMComboBox<String> cboBATechDefaultToolKit;
    private JLabel lblDoctorDefaultToolKit;
    private MMComboBox<String> cboDoctorDefaultToolKit;
    private JLabel lblAdminDefaultToolKit;
    private MMComboBox<String> cboAdminDefaultToolKit;
    private JCheckBox chkTechsNeedToolKit;

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
        cboMekWarriorDefaultKit = armorKitCombo("mekWarriorDefaultKit", Category.MEKWARRIOR);
        cboVehicleCrewDefaultKit = armorKitCombo("vehicleCrewDefaultKit", Category.INFANTRY);
        cboAircraftDefaultKit = armorKitCombo("aircraftDefaultKit", Category.AIRCRAFT);
        cboMekTechDefaultToolKit = toolKitCombo("mekTechDefaultToolKit");
        cboMechanicDefaultToolKit = toolKitCombo("mechanicDefaultToolKit");
        cboAeroTechDefaultToolKit = toolKitCombo("aeroTechDefaultToolKit");
        cboBATechDefaultToolKit = toolKitCombo("baTechDefaultToolKit");
        cboDoctorDefaultToolKit = toolKitCombo("doctorDefaultToolKit");
        cboAdminDefaultToolKit = toolKitCombo("adminDefaultToolKit");
        JPanel pnlPersonnelGeneralOptions = createGeneralOptionsPanel();
        JPanel pnlAdministrators = createAdministratorsPanel();
        JPanel pnlPersonnelCleanup = createPersonnelCleanUpPanel();
        JPanel pnlSupportTeams = createSupportTeamsPanel();
        JPanel pnlBlobCrew = createBlobCrewPanel();
        JPanel familiarityPanel = createChassisFamiliarityPanel();
        JPanel pnlArmorKits = createArmorKitsPanel();
        JPanel pnlToolKits = createToolKitsPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("PersonnelGeneralPage", "PersonnelGeneralPage", imageAddress)
                             .header(generalHeader)
                             .quote("personnelGeneralPage")
                             .section("lblPersonnelGeneralPage.text",
                                   "lblPersonnelGeneralPage.summary",
                                   pnlPersonnelGeneralOptions)
                             .section("lblAdministratorsPanel.text",
                                   "lblAdministratorsPanel.summary",
                                   pnlAdministrators)
                             .section("lblPersonnelCleanUpPanel.text",
                                   "lblPersonnelCleanUpPanel.summary",
                                   pnlPersonnelCleanup,
                                   getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                             .section("lblSupportTeamsPanel.text",
                                   "lblSupportTeamsPanel.summary",
                                   pnlSupportTeams,
                                   getMetadata(new Version(0, 51, 1), CampaignOptionFlag.CUSTOM_SYSTEM))
                             .section("lblBlobCrewPanel.text",
                                   "lblBlobCrewPanel.summary",
                                   pnlBlobCrew,
                                   getMetadata(new Version(0, 50, 12)))
                             .section("lblChassisFamiliarityPanel.text",
                                   "lblChassisFamiliarityPanel.summary",
                                   familiarityPanel,
                                   getMetadata(new Version(0, 51, 1),
                                         CampaignOptionFlag.CUSTOM_SYSTEM,
                                         CampaignOptionFlag.DOCUMENTED))
                             .section("lblArmorKitsPanel.text",
                                   "lblArmorKitsPanel.summary",
                                   pnlArmorKits)
                             .section("lblToolKitsPanel.text",
                                   "lblToolKitsPanel.summary",
                                   pnlToolKits)
                             .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    private JPanel createChassisFamiliarityPanel() {
        lblChassisFamiliarityMode = new CampaignOptionsLabel("ChassisFamiliarityMode",
              getMetadata(new Version(0, 51, 1), CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.DOCUMENTED));
        lblChassisFamiliarityMode.addMouseListener(createTipPanelUpdater("ChassisFamiliarityMode"));
        comboChassisFamiliarityMode = new MMComboBox<>("comboChassisFamiliarityMode", Familiarity.values());
        comboChassisFamiliarityMode.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Familiarity) {
                    list.setToolTipText(wordWrap(((Familiarity) value).getTooltip()));
                }
                return this;
            }
        });
        comboChassisFamiliarityMode.addMouseListener(createTipPanelUpdater("ChassisFamiliarityMode"));

        lblchassisFamiliaritySpeed = new CampaignOptionsLabel("chassisFamiliaritySpeed",
              getMetadata(new Version(0, 51, 1), CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.RECOMMENDED));
        lblchassisFamiliaritySpeed.addMouseListener(createTipPanelUpdater("chassisFamiliaritySpeed"));
        spnchassisFamiliaritySpeed = new CampaignOptionsSpinner("chassisFamiliaritySpeed",
              1, 0, 20, 1);
        spnchassisFamiliaritySpeed.addMouseListener(createTipPanelUpdater("chassisFamiliaritySpeed"));

        final SettingsFormPanel panel = new SettingsFormPanel("ChassisFamiliarityPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addRow(lblChassisFamiliarityMode, comboChassisFamiliarityMode);
        panel.addRow(lblchassisFamiliaritySpeed, spnchassisFamiliaritySpeed);

        return panel;
    }

    private MMComboBox<String> armorKitCombo(String name, Category category) {
        MMComboBox<String> combo = new MMComboBox<>(name,
              ArmorKitCatalog.optionKitNames(category).toArray(new String[0]));
        combo.setRenderer(new ArmorKitRenderer());
        return combo;
    }

    /** Renders the coveralls entry as "None" and every other kit by its own name, with a per-kit explanatory tooltip. */
    private static class ArmorKitRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            boolean isNone = ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME.equals(value);
            Object display = isNone ? getTextAt(getCampaignOptionsResourceBundle(), "armorKitNone.text") : value;
            super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
            setToolTipText(armorKitMechanics((String) value));
            return this;
        }
    }

    private MMComboBox<String> toolKitCombo(String name) {
        MMComboBox<String> combo = new MMComboBox<>(name, RepairKitCatalog.optionKitNames().toArray(new String[0]));
        combo.setRenderer(new ToolKitRenderer());
        return combo;
    }

    /** Renders the "none" sentinel as "None" and every other kit by its own name, with a per-kit explanatory tooltip. */
    private static class ToolKitRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            boolean isNone = RepairKitCatalog.NO_DEFAULT_KIT.equals(value);
            Object display = isNone ? getTextAt(getCampaignOptionsResourceBundle(), "armorKitNone.text") : value;
            super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
            setToolTipText(kitTooltip(isNone ? "NoDefaultKit" : (String) value));
            return this;
        }
    }

    /**
     * The explanatory tooltip for a tool-kit dropdown entry, describing what the kit does so the player can choose.
     * Looked up in the campaign-options bundle under {@code kitTooltip.<sanitized name>} (the kit's own name with all
     * non-alphanumeric characters stripped). Returns {@code null} when no description is authored, leaving the entry
     * with no tooltip rather than a visible missing-resource marker.
     *
     * @param kitName the kit's internal name, or the pseudo-name {@code "NoDefaultKit"} for the tool-kit "none" entry
     *
     * @return the tooltip text, or {@code null} if none is authored
     */
    private static String kitTooltip(String kitName) {
        if (kitName == null) {
            return null;
        }
        String text = getTextAt(getCampaignOptionsResourceBundle(),
              "kitTooltip." + kitName.replaceAll("[^A-Za-z0-9]", ""));
        return isResourceKeyValid(text) ? text : null;
    }

    /**
     * A tooltip stating an armor kit's game mechanics only - its damage divisor, the environmental conditions it seals
     * against, and whether it is encumbering - read straight from the kit's own data so the text always matches what
     * the kit actually does. Coveralls (and any kit with no protective effect) read as "no protection".
     *
     * @param kitName the armor kit's internal name
     *
     * @return the mechanics tooltip, or {@code null} if the kit cannot be resolved to build one
     */
    private static String armorKitMechanics(String kitName) {
        String bundle = getCampaignOptionsResourceBundle();
        EquipmentType kit = EquipmentType.get(kitName);
        if (kit == null) {
            // The coveralls default may not resolve; it grants no protection, which is itself the mechanic to state.
            return ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME.equals(kitName)
                         ? getTextAt(bundle, "armorKitMechanics.none")
                         : null;
        }

        List<String> parts = new ArrayList<>();
        double divisor = (kit instanceof MiscType misc) ? misc.getDamageDivisor() : 1.0;
        parts.add(getFormattedTextAt(bundle, "armorKitMechanics.divisor", formatDivisor(divisor)));
        List<String> protections = armorKitProtections(kit, bundle);
        if (!protections.isEmpty()) {
            parts.add(getFormattedTextAt(bundle, "armorKitMechanics.protects", String.join(", ", protections)));
        }
        if (kit.hasFlag(MiscTypeFlag.S_ENCUMBERING)) {
            parts.add(getTextAt(bundle, "armorKitMechanics.encumbering"));
        }
        return parts.isEmpty() ? getTextAt(bundle, "armorKitMechanics.none") : String.join("  ·  ", parts);
    }

    /** The environmental conditions an armor kit seals against, by display name (mirrors the issue dialog's badges). */
    private static List<String> armorKitProtections(EquipmentType kit, String bundle) {
        List<String> protections = new ArrayList<>();
        boolean combatSuit = kit.hasFlag(MiscTypeFlag.S_COMBAT_SUIT);
        if (kit.hasFlag(MiscTypeFlag.S_SPACE_SUIT) || kit.hasFlag(MiscTypeFlag.S_XCT_VACUUM)) {
            protections.add(getTextAt(bundle, "armorKitEnv.vacuum"));
        }
        if (kit.hasFlag(MiscTypeFlag.S_COLD_WEATHER)) {
            protections.add(getTextAt(bundle, "armorKitEnv.cold"));
        }
        if (kit.hasFlag(MiscTypeFlag.S_HOT_WEATHER) || combatSuit) {
            protections.add(getTextAt(bundle, "armorKitEnv.hot"));
        }
        if (kit.hasFlag(MiscTypeFlag.S_TAINTED_ATMOSPHERE) || combatSuit) {
            protections.add(getTextAt(bundle, "armorKitEnv.tainted"));
        }
        if (kit.hasFlag(MiscTypeFlag.S_TOXIC_ATMOSPHERE) || combatSuit) {
            protections.add(getTextAt(bundle, "armorKitEnv.toxic"));
        }
        return protections;
    }

    /** Renders a damage divisor without a trailing ".0" for whole numbers. */
    private static String formatDivisor(double divisor) {
        return (divisor == Math.rint(divisor)) ? String.valueOf((int) divisor) : String.valueOf(divisor);
    }

    private @Nonnull JPanel createArmorKitsPanel() {
        lblMekWarriorDefaultKit = new CampaignOptionsLabel("MekWarriorDefaultKit", getMetadata(new Version(0, 51, 1)));
        lblMekWarriorDefaultKit.addMouseListener(createTipPanelUpdater("MekWarriorDefaultKit"));
        cboMekWarriorDefaultKit.addMouseListener(createTipPanelUpdater("MekWarriorDefaultKit"));

        lblVehicleCrewDefaultKit = new CampaignOptionsLabel("VehicleCrewDefaultKit",
              getMetadata(new Version(0, 51, 1)));
        lblVehicleCrewDefaultKit.addMouseListener(createTipPanelUpdater("VehicleCrewDefaultKit"));
        cboVehicleCrewDefaultKit.addMouseListener(createTipPanelUpdater("VehicleCrewDefaultKit"));

        lblAircraftDefaultKit = new CampaignOptionsLabel("AircraftDefaultKit", getMetadata(new Version(0, 51, 1)));
        lblAircraftDefaultKit.addMouseListener(createTipPanelUpdater("AircraftDefaultKit"));
        cboAircraftDefaultKit.addMouseListener(createTipPanelUpdater("AircraftDefaultKit"));

        chkAddDefaultKitToProcurement = new CampaignOptionsCheckBox("AddDefaultKitToProcurement",
              getMetadata(new Version(0, 51, 1)));
        chkAddDefaultKitToProcurement.addMouseListener(createTipPanelUpdater("AddDefaultKitToProcurement"));

        chkNpcFactionArmorKits = new CampaignOptionsCheckBox("NpcFactionArmorKits", getMetadata(new Version(0, 51, 1)));
        chkNpcFactionArmorKits.addMouseListener(createTipPanelUpdater("NpcFactionArmorKits"));

        chkRequireMekWarriorKitToDeploy = new CampaignOptionsCheckBox("RequireMekWarriorKitToDeploy",
              getMetadata(new Version(0, 51, 1)));
        chkRequireMekWarriorKitToDeploy.addMouseListener(createTipPanelUpdater("RequireMekWarriorKitToDeploy"));

        final SettingsFormPanel panel = new SettingsFormPanel("ArmorKitsPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addRow(lblMekWarriorDefaultKit, cboMekWarriorDefaultKit);
        panel.addRow(lblVehicleCrewDefaultKit, cboVehicleCrewDefaultKit);
        panel.addRow(lblAircraftDefaultKit, cboAircraftDefaultKit);
        panel.addCheckBox(chkAddDefaultKitToProcurement);
        panel.addCheckBox(chkNpcFactionArmorKits);
        panel.addCheckBox(chkRequireMekWarriorKitToDeploy);

        return panel;
    }

    private @Nonnull JPanel createToolKitsPanel() {
        lblMekTechDefaultToolKit = new CampaignOptionsLabel("MekTechDefaultToolKit",
              getMetadata(new Version(0, 51, 1)));
        lblMekTechDefaultToolKit.addMouseListener(createTipPanelUpdater("MekTechDefaultToolKit"));
        cboMekTechDefaultToolKit.addMouseListener(createTipPanelUpdater("MekTechDefaultToolKit"));

        lblMechanicDefaultToolKit = new CampaignOptionsLabel("MechanicDefaultToolKit",
              getMetadata(new Version(0, 51, 1)));
        lblMechanicDefaultToolKit.addMouseListener(createTipPanelUpdater("MechanicDefaultToolKit"));
        cboMechanicDefaultToolKit.addMouseListener(createTipPanelUpdater("MechanicDefaultToolKit"));

        lblAeroTechDefaultToolKit = new CampaignOptionsLabel("AeroTechDefaultToolKit",
              getMetadata(new Version(0, 51, 1)));
        lblAeroTechDefaultToolKit.addMouseListener(createTipPanelUpdater("AeroTechDefaultToolKit"));
        cboAeroTechDefaultToolKit.addMouseListener(createTipPanelUpdater("AeroTechDefaultToolKit"));

        lblBATechDefaultToolKit = new CampaignOptionsLabel("BATechDefaultToolKit", getMetadata(new Version(0, 51, 1)));
        lblBATechDefaultToolKit.addMouseListener(createTipPanelUpdater("BATechDefaultToolKit"));
        cboBATechDefaultToolKit.addMouseListener(createTipPanelUpdater("BATechDefaultToolKit"));

        lblDoctorDefaultToolKit = new CampaignOptionsLabel("DoctorDefaultToolKit", getMetadata(new Version(0, 51, 1)));
        lblDoctorDefaultToolKit.addMouseListener(createTipPanelUpdater("DoctorDefaultToolKit"));
        cboDoctorDefaultToolKit.addMouseListener(createTipPanelUpdater("DoctorDefaultToolKit"));

        lblAdminDefaultToolKit = new CampaignOptionsLabel("AdminDefaultToolKit", getMetadata(new Version(0, 51, 1)));
        lblAdminDefaultToolKit.addMouseListener(createTipPanelUpdater("AdminDefaultToolKit"));
        cboAdminDefaultToolKit.addMouseListener(createTipPanelUpdater("AdminDefaultToolKit"));

        chkTechsNeedToolKit = new CampaignOptionsCheckBox("TechsNeedToolKit", getMetadata(new Version(0, 51, 1)));
        chkTechsNeedToolKit.addMouseListener(createTipPanelUpdater("TechsNeedToolKit"));

        final SettingsFormPanel panel = new SettingsFormPanel("ToolKitsPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addRow(lblMekTechDefaultToolKit, cboMekTechDefaultToolKit);
        panel.addRow(lblMechanicDefaultToolKit, cboMechanicDefaultToolKit);
        panel.addRow(lblAeroTechDefaultToolKit, cboAeroTechDefaultToolKit);
        panel.addRow(lblBATechDefaultToolKit, cboBATechDefaultToolKit);
        panel.addRow(lblDoctorDefaultToolKit, cboDoctorDefaultToolKit);
        panel.addRow(lblAdminDefaultToolKit, cboAdminDefaultToolKit);
        panel.addCheckBox(chkTechsNeedToolKit);

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
        chkUseTwistOfFateSurvival = new CampaignOptionsCheckBox("UseTwistOfFateSurvival",
              getMetadata(new Version(0, 51, 1)));
        chkUseTwistOfFateSurvival.addMouseListener(createTipPanelUpdater("UseTwistOfFateSurvival"));
        chkUseFoundersHavePlotArmor = new CampaignOptionsCheckBox("UseFoundersHavePlotArmor",
              getMetadata(new Version(0, 51, 1)));
        chkUseFoundersHavePlotArmor.addMouseListener(createTipPanelUpdater("UseFoundersHavePlotArmor"));

        JLabel lblMaximumEdge = new CampaignOptionsLabel("MaximumEdge", getMetadata(new Version(0, 51, 1)));
        lblMaximumEdge.addMouseListener(createTipPanelUpdater("MaximumEdge"));
        spnMaximumEdge = new CampaignOptionsSpinner("MaximumEdge",
              CampaignOption.MAXIMUM_EDGE.defaultValue(), 0, MAXIMUM_ATTRIBUTE_SCORE, 1);
        spnMaximumEdge.addMouseListener(createTipPanelUpdater("MaximumEdge"));

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
        final SettingsFormPanel panel = new SettingsFormPanel("PersonnelGeneralPage",
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
        panel.addRow(lblMaximumEdge, spnMaximumEdge);
        panel.addRow(lblEdgeRefreshCost, spnEdgeRefreshCost);
        panel.addRow(lblEdgeRefreshPeriod, comboEdgeRefreshPeriod);

        return panel;
    }

    /**
     * Creates the panel for administrator skill options in the General Page.
     *
     * @return a {@link JPanel} containing options controlling the Negotiation skill for administrators
     */
    private @Nonnull JPanel createAdministratorsPanel() {
        // Contents
        chkAdminsHaveNegotiation = new CampaignOptionsCheckBox("AdminsHaveNegotiation");
        chkAdminsHaveNegotiation.addMouseListener(createTipPanelUpdater("AdminsHaveNegotiation"));
        chkAdminExperienceLevelIncludeNegotiation =
              new CampaignOptionsCheckBox("AdminExperienceLevelIncludeNegotiation");
        chkAdminExperienceLevelIncludeNegotiation
              .addMouseListener(createTipPanelUpdater("AdminExperienceLevelIncludeNegotiation"));

        // Layout the Panel
        final SettingsFormPanel panel = new SettingsFormPanel("AdministratorsPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
              chkAdminsHaveNegotiation,
              chkAdminExperienceLevelIncludeNegotiation);

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
        final SettingsFormPanel panel = new SettingsFormPanel("PersonnelCleanUpPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
              chkUsePersonnelRemoval,
              chkUseRemovalExemptCemetery,
              chkUseRemovalExemptRetirees);

        return panel;
    }

    /**
     * Creates the panel for blob crew settings in the General Page.
     *
     * @return a {@link JPanel} containing settings related to blob crews (temporary personnel pools)
     */
    /**
     * Builds the Support Teams section: whether support staff are organized into support carriers in the TOE.
     */
    private @Nonnull JPanel createSupportTeamsPanel() {
        chkUseSupportTeams = new CampaignOptionsCheckBox("UseSupportTeams",
              getMetadata(new Version(0, 51, 1), CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseSupportTeams.addMouseListener(createTipPanelUpdater("UseSupportTeams"));

        final SettingsFormPanel panel = new SettingsFormPanel("SupportTeamsPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(1, chkUseSupportTeams);

        return panel;
    }

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
        final SettingsFormPanel panel = new SettingsFormPanel("BlobCrewPanel",
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
        spnMaximumEdge.setValue(model.maximumEdge);
        chkUseTwistOfFateSurvival.setSelected(model.useTwistOfFateSurvival);
        chkUseFoundersHavePlotArmor.setSelected(model.useFoundersHavePlotArmor);
        comboEdgeRefreshPeriod.setSelectedItem(model.edgeRefreshPeriod);
        spnEdgeRefreshCost.setValue(model.edgeRefreshCost);
        chkUseImplants.setSelected(model.useImplants);
        chkUseAlternativeQualityAveraging.setSelected(model.alternativeQualityAveraging);
        chkAdminsHaveNegotiation.setSelected(model.adminsHaveNegotiation);
        chkAdminExperienceLevelIncludeNegotiation.setSelected(model.adminExperienceLevelIncludeNegotiation);
        chkUsePersonnelRemoval.setSelected(model.usePersonnelRemoval);
        chkUseRemovalExemptCemetery.setSelected(model.useRemovalExemptCemetery);
        chkUseRemovalExemptRetirees.setSelected(model.useRemovalExemptRetirees);
        chkUseSupportTeams.setSelected(model.useSupportTeams);
        chkUseBlobInfantry.setSelected(model.useBlobInfantry);
        chkUseBlobBattleArmor.setSelected(model.useBlobBattleArmor);
        chkUseBlobVehicleCrewGround.setSelected(model.useBlobVehicleCrewGround);
        chkUseBlobVehicleCrewVTOL.setSelected(model.useBlobVehicleCrewVTOL);
        chkUseBlobVehicleCrewNaval.setSelected(model.useBlobVehicleCrewNaval);
        chkUseBlobVesselPilot.setSelected(model.useBlobVesselPilot);
        chkUseBlobVesselGunner.setSelected(model.useBlobVesselGunner);
        chkUseBlobVesselCrew.setSelected(model.useBlobVesselCrew);
        comboChassisFamiliarityMode.setSelectedItem(model.chassisFamiliarity);
        spnchassisFamiliaritySpeed.setValue(model.chassisFamiliaritySpeed);
        cboMekWarriorDefaultKit.setSelectedItem(model.mekWarriorDefaultKit);
        cboVehicleCrewDefaultKit.setSelectedItem(model.vehicleCrewDefaultKit);
        cboAircraftDefaultKit.setSelectedItem(model.aircraftDefaultKit);
        chkAddDefaultKitToProcurement.setSelected(model.addDefaultKitToProcurement);
        chkNpcFactionArmorKits.setSelected(model.npcFactionArmorKits);
        chkRequireMekWarriorKitToDeploy.setSelected(model.requireMekWarriorKitToDeploy);
        cboMekTechDefaultToolKit.setSelectedItem(model.mekTechDefaultToolKit);
        cboMechanicDefaultToolKit.setSelectedItem(model.mechanicDefaultToolKit);
        cboAeroTechDefaultToolKit.setSelectedItem(model.aeroTechDefaultToolKit);
        cboBATechDefaultToolKit.setSelectedItem(model.baTechDefaultToolKit);
        cboDoctorDefaultToolKit.setSelectedItem(model.doctorDefaultToolKit);
        cboAdminDefaultToolKit.setSelectedItem(model.adminDefaultToolKit);
        chkTechsNeedToolKit.setSelected(model.techsNeedToolKit);
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
        model.maximumEdge = (int) spnMaximumEdge.getValue();
        model.useTwistOfFateSurvival = chkUseTwistOfFateSurvival.isSelected();
        model.useFoundersHavePlotArmor = chkUseFoundersHavePlotArmor.isSelected();
        model.edgeRefreshPeriod = comboEdgeRefreshPeriod.getSelectedItem();
        model.edgeRefreshCost = (int) spnEdgeRefreshCost.getValue();
        model.useImplants = chkUseImplants.isSelected();
        model.alternativeQualityAveraging = chkUseAlternativeQualityAveraging.isSelected();
        model.adminsHaveNegotiation = chkAdminsHaveNegotiation.isSelected();
        model.adminExperienceLevelIncludeNegotiation = chkAdminExperienceLevelIncludeNegotiation.isSelected();
        model.usePersonnelRemoval = chkUsePersonnelRemoval.isSelected();
        model.useRemovalExemptCemetery = chkUseRemovalExemptCemetery.isSelected();
        model.useRemovalExemptRetirees = chkUseRemovalExemptRetirees.isSelected();
        model.useSupportTeams = chkUseSupportTeams.isSelected();
        model.useBlobInfantry = chkUseBlobInfantry.isSelected();
        model.useBlobBattleArmor = chkUseBlobBattleArmor.isSelected();
        model.useBlobVehicleCrewGround = chkUseBlobVehicleCrewGround.isSelected();
        model.useBlobVehicleCrewVTOL = chkUseBlobVehicleCrewVTOL.isSelected();
        model.useBlobVehicleCrewNaval = chkUseBlobVehicleCrewNaval.isSelected();
        model.useBlobVesselPilot = chkUseBlobVesselPilot.isSelected();
        model.useBlobVesselGunner = chkUseBlobVesselGunner.isSelected();
        model.useBlobVesselCrew = chkUseBlobVesselCrew.isSelected();
        model.chassisFamiliarity = comboChassisFamiliarityMode.getSelectedItem();
        model.chassisFamiliaritySpeed = (int) spnchassisFamiliaritySpeed.getValue();
        model.mekWarriorDefaultKit = cboMekWarriorDefaultKit.getSelectedItem();
        model.vehicleCrewDefaultKit = cboVehicleCrewDefaultKit.getSelectedItem();
        model.aircraftDefaultKit = cboAircraftDefaultKit.getSelectedItem();
        model.addDefaultKitToProcurement = chkAddDefaultKitToProcurement.isSelected();
        model.npcFactionArmorKits = chkNpcFactionArmorKits.isSelected();
        model.requireMekWarriorKitToDeploy = chkRequireMekWarriorKitToDeploy.isSelected();
        model.mekTechDefaultToolKit = cboMekTechDefaultToolKit.getSelectedItem();
        model.mechanicDefaultToolKit = cboMechanicDefaultToolKit.getSelectedItem();
        model.aeroTechDefaultToolKit = cboAeroTechDefaultToolKit.getSelectedItem();
        model.baTechDefaultToolKit = cboBATechDefaultToolKit.getSelectedItem();
        model.doctorDefaultToolKit = cboDoctorDefaultToolKit.getSelectedItem();
        model.adminDefaultToolKit = cboAdminDefaultToolKit.getSelectedItem();
        model.techsNeedToolKit = chkTechsNeedToolKit.isSelected();
    }
}
