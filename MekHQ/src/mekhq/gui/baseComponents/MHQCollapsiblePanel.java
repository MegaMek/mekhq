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

import static mekhq.utilities.MHQInternationalization.getTextAt;

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
import java.awt.Point;
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
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import megamek.common.annotations.Nullable;

/**
 * A lightweight, theme-friendly collapsible section panel for reusable MekHQ
 * screens.
 */
public class MHQCollapsiblePanel extends JPanel {
    public static final String EXPANDED_PROPERTY = "expanded";
    public static final String TOGGLE_ACTION = "toggle";

    private static final String RESOURCE_BUNDLE = "mekhq.resources.GUI";

    private static final int HEADER_VERTICAL_PADDING = 6;
    private static final int HEADER_HORIZONTAL_PADDING = 8;
    private static final int CONTENT_LEFT_PADDING = 24;
    private static final int CONTENT_VERTICAL_PADDING = 6;
    private static final int CHEVRON_ICON_SIZE = 12;

    private final JPanel headerPanel;
    private final JLabel iconLabel = new JLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel summaryLabel = new JLabel() {
        @Override
        public String getToolTipText(MouseEvent event) {
            // The summary is the header's flexible column, so when it is too long it is truncated with an ellipsis
            // rather than widening the section. Surface the full text as a tooltip only while it is actually
            // truncated, so sections whose summary already fits do not show a redundant tooltip. Calls are qualified
            // with this. so they resolve to this label's geometry, not the same-named methods on the enclosing panel.
            return this.getPreferredSize().width > this.getWidth() ? this.getText() : null;
        }
    };
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
    private boolean titleMuted;

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
        // Deliberately keep the summary label showing even when empty. It is the only header cell carrying horizontal
        // weight (weightx=1.0) in the GridBagLayout, so it absorbs the slack and keeps the icon and title left-aligned.
        // If it were hidden, GridBagLayout would drop its cell, and with every remaining cell at weightx=0 the layout
        // would center the icon+title in the header - making the title look centered for callers that set no summary.
        summaryLabel.setVisible(true);
    }

    /**
     * Mutes or restores the section title color. A muted title is drawn in the theme's disabled foreground, letting a
     * disabled section be spotted at a glance while the list is collapsed.
     *
     * @param muted {@code true} to draw the title in the disabled foreground color, {@code false} for the default
     */
    public void setTitleMuted(boolean muted) {
        this.titleMuted = muted;
        updateTitleForeground();
    }

    private void updateTitleForeground() {
        titleLabel.setForeground(titleMuted ? UIManager.getColor("Label.disabledForeground") : null);
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

    public int getContentPreferredWidth() {
        return contentPanel.getPreferredSize().width;
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
        // Set the hand cursor on the clickable child labels too. They share the header's toggle listener, so the
        // affordance should match; setting it explicitly keeps the cursor consistent regardless of any look-and-feel
        // that overrides label cursors. The trailing panel is intentionally excluded so it can host its own control.
        iconLabel.setCursor(handCursor);
        titleLabel.setCursor(handCursor);
        summaryLabel.setCursor(handCursor);
        headerPanel.setFocusable(true);

        titleLabel.putClientProperty("FlatLaf.styleClass", "h4");

        // Render the summary at the default body font size. It previously used FlatLaf's smaller "small" type class,
        // which reviewers found too small; the muted foreground still keeps it secondary to the bold section title.
        summaryLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        // Register so the truncation-only tooltip from getToolTipText is shown even though no static tooltip is set.
        ToolTipManager.sharedInstance().registerComponent(summaryLabel);

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

        // Toggle the section from anywhere in the header. The listener is shared by the header panel and its
        // non-interactive labels because Swing delivers a mouse event only to the deepest component under the pointer;
        // without this, clicking directly on the title or the (full-width) summary label would not reach the header.
        // The trailing panel is intentionally excluded so it can host its own interactive control.
        MouseAdapter headerInteractionListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                // Clicking a JPanel doesn't move focus on its own, so pull focus to the header on press. This keeps
                // mouse and keyboard interaction in sync: after a click the Space/Enter toggle bindings work and the
                // header shows its focused background without the user first having to tab to it.
                headerPanel.requestFocusInWindow();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                // Toggle on release rather than click: mouseClicked only fires when the press and release land on the
                // same component with negligible movement, so dragging the pointer horizontally across the header - or
                // crossing between the icon/title/summary child labels between press and release - would swallow the
                // click. mouseReleased is delivered to whichever component received the press regardless of movement;
                // we just confirm the pointer is still within the header bounds so a press-then-drag-away gesture can
                // still cancel the toggle.
                Point pointInHeader = SwingUtilities.convertPoint((java.awt.Component) event.getSource(),
                        event.getPoint(), headerPanel);
                if (headerPanel.contains(pointInHeader)) {
                    toggleExpanded();
                }
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                headerHovered = true;
                updateHeaderBackground();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                // A child label reports an exit when the pointer merely crosses onto it, so only clear the hover
                // state once the pointer has actually left the header's bounds.
                Point pointInHeader = SwingUtilities.convertPoint((java.awt.Component) event.getSource(),
                        event.getPoint(), headerPanel);
                headerHovered = headerPanel.contains(pointInHeader);
                updateHeaderBackground();
            }
        };
        headerPanel.addMouseListener(headerInteractionListener);
        iconLabel.addMouseListener(headerInteractionListener);
        titleLabel.addMouseListener(headerInteractionListener);
        summaryLabel.addMouseListener(headerInteractionListener);
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
        updateTitleForeground();
        iconLabel.setIcon(getDisclosureIcon());
        headerPanel.getAccessibleContext().setAccessibleName(title);
        headerPanel.getAccessibleContext()
                .setAccessibleDescription(isExpanded()
                        ? getTextAt(RESOURCE_BUNDLE, "MHQCollapsiblePanel.collapse.accessibleDescription")
                        : getTextAt(RESOURCE_BUNDLE, "MHQCollapsiblePanel.expand.accessibleDescription"));
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
        return getStableSize(getHeaderBaselineSize(), contentPanel.getPreferredSize());
    }

    @Override
    public Dimension getMinimumSize() {
        return getStableSize(getHeaderBaselineSize(), contentPanel.getMinimumSize());
    }

    private Dimension getHeaderBaselineSize() {
        Insets insets = headerPanel.getInsets();
        int width = insets.left + insets.right
                + iconLabel.getPreferredSize().width
                + HEADER_HORIZONTAL_PADDING
                + titleLabel.getPreferredSize().width;
        if (trailingPanel.isVisible()) {
            width += HEADER_HORIZONTAL_PADDING + trailingPanel.getPreferredSize().width;
        }

        return new Dimension(width, headerPanel.getPreferredSize().height);
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
