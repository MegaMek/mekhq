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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import jakarta.annotation.Nullable;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.utilities.glossary.DocumentationEntry;
import mekhq.campaign.utilities.glossary.GlossaryChange;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.campaign.utilities.glossary.GlossaryIndex;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.baseComponents.hud.HudTabStrip;
import mekhq.gui.dialog.glossary.GuideDocument.OutlineItem;
import mekhq.gui.dialog.glossary.GuideDocument.PageSize;
import mekhq.gui.dialog.glossary.GuideSearch.LibraryHit;
import mekhq.gui.dialog.glossary.GuideSearch.LibraryResult;
import mekhq.gui.dialog.glossary.GuideSearch.PageHit;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Reads MekHQ's guides, in the heads-up-display style of the glossary console.
 *
 * <p>The library down the left lists every guide, and can search inside all of them at once; choosing a page it found
 * opens the guide at that page. Open guides sit as tabs across the top. The guide itself scrolls as one run of pages,
 * drawn in the background so scrolling never waits, with page and zoom controls and a find box above it. Beside it,
 * the guide's own outline follows the reader, and the Pages, Matches and Related tabs show thumbnails, every place the
 * find box's words appear, and the glossary entries that point to the guide.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class GuideViewer extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(GuideViewer.class);
    private static final String RESOURCE_BUNDLE = GlossaryConsole.RESOURCE_BUNDLE;

    /** Screen pixels per PDF point at 100%, so 100% shows a page at its printed size on a 96 dpi screen. */
    private static final float ACTUAL_SIZE = 96f / 72f;
    private static final float MINIMUM_ZOOM = 0.25f;
    private static final float MAXIMUM_ZOOM = 4f;
    private static final float ZOOM_STEP = 1.15f;
    private static final int PAGE_GAP = 22;
    private static final int CANVAS_MARGIN = 24;
    private static final int THUMBNAIL_WIDTH = 104;
    /** Pages further than this from the one being read let their images go. */
    private static final int KEPT_PAGES = 8;
    private static final int MAXIMUM_LISTED_MATCHES = 300;
    private static final int MAXIMUM_LIBRARY_PAGES = 80;
    private static final int PASSAGE_BEFORE = 50;
    private static final int PASSAGE_AFTER = 110;
    private static final int TYPING_DELAY_MILLIS = 180;
    /** Section titles in the Page tile are cut to this many characters, so the tiles keep their width. */
    private static final int TILE_TEXT_LENGTH = 28;

    private static @Nullable GuideViewer openViewer;

    /** How the pages are sized. */
    private enum Fit {
        WIDTH,
        PAGE,
        CUSTOM
    }

    /** The tabs beside the guide. */
    private enum SideTab {
        OUTLINE,
        PAGES,
        MATCHES,
        RELATED
    }

    /** A guide open in a tab, and where the player was in it. */
    private static final class OpenGuide {
        private final DocumentationEntry entry;
        private @Nullable GuideRenderer renderer;
        private @Nullable GuideDocument document;
        private @Nullable String error;
        private int page = 1;
        private float zoom = ACTUAL_SIZE;
        private Fit fit = Fit.WIDTH;
        private String find = "";
        private List<PageHit> hits = List.of();
        private int hitIndex = -1;
        /** Set when the guide should be shown at a page once it has opened. */
        private int pendingPage = 1;

        private OpenGuide(DocumentationEntry entry) {
            this.entry = entry;
        }

        private boolean isReady() {
            return renderer != null && document != null;
        }
    }

    /** A row of the library. */
    private sealed interface LibraryRow permits Heading, GuideRow, PageRow {}

    private record Heading(String label, boolean isNotice) implements LibraryRow {}

    private record GuideRow(DocumentationEntry entry, @Nullable String query) implements LibraryRow {}

    private record PageRow(DocumentationEntry entry, PageHit hit, String text, String query) implements LibraryRow {}

    private final GuideLibrary library = GuideLibrary.getInstance();
    private final GlossaryGuidePreferences guidePreferences = GlossaryGuidePreferences.getInstance();
    private final List<OpenGuide> open = new ArrayList<>();
    private @Nullable OpenGuide active;
    private @Nullable GlossaryIndex glossaryIndex;
    private SideTab sideTab = SideTab.OUTLINE;
    private final List<GuidePageView> pageViews = new ArrayList<>();
    private final List<GuidePageView> thumbnails = new ArrayList<>();
    private final List<SideRow> outlineRows = new ArrayList<>();
    private boolean isRestoringView;

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final HudStatTile pageTile;
    private final HudStatTile zoomTile;
    private final HudStatTile findTile;
    private final JPanel tabStrip = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(2), 0));
    private final HudButton guideButton;
    private final JTextField librarySearch = new JTextField();
    private final HudCheckBox searchInsideBox;
    private final JLabel libraryCount = new JLabel();
    private final DefaultListModel<LibraryRow> libraryModel = new DefaultListModel<>();
    private final JList<LibraryRow> libraryList = new JList<>(libraryModel);
    private final HudButton previousButton;
    private final HudButton nextButton;
    private final JTextField pageField = new JTextField(3);
    private final JLabel pageOfLabel = new JLabel();
    private final HudButton zoomOutButton;
    private final HudButton zoomInButton;
    private final JLabel zoomLabel = new JLabel();
    private final HudChip fitWidthChip;
    private final HudChip fitPageChip;
    private final JTextField findField = new JTextField();
    private final JLabel findCount = new JLabel();
    private final HudButton findPreviousButton;
    private final HudButton findNextButton;
    private final JPanel pagesColumn = new JPanel();
    private final JScrollPane canvasScroll = new JScrollPane(pagesColumn);
    private final HudTabStrip sideTabs;
    private final CardLayout sideCards = new CardLayout();
    private final JPanel sideBody = new JPanel(sideCards);
    private final Map<SideTab, JPanel> sideColumns = new EnumMap<>(SideTab.class);
    private final Map<SideTab, JScrollPane> sideScrolls = new EnumMap<>(SideTab.class);
    private final JLabel footerSummary = new JLabel();
    private final HudButton saveButton;
    private final HudButton externalButton;
    private final HudButton closeTabButton;
    private final HudCheckBox autoOpenGuides;
    private final HudGuideDrawer guideDrawer;
    private final Timer libraryTimer;
    private final Timer findTimer;
    private final Timer refitTimer;
    private final Runnable libraryListener = this::onLibraryRead;

    // region Opening

    /**
     * Opens a guide in the viewer, at its first page, or where the player was if it is already open.
     *
     * @param owner the window opening it; {@code null} for whichever window is active
     * @param entry the guide
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void showGuide(@Nullable Window owner, DocumentationEntry entry) {
        showGuide(owner, entry, 0, "");
    }

    /**
     * Opens a guide in the viewer at a page, with words already in its find box.
     *
     * @param owner the window opening it; {@code null} for whichever window is active
     * @param entry the guide
     * @param page  the page to show, from 1; 0 to keep the place in a guide already open
     * @param find  the words to find, or empty for none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void showGuide(@Nullable Window owner, DocumentationEntry entry, int page, String find) {
        Window requester = owner != null ? owner
                                 : KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        // A modal dialog blocks every window but its own children, so a viewer opened from one must belong to it.
        boolean isFromModalDialog = requester instanceof Dialog dialog && dialog.isModal() && requester != openViewer;
        if (openViewer == null || !openViewer.isDisplayable()
                  || (isFromModalDialog && openViewer.getOwner() != requester)) {
            if (openViewer != null) {
                openViewer.dispose();
            }
            openViewer = new GuideViewer(requester);
            openViewer.setVisible(true);
        } else {
            openViewer.toFront();
            openViewer.requestFocus();
        }
        openViewer.openGuide(entry, page, find, false);
    }

    private GuideViewer(@Nullable Window owner) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "GuideViewer.windowTitle"), ModalityType.MODELESS);

        pageTile = new HudStatTile(text("GuideViewer.tile.page"), "");
        zoomTile = new HudStatTile(text("GuideViewer.tile.zoom"), "");
        findTile = new HudStatTile(text("GuideViewer.tile.find"), "");
        guideButton = new HudButton("?  " + upper("GlossaryGuide.viewer.button"), false, true);
        searchInsideBox = new HudCheckBox(text("GuideViewer.library.inside"));
        previousButton = new HudButton("\u2039", false, true);
        nextButton = new HudButton("\u203A", false, true);
        zoomOutButton = new HudButton("\u2212", false, true);
        zoomInButton = new HudButton("+", false, true);
        fitWidthChip = new HudChip(text("GuideViewer.zoom.fitWidth"), () -> setFit(Fit.WIDTH));
        fitPageChip = new HudChip(text("GuideViewer.zoom.fitPage"), () -> setFit(Fit.PAGE));
        findPreviousButton = new HudButton("\u2039", false, true);
        findNextButton = new HudButton("\u203A", false, true);
        sideTabs = new HudTabStrip(List.of(text("GuideViewer.side.OUTLINE"), text("GuideViewer.side.PAGES"),
              text("GuideViewer.side.MATCHES"), text("GuideViewer.side.RELATED")), true,
              index -> showSideTab(SideTab.values()[index]));
        saveButton = new HudButton(upper("GuideViewer.footer.save"), false);
        externalButton = new HudButton(upper("GuideViewer.footer.external"), false);
        closeTabButton = new HudButton(upper("GuideViewer.footer.closeTab"), false);
        autoOpenGuides = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.autoOpen"));
        autoOpenGuides.setSelected(guidePreferences.isGuideAutoOpen());
        autoOpenGuides.addActionListener(event -> guidePreferences.setGuideAutoOpen(autoOpenGuides.isSelected()));
        guideDrawer = new HudGuideDrawer(List.of(getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.tab.steps"),
              getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.tab.example"),
              getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.tab.terms")), autoOpenGuides,
              getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.close"), this::closeGuide);
        libraryTimer = new Timer(TYPING_DELAY_MILLIS, event -> refreshLibrary());
        libraryTimer.setRepeats(false);
        findTimer = new Timer(TYPING_DELAY_MILLIS, event -> runFind(true));
        findTimer.setRepeats(false);
        refitTimer = new Timer(120, event -> refit());
        refitTimer.setRepeats(false);

        buildUI();
        library.addListener(libraryListener);
        library.readAll();
        refreshLibrary();
        showActive();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(scaleForGUI(980, 620));
        getContentPane().setPreferredSize(scaleForGUI(1320, 840));
        pack();
        setLocationRelativeTo(owner);
        setPreferences();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                if (!guidePreferences.isViewerGuideSeen() && guidePreferences.isGuideAutoOpen()) {
                    openHowTo();
                }
            }

            @Override
            public void windowClosed(WindowEvent event) {
                libraryTimer.stop();
                findTimer.stop();
                refitTimer.stop();
                library.removeListener(libraryListener);
                open.forEach(GuideViewer::closeRenderer);
                open.clear();
                if (openViewer == GuideViewer.this) {
                    openViewer = null;
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
        body.add(buildLibrary(), BorderLayout.WEST);
        body.add(buildDocument(), BorderLayout.CENTER);
        body.add(buildSide(), BorderLayout.EAST);
        content.add(body, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);

        // The guide drawer claims Escape for itself, so its action is replaced with one that closes an open guide
        // first and otherwise does what Escape does in the viewer.
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
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), 0, scaleForGUI(16))));

        JPanel identity = column();
        JLabel eyebrow = new JLabel(upper("GuideViewer.eyebrow"));
        eyebrow.setForeground(TEXT_FAINT);
        eyebrow.setFont(hudFont(Font.BOLD, 0.72f, 0.18f));
        titleLabel.setForeground(ACCENT_BRIGHT);
        titleLabel.setFont(hudFont(Font.BOLD, 1.3f, 0.12f));
        subtitleLabel.setForeground(TEXT_MUTED);
        subtitleLabel.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        identity.add(leftAligned(eyebrow));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));
        identity.add(leftAligned(titleLabel));
        identity.add(Box.createVerticalStrut(scaleForGUI(4)));
        identity.add(leftAligned(subtitleLabel));

        JPanel topRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(24), 0));
        topRow.add(identity, BorderLayout.CENTER);
        topRow.add(Hud.tileRow(pageTile, zoomTile, findTile), BorderLayout.EAST);

        JScrollPane tabScroll = new JScrollPane(tabStrip, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tabScroll.setBorder(null);
        tabScroll.setOpaque(false);
        tabScroll.getViewport().setOpaque(false);
        tabScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, scaleForGUI(4)));
        guideButton.addActionListener(event -> openHowTo());
        JPanel tabsRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        tabsRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tabsRow.add(tabScroll, BorderLayout.CENTER);
        JPanel guideHolder = Hud.transparentPanel(new BorderLayout());
        guideHolder.setBorder(BorderFactory.createEmptyBorder(0, 0, scaleForGUI(6), 0));
        guideHolder.add(guideButton, BorderLayout.SOUTH);
        tabsRow.add(guideHolder, BorderLayout.EAST);

        commandBar.add(topRow, BorderLayout.CENTER);
        commandBar.add(tabsRow, BorderLayout.SOUTH);
        return commandBar;
    }

    private JPanel buildLibrary() {
        JPanel controls = column();
        controls.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(12), scaleForGUI(8),
              scaleForGUI(12)));
        Hud.styleField(librarySearch);
        librarySearch.putClientProperty("JTextField.placeholderText",
              getTextAt(RESOURCE_BUNDLE, "GuideViewer.library.placeholder"));
        librarySearch.putClientProperty("JTextField.showClearButton", true);
        librarySearch.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.library.toolTipText"));
        onTyping(librarySearch, libraryTimer);
        librarySearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_DOWN) {
                    focusLibraryList();
                    event.consume();
                }
            }
        });
        controls.add(leftAligned(librarySearch));
        controls.add(Box.createVerticalStrut(scaleForGUI(6)));
        searchInsideBox.setSelected(true);
        searchInsideBox.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.library.inside.toolTipText"));
        searchInsideBox.addActionListener(event -> refreshLibrary());
        controls.add(leftAligned(searchInsideBox));
        controls.add(Box.createVerticalStrut(scaleForGUI(8)));
        libraryCount.setForeground(TEXT_FAINT);
        libraryCount.setFont(hudFont(Font.PLAIN, 0.78f, 0.0f));
        controls.add(leftAligned(libraryCount));

        libraryList.setOpaque(true);
        libraryList.setBackground(SURFACE_DEEP);
        libraryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        libraryList.setCellRenderer(new LibraryRenderer());
        libraryList.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.library.list.toolTipText"));
        libraryList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                int index = libraryList.locationToIndex(event.getPoint());
                if (index >= 0 && libraryList.getCellBounds(index, index).contains(event.getPoint())) {
                    chooseLibraryRow(libraryModel.get(index), event.isShiftDown());
                }
            }
        });
        libraryList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER && libraryList.getSelectedValue() != null) {
                    chooseLibraryRow(libraryList.getSelectedValue(), event.isShiftDown());
                    event.consume();
                } else if (event.getKeyCode() == KeyEvent.VK_UP && libraryList.getSelectedIndex() <= 0) {
                    librarySearch.requestFocusInWindow();
                    event.consume();
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(libraryList);
        Hud.styleScroll(listScroll, SURFACE_DEEP, false);
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // Rows wrap to the list's width, so their heights are measured again whenever it changes.
        listScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                libraryList.setFixedCellHeight(10);
                libraryList.setFixedCellHeight(-1);
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(SURFACE_DEEP);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, scaleForGUI(1), BORDER));
        panel.setPreferredSize(scaleForGUI(300, 0));
        panel.add(controls, BorderLayout.NORTH);
        panel.add(listScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDocument() {
        previousButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.page.previous.toolTipText"));
        previousButton.addActionListener(event -> stepPage(-1));
        nextButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.page.next.toolTipText"));
        nextButton.addActionListener(event -> stepPage(1));
        Hud.styleField(pageField);
        pageField.setHorizontalAlignment(SwingConstants.CENTER);
        pageField.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.page.field.toolTipText"));
        pageField.setMaximumSize(pageField.getPreferredSize());
        pageField.addActionListener(event -> {
            try {
                goToPage(Integer.parseInt(pageField.getText().strip()));
            } catch (NumberFormatException ignored) {
                // Not a number; the field is put back to the current page below.
            }
            refreshPageControls();
            canvasScroll.requestFocusInWindow();
        });
        pageOfLabel.setForeground(TEXT_FAINT);
        pageOfLabel.setFont(hudFont(Font.PLAIN, 0.86f, 0.0f));
        zoomOutButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.zoom.out.toolTipText"));
        zoomOutButton.addActionListener(event -> zoomBy(1 / ZOOM_STEP));
        zoomInButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.zoom.in.toolTipText"));
        zoomInButton.addActionListener(event -> zoomBy(ZOOM_STEP));
        zoomLabel.setForeground(TEXT);
        zoomLabel.setFont(hudFont(Font.PLAIN, 0.86f, 0.0f));
        zoomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        zoomLabel.setPreferredSize(new Dimension(scaleForGUI(52), zoomLabel.getPreferredSize().height));
        fitWidthChip.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.zoom.fitWidth.toolTipText"));
        fitPageChip.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.zoom.fitPage.toolTipText"));

        Hud.styleField(findField);
        findField.putClientProperty("JTextField.placeholderText",
              getTextAt(RESOURCE_BUNDLE, "GuideViewer.find.placeholder"));
        findField.putClientProperty("JTextField.showClearButton", true);
        findField.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.find.toolTipText"));
        onTyping(findField, findTimer);
        findField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (findTimer.isRunning()) {
                        findTimer.stop();
                        runFind(true);
                    } else {
                        stepFind(event.isShiftDown() ? -1 : 1);
                    }
                    event.consume();
                }
            }
        });
        findCount.setForeground(TEXT_FAINT);
        findCount.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));
        findPreviousButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.find.previous.toolTipText"));
        findPreviousButton.addActionListener(event -> stepFind(-1));
        findNextButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.find.next.toolTipText"));
        findNextButton.addActionListener(event -> stepFind(1));

        JPanel navigation = Hud.transparentPanel(null);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.X_AXIS));
        navigation.add(previousButton);
        navigation.add(Box.createHorizontalStrut(scaleForGUI(4)));
        navigation.add(pageField);
        navigation.add(Box.createHorizontalStrut(scaleForGUI(6)));
        navigation.add(pageOfLabel);
        navigation.add(Box.createHorizontalStrut(scaleForGUI(6)));
        navigation.add(nextButton);
        navigation.add(Box.createHorizontalStrut(scaleForGUI(14)));
        navigation.add(zoomOutButton);
        navigation.add(zoomLabel);
        navigation.add(zoomInButton);
        navigation.add(Box.createHorizontalStrut(scaleForGUI(8)));
        navigation.add(fitWidthChip);
        navigation.add(Box.createHorizontalStrut(scaleForGUI(4)));
        navigation.add(fitPageChip);

        JPanel find = Hud.transparentPanel(null);
        find.setLayout(new BoxLayout(find, BoxLayout.X_AXIS));
        findField.setPreferredSize(new Dimension(scaleForGUI(210), findField.getPreferredSize().height));
        findField.setMaximumSize(findField.getPreferredSize());
        find.add(findField);
        find.add(Box.createHorizontalStrut(scaleForGUI(6)));
        find.add(findCount);
        find.add(Box.createHorizontalStrut(scaleForGUI(6)));
        find.add(findPreviousButton);
        find.add(Box.createHorizontalStrut(scaleForGUI(2)));
        find.add(findNextButton);

        JPanel toolbar = new JPanel(new BorderLayout(scaleForGUI(12), 0));
        toolbar.setOpaque(true);
        toolbar.setBackground(SURFACE_DEEP);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(7), scaleForGUI(12), scaleForGUI(7), scaleForGUI(12))));
        toolbar.add(navigation, BorderLayout.WEST);
        toolbar.add(find, BorderLayout.EAST);

        pagesColumn.setLayout(new BoxLayout(pagesColumn, BoxLayout.Y_AXIS));
        pagesColumn.setOpaque(true);
        pagesColumn.setBackground(GROUND);
        pagesColumn.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(PAGE_GAP), scaleForGUI(CANVAS_MARGIN),
              scaleForGUI(PAGE_GAP * 2), scaleForGUI(CANVAS_MARGIN)));
        Hud.styleScroll(canvasScroll, GROUND, false);
        canvasScroll.getHorizontalScrollBar().setUnitIncrement(scaleForGUI(16));
        canvasScroll.setFocusable(true);
        canvasScroll.getViewport().addChangeListener(event -> onScrolled());
        canvasScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                refitTimer.restart();
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(GROUND);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(canvasScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSide() {
        for (SideTab tab : SideTab.values()) {
            JPanel columnPanel = column();
            columnPanel.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8), 0, scaleForGUI(12), 0));
            JPanel holder = Hud.transparentPanel(new BorderLayout());
            holder.add(columnPanel, BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(holder);
            Hud.styleScroll(scroll, SURFACE_DEEP, false);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            sideColumns.put(tab, columnPanel);
            sideScrolls.put(tab, scroll);
            sideBody.add(scroll, tab.name());
        }
        sideBody.setOpaque(false);
        sideTabs.setSelected(SideTab.OUTLINE.ordinal());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(SURFACE_DEEP);
        panel.setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));
        panel.setPreferredSize(scaleForGUI(284, 0));
        panel.add(sideTabs, BorderLayout.NORTH);
        panel.add(sideBody, BorderLayout.CENTER);
        return panel;
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

        saveButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.footer.save.toolTipText"));
        saveButton.addActionListener(event -> saveCopy());
        externalButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.footer.external.toolTipText"));
        externalButton.addActionListener(event -> openExternally());
        closeTabButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.footer.closeTab.toolTipText"));
        closeTabButton.addActionListener(event -> {
            if (active != null) {
                closeGuideTab(active);
            }
        });
        HudButton close = new HudButton(upper("GuideViewer.footer.close"), false);
        close.addActionListener(event -> dispose());

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(saveButton);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(externalButton);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(closeTabButton);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(close);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    private void installKeyBindings(JComponent root) {
        int menuKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F, menuKey), "find", () -> {
            findField.requestFocusInWindow();
            findField.selectAll();
        });
        bind(root, KeyStroke.getKeyStroke('/'), "searchLibrary", () -> {
            if (!isTyping()) {
                librarySearch.requestFocusInWindow();
                librarySearch.selectAll();
            }
        });
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, menuKey), "zoomIn", () -> zoomBy(ZOOM_STEP));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, menuKey), "zoomInPlus", () -> zoomBy(ZOOM_STEP));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_ADD, menuKey), "zoomInPad", () -> zoomBy(ZOOM_STEP));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, menuKey), "zoomOut", () -> zoomBy(1 / ZOOM_STEP));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, menuKey), "zoomOutPad", () -> zoomBy(1 / ZOOM_STEP));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_0, menuKey), "fitWidth", () -> setFit(Fit.WIDTH));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_W, menuKey), "closeTab", () -> {
            if (active != null) {
                closeGuideTab(active);
            }
        });
        for (int key : new int[] { KeyEvent.VK_RIGHT, KeyEvent.VK_PAGE_DOWN }) {
            bind(root, KeyStroke.getKeyStroke(key, 0), "next" + key, () -> {
                if (!isTyping() && !isInLibraryList()) {
                    stepPage(1);
                }
            });
        }
        for (int key : new int[] { KeyEvent.VK_LEFT, KeyEvent.VK_PAGE_UP }) {
            bind(root, KeyStroke.getKeyStroke(key, 0), "previous" + key, () -> {
                if (!isTyping() && !isInLibraryList()) {
                    stepPage(-1);
                }
            });
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

    private static void onTyping(JTextField field, Timer timer) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                timer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                timer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                timer.restart();
            }
        });
    }

    private static boolean isTyping() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextComponent
                     component && component.isEditable();
    }

    private boolean isInLibraryList() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == libraryList;
    }

    /** Escape closes an open guide drawer, then clears the box being typed in, then closes the viewer. */
    private void onEscape() {
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (guideDrawer.isVisible()) {
            closeGuide();
        } else if (focus == findField && !findField.getText().isEmpty()) {
            findField.setText("");
            findTimer.stop();
            runFind(false);
        } else if (focus == librarySearch && !librarySearch.getText().isEmpty()) {
            librarySearch.setText("");
            libraryTimer.stop();
            refreshLibrary();
        } else {
            dispose();
        }
    }

    // endregion Layout

    // region Tabs

    /**
     * Opens a guide, or shows it if it is already open.
     *
     * @param entry  the guide
     * @param page   the page to show, from 1; 0 to keep the place in a guide already open
     * @param find   the words to put in the find box, or empty to keep the guide's own
     * @param newTab {@code true} to open it beside the guide being read rather than in its place
     */
    private void openGuide(DocumentationEntry entry, int page, String find, boolean newTab) {
        rememberView();
        OpenGuide guide = open.stream().filter(candidate -> candidate.entry == entry).findFirst().orElse(null);
        if (guide == null) {
            guide = new OpenGuide(entry);
            if (newTab || active == null) {
                open.add(guide);
            } else {
                // Opening without asking for a new tab replaces the guide being read, as a browser link does.
                int index = open.indexOf(active);
                closeRenderer(active);
                open.set(index, guide);
            }
            load(guide);
        }
        if (page > 0) {
            guide.pendingPage = page;
        }
        if (!find.isEmpty()) {
            guide.find = find;
        }
        active = guide;
        showActive();
    }

    private void load(OpenGuide guide) {
        File file = GuideLibrary.fileFor(guide.entry);
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                PDDocument pdf = Loader.loadPDF(file);
                try {
                    GuideDocument known = library.document(guide.entry);
                    return new Object[] { pdf, known != null ? known : GuideReader.read(pdf) };
                } catch (IOException | RuntimeException ex) {
                    pdf.close();
                    throw ex;
                }
            }

            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    PDDocument pdf = (PDDocument) result[0];
                    if (!open.contains(guide) || !isDisplayable()) {
                        pdf.close();
                        return;
                    }
                    guide.renderer = new GuideRenderer(pdf);
                    guide.document = (GuideDocument) result[1];
                    library.store(guide.entry, guide.document);
                } catch (Exception ex) {
                    guide.error = file.getPath();
                    LOGGER.error(ex, "Unable to open the guide {}", file);
                }
                if (guide == active) {
                    showActive();
                }
            }
        }.execute();
    }

    private void closeGuideTab(OpenGuide guide) {
        int index = open.indexOf(guide);
        if (index < 0) {
            return;
        }
        open.remove(index);
        closeRenderer(guide);
        if (guide == active) {
            active = open.isEmpty() ? null : open.get(Math.min(index, open.size() - 1));
        }
        showActive();
    }

    private static void closeRenderer(OpenGuide guide) {
        if (guide.renderer != null) {
            guide.renderer.close();
            guide.renderer = null;
        }
    }

    private void switchTo(OpenGuide guide) {
        if (guide != active) {
            rememberView();
            active = guide;
            showActive();
        }
    }

    /** Notes where the player is in the guide being read, to return there when they come back to its tab. */
    private void rememberView() {
        if (active != null && active.isReady()) {
            active.pendingPage = active.page;
            active.find = findField.getText();
        }
    }

    private void refreshTabs() {
        tabStrip.removeAll();
        for (OpenGuide guide : open) {
            tabStrip.add(new GuideTab(guide, guide == active));
        }
        tabStrip.revalidate();
        tabStrip.repaint();
    }

    // endregion Tabs

    // region Guide

    /** Shows the active guide: its header, its pages at the remembered page, its find and its side panel. */
    private void showActive() {
        refreshTabs();
        refreshLibrary();
        pageViews.clear();
        thumbnails.clear();
        outlineRows.clear();
        pagesColumn.removeAll();

        OpenGuide guide = active;
        if (guide == null) {
            titleLabel.setText(upper("GuideViewer.empty.title"));
            subtitleLabel.setText(getTextAt(RESOURCE_BUNDLE, "GuideViewer.empty.subtitle"));
            pagesColumn.add(centred(Hud.notice(getTextAt(RESOURCE_BUNDLE, "GuideViewer.empty.notice"))));
        } else {
            titleLabel.setText(guide.entry.getTitle().toUpperCase(Locale.ROOT));
            if (guide.error != null) {
                subtitleLabel.setText(guide.error);
                pagesColumn.add(centred(Hud.notice(getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.error",
                      guide.error))));
            } else if (!guide.isReady()) {
                subtitleLabel.setText(versionLine(guide));
                pagesColumn.add(centred(Hud.notice(getTextAt(RESOURCE_BUNDLE, "GuideViewer.loading"))));
            } else {
                subtitleLabel.setText(versionLine(guide) + "  \u00B7  " + getFormattedTextAt(RESOURCE_BUNDLE,
                      "GuideViewer.pages", guide.document.pageCount()) + "  \u00B7  "
                                            + GuideLibrary.fileFor(guide.entry).getName());
                buildPages(guide);
            }
        }
        pagesColumn.revalidate();
        pagesColumn.repaint();

        boolean isReady = guide != null && guide.isReady();
        findField.setText(guide == null ? "" : guide.find);
        findTimer.stop();
        findField.setEnabled(isReady);
        for (HudButton button : List.of(saveButton, externalButton)) {
            button.setArmed(guide != null && guide.error == null);
        }
        closeTabButton.setArmed(guide != null);
        runFind(false);
        rebuildSide();
        refreshZoomControls();
        refreshPageControls();

        if (isReady) {
            int page = guide.pendingPage;
            isRestoringView = true;
            SwingUtilities.invokeLater(() -> {
                canvasScroll.validate();
                goToPage(page);
                isRestoringView = false;
                onScrolled();
            });
        }
    }

    private void buildPages(OpenGuide guide) {
        guide.zoom = fittedZoom(guide);
        List<PageSize> sizes = guide.document.pageSizes();
        for (int index = 0; index < sizes.size(); index++) {
            GuidePageView view = new GuidePageView(guide.renderer, index, sizes.get(index), guide.zoom, false,
                  count -> getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.page.matches", count));
            view.setAlignmentX(Component.CENTER_ALIGNMENT);
            pageViews.add(view);
            pagesColumn.add(view);
            JLabel number = new JLabel(Integer.toString(index + 1));
            number.setForeground(TEXT_FAINT);
            number.setFont(hudFont(Font.PLAIN, 0.76f, 0.06f));
            number.setAlignmentX(Component.CENTER_ALIGNMENT);
            number.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), 0, scaleForGUI(PAGE_GAP - 8), 0));
            pagesColumn.add(number);
        }
    }

    private String versionLine(OpenGuide guide) {
        return GlossaryConsole.versionLine(guide.entry.getVersionAddedOrLastUpdated(),
              GlossaryChange.of(guide.entry.getVersionAddedOrLastUpdated()));
    }

    /** The zoom the guide's fit asks for, at the canvas's present size; a custom zoom is kept as it is. */
    private float fittedZoom(OpenGuide guide) {
        if (guide.fit == Fit.CUSTOM || guide.document == null || guide.document.pageCount() == 0) {
            return guide.zoom;
        }
        Dimension viewport = canvasScroll.getViewport().getExtentSize();
        if (viewport.width <= 0) {
            viewport = scaleForGUI(700, 600);
        }
        float widest = 1;
        for (PageSize size : guide.document.pageSizes()) {
            widest = Math.max(widest, size.width());
        }
        int scrollBar = canvasScroll.getVerticalScrollBar().getPreferredSize().width;
        float available = viewport.width - (scaleForGUI(CANVAS_MARGIN) * 2f) - scrollBar;
        float zoom = available / widest;
        if (guide.fit == Fit.PAGE) {
            PageSize first = guide.document.pageSizes().get(0);
            float tall = (viewport.height - (scaleForGUI(PAGE_GAP) * 2f)) / first.height();
            zoom = Math.min(zoom, tall);
        }
        return clampZoom(zoom);
    }

    private static float clampZoom(float zoom) {
        return Math.max(MINIMUM_ZOOM, Math.min(MAXIMUM_ZOOM, zoom));
    }

    private void setFit(Fit fit) {
        if (active == null || !active.isReady()) {
            return;
        }
        active.fit = fit;
        applyZoom(fittedZoom(active));
    }

    private void zoomBy(float factor) {
        if (active == null || !active.isReady()) {
            return;
        }
        active.fit = Fit.CUSTOM;
        applyZoom(clampZoom(active.zoom * factor));
    }

    /** Re-fits the pages when the window changes size, unless the player chose a zoom of their own. */
    private void refit() {
        if (active != null && active.isReady() && active.fit != Fit.CUSTOM) {
            float zoom = fittedZoom(active);
            if (Math.abs(zoom - active.zoom) > 0.001f) {
                applyZoom(zoom);
            }
        }
    }

    /** Resizes every page to a zoom, keeping the page being read in view. */
    private void applyZoom(float zoom) {
        OpenGuide guide = active;
        if (guide == null) {
            return;
        }
        int page = guide.page;
        guide.zoom = zoom;
        pageViews.forEach(view -> view.setZoom(zoom));
        refreshZoomControls();
        isRestoringView = true;
        SwingUtilities.invokeLater(() -> {
            canvasScroll.validate();
            goToPage(page);
            isRestoringView = false;
            onScrolled();
        });
    }

    private void refreshZoomControls() {
        boolean isReady = active != null && active.isReady();
        int percent = isReady ? Math.round(active.zoom / ACTUAL_SIZE * 100) : 0;
        zoomLabel.setText(isReady ? percent + "%" : "");
        zoomTile.setValue(isReady ? percent + "%" : "\u2013", TEXT);
        zoomTile.setSub(isReady ? getTextAt(RESOURCE_BUNDLE, "GuideViewer.zoom." + active.fit.name()) : "");
        fitWidthChip.setActive(isReady && active.fit == Fit.WIDTH);
        fitPageChip.setActive(isReady && active.fit == Fit.PAGE);
        zoomInButton.setArmed(isReady && active.zoom < MAXIMUM_ZOOM);
        zoomOutButton.setArmed(isReady && active.zoom > MINIMUM_ZOOM);
    }

    private void stepPage(int step) {
        if (active != null && active.isReady()) {
            goToPage(active.page + step);
        }
    }

    /** Scrolls a page's top into view. */
    private void goToPage(int page) {
        if (active == null || !active.isReady() || pageViews.isEmpty()) {
            return;
        }
        int index = Math.max(1, Math.min(pageViews.size(), page)) - 1;
        Rectangle bounds = pageViews.get(index).getBounds();
        JViewport viewport = canvasScroll.getViewport();
        int maxY = Math.max(0, pagesColumn.getHeight() - viewport.getExtentSize().height);
        int y = Math.max(0, Math.min(maxY, bounds.y - scaleForGUI(PAGE_GAP)));
        viewport.setViewPosition(new Point(viewport.getViewPosition().x, y));
        setPage(index + 1);
    }

    /** Works out which page is being read from how far the guide is scrolled. */
    private void onScrolled() {
        if (isRestoringView || active == null || !active.isReady() || pageViews.isEmpty()) {
            return;
        }
        Rectangle visible = canvasScroll.getViewport().getViewRect();
        int line = visible.y + (visible.height * 3 / 10);
        int page = 1;
        for (GuidePageView view : pageViews) {
            if (view.getY() <= line) {
                page = view.pageIndex() + 1;
            }
        }
        if (page != active.page) {
            setPage(page);
        }
    }

    private void setPage(int page) {
        if (active == null) {
            return;
        }
        active.page = page;
        refreshPageControls();
        // Pages far from the one being read let their images go; they are drawn again if they come back.
        for (GuidePageView view : pageViews) {
            if (Math.abs(view.pageIndex() + 1 - page) > KEPT_PAGES) {
                view.dropImage();
            }
        }
        thumbnails.forEach(thumbnail -> thumbnail.setCurrent(thumbnail.pageIndex() + 1 == page));
        refreshOutlineSelection();
    }

    private void refreshPageControls() {
        boolean isReady = active != null && active.isReady();
        int count = isReady ? active.document.pageCount() : 0;
        int page = isReady ? active.page : 0;
        if (!pageField.isFocusOwner()) {
            pageField.setText(isReady ? Integer.toString(page) : "");
        }
        pageField.setEnabled(isReady);
        pageOfLabel.setText(isReady ? getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.page.of", count) : "");
        previousButton.setArmed(isReady && page > 1);
        nextButton.setArmed(isReady && page < count);
        OutlineItem section = isReady ? active.document.sectionAt(page) : null;
        pageTile.setValue(isReady ? page + " / " + count : "\u2013", TEXT);
        pageTile.setSub(section != null ? shorten(section.title(), TILE_TEXT_LENGTH)
                              : (isReady ? getTextAt(RESOURCE_BUNDLE, "GuideViewer.page.noOutline") : ""));
        if (active == null) {
            footerSummary.setText("");
        } else if (!isReady) {
            footerSummary.setText(active.entry.getTitle());
        } else {
            footerSummary.setText(getFormattedTextAt(RESOURCE_BUNDLE, section == null ? "GuideViewer.footer.summary"
                  : "GuideViewer.footer.summary.section", active.entry.getTitle(), page, count,
                  section == null ? "" : section.title()));
        }
    }

    // endregion Guide

    // region Find

    private void runFind(boolean jump) {
        OpenGuide guide = active;
        if (guide == null || !guide.isReady()) {
            refreshFindControls();
            return;
        }
        guide.find = findField.getText();
        guide.hits = GuideSearch.find(guide.document.pageTexts(), guide.find);
        guide.hitIndex = -1;
        if (!guide.hits.isEmpty()) {
            guide.hitIndex = 0;
            for (int i = 0; i < guide.hits.size(); i++) {
                if (guide.hits.get(i).page() >= guide.page) {
                    guide.hitIndex = i;
                    break;
                }
            }
        }

        Map<Integer, Integer> counts = matchCounts(guide);
        pageViews.forEach(view -> view.setMatches(counts.getOrDefault(view.pageIndex() + 1, 0)));
        thumbnails.forEach(view -> view.setMatches(counts.getOrDefault(view.pageIndex() + 1, 0)));
        refreshFindControls();
        rebuildMatches();
        if (jump && guide.hitIndex >= 0) {
            showSideTab(SideTab.MATCHES);
            goToPage(guide.hits.get(guide.hitIndex).page());
        }
    }

    private static Map<Integer, Integer> matchCounts(OpenGuide guide) {
        Map<Integer, Integer> counts = new TreeMap<>();
        guide.hits.forEach(hit -> counts.merge(hit.page(), 1, Integer::sum));
        return counts;
    }

    private void stepFind(int step) {
        OpenGuide guide = active;
        if (guide == null || guide.hits.isEmpty()) {
            return;
        }
        guide.hitIndex = Math.floorMod(guide.hitIndex + step, guide.hits.size());
        goToPage(guide.hits.get(guide.hitIndex).page());
        refreshFindControls();
        rebuildMatches();
    }

    private void refreshFindControls() {
        OpenGuide guide = active;
        boolean hasQuery = guide != null && guide.isReady() && !GuideSearch.normalise(findField.getText()).isEmpty();
        int count = hasQuery ? guide.hits.size() : 0;
        findCount.setText(!hasQuery ? "" : count == 0 ? getTextAt(RESOURCE_BUNDLE, "GuideViewer.find.none")
                                             : getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.find.position",
                                                   guide.hitIndex + 1, count));
        findPreviousButton.setArmed(count > 1);
        findNextButton.setArmed(count > 1);
        findTile.setValue(hasQuery ? Integer.toString(count) : "\u2013", count > 0 ? AMBER : TEXT_FAINT);
        findTile.setSub(hasQuery ? getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.find.pages",
              matchCounts(guide).size()) : getTextAt(RESOURCE_BUNDLE, "GuideViewer.find.nothing"));
        sideTabs.setLabel(SideTab.MATCHES.ordinal(), count > 0
                                                          ? getFormattedTextAt(RESOURCE_BUNDLE,
              "GuideViewer.side.MATCHES.count", count) : text("GuideViewer.side.MATCHES"));
    }

    // endregion Find

    // region Library

    private void refreshLibrary() {
        String query = GuideSearch.normalise(librarySearch.getText());
        LibraryRow selected = libraryList.getSelectedValue();
        libraryModel.clear();
        if (query.isEmpty()) {
            libraryModel.addElement(new Heading(getTextAt(RESOURCE_BUNDLE, "GuideViewer.library.all"), false));
            for (DocumentationEntry entry : GuideLibrary.entries()) {
                libraryModel.addElement(new GuideRow(entry, null));
            }
            libraryCount.setText(getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.library.count",
                  DocumentationEntry.values().length) + readingNote());
        } else {
            LibraryResult result = GuideSearch.searchLibrary(library.searchable(), query,
                  searchInsideBox.isSelected());
            if (!result.titleMatches().isEmpty()) {
                libraryModel.addElement(new Heading(getFormattedTextAt(RESOURCE_BUNDLE,
                      "GuideViewer.library.titleMatches", result.titleMatches().size()), false));
                result.titleMatches().forEach(guide -> libraryModel.addElement(
                      new GuideRow(entryFor(guide.key()), query)));
            }
            if (!result.pageMatches().isEmpty()) {
                libraryModel.addElement(new Heading(getFormattedTextAt(RESOURCE_BUNDLE,
                      "GuideViewer.library.pageMatches", result.pageMatches().size()), false));
                for (LibraryHit hit : result.pageMatches().subList(0,
                      Math.min(MAXIMUM_LIBRARY_PAGES, result.pageMatches().size()))) {
                    libraryModel.addElement(new PageRow(entryFor(hit.guide().key()), hit.hit(),
                          hit.guide().pageTexts().get(hit.hit().page() - 1), query));
                }
            }
            if (result.titleMatches().isEmpty() && result.pageMatches().isEmpty()) {
                libraryModel.addElement(new Heading(getFormattedTextAt(RESOURCE_BUNDLE,
                      "GuideViewer.library.noMatches", librarySearch.getText().strip()), true));
            }
            long guides = result.pageMatches().stream().map(hit -> hit.guide().key()).distinct().count();
            libraryCount.setText(getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.library.found",
                  result.titleMatches().size() + guides, result.pageMatches().size()) + readingNote());
        }
        if (selected != null && libraryModel.contains(selected)) {
            libraryList.setSelectedValue(selected, false);
        }
    }

    private String readingNote() {
        if (library.isComplete() || !searchInsideBox.isSelected()) {
            return "";
        }
        return getTextAt(RESOURCE_BUNDLE, "GuideViewer.library.reading");
    }

    /** Refreshes the library's search as each guide is read in the background. */
    private void onLibraryRead() {
        if (!GuideSearch.normalise(librarySearch.getText()).isEmpty() || library.isComplete()) {
            refreshLibrary();
        } else {
            libraryCount.setText(getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.library.count",
                  DocumentationEntry.values().length) + readingNote());
        }
    }

    private static DocumentationEntry entryFor(String key) {
        DocumentationEntry entry = DocumentationEntry.getDocumentationEntryFromLookUpName(key);
        if (entry == null) {
            throw new IllegalStateException("Unknown guide " + key);
        }
        return entry;
    }

    private void chooseLibraryRow(LibraryRow row, boolean newTab) {
        if (row instanceof GuideRow guideRow) {
            openGuide(guideRow.entry(), 0, "", newTab);
        } else if (row instanceof PageRow pageRow) {
            openGuide(pageRow.entry(), pageRow.hit().page(), librarySearch.getText().strip(), newTab);
        }
    }

    private void focusLibraryList() {
        for (int i = 0; i < libraryModel.size(); i++) {
            if (!(libraryModel.get(i) instanceof Heading)) {
                libraryList.requestFocusInWindow();
                libraryList.setSelectedIndex(i);
                libraryList.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    // endregion Library

    // region Side panel

    private void showSideTab(SideTab tab) {
        sideTab = tab;
        sideTabs.setSelected(tab.ordinal());
        sideCards.show(sideBody, tab.name());
        if (tab == SideTab.PAGES && thumbnails.isEmpty()) {
            rebuildThumbnails();
        }
    }

    private void rebuildSide() {
        rebuildOutline();
        thumbnails.clear();
        sideColumns.get(SideTab.PAGES).removeAll();
        if (sideTab == SideTab.PAGES) {
            rebuildThumbnails();
        }
        rebuildMatches();
        rebuildRelated();
        for (JPanel columnPanel : sideColumns.values()) {
            columnPanel.revalidate();
            columnPanel.repaint();
        }
    }

    private void rebuildOutline() {
        JPanel outline = sideColumns.get(SideTab.OUTLINE);
        outline.removeAll();
        outlineRows.clear();
        if (active == null || !active.isReady()) {
            return;
        }
        if (active.document.outline().isEmpty()) {
            outline.add(leftAligned(sideNote(getTextAt(RESOURCE_BUNDLE, "GuideViewer.outline.none"))));
            return;
        }
        for (OutlineItem item : active.document.outline()) {
            SideRow row = new SideRow(escape(item.title()), item.page() > 0 ? Integer.toString(item.page()) : "",
                  item.depth(), () -> goToPage(item.page()));
            row.item = item;
            outlineRows.add(row);
            outline.add(leftAligned(row));
        }
        refreshOutlineSelection();
    }

    private void refreshOutlineSelection() {
        OutlineItem section = active != null && active.isReady() ? active.document.sectionAt(active.page) : null;
        for (SideRow row : outlineRows) {
            boolean isCurrent = row.item == section;
            row.setCurrent(isCurrent);
            if (isCurrent && sideTab == SideTab.OUTLINE) {
                SwingUtilities.invokeLater(() -> row.scrollRectToVisible(new Rectangle(0, 0, 1, row.getHeight())));
            }
        }
    }

    private void rebuildThumbnails() {
        JPanel pages = sideColumns.get(SideTab.PAGES);
        pages.removeAll();
        thumbnails.clear();
        if (active == null || !active.isReady()) {
            return;
        }
        JPanel grid = Hud.transparentPanel(new GridLayout(0, 2, scaleForGUI(12), scaleForGUI(12)));
        grid.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(14), 0, scaleForGUI(14)));
        Map<Integer, Integer> counts = matchCounts(active);
        List<PageSize> sizes = active.document.pageSizes();
        for (int index = 0; index < sizes.size(); index++) {
            PageSize size = sizes.get(index);
            float zoom = scaleForGUI(THUMBNAIL_WIDTH) / Math.max(1, size.width());
            GuidePageView thumbnail = new GuidePageView(active.renderer, index, size, zoom, true,
                  Integer::toString);
            thumbnail.setMatches(counts.getOrDefault(index + 1, 0));
            thumbnail.setCurrent(index + 1 == active.page);
            thumbnail.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            thumbnail.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.thumbnail.toolTipText",
                  index + 1));
            int page = index + 1;
            thumbnail.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    goToPage(page);
                }
            });
            thumbnails.add(thumbnail);
            JPanel cell = Hud.transparentPanel(new BorderLayout(0, scaleForGUI(3)));
            JPanel holder = Hud.transparentPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            holder.add(thumbnail);
            cell.add(holder, BorderLayout.CENTER);
            JLabel number = new JLabel(Integer.toString(page), SwingConstants.CENTER);
            number.setForeground(TEXT_FAINT);
            number.setFont(hudFont(Font.PLAIN, 0.74f, 0.04f));
            cell.add(number, BorderLayout.SOUTH);
            grid.add(cell);
        }
        pages.add(leftAligned(grid));
        pages.revalidate();
        pages.repaint();
    }

    private void rebuildMatches() {
        JPanel matches = sideColumns.get(SideTab.MATCHES);
        matches.removeAll();
        OpenGuide guide = active;
        if (guide == null || !guide.isReady()) {
            return;
        }
        if (guide.hits.isEmpty()) {
            matches.add(leftAligned(sideNote(getTextAt(RESOURCE_BUNDLE,
                  GuideSearch.normalise(findField.getText()).isEmpty() ? "GuideViewer.matches.empty"
                        : "GuideViewer.matches.none"))));
        } else {
            for (int i = 0; i < Math.min(MAXIMUM_LISTED_MATCHES, guide.hits.size()); i++) {
                PageHit hit = guide.hits.get(i);
                String[] passage = GuideSearch.passage(guide.document.pageTexts().get(hit.page() - 1), hit,
                      PASSAGE_BEFORE, PASSAGE_AFTER);
                String html = "<font color='" + hex(TEXT_FAINT) + "'><b>"
                                    + escape(getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.matches.page",
                      hit.page()).toUpperCase(Locale.ROOT)) + "</b></font><br>" + markedPassage(passage);
                int index = i;
                SideRow row = new SideRow(html, "", 0, () -> {
                    guide.hitIndex = index;
                    goToPage(hit.page());
                    refreshFindControls();
                    rebuildMatches();
                });
                row.setCurrent(i == guide.hitIndex);
                row.useAmberMark();
                matches.add(leftAligned(row));
                if (i == guide.hitIndex && sideTab == SideTab.MATCHES) {
                    SwingUtilities.invokeLater(() -> row.scrollRectToVisible(
                          new Rectangle(0, 0, 1, row.getHeight())));
                }
            }
        }
        matches.revalidate();
        matches.repaint();
    }

    private void rebuildRelated() {
        JPanel related = sideColumns.get(SideTab.RELATED);
        related.removeAll();
        if (active == null) {
            return;
        }
        related.add(leftAligned(sideHeading(getTextAt(RESOURCE_BUNDLE, "GuideViewer.related.glossary"))));
        if (glossaryIndex == null) {
            glossaryIndex = GlossaryIndex.build();
        }
        List<GlossaryEntry> entries = glossaryIndex.items().stream()
                                            .filter(item -> item.documents().contains(active.entry))
                                            .map(GlossaryIndex.Item::entry)
                                            .toList();
        if (entries.isEmpty()) {
            related.add(leftAligned(sideNote(getTextAt(RESOURCE_BUNDLE, "GuideViewer.related.none"))));
        }
        for (GlossaryEntry entry : entries) {
            related.add(leftAligned(new SideRow(escape(entry.getTitle()), "", 0,
                  () -> GlossaryConsole.showEntry(this, entry))));
        }
        related.add(Box.createVerticalStrut(scaleForGUI(10)));
        related.add(leftAligned(sideHeading(getTextAt(RESOURCE_BUNDLE, "GuideViewer.related.record"))));
        related.add(leftAligned(sideNote(versionLine(active))));
        if (active.isReady()) {
            related.add(leftAligned(sideNote(getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.pages",
                  active.document.pageCount()))));
        }
        related.add(leftAligned(sideNote(GuideLibrary.fileFor(active.entry).getName())));
    }

    private static String markedPassage(String[] passage) {
        return escape(passage[0]) + "<b><font color='" + hex(AMBER) + "'>" + escape(passage[1]) + "</font></b>"
                     + escape(passage[2]);
    }

    private static JComponent sideHeading(String text) {
        JComponent heading = Hud.sectionHeading(text);
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(14), scaleForGUI(6),
              scaleForGUI(14)));
        holder.add(heading, BorderLayout.CENTER);
        return holder;
    }

    private static JComponent sideNote(String text) {
        JLabel label = Hud.hint("<html><div style='width:" + scaleForGUI(230) + "px'>" + escape(text)
                                      + "</div></html>");
        label.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(14), scaleForGUI(3),
              scaleForGUI(14)));
        return label;
    }

    // endregion Side panel

    // region Actions

    private void openHowTo() {
        guidePreferences.setViewerGuideSeen(true);
        guideDrawer.open(GlossaryGuides.buildViewer());
    }

    private void closeGuide() {
        guideDrawer.close();
    }

    private void saveCopy() {
        if (active == null || active.error != null) {
            return;
        }
        File source = GuideLibrary.fileFor(active.entry);
        String name = source.getName().replaceFirst("(?i)\\.pdf$", "");
        Optional<File> target = FileDialogs.saveGuideCopy(GlossaryConsole.frameAbove(this), name);
        if (target.isEmpty()) {
            return;
        }
        try {
            Files.copy(source.toPath(), target.get().toPath(), StandardCopyOption.REPLACE_EXISTING);
            footerSummary.setText(getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.saved", target.get().getName()));
        } catch (IOException ex) {
            LOGGER.error(ex, "Unable to save a copy of {} to {}", source, target.get());
            JOptionPane.showMessageDialog(this, getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.saveFailed",
                  target.get().getName()), getTextAt(RESOURCE_BUNDLE, "GuideViewer.saveFailed.title"),
                  JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openExternally() {
        if (active == null || active.error != null) {
            return;
        }
        File file = GuideLibrary.fileFor(active.entry).getAbsoluteFile();
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            showExternalFailure(file);
            return;
        }
        // Handing a file to the system's reader can take a moment, so it is done off the Swing thread.
        Thread opener = new Thread(() -> {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException | RuntimeException ex) {
                LOGGER.warn(ex, "Unable to open {} externally", file);
                SwingUtilities.invokeLater(() -> showExternalFailure(file));
            }
        }, "Guide opener");
        opener.setDaemon(true);
        opener.start();
    }

    private void showExternalFailure(File file) {
        JOptionPane.showMessageDialog(this, getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.externalFailed",
              file.getPath()), getTextAt(RESOURCE_BUNDLE, "GuideViewer.externalFailed.title"),
              JOptionPane.WARNING_MESSAGE);
    }

    // endregion Actions

    // region Helpers

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    private static String upper(String key) {
        return getTextAt(RESOURCE_BUNDLE, key).toUpperCase(Locale.ROOT);
    }

    private static String escape(String text) {
        return GlossaryConsole.escape(text);
    }

    private static String shorten(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length - 1).strip() + "\u2026";
    }

    private static JPanel column() {
        JPanel column = Hud.transparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        return column;
    }

    private static JComponent centred(JComponent component) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        component.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(60), 0, 0, 0));
        return component;
    }

    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(GuideViewer.class);
            setName("GuideViewer");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed to set user preferences");
        }
    }

    // endregion Helpers

    /** An open guide's tab: its title, and a cross that closes it. */
    private final class GuideTab extends JPanel {
        private final boolean isSelected;
        private boolean isHovered;

        private GuideTab(OpenGuide guide, boolean isSelected) {
            super(new BorderLayout(scaleForGUI(8), 0));
            this.isSelected = isSelected;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(scaleForGUI(7), scaleForGUI(14), scaleForGUI(7), scaleForGUI(8)));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(guide.entry.getTitle());

            JLabel title = new JLabel(guide.entry.getTitle());
            title.setForeground(isSelected ? ACCENT_BRIGHT : TEXT_MUTED);
            title.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
            Dimension size = title.getPreferredSize();
            title.setPreferredSize(new Dimension(Math.min(size.width, scaleForGUI(230)), size.height));
            add(title, BorderLayout.CENTER);

            JLabel close = new JLabel("\u00D7");
            close.setForeground(TEXT_FAINT);
            close.setFont(hudFont(Font.PLAIN, 1.05f, 0.0f));
            close.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.tab.close.toolTipText",
                  guide.entry.getTitle()));
            close.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    if (close.contains(event.getPoint())) {
                        closeGuideTab(guide);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent event) {
                    close.setForeground(TEXT);
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    close.setForeground(TEXT_FAINT);
                }
            });
            add(close, BorderLayout.EAST);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    if (contains(event.getPoint())) {
                        // A middle click closes a tab, as it does in a browser.
                        if (SwingUtilities.isMiddleMouseButton(event)) {
                            closeGuideTab(guide);
                        } else {
                            switchTo(guide);
                        }
                    }
                }

                @Override
                public void mouseEntered(MouseEvent event) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            graphics.setColor(isSelected ? SURFACE_HIGHLIGHT : (isHovered ? SURFACE : SURFACE_DEEP));
            graphics.fillRect(0, 0, getWidth(), getHeight());
            graphics.setColor(BORDER);
            graphics.drawLine(0, 0, 0, getHeight());
            graphics.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            graphics.drawLine(0, 0, getWidth(), 0);
            if (isSelected) {
                graphics.setColor(ACCENT);
                graphics.fillRect(0, 0, getWidth(), scaleForGUI(2));
            }
        }
    }

    /** A clickable row of the side panel: an outline heading, a match, or a related entry. */
    private static final class SideRow extends JPanel {
        private final JLabel label = new JLabel();
        private @Nullable OutlineItem item;
        private boolean isCurrent;
        private boolean isHovered;
        private Color mark = ACCENT;

        private SideRow(String html, String trailing, int depth, Runnable onChoose) {
            super(new BorderLayout(scaleForGUI(8), 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(12 + (depth * 14)), scaleForGUI(5),
                  scaleForGUI(12)));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            int width = scaleForGUI(220 - (depth * 14));
            label.setText("<html><div style='width:" + width + "px'>" + html + "</div></html>");
            label.setFont(hudFont(Font.PLAIN, depth == 0 ? 0.88f : 0.84f, 0.0f));
            label.setForeground(depth == 0 ? TEXT_MUTED : TEXT_FAINT);
            add(label, BorderLayout.CENTER);
            if (!trailing.isEmpty()) {
                JLabel page = new JLabel(trailing);
                page.setForeground(TEXT_FAINT);
                page.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));
                page.setVerticalAlignment(SwingConstants.TOP);
                add(page, BorderLayout.EAST);
            }
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    if (contains(event.getPoint())) {
                        onChoose.run();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent event) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        private void setCurrent(boolean isCurrent) {
            this.isCurrent = isCurrent;
            label.setForeground(isCurrent ? ACCENT_BRIGHT : TEXT_MUTED);
            repaint();
        }

        /** Marks the current row in amber rather than cyan, as the find box's matches are. */
        private void useAmberMark() {
            mark = AMBER;
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (isCurrent || isHovered) {
                graphics.setColor(isCurrent ? SURFACE_HIGHLIGHT : SURFACE);
                graphics.fillRect(0, 0, getWidth(), getHeight());
            }
            if (isCurrent) {
                graphics.setColor(mark);
                graphics.fillRect(0, 0, scaleForGUI(2), getHeight());
            }
        }
    }

    /** Draws the library's rows: headings, guides, and the pages a search found. */
    private final class LibraryRenderer implements ListCellRenderer<LibraryRow> {
        private final JLabel label = new JLabel();

        private LibraryRenderer() {
            label.setOpaque(true);
            label.setVerticalAlignment(SwingConstants.TOP);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends LibraryRow> source, LibraryRow row, int index,
              boolean isSelected, boolean cellHasFocus) {
            int width = Math.max(scaleForGUI(120), source.getWidth() - scaleForGUI(40));
            label.setBackground(isSelected ? SURFACE_HIGHLIGHT : SURFACE_DEEP);
            label.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
            label.setForeground(TEXT);

            if (row instanceof Heading heading) {
                label.setBackground(SURFACE_DEEP);
                label.setFont(heading.isNotice() ? hudFont(Font.PLAIN, 0.88f, 0.0f) : hudFont(Font.BOLD, 0.7f, 0.14f));
                label.setForeground(heading.isNotice() ? TEXT_FAINT : ACCENT);
                label.setText(heading.isNotice() ? "<html><div style='width:" + width + "px'>"
                                                         + escape(heading.label()) + "</div></html>"
                                    : heading.label().toUpperCase(Locale.ROOT));
                label.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(heading.isNotice() ? 14 : 10),
                      scaleForGUI(12), scaleForGUI(4), scaleForGUI(10)));
                return label;
            }

            DocumentationEntry entry = row instanceof GuideRow guideRow ? guideRow.entry() : ((PageRow) row).entry();
            boolean isOpen = open.stream().anyMatch(guide -> guide.entry == entry);
            boolean isActive = active != null && active.entry == entry;
            label.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(0, scaleForGUI(2), 0, 0, isActive ? ACCENT : SURFACE_DEEP),
                  BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(10), scaleForGUI(5), scaleForGUI(8))));
            StringBuilder html = new StringBuilder("<html><div style='width:").append(width).append("px'>");
            if (isOpen) {
                html.append("<font color='").append(hex(ACCENT)).append("'>\u25CF</font> ");
            }
            String query = row instanceof GuideRow guideRow ? guideRow.query() : ((PageRow) row).query();
            html.append(marked(entry.getTitle(), query, isActive ? ACCENT_BRIGHT : TEXT));
            html.append("<br><font color='").append(hex(TEXT_FAINT)).append("'>");
            if (row instanceof PageRow pageRow) {
                html.append(escape(getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.matches.page",
                      pageRow.hit().page()))).append("</font><br><font color='").append(hex(TEXT_MUTED))
                      .append("'>").append(markedPassage(GuideSearch.passage(pageRow.text(), pageRow.hit(),
                            PASSAGE_BEFORE, PASSAGE_AFTER)));
            } else {
                GuideDocument document = library.document(entry);
                String pages = document == null ? "" : getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.pages",
                      document.pageCount()) + "  \u00B7  ";
                html.append(escape(pages + versionLine(entry)));
            }
            html.append("</font></div></html>");
            label.setText(html.toString());
            return label;
        }

        private String versionLine(DocumentationEntry entry) {
            return GlossaryConsole.versionLine(entry.getVersionAddedOrLastUpdated(),
                  GlossaryChange.of(entry.getVersionAddedOrLastUpdated()));
        }

        /** Escapes text and marks the search's words in it. */
        private String marked(String text, @Nullable String query, Color color) {
            String start = "<font color='" + hex(color) + "'>";
            if (query == null || query.isEmpty()) {
                return start + escape(text) + "</font>";
            }
            String lower = GuideSearch.lower(text);
            StringBuilder result = new StringBuilder(start);
            int last = 0;
            int at = lower.indexOf(query);
            while (at >= 0) {
                result.append(escape(text.substring(last, at))).append("<b><font color='").append(hex(AMBER))
                      .append("'>").append(escape(text.substring(at, at + query.length()))).append("</font></b>");
                last = at + query.length();
                at = lower.indexOf(query, last);
            }
            return result.append(escape(text.substring(last))).append("</font>").toString();
        }
    }
}
