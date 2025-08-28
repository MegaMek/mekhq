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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.MHQConstants.LIFE_PATHS_DEFAULT_DIRECTORY_PATH;
import static mekhq.MHQConstants.LIFE_PATHS_USER_DIRECTORY_PATH;
import static mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder.createRoundedLineBorder;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;
import static mekhq.utilities.spaUtilities.SpaUtilities.getSpaCategory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import megamek.Version;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.EnhancedTabbedPane;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import megamek.utilities.FastJScrollPane;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.*;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.gui.GUI;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.io.FileType;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;

public class LifePathBuilderDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(LifePathBuilderDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathBuilderDialog";

    private static final int MINIMUM_SIDE_COMPONENT_WIDTH = scaleForGUI(250);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(550);
    private static final Dimension PREFERRED_SIZE = new Dimension(MINIMUM_SIDE_COMPONENT_WIDTH * 4,
          MINIMUM_COMPONENT_HEIGHT);
    private static final int PADDING = scaleForGUI(10);

    private FastJScrollPane scrollInstructions;
    private JEditorPane txtInstructions;
    private JEditorPane txtTooltipArea;
    private FastJScrollPane scrollProgress;
    private JEditorPane txtProgress;
    private JPanel pnlInstructions;
    private JPanel pnlProgress;

    private UUID lifePathId = UUID.randomUUID();
    private LifePathTabBasicInformation basicInfoTab;
    private LifePathTab requirementsTab;
    private LifePathTab exclusionsTab;
    private LifePathTab fixedXPTab;
    private LifePathTab flexibleXPTab;

    private final Campaign campaign;
    private final List<String> level3Abilities = new ArrayList<>();
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo = new HashMap<>();

    static String getLifePathBuilderResourceBundle() {
        return RESOURCE_BUNDLE;
    }

    static int getLifePathBuilderMinimumComponentWidth() {
        return MINIMUM_SIDE_COMPONENT_WIDTH;
    }

    static int getLifePathBuilderPadding() {
        return PADDING;
    }

    public LifePathBuilderDialog(Campaign campaign, Frame owner, int gameYear) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.title"), true);
        this.campaign = campaign;

        JPanel contents = initialize(gameYear);

        SwingUtilities.invokeLater(() -> scrollInstructions.getVerticalScrollBar().setValue(0));
        SwingUtilities.invokeLater(() -> scrollProgress.getVerticalScrollBar().setValue(0));

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performDialogCloseAction(); // includes dispose() call
            }
        });
        setContentPane(contents);
        setPreferredSize(PREFERRED_SIZE);
        setSize(PREFERRED_SIZE);
        setLocationRelativeTo(owner);
        setPreferences(); // Must be before setVisible
        setVisible(true);
    }

    private void setTxtInstructions(String newTooltipText) {
        txtInstructions.setText(newTooltipText);
        SwingUtilities.invokeLater(() -> scrollInstructions.getVerticalScrollBar().setValue(0));
    }

    public void setTxtTooltipArea(String newText) {
        txtTooltipArea.setText("<div style='text-align:center;'>" + newText + "</div>");
    }

    void updateTxtProgress() {
        txtProgress.setText(LifePathProgressTextBuilder.getProgressText(basicInfoTab, requirementsTab,
              exclusionsTab, fixedXPTab, flexibleXPTab));
    }

    private JPanel initialize(int gameYear) {
        buildAllAbilityInfo();

        pnlInstructions = initializeInstructionsPanel();
        EnhancedTabbedPane tabMain = initializeMainPanel(gameYear);
        pnlProgress = initializeProgressPanel();

        // Layout using GridBagLayout for a width ratio of 1:2:1
        JPanel container = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weighty = 1.0;

        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 0.50;
        gridBagConstraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        container.add(pnlInstructions, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 0.30;
        gridBagConstraints.insets = new Insets(PADDING, 0, PADDING, 0);
        container.add(tabMain, gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.weightx = 0.50;
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
        JPanel pnlInstructions = new JPanel(new BorderLayout());

        // Border
        String titleInstructions = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.title.instructions");
        pnlInstructions.setBorder(createRoundedLineBorder(titleInstructions));

        // Text Area
        txtInstructions = new JEditorPane();
        txtInstructions.setContentType("text/html");
        txtInstructions.setEditable(false);
        String instructions = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.instructions.basic");
        txtInstructions.setText(instructions);

        scrollInstructions = new FastJScrollPane(txtInstructions);
        scrollInstructions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollInstructions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setBorder(null);

        // Final Touches
        pnlInstructions.add(scrollInstructions, BorderLayout.CENTER);

        return pnlInstructions;
    }

    private EnhancedTabbedPane initializeMainPanel(int gameYear) {
        EnhancedTabbedPane tabMain = new EnhancedTabbedPane();
        String title = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.title.lifePath");
        tabMain.setBorder(createRoundedLineBorder(title));

        basicInfoTab = new LifePathTabBasicInformation(this, tabMain);

        fixedXPTab = new LifePathTab(this, tabMain, gameYear, allAbilityInfo, LifePathBuilderTabType.FIXED_XP);
        fixedXPTab.buildTab();

        flexibleXPTab = new LifePathTab(this, tabMain, gameYear, allAbilityInfo, LifePathBuilderTabType.FLEXIBLE_XP);
        flexibleXPTab.buildTab();

        requirementsTab = new LifePathTab(this, tabMain, gameYear, allAbilityInfo, LifePathBuilderTabType.REQUIREMENTS);
        requirementsTab.buildTab();

        exclusionsTab = new LifePathTab(this, tabMain, gameYear, allAbilityInfo, LifePathBuilderTabType.EXCLUSIONS);
        exclusionsTab.buildTab();

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
        scrollProgress.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollProgress.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollProgress.setBorder(null);

        pnlProgress.add(scrollProgress, BorderLayout.CENTER);

        return pnlProgress;
    }

    private JPanel initializeControlPanel() {
        JPanel pnlControls = new JPanel(new BorderLayout());
        pnlControls.setBorder(createRoundedLineBorder());

        txtTooltipArea = new JEditorPane();
        txtTooltipArea.setContentType("text/html");
        txtTooltipArea.setEditable(false);
        txtTooltipArea.setBorder(new EmptyBorder(0, PADDING, 0, PADDING));
        setTxtTooltipArea(getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.tootltip.default"));

        FastJScrollPane scrollTooltipArea = new FastJScrollPane(txtTooltipArea);
        scrollTooltipArea.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollTooltipArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollTooltipArea.setBorder(null);
        scrollTooltipArea.setMinimumSize(new Dimension(MINIMUM_SIDE_COMPONENT_WIDTH, 50));

        pnlControls.add(scrollTooltipArea, BorderLayout.CENTER);

        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
        pnlButtons.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        pnlButtons.add(Box.createHorizontalGlue());

        String titleToggleInstructions = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.toggleInstructions");
        RoundedJButton btnToggleInstructions = new RoundedJButton(titleToggleInstructions);
        btnToggleInstructions.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnToggleInstructions.addActionListener(e -> pnlInstructions.setVisible(!pnlInstructions.isVisible()));
        pnlButtons.add(btnToggleInstructions);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));

        String titleCancel = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.cancel");
        RoundedJButton btnCancel = new RoundedJButton(titleCancel);
        btnCancel.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnCancel.addActionListener(e -> performDialogCloseAction());
        pnlButtons.add(btnCancel);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));

        String titleNew = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.new");
        RoundedJButton btnNew = new RoundedJButton(titleNew);
        btnNew.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnNew.addActionListener(e -> newLifePathAction());
        pnlButtons.add(btnNew);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));

        String titleSave = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.save");
        RoundedJButton btnSave = new RoundedJButton(titleSave);
        btnSave.setMargin(new Insets(PADDING, 0, PADDING, 0));
        btnSave.addActionListener(e -> {
            displayIDRegenerationDialogs();
            LifePath record = buildLifePathFromBuilderWizard();
            boolean isValid = validateLifePath(record);
            if (isValid) {
                writeToJSONWithDialog(record);
            }
        });
        pnlButtons.add(btnSave);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));

        String titleLoad = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.load");
        RoundedJButton btnLoad = new RoundedJButton(titleLoad);
        btnLoad.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnLoad.addActionListener(e -> {
            loadFromJSONWithDialog().ifPresent(LifePath -> {
                resetNonBasicTabs();
                updateBuilderFromExistingLifePathRecord(LifePath);
            });
            SwingUtilities.invokeLater(() -> {
                scrollProgress.getVerticalScrollBar().setValue(0);
                scrollProgress.getHorizontalScrollBar().setValue(0);
            });
        });
        pnlButtons.add(btnLoad);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));

        String titleToggleProgress = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.toggleProgress");
        RoundedJButton btnToggleProgress = new RoundedJButton(titleToggleProgress);
        btnToggleProgress.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnToggleProgress.addActionListener(e -> pnlProgress.setVisible(!pnlProgress.isVisible()));
        pnlButtons.add(btnToggleProgress);

        pnlButtons.add(Box.createHorizontalGlue());

        pnlControls.add(pnlButtons, BorderLayout.SOUTH);

        return pnlControls;
    }

    private void newLifePathAction() {
        ImmersiveDialogConfirmation confirmation = new ImmersiveDialogConfirmation(campaign);
        if (!confirmation.wasConfirmed()) {
            return;
        }

        lifePathId = UUID.randomUUID();
        resetBasicTab();
        resetNonBasicTabs();

        fixedXPTab.addTab();
        exclusionsTab.addTab();
    }

    private void resetBasicTab() {
        basicInfoTab.resetTab();
    }

    private void resetNonBasicTabs() {
        fixedXPTab.resetTab();
        flexibleXPTab.resetTab();
        requirementsTab.resetTab();
        exclusionsTab.resetTab();
    }

    private void performDialogCloseAction() {
        Map<UUID, LifePath> lifePaths = LifePathIO.loadAllLifePaths(campaign);
        campaign.setLifePathLibrary(lifePaths);
        dispose();
    }

    private boolean validateLifePath(LifePath record) {
        LifePathValidator validator = new LifePathValidator(record);
        Set<InvalidLifePathReason> invalidReasons = validator.getInvalidReasons();

        if (invalidReasons.isEmpty()) {
            return true;
        }

        StringBuilder invalidText = new StringBuilder(getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.invalid.label"));
        for (InvalidLifePathReason invalidReason : invalidReasons) {
            invalidText.append(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.invalid.format",
                  spanOpeningWithCustomColor(getWarningColor()), invalidReason.getDisplayName(),
                  CLOSING_SPAN_TAG, invalidReason.getDescription()));
        }


        new ImmersiveDialogNotification(campaign, invalidText.toString(), true);
        return false;
    }

    private void displayIDRegenerationDialogs() {
        final int REGENERATE_ID = 1;

        ImmersiveDialogSimple dialog = null;
        boolean choiceConfirmed = false;
        while (!choiceConfirmed) {
            dialog = new ImmersiveDialogSimple(campaign,
                  null,
                  null,
                  getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.warning.label.inCharacter"),
                  List.of(getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.warning.button.decline"),
                        getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.warning.button.confirm")),
                  getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.warning.label.outOfCharacter"),
                  null,
                  false);

            ImmersiveDialogConfirmation confirmationDialog = new ImmersiveDialogConfirmation(campaign);
            choiceConfirmed = confirmationDialog.wasConfirmed();
        }

        if (dialog.getDialogChoice() == REGENERATE_ID) {
            lifePathId = UUID.randomUUID();

            new ImmersiveDialogNotification(campaign,
                  getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.confirmation.label"), true);
        }
    }

    private void updateBuilderFromExistingLifePathRecord(LifePath record) {
        // Dynamic
        lifePathId = record.id();

        // Basic Info
        basicInfoTab.setSource(record.source());
        basicInfoTab.setName(record.name());
        basicInfoTab.setFlavorText(record.flavorText());
        basicInfoTab.setAge(record.age());
        basicInfoTab.setDiscount(record.xpDiscount());
        basicInfoTab.setLifeStages(record.lifeStages());
        basicInfoTab.setCategories(record.categories());
        basicInfoTab.setMinimumYear(record.minimumYear());
        basicInfoTab.setMaximumYear(record.maximumYear());
        basicInfoTab.setPlayerRestricted(record.isPlayerRestricted());
        basicInfoTab.setRandomWeight(record.randomWeight());

        // Requirements
        int requirementsMaxKey = -1;

        requirementsMaxKey = Math.max(requirementsMaxKey, getMaxKey(record.requirementsFactions()));
        requirementsMaxKey = Math.max(requirementsMaxKey, getMaxKey(record.requirementsLifePath()));
        requirementsMaxKey = Math.max(requirementsMaxKey, getMaxKey(record.requirementsCategories()));
        requirementsMaxKey = Math.max(requirementsMaxKey, getMaxKey(record.requirementsAttributes()));
        requirementsMaxKey = Math.max(requirementsMaxKey, getMaxKey(record.requirementsTraits()));
        requirementsMaxKey = Math.max(requirementsMaxKey, getMaxKey(record.requirementsSkills()));
        requirementsMaxKey = Math.max(requirementsMaxKey, getMaxKey(record.requirementsAbilities()));

        // This must be before we set the values below, otherwise the values will be overwritten
        addAdditionalTabsAsNecessary(requirementsMaxKey, requirementsTab);

        if (requirementsMaxKey > -1) {
            requirementsTab.setFactions(record.requirementsFactions());
            requirementsTab.setLifePaths(record.requirementsLifePath());
            requirementsTab.setCategories(record.requirementsCategories());
            requirementsTab.setAttributes(record.requirementsAttributes());
            requirementsTab.setTraits(record.requirementsTraits());
            requirementsTab.setSkills(record.requirementsSkills());
            requirementsTab.setAbilities(record.requirementsAbilities());
        }

        // Conversely, this must be after we set the values above, so the text can be current
        updateProgressTextPerTab(requirementsMaxKey, requirementsTab);

        // Exclusions
        int exclusionsMaxKey = -1;

        exclusionsMaxKey = Math.max(exclusionsMaxKey, getMaxKey(record.exclusionsFactions()));
        exclusionsMaxKey = Math.max(exclusionsMaxKey, getMaxKey(record.exclusionsLifePath()));
        exclusionsMaxKey = Math.max(exclusionsMaxKey, getMaxKey(record.exclusionsCategories()));
        exclusionsMaxKey = Math.max(exclusionsMaxKey, getMaxKey(record.exclusionsAttributes()));
        exclusionsMaxKey = Math.max(exclusionsMaxKey, getMaxKey(record.exclusionsTraits()));
        exclusionsMaxKey = Math.max(exclusionsMaxKey, getMaxKey(record.exclusionsSkills()));
        exclusionsMaxKey = Math.max(exclusionsMaxKey, getMaxKey(record.exclusionsAbilities()));

        // This must be before we set the values below, otherwise the values will be overwritten
        addAdditionalTabsAsNecessary(exclusionsMaxKey, exclusionsTab);

        if (exclusionsMaxKey > -1) {
            exclusionsTab.setFactions(record.exclusionsFactions());
            exclusionsTab.setLifePaths(record.exclusionsLifePath());
            exclusionsTab.setCategories(record.exclusionsCategories());
            exclusionsTab.setAttributes(record.exclusionsAttributes());
            exclusionsTab.setTraits(record.exclusionsTraits());
            exclusionsTab.setSkills(record.exclusionsSkills());
            exclusionsTab.setAbilities(record.exclusionsAbilities());
        }

        // Conversely, this must be after we set the values above, so the text can be current
        updateProgressTextPerTab(exclusionsMaxKey, exclusionsTab);

        // Fixed XP
        int fixedXPMaxKey = -1;

        fixedXPMaxKey = Math.max(fixedXPMaxKey, getMaxKey(record.fixedXPAttributes()));
        fixedXPMaxKey = Math.max(fixedXPMaxKey, getMaxKey(record.fixedXPTraits()));
        fixedXPMaxKey = Math.max(fixedXPMaxKey, getMaxKey(record.fixedXPSkills()));
        fixedXPMaxKey = Math.max(fixedXPMaxKey, getMaxKey(record.fixedXPAbilities()));

        // This must be before we set the values below, otherwise the values will be overwritten
        addAdditionalTabsAsNecessary(fixedXPMaxKey, fixedXPTab);

        if (fixedXPMaxKey > -1) {
            fixedXPTab.setAttributes(record.fixedXPAttributes());
            fixedXPTab.setTraits(record.fixedXPTraits());
            fixedXPTab.setSkills(record.fixedXPSkills());
            fixedXPTab.setAbilities(record.fixedXPAbilities());
        }

        // Conversely, this must be after we set the values above, so the text can be current
        updateProgressTextPerTab(fixedXPMaxKey, fixedXPTab);

        // Flexible XP
        int flexibleXPMaxKey = -1;

        flexibleXPMaxKey = Math.max(flexibleXPMaxKey, getMaxKey(record.flexibleXPAttributes()));
        flexibleXPMaxKey = Math.max(flexibleXPMaxKey, getMaxKey(record.flexibleXPTraits()));
        flexibleXPMaxKey = Math.max(flexibleXPMaxKey, getMaxKey(record.flexibleXPSkills()));
        flexibleXPMaxKey = Math.max(flexibleXPMaxKey, getMaxKey(record.flexibleXPAbilities()));

        // This must be before we set the values below, otherwise the values will be overwritten
        addAdditionalTabsAsNecessary(flexibleXPMaxKey, flexibleXPTab);

        if (flexibleXPMaxKey > -1) {
            flexibleXPTab.setAttributes(record.flexibleXPAttributes());
            flexibleXPTab.setTraits(record.flexibleXPTraits());
            flexibleXPTab.setSkills(record.flexibleXPSkills());
            flexibleXPTab.setAbilities(record.flexibleXPAbilities());
        }

        flexibleXPTab.setPickCount(record.flexibleXPPickCount());

        // Conversely, this must be after we set the values above, so the text can be current
        updateProgressTextPerTab(flexibleXPMaxKey, flexibleXPTab);

        updateTxtProgress();
    }

    private void addAdditionalTabsAsNecessary(int requirementsMaxKey, LifePathTab requirementsTab) {
        for (int i = -1; i < requirementsMaxKey; i++) {
            requirementsTab.addTab();
        }
    }

    private void updateProgressTextPerTab(int maxKey, LifePathTab lifePathTab) {
        for (int i = 0; i <= maxKey; i++) {
            EnhancedTabbedPane localTab = lifePathTab.getLocalTab();
            JPanel pnlNewTab = (JPanel) localTab.getComponentAt(i);
            JPanel pnlMain = (JPanel) pnlNewTab.getComponent(1);
            JEditorPane editorProgress = lifePathTab.findEditorPaneByName(pnlMain, "editorProgress");
            if (editorProgress != null) {
                editorProgress.setText(lifePathTab.buildIndividualProgressText(i).toString());
            } else {
                LOGGER.warn("Could not find editorProgress in updateBuilderFromExistingLifePathRecord");
            }
        }
    }

    private static int getMaxKey(Map<Integer, ?> map) {
        if (map == null || map.isEmpty()) {
            return Integer.MIN_VALUE;
        }

        return Collections.max(map.keySet());
    }

    public static void writeToJSONWithDialog(LifePath record) {
        String baseName = record.name();
        if (baseName.isBlank()) {
            baseName = "unnamed_life_path";
        } else {
            baseName = baseName.replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "");
            baseName = baseName.replaceAll("[. ]+$", "");
            baseName = baseName.replaceAll("^_|_$", "");
        }

        // Pick an initial directory (preferably the user directory or fallback)
        String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
        if (userDirectory == null || userDirectory.isBlank()) {
            userDirectory = LIFE_PATHS_DEFAULT_DIRECTORY_PATH;
        } else {
            userDirectory = userDirectory + LIFE_PATHS_USER_DIRECTORY_PATH;
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
            userDirectory = LIFE_PATHS_DEFAULT_DIRECTORY_PATH;
        } else {
            userDirectory = Paths.get(userDirectory, LIFE_PATHS_USER_DIRECTORY_PATH).toString();
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
        // Basic Info
        String source = basicInfoTab.getSource();
        String name = basicInfoTab.getName();
        String flavorText = basicInfoTab.getFlavorText();
        int age = basicInfoTab.getAge();
        int xpDiscount = basicInfoTab.getDiscount();
        int minimumYear = basicInfoTab.getMinimumYear();
        int maximumYear = basicInfoTab.getMaximumYear();
        double randomWeight = basicInfoTab.getRandomWeight();
        List<ATOWLifeStage> lifeStages = basicInfoTab.getLifeStages();
        List<LifePathCategory> categories = basicInfoTab.getCategories();
        boolean isPlayerRestricted = basicInfoTab.isPlayerRestricted();

        // Requirements
        Map<Integer, List<String>> requirementsFactions = requirementsTab.getFactions();
        Map<Integer, List<UUID>> requirementsLifePath = requirementsTab.getLifePaths();
        Map<Integer, Map<LifePathCategory, Integer>> requirementsCategories = requirementsTab.getCategories();
        Map<Integer, Map<SkillAttribute, Integer>> requirementsAttributes = requirementsTab.getAttributes();
        Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> requirementsTraits = requirementsTab.getTraits();
        Map<Integer, Map<String, Integer>> requirementsSkills = requirementsTab.getSkills();
        Map<Integer, Map<String, Integer>> requirementsAbilities = requirementsTab.getAbilities();

        // Exclusions
        Map<Integer, List<String>> exclusionsFactions = exclusionsTab.getFactions();
        Map<Integer, List<UUID>> exclusionsLifePath = exclusionsTab.getLifePaths();
        Map<Integer, Map<LifePathCategory, Integer>> exclusionsCategories = exclusionsTab.getCategories();
        Map<Integer, Map<SkillAttribute, Integer>> exclusionsAttributes = exclusionsTab.getAttributes();
        Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> exclusionsTraits = exclusionsTab.getTraits();
        Map<Integer, Map<String, Integer>> exclusionsSkills = exclusionsTab.getSkills();
        Map<Integer, Map<String, Integer>> exclusionsAbilities = exclusionsTab.getAbilities();

        // Fixed XP
        Map<Integer, Map<SkillAttribute, Integer>> fixedXPAttributes = fixedXPTab.getAttributes();
        Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> fixedXPTraits = fixedXPTab.getTraits();
        Map<Integer, Map<String, Integer>> fixedXPSkills = fixedXPTab.getSkills();
        Map<Integer, Map<String, Integer>> fixedXPAbilities = fixedXPTab.getAbilities();

        // Flexible XP
        Map<Integer, Map<SkillAttribute, Integer>> flexibleXPAttributes = flexibleXPTab.getAttributes();
        Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> flexibleXPTraits = flexibleXPTab.getTraits();
        Map<Integer, Map<String, Integer>> flexibleXPSkills = flexibleXPTab.getSkills();
        Map<Integer, Map<String, Integer>> flexibleXPAbilities = flexibleXPTab.getAbilities();
        int flexibleXPPickCount = flexibleXPTab.getPickCount();

        // Dynamic
        UUID id = lifePathId;
        Version version = MHQConstants.VERSION;
        int xpCost = LifePathXPCostCalculator.calculateXPCost(xpDiscount, fixedXPAttributes, fixedXPTraits,
              fixedXPSkills, fixedXPAbilities, flexibleXPTab.getTabCount(), flexibleXPPickCount, flexibleXPAttributes,
              flexibleXPTraits, flexibleXPSkills, flexibleXPAbilities);

        // Build and return the Record
        return new LifePath(id, version, xpCost, source, name, flavorText, age, xpDiscount, minimumYear, maximumYear,
              randomWeight, lifeStages, categories, isPlayerRestricted, requirementsFactions, requirementsLifePath,
              requirementsCategories, requirementsAttributes, requirementsTraits, requirementsSkills,
              requirementsAbilities, exclusionsFactions, exclusionsLifePath, exclusionsCategories,
              exclusionsAttributes, exclusionsTraits, exclusionsSkills, exclusionsAbilities, fixedXPAttributes,
              fixedXPTraits, fixedXPSkills, fixedXPAbilities, flexibleXPAttributes, flexibleXPTraits,
              flexibleXPSkills, flexibleXPAbilities, flexibleXPPickCount);
    }
}
