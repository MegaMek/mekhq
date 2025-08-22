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
import java.util.Comparator;
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
import javax.swing.border.EmptyBorder;

import megamek.common.EnhancedTabbedPane;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;

public class LifePathCategoryCountPicker extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathCategoryCountPicker";

    private static final int MINIMUM_INSTRUCTIONS_WIDTH = scaleForGUI(250);
    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(600);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(450);

    private static final int TOOLTIP_PANEL_WIDTH = (int) round(MINIMUM_MAIN_WIDTH * 0.95);
    private static final int TEXT_PANEL_WIDTH = (int) round(MINIMUM_INSTRUCTIONS_WIDTH * 0.7);
    private static final String PANEL_HTML_FORMAT = "<html><div style='width:%dpx;'>%s</div></html>";

    private static final int PADDING = scaleForGUI(10);

    private JLabel lblTooltipDisplay;
    private final Map<LifePathCategory, Integer> storedCategoryCounts;
    private Map<LifePathCategory, Integer> selectedCategoryCounts;

    public Map<LifePathCategory, Integer> getSelectedCategoryCounts() {
        return selectedCategoryCounts;
    }

    public LifePathCategoryCountPicker(Map<LifePathCategory, Integer> selectedCategoryCounts,
          LifePathBuilderTabType tabType) {
        super();

        // Defensive copies to avoid external modification
        this.selectedCategoryCounts = new HashMap<>(selectedCategoryCounts);
        storedCategoryCounts = new HashMap<>(selectedCategoryCounts);

        setTitle(getTextAt(RESOURCE_BUNDLE, "LifePathCategoryCountPicker.title"));

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
        pnlControls.setPreferredSize(scaleForGUI(0, 75));

        lblTooltipDisplay = new JLabel();
        lblTooltipDisplay.setBorder(new EmptyBorder(0, PADDING, 0, PADDING));
        lblTooltipDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        setLblTooltipDisplay("");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String titleCancel = getTextAt(RESOURCE_BUNDLE, "LifePathCategoryCountPicker.button.cancel");
        RoundedJButton btnCancel = new RoundedJButton(titleCancel);
        btnCancel.addActionListener(e -> {
            selectedCategoryCounts = storedCategoryCounts;
            dispose();
        });

        String titleConfirm = getTextAt(RESOURCE_BUNDLE, "LifePathCategoryCountPicker.button.confirm");
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

        String titleOptions = getTextAt(RESOURCE_BUNDLE, "LifePathCategoryCountPicker.options.label");
        pnlOptions.setBorder(createRoundedLineBorder(titleOptions));

        List<LifePathCategory> categories0 = new ArrayList<>();
        List<LifePathCategory> categories1 = new ArrayList<>();
        List<LifePathCategory> categories2 = new ArrayList<>();
        List<LifePathCategory> categories3 = new ArrayList<>();
        List<LifePathCategory> categories4 = new ArrayList<>();

        List<LifePathCategory> allCategories = new ArrayList<>(List.of(LifePathCategory.values()));
        allCategories.sort(Comparator.comparing(LifePathCategory::getDisplayName));

        // Roleplay Skills
        int groups = 3;
        int n = allCategories.size();
        for (int i = 0; i < n; i++) {
            LifePathCategory category = allCategories.get(i);
            int groupIdx = (int) Math.floor(i * groups / (double) n);
            switch (groupIdx) {
                case 0 -> categories0.add(category);
                case 1 -> categories1.add(category);
                case 2 -> categories2.add(category);
                case 3 -> categories3.add(category);
                default -> categories4.add(category);
            }
        }

        EnhancedTabbedPane optionPane = new EnhancedTabbedPane();

        if (!categories0.isEmpty()) {
            buildTab(categories0, optionPane, getCategoryOptions(categories0, tabType));
        }

        if (!categories1.isEmpty()) {
            buildTab(categories1, optionPane, getCategoryOptions(categories1, tabType));
        }

        if (!categories2.isEmpty()) {
            buildTab(categories2, optionPane, getCategoryOptions(categories2, tabType));
        }

        if (!categories3.isEmpty()) {
            buildTab(categories3, optionPane, getCategoryOptions(categories3, tabType));
        }

        if (!categories4.isEmpty()) {
            buildTab(categories4, optionPane, getCategoryOptions(categories4, tabType));
        }

        pnlOptions.add(optionPane);
        return pnlOptions;
    }

    private static void buildTab(List<LifePathCategory> categories, EnhancedTabbedPane optionPane,
          FastJScrollPane pnlOptions) {
        String firstName = categories.get(0).getDisplayName();
        String lastName = categories.get(categories.size() - 1).getDisplayName();

        char firstLetter = firstName.isEmpty() ? '\0' : firstName.charAt(0);
        char lastLetter = lastName.isEmpty() ? '\0' : lastName.charAt(0);

        optionPane.addTab(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathCategoryCountPicker.options.tab", firstLetter,
              lastLetter), pnlOptions);
    }


    private FastJScrollPane getCategoryOptions(List<LifePathCategory> categories, LifePathBuilderTabType tabType) {
        JPanel pnlSkills = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int columns = 3;
        for (int i = 0; i < categories.size(); i++) {
            LifePathCategory category = categories.get(i);
            String label = category.getDisplayName();
            String description = category.getDescription();

            int minimumSkillLevel = 0;
            int maximumSkillLevel = 10;

            boolean isDefaultMaximum = tabType == LifePathBuilderTabType.EXCLUSIONS;
            int defaultValue = selectedCategoryCounts.getOrDefault(category,
                  (isDefaultMaximum ? maximumSkillLevel : minimumSkillLevel));

            JLabel lblCategory = new JLabel(label);
            JSpinner spnCategoryCount = new JSpinner(new SpinnerNumberModel(defaultValue, minimumSkillLevel,
                  maximumSkillLevel, 1));

            if (selectedCategoryCounts.containsKey(category)) {
                int currentValue = selectedCategoryCounts.get(category);
                currentValue = clamp(currentValue, minimumSkillLevel, maximumSkillLevel);
                spnCategoryCount.setValue(currentValue);
            }

            final int finalTraitKeyValue = isDefaultMaximum ? maximumSkillLevel : minimumSkillLevel;
            spnCategoryCount.addChangeListener(evt -> {
                int value = (int) spnCategoryCount.getValue();
                if (value != finalTraitKeyValue) {
                    selectedCategoryCounts.put(category, value);
                }
            });

            lblCategory.addMouseListener(
                  TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, description)
            );
            spnCategoryCount.addMouseListener(
                  TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, description)
            );

            gbc.gridx = i % columns;
            gbc.gridy = i / columns;

            JPanel pnlRows = new JPanel();
            pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
            pnlRows.add(lblCategory);
            pnlRows.add(Box.createHorizontalStrut(PADDING));
            pnlRows.add(spnCategoryCount);
            pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlSkills.add(pnlRows, gbc);
        }

        FastJScrollPane scrollSkills = new FastJScrollPane(pnlSkills);
        scrollSkills.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setBorder(null);

        return scrollSkills;
    }

    private void setLblTooltipDisplay(String newText) {
        String newTooltipText = String.format(PANEL_HTML_FORMAT, TOOLTIP_PANEL_WIDTH, newText);
        lblTooltipDisplay.setText(newTooltipText);
    }

    private JPanel initializeInstructionsPanel(LifePathBuilderTabType tabType) {
        JPanel pnlInstructions = new JPanel();

        String titleInstructions = getTextAt(RESOURCE_BUNDLE, "LifePathCategoryCountPicker.instructions.label");
        pnlInstructions.setBorder(createRoundedLineBorder(titleInstructions));

        JEditorPane txtInstructions = new JEditorPane();
        txtInstructions.setContentType("text/html");
        txtInstructions.setEditable(false);
        String instructions = String.format(PANEL_HTML_FORMAT, TEXT_PANEL_WIDTH,
              getTextAt(RESOURCE_BUNDLE, "LifePathCategoryCountPicker.instructions.text." + tabType.getLookupName()));
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
