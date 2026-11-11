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
package mekhq.gui.baseComponents.hud;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JToolTip;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import jakarta.annotation.Nullable;

/**
 * A {@link JList} whose rows can hold clickable links and tooltips anywhere inside them.
 *
 * <p>A list's rows are drawn by its renderer as pictures, not live components, so a label inside a row cannot be
 * clicked or hovered on its own. This list works out which part of a row is under the pointer by laying out the
 * renderer's component for that row. A renderer marks a part as a link with {@link #markLink}, and draws it hovered
 * when {@link #isLinkHovered} says so. Clicking a link calls the {@link LinkHandler} and leaves the selection alone;
 * clicking anywhere else selects as usual.</p>
 *
 * <p>Tooltips come from the innermost part of the row that has one, so a badge inside a row can explain itself, and
 * are drawn as {@link HudToolTip}s.</p>
 *
 * <p>Links inside rows cannot be reached from the keyboard, so anything a row links to should also be reachable
 * another way, such as a real link in a detail panel for the selected row.</p>
 *
 * @param <E> the type of the list's items
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudLinkList<E> extends JList<E> {
    /** The client property a renderer puts on a component to make it a link; its value identifies the link. */
    public static final String LINK_PROPERTY = "HudLinkList.link";

    /**
     * Opens a link.
     *
     * @param <E> the type of the list's items
     */
    @FunctionalInterface
    public interface LinkHandler<E> {
        /**
         * @param item the row's item
         * @param link the value the renderer gave the link with {@link #markLink}
         */
        void open(E item, Object link);
    }

    private record LinkHit(int index, Object link) {}

    private transient LinkHandler<E> linkHandler = (item, link) -> {};
    private transient LinkHit hovered;
    private transient LinkHit pressed;

    /**
     * @param model the list's items
     */
    public HudLinkList(ListModel<E> model) {
        super(model);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * @param linkHandler called when the player clicks a link in a row
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setLinkHandler(LinkHandler<E> linkHandler) {
        this.linkHandler = Objects.requireNonNull(linkHandler);
    }

    /**
     * Marks part of a rendered row as a link. The renderer calls this each time it configures that part.
     *
     * @param component the part of the row
     * @param link      what the link points to, given back to the {@link LinkHandler}; {@code null} makes the part an
     *                  ordinary label again
     * @param <C>       the component's type
     *
     * @return the same component, for chaining
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static <C extends JComponent> C markLink(C component, @Nullable Object link) {
        component.putClientProperty(LINK_PROPERTY, link);
        return component;
    }

    /**
     * For renderers: whether the pointer is over a link, so it can be drawn hovered.
     *
     * @param list  the list the renderer is drawing for
     * @param index the row
     * @param link  the link's value
     *
     * @return {@code true} if the list is a {@link HudLinkList} and the pointer is over that link in that row
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isLinkHovered(JList<?> list, int index, Object link) {
        return (list instanceof HudLinkList<?> linkList)
                     && (linkList.hovered != null)
                     && (linkList.hovered.index() == index)
                     && Objects.equals(linkList.hovered.link(), link);
    }

    // region Mouse handling

    @Override
    protected void processMouseEvent(MouseEvent event) {
        switch (event.getID()) {
            case MouseEvent.MOUSE_PRESSED -> {
                LinkHit hit = SwingUtilities.isLeftMouseButton(event) ? linkAt(event.getPoint()) : null;
                pressed = hit;
                if (hit != null) {
                    // A link press must not change the selection
                    event.consume();
                    return;
                }
            }
            case MouseEvent.MOUSE_RELEASED -> {
                if (pressed != null) {
                    LinkHit hit = linkAt(event.getPoint());
                    LinkHit wasPressed = pressed;
                    pressed = null;
                    event.consume();
                    if (wasPressed.equals(hit)) {
                        linkHandler.open(getModel().getElementAt(hit.index()), hit.link());
                    }
                    return;
                }
            }
            case MouseEvent.MOUSE_CLICKED -> {
                if (linkAt(event.getPoint()) != null) {
                    event.consume();
                    return;
                }
            }
            case MouseEvent.MOUSE_EXITED -> setHovered(null);
            default -> {
                // entered events need nothing extra
            }
        }
        super.processMouseEvent(event);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent event) {
        if (event.getID() == MouseEvent.MOUSE_MOVED) {
            setHovered(linkAt(event.getPoint()));
        } else if ((event.getID() == MouseEvent.MOUSE_DRAGGED) && (pressed != null)) {
            // Dragging off a pressed link must not start a selection drag
            event.consume();
            return;
        }
        super.processMouseMotionEvent(event);
    }

    private void setHovered(@Nullable LinkHit hit) {
        if (Objects.equals(hovered, hit)) {
            return;
        }
        repaintRow(hovered);
        hovered = hit;
        repaintRow(hovered);
        setCursor((hit == null) ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void repaintRow(@Nullable LinkHit hit) {
        if ((hit != null) && (hit.index() < getModel().getSize())) {
            Rectangle bounds = getCellBounds(hit.index(), hit.index());
            if (bounds != null) {
                repaint(bounds);
            }
        }
    }

    // endregion Mouse handling

    // region Finding what is under the pointer

    private @Nullable LinkHit linkAt(Point point) {
        RowHit row = rowAt(point);
        if (row == null) {
            return null;
        }
        Object link = linkOf(row.deepest(), row.root());
        return (link == null) ? null : new LinkHit(row.index(), link);
    }

    private record RowHit(int index, Component root, @Nullable Component deepest) {}

    /**
     * Lays out the renderer's component for the row under the point and finds the innermost part of it at that point.
     */
    private @Nullable RowHit rowAt(Point point) {
        int index = locationToIndex(point);
        if (index < 0) {
            return null;
        }
        Rectangle cell = getCellBounds(index, index);
        if ((cell == null) || !cell.contains(point)) {
            return null;
        }
        Component root = getCellRenderer().getListCellRendererComponent(this, getModel().getElementAt(index), index,
              isSelectedIndex(index), false);
        root.setBounds(0, 0, cell.width, cell.height);
        layOut(root);
        return new RowHit(index, root, SwingUtilities.getDeepestComponentAt(root, point.x - cell.x,
              point.y - cell.y));
    }

    private static void layOut(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layOut(child);
            }
        }
    }

    /**
     * @param deepest the innermost part of a row under the pointer, or {@code null}
     * @param root    the row's renderer component
     *
     * @return the link value of the nearest part, from {@code deepest} up to {@code root}, that is a link, or
     *       {@code null} if none is
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable Object linkOf(@Nullable Component deepest, Component root) {
        for (Component part = deepest; part != null; part = part.getParent()) {
            if (part instanceof JComponent component) {
                Object link = component.getClientProperty(LINK_PROPERTY);
                if (link != null) {
                    return link;
                }
            }
            if (part == root) {
                break;
            }
        }
        return null;
    }

    /**
     * @param deepest the innermost part of a row under the pointer, or {@code null}
     * @param root    the row's renderer component
     *
     * @return the tooltip of the nearest part, from {@code deepest} up to {@code root}, that has one, or {@code null}
     *       if none does
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable String toolTipOf(@Nullable Component deepest, Component root) {
        for (Component part = deepest; part != null; part = part.getParent()) {
            if (part instanceof JComponent component) {
                String toolTip = component.getToolTipText();
                if ((toolTip != null) && !toolTip.isBlank()) {
                    return toolTip;
                }
            }
            if (part == root) {
                break;
            }
        }
        return null;
    }

    // endregion Finding what is under the pointer

    // region Tooltips

    @Override
    public @Nullable String getToolTipText(MouseEvent event) {
        RowHit row = rowAt(event.getPoint());
        if (row == null) {
            return getToolTipText();
        }
        String toolTip = toolTipOf(row.deepest(), row.root());
        return (toolTip == null) ? getToolTipText() : toolTip;
    }

    @Override
    public JToolTip createToolTip() {
        return new HudToolTip(this);
    }

    // endregion Tooltips
}
