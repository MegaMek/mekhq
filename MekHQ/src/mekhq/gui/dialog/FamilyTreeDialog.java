/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import megamek.utilities.ImageUtilities;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.familyTree.FormerSpouse;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.baseComponents.roundedComponents.RoundedMMToggleButton;

/**
 * A dialog that displays an interactive family tree visualization.
 *
 * <p>This dialog shows a genealogical tree with the ability to:
 * <ul>
 *   <li>View ancestors (parents, grandparents, etc.) above the origin person</li>
 *   <li>View descendants (children, grandchildren, etc.) below the origin person</li>
 *   <li>Zoom in and out using the mouse wheel or the toolbar controls, with fit-to-window and reset</li>
 *   <li>A compact mode that shows the full details only for the focused person and reduces everyone else to a tightly
 *       packed portrait, decluttering large trees</li>
 *   <li>Single-click a person to focus them; double-click to open their family tree in a new tab</li>
 *   <li>Close individual tabs</li>
 * </ul>
 *
 * <p>Direct lineage lines are gender-colored: pink for female, blue for male, and green for non-binary. When two people
 * in the tree are related by a link that is not part of that direct lineage - the hallmark of an intertwined
 * ("braided") family, such as a child whose parents share an ancestor - that extra relationship is drawn as a dashed
 * red line, so the loop is visible without cluttering the primary tree.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class FamilyTreeDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(FamilyTreeDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FamilyTreeDialog";

    private final int PADDING = scaleForGUI(5);

    private final EnhancedTabbedPane tabbedPane;

    private JLabel zoomLabel;
    private RoundedMMToggleButton compactToggle;

    /**
     * Constructs a new {@link FamilyTreeDialog}.
     *
     * @param owner     the parent frame that owns this dialog
     * @param genealogy the genealogy tree to display initially
     * @param personnel the collection of all personnel in the campaign
     *
     * @author Illiani
     * @since 0.50.10
     */
    public FamilyTreeDialog(Frame owner, Genealogy genealogy, Collection<Person> personnel) {
        super(owner, getText("accessingTerminal.title"), true);

        tabbedPane = new EnhancedTabbedPane();
        tabbedPane.addChangeListener(e -> syncToolbar());
        tabbedPane.addTabStateListener(new EnhancedTabbedPane.TabStateListener() {
            @Override
            public void onTabCloseRequest(int tabIndex, Component component, InputEvent event) {
                if (tabIndex >= 0 && tabIndex < tabbedPane.getTabCount()) {
                    tabbedPane.remove(tabIndex);
                }
                // Closing the final tab closes the whole dialog rather than leaving an empty window.
                if (tabbedPane.getTabCount() == 0) {
                    dispose();
                }
            }
        });

        // Add the initial tree as the first tab
        addFamilyTreeTab(genealogy, personnel);

        // Layout
        setLayout(new BorderLayout());
        add(buildToolbar(), BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        setPreferredSize(scaleForGUI(900, 720));
        // pack() is required: setPreferredSize is only a layout hint, so without it the JDialog opens at its
        // minimum displayable size (a "bar") for any user who does not yet have a saved JWindowPreference.
        // setPreferences() below still overrides this with a restored size if one was previously saved.
        pack();
        setLocationRelativeTo(owner);
        setPreferences(this); // Must be before setVisible

        // Defensive: an earlier version of this constructor lacked pack() and could persist a degenerate
        // dialog size (e.g. ~150x40, just title bar plus tab strip) into JWindowPreference. setPreferences
        // above unconditionally restores that bad size via element.setSize() and would re-trap affected
        // users in the bar state on every subsequent open. If the restored size is clearly below usable
        // thresholds, fall back to the preferred size.
        Dimension restored = getSize();
        Dimension minimum = scaleForGUI(400, 300);
        if (restored.width < minimum.width || restored.height < minimum.height) {
            setSize(scaleForGUI(900, 720));
        }

        syncToolbar();

        setVisible(true); // Should always be last
    }

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences(JDialog dialog) {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(FamilyTreeDialog.class);
            dialog.setName("FamilyTreeDialog");
            preferences.manage(new JWindowPreference(dialog));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    /**
     * Builds the top toolbar: zoom controls, fit/reset, the compact-mode toggle, and a gender legend. The controls act
     * on whichever tab is currently in front, so their state is synced from the active panel by
     * {@link #syncToolbar()}.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private JComponent buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, PADDING, PADDING));
        toolbar.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, 0, PADDING));

        RoundedJButton zoomOut = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.zoomOut.text"));
        zoomOut.setToolTipText(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.zoomOut.tooltip"));
        zoomOut.addActionListener(e -> withActivePanel(FamilyTreePanel::zoomOutStep));

        zoomLabel = new JLabel();
        zoomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        zoomLabel.setPreferredSize(scaleForGUI(52, 20));

        RoundedJButton zoomIn = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.zoomIn.text"));
        zoomIn.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.zoomIn.tooltip")));
        zoomIn.addActionListener(e -> withActivePanel(FamilyTreePanel::zoomInStep));

        RoundedJButton fit = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.fit.text"));
        fit.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.fit.tooltip")));
        fit.addActionListener(e -> withActivePanel(FamilyTreePanel::fitToViewport));

        RoundedJButton reset = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.reset.text"));
        reset.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.reset.tooltip")));
        reset.addActionListener(e -> withActivePanel(FamilyTreePanel::resetZoom));

        compactToggle = new RoundedMMToggleButton(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.compact.text"));
        compactToggle.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.compact.tooltip")));
        compactToggle.addActionListener(e -> withActivePanel(panel -> panel.setCompactMode(compactToggle.isSelected())));

        toolbar.add(zoomOut);
        toolbar.add(zoomLabel);
        toolbar.add(zoomIn);
        toolbar.add(fit);
        toolbar.add(reset);
        toolbar.add(buildToolbarSeparator());
        toolbar.add(compactToggle);
        toolbar.add(buildToolbarSeparator());
        toolbar.add(buildLegend());

        return toolbar;
    }

    private JComponent buildToolbarSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(scaleForGUI(2, 24));
        return separator;
    }

    /**
     * A small legend showing the three gender colors used for the lineage lines, plus the dashed red "related" marker.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private JComponent buildLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(8), 0));
        legend.add(legendSwatch(FamilyTreePanel.COLOR_MALE,
              getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.legend.male")));
        legend.add(legendSwatch(FamilyTreePanel.COLOR_FEMALE,
              getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.legend.female")));
        legend.add(legendSwatch(FamilyTreePanel.COLOR_OTHER,
              getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.legend.other")));
        legend.add(legendSwatch(FamilyTreePanel.COLOR_RELATED,
              getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.legend.related")));
        return legend;
    }

    private JComponent legendSwatch(Color color, String text) {
        JLabel label = new JLabel(text);
        final int size = scaleForGUI(10);
        label.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(color);
                g2d.fillRoundRect(x, y, size, size, size, size);
                g2d.dispose();
            }

            @Override
            public int getIconWidth() {return size;}

            @Override
            public int getIconHeight() {return size;}
        });
        return label;
    }

    /**
     * Builds the bottom bar with the flavor text and the close button.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private JComponent buildFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(BorderFactory.createEmptyBorder(0, PADDING, PADDING, PADDING));

        JLabel hintLabel = new JLabel(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.hint"));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        footer.add(hintLabel);

        JLabel infoLabel = new JLabel(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.flavorText"));
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        footer.add(infoLabel);

        footer.add(Box.createRigidArea(scaleForGUI(0, 5)));

        JButton closeButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.button"));
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(e -> dispose());
        footer.add(closeButton);

        return footer;
    }

    /**
     * Returns the family tree panel shown in the currently selected tab, or {@code null} if none is present.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private @Nullable FamilyTreePanel getActivePanel() {
        Component selected = tabbedPane.getSelectedComponent();
        if (selected instanceof JScrollPane scrollPane
                  && scrollPane.getViewport().getView() instanceof FamilyTreePanel panel) {
            return panel;
        }
        return null;
    }

    private void withActivePanel(java.util.function.Consumer<FamilyTreePanel> action) {
        FamilyTreePanel panel = getActivePanel();
        if (panel != null) {
            action.accept(panel);
        }
    }

    /**
     * Refreshes the toolbar controls (zoom readout and compact toggle) to reflect the active tab's panel. Called when
     * the front tab changes and whenever a panel's zoom changes.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void syncToolbar() {
        FamilyTreePanel panel = getActivePanel();
        if (panel == null || zoomLabel == null) {
            return;
        }
        zoomLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.zoomLabel", panel.getZoomPercent()));
        compactToggle.setSelected(panel.isCompactMode());
    }

    /**
     * Adds a new family tree tab for the specified genealogy.
     *
     * <p>If a tab for this person already exists, it will be selected instead of creating a duplicate.</p>
     *
     * @param genealogy the genealogy tree to display in the new tab
     * @param personnel the collection of all personnel in the campaign
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void addFamilyTreeTab(Genealogy genealogy, Collection<Person> personnel) {
        String title = genealogy.getOrigin().getFullTitle();

        // Check if this person already has a tab open (by title)
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (title.equals(tabbedPane.getTitleAt(i))) {
                tabbedPane.setSelectedIndex(i);
                JScrollPane existingScrollPane = (JScrollPane) tabbedPane.getComponentAt(i);
                centerTreeOnOrigin(existingScrollPane);
                return;
            }
        }

        FamilyTreePanel panel = new FamilyTreePanel(genealogy, personnel, this);
        panel.setZoomChangeListener(this::syncToolbar);
        JScrollPane scrollPane = new FastJScrollPane(panel);
        scrollPane.setBorder(RoundedLineBorder.createRoundedLineBorder());
        panel.setParentScrollPane(scrollPane);
        scrollPane.setPreferredSize(scaleForGUI(800, 600));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(scaleForGUI(16));
        scrollPane.getHorizontalScrollBar().setUnitIncrement(scaleForGUI(16));

        int index = tabbedPane.addCloseableTab(title, null, scrollPane);
        tabbedPane.setSelectedIndex(index);

        // Default to a fit-to-window view so the whole tree is visible when a tab is first opened. Runs via
        // invokeLater so the viewport has been realized and has a usable size to fit against.
        EventQueue.invokeLater(panel::fitToViewport);
    }

    /**
     * Centers the viewport on the origin person of the family tree.
     *
     * <p>This is called when an already-open tab is re-selected to bring that person back into view.</p>
     *
     * @param scrollPane the scroll pane containing the family tree panel
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void centerTreeOnOrigin(JScrollPane scrollPane) {
        if (!(scrollPane.getViewport().getView() instanceof FamilyTreePanel panel)) {
            return;
        }

        // Use invokeLater so this runs AFTER the next layout/paint event and any scroll snaps
        EventQueue.invokeLater(() -> {
            Rectangle box = panel.getOriginPersonBox();
            if (box != null) {
                double zoom = panel.getZoomFactor();
                int panelW = panel.getPreferredSize().width;
                int panelH = panel.getPreferredSize().height;
                int viewW = scrollPane.getViewport().getWidth();
                int viewH = scrollPane.getViewport().getHeight();

                int personCenterX = (int) ((box.x + box.width / 2.0) * zoom);
                int personCenterY = (int) ((box.y + box.height / 2.0) * zoom);

                int targetX = personCenterX - viewW / 2;
                int targetY = personCenterY - viewH / 2;

                // Clamp to viewport and panel bounds for correct scrolling. When the rendered tree fits
                // entirely inside the viewport, panel - view goes negative, which violates Math.clamp's
                // min <= max contract. Floor max at 0 — there's nothing to scroll when content fits.
                int maxX = Math.max(0, panelW - viewW);
                int maxY = Math.max(0, panelH - viewH);
                targetX = Math.clamp(targetX, 0, maxX);
                targetY = Math.clamp(targetY, 0, maxY);

                scrollPane.getViewport().setViewPosition(new Point(targetX, targetY));
            }
        });
    }

    /**
     * Opens a new family tree tab for the specified person.
     *
     * <p>Package-private to allow the {@link FamilyTreePanel} to open new tabs when double-clicking a person.</p>
     *
     * @param person    the person whose family tree should be displayed
     * @param personnel the collection of all personnel in the campaign
     *
     * @author Illiani
     * @since 0.50.10
     */
    void openTreeFor(Person person, Collection<Person> personnel) {
        Genealogy gen = person.getGenealogy();
        if (gen != null) {
            addFamilyTreeTab(gen, personnel);
        }
    }
}

/**
 * Helper class to store layout information for a single person node in the family tree.
 *
 * <p>Contains the person, their position, calculated subtree width, and references to children and parents. The
 * {@code children} and {@code parents} lists form the direct-lineage spanning tree used to lay the graph out; any extra
 * relationships beyond that tree are drawn separately as dashed "related" links.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
class TreeNodeBox {
    final Person person;
    int x, y;
    int generation;   // 0 = origin, negative = ancestors (upward), positive = descendants (downward)
    int subtreeWidth; // Dynamic width used to pack the ancestor pedigree without overlaps

    /** The marriages (current and former) in which this person is the bloodline anchor, each with its own children. */
    final List<Union> descendantUnions = new ArrayList<>();

    /**
     * For a bloodline person on the origin's level or above, the couple formed by their parents (drives the upward
     * pedigree layout); {@code null} once the tree runs out of recorded ancestors.
     */
    Union parentCouple;

    /**
     * Constructs a new {@link TreeNodeBox} for the specified person.
     *
     * @param person the person this node represents
     */
    TreeNodeBox(Person person) {this.person = person;}
}

/**
 * A marriage between two people, together with the children of that marriage. Used both for descendant marriages (a
 * bloodline anchor plus a married-in spouse, with their shared children hanging below) and for ancestor couples (the
 * two parents of a bloodline person). Children are ordered left-to-right by birth date. Drawing reads the partners'
 * actual laid-out X to decide which is physically left, so the {@code left}/{@code right} labels here are only about
 * which partner anchors the layout, not screen position.
 *
 * @author Illiani
 * @since 0.50.10
 */
class Union {
    final TreeNodeBox left;             // the anchoring partner
    final TreeNodeBox right;            // the other partner, or null for a lone parent
    final boolean former;              // true => a former spouse, drawn with a severed (dashed) marriage line
    final List<TreeNodeBox> children = new ArrayList<>();

    Union(TreeNodeBox left, TreeNodeBox right, boolean former) {
        this.left = left;
        this.right = right;
        this.former = former;
    }
}

/**
 * A custom {@link JPanel} that renders an interactive family tree visualization with zoom capability.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Displays both ancestors (upward) and descendants (downward) from an origin person</li>
 *   <li>Mouse-wheel and toolbar zooming with fit-to-window and reset (10% to 300%)</li>
 *   <li>Compact mode that shows details only for the focused person and packs everyone else in as bare portraits</li>
 *   <li>Single-click a person to focus them; double-click to open their family tree</li>
 *   <li>Gender-coded lineage lines (pink/blue/green) plus dashed red links for extra ("braided") relationships</li>
 *   <li>Theme-aware, rounded person cards with a gender accent band and portraits</li>
 * </ul>
 *
 * <p><b>Performance:</b> the tree layout, node dimensions, portraits, relationship edges, and click regions are
 * computed once and cached. Painting only applies the current zoom transform, so scrolling and zooming never rebuild
 * the tree; only toggling compact mode (which changes node footprints) invalidates the cached layout.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
class FamilyTreePanel extends JPanel {
    private final Genealogy genealogy;
    private final Collection<Person> personnel;
    private final FamilyTreeDialog parentDialog;

    private TreeNodeBox root;
    private final List<TreeNodeBox> allNodes = new ArrayList<>();
    /** Vertical gap between generations; horizontal gaps between spouses and between sibling subtrees. */
    private final int vGap = scaleForGUI(64);
    private final int spouseGap = scaleForGUI(18);
    private final int siblingGap = scaleForGUI(30);
    /** Vertical run of the drop from a couple's marriage line down to the sibling bar that feeds their children. */
    private final int busDrop = scaleForGUI(24);

    private final Map<TreeNodeBox, Dimension> nodeDimensions = new HashMap<>();
    private final Map<TreeNodeBox, ImageIcon> nodePortraits = new HashMap<>();
    private final Map<Rectangle, Person> rectToPerson = new HashMap<>();
    /**
     * Every marriage drawn: descendant marriages (with children) and ancestor couples (whose child is the next
     * bloodline person down). Drives the marriage lines and the orthogonal child buses.
     */
    private final List<Union> unions = new ArrayList<>();
    /**
     * Extra links that are not part of the drawn tree structure (e.g. two bloodline people who married each other -
     * "pedigree collapse"), drawn as dashed red so the loop is visible without being confused for normal lineage.
     */
    private final List<TreeNodeBox[]> relatedLinks = new ArrayList<>();
    private int boxHeight = 0;
    private int minGeneration = 0;
    /**
     * Top Y of each generation's row, keyed by generation. Row heights are per-generation so that one taller node (e.g.
     * the focused person's full card in compact mode) only stretches its own row, not the whole tree.
     */
    private final Map<Integer, Integer> rowTopY = new HashMap<>();

    private int panelWidth = 1200, panelHeight = 1000; // Will be dynamically set
    private Rectangle layoutBounds = new Rectangle(0, 0, 0, 0);
    private boolean layoutValid = false;

    // Zoom variables
    private double zoomFactor = 1.0;
    private static final double MIN_ZOOM = 0.10;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_MULTIPLIER = 1.05; // 5% change per scroll notch
    private static final double ZOOM_STEP = 1.25;       // larger step for the toolbar buttons

    // Gender colors for lineage lines and node accent bands. Chosen to read on both light and dark themes.
    static final Color COLOR_FEMALE = new Color(0xE8, 0x6A, 0x92);
    static final Color COLOR_MALE = new Color(0x4C, 0x86, 0xC6);
    static final Color COLOR_OTHER = new Color(0x5F, 0xB3, 0x7A);
    // Dashed line marking two related people whose link is not part of the direct lineage (e.g. incest / shared
    // ancestor loops).
    static final Color COLOR_RELATED = new Color(0xE5, 0x3E, 0x3E);
    // Marriage line joining a couple. Gold reads as distinct from the gender-colored lineage lines on both themes.
    static final Color COLOR_MARRIAGE = new Color(0xC9, 0xA2, 0x27);
    // Red wash laid over the portraits of the deceased.
    static final Color COLOR_DECEASED_TINT = new Color(0xB0, 0x1E, 0x1E);

    private static final int BAND_HEIGHT = scaleForGUI(6);
    private static final int ARC = scaleForGUI(16);
    private static final int PORTRAIT_GAP = scaleForGUI(6);

    private boolean compactMode = false;
    private Person focusedPerson;

    private JScrollPane parentScrollPane = null;
    private Timer zoomTimer = null;
    private Runnable onZoomChanged = null;

    /**
     * Constructs a new {@link FamilyTreePanel}.
     *
     * @param genealogy    the genealogy tree to display
     * @param personnel    the collection of all personnel in the campaign
     * @param parentDialog the parent dialog that owns this panel
     *
     * @author Illiani
     * @since 0.50.10
     */
    public FamilyTreePanel(Genealogy genealogy, Collection<Person> personnel, FamilyTreeDialog parentDialog) {
        this.genealogy = genealogy;
        this.personnel = personnel;
        this.parentDialog = parentDialog;
        this.focusedPerson = genealogy.getOrigin();

        setPreferredSize(new Dimension(panelWidth, panelHeight));

        // Build the layout eagerly so the preferred size and origin box are known before the first paint. Font metrics
        // are available from the component's font without needing a paint-time Graphics.
        ensureLayout();

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                Person person = getPersonAt(evt.getPoint());
                if (person == null) {
                    return;
                }
                if (evt.getClickCount() >= 2) {
                    // Double-click opens the person's own family tree in a new tab.
                    FamilyTreePanel.this.parentDialog.openTreeFor(person, FamilyTreePanel.this.personnel);
                } else {
                    // Single-click focuses the person (relevant in compact mode, and highlights them otherwise).
                    setFocusedPerson(person);
                }
            }

            @Override
            public void mouseMoved(MouseEvent evt) {
                int desired = getPersonAt(evt.getPoint()) != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR;
                if (getCursor().getType() != desired) {
                    setCursor(Cursor.getPredefinedCursor(desired));
                }
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);

        // Mouse wheel listener for zooming, anchored on the cursor position.
        addMouseWheelListener(evt -> {
            double target = evt.getWheelRotation() < 0
                                  ? zoomFactor * ZOOM_MULTIPLIER
                                  : zoomFactor / ZOOM_MULTIPLIER;
            applyZoom(target, evt.getPoint());
        });
    }

    /**
     * Sets the parent scroll pane for zoom navigation.
     *
     * @param scrollPane the scroll pane that contains this panel
     *
     * @author Illiani
     * @since 0.50.10
     */
    void setParentScrollPane(JScrollPane scrollPane) {
        this.parentScrollPane = scrollPane;
    }

    /**
     * Registers a callback fired whenever the zoom level changes, so the owning dialog can refresh its zoom readout.
     *
     * @author Illiani
     * @since 0.50.10
     */
    void setZoomChangeListener(Runnable listener) {
        this.onZoomChanged = listener;
    }

    double getZoomFactor() {
        return zoomFactor;
    }

    int getZoomPercent() {
        return (int) Math.round(zoomFactor * 100);
    }

    boolean isCompactMode() {
        return compactMode;
    }

    /**
     * Toggles compact mode, in which only the focused person's card shows their name and dates while everyone else is
     * reduced to a portrait. Compact mode also lays every node out at portrait size so those portraits pack together
     * and the connecting lines stay short, so toggling it invalidates the cached layout and rebuilds it.
     *
     * @author Illiani
     * @since 0.50.10
     */
    void setCompactMode(boolean compactMode) {
        if (this.compactMode != compactMode) {
            this.compactMode = compactMode;
            layoutValid = false;
            // The footprint changes a lot between modes, so rebuild the layout and rescale so the whole tree fits.
            fitToViewport();
        }
    }

    private void setFocusedPerson(Person person) {
        if (person != null && person != focusedPerson) {
            focusedPerson = person;
            // In compact mode the focused person is the only one drawn at full size, so changing the focus changes
            // node footprints and the tree must be re-laid-out; otherwise a repaint is enough.
            if (compactMode) {
                layoutValid = false;
                ensureLayout();
                revalidate();
            }
            repaint();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Zoom
    // ---------------------------------------------------------------------------------------------------------

    void zoomInStep() {
        applyZoom(zoomFactor * ZOOM_STEP, viewportCenter());
    }

    void zoomOutStep() {
        applyZoom(zoomFactor / ZOOM_STEP, viewportCenter());
    }

    void resetZoom() {
        applyZoom(1.0, viewportCenter());
    }

    /**
     * Zooms so the entire tree fits within the viewport, then scrolls to the top-left of the content.
     *
     * @author Illiani
     * @since 0.50.10
     */
    void fitToViewport() {
        ensureLayout();
        if (parentScrollPane == null) {
            return;
        }
        Dimension view = parentScrollPane.getViewport().getExtentSize();
        double contentWidth = layoutBounds.x + layoutBounds.width + scaleForGUI(40);
        double contentHeight = layoutBounds.y + layoutBounds.height + scaleForGUI(40);
        if (contentWidth <= 0 || contentHeight <= 0 || view.width <= 0 || view.height <= 0) {
            return;
        }
        // Fit only ever zooms out to reveal the whole tree; it never enlarges past 100%, so a lone person or a small
        // tree that already fits stays at its natural size rather than ballooning to fill the window.
        double fit = Math.min(view.width / contentWidth, view.height / contentHeight);
        zoomFactor = Math.clamp(fit, MIN_ZOOM, 1.0);
        updatePreferredSize();
        revalidate();
        parentScrollPane.getViewport().setViewPosition(new Point(0, 0));
        repaint();
        fireZoomChanged();
    }

    /**
     * The center of the visible area, in panel (viewport-relative) coordinates, used as the zoom anchor for the toolbar
     * buttons so they zoom toward the middle of what the user is looking at.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private Point viewportCenter() {
        if (parentScrollPane == null) {
            return new Point(getWidth() / 2, getHeight() / 2);
        }
        Dimension extent = parentScrollPane.getViewport().getExtentSize();
        return new Point(extent.width / 2, extent.height / 2);
    }

    /**
     * Applies a new zoom level, keeping the content under {@code anchor} (a viewport-relative point) fixed on screen.
     * The layout is never rebuilt; only the preferred size and viewport position change.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void applyZoom(double targetZoom, Point anchor) {
        double oldZoom = zoomFactor;
        double newZoom = Math.clamp(targetZoom, MIN_ZOOM, MAX_ZOOM);
        if (newZoom == oldZoom) {
            return;
        }
        zoomFactor = newZoom;
        updatePreferredSize();

        if (parentScrollPane != null) {
            Point viewPos = parentScrollPane.getViewport().getViewPosition();

            // Content coordinate (unscaled) currently under the anchor point.
            double contentX = (viewPos.x + anchor.x) / oldZoom;
            double contentY = (viewPos.y + anchor.y) / oldZoom;

            int newX = (int) (contentX * zoomFactor - anchor.x);
            int newY = (int) (contentY * zoomFactor - anchor.y);

            // Clamp to valid bounds. When zoomed out far enough that the panel fits inside the viewport,
            // content - view goes negative and violates Math.clamp's min <= max contract; floor max at 0.
            Dimension viewSize = parentScrollPane.getViewport().getExtentSize();
            Dimension contentSize = getPreferredSize();
            int maxX = Math.max(0, contentSize.width - viewSize.width);
            int maxY = Math.max(0, contentSize.height - viewSize.height);
            newX = Math.clamp(newX, 0, maxX);
            newY = Math.clamp(newY, 0, maxY);

            parentScrollPane.getViewport().setViewPosition(new Point(newX, newY));
        }

        // Batch revalidate calls with a timer to avoid excessive layout passes on rapid scroll-zoom.
        if (zoomTimer != null && zoomTimer.isRunning()) {
            zoomTimer.restart();
        } else {
            zoomTimer = new Timer(0, e -> {
                revalidate();
                ((Timer) e.getSource()).stop();
            });
            zoomTimer.setRepeats(false);
            zoomTimer.start();
        }

        repaint();
        fireZoomChanged();
    }

    private void fireZoomChanged() {
        if (onZoomChanged != null) {
            onZoomChanged.run();
        }
    }

    private void updatePreferredSize() {
        panelWidth = Math.max(scaleForGUI(50),
              (int) ((layoutBounds.x + layoutBounds.width + scaleForGUI(40)) * zoomFactor));
        panelHeight = Math.max(scaleForGUI(50),
              (int) ((layoutBounds.y + layoutBounds.height + scaleForGUI(40)) * zoomFactor));
        setPreferredSize(new Dimension(panelWidth, panelHeight));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Painting
    // ---------------------------------------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ensureLayout();

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.scale(zoomFactor, zoomFactor);

        drawRelationships(g2d);
        drawNodes(g2d);

        g2d.dispose();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Layout (computed once, then cached)
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Builds and caches the entire tree layout: the set of people to show, their generation rows, node dimensions and
     * portraits, the marriages and their children, the final coordinates, and the click regions. Idempotent:
     * subsequent calls are no-ops until {@link #layoutValid} is cleared.
     *
     * <p>The graph is a genealogical family tree rather than a bare lineage chart: spouses are paired on the same row
     * and joined by a marriage line, children hang from the couple, and everyone in a generation shares a row. See
     * {@link #buildUnions} and {@link #layoutDescendants}/{@link #layoutAncestors} for the structure and placement.</p>
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void ensureLayout() {
        if (layoutValid) {
            return;
        }

        nodeDimensions.clear();
        nodePortraits.clear();
        rectToPerson.clear();
        unions.clear();
        relatedLinks.clear();
        allNodes.clear();
        boxHeight = 0;

        FontMetrics fontMetrics = getFontMetrics(getFont());

        // 1. Build the structural model (who is shown, generation rows, marriages and their children, and any
        // pedigree-collapse links). See FamilyTreeGraph, which is kept Swing-free so it can be unit-tested.
        FamilyTreeGraph graph = new FamilyTreeGraph(genealogy.getOrigin());
        allNodes.addAll(graph.nodes);
        unions.addAll(graph.unions);
        relatedLinks.addAll(graph.relatedLinks);
        root = graph.root;
        minGeneration = graph.minGeneration;

        // 2. Node dimensions and portraits (needed before generations can become Y positions), then row heights.
        for (TreeNodeBox node : allNodes) {
            calculateNodeDimensions(node, fontMetrics);
        }
        computeRowPositions();

        // 3. Coordinates: descendants downward from the origin, ancestors upward, everyone aligned by generation.
        layoutDescendants(root, scaleForGUI(20));
        layoutAncestors(root);

        // Shift everything so the tree has consistent top-left padding.
        Rectangle bounds = computeLayoutBounds();
        int shiftX = scaleForGUI(20) - bounds.x;
        int shiftY = scaleForGUI(20) - bounds.y;
        for (TreeNodeBox node : allNodes) {
            node.x += shiftX;
            node.y += shiftY;
        }
        layoutBounds = computeLayoutBounds();

        // 4. Click regions.
        for (TreeNodeBox node : allNodes) {
            Dimension d = nodeDimensions.get(node);
            rectToPerson.put(new Rectangle(node.x, node.y, d.width, d.height), node.person);
        }

        layoutValid = true;
        updatePreferredSize();
    }

    /**
     * Computes the top Y of each generation's row by stacking rows from the topmost generation down, each as tall as
     * its own tallest node. This keeps compact mode tight vertically while still giving a focused full-size card room.
     */
    private void computeRowPositions() {
        rowTopY.clear();
        Map<Integer, Integer> rowHeight = new HashMap<>();
        int maxGeneration = minGeneration;
        for (TreeNodeBox node : allNodes) {
            rowHeight.merge(node.generation, nodeDimensions.get(node).height, Math::max);
            maxGeneration = Math.max(maxGeneration, node.generation);
        }
        int y = 0;
        for (int generation = minGeneration; generation <= maxGeneration; generation++) {
            rowTopY.put(generation, y);
            y += rowHeight.getOrDefault(generation, boxHeight) + vGap;
        }
    }

    /** The top Y of the row for the given generation. */
    private int rowY(int generation) {
        return rowTopY.getOrDefault(generation, (generation - minGeneration) * (boxHeight + vGap));
    }

    /**
     * Lays out a bloodline person and their descendants downward. Each of the person's marriages places its children
     * left to right (recursively), then the person and their spouse(s) are positioned over those children. Returns the
     * right edge of the whole block so siblings can be packed without overlapping.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private int layoutDescendants(TreeNodeBox node, int leftX) {
        node.y = rowY(node.generation);
        Dimension nodeDim = nodeDimensions.get(node);

        List<int[]> groupSpan = new ArrayList<>();
        int cursor = leftX;
        int childrenRight = leftX;
        boolean anyChildren = false;
        for (Union union : node.descendantUnions) {
            if (union.children.isEmpty()) {
                groupSpan.add(null);
                continue;
            }
            int start = cursor;
            for (int i = 0; i < union.children.size(); i++) {
                if (i > 0) {
                    cursor += siblingGap;
                }
                cursor = layoutDescendants(union.children.get(i), cursor);
            }
            groupSpan.add(new int[] { start, cursor });
            childrenRight = cursor;
            anyChildren = true;
            cursor += siblingGap * 2;
        }

        placeCoupleRow(node, nodeDim, groupSpan, leftX, childrenRight, anyChildren);

        int rowLeft = node.x;
        int rowRight = node.x + nodeDim.width;
        for (Union union : node.descendantUnions) {
            if (union.right != null) {
                rowLeft = Math.min(rowLeft, union.right.x);
                rowRight = Math.max(rowRight, union.right.x + nodeDimensions.get(union.right).width);
            }
        }

        int blockLeft = anyChildren ? Math.min(leftX, rowLeft) : rowLeft;
        int blockRight = anyChildren ? Math.max(childrenRight, rowRight) : rowRight;
        if (blockLeft < leftX) {
            int dx = leftX - blockLeft;
            shiftSubtree(node, dx);
            blockRight += dx;
        }
        return blockRight;
    }

    /**
     * Positions a person and the spouse(s) they are drawn beside. With no spouse the person is centered over their
     * children; with one, the couple is centered over that marriage's children; with several the person sits over the
     * middle of all their children and each spouse is placed on the side its own children occupy. The children
     * themselves are already placed.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void placeCoupleRow(TreeNodeBox node, Dimension nodeDim, List<int[]> groupSpan, int childrenLeft,
          int childrenRight, boolean anyChildren) {
        int y = node.y;
        List<Union> spouseUnions = new ArrayList<>();
        for (Union union : node.descendantUnions) {
            if (union.right != null) {
                spouseUnions.add(union);
            }
        }

        int childrenMid = anyChildren ? (childrenLeft + childrenRight) / 2 : childrenLeft;

        if (spouseUnions.isEmpty()) {
            node.x = anyChildren ? childrenMid - nodeDim.width / 2 : childrenLeft;
            return;
        }

        if (spouseUnions.size() == 1) {
            Union union = spouseUnions.get(0);
            Dimension spouseDim = nodeDimensions.get(union.right);
            int coupleWidth = nodeDim.width + spouseGap + spouseDim.width;
            int center = childrenMid;
            int[] span = groupSpan.get(node.descendantUnions.indexOf(union));
            if (span != null) {
                center = (span[0] + span[1]) / 2;
            }
            int rowLeft = center - coupleWidth / 2;
            node.x = rowLeft;
            union.right.x = rowLeft + nodeDim.width + spouseGap;
            union.right.y = y;
            return;
        }

        node.x = childrenMid - nodeDim.width / 2;
        int leftEdge = node.x;
        int rightEdge = node.x + nodeDim.width;
        for (Union union : spouseUnions) {
            TreeNodeBox spouse = union.right;
            Dimension spouseDim = nodeDimensions.get(spouse);
            spouse.y = y;
            // Place each spouse on the side its own children were laid out, so the marriage line and child bus do
            // not cross over to the far side of the person.
            int[] span = groupSpan.get(node.descendantUnions.indexOf(union));
            int groupCenter = span != null ? (span[0] + span[1]) / 2 : childrenMid;
            if (groupCenter < childrenMid) {
                spouse.x = leftEdge - spouseGap - spouseDim.width;
                leftEdge = spouse.x;
            } else {
                spouse.x = rightEdge + spouseGap;
                rightEdge = spouse.x + spouseDim.width;
            }
        }
    }

    /** Shifts a person, their drawn spouses, and their whole descendant block horizontally by {@code dx}. */
    private void shiftSubtree(TreeNodeBox node, int dx) {
        node.x += dx;
        for (Union union : node.descendantUnions) {
            if (union.right != null) {
                union.right.x += dx;
            }
            for (TreeNodeBox child : union.children) {
                shiftSubtree(child, dx);
            }
        }
    }

    /**
     * Lays out the ancestor pedigree upward from a bloodline person. The two parents are spread <b>symmetrically</b>
     * about the child so the descent line drops straight down from the middle of their marriage line, and each parent
     * is given enough room on its own side for its ancestors above. Only the direct bloodline is followed.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void layoutAncestors(TreeNodeBox node) {
        Union couple = node.parentCouple;
        if (couple == null) {
            return;
        }
        TreeNodeBox first = couple.left;
        TreeNodeBox second = couple.right;
        int childCenter = node.x + nodeWidth(node) / 2;

        if (second == null) {
            first.x = childCenter - nodeWidth(first) / 2;
            first.y = rowY(first.generation);
            layoutAncestors(first);
            return;
        }

        // Offset each parent the same distance from the child's center - enough for the wider parent's subtree plus a
        // gap - so the couple straddles the child evenly and the child hangs straight below their midpoint.
        int offset = Math.max(ancestorSubtreeWidth(first), ancestorSubtreeWidth(second)) / 2 + siblingGap / 2;
        first.x = childCenter - offset - nodeWidth(first) / 2;
        second.x = childCenter + offset - nodeWidth(second) / 2;
        first.y = rowY(first.generation);
        second.y = rowY(second.generation);
        layoutAncestors(first);
        layoutAncestors(second);
    }

    /**
     * The horizontal room a person and everything above them in the pedigree need, used to space ancestor branches.
     * layoutAncestors queries this at every level, so the result is memoized in {@link TreeNodeBox#subtreeWidth} (0 =
     * not yet computed; nodes are rebuilt each layout, so the cache never goes stale).
     */
    private int ancestorSubtreeWidth(TreeNodeBox node) {
        if (node.subtreeWidth != 0) {
            return node.subtreeWidth;
        }
        Union couple = node.parentCouple;
        int width;
        if (couple == null) {
            width = nodeWidth(node);
        } else if (couple.right == null) {
            width = Math.max(nodeWidth(node), ancestorSubtreeWidth(couple.left));
        } else {
            int firstWidth = ancestorSubtreeWidth(couple.left);
            int secondWidth = ancestorSubtreeWidth(couple.right);
            int offset = Math.max(firstWidth, secondWidth) / 2 + siblingGap / 2;
            int total = 2 * offset + firstWidth / 2 + secondWidth / 2;
            width = Math.max(nodeWidth(node), total);
        }
        node.subtreeWidth = width;
        return width;
    }

    private int nodeWidth(TreeNodeBox node) {
        return nodeDimensions.get(node).width;
    }

    /**
     * Computes the bounding rectangle that encloses every node.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private Rectangle computeLayoutBounds() {
        if (allNodes.isEmpty()) {
            return new Rectangle(0, 0, 0, 0);
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (TreeNodeBox node : allNodes) {
            Dimension d = nodeDimensions.get(node);
            minX = Math.min(minX, node.x);
            minY = Math.min(minY, node.y);
            maxX = Math.max(maxX, node.x + d.width);
            maxY = Math.max(maxY, node.y + d.height);
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Calculates and caches the layout dimensions and portrait for a single node.
     *
     * <p>Outside compact mode every node reserves space for its full detail (portrait, name, and dates), since every
     * node paints a full card. In compact mode only the focused person paints a full card, so only they reserve full
     * space; everyone else is laid out at portrait size. Reserving the focused card's real footprint (rather than
     * overlaying it) means it never covers its neighbors, so no one gets hidden.</p>
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void calculateNodeDimensions(TreeNodeBox node, FontMetrics fontMetrics) {
        ImageIcon portraitImage = node.person.getPortraitImageIconWithFallback(true);
        // Wash the portraits of the deceased in red so a glance tells the living from the dead.
        if (portraitImage != null && node.person.getStatus().isDead()) {
            // transparencyPercent is a 0.0-1.0 fraction where higher = subtler; 0.65 leaves a clear but not
            // overpowering red wash (alpha ~89) over the portrait.
            portraitImage = ImageUtilities.addTintToImageIcon(portraitImage.getImage(),
                  COLOR_DECEASED_TINT,
                  true,
                  0.65);
        }
        nodePortraits.put(node, portraitImage);
        int portraitW = 0, portraitH = 0;
        if (portraitImage != null) {
            portraitW = portraitImage.getIconWidth();
            portraitH = portraitImage.getIconHeight();
        }

        Dimension size;
        if (compactMode && node.person != focusedPerson) {
            // Portrait-only footprint for everyone but the focused person, so portraits pack tightly.
            size = new Dimension(Math.max(portraitW, scaleForGUI(20)), Math.max(portraitH, scaleForGUI(20)));
        } else {
            size = fullNodeDimension(node, fontMetrics, portraitW, portraitH);
        }

        nodeDimensions.put(node, size);
        if (size.height > boxHeight) {boxHeight = size.height;}
    }

    /**
     * The full-detail card size for a node (portrait plus name and dates), used for layout and for the compact-mode
     * focused-card overlay.
     */
    private Dimension fullNodeDimension(TreeNodeBox node, FontMetrics fontMetrics, int portraitW, int portraitH) {
        String name = node.person.getFullTitle();
        String dates = getDateString(node.person);

        int paddingX = scaleForGUI(28), paddingY = scaleForGUI(20);
        int nameWidth = fontMetrics.stringWidth(name);
        int datesWidth = fontMetrics.stringWidth(dates);
        int textWidth = Math.max(nameWidth, datesWidth);
        int width = Math.max(textWidth + paddingX, portraitW);

        int lineHeight = fontMetrics.getHeight();
        int height = BAND_HEIGHT + (lineHeight * 2) + paddingY + (portraitH > 0 ? portraitH + PORTRAIT_GAP : 0);

        return new Dimension(width, height);
    }

    /**
     * Formats the birth and death dates for display.
     *
     * @return a formatted string like "(YYYY-MM-DD - YYYY-MM-DD)" or "(YYYY-MM-DD)" for living persons
     *
     * @author Illiani
     * @since 0.50.10
     */
    private String getDateString(Person person) {
        String birthDate = person.getDateOfBirth() != null
                                 ? person.getDateOfBirth().toString()
                                 : "?";
        String deathDate = person.getDateOfDeath() != null
                                 ? person.getDateOfDeath().toString()
                                 : (person.getStatus().isDead() ? "?" : "");

        if (deathDate.isEmpty()) {
            return "(" + birthDate + ")";
        } else {
            return "(" + birthDate + " - " + deathDate + ")";
        }
    }

    /**
     * Gets the bounding rectangle for the origin person's node, in unscaled layout coordinates.
     *
     * @return a {@link Rectangle} representing the origin person's position and size, or null if no root exists
     *
     * @author Illiani
     * @since 0.50.10
     */
    Rectangle getOriginPersonBox() {
        ensureLayout();
        if (root == null) {
            return null;
        }
        Dimension boxDim = nodeDimensions.get(root);
        return new Rectangle(root.x, root.y, boxDim.width, boxDim.height);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Draws the marriage lines and the orthogonal child buses that connect couples to their children, plus any dashed
     * "related" links (two blood relatives who married - pedigree collapse). A marriage line is gold and solid, or
     * dashed for a former spouse; each child hangs from a sibling bar by a drop colored to the child's gender.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void drawRelationships(Graphics2D g2d) {
        Stroke busStroke = new BasicStroke(scaleForGUI(3), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Stroke marriageStroke = new BasicStroke(scaleForGUI(3), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
        Stroke marriageFormerStroke = new BasicStroke(scaleForGUI(3), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f,
              new float[] { scaleForGUI(7), scaleForGUI(6) }, 0f);
        Stroke relatedStroke = new BasicStroke(scaleForGUI(3), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f,
              new float[] { scaleForGUI(9), scaleForGUI(7) }, 0f);
        Color busColor = UIUtil.uiIndependentGray();

        for (Union union : unions) {
            if (!union.children.isEmpty()) {
                drawChildBus(g2d, union, busColor, busStroke);
            }
            if (union.right != null) {
                drawMarriageLine(g2d, union, marriageStroke, marriageFormerStroke);
            }
        }

        g2d.setStroke(relatedStroke);
        g2d.setColor(COLOR_RELATED);
        for (TreeNodeBox[] link : relatedLinks) {
            g2d.drawLine(centerX(link[0]), rowCenterY(link[0]), centerX(link[1]), rowCenterY(link[1]));
        }
    }

    /** Draws the horizontal marriage line joining a couple, solid gold for a current spouse and dashed for a former. */
    private void drawMarriageLine(Graphics2D g2d, Union union, Stroke solid, Stroke dashed) {
        TreeNodeBox leftBox = union.left.x <= union.right.x ? union.left : union.right;
        TreeNodeBox rightBox = leftBox == union.left ? union.right : union.left;
        int y = rowCenterY(leftBox);
        int x1 = leftBox.x + nodeDimensions.get(leftBox).width;
        int x2 = rightBox.x;
        g2d.setColor(COLOR_MARRIAGE);
        g2d.setStroke(union.former ? dashed : solid);
        g2d.drawLine(x1, y, x2, y);
    }

    /**
     * Draws the orthogonal descent from a couple to their children: a stem down from the marriage line (or the lone
     * parent), a horizontal sibling bar spanning the children, and a gender-colored drop to each child.
     */
    private void drawChildBus(Graphics2D g2d, Union union, Color busColor, Stroke busStroke) {
        int stemX;
        int originY;
        if (union.right != null) {
            // Drop from the middle of the marriage line, so the descent meets the couple's tie with no gap.
            stemX = (centerX(union.left) + centerX(union.right)) / 2;
            TreeNodeBox leftBox = union.left.x <= union.right.x ? union.left : union.right;
            originY = rowCenterY(leftBox);
        } else {
            stemX = centerX(union.left);
            originY = connectionBottomY(union.left);
        }

        int childrenTopY = Integer.MAX_VALUE;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (TreeNodeBox child : union.children) {
            childrenTopY = Math.min(childrenTopY, connectionTopY(child));
            minX = Math.min(minX, centerX(child));
            maxX = Math.max(maxX, centerX(child));
        }

        int busY = childrenTopY - busDrop;
        if (busY <= originY) {
            busY = (originY + childrenTopY) / 2;
        }

        g2d.setStroke(busStroke);
        if (union.children.size() == 1) {
            // A single child: draw the whole descent in that child's gender color as one continuous path.
            TreeNodeBox child = union.children.get(0);
            int childX = centerX(child);
            g2d.setColor(getGenderColor(child.person));
            g2d.drawLine(stemX, originY, stemX, busY);
            g2d.drawLine(stemX, busY, childX, busY);
            g2d.drawLine(childX, busY, childX, connectionTopY(child));
            return;
        }

        // Several children share the bar, which stays neutral; each child's own drop carries its gender color.
        g2d.setColor(busColor);
        g2d.drawLine(stemX, originY, stemX, busY);
        g2d.drawLine(Math.min(minX, stemX), busY, Math.max(maxX, stemX), busY);
        for (TreeNodeBox child : union.children) {
            g2d.setColor(getGenderColor(child.person));
            g2d.drawLine(centerX(child), busY, centerX(child), connectionTopY(child));
        }
    }

    private int centerX(TreeNodeBox node) {
        return node.x + nodeDimensions.get(node).width / 2;
    }

    /** Vertical center of a node's drawn shape (card center, or portrait center when only the portrait shows). */
    private int rowCenterY(TreeNodeBox node) {
        return (connectionTopY(node) + connectionBottomY(node)) / 2;
    }

    /**
     * Whether the given node currently paints its name and dates (and therefore its full card chrome). This is driven
     * purely by compact mode, not by zoom: outside compact mode every person shows their full card at any zoom level;
     * in compact mode only the focused person does, while everyone else is reduced to a bare portrait.
     */
    private boolean showTextFor(TreeNodeBox node) {
        return !compactMode || node.person == focusedPerson;
    }

    /** Y of the top of the drawn shape for line connection: the card top, or the portrait top in portrait-only mode. */
    private int connectionTopY(TreeNodeBox node) {
        if (showTextFor(node)) {
            return node.y;
        }
        return node.y + Math.max(0, (nodeDimensions.get(node).height - portraitHeight(node)) / 2);
    }

    /** Y of the bottom of the drawn shape for line connection: the card bottom, or the portrait bottom otherwise. */
    private int connectionBottomY(TreeNodeBox node) {
        int height = nodeDimensions.get(node).height;
        if (showTextFor(node)) {
            return node.y + height;
        }
        int portraitHeight = portraitHeight(node);
        return node.y + Math.max(0, (height - portraitHeight) / 2) + portraitHeight;
    }

    private int portraitHeight(TreeNodeBox node) {
        ImageIcon portrait = nodePortraits.get(node);
        return portrait != null ? portrait.getIconHeight() : 0;
    }

    /**
     * Draws every person card. What is painted inside each card (portrait, name, dates) depends on compact mode. The
     * focused person is drawn last so its highlight - and, in compact mode, its full-card overlay - sits on top.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void drawNodes(Graphics2D g2d) {
        Color cardColor = cardColor();
        Color borderColor = UIUtil.uiIndependentGray();
        Color textColor = getForeground();
        Color mutedColor = blend(textColor, cardColor, 0.45);

        TreeNodeBox focusedBox = null;
        for (TreeNodeBox node : allNodes) {
            if (node.person == focusedPerson) {
                focusedBox = node;
                continue;
            }
            drawSingleNode(g2d, node, cardColor, borderColor, textColor, mutedColor);
        }
        if (focusedBox != null) {
            drawSingleNode(g2d, focusedBox, cardColor, borderColor, textColor, mutedColor);
        }
    }

    /**
     * Draws a single person card: a rounded, theme-colored card with a gender accent band and, depending on the level
     * of detail, a portrait, the name, and the dates. The focused person is highlighted with a thicker accent border.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void drawSingleNode(Graphics2D g2d, TreeNodeBox node, Color cardColor, Color borderColor, Color textColor,
          Color mutedColor) {
        Dimension boxDim = nodeDimensions.get(node);
        boolean focused = node.person == focusedPerson;
        Color accent = getGenderColor(node.person);

        // When no text is shown (an unfocused person in compact mode), draw ONLY the portrait - no card background,
        // gender band, or border - so the view reduces to bare portraits. Otherwise draw the full card.
        boolean showText = showTextFor(node);

        // The focused person reserves a full-size box in compact mode too (see calculateNodeDimensions), so the card
        // is drawn in its own footprint and never covers a neighbor.
        int w = boxDim.width;
        int h = boxDim.height;
        int x = node.x;
        int y = node.y;
        boolean drawName = showText;
        boolean drawDates = showText;

        if (showText) {
            // Card background.
            g2d.setColor(cardColor);
            g2d.fillRoundRect(x, y, w, h, ARC, ARC);

            // Gender accent band along the top, clipped so its top corners stay rounded.
            Shape oldClip = g2d.getClip();
            g2d.setClip(new Rectangle(x, y, w, BAND_HEIGHT + ARC));
            g2d.setColor(accent);
            g2d.fillRoundRect(x, y, w, BAND_HEIGHT + ARC, ARC, ARC);
            g2d.setClip(oldClip);

            // Border (accent + thicker for the focused person).
            g2d.setColor(focused ? accent : borderColor);
            g2d.setStroke(new BasicStroke(focused ? scaleForGUI(3) : scaleForGUI(1)));
            g2d.drawRoundRect(x, y, w, h, ARC, ARC);
            g2d.setStroke(new BasicStroke(1));
        }

        // Assemble the vertically-centered content block. With the card shown the content sits below the accent band;
        // as a bare portrait it is centered in the whole reserved box.
        FontMetrics fm = g2d.getFontMetrics();
        int lineHeight = fm.getHeight();
        ImageIcon portrait = nodePortraits.get(node);
        int portraitH = portrait != null ? portrait.getIconHeight() : 0;
        int portraitW = portrait != null ? portrait.getIconWidth() : 0;

        int gap = scaleForGUI(4);
        int contentHeight = 0;
        if (portraitH > 0) {
            contentHeight += portraitH;
        }
        if (drawName) {
            contentHeight += (contentHeight > 0 ? gap : 0) + lineHeight;
        }
        if (drawDates) {
            contentHeight += (contentHeight > 0 ? gap : 0) + lineHeight;
        }

        int regionTop = y + (showText ? BAND_HEIGHT : 0);
        int regionHeight = h - (showText ? BAND_HEIGHT : 0);
        int cursorY = regionTop + Math.max(0, (regionHeight - contentHeight) / 2);

        if (portraitH > 0) {
            int px = x + (w - portraitW) / 2;
            g2d.drawImage(portrait.getImage(), px, cursorY, null);
            cursorY += portraitH + gap;
        }

        if (drawName) {
            String name = node.person.getFullTitle();
            g2d.setColor(textColor);
            g2d.setFont(fm.getFont().deriveFont(Font.BOLD));
            FontMetrics boldMetrics = g2d.getFontMetrics();
            int nameX = x + (w - boldMetrics.stringWidth(name)) / 2;
            g2d.drawString(name, nameX, cursorY + boldMetrics.getAscent());
            g2d.setFont(fm.getFont());
            cursorY += lineHeight + gap;
        }

        if (drawDates) {
            String dates = getDateString(node.person);
            g2d.setColor(mutedColor);
            int datesX = x + (w - fm.stringWidth(dates)) / 2;
            g2d.drawString(dates, datesX, cursorY + fm.getAscent());
        }
    }

    /**
     * A theme-aware card color derived from the panel background: slightly lighter in light themes and slightly lighter
     * than the (dark) background in dark themes, so cards read as raised surfaces in both.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private Color cardColor() {
        Color base = getBackground();
        if (base == null) {
            base = UIManager.getColor("Panel.background");
        }
        if (base == null) {
            base = Color.LIGHT_GRAY;
        }
        double luminance = (0.299 * base.getRed() + 0.587 * base.getGreen() + 0.114 * base.getBlue()) / 255.0;
        double factor = luminance < 0.5 ? 0.14 : 0.06;
        return blend(Color.WHITE, base, factor);
    }

    /** Blends {@code a} into {@code b} by {@code ratio} (0 returns b, 1 returns a). */
    private static Color blend(Color a, Color b, double ratio) {
        double inverse = 1.0 - ratio;
        return new Color(
              (int) Math.round(a.getRed() * ratio + b.getRed() * inverse),
              (int) Math.round(a.getGreen() * ratio + b.getGreen() * inverse),
              (int) Math.round(a.getBlue() * ratio + b.getBlue() * inverse));
    }

    /**
     * Returns the color for a lineage line / accent band based on the person's gender.
     *
     * @return green for non-binary, pink for female, blue for male
     *
     * @author Illiani
     * @since 0.50.10
     */
    private Color getGenderColor(Person person) {
        Gender gender = person.getGender();
        if (gender.isGenderNeutral()) {
            return COLOR_OTHER;
        }
        if (gender.isFemale()) {
            return COLOR_FEMALE;
        }
        return COLOR_MALE;
    }

    /**
     * Finds the person at the specified screen coordinates, accounting for zoom level.
     *
     * @return the Person at that location, or {@code null} if none found
     *
     * @author Illiani
     * @since 0.50.10
     */
    private @Nullable Person getPersonAt(Point point) {
        Point scaledPoint = new Point(
              (int) (point.x / zoomFactor),
              (int) (point.y / zoomFactor)
        );

        for (Map.Entry<Rectangle, Person> entry : rectToPerson.entrySet()) {
            if (entry.getKey().contains(scaledPoint)) {
                return entry.getValue();
            }
        }
        return null;
    }
}

/**
 * The Swing-free structural model of a family tree: who is shown, which generation each person sits in, and the
 * marriages (with their children) and pedigree-collapse links that connect them. {@link FamilyTreePanel} turns this
 * model into coordinates and paints it; keeping the model separate makes the graph logic unit-testable without a GUI.
 *
 * <p>The model is built from a single origin person: the origin, all of their ancestors and all of their descendants
 * form the bloodline; spouses on the origin's generation and below are married in on top. Generations run from 0 at the
 * origin, negative upward and positive downward. Descendant marriages group a person's children by their other parent
 * (so children of different partners form half-sibling groups), while ancestor couples are simply the two parents of
 * each bloodline person.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
class FamilyTreeGraph {
    final Map<Person, TreeNodeBox> nodesByPerson = new HashMap<>();
    final List<TreeNodeBox> nodes = new ArrayList<>();
    final Set<Person> bloodline = new HashSet<>();
    final List<Union> unions = new ArrayList<>();
    final List<TreeNodeBox[]> relatedLinks = new ArrayList<>();
    TreeNodeBox root;
    int minGeneration;

    FamilyTreeGraph(Person origin) {
        collectBloodline(origin);
        for (Person person : bloodline) {
            nodesByPerson.computeIfAbsent(person, TreeNodeBox::new);
        }
        root = nodesByPerson.get(origin);
        nodes.addAll(nodesByPerson.values());

        assignGenerations();

        // Marry spouses in on top of the bloodline, but only around the origin and below - ancestors are shown as a
        // clean pedigree of couples, without married-in step-partners. A spouse shares its partner's generation.
        for (TreeNodeBox blood : new ArrayList<>(nodes)) {
            if (blood.generation < 0) {
                continue;
            }
            Genealogy g = blood.person.getGenealogy();
            if (g == null) {
                continue;
            }
            addSpouseNode(g.getSpouse(), blood);
            for (FormerSpouse formerSpouse : g.getFormerSpouses()) {
                addSpouseNode(formerSpouse.getFormerSpouse(), blood);
            }
        }
        nodes.clear();
        nodes.addAll(nodesByPerson.values());

        buildUnions();
    }

    /**
     * Fills {@link #bloodline} with the origin, every ancestor (following parents upward) and every descendant
     * (following children downward). Married-in spouses are deliberately excluded here; they are added afterward.
     */
    private void collectBloodline(Person origin) {
        bloodline.add(origin);

        Deque<Person> stack = new ArrayDeque<>();
        stack.push(origin);
        while (!stack.isEmpty()) {
            Genealogy g = stack.pop().getGenealogy();
            if (g == null) {
                continue;
            }
            for (Person parent : g.getParents()) {
                if (parent != null && bloodline.add(parent)) {
                    stack.push(parent);
                }
            }
        }

        stack.push(origin);
        while (!stack.isEmpty()) {
            Genealogy g = stack.pop().getGenealogy();
            if (g == null) {
                continue;
            }
            for (Person child : g.getChildren()) {
                if (child != null && bloodline.add(child)) {
                    stack.push(child);
                }
            }
        }
    }

    /** Adds a married-in spouse node (if not already present) sharing its partner's generation. */
    private void addSpouseNode(@Nullable Person spouse, TreeNodeBox partner) {
        if (spouse == null || nodesByPerson.containsKey(spouse)) {
            return;
        }
        TreeNodeBox node = new TreeNodeBox(spouse);
        node.generation = partner.generation;
        nodesByPerson.put(spouse, node);
    }

    /**
     * Assigns a generation to every bloodline node by breadth-first search from the origin: a parent is one generation
     * up, a child one down. The first assignment for a person wins, which keeps generations consistent through the
     * occasional relationship loop.
     */
    private void assignGenerations() {
        for (TreeNodeBox node : nodes) {
            node.generation = Integer.MIN_VALUE;
        }
        Deque<TreeNodeBox> queue = new ArrayDeque<>();
        root.generation = 0;
        queue.add(root);
        while (!queue.isEmpty()) {
            TreeNodeBox node = queue.poll();
            Genealogy g = node.person.getGenealogy();
            if (g == null) {
                continue;
            }
            for (Person parent : g.getParents()) {
                visitGeneration(parent, node.generation - 1, queue);
            }
            for (Person child : g.getChildren()) {
                visitGeneration(child, node.generation + 1, queue);
            }
        }

        minGeneration = 0;
        for (TreeNodeBox node : nodes) {
            if (node.generation == Integer.MIN_VALUE) {
                node.generation = 0;
            }
            minGeneration = Math.min(minGeneration, node.generation);
        }
    }

    private void visitGeneration(@Nullable Person person, int generation, Deque<TreeNodeBox> queue) {
        if (person == null) {
            return;
        }
        TreeNodeBox node = nodesByPerson.get(person);
        if (node == null || node.generation != Integer.MIN_VALUE) {
            return;
        }
        node.generation = generation;
        queue.add(node);
    }

    /**
     * Builds the marriages that structure the tree. Two kinds:
     * <ul>
     *   <li><b>Descendant marriages</b> (origin and below): each bloodline person's children are grouped by their other
     *       parent, so children of different partners become half-sibling groups under separate marriages. A married-in
     *       partner becomes the spouse of the couple; two blood relatives who married each other are not fused into a
     *       couple but flagged as a dashed "related" link (pedigree collapse).</li>
     *   <li><b>Ancestor couples</b> (origin and above): the two parents of each bloodline person, whose only shown child
     *       is that person - the direct line upward.</li>
     * </ul>
     */
    private void buildUnions() {
        Set<String> relatedSeen = new HashSet<>();
        // A child is laid out under exactly one parent. This matters only when both of a child's parents are bloodline
        // (pedigree collapse); whichever parent is processed first claims the child, the other still gets a dashed link.
        Set<TreeNodeBox> claimedChildren = new HashSet<>();

        for (TreeNodeBox anchor : nodes) {
            if (!bloodline.contains(anchor.person) || anchor.generation < 0) {
                continue;
            }
            Genealogy g = anchor.person.getGenealogy();
            if (g == null) {
                continue;
            }

            Map<Person, List<TreeNodeBox>> childrenByCoParent = new LinkedHashMap<>();
            List<TreeNodeBox> singleParentChildren = new ArrayList<>();
            for (Person childPerson : g.getChildren()) {
                TreeNodeBox childNode = nodesByPerson.get(childPerson);
                if (childNode == null || childNode.generation != anchor.generation + 1
                          || !claimedChildren.add(childNode)) {
                    continue;
                }
                Person coParent = otherParent(childPerson, anchor.person);
                TreeNodeBox coParentNode = coParent == null ? null : nodesByPerson.get(coParent);
                if (coParentNode == null || coParentNode.generation != anchor.generation) {
                    singleParentChildren.add(childNode);
                } else {
                    childrenByCoParent.computeIfAbsent(coParent, key -> new ArrayList<>()).add(childNode);
                }
            }

            for (Person coParent : orderedPartners(g, childrenByCoParent.keySet(), anchor.generation)) {
                TreeNodeBox spouseNode = nodesByPerson.get(coParent);
                List<TreeNodeBox> kids = childrenByCoParent.getOrDefault(coParent, new ArrayList<>());
                if (bloodline.contains(coParent)) {
                    // Two blood relatives married each other: keep them in their own bloodline slots and show the tie
                    // as a dashed related link rather than fusing them into a couple that would place one of them twice.
                    if (relatedSeen.add(relatedKey(anchor, spouseNode))) {
                        relatedLinks.add(new TreeNodeBox[] { anchor, spouseNode });
                    }
                    singleParentChildren.addAll(kids);
                    continue;
                }
                Union union = new Union(anchor, spouseNode, isFormerSpouse(g, coParent));
                sortByBirth(kids);
                union.children.addAll(kids);
                anchor.descendantUnions.add(union);
                unions.add(union);
            }

            if (!singleParentChildren.isEmpty()) {
                Union union = new Union(anchor, null, false);
                sortByBirth(singleParentChildren);
                union.children.addAll(singleParentChildren);
                anchor.descendantUnions.add(union);
                unions.add(union);
            }
        }

        for (TreeNodeBox node : nodes) {
            if (!bloodline.contains(node.person) || node.generation > 0) {
                continue;
            }
            Genealogy g = node.person.getGenealogy();
            if (g == null) {
                continue;
            }
            List<TreeNodeBox> parents = new ArrayList<>();
            for (Person parent : g.getParents()) {
                TreeNodeBox parentNode = nodesByPerson.get(parent);
                if (parentNode != null && bloodline.contains(parent)
                          && parentNode.generation == node.generation - 1) {
                    parents.add(parentNode);
                }
            }
            if (parents.isEmpty()) {
                continue;
            }
            Union couple = new Union(parents.get(0), parents.size() > 1 ? parents.get(1) : null, false);
            couple.children.add(node);
            node.parentCouple = couple;
            unions.add(couple);
        }
    }

    /** The first included parent of {@code child} that is not {@code parent}, or {@code null} for a lone parent. */
    private @Nullable Person otherParent(Person child, Person parent) {
        Genealogy g = child.getGenealogy();
        if (g == null) {
            return null;
        }
        for (Person candidate : g.getParents()) {
            if (candidate != null && !candidate.equals(parent) && nodesByPerson.containsKey(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Orders a person's partners for stable, readable placement: the current spouse first, then former spouses by the
     * date the marriage ended, then any remaining co-parents. Only partners that are shown and sit on the person's own
     * generation are returned.
     */
    private List<Person> orderedPartners(Genealogy g, Set<Person> coParents, int generation) {
        List<Person> order = new ArrayList<>();
        if (isPartnerCandidate(g.getSpouse(), generation)) {
            order.add(g.getSpouse());
        }
        List<FormerSpouse> formerSpouses = new ArrayList<>(g.getFormerSpouses());
        formerSpouses.sort(Comparator.comparing(FormerSpouse::getDate,
              Comparator.nullsLast(Comparator.naturalOrder())));
        for (FormerSpouse formerSpouse : formerSpouses) {
            Person person = formerSpouse.getFormerSpouse();
            if (isPartnerCandidate(person, generation) && !order.contains(person)) {
                order.add(person);
            }
        }
        for (Person coParent : coParents) {
            if (isPartnerCandidate(coParent, generation) && !order.contains(coParent)) {
                order.add(coParent);
            }
        }
        return order;
    }

    private boolean isPartnerCandidate(@Nullable Person person, int generation) {
        if (person == null) {
            return false;
        }
        TreeNodeBox node = nodesByPerson.get(person);
        return node != null && node.generation == generation;
    }

    private boolean isFormerSpouse(Genealogy g, Person person) {
        if (person.equals(g.getSpouse())) {
            return false;
        }
        for (FormerSpouse formerSpouse : g.getFormerSpouses()) {
            if (person.equals(formerSpouse.getFormerSpouse())) {
                return true;
            }
        }
        return false;
    }

    private static void sortByBirth(List<TreeNodeBox> kids) {
        kids.sort(Comparator.comparing((TreeNodeBox kid) -> kid.person.getDateOfBirth(),
              Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /** Canonical key for a related-link pair, order-independent, so a mutual link is only recorded once. */
    private static String relatedKey(TreeNodeBox a, TreeNodeBox b) {
        String idA = a.person.getId().toString();
        String idB = b.person.getId().toString();
        return idA.compareTo(idB) <= 0 ? idA + "~" + idB : idB + "~" + idA;
    }
}
