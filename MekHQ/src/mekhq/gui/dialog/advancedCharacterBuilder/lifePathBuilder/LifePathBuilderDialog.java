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
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.advancedCharacterBuilder.ATOWLifeStage;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathEntryDataTraitLookup;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathRecord;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;

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
    private JEditorPane txtProgressBasic;

    private LifePathBuilderTabBasicInformation basicInfoTab;

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

    private void setTxtProgressBasic(String newText) {
        txtProgressBasic.setText(newText);
        SwingUtilities.invokeLater(() -> scrollProgress.getVerticalScrollBar().setValue(0));
    }

    void setLblTooltipDisplay(String newText) {
        String newTooltipText = String.format(PANEL_HTML_FORMAT, TOOLTIP_PANEL_WIDTH, newText);
        lblTooltipDisplay.setText(newTooltipText);
    }

    void updateTxtProgressBasic() {
        String name = basicInfoTab.getName();
        String flavorText = basicInfoTab.getFlavorText();
        int age = basicInfoTab.getAge();
        int discount = basicInfoTab.getDiscount();

        StringBuilder lifeStageText = new StringBuilder();
        for (ATOWLifeStage lifeStage : basicInfoTab.getLifeStages()) {
            if (lifeStageText.toString().isBlank()) {
                lifeStageText = new StringBuilder(lifeStage.getDisplayName());
            } else {
                lifeStageText.append(", ").append(lifeStage.getDisplayName());
            }
        }

        StringBuilder categoriesText = new StringBuilder();
        for (LifePathCategory category : basicInfoTab.getCategories()) {
            if (categoriesText.toString().isBlank()) {
                categoriesText = new StringBuilder(category.getDisplayName());
            } else {
                categoriesText.append(", ").append(category.getDisplayName());
            }
        }

        String newText = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.progress.basic", name,
              flavorText, age, discount, lifeStageText.toString(), categoriesText.toString());

        String newProgressText = String.format(PANEL_HTML_FORMAT, TEXT_PANEL_WIDTH, newText);
        setTxtProgressBasic(newProgressText);

        SwingUtilities.invokeLater(() -> scrollProgress.getVerticalScrollBar().setValue(0));
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

        // Requirements
        buildRequirementsTab(tabMain, gameYear);

        // Exclusions
        JPanel tabExclusions = new JPanel();
        tabExclusions.setName("exclusions");
        String titleExclusions = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.exclusions");
        tabMain.addTab(titleExclusions, tabExclusions);
        tabExclusions.add(new JLabel("First Tab Content"));

        // TODO Add Exclusion
        // TODO Remove Exclusion

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

    private void buildRequirementsTab(EnhancedTabbedPane tabMain, int gameYear) {
        JPanel tabRequirements = new JPanel(new BorderLayout());
        tabRequirements.setName("requirements");
        String titleRequirements = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.requirements");
        tabMain.addTab(titleRequirements, tabRequirements);

        // Panel for the two buttons at the top
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        String titleAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addGroup.label");
        String tooltipAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addGroup.tooltip");
        RoundedJButton btnAddRequirementGroup = new RoundedJButton(titleAddGroup);
        btnAddRequirementGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipAddGroup)
        );
        buttonPanel.add(btnAddRequirementGroup);

        String titleRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.removeGroup.label");
        String tooltipRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.removeGroup.tooltip");
        RoundedJButton btnRemoveRequirementGroup = new RoundedJButton(titleRemoveGroup);
        btnRemoveRequirementGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipRemoveGroup)
        );
        buttonPanel.add(btnRemoveRequirementGroup);

        // The actual tabbed pane and any action listeners (we add them here to avoid a situation where they're
        // called before the pane has been initialized)
        EnhancedTabbedPane tabbedPane = new EnhancedTabbedPane();
        tabbedPane.addChangeListener(e -> btnRemoveRequirementGroup.setEnabled(tabbedPane.getSelectedIndex() != 0));
        btnAddRequirementGroup.addActionListener(e -> createRequirementsTab(tabbedPane, gameYear));
        btnRemoveRequirementGroup.addActionListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();

            // Remove the current tab, unless it's Group 0
            if (selectedIndex > 0) {
                tabbedPane.removeTabAt(selectedIndex);
            } else {
                return;
            }

            // Update the names of the remaining tabs
            int tabCount = tabbedPane.getTabCount();
            for (int i = 0; i < tabCount; i++) {
                String titleTab = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.labelFormat", i);
                tabbedPane.setTitleAt(i, titleTab);
            }
        });

        // Add 'Group 0' - this group is required
        createRequirementsTab(tabbedPane, gameYear);

        tabRequirements.add(buttonPanel, BorderLayout.NORTH);
        tabRequirements.add(tabbedPane, BorderLayout.CENTER);
    }

    private void createRequirementsTab(EnhancedTabbedPane tabbedPane, int gameYear) {
        // Create the panel to be used in the tab
        JPanel requirementGroupPanel = new JPanel();
        requirementGroupPanel.setLayout(new BorderLayout());

        // Panel for the 8 buttons (using GridLayout: 2 rows, 4 columns)
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 4, PADDING, PADDING));

        // Attributes
        Map<SkillAttribute, Integer> attributes = new HashMap<>();
        String titleAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addAttribute.label");
        String tooltipAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addAttribute.tooltip");
        RoundedJButton btnAddAttribute = new RoundedJButton(titleAddAttribute);
        btnAddAttribute.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipAddAttribute)
        );
        buttonsPanel.add(btnAddAttribute);

        // Traits
        Map<LifePathEntryDataTraitLookup, Integer> traits = new HashMap<>();
        String titleAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addTrait.label");
        String tooltipAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addTrait.tooltip");
        RoundedJButton btnAddTrait = new RoundedJButton(titleAddTrait);
        btnAddTrait.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipAddTrait)
        );
        buttonsPanel.add(btnAddTrait);

        // Skills
        Map<String, Integer> skills = new HashMap<>();
        String titleAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSkill.label");
        String tooltipAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSkill.tooltip");
        RoundedJButton btnAddSkill = new RoundedJButton(titleAddSkill);
        btnAddSkill.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipAddSkill)
        );
        buttonsPanel.add(btnAddSkill);

        // SPAs
        PersonnelOptions spas = new PersonnelOptions();
        String titleAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSPA.label");
        String tooltipAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSPA.tooltip");
        RoundedJButton btnAddSPA = new RoundedJButton(titleAddSPA);
        btnAddSPA.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipAddSPA)
        );
        buttonsPanel.add(btnAddSPA);

        // Factions
        List<Faction> factions = new ArrayList<>();
        String titleAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addFaction.label");
        String tooltipAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addFaction.tooltip");
        RoundedJButton btnAddFaction = new RoundedJButton(titleAddFaction);
        btnAddFaction.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipAddFaction)
        );
        buttonsPanel.add(btnAddFaction);

        // Life Paths
        List<LifePathRecord> lifePaths = new ArrayList<>();
        String titleAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addLifePath.label");
        String tooltipAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addLifePath.tooltip");
        RoundedJButton btnAddLifePath = new RoundedJButton(titleAddLifePath);
        btnAddLifePath.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipAddLifePath)
        );
        buttonsPanel.add(btnAddLifePath);

        // Categories
        Map<LifePathCategory, Integer> categories = new HashMap<>();
        String titleAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addCategory.label");
        String tooltipAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addCategory.tooltip");
        RoundedJButton btnAddCategory = new RoundedJButton(titleAddCategory);
        btnAddCategory.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipAddCategory)
        );
        buttonsPanel.add(btnAddCategory);

        // Panel below the buttons
        JPanel pnlDisplay = new JPanel();
        pnlDisplay.setLayout(new BorderLayout());

        String titleBorder = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.tab.title");
        pnlDisplay.setBorder(RoundedLineBorder.createRoundedLineBorder(titleBorder));

        JEditorPane txtRequirements = new JEditorPane();
        txtRequirements.setContentType("text/html");
        txtRequirements.setEditable(false);
        String progressText = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits,
              skills, spas);
        txtRequirements.setText(progressText);

        FastJScrollPane scrollRequirements = new FastJScrollPane(txtRequirements);
        scrollRequirements.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollRequirements.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollRequirements.setBorder(null);

        pnlDisplay.add(scrollRequirements, BorderLayout.CENTER);

        // Action Listeners
        btnAddAttribute.addActionListener(e -> {
            this.setVisible(false);
            LifePathAttributePicker picker = new LifePathAttributePicker(attributes);
            attributes.clear();
            attributes.putAll(picker.getSelectedAttributeScores());
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            this.setVisible(true);
        });
        btnAddTrait.addActionListener(e -> {
            this.setVisible(false);
            // TODO launch a dialog that lists the Traits and allows the user to set levels needed
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            this.setVisible(true);
        });
        btnAddSkill.addActionListener(e -> {
            this.setVisible(false);
            // TODO launch a dialog that lists the Skills and allows the user to specify levels for each
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            this.setVisible(true);
        });
        btnAddSPA.addActionListener(e -> {
            this.setVisible(false);
            // TODO launch a dialog that lists the currently enabled SPAs and allows the user to pick x
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            this.setVisible(true);
        });
        btnAddFaction.addActionListener(e -> {
            this.setVisible(false);
            // TODO launch a dialog that allows the player to add and remove factions from the list of playable factions
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            this.setVisible(true);
        });
        btnAddLifePath.addActionListener(e -> {
            this.setVisible(false);
            // TODO launch a dialog that lists the curreent requirements and allows the user to remove one
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            this.setVisible(true);
        });
        btnAddCategory.addActionListener(e -> {
            this.setVisible(false);
            // TODO launch a dialog that lists the categories and allows the user to pick x
            String text = buildRequirementText(gameYear, factions, lifePaths, categories, attributes, traits, skills,
                  spas);
            txtRequirements.setText(text);
            this.setVisible(true);
        });

        // Add panels and then add Tab
        requirementGroupPanel.add(buttonsPanel, BorderLayout.NORTH);
        requirementGroupPanel.add(pnlDisplay, BorderLayout.CENTER);

        int count = tabbedPane.getComponentCount();
        String titleTab = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.labelFormat", count);
        tabbedPane.addTab(titleTab, requirementGroupPanel);
    }

    private static String buildRequirementText(int gameYear, List<Faction> factions, List<LifePathRecord> lifePaths,
          Map<LifePathCategory, Integer> categories, Map<SkillAttribute, Integer> attributes,
          Map<LifePathEntryDataTraitLookup, Integer> traits, Map<String, Integer> skills, PersonnelOptions spas) {
        StringBuilder progressText = new StringBuilder();

        // Factions
        if (!factions.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.factions");
            progressText.append(title);
            for (int i = 0; i < factions.size(); i++) {
                Faction faction = factions.get(i);
                progressText.append(faction.getFullName(gameYear));
                if (i != factions.size() - 1) {
                    progressText.append(", ");
                }
            }
        }

        // Life Paths
        if (!lifePaths.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.lifePaths");
            progressText.append(title);
            for (int i = 0; i < lifePaths.size(); i++) {
                LifePathRecord lifePath = lifePaths.get(i);
                progressText.append(lifePath.name());
                if (i != lifePaths.size() - 1) {
                    progressText.append(", ");
                }
            }
        }

        // Categories
        if (!categories.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.categories");
            progressText.append(title);

            int counter = 0;
            int length = categories.size();
            for (Map.Entry<LifePathCategory, Integer> entry : categories.entrySet()) {
                progressText.append("<b>");
                progressText.append(entry.getKey().getDisplayName());
                progressText.append(":</b> ");
                progressText.append(entry.getValue());
                progressText.append('+');
                counter++;
                if (counter != length) {
                    progressText.append("<br>");
                }
            }
        }

        // Attributes
        if (!attributes.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.attributes");
            progressText.append(title);

            int counter = 0;
            int length = attributes.size();
            for (Map.Entry<SkillAttribute, Integer> entry : attributes.entrySet()) {
                progressText.append("<b>");
                progressText.append(entry.getKey().getLabel());
                progressText.append(":</b> ");
                progressText.append(entry.getValue());
                progressText.append('+');
                counter++;
                if (counter != length) {
                    progressText.append("<br>");
                }
            }
        }

        // Traits
        if (!traits.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.traits");
            progressText.append(title);

            int counter = 0;
            int length = traits.size();
            for (Map.Entry<LifePathEntryDataTraitLookup, Integer> entry : traits.entrySet()) {
                progressText.append("<b>");
                progressText.append(entry.getKey().getDisplayName());
                progressText.append(":</b> ");
                progressText.append(entry.getValue());
                progressText.append('+');
                counter++;
                if (counter != length) {
                    progressText.append("<br>");
                }
            }
        }

        // Skills
        if (!skills.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.skills");
            progressText.append(title);

            int counter = 0;
            int length = traits.size();
            for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                progressText.append("<b>");
                progressText.append(entry.getKey());
                progressText.append(":</b> ");
                progressText.append(entry.getValue());
                progressText.append('+');
                counter++;
                if (counter != length) {
                    progressText.append("<br>");
                }
            }
        }

        // SPAs
        List<String> selectedSPAs = new ArrayList<>();
        for (final Enumeration<IOptionGroup> i = spas.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PersonnelOptions.LVL3_ADVANTAGES)) {
                continue;
            }

            for (final Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();
                String name = option.getName();
                if (spas.booleanOption(name)) {
                    selectedSPAs.add(option.getDisplayableName());
                }
            }
        }

        if (!selectedSPAs.isEmpty()) {
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.panel.spas");
            progressText.append(title);

            for (int i = 0; i < selectedSPAs.size(); i++) {
                String spa = selectedSPAs.get(i);
                progressText.append(spa);
                if (i != selectedSPAs.size() - 1) {
                    progressText.append(", ");
                }
            }
        }

        return progressText.toString();
    }

    private JPanel initializeProgressPanel() {
        JPanel pnlProgress = new JPanel(new BorderLayout());

        String titleProgress = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.title.progress");
        pnlProgress.setBorder(createRoundedLineBorder(titleProgress));

        txtProgressBasic = new JEditorPane();
        txtProgressBasic.setContentType("text/html");
        txtProgressBasic.setEditable(false);
        updateTxtProgressBasic();

        scrollProgress = new FastJScrollPane(txtProgressBasic);
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
        pnlButtons.add(btnLoad);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));
        pnlButtons.add(btnSave);
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
