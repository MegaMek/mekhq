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
import static mekhq.MHQConstants.CONFIRMATION_NEW_LIFE_PATH;
import static mekhq.MHQConstants.CONFIRMATION_REGEN_PATH_ID;
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
import java.time.LocalDate;
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

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePath;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilder;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathIO;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathProgressTextBuilder;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathValidator;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathXPCostCalculator;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
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

    /**
     * Whether the path currently in the wizard was read from a file.
     *
     * <p>Only such a path has an id worth keeping or replacing, so this gates the "regenerate unique id?" question
     * on save.</p>
     */
    private boolean loadedFromFile = false;

    /**
     * Whether anything has been edited since the Life Path was last saved, loaded or started fresh.
     *
     * <p>Gates the prompt on close. Closing used to discard an afternoon's work without a word.</p>
     */
    private boolean hasUnsavedChanges = false;

    /**
     * Suppresses dirty tracking while the wizard is populating itself.
     *
     * <p>Loading a file fires the same change listeners an author's typing does, so without this a freshly loaded
     * Life Path would immediately count as edited.</p>
     */
    private boolean isPopulating = false;
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

    public LifePathBuilderDialog(Campaign campaign, Frame owner) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.title"), true);
        this.campaign = campaign;

        JPanel contents = initialize(campaign.getLocalDate());

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
        basicInfoTab.setLifePathId(lifePathId);
        hasUnsavedChanges = false;

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

    /**
     * Rebuilds the progress panel, and notes that the Life Path now differs from what is on disk.
     *
     * <p>Every control in the wizard routes its changes through here, which makes it the one place that knows an
     * edit happened.</p>
     *
     * @since 0.50.11
     */
    /**
     * Returns the identifier of the Life Path currently in the wizard.
     *
     * <p>Needed by the Life Path picker, which must not offer the path being edited as something that path requires
     * or excludes.</p>
     *
     * @return the current Life Path's identifier
     *
     * @since 0.50.11
     */
    UUID getLifePathId() {
        return lifePathId;
    }

    void updateTxtProgress() {
        if (!isPopulating) {
            hasUnsavedChanges = true;
        }

        txtProgress.setText(buildValidationSummary() +
                                  LifePathProgressTextBuilder.getProgressText(basicInfoTab,
                                        requirementsTab,
                                        exclusionsTab,
                                        fixedXPTab,
                                        flexibleXPTab));
    }

    /**
     * Returns the list of current problems, ready to sit above the progress text.
     *
     * <p>Shown while editing rather than only on save, so an author finds out about a missing faction or an
     * impossible pick count when they cause it.</p>
     *
     * @return the problems as HTML, or an empty string when there are none
     *
     * @since 0.50.11
     */
    private String buildValidationSummary() {
        Set<InvalidLifePathReason> invalidReasons = LifePathValidator.validate(readWizardIntoBuilder());

        if (invalidReasons.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder(getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.invalid.label"));

        for (InvalidLifePathReason invalidReason : invalidReasons) {
            summary.append(getFormattedTextAt(RESOURCE_BUNDLE,
                  "LifePathBuilderDialog.invalid.format",
                  spanOpeningWithCustomColor(getWarningColor()),
                  invalidReason.getDisplayName(),
                  CLOSING_SPAN_TAG,
                  invalidReason.getDescription()));
        }

        return summary.toString();
    }

    private JPanel initialize(LocalDate today) {
        buildAllAbilityInfo();

        pnlInstructions = initializeInstructionsPanel();
        EnhancedTabbedPane tabMain = initializeMainPanel(today);
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

    private EnhancedTabbedPane initializeMainPanel(LocalDate today) {
        Map<UUID, LifePath> lifePathLibrary = campaign.getLifePathLibrary();

        EnhancedTabbedPane tabMain = new EnhancedTabbedPane();
        String title = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.title.lifePath");
        tabMain.setBorder(createRoundedLineBorder(title));

        basicInfoTab = new LifePathTabBasicInformation(this, tabMain);

        fixedXPTab = new LifePathTab(this, tabMain, today, allAbilityInfo, LifePathBuilderTabType.FIXED_XP,
              lifePathLibrary);
        fixedXPTab.buildTab();

        flexibleXPTab = new LifePathTab(this, tabMain, today, allAbilityInfo, LifePathBuilderTabType.FLEXIBLE_XP,
              lifePathLibrary);
        flexibleXPTab.buildTab();

        requirementsTab = new LifePathTab(this, tabMain, today, allAbilityInfo, LifePathBuilderTabType.REQUIREMENTS,
              lifePathLibrary);
        requirementsTab.buildTab();

        exclusionsTab = new LifePathTab(this, tabMain, today, allAbilityInfo, LifePathBuilderTabType.EXCLUSIONS,
              lifePathLibrary);
        exclusionsTab.buildTab();

        // Add a listener to handle tab selection changes
        tabMain.addChangeListener(changeEvent -> {
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
        setTxtTooltipArea(getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.tooltip.default"));

        FastJScrollPane scrollTooltipArea = new FastJScrollPane(txtTooltipArea);
        scrollTooltipArea.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollTooltipArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollTooltipArea.setBorder(null);
        scrollTooltipArea.setMinimumSize(scaleForGUI(250, 50));

        pnlControls.add(scrollTooltipArea, BorderLayout.CENTER);

        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
        pnlButtons.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        pnlButtons.add(Box.createHorizontalGlue());

        String titleToggleInstructions = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.toggleInstructions");
        RoundedJButton btnToggleInstructions = new RoundedJButton(titleToggleInstructions);
        btnToggleInstructions.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnToggleInstructions.addActionListener(actionEvent -> pnlInstructions.setVisible(!pnlInstructions.isVisible()));
        pnlButtons.add(btnToggleInstructions);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));

        String titleCancel = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.close");
        RoundedJButton btnClose = new RoundedJButton(titleCancel);
        btnClose.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnClose.addActionListener(actionEvent -> performDialogCloseAction());
        pnlButtons.add(btnClose);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));

        String titleNew = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.new");
        RoundedJButton btnNew = new RoundedJButton(titleNew);
        btnNew.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnNew.addActionListener(actionEvent -> newLifePathAction());
        pnlButtons.add(btnNew);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));

        String titleSave = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.save");
        RoundedJButton btnSave = new RoundedJButton(titleSave);
        btnSave.setMargin(new Insets(PADDING, 0, PADDING, 0));
        btnSave.addActionListener(event -> saveLifePathAction());
        pnlButtons.add(btnSave);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));

        String titleLoad = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.load");
        RoundedJButton btnLoad = new RoundedJButton(titleLoad);
        btnLoad.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnLoad.addActionListener(event -> loadLifePathAction());
        pnlButtons.add(btnLoad);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));

        String titleToggleProgress = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.toggleProgress");
        RoundedJButton btnToggleProgress = new RoundedJButton(titleToggleProgress);
        btnToggleProgress.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnToggleProgress.addActionListener(actionEvent -> pnlProgress.setVisible(!pnlProgress.isVisible()));
        pnlButtons.add(btnToggleProgress);

        pnlButtons.add(Box.createHorizontalGlue());

        pnlControls.add(pnlButtons, BorderLayout.SOUTH);

        return pnlControls;
    }

    /**
     * Validates the wizard's contents, asks about the unique id where that question makes sense, then writes the
     * Life Path to a file the author chooses.
     *
     * <p>The order matters. Validation runs first, so an author with an invalid path is not made to answer the id
     * question, read "Save Failed", and answer it again on the retry. The id is only committed once the file has
     * actually been written, so cancelling the save dialog leaves the id on screen unchanged.</p>
     *
     * @since 0.50.11
     */
    private void saveLifePathAction() {
        // Returns null and shows the reasons when the path is not valid.
        LifePath record = buildLifePathFromBuilderWizard();

        if (record == null) {
            return;
        }

        Optional<File> writtenFile = LifePathIO.writeToJSONWithDialog(record,
              basicInfoTab.isIncludeLegalStatement());

        if (writtenFile.isPresent()) {
            lifePathId = record.id();
            loadedFromFile = true;
            hasUnsavedChanges = false;
            basicInfoTab.setLifePathId(lifePathId);
        }
    }

    /**
     * Asks the author for a Life Path file and loads it into the wizard.
     *
     * <p>A file that cannot be read now says so. Previously the failure was swallowed, so picking a bad file did
     * nothing at all and looked like the button was broken.</p>
     *
     * @since 0.50.11
     */
    private void loadLifePathAction() {
        LifePathIO.LifePathLoadResult result = LifePathIO.loadFromJSONWithDialog();

        if (result.isFailed()) {
            new ImmersiveDialogNotification(campaign, result.errorMessage(), true);
            return;
        }

        if (!result.isLoaded()) {
            return;
        }

        isPopulating = true;
        try {
            resetNonBasicTabs();
            updateBuilderFromExistingLifePathRecord(result.lifePath());
        } finally {
            isPopulating = false;
        }

        loadedFromFile = true;
        hasUnsavedChanges = false;
        basicInfoTab.setIncludeLegalStatement(result.fileHasLegalStatement());

        SwingUtilities.invokeLater(() -> {
            scrollProgress.getVerticalScrollBar().setValue(0);
            scrollProgress.getHorizontalScrollBar().setValue(0);
        });
    }

    private void newLifePathAction() {
        if (!MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_NEW_LIFE_PATH)) {
            ImmersiveDialogConfirmation confirmation = new ImmersiveDialogConfirmation(campaign,
                  CONFIRMATION_NEW_LIFE_PATH);
            if (!confirmation.wasConfirmed()) {
                return;
            }
        }

        lifePathId = UUID.randomUUID();
        // A brand new path has never been written anywhere, so there is no id to keep or replace on the next save.
        loadedFromFile = false;

        isPopulating = true;
        try {
            resetBasicTab();
            resetNonBasicTabs();

            fixedXPTab.addTab();
            exclusionsTab.addTab();
            basicInfoTab.setLifePathId(lifePathId);
        } finally {
            isPopulating = false;
        }

        hasUnsavedChanges = false;
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

    /**
     * Closes the wizard, asking first when there is unsaved work.
     *
     * <p>The Life Path library is reloaded on the way out so anything saved during this session is visible to the
     * campaign straight away.</p>
     *
     * @since 0.50.11
     */
    private void performDialogCloseAction() {
        if (hasUnsavedChanges && !confirmDiscardUnsavedChanges()) {
            return;
        }

        Map<UUID, LifePath> lifePaths = LifePathIO.loadAllLifePaths(campaign);
        campaign.setLifePathLibrary(lifePaths);
        dispose();
    }

    /**
     * Asks whether unsaved edits should be thrown away.
     *
     * @return {@code true} when the author chose to close anyway
     *
     * @since 0.50.11
     */
    private boolean confirmDiscardUnsavedChanges() {
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              null,
              null,
              getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.unsaved.label.inCharacter"),
              List.of(getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.unsaved.button.stay"),
                    getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.unsaved.button.discard")),
              getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.unsaved.label.outOfCharacter"),
              null,
              false);

        final int DISCARD = 1;
        return dialog.getDialogChoice() == DISCARD;
    }

    /**
     * Asks the author whether this save should become a new Life Path rather than replace the one it was loaded
     * from.
     *
     * <p>The caller decides what to do with the answer. This method no longer replaces the id itself, because the
     * save dialog that follows can still be cancelled, and an id replaced before that point would leave the wizard
     * showing one id while the next save wrote another.</p>
     *
     * @return {@code true} when the author asked for a new unique id
     *
     * @since 0.50.11
     */
    private boolean displayIDRegenerationDialogs() {
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

            if (!MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_REGEN_PATH_ID)) {
                ImmersiveDialogConfirmation confirmationDialog = new ImmersiveDialogConfirmation(campaign,
                      CONFIRMATION_REGEN_PATH_ID);
                choiceConfirmed = confirmationDialog.wasConfirmed();
            } else {
                choiceConfirmed = true;
            }
        }

        if (dialog.getDialogChoice() != REGENERATE_ID) {
            return false;
        }

        new ImmersiveDialogNotification(campaign,
              getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.confirmation.label"), true);

        return true;
    }

    /**
     * Fills the wizard in from an existing Life Path.
     *
     * <p>Each section's groups have to exist before its values are written, and its progress text has to be built
     * after, which is why the three steps are ordered the way they are.</p>
     *
     * @param record the Life Path to load
     *
     * @since 0.50.11
     */
    private void updateBuilderFromExistingLifePathRecord(LifePath record) {
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
        basicInfoTab.setLifePathId(record.id());

        loadSection(LifePathBuilderTabType.REQUIREMENTS, record, requirementsTab);
        loadSection(LifePathBuilderTabType.EXCLUSIONS, record, exclusionsTab);
        loadSection(LifePathBuilderTabType.FIXED_XP, record, fixedXPTab);
        loadSection(LifePathBuilderTabType.FLEXIBLE_XP, record, flexibleXPTab);

        updateTxtProgress();
    }

    /**
     * Loads one section of a Life Path onto its tab, creating the groups it needs first.
     *
     * @param tabType the section to load
     * @param record  the Life Path being loaded
     * @param tab     the tab for that section
     *
     * @since 0.50.11
     */
    private void loadSection(LifePathBuilderTabType tabType, LifePath record, LifePathTab tab) {
        int maximumGroupIndex = LifePathSection.getMaximumGroupIndex(tabType, record);

        // Before the values, or creating the groups would overwrite them.
        addAdditionalTabsAsNecessary(maximumGroupIndex, tab);

        if (maximumGroupIndex > -1) {
            LifePathSection.writeToTab(tabType, record, tab);
        }

        // After the values, so the text describes what was just loaded.
        updateProgressTextPerTab(maximumGroupIndex, tab);
    }

    /**
     * Creates the group tabs a loaded Life Path needs.
     *
     * <p>One group is always created, even for a section the file left empty. Fixed XP and Exclusions hide their Add
     * Group button, so a section left with no groups at all could not be given one.</p>
     *
     * @param maximumGroupIndex the highest group index the file uses, or {@code -1} when it uses none
     * @param lifePathTab       the section to add groups to
     *
     * @since 0.50.11
     */
    private void addAdditionalTabsAsNecessary(int maximumGroupIndex, LifePathTab lifePathTab) {
        for (int groupIndex = -1; groupIndex < maximumGroupIndex; groupIndex++) {
            lifePathTab.addTab();
        }

        if (lifePathTab.getTabCount() == 0) {
            lifePathTab.addTab();
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

    /**
     * Returns the highest group index present across the supplied group maps.
     *
     * <p>Every map is keyed by group index, so the highest key across all of a section's maps is the index of the
     * last group that section needs. Empty and {@code null} maps contribute nothing.</p>
     *
     * @param maps the group maps belonging to a single section
     *
     * @return the highest group index found, or {@code -1} when every map is empty
     *
     * @since 0.50.11
     */
    @SafeVarargs
    private static int getMaxKey(Map<Integer, ?>... maps) {
        int maxKey = -1;

        for (Map<Integer, ?> map : maps) {
            if (map == null || map.isEmpty()) {
                continue;
            }

            maxKey = Math.max(maxKey, Collections.max(map.keySet()));
        }

        return maxKey;
    }

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(LifePathBuilderDialog.class);
            this.setName("LifePathBuilderDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception exception) {
            LOGGER.error("Failed to set user preferences", exception);
        }
    }

    /**
     * Reads the whole wizard into a Life Path, or reports why it cannot be one.
     *
     * <p>Assembled through {@link LifePathBuilder} rather than by calling the record's 55-argument constructor
     * directly, so adding or moving a component cannot silently shift the arguments after it.</p>
     *
     * @return the finished Life Path, or {@code null} when the author was shown a list of problems instead
     *
     * @since 0.50.11
     */
    private @Nullable LifePath buildLifePathFromBuilderWizard() {
        LifePathBuilder builder = readWizardIntoBuilder();

        Set<InvalidLifePathReason> invalidReasons = LifePathValidator.validate(builder);
        showInvalidReasonsDialog(invalidReasons);

        if (!invalidReasons.isEmpty()) {
            return null;
        }

        // Asked only now that the path is known to be valid, and only for a path that came from a file: a brand new
        // path has no previous id, so "keep or replace it?" has no meaning. The answer goes onto the record but not
        // onto the wizard, because the save dialog can still be cancelled.
        if (loadedFromFile && displayIDRegenerationDialogs()) {
            builder.id(UUID.randomUUID());
        }

        return builder.build();
    }

    /**
     * Collects everything currently in the wizard into a builder, without validating it.
     *
     * @return the Life Path as the wizard currently describes it
     *
     * @since 0.50.11
     */
    private LifePathBuilder readWizardIntoBuilder() {
        LifePathBuilder builder = new LifePathBuilder().id(lifePathId)
                                        .version(MHQConstants.VERSION)
                                        .source(basicInfoTab.getSource())
                                        .name(basicInfoTab.getName())
                                        .flavorText(basicInfoTab.getFlavorText())
                                        .age(basicInfoTab.getAge())
                                        .xpDiscount(basicInfoTab.getDiscount())
                                        .minimumYear(basicInfoTab.getMinimumYear())
                                        .maximumYear(basicInfoTab.getMaximumYear())
                                        .randomWeight(basicInfoTab.getRandomWeight())
                                        .lifeStages(basicInfoTab.getLifeStages())
                                        .categories(basicInfoTab.getCategories())
                                        .isPlayerRestricted(basicInfoTab.isPlayerRestricted());

        LifePathSection.readFromTab(LifePathBuilderTabType.REQUIREMENTS, requirementsTab, builder);
        LifePathSection.readFromTab(LifePathBuilderTabType.EXCLUSIONS, exclusionsTab, builder);
        LifePathSection.readFromTab(LifePathBuilderTabType.FIXED_XP, fixedXPTab, builder);
        LifePathSection.readFromTab(LifePathBuilderTabType.FLEXIBLE_XP, flexibleXPTab, builder);

        builder.xpCost(calculateXPCost(builder));

        return builder;
    }

    /**
     * Returns what the Life Path under construction would cost.
     *
     * @param builder the Life Path being assembled
     *
     * @return the XP cost
     *
     * @since 0.50.11
     */
    private int calculateXPCost(LifePathBuilder builder) {
        return LifePathXPCostCalculator.calculateXPCost(builder);
    }

    /**
     * Shows the author every reason the Life Path cannot be saved, and does nothing when there are none.
     *
     * @param invalidReasons the reasons reported by {@link LifePathValidator}
     *
     * @since 0.50.11
     */
    private void showInvalidReasonsDialog(Set<InvalidLifePathReason> invalidReasons) {
        if (invalidReasons.isEmpty()) {
            return;
        }

        StringBuilder invalidText = new StringBuilder(getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.invalid.label"));
        for (InvalidLifePathReason invalidReason : invalidReasons) {
            invalidText.append(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.invalid.format",
                  spanOpeningWithCustomColor(getWarningColor()), invalidReason.getDisplayName(),
                  CLOSING_SPAN_TAG, invalidReason.getDescription()));
        }

        new ImmersiveDialogNotification(campaign, invalidText.toString(), true);
    }

}
