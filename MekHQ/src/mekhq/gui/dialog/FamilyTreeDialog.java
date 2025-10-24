package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.common.enums.Gender;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.familyTree.Genealogy;

public class FamilyTreeDialog extends JDialog {
    private final EnhancedTabbedPane tabbedPane;

    public FamilyTreeDialog(Frame owner, Genealogy genealogy, Collection<Person> personnel) {
        super(owner, "Family Tree", true);

        tabbedPane = new EnhancedTabbedPane();

        // Add the initial tree as the first tab
        addFamilyTreeTab(genealogy, personnel);

        // Layout
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(900, 700));
        pack();
        setLocationRelativeTo(owner);
    }

    /** Add a new tab for the given genealogy if not already open. */
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
        scrollPane.setPreferredSize(new Dimension(800, 600));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        tabbedPane.addTab(title, scrollPane);
        tabbedPane.setSelectedComponent(scrollPane);

        // Center on the origin person when the tab is added and visible
        java.awt.EventQueue.invokeLater(() -> centerTreeOnOrigin(scrollPane));
    }

    /** Ensures the scroll pane is centered on the person for the current tree. */
    private void centerTreeOnOrigin(JScrollPane scrollPane) {
        if (!(scrollPane.getViewport().getView() instanceof FamilyTreePanel)) {return;}
        FamilyTreePanel panel = (FamilyTreePanel) scrollPane.getViewport().getView();

        // Use invokeLater so this runs AFTER the next layout/paint event and any scroll snaps
        java.awt.EventQueue.invokeLater(() -> {
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

                scrollPane.getViewport().setViewPosition(new java.awt.Point(targetX, targetY));
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
    List<TreeNodeBox> parents = new ArrayList<>(); // Add parents list

    TreeNodeBox(Person person) {this.person = person;}
}

class FamilyTreePanel extends JPanel {
    private final Genealogy genealogy;
    private final Collection<Person> personnel;
    private final FamilyTreeDialog parentDialog;
    private TreeNodeBox root;
    private final int hGap = 40, vGap = 70; // Increased for clarity

    private Map<TreeNodeBox, Dimension> nodeDimensions = new HashMap<>();
    private int boxHeight = 0;
    private int boxWidth = 0;

    private int panelWidth = 1200, panelHeight = 1000; // Will be dynamically set

    private final Map<Rectangle, Person> rectToPerson = new HashMap<>();

    public FamilyTreePanel(Genealogy genealogy, Collection<Person> personnel, FamilyTreeDialog parentDialog) {
        this.genealogy = genealogy;
        this.personnel = personnel;
        this.parentDialog = parentDialog;

        setPreferredSize(new Dimension(panelWidth, panelHeight));

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Person person = getPersonAt(evt.getPoint());
                if (person != null) {
                    // Open new tab in dialog
                    parentDialog.openTreeFor(person, personnel);
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        rectToPerson.clear(); // Clear hitboxes before drawing
        buildAndLayoutTree(g);
        if (root != null) {
            drawTree((Graphics2D) g, root);
        }
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
        int ancestorDepth = calculateAncestorDepth(root);
        int rootLevel = ancestorDepth; // Root will be at this level (0-indexed from top)

        // Then assign coords based on subtree widths
        int startingX = 20; // Leftmost padding

        // Position the root at the calculated level, then descendants below
        assignCoordsWithSubtreeSpacing(root, rootLevel, startingX);

        // Assign coords for ancestors (going upward from root)
        assignParentCoords(root, rootLevel - 1, startingX);

        // Calculate bounds to find if any nodes went negative
        Rectangle bounds = calculateTreeBounds(root);

        // If tree extends into negative X, shift everything right
        if (bounds.x < 0) {
            int shiftX = 20 - bounds.x; // Shift to have 20px left padding
            shiftTreeHorizontally(root, shiftX, new HashSet<>());

            // Recalculate bounds after shift
            bounds = calculateTreeBounds(root);
        }

        // Now dynamically set preferred size to fit the tree
        panelWidth = bounds.x + bounds.width + 40;
        panelHeight = bounds.y + bounds.height + 40;
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

    private void assignParentCoords(TreeNodeBox node, int level, int leftX) {
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
        int parentsStartX = nodeCenter - parentsWidth / 2;

        int parentX = parentsStartX;
        for (TreeNodeBox parent : node.parents) {
            Dimension parentBoxDim = nodeDimensions.get(parent);
            int parentSubtreeWidth = parent.subtreeWidth;
            int parentBoxWidth = parentBoxDim.width;

            // Center parent box within its subtree
            parent.x = parentX + parentSubtreeWidth / 2 - parentBoxWidth / 2;
            parent.y = level * (boxHeight + vGap);

            // Recursively assign coords to this parent's parents
            assignParentCoords(parent, level - 1, parentX);

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
        FontMetrics fm = g.getFontMetrics();

        // Get portrait info
        ImageIcon portraitImage = node.person.getPortraitImageIconWithFallback(true);
        int portraitW = 0, portraitH = 0;
        if (portraitImage != null) {
            portraitW = portraitImage.getIconWidth();
            portraitH = portraitImage.getIconHeight();
        }

        int paddingX = 28, paddingY = 12;
        int width = Math.max(fm.stringWidth(name) + paddingX, portraitW);
        int height = fm.getHeight() + paddingY + (portraitH > 0 ? portraitH + 6 : 0);

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

    Rectangle getOriginPersonBox() {
        if (root == null) {return null;}
        Dimension boxDim = nodeDimensions.get(root);
        return new Rectangle(root.x, root.y, boxDim.width, boxDim.height);
    }

    // drawTree now draws portrait (if present) centered above the text box
    private void drawTree(Graphics2D g, TreeNodeBox node) {
        if (node == null) {return;}

        // First pass: Draw all lines with consistent stroke
        drawLines(g, node, new HashSet<>());

        // Second pass: Draw all boxes and portraits
        drawNodes(g, node, new HashSet<>());
    }

    /** Draw all connecting lines in the tree. */
    private void drawLines(Graphics2D g, TreeNodeBox node, Set<TreeNodeBox> visited) {
        if (node == null || visited.contains(node)) {
            return;
        }
        visited.add(node);

        Dimension boxDim = nodeDimensions.get(node);
        int nodeBoxWidth = boxDim.width;
        int nodeBoxHeight = boxDim.height;

        // Set line thickness to 5px
        g.setStroke(new java.awt.BasicStroke(5));

        // Draw lines to children
        for (TreeNodeBox child : node.children) {
            Dimension childBoxDim = nodeDimensions.get(child);

            // Set color based on child's gender
            g.setColor(getGenderColor(child.person));

            g.drawLine(
                  node.x + nodeBoxWidth / 2, node.y + nodeBoxHeight,
                  child.x + childBoxDim.width / 2, child.y
            );

            drawLines(g, child, visited);
        }

        // Draw lines to parents
        for (TreeNodeBox parent : node.parents) {
            Dimension parentBoxDim = nodeDimensions.get(parent);

            // Set color based on parent's gender
            g.setColor(getGenderColor(parent.person));

            g.drawLine(
                  node.x + nodeBoxWidth / 2, node.y,
                  parent.x + parentBoxDim.width / 2, parent.y + parentBoxDim.height
            );

            drawLines(g, parent, visited);
        }
    }

    /** Draw all node boxes and portraits in the tree. */
    private void drawNodes(Graphics2D g, TreeNodeBox node, Set<TreeNodeBox> visited) {
        if (node == null || visited.contains(node)) {
            return;
        }
        visited.add(node);

        Dimension boxDim = nodeDimensions.get(node);
        int nodeBoxWidth = boxDim.width;
        int nodeBoxHeight = boxDim.height;

        // Reset stroke to default for drawing boxes
        g.setStroke(new java.awt.BasicStroke(1));

        // --- Portrait drawing logic ---
        ImageIcon portraitImage = node.person.getPortraitImageIconWithFallback(true);
        int portraitW = 0, portraitH = 0;
        int portraitPadBtm = 6;
        if (portraitImage != null) {
            portraitW = portraitImage.getIconWidth();
            portraitH = portraitImage.getIconHeight();
            if (portraitW > 0 && portraitH > 0) {
                int px = node.x + (nodeBoxWidth - portraitW) / 2;
                int py = node.y;
                g.drawImage(portraitImage.getImage(), px, py, null);
            }
        }

        int boxY = node.y + (portraitH > 0 ? portraitH + portraitPadBtm : 0);

        // Draw person box with button feel
        g.setColor(new Color(230, 240, 255));
        g.fillRect(node.x, boxY, nodeBoxWidth, nodeBoxHeight - (portraitH > 0 ? portraitH + portraitPadBtm : 0));
        g.setColor(Color.BLACK);
        g.drawRect(node.x, boxY, nodeBoxWidth, nodeBoxHeight - (portraitH > 0 ? portraitH + portraitPadBtm : 0));
        String name = node.person.getFullTitle();
        g.drawString(
              name,
              node.x + 14,
              boxY +
                    (nodeBoxHeight - (portraitH > 0 ? portraitH + portraitPadBtm : 0)) / 2 +
                    g.getFontMetrics().getAscent() / 3
        );

        // Create hit area that includes portrait + box + name (generously)
        int clickableTop = node.y;
        int clickableHeight = boxY + (nodeBoxHeight - (portraitH > 0 ? portraitH + portraitPadBtm : 0)) - node.y + 2;
        rectToPerson.put(
              new Rectangle(node.x,
                    clickableTop,
                    nodeBoxWidth,
                    (portraitH > 0 ? portraitH : 0) +
                          (boxDim.height - (portraitH > 0 ? portraitH + portraitPadBtm : 0)) +
                          portraitPadBtm +
                          2),
              node.person
        );

        // Recursively draw children and parents
        for (TreeNodeBox child : node.children) {
            drawNodes(g, child, visited);
        }

        for (TreeNodeBox parent : node.parents) {
            drawNodes(g, parent, visited);
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

    private Person getPersonAt(java.awt.Point pt) {
        for (Map.Entry<Rectangle, Person> entry : rectToPerson.entrySet()) {
            if (entry.getKey().contains(pt)) {
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
        int nodeBoxHeight = boxDim.height;
        int childrenTotalWidth = 0;
        for (TreeNodeBox child : node.children) {
            childrenTotalWidth += child.subtreeWidth;
        }
        childrenTotalWidth += hGap * Math.max(0, node.children.size() - 1);

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
