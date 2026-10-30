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

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT_BRIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.DANGER;
import static mekhq.gui.baseComponents.hud.HudStyle.GROUND;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jakarta.annotation.Nullable;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.ShellContract;
import mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable;
import mekhq.campaign.mission.contract.contractData.ContractCommandRights;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.EmployerLiaison;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.OpposingCommander;
import mekhq.campaign.mission.contract.utilities.ShellContractFactory;
import mekhq.campaign.mission.contract.utilities.ShellContractFactory.ShellContractDetails;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudSegmentedControl;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;
import mekhq.gui.baseComponents.hud.HudStyle;
import mekhq.gui.dialog.DateChooser;
import mekhq.gui.dialog.markets.contractMarket.EditorHud.Row;
import mekhq.gui.utilities.JSuggestField;

/**
 * Creates or edits a {@link ShellContract}: a player-authored contract that holds manually created scenarios.
 *
 * <p>Drawn as a heads-up display in the style of the contract market and its editor: a command bar naming the
 * contract, a form of labelled rows grouped under section headings, and a footer holding Cancel and Save. The player
 * supplies only the basics - name, description, objective, belligerents, location, schedule, monthly pay, and salvage
 * and command rights. The belligerents are picked the way the system is, by typing a faction's name and choosing from
 * the suggestions, but either side may also be given any name at all. A custom name stands in front of a generic Inner
 * Sphere, Periphery or Clan faction, the same customs the GM contract editor offers, which decides the logo shown for
 * it and the faction its liaison or commander is drawn from. A problem with the form is reported in the footer rather
 * than in a pop-up.</p>
 *
 * <p>In create mode, confirming adds the new contract to the campaign as an active mission (read it back with
 * {@link #getContract()}); in edit mode the given contract is updated in place.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ShellContractDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(ShellContractDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ShellContractDialog";

    private static final int DEFAULT_LENGTH_MONTHS = 6;
    private static final int MONEY_STEP = 1000;
    private static final int LABEL_WIDTH = 110;
    private static final int COLUMN_GAP = 22;
    /** The search type liaisons are generated for: shell contracts have no market, so treat them as a government's */
    private static final ContractSearchType LIAISON_SEARCH_TYPE = ContractSearchType.GOVERNMENT;
    /**
     * The generic factions a custom belligerent can stand in front of, in the order they are offered: Inner Sphere,
     * Periphery and Clan. The first is the default.
     */
    private static final List<String> CUSTOM_FACTION_CODES = List.of("IS", "Periphery", "CLAN");

    private final transient Campaign campaign;
    private final LocalDate currentDate;
    private transient @Nullable ShellContract contract;

    private final JLabel titleLabel = new JLabel();
    private JLabel footerMessage;

    private JTextField nameField;
    private JTextArea descriptionArea;
    private JComboBox<ContractObjectiveType> objectiveCombo;
    /** Faction codes keyed by the names the employer and enemy fields suggest for them */
    private final Map<String, String> factionCodesByName = new HashMap<>();
    private BelligerentInput employerInput;
    private BelligerentInput enemyInput;
    private JSuggestField systemField;
    private JComboBox<Planet> planetCombo;
    private HudButton startDateButton;
    private LocalDate startDate;
    private JSpinner lengthSpinner;
    private JCheckBox openEndedCheck;
    private JSpinner monthlyPaySpinner;
    private JComboBox<ChaosContractStepsTable> salvageCombo;
    private HudSegmentedControl<ContractCommandRights> commandPicker;

    private boolean confirmed;

    /**
     * Opens the dialog modally.
     *
     * @param owner    the owning window: the main frame, or the contract market when opened from there
     * @param campaign the active campaign
     * @param contract the shell contract to edit, or {@code null} to create a new one
     */
    public ShellContractDialog(Window owner, Campaign campaign, @Nullable ShellContract contract) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.campaign = campaign;
        this.currentDate = campaign.getLocalDate();
        this.contract = contract;

        initializeComponents();
    }

    /** @return whether the player confirmed the dialog */
    public boolean wasConfirmed() {
        return confirmed;
    }

    /** @return the created or edited contract, or {@code null} if the dialog was cancelled in create mode */
    public @Nullable ShellContract getContract() {
        return contract;
    }

    // region Layout

    private void initializeComponents() {
        setTitle(getTextAt(RESOURCE_BUNDLE, contract == null ? "title.create" : "title.edit"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(buildCommandBar(), BorderLayout.NORTH);
        content.add(MarketHud.scroll(buildForm(), SURFACE_DEEP), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);

        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              "cancel");
        content.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        refreshTitle();
        setMinimumSize(scaleForGUI(720, 520));
        pack();
        setSize(scaleForGUI(860, 680)); // Default opening size; saved preferences (below) override it on later opens
        setLocationRelativeTo(getOwner());
        setPreferences(); // Must be before setVisible
        setVisible(true);
    }

    /**
     * Tracks this dialog's window size and position in MekHQ's preferences so it reopens where the player left it.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ShellContractDialog.class);
            setName("ShellContractDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    /** The market-style command bar: what is being made or edited, over the contract's live name. */
    private JPanel buildCommandBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(true);
        bar.setBackground(GROUND);
        bar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), scaleForGUI(10), scaleForGUI(14))));

        JPanel identity = MarketHud.column();
        identity.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE,
              contract == null ? "header.eyebrow.create" : "header.eyebrow.edit"))));
        identity.add(Box.createVerticalStrut(scaleForGUI(2)));
        titleLabel.setForeground(ACCENT_BRIGHT);
        titleLabel.setFont(hudFont(Font.BOLD, 1.12f, 0.12f));
        identity.add(leftAligned(titleLabel));

        bar.add(identity, BorderLayout.CENTER);
        return bar;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(14), scaleForGUI(8), scaleForGUI(14))));

        footerMessage = Hud.hint(getTextAt(RESOURCE_BUNDLE, "footer.hint"));
        footer.add(footerMessage, BorderLayout.CENTER);

        HudButton cancel = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.cancel").toUpperCase(Locale.ROOT), false);
        cancel.addActionListener(event -> dispose());
        HudButton save = new HudButton(getTextAt(RESOURCE_BUNDLE,
              contract == null ? "button.create" : "button.save").toUpperCase(Locale.ROOT), true);
        save.addActionListener(event -> confirm());

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(save);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    /** The form: the contract's identity, then its belligerents, location, schedule and terms under sub-headings. */
    private JPanel buildForm() {
        JPanel form = MarketHud.column();
        form.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(16), scaleForGUI(18), scaleForGUI(24),
              scaleForGUI(18)));
        form.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "section.contract"))));
        form.add(Box.createVerticalStrut(scaleForGUI(4)));
        buildIdentityRows(form);

        form.add(leftAligned(subHeading("sub.belligerents")));
        Grid grid = new Grid();
        buildBelligerentRows(grid);
        form.add(leftAligned(grid.panel));

        form.add(leftAligned(subHeading("sub.where")));
        grid = new Grid();
        buildTargetRows(grid);
        form.add(leftAligned(grid.panel));

        form.add(leftAligned(subHeading("sub.when")));
        grid = new Grid();
        buildScheduleRows(grid);
        form.add(leftAligned(grid.panel));

        form.add(leftAligned(subHeading("sub.terms")));
        grid = new Grid();
        buildTermsRows(grid);
        form.add(leftAligned(grid.panel));
        return form;
    }

    private void buildIdentityRows(JPanel form) {
        nameField = new JTextField(contract == null ? "" : contract.getName());
        EditorHud.styleField(nameField);
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshTitle();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshTitle();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshTitle();
            }
        });
        form.add(leftAligned(row("field.name", nameField)));

        objectiveCombo = new JComboBox<>(ContractObjectiveType.values());
        EditorHud.styleCombo(objectiveCombo, value -> value == null ? "" : value.toString());
        objectiveCombo.setSelectedItem(contract == null ? ContractObjectiveType.UNDEFINED
                                             : contract.getObjectiveType());
        form.add(leftAligned(row("field.objective", objectiveCombo)));

        descriptionArea = new JTextArea(contract == null ? "" : contract.getDescription(), 4, 20);
        EditorHud.styleTextArea(descriptionArea);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        Hud.styleScroll(descriptionScroll, GROUND, true);
        descriptionScroll.setPreferredSize(new Dimension(scaleForGUI(300), scaleForGUI(92)));
        form.add(leftAligned(row("field.description", descriptionScroll)));
    }

    private void buildBelligerentRows(Grid grid) {
        Faction playerFaction = campaign.getPlayerForce().getFaction();
        String employerCode = contract == null
                                    ? (playerFaction == null ? null : playerFaction.getShortName())
                                    : contract.getEmployerFactionCode();
        String enemyCode = contract == null ? Faction.PIRATE_FACTION_CODE : contract.getEnemyFactionCode();
        String employerName = contract == null ? null : contract.getEmployerDisplayName();
        String enemyName = contract == null ? null : contract.getEnemyDisplayName();
        boolean employerCustom = isCustomBelligerent(employerCode, employerName);
        boolean enemyCustom = isCustomBelligerent(enemyCode, enemyName);

        // A custom side's generic faction is not one to suggest, so only a real faction is kept in the names.
        buildFactionNames(employerCustom ? null : employerCode, enemyCustom ? null : enemyCode);
        Vector<String> factionNames = new Vector<>(factionCodesByName.keySet());
        factionNames.sort(String.CASE_INSENSITIVE_ORDER);

        employerInput = new BelligerentInput(factionNames, employerCode, employerCustom ? employerName : null);
        grid.add(row("field.employer", employerInput.component));

        enemyInput = new BelligerentInput(factionNames, enemyCode, enemyCustom ? enemyName : null);
        grid.add(row("field.enemy", enemyInput.component));
    }

    /**
     * @return {@code true} if a saved side is a custom belligerent: one of the generic factions, named as something
     *       other than that faction
     */
    private boolean isCustomBelligerent(@Nullable String code, @Nullable String displayName) {
        if ((displayName == null) || !CUSTOM_FACTION_CODES.contains(code)) {
            return false;
        }
        return !displayName.equals(Factions.getInstance().getFaction(code).getFullName(campaign.getGameYear()));
    }

    private void buildTargetRows(Grid grid) {
        PlanetarySystem system = contract == null ? campaign.getCurrentSystem() : contract.getTargetSystem();
        systemField = new JSuggestField(this, campaign.getSystemNames());
        EditorHud.styleField(systemField);
        if (system != null) {
            systemField.setText(system.getName(currentDate));
        }
        // JSuggestField reports a suggestion picked from its dropdown through addSelectionListener, while a bare Enter
        // fires the text field's own action event - wire both so the planet list tracks the system however it is
        // chosen.
        ActionListener onSystemChanged = event -> repopulatePlanets(resolveSystem(), null);
        systemField.addActionListener(onSystemChanged);
        systemField.addSelectionListener(onSystemChanged);
        grid.add(row("field.system", systemField));

        planetCombo = new JComboBox<>();
        EditorHud.styleCombo(planetCombo, value -> value instanceof Planet planet
                                                         ? planet.getName(currentDate)
                                                         : getTextAt(RESOURCE_BUNDLE, "field.planet.none"));
        String planetId = contract == null
                                ? (system == null || system.getPrimaryPlanet() == null
                                         ? null : system.getPrimaryPlanet().getId())
                                : contract.getTargetPlanetId();
        repopulatePlanets(system, planetId);
        grid.add(row("field.planet", planetCombo));
    }

    private void buildScheduleRows(Grid grid) {
        startDate = (contract == null || contract.getStartDate() == null) ? currentDate : contract.getStartDate();
        startDateButton = new HudButton("", false, true);
        startDateButton.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "field.startDate.pick.tooltip")));
        startDateButton.addActionListener(event -> pickStartDate());
        refreshStartDateButton();
        grid.add(row("field.startDate", EditorHud.line(null, startDateButton)));

        boolean openEnded = contract != null && contract.getEndingDate() == null;
        int length = (contract == null || openEnded) ? DEFAULT_LENGTH_MONTHS : Math.max(1,
              contract.getLengthInMonths());
        lengthSpinner = new JSpinner(new SpinnerNumberModel(length, 1, 999, 1));
        EditorHud.styleSpinner(lengthSpinner);
        openEndedCheck = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "field.openEnded"));
        openEndedCheck.setSelected(openEnded);
        openEndedCheck.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "field.openEnded.tooltip")));
        openEndedCheck.addActionListener(event -> lengthSpinner.setEnabled(!openEndedCheck.isSelected()));
        lengthSpinner.setEnabled(!openEnded);
        grid.add(row("field.length", EditorHud.line(lengthSpinner, openEndedCheck)));
    }

    private void buildTermsRows(Grid grid) {
        double monthlyPay = contract == null ? 0.0 : contract.getMonthlyPayOut().getAmount().doubleValue();
        monthlyPaySpinner = new JSpinner(new SpinnerNumberModel(monthlyPay, 0.0, Double.MAX_VALUE, MONEY_STEP));
        // Without this the editor sizes itself to fit Double.MAX_VALUE, blowing the row far past the viewport.
        if (monthlyPaySpinner.getEditor() instanceof JSpinner.NumberEditor editor) {
            editor.getTextField().setColumns(12);
        }
        EditorHud.styleSpinner(monthlyPaySpinner);
        grid.add(row("field.monthlyPay", monthlyPaySpinner));

        List<ChaosContractStepsTable> salvageSteps = ShellContractFactory.getSalvageRightsSteps();
        salvageCombo = new JComboBox<>(salvageSteps.toArray(ChaosContractStepsTable[]::new));
        EditorHud.styleCombo(salvageCombo, value -> value instanceof ChaosContractStepsTable step
                                                          ? salvageLabel(step) : "");
        ChaosContractStepsTable salvageRights = contract == null ? ShellContractFactory.DEFAULT_SALVAGE_RIGHTS_STEP
                                                      : contract.getSalvageRightsStep();
        salvageCombo.setSelectedItem(closestSalvageStep(salvageSteps, salvageRights));
        grid.add(row("field.salvage", salvageCombo));

        commandPicker = new HudSegmentedControl<>(value -> {});
        List<Segment<ContractCommandRights>> segments = new ArrayList<>();
        for (ContractCommandRights commandRights : ContractCommandRights.values()) {
            segments.add(new Segment<>(commandRights, commandRights.toString(), null, true));
        }
        commandPicker.setSegments(segments);
        commandPicker.setSelected(contract == null ? ShellContractFactory.DEFAULT_COMMAND_RIGHTS
                                        : contract.getCommandRights());
        grid.addWide(row("field.command", commandPicker));
    }

    /** A labelled form row, with the label's tooltip read from {@code labelKey + ".tooltip"} when there is one. */
    private static Row row(String labelKey, JComponent control) {
        String toolTip = getTextAt(RESOURCE_BUNDLE, labelKey + ".tooltip");
        return new Row(getTextAt(RESOURCE_BUNDLE, labelKey), isResourceKeyValid(toolTip) ? wordWrap(toolTip) : null,
              control, null, LABEL_WIDTH);
    }

    private static JComponent subHeading(String key) {
        JLabel label = HudStyle.keyLabel(getTextAt(RESOURCE_BUNDLE, key).toUpperCase(Locale.ROOT));
        label.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(9), scaleForGUI(2), 0));
        return label;
    }

    /** Lays rows out in two columns, with wide rows taking both. */
    private static final class Grid {
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

    /**
     * Fills {@link #factionCodesByName} with the real factions active in the campaign year, named as they are that
     * year. A faction already on the contract that is no longer active is still added, so its name stays valid.
     * Factions sharing a name have their code appended, as the faction pickers elsewhere do.
     *
     * @param extraCodes faction codes to include whether or not they are active; blank or {@code null} codes are
     *                   skipped
     */
    private void buildFactionNames(String... extraCodes) {
        Set<String> codes = new HashSet<>();
        for (Faction faction : Factions.getInstance().getActiveFactions(currentDate)) {
            if (!faction.isAggregate()) {
                codes.add(faction.getShortName());
            }
        }
        for (String code : extraCodes) {
            if (code != null && !code.isBlank()) {
                codes.add(code);
            }
        }

        int gameYear = campaign.getGameYear();
        Map<String, Integer> nameCounts = new HashMap<>();
        for (String code : codes) {
            nameCounts.merge(Factions.getInstance().getFaction(code).getFullName(gameYear), 1, Integer::sum);
        }
        for (String code : codes) {
            String name = Factions.getInstance().getFaction(code).getFullName(gameYear);
            factionCodesByName.put(nameCounts.get(name) > 1 ? name + " (" + code + ')' : name, code);
        }
    }

    /** One side of the contract as entered: the faction it belongs to and the name shown for it. */
    private record Belligerent(String factionCode, String displayName) {}

    /**
     * One side's belligerent input: a faction field that suggests the known factions but takes any name, with a picker
     * for the generic faction a custom name stands in front of. The picker shows only while the name matches no
     * faction.
     */
    private final class BelligerentInput {
        private final JSuggestField field;
        private final HudSegmentedControl<String> customPicker = new HudSegmentedControl<>(value -> {});
        private final JPanel component = Hud.transparentPanel(new BorderLayout(0, scaleForGUI(6)));

        /**
         * @param factionNames the faction names to suggest
         * @param code         the side's faction code to start with
         * @param customName   the side's custom name to start with, or {@code null} when it is a known faction
         */
        private BelligerentInput(Vector<String> factionNames, @Nullable String code, @Nullable String customName) {
            field = new JSuggestField(ShellContractDialog.this, factionNames);
            EditorHud.styleField(field);

            List<Segment<String>> segments = new ArrayList<>();
            for (String customCode : CUSTOM_FACTION_CODES) {
                segments.add(new Segment<>(customCode, getTextAt(RESOURCE_BUNDLE, "field.custom." + customCode),
                      null, true));
            }
            customPicker.setSegments(segments);
            customPicker.setSelected((customName != null) ? code : CUSTOM_FACTION_CODES.getFirst());
            customPicker.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "field.custom.tooltip")));

            component.add(field, BorderLayout.NORTH);
            component.add(customPicker, BorderLayout.SOUTH);

            if (customName != null) {
                field.setText(customName);
            } else {
                for (Map.Entry<String, String> entry : factionCodesByName.entrySet()) {
                    if (entry.getValue().equals(code)) {
                        field.setText(entry.getKey());
                        break;
                    }
                }
            }

            field.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    refresh();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    refresh();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    refresh();
                }
            });
            refresh();
        }

        /** Shows the generic faction picker only while the name is a custom one. */
        private void refresh() {
            String text = field.getText().trim();
            boolean custom = !text.isEmpty() && (matchFactionCode(text) == null);
            if (customPicker.isVisible() != custom) {
                customPicker.setVisible(custom);
                component.revalidate();
                component.repaint();
            }
        }

        /**
         * @return the side as entered: the matching faction under its own name, or else the chosen generic faction
         *       under the name typed; {@code null} when the field is blank
         */
        private @Nullable Belligerent resolve() {
            String text = field.getText().trim();
            if (text.isEmpty()) {
                return null;
            }

            String code = matchFactionCode(text);
            if (code != null) {
                return new Belligerent(code,
                      Factions.getInstance().getFaction(code).getFullName(campaign.getGameYear()));
            }

            String customCode = customPicker.getSelected();
            return new Belligerent(customCode == null ? CUSTOM_FACTION_CODES.getFirst() : customCode, text);
        }
    }

    private static String salvageLabel(ChaosContractStepsTable step) {
        int percent = (int) Math.round(step.getSalvageMultiplier() * 100);
        return step.isExchangeSalvage()
                     ? getFormattedTextAt(RESOURCE_BUNDLE, "field.salvage.exchange", percent)
                     : getFormattedTextAt(RESOURCE_BUNDLE, "field.salvage.percent", percent);
    }

    /** The offered salvage step with the same outcome as {@code current}, so an edited contract keeps its rights. */
    private static ChaosContractStepsTable closestSalvageStep(List<ChaosContractStepsTable> salvageSteps,
          ChaosContractStepsTable current) {
        for (ChaosContractStepsTable step : salvageSteps) {
            if ((step.getSalvageMultiplier() == current.getSalvageMultiplier())
                      && (step.isExchangeSalvage() == current.isExchangeSalvage())) {
                return step;
            }
        }
        return salvageSteps.getFirst();
    }

    // endregion Layout

    // region Inputs

    /** Shows the contract's name in the command bar as it is typed, or a placeholder while it has none. */
    private void refreshTitle() {
        String name = nameField == null ? "" : nameField.getText().trim();
        titleLabel.setText((name.isEmpty() ? getTextAt(RESOURCE_BUNDLE, "header.title.unnamed") : name)
                                 .toUpperCase(Locale.ROOT));
    }

    /**
     * @return the code of the faction the text names, matched against the suggested names (or, failing that, faction
     *       codes) without regard to case; {@code null} when it names no faction
     */
    private @Nullable String matchFactionCode(String text) {
        for (Map.Entry<String, String> entry : factionCodesByName.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(text) || entry.getValue().equalsIgnoreCase(text)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private @Nullable PlanetarySystem resolveSystem() {
        String name = systemField.getText().trim();
        return name.isEmpty() ? null : campaign.getSystemByName(name);
    }

    /** Fills the planet combo with a leading "none" entry then the given system's planets, selecting one by id. */
    private void repopulatePlanets(@Nullable PlanetarySystem system, @Nullable String selectId) {
        planetCombo.removeAllItems();
        planetCombo.addItem(null);
        if (system == null) {
            return;
        }

        Planet toSelect = selectId == null ? system.getPrimaryPlanet() : system.getPlanetById(selectId);
        for (Planet planet : system.getPlanets()) {
            planetCombo.addItem(planet);
        }
        planetCombo.setSelectedItem(toSelect);
    }

    private void pickStartDate() {
        DateChooser chooser = new DateChooser(this, startDate);
        if (chooser.showDateChooser() == DateChooser.OK_OPTION) {
            startDate = chooser.getDate();
            refreshStartDateButton();
        }
    }

    private void refreshStartDateButton() {
        startDateButton.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(startDate));
    }

    // endregion Inputs

    // region Save

    private void confirm() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("error.name");
            return;
        }

        String systemText = systemField.getText().trim();
        PlanetarySystem system = resolveSystem();
        if (!systemText.isEmpty() && system == null) {
            showError("error.system");
            return;
        }

        Belligerent employer = employerInput.resolve();
        if (employer == null) {
            showError("error.employer");
            return;
        }

        Belligerent enemy = enemyInput.resolve();
        if (enemy == null) {
            showError("error.enemy");
            return;
        }

        Planet planet = (Planet) planetCombo.getSelectedItem();
        ContractCommandRights commandRights = commandPicker.getSelected();
        ShellContractDetails details = new ShellContractDetails(name,
              descriptionArea.getText().trim(),
              (ContractObjectiveType) objectiveCombo.getSelectedItem(),
              employer.factionCode(),
              employer.displayName(),
              enemy.factionCode(),
              enemy.displayName(),
              system == null ? null : system.getId(),
              planet == null ? null : planet.getId(),
              startDate,
              ((Number) lengthSpinner.getValue()).intValue(),
              openEndedCheck.isSelected(),
              Money.of(((Number) monthlyPaySpinner.getValue()).doubleValue()),
              (ChaosContractStepsTable) salvageCombo.getSelectedItem(),
              commandRights == null ? ShellContractFactory.DEFAULT_COMMAND_RIGHTS : commandRights);

        Faction employerFaction = Factions.getInstance().getFaction(employer.factionCode());
        Faction enemyFaction = Factions.getInstance().getFaction(enemy.factionCode());
        if (contract == null) {
            contract = ShellContractFactory.create(details, generateLiaison(employerFaction),
                  generateCommander(enemyFaction));
            campaign.addMission(contract);
        } else {
            // Only generate replacement NPCs for a side whose faction changed; the factory keeps the rest. Renaming a
            // custom side keeps its generic faction, so it keeps its NPC too.
            boolean employerChanged = !employer.factionCode().equals(contract.getEmployerFactionCode());
            boolean enemyChanged = !enemy.factionCode().equals(contract.getEnemyFactionCode());
            ShellContractFactory.update(contract, details,
                  employerChanged ? generateLiaison(employerFaction) : null,
                  enemyChanged ? generateCommander(enemyFaction) : null);
        }

        confirmed = true;
        dispose();
    }

    /**
     * Generates the employer's liaison, who speaks for the employer in briefings and events. A failure leaves the
     * contract without one rather than blocking its creation.
     */
    private @Nullable Person generateLiaison(Faction employerFaction) {
        try {
            return EmployerLiaison.generateLiaison(campaign, LIAISON_SEARCH_TYPE, employerFaction.isClan(),
                  employerFaction.getShortName());
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to generate a liaison for shell contract employer {}",
                  employerFaction.getShortName());
            return null;
        }
    }

    /**
     * Generates the opposing commander, who speaks for the enemy in events. A failure leaves the contract without one
     * rather than blocking its creation.
     */
    private @Nullable Person generateCommander(Faction enemyFaction) {
        try {
            return OpposingCommander.generateOpposingCommander(campaign, enemyFaction);
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to generate an opposing commander for shell contract enemy {}",
                  enemyFaction.getShortName());
            return null;
        }
    }

    /** Reports a problem with the form in the footer, in place of its usual hint. */
    private void showError(String key) {
        footerMessage.setText(getTextAt(RESOURCE_BUNDLE, key));
        footerMessage.setForeground(DANGER);
    }

    // endregion Save
}
