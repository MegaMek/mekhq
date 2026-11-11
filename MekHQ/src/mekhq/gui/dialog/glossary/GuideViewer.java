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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.dialog.glossary.GuideDocument.OutlineItem;
import mekhq.gui.dialog.glossary.GuideDocument.PageSize;
import mekhq.gui.dialog.glossary.GuideSearch.PageHit;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Reads one of MekHQ's guides, in the heads-up-display style of the glossary console.
 *
 * <p>The guide scrolls as one run of pages, drawn in the background so scrolling never waits, with page and zoom
 * controls and a find box above it. Tiles across the top show the page being read and the section it is in, the zoom,
 * and how many times the find box's words appear; pages with matches are outlined.</p>
 *
 * <p>The viewer shows one guide at a time. Opening another guide while it is open shows that guide in its place.</p>
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
    /** Pages further than this from the one being read let their images go. */
    private static final int KEPT_PAGES = 8;
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

    private final GlossaryGuidePreferences guidePreferences = GlossaryGuidePreferences.getInstance();

    private @Nullable DocumentationEntry entry;
    private @Nullable GuideRenderer renderer;
    private @Nullable GuideDocument document;
    private @Nullable String error;
    private int page = 1;
    private float zoom = ACTUAL_SIZE;
    private Fit fit = Fit.WIDTH;
    private List<PageHit> hits = List.of();
    private int hitIndex = -1;
    /** Counts the guides opened, so a guide still loading when another is opened is let go when it arrives. */
    private int loadCount;
    private final List<GuidePageView> pageViews = new ArrayList<>();
    private boolean isRestoringView;

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final HudStatTile pageTile;
    private final HudStatTile zoomTile;
    private final HudStatTile findTile;
    private final HudButton guideButton;
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
    private final JLabel footerSummary = new JLabel();
    private final HudButton saveButton;
    private final HudButton externalButton;
    private final HudCheckBox autoOpenGuides;
    private final HudGuideDrawer guideDrawer;
    private final Timer findTimer;
    private final Timer refitTimer;

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
        openViewer.open(entry, page, find);
    }

    private GuideViewer(@Nullable Window owner) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "GuideViewer.windowTitle"), ModalityType.MODELESS);

        pageTile = new HudStatTile(text("GuideViewer.tile.page"), "");
        zoomTile = new HudStatTile(text("GuideViewer.tile.zoom"), "");
        findTile = new HudStatTile(text("GuideViewer.tile.find"), "");
        guideButton = new HudButton("?  " + upper("GlossaryGuide.viewer.button"), false, true);
        previousButton = new HudButton("\u2039", false, true);
        nextButton = new HudButton("\u203A", false, true);
        zoomOutButton = new HudButton("\u2212", false, true);
        zoomInButton = new HudButton("+", false, true);
        fitWidthChip = new HudChip(text("GuideViewer.zoom.fitWidth"), () -> setFit(Fit.WIDTH));
        fitPageChip = new HudChip(text("GuideViewer.zoom.fitPage"), () -> setFit(Fit.PAGE));
        findPreviousButton = new HudButton("\u2039", false, true);
        findNextButton = new HudButton("\u203A", false, true);
        saveButton = new HudButton(upper("GuideViewer.footer.save"), false);
        externalButton = new HudButton(upper("GuideViewer.footer.external"), false);
        autoOpenGuides = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.autoOpen"));
        autoOpenGuides.setSelected(guidePreferences.isGuideAutoOpen());
        autoOpenGuides.addActionListener(event -> guidePreferences.setGuideAutoOpen(autoOpenGuides.isSelected()));
        guideDrawer = new HudGuideDrawer(List.of(getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.tab.steps"),
              getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.tab.example"),
              getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.tab.terms")), autoOpenGuides,
              getTextAt(RESOURCE_BUNDLE, "GlossaryGuide.close"), this::closeGuide);
        findTimer = new Timer(TYPING_DELAY_MILLIS, event -> runFind(true));
        findTimer.setRepeats(false);
        refitTimer = new Timer(120, event -> refit());
        refitTimer.setRepeats(false);

        buildUI();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(scaleForGUI(760, 560));
        getContentPane().setPreferredSize(scaleForGUI(1040, 840));
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
                findTimer.stop();
                refitTimer.stop();
                closeRenderer();
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
        content.add(buildDocument(), BorderLayout.CENTER);
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
        JPanel commandBar = new JPanel(new BorderLayout(scaleForGUI(24), 0));
        commandBar.setOpaque(true);
        commandBar.setBackground(GROUND);
        commandBar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(12), scaleForGUI(16))));

        JPanel identity = column();
        JLabel eyebrow = new JLabel(upper("GuideViewer.eyebrow"));
        eyebrow.setForeground(TEXT_FAINT);
        eyebrow.setFont(hudFont(Font.BOLD, 0.72f, 0.18f));
        titleLabel.setForeground(ACCENT_BRIGHT);
        titleLabel.setFont(hudFont(Font.BOLD, 1.3f, 0.12f));
        subtitleLabel.setForeground(TEXT_MUTED);
        subtitleLabel.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        guideButton.addActionListener(event -> openHowTo());
        identity.add(leftAligned(eyebrow));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));
        identity.add(leftAligned(titleLabel));
        identity.add(Box.createVerticalStrut(scaleForGUI(4)));
        identity.add(leftAligned(subtitleLabel));
        identity.add(Box.createVerticalStrut(scaleForGUI(10)));
        identity.add(leftAligned(guideButton));

        commandBar.add(identity, BorderLayout.CENTER);
        commandBar.add(Hud.tileRow(pageTile, zoomTile, findTile), BorderLayout.EAST);
        return commandBar;
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
        findField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                findTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                findTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                findTimer.restart();
            }
        });
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
        HudButton close = new HudButton(upper("GuideViewer.footer.close"), false);
        close.addActionListener(event -> dispose());

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(saveButton);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(externalButton);
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
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, menuKey), "zoomIn", () -> zoomBy(ZOOM_STEP));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, menuKey), "zoomInPlus", () -> zoomBy(ZOOM_STEP));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_ADD, menuKey), "zoomInPad", () -> zoomBy(ZOOM_STEP));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, menuKey), "zoomOut", () -> zoomBy(1 / ZOOM_STEP));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, menuKey), "zoomOutPad", () -> zoomBy(1 / ZOOM_STEP));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_0, menuKey), "fitWidth", () -> setFit(Fit.WIDTH));
        for (int key : new int[] { KeyEvent.VK_RIGHT, KeyEvent.VK_PAGE_DOWN }) {
            bindPageKey(root, KeyStroke.getKeyStroke(key, 0), "next" + key, () -> {
                if (!isTyping()) {
                    stepPage(1);
                }
            });
        }
        for (int key : new int[] { KeyEvent.VK_LEFT, KeyEvent.VK_PAGE_UP }) {
            bindPageKey(root, KeyStroke.getKeyStroke(key, 0), "previous" + key, () -> {
                if (!isTyping()) {
                    stepPage(-1);
                }
            });
        }
    }

    /**
     * Binds a page key for the whole window and, as the scroll pane claims the arrow and page keys for scrolling
     * while it or the pages have focus, for the scroll pane too.
     */
    private void bindPageKey(JComponent root, KeyStroke keyStroke, String name, Runnable action) {
        bind(root, keyStroke, name, action);
        canvasScroll.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, name);
        canvasScroll.getActionMap().put(name, root.getActionMap().get(name));
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

    /** Escape closes an open guide drawer, then clears the find box, then closes the viewer. */
    private void onEscape() {
        if (guideDrawer.isVisible()) {
            closeGuide();
        } else if (!findField.getText().isEmpty()) {
            findField.setText("");
            findTimer.stop();
            runFind(false);
        } else {
            dispose();
        }
    }

    // endregion Layout

    // region Guide

    /**
     * Shows a guide, opening it if it is not the one already shown.
     *
     * @param newEntry the guide
     * @param newPage  the page to show, from 1; 0 to keep the place in the guide already shown
     * @param find     the words to put in the find box, or empty to leave the box as it is
     */
    private void open(DocumentationEntry newEntry, int newPage, String find) {
        if (newEntry == entry && error == null) {
            if (!find.isEmpty()) {
                findField.setText(find);
                findTimer.stop();
            }
            if (!isReady()) {
                // Still loading: the page and the find box are taken up once the guide arrives.
                if (newPage > 0) {
                    page = newPage;
                }
                return;
            }
            if (!find.isEmpty()) {
                runFind(newPage <= 0);
            }
            if (newPage > 0) {
                goToPage(newPage);
            }
            return;
        }

        closeRenderer();
        entry = newEntry;
        document = null;
        error = null;
        page = Math.max(1, newPage);
        hits = List.of();
        hitIndex = -1;
        findField.setText(find);
        findTimer.stop();
        showCurrent();
        load(newEntry, ++loadCount);
    }

    private void load(DocumentationEntry loading, int loadNumber) {
        File file = GuideReader.fileFor(loading);
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                PDDocument pdf = Loader.loadPDF(file);
                try {
                    return new Object[] { pdf, GuideReader.read(pdf) };
                } catch (IOException | RuntimeException ex) {
                    pdf.close();
                    throw ex;
                }
            }

            @Override
            protected void done() {
                boolean isCurrent = loadNumber == loadCount && isDisplayable();
                try {
                    Object[] result = get();
                    PDDocument pdf = (PDDocument) result[0];
                    if (!isCurrent) {
                        pdf.close();
                        return;
                    }
                    renderer = new GuideRenderer(pdf);
                    document = (GuideDocument) result[1];
                } catch (Exception ex) {
                    if (!isCurrent) {
                        return;
                    }
                    error = file.getPath();
                    LOGGER.error(ex, "Unable to open the guide {}", file);
                }
                showCurrent();
            }
        }.execute();
    }

    private void closeRenderer() {
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
    }

    private boolean isReady() {
        return renderer != null && document != null;
    }

    /** Shows the guide: its header, and its pages at the page asked for, or why it cannot be shown yet. */
    private void showCurrent() {
        pageViews.clear();
        pagesColumn.removeAll();
        if (entry == null) {
            return;
        }

        titleLabel.setText(entry.getTitle().toUpperCase(Locale.ROOT));
        if (error != null) {
            subtitleLabel.setText(error);
            pagesColumn.add(centred(Hud.notice(getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.error", error))));
        } else if (!isReady()) {
            subtitleLabel.setText(versionLine());
            pagesColumn.add(centred(Hud.notice(getTextAt(RESOURCE_BUNDLE, "GuideViewer.loading"))));
        } else {
            subtitleLabel.setText(versionLine() + "  \u00B7  " + getFormattedTextAt(RESOURCE_BUNDLE,
                  "GuideViewer.pages", document.pageCount()) + "  \u00B7  " + GuideReader.fileFor(entry).getName());
            buildPages();
        }
        pagesColumn.revalidate();
        pagesColumn.repaint();

        findField.setEnabled(isReady());
        saveButton.setArmed(error == null);
        externalButton.setArmed(error == null);
        runFind(false);
        refreshZoomControls();
        refreshPageControls();

        if (isReady()) {
            int target = page;
            isRestoringView = true;
            SwingUtilities.invokeLater(() -> {
                // The scroll pane sizes the pages first, so the page is found where it will be drawn.
                canvasScroll.validate();
                goToPage(target);
                isRestoringView = false;
                onScrolled();
                if (!hits.isEmpty()) {
                    runFind(true);
                }
            });
        }
    }

    private void buildPages() {
        zoom = fittedZoom();
        List<PageSize> sizes = document.pageSizes();
        for (int index = 0; index < sizes.size(); index++) {
            GuidePageView view = new GuidePageView(renderer, index, sizes.get(index), zoom,
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

    private String versionLine() {
        return GlossaryConsole.versionLine(entry.getVersionAddedOrLastUpdated(),
              GlossaryChange.of(entry.getVersionAddedOrLastUpdated()));
    }

    /** The zoom the fit asks for, at the canvas's present size; a zoom of the player's own is kept as it is. */
    private float fittedZoom() {
        if (fit == Fit.CUSTOM || document == null || document.pageCount() == 0) {
            return zoom;
        }
        Dimension viewport = canvasScroll.getViewport().getExtentSize();
        if (viewport.width <= 0) {
            viewport = scaleForGUI(700, 600);
        }
        float widest = 1;
        for (PageSize size : document.pageSizes()) {
            widest = Math.max(widest, size.width());
        }
        int scrollBar = canvasScroll.getVerticalScrollBar().getPreferredSize().width;
        float available = viewport.width - (scaleForGUI(CANVAS_MARGIN) * 2f) - scrollBar;
        float fitted = available / widest;
        if (fit == Fit.PAGE) {
            PageSize first = document.pageSizes().get(0);
            fitted = Math.min(fitted, (viewport.height - (scaleForGUI(PAGE_GAP) * 2f)) / first.height());
        }
        return clampZoom(fitted);
    }

    private static float clampZoom(float zoom) {
        return Math.max(MINIMUM_ZOOM, Math.min(MAXIMUM_ZOOM, zoom));
    }

    private void setFit(Fit newFit) {
        if (isReady()) {
            fit = newFit;
            applyZoom(fittedZoom());
        }
    }

    private void zoomBy(float factor) {
        if (isReady()) {
            fit = Fit.CUSTOM;
            applyZoom(clampZoom(zoom * factor));
        }
    }

    /** Re-fits the pages when the window changes size, unless the player chose a zoom of their own. */
    private void refit() {
        if (isReady() && fit != Fit.CUSTOM) {
            float fitted = fittedZoom();
            if (Math.abs(fitted - zoom) > 0.001f) {
                applyZoom(fitted);
            }
        }
    }

    /** Resizes every page to a zoom, keeping the page being read in view. */
    private void applyZoom(float newZoom) {
        int target = page;
        zoom = newZoom;
        pageViews.forEach(view -> view.setZoom(newZoom));
        refreshZoomControls();
        isRestoringView = true;
        SwingUtilities.invokeLater(() -> {
            canvasScroll.validate();
            goToPage(target);
            isRestoringView = false;
            onScrolled();
        });
    }

    private void refreshZoomControls() {
        boolean isReady = isReady();
        int percent = Math.round(zoom / ACTUAL_SIZE * 100);
        zoomLabel.setText(isReady ? percent + "%" : "");
        zoomTile.setValue(isReady ? percent + "%" : "\u2013", TEXT);
        zoomTile.setSub(isReady ? getTextAt(RESOURCE_BUNDLE, "GuideViewer.zoom." + fit.name()) : "");
        fitWidthChip.setActive(isReady && fit == Fit.WIDTH);
        fitPageChip.setActive(isReady && fit == Fit.PAGE);
        zoomInButton.setArmed(isReady && zoom < MAXIMUM_ZOOM);
        zoomOutButton.setArmed(isReady && zoom > MINIMUM_ZOOM);
    }

    private void stepPage(int step) {
        if (isReady()) {
            goToPage(page + step);
        }
    }

    /** Scrolls a page's top into view. */
    private void goToPage(int target) {
        if (!isReady() || pageViews.isEmpty()) {
            return;
        }
        int index = Math.max(1, Math.min(pageViews.size(), target)) - 1;
        Rectangle bounds = pageViews.get(index).getBounds();
        JViewport viewport = canvasScroll.getViewport();
        int maxY = Math.max(0, pagesColumn.getHeight() - viewport.getExtentSize().height);
        int y = Math.max(0, Math.min(maxY, bounds.y - scaleForGUI(PAGE_GAP)));
        viewport.setViewPosition(new Point(viewport.getViewPosition().x, y));
        setPage(index + 1);
    }

    /** Works out which page is being read from how far the guide is scrolled. */
    private void onScrolled() {
        if (isRestoringView || !isReady() || pageViews.isEmpty()) {
            return;
        }
        Rectangle visible = canvasScroll.getViewport().getViewRect();
        int line = visible.y + (visible.height * 3 / 10);
        int current = 1;
        for (GuidePageView view : pageViews) {
            if (view.getY() <= line) {
                current = view.pageIndex() + 1;
            }
        }
        if (current != page) {
            setPage(current);
        }
    }

    private void setPage(int newPage) {
        page = newPage;
        refreshPageControls();
        // Pages far from the one being read let their images go; they are drawn again if they come back.
        for (GuidePageView view : pageViews) {
            if (Math.abs(view.pageIndex() + 1 - newPage) > KEPT_PAGES) {
                view.dropImage();
            }
        }
    }

    private void refreshPageControls() {
        boolean isReady = isReady();
        int count = isReady ? document.pageCount() : 0;
        if (!pageField.isFocusOwner()) {
            pageField.setText(isReady ? Integer.toString(page) : "");
        }
        pageField.setEnabled(isReady);
        pageOfLabel.setText(isReady ? getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.page.of", count) : "");
        previousButton.setArmed(isReady && page > 1);
        nextButton.setArmed(isReady && page < count);
        OutlineItem section = isReady ? document.sectionAt(page) : null;
        pageTile.setValue(isReady ? page + " / " + count : "\u2013", TEXT);
        pageTile.setSub(section != null ? shorten(section.title())
                              : (isReady ? getTextAt(RESOURCE_BUNDLE, "GuideViewer.page.noOutline") : ""));
        if (entry == null) {
            footerSummary.setText("");
        } else if (!isReady) {
            footerSummary.setText(entry.getTitle());
        } else {
            footerSummary.setText(getFormattedTextAt(RESOURCE_BUNDLE, section == null ? "GuideViewer.footer.summary"
                  : "GuideViewer.footer.summary.section", entry.getTitle(), page, count,
                  section == null ? "" : section.title()));
        }
    }

    // endregion Guide

    // region Find

    private void runFind(boolean jump) {
        hits = isReady() ? GuideSearch.find(document.pageTexts(), findField.getText()) : List.of();
        hitIndex = -1;
        for (int i = 0; i < hits.size(); i++) {
            if (hits.get(i).page() >= page) {
                hitIndex = i;
                break;
            }
        }
        if (hitIndex < 0 && !hits.isEmpty()) {
            hitIndex = 0;
        }

        Map<Integer, Integer> counts = matchCounts();
        pageViews.forEach(view -> view.setMatches(counts.getOrDefault(view.pageIndex() + 1, 0)));
        refreshFindControls();
        if (jump && hitIndex >= 0) {
            goToPage(hits.get(hitIndex).page());
        }
    }

    private Map<Integer, Integer> matchCounts() {
        Map<Integer, Integer> counts = new TreeMap<>();
        hits.forEach(hit -> counts.merge(hit.page(), 1, Integer::sum));
        return counts;
    }

    private void stepFind(int step) {
        if (hits.isEmpty()) {
            return;
        }
        hitIndex = Math.floorMod(hitIndex + step, hits.size());
        goToPage(hits.get(hitIndex).page());
        refreshFindControls();
    }

    private void refreshFindControls() {
        boolean hasQuery = isReady() && !GuideSearch.normalise(findField.getText()).isEmpty();
        int count = hits.size();
        findCount.setText(!hasQuery ? "" : count == 0 ? getTextAt(RESOURCE_BUNDLE, "GuideViewer.find.none")
                                             : getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.find.position",
                                                   hitIndex + 1, count));
        findPreviousButton.setArmed(count > 1);
        findNextButton.setArmed(count > 1);
        findTile.setValue(hasQuery ? Integer.toString(count) : "\u2013", count > 0 ? AMBER : TEXT_FAINT);
        findTile.setSub(hasQuery ? getFormattedTextAt(RESOURCE_BUNDLE, "GuideViewer.find.pages",
              matchCounts().size()) : getTextAt(RESOURCE_BUNDLE, "GuideViewer.find.nothing"));
    }

    // endregion Find

    // region Actions

    private void openHowTo() {
        guidePreferences.setViewerGuideSeen(true);
        guideDrawer.open(GlossaryGuides.buildViewer());
    }

    private void closeGuide() {
        guideDrawer.close();
    }

    private void saveCopy() {
        if (entry == null || error != null) {
            return;
        }
        File source = GuideReader.fileFor(entry);
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
        if (entry == null || error != null) {
            return;
        }
        File file = GuideReader.fileFor(entry).getAbsoluteFile();
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

    private static String shorten(String text) {
        return text.length() <= TILE_TEXT_LENGTH ? text : text.substring(0, TILE_TEXT_LENGTH - 1).strip() + "\u2026";
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
}
