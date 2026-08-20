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
package mekhq.gui.dialog.markets.contractMarket;

import static java.lang.Math.max;
import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;

import jakarta.annotation.Nullable;
import megamek.client.ui.dialogs.iconChooser.CamoChooserDialog;
import megamek.client.ui.dialogs.iconChooser.PortraitChooserDialog;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;
import megamek.common.icons.Portrait;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.*;
import mekhq.campaign.mission.contract.contractGeneration.AbstractContractGeneration;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.EmployerLiaison;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.EmployerNegotiator;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.OpposingCommander;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import mekhq.gui.FactionComboBox;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.DateChooser;
import mekhq.gui.displayWrappers.RankDisplay;
import mekhq.gui.utilities.JSuggestField;

/**
 * A GM-only editor for a single {@link AbstractContract} market offer. It surfaces essentially every field a game
 * master would want to hand-tune - identity, board parameters, schedule, target system, employer and enemy make-up
 * (including camouflage and force colour), terms, objectives, pay, rented facilities, and morale - so an offer can be
 * reshaped to their specifications. Faction and system fields are pickers rather than free text to prevent typos.
 *
 * <p>The generated NPC personnel attached to a contract (the employer's negotiator and liaison, and the opposing
 * commander) can have their name, rank, and portrait edited. On confirmation the contract is mutated in place; query
 * {@link #wasConfirmed()} afterward and rebuild any view showing it. The player's chosen negotiator and the StratCon
 * campaign state are preserved as-is, not edited here.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractEditorDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(ContractEditorDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    private static final int MONEY_STEP = 1000;
    private static final int LABEL_WIDTH = 170;
    /** The end date is held at least this many months after the start date. */
    private static final int MINIMUM_CONTRACT_LENGTH_MONTHS = 1;

    /** Synthetic "Custom" faction codes offered as leading options in the employer and enemy faction pickers. */
    private static final String CUSTOM_INNER_SPHERE_CODE = "IS";
    private static final String CUSTOM_PERIPHERY_CODE = "Periphery";
    private static final String CUSTOM_CLAN_CODE = "CLAN";
    private static final int ICON_SIZE = 48;
    /** Profession used to label ranks in the create-mode NPC pickers; the numeric rank is what actually applies. */
    private static final Profession DEFAULT_PROFESSION = Profession.MEKWARRIOR;

    private final int PADDING = scaleForGUI(6);

    private final transient Campaign campaign;
    private final transient AbstractContract contract;
    private final transient LocalDate currentDate;
    private final boolean createMode;
    private final transient ContractSearchType initialBucket;

    // Identity
    private JComboBox<ContractSearchType> bucketCombo; // create mode only
    private JTextField nameField;
    private JTextArea descriptionArea;
    private JComboBox<MissionStatus> statusCombo;
    private boolean statusEditable;

    // Parameters
    private JSpinner scaleSpinner;
    private JCheckBox scaleAutomatic;
    private JSpinner trackCountSpinner;
    private JCheckBox trackCountAutomatic;
    private JSpinner combatElementsSpinner;
    private JCheckBox combatElementsAutomatic;
    private JSpinner victoryPointsSpinner;
    private JCheckBox victoryPointsAutomatic;

    // Schedule
    private JButton startDateButton;
    private JCheckBox startAutomatic;
    private JButton endDateButton;
    private JCheckBox endAutomatic;
    private transient LocalDate startDate;
    private transient LocalDate endDate;
    private JSpinner lengthSpinner;

    // Target
    private JSuggestField systemField;
    private JComboBox<Planet> planetCombo;
    private JCheckBox planetAutomatic;

    // Employer
    private JComboBox<ChaosEmployerType> employerTypeCombo;
    private FactionComboBox employerFactionCombo;
    private FactionComboBox employerAnchorCombo;
    private FactionComboBox employerSponsorCombo;
    private JTextField employerDisplayNameField;
    private JComboBox<SkillLevel> employerSkillCombo;
    private JComboBox<DragoonRating> employerEquipmentCombo;
    private JComboBox<PlayerColour> employerColorCombo;
    private JButton employerCamoButton;
    private transient Camouflage employerCamouflage;

    // Enemy
    private FactionComboBox enemyFactionCombo;
    private FactionComboBox enemySponsorCombo;
    private JTextField enemyDisplayNameField;
    private JComboBox<SkillLevel> enemySkillCombo;
    private JComboBox<DragoonRating> enemyEquipmentCombo;
    private JCheckBox batchallAcceptedCheckbox;
    private JComboBox<PlayerColour> enemyColorCombo;
    private JButton enemyCamoButton;
    private transient Camouflage enemyCamouflage;

    // Terms
    private JComboBox<ChaosContractStepsTable> payRateCombo;
    private JComboBox<ChaosContractStepsTable> supportCombo;
    private JComboBox<ChaosContractStepsTable> transportTermCombo;
    private JComboBox<ChaosContractStepsTable> salvageCombo;
    private JComboBox<ChaosContractStepsTable> commandCombo;

    // Objectives
    private JComboBox<ContractObjectiveType> playerObjectiveCombo;
    private JComboBox<ContractObjectiveType> opposingObjectiveCombo;

    // Finance
    private JSpinner monthlyPaySpinner;
    private JCheckBox monthlyPayAutomatic;
    private JSpinner transportPaySpinner;
    private JCheckBox transportPayAutomatic;
    private JSpinner combatPaySpinner;
    private JCheckBox combatPayAutomatic;

    // Intel obfuscation (hidden from the player in the market; always GM-editable, never auto-applied here)
    private JCheckBox obfuscateAlliedCommandCheckbox;
    private JCheckBox obfuscateOppositionCheckbox;
    private JCheckBox obfuscateThreatCheckbox;
    private JCheckBox obfuscateMoraleCheckbox;

    // Facilities
    private JSpinner hospitalBedsSpinner;
    private JSpinner kitchensSpinner;
    private JSpinner holdingCellsSpinner;

    // Morale
    private JComboBox<ContractMoraleLevel> moraleLevelCombo;
    private JTextField routEndDateField;
    private JSpinner routedPayoutSpinner;
    private boolean routFieldsEditable;

    // Personnel - edit mode wraps the existing NPCs; create mode captures overrides applied to NPCs generated on
    // save.
    private transient NpcEditor negotiatorEditor;
    private transient NpcEditor liaisonEditor;
    private transient NpcEditor commanderEditor;
    private transient NpcOverride negotiatorOverride;
    private transient NpcOverride liaisonOverride;
    private transient NpcOverride commanderOverride;

    private boolean confirmed;

    /**
     * Opens the modal GM editor for an existing offer. On confirmation the contract's fields are updated in place;
     * query {@link #wasConfirmed()} afterward.
     *
     * @param campaign the active campaign
     * @param contract the offer to edit
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ContractEditorDialog(Campaign campaign, AbstractContract contract) {
        this(campaign, contract, null);
    }

    /**
     * Opens the modal GM editor. Passing a non-null {@code createBucket} puts the dialog in "create" mode: the title
     * and header eyebrow change and a market-bucket picker (seeded to {@code createBucket}) is shown, letting the GM
     * choose which {@link ContractSearchType} the new offer belongs to (read back via
     * {@link #getSelectedSearchType()}).
     *
     * <p>In both modes the given contract is mutated in place; in create mode the caller is expected to build a blank
     * contract (see {@link NewContractFactory}) and add it to the chosen bucket when {@link #wasConfirmed()} is
     * {@code true}.</p>
     *
     * @param campaign     the active campaign
     * @param contract     the contract to edit (a blank one, for create mode)
     * @param createBucket the market bucket to seed the picker with, or {@code null} for edit mode
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ContractEditorDialog(Campaign campaign, AbstractContract contract,
          @Nullable ContractSearchType createBucket) {
        super(campaign.getGUI().getFrame(), true);
        this.campaign = campaign;
        this.contract = contract;
        this.currentDate = campaign.getLocalDate();
        this.createMode = createBucket != null;
        this.initialBucket = createBucket;

        initializeComponents();
    }

    /**
     * @return in create mode, the market bucket the GM selected for the new offer; in edit mode, {@code null}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ContractSearchType getSelectedSearchType() {
        return bucketCombo != null ? (ContractSearchType) bucketCombo.getSelectedItem() : initialBucket;
    }

    /** @return whether the GM confirmed their edits. */
    public boolean wasConfirmed() {
        return confirmed;
    }

    private void initializeComponents() {
        setTitle(getTextAt(RESOURCE_BUNDLE, createMode ? "title.contractMarket.create" : "title.contractMarket.edit"));

        // Edge-to-edge accent header (matching the market dossier), then a padded body and footer beneath it.
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        content.add(buildTabs(), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);

        root.add(content, BorderLayout.CENTER);
        getContentPane().add(root);

        pack();
        setSize(scaleForGUI(640, 560)); // Default opening size; saved preferences (below) override it on later opens
        setLocationRelativeTo(getParent());
        setPreferences(); // Must be before setVisible
        setVisible(true);
    }

    /**
     * Tracks this dialog's window size and position in MekHQ's preferences so it reopens where the GM left it.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ContractEditorDialog.class);
            setName("ContractEditorDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    /**
     * The market-style accent header: an eyebrow over the contract name, on a bar tinted with the employer's force
     * colour, mirroring the offer dossier for visual uniformity.
     */
    private JPanel buildHeader() {
        Color accent = contract.getEmployerColor().getColour();
        Color onAccent = contrastingText(accent);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(accent);
        int pad = scaleForGUI(12);
        header.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        String eyebrow = getTextAt(RESOURCE_BUNDLE,
              createMode ? "create.contractMarket.eyebrow" : "edit.contractMarket.eyebrow");
        JLabel title = new JLabel("<html><span style='font-size:smaller'>"
                                        + escape(eyebrow).toUpperCase()
                                        + "</span><br><b style='font-size:larger'>"
                                        + escape(contract.getName())
                                        + "</b></html>");
        title.setForeground(onAccent);
        header.add(title, BorderLayout.WEST);

        return header;
    }

    /** Lays each category out on its own tab, so the GM steps through them rather than scrolling one long column. */
    private EnhancedTabbedPane buildTabs() {
        EnhancedTabbedPane tabs = new EnhancedTabbedPane(false, false);
        tabs.setTabPlacement(JTabbedPane.LEFT);

        addTab(tabs, "edit.contractMarket.section.identity", buildIdentityCard());
        addTab(tabs, "edit.contractMarket.section.parameters", buildParametersCard());
        addTab(tabs, "edit.contractMarket.section.schedule", buildScheduleCard());
        addTab(tabs, "edit.contractMarket.section.target", buildTargetCard());
        addTab(tabs, "edit.contractMarket.section.employer", buildEmployerCard());
        addTab(tabs, "edit.contractMarket.section.enemy", buildEnemyCard());
        addTab(tabs, "edit.contractMarket.section.intel", buildIntelCard());
        addTab(tabs, "edit.contractMarket.section.terms", buildTermsCard());
        addTab(tabs, "edit.contractMarket.section.objectives", buildObjectivesCard());
        addTab(tabs, "edit.contractMarket.section.finance", buildFinanceCard());
        addTab(tabs, "edit.contractMarket.section.facilities", buildFacilitiesCard());
        addTab(tabs, "edit.contractMarket.section.morale", buildMoraleCard());
        buildPersonnelTabs(tabs);

        return tabs;
    }

    /**
     * Adds one category to the pane as a tab titled by its section key, with the content top-anchored and vertically
     * scrollable so a tall category never gets clipped.
     */
    private void addTab(EnhancedTabbedPane tabs, String sectionKey, JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        wrapper.add(content, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(scaleForGUI(16));

        tabs.addTab(getTextAt(RESOURCE_BUNDLE, sectionKey), scroll);
    }

    // region Cards

    private JPanel buildIdentityCard() {
        JPanel rows = rowsPanel();

        if (createMode) {
            bucketCombo = new JComboBox<>(new DefaultComboBoxModel<>(ContractSearchType.values()));
            bucketCombo.setSelectedItem(initialBucket);
            bucketCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                      boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof ContractSearchType type) {
                        setText(getTextAt(RESOURCE_BUNDLE, "searchType.contractMarket." + type.name()));
                    }
                    return this;
                }
            });
            rows.add(formRow("edit.contractMarket.field.bucket", bucketCombo));
        }

        nameField = new JTextField(contract.getName(), 24);
        rows.add(formRow("edit.contractMarket.field.name", nameField));

        descriptionArea = new JTextArea(contract.getDescription(), 4, 24);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        descriptionScroll.setPreferredSize(new Dimension(scaleForGUI(260), scaleForGUI(90)));
        rows.add(formRow("edit.contractMarket.field.description", descriptionScroll));

        // A market offer has no status, and an accepted-but-active contract is finalized through the normal completion
        // flow, so status is only hand-editable once the contract has actually concluded. Even then it may only move
        // between concluded outcomes - a concluded contract can never be turned back to ACTIVE - so the enabled picker
        // offers the completed statuses alone. When not editable the field is disabled (showing the current value),
        // never hidden.
        MissionStatus status = contract.getStatus();
        statusEditable = status != null && status.isCompleted();
        MissionStatus[] statusOptions = statusEditable
                                              ?
                                              Arrays.stream(MissionStatus.values())
                                                    .filter(MissionStatus::isCompleted)
                                                    .toArray(MissionStatus[]::new)
                                              :
                                              MissionStatus.values();
        statusCombo = enumCombo(statusOptions, status);
        statusCombo.setEnabled(statusEditable);
        if (!statusEditable) {
            statusCombo.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE,
                  "edit.contractMarket.field.status.disabled.tooltip")));
        }
        rows.add(formRow("edit.contractMarket.field.status", statusCombo));

        return card(rows);
    }

    private JPanel buildParametersCard() {
        JPanel rows = rowsPanel();

        scaleSpinner = intSpinner(contract.getScale(), 1);
        scaleAutomatic = automaticToggle(scaleSpinner);
        rows.add(formRow("edit.contractMarket.field.scale", withAutomatic(scaleSpinner, scaleAutomatic)));

        trackCountSpinner = intSpinner(contract.getTrackCount(), 0);
        trackCountAutomatic = automaticToggle(trackCountSpinner);
        rows.add(formRow("edit.contractMarket.field.trackCount",
              withAutomatic(trackCountSpinner, trackCountAutomatic)));

        combatElementsSpinner = intSpinner(contract.getRequiredCombatElements(), 0);
        combatElementsAutomatic = automaticToggle(combatElementsSpinner);
        rows.add(formRow("edit.contractMarket.field.combatElements",
              withAutomatic(combatElementsSpinner, combatElementsAutomatic)));

        victoryPointsSpinner = intSpinner(contract.getRequiredVictoryPoints(), 0);
        victoryPointsAutomatic = automaticToggle(victoryPointsSpinner);
        rows.add(formRow("edit.contractMarket.field.victoryPoints",
              withAutomatic(victoryPointsSpinner, victoryPointsAutomatic)));

        return card(rows);
    }

    private JPanel buildScheduleCard() {
        JPanel rows = rowsPanel();
        ContractScheduleData schedule = contract.getScheduleData();

        startDate = schedule == null ? null : schedule.startDate();
        endDate = schedule == null ? null : schedule.endDate();

        // Automatic start/end are offered in both modes since they are deterministic (start from travel time, end from
        // the length). They default on for a brand-new offer and off when editing, so an existing contract's dates show
        // as they stand.
        startDateButton = dateButton(this::pickStartDate);
        startAutomatic = scheduleAutomaticToggle(this::onStartAutomaticToggled);
        rows.add(formRow("edit.contractMarket.field.startDate", withAutomatic(startDateButton, startAutomatic)));

        endDateButton = dateButton(this::pickEndDate);
        endAutomatic = scheduleAutomaticToggle(this::onEndAutomaticToggled);
        rows.add(formRow("edit.contractMarket.field.endDate", withAutomatic(endDateButton, endAutomatic)));

        // The length is derived from the dates when the end date is set by hand, but becomes the GM's own input that
        // drives the end date when the end date is automatic.
        lengthSpinner = intSpinner(max(MINIMUM_CONTRACT_LENGTH_MONTHS, contract.getLengthInMonths()),
              MINIMUM_CONTRACT_LENGTH_MONTHS);
        lengthSpinner.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.length.tooltip")));
        lengthSpinner.addChangeListener(e -> onLengthChanged());
        rows.add(formRow("edit.contractMarket.field.length", lengthSpinner));

        // Seed the automatic values so the initial display already reflects the checkbox defaults.
        if (startAutomatic.isSelected()) {
            startDate = AbstractContractGeneration.determineStartDate(campaign, contract);
        }
        if (endAutomatic.isSelected() && startDate != null) {
            endDate = startDate.plusMonths(lengthValue());
        }
        refreshScheduleControls();
        return card(rows);
    }

    /** A button that shows a date (or "not set") and runs {@code onPick} - a {@link DateChooser} flow - when clicked. */
    private JButton dateButton(Runnable onPick) {
        JButton button = new JButton();
        button.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.date.pick.tooltip")));
        button.addActionListener(e -> onPick.run());
        return button;
    }

    /** An "Automatic" checkbox for a schedule field, defaulting on for a new offer and off when editing. */
    private JCheckBox scheduleAutomaticToggle(Runnable onToggle) {
        JCheckBox automatic = new JCheckBox(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.automatic"),
              createMode);
        automatic.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.automatic.tooltip")));
        automatic.addActionListener(e -> onToggle.run());
        return automatic;
    }

    private void onStartAutomaticToggled() {
        if (startAutomatic.isSelected()) {
            startDate = AbstractContractGeneration.determineStartDate(campaign, contract);
        }
        reconcileScheduleAfterStartChange();
        refreshScheduleControls();
    }

    private void onEndAutomaticToggled() {
        if (endAutomatic.isSelected() && startDate != null) {
            endDate = startDate.plusMonths(lengthValue());
        }
        refreshScheduleControls();
    }

    private void onLengthChanged() {
        if (endAutomatic.isSelected() && startDate != null) {
            endDate = startDate.plusMonths(lengthValue());
            endDateButton.setText(dateButtonLabel(endDate));
        }
    }

    private void pickStartDate() {
        LocalDate picked = pickDate(startDate);
        if (picked == null) {
            return;
        }
        startDate = picked;
        reconcileScheduleAfterStartChange();
        refreshScheduleControls();
    }

    private void pickEndDate() {
        LocalDate seed = endDate != null ?
                               endDate
                               :
                               (startDate != null ? startDate.plusMonths(MINIMUM_CONTRACT_LENGTH_MONTHS) : currentDate);
        LocalDate picked = pickDate(seed);
        if (picked == null) {
            return;
        }
        // With no start yet, anchor one a month before the chosen end so the pair is valid.
        if (startDate == null) {
            startDate = picked.minusMonths(MINIMUM_CONTRACT_LENGTH_MONTHS);
        }
        // Keep the one-month minimum span: an end nearer than a month to the start is clamped up to the minimum.
        LocalDate earliestEnd = startDate.plusMonths(MINIMUM_CONTRACT_LENGTH_MONTHS);
        endDate = picked.isBefore(earliestEnd) ? earliestEnd : picked;
        refreshScheduleControls();
    }

    /** After the start date moves, re-derive the end (when it is automatic) or nudge it out to keep the minimum span. */
    private void reconcileScheduleAfterStartChange() {
        if (startDate == null) {
            return;
        }
        if (endAutomatic.isSelected()) {
            endDate = startDate.plusMonths(lengthValue());
            return;
        }
        LocalDate earliestEnd = startDate.plusMonths(MINIMUM_CONTRACT_LENGTH_MONTHS);
        if (endDate == null || endDate.isBefore(earliestEnd)) {
            endDate = earliestEnd;
        }
    }

    /**
     * Opens the shared date chooser seeded at {@code seed} (today when null); returns the chosen date, or null if
     * cancelled.
     */
    private @Nullable LocalDate pickDate(@Nullable LocalDate seed) {
        DateChooser chooser = new DateChooser(this, seed != null ? seed : currentDate);
        return chooser.showDateChooser() == DateChooser.OK_OPTION ? chooser.getDate() : null;
    }

    /** The length spinner's value, never below the one-month minimum. */
    private int lengthValue() {
        return max(MINIMUM_CONTRACT_LENGTH_MONTHS, intValue(lengthSpinner));
    }

    /**
     * Repaints the date buttons and applies the enabled states: a date button is editable only when its "Automatic" box
     * is off, and the length spinner is editable only when the end date is automatic (where it drives the end). When
     * the end date is set by hand the spinner instead shows the length the dates imply.
     */
    private void refreshScheduleControls() {
        startDateButton.setText(dateButtonLabel(startDate));
        endDateButton.setText(dateButtonLabel(endDate));
        startDateButton.setEnabled(!startAutomatic.isSelected());
        endDateButton.setEnabled(!endAutomatic.isSelected());
        lengthSpinner.setEnabled(endAutomatic.isSelected());
        if (!endAutomatic.isSelected() && startDate != null && endDate != null) {
            lengthSpinner.setValue((int) max(MINIMUM_CONTRACT_LENGTH_MONTHS,
                  ChronoUnit.MONTHS.between(startDate, endDate)));
        }
    }

    private String dateButtonLabel(@Nullable LocalDate date) {
        return date == null ? getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.date.notSet")
                     : MekHQ.getMHQOptions().getDisplayFormattedDate(date);
    }

    private JPanel buildTargetCard() {
        JPanel rows = rowsPanel();

        systemField = new JSuggestField(this, campaign.getSystemNames());
        PlanetarySystem targetSystem = contract.getTargetSystem();
        if (targetSystem != null) {
            systemField.setText(targetSystem.getName(currentDate));
        }
        systemField.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.system.tooltip")));
        // Repopulate the planet list for the newly-named system. JSuggestField reports a suggestion picked from its
        // dropdown through addSelectionListener, while a bare Enter typed in the field fires the text field's own
        // action event - wire both so the planet list tracks the system however it is chosen.
        ActionListener onSystemChanged = e -> repopulatePlanets(resolveSystem(systemField.getText()), null);
        systemField.addActionListener(onSystemChanged);
        systemField.addSelectionListener(onSystemChanged);
        rows.add(formRow("edit.contractMarket.field.systemId", systemField));

        planetCombo = new JComboBox<>();
        planetCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value instanceof Planet planet ? planet.getName(currentDate)
                              : getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.planet.none"));
                return this;
            }
        });
        repopulatePlanets(targetSystem, contract.getTargetPlanetId());
        planetAutomatic = automaticToggle(planetCombo);
        rows.add(formRow("edit.contractMarket.field.planetId", withAutomatic(planetCombo, planetAutomatic)));

        return card(rows);
    }

    private JPanel buildEmployerCard() {
        JPanel rows = rowsPanel();
        EmployerData data = contract.getEmployerData();

        employerTypeCombo = employerTypeCombo(contract.getEmployerType());
        rows.add(formRow("edit.contractMarket.field.employerType", employerTypeCombo));

        employerFactionCombo = factionComboWithCustoms(contract.getEmployerFactionCode(), false);
        rows.add(formRow("edit.contractMarket.field.factionCode", employerFactionCombo));

        employerAnchorCombo = factionCombo(data == null ? null : data.anchorFactionCode());
        rows.add(formRow("edit.contractMarket.field.anchorFactionCode", employerAnchorCombo));

        employerSponsorCombo = sponsorCombo(data == null ? null : data.sponsorFactionCode());
        rows.add(formRow("edit.contractMarket.field.sponsorFactionCode", employerSponsorCombo));

        employerDisplayNameField = new JTextField(orEmpty(contract.getEmployerDisplayName()), 24);
        rows.add(formRow("edit.contractMarket.field.displayName", employerDisplayNameField));

        employerSkillCombo = enumCombo(SkillLevel.values(), contract.getEmployerForceSkill());
        rows.add(formRow("edit.contractMarket.field.forceSkill", employerSkillCombo));

        employerEquipmentCombo = equipmentRatingCombo(contract.getEmployerEquipmentRating());
        rows.add(formRow("edit.contractMarket.field.equipmentRating", employerEquipmentCombo));

        employerColorCombo = enumCombo(PlayerColour.values(), contract.getEmployerColor());
        rows.add(formRow("edit.contractMarket.field.color", employerColorCombo));

        employerCamouflage = contract.getEmployerCamouflage();
        employerCamoButton = camoButton(() -> employerCamouflage, camo -> employerCamouflage = camo);
        rows.add(formRow("edit.contractMarket.field.camouflage", employerCamoButton));

        return card(rows);
    }

    private JPanel buildEnemyCard() {
        JPanel rows = rowsPanel();
        EnemyData data = contract.getEnemyData();

        enemyFactionCombo = factionComboWithCustoms(contract.getEnemyFactionCode(), false);
        rows.add(formRow("edit.contractMarket.field.factionCode", enemyFactionCombo));

        enemySponsorCombo = sponsorCombo(data == null ? null : data.sponsorFactionCode());
        rows.add(formRow("edit.contractMarket.field.sponsorFactionCode", enemySponsorCombo));

        enemyDisplayNameField = new JTextField(orEmpty(contract.getEnemyDisplayName()), 24);
        rows.add(formRow("edit.contractMarket.field.displayName", enemyDisplayNameField));

        enemySkillCombo = enumCombo(SkillLevel.values(), contract.getEnemyForceSkill());
        rows.add(formRow("edit.contractMarket.field.forceSkill", enemySkillCombo));

        enemyEquipmentCombo = equipmentRatingCombo(contract.getEnemyEquipmentRating());
        rows.add(formRow("edit.contractMarket.field.equipmentRating", enemyEquipmentCombo));

        batchallAcceptedCheckbox = new JCheckBox(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.batchall"),
              data != null && data.batchallAccepted());
        // A batchall is only offered on an accepted contract fought against a Clan enemy, so keep the checkbox in step
        // with the enemy faction the GM picks.
        enemyFactionCombo.addActionListener(e -> refreshBatchallEnabled());
        refreshBatchallEnabled();
        rows.add(formRow("edit.contractMarket.field.batchall.label", batchallAcceptedCheckbox));

        enemyColorCombo = enumCombo(PlayerColour.values(), contract.getEnemyColour());
        rows.add(formRow("edit.contractMarket.field.color", enemyColorCombo));

        enemyCamouflage = contract.getEnemyCamouflage();
        enemyCamoButton = camoButton(() -> enemyCamouflage, camo -> enemyCamouflage = camo);
        rows.add(formRow("edit.contractMarket.field.camouflage", enemyCamoButton));

        return card(rows);
    }

    /**
     * The Intel tab: GM toggles for which intel fields are hidden from the player in the contract market. Always
     * available (create and edit alike); the editor itself always shows the true values. Ticking a box hides that field
     * behind a "No Intel" label in the market dossier - and hiding the opposition also blanks the offer's title.
     * Assessment is intentionally absent: it is never obfuscated.
     */
    private JPanel buildIntelCard() {
        JPanel rows = rowsPanel();

        obfuscateAlliedCommandCheckbox = obfuscationCheckbox(ObfuscatableIntel.ALLIED_COMMAND);
        rows.add(formRow("dossier.contractMarket.intel.alliedCommand", obfuscateAlliedCommandCheckbox));

        obfuscateOppositionCheckbox = obfuscationCheckbox(ObfuscatableIntel.OPPOSITION);
        rows.add(formRow("dossier.contractMarket.intel.opposition", obfuscateOppositionCheckbox));

        obfuscateThreatCheckbox = obfuscationCheckbox(ObfuscatableIntel.THREAT);
        rows.add(formRow("dossier.contractMarket.intel.threat", obfuscateThreatCheckbox));

        obfuscateMoraleCheckbox = obfuscationCheckbox(ObfuscatableIntel.MORALE);
        rows.add(formRow("dossier.contractMarket.intel.morale", obfuscateMoraleCheckbox));

        return card(rows);
    }

    /** A checkbox for hiding one intel field from the player, initialized from the contract's current state. */
    private JCheckBox obfuscationCheckbox(ObfuscatableIntel field) {
        JCheckBox checkbox = new JCheckBox(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.obfuscate"),
              contract.isIntelObfuscated(field));
        checkbox.setToolTipText(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.obfuscate.tooltip"));
        return checkbox;
    }

    /** Whether the player's batchall acceptance is editable: an accepted contract fought against a Clan enemy. */
    private boolean batchallEditable() {
        return contract.getStatus() != null && isEnemyClan();
    }

    /** Whether the enemy faction currently selected in the combo is a Clan. */
    private boolean isEnemyClan() {
        String code = enemyFactionCombo.getSelectedItemKey();
        return code != null && !code.isBlank() && Factions.getInstance().getFaction(code).isClan();
    }

    /** Enables the batchall checkbox only when it is editable, with a tooltip explaining when it is not. */
    private void refreshBatchallEnabled() {
        boolean editable = batchallEditable();
        batchallAcceptedCheckbox.setEnabled(editable);
        batchallAcceptedCheckbox.setToolTipText(editable ?
                                                      null
                                                      :
                                                      wordWrap(getTextAt(RESOURCE_BUNDLE,
                                                            "edit.contractMarket.field.batchall.disabled.tooltip")));
    }

    private JPanel buildTermsCard() {
        JPanel rows = rowsPanel();

        payRateCombo = stepsCombo(contract.getBasePayRateStep(), TermEffect.PAY_RATE);
        rows.add(formRow("edit.contractMarket.field.payRate", payRateCombo));

        supportCombo = stepsCombo(contract.getSupportStep(), TermEffect.SUPPORT);
        rows.add(formRow("edit.contractMarket.field.support", supportCombo));

        transportTermCombo = stepsCombo(contract.getTransportStep(), TermEffect.TRANSPORT);
        rows.add(formRow("edit.contractMarket.field.transport", transportTermCombo));

        salvageCombo = stepsCombo(contract.getSalvageRightsStep(), TermEffect.SALVAGE);
        rows.add(formRow("edit.contractMarket.field.salvage", salvageCombo));

        commandCombo = stepsCombo(contract.getCommandRightsStep(), TermEffect.COMMAND);
        rows.add(formRow("edit.contractMarket.field.command", commandCombo));

        return card(rows);
    }

    private JPanel buildObjectivesCard() {
        JPanel rows = rowsPanel();

        playerObjectiveCombo = objectiveCombo(contract.getObjectiveType());
        rows.add(formRow("edit.contractMarket.field.playerObjective", playerObjectiveCombo));

        opposingObjectiveCombo = objectiveCombo(contract.getOpposingObjectiveType());
        rows.add(formRow("edit.contractMarket.field.opposingObjective", opposingObjectiveCombo));

        return card(rows);
    }

    private JPanel buildFinanceCard() {
        JPanel rows = rowsPanel();

        // Seed from the record directly (guarding null) rather than the convenience getters, which dereference the
        // finance data unchecked.
        ContractFinanceData finance = contract.getContractFinanceData();
        Money transport = finance == null ? Money.zero() : finance.transport();
        Money monthlyPay = finance == null ? Money.zero() : finance.monthlyPay();
        Money combatPay = finance == null ? Money.zero() : finance.combatPay();

        monthlyPaySpinner = moneySpinner(monthlyPay);
        monthlyPayAutomatic = financeAutomatic(monthlyPaySpinner);
        rows.add(formRow("edit.contractMarket.field.monthlyPay",
              withAutomatic(monthlyPaySpinner, monthlyPayAutomatic)));

        transportPaySpinner = moneySpinner(transport);
        transportPaySpinner.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE,
              "edit.contractMarket.field.transportPay.tooltip")));
        transportPayAutomatic = financeAutomatic(transportPaySpinner);
        rows.add(formRow("edit.contractMarket.field.transportPay",
              withAutomatic(transportPaySpinner, transportPayAutomatic)));

        combatPaySpinner = moneySpinner(combatPay);
        combatPayAutomatic = financeAutomatic(combatPaySpinner);
        rows.add(formRow("edit.contractMarket.field.combatPay",
              withAutomatic(combatPaySpinner, combatPayAutomatic)));

        return card(rows);
    }

    private JPanel buildFacilitiesCard() {
        JPanel rows = rowsPanel();

        hospitalBedsSpinner = intSpinner(contract.getRentedHospitalBeds(), 0);
        rows.add(formRow("edit.contractMarket.field.hospitalBeds", hospitalBedsSpinner));

        kitchensSpinner = intSpinner(contract.getRentedKitchens(), 0);
        rows.add(formRow("edit.contractMarket.field.kitchens", kitchensSpinner));

        holdingCellsSpinner = intSpinner(contract.getRentedHoldingCells(), 0);
        rows.add(formRow("edit.contractMarket.field.holdingCells", holdingCellsSpinner));

        return card(rows);
    }

    private JPanel buildMoraleCard() {
        JPanel rows = rowsPanel();

        moraleLevelCombo = enumCombo(ContractMoraleLevel.values(), contract.getMoraleLevel());
        rows.add(formRow("edit.contractMarket.field.moraleLevel", moraleLevelCombo));

        // A rout only begins once the contract is accepted (and even then only for certain objectives), so the rout
        // fields are only editable for an accepted contract - one with a status. On an unstarted offer they are
        // disabled, never hidden.
        routFieldsEditable = contract.getStatus() != null;

        routEndDateField = dateField(contract.getRoutEndDate());
        routEndDateField.setEnabled(routFieldsEditable);
        routedPayoutSpinner = moneySpinner(contract.getRoutPayout());
        routedPayoutSpinner.setEnabled(routFieldsEditable);
        if (!routFieldsEditable) {
            String tooltip = wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.rout.disabled.tooltip"));
            routEndDateField.setToolTipText(tooltip);
            routedPayoutSpinner.setToolTipText(tooltip);
        }
        rows.add(formRow("edit.contractMarket.field.routEndDate", routEndDateField));
        rows.add(formRow("edit.contractMarket.field.routedPayout", routedPayoutSpinner));

        return card(rows);
    }

    /**
     * Adds an editor card for each generated NPC that exists on the contract. Contracts missing an NPC (for example an
     * opposing commander that has not been generated) simply contribute no card.
     */
    private void buildPersonnelTabs(EnhancedTabbedPane tabs) {
        // Create mode: the NPCs do not exist yet, so show override cards whose name/portrait are layered onto the NPCs
        // generated when the dialog is confirmed.
        if (createMode) {
            negotiatorOverride = new NpcOverride();
            liaisonOverride = new NpcOverride();
            commanderOverride = new NpcOverride();

            // Rank options follow the chosen faction's rank system: the employer's for its negotiator and liaison, the
            // enemy's for the opposing commander. Refresh them now and whenever the relevant faction changes.
            Runnable refreshEmployerRanks = () -> {
                RankSystem rankSystem = rankSystemOf(employerFactionCombo);
                negotiatorOverride.refreshRanks(rankSystem);
                liaisonOverride.refreshRanks(rankSystem);
            };
            Runnable refreshEnemyRanks = () -> commanderOverride.refreshRanks(rankSystemOf(enemyFactionCombo));
            refreshEmployerRanks.run();
            refreshEnemyRanks.run();
            employerFactionCombo.addActionListener(e -> refreshEmployerRanks.run());
            enemyFactionCombo.addActionListener(e -> refreshEnemyRanks.run());

            addTab(tabs, "edit.contractMarket.section.negotiator", negotiatorOverride.buildCard());
            addTab(tabs, "edit.contractMarket.section.liaison", liaisonOverride.buildCard());
            addTab(tabs, "edit.contractMarket.section.commander", commanderOverride.buildCard());
            return;
        }

        EmployerData employer = contract.getEmployerData();
        EnemyData enemy = contract.getEnemyData();

        negotiatorEditor = npcEditor(employer == null ? null : employer.negotiator());
        liaisonEditor = npcEditor(employer == null ? null : employer.liaison());
        commanderEditor = npcEditor(enemy == null ? null : enemy.opposingCommander());

        if (negotiatorEditor != null) {
            addTab(tabs, "edit.contractMarket.section.negotiator", negotiatorEditor.buildCard());
        }
        if (liaisonEditor != null) {
            addTab(tabs, "edit.contractMarket.section.liaison", liaisonEditor.buildCard());
        }
        if (commanderEditor != null) {
            addTab(tabs, "edit.contractMarket.section.commander", commanderEditor.buildCard());
        }
    }

    // endregion Cards

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, 0, PADDING));

        // Instructions sit on the left, apart from the Cancel/Save actions on the right.
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, PADDING, 0));
        RoundedJButton instructions = new RoundedJButton(
              getTextAt(RESOURCE_BUNDLE, "button.contractMarket.edit.instructions"));
        instructions.addActionListener(e -> showInstructions());
        left.add(instructions);
        footer.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, PADDING, 0));
        RoundedJButton cancel = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.edit.cancel"));
        cancel.addActionListener(e -> dispose());
        right.add(cancel);

        RoundedJButton save = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.edit.save"));
        save.addActionListener(e -> saveAction());
        right.add(save);
        footer.add(right, BorderLayout.EAST);

        return footer;
    }

    /** Opens a modal, scrollable overview of the dialog and what each tab controls. */
    private void showInstructions() {
        JEditorPane pane = new JEditorPane("text/html", getTextAt(RESOURCE_BUNDLE, "instructions.contractMarket.body"));
        pane.setEditable(false);
        pane.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(pane);
        scroll.setPreferredSize(new Dimension(scaleForGUI(580), scaleForGUI(520)));
        JOptionPane.showMessageDialog(this, scroll,
              getTextAt(RESOURCE_BUNDLE, "instructions.contractMarket.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Writes every edited field back onto the contract, then closes. The player's chosen negotiator and the StratCon
     * state attached to the contract are preserved untouched.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void saveAction() {
        // Identity
        String name = nameField.getText().trim();
        if (!name.isBlank()) {
            contract.setContractName(name);
        }
        contract.setDescription(descriptionArea.getText());
        // Only write status back when the field was editable; otherwise leave the contract's status as-is (an offer's
        // absent status, or an active contract's ACTIVE) rather than stamping the combo's fallback onto it.
        if (statusEditable) {
            contract.setStatus(statusValue(statusCombo));
        }

        // Parameters. The "Automatic" checkboxes exist only in create mode (see automaticToggle): a ticked box
        // (re)determines the value with the same rules the contract generator uses (AbstractContractGeneration),
        // while an unticked box writes the GM's manual spinner value. Scale is resolved before the required victory
        // points, which are derived from it. When editing an existing contract there are no checkboxes - the GM's
        // spinner values are written directly, with no regeneration.
        if (createMode) {
            contract.setScale(scaleAutomatic.isSelected()
                                    ? AbstractContractGeneration.determineScale(campaign, campaign.getPlayerForce(),
                  campaign.getPlayerForce().getForceDetachment().getHangar(), contract)
                                    : intValue(scaleSpinner));
            contract.setRequiredCombatElements(combatElementsAutomatic.isSelected()
                                                     ?
                                                     AbstractContractGeneration.determineRequiredCombatElements(campaign)
                                                     :
                                                     intValue(combatElementsSpinner));
            contract.setTrackCount(trackCountAutomatic.isSelected()
                                         ? AbstractContractGeneration.determineTrackCount(contract)
                                         : intValue(trackCountSpinner));
            contract.setRequiredVictoryPoints(victoryPointsAutomatic.isSelected()
                                                    ?
                                                    AbstractContractGeneration.determineRequiredVictoryPoints(contract)
                                                    :
                                                    intValue(victoryPointsSpinner));
        } else {
            contract.setScale(intValue(scaleSpinner));
            contract.setRequiredCombatElements(intValue(combatElementsSpinner));
            contract.setTrackCount(intValue(trackCountSpinner));
            contract.setRequiredVictoryPoints(intValue(victoryPointsSpinner));
        }

        // Target. Resolved before the schedule so an automatic start date can factor travel time to the chosen target.
        PlanetarySystem system = resolveSystem(systemField.getText());
        PlanetarySystem targetSystem = system != null ? system : contract.getTargetSystem();
        String systemId = targetSystem != null ? targetSystem.getId() : contract.getTargetSystemId();
        // An automatic planet is drawn for the resolved system with the generator's own rule; otherwise the GM's pick.
        Planet planet = (planetAutomatic != null && planetAutomatic.isSelected())
                              ? AbstractContractGeneration.determineTargetPlanet(contract, targetSystem, currentDate)
                              : (Planet) planetCombo.getSelectedItem();
        contract.setSystemsTargetData(new SystemsTargetData(systemId, planet == null ? null : planet.getId()));

        // Schedule. An automatic start is (re)derived from travel time to the target just saved; an automatic end is
        // the start plus the GM's chosen length. The date pickers keep at least a one-month span, but enforce it
        // defensively here too in case the loaded data predates the rule. lengthInMonths is derived from the dates
        // everywhere else in the model, so go through the copy constructor and let it recompute.
        LocalDate saveStart = startAutomatic.isSelected()
                                    ? AbstractContractGeneration.determineStartDate(campaign, contract)
                                    : startDate;
        LocalDate saveEnd = endAutomatic.isSelected()
                                  ? (saveStart == null ? null : saveStart.plusMonths(lengthValue()))
                                  : endDate;
        if (saveStart != null && saveEnd != null) {
            LocalDate earliestEnd = saveStart.plusMonths(MINIMUM_CONTRACT_LENGTH_MONTHS);
            if (saveEnd.isBefore(earliestEnd)) {
                saveEnd = earliestEnd;
            }
        }
        ContractScheduleData schedule = contract.getScheduleData();
        ContractScheduleData updatedSchedule = (schedule == null) ? new ContractScheduleData(null, null, 0) : schedule;
        if (saveStart != null) {
            updatedSchedule = updatedSchedule.withStartDate(saveStart);
        }
        if (saveEnd != null) {
            updatedSchedule = updatedSchedule.withEndDate(saveEnd);
        }
        contract.setScheduleData(updatedSchedule);

        // Employer
        EmployerData employer = contract.getEmployerData();
        contract.setEmployerData(new EmployerData(enumValue(employerTypeCombo, contract.getEmployerType()),
              factionKey(employerFactionCombo, contract.getEmployerFactionCode()),
              factionKey(employerAnchorCombo, employer == null ? null : employer.anchorFactionCode()),
              sponsorKey(employerSponsorCombo), text(employerDisplayNameField),
              employer == null ? null : employer.negotiator(), employer == null ? null : employer.liaison(),
              enumValue(employerSkillCombo, SkillLevel.REGULAR), equipmentValue(employerEquipmentCombo),
              employerCamouflage, enumValue(employerColorCombo, PlayerColour.BLUE)));

        // Enemy
        EnemyData enemy = contract.getEnemyData();
        // The batchall is only written when editable (an accepted contract against a Clan enemy); otherwise the
        // contract's existing value is preserved.
        boolean batchallAccepted = batchallEditable()
                                         ? batchallAcceptedCheckbox.isSelected()
                                         : (enemy != null && enemy.batchallAccepted());
        contract.setEnemyData(new EnemyData(factionKey(enemyFactionCombo, contract.getEnemyFactionCode()),
              sponsorKey(enemySponsorCombo), text(enemyDisplayNameField),
              enumValue(enemySkillCombo, SkillLevel.REGULAR), equipmentValue(enemyEquipmentCombo),
              enemy == null ? null : enemy.opposingCommander(), enemyCamouflage,
              enumValue(enemyColorCombo, PlayerColour.RED), batchallAccepted));

        // Terms
        contract.setContractTerms(new ContractTermsData(stepValue(payRateCombo), stepValue(supportCombo),
              stepValue(transportTermCombo), stepValue(salvageCombo), stepValue(commandCombo)));

        // Objectives
        contract.setObjectiveData(new ContractObjectiveData(objectiveValue(playerObjectiveCombo),
              objectiveValue(opposingObjectiveCombo)));

        // Finance (record components are ordered transport, monthlyPay, combatPay). A ticked "Automatic" box computes
        // the value with the generator's rule from the contract's just-saved scale, terms, and target; otherwise the
        // GM's spinner value is used.
        Money transportPay = transportPayAutomatic.isSelected()
                                   ? AbstractContractGeneration.determineTransportPay(campaign, contract)
                                   : moneyValue(transportPaySpinner);
        Money monthlyPayValue = monthlyPayAutomatic.isSelected()
                                      ? AbstractContractGeneration.determineMonthlyPay(campaign, contract)
                                      : moneyValue(monthlyPaySpinner);
        Money combatPayValue = combatPayAutomatic.isSelected()
                                     ? AbstractContractGeneration.determineCombatPay(campaign, contract)
                                     : moneyValue(combatPaySpinner);
        contract.setContractFinanceData(new ContractFinanceData(transportPay, monthlyPayValue, combatPayValue));

        // Intel obfuscation - which fields are hidden from the player in the market.
        contract.setIntelObfuscated(ObfuscatableIntel.ALLIED_COMMAND, obfuscateAlliedCommandCheckbox.isSelected());
        contract.setIntelObfuscated(ObfuscatableIntel.OPPOSITION, obfuscateOppositionCheckbox.isSelected());
        contract.setIntelObfuscated(ObfuscatableIntel.THREAT, obfuscateThreatCheckbox.isSelected());
        contract.setIntelObfuscated(ObfuscatableIntel.MORALE, obfuscateMoraleCheckbox.isSelected());

        // Facilities
        contract.setRentedFacilitiesData(new RentedFacilitiesData(intValue(hospitalBedsSpinner),
              intValue(kitchensSpinner), intValue(holdingCellsSpinner)));

        // Morale. The rout fields are only written when editable (an accepted contract); on an unstarted offer they
        // are disabled and the contract's existing rout values (an absent end date, a zero payout) are preserved.
        LocalDate routEndDate = routFieldsEditable
                                      ? parseDate(routEndDateField, contract.getRoutEndDate())
                                      : contract.getRoutEndDate();
        Money routPayout = routFieldsEditable ? moneyValue(routedPayoutSpinner) : contract.getRoutPayout();
        contract.setMoraleData(new MoraleData(enumValue(moraleLevelCombo, contract.getMoraleLevel()),
              routEndDate, routPayout));

        // Personnel. Create mode generates the NPCs now (from the just-applied factions) and layers the GM's overrides
        // on top; edit mode mutates the existing NPCs in place (their references are preserved by the rebuilds above).
        if (createMode) {
            generateAndApplyNpcs();
        } else {
            applyNpc(negotiatorEditor);
            applyNpc(liaisonEditor);
            applyNpc(commanderEditor);
        }

        LOGGER.info("GM {} contract: {}", createMode ? "created" : "edited", contract.getName());
        confirmed = true;
        dispose();
    }

    private static void applyNpc(NpcEditor editor) {
        if (editor != null) {
            editor.apply();
        }
    }

    /**
     * Generates the contract's NPCs from its now-final employer and enemy factions and the chosen market bucket, then
     * layers on the GM's name/portrait overrides, and stores them on the employer and enemy records.
     */
    private void generateAndApplyNpcs() {
        ContractSearchType bucket = getSelectedSearchType();
        Faction employerFaction = contract.getEmployerFaction();
        Faction enemyFaction = contract.getEnemyFaction();
        PlanetarySystem currentSystem = campaign.getCurrentSystem();

        Person negotiator = null;
        Person liaison = null;
        if (employerFaction != null && currentSystem != null) {
            HiringHallLevel hiringHall = currentSystem.getHiringHallLevel(currentDate);
            negotiator = EmployerNegotiator.generateNegotiator(campaign, bucket, employerFaction, hiringHall);
            liaison = EmployerLiaison.generateLiaison(campaign, bucket, employerFaction.isClan(),
                  employerFaction.getShortName());
        }
        Person commander = enemyFaction == null ? null
                                 : OpposingCommander.generateOpposingCommander(campaign, enemyFaction);

        negotiatorOverride.applyTo(negotiator);
        liaisonOverride.applyTo(liaison);
        commanderOverride.applyTo(commander);

        EmployerData employer = contract.getEmployerData();
        contract.setEmployerData(new EmployerData(employer.type(), employer.factionCode(),
              employer.anchorFactionCode(), employer.sponsorFactionCode(), employer.displayName(), negotiator, liaison,
              employer.forceSkill(), employer.equipmentRating(), employer.camouflage(), employer.color()));

        EnemyData enemy = contract.getEnemyData();
        contract.setEnemyData(new EnemyData(enemy.factionCode(), enemy.sponsorFactionCode(), enemy.displayName(),
              enemy.forceSkill(), enemy.equipmentRating(), commander, enemy.camouflage(), enemy.color(),
              enemy.batchallAccepted()));
    }

    // region Component builders

    /** Creates a fresh, vertically-stacked rows panel for a card's fields. */
    private static JPanel rowsPanel() {
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        return rows;
    }

    /** Wraps a rows panel in a subtly-bordered card; the enclosing tab supplies the section title. */
    private JPanel card(JPanel rows) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(RoundedLineBorder.createSubtleRoundedLineBorder(),
              BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)));
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel formRow(String labelKey, Component field) {
        JPanel row = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        row.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), 0, scaleForGUI(4), 0));

        JLabel label = new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey));
        // Explain the field on its label (leaving field-specific tooltips - term effects, date pickers, the automatic
        // checkboxes - untouched). Only set one when a "<labelKey>.tooltip" string actually exists.
        String tooltip = getTextAt(RESOURCE_BUNDLE, labelKey + ".tooltip");
        if (isResourceKeyValid(tooltip)) {
            label.setToolTipText(wordWrap(tooltip));
        }
        label.setPreferredSize(new Dimension(scaleForGUI(LABEL_WIDTH), label.getPreferredSize().height));
        label.setVerticalAlignment(JLabel.TOP);
        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private static JSpinner intSpinner(int value, int minimum) {
        return new JSpinner(new SpinnerNumberModel(max(value, minimum), minimum, Integer.MAX_VALUE, 1));
    }

    /**
     * Builds the "Automatic" checkbox that governs a field, or {@code null} when the dialog is editing an existing
     * contract rather than creating one. The checkbox (re)generates the value with the contract-generation rules,
     * which for some fields (combat elements, target planet) carry random variance - appropriate for a brand-new
     * offer, but not for a GM tuning an accepted contract's values by hand - so it is only offered in create mode.
     * When present it is ticked by default, which disables the field; unticking it hands control back to the GM.
     */
    private @Nullable JCheckBox automaticToggle(JComponent field) {
        if (!createMode) {
            return null;
        }
        JCheckBox automatic = new JCheckBox(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.automatic"), true);
        automatic.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.automatic.tooltip")));
        field.setEnabled(false); // ticked by default, so the field starts disabled
        automatic.addActionListener(e -> field.setEnabled(!automatic.isSelected()));
        return automatic;
    }

    /**
     * Lays a field beside its "Automatic" checkbox as a single component for {@link #formRow}. With no checkbox (a
     * parameter in edit mode) the field is returned on its own.
     */
    private JComponent withAutomatic(JComponent field, @Nullable JCheckBox automatic) {
        if (automatic == null) {
            return field;
        }
        JPanel panel = new JPanel(new BorderLayout(scaleForGUI(8), 0));
        panel.add(field, BorderLayout.CENTER);
        panel.add(automatic, BorderLayout.EAST);
        return panel;
    }

    private static JSpinner moneySpinner(Money money) {
        double amount = money == null ? 0.0 : money.getAmount().doubleValue();
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(amount, 0.0, Double.MAX_VALUE, (double) MONEY_STEP));
        // Without this the editor sizes itself to fit Double.MAX_VALUE, blowing the row far past the viewport.
        if (spinner.getEditor() instanceof JSpinner.NumberEditor editor) {
            editor.getTextField().setColumns(12);
        }
        return spinner;
    }

    /**
     * An "Automatic" checkbox for a finance field. Finance determination is deterministic (derived from scale, terms,
     * and the journey to the target), so unlike the parameter automatics it is offered in both modes - defaulting on
     * for a new offer and off when editing, so an existing contract's figures show as they stand. Ticking it disables
     * the spinner and, on save, (re)computes the value via {@link AbstractContractGeneration}.
     */
    private JCheckBox financeAutomatic(JSpinner spinner) {
        JCheckBox automatic = new JCheckBox(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.automatic"),
              createMode);
        automatic.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.automatic.tooltip")));
        spinner.setEnabled(!automatic.isSelected());
        automatic.addActionListener(e -> spinner.setEnabled(!automatic.isSelected()));
        return automatic;
    }

    private JTextField dateField(LocalDate date) {
        JTextField field = new JTextField(date == null ? "" : date.toString(), 12);
        field.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.date.tooltip")));
        return field;
    }

    /**
     * A faction picker keyed by faction code, seeded to the given code. Following the Faction Standings GM tool, it
     * lists only real factions active in the campaign year - Commands (dot-coded) and aggregate factions are excluded.
     * A code already on the contract that is no longer active is still added, so it stays visible for editing.
     */
    private FactionComboBox factionCombo(String selectedCode) {
        Collection<String> codes = activeFactionCodes();
        if (selectedCode != null && !selectedCode.isBlank() && !codes.contains(selectedCode)) {
            codes.add(selectedCode);
        }
        FactionComboBox combo = new FactionComboBox();
        combo.addFactionEntries(codes, campaign.getGameYear());
        combo.setSelectedItemByKey(selectedCode);
        return combo;
    }

    /** The codes of the real factions active in the campaign year (Commands and aggregate factions excluded). */
    private Collection<String> activeFactionCodes() {
        Collection<String> codes = new ArrayList<>();
        for (Faction faction : Factions.getInstance().getActiveFactions(currentDate)) {
            if (!faction.isAggregate()) {
                codes.add(faction.getShortName());
            }
        }
        return codes;
    }

    /**
     * A faction picker that leads with the synthetic "Custom Inner Sphere / Periphery / Clan" options, followed by the
     * real active factions. The value-sorting {@link FactionComboBox} model is replaced with an explicit ordered one so
     * the leading entries stay on top rather than being sorted in among the factions.
     *
     * @param selectedCode the code to preselect (a custom code, a faction code, or {@code null}/blank)
     * @param includeNone  whether to lead with a "None" entry before the customs (used for the sponsor pickers)
     */
    private FactionComboBox factionComboWithCustoms(@Nullable String selectedCode, boolean includeNone) {
        FactionComboBox combo = new FactionComboBox();
        DefaultComboBoxModel<Map.Entry<String, String>> model = new DefaultComboBoxModel<>();
        combo.setModel(model);

        if (includeNone) {
            model.addElement(new AbstractMap.SimpleEntry<>("",
                  getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.faction.none")));
        }
        model.addElement(new AbstractMap.SimpleEntry<>(CUSTOM_INNER_SPHERE_CODE,
              getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.faction.customInnerSphere")));
        model.addElement(new AbstractMap.SimpleEntry<>(CUSTOM_PERIPHERY_CODE,
              getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.faction.customPeriphery")));
        model.addElement(new AbstractMap.SimpleEntry<>(CUSTOM_CLAN_CODE,
              getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.faction.customClan")));
        for (Map.Entry<String, String> faction : factionEntries(selectedCode)) {
            model.addElement(faction);
        }

        if (selectedCode == null || selectedCode.isBlank()) {
            combo.setSelectedIndex(0);
        } else {
            combo.setSelectedItemByKey(selectedCode);
        }
        return combo;
    }

    /**
     * The real active-faction entries (code -&gt; display name), sorted by name, with name collisions disambiguated by
     * appending the code - the same treatment {@link FactionComboBox#addFactionEntries} gives them. A non-custom
     * {@code extraCode} that is not currently active is included so an already-set faction stays visible.
     */
    private List<Map.Entry<String, String>> factionEntries(@Nullable String extraCode) {
        Collection<String> codes = activeFactionCodes();
        codes.removeIf(this::isCustomFactionCode); // the customs are added separately, never duplicated here
        if (extraCode != null &&
                  !extraCode.isBlank() &&
                  !isCustomFactionCode(extraCode) &&
                  !codes.contains(extraCode)) {
            codes.add(extraCode);
        }

        int year = campaign.getGameYear();
        Map<String, String> names = new HashMap<>();
        Set<String> collisions = new HashSet<>();
        for (String code : codes) {
            String name = Factions.getInstance().getFaction(code).getFullName(year);
            if (names.containsValue(name)) {
                collisions.add(name);
            }
            names.put(code, name);
        }

        List<Map.Entry<String, String>> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            String label = collisions.contains(entry.getValue())
                                 ? entry.getValue() + " (" + entry.getKey() + ")"
                                 : entry.getValue();
            entries.add(new AbstractMap.SimpleEntry<>(entry.getKey(), label));
        }
        entries.sort(Map.Entry.comparingByValue());
        return entries;
    }

    private boolean isCustomFactionCode(String code) {
        return CUSTOM_INNER_SPHERE_CODE.equals(code) || CUSTOM_PERIPHERY_CODE.equals(code)
                     || CUSTOM_CLAN_CODE.equals(code);
    }

    /** A faction picker that also offers a leading "none" entry, for the optional covert sponsor. */
    /** The sponsor picker: a "None" entry, then the custom options, then the real active factions. */
    private FactionComboBox sponsorCombo(String selectedCode) {
        return factionComboWithCustoms(selectedCode, true);
    }

    private static <E> JComboBox<E> enumCombo(E[] values, E selected) {
        JComboBox<E> combo = new JComboBox<>(new DefaultComboBoxModel<>(values));
        combo.setSelectedItem(selected);
        return combo;
    }

    /** The employer-type picker, rendering each {@link ChaosEmployerType} with its player-facing label. */
    private JComboBox<ChaosEmployerType> employerTypeCombo(ChaosEmployerType selected) {
        JComboBox<ChaosEmployerType> combo = enumCombo(ChaosEmployerType.values(), selected);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ChaosEmployerType type) {
                    setText(type.toString());
                }
                return this;
            }
        });
        return combo;
    }

    /**
     * An equipment-rating picker showing the {@link DragoonRating} letter codes (F - A*), seeded from a numeric
     * rating.
     */
    private JComboBox<DragoonRating> equipmentRatingCombo(int rating) {
        JComboBox<DragoonRating> combo = enumCombo(DragoonRating.values(), DragoonRating.fromRating(rating));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DragoonRating dragoonRating) {
                    setText(dragoonRating.getLabel());
                }
                return this;
            }
        });
        return combo;
    }

    /** The numeric equipment rating for the combo's selection, defaulting to the middling C rating. */
    private static int equipmentValue(JComboBox<DragoonRating> combo) {
        DragoonRating rating = (DragoonRating) combo.getSelectedItem();
        return (rating == null ? DragoonRating.DRAGOON_C : rating).getRating();
    }

    /** Which single facet of a {@link ChaosContractStepsTable} step a Terms picker governs, for its effect tooltips. */
    private enum TermEffect {PAY_RATE, SUPPORT, TRANSPORT, SALVAGE, COMMAND}

    private JComboBox<ChaosContractStepsTable> stepsCombo(ChaosContractStepsTable selected, TermEffect term) {
        JComboBox<ChaosContractStepsTable> combo = enumCombo(ChaosContractStepsTable.values(), selected);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ChaosContractStepsTable step) {
                    // Per-option tooltip in the dropdown, showing only this term's effect for the hovered step.
                    setToolTipText(termEffectTooltip(term, step));
                    setText(getFormattedTextAt(RESOURCE_BUNDLE, "edit.contractMarket.stepLabel", step.stepValue()));
                }
                return this;
            }
        });
        // Mirror the selected option's effect onto the closed combo so it shows on hover without opening the dropdown.
        combo.addActionListener(e -> combo.setToolTipText(
              termEffectTooltip(term, (ChaosContractStepsTable) combo.getSelectedItem())));
        combo.setToolTipText(termEffectTooltip(term, selected));
        return combo;
    }

    /** The tooltip describing only the effect the given term draws from a step (pay multiplier, salvage %, and so on). */
    private @Nullable String termEffectTooltip(TermEffect term, @Nullable ChaosContractStepsTable step) {
        if (step == null) {
            return null;
        }
        String text = switch (term) {
            case PAY_RATE -> getFormattedTextAt(RESOURCE_BUNDLE, "edit.contractMarket.term.tooltip.payRate",
                  percent(step.getBasePayMultiplier()));
            case SUPPORT -> getFormattedTextAt(RESOURCE_BUNDLE, "edit.contractMarket.term.tooltip.support",
                  percent(step.getStraightSupportMultiplier()), percent(step.getBattlefieldLossMultiplier()));
            case TRANSPORT -> getFormattedTextAt(RESOURCE_BUNDLE, "edit.contractMarket.term.tooltip.transport",
                  percent(step.getTransportMultiplier()));
            case SALVAGE -> getFormattedTextAt(RESOURCE_BUNDLE,
                  step.isExchangeSalvage() ? "edit.contractMarket.term.tooltip.salvage.exchange"
                        : "edit.contractMarket.term.tooltip.salvage",
                  percent(step.getSalvageMultiplier()));
            case COMMAND -> getFormattedTextAt(RESOURCE_BUNDLE, "edit.contractMarket.term.tooltip.command",
                  step.getContractCommandRights().toString());
        };
        return wordWrap(text);
    }

    /** A 0-1 (or higher, for pay) multiplier as a whole-number percentage. */
    private static int percent(double multiplier) {
        return (int) Math.round(multiplier * 100);
    }

    private static JComboBox<ContractObjectiveType> objectiveCombo(ContractObjectiveType selected) {
        JComboBox<ContractObjectiveType> combo = new JComboBox<>(
              new DefaultComboBoxModel<>(ContractObjectiveType.values()));
        combo.setSelectedItem(selected);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ContractObjectiveType type) {
                    setText(type.toString());
                    setToolTipText(wordWrap(type.getToolTipText()));
                }
                return this;
            }
        });
        return combo;
    }

    /**
     * Builds a camouflage preview button. Clicking it opens the chooser seeded with the current selection; the button's
     * icon and the backing value are refreshed on confirmation.
     */
    private JButton camoButton(java.util.function.Supplier<Camouflage> current,
          java.util.function.Consumer<Camouflage> onChange) {
        JButton button = new JButton();
        applyIcon(button, current.get());
        button.addActionListener(e -> openCamoChooser(button, current, onChange));
        return button;
    }

    private void openCamoChooser(JButton button, java.util.function.Supplier<Camouflage> current,
          java.util.function.Consumer<Camouflage> onChange) {
        CamoChooserDialog chooser = new CamoChooserDialog(campaign.getGUI().getFrame(), current.get());
        if (chooser.showDialog().isConfirmed()) {
            Camouflage chosen = chooser.getSelectedItem();
            onChange.accept(chosen);
            applyIcon(button, chosen);
        }
    }

    private void applyIcon(JButton button, AbstractIcon icon) {
        int size = scaleForGUI(ICON_SIZE);
        ImageIcon image = icon == null ? null : icon.getImageIcon(size);
        button.setIcon(image);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setPreferredSize(new Dimension(size + scaleForGUI(12), size + scaleForGUI(8)));
    }

    private JComboBox<RankDisplay> rankCombo(Person person) {
        DefaultComboBoxModel<RankDisplay> model = new DefaultComboBoxModel<>();
        model.addAll(RankDisplay.getRankDisplaysForSystem(person.getRankSystem(),
              Profession.getProfessionFromPersonnelRole(person.getPrimaryRole())));
        JComboBox<RankDisplay> combo = new JComboBox<>(model);
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).rankNumeric() == person.getRankNumeric()) {
                combo.setSelectedIndex(i);
                break;
            }
        }
        return combo;
    }

    private NpcEditor npcEditor(Person person) {
        return person == null ? null : new NpcEditor(person);
    }

    /** The rank system of the faction currently selected in the given combo, or {@code null} if none is resolvable. */
    private static RankSystem rankSystemOf(FactionComboBox combo) {
        String code = combo.getSelectedItemKey();
        Faction faction = (code == null || code.isBlank()) ? null : Factions.getInstance().getFaction(code);
        return faction == null ? null : faction.getRankSystem();
    }

    // endregion Component builders

    // region Target helpers

    private PlanetarySystem resolveSystem(String name) {
        String trimmed = name == null ? "" : name.trim();
        return trimmed.isEmpty() ? null : campaign.getSystemByName(trimmed);
    }

    /** Fills the planet combo with a leading "none" entry then the given system's planets, selecting {@code selectId}. */
    private void repopulatePlanets(PlanetarySystem system, String selectId) {
        planetCombo.removeAllItems();
        planetCombo.addItem(null);
        if (system == null) {
            return;
        }
        for (Planet planet : system.getPlanets()) {
            planetCombo.addItem(planet);
            if (planet.getId().equals(selectId)) {
                planetCombo.setSelectedItem(planet);
            }
        }
    }

    // endregion Target helpers

    // region Value helpers

    private static int intValue(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    private static Money moneyValue(JSpinner spinner) {
        return Money.of(((Number) spinner.getValue()).doubleValue());
    }

    private static <E> E enumValue(JComboBox<E> combo, E fallback) {
        int index = combo.getSelectedIndex();
        return index < 0 ? fallback : combo.getItemAt(index);
    }

    private static ChaosContractStepsTable stepValue(JComboBox<ChaosContractStepsTable> combo) {
        return enumValue(combo, ChaosContractStepsTable.STEP_ONE);
    }

    /**
     * Falls back to {@link MissionStatus#ACTIVE} rather than the contract's current status: a contract being edited
     * here is one the campaign holds, and the editor must not be able to write a status back out that is absent.
     */
    private static MissionStatus statusValue(JComboBox<MissionStatus> combo) {
        return enumValue(combo, MissionStatus.ACTIVE);
    }

    private static ContractObjectiveType objectiveValue(JComboBox<ContractObjectiveType> combo) {
        return enumValue(combo, ContractObjectiveType.UNDEFINED);
    }

    /** The picked faction code, or {@code fallback} when nothing (or the blank entry) is selected. */
    private static String factionKey(FactionComboBox combo, String fallback) {
        String key = combo.getSelectedItemKey();
        return (key == null || key.isBlank()) ? fallback : key;
    }

    /** The picked sponsor faction code, or {@code null} when the "none" entry is selected. */
    private static String sponsorKey(FactionComboBox combo) {
        String key = combo.getSelectedItemKey();
        return (key == null || key.isBlank()) ? null : key;
    }

    /** Trimmed field text (never null; blank stays blank). */
    private static String text(JTextField field) {
        return field.getText().trim();
    }

    /** Parses an ISO date from a field, treating blank as {@code null} and an unparseable value as {@code fallback}. */
    private static LocalDate parseDate(JTextField field, LocalDate fallback) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    // endregion Value helpers

    private static Color contrastingText(Color background) {
        double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen()
                                  + 0.114 * background.getBlue()) / 255.0;
        return luminance > 0.6 ? Color.BLACK : Color.WHITE;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Editor controls for one generated NPC (name, rank, and portrait). The portrait is held locally and only written
     * back on save, so cancelling the dialog leaves the NPC untouched.
     */
    private final class NpcEditor {
        private final transient Person person;
        private final JTextField givenNameField;
        private final JTextField surnameField;
        private final JComboBox<RankDisplay> ranks;
        private final JButton portraitButton;
        private transient Portrait portrait;

        private NpcEditor(Person person) {
            this.person = person;
            this.givenNameField = new JTextField(person.getGivenName(), 16);
            this.surnameField = new JTextField(person.getSurname(), 16);
            this.ranks = rankCombo(person);
            this.portrait = person.getPortrait();
            this.portraitButton = new JButton();
            applyIcon(portraitButton, portrait);
            portraitButton.addActionListener(e -> {
                PortraitChooserDialog chooser = new PortraitChooserDialog(campaign.getGUI().getFrame(), portrait);
                if (chooser.showDialog().isConfirmed()) {
                    portrait = chooser.getSelectedItem();
                    applyIcon(portraitButton, portrait);
                }
            });
        }

        private JPanel buildCard() {
            JPanel rows = rowsPanel();
            rows.add(formRow("edit.contractMarket.field.givenName", givenNameField));
            rows.add(formRow("edit.contractMarket.field.surname", surnameField));
            rows.add(formRow("edit.contractMarket.field.rank", ranks));
            rows.add(formRow("edit.contractMarket.field.portrait", portraitButton));
            return card(rows);
        }

        private void apply() {
            person.setGivenName(givenNameField.getText().trim());
            person.setSurname(surnameField.getText().trim());
            RankDisplay rank = (RankDisplay) ranks.getSelectedItem();
            if (rank != null) {
                person.setRank(rank.rankNumeric());
            }
            if (portrait != null) {
                person.setPortrait(portrait);
            }
        }
    }

    /**
     * Create-mode capture for one NPC that does not exist yet: the optional name, rank, and portrait the GM wants the
     * generated NPC to use. The rank options track the driving faction's rank system (refreshed via
     * {@link #refreshRanks(RankSystem)} when that faction changes). Any field left blank - or rank left on "(use
     * generated)" - leaves the corresponding generated value untouched.
     */
    private final class NpcOverride {
        private final JTextField givenNameField = new JTextField(16);
        private final JTextField surnameField = new JTextField(16);
        private final JComboBox<RankDisplay> ranks = new JComboBox<>();
        private final JButton portraitButton = new JButton();
        private transient Portrait portrait;
        private JCheckBox givenNameAutomatic;
        private JCheckBox surnameAutomatic;
        private JCheckBox rankAutomatic;
        private JCheckBox portraitAutomatic;

        private NpcOverride() {
            applyIcon(portraitButton, new Portrait()); // placeholder; portrait stays null until the GM picks one
            givenNameField.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "create.contractMarket.npc.tooltip")));
            surnameField.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "create.contractMarket.npc.tooltip")));
            refreshRanks(null);
            portraitButton.addActionListener(e -> {
                PortraitChooserDialog chooser = new PortraitChooserDialog(campaign.getGUI().getFrame(),
                      portrait == null ? new Portrait() : portrait);
                if (chooser.showDialog().isConfirmed()) {
                    portrait = chooser.getSelectedItem();
                    applyIcon(portraitButton, portrait);
                }
            });
        }

        /**
         * Repopulates the rank options for the given rank system. Whether the generated rank is kept is governed by the
         * field's "Automatic" checkbox, so no synthetic "(use generated)" entry is needed here.
         */
        private void refreshRanks(RankSystem rankSystem) {
            DefaultComboBoxModel<RankDisplay> model = new DefaultComboBoxModel<>();
            if (rankSystem != null) {
                model.addAll(RankDisplay.getRankDisplaysForSystem(rankSystem, DEFAULT_PROFESSION));
            }
            ranks.setModel(model);
            if (model.getSize() > 0) {
                ranks.setSelectedIndex(0);
            }
        }

        private JPanel buildCard() {
            JPanel rows = rowsPanel();
            givenNameAutomatic = automaticToggle(givenNameField);
            rows.add(formRow("edit.contractMarket.field.givenName", withAutomatic(givenNameField, givenNameAutomatic)));
            surnameAutomatic = automaticToggle(surnameField);
            rows.add(formRow("edit.contractMarket.field.surname", withAutomatic(surnameField, surnameAutomatic)));
            rankAutomatic = automaticToggle(ranks);
            rows.add(formRow("edit.contractMarket.field.rank", withAutomatic(ranks, rankAutomatic)));
            portraitAutomatic = automaticToggle(portraitButton);
            rows.add(formRow("edit.contractMarket.field.portrait", withAutomatic(portraitButton, portraitAutomatic)));
            return card(rows);
        }

        /** Applies each field the GM took off "Automatic" onto the freshly-generated NPC, leaving the rest generated. */
        private void applyTo(Person generated) {
            if (generated == null) {
                return;
            }
            if (!givenNameAutomatic.isSelected()) {
                String givenName = givenNameField.getText().trim();
                if (!givenName.isEmpty()) {
                    generated.setGivenName(givenName);
                }
            }
            if (!surnameAutomatic.isSelected()) {
                String surname = surnameField.getText().trim();
                if (!surname.isEmpty()) {
                    generated.setSurname(surname);
                }
            }
            if (!rankAutomatic.isSelected()) {
                RankDisplay rank = (RankDisplay) ranks.getSelectedItem();
                if (rank != null) {
                    generated.setRank(rank.rankNumeric());
                }
            }
            if (!portraitAutomatic.isSelected() && portrait != null) {
                generated.setPortrait(portrait);
            }
        }
    }
}
