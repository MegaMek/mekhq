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
package mekhq.gui.dialog.camOpsSalvage;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.swing.*;

import megamek.client.ui.comboBoxes.FilteredComboBoxModel;
import megamek.client.ui.comboBoxes.SearchableComboBox;
import megamek.client.ui.dialogs.unitSelectorDialogs.EntityReadoutDialog;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBScenario;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.salvage.AbstractSalvage;
import mekhq.campaign.mission.scenarios.salvage.CamOpsSalvageUtilities;
import mekhq.campaign.mission.scenarios.salvage.RecoveryMethod;
import mekhq.campaign.mission.scenarios.salvage.RecoveryStatus;
import mekhq.campaign.mission.scenarios.salvage.RecoveryTimeCalculations;
import mekhq.campaign.mission.scenarios.salvage.RecoveryTimeData;
import mekhq.campaign.mission.scenarios.salvage.SalvageRecoveryPlan.RemainingCapacity;
import mekhq.campaign.mission.scenarios.salvage.SalvageRecoverySession;
import mekhq.campaign.mission.scenarios.salvage.SalvageRecoverySession.ConfirmBlocker;
import mekhq.campaign.mission.scenarios.salvage.SalvageRecoverySession.SalvageClaim;
import mekhq.campaign.mission.scenarios.salvage.SalvageSettlement;
import mekhq.campaign.mission.scenarios.salvage.WreckRecovery;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCard;
import mekhq.gui.baseComponents.hud.HudCard.Tag;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudSegmentedControl;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.baseComponents.hud.HudVerdictBanner;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * The post-scenario salvage console, where the player recovers a scenario's wrecks and decides what happens to them.
 *
 * <p>The console is styled on the contract debrief and the StratCon deployment wizard. Its header shows the recovery
 * conditions and running tallies (salvage rights, money, tech time), and a verdict banner lists anything that stops
 * the player confirming. Below, a board of wreck cards sits beside an inspector for the selected wreck: its recovery
 * units, how they recover it, what the player claims, and the recovery fleet with each unit's remaining capacity.</p>
 *
 * <p>The console holds no salvage rules of its own. It renders a {@link SalvageRecoverySession}, which applies the
 * campaign's salvage system and settlement. When the player confirms, the salvage is settled with
 * {@link CamOpsSalvageUtilities#resolveSalvage}. The console can't be cancelled.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class SalvageRecoveryConsole extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(SalvageRecoveryConsole.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CamOpsSalvage";
    private static final String KEY = "SalvageRecoveryConsole.";
    private static final Dimension DEFAULT_SIZE = scaleForGUI(1120, 760);

    private final transient Campaign campaign;
    private final boolean isInSpace;
    /** The recovery being settled, or {@code null} when there is nothing to settle and the console never opens. */
    private final transient SalvageRecoverySession session;
    private final transient Map<String, Integer> ownedVariantCounts = new HashMap<>();
    private final transient Map<String, Integer> ownedChassisCounts = new HashMap<>();

    /** The wreck shown in the dossier, or {@code null} while none is focused. */
    private transient WreckRecovery focused;
    private BoardFilter boardFilter = BoardFilter.ALL;
    private boolean isRefreshing;

    // Header
    private HudStatTile rightsTile;
    private HudStatTile firstMoneyTile;
    private HudStatTile secondMoneyTile;
    /** The tech time tile, or {@code null} under salvage systems that don't use salvage teams. */
    private HudStatTile techTimeTile;
    private final HudVerdictBanner verdictBanner = new HudVerdictBanner();

    // Board
    private final Map<BoardFilter, HudChip> chips = new HashMap<>();
    private final DefaultListModel<WreckRecovery> boardModel = new DefaultListModel<>();
    private final JList<WreckRecovery> boardList = new JList<>(boardModel);

    // Inspector
    private final JLabel dossierTitle = new JLabel();
    private final JLabel dossierSub = new JLabel();
    private final HudButton readoutButton = new HudButton(text("dossier.readout").toUpperCase(Locale.ROOT), false);
    private final HudButton stripButton = new HudButton(text("dossier.strip").toUpperCase(Locale.ROOT), false);
    private final JPanel assignmentSection = Hud.transparentPanel(null);
    // Each wreck offers different recovery units, so the searchable slots are rebuilt inside their rows. The slots are
    // null until the first wreck is shown.
    private final JPanel firstSlotRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
    private final JPanel secondSlotRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
    private SearchableComboBox<SlotChoice> firstSlot;
    private SearchableComboBox<SlotChoice> secondSlot;
    private final JPanel methodRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
    private final HudSegmentedControl<RecoveryMethod> methodControl = new HudSegmentedControl<>(this::onMethodChosen);
    private final JLabel statusLabel = new JLabel();
    private final HudSegmentedControl<SalvageClaim> claimControl = new HudSegmentedControl<>(this::onClaimChosen);
    private final JLabel claimHint = new JLabel();
    private final DefaultListModel<Unit> fleetModel = new DefaultListModel<>();
    private final JList<Unit> fleetList = new JList<>(fleetModel);
    private final JPanel dossier = Hud.transparentPanel(null);
    private final JLabel emptyDossier = Hud.notice(text("dossier.empty"));

    // Buttons
    private HudButton confirmButton;

    /** The board's filters. */
    private enum BoardFilter {
        ALL, UNASSIGNED, PROBLEMS, RECOVERED
    }

    /**
     * Opens the console for a scenario's salvage, and settles the salvage once the player confirms.
     *
     * <p>If the contract grants no salvage rights, the console isn't shown and nothing is settled.</p>
     *
     * @param campaign      the current campaign
     * @param salvageRules  the rules of the campaign's salvage system
     * @param contract      the contract the scenario belongs to
     * @param scenario      the scenario whose salvage is being recovered
     * @param actualSalvage the wrecks left on the battlefield
     * @param soldSalvage   wrecks the resolve wizard marked for sale
     */
    public SalvageRecoveryConsole(Campaign campaign, AbstractSalvage salvageRules, AbstractContract contract,
          Scenario scenario, List<TestUnit> actualSalvage, List<TestUnit> soldSalvage) {
        super((Frame) null, text("title"), true);
        this.campaign = campaign;
        this.isInSpace = scenario.getBoardType() == AtBScenario.T_SPACE;

        int availableMinutes = getAvailableTechMinutes(campaign, scenario);
        List<Integer> salvageFormations = new ArrayList<>(scenario.getSalvageFormations());
        List<Unit> recoveryUnits = getRecoveryUnits(campaign, scenario, salvageRules);
        sanitizeOtherScenarioAssignments(campaign.getActiveScenarios(), scenario, scenario.getSalvageTechs(),
              salvageFormations);

        List<TestUnit> wrecks = new ArrayList<>(actualSalvage);
        wrecks.addAll(soldSalvage);

        if (!contract.canSalvage()) {
            session = null;
            return; // There isn't going to be anything to process
        }

        if (wrecks.isEmpty()) {
            // Nothing was left on the field, so there's no point making the player confirm an empty board
            LOGGER.debug("[Salvage] No wrecks to recover after {}; skipping the salvage recovery console",
                  scenario.getName());
            session = null;
            return;
        }

        SalvageSettlement settlement = salvageRules.createSettlement(contract);
        session = new SalvageRecoverySession(salvageRules, settlement, isInSpace, wrecks, recoveryUnits,
              getRecoveryTimes(campaign, scenario, wrecks), availableMinutes, contract.getSalvagedByUnitValue(),
              contract.getSalvagedByEmployerValue(), campaign.getPlayerForce().getFinances().getBalance());

        countOwnedUnits();
        buildWindow(scenario);
        focused = session.getRecoveries().isEmpty() ? null : session.getRecoveries().getFirst();
        refreshAll();
        setVisible(true);

        CamOpsSalvageUtilities.resolveSalvage(campaign, contract, scenario, settlement, session.getKeptSalvage(),
              session.getSoldSalvage(), session.getEmployerSalvage());
    }

    /**
     * @return the tech minutes spent recovering salvage; {@code 0} if the console was never shown
     */
    public int getUsedSalvageTime() {
        return (session == null) ? 0 : session.getUsedMinutes();
    }

    /**
     * @return the number of wrecks recovered, whatever happens to them; {@code 0} if the console was never shown
     */
    public int getCountOfSalvageUnits() {
        return (session == null) ? 0 : session.getRecoveredCount();
    }

    // region Setup

    /**
     * Sums the remaining work time of the techs assigned to the scenario's salvage.
     */
    private static int getAvailableTechMinutes(Campaign campaign, Scenario scenario) {
        int minutes = 0;
        for (UUID techId : scenario.getSalvageTechs()) {
            Person tech = campaign.getPlayerForce().getHumanResources().getPerson(techId);
            if (tech == null) {
                LOGGER.error("Salvage tech {} not found in campaign", techId);
                continue;
            }
            // I don't expect we'll have negative tech minutes, but you never know
            minutes += Math.max(0, tech.getMinutesLeft());
        }
        return minutes;
    }

    /**
     * Collects the units of the scenario's salvage formations that can recover wrecks. Units that fought in the
     * scenario are left out, unless the salvage system lets their formation fight and then salvage.
     */
    private List<Unit> getRecoveryUnits(Campaign campaign, Scenario scenario, AbstractSalvage salvageRules) {
        // A set, as a formation and one nested inside it can both be assigned, and their units must only be listed once
        Set<Unit> recoveryUnits = new LinkedHashSet<>();
        LocalHangar hangar = campaign.getPlayerForce().getHangar();
        for (Integer formationId : scenario.getSalvageFormations()) {
            Formation formation = campaign.getPlayerForce().getFormation(formationId);
            if (formation == null) {
                LOGGER.error("Force {} not found in campaign", formationId);
                continue;
            }

            boolean canCombatUnitsSalvage = salvageRules.canSalvageAfterFighting(formation);
            for (Unit unit : formation.getAllUnitsAsUnits(hangar, false)) {
                boolean didFightInScenario = unit.getScenarioId() == scenario.getId();
                if ((didFightInScenario && !canCombatUnitsSalvage) ||
                          !salvageRules.isAvailableForSalvage(unit, isInSpace)) {
                    continue;
                }
                recoveryUnits.add(unit);
            }
        }
        return new ArrayList<>(recoveryUnits);
    }

    /**
     * A convoluted series of steps can leave the same formation or tech assigned to several salvage operations on the
     * same day. This removes this operation's formations and techs from every other active scenario.
     */
    private static void sanitizeOtherScenarioAssignments(List<Scenario> activeScenarios, Scenario currentScenario,
          List<UUID> salvageTechs, List<Integer> salvageFormations) {
        for (Scenario activeScenario : activeScenarios) {
            if (activeScenario == currentScenario) {
                continue;
            }
            activeScenario.removeSalvageFormation(salvageFormations);
            activeScenario.removeSalvageTechs(salvageTechs);
        }
    }

    private static Map<UUID, RecoveryTimeData> getRecoveryTimes(Campaign campaign, Scenario scenario,
          List<TestUnit> wrecks) {
        Map<UUID, RecoveryTimeData> recoveryTimes = new HashMap<>();
        for (TestUnit wreck : wrecks) {
            Entity entity = wreck.getEntity();
            if (entity == null) {
                LOGGER.error("Entity for unit {} not found in campaign", wreck.getId());
                continue;
            }
            recoveryTimes.put(wreck.getId(), RecoveryTimeCalculations.calculateRecoveryTimeForEntity(
                  entity.getDisplayName(), entity.getRecoveryTime(), entity.isAero(), scenario,
                  campaign.getPlayerForce().getForceDetachment().getCurrentLocation().getPlanet()));
        }
        return recoveryTimes;
    }

    /**
     * Counts the units the player already owns, by exact variant and by chassis, so each wreck can show how many of it
     * the player already has.
     */
    private void countOwnedUnits() {
        for (Unit ownedUnit : campaign.getPlayerForce().getHangar().getUnits()) {
            Entity ownedEntity = ownedUnit.getEntity();
            if (ownedEntity != null) {
                ownedVariantCounts.merge(variantKey(ownedEntity), 1, Integer::sum);
                ownedChassisCounts.merge(ownedEntity.getChassis(), 1, Integer::sum);
            }
        }
    }

    private static String variantKey(Entity entity) {
        return entity.getChassis() + ' ' + entity.getModel();
    }

    // endregion Setup

    // region Layout

    private void buildWindow(Scenario scenario) {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // The player can't back out of salvage
        getContentPane().setBackground(GROUND);
        setLayout(new BorderLayout());

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(leftAligned(buildCommandBar(scenario)));
        north.add(leftAligned(buildTiles()));
        JPanel bannerHolder = Hud.transparentPanel(new BorderLayout());
        bannerHolder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), 0, scaleForGUI(12)));
        bannerHolder.add(verdictBanner, BorderLayout.CENTER);
        north.add(leftAligned(bannerHolder));
        add(north, BorderLayout.NORTH);

        JSplitPane body = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildBoard(), buildInspector());
        body.setResizeWeight(0.56);
        body.setBorder(null);
        body.setOpaque(false);
        body.setBackground(GROUND);
        body.setDividerSize(scaleForGUI(6));
        add(body, BorderLayout.CENTER);

        add(buildButtons(), BorderLayout.SOUTH);

        setSize(DEFAULT_SIZE);
        setPreferredSize(DEFAULT_SIZE);
        setLocationRelativeTo(null);
        setPreferences(); // Must be before setVisible
    }

    private JComponent buildCommandBar(Scenario scenario) {
        JPanel commandBar = new JPanel();
        commandBar.setOpaque(true);
        commandBar.setBackground(GROUND);
        commandBar.setLayout(new BoxLayout(commandBar, BoxLayout.Y_AXIS));
        int pad = scaleForGUI(12);
        commandBar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(pad, scaleForGUI(14), pad, scaleForGUI(14))));

        JPanel titleRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(16), 0));
        JLabel title = new JLabel(formatted("heading", scenario.getName()).toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.25f, 0.16f));
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(buildConditionChips(), BorderLayout.EAST);
        commandBar.add(leftAligned(titleRow));
        commandBar.add(Box.createVerticalStrut(scaleForGUI(6)));
        commandBar.add(leftAligned(Hud.hint(instructions())));
        return commandBar;
    }

    private String instructions() {
        SalvageRecoverySession session = requireSession();
        String instructions = session.isUsingSalvageOperations() ?
                                    text("instructions.operations") :
                                    text("instructions.automatic");
        if (session.isUsingSalvageOperations() && session.getSalvageRules().isMultipleSalvagePerUnitAllowed()) {
            instructions += text("instructions.sharedCapacity");
        }
        if (session.getSettlement().isKeptSalvageBought()) {
            instructions += formatted("instructions.purchases", session.getSettlement().getPlayerSharePercent());
        }
        return "<html>" + instructions + "</html>";
    }

    /**
     * Shows the recovery conditions as small chips: each environmental multiplier in effect, then the total. The
     * multipliers are the scenario's, taken from its first non-aerospace wreck (aerospace wrecks in space ignore
     * them).
     */
    private JComponent buildConditionChips() {
        JPanel chipsRow = Hud.transparentPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(6), 0));
        RecoveryTimeData conditions = representativeConditions();
        if (conditions == null) {
            return chipsRow;
        }

        addConditionChip(chipsRow, "weather", conditions.weatherMultiplier());
        addConditionChip(chipsRow, "wind", conditions.windMultiplier());
        addConditionChip(chipsRow, "temperature", conditions.temperatureMultiplier());
        addConditionChip(chipsRow, "gravity", conditions.gravityMultiplier());
        addConditionChip(chipsRow, "atmosphere", conditions.atmosphereMultiplier());
        addConditionChip(chipsRow, "light", conditions.lightMultiplier());
        double total = 1.0 + conditions.weatherMultiplier() + conditions.windMultiplier() +
                             conditions.temperatureMultiplier() + conditions.gravityMultiplier() +
                             conditions.atmosphereMultiplier() + conditions.lightMultiplier();
        chipsRow.add(conditionChip(formatted("condition.total", total), ACCENT_BRIGHT, BORDER_CYAN));
        return chipsRow;
    }

    private @Nullable RecoveryTimeData representativeConditions() {
        RecoveryTimeData fallback = null;
        for (WreckRecovery recovery : requireSession().getRecoveries()) {
            RecoveryTimeData data = requireSession().getRecoveryTimeData(recovery);
            Entity entity = recovery.getWreck().getEntity();
            if (data == null) {
                continue;
            }
            if ((entity != null) && !entity.isAero()) {
                return data;
            }
            if (fallback == null) {
                fallback = data;
            }
        }
        return fallback;
    }

    private void addConditionChip(JPanel chipsRow, String conditionKey, double multiplier) {
        if (multiplier > 0.0) {
            chipsRow.add(conditionChip(formatted("condition.chip", text("condition." + conditionKey), multiplier),
                  TEXT_MUTED, BORDER));
        }
    }

    private static JLabel conditionChip(String text, Color foreground, Color border) {
        JLabel chip = new JLabel(text);
        chip.setForeground(foreground);
        chip.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));
        chip.setOpaque(true);
        chip.setBackground(SURFACE_DEEP);
        chip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(border, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(2), scaleForGUI(7), scaleForGUI(2), scaleForGUI(7))));
        return chip;
    }

    private JComponent buildTiles() {
        SalvageRecoverySession session = requireSession();
        SalvageSettlement settlement = session.getSettlement();

        rightsTile = new HudStatTile(text("tile.rights"), "");
        if (settlement.isKeptSalvageBought()) {
            firstMoneyTile = new HudStatTile(text("tile.cashShare"), text("tile.cashShare.sub"));
            secondMoneyTile = new HudStatTile(text("tile.purchaseCost"), "");
        } else {
            firstMoneyTile = new HudStatTile(text("tile.yourSalvage"), "");
            secondMoneyTile = new HudStatTile(text("tile.employer"), text("tile.employer.sub"));
        }

        List<JComponent> tiles = new ArrayList<>(List.of(rightsTile, firstMoneyTile, secondMoneyTile));
        if (session.isUsingSalvageOperations()) {
            techTimeTile = new HudStatTile(text("tile.techTime"), "");
            tiles.add(techTimeTile);
        }

        JPanel row = Hud.tileRow(tiles.toArray(new JComponent[0]));
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), 0, scaleForGUI(12)));
        holder.add(row, BorderLayout.CENTER);
        return holder;
    }

    private JComponent buildBoard() {
        JPanel board = new JPanel(new BorderLayout(0, scaleForGUI(6)));
        board.setOpaque(true);
        board.setBackground(GROUND);
        int pad = scaleForGUI(12);
        board.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, scaleForGUI(6)));

        JPanel chipRow = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(4), 0));
        chipRow.add(Hud.eyebrow(text("board.heading")));
        chipRow.add(Box.createHorizontalStrut(scaleForGUI(6)));
        for (BoardFilter filter : BoardFilter.values()) {
            boolean isRelevant = requireSession().isUsingSalvageOperations() ||
                                       (filter == BoardFilter.ALL) || (filter == BoardFilter.RECOVERED);
            if (!isRelevant) {
                continue;
            }
            HudChip chip = new HudChip("", () -> {
                boardFilter = filter;
                refreshAll();
            });
            chips.put(filter, chip);
            chipRow.add(chip);
        }
        board.add(chipRow, BorderLayout.NORTH);

        boardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        boardList.setBackground(SURFACE_DEEP);
        boardList.setCellRenderer(new WreckRenderer());
        boardList.addListSelectionListener(event -> {
            if (!isRefreshing && !event.getValueIsAdjusting() && (boardList.getSelectedValue() != null)) {
                focused = boardList.getSelectedValue();
                refreshAll();
            }
        });
        JScrollPane scroll = new JScrollPane(boardList);
        Hud.styleScroll(scroll, SURFACE_DEEP, true);
        board.add(scroll, BorderLayout.CENTER);
        return board;
    }

    private JComponent buildInspector() {
        SalvageRecoverySession session = requireSession();

        dossier.setLayout(new BoxLayout(dossier, BoxLayout.Y_AXIS));
        dossier.add(leftAligned(Hud.eyebrow(text("dossier.heading"))));
        dossier.add(Box.createVerticalStrut(scaleForGUI(4)));
        dossierTitle.setForeground(TEXT);
        dossierTitle.setFont(hudFont(Font.BOLD, 1.15f, 0.04f));
        dossier.add(leftAligned(dossierTitle));
        dossierSub.setForeground(TEXT_MUTED);
        dossierSub.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        dossier.add(leftAligned(dossierSub));
        dossier.add(Box.createVerticalStrut(scaleForGUI(8)));
        readoutButton.addActionListener(event -> showReadout());
        // Field stripping isn't implemented yet; the disabled button reserves its place in the dossier
        stripButton.setArmed(false);
        stripButton.setToolTipText(text("dossier.strip.tooltip"));
        stripButton.addActionListener(event -> {
            if (focused != null) {
                fieldStrip(focused);
            }
        });
        JPanel dossierButtons = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(8), 0));
        dossierButtons.add(readoutButton);
        dossierButtons.add(stripButton);
        dossier.add(leftAligned(wrapWest(dossierButtons)));

        if (session.isUsingSalvageOperations()) {
            assignmentSection.setLayout(new BoxLayout(assignmentSection, BoxLayout.Y_AXIS));
            assignmentSection.add(Box.createVerticalStrut(scaleForGUI(12)));
            assignmentSection.add(leftAligned(slotRow(text("slot.first"), firstSlotRow)));
            assignmentSection.add(Box.createVerticalStrut(scaleForGUI(6)));
            assignmentSection.add(leftAligned(slotRow(text("slot.second"), secondSlotRow)));

            if (session.getPlan().isRecoveryMethodChoiceOffered()) {
                JLabel methodLabel = labelFor(text("method"));
                methodRow.add(methodLabel, BorderLayout.WEST);
                methodRow.add(methodControl, BorderLayout.CENTER);
                assignmentSection.add(Box.createVerticalStrut(scaleForGUI(6)));
                assignmentSection.add(leftAligned(methodRow));
            }
            dossier.add(leftAligned(assignmentSection));
        }

        dossier.add(Box.createVerticalStrut(scaleForGUI(8)));
        statusLabel.setFont(hudFont(Font.BOLD, 0.82f, 0.08f));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(SURFACE_DEEP);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER,
              scaleForGUI(1)), BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(8), scaleForGUI(5),
              scaleForGUI(8))));
        dossier.add(leftAligned(stretch(statusLabel)));

        dossier.add(Box.createVerticalStrut(scaleForGUI(12)));
        dossier.add(leftAligned(Hud.sectionHeading(text("claim"))));
        dossier.add(Box.createVerticalStrut(scaleForGUI(6)));
        dossier.add(leftAligned(stretch(claimControl)));
        claimHint.setForeground(TEXT_FAINT);
        claimHint.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));
        dossier.add(leftAligned(claimHint));

        JPanel inspector = new JPanel(new BorderLayout(0, scaleForGUI(10)));
        inspector.setOpaque(true);
        inspector.setBackground(GROUND);
        int pad = scaleForGUI(12);
        inspector.setBorder(BorderFactory.createEmptyBorder(pad, scaleForGUI(6), pad, pad));

        JPanel top = Hud.transparentPanel(new BorderLayout());
        top.add(dossier, BorderLayout.NORTH);
        top.add(emptyDossier, BorderLayout.CENTER);
        inspector.add(top, BorderLayout.NORTH);

        if (session.isUsingSalvageOperations()) {
            inspector.add(buildFleet(), BorderLayout.CENTER);
        }
        return inspector;
    }

    private JComponent buildFleet() {
        JPanel fleet = Hud.transparentPanel(new BorderLayout(0, scaleForGUI(6)));
        fleet.add(Hud.sectionHeading(text("fleet.heading")), BorderLayout.NORTH);

        if (requireSession().getRecoveryUnits().isEmpty()) {
            fleet.add(Hud.notice(text("fleet.empty")), BorderLayout.CENTER);
            return fleet;
        }

        fleetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fleetList.setBackground(SURFACE_DEEP);
        fleetList.setCellRenderer(new FleetRenderer());
        fleetList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    assignSelectedFleetUnit();
                }
            }
        });
        fleetList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    assignSelectedFleetUnit();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(fleetList);
        Hud.styleScroll(scroll, SURFACE_DEEP, true);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        fleet.add(scroll, BorderLayout.CENTER);
        return fleet;
    }

    private JComponent slotRow(String label, JPanel row) {
        row.add(labelFor(label), BorderLayout.WEST);
        return stretch(row);
    }

    private static JLabel labelFor(String text) {
        JLabel label = Hud.eyebrow(text);
        Dimension size = new Dimension(scaleForGUI(62), label.getPreferredSize().height);
        label.setPreferredSize(size);
        label.setMinimumSize(size);
        return label;
    }

    private JComponent buildButtons() {
        JPanel buttons = new JPanel(new BorderLayout(scaleForGUI(12), 0));
        buttons.setOpaque(true);
        buttons.setBackground(GROUND);
        buttons.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), scaleForGUI(10), scaleForGUI(14))));

        JPanel actions = Hud.transparentPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(8), 0));
        if (requireSession().isUsingSalvageOperations()) {
            buttons.add(Hud.hint(text("footer")), BorderLayout.WEST);

            HudButton autoAssignButton = new HudButton(text("button.autoAssign").toUpperCase(Locale.ROOT), false);
            autoAssignButton.addActionListener(event -> {
                requireSession().autoAssign();
                refreshAll();
            });
            actions.add(autoAssignButton);

            HudButton clearButton = new HudButton(text("button.clear").toUpperCase(Locale.ROOT), false);
            clearButton.addActionListener(event -> {
                requireSession().clearAssignments();
                refreshAll();
            });
            actions.add(clearButton);
        }

        confirmButton = new HudButton(text("button.confirm").toUpperCase(Locale.ROOT), true);
        confirmButton.addActionListener(event -> confirm());
        actions.add(confirmButton);
        buttons.add(actions, BorderLayout.EAST);
        return buttons;
    }

    private static JComponent wrapWest(JComponent component) {
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.add(component, BorderLayout.WEST);
        return stretch(holder);
    }

    /**
     * Lets a component fill the width of a vertical box. The wrapper follows the component's height as its content
     * changes, where a fixed maximum size would not.
     */
    private static JComponent stretch(JComponent component) {
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.add(component, BorderLayout.CENTER);
        return holder;
    }

    // endregion Layout

    // region Refresh

    private void refreshAll() {
        isRefreshing = true;
        try {
            refreshTiles();
            refreshBanner();
            refreshBoard();
            refreshInspector();
            refreshFleet();
            confirmButton.setArmed(requireSession().canConfirm());
        } finally {
            isRefreshing = false;
        }
    }

    private void refreshTiles() {
        SalvageRecoverySession session = requireSession();
        SalvageSettlement settlement = session.getSettlement();

        if (settlement.isSalvageCapped()) {
            double percent = session.getSalvagePercent().doubleValue();
            int cap = settlement.getPlayerSharePercent();
            Color color = session.isOverSalvageCap() ? DANGER : READY;
            rightsTile.setValue(formatted("tile.rights.capped.value", percent), color);
            rightsTile.setSub(formatted("tile.rights.capped.sub", cap));
            rightsTile.setMeter(percent / 100.0, color, cap / 100.0);
        } else {
            rightsTile.setValue(formatted("tile.rights.share.value", settlement.getPlayerSharePercent()), TEXT);
            rightsTile.setSub(text("tile.rights.share.sub"));
        }

        if (settlement.isKeptSalvageBought()) {
            firstMoneyTile.setValue(session.getCashShare().toAmountString(), READY);
            Money purchaseCost = session.getPurchaseCost();
            boolean isAffordable = purchaseCost.compareTo(session.getAvailableFunds()) <= 0;
            secondMoneyTile.setValue(purchaseCost.toAmountString(), isAffordable ? TEXT : DANGER);
            secondMoneyTile.setSub(formatted("tile.purchaseCost.sub", session.getAvailableFunds().toAmountString()));
        } else if (!settlement.canKeepSalvage()) {
            firstMoneyTile.setValue(session.getExchangeUnitTotal().toAmountString(), TEXT);
            firstMoneyTile.setSub(formatted("tile.yourSalvage.exchange.sub", session.getCashShare().toAmountString()));
            secondMoneyTile.setValue(session.getEmployerSalvageTotal().toAmountString(), TEXT);
        } else {
            firstMoneyTile.setValue(session.getUnitSalvageTotal().toAmountString(), TEXT);
            firstMoneyTile.setSub(formatted("tile.yourSalvage.sub", session.getKeptValue().toAmountString(),
                  session.getSoldValue().toAmountString()));
            secondMoneyTile.setValue(session.getEmployerSalvageTotal().toAmountString(), TEXT);
        }

        if (techTimeTile != null) {
            int used = session.getUsedMinutes();
            int available = session.getAvailableMinutes();
            boolean isOver = used > available;
            Color color = isOver ? DANGER : TEXT;
            techTimeTile.setValue(formatted("tile.techTime.value", used, available), color);
            techTimeTile.setSub(isOver ?
                                      formatted("tile.techTime.over.sub", used - available) :
                                      formatted("tile.techTime.sub", available - used));
            double fill = (available == 0) ? ((used == 0) ? 0.0 : 1.0) : (double) used / available;
            techTimeTile.setMeter(fill, isOver ? DANGER : ACCENT, null);
        }
    }

    private void refreshBanner() {
        SalvageRecoverySession session = requireSession();
        List<ConfirmBlocker> blockers = session.getConfirmBlockers();
        if (blockers.isEmpty()) {
            int wreckCount = session.getRecoveries().size();
            String reason = session.isUsingSalvageOperations() ?
                                  formatted("verdict.ready.operations", session.getRecoveredCount(), wreckCount,
                                        wreckCount - session.getRecoveredCount()) :
                                  formatted("verdict.ready.automatic", wreckCount);
            verdictBanner.setVerdict(text("verdict.ready"), reason, text("badge.ready"), READY);
            return;
        }

        List<String> reasons = new ArrayList<>();
        for (ConfirmBlocker blocker : blockers) {
            reasons.add(switch (blocker) {
                case UNRECOVERABLE_ASSIGNMENTS -> formatted("blocker." + blocker.name(), session.getProblemCount());
                case OVER_SALVAGE_CAP -> formatted("blocker." + blocker.name(),
                      session.getSettlement().getPlayerSharePercent());
                case UNAFFORDABLE_PURCHASES -> formatted("blocker." + blocker.name(),
                      session.getPurchaseCost().toAmountString(), session.getAvailableFunds().toAmountString());
                case NOT_ENOUGH_TECH_TIME -> formatted("blocker." + blocker.name(), session.getUsedMinutes(),
                      session.getAvailableMinutes());
            });
        }
        verdictBanner.setVerdict(text("verdict.blocked"), String.join(text("blocker.separator"), reasons),
              text("badge.blocked"), DANGER);
    }

    private void refreshBoard() {
        SalvageRecoverySession session = requireSession();
        for (Map.Entry<BoardFilter, HudChip> entry : chips.entrySet()) {
            BoardFilter filter = entry.getKey();
            int count = switch (filter) {
                case ALL -> session.getRecoveries().size();
                case UNASSIGNED -> session.getUnassignedCount();
                case PROBLEMS -> session.getProblemCount();
                case RECOVERED -> session.getRecoveredCount();
            };
            entry.getValue().setText(formatted("chip." + filter.name().toLowerCase(Locale.ROOT), count));
            entry.getValue().setActive(filter == boardFilter);
        }

        boardModel.clear();
        for (WreckRecovery recovery : session.getRecoveries()) {
            if (isShown(recovery)) {
                boardModel.addElement(recovery);
            }
        }
        if ((focused != null) && boardModel.contains(focused)) {
            boardList.setSelectedValue(focused, true);
        } else {
            boardList.clearSelection();
        }
    }

    private boolean isShown(WreckRecovery recovery) {
        return switch (boardFilter) {
            case ALL -> true;
            case UNASSIGNED -> recovery.getStatus() == RecoveryStatus.UNASSIGNED;
            case PROBLEMS -> recovery.getStatus().isProblem();
            case RECOVERED -> recovery.isRecovered();
        };
    }

    private void refreshInspector() {
        SalvageRecoverySession session = requireSession();
        WreckRecovery recovery = focused;
        dossier.setVisible(recovery != null);
        emptyDossier.setVisible(recovery == null);
        if (recovery == null) {
            return;
        }

        TestUnit wreck = recovery.getWreck();
        dossierTitle.setText(wreck.getName());
        String sub = formatted("dossier.sub", wreckWeight(wreck), wreck.getSellValue().toAmountString());
        if (session.isUsingSalvageOperations()) {
            sub += formatted("dossier.sub.minutes", session.getRecoveryMinutes(recovery));
        }
        dossierSub.setText(sub);
        RecoveryTimeData timeData = session.getRecoveryTimeData(recovery);
        dossierSub.setToolTipText((timeData == null) ? null : timeData.getRecoveryTimeBreakdownString(true));

        if (session.isUsingSalvageOperations()) {
            firstSlot = refreshSlot(firstSlotRow, firstSlot, recovery, recovery.getFirstUnit(),
                  recovery.getSecondUnit());
            secondSlot = refreshSlot(secondSlotRow, secondSlot, recovery, recovery.getSecondUnit(),
                  recovery.getFirstUnit());
            refreshMethod(recovery);
        }

        RecoveryStatus status = recovery.getStatus();
        String statusText;
        if (status.getLabel().isEmpty()) {
            statusText = text("tag.unassigned");
        } else if (status.isProblem()) {
            statusText = formatted("status.invalid", status.getLabel());
        } else {
            statusText = status.getLabel();
        }
        statusLabel.setText(statusText.toUpperCase(Locale.ROOT));
        statusLabel.setForeground(statusColor(recovery));

        refreshClaim(recovery);
    }

    /**
     * Offers a slot the units that can take on the focused wreck, and selects its current unit.
     *
     * <p>A searchable slot can't be given new entries, so when the units on offer change, a new slot replaces the old
     * one in its row. While they stay the same, the slot is kept, so a search the player is typing isn't lost.</p>
     *
     * @param slotRow       the row holding the slot
     * @param slot          the slot now in the row, or {@code null} before the first wreck is shown
     * @param recovery      the focused wreck
     * @param current       the unit assigned to this slot, or {@code null} if it is empty
     * @param otherSlotUnit the unit assigned to the other slot, or {@code null} if it is empty
     *
     * @return the slot now in the row
     */
    private SearchableComboBox<SlotChoice> refreshSlot(JPanel slotRow, @Nullable SearchableComboBox<SlotChoice> slot,
          WreckRecovery recovery, @Nullable Unit current, @Nullable Unit otherSlotUnit) {
        List<SlotChoice> choices = new ArrayList<>();
        choices.add(SlotChoice.NONE);
        for (Unit unit : requireSession().getRecoveryUnits()) {
            if ((unit == current) || requireSession().getPlan().isOffered(recovery, unit, otherSlotUnit)) {
                choices.add(new SlotChoice(unit));
            }
        }
        SlotChoice currentChoice = new SlotChoice(current);

        boolean isOfferUnchanged = (slot != null)
              && (slot.getModel() instanceof FilteredComboBoxModel<?> model)
              && model.getAllItems().equals(choices);
        if (isOfferUnchanged) {
            slot.setSelectedItem(currentChoice);
            return slot;
        }

        SearchableComboBox<SlotChoice> newSlot = new SearchableComboBox<>("recoveryUnitSlot", choices,
              SalvageRecoveryConsole::slotChoiceText);
        Hud.styleComboBox(newSlot);
        newSlot.setRenderer(new SlotRenderer());
        Component editor = newSlot.getEditor().getEditorComponent();
        editor.setBackground(SURFACE_DEEP);
        editor.setForeground(TEXT);
        editor.setFont(newSlot.getFont());
        if (editor instanceof JTextField editorField) {
            editorField.setCaretColor(TEXT);
        }
        // Also puts the tooltip on the editor, which is where the pointer usually is
        newSlot.setToolTipText(text("slot.tooltip"));
        newSlot.setSelectedItem(currentChoice);
        newSlot.addActionListener(event -> onSlotChanged());

        if (slot != null) {
            slotRow.remove(slot);
        }
        slotRow.add(newSlot, BorderLayout.CENTER);
        slotRow.revalidate();
        slotRow.repaint();
        return newSlot;
    }

    /** The text a slot shows, and searches, for a choice: the unit's name, or "None" for the empty slot. */
    private static String slotChoiceText(SlotChoice choice) {
        Unit unit = choice.unit();
        return (unit == null) ? text("slot.none") : unit.getName();
    }

    private static @Nullable Unit selectedUnit(@Nullable SearchableComboBox<SlotChoice> slot) {
        SlotChoice choice = (slot == null) ? null : slot.getSelectedItem();
        return (choice == null) ? null : choice.unit();
    }

    private void refreshMethod(WreckRecovery recovery) {
        if (!requireSession().getPlan().isRecoveryMethodChoiceOffered()) {
            return;
        }
        // Both methods stay on offer; a method the units can't manage shows as an invalid status instead
        boolean hasRecoveryUnits = recovery.hasRecoveryUnits();
        List<Segment<RecoveryMethod>> segments = new ArrayList<>();
        for (RecoveryMethod option : RecoveryMethod.values()) {
            segments.add(new Segment<>(option, option.toString(), null, hasRecoveryUnits));
        }
        methodControl.setSegments(segments);
        methodControl.setSelected(recovery.getRecoveryMethod());
    }

    private void refreshClaim(WreckRecovery recovery) {
        SalvageRecoverySession session = requireSession();
        SalvageSettlement settlement = session.getSettlement();
        Money value = recovery.getWreck().getSellValue();
        boolean isRecovered = recovery.isRecovered();

        List<Segment<SalvageClaim>> segments = new ArrayList<>();
        for (SalvageClaim claim : session.getAvailableClaims()) {
            String title;
            String sub;
            switch (claim) {
                case EMPLOYER -> {
                    title = text("claim.EMPLOYER");
                    Money cashShare = settlement.getCashShare(value);
                    sub = cashShare.isPositive() ?
                                formatted("claim.EMPLOYER.share.sub", compactMoney(cashShare)) :
                                text("claim.EMPLOYER.sub");
                }
                case KEEP -> {
                    if (settlement.isKeptSalvageBought()) {
                        title = text("claim.BUY");
                        sub = formatted("claim.BUY.sub", compactMoney(settlement.getPurchaseCost(value)));
                    } else {
                        title = text("claim.KEEP");
                        sub = text("claim.KEEP.sub");
                    }
                }
                case SELL -> {
                    title = text("claim.SELL");
                    sub = formatted("claim.SELL.sub", compactMoney(value));
                }
                default -> throw new IllegalStateException("Unexpected claim: " + claim);
            }
            segments.add(new Segment<>(claim, title, sub, isRecovered));
        }
        claimControl.setSegments(segments);
        claimControl.setSelected(session.getClaim(recovery));
        claimHint.setText(isRecovered ? " " : text("claim.unrecovered"));
    }

    private void refreshFleet() {
        if (!requireSession().isUsingSalvageOperations()) {
            return;
        }
        Unit selected = fleetList.getSelectedValue();
        fleetModel.clear();
        for (Unit unit : requireSession().getRecoveryUnits()) {
            fleetModel.addElement(unit);
        }
        if (selected != null) {
            fleetList.setSelectedValue(selected, false);
        }
    }

    // endregion Refresh

    // region Actions

    private void onSlotChanged() {
        if (isRefreshing || (focused == null)) {
            return;
        }
        requireSession().assign(focused, selectedUnit(firstSlot), selectedUnit(secondSlot));
        logRejectedAssignment(focused);
        refreshAll();
    }

    /** Logs why a wreck's assigned units can't recover it, if they can't. */
    private static void logRejectedAssignment(WreckRecovery recovery) {
        if (recovery.hasRecoveryUnits() && recovery.getStatus().isProblem()) {
            LOGGER.debug("[Salvage] Assignment to {} rejected: {}", recovery.getWreck().getName(),
                  recovery.getStatus().name());
        }
    }

    private void onMethodChosen(RecoveryMethod method) {
        if (isRefreshing || (focused == null)) {
            return;
        }
        requireSession().setPreferredRecoveryMethod(focused, method);
        refreshAll();
    }

    private void onClaimChosen(SalvageClaim claim) {
        if (isRefreshing || (focused == null)) {
            return;
        }
        requireSession().setClaim(focused, claim);
        refreshAll();
    }

    private void assignSelectedFleetUnit() {
        Unit unit = fleetList.getSelectedValue();
        if ((unit == null) || (focused == null)) {
            return;
        }
        if (requireSession().assignToFreeSlot(focused, unit)) {
            logRejectedAssignment(focused);
        } else {
            LOGGER.debug("[Salvage] {} not assigned to {}: no free slot, or the unit isn't offered for it",
                  unit.getName(), focused.getWreck().getName());
        }
        refreshAll();
    }

    /**
     * Strips parts from a wreck in the field. Not implemented yet: the button that calls this is disabled.
     *
     * <p>This is the entry point for whoever implements field stripping, so it can be added without first working
     * out where it fits in the console. For example: open a dialog listing the wreck's parts that could be stripped,
     * with a way to pick one of the scenario's salvage techs to do it (much like the Repair tab), then refresh the
     * console.</p>
     *
     * @param recovery the wreck to strip
     */
    private void fieldStrip(WreckRecovery recovery) {
        // Not implemented yet
    }

    private void showReadout() {
        if (focused == null) {
            return;
        }
        Entity entity = focused.getWreck().getEntity();
        if (entity == null) {
            return;
        }
        new EntityReadoutDialog(null, true, entity).setVisible(true);
    }

    private void confirm() {
        if (!requireSession().canConfirm()) {
            LOGGER.debug("[Salvage] Confirm refused: {}", requireSession().getConfirmBlockers());
            return;
        }
        setVisible(false);
        Person speaker = campaign.getPlayerForce()
              .getHumanResources()
              .getSeniorTechPerson(campaign.getCampaignOptions(), campaign.getPlayerForce().isClanForce(),
                    campaign.getLocalDate());
        ImmersiveDialogSimple confirmationDialog = new ImmersiveDialogSimple(campaign, speaker, null,
              text("confirmation"), List.of(getText("Cancel.text"), getText("Confirm.text")), null, null, false);
        if (confirmationDialog.getDialogChoice() == 0) { // Cancelled
            setVisible(true);
            return;
        }
        dispose();
    }

    // endregion Actions

    // region Presentation helpers

    private SalvageRecoverySession requireSession() {
        if (session == null) {
            throw new IllegalStateException("The salvage console has no session");
        }
        return session;
    }

    private Color statusColor(WreckRecovery recovery) {
        if (recovery.isRecovered()) {
            return READY;
        }
        return recovery.getStatus().isProblem() ? DANGER : TEXT_FAINT;
    }

    private static double wreckWeight(TestUnit wreck) {
        Entity entity = wreck.getEntity();
        return (entity == null) ? 0.0 : entity.getWeight();
    }

    /** Shortens a money amount for cards and segments: 6.4M, 950K, or the plain amount below a thousand. */
    private static String compactMoney(Money money) {
        double amount = money.getAmount().doubleValue();
        if (Math.abs(amount) >= 1_000_000) {
            return formatted("money.millions", amount / 1_000_000);
        }
        if (Math.abs(amount) >= 1_000) {
            return formatted("money.thousands", amount / 1_000);
        }
        return money.toAmountString();
    }

    /** Describes what a recovery unit can haul. */
    private String capacityText(Unit unit) {
        double cargo = unit.getCargoCapacityForSalvage();
        if (!isInSpace) {
            return formatted("capacity.ground", cargo, CamOpsSalvageUtilities.getTowCapacity(unit));
        }

        StringBuilder capacity = new StringBuilder(formatted("capacity.space", cargo));
        Entity entity = unit.getEntity();
        if (entity != null) {
            if (CamOpsSalvageUtilities.hasNavalTug(entity)) {
                capacity.append(text("capacity.tug"));
            }
            int baySlots = CamOpsSalvageUtilities.getFreeFighterBaySlots(unit) +
                                 CamOpsSalvageUtilities.getFreeSmallCraftBaySlots(unit);
            if (baySlots > 0) {
                capacity.append(formatted("capacity.bays", baySlots));
            }
        }
        return capacity.toString();
    }

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, KEY + key);
    }

    private static String formatted(String key, Object... arguments) {
        return getFormattedTextAt(RESOURCE_BUNDLE, KEY + key, arguments);
    }

    /** Keeps the window's size and position in MekHQ's preferences. */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(SalvageRecoveryConsole.class);
            setName("SalvageRecoveryConsole");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception exception) {
            LOGGER.error("Failed to set user preferences", exception);
        }
    }

    // endregion Presentation helpers

    // region Renderers

    /** Renders a wreck as a board card: its status, claim, how it is recovered, and by whom. */
    private final class WreckRenderer implements ListCellRenderer<WreckRecovery> {
        private final HudCard card = new HudCard();

        @Override
        public Component getListCellRendererComponent(JList<? extends WreckRecovery> list, WreckRecovery recovery,
              int index, boolean isSelected, boolean cellHasFocus) {
            SalvageRecoverySession session = requireSession();
            TestUnit wreck = recovery.getWreck();
            RecoveryStatus status = recovery.getStatus();

            List<Tag> tags = new ArrayList<>();
            if (recovery.isRecovered()) {
                tags.add(claimTag(session.getClaim(recovery)));
                String methodKey = switch (status) {
                    case CARRIED_IN_CARGO -> "tag.cargo";
                    case CARRIED_IN_BAY -> "tag.bay";
                    case COMMITTED -> "tag.committed";
                    default -> null;
                };
                if (methodKey != null) {
                    tags.add(new Tag(text(methodKey), ACCENT));
                }
            } else if (status.isProblem()) {
                tags.add(new Tag(status.getLabel(), DANGER));
            } else {
                tags.add(new Tag(text("tag.unassigned"), TEXT_FAINT));
            }

            StringBuilder sub = new StringBuilder(formatted("card.sub", wreckWeight(wreck)));
            if (session.isUsingSalvageOperations()) {
                sub.append(formatted("card.sub.minutes", session.getRecoveryMinutes(recovery)));
                List<String> unitNames = new ArrayList<>();
                for (Unit unit : recovery.getRecoveryUnits()) {
                    unitNames.add(unit.getName());
                }
                if (!unitNames.isEmpty()) {
                    sub.append(formatted("card.sub.units", String.join(text("card.unitSeparator"), unitNames)));
                }
            }
            Entity entity = wreck.getEntity();
            if (entity != null) {
                sub.append(formatted("card.sub.owned", ownedVariantCounts.getOrDefault(variantKey(entity), 0),
                      ownedChassisCounts.getOrDefault(entity.getChassis(), 0)));
            }

            return card.show(statusColor(recovery), recovery.isRecovered(), wreck.getName(), null, tags,
                  sub.toString(), compactMoney(wreck.getSellValue()), text("money.unit"), isSelected);
        }

        private Tag claimTag(SalvageClaim claim) {
            return switch (claim) {
                case EMPLOYER -> new Tag(text("claim.EMPLOYER"), TEXT_FAINT);
                case KEEP -> new Tag(requireSession().getSettlement().isKeptSalvageBought() ?
                                           text("claim.BUY") :
                                           text("claim.KEEP"), READY);
                case SELL -> new Tag(text("claim.SELL"), AMBER);
            };
        }
    }

    /** Renders a recovery unit as a card: its capacity, and whether it is carrying salvage or committed to a wreck. */
    private final class FleetRenderer implements ListCellRenderer<Unit> {
        private final HudCard card = new HudCard();

        @Override
        public Component getListCellRendererComponent(JList<? extends Unit> list, Unit unit, int index,
              boolean isSelected, boolean cellHasFocus) {
            SalvageRecoverySession session = requireSession();
            List<Tag> tags = new ArrayList<>();
            String sub = capacityText(unit);
            String figure = null;
            Color ringColor = READY;

            WreckRecovery committedTo = null;
            boolean isUsed = false;
            for (WreckRecovery recovery : session.getRecoveries()) {
                if (recovery.getRecoveryUnits().contains(unit)) {
                    isUsed = true;
                    if (recovery.getStatus() == RecoveryStatus.COMMITTED) {
                        committedTo = recovery;
                    }
                }
            }

            RemainingCapacity remainingCapacity = session.getPlan().getRemainingCapacity(unit);
            if (committedTo != null) {
                tags.add(new Tag(text("fleet.busy"), ACCENT));
                sub = formatted("fleet.committed", committedTo.getWreck().getName());
                ringColor = ACCENT;
            } else if (remainingCapacity != null) {
                tags.add(new Tag(text("fleet.carrying"), ACCENT));
                List<String> figures = new ArrayList<>();
                if (remainingCapacity.isBayCapacity()) {
                    figures.add(formatted("freeBays",
                          remainingCapacity.freeBays()));
                }
                if (remainingCapacity.isCargoCapacity()) {
                    figures.add(formatted("freeCargo",
                          remainingCapacity.freeCargoTons()));
                }
                figure = String.join(text("capacity.separator"), figures);
                ringColor = ACCENT;
            } else if (isUsed) {
                tags.add(new Tag(text("fleet.busy"), ACCENT));
                ringColor = ACCENT;
            }

            return card.show(ringColor, !isUsed, unit.getName(), null, tags, sub, figure, null, isSelected);
        }
    }

    /**
     * An entry in a recovery unit slot. The empty slot is an entry of its own, because a searchable slot can't hold a
     * {@code null} entry.
     *
     * @param unit the recovery unit, or {@code null} for the empty slot
     */
    private record SlotChoice(@Nullable Unit unit) {
        private static final SlotChoice NONE = new SlotChoice(null);
    }

    /** Renders a recovery unit in a slot's drop-down: its name and what it can haul. */
    private final class SlotRenderer implements ListCellRenderer<SlotChoice> {
        private final ListCellRenderer<Object> delegate = Hud.comboRenderer();

        @Override
        public Component getListCellRendererComponent(JList<? extends SlotChoice> list, @Nullable SlotChoice choice,
              int index, boolean isSelected, boolean cellHasFocus) {
            Unit unit = (choice == null) ? null : choice.unit();
            String label = (unit == null) ? text("slot.none") : unit.getName() + "  ·  " + capacityText(unit);
            return delegate.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
        }
    }

    // endregion Renderers
}
