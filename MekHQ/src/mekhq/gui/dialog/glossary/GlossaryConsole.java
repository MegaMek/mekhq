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
package mekhq.gui.dialog.glossary;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.utilities.glossary.DocumentationEntry;
import mekhq.campaign.utilities.glossary.GlossaryChange;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.campaign.utilities.glossary.GlossaryIndex;
import mekhq.campaign.utilities.glossary.GlossaryIndex.Item;
import mekhq.campaign.utilities.glossary.GlossarySearch;
import mekhq.campaign.utilities.glossary.GlossarySearch.Hit;
import mekhq.campaign.utilities.glossary.GlossaryText;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.baseComponents.hud.HudModeSelector;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.utilities.WrapLayout;

/**
 * The glossary, as a heads-up-display console in the style of the Oracle Console and the contract market.
 *
 * <p>The index down the left lists every entry, and can be searched by title or by the words in an entry's text, so
 * players can find an entry from what they remember reading rather than from its label. Entries added or updated in
 * this build, or since the last core release, are tagged, and the ones the player has not yet read since they changed
 * are marked; tiles across the top count each and narrow the index to them. The reader shows the chosen entry, with
 * links to the entries it mentions and the entries that mention it, and back and forward through what the player has
 * read. The Documentation page lists the longer guides, which open in the document viewer.</p>
 *
 * <p>Any set of entries can be exported to a PDF for reading away from MekHQ.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class GlossaryConsole extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(GlossaryConsole.class);
    static final String RESOURCE_BUNDLE = "mekhq.resources.GlossaryConsole";

    /**
     * The command a hyperlink to a glossary entry starts with, as in {@code "GLOSSARY:entryKey"}.
     *
     * @see #handleGlossaryHyperlinkClick(Window, HyperlinkEvent)
     */
    public static final String GLOSSARY_COMMAND_STRING = "GLOSSARY";

    /**
     * The command a hyperlink to a documentation entry starts with, as in {@code "DOCUMENTATION:entryKey"}.
     *
     * @see #handleGlossaryHyperlinkClick(Window, HyperlinkEvent)
     */
    public static final String DOCUMENTATION_COMMAND_STRING = "DOCUMENTATION";

    private static final int INDEX_WIDTH = 330;
    private static final int SIDE_WIDTH = 230;
    private static final int SNIPPET_BEFORE = 45;
    private static final int SNIPPET_AFTER = 120;
    private static final int SEARCH_DELAY_MILLIS = 150;
    private static final int LINK_PREVIEW_LENGTH = 220;
    private static final Pattern UNSAFE_FILE_NAME_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|]");

    private static @Nullable GlossaryConsole openConsole;

    /** The console's two pages. */
    enum Mode {
        GLOSSARY,
        DOCUMENTATION;

        String label() {
            return getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.mode." + name());
        }
    }

    /** Which entries the index shows. */
    enum Filter {
        ALL,
        UNREAD,
        THIS_BUILD,
        SINCE_LAST_CORE
    }

    /** The order of the index, when not searching. */
    enum Sort {
        TITLE,
        NEWEST
    }

    /** A row of the index. */
    private sealed interface Row permits Heading, EntryRow, DocumentRow {}

    /** A heading over a group of rows, or a notice standing in for an empty index. */
    private record Heading(String label, boolean isNotice) implements Row {}

    /** A glossary entry, with an excerpt of its text when it was found there. */
    private record EntryRow(Item item, @Nullable String snippet) implements Row {}

    /** A document. */
    private record DocumentRow(DocumentationEntry document, GlossaryChange change) implements Row {}

    /** How many entries are unread, changed in this build, and changed since the last core release. */
    private record Counts(int unread, int thisBuild, int sinceCore) {}

    private final GlossaryIndex index = GlossaryIndex.build();
    private final GlossaryReadPreferences readPreferences = GlossaryReadPreferences.getInstance();

    private Mode mode = Mode.GLOSSARY;
    private Filter filter = Filter.ALL;
    private Sort sort = Sort.TITLE;
    private List<String> terms = List.of();
    private @Nullable GlossaryEntry current;
    private @Nullable DocumentationEntry currentDocument;
    private final List<GlossaryEntry> history = new ArrayList<>();
    private int historyIndex = -1;
    /** The entries in the index as it stands, in its order; the export's "current list". */
    private List<GlossaryEntry> listedEntries = List.of();
    /** Set while the index is rebuilt or its selection is set to follow the reader, so selection is not navigation. */
    private boolean isSyncing;
    private int lastSelectedIndex = -1;
    /** The glossary's counts, taken again whenever the player reads an entry. */
    private Counts glossaryCounts = new Counts(0, 0, 0);

    private final HudModeSelector<Mode> modeSelector;
    private final HudStatTile entriesTile;
    private final HudStatTile unreadTile;
    private final HudStatTile buildTile;
    private final HudStatTile coreTile;
    private final JTextField searchField = new JTextField();
    private final HudCheckBox searchInTextBox;
    private final Map<Filter, HudChip> filterChips = new EnumMap<>(Filter.class);
    private final Map<Sort, HudChip> sortChips = new EnumMap<>(Sort.class);
    private final DefaultListModel<Row> listModel = new DefaultListModel<>();
    private final JList<Row> list = new JList<>(listModel);
    private final JLabel listSummary = new JLabel();
    private final JEditorPane reader = new JEditorPane();
    private final JScrollPane readerScroll = new JScrollPane(reader);
    private final JLabel readerEyebrow = new JLabel();
    private final JLabel readerTitle = new JLabel();
    private final HudButton backButton;
    private final HudButton forwardButton;
    private final HudButton overviewButton;
    private final HudButton openDocumentButton;
    private final JPanel sidePanel = Hud.transparentPanel(null);
    private final JLabel footerSummary = new JLabel();
    private final HudButton markAllReadButton;
    private final HudButton exportButton;
    private final HudButton guideButton;
    private final HudCheckBox autoOpenGuides;
    private final HudGuideDrawer guideDrawer;
    private final GlossaryGuidePreferences guidePreferences = GlossaryGuidePreferences.getInstance();
    private JPanel welcomeBanner;
    private JScrollPane sideScroll;
    private final Timer searchTimer;

    // region Opening

    /**
     * Opens the glossary on its overview, or brings it to the front if it is already open.
     *
     * @param owner the window opening it; {@code null} for whichever window is active
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void showFor(@Nullable Window owner) {
        open(owner);
    }

    /**
     * Opens the glossary at an entry.
     *
     * @param owner the window opening it; {@code null} for whichever window is active
     * @param entry the entry to show
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void showEntry(@Nullable Window owner, GlossaryEntry entry) {
        open(owner).navigate(entry, true);
    }

    /**
     * Opens the glossary's Documentation page at a document, and opens the document.
     *
     * @param owner    the window opening it; {@code null} for whichever window is active
     * @param document the document
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void showDocument(@Nullable Window owner, DocumentationEntry document) {
        GlossaryConsole console = open(owner);
        console.showMode(Mode.DOCUMENTATION, false);
        console.selectDocument(document);
        console.openDocument(document);
    }

    /**
     * Follows a glossary or documentation hyperlink clicked anywhere in MekHQ: a glossary link opens the glossary at
     * that entry, and a documentation link opens the document in the document viewer.
     *
     * @param parent         the window the link was clicked in; {@code null} for whichever window is active
     * @param hyperlinkEvent the event; only activations are followed
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static void handleGlossaryHyperlinkClick(@Nullable Window parent, HyperlinkEvent hyperlinkEvent) {
        if (hyperlinkEvent.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }
        String[] splitReference = hyperlinkEvent.getDescription().split(":", 2);
        if (splitReference.length != 2) {
            LOGGER.warn("Malformed hyperlink: {}", hyperlinkEvent.getDescription());
            return;
        }
        String command = splitReference[0];
        String entry = splitReference[1];

        if (command.equalsIgnoreCase(GLOSSARY_COMMAND_STRING)) {
            GlossaryEntry glossaryEntry = GlossaryEntry.getGlossaryEntryFromLookUpName(entry);
            if (glossaryEntry == null) {
                LOGGER.warn("Glossary entry not found: {}", entry);
                return;
            }
            showEntry(parent, glossaryEntry);
        } else if (command.equalsIgnoreCase(DOCUMENTATION_COMMAND_STRING)) {
            DocumentationEntry documentationEntry = DocumentationEntry.getDocumentationEntryFromLookUpName(entry);
            if (documentationEntry == null) {
                LOGGER.warn("Documentation entry not found: {}", entry);
                return;
            }
            GuideViewer.showGuide(parent, documentationEntry);
        }
    }

    private static GlossaryConsole open(@Nullable Window owner) {
        Window requester = owner != null ? owner :
                                 KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        // A modal dialog blocks every window but its own children, so a glossary opened from one must belong to it.
        boolean isFromModalDialog = requester instanceof Dialog dialog && dialog.isModal() && requester != openConsole;
        if (openConsole != null && openConsole.isDisplayable()
                  && (!isFromModalDialog || openConsole.getOwner() == requester)) {
            openConsole.toFront();
            openConsole.requestFocus();
            return openConsole;
        }
        if (openConsole != null) {
            openConsole.dispose();
        }
        openConsole = new GlossaryConsole(requester);
        openConsole.setVisible(true);
        return openConsole;
    }

    private GlossaryConsole(@Nullable Window owner) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.windowTitle"), ModalityType.MODELESS);

        modeSelector = new HudModeSelector<>(Mode.class, Mode::label, this::showMode);
        modeSelector.setSelected(Mode.GLOSSARY);
        entriesTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.tile.entries"), "");
        unreadTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.tile.unread"),
              getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.tile.unread.sub"));
        buildTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.tile.thisBuild"),
              MHQConstants.VERSION.toString());
        coreTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.tile.sinceLastCore"),
              getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.tile.sinceLastCore.sub",
                    MHQConstants.LAST_MILESTONE.toString()));
        searchInTextBox = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.search.inText"));
        backButton = new HudButton("\u2039  " + text("GlossaryConsole.reader.back"), false, true);
        forwardButton = new HudButton(text("GlossaryConsole.reader.forward") + "  \u203A", false, true);
        overviewButton = new HudButton(text("GlossaryConsole.reader.overview"), false, true);
        openDocumentButton = new HudButton(text("GlossaryConsole.reader.openDocument"), true, true);
        markAllReadButton = new HudButton(text("GlossaryConsole.footer.markAllRead"), false);
        exportButton = new HudButton(text("GlossaryConsole.footer.export"), false);
        searchTimer = new Timer(SEARCH_DELAY_MILLIS, event -> refreshList());
        searchTimer.setRepeats(false);
        guideButton = new HudButton("?  " + GlossaryGuides.buttonLabel(Mode.GLOSSARY).toUpperCase(Locale.ROOT),
              false, true);
        autoOpenGuides = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.autoOpen"));
        autoOpenGuides.setSelected(guidePreferences.isGuideAutoOpen());
        autoOpenGuides.addActionListener(event -> guidePreferences.setGuideAutoOpen(autoOpenGuides.isSelected()));
        guideDrawer = new HudGuideDrawer(List.of(getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.tab.steps"),
              getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.tab.example"),
              getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.tab.terms")), autoOpenGuides,
              getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.close"), this::closeGuide);

        buildUI();
        recount();
        refreshTiles();
        refreshList();
        showOverview();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(scaleForGUI(940, 600));
        getContentPane().setPreferredSize(scaleForGUI(1180, 780));
        pack();
        setLocationRelativeTo(owner);
        setPreferences();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                searchField.requestFocusInWindow();
            }

            @Override
            public void windowClosed(WindowEvent event) {
                searchTimer.stop();
                if (openConsole == GlossaryConsole.this) {
                    openConsole = null;
                }
            }
        });
    }

    // endregion Opening

    // region Layout

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(buildCommandBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(true);
        body.setBackground(GROUND);
        body.add(buildIndex(), BorderLayout.WEST);
        body.add(buildReader(), BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);

        // The guide drawer and its scrim float over the console. The drawer claims Escape for itself, so its action is
        // replaced with one that closes an open guide first and otherwise does what Escape does in the console.
        guideDrawer.install(getRootPane());
        getRootPane().getActionMap().put("closeGuide", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                onEscape();
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
        JLabel eyebrow = new JLabel(text("GlossaryConsole.eyebrow"));
        eyebrow.setForeground(TEXT_FAINT);
        eyebrow.setFont(hudFont(Font.BOLD, 0.72f, 0.18f));
        JLabel title = new JLabel(text("GlossaryConsole.title"));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.35f, 0.16f));
        JLabel subtitle = new JLabel(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.subtitle"));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        identity.add(leftAligned(eyebrow));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));
        identity.add(leftAligned(title));
        identity.add(Box.createVerticalStrut(scaleForGUI(4)));
        identity.add(leftAligned(subtitle));

        makeFilterTile(entriesTile, Filter.ALL, "GlossaryConsole.tile.entries.toolTipText");
        makeFilterTile(unreadTile, Filter.UNREAD, "GlossaryConsole.tile.unread.toolTipText");
        makeFilterTile(buildTile, Filter.THIS_BUILD, "GlossaryConsole.tile.thisBuild.toolTipText");
        makeFilterTile(coreTile, Filter.SINCE_LAST_CORE, "GlossaryConsole.tile.sinceLastCore.toolTipText");
        JPanel tiles = Hud.tileRow(entriesTile, unreadTile, buildTile, coreTile);

        JPanel topRow = new JPanel(new BorderLayout(scaleForGUI(24), 0));
        topRow.setOpaque(false);
        topRow.add(identity, BorderLayout.CENTER);
        topRow.add(tiles, BorderLayout.EAST);

        guideButton.addActionListener(event -> openGuide(mode));
        JPanel modesRow = new JPanel(new BorderLayout(scaleForGUI(8), 0));
        modesRow.setOpaque(false);
        modesRow.add(modeSelector, BorderLayout.CENTER);
        modesRow.add(guideButton, BorderLayout.EAST);

        commandBar.add(topRow, BorderLayout.CENTER);
        commandBar.add(modesRow, BorderLayout.SOUTH);
        return commandBar;
    }

    /** Lets a tile be clicked to narrow the index to the entries it counts. */
    private void makeFilterTile(HudStatTile tile, Filter tileFilter, String toolTipKey) {
        tile.setToolTipText(getTextAt(RESOURCE_BUNDLE, toolTipKey));
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (tile.contains(event.getPoint())) {
                    showMode(Mode.GLOSSARY);
                    setFilter(tileFilter);
                }
            }
        });
    }

    private JPanel buildIndex() {
        JPanel controls = column();
        controls.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(14), scaleForGUI(8),
              scaleForGUI(12)));

        Hud.styleField(searchField);
        searchField.putClientProperty("JTextField.placeholderText",
              getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.search.placeholder"));
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.search.toolTipText"));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                searchTimer.restart();
            }
        });
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_DOWN) {
                    focusList();
                    event.consume();
                } else if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchTimer.stop();
                    refreshList();
                    openFirstRow();
                    event.consume();
                }
            }
        });
        controls.add(leftAligned(searchField));
        controls.add(Box.createVerticalStrut(scaleForGUI(6)));

        searchInTextBox.setSelected(true);
        searchInTextBox.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.search.inText.toolTipText"));
        searchInTextBox.addActionListener(event -> refreshList());
        controls.add(leftAligned(searchInTextBox));
        controls.add(Box.createVerticalStrut(scaleForGUI(8)));

        JPanel filters = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT,
              scaleForGUI(4), scaleForGUI(3)));
        for (Filter chipFilter : Filter.values()) {
            HudChip chip = new HudChip("", () -> setFilter(chipFilter));
            filterChips.put(chipFilter, chip);
            filters.add(chip);
        }
        controls.add(leftAligned(filters));

        JPanel sortRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        listSummary.setForeground(TEXT_FAINT);
        listSummary.setFont(hudFont(Font.PLAIN, 0.78f, 0.0f));
        sortRow.add(listSummary, BorderLayout.CENTER);
        JPanel sorts = Hud.transparentPanel(null);
        sorts.setLayout(new BoxLayout(sorts, BoxLayout.X_AXIS));
        for (Sort chipSort : Sort.values()) {
            HudChip chip = new HudChip(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.sort." + chipSort.name()),
                  () -> setSort(chipSort));
            sortChips.put(chipSort, chip);
            sorts.add(chip);
        }
        sortRow.add(sorts, BorderLayout.EAST);
        controls.add(Box.createVerticalStrut(scaleForGUI(4)));
        controls.add(leftAligned(sortRow));

        list.setOpaque(true);
        list.setBackground(SURFACE_DEEP);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new RowRenderer());
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !isSyncing) {
                onRowSelected();
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && list.getSelectedValue() instanceof DocumentRow row) {
                    openDocument(row.document());
                }
            }
        });
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER && list.getSelectedValue() instanceof DocumentRow row) {
                    openDocument(row.document());
                } else if (event.getKeyCode() == KeyEvent.VK_UP && list.getSelectedIndex() <= firstSelectableRow()) {
                    searchField.requestFocusInWindow();
                    event.consume();
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(list);
        Hud.styleScroll(listScroll, SURFACE_DEEP, false);
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // Rows wrap to the list's width, so their heights are measured again whenever it changes.
        listScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                list.setFixedCellHeight(10);
                list.setFixedCellHeight(-1);
            }
        });

        JPanel indexPanel = new JPanel(new BorderLayout());
        indexPanel.setOpaque(true);
        indexPanel.setBackground(SURFACE_DEEP);
        indexPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, scaleForGUI(1), BORDER));
        indexPanel.setPreferredSize(scaleForGUI(INDEX_WIDTH, 0));
        indexPanel.add(controls, BorderLayout.NORTH);
        indexPanel.add(listScroll, BorderLayout.CENTER);
        return indexPanel;
    }

    private JPanel buildReader() {
        JPanel navigation = Hud.transparentPanel(null);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.X_AXIS));
        backButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.reader.back.toolTipText"));
        backButton.addActionListener(event -> goBack());
        forwardButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.reader.forward.toolTipText"));
        forwardButton.addActionListener(event -> goForward());
        overviewButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.reader.overview.toolTipText"));
        overviewButton.addActionListener(event -> showOverview());
        openDocumentButton.addActionListener(event -> {
            if (currentDocument != null) {
                openDocument(currentDocument);
            }
        });
        navigation.add(backButton);
        navigation.add(Box.createHorizontalStrut(scaleForGUI(4)));
        navigation.add(forwardButton);
        navigation.add(Box.createHorizontalStrut(scaleForGUI(4)));
        navigation.add(overviewButton);
        navigation.add(Box.createHorizontalGlue());
        navigation.add(openDocumentButton);

        readerEyebrow.setForeground(TEXT_FAINT);
        readerEyebrow.setFont(hudFont(Font.BOLD, 0.72f, 0.16f));
        readerTitle.setForeground(ACCENT_BRIGHT);
        readerTitle.setFont(hudFont(Font.BOLD, 1.3f, 0.08f));

        JPanel header = column();
        header.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(18), scaleForGUI(10), scaleForGUI(18))));
        header.add(leftAligned(navigation));
        header.add(Box.createVerticalStrut(scaleForGUI(10)));
        header.add(leftAligned(readerEyebrow));
        header.add(Box.createVerticalStrut(scaleForGUI(3)));
        header.add(leftAligned(readerTitle));

        reader.setEditable(false);
        reader.setOpaque(true);
        reader.setBackground(GROUND);
        HTMLEditorKit kit = new HTMLEditorKit();
        reader.setEditorKit(kit);
        HTMLDocument document = (HTMLDocument) kit.createDefaultDocument();
        applyReaderStyles(document.getStyleSheet());
        reader.setDocument(document);
        reader.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(20), scaleForGUI(20),
              scaleForGUI(20)));
        reader.addHyperlinkListener(this::onReaderLink);
        Hud.styleScroll(readerScroll, GROUND, false);
        readerScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(14), scaleForGUI(14),
              scaleForGUI(14)));
        sideScroll = new JScrollPane(topAligned(sidePanel));
        Hud.styleScroll(sideScroll, SURFACE_DEEP, false);
        sideScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));
        sideScroll.setPreferredSize(scaleForGUI(SIDE_WIDTH, 0));

        JPanel readerPanel = new JPanel(new BorderLayout());
        readerPanel.setOpaque(true);
        readerPanel.setBackground(GROUND);
        welcomeBanner = buildWelcome();
        JPanel welcomeHolder = Hud.transparentPanel(new BorderLayout());
        welcomeHolder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(20), 0,
              scaleForGUI(20)));
        welcomeHolder.add(welcomeBanner, BorderLayout.CENTER);
        JPanel readerArea = Hud.transparentPanel(new BorderLayout());
        readerArea.add(welcomeHolder, BorderLayout.NORTH);
        readerArea.add(readerScroll, BorderLayout.CENTER);

        readerPanel.add(header, BorderLayout.NORTH);
        readerPanel.add(readerArea, BorderLayout.CENTER);
        readerPanel.add(sideScroll, BorderLayout.EAST);
        return readerPanel;
    }

    /** The overview's invitation to read the guide, shown until the player takes it up or dismisses it. */
    private JPanel buildWelcome() {
        JPanel banner = new JPanel(new BorderLayout(scaleForGUI(12), 0));
        banner.setOpaque(true);
        banner.setBackground(SURFACE_HIGHLIGHT);
        banner.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_CYAN,
              scaleForGUI(1)), BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(10),
              scaleForGUI(12))));
        JPanel words = column();
        JLabel title = new JLabel(text("GlossaryGuide.welcome.title"));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 0.95f, 0.12f));
        words.add(leftAligned(title));
        words.add(Box.createVerticalStrut(scaleForGUI(2)));
        JLabel intro = new JLabel("<html>" + escape(getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.welcome.text"))
                                        + "</html>");
        intro.setForeground(TEXT_MUTED);
        intro.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        words.add(leftAligned(intro));

        HudButton notNow = new HudButton(text("GlossaryGuide.welcome.dismiss"), false, true);
        notNow.addActionListener(event -> dismissWelcome());
        HudButton showMe = new HudButton(text("GlossaryGuide.welcome.show"), true, true);
        showMe.addActionListener(event -> {
            dismissWelcome();
            openGuide(Mode.GLOSSARY);
        });
        JPanel buttons = column();
        buttons.add(showMe);
        buttons.add(Box.createVerticalStrut(scaleForGUI(6)));
        buttons.add(notNow);

        banner.add(words, BorderLayout.CENTER);
        banner.add(buttons, BorderLayout.EAST);
        return banner;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(18), scaleForGUI(10), scaleForGUI(18))));

        footerSummary.setForeground(TEXT_MUTED);
        footerSummary.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        footer.add(footerSummary, BorderLayout.CENTER);

        markAllReadButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.footer.markAllRead.toolTipText"));
        markAllReadButton.addActionListener(event -> markAllRead());
        exportButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.footer.export.toolTipText"));
        exportButton.addActionListener(event -> exportPdf());
        HudButton close = new HudButton(text("GlossaryConsole.footer.close"), false);
        close.addActionListener(event -> dispose());

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(markAllReadButton);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(10)));
        buttons.add(exportButton);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(10)));
        buttons.add(close);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    private void installKeyBindings(JComponent root) {
        int menuKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F, menuKey), "search", this::focusSearch);
        bind(root, KeyStroke.getKeyStroke('/'), "searchSlash", () -> {
            if (!isTyping()) {
                focusSearch();
            }
        });
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_P, menuKey), "export", this::exportPdf);
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK), "back", this::goBack);
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK), "forward", this::goForward);
    }

    /** Escape closes an open guide, then clears a search, then closes the glossary. */
    private void onEscape() {
        if (guideDrawer.isVisible()) {
            closeGuide();
        } else if (!searchField.getText().isEmpty()) {
            searchField.setText("");
            searchTimer.stop();
            refreshList();
            focusSearch();
        } else {
            dispose();
        }
    }

    private static void bind(JComponent root, KeyStroke keyStroke, String name, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    private static boolean isTyping() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextComponent
                     component && component.isEditable();
    }

    /** Adds the HUD's text styles to the reader's own stylesheet, so they affect only the reader. */
    private static void applyReaderStyles(StyleSheet styles) {
        Font font = hudFont(Font.PLAIN, 1.0f, 0.0f);
        int size = font.getSize();
        styles.addRule("body { color: " + hex(TEXT) + "; background-color: " + hex(GROUND) + "; font-family: '"
                             + font.getFamily() + "'; font-size: " + size + "pt; margin: 0; }");
        styles.addRule("p { margin-top: 0; margin-bottom: 8px; }");
        styles.addRule("h1 { color: " + hex(ACCENT_BRIGHT) + "; font-size: " + (size + 6)
                             + "pt; margin-top: 6px; margin-bottom: 6px; }");
        styles.addRule("h2 { color: " + hex(ACCENT_BRIGHT) + "; font-size: " + (size + 3)
                             + "pt; margin-top: 10px; margin-bottom: 4px; }");
        styles.addRule("h3 { color: " + hex(ACCENT) + "; font-size: " + (size + 1)
                             + "pt; margin-top: 8px; margin-bottom: 4px; }");
        styles.addRule("ul { margin-left: 18px; margin-top: 0; margin-bottom: 8px; }");
        styles.addRule("ol { margin-left: 22px; margin-top: 0; margin-bottom: 8px; }");
        styles.addRule("li { margin-bottom: 3px; }");
        styles.addRule("blockquote { color: " + hex(TEXT_MUTED) + "; margin-left: 10px; margin-top: 2px; "
                             + "margin-bottom: 8px; padding-left: 8px; border-left: 2px solid " + hex(AMBER) + "; }");
        styles.addRule("a { color: " + hex(ACCENT_BRIGHT) + "; }");
        styles.addRule("th { color: " + hex(ACCENT) + "; text-align: left; padding: 3px; }");
        styles.addRule("td { padding: 3px; }");
        styles.addRule(".muted { color: " + hex(TEXT_MUTED) + "; }");
        styles.addRule(".faint { color: " + hex(TEXT_FAINT) + "; font-size: " + Math.max(8, size - 2) + "pt; }");
        styles.addRule(".build { color: " + hex(AMBER) + "; }");
        styles.addRule(".core { color: " + hex(READY) + "; }");
    }

    // endregion Layout

    // region Index

    private void showMode(Mode newMode) {
        showMode(newMode, true);
    }

    /**
     * Switches page.
     *
     * @param newMode  the page
     * @param showPage {@code true} to show the page's entry, overview or introduction in the reader; {@code false}
     *                 when the caller is about to show something there itself
     */
    private void showMode(Mode newMode, boolean showPage) {
        modeSelector.setSelected(newMode);
        if (mode == newMode) {
            return;
        }
        mode = newMode;
        closeGuide();
        guideButton.setText("?  " + GlossaryGuides.buttonLabel(mode).toUpperCase(Locale.ROOT));
        if (mode == Mode.DOCUMENTATION && filter == Filter.UNREAD) {
            filter = Filter.ALL;
        }
        searchInTextBox.setEnabled(mode == Mode.GLOSSARY);
        filterChips.get(Filter.UNREAD).setVisible(mode == Mode.GLOSSARY);
        refreshList();
        if (showPage && mode == Mode.GLOSSARY) {
            if (current != null) {
                renderEntry(current, false);
            } else {
                showOverview();
            }
        } else if (showPage) {
            showDocumentationIntro();
        }
        maybeOpenGuide(mode);
    }

    private void setFilter(Filter newFilter) {
        filter = newFilter;
        refreshList();
    }

    private void setSort(Sort newSort) {
        sort = newSort;
        refreshList();
    }

    /** Rebuilds the index for the page, filter, order and search as they stand. */
    private void refreshList() {
        terms = GlossarySearch.parseQuery(searchField.getText());
        isSyncing = true;
        try {
            listModel.clear();
            if (mode == Mode.GLOSSARY) {
                fillGlossaryRows();
            } else {
                fillDocumentRows();
            }
            selectCurrentRow();
        } finally {
            isSyncing = false;
        }
        refreshChips();
        refreshFooter();
        // The entry in the reader keeps up with the search's words, without jumping while the player types.
        if (mode == Mode.GLOSSARY && current != null) {
            reader.getHighlighter().removeAllHighlights();
            highlightMatches(false);
        }
    }

    private void fillGlossaryRows() {
        List<Item> candidates = index.items().stream().filter(this::passesFilter).toList();
        List<GlossaryEntry> entries = new ArrayList<>();

        if (!terms.isEmpty()) {
            List<GlossarySearch.Document> documents = candidates.stream().map(Item::document).toList();
            List<Hit> hits = GlossarySearch.search(documents, searchField.getText(), searchInTextBox.isSelected());
            List<Hit> titleHits = hits.stream().filter(hit -> hit.where() == GlossarySearch.Where.TITLE).toList();
            List<Hit> textHits = hits.stream().filter(hit -> hit.where() == GlossarySearch.Where.TEXT).toList();
            addHitGroup("GlossaryConsole.list.titleMatches", titleHits, false, entries);
            addHitGroup("GlossaryConsole.list.textMatches", textHits, true, entries);
            listSummary.setText(getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.list.matches", hits.size()));
        } else {
            List<Item> ordered = new ArrayList<>(candidates);
            ordered.sort(sort == Sort.NEWEST ? GlossaryIndex.NEWEST_FIRST : GlossaryIndex.BY_TITLE);
            String lastGroup = null;
            for (Item item : ordered) {
                String group = sort == Sort.NEWEST ? versionHeading(item) : letterHeading(item.title());
                if (!group.equals(lastGroup)) {
                    listModel.addElement(new Heading(group, false));
                    lastGroup = group;
                }
                listModel.addElement(new EntryRow(item, null));
                entries.add(item.entry());
            }
            listSummary.setText(getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.list.count", ordered.size()));
        }
        if (entries.isEmpty()) {
            listModel.addElement(new Heading(getTextAt(RESOURCE_BUNDLE,
                  terms.isEmpty() ? "GlossaryConsole.list.emptyFilter" : "GlossaryConsole.list.noMatches"), true));
        }
        listedEntries = List.copyOf(entries);
    }

    private void addHitGroup(String headingKey, List<Hit> hits, boolean withSnippets, List<GlossaryEntry> entries) {
        if (hits.isEmpty()) {
            return;
        }
        listModel.addElement(new Heading(getFormattedTextAt(RESOURCE_BUNDLE, headingKey, hits.size()), false));
        for (Hit hit : hits) {
            GlossaryEntry entry = index.entry(hit.document().key());
            Item item = entry == null ? null : index.item(entry);
            if (item == null) {
                continue;
            }
            String snippet = withSnippets ? GlossarySearch.snippet(hit.document(), terms, SNIPPET_BEFORE,
                  SNIPPET_AFTER) : null;
            listModel.addElement(new EntryRow(item, snippet));
            entries.add(entry);
        }
    }

    private void fillDocumentRows() {
        List<DocumentationEntry> documents = new ArrayList<>(List.of(DocumentationEntry.values()));
        documents.removeIf(document -> !passesFilter(GlossaryChange.of(document.getVersionAddedOrLastUpdated()),
              true));
        String query = String.join(" ", terms);
        if (!terms.isEmpty()) {
            // Documents have only titles to search; every word, or a simpler form of it, must be in the title.
            documents.removeIf(document -> {
                String title = document.getTitle().toLowerCase(Locale.ROOT);
                return !terms.stream()
                              .allMatch(term -> GlossarySearch.variants(term).stream().anyMatch(title::contains));
            });
        }
        Comparator<DocumentationEntry> byTitle = Comparator.comparing(DocumentationEntry::getTitle,
              String.CASE_INSENSITIVE_ORDER);
        documents.sort(sort == Sort.NEWEST
                             ? Comparator.comparing(DocumentationEntry::getVersionAddedOrLastUpdated,
              Comparator.reverseOrder()).thenComparing(byTitle)
                             : byTitle);
        for (DocumentationEntry document : documents) {
            listModel.addElement(new DocumentRow(document,
                  GlossaryChange.of(document.getVersionAddedOrLastUpdated())));
        }
        if (documents.isEmpty()) {
            listModel.addElement(new Heading(getTextAt(RESOURCE_BUNDLE,
                  query.isEmpty() ? "GlossaryConsole.list.emptyFilter" : "GlossaryConsole.list.noMatches"), true));
        }
        listSummary.setText(getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.list.documents",
              documents.size()));
        listedEntries = List.of();
    }

    private boolean passesFilter(Item item) {
        if (filter == Filter.UNREAD) {
            return isUnread(item);
        }
        return passesFilter(item.change(), false);
    }

    private boolean passesFilter(GlossaryChange change, boolean isDocument) {
        return switch (filter) {
            case ALL -> true;
            case UNREAD -> !isDocument;
            case THIS_BUILD -> change == GlossaryChange.THIS_BUILD;
            case SINCE_LAST_CORE -> change.isChanged();
        };
    }

    private boolean isUnread(Item item) {
        return item.change().isChanged() && !readPreferences.isRead(item.entry().getLookUpName(), item.version());
    }

    private static String letterHeading(String title) {
        if (title.isEmpty()) {
            return "#";
        }
        char first = Character.toUpperCase(title.charAt(0));
        return Character.isLetter(first) ? String.valueOf(first) : "#";
    }

    private static String versionHeading(Item item) {
        String key = switch (item.change()) {
            case THIS_BUILD -> "GlossaryConsole.list.version.thisBuild";
            case SINCE_LAST_CORE -> "GlossaryConsole.list.version.sinceLastCore";
            case UNCHANGED -> "GlossaryConsole.list.version";
        };
        return getFormattedTextAt(RESOURCE_BUNDLE, key, item.version().toString());
    }

    /** Counts the glossary's unread and recently changed entries again, after the player has read some. */
    private void recount() {
        int unread = 0;
        int thisBuild = 0;
        int sinceCore = 0;
        for (Item item : index.items()) {
            if (isUnread(item)) {
                unread++;
            }
            if (item.change() == GlossaryChange.THIS_BUILD) {
                thisBuild++;
            }
            if (item.change().isChanged()) {
                sinceCore++;
            }
        }
        glossaryCounts = new Counts(unread, thisBuild, sinceCore);
    }

    /** Counts the recently changed documents; documents are never unread. */
    private static Counts documentCounts() {
        int thisBuild = 0;
        int sinceCore = 0;
        for (DocumentationEntry document : DocumentationEntry.values()) {
            GlossaryChange change = GlossaryChange.of(document.getVersionAddedOrLastUpdated());
            if (change == GlossaryChange.THIS_BUILD) {
                thisBuild++;
            }
            if (change.isChanged()) {
                sinceCore++;
            }
        }
        return new Counts(0, thisBuild, sinceCore);
    }

    private void refreshChips() {
        // The chips count what they filter: entries on the Glossary page, documents on the Documentation page.
        Counts counts = mode == Mode.GLOSSARY ? glossaryCounts : documentCounts();
        int all = mode == Mode.GLOSSARY ? index.items().size() : DocumentationEntry.values().length;
        filterChips.get(Filter.ALL).setText(chipLabel("GlossaryConsole.filter.ALL", all));
        filterChips.get(Filter.UNREAD).setText(chipLabel("GlossaryConsole.filter.UNREAD", counts.unread()));
        filterChips.get(Filter.THIS_BUILD).setText(chipLabel("GlossaryConsole.filter.THIS_BUILD",
              counts.thisBuild()));
        filterChips.get(Filter.SINCE_LAST_CORE).setText(chipLabel("GlossaryConsole.filter.SINCE_LAST_CORE",
              counts.sinceCore()));
        filterChips.forEach((chipFilter, chip) -> chip.setActive(chipFilter == filter));
        sortChips.forEach((chipSort, chip) -> {
            chip.setActive(chipSort == sort);
            // A search lists the best matches first, so the order only shows while not searching.
            chip.setVisible(terms.isEmpty());
        });
    }

    private static String chipLabel(String key, int count) {
        return getTextAt(RESOURCE_BUNDLE, key).toUpperCase(Locale.ROOT) + "  " + count;
    }

    private void refreshTiles() {
        int unread = glossaryCounts.unread();
        int thisBuild = glossaryCounts.thisBuild();
        int sinceCore = glossaryCounts.sinceCore();
        entriesTile.setValue(Integer.toString(index.items().size()), TEXT);
        entriesTile.setSub(getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.tile.entries.sub",
              DocumentationEntry.values().length));
        unreadTile.setValue(Integer.toString(unread), unread > 0 ? ACCENT_BRIGHT : TEXT_FAINT);
        buildTile.setValue(Integer.toString(thisBuild), thisBuild > 0 ? AMBER : TEXT_FAINT);
        coreTile.setValue(Integer.toString(sinceCore), sinceCore > 0 ? READY : TEXT_FAINT);
        markAllReadButton.setArmed(unread > 0);
    }

    private void refreshFooter() {
        if (mode == Mode.DOCUMENTATION) {
            footerSummary.setText(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.footer.documentation"));
        } else if (!terms.isEmpty()) {
            footerSummary.setText(getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.footer.search",
                  listedEntries.size(), searchField.getText().strip()));
        } else {
            footerSummary.setText(getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.footer.showing",
                  listedEntries.size(), index.items().size()));
        }
        exportButton.setArmed(mode == Mode.GLOSSARY);
    }

    /** Selects the row of the entry or document in the reader, if it is listed, without navigating. */
    private void selectCurrentRow() {
        boolean wasSyncing = isSyncing;
        isSyncing = true;
        try {
            int rowIndex = -1;
            for (int i = 0; i < listModel.size(); i++) {
                Row row = listModel.get(i);
                if ((row instanceof EntryRow entryRow && entryRow.item().entry() == current && mode == Mode.GLOSSARY)
                          || (row instanceof DocumentRow documentRow && documentRow.document() == currentDocument
                                    && mode == Mode.DOCUMENTATION)) {
                    rowIndex = i;
                    break;
                }
            }
            if (rowIndex >= 0) {
                list.setSelectedIndex(rowIndex);
                list.ensureIndexIsVisible(rowIndex);
            } else {
                list.clearSelection();
            }
            lastSelectedIndex = rowIndex;
        } finally {
            isSyncing = wasSyncing;
        }
    }

    private void onRowSelected() {
        int selected = list.getSelectedIndex();
        if (selected < 0) {
            return;
        }
        Row row = listModel.get(selected);
        if (row instanceof Heading) {
            if (Math.abs(selected - lastSelectedIndex) != 1) {
                // A heading was clicked rather than stepped onto; it cannot be chosen, so the selection stays put.
                selectRowQuietly(lastSelectedIndex);
                return;
            }
            // Headings are passed over in the direction the selection was moving.
            int step = selected >= lastSelectedIndex ? 1 : -1;
            int next = nextSelectableRow(selected, step);
            if (next < 0) {
                next = nextSelectableRow(selected, -step);
            }
            if (next >= 0) {
                list.setSelectedIndex(next);
                list.ensureIndexIsVisible(next);
            } else {
                list.clearSelection();
            }
            return;
        }
        lastSelectedIndex = selected;
        if (row instanceof EntryRow entryRow) {
            navigate(entryRow.item().entry(), true);
        } else if (row instanceof DocumentRow documentRow) {
            showDocumentCard(documentRow.document());
        }
    }

    private void selectRowQuietly(int rowIndex) {
        isSyncing = true;
        try {
            if (rowIndex >= 0 && rowIndex < listModel.size()) {
                list.setSelectedIndex(rowIndex);
            } else {
                list.clearSelection();
            }
        } finally {
            isSyncing = false;
        }
    }

    private int nextSelectableRow(int from, int step) {
        for (int i = from + step; i >= 0 && i < listModel.size(); i += step) {
            if (!(listModel.get(i) instanceof Heading)) {
                return i;
            }
        }
        return -1;
    }

    private int firstSelectableRow() {
        return nextSelectableRow(-1, 1);
    }

    private void focusList() {
        int first = list.getSelectedIndex() >= 0 ? list.getSelectedIndex() : firstSelectableRow();
        if (first >= 0) {
            list.requestFocusInWindow();
            list.setSelectedIndex(first);
            list.ensureIndexIsVisible(first);
        }
    }

    private void openFirstRow() {
        int first = firstSelectableRow();
        if (first >= 0) {
            list.setSelectedIndex(first);
            list.ensureIndexIsVisible(first);
            if (listModel.get(first) instanceof DocumentRow row) {
                openDocument(row.document());
            }
        }
    }

    private void focusSearch() {
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    private void selectDocument(DocumentationEntry document) {
        currentDocument = document;
        showDocumentCard(document);
        selectCurrentRow();
    }

    // endregion Index

    // region Reader

    /**
     * Shows an entry in the reader and notes it as read.
     *
     * @param entry        the entry
     * @param addToHistory {@code true} to add it to the back and forward history
     */
    private void navigate(GlossaryEntry entry, boolean addToHistory) {
        if (addToHistory && (historyIndex < 0 || history.get(historyIndex) != entry)) {
            while (history.size() > historyIndex + 1) {
                history.remove(history.size() - 1);
            }
            history.add(entry);
            historyIndex = history.size() - 1;
        }
        current = entry;
        if (mode != Mode.GLOSSARY) {
            showMode(Mode.GLOSSARY, false);
        }
        renderEntry(entry, true);
        selectCurrentRow();
    }

    private void goBack() {
        if (current == null && historyIndex >= 0) {
            // From the overview, back returns to the entry last read.
            navigate(history.get(historyIndex), false);
        } else if (historyIndex > 0) {
            historyIndex--;
            navigate(history.get(historyIndex), false);
        }
    }

    private void goForward() {
        if (current != null && historyIndex < history.size() - 1) {
            historyIndex++;
            navigate(history.get(historyIndex), false);
        }
    }

    private void renderEntry(GlossaryEntry entry, boolean markRead) {
        Item item = index.item(entry);
        if (item == null) {
            return;
        }
        // Only a changed entry can be unread, so only reading one is worth noting.
        boolean wasUnread = isUnread(item);
        if (markRead && wasUnread) {
            readPreferences.markRead(entry.getLookUpName(), item.version());
        }

        readerEyebrow.setText(versionLine(item.version(), item.change()).toUpperCase(Locale.ROOT));
        readerTitle.setText(item.title().toUpperCase(Locale.ROOT));
        setReaderText(entry.getDefinition());
        highlightMatches(true);
        refreshSide(item);
        refreshNavigation(true);

        if (wasUnread && markRead) {
            recount();
            refreshTiles();
            refreshChips();
            list.repaint();
        }
    }

    /** Shows the glossary's front page: what it is, and what changed recently. */
    private void showOverview() {
        current = null;
        if (mode != Mode.GLOSSARY) {
            showMode(Mode.GLOSSARY, false);
        }
        readerEyebrow.setText(text("GlossaryConsole.overview.eyebrow"));
        readerTitle.setText(text("GlossaryConsole.overview.title"));

        StringBuilder html = new StringBuilder(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.overview.about"));
        Map<Version, List<Item>> changed = index.changedByVersion();
        if (changed.isEmpty()) {
            html.append("<p class='muted'>").append(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.overview.noChanges"))
                  .append("</p>");
        } else {
            html.append("<h2>").append(escape(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.overview.changes")))
                  .append("</h2>");
            changed.forEach((version, items) -> {
                GlossaryChange change = items.get(0).change();
                html.append("<h3>").append(escape(versionLine(version, change))).append("</h3><ul>");
                for (Item item : items) {
                    html.append("<li><a href='").append(GlossaryText.GLOSSARY_LINK_PREFIX)
                          .append(item.entry().getLookUpName()).append("'>").append(escape(item.title()))
                          .append("</a>");
                    if (isUnread(item)) {
                        html.append(" <span class='build'>\u25CF</span>");
                    }
                    html.append("</li>");
                }
                html.append("</ul>");
            });
        }
        setReaderText(html.toString());
        clearSide();
        refreshNavigation(true);
        selectCurrentRow();
    }

    private void showDocumentationIntro() {
        currentDocument = null;
        readerEyebrow.setText(text("GlossaryConsole.documentation.eyebrow"));
        readerTitle.setText(text("GlossaryConsole.documentation.title"));
        setReaderText(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.documentation.about"));
        clearSide();
        refreshNavigation(false);
        selectCurrentRow();
    }

    private void showDocumentCard(DocumentationEntry document) {
        currentDocument = document;
        GlossaryChange change = GlossaryChange.of(document.getVersionAddedOrLastUpdated());
        readerEyebrow.setText(versionLine(document.getVersionAddedOrLastUpdated(), change).toUpperCase(Locale.ROOT));
        readerTitle.setText(document.getTitle().toUpperCase(Locale.ROOT));
        setReaderText(getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.documentation.card",
              escape(document.getTitle())));

        // The glossary entries that point to this document, so a player can read the summary first.
        sidePanel.removeAll();
        List<Item> mentioning = index.items().stream().filter(item -> item.documents().contains(document)).toList();
        if (!mentioning.isEmpty()) {
            addSideSection("GlossaryConsole.side.mentionedIn",
                  mentioning.stream().map(Item::entry).toList());
        }
        setSideVisible(!mentioning.isEmpty());
        refreshNavigation(false);
    }

    private void openDocument(DocumentationEntry document) {
        GuideViewer.showGuide(this, document);
    }

    private void refreshNavigation(boolean isGlossary) {
        backButton.setVisible(isGlossary);
        forwardButton.setVisible(isGlossary);
        overviewButton.setVisible(isGlossary);
        openDocumentButton.setVisible(!isGlossary && currentDocument != null);
        backButton.setArmed((current == null && historyIndex >= 0) || historyIndex > 0);
        forwardButton.setArmed(current != null && historyIndex < history.size() - 1);
        overviewButton.setArmed(current != null);
    }

    private void setReaderText(String html) {
        // The welcome banner belongs to the overview, until the player takes up or dismisses it.
        welcomeBanner.setVisible(mode == Mode.GLOSSARY && current == null && !guidePreferences.isWelcomeDismissed());
        reader.setText("<html><body>" + html + "</body></html>");
        reader.getHighlighter().removeAllHighlights();
        reader.setCaretPosition(0);
        SwingUtilities.invokeLater(() -> readerScroll.getVerticalScrollBar().setValue(0));
    }

    /** Marks the search's words in the reader and, if asked, scrolls to the first. */
    private void highlightMatches(boolean scrollToFirst) {
        Optional<Pattern> pattern = GlossarySearch.highlightPattern(terms);
        if (pattern.isEmpty()) {
            return;
        }
        try {
            String text = reader.getDocument().getText(0, reader.getDocument().getLength());
            Matcher matcher = pattern.get().matcher(text);
            DefaultHighlighter.DefaultHighlightPainter painter =
                  new DefaultHighlighter.DefaultHighlightPainter(translucent(AMBER, 110));
            int first = -1;
            while (matcher.find()) {
                reader.getHighlighter().addHighlight(matcher.start(), matcher.end(), painter);
                if (first < 0) {
                    first = matcher.start();
                }
            }
            if (scrollToFirst && first >= 0) {
                int firstMatch = first;
                SwingUtilities.invokeLater(() -> {
                    try {
                        Rectangle2D area = reader.modelToView2D(firstMatch);
                        if (area != null) {
                            Rectangle visible = area.getBounds();
                            visible.grow(0, scaleForGUI(60));
                            reader.scrollRectToVisible(visible);
                        }
                    } catch (BadLocationException ignored) {
                        // The text changed before the reader was laid out; there is nothing to scroll to.
                    }
                });
            }
        } catch (BadLocationException ex) {
            LOGGER.warn(ex, "Unable to highlight search matches");
        }
    }

    private void onReaderLink(HyperlinkEvent event) {
        String description = event.getDescription();
        if (description == null) {
            return;
        }
        if (event.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            reader.setToolTipText(linkPreview(description));
            return;
        } else if (event.getEventType() == HyperlinkEvent.EventType.EXITED) {
            reader.setToolTipText(null);
            return;
        } else if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }

        reader.setToolTipText(null);
        if (description.startsWith(GlossaryText.GLOSSARY_LINK_PREFIX)) {
            GlossaryEntry entry = index.entry(description.substring(GlossaryText.GLOSSARY_LINK_PREFIX.length()));
            if (entry == null) {
                LOGGER.warn("Glossary entry not found: {}", description);
                return;
            }
            navigate(entry, true);
        } else if (description.startsWith(GlossaryText.DOCUMENTATION_LINK_PREFIX)) {
            DocumentationEntry document = DocumentationEntry.getDocumentationEntryFromLookUpName(
                  description.substring(GlossaryText.DOCUMENTATION_LINK_PREFIX.length()));
            if (document == null) {
                LOGGER.warn("Documentation entry not found: {}", description);
                return;
            }
            openDocument(document);
        } else {
            LOGGER.warn("Unrecognised glossary link: {}", description);
        }
    }

    /** A tooltip previewing where a link leads: an entry's title and opening words, or a document's title. */
    private @Nullable String linkPreview(String description) {
        if (description.startsWith(GlossaryText.GLOSSARY_LINK_PREFIX)) {
            GlossaryEntry entry = index.entry(description.substring(GlossaryText.GLOSSARY_LINK_PREFIX.length()));
            Item item = entry == null ? null : index.item(entry);
            if (item == null) {
                return null;
            }
            String text = item.document().text();
            String opening = text;
            if (text.length() > LINK_PREVIEW_LENGTH) {
                int cut = text.lastIndexOf(' ', LINK_PREVIEW_LENGTH);
                opening = text.substring(0, cut > 0 ? cut : LINK_PREVIEW_LENGTH) + "\u2026";
            }
            return "<html><div style='width:" + scaleForGUI(300) + "px'><b>" + escape(item.title()) + "</b><br>"
                         + escape(opening) + "</div></html>";
        } else if (description.startsWith(GlossaryText.DOCUMENTATION_LINK_PREFIX)) {
            DocumentationEntry document = DocumentationEntry.getDocumentationEntryFromLookUpName(
                  description.substring(GlossaryText.DOCUMENTATION_LINK_PREFIX.length()));
            return document == null ? null : getFormattedTextAt(RESOURCE_BUNDLE,
                  "GlossaryConsole.reader.documentLink.toolTipText", document.getTitle());
        }
        return null;
    }

    // endregion Reader

    // region Guides

    /**
     * Opens a page's guide the first time the page is shown, if the player has not turned that off. The Glossary
     * page offers its guide from the overview's welcome banner instead, as the glossary is often opened at an entry.
     */
    private void maybeOpenGuide(Mode page) {
        if (page != Mode.GLOSSARY && !guidePreferences.isGuideSeen(page) && guidePreferences.isGuideAutoOpen()) {
            openGuide(page);
        }
    }

    private void openGuide(Mode page) {
        guidePreferences.setGuideSeen(page, true);
        guideDrawer.open(GlossaryGuides.build(page));
    }

    private void closeGuide() {
        guideDrawer.close();
    }

    private void dismissWelcome() {
        guidePreferences.setWelcomeDismissed(true);
        welcomeBanner.setVisible(false);
    }

    // endregion Guides

    // region Side panel

    private void refreshSide(Item item) {
        sidePanel.removeAll();
        addSideSection("GlossaryConsole.side.seeAlso", item.seeAlso());
        addSideSection("GlossaryConsole.side.referencedBy", index.referencedBy(item.entry()));
        if (!item.documents().isEmpty()) {
            sidePanel.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE,
                  "GlossaryConsole.side.documents"))));
            sidePanel.add(Box.createVerticalStrut(scaleForGUI(6)));
            for (DocumentationEntry document : item.documents()) {
                sidePanel.add(leftAligned(sideLink(document.getTitle(), () -> openDocument(document))));
                sidePanel.add(Box.createVerticalStrut(scaleForGUI(4)));
            }
            sidePanel.add(Box.createVerticalStrut(scaleForGUI(12)));
        }

        sidePanel.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.side.record"))));
        sidePanel.add(Box.createVerticalStrut(scaleForGUI(6)));
        sidePanel.add(leftAligned(Hud.hint(versionLine(item.version(), item.change()))));
        sidePanel.add(Box.createVerticalStrut(scaleForGUI(4)));
        sidePanel.add(leftAligned(Hud.hint(getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.side.words",
              item.document().text().isBlank() ? 0 : item.document().text().split("\\s+").length))));
        setSideVisible(true);
    }

    /** Empties the side panel and hides it, giving the reader its width. */
    private void clearSide() {
        sidePanel.removeAll();
        setSideVisible(false);
    }

    private void setSideVisible(boolean visible) {
        sideScroll.setVisible(visible);
        sidePanel.revalidate();
        sidePanel.repaint();
        sideScroll.getParent().revalidate();
    }

    /** A link that wraps to the side panel's width rather than running off its edge. */
    private static JLabel sideLink(String title, Runnable onChoose) {
        return Hud.link("<html><div style='width:" + scaleForGUI(SIDE_WIDTH - 40) + "px'>" + escape(title)
                              + "</div></html>", onChoose);
    }

    private void addSideSection(String headingKey, List<GlossaryEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        sidePanel.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, headingKey))));
        sidePanel.add(Box.createVerticalStrut(scaleForGUI(6)));
        for (GlossaryEntry entry : entries) {
            sidePanel.add(leftAligned(sideLink(entry.getTitle(), () -> navigate(entry, true))));
            sidePanel.add(Box.createVerticalStrut(scaleForGUI(4)));
        }
        sidePanel.add(Box.createVerticalStrut(scaleForGUI(12)));
    }

    // endregion Side panel

    // region Actions

    private void markAllRead() {
        for (Item item : index.items()) {
            if (isUnread(item)) {
                readPreferences.markRead(item.entry().getLookUpName(), item.version());
            }
        }
        recount();
        refreshTiles();
        refreshList();
        if (current == null && mode == Mode.GLOSSARY) {
            showOverview();
        }
    }

    private void exportPdf() {
        if (mode != Mode.GLOSSARY) {
            return;
        }
        GlossaryExportDialog exportDialog = new GlossaryExportDialog(this, index, current,
              terms.isEmpty() && filter == Filter.ALL ? List.of() : listedEntries);
        List<GlossaryEntry> chosen = exportDialog.getSelectedEntries();
        if (!exportDialog.wasConfirmed() || chosen.isEmpty()) {
            return;
        }

        String fileName = chosen.size() == 1
                                ? getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.export.fileName.single",
                  chosen.get(0).getTitle())
                                : getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.export.fileName",
                                      MHQConstants.VERSION.toString());
        // Titles such as "Mission Type: Security Duty" hold characters some systems refuse in a file name.
        fileName = UNSAFE_FILE_NAME_CHARACTERS.matcher(fileName).replaceAll("-");
        Optional<File> file = FileDialogs.saveGlossaryPDF(frameAbove(this), fileName);
        if (file.isEmpty()) {
            return;
        }

        List<GlossaryPdfExporter.Entry> entries = chosen.stream().map(this::pdfEntry).toList();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            int pages = GlossaryPdfExporter.export(file.get(), getTextAt(RESOURCE_BUNDLE, "pdf.glossary.title"),
                  getFormattedTextAt(RESOURCE_BUNDLE, "pdf.glossary.subtitle", MHQConstants.VERSION.toString()),
                  entries, exportDialog.getOptions());
            footerSummary.setText(getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.export.saved",
                  file.get().getName(), pages));
            LOGGER.info("Exported {} glossary entries to PDF {}", entries.size(), file.get());
        } catch (IOException ex) {
            LOGGER.error(ex, "Failed to export the glossary to {}", file.get());
            JOptionPane.showMessageDialog(this, getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryConsole.export.failed",
                  file.get().getName()), getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.export.failed.title"),
                  JOptionPane.ERROR_MESSAGE);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private GlossaryPdfExporter.Entry pdfEntry(GlossaryEntry entry) {
        Item item = index.item(entry);
        List<String> referencedBy = index.referencedBy(entry).stream().map(GlossaryEntry::getTitle).toList();
        String versionLine = item == null ? "" : versionLine(item.version(), item.change());
        String title = item == null ? entry.getTitle() : item.title();
        return new GlossaryPdfExporter.Entry(entry.getLookUpName(), title, versionLine, entry.getDefinition(),
              referencedBy);
    }

    /**
     * The file dialogs want a frame, and the glossary and the guide viewer may belong to a dialog, so the frame above
     * a window is found; failing that, any frame MekHQ is showing.
     *
     * @param window the window
     *
     * @return the frame, or {@code null} if MekHQ is showing none
     */
    static @Nullable JFrame frameAbove(@Nullable Window window) {
        while (window != null && !(window instanceof JFrame)) {
            window = window.getOwner();
        }
        if (window instanceof JFrame frame) {
            return frame;
        }
        for (Frame frame : Frame.getFrames()) {
            if (frame instanceof JFrame jFrame && frame.isVisible()) {
                return jFrame;
            }
        }
        return null;
    }

    // endregion Actions

    // region Helpers

    static String versionLine(Version version, GlossaryChange change) {
        String key = switch (change) {
            case THIS_BUILD -> "GlossaryConsole.version.thisBuild";
            case SINCE_LAST_CORE -> "GlossaryConsole.version.sinceLastCore";
            case UNCHANGED -> "GlossaryConsole.version.unchanged";
        };
        return getFormattedTextAt(RESOURCE_BUNDLE, key, version.toString());
    }

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, key).toUpperCase(Locale.ROOT);
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JPanel column() {
        JPanel column = Hud.transparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        return column;
    }

    /** Wraps a component so it keeps its preferred height at the top of a scroll pane, rather than stretching. */
    private static JPanel topAligned(JComponent component) {
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.add(component, BorderLayout.NORTH);
        return holder;
    }

    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(GlossaryConsole.class);
            setName("GlossaryConsole");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed to set user preferences");
        }
    }

    // endregion Helpers

    /** Draws the index's rows: headings, and entries with their tags, unread mark and search excerpt. */
    private final class RowRenderer implements ListCellRenderer<Row> {
        private final JLabel label = new JLabel();

        private RowRenderer() {
            label.setOpaque(true);
            label.setVerticalAlignment(SwingConstants.TOP);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Row> source, Row row, int rowIndex,
              boolean isSelected, boolean cellHasFocus) {
            int width = Math.max(scaleForGUI(120), source.getWidth() - scaleForGUI(36));
            label.setBackground(isSelected ? SURFACE_HIGHLIGHT : SURFACE_DEEP);
            label.setForeground(TEXT);
            label.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
            label.setToolTipText(null);

            if (row instanceof Heading heading) {
                label.setBackground(SURFACE_DEEP);
                label.setFont(heading.isNotice() ? hudFont(Font.PLAIN, 0.88f, 0.0f)
                                    : hudFont(Font.BOLD, 0.7f, 0.14f));
                label.setForeground(heading.isNotice() ? TEXT_FAINT : ACCENT);
                label.setText(heading.isNotice() ? "<html><div style='width:" + width + "px'>"
                                                         + escape(heading.label()) + "</div></html>"
                                    : heading.label().toUpperCase(Locale.ROOT));
                label.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(heading.isNotice() ? 14 : 10),
                      scaleForGUI(14), scaleForGUI(4), scaleForGUI(10)));
                return label;
            }

            label.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(0, scaleForGUI(2), 0, 0, isSelected ? ACCENT : SURFACE_DEEP),
                  BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(12), scaleForGUI(5), scaleForGUI(8))));
            StringBuilder html = new StringBuilder("<html><div style='width:").append(width).append("px'>");
            if (row instanceof EntryRow entryRow) {
                Item item = entryRow.item();
                if (isUnread(item)) {
                    html.append("<font color='").append(hex(ACCENT_BRIGHT)).append("'>\u25CF</font> ");
                }
                html.append(highlight(item.title(), ACCENT_BRIGHT));
                appendTag(html, item.change(), item.version());
                if (entryRow.snippet() != null && !entryRow.snippet().isEmpty()) {
                    html.append("<br><font color='").append(hex(TEXT_FAINT)).append("'>")
                          .append(highlight(entryRow.snippet(), AMBER)).append("</font>");
                }
            } else if (row instanceof DocumentRow documentRow) {
                html.append(highlight(documentRow.document().getTitle(), ACCENT_BRIGHT));
                appendTag(html, documentRow.change(), documentRow.document().getVersionAddedOrLastUpdated());
            }
            label.setText(html.append("</div></html>").toString());
            return label;
        }

        private void appendTag(StringBuilder html, GlossaryChange change, Version version) {
            if (change == GlossaryChange.THIS_BUILD) {
                html.append("&nbsp; <font color='").append(hex(AMBER)).append("'>")
                      .append(escape(getTextAt(RESOURCE_BUNDLE, "GlossaryConsole.tag.thisBuild")))
                      .append("</font>");
            } else if (change == GlossaryChange.SINCE_LAST_CORE) {
                html.append("&nbsp; <font color='").append(hex(READY)).append("'>")
                      .append(escape(version.toString())).append("</font>");
            }
        }

        /** Escapes text and marks the search's words in it. */
        private String highlight(String text, Color color) {
            Optional<Pattern> pattern = GlossarySearch.highlightPattern(terms);
            if (pattern.isEmpty()) {
                return escape(text);
            }
            StringBuilder result = new StringBuilder();
            Matcher matcher = pattern.get().matcher(text);
            int last = 0;
            while (matcher.find()) {
                result.append(escape(text.substring(last, matcher.start())))
                      .append("<b><font color='").append(hex(color)).append("'>")
                      .append(escape(matcher.group())).append("</font></b>");
                last = matcher.end();
            }
            return result.append(escape(text.substring(last))).toString();
        }
    }
}
