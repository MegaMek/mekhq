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
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_ATTRIBUTE_SCORE;
import static mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder.createRoundedLineBorder;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
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

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;

class LifePathAttributePicker extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(LifePathAttributePicker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathAttributePicker";

    private static final int MINIMUM_INSTRUCTIONS_WIDTH = scaleForGUI(250);
    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(200);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(400);

    private static final int TOOLTIP_PANEL_WIDTH = (int) round(MINIMUM_MAIN_WIDTH * 0.75);
    private static final int TEXT_PANEL_WIDTH = (int) round(MINIMUM_INSTRUCTIONS_WIDTH * 0.75);
    private static final String PANEL_HTML_FORMAT = "<html><div style='width:%dpx;'>%s</div></html>";

    private static final int PADDING = scaleForGUI(10);

    private JLabel lblTooltipDisplay;

    private final Map<SkillAttribute, Integer> storedAttributeScores;
    private Map<SkillAttribute, Integer> selectedAttributeScores;

    private final Integer storedFlexibleAttribute;
    private Integer selectedFlexibleAttribute;

    Map<SkillAttribute, Integer> getSelectedAttributeScores() {
        return selectedAttributeScores;
    }

    @Nullable
    Integer getFlexibleAttribute() {
        return selectedFlexibleAttribute;
    }

    LifePathAttributePicker(Map<SkillAttribute, Integer> selectedAttributeScores,
          @Nullable Integer selectedFlexibleAttribute,
          LifePathBuilderTabType tabType) {
        super();

        // Defensive copies to avoid external modification
        this.selectedAttributeScores = new HashMap<>(selectedAttributeScores);
        storedAttributeScores = new HashMap<>(selectedAttributeScores);

        this.selectedFlexibleAttribute = selectedFlexibleAttribute;
        storedFlexibleAttribute = selectedFlexibleAttribute;

        setTitle(getTextAt(RESOURCE_BUNDLE, "LifePathAttributePicker.title"));

        JPanel pnlInstructions = initializeInstructionsPanel(tabType);
        JPanel pnlOptions = buildOptionsPanel(tabType);
        JPanel pnlControls = buildControlPanel();

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.gridy = 0;

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        mainPanel.add(pnlInstructions, gbc);

        JPanel pnlMain = new JPanel();
        pnlMain.setLayout(new BorderLayout());

        pnlMain.add(pnlOptions, BorderLayout.NORTH);
        pnlMain.add(pnlControls, BorderLayout.SOUTH);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(pnlMain, gbc);

        setContentPane(mainPanel);
        setMinimumSize(new Dimension((int) round((MINIMUM_INSTRUCTIONS_WIDTH + MINIMUM_MAIN_WIDTH) * 1.25),
              MINIMUM_COMPONENT_HEIGHT));
        setLocationRelativeTo(null);
        setModal(true);
        setPreferences(); // Must be before setVisible
        setVisible(true);
    }

    private JPanel buildControlPanel() {
        JPanel pnlControls = new JPanel();
        pnlControls.setLayout(new BoxLayout(pnlControls, BoxLayout.Y_AXIS));
        pnlControls.setBorder(RoundedLineBorder.createRoundedLineBorder());

        lblTooltipDisplay = new JLabel();
        lblTooltipDisplay.setBorder(new EmptyBorder(0, PADDING, 0, PADDING));
        lblTooltipDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        setLblTooltipDisplay("");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String titleCancel = getTextAt(RESOURCE_BUNDLE, "LifePathAttributePicker.button.cancel");
        RoundedJButton btnCancel = new RoundedJButton(titleCancel);
        btnCancel.addActionListener(e -> {
            selectedAttributeScores = storedAttributeScores;
            selectedFlexibleAttribute = storedFlexibleAttribute;
            dispose();
        });

        String titleConfirm = getTextAt(RESOURCE_BUNDLE, "LifePathAttributePicker.button.confirm");
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

    private JPanel buildAttributeRow(
          String label, String tooltip, int minValue, int maxValue, int defaultValue,
          java.util.function.Consumer<Integer> onChanged) {

        JLabel lblAttribute = new JLabel(label);
        lblAttribute.setToolTipText(tooltip);

        JSpinner spnAttributeScore = new JSpinner(new SpinnerNumberModel(defaultValue, minValue, maxValue, 1));
        spnAttributeScore.addChangeListener(evt -> onChanged.accept((Integer) spnAttributeScore.getValue()));
        spnAttributeScore.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltip)
        );

        JPanel pnlRows = new JPanel();
        pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
        pnlRows.add(lblAttribute);
        pnlRows.add(Box.createHorizontalStrut(PADDING));
        pnlRows.add(spnAttributeScore);
        pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

        return pnlRows;
    }

    private JPanel buildOptionsPanel(LifePathBuilderTabType tabType) {
        final int LOWEST_VALUE = -1000;
        final int HIGHEST_VALUE = 1000;

        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));

        String titleOptions = getTextAt(RESOURCE_BUNDLE, "LifePathAttributePicker.options.label");
        pnlOptions.setBorder(RoundedLineBorder.createRoundedLineBorder(titleOptions));

        for (SkillAttribute attribute : SkillAttribute.values()) {
            if (attribute == SkillAttribute.NONE) {
                continue;
            }

            String label = attribute.getLabel();
            String tooltip = attribute.getDescription();

            int categoryMinimumValue = switch (tabType) {
                case FIXED_XP, FLEXIBLE_XP -> LOWEST_VALUE;
                case REQUIREMENTS, EXCLUSIONS -> MINIMUM_ATTRIBUTE_SCORE;
            };
            int categoryMaximumValue = switch (tabType) {
                case FIXED_XP, FLEXIBLE_XP -> HIGHEST_VALUE;
                case REQUIREMENTS, EXCLUSIONS -> MAXIMUM_ATTRIBUTE_SCORE;
            };

            int keyValue = switch (tabType) {
                case REQUIREMENTS -> categoryMinimumValue;
                case EXCLUSIONS -> categoryMaximumValue;
                case FIXED_XP, FLEXIBLE_XP -> 0;
            };

            int defaultValue = selectedAttributeScores.getOrDefault(attribute, keyValue);

            JPanel row = buildAttributeRow(
                  label, tooltip, categoryMinimumValue, categoryMaximumValue, defaultValue,
                  value -> {
                      if (value == keyValue) {
                          selectedAttributeScores.remove(attribute);
                      } else {
                          selectedAttributeScores.put(attribute, value);
                      }
                  }
            );
            pnlOptions.add(row);
        }

        // Flexible attribute row
        String label = getTextAt(RESOURCE_BUNDLE, "LifePathAttributePicker.flexible.label");
        String tooltip = getTextAt(RESOURCE_BUNDLE, "LifePathAttributePicker.flexible.tooltip");

        int categoryMinimumValue = switch (tabType) {
            case FIXED_XP, FLEXIBLE_XP -> LOWEST_VALUE;
            case REQUIREMENTS, EXCLUSIONS -> MINIMUM_ATTRIBUTE_SCORE;
        };
        int categoryMaximumValue = switch (tabType) {
            case FIXED_XP, FLEXIBLE_XP -> HIGHEST_VALUE;
            case REQUIREMENTS, EXCLUSIONS -> MAXIMUM_ATTRIBUTE_SCORE;
        };
        int keyValue = switch (tabType) {
            case REQUIREMENTS -> categoryMinimumValue;
            case EXCLUSIONS -> categoryMaximumValue;
            case FIXED_XP, FLEXIBLE_XP -> 0;
        };
        int defaultValue = selectedFlexibleAttribute == null ? keyValue : selectedFlexibleAttribute;

        JPanel row = buildAttributeRow(
              label, tooltip, categoryMinimumValue, categoryMaximumValue, defaultValue,
              value -> {
                  if (value == keyValue) {
                      selectedFlexibleAttribute = null;
                  } else {
                      selectedFlexibleAttribute = value;
                  }
              }
        );
        pnlOptions.add(row);

        return pnlOptions;
    }

    private void setLblTooltipDisplay(String newText) {
        String newTooltipText = String.format(PANEL_HTML_FORMAT, TOOLTIP_PANEL_WIDTH, newText);
        lblTooltipDisplay.setText(newTooltipText);
    }

    private JPanel initializeInstructionsPanel(LifePathBuilderTabType tabType) {
        JPanel pnlInstructions = new JPanel();

        String titleInstructions = getTextAt(RESOURCE_BUNDLE, "LifePathAttributePicker.instructions.label");
        pnlInstructions.setBorder(createRoundedLineBorder(titleInstructions));

        JEditorPane txtInstructions = new JEditorPane();
        txtInstructions.setContentType("text/html");
        txtInstructions.setEditable(false);
        String instructions = String.format(PANEL_HTML_FORMAT, TEXT_PANEL_WIDTH,
              getTextAt(RESOURCE_BUNDLE, "LifePathAttributePicker.instructions.text." + tabType.getLookupName()));
        txtInstructions.setText(instructions);

        FastJScrollPane scrollInstructions = new FastJScrollPane(txtInstructions);
        scrollInstructions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setBorder(null);

        pnlInstructions.add(scrollInstructions);
        pnlInstructions.setMinimumSize(new Dimension(MINIMUM_INSTRUCTIONS_WIDTH, MINIMUM_COMPONENT_HEIGHT));

        return pnlInstructions;
    }

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(LifePathAttributePicker.class);
            this.setName("LifePathAttributePicker");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
