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
package mekhq.gui.commandGeneration.designer;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.commandGeneration.designer.DesignerUi.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import jakarta.annotation.Nullable;
import megamek.SuiteOptions;
import megamek.client.ratgenerator.CrewDescriptor;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView;
import megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView.Picker;
import megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView.PickerChoice;
import megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView.TransportShare;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.universe.FactionTag;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudRing;
import mekhq.gui.baseComponents.hud.HudVerdictBanner;
import mekhq.gui.commandGeneration.contents.ForceGeneratorTab;

/**
 * The Command Designer's Force page: what fights.
 *
 * <p>A rail on the left holds the settings that shape a roll - who, size, quality, shape and transport - drawn as HUD
 * controls over MegaMek's force generator, which keeps deciding what each selection offers. The board in the middle
 * lists the rolled command, formation by formation, with a ring on every unit that strikes it out of the build. The
 * readout on the right sums the command up and shows the unit selected on the board.</p>
 */
public final class ForcePage {
    private static final int RAIL_WIDTH = 300;
    private static final int READOUT_WIDTH = 400;
    // The readout's text column: its width less its padding and scroll bar.
    private static final int WRAP_WIDTH = READOUT_WIDTH - 70;
    // The verdict banner's reason shares its row with the unit-count badge.
    private static final int VERDICT_WRAP_WIDTH = WRAP_WIDTH - 140;
    // A board row's left padding, and how much further each echelon below the top one is indented.
    private static final int BOARD_ROW_PADDING = 12;
    private static final int BOARD_INDENT = 22;
    /** Weight-split colours, light to assault: the accent in four strengths. */
    private static final Color[] WEIGHT_COLORS = {
          new Color(46, 143, 154), ACCENT, ACCENT_BRIGHT, new Color(201, 243, 246) };
    private static final int[] WEIGHT_CLASSES = {
          EntityWeightClass.WEIGHT_LIGHT, EntityWeightClass.WEIGHT_MEDIUM, EntityWeightClass.WEIGHT_HEAVY,
          EntityWeightClass.WEIGHT_ASSAULT };

    private final Campaign campaign;
    private final ForceGeneratorTab engine;
    private final Runnable onSummaryChanged;
    private final ForceSummary.Cache cache = new ForceSummary.Cache();
    private ForceSummary summary = ForceSummary.EMPTY;

    private final JPanel root = new JPanel(new BorderLayout());

    // Rail
    private final Map<Picker, HudChoice<Object>> pickers = new EnumMap<>(Picker.class);
    private final Map<Picker, JComponent> pickerRows = new EnumMap<>(Picker.class);
    private final Map<TransportShare, HudStepper> transport = new EnumMap<>(TransportShare.class);
    private final JLabel yearLabel = muted("");
    private HudCheckBox fighterComplement;
    private HudCheckBox detachments;
    private HudCheckBox commandLance;
    private JLabel formationMixLink;
    private boolean railRefreshPending;

    // Board
    private final JPanel boardRows = column();
    private final JLabel boardMeta = muted("");
    private final Set<ForceDescriptor> collapsed = Collections.newSetFromMap(new IdentityHashMap<>());
    private @Nullable ForceDescriptor selected;
    private boolean boardRefreshPending;

    // Readout
    private final HudVerdictBanner banner = new HudVerdictBanner();
    private final WeightBar weightBar = new WeightBar();
    private final JLabel[] weightLegend = new JLabel[WEIGHT_CLASSES.length];
    private final JPanel totals = column();
    private final JPanel detail = column();

    /**
     * @param campaign         the campaign the command is built into
     * @param engine           the force generator, already initialized
     * @param onSummaryChanged told whenever the rolled command changes, so the tiles and Review can follow
     */
    public ForcePage(Campaign campaign, ForceGeneratorTab engine, Runnable onSummaryChanged) {
        this.campaign = campaign;
        this.engine = engine;
        this.onSummaryChanged = onSummaryChanged;

        root.setOpaque(true);
        root.setBackground(GROUND);
        root.add(buildRail(), BorderLayout.WEST);
        root.add(buildBoard(), BorderLayout.CENTER);
        root.add(buildReadout(), BorderLayout.EAST);

        ForceGeneratorOptionsView view = engine.getOptionsView();
        if (view != null) {
            view.addSelectionListener(this::scheduleRailRefresh);
        }
        engine.addModelChangeListener(this::scheduleBoardRefresh);
        refreshRail();
        refreshBoard();
    }

    public JComponent getComponent() {
        return root;
    }

    /** @return {@code true} if the roll is set to bring DropShips, so the command does not have to hire its lift */
    public boolean isLiftRolled() {
        ForceGeneratorOptionsView view = engine.getOptionsView();
        return (view != null) && (view.getTransportPercent(TransportShare.DROPSHIP) > 0);
    }

    /** @return what the rolled command adds up to */
    public ForceSummary getSummary() {
        return summary;
    }

    // region Rail

    private JComponent buildRail() {
        HudChoice<Object> faction = picker(Picker.FACTION, 0);
        HudChoice<Object> subFaction = picker(Picker.SUB_FACTION, 0);
        HudChoice<Object> formation = picker(Picker.FORMATION, 5);
        HudChoice<Object> rating = picker(Picker.RATING, 6);
        HudChoice<Object> experience = picker(Picker.EXPERIENCE, 5);
        // Experience's Random entry is a label rather than a blank value, so it is marked to stand on its own row.
        experience.setFirstIsRandom(true);
        HudChoice<Object> unitType = picker(Picker.UNIT_TYPE, 6);
        HudChoice<Object> weightClass = picker(Picker.WEIGHT_CLASS, 5);
        HudChoice<Object> infantryClass = picker(Picker.INFANTRY_CLASS, 0);
        HudChoice<Object> flags = picker(Picker.FLAGS, 0);

        pickerRows.put(Picker.FACTION, field(text("Designer.force.faction"), faction));
        pickerRows.put(Picker.SUB_FACTION, field(text("Designer.force.subFaction"), subFaction));
        pickerRows.put(Picker.FORMATION, field(text("Designer.force.formation"), formation));
        pickerRows.put(Picker.RATING, field(text("Designer.force.rating"), rating));
        pickerRows.put(Picker.EXPERIENCE, field(text("Designer.force.experience"), experience));
        pickerRows.put(Picker.UNIT_TYPE, field(text("Designer.force.unitType"), unitType));
        pickerRows.put(Picker.WEIGHT_CLASS, field(text("Designer.force.weightClass"), weightClass));
        pickerRows.put(Picker.INFANTRY_CLASS, field(text("Designer.force.infantryClass"), infantryClass));
        pickerRows.put(Picker.FLAGS, field(text("Designer.force.flags"), flags));

        commandLance = optionCheck("GenerateMercenaryCompanyCommandLance");
        commandLance.addActionListener(event -> engine.setGenerateCompanyCommandLance(commandLance.isSelected()));
        detachments = check(text("Designer.force.detachments"), text("Designer.force.detachments.tooltip"));
        detachments.addActionListener(event -> {
            ForceGeneratorOptionsView view = engine.getOptionsView();
            if (view != null) {
                view.setDetachmentsSelected(detachments.isSelected());
            }
        });
        formationMixLink = Hud.link("", () -> {
            ForceGeneratorOptionsView view = engine.getOptionsView();
            if (view != null) {
                view.openFormationMixEditor();
            }
        });
        formationMixLink.setToolTipText(text("Designer.force.formationMix.tooltip"));

        for (TransportShare share : TransportShare.values()) {
            HudStepper stepper = new HudStepper(0, (share == TransportShare.CARGO) ? 500 : 200, 10,
                  value -> formatted("Designer.percent", value), 64, value -> {
                ForceGeneratorOptionsView view = engine.getOptionsView();
                if (view != null) {
                    view.setTransportPercent(share, value);
                }
            });
            transport.put(share, stepper);
        }
        fighterComplement = check(text("Designer.force.fighterComplement"),
              text("Designer.force.fighterComplement.tooltip"));
        fighterComplement.addActionListener(event -> {
            ForceGeneratorOptionsView view = engine.getOptionsView();
            if (view != null) {
                view.setFighterComplementSelected(fighterComplement.isSelected());
            }
        });

        // One share per row, its name beside its stepper: the rail is too narrow for two steppers side by side.
        JPanel transportGrid = Hud.transparentPanel(new GridBagLayout());
        GridBagConstraints cell = new GridBagConstraints();
        cell.anchor = GridBagConstraints.WEST;
        cell.insets = new Insets(0, 0, scaleForGUI(6), 0);
        String[] shareKeys = { "Designer.force.dropShips", "Designer.force.jumpShips", "Designer.force.warShips",
                               "Designer.force.cargo" };
        TransportShare[] shares = { TransportShare.DROPSHIP, TransportShare.JUMPSHIP, TransportShare.WARSHIP,
                                    TransportShare.CARGO };
        for (int index = 0; index < shares.length; index++) {
            cell.gridy = index;
            cell.gridx = 0;
            cell.weightx = 1.0;
            cell.fill = GridBagConstraints.HORIZONTAL;
            transportGrid.add(muted(text(shareKeys[index])), cell);
            cell.gridx = 1;
            cell.weightx = 0.0;
            cell.fill = GridBagConstraints.NONE;
            transportGrid.add(transport.get(shares[index]), cell);
        }

        JPanel rail = stack(
              section(text("Designer.force.section.who"), null,
                    pickerRows.get(Picker.FACTION), pickerRows.get(Picker.SUB_FACTION), yearLabel),
              section(text("Designer.force.section.size"), null, pickerRows.get(Picker.FORMATION)),
              section(text("Designer.force.section.quality"), null,
                    pickerRows.get(Picker.RATING), pickerRows.get(Picker.EXPERIENCE)),
              section(text("Designer.force.section.shape"), null,
                    pickerRows.get(Picker.UNIT_TYPE), pickerRows.get(Picker.WEIGHT_CLASS),
                    pickerRows.get(Picker.INFANTRY_CLASS), pickerRows.get(Picker.FLAGS),
                    commandLance, detachments, formationMixLink),
              section(text("Designer.force.section.transport"), null, transportGrid, fighterComplement));
        padded(rail, 12, 14, 18, 14);
        JComponent scroll = scroll(rail, SURFACE_DEEP);
        scroll.setPreferredSize(new Dimension(scaleForGUI(RAIL_WIDTH), scaleForGUI(400)));
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, scaleForGUI(1), BORDER));
        return scroll;
    }

    private HudChoice<Object> picker(Picker picker, int maximumSegments) {
        // The rail's width less its padding and scroll bar.
        HudChoice<Object> choice = new HudChoice<>(maximumSegments, RAIL_WIDTH - 44, value -> {
            ForceGeneratorOptionsView view = engine.getOptionsView();
            if (view != null) {
                view.selectPickerValue(picker, value);
            }
        });
        pickers.put(picker, choice);
        return choice;
    }

    /** Redraws the rail once, however many selection events arrive while the view refreshes its pickers. */
    private void scheduleRailRefresh() {
        if (railRefreshPending) {
            return;
        }
        railRefreshPending = true;
        SwingUtilities.invokeLater(() -> {
            railRefreshPending = false;
            refreshRail();
        });
    }

    /** Redraws the rail from the force generator's current choices and selections. */
    public void refreshRail() {
        ForceGeneratorOptionsView view = engine.getOptionsView();
        if (view == null) {
            return;
        }
        for (Map.Entry<Picker, HudChoice<Object>> entry : pickers.entrySet()) {
            Picker picker = entry.getKey();
            List<HudChoice.Choice<Object>> choices = new ArrayList<>();
            Object selection = view.getPickerSelection(picker);
            for (PickerChoice choice : view.getPickerChoices(picker)) {
                // The aggregate and placeholder factions - All Clans, Inner Sphere, Periphery and the like - are
                // not commands anyone founds. The one already selected is kept, so the picker never shows a blank.
                if (isSpecialFaction(choice.value()) && (choice.value() != selection)) {
                    continue;
                }
                choices.add(new HudChoice.Choice<>(choice.value(), choice.label()));
            }
            entry.getValue().setChoices(choices, selection);
            boolean worthShowing = switch (picker) {
                // A list with nothing but its blank entry offers no choice.
                case SUB_FACTION, FLAGS -> choices.size() > 1;
                default -> true;
            };
            pickerRows.get(picker).setVisible(view.isPickerAvailable(picker) && worthShowing);
        }
        yearLabel.setText(formatted("Designer.force.year", view.getCurrentYear()));
        for (Map.Entry<TransportShare, HudStepper> entry : transport.entrySet()) {
            entry.getValue().setValue((int) Math.round(view.getTransportPercent(entry.getKey())));
        }
        fighterComplement.setEnabled(view.isFighterComplementAvailable());
        fighterComplement.setSelected(view.isFighterComplementSelected());
        detachments.setSelected(view.isDetachmentsSelected());
        commandLance.setSelected(engine.isGenerateCompanyCommandLance());
        String request = view.getFormationMixRequest();
        formationMixLink.setText((request == null) ? text("Designer.force.formationMix.none")
              : formatted("Designer.force.formationMix.request", request));
        root.revalidate();
        root.repaint();
    }

    /**
     * @param value a picker value
     *
     * @return {@code true} if the value is a faction tagged {@link FactionTag#SPECIAL}, such as All Clans
     */
    private static boolean isSpecialFaction(@Nullable Object value) {
        if (!(value instanceof FactionRecord record)) {
            return false;
        }
        // Factions.getFaction never answers null, so membership is checked against the key list first.
        Factions factions = Factions.getInstance();
        return factions.getFactionList().contains(record.getKey())
              && factions.getFaction(record.getKey()).is(FactionTag.SPECIAL);
    }

    // endregion Rail

    // region Board

    private JComponent buildBoard() {
        JPanel board = new JPanel(new BorderLayout());
        board.setOpaque(true);
        board.setBackground(GROUND);

        HudButton clear = new HudButton(upper(text("Designer.force.clearRolls")), false, true);
        clear.setToolTipText(text("Designer.force.clearRolls.tooltip"));
        clear.addActionListener(event -> {
            selected = null;
            engine.clearRolls();
        });
        HudButton roll = new HudButton(upper(text("Designer.force.roll")), true, true);
        roll.setToolTipText(text("Designer.force.roll.tooltip"));
        roll.addActionListener(event -> engine.requestGenerate());
        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(clear);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(roll);

        JPanel bar = new JPanel(new BorderLayout(scaleForGUI(12), 0));
        bar.setOpaque(true);
        bar.setBackground(GROUND);
        bar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(9), scaleForGUI(14), scaleForGUI(9), scaleForGUI(14))));
        bar.add(boardMeta, BorderLayout.CENTER);
        bar.add(buttons, BorderLayout.EAST);

        board.add(bar, BorderLayout.NORTH);
        board.add(scroll(boardRows, GROUND), BorderLayout.CENTER);
        return board;
    }

    private void scheduleBoardRefresh() {
        if (boardRefreshPending) {
            return;
        }
        boardRefreshPending = true;
        SwingUtilities.invokeLater(() -> {
            boardRefreshPending = false;
            refreshBoard();
        });
    }

    /** Rebuilds the board, the readout and the summary from the rolled command. */
    void refreshBoard() {
        ForceDescriptor modelRoot = engine.getGeneratedForce();
        summary = ForceSummary.of(modelRoot, campaign.getCampaignOptions(), cache);

        boardRows.removeAll();
        if (modelRoot == null) {
            selected = null;
            JLabel empty = Hud.notice(wrapHtml(text("Designer.force.board.empty"), 420));
            boardRows.add(padded(empty, 40, 20, 20, 20));
            boardMeta.setText(text("Designer.force.board.meta.empty"));
        } else {
            for (ForceDescriptor child : childrenOf(modelRoot)) {
                addNode(child, null, true, 0);
            }
            String top = (summary.getTopCommand() == null) ? "" : nameOf(summary.getTopCommand());
            boardMeta.setText("<html>" + formatted("Designer.force.board.meta", escape(top),
                  summary.getRolledUnits(), summary.getStruckUnits()) + "</html>");
        }
        boardRows.revalidate();
        boardRows.repaint();
        refreshReadout();
        onSummaryChanged.run();
    }

    private static List<ForceDescriptor> childrenOf(ForceDescriptor node) {
        List<ForceDescriptor> children = new ArrayList<>(node.getSubForces());
        children.addAll(node.getAttached());
        return children;
    }

    /**
     * Adds a node's row and, unless it is collapsed, its children's rows below it, each indented one step further than
     * its parent so the board reads as the chain of command.
     *
     * @param depth how many echelons sit above the node on the board, {@code 0} for the top formation
     */
    private void addNode(ForceDescriptor node, @Nullable ForceDescriptor formation, boolean ancestorsIncluded,
          int depth) {
        boolean included = ancestorsIncluded && node.isIncluded();
        List<ForceDescriptor> children = childrenOf(node);
        int childDepth = depth;
        if (node.getEntity() != null) {
            boardRows.add(leftAligned(unitRow(node, formation, included, depth)));
        } else if (!children.isEmpty()) {
            boardRows.add(leftAligned(formationRow(node, included, depth)));
            if (collapsed.contains(node)) {
                return;
            }
            childDepth = depth + 1;
        }
        for (ForceDescriptor child : children) {
            addNode(child, node, included, childDepth);
        }
    }

    /** @return the left padding of a board row at the given depth, before GUI scaling */
    private static int indentFor(int depth) {
        return BOARD_ROW_PADDING + (depth * BOARD_INDENT);
    }

    private String nameOf(ForceDescriptor formation) {
        String preview = engine.previewName(formation);
        return ((preview != null) && !preview.isBlank()) ? preview : formation.parseName();
    }

    private JComponent formationRow(ForceDescriptor node, boolean included, int depth) {
        Tally tally = new Tally();
        tally.add(node, true);
        boolean isCollapsed = collapsed.contains(node);

        JLabel name = new JLabel(upper(text(isCollapsed ? "Designer.symbol.collapsed" : "Designer.symbol.expanded")
              + " " + nameOf(node)));
        name.setForeground(included ? TEXT_MUTED : TEXT_FAINT);
        name.setFont(hudFont(Font.BOLD, 0.78f, 0.14f));
        JLabel stats = new JLabel(formatted("Designer.force.formation.stats", tally.units,
              formatNumber(tally.battleValue), formatNumber(Math.round(tally.tonnage))));
        stats.setForeground(TEXT_FAINT);
        stats.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));

        JPanel row = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        row.setOpaque(true);
        row.setBackground((node == selected) ? SURFACE : GROUND);
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(indentFor(depth)), scaleForGUI(5),
                    scaleForGUI(14))));
        row.add(ring(node, included, false), BorderLayout.WEST);
        row.add(name, BorderLayout.CENTER);
        row.add(stats, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, row.getPreferredSize().height));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setToolTipText(text("Designer.force.formation.tooltip"));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (!row.contains(event.getPoint())) {
                    return;
                }
                if (!collapsed.remove(node)) {
                    collapsed.add(node);
                }
                select(node);
            }
        });
        return row;
    }

    private JComponent unitRow(ForceDescriptor node, @Nullable ForceDescriptor formation, boolean included,
          int depth) {
        Entity entity = node.getEntity();
        CrewDescriptor pilot = node.getCo();
        boolean isCommander = (formation != null) && (pilot != null) && (formation.getCo() == pilot);

        JPanel nameLine = Hud.transparentPanel(null);
        nameLine.setLayout(new BoxLayout(nameLine, BoxLayout.X_AXIS));
        JLabel name = new JLabel(included ? escape(entity.getShortName())
              : "<html><s>" + escape(entity.getShortName()) + "</s></html>");
        name.setForeground(included ? TEXT : TEXT_FAINT);
        name.setFont(hudFont(Font.BOLD, 1.0f, 0.02f));
        nameLine.add(name);
        if (entity.getRole() != null) {
            nameLine.add(Box.createHorizontalStrut(scaleForGUI(8)));
            JLabel role = new JLabel(entity.getRole().toString());
            role.setForeground(TEXT_MUTED);
            role.setFont(hudFont(Font.PLAIN, 0.82f, 0.04f));
            nameLine.add(role);
        }
        if (isCommander) {
            nameLine.add(Box.createHorizontalStrut(scaleForGUI(8)));
            nameLine.add(tag(text("Designer.force.tag.commander"), ACCENT));
        }
        if (entity.getJumpMP() > 0) {
            nameLine.add(Box.createHorizontalStrut(scaleForGUI(8)));
            nameLine.add(tag(formatted("Designer.force.tag.jump", entity.getJumpMP()), TEXT_FAINT));
        }
        if (!included) {
            nameLine.add(Box.createHorizontalStrut(scaleForGUI(8)));
            nameLine.add(tag(text("Designer.force.tag.struck"), AMBER));
        }

        JLabel sub = new JLabel((pilot == null) ? text("Designer.force.noPilot") : pilotLine(pilot));
        sub.setForeground(TEXT_FAINT);
        sub.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));

        JPanel text = column();
        text.add(leftAligned(nameLine));
        text.add(Box.createVerticalStrut(scaleForGUI(2)));
        text.add(leftAligned(sub));

        JPanel figures = Hud.transparentPanel(new GridLayout(1, 2, scaleForGUI(12), 0));
        figures.add(figure(formatNumber(cache.battleValue(entity)), text("Designer.force.bv"), included));
        figures.add(figure(formatNumber(Math.round(entity.getWeight())), text("Designer.force.tons"), included));

        boolean isSelected = node == selected;
        JPanel row = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        row.setOpaque(true);
        row.setBackground(isSelected ? SURFACE : GROUND);
        // The selection bar sits at the row's indent, beside the unit it marks.
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(0, scaleForGUI(depth * BOARD_INDENT), 0, 0),
                    BorderFactory.createCompoundBorder(
                          BorderFactory.createMatteBorder(0, scaleForGUI(isSelected ? 3 : 0), 0, 0, ACCENT),
                          BorderFactory.createEmptyBorder(scaleForGUI(7),
                                scaleForGUI(BOARD_ROW_PADDING - (isSelected ? 3 : 0)), scaleForGUI(7),
                                scaleForGUI(14))))));
        row.add(ring(node, included, isCommander), BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(figures, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, row.getPreferredSize().height));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (row.contains(event.getPoint())) {
                    select(node);
                }
            }
        });
        return row;
    }

    private static String pilotLine(CrewDescriptor pilot) {
        String title = pilot.getTitle();
        String name = ((title == null) || title.isBlank()) ? pilot.getName() : title + " " + pilot.getName();
        return formatted("Designer.force.pilot", name, pilot.getGunnery(), pilot.getPiloting());
    }

    private static JComponent figure(String value, String unit, boolean included) {
        JLabel number = new JLabel(value, JLabel.RIGHT);
        number.setForeground(included ? TEXT : TEXT_FAINT);
        number.setFont(hudFont(Font.BOLD, 1.0f, 0.0f));
        JLabel caption = new JLabel(unit, JLabel.RIGHT);
        caption.setForeground(TEXT_FAINT);
        caption.setFont(hudFont(Font.PLAIN, 0.74f, 0.0f));
        JPanel figure = Hud.transparentPanel(new BorderLayout());
        figure.add(number, BorderLayout.CENTER);
        figure.add(caption, BorderLayout.SOUTH);
        figure.setPreferredSize(new Dimension(scaleForGUI(58), figure.getPreferredSize().height));
        return figure;
    }

    private static JLabel tag(String text, Color color) {
        JLabel tag = new JLabel(upper(text));
        tag.setForeground(color);
        tag.setFont(hudFont(Font.BOLD, 0.68f, 0.12f));
        tag.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(translucent(color, 140), scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(1), scaleForGUI(4), scaleForGUI(1), scaleForGUI(4))));
        return tag;
    }

    /** The status ring at the start of a row; clicking it strikes the unit or formation out, or brings it back. */
    private JComponent ring(ForceDescriptor node, boolean included, boolean isCommander) {
        HudRing ring = new HudRing();
        ring.setColor(!included ? TEXT_FAINT : (isCommander ? ACCENT : READY), included);
        ring.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ring.setToolTipText(text(node.isIncluded() ? "Designer.force.ring.strike" : "Designer.force.ring.include"));
        ring.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (ring.contains(event.getPoint())) {
                    toggleIncluded(node);
                    event.consume();
                }
            }
        });
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.add(ring, BorderLayout.NORTH);
        holder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(3), 0, 0, 0));
        return holder;
    }

    private void toggleIncluded(ForceDescriptor node) {
        node.setIncludedRecursively(!node.isIncluded());
        // The view only hears of toggles made in its own tree, so the engine is told directly; it recomputes the
        // preview names and the board is redrawn from its listener.
        engine.modelChanged();
    }

    private void select(ForceDescriptor node) {
        selected = node;
        // Selecting a formation points the formation mix at it, so its editor offers that formation's options.
        engine.setFormationMixContext((node.getEntity() == null) ? node : null);
        scheduleBoardRefresh();
    }

    // endregion Board

    // region Readout

    private JComponent buildReadout() {
        for (int index = 0; index < WEIGHT_CLASSES.length; index++) {
            weightLegend[index] = muted("");
        }
        JPanel legend = Hud.transparentPanel(new GridLayout(1, WEIGHT_CLASSES.length, scaleForGUI(4), 0));
        for (JLabel label : weightLegend) {
            legend.add(label);
        }
        JPanel readout = stack(
              banner,
              section(text("Designer.force.readout.weight"), text("Designer.force.readout.weight.aside"),
                    weightBar, legend),
              section(text("Designer.force.readout.totals"), null, totals),
              section(text("Designer.force.readout.selected"), null, detail));
        padded(readout, 12, 14, 18, 14);
        JComponent scroll = scroll(readout, SURFACE_DEEP);
        scroll.setPreferredSize(new Dimension(scaleForGUI(READOUT_WIDTH), scaleForGUI(400)));
        scroll.setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));
        return scroll;
    }

    private void refreshReadout() {
        if (!summary.hasRoll()) {
            banner.setVerdict(text("Designer.force.verdict.none"),
                  wrapHtml(text("Designer.force.verdict.none.reason"), VERDICT_WRAP_WIDTH),
                  text("Designer.force.verdict.none.badge"), TEXT_FAINT);
        } else if (summary.getIncludedUnits() == 0) {
            banner.setVerdict(text("Designer.force.verdict.allStruck"),
                  wrapHtml(text("Designer.force.verdict.allStruck.reason"), VERDICT_WRAP_WIDTH),
                  text("Designer.force.verdict.allStruck.badge"), DANGER);
        } else {
            banner.setVerdict(text("Designer.force.verdict.ready"),
                  wrapHtml(formatted("Designer.force.verdict.ready.reason", summary.getIncludedUnits(),
                        summary.getStruckUnits()), VERDICT_WRAP_WIDTH),
                  formatted("Designer.force.verdict.ready.badge", summary.getIncludedUnits()), READY);
        }

        int[] counts = new int[WEIGHT_CLASSES.length];
        for (int index = 0; index < WEIGHT_CLASSES.length; index++) {
            counts[index] = summary.getWeightCount(WEIGHT_CLASSES[index]);
            weightLegend[index].setText("<html><b>" + counts[index] + "</b><br>"
                  + escape(EntityWeightClass.getClassName(WEIGHT_CLASSES[index])) + "</html>");
        }
        weightBar.setCounts(counts);

        totals.removeAll();
        addRows(totals,
              keyValue(text("Designer.force.readout.bv"), formatNumber(summary.getBattleValue())),
              keyValue(text("Designer.force.readout.tonnage"),
                    formatted("Designer.force.readout.tonnage.value", formatNumber(Math.round(summary.getTonnage())))),
              keyValue(text("Designer.force.readout.pilots"), summary.hasPilots()
                    ? formatted("Designer.force.readout.pilots.value", summary.getAverageGunnery(),
                    summary.getAveragePiloting()) : text("Designer.symbol.none")),
              keyValue(text("Designer.force.readout.jump"),
                    formatted("Designer.force.readout.jump.value", summary.getJumpCapable(),
                          summary.getCombatUnits())),
              keyValue(text("Designer.force.readout.ships"),
                    formatted("Designer.force.readout.ships.value", summary.getDropShips(), summary.getJumpShips(),
                          summary.getWarShips())),
              keyValue(text("Designer.force.readout.price"), summary.getPurchaseValue().toAmountAndSymbolString()));

        detail.removeAll();
        if (selected == null) {
            addRows(detail, wrapped(text("Designer.force.readout.selected.none"), WRAP_WIDTH));
        } else if (selected.getEntity() != null) {
            Entity entity = selected.getEntity();
            CrewDescriptor pilot = selected.getCo();
            HudButton toggle = new HudButton(upper(text(selected.isIncluded() ? "Designer.force.strike"
                  : "Designer.force.include")), false, true);
            ForceDescriptor node = selected;
            toggle.addActionListener(event -> toggleIncluded(node));
            addRows(detail,
                  title(entity.getShortName()),
                  keyValue(text("Designer.force.readout.role"),
                        (entity.getRole() == null) ? text("Designer.symbol.none") : entity.getRole().toString()),
                  keyValue(text("Designer.force.readout.pilot"),
                        (pilot == null) ? text("Designer.symbol.none") : pilot.getName()),
                  keyValue(text("Designer.force.readout.skills"), (pilot == null) ? text("Designer.symbol.none")
                        : formatted("Designer.force.readout.pilots.value", pilot.getGunnery(), pilot.getPiloting())),
                  keyValue(text("Designer.force.readout.bv"), formatNumber(cache.battleValue(entity))),
                  keyValue(text("Designer.force.readout.price"),
                        cache.price(entity, campaign.getCampaignOptions()).toAmountAndSymbolString()),
                  toggle);
        } else {
            Tally tally = new Tally();
            tally.add(selected, true);
            HudButton toggle = new HudButton(upper(text(selected.isIncluded() ? "Designer.force.strike"
                  : "Designer.force.include")), false, true);
            ForceDescriptor node = selected;
            toggle.addActionListener(event -> toggleIncluded(node));
            addRows(detail,
                  title(nameOf(selected)),
                  keyValue(text("Designer.force.readout.units"), Integer.toString(tally.units)),
                  keyValue(text("Designer.force.readout.bv"), formatNumber(tally.battleValue)),
                  wrapped(text("Designer.force.readout.selected.formation"), WRAP_WIDTH),
                  toggle);
        }
        totals.revalidate();
        detail.revalidate();
        root.repaint();
    }

    private static JComponent title(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(hudFont(Font.BOLD, 1.05f, 0.02f));
        return label;
    }

    private static JComponent keyValue(String key, String value) {
        JPanel row = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        JLabel keyLabel = new JLabel(key);
        keyLabel.setForeground(TEXT_MUTED);
        keyLabel.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        JLabel valueLabel = new JLabel(value, JLabel.RIGHT);
        valueLabel.setForeground(TEXT);
        valueLabel.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        row.add(keyLabel, BorderLayout.CENTER);
        row.add(valueLabel, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    // endregion Readout

    static String formatNumber(long number) {
        return NumberFormat.getIntegerInstance(SuiteOptions.getNumberFormatLocale()).format(number);
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Counts what a formation holds that is still included. */
    private final class Tally {
        private int units;
        private int battleValue;
        private double tonnage;

        private void add(ForceDescriptor node, boolean ancestorsIncluded) {
            boolean included = ancestorsIncluded && node.isIncluded();
            if (included && (node.getEntity() != null)) {
                units++;
                battleValue += cache.battleValue(node.getEntity());
                tonnage += node.getEntity().getWeight();
            }
            for (ForceDescriptor child : childrenOf(node)) {
                add(child, included);
            }
        }
    }

    /** The command's weight split as one bar, light to assault. */
    private static final class WeightBar extends JComponent {
        private int[] counts = new int[WEIGHT_CLASSES.length];

        WeightBar() {
            setPreferredSize(new Dimension(scaleForGUI(200), scaleForGUI(12)));
            setMaximumSize(new Dimension(Short.MAX_VALUE, scaleForGUI(12)));
        }

        void setCounts(int[] counts) {
            this.counts = counts.clone();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            int total = 0;
            for (int count : counts) {
                total += count;
            }
            graphics.setColor(SURFACE_HIGHLIGHT);
            graphics.fillRect(0, 0, getWidth(), getHeight());
            if (total > 0) {
                int x = 0;
                for (int index = 0; index < counts.length; index++) {
                    int width = (index == counts.length - 1) ? getWidth() - x
                          : (int) Math.round(getWidth() * counts[index] / (double) total);
                    graphics.setColor(WEIGHT_COLORS[index]);
                    graphics.fillRect(x, 0, width, getHeight());
                    x += width;
                }
            }
            graphics.setColor(BORDER);
            graphics.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }
}
