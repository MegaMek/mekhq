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
import static mekhq.MHQConstants.LIFE_PATHS_DIRECTORY_PATH;
import static mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder.createRoundedLineBorder;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.spaUtilities.SpaUtilities.getSpaCategory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.EnhancedTabbedPane;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import megamek.utilities.FastJScrollPane;
import mekhq.MekHQ;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePath;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathComponentStorage;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathProgressTextBuilder;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathXPCostCalculator;
import mekhq.gui.GUI;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.io.FileType;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;

public class LifePathBuilderDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(LifePathBuilderDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathBuilderDialog";

    private static final int MINIMUM_SIDE_COMPONENT_WIDTH = scaleForGUI(250);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(550);
    private static final Dimension SIDE_PANEL_MINIMUM_SIZE = new Dimension(MINIMUM_SIDE_COMPONENT_WIDTH,
          MINIMUM_COMPONENT_HEIGHT);
    private static final Dimension MINIMUM_SIZE = new Dimension(MINIMUM_SIDE_COMPONENT_WIDTH * 3,
          MINIMUM_COMPONENT_HEIGHT);
    private static final Dimension PREFERRED_SIZE = new Dimension(MINIMUM_SIDE_COMPONENT_WIDTH * 5,
          MINIMUM_COMPONENT_HEIGHT);
    private static final int PADDING = scaleForGUI(10);

    private static final int TOOLTIP_PANEL_WIDTH = (int) round(MINIMUM_SIZE.width * 0.95);
    private static final int TEXT_PANEL_WIDTH = (int) round(MINIMUM_SIDE_COMPONENT_WIDTH * 0.85);
    private static final String PANEL_HTML_FORMAT = "<html><div style='width:%dpx;'>%s</div></html>";

    private FastJScrollPane scrollInstructions;
    private JEditorPane txtInstructions;
    private JLabel lblTooltipDisplay;
    private FastJScrollPane scrollProgress;
    private JEditorPane txtProgress;
    private JPanel pnlInstructions;
    private JPanel pnlProgress;

    private LifePathBuilderTabBasicInformation basicInfoTab;
    private LifePathTab requirementsTab;
    private LifePathBuilderTabExclusions exclusionsTab;
    private LifePathBuilderTabFixedXP fixedXPTab;
    private LifePathBuilderTabFlexibleXP flexibleXPTab;

    private UUID lifePathId = UUID.randomUUID();
    private final List<String> level3Abilities = new ArrayList<>();
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo = new HashMap<>();

    public static int getTextPanelWidth() {
        return TEXT_PANEL_WIDTH;
    }

    static String getLifePathBuilderResourceBundle() {
        return RESOURCE_BUNDLE;
    }

    static int getLifePathBuilderMinimumComponentWidth() {
        return MINIMUM_SIDE_COMPONENT_WIDTH;
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
        setPreferredSize(PREFERRED_SIZE);
        setSize(PREFERRED_SIZE);
        setLocationRelativeTo(owner);
        setPreferences(); // Must be before setVisible
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

    void updateTxtProgress(int gameYear) {
        txtProgress.setText(LifePathProgressTextBuilder.getProgressText(gameYear, basicInfoTab, requirementsTab,
              exclusionsTab, fixedXPTab, flexibleXPTab));
    }

    private JPanel initialize(int gameYear) {
        buildAllAbilityInfo();

        pnlInstructions = initializeInstructionsPanel();
        EnhancedTabbedPane tabMain = initializeMainPanel(gameYear);
        pnlProgress = initializeProgressPanel(gameYear);

        // Layout using GridBagLayout for a width ratio of 1:2:1
        JPanel container = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weighty = 1.0;

        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 0.25; // Instructions panel (1x width)
        gridBagConstraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        container.add(pnlInstructions, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 0.5; // Main panel (2x width)
        gridBagConstraints.insets = new Insets(PADDING, 0, PADDING, 0);
        container.add(tabMain, gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.weightx = 0.25; // Progress panel (1x width)
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
        container.add(initializeControlPanel(gameYear), gridBagConstraints);

        return container;
    }

    private void buildAllAbilityInfo() {
        // Remove old data
        allAbilityInfo.clear();
        level3Abilities.clear();

        // Build list of Level 3 abilities
        PersonnelOptions personnelOptions = new PersonnelOptions();
        for (final Enumeration<IOptionGroup> i = personnelOptions.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PersonnelOptions.LVL3_ADVANTAGES)) {
                continue;
            }

            for (final Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();
                level3Abilities.add(option.getName());
            }
        }

        // Build abilities
        buildAbilityInfo(SpecialAbility.getSpecialAbilities(), true);

        Map<String, SpecialAbility> allSpecialAbilities = SpecialAbility.getDefaultSpecialAbilities();
        Map<String, SpecialAbility> missingAbilities = new HashMap<>();

        for (SpecialAbility ability : allSpecialAbilities.values()) {
            if (!allAbilityInfo.containsKey(ability.getName())) {
                missingAbilities.put(ability.getName(), ability);
            }
        }

        if (!missingAbilities.isEmpty()) {
            buildAbilityInfo(missingAbilities, false);
        }
    }

    private void buildAbilityInfo(Map<String, SpecialAbility> abilities, boolean isEnabled) {
        for (Map.Entry<String, SpecialAbility> entry : abilities.entrySet()) {
            SpecialAbility clonedAbility = entry.getValue().clone();
            String abilityName = clonedAbility.getName();
            AbilityCategory category = getSpaCategory(clonedAbility);

            if (!level3Abilities.contains(abilityName)) {
                continue;
            }

            // Mark the ability as active
            allAbilityInfo.put(abilityName,
                  new CampaignOptionsAbilityInfo(abilityName, clonedAbility, isEnabled, category));
        }
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

        basicInfoTab = new LifePathBuilderTabBasicInformation(this, tabMain, gameYear);
        fixedXPTab = new LifePathBuilderTabFixedXP(this, tabMain, gameYear, allAbilityInfo);
        flexibleXPTab = new LifePathBuilderTabFlexibleXP(this, tabMain, gameYear, allAbilityInfo);
        requirementsTab = new LifePathTab(this, tabMain, gameYear, allAbilityInfo);
        requirementsTab.buildTab(true);
        exclusionsTab = new LifePathBuilderTabExclusions(this, tabMain, gameYear, allAbilityInfo);

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

    private JPanel initializeProgressPanel(int gameYear) {
        JPanel pnlProgress = new JPanel(new BorderLayout());

        String titleProgress = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.title.progress");
        pnlProgress.setBorder(createRoundedLineBorder(titleProgress));

        txtProgress = new JEditorPane();
        txtProgress.setContentType("text/html");
        txtProgress.setEditable(false);
        updateTxtProgress(gameYear);

        scrollProgress = new FastJScrollPane(txtProgress);
        scrollProgress.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollProgress.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollProgress.setBorder(null);

        pnlProgress.add(scrollProgress, BorderLayout.CENTER);

        pnlProgress.setMinimumSize(SIDE_PANEL_MINIMUM_SIZE);

        return pnlProgress;
    }

    private JPanel initializeControlPanel(int gameYear) {
        JPanel pnlControls = new JPanel(new GridBagLayout());
        pnlControls.setBorder(RoundedLineBorder.createRoundedLineBorder());

        JPanel pnlContents = new JPanel();
        pnlContents.setLayout(new BoxLayout(pnlContents, BoxLayout.Y_AXIS));
        pnlContents.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblTooltipDisplay = new JLabel();
        lblTooltipDisplay.setBorder(new EmptyBorder(0, PADDING, 0, PADDING));
        setLblTooltipDisplay("");
        lblTooltipDisplay.setAlignmentX(Component.LEFT_ALIGNMENT);

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
        btnSave.addActionListener(e -> {
            LifePath record = buildLifePathFromBuilderWizard();
            writeToJSONWithDialog(record);
        });

        String titleLoad = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.load");
        RoundedJButton btnLoad = new RoundedJButton(titleLoad);
        btnLoad.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLoad.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnLoad.addActionListener(e -> {
            LifePath record = loadFromJSONWithDialog().orElse(null);
            if (record != null) {
                lifePathId = record.id();
                LOGGER.debug("Loaded lifePathId from: {}", record.id());

                basicInfoTab.setSource(record.source());
                LOGGER.debug("Loaded source from: {}", record.source());

                basicInfoTab.setName(record.name());
                LOGGER.debug("Loaded name from: {}", record.name());

                basicInfoTab.setFlavorText(record.flavorText());
                LOGGER.debug("Loaded flavorText from: {}", record.flavorText());

                basicInfoTab.setAge(record.age());
                LOGGER.debug("Loaded age from: {}", record.age());

                basicInfoTab.setDiscount(record.xpDiscount());
                LOGGER.debug("Loaded xpDiscount from: {}", record.xpDiscount());

                basicInfoTab.setLifeStages(record.lifeStages());
                LOGGER.debug("Loaded lifeStages from: {}", record.lifeStages());

                basicInfoTab.setCategories(record.categories());
                LOGGER.debug("Loaded categories from: {}", record.categories());

                requirementsTab.updateTabStorage(record.requirements());
                LOGGER.debug("Loaded requirements from: {}", record.requirements());

                exclusionsTab.setExclusionsTabStorage(record.exclusions());
                LOGGER.debug("Loaded exclusions from: {}", record.exclusions());

                fixedXPTab.setFixedXPTabStorage(record.fixedXpAwards());
                LOGGER.debug("Loaded fixedXpAwards from: {}", record.fixedXpAwards());

                flexibleXPTab.setFlexibleXPTabStorageMap(record.flexibleXpAwards());
                LOGGER.debug("Loaded flexibleXpAwards from: {}", record.flexibleXpAwards());

                updateTxtProgress(gameYear);
                invalidate();
                repaint();
            }
        });

        String titleToggleInstructions = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.toggleInstructions");
        RoundedJButton btnToggleInstructions = new RoundedJButton(titleToggleInstructions);
        btnToggleInstructions.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnToggleInstructions.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnToggleInstructions.addActionListener(e -> pnlInstructions.setVisible(!pnlInstructions.isVisible()));

        String titleToggleProgress = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.toggleProgress");
        RoundedJButton btnToggleProgress = new RoundedJButton(titleToggleProgress);
        btnToggleProgress.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnToggleProgress.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnToggleProgress.addActionListener(e -> pnlProgress.setVisible(!pnlProgress.isVisible()));

        pnlButtons.add(Box.createHorizontalGlue());
        pnlButtons.add(btnToggleInstructions);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));
        pnlButtons.add(btnCancel);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));
        pnlButtons.add(btnSave);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));
        pnlButtons.add(btnLoad);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));
        pnlButtons.add(btnToggleProgress);
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

    public static void writeToJSONWithDialog(LifePath record) {
        String baseName = record.name().replace(" ", "_");
        if (baseName.isBlank()) {
            baseName = "unnamed_life_path";
        }

        // Pick an initial directory (preferably the user directory or fallback)
        String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
        if (userDirectory == null || userDirectory.isBlank()) {
            userDirectory = LIFE_PATHS_DIRECTORY_PATH;
        }

        Optional<File> dialogFile = GUI.fileDialogSave(
              null,
              getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.io.save"),
              FileType.JSON,
              userDirectory,
              baseName
        );

        if (dialogFile.isPresent()) {
            File file = dialogFile.get();
            // Ensure it ends with ".json"
            String name = file.getName();
            if (!name.toLowerCase().endsWith(".json")) {
                file = new File(file.getParent(), name + ".json");
            }
            // Write the record
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
                objectMapper.writeValue(file, record);
                LOGGER.info("Wrote LifePathRecord JSON to: {}", file.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        } else {
            LOGGER.info("Save operation cancelled by user.");
        }
    }

    public static Optional<LifePath> loadFromJSONWithDialog() {
        String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
        if (userDirectory == null || userDirectory.isBlank()) {
            userDirectory = LIFE_PATHS_DIRECTORY_PATH;
        }

        Optional<File> fileOpt = GUI.fileDialogOpen(
              null,
              getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.io.load"),
              FileType.JSON,
              userDirectory
        );

        if (fileOpt.isPresent()) {
            File file = fileOpt.get();
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                LifePath record = objectMapper.readValue(file, LifePath.class);
                LOGGER.info("Loaded LifePathRecord from: {}", file.getAbsolutePath());
                return Optional.of(record);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        } else {
            LOGGER.info("Load operation cancelled by user.");
        }

        return Optional.empty();
    }

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(LifePathBuilderDialog.class);
            this.setName("LifePathBuilderDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    private LifePath buildLifePathFromBuilderWizard() {
        int discount = basicInfoTab.getDiscount();
        LifePathComponentStorage fixedXPTabStorage = fixedXPTab.getFixedXPTabStorage();
        Map<Integer, LifePathComponentStorage> flexibleXPTabStorage = flexibleXPTab.getFlexibleXPTabStorageMap();

        int xpCost = LifePathXPCostCalculator.calculateXPCost(discount, fixedXPTabStorage, flexibleXPTabStorage);

        //        return new LifePath(lifePathId, basicInfoTab.getSource(), MHQConstants.VERSION, basicInfoTab.getName(),
        //              basicInfoTab.getFlavorText(), basicInfoTab.getAge(), basicInfoTab.getDiscount(), xpCost,
        //              basicInfoTab.getLifeStages(), basicInfoTab.getCategories(),
        //              requirementsTab.getRequirementsTabStorageMap(), exclusionsTab.getExclusionsTabStorage(),
        //              fixedXPTabStorage, flexibleXPTabStorage, flexibleXPTab.getPickCount());
        return null;
    }
}
