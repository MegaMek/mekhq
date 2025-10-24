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
        root = buildTreeRecursive(genealogy, nodeMap, new HashSet<>());
        calculateNodeDimensions(root, g);

        // First, compute each node's subtree width recursively
        computeSubtreeWidth(root);

        // Then assign coords based on subtree widths
        int startingX = 20; // Leftmost padding
        assignCoordsWithSubtreeSpacing(root, 0, startingX);

        // Now dynamically set preferred size to fit the tree
        Rectangle bounds = calculateTreeBounds(root);
        panelWidth = bounds.x + bounds.width + 40;
        panelHeight = bounds.y + bounds.height + 40;
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        revalidate(); // Tell scrollpane the preferred size has changed
    }

    /** Recursively computes the bounding rectangle of the tree. */
    private Rectangle calculateTreeBounds(TreeNodeBox node) {
        if (node == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        Dimension d = nodeDimensions.get(node);
        int minX = node.x, minY = node.y;
        int maxX = node.x + d.width, maxY = node.y + d.height;
        for (TreeNodeBox child : node.children) {
            Rectangle childBounds = calculateTreeBounds(child);
            minX = Math.min(minX, childBounds.x);
            minY = Math.min(minY, childBounds.y);
            maxX = Math.max(maxX, childBounds.x + childBounds.width);
            maxY = Math.max(maxY, childBounds.y + childBounds.height);
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
        for (TreeNodeBox child : node.children) {
            calculateNodeDimensions(child, g);
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
        Dimension boxDim = nodeDimensions.get(node);
        int nodeBoxWidth = boxDim.width;
        int nodeBoxHeight = boxDim.height;

        // Draw lines to children
        for (TreeNodeBox child : node.children) {
            Dimension childBoxDim = nodeDimensions.get(child);
            g.drawLine(
                  node.x + nodeBoxWidth / 2, node.y + nodeBoxHeight,
                  child.x + childBoxDim.width / 2, child.y
            );
            drawTree(g, child);
        }

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
