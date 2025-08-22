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
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import megamek.common.EnhancedTabbedPane;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.advancedCharacterBuilder.ATOWLifeStage;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

public class LifePathBuilderDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathBuilderDialog";

    private static final int MINIMUM_COMPONENT_WIDTH = scaleForGUI(200);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(600);
    private static final Dimension SIDE_PANEL_MINIMUM_SIZE = new Dimension(MINIMUM_COMPONENT_WIDTH,
          MINIMUM_COMPONENT_HEIGHT);
    private static final Dimension MINIMUM_SIZE = new Dimension(MINIMUM_COMPONENT_WIDTH * 6,
          MINIMUM_COMPONENT_HEIGHT);
    private static final int PADDING = scaleForGUI(10);

    private static final int TOOLTIP_PANEL_WIDTH = (int) round(MINIMUM_SIZE.width * 0.95);
    private static final int TEXT_PANEL_WIDTH = (int) round(MINIMUM_COMPONENT_WIDTH * 0.95);
    private static final String PANEL_HTML_FORMAT = "<html><div style='width:%dpx;'>%s</div></html>";

    private FastJScrollPane scrollInstructions;
    private JEditorPane txtInstructions;

    private JLabel lblTooltipDisplay;

    private FastJScrollPane scrollProgress;
    private JEditorPane txtProgress;

    private LifePathBuilderTabBasicInformation basicInfoTab;
    private LifePathBuilderTabRequirements requirementsTab;
    private LifePathBuilderTabExclusions exclusionsTab;

    static String getLifePathBuilderResourceBundle() {
        return RESOURCE_BUNDLE;
    }

    static int getLifePathBuilderMinimumComponentWidth() {
        return MINIMUM_COMPONENT_WIDTH;
    }

    static int getLifePathBuilderPadding() {
        return PADDING;
    }

    public LifePathBuilderDialog(Frame owner, int gameYear) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.title"), true);
        JPanel contents = initialize(gameYear);

        SwingUtilities.invokeLater(() -> scrollInstructions.getVerticalScrollBar().setValue(0));
        SwingUtilities.invokeLater(() -> scrollProgress.getVerticalScrollBar().setValue(0));

        setContentPane(contents);
        setMinimumSize(MINIMUM_SIZE);
        setLocationRelativeTo(owner);
        setVisible(true);
    }

    private void setTxtInstructions(String newText) {
        String newTooltipText = String.format(PANEL_HTML_FORMAT, TEXT_PANEL_WIDTH, newText);
        txtInstructions.setText(newTooltipText);

        SwingUtilities.invokeLater(() -> scrollInstructions.getVerticalScrollBar().setValue(0));
    }

    void setLblTooltipDisplay(String newText) {
        String newTooltipText = String.format(PANEL_HTML_FORMAT, TOOLTIP_PANEL_WIDTH, newText);
        lblTooltipDisplay.setText(newTooltipText);
    }

    void updateTxtProgress() {
        StringBuilder newProgressText = new StringBuilder();

        String newBasicText = getNewBasicText();
        newProgressText.append(newBasicText);

        String newRequirementsText = getNewRequirementsText();
        newProgressText.append(newRequirementsText);

        String newExclusionsText = getNewExclusionsText();
        newProgressText.append(newExclusionsText);

        txtProgress.setText(newProgressText.toString());
    }

    private String getNewBasicText() {
        StringBuilder newText = new StringBuilder();

        String name = basicInfoTab.getName();
        if (!name.isBlank()) {
            newText.append("<h1 style='text-align:center; margin:0'>").append(name).append("</h1>");
        }

        String flavorText = basicInfoTab.getFlavorText();
        if (!flavorText.isBlank()) {
            newText.append("<i>").append(flavorText).append("</i>");
        }

        int age = basicInfoTab.getAge();
        if (age > 0) {
            if (!newText.isEmpty()) {
                newText.append("<br>");
            }
            newText.append(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.progress.basic.age", age));
        }

        int cost = Math.max(basicInfoTab.getDiscount(), 0);
        if (cost > 0) {
            if (!newText.isEmpty()) {
                newText.append("<br>");
            }
            newText.append(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.progress.basic.cost", cost));
        }

        Set<ATOWLifeStage> lifeStages = basicInfoTab.getLifeStages();
        if (!lifeStages.isEmpty()) {
            StringBuilder lifeStageText = new StringBuilder();
            lifeStageText.append(getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.progress.basic.stages"));

            List<ATOWLifeStage> orderedLifeStages = lifeStages.stream()
                                                          .sorted(ATOWLifeStage::compareTo)
                                                          .toList();

            for (int i = 0; i < orderedLifeStages.size(); i++) {
                ATOWLifeStage lifeStage = orderedLifeStages.get(i);
                if (i == 0) {
                    lifeStageText.append(lifeStage.getDisplayName());
                } else {
                    lifeStageText.append(", ").append(lifeStage.getDisplayName());
                }
            }
            newText.append("<br>").append(lifeStageText);
        }

        Set<LifePathCategory> categories = basicInfoTab.getCategories();
        if (!categories.isEmpty()) {
            StringBuilder categoriesText = new StringBuilder();
            categoriesText.append(getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.progress.basic.categories"));

            List<LifePathCategory> orderedCategories = categories.stream()
                                                             .sorted(LifePathCategory::compareTo)
                                                             .toList();

            for (int i = 0; i < orderedCategories.size(); i++) {
                LifePathCategory category = orderedCategories.get(i);
                if (i == 0) {
                    categoriesText.append(category.getDisplayName());
                } else {
                    categoriesText.append(", ").append(category.getDisplayName());
                }
            }
            newText.append("<br>").append(categoriesText);
        }

        return String.format("<div style='width:%dpx;'>%s</div>", TEXT_PANEL_WIDTH, newText);
    }

    private String getNewRequirementsText() {
        StringBuilder newRequirementsText = new StringBuilder();

        boolean isEmpty = true;
        Map<Integer, String> unorderedRequirements = requirementsTab.getRequirementsTabTextMap();
        for (int i = 0; i < unorderedRequirements.size(); i++) {
            String requirements = unorderedRequirements.get(i);
            if (!requirements.isBlank()) {
                isEmpty = false;
                break;
            }
        }
        if (isEmpty) {
            return "";
        }

        String requirementsTitle = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.tab.title");
        newRequirementsText.append("<h2 style='text-align:center; margin:0;'>").append(requirementsTitle).append(
              "</h2>");

        List<String> orderedRequirements = unorderedRequirements.entrySet().stream()
                                                 .sorted(Map.Entry.comparingByKey())
                                                 .map(Map.Entry::getValue)
                                                 .toList();

        boolean firstRequirement = true;
        for (int i = 0; i < orderedRequirements.size(); i++) {
            String requirements = orderedRequirements.get(i);
            if (requirements.isBlank()) {
                continue;
            }

            if (!firstRequirement) {
                newRequirementsText.append("<br>");
            }
            firstRequirement = false;

            String requirementTitle = getFormattedTextAt(RESOURCE_BUNDLE,
                  "LifePathBuilderDialog.tab." + (i == 0 ? "compulsory" : "optional") + ".label");
            newRequirementsText.append("&#9654; <b>").append(requirementTitle).append(": </b>");
            newRequirementsText.append(requirements);
        }

        return newRequirementsText.toString();
    }

    private String getNewExclusionsText() {
        StringBuilder newExclusionsText = new StringBuilder();

        String exclusions = exclusionsTab.getExclusionsTabTextStorage();
        if (exclusions.isBlank()) {
            return "";
        }

        String exclusionsTitle = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.exclusions.tab.title");
        newExclusionsText.append("<h2 style='text-align:center; margin:0;'>").append(exclusionsTitle).append(
              "</h2>");

        newExclusionsText.append(exclusions);

        return newExclusionsText.toString();
    }

    private JPanel initialize(int gameYear) {
        JPanel pnlInstructions = initializeInstructionsPanel();
        EnhancedTabbedPane tabMain = initializeMainPanel(gameYear);
        JPanel pnlProgress = initializeProgressPanel();

        // Layout using GridBagLayout for a width ratio of 1:2:1
        JPanel container = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weighty = 1.0;

        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1.0; // Instructions panel (1x width)
        gridBagConstraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        container.add(pnlInstructions, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 2.0; // Main panel (2x width)
        gridBagConstraints.insets = new Insets(PADDING, 0, PADDING, 0);
        container.add(tabMain, gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.weightx = 1.0; // Progress panel (1x width)
        gridBagConstraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        container.add(pnlProgress, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.insets = new Insets(0, PADDING, PADDING, PADDING);
        container.add(initializeControlPanel(), gridBagConstraints);

        return container;
    }

    private JPanel initializeInstructionsPanel() {
        JPanel pnlInstructions = new JPanel();

        // Border
        String titleInstructions = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.title.instructions");
        pnlInstructions.setBorder(createRoundedLineBorder(titleInstructions));

        // Text Area
        txtInstructions = new JEditorPane();
        txtInstructions.setContentType("text/html");
        txtInstructions.setEditable(false);
        String instructions = String.format(PANEL_HTML_FORMAT, TEXT_PANEL_WIDTH,
              getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.instructions.basic"));
        txtInstructions.setText(instructions);

        // Scroll Pane
        scrollInstructions = new FastJScrollPane(txtInstructions);
        scrollInstructions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setBorder(null);

        // Final Touches
        pnlInstructions.add(scrollInstructions);
        pnlInstructions.setMinimumSize(SIDE_PANEL_MINIMUM_SIZE);

        return pnlInstructions;
    }

    private EnhancedTabbedPane initializeMainPanel(int gameYear) {
        EnhancedTabbedPane tabMain = new EnhancedTabbedPane();
        String title = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.title.lifePath");
        tabMain.setBorder(RoundedLineBorder.createRoundedLineBorder(title));

        basicInfoTab = new LifePathBuilderTabBasicInformation(this, tabMain);
        requirementsTab = new LifePathBuilderTabRequirements(this, tabMain, gameYear);
        exclusionsTab = new LifePathBuilderTabExclusions(this, tabMain, gameYear);

        // Fixed XP
        JPanel tabFixedXP = new JPanel();
        tabFixedXP.setName("fixedXP");
        String titleFixedXP = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.fixedXP");
        tabMain.addTab(titleFixedXP, tabFixedXP);
        tabFixedXP.add(new JLabel("First Tab Content"));

        // TODO Add Award
        // TODO Remove Award

        // Flexible XP
        JPanel tabFlexibleXP = new JPanel();
        tabFlexibleXP.setName("flexibleXP");
        String titleFlexibleXP = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.flexibleXP");
        tabMain.addTab(titleFlexibleXP, tabFlexibleXP);
        tabFlexibleXP.add(new JLabel("First Tab Content"));

        // TODO Add Award Group
        // TODO Remove Award Group
        // TODO Add Award
        // TODO Remove Award

        // Add a listener to handle tab selection changes
        tabMain.addChangeListener(e -> {
            int selectedIndex = tabMain.getSelectedIndex();
            Component selectedTab = tabMain.getComponentAt(selectedIndex);
            String tabName = selectedTab.getName();

            String tabInstructionsKey = "LifePathBuilderDialog.tab.instructions." + tabName;
            String instructions = getTextAt(RESOURCE_BUNDLE, tabInstructionsKey);

            setTxtInstructions(instructions);
        });

        return tabMain;
    }

    private JPanel initializeProgressPanel() {
        JPanel pnlProgress = new JPanel(new BorderLayout());

        String titleProgress = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.title.progress");
        pnlProgress.setBorder(createRoundedLineBorder(titleProgress));

        txtProgress = new JEditorPane();
        txtProgress.setContentType("text/html");
        txtProgress.setEditable(false);
        updateTxtProgress();

        scrollProgress = new FastJScrollPane(txtProgress);
        scrollProgress.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollProgress.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollProgress.setBorder(null);

        pnlProgress.add(scrollProgress, BorderLayout.CENTER);

        pnlProgress.setMinimumSize(SIDE_PANEL_MINIMUM_SIZE);

        return pnlProgress;
    }

    private JPanel initializeControlPanel() {
        JPanel pnlControls = new JPanel(new GridBagLayout());
        pnlControls.setBorder(RoundedLineBorder.createRoundedLineBorder());

        JPanel pnlContents = new JPanel();
        pnlContents.setLayout(new BoxLayout(pnlContents, BoxLayout.Y_AXIS));
        pnlContents.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblTooltipDisplay = new JLabel();
        lblTooltipDisplay.setBorder(new EmptyBorder(0, PADDING, 0, PADDING));
        setLblTooltipDisplay("");
        lblTooltipDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
        pnlButtons.setAlignmentX(Component.CENTER_ALIGNMENT);

        String titleCancel = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.cancel");
        RoundedJButton btnCancel = new RoundedJButton(titleCancel);
        btnCancel.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCancel.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnCancel.addActionListener(e -> dispose());

        String titleSave = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.save");
        RoundedJButton btnSave = new RoundedJButton(titleSave);
        btnSave.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSave.setMargin(new Insets(PADDING, 0, PADDING, 0));

        String titleLoad = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.load");
        RoundedJButton btnLoad = new RoundedJButton(titleLoad);
        btnLoad.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLoad.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));

        pnlButtons.add(Box.createHorizontalGlue());
        pnlButtons.add(btnCancel);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));
        pnlButtons.add(btnSave);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));
        pnlButtons.add(btnLoad);
        pnlButtons.add(Box.createHorizontalGlue());

        pnlContents.add(Box.createVerticalGlue());
        pnlContents.add(lblTooltipDisplay);
        pnlContents.add(Box.createVerticalStrut(PADDING));
        pnlContents.add(pnlButtons);
        pnlContents.add(Box.createVerticalGlue());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlControls.add(pnlContents, gridBagConstraints);

        return pnlControls;
    }
}
