/*
 * Copyright (c) 2021 - The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.MouseInputAdapter;

import megamek.common.annotations.Nullable;

/**
 * Provides a popup menu adapter for a component which also ensures that
 * the accessibility chord SHIFT+F10 opens the popup as well.
 */
public abstract class JPopupMenuAdapter extends MouseInputAdapter implements ActionListener {
    public static final String COMMAND_OPEN_POPUP = "SHIFT_F10";

    /**
     * Connect the popup menu adapter to the component. Implementations should call this
     * to connect the popup menu to both right click and the SHIFT+F10 accessibility chord.
     * @param component The component to trap context menu actions.
     */
    protected void connect(JComponent component) {
        component.addMouseListener(this);

        // Setup SHIFT+F10 for context menu support
        KeyStroke keystroke = KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(keystroke, COMMAND_OPEN_POPUP);
        component.getActionMap().put(COMMAND_OPEN_POPUP, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                // Immediately return if there are no units selected
                if (!shouldShowPopup()) {
                    return;
                }

                JPopupMenu popup = createPopupMenu();
                popup.show(component, component.getX(), component.getY());
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Do nothing
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            // Immediately return if there are no units selected
            if (!shouldShowPopup()) {
                return;
            }

            JPopupMenu popup = createPopupMenu();
            if (popup != null) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    /**
     * Gets a value indicating whether or not to show the popup.
     */
    protected abstract boolean shouldShowPopup();

    /**
     * Creates the popup menu, or returns {@code null} if the
     * popup menu should not be shown.
     * @return A {@link JPopupMenu} to show, or {@code null} if no
     *         popup menu should be shown.
     */
    @Nullable
    protected abstract JPopupMenu createPopupMenu();
}
