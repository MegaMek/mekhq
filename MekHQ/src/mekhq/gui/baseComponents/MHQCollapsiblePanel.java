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
package mekhq.gui.baseComponents;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import megamek.common.annotations.Nullable;

/**
 * A lightweight, theme-friendly collapsible section panel for reusable MekHQ
 * screens.
 */
public class MHQCollapsiblePanel extends JPanel {
    public static final String EXPANDED_PROPERTY = "expanded";
    public static final String TOGGLE_ACTION = "toggle";

    private static final int HEADER_VERTICAL_PADDING = 6;
    private static final int HEADER_HORIZONTAL_PADDING = 8;
    private static final int CONTENT_LEFT_PADDING = 24;
    private static final int CONTENT_VERTICAL_PADDING = 6;
    private static final int CHEVRON_ICON_SIZE = 12;

    private final JPanel headerPanel;
    private final JLabel iconLabel = new JLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel summaryLabel = new JLabel();
    private final JPanel trailingPanel = new JPanel(new BorderLayout());
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private final Action toggleAction = new AbstractAction(TOGGLE_ACTION) {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent event) {
            toggleExpanded();
        }
    };

    private String title;
    private boolean expanded;
    private boolean headerHovered;

    public MHQCollapsiblePanel(String title) {
        this(title, null);
    }

    public MHQCollapsiblePanel(String title, @Nullable JComponent content) {
        this.title = title;

        setLayout(new BorderLayout());
        setOpaque(false);
        getActionMap().put(TOGGLE_ACTION, toggleAction);

        headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(CONTENT_VERTICAL_PADDING,
                CONTENT_LEFT_PADDING,
                CONTENT_VERTICAL_PADDING,
                0));
        add(contentPanel, BorderLayout.CENTER);

        setSummary("");
        setContent(content);
        setExpanded(true);
    }

    public void setTitle(String title) {
        this.title = title;
        updateHeader();
    }

    public void setSummary(String summary) {
        summaryLabel.setText(summary == null ? "" : summary);
        summaryLabel.setVisible(summary != null && !summary.isBlank());
    }

    public void setTrailingComponent(@Nullable JComponent component) {
        trailingPanel.removeAll();
        if (component != null) {
            trailingPanel.add(component, BorderLayout.CENTER);
        }
        trailingPanel.setVisible(component != null);
        revalidate();
        repaint();
    }

    public void setContent(@Nullable JComponent content) {
        contentPanel.removeAll();
        if (content != null) {
            contentPanel.add(content, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded) {
            return;
        }

        boolean previousValue = this.expanded;
        this.expanded = expanded;
        contentPanel.setVisible(expanded);
        updateHeader();
        firePropertyChange(EXPANDED_PROPERTY, previousValue, expanded);
        revalidate();
        repaint();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(HEADER_VERTICAL_PADDING,
                HEADER_HORIZONTAL_PADDING,
                HEADER_VERTICAL_PADDING,
                HEADER_HORIZONTAL_PADDING));
        Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        headerPanel.setCursor(handCursor);
        headerPanel.setFocusable(true);

        titleLabel.putClientProperty("FlatLaf.styleClass", "h4");

        summaryLabel.putClientProperty("FlatLaf.styleClass", "small");
        summaryLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        trailingPanel.setOpaque(false);
        trailingPanel.setVisible(false);

        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = 0;
        layout.weightx = 0.0;
        layout.fill = GridBagConstraints.NONE;
        layout.anchor = GridBagConstraints.WEST;
        headerPanel.add(iconLabel, layout);

        layout.gridx++;
        layout.insets = new Insets(0, HEADER_HORIZONTAL_PADDING, 0, 0);
        headerPanel.add(titleLabel, layout);

        layout.gridx++;
        layout.weightx = 1.0;
        layout.insets = new Insets(0, HEADER_HORIZONTAL_PADDING, 0, 0);
        layout.fill = GridBagConstraints.HORIZONTAL;
        headerPanel.add(summaryLabel, layout);

        layout.gridx++;
        layout.weightx = 0.0;
        layout.fill = GridBagConstraints.NONE;
        headerPanel.add(trailingPanel, layout);

        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                toggleExpanded();
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                headerHovered = true;
                updateHeaderBackground();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                headerHovered = false;
                updateHeaderBackground();
            }
        });
        headerPanel.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                updateHeaderBackground();
            }

            @Override
            public void focusLost(FocusEvent event) {
                updateHeaderBackground();
            }
        });
        headerPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                TOGGLE_ACTION);
        headerPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                TOGGLE_ACTION);
        headerPanel.getActionMap().put(TOGGLE_ACTION, toggleAction);

        return headerPanel;
    }

    private void toggleExpanded() {
        setExpanded(!isExpanded());
    }

    private void updateHeader() {
        titleLabel.setText(title);
        iconLabel.setIcon(getDisclosureIcon());
        headerPanel.getAccessibleContext().setAccessibleName(title);
        headerPanel.getAccessibleContext()
                .setAccessibleDescription(isExpanded() ? "Collapse section" : "Expand section");
        updateHeaderBackground();
    }

    private void updateHeaderBackground() {
        boolean active = headerHovered || headerPanel.isFocusOwner();
        headerPanel.setOpaque(active);
        headerPanel.setBackground(active ? getHeaderBackgroundColor() : null);
        headerPanel.repaint();
    }

    private Icon getDisclosureIcon() {
        Icon disclosureIcon = UIManager.getIcon(isExpanded() ? "Tree.expandedIcon" : "Tree.collapsedIcon");
        if (disclosureIcon != null) {
            return disclosureIcon;
        }
        return new ChevronIcon(isExpanded(), getForeground());
    }

    private Color getHeaderBackgroundColor() {
        Color backgroundColor = UIManager.getColor("List.hoverBackground");
        if (backgroundColor == null) {
            backgroundColor = UIManager.getColor("Button.hoverBackground");
        }
        return backgroundColor == null ? UIManager.getColor("Panel.background") : backgroundColor;
    }

    @Override
    public Dimension getPreferredSize() {
        return getStableSize(headerPanel.getPreferredSize(), contentPanel.getPreferredSize());
    }

    @Override
    public Dimension getMinimumSize() {
        return getStableSize(headerPanel.getMinimumSize(), contentPanel.getMinimumSize());
    }

    private Dimension getStableSize(Dimension headerSize, Dimension contentSize) {
        int width = Math.max(headerSize.width, contentSize.width);
        int height = headerSize.height + (isExpanded() ? contentSize.height : 0);
        return new Dimension(width, height);
    }

    private static final class ChevronIcon implements Icon {
        private final boolean expanded;
        private final Color color;

        private ChevronIcon(boolean expanded, Color color) {
            this.expanded = expanded;
            this.color = color;
        }

        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(color == null ? component.getForeground() : color);
            graphics2D.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int left = x + 3;
            int right = x + getIconWidth() - 3;
            int top = y + 4;
            int middle = y + getIconHeight() / 2;
            int bottom = y + getIconHeight() - 4;

            if (expanded) {
                graphics2D.drawLine(left, top, middle, bottom);
                graphics2D.drawLine(middle, bottom, right, top);
            } else {
                graphics2D.drawLine(left, top, right, middle);
                graphics2D.drawLine(right, middle, left, bottom);
            }

            graphics2D.dispose();
        }

        @Override
        public int getIconWidth() {
            return CHEVRON_ICON_SIZE;
        }

        @Override
        public int getIconHeight() {
            return CHEVRON_ICON_SIZE;
        }
    }
}