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
 * of The Topps Company Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.campaignOptions;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * Left-side navigation tree and filter for the Campaign Options dialog.
 */
class CampaignOptionsNavigationPanel extends JPanel {
    private static final int NAVIGATION_WIDTH = 240;
    private static final int SCROLL_SPEED = 16;

    private final List<CampaignOptionsRoute> routes;
    private final Consumer<CampaignOptionsRoute> routeSelectionListener;
    private final JTextField filterField;
    private final JTree navigationTree;
    private boolean isSyncingSelection;

    CampaignOptionsNavigationPanel(List<CampaignOptionsRoute> routes,
          Consumer<CampaignOptionsRoute> routeSelectionListener) {
        super(new BorderLayout());
        this.routes = routes;
        this.routeSelectionListener = routeSelectionListener;

        setName("campaignOptionsNavigationPanel");
        String navigationTitle = getTextAt(getCampaignOptionsResourceBundle(), "campaignOptionsNavigation.title");
        setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder(navigationTitle),
              BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        setPreferredSize(new Dimension(NAVIGATION_WIDTH, 1));

        filterField = new JTextField();
        filterField.setName("txtCampaignOptionsFilter");
        filterField.putClientProperty("JTextField.placeholderText",
              getTextAt(getCampaignOptionsResourceBundle(), "txtCampaignOptionsFilter.text"));
        filterField.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(), "txtCampaignOptionsFilter.tooltip"));
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                filterNavigation();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                filterNavigation();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                filterNavigation();
            }
        });

        navigationTree = new JTree();
        navigationTree.setName("campaignOptionsNavigationTree");
        navigationTree.setRootVisible(false);
        navigationTree.setShowsRootHandles(true);
        navigationTree.addTreeSelectionListener(evt -> notifySelectedRoute());

        JScrollPane navigationScrollPane = new JScrollPane(navigationTree,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navigationScrollPane.setName("campaignOptionsNavigationScrollPane");
        navigationScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_SPEED);

        add(filterField, BorderLayout.NORTH);
        add(navigationScrollPane, BorderLayout.CENTER);

        buildNavigationTree("");
    }

    void selectRoute(CampaignOptionsRoute route) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) navigationTree.getModel().getRoot();
        DefaultMutableTreeNode routeNode = findNavigationNode(root, route);
        if (routeNode == null) {
            return;
        }

        TreePath treePath = new TreePath(routeNode.getPath());
        if (!treePath.equals(navigationTree.getSelectionPath())) {
            isSyncingSelection = true;
            try {
                navigationTree.setSelectionPath(treePath);
            } finally {
                isSyncingSelection = false;
            }
        }
    }

    private void notifySelectedRoute() {
        if (isSyncingSelection) {
            return;
        }

        CampaignOptionsRoute selectedRoute = getSelectedRoute();
        if (selectedRoute != null) {
            routeSelectionListener.accept(selectedRoute);
        }
    }

    private void filterNavigation() {
        buildNavigationTree(filterField.getText());
    }

    private void buildNavigationTree(String filterText) {
        String normalizedFilter = filterText == null ? "" : filterText.trim().toLowerCase();
        CampaignOptionsRoute selectedRoute = getSelectedRoute();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NavigationTreeNode("CampaignOptionsRoot", null));

        for (CampaignOptionsRoute route : routes) {
            if (normalizedFilter.isBlank() || route.matches(normalizedFilter)) {
                addNavigationTarget(root, route);
            }
        }

        navigationTree.setModel(new DefaultTreeModel(root));

        for (int row = 0; row < navigationTree.getRowCount(); row++) {
            navigationTree.expandRow(row);
        }

        if (selectedRoute != null) {
            selectRoute(selectedRoute);
        }
    }

    private void addNavigationTarget(DefaultMutableTreeNode root, CampaignOptionsRoute route) {
        DefaultMutableTreeNode currentNode = root;
        List<String> path = route.getPath();
        for (int index = 0; index < path.size(); index++) {
            String label = path.get(index);
            currentNode = getOrCreateNavigationNode(currentNode, label);
            if (index == path.size() - 1) {
                NavigationTreeNode treeNode = (NavigationTreeNode) currentNode.getUserObject();
                treeNode.route = route;
            }
        }
    }

    private DefaultMutableTreeNode getOrCreateNavigationNode(DefaultMutableTreeNode parent, String label) {
        for (int childIndex = 0; childIndex < parent.getChildCount(); childIndex++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(childIndex);
            if (child.getUserObject() instanceof NavigationTreeNode treeNode && treeNode.label.equals(label)) {
                return child;
            }
        }

        DefaultMutableTreeNode child = new DefaultMutableTreeNode(new NavigationTreeNode(label, null));
        parent.add(child);
        return child;
    }

    private CampaignOptionsRoute getSelectedRoute() {
        TreePath selectionPath = navigationTree.getSelectionPath();
        if (selectionPath == null) {
            return null;
        }

        Object selectedComponent = selectionPath.getLastPathComponent();
        if (selectedComponent instanceof DefaultMutableTreeNode treeNode &&
                  treeNode.getUserObject() instanceof NavigationTreeNode navigationTreeNode) {
            return navigationTreeNode.route;
        }

        return null;
    }

    private DefaultMutableTreeNode findNavigationNode(DefaultMutableTreeNode node, CampaignOptionsRoute route) {
        if (node.getUserObject() instanceof NavigationTreeNode treeNode && treeNode.route == route) {
            return node;
        }

        for (int childIndex = 0; childIndex < node.getChildCount(); childIndex++) {
            DefaultMutableTreeNode result = findNavigationNode((DefaultMutableTreeNode) node.getChildAt(childIndex),
                  route);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private static class NavigationTreeNode {
        private final String label;
        private CampaignOptionsRoute route;

        private NavigationTreeNode(String label, CampaignOptionsRoute route) {
            this.label = label;
            this.route = route;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}