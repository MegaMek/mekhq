/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder;

import static java.lang.Math.round;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder.createRoundedLineBorder;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import megamek.common.EnhancedTabbedPane;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;

class LifePathSPAPicker extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathSPAPicker";

    private static final int MINIMUM_INSTRUCTIONS_WIDTH = scaleForGUI(250);
    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(575);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(575);

    private static final int TOOLTIP_PANEL_WIDTH = (int) round(MINIMUM_MAIN_WIDTH * 0.95);
    private static final int TEXT_PANEL_WIDTH = (int) round(MINIMUM_INSTRUCTIONS_WIDTH * 0.70);
    private static final String PANEL_HTML_FORMAT = "<html><div style='width:%dpx;'>%s</div></html>";

    private static final int PADDING = scaleForGUI(10);

    private JLabel lblTooltipDisplay;
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo;
    private final Map<CampaignOptionsAbilityInfo, Integer> storedAbilities;
    private Map<CampaignOptionsAbilityInfo, Integer> selectedAbilities;

    Map<CampaignOptionsAbilityInfo, Integer> getSelectedAbilities() {
        return selectedAbilities;
    }

    LifePathSPAPicker(Map<CampaignOptionsAbilityInfo, Integer> selectedAbilities,
          Map<String, CampaignOptionsAbilityInfo> allAbilityInfo, LifePathBuilderTabType tabType) {
        super();

        this.allAbilityInfo = allAbilityInfo;

        // Defensive copies to avoid external modification
        this.selectedAbilities = new HashMap<>(selectedAbilities);
        storedAbilities = new HashMap<>(selectedAbilities);

        setTitle(getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.title"));

        JPanel pnlInstructions = initializeInstructionsPanel(tabType);
        JPanel pnlOptions = buildOptionsPanel(tabType);
        JPanel pnlControls = buildControlPanel();

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.gridy = 0;

        gbc.gridx = 0;
        gbc.weightx = 0.2;
        gbc.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        mainPanel.add(pnlInstructions, gbc);

        JPanel pnlMain = new JPanel();
        pnlMain.setLayout(new BorderLayout());

        pnlMain.add(pnlOptions, BorderLayout.CENTER);
        pnlMain.add(pnlControls, BorderLayout.SOUTH);

        gbc.gridx = 1;
        gbc.weightx = 8;
        mainPanel.add(pnlMain, gbc);

        setContentPane(mainPanel);
        setMinimumSize(new Dimension((int) round((MINIMUM_INSTRUCTIONS_WIDTH + MINIMUM_MAIN_WIDTH) * 1.25),
              MINIMUM_COMPONENT_HEIGHT));
        setLocationRelativeTo(null);
        setModal(true);
        setVisible(true);
    }

    private JPanel buildControlPanel() {
        JPanel pnlControls = new JPanel();
        pnlControls.setLayout(new BoxLayout(pnlControls, BoxLayout.Y_AXIS));
        pnlControls.setBorder(createRoundedLineBorder());
        pnlControls.setPreferredSize(scaleForGUI(0, 150));

        lblTooltipDisplay = new JLabel();
        lblTooltipDisplay.setBorder(new EmptyBorder(0, PADDING, 0, PADDING));
        lblTooltipDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        setLblTooltipDisplay("");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String titleCancel = getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.button.cancel");
        RoundedJButton btnCancel = new RoundedJButton(titleCancel);
        btnCancel.addActionListener(e -> {
            selectedAbilities = storedAbilities;
            dispose();
        });

        String titleConfirm = getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.button.confirm");
        RoundedJButton btnConfirm = new RoundedJButton(titleConfirm);
        btnConfirm.addActionListener(e -> dispose());

        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(btnCancel);
        buttonPanel.add(Box.createHorizontalStrut(PADDING));
        buttonPanel.add(btnConfirm);
        buttonPanel.add(Box.createHorizontalGlue());

        pnlControls.add(lblTooltipDisplay);
        pnlControls.add(Box.createVerticalStrut(PADDING));
        pnlControls.add(buttonPanel);

        return pnlControls;
    }

    private JPanel buildOptionsPanel(LifePathBuilderTabType tabType) {
        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));

        String titleOptions = getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.options.label");
        pnlOptions.setBorder(createRoundedLineBorder(titleOptions));

        List<CampaignOptionsAbilityInfo> combatAbilities = new ArrayList<>();
        List<CampaignOptionsAbilityInfo> maneuveringAbilities = new ArrayList<>();
        List<CampaignOptionsAbilityInfo> utilityAbilities = new ArrayList<>();
        List<CampaignOptionsAbilityInfo> flawsAbilities = new ArrayList<>();
        List<CampaignOptionsAbilityInfo> originsAbilities = new ArrayList<>();

        for (CampaignOptionsAbilityInfo abilityInfo : allAbilityInfo.values()) {
            switch (abilityInfo.getCategory()) {
                case COMBAT_ABILITY -> combatAbilities.add(abilityInfo);
                case MANEUVERING_ABILITY -> maneuveringAbilities.add(abilityInfo);
                case UTILITY_ABILITY -> utilityAbilities.add(abilityInfo);
                case CHARACTER_FLAW -> flawsAbilities.add(abilityInfo);
                case CHARACTER_CREATION_ONLY -> originsAbilities.add(abilityInfo);
            }
        }

        Comparator<CampaignOptionsAbilityInfo> byDisplayName = Comparator.comparing(
              a -> a.getAbility().getDisplayName(), String.CASE_INSENSITIVE_ORDER);

        combatAbilities.sort(byDisplayName);
        maneuveringAbilities.sort(byDisplayName);
        utilityAbilities.sort(byDisplayName);
        flawsAbilities.sort(byDisplayName);
        originsAbilities.sort(byDisplayName);

        EnhancedTabbedPane optionPane = new EnhancedTabbedPane();

        boolean useBinaryOptions = tabType == LifePathBuilderTabType.REQUIREMENTS ||
                                         tabType == LifePathBuilderTabType.EXCLUSIONS;

        FastJScrollPane pnlCombatSkills = useBinaryOptions ? getAbilityOptionsBinary(combatAbilities) :
                                                getAbilityOptionsVariable(combatAbilities, tabType);
        optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.options.combat.label"),
              pnlCombatSkills);

        FastJScrollPane pnlManeuveringAbilities = useBinaryOptions ? getAbilityOptionsBinary(maneuveringAbilities) :
                                                        getAbilityOptionsVariable(maneuveringAbilities, tabType);
        optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.options.maneuvering.label"),
              pnlManeuveringAbilities);

        FastJScrollPane pnlUtilityAbilities = useBinaryOptions ? getAbilityOptionsBinary(utilityAbilities) :
                                                    getAbilityOptionsVariable(utilityAbilities, tabType);
        optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.options.utility.label"),
              pnlUtilityAbilities);

        FastJScrollPane pnlFlawsAbilities = useBinaryOptions ? getAbilityOptionsBinary(flawsAbilities) :
                                                  getAbilityOptionsVariable(flawsAbilities, tabType);
        optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.options.flaws.label"),
              pnlFlawsAbilities);

        FastJScrollPane pnlOriginsAbilities = useBinaryOptions ? getAbilityOptionsBinary(originsAbilities) :
                                                    getAbilityOptionsVariable(originsAbilities, tabType);
        optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.options.origins.label"),
              pnlOriginsAbilities);

        pnlOptions.add(optionPane, BorderLayout.NORTH);
        return pnlOptions;
    }

    private FastJScrollPane getAbilityOptionsBinary(List<CampaignOptionsAbilityInfo> abilityInfo) {
        JPanel pnlSkills = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int columns = 3;
        for (int i = 0; i < abilityInfo.size(); i++) {
            CampaignOptionsAbilityInfo info = abilityInfo.get(i);

            SpecialAbility ability = info.getAbility();
            String label = ability.getDisplayName().replaceAll("\\s*\\(.*$", "");
            String description = ability.getDescription() + " (" + ability.getCost() + " XP)";

            JCheckBox chkAbilityOption = new JCheckBox(label);
            chkAbilityOption.setSelected(selectedAbilities.containsKey(info));
            chkAbilityOption.addActionListener(evt -> {
                if (chkAbilityOption.isSelected()) {
                    selectedAbilities.put(info, 0);
                } else {
                    selectedAbilities.remove(info);
                }
            });
            chkAbilityOption.addMouseListener(
                  TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, description)
            );

            gbc.gridx = i % columns;
            gbc.gridy = i / columns;

            JPanel pnlRows = new JPanel();
            pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
            pnlRows.add(chkAbilityOption);
            pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlSkills.add(pnlRows, gbc);
        }

        FastJScrollPane scrollSkills = new FastJScrollPane(pnlSkills);
        scrollSkills.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setBorder(null);

        return scrollSkills;
    }

    private FastJScrollPane getAbilityOptionsVariable(List<CampaignOptionsAbilityInfo> allAbilityInfo,
          LifePathBuilderTabType tabType) {
        JPanel pnlAbilityOptions = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int columns = 3;
        for (int i = 0; i < allAbilityInfo.size(); i++) {
            CampaignOptionsAbilityInfo abilityInfo = allAbilityInfo.get(i);
            SpecialAbility ability = abilityInfo.getAbility();
            String label = ability.getDisplayName().replaceAll("\\s*\\(.*$", "");
            String description = ability.getDescription() + " (" + ability.getCost() + " XP)";

            int minimumValue = 0;
            int maximumValue = ability.getCost();
            if (maximumValue == 0) {
                minimumValue = -1;
            }

            switch (tabType) {
                case FIXED_XP, FLEXIBLE_XP -> {
                    minimumValue = -1000;
                    maximumValue = 1000;
                }
                case REQUIREMENTS, EXCLUSIONS -> {
                    maximumValue = ability.getCost();
                    if (maximumValue == 0) {
                        minimumValue = -1;
                    }
                }
            }

            int keyValue = switch (tabType) {
                case REQUIREMENTS -> minimumValue;
                case EXCLUSIONS -> maximumValue;
                case FIXED_XP, FLEXIBLE_XP -> 0;
            };

            int defaultValue = selectedAbilities.getOrDefault(abilityInfo, keyValue);

            JLabel lblAbility = new JLabel(label);
            JSpinner spnAbilityValue = new JSpinner(new SpinnerNumberModel(defaultValue, minimumValue,
                  maximumValue, 1));
            lblAbility.addMouseListener(
                  TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, description)
            );
            spnAbilityValue.addMouseListener(
                  TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, description)
            );

            final int finalTraitKeyValue = keyValue;
            spnAbilityValue.addChangeListener(evt -> {
                int value = (int) spnAbilityValue.getValue();
                if (value != finalTraitKeyValue) {
                    selectedAbilities.put(abilityInfo, value);
                }
            });

            gbc.gridx = i % columns;
            gbc.gridy = i / columns;

            JPanel pnlRows = new JPanel();
            pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
            pnlRows.add(lblAbility);
            pnlRows.add(Box.createHorizontalStrut(PADDING));
            pnlRows.add(spnAbilityValue);
            pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlAbilityOptions.add(pnlRows, gbc);
        }

        FastJScrollPane scrollAbilityOptions = new FastJScrollPane(pnlAbilityOptions);
        scrollAbilityOptions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollAbilityOptions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollAbilityOptions.setBorder(null);

        return scrollAbilityOptions;
    }

    private void setLblTooltipDisplay(String newText) {
        String newTooltipText = String.format(PANEL_HTML_FORMAT, TOOLTIP_PANEL_WIDTH, newText);
        lblTooltipDisplay.setText(newTooltipText);
    }

    private JPanel initializeInstructionsPanel(LifePathBuilderTabType tabType) {
        JPanel pnlInstructions = new JPanel();

        String titleInstructions = getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.instructions.label");
        pnlInstructions.setBorder(createRoundedLineBorder(titleInstructions));

        JEditorPane txtInstructions = new JEditorPane();
        txtInstructions.setContentType("text/html");
        txtInstructions.setEditable(false);
        String instructions = String.format(PANEL_HTML_FORMAT, TEXT_PANEL_WIDTH,
              getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.instructions.text." + tabType.getLookupName()));
        txtInstructions.setText(instructions);

        FastJScrollPane scrollInstructions = new FastJScrollPane(txtInstructions);
        scrollInstructions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setBorder(null);

        pnlInstructions.add(scrollInstructions);
        pnlInstructions.setMinimumSize(new Dimension(MINIMUM_INSTRUCTIONS_WIDTH, MINIMUM_COMPONENT_HEIGHT));

        return pnlInstructions;
    }
}
