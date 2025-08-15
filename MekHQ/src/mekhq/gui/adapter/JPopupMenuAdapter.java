/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
 * Provides a popup menu adapter for a component which also ensures that the accessibility chord SHIFT+F10 opens the
 * popup as well.
 */
public abstract class JPopupMenuAdapter extends MouseInputAdapter implements ActionListener {
    public static final String COMMAND_OPEN_POPUP = "SHIFT_F10";

    /**
     * Connect the popup menu adapter to the component. Implementations should call this to connect the popup menu to
     * both right click and the SHIFT+F10 accessibility chord.
     *
     * @param component The component to trap context menu actions.
     */
    protected void connect(JComponent component) {
        component.addMouseListener(this);

        // Setup SHIFT+F10 for context menu support
        KeyStroke keystroke = KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(keystroke, COMMAND_OPEN_POPUP);
        component.getActionMap().put(COMMAND_OPEN_POPUP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createPopupMenu().ifPresent(popup ->
                                                  popup.show(component, component.getX(), component.getY()));
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
            createPopupMenu().ifPresent(popup -> popup.show(e.getComponent(), e.getX(), e.getY()));
        }
    }

    /**
     * A {@link JPopupMenu} to show, if applicable.
     *
     * @return An optional {@link JPopupMenu} to show.
     */
    protected abstract Optional<JPopupMenu> createPopupMenu();
}
