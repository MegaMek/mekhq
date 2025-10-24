/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * A dialog that displays an interactive family tree visualization.
 *
 * <p>This dialog shows a genealogical tree with the ability to:
 * <ul>
 *   <li>View ancestors (parents, grandparents, etc.) above the origin person</li>
 *   <li>View descendants (children, grandchildren, etc.) below the origin person</li>
 *   <li>Zoom in and out using the mouse wheel</li>
 *   <li>Click on any person to open their family tree in a new tab</li>
 *   <li>Navigate between multiple family trees via tabs</li>
 * </ul>
 *
 * <p>The tree uses gender-based color coding for relationship lines: pink for female, blue for male, and green for
 * non-binary.
 *
 * @author Illiani
 * @since 0.50.10
 */
public class FamilyTreeDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FamilyTreeDialog";

    private final EnhancedTabbedPane tabbedPane;

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

        // Add the initial tree as the first tab
        addFamilyTreeTab(genealogy, personnel);

        // Layout
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        // Create bottom panel with BoxLayout for vertical stacking
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        // Add label
        JLabel infoLabel = new JLabel(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.flavorText"));
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        buttonPanel.add(infoLabel);

        // Add some spacing
        buttonPanel.add(Box.createRigidArea(scaleForGUI(0, 5)));

        // Add close button
        JButton closeButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "FamilyTreeDialog.button"));
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        setPreferredSize(scaleForGUI(900, 700));
        pack();
        setLocationRelativeTo(owner);
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

        // Check if this person already has a tab open (by id)
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getTitleAt(i).equals(title)) {
                tabbedPane.setSelectedIndex(i);
                JScrollPane existingScrollPane = (JScrollPane) tabbedPane.getComponentAt(i);
                centerTreeOnOrigin(existingScrollPane);
                return;
            }
        }

        FamilyTreePanel panel = new FamilyTreePanel(genealogy, personnel, this);
        JScrollPane scrollPane = new FastJScrollPane(panel);
        scrollPane.setBorder(RoundedLineBorder.createRoundedLineBorder());
        panel.setParentScrollPane(scrollPane);
        scrollPane.setPreferredSize(scaleForGUI(800, 600));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        tabbedPane.addTab(title, scrollPane);
        tabbedPane.setSelectedComponent(scrollPane);

        // Center on the origin person when the tab is added and visible
        EventQueue.invokeLater(() -> centerTreeOnOrigin(scrollPane));
    }

    /**
     * Centers the viewport on the origin person of the family tree.
     *
     * <p>This is called when a tab is opened or switched to ensure the origin person is visible.</p>
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
                int panelW = panel.getPreferredSize().width;
                int panelH = panel.getPreferredSize().height;
                int viewW = scrollPane.getViewport().getWidth();
                int viewH = scrollPane.getViewport().getHeight();

                int personCenterX = box.x + box.width / 2;
                int personCenterY = box.y + box.height / 2;

                int targetX = personCenterX - viewW / 2;
                int targetY = personCenterY - viewH / 2;

                // Clamp to viewport and panel bounds for correct scrolling
                targetX = Math.max(0, Math.min(targetX, panelW - viewW));
                targetY = Math.max(0, Math.min(targetY, panelH - viewH));

                scrollPane.getViewport().setViewPosition(new Point(targetX, targetY));
            }
        });
    }

    /**
     * Opens a new family tree tab for the specified person.
     *
     * <p>Package-private to allow the {@link FamilyTreePanel} to open new tabs when clicking on persons.</p>
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
 * <p>Contains the person, their position, calculated subtree width, and references to children and parents.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
class TreeNodeBox {
    Person person;
    int x, y;
    int subtreeWidth; // Dynamic width required to space children appropriately
    List<TreeNodeBox> children = new ArrayList<>();
    List<TreeNodeBox> parents = new ArrayList<>();

    /**
     * Constructs a new {@link TreeNodeBox} for the specified person.
     *
     * @param person the person this node represents
     */
    TreeNodeBox(Person person) {this.person = person;}
}

/**
 * A custom {@link JPanel} that renders an interactive family tree visualization with zoom capability.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Displays both ancestors (upward) and descendants (downward) from an origin person</li>
 *   <li>Mouse wheel zooming with smooth scaling (25% to 300%)</li>
 *   <li>Click on any person to open their family tree</li>
 *   <li>Gender-coded relationship lines (pink/blue/green)</li>
 *   <li>Rounded corners and portraits for each person</li>
 *   <li>Birth and death dates displayed for each person</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.50.10
 */
class FamilyTreePanel extends JPanel {
    private final Genealogy genealogy;
    private TreeNodeBox root;
    private final int hGap = 40, vGap = 70;

    private final Map<TreeNodeBox, Dimension> nodeDimensions = new HashMap<>();
    private int boxHeight = 0;
    private int boxWidth = 0;

    private int panelWidth = 1200, panelHeight = 1000; // Will be dynamically set

    private final Map<Rectangle, Person> rectToPerson = new HashMap<>();

    // Zoom variables
    private double zoomFactor = 1.0;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_MULTIPLIER = 1.05; // 5% change per scroll notch

    private JScrollPane parentScrollPane = null;
    private Timer zoomTimer = null;

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

        setPreferredSize(new Dimension(panelWidth, panelHeight));

        // Mouse listener for clicking on persons
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                Person person = getPersonAt(evt.getPoint());
                if (person != null) {
                    // Open new tab in dialog
                    parentDialog.openTreeFor(person, personnel);
                }
            }
        });

        // Mouse wheel listener for zooming
        addMouseWheelListener(evt -> {
            double oldZoom = zoomFactor;

            if (evt.getWheelRotation() < 0) {
                // Zoom in
                zoomFactor = Math.min(MAX_ZOOM, zoomFactor * ZOOM_MULTIPLIER);
            } else {
                // Zoom out
                zoomFactor = Math.max(MIN_ZOOM, zoomFactor / ZOOM_MULTIPLIER);
            }

            if (oldZoom != zoomFactor && parentScrollPane != null) {
                Point viewPos = parentScrollPane.getViewport().getViewPosition();
                Point mousePos = evt.getPoint();

                // Calculate the mouse position relative to the content before zoom
                double contentX = (viewPos.x + mousePos.x) / oldZoom;
                double contentY = (viewPos.y + mousePos.y) / oldZoom;

                // Update panel size immediately
                Rectangle bounds = calculateTreeBounds(root);
                if (bounds.width > 0 && bounds.height > 0) {
                    panelWidth = (int) ((bounds.x + bounds.width + scaleForGUI(40)) * zoomFactor);
                    panelHeight = (int) ((bounds.y + bounds.height + scaleForGUI(40)) * zoomFactor);
                    setPreferredSize(new Dimension(panelWidth, panelHeight));
                }

                // Calculate new viewport position to keep content under mouse
                int newX = (int) (contentX * zoomFactor - mousePos.x);
                int newY = (int) (contentY * zoomFactor - mousePos.y);

                // Clamp to valid bounds
                Dimension viewSize = parentScrollPane.getViewport().getExtentSize();
                Dimension contentSize = getPreferredSize();

                newX = Math.max(0, Math.min(newX, contentSize.width - viewSize.width));
                newY = Math.max(0, Math.min(newY, contentSize.height - viewSize.height));

                parentScrollPane.getViewport().setViewPosition(new Point(newX, newY));

                // Batch revalidate calls with a timer to avoid excessive updates
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
            }
        });
    }

    /**
     * Sets the parent scroll pane for zoom navigation.
     *
     * <p>Required to properly adjust the viewport position during zoom operations.</p>
     *
     * @param scrollPane the scroll pane that contains this panel
     *
     * @author Illiani
     * @since 0.50.10
     */
    void setParentScrollPane(JScrollPane scrollPane) {
        this.parentScrollPane = scrollPane;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();

        // Apply zoom transformation
        g2d.scale(zoomFactor, zoomFactor);

        rectToPerson.clear(); // Clear hitboxes before drawing
        buildAndLayoutTree(g2d);
        if (root != null) {
            drawTree(g2d, root);
        }

        g2d.dispose();
    }

    /**
     * Builds and lays out the entire family tree, calculating positions and dimensions for all nodes.
     *
     * <p>This includes both ancestor and descendant branches.</p>
     *
     * @param graphics the graphics context used for font metrics calculations
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void buildAndLayoutTree(Graphics graphics) {
        nodeDimensions.clear();
        Map<Person, TreeNodeBox> nodeMap = new HashMap<>();
        Set<Person> visited = new HashSet<>();
        root = buildTreeRecursive(genealogy, nodeMap, visited);

        // Build parent tree upward from root
        buildParentTree(root, nodeMap, new HashSet<>());

        calculateNodeDimensions(root, graphics);

        // First, compute each node's subtree width recursively
        computeSubtreeWidth(root);

        // Compute parent tree width
        computeParentTreeWidth(root);

        // Calculate how many ancestor generations we have to determine vertical offset
        int rootLevel = calculateAncestorDepth(root); // Root will be at this level (0-indexed from top)

        // Then assign coords based on subtree widths
        int startingX = scaleForGUI(20); // Leftmost padding

        // Position the root at the calculated level, then descendants below
        assignCoordsWithSubtreeSpacing(root, rootLevel, startingX);

        // Assign coords for ancestors (going upward from root)
        assignParentCoords(root, rootLevel - 1);

        // Calculate bounds to find if any nodes went negative
        Rectangle bounds = calculateTreeBounds(root);

        // If tree extends into negative X, shift everything right
        if (bounds.x < 0) {
            int shiftX = scaleForGUI(20) - bounds.x; // Shift to have 20px left padding
            shiftTreeHorizontally(root, shiftX, new HashSet<>());

            // Recalculate bounds after shift
            bounds = calculateTreeBounds(root);
        }

        // Now dynamically set preferred size to fit the tree (scaled by zoom)
        panelWidth = (int) ((bounds.x + bounds.width + scaleForGUI(40)) * zoomFactor);
        panelHeight = (int) ((bounds.y + bounds.height + scaleForGUI(40)) * zoomFactor);
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        revalidate(); // Tell scrollpane the preferred size has changed
    }

    /**
     * Recursively shifts all nodes in the tree horizontally by the specified amount.
     *
     * <p>Used to ensure all nodes have positive X coordinates with proper padding.</p>
     *
     * @param node    the starting node for the shift operation
     * @param shiftX  the horizontal shift amount in pixels
     * @param visited set of already visited nodes to prevent infinite loops
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void shiftTreeHorizontally(TreeNodeBox node, int shiftX, Set<TreeNodeBox> visited) {
        if (node == null || visited.contains(node)) {
            return;
        }
        visited.add(node);

        node.x += shiftX;

        for (TreeNodeBox child : node.children) {
            shiftTreeHorizontally(child, shiftX, visited);
        }

        for (TreeNodeBox parent : node.parents) {
            shiftTreeHorizontally(parent, shiftX, visited);
        }
    }

    /**
     * Calculates the depth of the ancestor tree (number of generations upward from the given node).
     *
     * @param node the node to calculate ancestor depth from
     *
     * @return the maximum number of generations of ancestors, or 0 if none
     *
     * @author Illiani
     * @since 0.50.10
     */
    private int calculateAncestorDepth(TreeNodeBox node) {
        if (node == null || node.parents.isEmpty()) {
            return 0;
        }
        int maxDepth = 0;
        for (TreeNodeBox parent : node.parents) {
            maxDepth = Math.max(maxDepth, calculateAncestorDepth(parent));
        }
        return maxDepth + 1;
    }

    /**
     * Recursively builds the parent/ancestor tree upward from the specified node.
     *
     * <p>Adds mother and father nodes and their ancestors to the tree structure.</p>
     *
     * @param node    the node to build parents for
     * @param nodeMap map of persons to their tree nodes
     * @param visited set of already visited persons to prevent infinite loops
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void buildParentTree(TreeNodeBox node, Map<Person, TreeNodeBox> nodeMap, Set<Person> visited) {
        if (node == null || visited.contains(node.person)) {
            return;
        }
        visited.add(node.person);

        Genealogy gen = node.person.getGenealogy();
        if (gen != null) {
            List<Person> parents = gen.getParents();
            int parentCount = parents.size();

            // Add the first parent, if any
            if (parentCount > 0) {
                Person parent0 = parents.get(0);
                if (parent0 != null && !visited.contains(parent0)) {
                    TreeNodeBox parent0Box = nodeMap.computeIfAbsent(parent0, TreeNodeBox::new);
                    node.parents.add(parent0Box);
                    buildParentTree(parent0Box, nodeMap, visited);
                }
            }

            // Add a second parent, if any
            if (parentCount > 1) {
                Person parent1 = parents.get(1);
                if (parent1 != null && !visited.contains(parent1)) {
                    TreeNodeBox parent1Box = nodeMap.computeIfAbsent(parent1, TreeNodeBox::new);
                    node.parents.add(parent1Box);
                    buildParentTree(parent1Box, nodeMap, visited);
                }
            }
        }
    }

    /**
     * Recursively computes the width required for the parent/ancestor subtree.
     *
     * <p>Adjusts the node's subtreeWidth to accommodate all parent branches.</p>
     *
     * @param node the node to compute parent tree width for
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void computeParentTreeWidth(TreeNodeBox node) {
        if (node == null) {
            return;
        }

        // First, recursively compute subtree widths for all parents and their ancestors
        for (TreeNodeBox parent : node.parents) {
            computeParentTreeWidth(parent);
        }

        // Now compute subtree width for each parent at this level
        for (TreeNodeBox parent : node.parents) {
            if (parent.parents.isEmpty()) {
                // Leaf parent (oldest ancestor) - width is just the box width
                parent.subtreeWidth = nodeDimensions.get(parent).width;
            } else {
                // Parent has parents - compute width based on their subtree widths
                int width = 0;
                for (TreeNodeBox grandparent : parent.parents) {
                    width += grandparent.subtreeWidth;
                }
                width += hGap * Math.max(0, parent.parents.size() - 1);
                parent.subtreeWidth = Math.max(width, nodeDimensions.get(parent).width);
            }
        }

        // Adjust current node's subtree width to accommodate parents if needed
        if (!node.parents.isEmpty()) {
            int parentsWidth = 0;
            for (TreeNodeBox parent : node.parents) {
                parentsWidth += parent.subtreeWidth;
            }
            parentsWidth += hGap * Math.max(0, node.parents.size() - 1);
            node.subtreeWidth = Math.max(node.subtreeWidth, parentsWidth);
        }
    }

    /**
     * Recursively assigns X and Y coordinates to parent/ancestor nodes.
     *
     * <p>Parents are centered above their children with appropriate spacing.</p>
     *
     * @param node  the node whose parents should be positioned
     * @param level the vertical level (generation) to place parents at
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void assignParentCoords(TreeNodeBox node, int level) {
        if (node == null || node.parents.isEmpty()) {
            return;
        }

        Dimension boxDim = nodeDimensions.get(node);
        int nodeBoxWidth = boxDim.width;

        // Calculate total width of all parents
        int parentsWidth = 0;
        for (TreeNodeBox parent : node.parents) {
            parentsWidth += parent.subtreeWidth;
        }
        parentsWidth += hGap * Math.max(0, node.parents.size() - 1);

        // Center parents above the current node
        int nodeCenter = node.x + nodeBoxWidth / 2;

        int parentX = nodeCenter - parentsWidth / 2;
        for (TreeNodeBox parent : node.parents) {
            Dimension parentBoxDim = nodeDimensions.get(parent);
            int parentSubtreeWidth = parent.subtreeWidth;
            int parentBoxWidth = parentBoxDim.width;

            // Center parent box within its subtree
            parent.x = parentX + parentSubtreeWidth / 2 - parentBoxWidth / 2;
            parent.y = level * (boxHeight + vGap);

            // Recursively assign coords to this parent's parents
            assignParentCoords(parent, level - 1);

            parentX += parentSubtreeWidth + hGap;
        }
    }

    /**
     * Recursively computes the bounding rectangle that encompasses the entire tree.
     *
     * <p>Includes both ancestor and descendant branches.</p>
     *
     * @param node the starting node for bounds calculation
     *
     * @return a {@link Rectangle} representing the minimum bounding box for the tree
     *
     * @author Illiani
     * @since 0.50.10
     */
    private Rectangle calculateTreeBounds(TreeNodeBox node) {
        if (node == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        Dimension d = nodeDimensions.get(node);
        int minX = node.x, minY = node.y;
        int maxX = node.x + d.width, maxY = node.y + d.height;

        // Include children
        for (TreeNodeBox child : node.children) {
            Rectangle childBounds = calculateTreeBounds(child);
            minX = Math.min(minX, childBounds.x);
            minY = Math.min(minY, childBounds.y);
            maxX = Math.max(maxX, childBounds.x + childBounds.width);
            maxY = Math.max(maxY, childBounds.y + childBounds.height);
        }

        // Include parents
        for (TreeNodeBox parent : node.parents) {
            Rectangle parentBounds = calculateTreeBounds(parent);
            minX = Math.min(minX, parentBounds.x);
            minY = Math.min(minY, parentBounds.y);
            maxX = Math.max(maxX, parentBounds.x + parentBounds.width);
            maxY = Math.max(maxY, parentBounds.y + parentBounds.height);
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Recursively calculates and stores the dimensions (width and height) for each node.
     *
     * <p>Takes into account name text, dates, portrait size, and padding.</p>
     *
     * @param node     the node to calculate dimensions for
     * @param graphics the graphics context used for font metrics
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void calculateNodeDimensions(TreeNodeBox node, Graphics graphics) {
        if (node == null) {return;}
        String name = node.person.getFullTitle();
        String dates = getDateString(node.person);
        FontMetrics fontMetrics = graphics.getFontMetrics();

        // Get portrait info
        ImageIcon portraitImage = node.person.getPortraitImageIconWithFallback(true);
        int portraitW = 0, portraitH = 0;
        if (portraitImage != null) {
            portraitW = portraitImage.getIconWidth();
            portraitH = portraitImage.getIconHeight();
        }

        int paddingX = 28, paddingY = 20;
        // Calculate width to fit both name and dates
        int nameWidth = fontMetrics.stringWidth(name);
        int datesWidth = fontMetrics.stringWidth(dates);
        int textWidth = Math.max(nameWidth, datesWidth);
        int width = Math.max(textWidth + paddingX, portraitW);

        // Height now includes space for two lines of text
        int lineHeight = fontMetrics.getHeight();
        int height = (lineHeight * 2) + paddingY + (portraitH > 0 ? portraitH + 6 : 0);

        nodeDimensions.put(node, new Dimension(width, height));
        if (width > boxWidth) {boxWidth = width;}
        if (height > boxHeight) {boxHeight = height;}

        // Calculate dimensions for children
        for (TreeNodeBox child : node.children) {
            calculateNodeDimensions(child, graphics);
        }

        // Calculate dimensions for parents
        for (TreeNodeBox parent : node.parents) {
            if (!nodeDimensions.containsKey(parent)) {
                calculateNodeDimensions(parent, graphics);
            }
        }
    }

    /**
     * Formats the birth and death dates for display.
     *
     * @param person the person whose dates should be formatted
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
     * Gets the bounding rectangle for the origin person's node.
     *
     * <p>Used for centering the viewport on the origin person.</p>
     *
     * @return a {@link Rectangle} representing the origin person's position and size, or null if no root exists
     *
     * @author Illiani
     * @since 0.50.10
     */
    Rectangle getOriginPersonBox() {
        if (root == null) {return null;}
        Dimension boxDim = nodeDimensions.get(root);
        return new Rectangle(root.x, root.y, boxDim.width, boxDim.height);
    }

    /**
     * Draws the entire family tree by first drawing all connecting lines, then all person nodes.
     *
     * @param g2d  the graphics context to draw with
     * @param node the root node of the tree to draw
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void drawTree(Graphics2D g2d, TreeNodeBox node) {
        if (node == null) {return;}

        // First pass: Draw all lines with consistent stroke
        drawLines(g2d, node, new HashSet<>());

        // Second pass: Draw all boxes and portraits
        drawNodes(g2d, node, new HashSet<>());
    }

    /**
     * Recursively draws all connecting lines between nodes in the tree.
     *
     * <p>Lines are color-coded based on the child/parent's gender.</p>
     *
     * @param g2d     the graphics context to draw with
     * @param node    the current node being processed
     * @param visited set of already visited nodes to prevent duplicate drawing
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void drawLines(Graphics2D g2d, TreeNodeBox node, Set<TreeNodeBox> visited) {
        if (node == null || visited.contains(node)) {
            return;
        }
        visited.add(node);

        Dimension boxDim = nodeDimensions.get(node);
        int nodeBoxWidth = boxDim.width;
        int nodeBoxHeight = boxDim.height;

        // Enable anti-aliasing for smooth lines
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
              RenderingHints.VALUE_ANTIALIAS_ON);

        // Set line thickness to 5px with rounded caps and joins
        g2d.setStroke(new BasicStroke(5,
              BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_ROUND));

        // Draw lines to children
        for (TreeNodeBox child : node.children) {
            Dimension childBoxDim = nodeDimensions.get(child);

            // Set color based on child's gender
            g2d.setColor(getGenderColor(child.person));

            g2d.drawLine(
                  node.x + nodeBoxWidth / 2, node.y + nodeBoxHeight,
                  child.x + childBoxDim.width / 2, child.y
            );

            drawLines(g2d, child, visited);
        }

        // Draw lines to parents
        for (TreeNodeBox parent : node.parents) {
            Dimension parentBoxDim = nodeDimensions.get(parent);

            // Set color based on parent's gender
            g2d.setColor(getGenderColor(parent.person));

            g2d.drawLine(
                  node.x + nodeBoxWidth / 2, node.y,
                  parent.x + parentBoxDim.width / 2, parent.y + parentBoxDim.height
            );

            drawLines(g2d, parent, visited);
        }
    }

    /**
     * Recursively draws all person nodes including portraits, boxes, names, and dates.
     *
     * <p>Also registers click regions for each person.</p>
     *
     * @param g2d     the graphics context to draw with
     * @param node    the current node being processed
     * @param visited set of already visited nodes to prevent duplicate drawing
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void drawNodes(Graphics2D g2d, TreeNodeBox node, Set<TreeNodeBox> visited) {
        if (node == null || visited.contains(node)) {
            return;
        }
        visited.add(node);

        Dimension boxDim = nodeDimensions.get(node);
        int nodeBoxWidth = boxDim.width;
        int nodeBoxHeight = boxDim.height;

        // Enable anti-aliasing for smooth rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
              RenderingHints.VALUE_ANTIALIAS_ON);

        // --- Portrait drawing logic ---
        ImageIcon portraitImage = node.person.getPortraitImageIconWithFallback(true);
        int portraitW, portraitH = 0;
        int portraitPadBtm = 6;
        if (portraitImage != null) {
            portraitW = portraitImage.getIconWidth();
            portraitH = portraitImage.getIconHeight();
            if (portraitW > 0 && portraitH > 0) {
                int px = node.x + (nodeBoxWidth - portraitW) / 2;
                int py = node.y;
                g2d.drawImage(portraitImage.getImage(), px, py, null);
            }
        }

        int boxY = node.y + (portraitH > 0 ? portraitH + portraitPadBtm : 0);
        int boxDrawHeight = nodeBoxHeight - (portraitH > 0 ? portraitH + portraitPadBtm : 0);

        // Arc radius for rounded corners
        int arc = 16;
        int borderThickness = 2;

        // Draw person box with rounded corners
        g2d.setColor(new Color(230, 240, 255));
        g2d.fillRoundRect(node.x, boxY, nodeBoxWidth, boxDrawHeight, arc, arc);

        // Draw rounded border
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(borderThickness));
        g2d.drawRoundRect(node.x, boxY, nodeBoxWidth, boxDrawHeight, arc, arc);

        // Reset stroke for text
        g2d.setStroke(new BasicStroke(1));

        // Draw name and dates text
        g2d.setColor(Color.BLACK);
        FontMetrics fm = g2d.getFontMetrics();
        int lineHeight = fm.getHeight();

        String name = node.person.getFullTitle();
        String dates = getDateString(node.person);

        // Center the text block vertically in the box
        int textBlockHeight = lineHeight * 2;
        int textStartY = boxY + (boxDrawHeight - textBlockHeight) / 2 + fm.getAscent();

        // Draw name (centered horizontally)
        int nameWidth = fm.stringWidth(name);
        int nameX = node.x + (nodeBoxWidth - nameWidth) / 2;
        g2d.drawString(name, nameX, textStartY);

        // Draw dates below name (centered horizontally)
        int datesWidth = fm.stringWidth(dates);
        int datesX = node.x + (nodeBoxWidth - datesWidth) / 2;
        g2d.drawString(dates, datesX, textStartY + lineHeight);

        // Create hit area that includes portrait + box + name (generously)
        int clickableTop = node.y;
        rectToPerson.put(
              new Rectangle(node.x,
                    clickableTop,
                    nodeBoxWidth,
                    (Math.max(portraitH, 0)) + boxDrawHeight + portraitPadBtm + 2),
              node.person
        );

        // Recursively draw children and parents
        for (TreeNodeBox child : node.children) {
            drawNodes(g2d, child, visited);
        }

        for (TreeNodeBox parent : node.parents) {
            drawNodes(g2d, parent, visited);
        }
    }

    /**
     * Returns the color for a relationship line based on the person's gender.
     *
     * @param person the person whose gender determines the color
     *
     * @return light green for non-binary, pink for female, light blue for male
     *
     * @author Illiani
     * @since 0.50.10
     */
    private Color getGenderColor(Person person) {
        Gender gender = person.getGender();
        if (gender.isGenderNeutral()) {
            return new Color(144, 238, 144); // Light green
        }

        if (gender.isFemale()) {
            return new Color(255, 182, 193); // Pink
        }

        return new Color(135, 206, 250); // Light blue
    }

    /**
     * Finds the person at the specified screen coordinates, accounting for zoom level.
     *
     * @param point the point to check for a person
     *
     * @return the Person at that location, or {@code null} if none found
     *
     * @author Illiani
     * @since 0.50.10
     */
    private @Nullable Person getPersonAt(Point point) {
        // Account for zoom when checking hit detection
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

    /**
     * Recursively computes the subtree width required to properly space child nodes.
     *
     * <p>A node's subtree width is the sum of all child subtree widths plus gaps.</p>
     *
     * @param node the node to compute subtree width for
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void computeSubtreeWidth(TreeNodeBox node) {
        if (node == null) {return;}
        // If leaf, subtree width is its box width
        if (node.children.isEmpty()) {
            node.subtreeWidth = nodeDimensions.get(node).width;
        } else {
            int width = 0;
            for (TreeNodeBox child : node.children) {
                computeSubtreeWidth(child);
                width += child.subtreeWidth;
            }
            width += hGap * (node.children.size() - 1); // gap between child subtrees
            // Make sure parent is at least as wide as box
            node.subtreeWidth = Math.max(width, nodeDimensions.get(node).width);
        }
    }

    /**
     * Recursively assigns X and Y coordinates to nodes in the descendant tree.
     *
     * <p>Nodes are centered above their children with appropriate horizontal spacing.</p>
     *
     * @param node  the node to assign coordinates to
     * @param level the vertical level (generation) to place this node at
     * @param leftX the leftmost X coordinate for this node's subtree
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void assignCoordsWithSubtreeSpacing(TreeNodeBox node, int level, int leftX) {
        if (node == null) {return;}
        Dimension boxDim = nodeDimensions.get(node);
        int nodeBoxWidth = boxDim.width;

        // Center the node above its children/subtree, or just at leftX if leaf
        if (node.children.isEmpty()) {
            node.x = leftX;
        } else {
            int subtreeWidth = node.subtreeWidth;
            int nodeCenter = leftX + subtreeWidth / 2;
            node.x = nodeCenter - nodeBoxWidth / 2;
        }
        node.y = level * (boxHeight + vGap);

        // Place children below, distributed horizontally
        int childX = leftX;
        for (TreeNodeBox child : node.children) {
            assignCoordsWithSubtreeSpacing(child, level + 1, childX);
            childX += child.subtreeWidth + hGap;
        }
    }

    /**
     * Recursively builds the tree structure for descendants of the given genealogy.
     *
     * <p>Creates {@link TreeNodeBox} nodes for the person and all their children.</p>
     *
     * @param genealogy the genealogy to build from
     * @param nodeMap   map of persons to their tree nodes
     * @param visited   set of already visited persons to prevent infinite loops
     *
     * @return the {@link TreeNodeBox} for the origin person, or null if already visited
     *
     * @author Illiani
     * @since 0.50.10
     */
    private TreeNodeBox buildTreeRecursive(Genealogy genealogy, Map<Person, TreeNodeBox> nodeMap, Set<Person> visited) {
        Person person = genealogy.getOrigin();
        if (visited.contains(person)) {return null;}
        visited.add(person);

        TreeNodeBox node = nodeMap.computeIfAbsent(person, TreeNodeBox::new);

        for (Person child : genealogy.getChildren()) {
            TreeNodeBox childBox = buildTreeRecursive(child.getGenealogy(), nodeMap, visited);
            if (childBox != null) {
                node.children.add(childBox);
            }
        }
        return node;
    }
}
