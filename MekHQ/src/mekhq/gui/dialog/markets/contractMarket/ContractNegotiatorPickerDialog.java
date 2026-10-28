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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import jakarta.annotation.Nullable;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.options.IOption;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractDeterminationScale;
import mekhq.campaign.mission.contract.utilities.ActiveNegotiationMath.Odds;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudSegmentedControl;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;
import mekhq.gui.utilities.WrapLayout;

/**
 * A modal dialog for choosing the officer who will represent the player at a contract's negotiation table, drawn as a
 * heads-up display in the style of the contract market.
 *
 * <p>Candidates are the detachment's personnel trained in Negotiation; if nobody is trained, the detachment commander
 * stands in. The board on the left lists them with their target number, their negotiation traits and, when the
 * campaign uses re-negotiation, their chance of coming out ahead of the employer's negotiator. It can be searched and
 * ranked by skill, chance or name. The dossier on the right shows what the highlighted candidate changes at the table
 * (see {@link NegotiatorProfile}), their odds on each re-negotiation, and what each of their traits does.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractNegotiatorPickerDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(ContractNegotiatorPickerDialog.class);
    private static final String RESOURCE_BUNDLE = ChaosContractMarketDialog.RESOURCE_BUNDLE;

    private static final int BOARD_WIDTH = 400;
    private static final int PORTRAIT_SIZE = 40;
    private static final int DETAIL_PORTRAIT_SIZE = 72;
    private static final int ODDS_BAR_WIDTH = 56;
    private static final int ODDS_LABEL_WIDTH = 110;

    /**
     * The negotiator SPAs and Flaws whose effects the negotiation table actually reads, in the order they are shown.
     * Kept in sync with {@link NegotiatorProfile}: manual-haggle caps, the two re-negotiation options, the general
     * penalty, the extra attempt, and the Edge trigger.
     */
    static final String[] RELEVANT_TRAITS = {
          PersonnelOptions.HARD_BARGAINER, PersonnelOptions.PUSHOVER,
          PersonnelOptions.SHREWD_TRADER, PersonnelOptions.INFLEXIBLE,
          PersonnelOptions.FINE_PRINT_READER, PersonnelOptions.EASILY_FOOLED,
          PersonnelOptions.LOOPHOLE_FINDER, PersonnelOptions.BLACKLISTED,
          PersonnelOptions.RELENTLESS_BARGAINER, PersonnelOptions.ABRASIVE,
          PersonnelOptions.EDGE_COMMANDER_NEGOTIATION };

    /** How the board is ranked. */
    private enum Ranking {
        SKILL, CHANCE, NAME;

        String label() {
            return getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.sort." + name());
        }
    }

    private final transient Campaign campaign;
    private final transient AbstractContract contract;
    private final transient @Nullable Person currentSelection;
    private final transient List<Person> candidates;
    private final transient @Nullable Person employerNegotiator;
    private final int scale;
    private final boolean activeNegotiators;
    private final boolean useEdge;
    /** Every candidate's re-negotiation odds, worked out once; empty when the campaign has no re-negotiation. */
    private final transient Map<Person, NegotiatorProfile.TableOdds> odds = new HashMap<>();
    private final transient Map<Person, Icon> portraits = new HashMap<>();

    private final DefaultListModel<Person> listModel = new DefaultListModel<>();
    private final JList<Person> list = new JList<>(listModel);
    private final CardLayout boardLayout = new CardLayout();
    private final JPanel boardBody = new JPanel(boardLayout);
    private final JLabel noMatch = Hud.notice("");
    private final JPanel detailHolder = new JPanel(new BorderLayout());
    private final JTextField searchField = new JTextField();
    private HudSegmentedControl<Ranking> rankingControl;
    private Ranking ranking = Ranking.SKILL;
    private HudButton chooseButton;
    private boolean updatingList;

    private transient Person selectedNegotiator;
    private boolean confirmed;

    /**
     * Opens the modal picker.
     *
     * @param campaign         the active campaign
     * @param contract         the contract being negotiated, whose employer's negotiator the odds are worked out
     *                         against
     * @param currentSelection the currently chosen negotiator, preselected and marked if still eligible
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ContractNegotiatorPickerDialog(Campaign campaign, AbstractContract contract,
          @Nullable Person currentSelection) {
        super(campaign.getGUI().getFrame(), getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.title"), true);
        this.campaign = campaign;
        this.contract = contract;
        this.currentSelection = currentSelection;
        this.candidates = eligibleNegotiators(campaign);
        this.employerNegotiator = contract.getEmployerNegotiator();
        this.scale = Math.max(1, contract.getScale());
        this.activeNegotiators = campaign.getCampaignOptions().get(CampaignOption.USE_ACTIVE_NEGOTIATORS);
        this.useEdge = campaign.getCampaignOptions().get(CampaignOption.USE_EDGE);
        if (activeNegotiators) {
            for (Person candidate : candidates) {
                odds.put(candidate, NegotiatorProfile.odds(campaign, candidate, employerNegotiator, scale));
            }
        }

        initializeComponents();
    }

    /** @return whether the player confirmed a selection. */
    public boolean wasConfirmed() {
        return confirmed;
    }

    /** @return the chosen negotiator, or {@code null} if the player cancelled. */
    public Person getSelectedNegotiator() {
        return selectedNegotiator;
    }

    /**
     * The people eligible to negotiate for the player: the detachment's lead negotiators (if any) first, then detachment
     * personnel trained in Negotiation, each group ranked best-first, or the detachment commander if there are no such
     * people.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<Person> eligibleNegotiators(Campaign campaign) {
        Detachment detachment = campaign.getPlayerForce().getForceDetachment();

        List<Person> leadNegotiators = new ArrayList<>();
        List<Person> trained = new ArrayList<>();
        for (Person person : detachment.getPersonnel().values()) {
            if (!person.isActive()) {
                continue;
            }

            // Lead negotiators are the player's explicit picks, so they are offered even without Negotiation training
            if (person.isLeadNegotiator()) {
                leadNegotiators.add(person);
            } else if (person.hasSkill(SkillType.S_NEGOTIATION)) {
                trained.add(person);
            }
        }
        Comparator<Person> bestFirst = Comparator.comparingInt(ContractNegotiatorPickerDialog::negotiationLevel)
                                             .reversed();
        leadNegotiators.sort(bestFirst);
        trained.sort(bestFirst);

        List<Person> candidates = new ArrayList<>(leadNegotiators);
        candidates.addAll(trained);
        if (!candidates.isEmpty()) {
            return candidates;
        }

        PlayerForce playerForce = campaign.getPlayerForce();
        Person commander = playerForce.getHumanResources()
                                 .getCommander(campaign.getCampaignOptions(),
                                       playerForce.isClanForce(),
                                       campaign.getLocalDate());
        return commander == null ? List.of() : List.of(commander);
    }

    /**
     * The negotiator to put forward by default: the detachment's most skilled lead negotiator, if any are flagged and
     * able to negotiate.
     *
     * @return the best lead negotiator, or {@code null} if there is none
     */
    public static @Nullable Person leadNegotiator(Campaign campaign) {
        return eligibleNegotiators(campaign).stream().filter(Person::isLeadNegotiator).findFirst().orElse(null);
    }

    private static int negotiationLevel(Person person) {
        Skill skill = person.getSkill(SkillType.S_NEGOTIATION);
        return skill == null ? -1 : skill.getLevel();
    }

    // region Layout

    private void initializeComponents() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);

        JPanel north = Hud.transparentPanel(new BorderLayout());
        north.add(buildCommandBar(), BorderLayout.NORTH);
        if (isFallback()) {
            north.add(buildFallbackBanner(), BorderLayout.SOUTH);
        }
        content.add(north, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(true);
        body.setBackground(GROUND);
        if (candidates.isEmpty()) {
            body.add(Hud.notice(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.empty")),
                  BorderLayout.CENTER);
        } else {
            body.add(buildBoard(), BorderLayout.WEST);
            detailHolder.setOpaque(true);
            detailHolder.setBackground(GROUND);
            body.add(detailHolder, BorderLayout.CENTER);
        }
        content.add(body, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);
        installKeyBindings(content);

        // The current negotiator is highlighted if still eligible, otherwise the best-ranked candidate.
        refreshList(currentSelection != null && candidates.contains(currentSelection) ? currentSelection : null);

        setMinimumSize(scaleForGUI(900, 560));
        pack();
        setSize(scaleForGUI(1020, 660)); // Default opening size; saved preferences (below) override it on later opens
        setLocationRelativeTo(getParent());
        setPreferences(); // Must be before setVisible
        if (!candidates.isEmpty()) {
            SwingUtilities.invokeLater(searchField::requestFocusInWindow);
        }
        setVisible(true);
    }

    /** Tracks this dialog's window size and position in MekHQ's preferences so it reopens where the player left it. */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ContractNegotiatorPickerDialog.class);
            setName("ContractNegotiatorPickerDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception exception) {
            LOGGER.error("Failed to set user preferences", exception);
        }
    }

    /** The market-style command bar: the contract, the employer's negotiator across the table, and the count. */
    private JPanel buildCommandBar() {
        JPanel bar = new JPanel(new BorderLayout(scaleForGUI(20), 0));
        bar.setOpaque(true);
        bar.setBackground(GROUND);
        bar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), scaleForGUI(10), scaleForGUI(14))));

        JPanel identity = MarketHud.column();
        String scaleText = ChaosContractDeterminationScale.formatScale(contract.getExactScale(),
              campaign.getCampaignOptions().get(CampaignOption.USE_FRACTIONAL_SCALE));
        identity.add(leftAligned(Hud.eyebrow(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.eyebrow",
              contract.getEmployerMarketDisplayName(), scaleText))));
        identity.add(Box.createVerticalStrut(scaleForGUI(2)));
        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.title")
                                        .toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.12f, 0.12f));
        identity.add(leftAligned(title));

        JPanel readouts = Hud.transparentPanel(null);
        readouts.setLayout(new BoxLayout(readouts, BoxLayout.X_AXIS));
        if (employerNegotiator != null) {
            JPanel across = MarketHud.readout(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.across"),
                  getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.across.value",
                        employerNegotiator.getFullTitle(), targetText(employerNegotiator)), TEXT, SURFACE_DEEP);
            across.setToolTipText(MarketHud.wrapToolTip(getTextAt(RESOURCE_BUNDLE,
                  "picker.contractMarket.negotiator.across.tooltip")));
            readouts.add(across);
            readouts.add(Box.createHorizontalStrut(scaleForGUI(6)));
        }
        readouts.add(MarketHud.readout(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.candidates"),
              Integer.toString(candidates.size()), ACCENT_BRIGHT, SURFACE_DEEP));

        bar.add(identity, BorderLayout.CENTER);
        bar.add(readouts, BorderLayout.EAST);
        return bar;
    }

    /** Says that nobody is trained, so the commander stands in. */
    private JComponent buildFallbackBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setOpaque(true);
        banner.setBackground(SURFACE);
        banner.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), 0, scaleForGUI(14)),
              BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, scaleForGUI(3), 0, 0, AMBER),
                    BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(12), scaleForGUI(8),
                          scaleForGUI(12)))));
        JLabel text = new MarketHud.WrappingLabel();
        text.setForeground(TEXT);
        text.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        text.setText("<html><b style='color:" + hex(AMBER) + "'>"
                           + MarketHud.escape(getTextAt(RESOURCE_BUNDLE,
              "picker.contractMarket.negotiator.fallback.title")) + "</b> "
                           + MarketHud.escape(getTextAt(RESOURCE_BUNDLE,
              "picker.contractMarket.negotiator.fallback.text")) + "</html>");
        banner.add(text, BorderLayout.CENTER);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(true);
        holder.setBackground(GROUND);
        holder.add(banner, BorderLayout.CENTER);
        return holder;
    }

    /** The candidate board: a search field and the ranking above the list of candidates. */
    private JPanel buildBoard() {
        JPanel board = new JPanel(new BorderLayout());
        board.setOpaque(true);
        board.setBackground(SURFACE_DEEP);
        board.setBorder(BorderFactory.createMatteBorder(0, 0, 0, scaleForGUI(1), BORDER));
        board.setPreferredSize(new Dimension(scaleForGUI(BOARD_WIDTH), 0));

        Hud.styleField(searchField);
        searchField.putClientProperty("JTextField.placeholderText",
              getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.search"));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshList(selectedOnBoard());
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshList(selectedOnBoard());
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshList(selectedOnBoard());
            }
        });
        // Down moves from the search to the board, so the player can search and pick without the mouse.
        searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "toBoard");
        searchField.getActionMap().put("toBoard", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                list.requestFocusInWindow();
            }
        });
        searchField.addActionListener(event -> choose());

        rankingControl = new HudSegmentedControl<>(this::setRanking);
        List<Segment<Ranking>> segments = new ArrayList<>();
        for (Ranking value : Ranking.values()) {
            // Ranking by chance needs re-negotiation odds, which only exist when the campaign uses re-negotiation.
            segments.add(new Segment<>(value, value.label(), null, value != Ranking.CHANCE || activeNegotiators));
        }
        rankingControl.setSegments(segments);
        rankingControl.setSelected(ranking);
        JPanel rankingRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        rankingRow.add(keyLabel(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.sort")
                                      .toUpperCase(Locale.ROOT)), BorderLayout.WEST);
        rankingRow.add(rankingControl, BorderLayout.CENTER);

        JPanel head = MarketHud.column();
        head.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(8),
              scaleForGUI(12)));
        head.add(leftAligned(searchField));
        head.add(Box.createVerticalStrut(scaleForGUI(8)));
        head.add(leftAligned(rankingRow));
        board.add(head, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(SURFACE_DEEP);
        list.setCellRenderer(new CandidateRenderer());
        // Rows stretch to the list's width; a narrow fixed width only stops the list asking for a wider board.
        list.setFixedCellWidth(scaleForGUI(200));
        // Tags wrap to the list's width, so each row's height depends on it: re-measure when the width changes.
        list.addComponentListener(new ComponentAdapter() {
            private int measuredWidth = -1;

            @Override
            public void componentResized(ComponentEvent event) {
                if (list.getWidth() != measuredWidth) {
                    measuredWidth = list.getWidth();
                    list.setFixedCellHeight(1);
                    list.setFixedCellHeight(-1);
                }
            }
        });
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !updatingList) {
                showDetail(list.getSelectedValue());
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && list.locationToIndex(event.getPoint()) >= 0) {
                    choose();
                }
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "choose");
        list.getActionMap().put("choose", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                choose();
            }
        });
        JScrollPane listScroll = new JScrollPane(list);
        Hud.styleScroll(listScroll, SURFACE_DEEP, false);
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        noMatch.setVerticalAlignment(SwingConstants.TOP);
        noMatch.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(18), scaleForGUI(12), 0, scaleForGUI(12)));
        boardBody.setOpaque(false);
        boardBody.add(listScroll, "list");
        boardBody.add(noMatch, "none");
        board.add(boardBody, BorderLayout.CENTER);
        return board;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(14), scaleForGUI(8), scaleForGUI(14))));

        // The hint wraps onto a second line rather than being cut short beside the buttons.
        JLabel hint = new MarketHud.WrappingLabel();
        hint.setForeground(TEXT_MUTED);
        hint.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        String hintKey = isFallback() ? "picker.contractMarket.negotiator.footer.fallback"
                               : "picker.contractMarket.negotiator.intro";
        hint.setText("<html>" + MarketHud.escape(getTextAt(RESOURCE_BUNDLE, hintKey)) + "</html>");
        footer.add(hint, BorderLayout.CENTER);

        HudButton cancel = new HudButton(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.cancel")
                                               .toUpperCase(Locale.ROOT), false);
        cancel.addActionListener(event -> dispose());
        chooseButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.select")
                                           .toUpperCase(Locale.ROOT), true);
        chooseButton.setArmed(false);
        chooseButton.addActionListener(event -> choose());

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(chooseButton);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    private void installKeyBindings(JComponent root) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              "cancel");
        root.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
    }

    // endregion Layout

    // region Board

    private void setRanking(Ranking newRanking) {
        ranking = newRanking;
        rankingControl.setSelected(newRanking);
        refreshList(selectedOnBoard());
    }

    private @Nullable Person selectedOnBoard() {
        return list.getSelectedValue();
    }

    /**
     * Refills the board with the candidates matching the search, in the chosen ranking, keeping the given candidate
     * highlighted if they are still on it and otherwise highlighting the first.
     */
    private void refreshList(@Nullable Person keep) {
        if (candidates.isEmpty()) {
            return;
        }
        String query = searchField.getText().strip().toLowerCase(Locale.ROOT);
        List<Person> shown = new ArrayList<>();
        for (Person candidate : candidates) {
            if (query.isEmpty() || searchText(candidate).contains(query)) {
                shown.add(candidate);
            }
        }
        shown.sort(comparator());

        Person highlight = keep != null && shown.contains(keep) ? keep : (shown.isEmpty() ? null : shown.getFirst());
        updatingList = true;
        listModel.clear();
        shown.forEach(listModel::addElement);
        if (highlight != null) {
            list.setSelectedValue(highlight, true);
        }
        updatingList = false;

        if (highlight == null) {
            noMatch.setText(getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.noMatch",
                  searchField.getText().strip()));
            boardLayout.show(boardBody, "none");
            showDetail(null);
            return;
        }
        boardLayout.show(boardBody, "list");
        showDetail(highlight);
    }

    private Comparator<Person> comparator() {
        Comparator<Person> byName = Comparator.comparing(Person::getFullName, String.CASE_INSENSITIVE_ORDER);
        return switch (ranking) {
            case SKILL -> Comparator.comparingInt(ContractNegotiatorPickerDialog::negotiationLevel).reversed()
                                .thenComparing(byName);
            case CHANCE -> Comparator.comparingDouble((Person person) -> haggleOdds(person) == null ? 0
                                                                               : haggleOdds(person).win())
                                 .reversed().thenComparing(byName);
            case NAME -> byName;
        };
    }

    /** What the search matches: the candidate's name and title, role, and their negotiation traits' names. */
    private String searchText(Person person) {
        StringBuilder text = new StringBuilder(person.getFullTitle()).append(' ')
                                   .append(person.getPrimaryRole().toString());
        for (String trait : traits(person)) {
            text.append(' ').append(traitName(person, trait));
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }

    private @Nullable Odds haggleOdds(Person person) {
        NegotiatorProfile.TableOdds tableOdds = odds.get(person);
        return tableOdds == null ? null : tableOdds.haggle();
    }

    /** One candidate on the board: portrait, name and role with their tags, and their target number and odds. */
    private final class CandidateRenderer extends JPanel implements ListCellRenderer<Person> {
        /** Gaps and padding around the text: the row's insets, the accent bar, the portrait, and the gaps between. */
        private static final int TEXT_CHROME = 10 + 10 + 3 + PORTRAIT_SIZE + 10 + 10 + 6;
        private static final String WIDEST_TARGET = targetNumberText(12);
        private static final String WIDEST_CHANCE = percent(1.0);

        private final JLabel portrait = MarketHud.portrait(null, scaleForGUI(PORTRAIT_SIZE));
        private final JLabel text = new JLabel();
        private final JLabel target = new JLabel();
        private final JLabel targetKey = new JLabel();
        private final JLabel chance = new JLabel();
        private final JPanel chanceBar = Hud.transparentPanel(new BorderLayout());
        private final int figureWidth;

        private CandidateRenderer() {
            super(new BorderLayout(scaleForGUI(10), 0));
            text.setVerticalAlignment(SwingConstants.TOP);
            text.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));

            target.setFont(hudFont(Font.BOLD, 1.25f, 0.0f));
            target.setAlignmentX(Component.RIGHT_ALIGNMENT);
            targetKey.setFont(hudFont(Font.BOLD, 0.66f, 0.12f));
            targetKey.setForeground(TEXT_FAINT);
            targetKey.setAlignmentX(Component.RIGHT_ALIGNMENT);
            chance.setFont(hudFont(Font.BOLD, 0.8f, 0.0f));
            chance.setAlignmentX(Component.RIGHT_ALIGNMENT);
            chance.setToolTipText(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.chance.tooltip"));
            chanceBar.setAlignmentX(Component.RIGHT_ALIGNMENT);
            chanceBar.setPreferredSize(new Dimension(scaleForGUI(ODDS_BAR_WIDTH), scaleForGUI(5)));
            chanceBar.setMaximumSize(chanceBar.getPreferredSize());
            figureWidth = Math.max(Math.max(scaleForGUI(ODDS_BAR_WIDTH),
                        getFontMetrics(target.getFont()).stringWidth(WIDEST_TARGET)),
                  getFontMetrics(targetKey.getFont()).stringWidth(
                        getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.untrained")
                              .toUpperCase(Locale.ROOT)));

            // Measured with sample text: an empty label has no height, which would cut the figure off at the bottom.
            target.setText(WIDEST_TARGET);
            targetKey.setText(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.untrained")
                                    .toUpperCase(Locale.ROOT));
            chance.setText(WIDEST_CHANCE);

            JPanel figure = Hud.transparentPanel(null);
            figure.setLayout(new BoxLayout(figure, BoxLayout.Y_AXIS));
            figure.add(target);
            figure.add(targetKey);
            if (activeNegotiators) {
                figure.add(Box.createVerticalStrut(scaleForGUI(6)));
                figure.add(chanceBar);
                figure.add(Box.createVerticalStrut(scaleForGUI(2)));
                figure.add(chance);
            }
            figure.setPreferredSize(new Dimension(figureWidth, figure.getPreferredSize().height));
            JPanel figureHolder = Hud.transparentPanel(new BorderLayout());
            figureHolder.add(figure, BorderLayout.NORTH);

            add(MarketHud.topAligned(portrait), BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
            add(figureHolder, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Person> source, Person person, int index,
              boolean isSelected, boolean cellHasFocus) {
            portrait.setIcon(portraits.computeIfAbsent(person,
                  key -> MarketHud.portrait(key, scaleForGUI(PORTRAIT_SIZE)).getIcon()));

            Skill skill = person.getSkill(SkillType.S_NEGOTIATION);
            String role = person.getPrimaryRole().toString();
            String sub = skill == null ? role
                               : getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.role.skill",
                                     role, skill.getTotalSkillLevel(person.getSkillModifierData()));
            StringBuilder html = new StringBuilder("<html><b style='color:").append(hex(TEXT)).append("'>")
                                       .append(MarketHud.escape(person.getFullTitle())).append("</b><br>")
                                       .append("<span style='font-size:smaller; color:").append(hex(TEXT_FAINT))
                                       .append("'>").append(MarketHud.escape(sub)).append("</span>");
            List<String> tags = new ArrayList<>();
            if (person.equals(currentSelection)) {
                tags.add(tag(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.tag.current"), AMBER));
            }
            if (skill != null && skill.getHasNaturalAptitude()) {
                tags.add(tag(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.tag.aptitude"), ACCENT));
            }
            for (String trait : traits(person)) {
                tags.add(tag(traitName(person, trait), NegotiatorProfile.isHelpfulTrait(trait) ? READY : DANGER));
            }
            if (!tags.isEmpty()) {
                html.append("<br><span style='font-size:smaller'><b>")
                      .append(String.join(getTextAt(RESOURCE_BUNDLE, "value.contractMarket.separator"), tags))
                      .append("</b></span>");
            }
            text.setText(html.append("</html>").toString());
            fitTextToWidth(textWidth(source));

            target.setText(targetText(person));
            target.setForeground(skill == null ? TEXT_FAINT : ACCENT_BRIGHT);
            targetKey.setText(getTextAt(RESOURCE_BUNDLE, skill == null
                                                               ? "picker.contractMarket.negotiator.untrained"
                                                               : "picker.contractMarket.negotiator.needs")
                                    .toUpperCase(Locale.ROOT));
            Odds haggle = haggleOdds(person);
            if (haggle != null) {
                chanceBar.removeAll();
                chanceBar.add(oddsBar(haggle), BorderLayout.CENTER);
                chance.setText(percent(haggle.win()));
                chance.setForeground(haggle.win() >= haggle.loss() ? READY : DANGER);
            }

            setOpaque(true);
            setBackground(isSelected ? SURFACE : SURFACE_DEEP);
            setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
                  BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, scaleForGUI(isSelected ? 3 : 0), 0, 0, ACCENT),
                        BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(isSelected ? 7 : 10),
                              scaleForGUI(8), scaleForGUI(10)))));
            return this;
        }

        private static String tag(String text, Color color) {
            return "<span style='color:" + hex(color) + "'>" + MarketHud.escape(text.toUpperCase(Locale.ROOT))
                         + "</span>";
        }

        /**
         * Sizes the text to wrap at the given width. Swing's HTML view reports how tall the text is when laid out that
         * wide; a CSS width would not do, as Swing scales its {@code px} unit by about 1.3 and the text would overrun
         * the figure beside it.
         */
        private void fitTextToWidth(int width) {
            View view = (View) text.getClientProperty(BasicHTML.propertyKey);
            if (view == null) {
                text.setPreferredSize(null);
                return;
            }
            view.setSize(width, 0);
            int height = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS));
            text.setPreferredSize(new Dimension(width, height));
            text.setMinimumSize(new Dimension(0, height));
        }

        /** The width the text wraps to: the list's width less the portrait, the figure and the row's padding. */
        private int textWidth(JList<?> source) {
            int listWidth = source.getWidth() > 0 ? source.getWidth() : scaleForGUI(BOARD_WIDTH);
            return Math.max(scaleForGUI(80), listWidth - scaleForGUI(TEXT_CHROME) - figureWidth);
        }
    }

    // endregion Board

    // region Detail

    /** Rebuilds the dossier for the highlighted candidate, or empties it when nobody matches the search. */
    private void showDetail(@Nullable Person person) {
        detailHolder.removeAll();
        if (person == null) {
            chooseButton.setArmed(false);
            chooseButton.setText(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.select")
                                       .toUpperCase(Locale.ROOT));
        } else {
            detailHolder.add(MarketHud.scroll(buildDetail(person), GROUND), BorderLayout.CENTER);
            chooseButton.setArmed(true);
            chooseButton.setText(getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.choose",
                  person.getFullName()).toUpperCase(Locale.ROOT));
        }
        detailHolder.revalidate();
        detailHolder.repaint();
    }

    private JPanel buildDetail(Person person) {
        JPanel detail = MarketHud.column();
        detail.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(18), scaleForGUI(18),
              scaleForGUI(18)));
        detail.add(leftAligned(buildPersonHeader(person)));
        detail.add(Box.createVerticalStrut(scaleForGUI(16)));
        detail.add(leftAligned(buildTableSection(person)));
        NegotiatorProfile.TableOdds tableOdds = odds.get(person);
        if (tableOdds != null) {
            detail.add(Box.createVerticalStrut(scaleForGUI(16)));
            detail.add(leftAligned(buildOddsSection(tableOdds)));
        }
        detail.add(Box.createVerticalStrut(scaleForGUI(16)));
        detail.add(leftAligned(buildTraitsSection(person)));
        return detail;
    }

    /** Portrait beside the name, role, and Negotiation skill, Edge and natural aptitude. */
    private JPanel buildPersonHeader(Person person) {
        JPanel header = Hud.transparentPanel(new BorderLayout(scaleForGUI(14), 0));
        header.add(MarketHud.topAligned(MarketHud.portrait(person, scaleForGUI(DETAIL_PORTRAIT_SIZE))),
              BorderLayout.WEST);

        JPanel text = MarketHud.column();
        JLabel name = new JLabel(person.getFullTitle());
        name.setForeground(TEXT);
        name.setFont(hudFont(Font.BOLD, 1.15f, 0.0f));
        text.add(leftAligned(name));
        JLabel role = new JLabel(person.getPrimaryRole().toString());
        role.setForeground(TEXT_FAINT);
        role.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        text.add(leftAligned(role));
        text.add(Box.createVerticalStrut(scaleForGUI(8)));

        JPanel stats = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(6), scaleForGUI(4)));
        Skill skill = person.getSkill(SkillType.S_NEGOTIATION);
        if (skill == null) {
            stats.add(stat(getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.stat.untrained",
                  targetText(person))));
        } else {
            stats.add(stat(getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.stat.skill",
                  skill.getTotalSkillLevel(person.getSkillModifierData()), targetText(person))));
        }
        if (useEdge) {
            stats.add(stat(getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.stat.edge",
                  person.getCurrentEdge())));
        }
        if (skill != null && skill.getHasNaturalAptitude()) {
            JLabel aptitude = stat(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.stat.aptitude"));
            aptitude.setToolTipText(getTextAt(RESOURCE_BUNDLE,
                  "picker.contractMarket.negotiator.tag.aptitude.tooltip"));
            stats.add(aptitude);
        }
        // The stats wrap beside the portrait; the panel is offset by the layout's own gap so it lines up with the name.
        stats.setBorder(BorderFactory.createEmptyBorder(0, -scaleForGUI(6), 0, 0));
        text.add(leftAligned(stats));
        header.add(text, BorderLayout.CENTER);
        return header;
    }

    private static JLabel stat(String text) {
        JLabel stat = new JLabel(text);
        stat.setOpaque(true);
        stat.setBackground(SURFACE_DEEP);
        stat.setForeground(TEXT_MUTED);
        stat.setFont(hudFont(Font.PLAIN, 0.85f, 0.0f));
        stat.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(8), scaleForGUI(3), scaleForGUI(8))));
        return stat;
    }

    /**
     * What this candidate changes at the table, against what anyone would get: the raise cap, how much can be given
     * up, and, with re-negotiation, the attempts and the Edge reroll. A row that differs is coloured and says why.
     */
    private JPanel buildTableSection(Person person) {
        NegotiatorProfile profile = NegotiatorProfile.of(person, scale);
        NegotiatorProfile base = NegotiatorProfile.of(null, scale);
        JPanel section = MarketHud.section(getTextAt(RESOURCE_BUNDLE,
              "picker.contractMarket.negotiator.section.table"));

        String capReason = profile.raiseCap() == base.raiseCap()
                                 ? getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.row.raiseCap.scale")
                                 : getFormattedTextAt(RESOURCE_BUNDLE,
                                       "picker.contractMarket.negotiator.row.raiseCap.changed",
                                       traitName(person, profile.raiseCap() > base.raiseCap()
                                                               ? PersonnelOptions.HARD_BARGAINER
                                                               : PersonnelOptions.PUSHOVER), base.raiseCap());
        tableRow(section, "picker.contractMarket.negotiator.row.raiseCap",
              getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.row.raiseCap.value",
                    profile.raiseCap()), Integer.compare(profile.raiseCap(), base.raiseCap()), capReason);

        String giveUpReason = profile.sacrificeTerms() == base.sacrificeTerms() ? null
                                    : getFormattedTextAt(RESOURCE_BUNDLE,
                                          "picker.contractMarket.negotiator.row.giveUp.changed",
                                          traitName(person, profile.sacrificeTerms() > base.sacrificeTerms()
                                                                  ? PersonnelOptions.SHREWD_TRADER
                                                                  : PersonnelOptions.INFLEXIBLE),
                                          base.sacrificeTerms(), base.sacrificeSteps());
        tableRow(section, "picker.contractMarket.negotiator.row.giveUp",
              getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.row.giveUp.value",
                    profile.sacrificeTerms(), profile.sacrificeSteps()),
              Integer.compare(profile.sacrificeTerms(), base.sacrificeTerms()), giveUpReason);

        if (activeNegotiators) {
            tableRow(section, "picker.contractMarket.negotiator.row.attempts",
                  getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.row.attempts.value",
                        profile.attempts()), Integer.compare(profile.attempts(), base.attempts()),
                  profile.attempts() > base.attempts()
                        ? traitName(person, PersonnelOptions.RELENTLESS_BARGAINER) : null);
            if (useEdge) {
                boolean rerolls = NegotiatorProfile.playerUsesEdge(person, campaign);
                String value;
                if (!rerolls) {
                    value = getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.row.edge.no");
                } else if (person.getCurrentEdge() > 0) {
                    value = getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.row.edge.yes",
                          person.getCurrentEdge());
                } else {
                    value = getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.row.edge.spent");
                }
                tableRow(section, "picker.contractMarket.negotiator.row.edge", value,
                      rerolls && person.getCurrentEdge() > 0 ? 1 : 0,
                      rerolls ? getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.row.edge.note") : null);
            }
        }
        return section;
    }

    /** A row of the table section, its value green when better than anyone would get and red when worse. */
    private static void tableRow(JPanel section, String labelKey, String value, int comparedToBase,
          @Nullable String reason) {
        Color color = comparedToBase > 0 ? READY : (comparedToBase < 0 ? DANGER : null);
        MarketHud.addRow(section, getTextAt(RESOURCE_BUNDLE, labelKey), value, color, null, false);
        if (reason != null) {
            JTextArea note = MarketHud.wrappedText(reason, TEXT_FAINT, Font.PLAIN, 0.8f);
            note.setBorder(BorderFactory.createEmptyBorder(0, 0, scaleForGUI(4), 0));
            MarketHud.addSpanning(section, note, 2);
        }
    }

    /** The candidate's chance on each re-negotiation, as you prevail / stalemate / they prevail bars. */
    private JPanel buildOddsSection(NegotiatorProfile.TableOdds tableOdds) {
        String heading = employerNegotiator == null
                               ? getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.section.odds.generic")
                               : getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.section.odds",
                                     employerNegotiator.getFullTitle());
        JPanel section = MarketHud.section(heading);
        MarketHud.addSpanning(section, oddsRow("negotiate.contractMarket.odds.haggle", tableOdds.haggle()), 0);
        MarketHud.addSpanning(section, oddsRow("negotiate.contractMarket.odds.exception", tableOdds.exception()), 0);

        String separator = getTextAt(RESOURCE_BUNDLE, "value.contractMarket.separator");
        StringBuilder legend = new StringBuilder("<html>")
                                     .append(legendEntry(READY, "picker.contractMarket.negotiator.legend.win"))
                                     .append(separator)
                                     .append(legendEntry(TEXT_FAINT, "picker.contractMarket.negotiator.legend.even"))
                                     .append(separator)
                                     .append(legendEntry(DANGER, "picker.contractMarket.negotiator.legend.loss"));
        if (employerNegotiator != null) {
            String target = targetText(employerNegotiator);
            int edge = employerNegotiator.getCurrentEdge();
            String them = useEdge && edge > 0
                                ? getFormattedTextAt(RESOURCE_BUNDLE,
                                      "picker.contractMarket.negotiator.legend.them.edge", target, edge)
                                : getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.legend.them",
                                      target);
            legend.append(separator).append(MarketHud.escape(them));
        }
        JLabel legendLabel = new MarketHud.WrappingLabel();
        legendLabel.setForeground(TEXT_FAINT);
        legendLabel.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));
        legendLabel.setText(legend.append("</html>").toString());
        MarketHud.addSpanning(section, legendLabel, 6);
        return section;
    }

    private static String legendEntry(Color color, String key) {
        return "<span style='color:" + hex(color) + "'>&#9632;</span>&nbsp;"
                     + MarketHud.escape(getTextAt(RESOURCE_BUNDLE, key));
    }

    /** One re-negotiation: its name, a bar split into you prevail / stalemate / they prevail, and the chance. */
    private static JPanel oddsRow(String labelKey, Odds odds) {
        JPanel row = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(6), 0, scaleForGUI(6), 0)));
        JLabel name = new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey));
        name.setForeground(TEXT_MUTED);
        name.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        name.setPreferredSize(new Dimension(scaleForGUI(ODDS_LABEL_WIDTH), name.getPreferredSize().height));
        row.add(name, BorderLayout.WEST);
        JPanel barHolder = Hud.transparentPanel(new GridBagLayout());
        JComponent bar = oddsBar(odds);
        bar.setPreferredSize(new Dimension(scaleForGUI(100), scaleForGUI(10)));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        barHolder.add(bar, constraints);
        row.add(barHolder, BorderLayout.CENTER);
        JLabel value = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.odds.value",
              percent(odds.win())));
        value.setForeground(odds.win() >= odds.loss() ? READY : DANGER);
        value.setFont(hudFont(Font.BOLD, 0.9f, 0.0f));
        row.add(value, BorderLayout.EAST);
        row.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.odds.row.tooltip",
              Math.round(odds.win() * 100), Math.round(odds.stalemate() * 100), Math.round(odds.loss() * 100)));
        return row;
    }

    private static JComponent oddsBar(Odds odds) {
        return new MarketHud.SegmentBar(List.of(odds.win(), odds.stalemate(), odds.loss()),
              List.of(READY, translucent(TEXT_FAINT, 150), DANGER));
    }

    /** Each negotiation trait the candidate carries, its name over what it does at the table. */
    private JPanel buildTraitsSection(Person person) {
        JPanel section = MarketHud.section(getTextAt(RESOURCE_BUNDLE,
              "picker.contractMarket.negotiator.detail.traits"));
        List<String> traits = traits(person);
        if (traits.isEmpty()) {
            JLabel none = new JLabel(getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.detail.noTraits"));
            none.setForeground(TEXT_FAINT);
            none.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
            MarketHud.addSpanning(section, none, 2);
            return section;
        }
        for (String trait : traits) {
            JPanel row = MarketHud.column();
            row.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
                  BorderFactory.createEmptyBorder(scaleForGUI(6), 0, scaleForGUI(6), 0)));
            JLabel name = new JLabel(traitName(person, trait));
            name.setForeground(NegotiatorProfile.isHelpfulTrait(trait) ? READY : DANGER);
            name.setFont(hudFont(Font.BOLD, 0.9f, 0.0f));
            row.add(leftAligned(name));
            JTextArea effect = MarketHud.wrappedText(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.trait."
                                                                                      + trait), TEXT_FAINT,
                  Font.PLAIN, 0.84f);
            row.add(leftAligned(effect));
            IOption option = person.getOptions().getOption(trait);
            if (option != null && option.getDescription() != null) {
                String toolTip = MarketHud.wrapToolTip(option.getDescription());
                name.setToolTipText(toolTip);
                effect.setToolTipText(toolTip);
            }
            MarketHud.addSpanning(section, row, 0);
        }
        return section;
    }

    // endregion Detail

    // region Helpers

    /** Whether nobody is trained, so the board only holds the commander standing in. */
    private boolean isFallback() {
        return !candidates.isEmpty()
                     && candidates.stream().noneMatch(person -> person.hasSkill(SkillType.S_NEGOTIATION));
    }

    /** The negotiation traits this person carries that the table reads, leaving out the Edge trigger without Edge. */
    private List<String> traits(Person person) {
        List<String> traits = new ArrayList<>();
        for (String trait : RELEVANT_TRAITS) {
            if (trait.equals(PersonnelOptions.EDGE_COMMANDER_NEGOTIATION) && !useEdge) {
                continue;
            }
            if (person.getOptions().booleanOption(trait) && person.getOptions().getOption(trait) != null) {
                traits.add(trait);
            }
        }
        return traits;
    }

    /** A trait's display name, without the bracketed category the option's name carries. */
    private static String traitName(Person person, String trait) {
        IOption option = person.getOptions().getOption(trait);
        return option == null ? trait : option.getDisplayableName().replaceAll("\\s*\\([^)]*\\)", "");
    }

    /** The roll a person needs on their Negotiation check, such as "5+". */
    private String targetText(Person person) {
        Skill skill = person.getSkill(SkillType.S_NEGOTIATION);
        if (skill != null) {
            SkillModifierData modifiers = person.getSkillModifierData();
            return skill.getSkillTargetNumber(modifiers, 0);
        }
        TargetRoll target = person.checkSkill(SkillType.S_NEGOTIATION, campaign).getTargetNumber();
        return target.cannotSucceed() ? getTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.impossible")
                     : targetNumberText(target.getValue());
    }

    /** A target number as the roll it needs, such as "7+". */
    private static String targetNumberText(int target) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "picker.contractMarket.negotiator.target", target);
    }

    private static String percent(double chance) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.percent", Math.round(chance * 100));
    }

    private void choose() {
        Person person = list.getSelectedValue();
        if (person == null) {
            return;
        }
        selectedNegotiator = person;
        confirmed = true;
        dispose();
    }

    // endregion Helpers
}
