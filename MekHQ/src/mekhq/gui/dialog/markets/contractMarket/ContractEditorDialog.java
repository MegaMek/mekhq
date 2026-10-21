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
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

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
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConScenarioTempo;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.*;
import mekhq.campaign.mission.contract.contractGeneration.AbstractContractDeterminationPay;
import mekhq.campaign.mission.contract.contractGeneration.AbstractContractGeneration;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractDeterminationDifficulty;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractDeterminationScale;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.EmployerLiaison;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.EmployerNegotiator;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.OpposingCommander;
import mekhq.campaign.mission.contract.utilities.ContractCharacteristics;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import mekhq.gui.FactionComboBox;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudDateField;
import mekhq.gui.baseComponents.hud.HudModeSelector;
import mekhq.gui.baseComponents.hud.HudSegmentedControl;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;
import mekhq.gui.baseComponents.hud.HudStyle;
import mekhq.gui.dialog.DateChooser;
import mekhq.gui.dialog.markets.contractMarket.EditorHud.Chip;
import mekhq.gui.dialog.markets.contractMarket.EditorHud.Pill;
import mekhq.gui.dialog.markets.contractMarket.EditorHud.PillKind;
import mekhq.gui.dialog.markets.contractMarket.EditorHud.Row;
import mekhq.gui.dialog.markets.contractMarket.EditorHud.StepTrack;
import mekhq.gui.displayWrappers.RankDisplay;
import mekhq.gui.utilities.JSuggestField;
import mekhq.gui.utilities.SkillLevelPickerUtility;
import mekhq.gui.utilities.WrapLayout;

/**
 * A GM-only editor for a single {@link AbstractContract}, drawn as a heads-up display in the style of the contract
 * market. It surfaces essentially every field a game master would want to hand-tune - identity, objectives, target,
 * schedule, size, employer and enemy make-up (including camouflage and force colour), intel the player is shown, terms,
 * pay, rented facilities, morale, characteristics and the generated NPCs - so an offer can be reshaped to their
 * specifications. Faction and system fields are pickers rather than free text to prevent typos.
 *
 * <p>The form is split across six tabs, chosen from the selector in the command bar as the Oracle Console's pages are,
 * with a preview on the right of how the offer will read on the board and in the dossier. Each tab has a guide that
 * slides over the window, opened by the guide button or by itself the first time the tab is visited. Each row that
 * differs from the contract as it was opened is marked, each tab counts its changes, and Revert puts every field
 * back.</p>
 *
 * <p>The generated NPC personnel attached to a contract (the employer's negotiator and liaison, and the opposing
 * commander) can have their name, rank, and portrait edited. On confirmation the contract is mutated in place; query
 * {@link #wasConfirmed()} afterward and rebuild any view showing it. The player's chosen negotiator is preserved
 * as-is. The StratCon campaign state is not edited here, but when its contract's scale, track count, or dates change,
 * its pre-rolled schedule is rolled again to match (see {@link StratConScenarioTempo#regenerateSchedules}).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractEditorDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(ContractEditorDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    private static final int MONEY_STEP = 1000;
    private static final double FRACTIONAL_SCALE_STEP = 0.1;
    private static final int LABEL_WIDTH = 120;
    private static final int PREVIEW_WIDTH = 300;
    private static final int COLUMN_GAP = 22;
    /** The end date is held at least this many months after the start date. */
    private static final int MINIMUM_CONTRACT_LENGTH_MONTHS = 1;

    /** Synthetic "Custom" faction codes offered as leading options in the employer and enemy faction pickers. */
    private static final String CUSTOM_INNER_SPHERE_CODE = "IS";
    private static final String CUSTOM_PERIPHERY_CODE = "Periphery";
    private static final String CUSTOM_CLAN_CODE = "CLAN";
    private static final int ICON_SIZE = 48;
    /** Profession used to label ranks in the create-mode NPC pickers; the numeric rank is what actually applies. */
    private static final Profession DEFAULT_PROFESSION = Profession.MEKWARRIOR;

    /** The form's tabs, in order, keyed for the tab selector, the change counts and the guides. */
    enum Section {
        OFFER, JOB, EMPLOYER, ENEMY, TERMS, CHARACTERISTICS;

        String label() {
            return getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.page." + name());
        }

        /** @return the label of the section's guide button, such as "How the offer works" */
        String guideLabel() {
            return getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.page." + name() + ".guide");
        }

        /** @return the resource-key prefix the section's guide is read from */
        String guidePrefix() {
            return "guide.contractEditor." + name().toLowerCase(Locale.ROOT) + ".";
        }
    }

    private final transient Campaign campaign;
    private final transient AbstractContract contract;
    private final transient LocalDate currentDate;
    private final boolean createMode;
    private final transient ContractSearchType initialBucket;

    // Identity
    private HudSegmentedControl<ContractSearchType> bucketPicker; // create mode only
    private JTextField nameField;
    private JTextArea descriptionArea;
    private JComboBox<MissionStatus> statusCombo;
    private boolean statusEditable;
    private HudSegmentedControl<ContractNature> naturePicker;
    private boolean natureEditable;

    // Parameters
    private JSpinner scaleSpinner;
    /** The Scale the spinner opened on, so saving an untouched spinner keeps the exact Scale it was rounded from */
    private double initialScaleSpinnerValue;
    private JCheckBox scaleAutomatic;
    /** Not shown - track count has no effect yet - but kept so its value (or create-mode roll) is still saved. */
    private JSpinner trackCountSpinner;
    private JCheckBox trackCountAutomatic;
    private JSpinner victoryPointsSpinner;
    private JCheckBox victoryPointsAutomatic;

    // Schedule
    private HudButton startDateButton;
    private JCheckBox startAutomatic;
    private HudButton endDateButton;
    private JCheckBox endAutomatic;
    private transient LocalDate startDate;
    private transient LocalDate endDate;
    private JSpinner lengthSpinner;
    private JCheckBox lengthAutomatic;
    /**
     * The generator's length, rolled once when the length goes on "Automatic". Rolling it on every read would give the
     * end date, the length shown and the length saved different values when contract lengths vary.
     */
    private @Nullable Integer automaticLengthMonths;

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
    private HudSegmentedControl<DragoonRating> employerEquipmentPicker;
    private JComboBox<PlayerColour> employerColorCombo;
    private JButton employerCamoButton;
    private transient Camouflage employerCamouflage;

    // Enemy
    private FactionComboBox enemyFactionCombo;
    private FactionComboBox enemySponsorCombo;
    private JTextField enemyDisplayNameField;
    private JComboBox<SkillLevel> enemySkillCombo;
    private HudSegmentedControl<DragoonRating> enemyEquipmentPicker;
    private HudCheckBox batchallAcceptedCheckbox;
    private Row batchallRow;
    private JComboBox<PlayerColour> enemyColorCombo;
    private JButton enemyCamoButton;
    private transient Camouflage enemyCamouflage;

    // Terms
    private StepTrack payRateTrack;
    private StepTrack supportTrack;
    private StepTrack transportTermTrack;
    private StepTrack salvageTrack;
    private StepTrack commandTrack;
    private JCheckBox payNonNegotiableCheck;
    private JCheckBox supportNonNegotiableCheck;
    private JCheckBox transportNonNegotiableCheck;
    private JCheckBox salvageNonNegotiableCheck;
    private JCheckBox commandNonNegotiableCheck;

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

    /** One chip per characteristic; within a category at most one is chosen, with a "None" chip standing for none. */
    private final Map<ContractCharacteristic, Chip> characteristicCheckboxes =
          new EnumMap<>(ContractCharacteristic.class);
    private final Map<ContractCharacteristic.Category, Chip> noneChips =
          new EnumMap<>(ContractCharacteristic.Category.class);

    // Facilities
    private JSpinner hospitalBedsSpinner;
    private JSpinner kitchensSpinner;
    private JSpinner holdingCellsSpinner;

    // Morale
    private HudSegmentedControl<ContractMoraleLevel> moralePicker;
    private HudDateField routEndDateField;
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

    // Layout and change tracking
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageHolder = new JPanel(pageLayout);
    private final JPanel previewHolder = new JPanel(new BorderLayout());
    /**
     * The preview's content, refilled on every edit inside one long-lived scroll pane, so the preview keeps its scroll
     * position while the GM types.
     */
    private final JPanel previewContent = MarketHud.column();
    private final List<Tracked> tracked = new ArrayList<>();
    private HudModeSelector<Section> sectionSelector;
    private Section buildingSection = Section.OFFER;
    private boolean building;
    private JLabel titleLabel;
    private JLabel changesValue;
    private HudButton guideButton;
    private MarketGuide guide;
    /** The tab on screen, whose guide the guide button opens. */
    private Section currentSection = Section.OFFER;
    private HudButton revertButton;
    private HudButton saveButton;

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
     * and header eyebrow change and a market-list picker (seeded to {@code createBucket}) is shown, letting the GM
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
        if (bucketPicker != null && bucketPicker.getSelected() != null) {
            return bucketPicker.getSelected();
        }
        return initialBucket;
    }

    /** @return whether the GM confirmed their edits. */
    public boolean wasConfirmed() {
        return confirmed;
    }

    // region Layout

    private void initializeComponents() {
        setTitle(getTextAt(RESOURCE_BUNDLE, createMode ? "title.contractMarket.create" : "title.contractMarket.edit"));

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(buildCommandBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(true);
        body.setBackground(SURFACE_DEEP);
        pageHolder.setOpaque(true);
        pageHolder.setBackground(SURFACE_DEEP);
        body.add(pageHolder, BorderLayout.CENTER);
        previewHolder.setOpaque(true);
        previewHolder.setBackground(GROUND);
        previewHolder.setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));
        previewHolder.setPreferredSize(new Dimension(scaleForGUI(PREVIEW_WIDTH), 0));
        previewContent.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(14), scaleForGUI(16),
              scaleForGUI(14)));
        previewHolder.add(MarketHud.scroll(previewContent, GROUND), BorderLayout.CENTER);
        body.add(previewHolder, BorderLayout.EAST);
        content.add(body, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);

        guide = new MarketGuide(this);
        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              "closeGuide");
        content.getActionMap().put("closeGuide", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                guide.close();
            }
        });
        buildForm();

        setMinimumSize(scaleForGUI(1100, 640));
        pack();
        setSize(scaleForGUI(1240, 780)); // Default opening size; saved preferences (below) override it on later opens
        setLocationRelativeTo(getParent());
        setPreferences(); // Must be before setVisible
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                maybeOpenGuide(currentSection);
            }
        });
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
     * The market-style command bar: what is being edited, the live title and the change count, over the tab selector
     * and the guide button for the tab on screen.
     */
    private JPanel buildCommandBar() {
        JPanel bar = new JPanel(new BorderLayout(0, scaleForGUI(10)));
        bar.setOpaque(true);
        bar.setBackground(GROUND);
        bar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), scaleForGUI(10), scaleForGUI(14))));

        JPanel identity = MarketHud.column();
        identity.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE,
              createMode ? "create.contractMarket.eyebrow" : "edit.contractMarket.eyebrow"))));
        identity.add(Box.createVerticalStrut(scaleForGUI(2)));
        titleLabel = new JLabel();
        titleLabel.setForeground(ACCENT_BRIGHT);
        titleLabel.setFont(hudFont(Font.BOLD, 1.12f, 0.12f));
        titleLabel.setIconTextGap(scaleForGUI(8));
        identity.add(leftAligned(titleLabel));

        JPanel changes = MarketHud.readout(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.readout.changes"), "0",
              AMBER, SURFACE_DEEP);
        changesValue = (JLabel) changes.getComponent(1);
        String statusText = contract.getStatus() == null
                                  ? getTextAt(RESOURCE_BUNDLE, createMode
                                                                     ? "edit.contractMarket.readout.status.new"
                                                                     : "edit.contractMarket.readout.status.offer")
                                  : contract.getStatus().toString();
        JPanel status = MarketHud.readout(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.readout.status"),
              statusText, ACCENT_BRIGHT, SURFACE_DEEP);
        JPanel readouts = Hud.transparentPanel(null);
        readouts.setLayout(new BoxLayout(readouts, BoxLayout.X_AXIS));
        readouts.add(changes);
        readouts.add(Box.createHorizontalStrut(scaleForGUI(6)));
        readouts.add(status);

        JPanel topRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(20), 0));
        topRow.add(identity, BorderLayout.CENTER);
        topRow.add(readouts, BorderLayout.EAST);

        sectionSelector = new HudModeSelector<>(Section.class, section -> sectionLabel(section, 0),
              this::showSection);
        sectionSelector.setSelected(currentSection);
        guideButton = new HudButton("", false, true);
        guideButton.addActionListener(event -> openGuide(currentSection));
        refreshGuideButton();
        JPanel tabsRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        tabsRow.add(sectionSelector, BorderLayout.CENTER);
        tabsRow.add(guideButton, BorderLayout.EAST);

        bar.add(topRow, BorderLayout.CENTER);
        bar.add(tabsRow, BorderLayout.SOUTH);
        return bar;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(14), scaleForGUI(8), scaleForGUI(14))));

        footer.add(Hud.hint(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.footer.legend")), BorderLayout.CENTER);

        revertButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.edit.revert")
                                           .toUpperCase(Locale.ROOT), false);
        revertButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.edit.revert.tooltip"));
        revertButton.addActionListener(event -> buildForm());
        HudButton cancel = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.edit.cancel")
                                               .toUpperCase(Locale.ROOT), false);
        cancel.addActionListener(event -> dispose());
        saveButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.edit.save")
                                         .toUpperCase(Locale.ROOT), true);
        saveButton.addActionListener(event -> saveAction());

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(revertButton);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(saveButton);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    /**
     * Builds (or, for Revert, rebuilds) the whole form from the contract. The contract is only written on Save, so
     * rebuilding puts every field back to how the editor opened.
     */
    private void buildForm() {
        building = true;
        tracked.clear();
        characteristicCheckboxes.clear();
        noneChips.clear();

        pageHolder.removeAll();
        addSection(Section.OFFER, this::buildOfferSection);
        addSection(Section.JOB, this::buildJobSection);
        addSection(Section.EMPLOYER, this::buildEmployerSection);
        addSection(Section.ENEMY, this::buildEnemySection);
        addSection(Section.TERMS, this::buildTermsSection);
        addSection(Section.CHARACTERISTICS, this::buildCharacteristicsSection);
        pageLayout.show(pageHolder, currentSection.name());

        for (Tracked entry : tracked) {
            entry.initial = entry.value.get();
        }
        building = false;
        onEdited();
        pageHolder.revalidate();
        pageHolder.repaint();
    }

    /** Builds a tab's page, scrolling on its own, and adds it to the tab pages under the tab's name. */
    private void addSection(Section section, Consumer<JPanel> builder) {
        buildingSection = section;
        JPanel panel = MarketHud.column();
        panel.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(16), scaleForGUI(18), scaleForGUI(40),
              scaleForGUI(18)));
        panel.add(leftAligned(Hud.sectionHeading(section.label())));
        panel.add(Box.createVerticalStrut(scaleForGUI(4)));
        builder.accept(panel);
        pageHolder.add(MarketHud.scroll(panel, SURFACE_DEEP), section.name());
    }

    // endregion Layout

    // region Sections

    private void buildOfferSection(JPanel section) {
        if (createMode) {
            bucketPicker = segments(ContractSearchType.values(), ChaosContractMarketDialog::searchTypeLabel,
                  initialBucket);
            Row row = row("edit.contractMarket.field.bucket", bucketPicker, null);
            track(row, bucketPicker::getSelected);
            section.add(leftAligned(row));
        } else {
            bucketPicker = null;
        }

        nameField = new JTextField(contract.getName());
        EditorHud.styleField(nameField);
        section.add(leftAligned(trackedRow("edit.contractMarket.field.name", nameField, null, nameField::getText,
              nameField)));

        // A contract's type only shapes how its enemy is chosen while it is still an offer, so it is fixed afterwards.
        natureEditable = contract.getStatus() == null;
        naturePicker = segments(ContractNature.values(),
              nature -> getTextAt(RESOURCE_BUNDLE, "contractNature." + nature.name()), contract.getNature());
        if (!natureEditable) {
            for (ContractNature nature : ContractNature.values()) {
                naturePicker.setSegmentEnabled(nature, false);
            }
        }
        Row natureRow = row("edit.contractMarket.field.nature", naturePicker,
              natureEditable ? null : getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.note.nature"));
        track(natureRow, naturePicker::getSelected);
        section.add(leftAligned(natureRow));

        // A market offer has no status, and an accepted-but-active contract is finalized through the normal completion
        // flow, so status is only hand-editable once the contract has actually concluded. Even then it may only move
        // between concluded outcomes - a concluded contract can never be turned back to ACTIVE - so the enabled picker
        // offers the completed statuses alone. When not editable the field is disabled (showing the current value),
        // never hidden.
        MissionStatus status = contract.getStatus();
        statusEditable = status != null && status.isCompleted();
        MissionStatus[] statusOptions = statusEditable
                                              ? Arrays.stream(MissionStatus.values())
                                                      .filter(MissionStatus::isCompleted)
                                                      .toArray(MissionStatus[]::new)
                                              : MissionStatus.values();
        statusCombo = enumCombo(statusOptions, status, value -> value == null
                                                                       ? getTextAt(RESOURCE_BUNDLE,
              "edit.contractMarket.readout.status.offer")
                                                                       : value.toString());
        statusCombo.setEnabled(statusEditable);
        section.add(leftAligned(trackedRow("edit.contractMarket.field.status", statusCombo,
              statusEditable ? null : getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.note.status"),
              statusCombo::getSelectedItem, statusCombo)));

        descriptionArea = new JTextArea(contract.getDescription() == null ? "" : contract.getDescription(), 4, 20);
        EditorHud.styleTextArea(descriptionArea);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        Hud.styleScroll(descriptionScroll, GROUND, true);
        descriptionScroll.setPreferredSize(new Dimension(scaleForGUI(300), scaleForGUI(92)));
        section.add(leftAligned(trackedRow("edit.contractMarket.field.description", descriptionScroll,
              getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.note.description"), descriptionArea::getText,
              descriptionArea)));
    }

    private void buildJobSection(JPanel section) {
        Grid grid = new Grid();
        playerObjectiveCombo = objectiveCombo(contract.getObjectiveType());
        grid.add(trackedRow("edit.contractMarket.field.playerObjective", playerObjectiveCombo, null,
              playerObjectiveCombo::getSelectedItem, playerObjectiveCombo));
        opposingObjectiveCombo = objectiveCombo(contract.getOpposingObjectiveType());
        grid.add(trackedRow("edit.contractMarket.field.opposingObjective", opposingObjectiveCombo, null,
              opposingObjectiveCombo::getSelectedItem, opposingObjectiveCombo));
        section.add(leftAligned(grid.panel));

        section.add(leftAligned(subHeading("edit.contractMarket.sub.where")));
        grid = new Grid();
        buildTargetRows(grid);
        section.add(leftAligned(grid.panel));

        section.add(leftAligned(subHeading("edit.contractMarket.sub.when")));
        grid = new Grid();
        buildScheduleRows(grid);
        section.add(leftAligned(grid.panel));

        section.add(leftAligned(subHeading("edit.contractMarket.sub.size")));
        grid = new Grid();
        buildParameterRows(grid);
        section.add(leftAligned(grid.panel));
    }

    private void buildParameterRows(Grid grid) {
        scaleSpinner = scaleSpinner(contract.getExactScale(),
              campaign.getCampaignOptions().get(CampaignOption.USE_FRACTIONAL_SCALE));
        initialScaleSpinnerValue = ((Number) scaleSpinner.getValue()).doubleValue();
        scaleAutomatic = automaticToggle(scaleSpinner);
        grid.add(trackedRow("edit.contractMarket.field.scale", withAutomatic(scaleSpinner, scaleAutomatic), null,
              () -> List.of(scaleSpinner.getValue(), automaticState(scaleAutomatic)), scaleSpinner, scaleAutomatic));

        // Track count has no effect on play yet, so it is not shown; its value (or a create-mode roll) still saves.
        trackCountSpinner = intSpinner(contract.getTrackCount(), 0);
        trackCountAutomatic = automaticToggle(trackCountSpinner);

        victoryPointsSpinner = intSpinner(contract.getRequiredVictoryPoints(), 0);
        victoryPointsAutomatic = automaticToggle(victoryPointsSpinner);
        grid.add(trackedRow("edit.contractMarket.field.victoryPoints",
              withAutomatic(victoryPointsSpinner, victoryPointsAutomatic), null,
              () -> List.of(victoryPointsSpinner.getValue(), automaticState(victoryPointsAutomatic)),
              victoryPointsSpinner, victoryPointsAutomatic));
    }

    private void buildScheduleRows(Grid grid) {
        ContractScheduleData schedule = contract.getScheduleData();

        startDate = schedule == null ? null : schedule.startDate();
        endDate = schedule == null ? null : schedule.endDate();

        // Automatic start/end are offered in both modes since they are deterministic (start from travel time, end from
        // the length). They default on for a brand-new offer and off when editing, so an existing contract's dates show
        // as they stand.
        startDateButton = dateButton(this::pickStartDate);
        startAutomatic = scheduleAutomaticToggle(this::onStartAutomaticToggled);
        Row startRow = row("edit.contractMarket.field.startDate", EditorHud.line(startDateButton, startAutomatic),
              null);
        track(startRow, () -> List.of(String.valueOf(startDate), startAutomatic.isSelected()));
        grid.add(startRow);

        endDateButton = dateButton(this::pickEndDate);
        endAutomatic = scheduleAutomaticToggle(this::onEndAutomaticToggled);
        Row endRow = row("edit.contractMarket.field.endDate", EditorHud.line(endDateButton, endAutomatic), null);
        track(endRow, () -> List.of(String.valueOf(endDate), endAutomatic.isSelected()));
        grid.add(endRow);

        // The length is derived from the dates when the end date is set by hand, but becomes the GM's own input that
        // drives the end date when the end date is automatic. Its own "Automatic" box instead (re)derives the length
        // from the generator - and bakes any Quick Job / Long Haul characteristic - the way pay and objectives do.
        lengthSpinner = intSpinner(max(MINIMUM_CONTRACT_LENGTH_MONTHS, contract.getLengthInMonths()),
              MINIMUM_CONTRACT_LENGTH_MONTHS);
        lengthSpinner.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.length.tooltip")));
        lengthSpinner.addChangeListener(e -> onLengthChanged());
        lengthAutomatic = scheduleAutomaticToggle(this::onLengthAutomaticToggled);
        grid.add(trackedRow("edit.contractMarket.field.length", withAutomatic(lengthSpinner, lengthAutomatic),
              getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.note.length"),
              () -> List.of(lengthSpinner.getValue(), lengthAutomatic.isSelected()), lengthSpinner,
              lengthAutomatic));

        // Seed the automatic values so the initial display already reflects the checkbox defaults.
        if (startAutomatic.isSelected()) {
            startDate = AbstractContractGeneration.determineStartDate(campaign, contract);
        }
        if ((endAutomatic.isSelected() || lengthAutomatic.isSelected()) && startDate != null) {
            endDate = startDate.plusMonths(effectiveLengthMonths());
        }
        refreshScheduleControls();
    }

    private void buildTargetRows(Grid grid) {
        systemField = new JSuggestField(this, campaign.getSystemNames());
        EditorHud.styleField(systemField);
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
        grid.add(trackedRow("edit.contractMarket.field.systemId", systemField, null, systemField::getText,
              systemField));

        planetCombo = new JComboBox<>();
        EditorHud.styleCombo(planetCombo, value -> value instanceof Planet planet
                                                         ? planet.getName(currentDate)
                                                         : getTextAt(RESOURCE_BUNDLE,
              "edit.contractMarket.planet.none"));
        repopulatePlanets(targetSystem, contract.getTargetPlanetId());
        planetAutomatic = automaticToggle(planetCombo);
        grid.add(trackedRow("edit.contractMarket.field.planetId", withAutomatic(planetCombo, planetAutomatic), null,
              () -> List.of(String.valueOf(planetCombo.getSelectedItem()), automaticState(planetAutomatic)),
              planetCombo, planetAutomatic));
    }

    private void buildEmployerSection(JPanel section) {
        EmployerData data = contract.getEmployerData();
        Grid grid = new Grid();

        employerTypeCombo = enumCombo(ChaosEmployerType.values(), contract.getEmployerType(),
              type -> type == null ? "" : type.toString());
        grid.add(trackedRow("edit.contractMarket.field.employerType", employerTypeCombo, null,
              employerTypeCombo::getSelectedItem, employerTypeCombo));

        employerFactionCombo = factionComboWithCustoms(contract.getEmployerFactionCode(), false);
        grid.add(trackedRow("edit.contractMarket.field.factionCode", employerFactionCombo, null,
              employerFactionCombo::getSelectedItem, employerFactionCombo));

        employerAnchorCombo = factionCombo(data == null ? null : data.anchorFactionCode());
        grid.add(trackedRow("edit.contractMarket.field.anchorFactionCode", employerAnchorCombo, null,
              employerAnchorCombo::getSelectedItem, employerAnchorCombo));

        employerSponsorCombo = sponsorCombo(data == null ? null : data.sponsorFactionCode());
        grid.add(trackedRow("edit.contractMarket.field.sponsorFactionCode", employerSponsorCombo, null,
              employerSponsorCombo::getSelectedItem, employerSponsorCombo));

        employerDisplayNameField = new JTextField(orEmpty(contract.getEmployerDisplayName()));
        EditorHud.styleField(employerDisplayNameField);
        grid.add(trackedRow("edit.contractMarket.field.displayName", employerDisplayNameField, null,
              employerDisplayNameField::getText, employerDisplayNameField));

        employerColorCombo = enumCombo(PlayerColour.values(), contract.getEmployerColor(), String::valueOf);
        employerCamouflage = contract.getEmployerCamouflage();
        employerCamoButton = camoButton(() -> employerCamouflage, camo -> employerCamouflage = camo);
        grid.add(trackedRow("edit.contractMarket.field.color", EditorHud.line(employerColorCombo, employerCamoButton),
              null, () -> List.of(String.valueOf(employerColorCombo.getSelectedItem()),
                    String.valueOf(employerCamouflage)), employerColorCombo));

        employerSkillCombo = forceSkillCombo(contract.getEmployerForceSkill());
        employerEquipmentPicker = equipmentPicker(contract.getEmployerEquipmentRating());
        obfuscateAlliedCommandCheckbox = obfuscationToggle(ObfuscatableIntel.ALLIED_COMMAND);
        grid.addWide(trackedRow("edit.contractMarket.field.forces",
              forcesLine(employerSkillCombo, employerEquipmentPicker, obfuscateAlliedCommandCheckbox), null,
              () -> List.of(String.valueOf(employerSkillCombo.getSelectedItem()),
                    String.valueOf(employerEquipmentPicker.getSelected()),
                    obfuscateAlliedCommandCheckbox.isSelected()),
              employerSkillCombo, obfuscateAlliedCommandCheckbox));
        section.add(leftAligned(grid.panel));

        section.add(leftAligned(subHeading("edit.contractMarket.sub.people")));
        buildPersonnel();
        Grid people = new Grid();
        if (createMode) {
            people.addWide(npcRow("edit.contractMarket.section.negotiator", negotiatorOverride));
            people.addWide(npcRow("edit.contractMarket.section.liaison", liaisonOverride));
        } else {
            addNpcRow(people, "edit.contractMarket.section.negotiator", negotiatorEditor);
            addNpcRow(people, "edit.contractMarket.section.liaison", liaisonEditor);
        }
        section.add(leftAligned(people.panel));
    }

    private void buildEnemySection(JPanel section) {
        EnemyData data = contract.getEnemyData();
        Grid grid = new Grid();

        enemyFactionCombo = factionComboWithCustoms(contract.getEnemyFactionCode(), false);
        obfuscateOppositionCheckbox = obfuscationToggle(ObfuscatableIntel.OPPOSITION);
        obfuscateOppositionCheckbox.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE,
              "edit.contractMarket.field.obfuscate.opposition.tooltip")));
        grid.add(trackedRow("edit.contractMarket.field.factionCode",
              EditorHud.line(enemyFactionCombo, obfuscateOppositionCheckbox), null,
              () -> List.of(String.valueOf(enemyFactionCombo.getSelectedItem()),
                    obfuscateOppositionCheckbox.isSelected()), enemyFactionCombo, obfuscateOppositionCheckbox));

        enemySponsorCombo = sponsorCombo(data == null ? null : data.sponsorFactionCode());
        grid.add(trackedRow("edit.contractMarket.field.sponsorFactionCode", enemySponsorCombo, null,
              enemySponsorCombo::getSelectedItem, enemySponsorCombo));

        enemyDisplayNameField = new JTextField(orEmpty(contract.getEnemyDisplayName()));
        EditorHud.styleField(enemyDisplayNameField);
        grid.add(trackedRow("edit.contractMarket.field.displayName", enemyDisplayNameField, null,
              enemyDisplayNameField::getText, enemyDisplayNameField));

        enemyColorCombo = enumCombo(PlayerColour.values(), contract.getEnemyColour(), String::valueOf);
        enemyCamouflage = contract.getEnemyCamouflage();
        enemyCamoButton = camoButton(() -> enemyCamouflage, camo -> enemyCamouflage = camo);
        grid.add(trackedRow("edit.contractMarket.field.color", EditorHud.line(enemyColorCombo, enemyCamoButton), null,
              () -> List.of(String.valueOf(enemyColorCombo.getSelectedItem()), String.valueOf(enemyCamouflage)),
              enemyColorCombo));

        enemySkillCombo = forceSkillCombo(contract.getEnemyForceSkill());
        enemyEquipmentPicker = equipmentPicker(contract.getEnemyEquipmentRating());
        obfuscateThreatCheckbox = obfuscationToggle(ObfuscatableIntel.THREAT);
        grid.addWide(trackedRow("edit.contractMarket.field.forces",
              forcesLine(enemySkillCombo, enemyEquipmentPicker, obfuscateThreatCheckbox), null,
              () -> List.of(String.valueOf(enemySkillCombo.getSelectedItem()),
                    String.valueOf(enemyEquipmentPicker.getSelected()), obfuscateThreatCheckbox.isSelected()),
              enemySkillCombo, obfuscateThreatCheckbox));

        moralePicker = segments(ContractMoraleLevel.values(), ContractMoraleLevel::toString,
              contract.getMoraleLevel());
        obfuscateMoraleCheckbox = obfuscationToggle(ObfuscatableIntel.MORALE);
        grid.addWide(trackedRow("edit.contractMarket.field.moraleLevel",
              EditorHud.line(moralePicker, obfuscateMoraleCheckbox), null,
              () -> List.of(String.valueOf(moralePicker.getSelected()), obfuscateMoraleCheckbox.isSelected()),
              obfuscateMoraleCheckbox));

        batchallAcceptedCheckbox = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.batchall"));
        batchallAcceptedCheckbox.setSelected(data != null && data.batchallAccepted());
        batchallRow = trackedRow("edit.contractMarket.field.batchall.label", batchallAcceptedCheckbox, null,
              batchallAcceptedCheckbox::isSelected, batchallAcceptedCheckbox);
        // A batchall is only offered on an accepted contract fought against a Clan enemy, so keep the checkbox in step
        // with the enemy faction the GM picks.
        enemyFactionCombo.addActionListener(e -> refreshBatchallEnabled());
        refreshBatchallEnabled();
        grid.add(batchallRow);

        // A rout only begins once the contract is accepted (and even then only for certain objectives), so the rout
        // fields are only editable for an accepted contract - one with a status. On an unstarted offer they are
        // disabled, never hidden.
        routFieldsEditable = contract.getStatus() != null;
        String routNote = routFieldsEditable ? null : getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.note.rout");
        routEndDateField = new HudDateField(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.date.placeholder"),
              this::onEdited);
        LocalDate routEnd = contract.getRoutEndDate();
        routEndDateField.setText(routEnd == null ? "" : routEnd.toString());
        routEndDateField.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.date.tooltip")));
        routEndDateField.setEnabled(routFieldsEditable);
        // The date field already refreshes the form on every change, so it is not watched a second time.
        grid.add(trackedRow("edit.contractMarket.field.routEndDate", routEndDateField, routNote,
              routEndDateField::getText));

        routedPayoutSpinner = moneySpinner(contract.getRoutPayout());
        routedPayoutSpinner.setEnabled(routFieldsEditable);
        grid.add(trackedRow("edit.contractMarket.field.routedPayout", routedPayoutSpinner, routNote,
              routedPayoutSpinner::getValue, routedPayoutSpinner));
        section.add(leftAligned(grid.panel));

        section.add(leftAligned(subHeading("edit.contractMarket.sub.people")));
        linkOverrideRanks();
        Grid people = new Grid();
        if (createMode) {
            people.addWide(npcRow("edit.contractMarket.section.commander", commanderOverride));
        } else {
            addNpcRow(people, "edit.contractMarket.section.commander", commanderEditor);
        }
        section.add(leftAligned(people.panel));
    }

    private void buildTermsSection(JPanel section) {
        NonNegotiableTermsData locks = contract.getNonNegotiableTermsData();
        if (locks == null) {
            locks = NonNegotiableTermsData.none();
        }
        // Which terms the employer has locked can only be set while the contract is still an offer; once it has been
        // accepted the player has agreed to those terms, so the locks show their state but are disabled.
        boolean lockEditable = contract.getStatus() == null;

        payRateTrack = stepTrack(contract.getBasePayRateStep(), TermEffect.PAY_RATE);
        payNonNegotiableCheck = nonNegotiableToggle(locks.payLocked(), lockEditable);
        section.add(leftAligned(termRow("edit.contractMarket.field.payRate", payRateTrack, TermEffect.PAY_RATE,
              payNonNegotiableCheck)));

        supportTrack = stepTrack(contract.getSupportStep(), TermEffect.SUPPORT);
        supportNonNegotiableCheck = nonNegotiableToggle(locks.supportLocked(), lockEditable);
        section.add(leftAligned(termRow("edit.contractMarket.field.support", supportTrack, TermEffect.SUPPORT,
              supportNonNegotiableCheck)));

        transportTermTrack = stepTrack(contract.getTransportStep(), TermEffect.TRANSPORT);
        transportNonNegotiableCheck = nonNegotiableToggle(locks.transportLocked(), lockEditable);
        section.add(leftAligned(termRow("edit.contractMarket.field.transport", transportTermTrack,
              TermEffect.TRANSPORT, transportNonNegotiableCheck)));

        salvageTrack = stepTrack(contract.getSalvageRightsStep(), TermEffect.SALVAGE);
        salvageNonNegotiableCheck = nonNegotiableToggle(locks.salvageLocked(), lockEditable);
        section.add(leftAligned(termRow("edit.contractMarket.field.salvage", salvageTrack, TermEffect.SALVAGE,
              salvageNonNegotiableCheck)));

        commandTrack = stepTrack(contract.getCommandRightsStep(), TermEffect.COMMAND);
        commandNonNegotiableCheck = nonNegotiableToggle(locks.commandLocked(), lockEditable);
        section.add(leftAligned(termRow("edit.contractMarket.field.command", commandTrack, TermEffect.COMMAND,
              commandNonNegotiableCheck)));
        if (!lockEditable) {
            JLabel note = Hud.hint(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.note.locks"));
            note.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(9), 0, 0));
            section.add(leftAligned(note));
        }

        section.add(leftAligned(subHeading("edit.contractMarket.sub.pay")));
        Grid grid = new Grid();
        // Seed from the record directly (guarding null) rather than the convenience getters, which dereference the
        // finance data unchecked.
        ContractFinanceData finance = contract.getContractFinanceData();
        Money transport = finance == null ? Money.zero() : finance.transport();
        Money monthlyPay = finance == null ? Money.zero() : finance.monthlyPay();
        Money combatPay = finance == null ? Money.zero() : finance.combatPay();

        monthlyPaySpinner = moneySpinner(monthlyPay);
        monthlyPayAutomatic = financeAutomatic(monthlyPaySpinner);
        grid.add(trackedRow("edit.contractMarket.field.monthlyPay",
              withAutomatic(monthlyPaySpinner, monthlyPayAutomatic), null,
              () -> List.of(monthlyPaySpinner.getValue(), monthlyPayAutomatic.isSelected()), monthlyPaySpinner,
              monthlyPayAutomatic));

        transportPaySpinner = moneySpinner(transport);
        transportPaySpinner.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE,
              "edit.contractMarket.field.transportPay.tooltip")));
        transportPayAutomatic = financeAutomatic(transportPaySpinner);
        grid.add(trackedRow("edit.contractMarket.field.transportPay",
              withAutomatic(transportPaySpinner, transportPayAutomatic), null,
              () -> List.of(transportPaySpinner.getValue(), transportPayAutomatic.isSelected()), transportPaySpinner,
              transportPayAutomatic));

        combatPaySpinner = moneySpinner(combatPay);
        combatPayAutomatic = financeAutomatic(combatPaySpinner);
        grid.add(trackedRow("edit.contractMarket.field.combatPay",
              withAutomatic(combatPaySpinner, combatPayAutomatic), null,
              () -> List.of(combatPaySpinner.getValue(), combatPayAutomatic.isSelected()), combatPaySpinner,
              combatPayAutomatic));
        section.add(leftAligned(grid.panel));
        linkPayToTerm(payRateTrack, monthlyPaySpinner, monthlyPayAutomatic,
              ChaosContractStepsTable::getBasePayMultiplier);
        linkPayToTerm(transportTermTrack, transportPaySpinner, transportPayAutomatic,
              ChaosContractStepsTable::getTransportMultiplier);

        section.add(leftAligned(subHeading("edit.contractMarket.section.facilities")));
        grid = new Grid();
        hospitalBedsSpinner = intSpinner(contract.getRentedHospitalBeds(), 0);
        grid.add(trackedRow("edit.contractMarket.field.hospitalBeds", hospitalBedsSpinner, null,
              hospitalBedsSpinner::getValue, hospitalBedsSpinner));
        kitchensSpinner = intSpinner(contract.getRentedKitchens(), 0);
        grid.add(trackedRow("edit.contractMarket.field.kitchens", kitchensSpinner, null, kitchensSpinner::getValue,
              kitchensSpinner));
        holdingCellsSpinner = intSpinner(contract.getRentedHoldingCells(), 0);
        grid.add(trackedRow("edit.contractMarket.field.holdingCells", holdingCellsSpinner, null,
              holdingCellsSpinner::getValue, holdingCellsSpinner));
        section.add(leftAligned(grid.panel));
    }

    /**
     * The characteristics: one row per category, a "None" chip then a chip for each characteristic in it, coloured by
     * whether it helps or hurts the player. Characteristics in a category are mutually exclusive, so choosing one
     * clears the rest.
     */
    private void buildCharacteristicsSection(JPanel section) {
        JLabel note = Hud.hint(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.characteristics.note"));
        note.setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(9), scaleForGUI(4), 0));
        section.add(leftAligned(note));

        for (ContractCharacteristic.Category category : ContractCharacteristic.Category.values()) {
            List<ContractCharacteristic> members = new ArrayList<>();
            for (ContractCharacteristic characteristic : ContractCharacteristic.values()) {
                if (characteristic.getCategory() == category) {
                    members.add(characteristic);
                }
            }
            if (members.isEmpty()) {
                continue;
            }

            JPanel chips = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(4), scaleForGUI(3)));
            boolean anyChosen = members.stream().anyMatch(contract::hasCharacteristic);
            Chip none = new Chip(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.characteristic.none"), ACCENT_BRIGHT,
                  !anyChosen);
            none.addActionListener(event -> chooseCharacteristic(category, null));
            noneChips.put(category, none);
            chips.add(none);
            for (ContractCharacteristic characteristic : members) {
                Chip chip = new Chip(characteristic.getName(), polarityColor(characteristic),
                      contract.hasCharacteristic(characteristic));
                chip.setToolTipText(wordWrap(characteristic.getToolTipText()));
                chip.addActionListener(event -> chooseCharacteristic(category,
                      chip.isSelected() ? characteristic : null));
                characteristicCheckboxes.put(characteristic, chip);
                chips.add(chip);
            }

            Row row = new Row(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.category." + category.name()), null,
                  chips, null, LABEL_WIDTH);
            track(row, () -> members.stream().filter(member -> characteristicCheckboxes.get(member).isSelected())
                                   .toList());
            watch(none);
            members.forEach(member -> watch(characteristicCheckboxes.get(member)));
            section.add(leftAligned(row));
        }
    }

    /** Chooses one characteristic in a category (or none), clearing the others so at most one is ever chosen. */
    private void chooseCharacteristic(ContractCharacteristic.Category category,
          @Nullable ContractCharacteristic chosen) {
        characteristicCheckboxes.forEach((characteristic, chip) -> {
            if (characteristic.getCategory() == category) {
                chip.setSelected(characteristic == chosen);
            }
        });
        Chip none = noneChips.get(category);
        if (none != null) {
            none.setSelected(chosen == null);
        }
        onEdited();
    }

    private static Color polarityColor(ContractCharacteristic characteristic) {
        return switch (characteristic.getPolarity()) {
            case BENEFICIAL -> READY;
            case ADVERSE -> DANGER;
            case NEUTRAL -> AMBER;
        };
    }

    /**
     * Creates the NPC editors (edit mode) or overrides (create mode). The overrides' rank lists follow the chosen
     * faction's rank system: the employer's for its negotiator and liaison, the enemy's for the opposing commander.
     */
    private void buildPersonnel() {
        if (createMode) {
            negotiatorOverride = new NpcOverride();
            liaisonOverride = new NpcOverride();
            commanderOverride = new NpcOverride();
            return;
        }
        EmployerData employer = contract.getEmployerData();
        EnemyData enemy = contract.getEnemyData();
        negotiatorEditor = npcEditor(employer == null ? null : employer.negotiator());
        liaisonEditor = npcEditor(employer == null ? null : employer.liaison());
        commanderEditor = npcEditor(enemy == null ? null : enemy.opposingCommander());
    }

    /** Hooks the create-mode NPC overrides' rank lists to their factions, once both faction pickers exist. */
    private void linkOverrideRanks() {
        if (!createMode || enemyFactionCombo == null) {
            return;
        }
        Runnable refreshEmployerRanks = () -> {
            RankSystem rankSystem = rankSystemOf(employerFactionCombo);
            negotiatorOverride.refreshRanks(rankSystem);
            liaisonOverride.refreshRanks(rankSystem);
        };
        Runnable refreshEnemyRanks = () -> commanderOverride.refreshRanks(rankSystemOf(enemyFactionCombo));
        refreshEmployerRanks.run();
        refreshEnemyRanks.run();
        employerFactionCombo.addActionListener(event -> refreshEmployerRanks.run());
        enemyFactionCombo.addActionListener(event -> refreshEnemyRanks.run());
    }

    private void addNpcRow(Grid grid, String labelKey, @Nullable NpcEditor editor) {
        // Contracts missing an NPC (for example an opposing commander that has not been generated) show no row.
        if (editor != null) {
            Row row = new Row(getTextAt(RESOURCE_BUNDLE, labelKey), null, editor.buildCard(), null, LABEL_WIDTH);
            track(row, editor::state);
            grid.addWide(row);
        }
    }

    private Row npcRow(String labelKey, NpcOverride override) {
        Row row = new Row(getTextAt(RESOURCE_BUNDLE, labelKey), null, override.buildCard(), null, LABEL_WIDTH);
        track(row, override::state);
        return row;
    }

    // endregion Sections

    // region Change tracking and preview

    /** One tracked value: the row it belongs to, how to read it, and what it was when the editor opened. */
    private static final class Tracked {
        private final Row row;
        private final Section section;
        private final Supplier<Object> value;
        private Object initial;

        private Tracked(Row row, Section section, Supplier<Object> value) {
            this.row = row;
            this.section = section;
            this.value = value;
        }
    }

    private void track(Row row, Supplier<Object> value) {
        tracked.add(new Tracked(row, buildingSection, value));
    }

    /**
     * Builds a labelled row, records its value for change tracking, and listens to its controls so the change marks
     * and the preview follow every edit.
     */
    private Row trackedRow(String labelKey, JComponent control, @Nullable String note, Supplier<Object> value,
          JComponent... sources) {
        Row row = row(labelKey, control, note);
        track(row, value);
        for (JComponent source : sources) {
            watch(source);
        }
        return row;
    }

    private Row row(String labelKey, JComponent control, @Nullable String note) {
        String toolTip = getTextAt(RESOURCE_BUNDLE, labelKey + ".tooltip");
        return new Row(getTextAt(RESOURCE_BUNDLE, labelKey), isResourceKeyValid(toolTip) ? wordWrap(toolTip) : null,
              control, note, LABEL_WIDTH);
    }

    /** Listens to a control so any edit to it refreshes the change marks and the preview. */
    private void watch(@Nullable JComponent source) {
        if (source == null) {
            return;
        }
        if (source instanceof JTextComponent text) {
            text.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    onEdited();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    onEdited();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    onEdited();
                }
            });
        } else if (source instanceof JComboBox<?> combo) {
            combo.addActionListener(event -> onEdited());
        } else if (source instanceof JSpinner spinner) {
            spinner.addChangeListener(event -> onEdited());
        } else if (source instanceof AbstractButton button) {
            button.addItemListener(event -> onEdited());
        }
    }

    /**
     * Marks every changed row, counts the changes per tab for the tab selector and overall for the command bar and the
     * Save button, and redraws the preview.
     */
    private void onEdited() {
        if (building) {
            return;
        }
        Map<Row, Boolean> rowChanged = new LinkedHashMap<>();
        Map<Row, Section> rowSection = new HashMap<>();
        for (Tracked entry : tracked) {
            boolean changed = !Objects.equals(entry.value.get(), entry.initial);
            rowChanged.merge(entry.row, changed, Boolean::logicalOr);
            rowSection.put(entry.row, entry.section);
        }
        Map<Section, Integer> perSection = new EnumMap<>(Section.class);
        int total = 0;
        for (Map.Entry<Row, Boolean> entry : rowChanged.entrySet()) {
            entry.getKey().setChanged(entry.getValue());
            if (entry.getValue()) {
                total++;
                perSection.merge(rowSection.get(entry.getKey()), 1, Integer::sum);
            }
        }
        for (Section section : Section.values()) {
            sectionSelector.setModeLabel(section, sectionLabel(section, perSection.getOrDefault(section, 0)));
        }
        changesValue.setText(Integer.toString(total));
        revertButton.setArmed(total > 0);
        String saveText = total == 0
                                ? getTextAt(RESOURCE_BUNDLE, "button.contractMarket.edit.save")
                                : getFormattedTextAt(RESOURCE_BUNDLE, "button.contractMarket.edit.saveChanges", total);
        saveButton.setText(saveText.toUpperCase(Locale.ROOT));

        String name = nameField.getText().isBlank() ? contract.getName() : nameField.getText().trim();
        titleLabel.setText(name.toUpperCase(Locale.ROOT));
        PlayerColour colour = (PlayerColour) employerColorCombo.getSelectedItem();
        titleLabel.setIcon(colour == null ? null : new MarketHud.SwatchIcon(colour.getColour(), scaleForGUI(9)));
        refreshPreview();
    }

    /**
     * Redraws the preview from the form as it stands: the offer as it will read on the board, its terms, and the intel
     * the player will see. Difficulty and pay are only worked out again on save, so the board's ring keeps the saved
     * difficulty and an automatic pay figure reads "Auto". Only the content is refilled; its scroll pane stays, so the
     * preview does not jump back to the top with each keystroke.
     */
    private void refreshPreview() {
        JPanel preview = previewContent;
        preview.removeAll();

        preview.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.preview.board"))));
        preview.add(Box.createVerticalStrut(scaleForGUI(5)));
        preview.add(leftAligned(previewCard()));
        preview.add(Box.createVerticalStrut(scaleForGUI(14)));

        JPanel terms = MarketHud.section(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.preview.terms"));
        previewTerm(terms, "edit.contractMarket.field.payRate", payRateTrack, TermEffect.PAY_RATE,
              payNonNegotiableCheck);
        previewTerm(terms, "edit.contractMarket.field.support", supportTrack, TermEffect.SUPPORT,
              supportNonNegotiableCheck);
        previewTerm(terms, "edit.contractMarket.field.transport", transportTermTrack, TermEffect.TRANSPORT,
              transportNonNegotiableCheck);
        previewTerm(terms, "edit.contractMarket.field.salvage", salvageTrack, TermEffect.SALVAGE,
              salvageNonNegotiableCheck);
        previewTerm(terms, "edit.contractMarket.field.command", commandTrack, TermEffect.COMMAND,
              commandNonNegotiableCheck);
        preview.add(leftAligned(terms));
        preview.add(Box.createVerticalStrut(scaleForGUI(12)));

        JPanel intel = MarketHud.section(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.preview.intel"));
        previewIntel(intel, "dossier.contractMarket.intel.alliedCommand",
              forcesText(employerSkillCombo, employerEquipmentPicker), obfuscateAlliedCommandCheckbox);
        previewIntel(intel, "dossier.contractMarket.intel.opposition", enemyLabel(), obfuscateOppositionCheckbox);
        previewIntel(intel, "dossier.contractMarket.intel.threat",
              forcesText(enemySkillCombo, enemyEquipmentPicker), obfuscateThreatCheckbox);
        previewIntel(intel, "dossier.contractMarket.intel.morale", String.valueOf(moralePicker.getSelected()),
              obfuscateMoraleCheckbox);
        preview.add(leftAligned(intel));

        List<JComponent> badges = new ArrayList<>();
        characteristicCheckboxes.forEach((characteristic, chip) -> {
            if (chip.isSelected()) {
                badges.add(MarketHud.badge(characteristic.getName(), polarityColor(characteristic),
                      characteristic.getToolTipText()));
            }
        });
        if (!badges.isEmpty()) {
            preview.add(Box.createVerticalStrut(scaleForGUI(10)));
            JPanel flow = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(4), scaleForGUI(4)));
            badges.forEach(flow::add);
            preview.add(leftAligned(flow));
        }

        preview.add(Box.createVerticalStrut(scaleForGUI(12)));
        preview.add(leftAligned(MarketHud.wrappedText(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.preview.note"),
              TEXT_FAINT, Font.PLAIN, 0.8f)));

        preview.revalidate();
        preview.repaint();
    }

    /** The offer's board row: difficulty ring, name, employer and job, any badges, and the total payout. */
    private JComponent previewCard() {
        JPanel card = new JPanel(new BorderLayout(scaleForGUI(9), 0));
        card.setOpaque(true);
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
                    BorderFactory.createMatteBorder(0, scaleForGUI(3), 0, 0, ACCENT)),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(8), scaleForGUI(8), scaleForGUI(10))));

        JLabel ring = new JLabel(new MarketHud.RingIcon(ContractMarketFormatting.severityColor(
              ContractMarketFormatting.assessmentDifficulty(contract))));
        ring.setVerticalAlignment(SwingConstants.TOP);
        ring.setToolTipText(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.preview.difficulty.tooltip"));
        card.add(ring, BorderLayout.WEST);

        String separator = getTextAt(RESOURCE_BUNDLE, "value.contractMarket.separator");
        List<String> parts = new ArrayList<>();
        parts.add(employerLabel());
        ContractObjectiveType objective = (ContractObjectiveType) playerObjectiveCombo.getSelectedItem();
        if (objective != null) {
            parts.add(objective.toString());
        }
        parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "card.contractMarket.months", lengthValue()));
        StringBuilder html = new StringBuilder("<html><b style='color:").append(HudStyle.hex(TEXT)).append("'>")
                                   .append(MarketHud.escape(nameField.getText().isBlank()
                                                                  ? contract.getName()
                                                                  : nameField.getText().trim()))
                                   .append("</b><br><span style='font-size:smaller; color:")
                                   .append(HudStyle.hex(TEXT_MUTED)).append("'>")
                                   .append(MarketHud.escape(String.join(separator, parts))).append("</span>");
        List<String> tags = previewTags();
        if (!tags.isEmpty()) {
            html.append("<br><span style='font-size:smaller'><b>").append(String.join(separator, tags))
                  .append("</b></span>");
        }
        JLabel text = new JLabel(html.append("</html>").toString());
        text.setVerticalAlignment(SwingConstants.TOP);
        text.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        card.add(text, BorderLayout.CENTER);

        JLabel figure = new JLabel(previewTotal(), SwingConstants.RIGHT);
        figure.setForeground(ACCENT_BRIGHT);
        figure.setFont(hudFont(Font.BOLD, 1.0f, 0.0f));
        figure.setVerticalAlignment(SwingConstants.TOP);
        figure.setToolTipText(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.preview.total.tooltip"));
        card.add(figure, BorderLayout.EAST);
        return card;
    }

    /** The badges the offer will carry: batchall, proving ground or covert operation, and its special rules. */
    private List<String> previewTags() {
        List<String> tags = new ArrayList<>();
        Faction enemy = factionOf(enemyFactionCombo);
        if (!obfuscateOppositionCheckbox.isSelected() && enemy != null && enemy.isClan()) {
            tags.add(tag(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.batchall"), AMBER));
        }
        ContractNature nature = naturePicker.getSelected();
        if (nature == ContractNature.PROVING_GROUND) {
            tags.add(tag(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.provingGround"), READY));
        } else if (nature == ContractNature.COVERT) {
            tags.add(tag(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.covert"), ACCENT_BRIGHT));
        }
        // The special rules follow the objective chosen on the form, as the board's badges follow the saved one.
        ContractObjectiveType objective = (ContractObjectiveType) playerObjectiveCombo.getSelectedItem();
        if (objective != null) {
            for (ChaosObjectiveSpecialRules specialRule : objective.getChaosObjectiveType().getSpecialRules()) {
                tags.add(tag(specialRule.getName(), AMBER));
            }
        }
        return tags;
    }

    private static String tag(String text, Color color) {
        return "<span style='color:" + HudStyle.hex(color) + "'>" + MarketHud.escape(text.toUpperCase(Locale.ROOT))
                     + "</span>";
    }

    /** The total payout the form adds up to, or "Auto" while either pay figure is left to the generator. */
    private String previewTotal() {
        if (monthlyPayAutomatic.isSelected() || transportPayAutomatic.isSelected()) {
            return getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.preview.total.auto");
        }
        Money total = moneyValue(monthlyPaySpinner).multipliedBy(lengthValue()).plus(moneyValue(transportPaySpinner));
        return ContractMarketFormatting.compactMoney(total);
    }

    private void previewTerm(JPanel terms, String labelKey, StepTrack track, TermEffect effect, JCheckBox lock) {
        String value = termEffectText(effect, track.getSelected());
        if (lock.isSelected()) {
            value = getFormattedTextAt(RESOURCE_BUNDLE, "edit.contractMarket.preview.locked", value);
        }
        MarketHud.addRow(terms, getTextAt(RESOURCE_BUNDLE, labelKey), value, null, null, false);
    }

    private void previewIntel(JPanel intel, String labelKey, String value, JCheckBox hidden) {
        boolean isHidden = hidden.isSelected();
        MarketHud.addRow(intel, getTextAt(RESOURCE_BUNDLE, labelKey),
              isHidden ? getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.intel.obfuscated") : value,
              isHidden ? AMBER : null, null, false);
    }

    /** The employer as the market will name it: hidden for a covert job, else the display name or the faction. */
    private String employerLabel() {
        if (naturePicker.getSelected() == ContractNature.COVERT) {
            return getTextAt(RESOURCE_BUNDLE, "value.contractMarket.employer.hidden");
        }
        String displayName = employerDisplayNameField.getText().trim();
        return displayName.isBlank() ? factionLabel(employerFactionCombo) : displayName;
    }

    private String enemyLabel() {
        String displayName = enemyDisplayNameField.getText().trim();
        return displayName.isBlank() ? factionLabel(enemyFactionCombo) : displayName;
    }

    private static String factionLabel(FactionComboBox combo) {
        Object selected = combo.getSelectedItem();
        return selected instanceof Map.Entry<?, ?> entry ? String.valueOf(entry.getValue()) : "";
    }

    private static @Nullable Faction factionOf(FactionComboBox combo) {
        String code = combo.getSelectedItemKey();
        return (code == null || code.isBlank()) ? null : Factions.getInstance().getFaction(code);
    }

    private static String forcesText(JComboBox<SkillLevel> skill, HudSegmentedControl<DragoonRating> rating) {
        SkillLevel level = (SkillLevel) skill.getSelectedItem();
        String skillText = SkillLevelPickerUtility.isRandom(level)
                                 ? SkillLevelPickerUtility.getRandomLabel()
                                 : String.valueOf(level);
        DragoonRating selected = rating.getSelected();
        return getFormattedTextAt(RESOURCE_BUNDLE, "value.contractMarket.threat", skillText,
              selected == null ? "" : selected.getLabel());
    }

    // endregion Change tracking and preview

    // region Tabs and guides

    /** A tab's label, with how many of its rows have changed beside it once there are any. */
    private static String sectionLabel(Section section, int changes) {
        String label = MarketHud.escape(section.label().toUpperCase(Locale.ROOT));
        return changes == 0 ? "<html>" + label + "</html>"
                     : "<html>" + label + "&nbsp;&nbsp;<span style='color:" + hex(AMBER) + "'>" + changes
                             + "</span></html>";
    }

    /**
     * Switches to a tab, opening its guide if the player has not seen it yet.
     *
     * @param section the tab
     */
    private void showSection(Section section) {
        guide.close();
        currentSection = section;
        sectionSelector.setSelected(section);
        pageLayout.show(pageHolder, section.name());
        refreshGuideButton();
        maybeOpenGuide(section);
    }

    private void refreshGuideButton() {
        guideButton.setText(getFormattedTextAt(RESOURCE_BUNDLE, "button.contractMarket.guide",
              currentSection.guideLabel()).toUpperCase(Locale.ROOT));
    }

    /** Opens a tab's guide by itself the first time the player visits the tab, unless they turned that off. */
    private void maybeOpenGuide(Section section) {
        if (isShowing() && ContractMarketGuidePreferences.getInstance().shouldAutoOpen(section.guidePrefix())) {
            openGuide(section);
        }
    }

    /** Opens a tab's guide, marking it seen so it does not open by itself again. */
    private void openGuide(Section section) {
        ContractMarketGuidePreferences.getInstance().setGuideSeen(section.guidePrefix(), true);
        guide.open(section.guidePrefix());
    }

    /** @return every tab guide's resource-key prefix, so the guides' seen flags can be registered */
    static List<String> guidePrefixes() {
        return Arrays.stream(Section.values()).map(Section::guidePrefix).toList();
    }

    // endregion Tabs and guides

    // region Schedule

    /** When the length's "Automatic" box is toggled, re-derive the length and (automatic) end date, and repaint. */
    private void onLengthAutomaticToggled() {
        // Each time the length goes on "Automatic" it is rolled afresh, then kept.
        automaticLengthMonths = null;
        if (startDate != null && (lengthAutomatic.isSelected() || endAutomatic.isSelected())) {
            endDate = startDate.plusMonths(effectiveLengthMonths());
        }
        refreshScheduleControls();
    }

    /**
     * The length in months to use for an automatic end date: the generator's length (with any Quick Job / Long Haul
     * characteristic baked in) when the length is on "Automatic", otherwise the GM's spinner value.
     */
    private int effectiveLengthMonths() {
        if (lengthAutomatic != null && lengthAutomatic.isSelected()) {
            if (automaticLengthMonths == null) {
                automaticLengthMonths = ContractCharacteristics.bakeLength(
                      AbstractContractGeneration.determineLength(campaign, contract), contract);
            }
            return automaticLengthMonths;
        }
        return lengthValue();
    }

    /** A button that shows a date (or "not set") and runs {@code onPick} - a {@link DateChooser} flow - when clicked. */
    private HudButton dateButton(Runnable onPick) {
        HudButton button = new HudButton("", false, true);
        button.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.date.pick.tooltip")));
        button.addActionListener(event -> onPick.run());
        return button;
    }

    /** An "Automatic" pill for a schedule field, defaulting on for a new offer and off when editing. */
    private JCheckBox scheduleAutomaticToggle(Runnable onToggle) {
        Pill automatic = autoPill(createMode);
        automatic.addActionListener(event -> onToggle.run());
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
            endDate = startDate.plusMonths(effectiveLengthMonths());
        }
        refreshScheduleControls();
    }

    private void onLengthChanged() {
        if ((endAutomatic.isSelected() || (lengthAutomatic != null && lengthAutomatic.isSelected()))
                  && startDate != null) {
            endDate = startDate.plusMonths(effectiveLengthMonths());
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
        LocalDate seed = endDate != null
                               ? endDate
                               : (startDate != null ? startDate.plusMonths(MINIMUM_CONTRACT_LENGTH_MONTHS)
                                        : currentDate);
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
        if (endAutomatic.isSelected() || (lengthAutomatic != null && lengthAutomatic.isSelected())) {
            endDate = startDate.plusMonths(effectiveLengthMonths());
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
     * Repaints the date buttons and applies the enabled states: a date button is usable only when its "Automatic" pill
     * is off, and the length spinner is editable only when the end date is automatic (where it drives the end). When
     * the end date is set by hand the spinner instead shows the length the dates imply.
     */
    private void refreshScheduleControls() {
        startDateButton.setText(dateButtonLabel(startDate));
        endDateButton.setText(dateButtonLabel(endDate));
        boolean lengthAuto = lengthAutomatic != null && lengthAutomatic.isSelected();
        startDateButton.setArmed(!startAutomatic.isSelected());
        // An automatic length derives the end date, so it takes the end button (and the length spinner) out of play.
        endDateButton.setArmed(!endAutomatic.isSelected() && !lengthAuto);
        lengthSpinner.setEnabled(endAutomatic.isSelected() && !lengthAuto);
        if (lengthAuto) {
            lengthSpinner.setValue(effectiveLengthMonths());
        } else if (!endAutomatic.isSelected() && startDate != null && endDate != null) {
            lengthSpinner.setValue((int) max(MINIMUM_CONTRACT_LENGTH_MONTHS,
                  ChronoUnit.MONTHS.between(startDate, endDate)));
        }
        onEdited();
    }

    private String dateButtonLabel(@Nullable LocalDate date) {
        return (date == null ? getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.date.notSet")
                      : MekHQ.getMHQOptions().getDisplayFormattedDate(date)).toUpperCase(Locale.ROOT);
    }

    // endregion Schedule

    // region Batchall

    /** Whether the player's batchall acceptance is editable: an accepted contract fought against a Clan enemy. */
    private boolean batchallEditable() {
        return contract.getStatus() != null && isEnemyClan();
    }

    /** Whether the enemy faction currently selected in the combo is a Clan. */
    private boolean isEnemyClan() {
        Faction faction = factionOf(enemyFactionCombo);
        return faction != null && faction.isClan();
    }

    /** Enables the batchall checkbox only when it is editable, with a note saying why when it is not. */
    private void refreshBatchallEnabled() {
        boolean editable = batchallEditable();
        batchallAcceptedCheckbox.setEnabled(editable);
        batchallRow.setNote(editable ? null : getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.note.batchall"));
    }

    // endregion Batchall

    // region Save

    /**
     * Writes every edited field back onto the contract, then closes. The player's chosen negotiator is preserved
     * untouched. If the contract's scale, track count, or dates changed, its StratCon schedule is rolled again (see
     * {@link StratConScenarioTempo#regenerateSchedules}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void saveAction() {
        // What StratCon's pre-rolled schedule was rolled from, so an edit to any of it rolls the schedule again below.
        int previousScale = contract.getScale();
        int previousTrackCount = contract.getTrackCount();
        LocalDate previousStartDate = contract.getStartDate();
        LocalDate previousEndDate = contract.getEndingDate();
        int previousLengthInMonths = contract.getLengthInMonths();

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

        if (natureEditable) {
            contract.setNature(selectedOr(naturePicker, contract.getNature()));
        }

        // Characteristics - the GM's chosen set (one-per-category is enforced by the chips). Set before the numeric
        // parameters below so their "Automatic" values can be baked from the selected characteristics.
        EnumSet<ContractCharacteristic> selectedCharacteristics = EnumSet.noneOf(ContractCharacteristic.class);
        characteristicCheckboxes.forEach((characteristic, chip) -> {
            if (chip.isSelected()) {
                selectedCharacteristics.add(characteristic);
            }
        });
        contract.setCharacteristics(selectedCharacteristics);

        // Parameters. The "Automatic" pills exist only in create mode (see automaticToggle): an "on" pill
        // (re)determines the value with the same rules the contract generator uses (AbstractContractGeneration),
        // while an "off" pill writes the GM's manual spinner value. Scale is resolved before the required victory
        // points, which are derived from it. When editing an existing contract there are no pills - the GM's
        // spinner values are written directly, with no regeneration.
        if (createMode) {
            contract.setScale(scaleAutomatic.isSelected()
                                    ? AbstractContractGeneration.determineScale(campaign, campaign.getPlayerForce(),
                  campaign.getPlayerForce().getForceDetachment().getHangar(), contract)
                                    : getEditedScale());
            contract.setTrackCount(trackCountAutomatic.isSelected()
                                         ? ContractCharacteristics.bakeTrackCount(
                  AbstractContractGeneration.determineTrackCount(contract), contract)
                                         : intValue(trackCountSpinner));
            contract.setRequiredVictoryPoints(victoryPointsAutomatic.isSelected()
                                                    ? ContractCharacteristics.bakeRequiredVictoryPoints(
                  AbstractContractGeneration.determineRequiredVictoryPoints(
                        contract), contract)
                                                    :
                                                    intValue(victoryPointsSpinner));
        } else {
            contract.setScale(getEditedScale());
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
        // An automatic length (Quick Job / Long Haul baked onto the generator's length) or an automatic end both derive
        // the end date from the start; otherwise the GM's manual end date stands.
        LocalDate saveEnd = (endAutomatic.isSelected() || lengthAutomatic.isSelected())
                                  ? (saveStart == null ? null : saveStart.plusMonths(effectiveLengthMonths()))
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
              SkillLevelPickerUtility.resolve(enumValue(employerSkillCombo, SkillLevel.REGULAR)),
              equipmentValue(employerEquipmentPicker),
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
              SkillLevelPickerUtility.resolve(enumValue(enemySkillCombo, SkillLevel.REGULAR)),
              equipmentValue(enemyEquipmentPicker),
              enemy == null ? null : enemy.opposingCommander(), enemyCamouflage,
              enumValue(enemyColorCombo, PlayerColour.RED), batchallAccepted));

        // Terms
        contract.setContractTerms(new ContractTermsData(payRateTrack.getSelected(), supportTrack.getSelected(),
              transportTermTrack.getSelected(), salvageTrack.getSelected(), commandTrack.getSelected()));
        // A disabled lock still reports its loaded state, so on an accepted contract this preserves the existing locks
        // unchanged.
        contract.setNonNegotiableTermsData(new NonNegotiableTermsData(payNonNegotiableCheck.isSelected(),
              supportNonNegotiableCheck.isSelected(), transportNonNegotiableCheck.isSelected(),
              salvageNonNegotiableCheck.isSelected(), commandNonNegotiableCheck.isSelected()));

        // Objectives
        contract.setObjectiveData(new ContractObjectiveData(objectiveValue(playerObjectiveCombo),
              objectiveValue(opposingObjectiveCombo)));

        // Finance (record components are ordered transport, monthlyPay, combatPay). An "on" Automatic pill computes the
        // value with the generator's rule from the contract's just-saved scale, terms, and target; otherwise the GM's
        // spinner value is used.
        Money transportPay = transportPayAutomatic.isSelected()
                                   ? AbstractContractGeneration.determineTransportPay(campaign, contract)
                                   : moneyValue(transportPaySpinner);
        // On "Automatic" the base is recomputed from the generator, then the pay / combat-pay characteristic is baked
        // on top - recomputing each commit keeps the multiplier idempotent. A manual spinner value is left untouched.
        Money monthlyPayValue = monthlyPayAutomatic.isSelected()
                                      ? ContractCharacteristics.bakeMonthlyPay(
              AbstractContractGeneration.determineMonthlyPay(campaign, contract), contract)
                                      : moneyValue(monthlyPaySpinner);
        Money combatPayValue = combatPayAutomatic.isSelected()
                                     ? ContractCharacteristics.bakeCombatPay(
              AbstractContractGeneration.determineCombatPay(campaign, contract), contract)
                                     : moneyValue(combatPaySpinner);
        contract.setContractFinanceData(new ContractFinanceData(transportPay, monthlyPayValue, combatPayValue));
        // The salvage taper follows the contract's (possibly edited) Scale
        contract.setSalvageTaperMultiplier(AbstractContractDeterminationPay.forCampaign(campaign)
                                                 .getSalvageTaperMultiplier(campaign, contract));

        // Intel obfuscation - which fields are hidden from the player in the market.
        contract.setIntelObfuscated(ObfuscatableIntel.ALLIED_COMMAND, obfuscateAlliedCommandCheckbox.isSelected());
        contract.setIntelObfuscated(ObfuscatableIntel.OPPOSITION, obfuscateOppositionCheckbox.isSelected());
        contract.setIntelObfuscated(ObfuscatableIntel.THREAT, obfuscateThreatCheckbox.isSelected());
        contract.setIntelObfuscated(ObfuscatableIntel.MORALE, obfuscateMoraleCheckbox.isSelected());

        // Facilities
        contract.setRentedFacilitiesData(new RentedFacilitiesData(intValue(hospitalBedsSpinner),
              intValue(kitchensSpinner), intValue(holdingCellsSpinner)));

        // Morale. The rout fields are only written when editable (an accepted contract); on an unstarted offer they
        // are disabled and the contract's existing rout values (an absent end date, an unset payout) are preserved. A
        // zero payout from the spinner means none has been set, so the remaining escrow is paid as normal.
        LocalDate routEndDate = routFieldsEditable
                                      ? parseDate(routEndDateField, contract.getRoutEndDate())
                                      : contract.getRoutEndDate();
        Money routPayout = contract.getRoutPayout();
        if (routFieldsEditable) {
            Money spinnerPayout = moneyValue(routedPayoutSpinner);
            routPayout = spinnerPayout.isZero() ? null : spinnerPayout;
        }
        contract.setMoraleData(new MoraleData(selectedOr(moralePicker, contract.getMoraleLevel()),
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

        // Tier the employer negotiator to match the Elite / Novice Negotiator characteristic (Veteran when neither).
        // Done last, after the negotiator has been generated (create) or edited (edit).
        ContractCharacteristics.syncNegotiatorTier(contract, campaign);

        // A running contract's schedule was rolled from what was just edited, so it is rolled again to match. A market
        // offer has no StratCon state yet, and is scheduled from its edited values when accepted.
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        boolean isScheduleChanged = (contract.getScale() != previousScale)
                                          || (contract.getTrackCount() != previousTrackCount)
                                          || !Objects.equals(contract.getStartDate(), previousStartDate)
                                          || !Objects.equals(contract.getEndingDate(), previousEndDate)
                                          || (contract.getLengthInMonths() != previousLengthInMonths);
        if ((campaignState != null) && isScheduleChanged) {
            StratConScenarioTempo.regenerateSchedules(campaign, contract, campaignState);
        }

        // The difficulty is worked out again from the saved opposition and forces, as the editor tells the GM it will
        // be; a created offer would otherwise read as an even fight.
        contract.setCachedContractDifficulty(
              ChaosContractDeterminationDifficulty.calculateContractDifficulty(campaign, contract));

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
            negotiator = EmployerNegotiator.generateNegotiator(campaign, bucket, employerFaction, hiringHall,
                  enumValue(employerTypeCombo, contract.getEmployerType()));
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

    // endregion Save

    // region Component builders

    /** Lays rows out in two columns, with wide rows taking both. */
    private final class Grid {
        private final JPanel panel = Hud.transparentPanel(new GridBagLayout());
        private int cell;

        private void add(Row row) {
            GridBagConstraints constraints = constraints(cell % 2, cell / 2, 1);
            constraints.insets = new Insets(0, 0, 0, (cell % 2 == 0) ? scaleForGUI(COLUMN_GAP) : 0);
            panel.add(row, constraints);
            cell++;
        }

        private void addWide(Row row) {
            if (cell % 2 == 1) {
                cell++;
            }
            panel.add(row, constraints(0, cell / 2, 2));
            cell += 2;
        }

        private static GridBagConstraints constraints(int x, int y, int width) {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = x;
            constraints.gridy = y;
            constraints.gridwidth = width;
            constraints.weightx = 0.5;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            return constraints;
        }
    }

    private static JComponent subHeading(String key) {
        JLabel label = HudStyle.keyLabel(getTextAt(RESOURCE_BUNDLE, key).toUpperCase(Locale.ROOT));
        label.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(9), scaleForGUI(2), 0));
        return label;
    }

    /** A segmented picker over an enum's values, seeded to {@code selected}; every choice counts as an edit. */
    private <T> HudSegmentedControl<T> segments(T[] values, Function<T, String> labeler, @Nullable T selected) {
        HudSegmentedControl<T> control = new HudSegmentedControl<>(value -> onEdited());
        List<Segment<T>> segments = new ArrayList<>();
        for (T value : values) {
            segments.add(new Segment<>(value, labeler.apply(value), null, true));
        }
        control.setSegments(segments);
        control.setSelected(selected);
        return control;
    }

    private HudSegmentedControl<DragoonRating> equipmentPicker(int rating) {
        return segments(DragoonRating.values(), DragoonRating::getLabel, DragoonRating.fromRating(rating));
    }

    /** A force's skill, equipment rating and hide-from-player pill on one line. */
    private static JPanel forcesLine(JComboBox<SkillLevel> skill, HudSegmentedControl<DragoonRating> rating,
          JCheckBox hidden) {
        JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        skill.setPreferredSize(new Dimension(scaleForGUI(150), skill.getPreferredSize().height));
        line.add(skill, BorderLayout.WEST);
        line.add(rating, BorderLayout.CENTER);
        line.add(hidden, BorderLayout.EAST);
        return line;
    }

    /** A term's row: its step track, what the chosen step does, and the non-negotiable lock. */
    private Row termRow(String labelKey, StepTrack track, TermEffect effect, JCheckBox lock) {
        JLabel effectLabel = new JLabel();
        effectLabel.setFont(hudFont(Font.BOLD, 0.9f, 0.0f));
        effectLabel.setForeground(TEXT);
        Runnable showEffect = () -> effectLabel.setText("<html>" + MarketHud.escape(termEffectText(effect,
              track.getSelected())) + "<br><span style='font-weight:normal; font-size:smaller; color:"
                                                              + HudStyle.hex(TEXT_FAINT) + "'>"
                                                              + MarketHud.escape(getFormattedTextAt(RESOURCE_BUNDLE,
              "edit.contractMarket.stepLabel", track.getSelected().stepValue())) + "</span></html>");
        showEffect.run();
        // Sized only once its two lines are set: an empty label measures no height at all, which would clip them.
        effectLabel.setPreferredSize(new Dimension(scaleForGUI(170), effectLabel.getPreferredSize().height));
        track.addChangeListener(showEffect);
        track.addChangeListener(this::onEdited);
        watch(lock);

        JPanel side = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        side.add(effectLabel, BorderLayout.CENTER);
        side.add(lock, BorderLayout.EAST);
        JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(12), 0));
        line.add(track, BorderLayout.CENTER);
        line.add(side, BorderLayout.EAST);

        Row row = row(labelKey, line, null);
        track(row, () -> List.of(track.getSelected(), lock.isSelected()));
        return row;
    }

    private StepTrack stepTrack(ChaosContractStepsTable selected, TermEffect effect) {
        return new StepTrack(selected == null ? ChaosContractStepsTable.STEP_ONE : selected,
              step -> termEffectTooltip(effect, step));
    }

    /** An "Automatic" pill, on or off. */
    private static Pill autoPill(boolean selected) {
        Pill pill = new Pill(PillKind.AUTO, getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.automatic"),
              getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.automatic"), selected);
        pill.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.automatic.tooltip")));
        return pill;
    }

    /** A hide-from-player pill for one intel field, set from the contract's current state. */
    private JCheckBox obfuscationToggle(ObfuscatableIntel field) {
        Pill pill = new Pill(PillKind.HIDE, getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.obfuscate.hidden"),
              getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.obfuscate.shown"),
              contract.isIntelObfuscated(field));
        pill.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.obfuscate.tooltip")));
        return pill;
    }

    /**
     * A non-negotiable lock for a term: on when the employer has locked that term, disabled (but still showing its
     * state) once the contract has been accepted, since the locks can no longer change at that point.
     */
    private static JCheckBox nonNegotiableToggle(boolean locked, boolean editable) {
        Pill pill = new Pill(PillKind.LOCK, getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.locked"),
              getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.negotiable"), locked);
        String toolTipKey = editable
                                  ? "edit.contractMarket.field.nonNegotiable.tooltip"
                                  : "edit.contractMarket.field.nonNegotiable.disabled.tooltip";
        pill.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, toolTipKey)));
        pill.setEnabled(editable);
        return pill;
    }

    private static JSpinner intSpinner(int value, int minimum) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(max(value, minimum), minimum, Integer.MAX_VALUE, 1));
        if (spinner.getEditor() instanceof JSpinner.NumberEditor editor) {
            editor.getTextField().setColumns(6);
        }
        EditorHud.styleSpinner(spinner);
        return spinner;
    }

    /**
     * Builds the Scale spinner: whole numbers as the rules use them, or - under the Fractional Scale option - the exact
     * Scale shown to two decimal places.
     *
     * @param exactScale         the contract's exact Scale
     * @param useFractionalScale whether the Fractional Scale option is set
     *
     * @return the Scale spinner
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static JSpinner scaleSpinner(double exactScale, boolean useFractionalScale) {
        if (!useFractionalScale) {
            return intSpinner(ChaosContractDeterminationScale.roundUpScale(exactScale), 1);
        }

        double minimumScale = ChaosContractDeterminationScale.MINIMUM_FRACTIONAL_SCALE;
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(max(exactScale, minimumScale), minimumScale,
              Double.MAX_VALUE, FRACTIONAL_SCALE_STEP));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.00");
        // Without this the editor sizes itself to fit Double.MAX_VALUE, blowing the row far past the viewport.
        editor.getTextField().setColumns(6);
        spinner.setEditor(editor);
        EditorHud.styleSpinner(spinner);
        return spinner;
    }

    /**
     * @return the Scale to save: the exact Scale the spinner opened on if the GM left it alone, otherwise the GM's
     *       value
     *
     * @author Illiani
     * @since 0.51.01
     */
    private double getEditedScale() {
        double editedScale = ((Number) scaleSpinner.getValue()).doubleValue();
        return (editedScale == initialScaleSpinnerValue) ? contract.getExactScale() : editedScale;
    }

    /**
     * Builds the "Automatic" pill that governs a field, or {@code null} when the dialog is editing an existing contract
     * rather than creating one. The pill (re)generates the value with the contract-generation rules, which for some
     * fields (combat elements, target planet) carry random variance - appropriate for a brand-new offer, but not for a
     * GM tuning an accepted contract's values by hand - so it is only offered in create mode. When present it starts
     * on, which disables the field; turning it off hands control back to the GM.
     */
    private @Nullable JCheckBox automaticToggle(JComponent field) {
        if (!createMode) {
            return null;
        }
        Pill automatic = autoPill(true);
        field.setEnabled(false); // on by default, so the field starts disabled
        automatic.addActionListener(e -> field.setEnabled(!automatic.isSelected()));
        return automatic;
    }

    /** The state of an "Automatic" pill for change tracking, treating a missing pill (edit mode) as off. */
    private static boolean automaticState(@Nullable JCheckBox automatic) {
        return automatic != null && automatic.isSelected();
    }

    /**
     * Lays a field beside its "Automatic" pill as a single component for a row. With no pill (a parameter in edit
     * mode) the field is returned on its own.
     */
    private static JComponent withAutomatic(JComponent field, @Nullable JCheckBox automatic) {
        return automatic == null ? field : EditorHud.line(field, automatic);
    }

    private static JSpinner moneySpinner(Money money) {
        double amount = money == null ? 0.0 : money.getAmount().doubleValue();
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(amount, 0.0, Double.MAX_VALUE, MONEY_STEP));
        // Without this the editor sizes itself to fit Double.MAX_VALUE, blowing the row far past the viewport.
        if (spinner.getEditor() instanceof JSpinner.NumberEditor editor) {
            editor.getTextField().setColumns(12);
        }
        EditorHud.styleSpinner(spinner);
        return spinner;
    }

    /**
     * An "Automatic" pill for a finance field. Finance determination is deterministic (derived from scale, terms, and
     * the journey to the target), so unlike the parameter automatics it is offered in both modes - defaulting on for a
     * new offer and off when editing, so an existing contract's figures show as they stand. Turning it on disables the
     * spinner and, on save, (re)computes the value via {@link AbstractContractGeneration}.
     */
    private static JCheckBox financeAutomatic(JSpinner spinner, boolean selected) {
        Pill automatic = autoPill(selected);
        spinner.setEnabled(!automatic.isSelected());
        automatic.addActionListener(e -> spinner.setEnabled(!automatic.isSelected()));
        return automatic;
    }

    private JCheckBox financeAutomatic(JSpinner spinner) {
        return financeAutomatic(spinner, createMode);
    }

    /**
     * Keeps a manual pay figure in step with the term that sets it. A manual figure is saved as it stands, so without
     * this a changed term would save the same pay and the market's estimate would not move. The figure is scaled by
     * the term's multiplier, the same way the negotiation table projects pay. A figure that has only ever stood at a
     * zero multiplier has nothing to scale from, so it is handed to its "Automatic" pill and worked out on save.
     */
    private static void linkPayToTerm(StepTrack track, JSpinner spinner, JCheckBox automatic,
          ToDoubleFunction<ChaosContractStepsTable> multiplier) {
        // The figure per unit of multiplier, or NaN until it has stood at a non-zero multiplier
        double[] perMultiplier = { Double.NaN };
        Runnable anchor = () -> {
            double currentMultiplier = multiplier.applyAsDouble(track.getSelected());
            if (currentMultiplier != 0) {
                perMultiplier[0] = ((Number) spinner.getValue()).doubleValue() / currentMultiplier;
            }
        };
        anchor.run();
        spinner.addChangeListener(event -> anchor.run());
        track.addChangeListener(() -> {
            if (automatic.isSelected()) {
                return;
            }
            if (Double.isNaN(perMultiplier[0])) {
                automatic.setSelected(true);
                spinner.setEnabled(false);
                return;
            }
            spinner.setValue(perMultiplier[0] * multiplier.applyAsDouble(track.getSelected()));
        });
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
        styleFactionCombo(combo);
        return combo;
    }

    private static void styleFactionCombo(FactionComboBox combo) {
        EditorHud.styleCombo(combo, value -> value instanceof Map.Entry<?, ?> entry
                                                   ? String.valueOf(entry.getValue())
                                                   : "");
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
        styleFactionCombo(combo);
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

    /** The sponsor picker: a "None" entry, then the custom options, then the real active factions. */
    private FactionComboBox sponsorCombo(String selectedCode) {
        return factionComboWithCustoms(selectedCode, true);
    }

    /** A HUD combo over the given values, seeded to {@code selected} and naming each value with {@code labeler}. */
    @SuppressWarnings("unchecked")
    private static <E> JComboBox<E> enumCombo(E[] values, @Nullable E selected, Function<E, String> labeler) {
        JComboBox<E> combo = new JComboBox<>(new DefaultComboBoxModel<>(values));
        combo.setSelectedItem(selected);
        EditorHud.styleCombo(combo, value -> labeler.apply((E) value));
        return combo;
    }

    /**
     * A force-skill picker offering the full Ultra-Green through Legendary range plus a "Random" entry (named from
     * {@link SkillLevel#NONE}). The stored, concrete skill is shown on open; a Random selection is resolved to a single
     * level when the edits are applied.
     */
    private static JComboBox<SkillLevel> forceSkillCombo(SkillLevel selected) {
        return enumCombo(SkillLevelPickerUtility.PICKER_LEVELS, selected,
              level -> SkillLevelPickerUtility.isRandom(level)
                             ? SkillLevelPickerUtility.getRandomLabel()
                             : String.valueOf(level));
    }

    /** The numeric equipment rating for the picker's choice, defaulting to the middling C rating. */
    private static int equipmentValue(HudSegmentedControl<DragoonRating> picker) {
        DragoonRating rating = picker.getSelected();
        return (rating == null ? DragoonRating.DRAGOON_C : rating).getRating();
    }

    /** Which single facet of a {@link ChaosContractStepsTable} step a term governs, for its effect text. */
    private enum TermEffect {PAY_RATE, SUPPORT, TRANSPORT, SALVAGE, COMMAND}

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
        return "<html>" + getFormattedTextAt(RESOURCE_BUNDLE, "edit.contractMarket.stepLabel", step.stepValue())
                     + ": " + wordWrap(text).replace("<html>", "").replace("</html>", "") + "</html>";
    }

    /** The short effect a step has on a term, shown beside its track (for example "x1.20" or "Exchange, 25%"). */
    private static String termEffectText(TermEffect term, ChaosContractStepsTable step) {
        return switch (term) {
            case PAY_RATE -> getFormattedTextAt(RESOURCE_BUNDLE, "edit.contractMarket.term.effect.payRate",
                  ContractMarketFormatting.decimalFormat("0.00").format(step.getBasePayMultiplier()));
            case SUPPORT -> getFormattedTextAt(RESOURCE_BUNDLE, "edit.contractMarket.term.effect.support",
                  percent(step.getStraightSupportMultiplier()), percent(step.getBattlefieldLossMultiplier()));
            case TRANSPORT -> getFormattedTextAt(RESOURCE_BUNDLE, "edit.contractMarket.term.effect.transport",
                  percent(step.getTransportMultiplier()));
            case SALVAGE -> {
                if (step.isExchangeSalvage()) {
                    yield getFormattedTextAt(RESOURCE_BUNDLE, "edit.contractMarket.term.effect.salvage.exchange",
                          percent(step.getSalvageMultiplier()));
                }
                yield step.getSalvageMultiplier() <= 0
                            ? getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.term.effect.salvage.none")
                            : getFormattedTextAt(RESOURCE_BUNDLE, "edit.contractMarket.term.effect.salvage",
                                  percent(step.getSalvageMultiplier()));
            }
            case COMMAND -> step.getContractCommandRights().toString();
        };
    }

    /** A 0-1 (or higher, for pay) multiplier as a whole-number percentage. */
    private static int percent(double multiplier) {
        return (int) Math.round(multiplier * 100);
    }

    private static JComboBox<ContractObjectiveType> objectiveCombo(ContractObjectiveType selected) {
        JComboBox<ContractObjectiveType> combo = enumCombo(ContractObjectiveType.values(), selected,
              type -> type == null ? "" : type.toString());
        combo.addActionListener(event -> {
            ContractObjectiveType type = (ContractObjectiveType) combo.getSelectedItem();
            combo.setToolTipText(type == null ? null : wordWrap(type.getToolTipText()));
        });
        combo.setToolTipText(selected == null ? null : wordWrap(selected.getToolTipText()));
        return combo;
    }

    /**
     * Builds a camouflage preview button. Clicking it opens the chooser seeded with the current selection; the button's
     * icon and the backing value are refreshed on confirmation.
     */
    private JButton camoButton(Supplier<Camouflage> current, Consumer<Camouflage> onChange) {
        JButton button = new JButton();
        EditorHud.styleIconButton(button);
        button.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.camouflage.tooltip")));
        applyIcon(button, current.get(), scaleForGUI(28));
        button.addActionListener(e -> openCamoChooser(button, current, onChange));
        return button;
    }

    private void openCamoChooser(JButton button, Supplier<Camouflage> current, Consumer<Camouflage> onChange) {
        CamoChooserDialog chooser = new CamoChooserDialog(campaign.getGUI().getFrame(), current.get());
        if (chooser.showDialog().isConfirmed()) {
            Camouflage chosen = chooser.getSelectedItem();
            onChange.accept(chosen);
            applyIcon(button, chosen, scaleForGUI(28));
            onEdited();
        }
    }

    private static void applyIcon(JButton button, AbstractIcon icon, int size) {
        ImageIcon image = icon == null ? null : icon.getImageIcon(size);
        button.setIcon(image);
        button.setHorizontalAlignment(JButton.CENTER);
        button.setPreferredSize(new Dimension(size + scaleForGUI(8), size + scaleForGUI(6)));
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
        EditorHud.styleCombo(combo, String::valueOf);
        return combo;
    }

    private @Nullable NpcEditor npcEditor(@Nullable Person person) {
        return person == null ? null : new NpcEditor(person);
    }

    /** The rank system of the faction currently selected in the given combo, or {@code null} if none is resolvable. */
    private static @Nullable RankSystem rankSystemOf(FactionComboBox combo) {
        Faction faction = factionOf(combo);
        return faction == null ? null : faction.getRankSystem();
    }

    /** A portrait tile for an NPC card: the chosen portrait, clickable to choose another. */
    private JButton portraitButton(@Nullable Portrait portrait) {
        JButton button = new JButton();
        EditorHud.styleIconButton(button);
        button.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "edit.contractMarket.field.portrait.tooltip")));
        applyIcon(button, portrait == null ? new Portrait() : portrait, scaleForGUI(ICON_SIZE));
        return button;
    }

    // endregion Component builders

    // region Target helpers

    private @Nullable PlanetarySystem resolveSystem(String name) {
        String trimmed = name == null ? "" : name.trim();
        return trimmed.isEmpty() ? null : campaign.getSystemByName(trimmed);
    }

    /** Fills the planet combo with a leading "none" entry then the given system's planets, selecting {@code selectId}. */
    private void repopulatePlanets(@Nullable PlanetarySystem system, @Nullable String selectId) {
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

    private static <E> E selectedOr(HudSegmentedControl<E> picker, E fallback) {
        E selected = picker.getSelected();
        return selected == null ? fallback : selected;
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
    private static @Nullable String sponsorKey(FactionComboBox combo) {
        String key = combo.getSelectedItemKey();
        return (key == null || key.isBlank()) ? null : key;
    }

    /** Trimmed field text (never null; blank stays blank). */
    private static String text(JTextField field) {
        return field.getText().trim();
    }

    /** Parses an ISO date from a field, treating blank as {@code null} and an unparseable value as {@code fallback}. */
    private static @Nullable LocalDate parseDate(JTextField field, LocalDate fallback) {
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
            this.givenNameField = new JTextField(person.getGivenName(), 12);
            this.surnameField = new JTextField(person.getSurname(), 12);
            EditorHud.styleField(givenNameField);
            EditorHud.styleField(surnameField);
            this.ranks = rankCombo(person);
            this.portrait = person.getPortrait();
            this.portraitButton = portraitButton(portrait);
            portraitButton.addActionListener(e -> {
                PortraitChooserDialog chooser = new PortraitChooserDialog(campaign.getGUI().getFrame(), portrait);
                if (chooser.showDialog().isConfirmed()) {
                    portrait = chooser.getSelectedItem();
                    applyIcon(portraitButton, portrait, scaleForGUI(ICON_SIZE));
                    onEdited();
                }
            });
            watch(givenNameField);
            watch(surnameField);
            watch(ranks);
        }

        /** The card: the portrait beside rank, given name and surname. */
        private JPanel buildCard() {
            JPanel fields = Hud.transparentPanel(new GridLayout(1, 3, scaleForGUI(6), 0));
            fields.add(labelled("edit.contractMarket.field.rank", ranks));
            fields.add(labelled("edit.contractMarket.field.givenName", givenNameField));
            fields.add(labelled("edit.contractMarket.field.surname", surnameField));
            return personCard(portraitButton, null, fields);
        }

        private Object state() {
            return List.of(givenNameField.getText(), surnameField.getText(), String.valueOf(ranks.getSelectedItem()),
                  String.valueOf(portrait));
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
     * {@link #refreshRanks(RankSystem)} when that faction changes). A field left on "Automatic" leaves the
     * corresponding generated value untouched.
     */
    private final class NpcOverride {
        private final JTextField givenNameField = new JTextField(12);
        private final JTextField surnameField = new JTextField(12);
        private final JComboBox<RankDisplay> ranks = new JComboBox<>();
        private final JButton portraitButton;
        private transient Portrait portrait;
        private JCheckBox givenNameAutomatic;
        private JCheckBox surnameAutomatic;
        private JCheckBox rankAutomatic;
        private JCheckBox portraitAutomatic;

        private NpcOverride() {
            EditorHud.styleField(givenNameField);
            EditorHud.styleField(surnameField);
            EditorHud.styleCombo(ranks, String::valueOf);
            portraitButton = portraitButton(null); // placeholder; portrait stays null until the GM picks one
            givenNameField.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "create.contractMarket.npc.tooltip")));
            surnameField.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "create.contractMarket.npc.tooltip")));
            refreshRanks(null);
            portraitButton.addActionListener(e -> {
                PortraitChooserDialog chooser = new PortraitChooserDialog(campaign.getGUI().getFrame(),
                      portrait == null ? new Portrait() : portrait);
                if (chooser.showDialog().isConfirmed()) {
                    portrait = chooser.getSelectedItem();
                    applyIcon(portraitButton, portrait, scaleForGUI(ICON_SIZE));
                    onEdited();
                }
            });
        }

        /**
         * Repopulates the rank options for the given rank system. Whether the generated rank is kept is governed by the
         * field's "Automatic" pill, so no synthetic "(use generated)" entry is needed here.
         */
        private void refreshRanks(@Nullable RankSystem rankSystem) {
            DefaultComboBoxModel<RankDisplay> model = new DefaultComboBoxModel<>();
            if (rankSystem != null) {
                model.addAll(RankDisplay.getRankDisplaysForSystem(rankSystem, DEFAULT_PROFESSION));
            }
            ranks.setModel(model);
            if (model.getSize() > 0) {
                ranks.setSelectedIndex(0);
            }
        }

        /** The card: the portrait (with its pill) beside rank, given name and surname, each with its own pill. */
        private JPanel buildCard() {
            givenNameAutomatic = automaticToggle(givenNameField);
            surnameAutomatic = automaticToggle(surnameField);
            rankAutomatic = automaticToggle(ranks);
            portraitAutomatic = automaticToggle(portraitButton);
            watch(givenNameField);
            watch(surnameField);
            watch(ranks);
            watch(givenNameAutomatic);
            watch(surnameAutomatic);
            watch(rankAutomatic);
            watch(portraitAutomatic);

            JPanel fields = Hud.transparentPanel(new GridLayout(1, 3, scaleForGUI(6), 0));
            fields.add(labelled("edit.contractMarket.field.rank", withAutomatic(ranks, rankAutomatic)));
            fields.add(labelled("edit.contractMarket.field.givenName",
                  withAutomatic(givenNameField, givenNameAutomatic)));
            fields.add(labelled("edit.contractMarket.field.surname", withAutomatic(surnameField, surnameAutomatic)));
            return personCard(portraitButton, portraitAutomatic, fields);
        }

        private Object state() {
            return List.of(givenNameField.getText(), surnameField.getText(), String.valueOf(ranks.getSelectedItem()),
                  String.valueOf(portrait), automaticState(givenNameAutomatic), automaticState(surnameAutomatic),
                  automaticState(rankAutomatic), automaticState(portraitAutomatic));
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

    /** An NPC card: the portrait (and, in create mode, its pill) beside the fields. */
    private static JPanel personCard(JButton portrait, @Nullable JCheckBox portraitAutomatic, JComponent fields) {
        JPanel portraitColumn = MarketHud.column();
        portrait.setAlignmentX(Component.LEFT_ALIGNMENT);
        portraitColumn.add(portrait);
        if (portraitAutomatic != null) {
            portraitColumn.add(Box.createVerticalStrut(scaleForGUI(4)));
            portraitAutomatic.setAlignmentX(Component.LEFT_ALIGNMENT);
            portraitColumn.add(portraitAutomatic);
        }
        JPanel card = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        card.setOpaque(true);
        card.setBackground(GROUND);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(8), scaleForGUI(8), scaleForGUI(8))));
        card.add(MarketHud.topAligned(portraitColumn), BorderLayout.WEST);
        card.add(MarketHud.topAligned(fields), BorderLayout.CENTER);
        return card;
    }

    /** A small caption above a field inside an NPC card. */
    private static JPanel labelled(String labelKey, JComponent field) {
        JPanel panel = Hud.transparentPanel(new BorderLayout(0, scaleForGUI(3)));
        panel.add(HudStyle.keyLabel(getTextAt(RESOURCE_BUNDLE, labelKey).toUpperCase(Locale.ROOT)),
              BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }
}
