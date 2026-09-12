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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.settings.SettingsFormPanel;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.Category;
import mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;

/**
 * The {@code PersonnelEquipmentPage} class builds the Equipment leaf page of the Personnel section of the Campaign
 * Options dialog: the quartermaster's default-kit settings. It carries a small General section for the option shared by
 * every equipment kind (whether a recruit's default kit is added to procurement), and two per-kind sections - Armor
 * Kits and Tool Kits - each with its per-role default and its own toggles. Values synchronise with the shared
 * {@link PersonnelOptionsModel}, the same model the other Personnel leaf pages use.
 */
class PersonnelEquipmentPage {
    private static final int LABEL_COLUMN_WIDTH = SettingsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = SettingsFormPanel.DEFAULT_CONTROL_WIDTH;

    private CampaignOptionsHeaderPanel equipmentHeader;

    private JCheckBox chkAddDefaultKitToProcurement;

    private JLabel lblMekWarriorDefaultKit;
    private MMComboBox<String> cboMekWarriorDefaultKit;
    private JLabel lblVehicleCrewDefaultKit;
    private MMComboBox<String> cboVehicleCrewDefaultKit;
    private JLabel lblAircraftDefaultKit;
    private MMComboBox<String> cboAircraftDefaultKit;
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
    private JLabel lblVesselCrewDefaultToolKit;
    private MMComboBox<String> cboVesselCrewDefaultToolKit;
    private JLabel lblAstechDefaultToolKit;
    private MMComboBox<String> cboAstechDefaultToolKit;
    private JLabel lblDoctorDefaultToolKit;
    private MMComboBox<String> cboDoctorDefaultToolKit;
    private JLabel lblMedicDefaultToolKit;
    private MMComboBox<String> cboMedicDefaultToolKit;
    private JLabel lblAdminDefaultToolKit;
    private MMComboBox<String> cboAdminDefaultToolKit;
    private JCheckBox chkTechsNeedToolKit;

    private boolean created;

    /**
     * Builds the Equipment page, populates its controls from the supplied model, and returns the assembled panel.
     *
     * @param model the shared personnel options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Equipment page
     */
    @Nonnull
    JPanel createPanel(@Nullable PersonnelOptionsModel model) {
        String imageAddress = getImageDirectory() + "logo_clan_wolverine.png";
        equipmentHeader = new CampaignOptionsHeaderPanel("PersonnelEquipmentPage", imageAddress);

        cboMekWarriorDefaultKit = armorKitCombo("mekWarriorDefaultKit", Category.MEKWARRIOR);
        cboVehicleCrewDefaultKit = armorKitCombo("vehicleCrewDefaultKit", Category.INFANTRY);
        cboAircraftDefaultKit = armorKitCombo("aircraftDefaultKit", Category.AIRCRAFT);
        cboMekTechDefaultToolKit = toolKitCombo("mekTechDefaultToolKit");
        cboMechanicDefaultToolKit = toolKitCombo("mechanicDefaultToolKit");
        cboAeroTechDefaultToolKit = toolKitCombo("aeroTechDefaultToolKit");
        cboBATechDefaultToolKit = toolKitCombo("baTechDefaultToolKit");
        cboVesselCrewDefaultToolKit = toolKitCombo("VesselCrewDefaultToolKit");
        cboAstechDefaultToolKit = toolKitCombo("astechDefaultToolKit");
        cboDoctorDefaultToolKit = toolKitCombo("doctorDefaultToolKit");
        cboMedicDefaultToolKit = toolKitCombo("medicDefaultToolKit");
        cboAdminDefaultToolKit = toolKitCombo("adminDefaultToolKit");

        JPanel pnlGeneral = createGeneralPanel();
        JPanel pnlArmorKits = createArmorKitsPanel();
        JPanel pnlToolKits = createToolKitsPanel();

        JPanel panel = CampaignOptionsPagePanel.builder("PersonnelEquipmentPage",
                    "PersonnelEquipmentPage",
                    imageAddress)
                             .header(equipmentHeader)
                             .quote("personnelEquipmentPage")
                             .section("lblEquipmentGeneralPanel.text",
                                   "lblEquipmentGeneralPanel.summary",
                                   pnlGeneral)
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

    /** The General section: options that apply to every equipment kind (armor and tool kits alike). */
    private @Nonnull JPanel createGeneralPanel() {
        chkAddDefaultKitToProcurement = new CampaignOptionsCheckBox("AddDefaultKitToProcurement",
              getMetadata(new Version(0, 51, 1)));
        chkAddDefaultKitToProcurement.addMouseListener(createTipPanelUpdater("AddDefaultKitToProcurement"));

        final SettingsFormPanel panel = new SettingsFormPanel("EquipmentGeneralPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkAddDefaultKitToProcurement);

        return panel;
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

        lblVesselCrewDefaultToolKit = new CampaignOptionsLabel("VesselCrewDefaultToolKit", getMetadata(new Version(0,
              51, 1)));
        lblVesselCrewDefaultToolKit.addMouseListener(createTipPanelUpdater("VesselCrewDefaultToolKit"));
        cboVesselCrewDefaultToolKit.addMouseListener(createTipPanelUpdater("VesselCrewDefaultToolKit"));

        lblAstechDefaultToolKit = new CampaignOptionsLabel("AstechDefaultToolKit", getMetadata(new Version(0, 51, 1)));
        lblAstechDefaultToolKit.addMouseListener(createTipPanelUpdater("AstechDefaultToolKit"));
        cboAstechDefaultToolKit.addMouseListener(createTipPanelUpdater("AstechDefaultToolKit"));

        lblDoctorDefaultToolKit = new CampaignOptionsLabel("DoctorDefaultToolKit", getMetadata(new Version(0, 51, 1)));
        lblDoctorDefaultToolKit.addMouseListener(createTipPanelUpdater("DoctorDefaultToolKit"));
        cboDoctorDefaultToolKit.addMouseListener(createTipPanelUpdater("DoctorDefaultToolKit"));

        lblMedicDefaultToolKit = new CampaignOptionsLabel("MedicDefaultToolKit", getMetadata(new Version(0, 51, 1)));
        lblMedicDefaultToolKit.addMouseListener(createTipPanelUpdater("MedicDefaultToolKit"));
        cboMedicDefaultToolKit.addMouseListener(createTipPanelUpdater("MedicDefaultToolKit"));

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
        panel.addRow(lblVesselCrewDefaultToolKit, cboVesselCrewDefaultToolKit);
        panel.addRow(lblAstechDefaultToolKit, cboAstechDefaultToolKit);
        panel.addRow(lblDoctorDefaultToolKit, cboDoctorDefaultToolKit);
        panel.addRow(lblMedicDefaultToolKit, cboMedicDefaultToolKit);
        panel.addRow(lblAdminDefaultToolKit, cboAdminDefaultToolKit);
        panel.addCheckBox(chkTechsNeedToolKit);

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
        MMComboBox<String> combo = new MMComboBox<>(name, EquipmentKitCatalog.optionKitNames().toArray(new String[0]));
        combo.setRenderer(new ToolKitRenderer());
        return combo;
    }

    /** Renders the "none" sentinel as "None" and every other kit by its own name, with a per-kit explanatory tooltip. */
    private static class ToolKitRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            boolean isNone = EquipmentKitCatalog.NO_DEFAULT_KIT.equals(value);
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
        String text = getTextAt("mekhq.resources.IssueEquipmentDialog",
              "tools.effect." + kitName.replaceAll("[^A-Za-z0-9]", ""));
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

    /**
     * Copies equipment values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared personnel options model to read values from
     */
    void readFromModel(@Nullable PersonnelOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkAddDefaultKitToProcurement.setSelected(model.addDefaultKitToProcurement);
        cboMekWarriorDefaultKit.setSelectedItem(model.mekWarriorDefaultKit);
        cboVehicleCrewDefaultKit.setSelectedItem(model.vehicleCrewDefaultKit);
        cboAircraftDefaultKit.setSelectedItem(model.aircraftDefaultKit);
        chkNpcFactionArmorKits.setSelected(model.npcFactionArmorKits);
        chkRequireMekWarriorKitToDeploy.setSelected(model.requireMekWarriorKitToDeploy);
        cboMekTechDefaultToolKit.setSelectedItem(model.mekTechDefaultToolKit);
        cboMechanicDefaultToolKit.setSelectedItem(model.mechanicDefaultToolKit);
        cboAeroTechDefaultToolKit.setSelectedItem(model.aeroTechDefaultToolKit);
        cboBATechDefaultToolKit.setSelectedItem(model.baTechDefaultToolKit);
        cboVesselCrewDefaultToolKit.setSelectedItem(model.vesselCrewDefaultToolKit);
        cboAstechDefaultToolKit.setSelectedItem(model.astechDefaultToolKit);
        cboDoctorDefaultToolKit.setSelectedItem(model.doctorDefaultToolKit);
        cboMedicDefaultToolKit.setSelectedItem(model.medicDefaultToolKit);
        cboAdminDefaultToolKit.setSelectedItem(model.adminDefaultToolKit);
        chkTechsNeedToolKit.setSelected(model.techsNeedToolKit);
    }

    /**
     * Copies equipment values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared personnel options model to write values into
     */
    void writeToModel(@Nullable PersonnelOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.addDefaultKitToProcurement = chkAddDefaultKitToProcurement.isSelected();
        model.mekWarriorDefaultKit = cboMekWarriorDefaultKit.getSelectedItem();
        model.vehicleCrewDefaultKit = cboVehicleCrewDefaultKit.getSelectedItem();
        model.aircraftDefaultKit = cboAircraftDefaultKit.getSelectedItem();
        model.npcFactionArmorKits = chkNpcFactionArmorKits.isSelected();
        model.requireMekWarriorKitToDeploy = chkRequireMekWarriorKitToDeploy.isSelected();
        model.mekTechDefaultToolKit = cboMekTechDefaultToolKit.getSelectedItem();
        model.mechanicDefaultToolKit = cboMechanicDefaultToolKit.getSelectedItem();
        model.vesselCrewDefaultToolKit = cboVesselCrewDefaultToolKit.getSelectedItem();
        model.baTechDefaultToolKit = cboBATechDefaultToolKit.getSelectedItem();
        model.astechDefaultToolKit = cboAstechDefaultToolKit.getSelectedItem();
        model.doctorDefaultToolKit = cboDoctorDefaultToolKit.getSelectedItem();
        model.medicDefaultToolKit = cboMedicDefaultToolKit.getSelectedItem();
        model.adminDefaultToolKit = cboAdminDefaultToolKit.getSelectedItem();
        model.techsNeedToolKit = chkTechsNeedToolKit.isSelected();
    }
}
