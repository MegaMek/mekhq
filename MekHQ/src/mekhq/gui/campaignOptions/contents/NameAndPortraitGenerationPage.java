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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.setSmallSizeVariant;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelRoleSubType;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;

/**
 * The {@code NameAndPortraitGenerationPage} class builds and manages the Name and Portrait Generation leaf page of the
 * Biography section of the Campaign Options dialog. It owns the widgets for name generation (origin-faction names and
 * the faction-name picker) and portrait assignment rules (per-role portrait toggles and the enable/disable-all
 * buttons), and synchronises them with a shared {@link BiographyOptionsModel}.
 *
 * <p>This view is a sub-component of {@link BiographyPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code BiographyPages}, while this class is responsible only for constructing the Name and Portrait
 * Generation panel and copying its values to and from the model. The page is built lazily; until
 * {@link #createPanel(BiographyOptionsModel)} is called, {@link #readFromModel(BiographyOptionsModel)} and
 * {@link #writeToModel(BiographyOptionsModel)} are no-ops.</p>
 */
class NameAndPortraitGenerationPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    // Wider than the default control column because the Biography combo boxes need the extra room.
    private static final int CONTROL_COLUMN_WIDTH = 240;

    private CampaignOptionsHeaderPanel nameAndPortraitGenerationHeader;
    private JCheckBox chkUseOriginFactionForNames;
    private JLabel lblFactionNames;
    private MMComboBox<String> comboFactionNames;
    private JPanel pnlRandomPortrait;
    private List<PersonnelRole> personnelRoles;
    private JCheckBox[] chkUsePortrait;
    private JButton btnEnableAllPortraits;
    private JButton btnDisableAllPortraits;
    private JCheckBox chkAssignPortraitOnRoleChange;
    private JCheckBox chkAllowDuplicatePortraits;
    private JCheckBox chkUseGenderedPortraitsOnly;
    private JCheckBox chkNoRandomPortraitsForChildren;
    private JCheckBox chkChildPortraitsWhenComingOfAge;

    private boolean created;

    /**
     * Builds the Name and Portrait Generation page, populates its controls from the supplied model, and returns the
     * assembled panel.
     *
     * @param model the shared biography options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Name and Portrait Generation Page
     */
    @Nonnull JPanel createPanel(@Nullable BiographyOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_nova_cat.png";
        nameAndPortraitGenerationHeader = new CampaignOptionsHeaderPanel("NameAndPortraitGenerationPage",
            imageAddress);

        // Contents
        comboFactionNames = new MMComboBox<>("comboFactionNames", getFactionNamesModel());
        personnelRoles = PersonnelRole.getCombatRoles();
        personnelRoles.addAll(PersonnelRole.getSupportRoles());

        chkAssignPortraitOnRoleChange = new CampaignOptionsCheckBox("AssignPortraitOnRoleChange");
        chkAssignPortraitOnRoleChange.addMouseListener(createTipPanelUpdater("AssignPortraitOnRoleChange"));

        chkAllowDuplicatePortraits = new CampaignOptionsCheckBox("AllowDuplicatePortraits");
        chkAllowDuplicatePortraits.addMouseListener(createTipPanelUpdater("AllowDuplicatePortraits"));

        chkUseGenderedPortraitsOnly = new CampaignOptionsCheckBox("UseGenderedPortraitsOnly",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseGenderedPortraitsOnly.addMouseListener(createTipPanelUpdater("UseGenderedPortraitsOnly"));

        chkNoRandomPortraitsForChildren = new CampaignOptionsCheckBox("NoRandomPortraitsForChildren",
                getMetadata(new Version(0, 51, 0)));
        chkNoRandomPortraitsForChildren.addMouseListener(createTipPanelUpdater("NoRandomPortraitsForChildren"));

        chkChildPortraitsWhenComingOfAge = new CampaignOptionsCheckBox("ChildPortraitsWhenComingOfAge",
                getMetadata(new Version(0, 51, 0)));
        chkChildPortraitsWhenComingOfAge.addMouseListener(createTipPanelUpdater("ChildPortraitsWhenComingOfAge"));

        chkUseOriginFactionForNames = new CampaignOptionsCheckBox("UseOriginFactionForNames");
        chkUseOriginFactionForNames.addMouseListener(createTipPanelUpdater("UseOriginFactionForNames"));

        lblFactionNames = new CampaignOptionsLabel("FactionNames");
        lblFactionNames.addMouseListener(createTipPanelUpdater("FactionNames"));
        comboFactionNames.addMouseListener(createTipPanelUpdater("FactionNames"));

        JPanel nameGenerationPanel = createNameGenerationPanel();
        JPanel portraitRulesPanel = createPortraitRulesPanel();
        pnlRandomPortrait = createRandomPortraitPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("NameAndPortraitGenerationPage",
                "NameAndPortraitGenerationPage",
                imageAddress)
            .header(nameAndPortraitGenerationHeader)
            .quote("nameAndPortraitGenerationPage")
            .section("lblNameGenerationPanel.text", "lblNameGenerationPanel.summary", nameGenerationPanel)
            .section("lblPortraitRulesPanel.text", "lblPortraitRulesPanel.summary", portraitRulesPanel)
            .section("lblRandomPortraitPanel.text", "lblRandomPortraitPanel.summary", pnlRandomPortrait)
            .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Builds and returns a `DefaultComboBoxModel` containing the names of all
     * available factions.
     *
     * @return A `DefaultComboBoxModel` populated with faction names for random name
     *         generation rules.
     */
    private static DefaultComboBoxModel<String> getFactionNamesModel() {
        DefaultComboBoxModel<String> factionNamesModel = new DefaultComboBoxModel<>();
        for (final String faction : RandomNameGenerator.getInstance().getFactions()) {
            factionNamesModel.addElement(faction);
        }
        return factionNamesModel;
    }

    private @Nonnull JPanel createNameGenerationPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("NameGenerationPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseOriginFactionForNames);
        panel.addRow(lblFactionNames, comboFactionNames);

        return panel;
    }

    private @Nonnull JPanel createPortraitRulesPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PortraitRulesPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
            chkAssignPortraitOnRoleChange,
            chkAllowDuplicatePortraits,
            chkUseGenderedPortraitsOnly,
            chkNoRandomPortraitsForChildren);
        panel.addCheckBox(chkChildPortraitsWhenComingOfAge);

        return panel;
    }

    /**
     * Creates a panel for customizing random portrait generation for personnel
     * roles.
     * <p>
     * This includes:
     * <p>
     * <li>Options to enable or disable the use of role-specific portraits.</li>
     * <li>Buttons to toggle all or no portrait options collectively.</li>
     * </p>
     *
     * @return A {@code JPanel} containing the random portrait generation
     *         configuration UI.
     */
    private @Nonnull JPanel createRandomPortraitPanel() {
        // Contents
        chkUsePortrait = new JCheckBox[personnelRoles.size() + 1];

        btnEnableAllPortraits = createPortraitAssignmentButton("EnableAllPortraits");
        btnEnableAllPortraits.addActionListener(evt -> {
            for (JCheckBox checkBox : chkUsePortrait) {
                if (checkBox != null) {
                    checkBox.setSelected(true);
                }
            }
        });
        btnEnableAllPortraits.addMouseListener(createTipPanelUpdater("EnableAllPortraits"));

        btnDisableAllPortraits = createPortraitAssignmentButton("DisableAllPortraits");
        btnDisableAllPortraits.addActionListener(evt -> {
            for (JCheckBox checkBox : chkUsePortrait) {
                if (checkBox != null) {
                    checkBox.setSelected(false);
                }
            }
        });
        btnDisableAllPortraits.addMouseListener(createTipPanelUpdater("DisableAllPortraits"));

        // Layout the Panel
        // BoxLayout (rather than FlowLayout.LEFT) so the first button sits flush at x=0
        // and lines up with the checkbox
        // column below; FlowLayout applies its hgap before the first component, which
        // pushed the buttons ~5px right.
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
        actionPanel.setOpaque(false);
        actionPanel.add(btnEnableAllPortraits);
        actionPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        actionPanel.add(btnDisableAllPortraits);
        actionPanel.add(Box.createHorizontalGlue());

        JCheckBox[] portraitCheckBoxes = new JCheckBox[personnelRoles.size() + 1];
        int portraitIndex = 0;

        // Add remaining checkboxes
        JCheckBox jCheckBox;
        for (final PersonnelRole role : personnelRoles) {
            jCheckBox = new JCheckBox(role.toString());
            jCheckBox.addMouseListener(createTipPanelUpdater(null,
                    role.getDescription(false)));
            portraitCheckBoxes[portraitIndex++] = jCheckBox;
            chkUsePortrait[role.ordinal()] = jCheckBox;
        }

        jCheckBox = new JCheckBox(PersonnelRoleSubType.CIVILIAN.toString());
        jCheckBox.addMouseListener(createTipPanelUpdater(null,
                getTextAt(getCampaignOptionsResourceBundle(), "lblCivilian.tooltip")));
        portraitCheckBoxes[portraitIndex] = jCheckBox;
        chkUsePortrait[personnelRoles.size()] = jCheckBox;

        CampaignOptionsFormPanel rolePanel = new CampaignOptionsFormPanel("RandomPortraitRolePanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        rolePanel.addCheckBoxGrid(2, portraitCheckBoxes);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setName("pnlRandomPortraitPanel");
        panel.setOpaque(false);
        panel.add(actionPanel, BorderLayout.NORTH);
        panel.add(rolePanel, BorderLayout.CENTER);

        return panel;
    }

    private @Nonnull JButton createPortraitAssignmentButton(String name) {
        JButton button = new JButton(getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".text"));
        button.setName("btn" + name);
        button.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".tooltip"));
        setSmallSizeVariant(button);
        return button;
    }

    /**
     * Copies name and portrait values from the shared model into this page's controls. This is a no-op until the page
     * has been built.
     *
     * @param model the shared biography options model to read values from
     */
    void readFromModel(@Nullable BiographyOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseOriginFactionForNames.setSelected(model.useOriginFactionForNames);
        comboFactionNames.setSelectedItem(model.factionNames);
        chkAssignPortraitOnRoleChange.setSelected(model.assignPortraitOnRoleChange);
        chkAllowDuplicatePortraits.setSelected(model.allowDuplicatePortraits);
        chkUseGenderedPortraitsOnly.setSelected(model.useGenderedPortraitsOnly);
        chkNoRandomPortraitsForChildren.setSelected(model.noRandomPortraitsForChildren);
        chkChildPortraitsWhenComingOfAge.setSelected(model.childPortraitsWhenComingOfAge);

        int portraitCount = Math.min(chkUsePortrait.length, model.usePortraitForRole.length);
        int civilianIndex = chkUsePortrait.length - 1;

        // Role-specific portraits are index-aligned with their role ordinal.
        for (int i = 0; i < portraitCount; i++) {
            if (chkUsePortrait[i] != null && i != civilianIndex) {
                chkUsePortrait[i].setSelected(model.usePortraitForRole[i]);
            }
        }

        // The last checkbox is the shared "Civilian" toggle; writeToModel spreads it across every civilian role,
        // so read it back from the first civilian role's slot to keep the two sides symmetric.
        if (civilianIndex < portraitCount && chkUsePortrait[civilianIndex] != null) {
            boolean civilianSelected = false;
            for (PersonnelRole role : PersonnelRole.getCivilianRoles()) {
                if (role.ordinal() < model.usePortraitForRole.length) {
                    civilianSelected = model.usePortraitForRole[role.ordinal()];
                    break;
                }
            }
            chkUsePortrait[civilianIndex].setSelected(civilianSelected);
        }
    }

    /**
     * Copies name and portrait values from this page's controls into the shared model. This is a no-op until the page
     * has been built.
     *
     * @param model the shared biography options model to write values into
     */
    void writeToModel(@Nullable BiographyOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useOriginFactionForNames = chkUseOriginFactionForNames.isSelected();
        model.factionNames = comboFactionNames.getSelectedItem();
        model.assignPortraitOnRoleChange = chkAssignPortraitOnRoleChange.isSelected();
        model.allowDuplicatePortraits = chkAllowDuplicatePortraits.isSelected();
        model.useGenderedPortraitsOnly = chkUseGenderedPortraitsOnly.isSelected();
        model.noRandomPortraitsForChildren = chkNoRandomPortraitsForChildren.isSelected();
        model.childPortraitsWhenComingOfAge = chkChildPortraitsWhenComingOfAge.isSelected();

        int portraitCount = Math.min(chkUsePortrait.length, model.usePortraitForRole.length);
        int civilianIndex = chkUsePortrait.length - 1;

        // Role-specific portraits are index-aligned with their role ordinal.
        for (int i = 0; i < portraitCount; i++) {
            if (chkUsePortrait[i] != null && i != civilianIndex) {
                model.usePortraitForRole[i] = chkUsePortrait[i].isSelected();
            }
        }

        // The shared "Civilian" toggle spreads across every civilian role slot.
        if (civilianIndex < portraitCount && chkUsePortrait[civilianIndex] != null) {
            boolean civilianSelected = chkUsePortrait[civilianIndex].isSelected();
            for (PersonnelRole role : PersonnelRole.getCivilianRoles()) {
                if (role.ordinal() < model.usePortraitForRole.length) {
                    model.usePortraitForRole[role.ordinal()] = civilianSelected;
                }
            }
        }
    }
}
