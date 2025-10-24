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

import megamek.common.enums.Gender;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

public class FamilyTreeDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FamilyTreeDialog";

    private final EnhancedTabbedPane tabbedPane;

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

    /** Package-private so the panel can call it. */
    void openTreeFor(Person person, Collection<Person> personnel) {
        Genealogy gen = person.getGenealogy();
        if (gen != null) {
            addFamilyTreeTab(gen, personnel);
        }
    }
}

// Helper class to store layout info
class TreeNodeBox {
    Person person;
    int x, y;
    int subtreeWidth; // Dynamic width required to space children appropriately
    List<TreeNodeBox> children = new ArrayList<>();
    List<TreeNodeBox> parents = new ArrayList<>();

    TreeNodeBox(Person person) {this.person = person;}
}

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
                if (bounds != null && bounds.width > 0 && bounds.height > 0) {
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

    /** Set the parent scroll pane for zoom navigation. */
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

    private void buildAndLayoutTree(Graphics g) {
        nodeDimensions.clear();
        Map<Person, TreeNodeBox> nodeMap = new HashMap<>();
        Set<Person> visited = new HashSet<>();
        root = buildTreeRecursive(genealogy, nodeMap, visited);

        // Build parent tree upward from root
        buildParentTree(root, nodeMap, new HashSet<>());

        calculateNodeDimensions(root, g);

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

    /** Shift all nodes in the tree horizontally by the given amount. */
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

    /** Calculate the depth of the ancestor tree (how many generations up). */
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

    private void buildParentTree(TreeNodeBox node, Map<Person, TreeNodeBox> nodeMap, Set<Person> visited) {
        if (node == null || visited.contains(node.person)) {
            return;
        }
        visited.add(node.person);

        Genealogy gen = node.person.getGenealogy();
        if (gen != null) {
            List<Person> parents = gen.getParents();
            int parentCount = parents.size();

            // Add mother
            if (parentCount > 0) {
                Person mother = parents.get(0);
                if (mother != null && !visited.contains(mother)) {
                    TreeNodeBox motherBox = nodeMap.computeIfAbsent(mother, TreeNodeBox::new);
                    node.parents.add(motherBox);
                    buildParentTree(motherBox, nodeMap, visited);
                }
            }

            // Add father
            if (parentCount > 1) {
                Person father = parents.get(1);
                if (father != null && !visited.contains(father)) {
                    TreeNodeBox fatherBox = nodeMap.computeIfAbsent(father, TreeNodeBox::new);
                    node.parents.add(fatherBox);
                    buildParentTree(fatherBox, nodeMap, visited);
                }
            }
        }
    }

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

    /** Recursively computes the bounding rectangle of the tree. */
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

    private void calculateNodeDimensions(TreeNodeBox node, Graphics g) {
        if (node == null) {return;}
        String name = node.person.getFullTitle();
        String dates = getDateString(node.person);
        FontMetrics fm = g.getFontMetrics();

        // Get portrait info
        ImageIcon portraitImage = node.person.getPortraitImageIconWithFallback(true);
        int portraitW = 0, portraitH = 0;
        if (portraitImage != null) {
            portraitW = portraitImage.getIconWidth();
            portraitH = portraitImage.getIconHeight();
        }

        int paddingX = 28, paddingY = 20;
        // Calculate width to fit both name and dates
        int nameWidth = fm.stringWidth(name);
        int datesWidth = fm.stringWidth(dates);
        int textWidth = Math.max(nameWidth, datesWidth);
        int width = Math.max(textWidth + paddingX, portraitW);

        // Height now includes space for two lines of text
        int lineHeight = fm.getHeight();
        int height = (lineHeight * 2) + paddingY + (portraitH > 0 ? portraitH + 6 : 0);

        nodeDimensions.put(node, new Dimension(width, height));
        if (width > boxWidth) {boxWidth = width;}
        if (height > boxHeight) {boxHeight = height;}

        // Calculate dimensions for children
        for (TreeNodeBox child : node.children) {
            calculateNodeDimensions(child, g);
        }

        // Calculate dimensions for parents
        for (TreeNodeBox parent : node.parents) {
            if (!nodeDimensions.containsKey(parent)) {
                calculateNodeDimensions(parent, g);
            }
        }
    }

    /** Format the birth and death dates for display. */
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

    Rectangle getOriginPersonBox() {
        if (root == null) {return null;}
        Dimension boxDim = nodeDimensions.get(root);
        return new Rectangle(root.x, root.y, boxDim.width, boxDim.height);
    }

    // drawTree now draws portrait (if present) centered above the text box
    private void drawTree(Graphics2D g2d, TreeNodeBox node) {
        if (node == null) {return;}

        // First pass: Draw all lines with consistent stroke
        drawLines(g2d, node, new HashSet<>());

        // Second pass: Draw all boxes and portraits
        drawNodes(g2d, node, new HashSet<>());
    }

    /** Draw all connecting lines in the tree. */
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

    /** Draw all node boxes and portraits in the tree. */
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

    /** Get the color for a line based on the person's gender. */
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

    private Person getPersonAt(Point pt) {
        // Account for zoom when checking hit detection
        Point scaledPoint = new Point(
              (int) (pt.x / zoomFactor),
              (int) (pt.y / zoomFactor)
        );

        for (Map.Entry<Rectangle, Person> entry : rectToPerson.entrySet()) {
            if (entry.getKey().contains(scaledPoint)) {
                return entry.getValue();
            }
        }
        return null;
    }

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
