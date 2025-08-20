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
import static megamek.codeUtilities.MathUtility.clamp;
import static mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder.createRoundedLineBorder;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;

import megamek.common.EnhancedTabbedPane;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

public class LifePathSkillPicker extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathSkillPicker";

    private static final int MINIMUM_INSTRUCTIONS_WIDTH = scaleForGUI(250);
    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(575);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(525);

    private static final int TEXT_PANEL_WIDTH = (int) round(MINIMUM_INSTRUCTIONS_WIDTH * 0.75);
    private static final String PANEL_HTML_FORMAT = "<html><div style='width:%dpx;'>%s</div></html>";

    private static final int PADDING = scaleForGUI(10);

    private final Map<SkillType, Integer> storedSkillLevels;
    private Map<SkillType, Integer> selectedSkillLevels;

    public Map<SkillType, Integer> getSelectedSkillLevels() {
        return selectedSkillLevels;
    }

    public LifePathSkillPicker(Map<SkillType, Integer> selectedSkillLevels, LifePathBuilderTabType tabType) {
        super();

        // Defensive copies to avoid external modification
        this.selectedSkillLevels = new HashMap<>(selectedSkillLevels);
        storedSkillLevels = new HashMap<>(selectedSkillLevels);

        setTitle(getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.title"));

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

        pnlMain.add(pnlOptions, BorderLayout.NORTH);
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

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String titleCancel = getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.button.cancel");
        RoundedJButton btnCancel = new RoundedJButton(titleCancel);
        btnCancel.addActionListener(e -> {
            selectedSkillLevels = storedSkillLevels;
            dispose();
        });

        String titleConfirm = getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.button.confirm");
        RoundedJButton btnConfirm = new RoundedJButton(titleConfirm);
        btnConfirm.addActionListener(e -> dispose());

        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(btnCancel);
        buttonPanel.add(Box.createHorizontalStrut(PADDING));
        buttonPanel.add(btnConfirm);
        buttonPanel.add(Box.createHorizontalGlue());

        pnlControls.add(Box.createVerticalStrut(PADDING));
        pnlControls.add(buttonPanel);

        return pnlControls;
    }

    private JPanel buildOptionsPanel(LifePathBuilderTabType tabType) {
        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));

        String titleOptions = getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.label");
        pnlOptions.setBorder(createRoundedLineBorder(titleOptions));

        List<SkillType> combatSkills = new ArrayList<>();
        List<SkillType> supportSkills = new ArrayList<>();
        List<SkillType> roleplaySkills1 = new ArrayList<>();
        List<SkillType> roleplaySkills2 = new ArrayList<>();
        List<SkillType> roleplaySkills3 = new ArrayList<>();
        List<SkillType> roleplaySkills4 = new ArrayList<>();
        List<String> allSkills = new ArrayList<>(List.of(SkillType.getSkillList()));
        Collections.sort(allSkills);

        // Normal Skills
        for (String skillName : new ArrayList<>(allSkills)) {
            SkillType type = SkillType.getType(skillName);
            if (type.isCombatSkill()) {
                combatSkills.add(type);
                allSkills.remove(skillName);
            } else if (type.isSupportSkill()) {
                supportSkills.add(type);
                allSkills.remove(skillName);
            }
        }

        // Roleplay Skills
        int groups = 4;
        int n = allSkills.size();
        for (int i = 0; i < n; i++) {
            String skillName = allSkills.get(i);
            SkillType skill = SkillType.getType(skillName);
            int groupIdx = (int) Math.floor(i * groups / (double) n);
            switch (groupIdx) {
                case 0 -> roleplaySkills1.add(skill);
                case 1 -> roleplaySkills2.add(skill);
                case 2 -> roleplaySkills3.add(skill);
                default -> roleplaySkills4.add(skill);
            }
        }

        EnhancedTabbedPane optionPane = new EnhancedTabbedPane();

        if (!combatSkills.isEmpty()) {
            FastJScrollPane pnlCombatSkills = getSkillOptions(combatSkills, tabType);
            optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.combat.label"),
                  pnlCombatSkills);
        }

        if (!supportSkills.isEmpty()) {
            FastJScrollPane pnlSupportSkills = getSkillOptions(supportSkills, tabType);
            optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.support.label"),
                  pnlSupportSkills);
        }

        if (!roleplaySkills1.isEmpty()) {
            buildTab(roleplaySkills1, optionPane, getSkillOptions(roleplaySkills1, tabType));
        }

        if (!roleplaySkills2.isEmpty()) {
            buildTab(roleplaySkills2, optionPane, getSkillOptions(roleplaySkills2, tabType));
        }

        if (!roleplaySkills3.isEmpty()) {
            buildTab(roleplaySkills3, optionPane, getSkillOptions(roleplaySkills3, tabType));
        }

        if (!roleplaySkills4.isEmpty()) {
            buildTab(roleplaySkills4, optionPane, getSkillOptions(roleplaySkills4, tabType));
        }

        pnlOptions.add(optionPane);
        return pnlOptions;
    }

    private static void buildTab(List<SkillType> skills, EnhancedTabbedPane optionPane,
          FastJScrollPane pnlOptions) {
        String firstName = skills.get(0).getName();
        String lastName = skills.get(skills.size() - 1).getName();

        char firstLetter = firstName.isEmpty() ? '\0' : firstName.charAt(0);
        char lastLetter = lastName.isEmpty() ? '\0' : lastName.charAt(0);

        optionPane.addTab(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.options.roleplay.label", firstLetter,
              lastLetter), pnlOptions);
    }

    private FastJScrollPane getSkillOptions(List<SkillType> skills, LifePathBuilderTabType tabType) {
        JPanel pnlSkills = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int columns = 3;
        for (int i = 0; i < skills.size(); i++) {
            SkillType type = skills.get(i);
            String label = type.getName();
            label = label.replace(SkillType.RP_ONLY_TAG, "");

            int minimumSkillLevel = 0;
            int maximumSkillLevel = switch (tabType) {
                case REQUIREMENTS, EXCLUSIONS -> type.getMaxLevel();
                case FIXED_XP, FLEXIBLE_XP -> 1000;
            };

            JLabel lblSkill = new JLabel(label);
            JSpinner spnSkillLevel = new JSpinner(new SpinnerNumberModel(minimumSkillLevel, minimumSkillLevel,
                  maximumSkillLevel, 1));

            if (selectedSkillLevels.containsKey(type)) {
                int currentValue = selectedSkillLevels.get(type);
                currentValue = clamp(currentValue, minimumSkillLevel, maximumSkillLevel);
                spnSkillLevel.setValue(currentValue);
            }

            spnSkillLevel.addChangeListener(evt -> {
                int value = (int) spnSkillLevel.getValue();
                if (value > minimumSkillLevel) {
                    selectedSkillLevels.put(type, value);
                }
            });

            gbc.gridx = i % columns;
            gbc.gridy = i / columns;

            JPanel pnlRows = new JPanel();
            pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
            pnlRows.add(lblSkill);
            pnlRows.add(Box.createHorizontalStrut(PADDING));
            pnlRows.add(spnSkillLevel);
            pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlSkills.add(pnlRows, gbc);
        }

        FastJScrollPane scrollSkills = new FastJScrollPane(pnlSkills);
        scrollSkills.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setBorder(null);

        return scrollSkills;
    }

    private JPanel initializeInstructionsPanel(LifePathBuilderTabType tabType) {
        JPanel pnlInstructions = new JPanel();

        String titleInstructions = getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.instructions.label");
        pnlInstructions.setBorder(createRoundedLineBorder(titleInstructions));

        JEditorPane txtInstructions = new JEditorPane();
        txtInstructions.setContentType("text/html");
        txtInstructions.setEditable(false);
        String instructions = String.format(PANEL_HTML_FORMAT, TEXT_PANEL_WIDTH,
              getTextAt(RESOURCE_BUNDLE, "LifePathSkillPicker.instructions.text." + tabType.getLookupName()));
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
