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
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.MouseInputAdapter;

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
                createPopupMenu().ifPresent(popup -> {
                    popup.show(component, component.getX(), component.getY());
                });
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Do nothing
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Implement mousePressed per:
        // https://docs.oracle.com/javase/tutorial/uiswing/components/menu.html#popup
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Implement mouseReleased per:
        // https://docs.oracle.com/javase/tutorial/uiswing/components/menu.html#popup
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            createPopupMenu().ifPresent(popup -> {
                popup.show(e.getComponent(), e.getX(), e.getY());
            });
        }
    }

    /**
     * A {@link JPopupMenu} to show, if applicable.
     * @return An optional {@link JPopupMenu} to show.
     */
    protected abstract Optional<JPopupMenu> createPopupMenu();
}
