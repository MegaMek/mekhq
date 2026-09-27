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
package mekhq.gui.roleplay;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.common.event.Subscribe;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.NewDayEvent;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.roleplay.FateChart;
import mekhq.campaign.roleplay.JournalEntry;
import mekhq.campaign.roleplay.OracleActions;
import mekhq.campaign.roleplay.OracleAnswer;
import mekhq.campaign.roleplay.OracleCharacter;
import mekhq.campaign.roleplay.PlotThread;
import mekhq.campaign.roleplay.Roleplay;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.baseComponents.hud.HudModeSelector;
import mekhq.gui.baseComponents.hud.HudStatTile;

/**
 * The Oracle Console: one modeless window for solo roleplay, drawn as a heads-up display in the style of the
 * interstellar map and the Issue Kit dialog. A command bar holds live Chaos, Threads and Journal tiles and the page
 * selector; the pages are {@link AskPage Ask}, {@link ThreadsPage Threads}, {@link CastPage Cast} and
 * {@link JournalPage Journal}. Each page has a guide that slides over it, opened by the guide button or by itself the
 * first time the page is visited.
 *
 * <p>The console stays open alongside the campaign. It refreshes whenever it changes something itself, when a new day
 * starts, and when someone in the personnel changes (so linked cast members keep their names).</p>
 */
public class OracleConsole extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(OracleConsole.class);
    static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    private static OracleConsole openConsole;

    private final Campaign campaign;
    private final OracleActions actions;

    private final Map<ConsolePage, ConsoleSection> pages = new EnumMap<>(ConsolePage.class);
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageHolder = new JPanel(pageLayout);
    private final HudModeSelector<ConsolePage> modeSelector;
    private ConsolePage currentPage = ConsolePage.ASK;

    // Pages are built the first time they are shown, so opening the console only builds the Ask page.
    private AskPage askPage;
    private ChecksPage checksPage;
    private ThreadsPage threadsPage;
    private CastPage castPage;
    private JournalPage journalPage;

    private HudStatTile chaosTile;
    private HudButton chaosDown;
    private HudButton chaosUp;
    private HudStatTile threadsTile;
    private HudStatTile journalTile;
    private HudButton guideButton;
    private JLabel footerSummary;
    private final JLabel subtitle = new JLabel();

    private final HudGuideDrawer guideDrawer;
    private final JPanel scrim;
    private final HudCheckBox autoOpenGuides;

    /**
     * Opens the console for a campaign, or brings the open console to the front.
     *
     * @param frame    the main window
     * @param campaign the campaign
     */
    public static void showFor(final JFrame frame, final Campaign campaign) {
        if (openConsole != null && openConsole.isDisplayable() && openConsole.campaign == campaign) {
            openConsole.toFront();
            openConsole.requestFocus();
            return;
        }
        if (openConsole != null) {
            openConsole.dispose();
        }
        openConsole = new OracleConsole(frame, campaign);
        openConsole.setVisible(true);
        openConsole.onShown();
    }

    /**
     * Opens the console on the Checks page, set up to check the given people.
     *
     * @param frame     the MekHQ main window
     * @param campaign  the campaign
     * @param people    the people to tick
     * @param attribute {@code true} for an attribute check, {@code false} for a skill check
     */
    public static void showChecks(final JFrame frame, final Campaign campaign, final List<Person> people,
          final boolean attribute) {
        showFor(frame, campaign);
        openConsole.showChecksFor(people, attribute);
    }

    private OracleConsole(final JFrame frame, final Campaign campaign) {
        super(frame, getTextAt(RESOURCE_BUNDLE, "OracleConsole.windowTitle"), false);
        this.campaign = campaign;
        this.actions = OracleActions.forCampaign(campaign);
        refreshLinkedNames();

        modeSelector = new HudModeSelector<>(ConsolePage.class, ConsolePage::getLabel, this::showPage);
        modeSelector.setSelected(ConsolePage.ASK);

        autoOpenGuides = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "OracleConsole.guide.autoOpen"));
        autoOpenGuides.setSelected(OracleGuidePreferences.getInstance().isGuideAutoOpen());
        autoOpenGuides.addActionListener(event ->
                                               OracleGuidePreferences.getInstance().setGuideAutoOpen(autoOpenGuides.isSelected()));
        guideDrawer = new HudGuideDrawer(List.of(getTextAt(RESOURCE_BUNDLE, "OracleConsole.guide.tab.steps"),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.guide.tab.example"),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.guide.tab.terms")), autoOpenGuides,
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.guide.close"), this::closeGuide);
        scrim = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                graphics.setColor(new Color(2, 7, 13, 160));
                graphics.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        scrim.setOpaque(false);
        scrim.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                closeGuide();
            }
        });

        buildUI();
        refresh();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(scaleForGUI(980, 680));
        getContentPane().setPreferredSize(scaleForGUI(1120, 760));
        pack();
        setLocationRelativeTo(frame);
        setPreferences();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                MekHQ.unregisterHandler(OracleConsole.this);
                if (openConsole == OracleConsole.this) {
                    openConsole = null;
                }
            }
        });
        MekHQ.registerHandler(this);
    }

    private void onShown() {
        maybeOpenGuide(ConsolePage.ASK);
    }

    // region Layout

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);

        pageHolder.setOpaque(true);
        pageHolder.setBackground(GROUND);
        pageHolder.setPreferredSize(scaleForGUI(1120, 560));
        page(ConsolePage.ASK);

        content.add(buildCommandBar(), BorderLayout.NORTH);
        content.add(pageHolder, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);

        // The guide drawer and its scrim float over the pages on the layered pane.
        getLayeredPane().add(scrim, JLayeredPane.MODAL_LAYER);
        getLayeredPane().add(guideDrawer, Integer.valueOf(JLayeredPane.MODAL_LAYER + 1));
        scrim.setVisible(false);
        guideDrawer.setVisible(false);
        getLayeredPane().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                layoutGuide();
            }
        });

        installKeyBindings(content);
    }

    private JPanel buildCommandBar() {
        JPanel commandBar = new JPanel(new BorderLayout(0, scaleForGUI(12)));
        commandBar.setOpaque(true);
        commandBar.setBackground(GROUND);
        commandBar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(12), scaleForGUI(16))));

        JPanel identity = new JPanel();
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.setOpaque(false);
        JLabel eyebrow = new JLabel(getTextAt(RESOURCE_BUNDLE, "OracleConsole.eyebrow").toUpperCase(Locale.ROOT));
        eyebrow.setForeground(TEXT_FAINT);
        eyebrow.setFont(hudFont(Font.BOLD, 0.72f, 0.18f));
        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, "OracleConsole.title").toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.35f, 0.16f));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        identity.add(leftAligned(eyebrow));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));
        identity.add(leftAligned(title));
        identity.add(Box.createVerticalStrut(scaleForGUI(4)));
        identity.add(leftAligned(subtitle));

        chaosTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "OracleConsole.tile.chaos"),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.tile.chaos.sub"));
        chaosTile.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.tile.chaos.toolTipText"));
        chaosDown = new HudButton("−", false, true);
        chaosDown.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.tile.chaos.down"));
        chaosDown.addActionListener(event -> changeChaos(-1));
        chaosUp = new HudButton("+", false, true);
        chaosUp.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.tile.chaos.up"));
        chaosUp.addActionListener(event -> changeChaos(1));
        JPanel stepper = Hud.transparentPanel(null);
        stepper.setLayout(new BoxLayout(stepper, BoxLayout.X_AXIS));
        stepper.add(chaosDown);
        stepper.add(Box.createHorizontalStrut(scaleForGUI(4)));
        stepper.add(chaosUp);
        chaosTile.setValueAccessory(stepper);
        JLabel[] sceneLink = new JLabel[1];
        sceneLink[0] = Hud.link(getTextAt(RESOURCE_BUNDLE, "OracleConsole.scene.link"),
              () -> showScenePrompt(sceneLink[0]));
        sceneLink[0].setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.scene.link.toolTipText"));
        chaosTile.addFooter(sceneLink[0]);

        threadsTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "OracleConsole.tile.threads"), "");
        journalTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "OracleConsole.tile.journal"), "");
        JPanel tiles = Hud.tileRow(chaosTile, threadsTile, journalTile);

        JPanel topRow = new JPanel(new BorderLayout(scaleForGUI(24), 0));
        topRow.setOpaque(false);
        topRow.add(identity, BorderLayout.CENTER);
        topRow.add(tiles, BorderLayout.EAST);

        guideButton = new HudButton("", false, true);
        guideButton.addActionListener(event -> openGuide(currentPage));
        JPanel modesRow = new JPanel(new BorderLayout(scaleForGUI(8), 0));
        modesRow.setOpaque(false);
        modesRow.add(modeSelector, BorderLayout.CENTER);
        modesRow.add(guideButton, BorderLayout.EAST);

        commandBar.add(topRow, BorderLayout.CENTER);
        commandBar.add(modesRow, BorderLayout.SOUTH);
        return commandBar;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(18), scaleForGUI(10), scaleForGUI(18))));

        footerSummary = new JLabel();
        footerSummary.setForeground(TEXT_MUTED);
        footerSummary.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        footer.add(footerSummary, BorderLayout.CENTER);

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        HudButton note = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.footer.note").toUpperCase(Locale.ROOT),
              false);
        note.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.footer.note.toolTipText"));
        note.addActionListener(event -> newNoteFromContext());
        HudButton close = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.footer.close")
                                              .toUpperCase(Locale.ROOT), false);
        close.addActionListener(event -> dispose());
        buttons.add(note);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(10)));
        buttons.add(close);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    private void installKeyBindings(JComponent root) {
        // Number keys pick the Fate Chart odds on the Ask page, unless the player is typing.
        for (int number = 1; number <= 9; number++) {
            final int index = number - 1;
            String name = "odds" + number;
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke((char) ('0' + number)),
                  name);
            root.getActionMap().put(name, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (currentPage == ConsolePage.ASK && !guideDrawer.isVisible() && !isTyping()) {
                        askPage.selectOdds(index);
                    }
                }
            });
        }
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              "closeGuide");
        root.getActionMap().put("closeGuide", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                closeGuide();
            }
        });
    }

    private static boolean isTyping() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextComponent;
    }

    // endregion Layout

    // region Pages and navigation

    /**
     * Returns a page, building it and adding it to the console the first time it is needed.
     *
     * @param page the page
     *
     * @return the page's section
     */
    private ConsoleSection page(final ConsolePage page) {
        ConsoleSection section = pages.get(page);
        if (section == null) {
            section = switch (page) {
                case ASK -> askPage = new AskPage(this);
                case CHECKS -> checksPage = new ChecksPage(this);
                case THREADS -> threadsPage = new ThreadsPage(this);
                case CAST -> castPage = new CastPage(this);
                case JOURNAL -> journalPage = new JournalPage(this);
            };
            pages.put(page, section);
            pageHolder.add(section.getComponent(), page.name());
        }
        return section;
    }

    /**
     * Switches to a page, opening its guide if the player has not seen it yet.
     *
     * @param page the page
     */
    void showPage(final ConsolePage page) {
        closeGuide();
        currentPage = page;
        modeSelector.setSelected(page);
        ConsoleSection section = page(page);
        // Only the page on screen is kept up to date, so bring this one up to date as it is shown.
        section.refresh();
        pageLayout.show(pageHolder, page.name());
        guideButton.setText("?  " + page.getGuideLabel().toUpperCase(Locale.ROOT));
        maybeOpenGuide(page);
    }

    /**
     * Opens the Threads page on a thread.
     *
     * @param threadId the thread's id
     */
    void showThread(final UUID threadId) {
        showPage(ConsolePage.THREADS);
        threadsPage.select(threadId);
    }

    /**
     * Opens the Cast page on a character.
     *
     * @param characterId the character's id
     */
    void showCharacter(final UUID characterId) {
        showPage(ConsolePage.CAST);
        castPage.select(characterId);
    }

    /**
     * Shows the Checks page, set up to check the given people.
     *
     * @param people    the people to tick
     * @param attribute {@code true} for an attribute check, {@code false} for a skill check
     */
    void showChecksFor(final List<Person> people, final boolean attribute) {
        page(ConsolePage.CHECKS);
        checksPage.preset(people, attribute);
        showPage(ConsolePage.CHECKS);
    }

    /**
     * Shows the Checks page, set up for an opposed check with a cast member defending.
     *
     * @param character the cast member
     */
    void showOpposedCheck(final OracleCharacter character) {
        page(ConsolePage.CHECKS);
        showPage(ConsolePage.CHECKS);
        checksPage.presetOpposed(character);
    }

    /**
     * Opens the Journal filtered to one storyline.
     *
     * @param threadId    a thread to filter by, or {@code null}
     * @param characterId a character to filter by, or {@code null}
     */
    void readStoryline(final @Nullable UUID threadId, final @Nullable UUID characterId) {
        showPage(ConsolePage.JOURNAL);
        journalPage.filterToStoryline(threadId, characterId);
    }

    /**
     * Starts a journal note tagged with whatever is on screen: the selected thread or character, or the last answer.
     */
    private void newNoteFromContext() {
        UUID thread = null;
        UUID character = null;
        OracleAnswer quote = null;
        JournalEntry lastAnswer = latestAnswer();
        switch (currentPage) {
            case THREADS -> thread = (threadsPage == null) ? null : threadsPage.getSelectedThreadId();
            case CAST -> character = (castPage == null) ? null : castPage.getSelectedCharacterId();
            case ASK, JOURNAL -> {
                if (lastAnswer != null && currentPage == ConsolePage.ASK) {
                    quote = lastAnswer.getAnswer();
                    thread = lastAnswer.getThreads().stream().findFirst().orElse(null);
                    character = lastAnswer.getCharacters().stream().findFirst().orElse(null);
                }
            }
        }
        showPage(ConsolePage.JOURNAL);
        journalPage.newNote(thread, character, quote);
    }

    // endregion Pages and navigation

    // region Guides

    private void maybeOpenGuide(final ConsolePage page) {
        if (!isShowing()) {
            return;
        }
        boolean seen = OracleGuidePreferences.getInstance().isGuideSeen(page);
        // The Ask page has its own welcome banner, so its guide only opens from the banner or the button.
        if (!seen && page != ConsolePage.ASK && OracleGuidePreferences.getInstance().isGuideAutoOpen()) {
            openGuide(page);
        }
    }

    /**
     * Opens a page's guide over the console.
     *
     * @param page the page
     */
    void openGuide(final ConsolePage page) {
        OracleGuidePreferences.getInstance().setGuideSeen(page, true);
        guideDrawer.showGuide(ConsoleGuides.build(page, this));
        scrim.setVisible(true);
        guideDrawer.setVisible(true);
        layoutGuide();
        guideDrawer.focusClose();
    }

    void closeGuide() {
        scrim.setVisible(false);
        guideDrawer.setVisible(false);
    }

    private void layoutGuide() {
        int width = getLayeredPane().getWidth();
        int height = getLayeredPane().getHeight();
        scrim.setBounds(0, 0, width, height);
        int drawerWidth = Math.min(HudGuideDrawer.drawerWidth(), width);
        guideDrawer.setBounds(width - drawerWidth, 0, drawerWidth, height);
        guideDrawer.revalidate();
    }

    // endregion Guides

    // region Chaos

    private void changeChaos(final int delta) {
        roleplay().setChaosFactor(roleplay().getChaosFactor() + delta);
        changed();
    }

    private void showScenePrompt(final JComponent anchor) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(BORDER_CYAN, scaleForGUI(1)));
        popup.setBackground(SURFACE);
        popup.add(ScenePrompt.build(roleplay().getChaosFactor(), more -> {
            popup.setVisible(false);
            changeChaos(more ? 1 : -1);
        }));
        popup.show(anchor, 0, anchor.getHeight() + scaleForGUI(4));
    }

    // endregion Chaos

    // region Refresh

    /**
     * Refreshes the tiles, footer and the page on screen after a change.
     */
    void changed() {
        refresh();
    }

    private void refresh() {
        Roleplay roleplay = roleplay();
        subtitle.setText(describeSubtitle());

        int chaos = roleplay.getChaosFactor();
        chaosTile.setValue(Integer.toString(chaos), AMBER);
        chaosTile.setMeter(chaos / (double) FateChart.MAXIMUM_CHAOS_FACTOR, AMBER, null);
        chaosDown.setArmed(chaos > FateChart.MINIMUM_CHAOS_FACTOR);
        chaosUp.setArmed(chaos < FateChart.MAXIMUM_CHAOS_FACTOR);

        List<PlotThread> open = roleplay.getPlotThreads().stream().filter(thread -> !thread.isComplete()).toList();
        long atFlashpoint = open.stream().filter(thread -> thread.getNextStep() != null
                                                                 && thread.getNextStep().majorRevelation()).count();
        threadsTile.setValue(Integer.toString(open.size()), ACCENT_BRIGHT);
        threadsTile.setSub(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.tile.threads.sub", atFlashpoint));

        int entries = roleplay.getJournal().size() + roleplay.getOracleLog().size();
        LocalDate today = campaign.getLocalDate();
        long todayCount = roleplay.getJournal().stream().filter(entry -> entry.getDate().equals(today)).count()
                                + roleplay.getOracleLog().stream().filter(entry -> entry.getDate().equals(today))
                                        .count();
        journalTile.setValue(Integer.toString(entries), TEXT);
        journalTile.setSub(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.tile.journal.sub", todayCount));

        refreshFooter();
        guideButton.setText("?  " + currentPage.getGuideLabel().toUpperCase(Locale.ROOT));
        // Other pages refresh when they are next shown.
        page(currentPage).refresh();
    }

    private String describeSubtitle() {
        String date = formatDate(campaign.getLocalDate());
        String force = campaign.getPlayerForce().getName();
        PlanetarySystem system = campaign.getCurrentSystem();
        return (system == null) ? getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.subtitle.noSystem", date, force)
                     : getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.subtitle", date, force,
                           system.getName(campaign.getLocalDate()));
    }

    private void refreshFooter() {
        JournalEntry latest = latestAnswer();
        if (latest == null) {
            footerSummary.setText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.footer.none"));
            return;
        }
        OracleAnswer answer = latest.getAnswer();
        String question = answer.question().isBlank() ? answer.odds().getLabel() : "“" + answer.question()
                                                                                       + "”";
        footerSummary.setText("<html>" + getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.footer.last",
              escape(question), hex(AskPage.colorFor(answer.answer())), escape(answer.answer().getLabel()),
              answer.roll()) + "</html>");
    }

    /**
     * @return the newest Oracle log record of a Fate Chart question, or {@code null} if there is none
     */
    @Nullable JournalEntry latestAnswer() {
        List<JournalEntry> log = roleplay().getOracleLog();
        for (int index = log.size() - 1; index >= 0; index--) {
            if (log.get(index).getAnswer() != null) {
                return log.get(index);
            }
        }
        return null;
    }

    private void refreshLinkedNames() {
        roleplay().refreshLinkedNames(id -> {
            Person person = campaign.getPlayerForce().getHumanResources().getPerson(id);
            return (person == null) ? null : person.getFullName();
        });
    }

    @Subscribe
    public void handle(final NewDayEvent event) {
        refresh();
    }

    @Subscribe
    public void handle(final PersonChangedEvent event) {
        refreshLinkedNames();
        refresh();
    }

    // endregion Refresh

    // region Shared helpers

    Campaign campaign() {
        return campaign;
    }

    OracleActions actions() {
        return actions;
    }

    Roleplay roleplay() {
        return campaign.getRoleplay();
    }

    static String formatDate(final LocalDate date) {
        return MekHQ.getMHQOptions().getDisplayFormattedDate(date);
    }

    static String escape(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // endregion Shared helpers

    /**
     * Tracks the console's size and position in MekHQ's preferences.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(OracleConsole.class);
            setName("OracleConsole");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
