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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;

/**
 * A {@link HudButton} for actions that cannot be undone, such as deleting. The first click turns the label into a
 * confirmation prompt for a few seconds; only a second click in that time fires the action. This keeps confirmation
 * inside the HUD instead of in a stock option pane.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudConfirmButton extends HudButton {
    private static final int CONFIRM_WINDOW_MILLIS = 3000;

    private final String text;
    private final String confirmText;
    private final transient List<ActionListener> confirmedListeners = new ArrayList<>();
    private final Timer resetTimer;
    private boolean awaitingConfirmation;

    /**
     * @param text        the button's label
     * @param confirmText the label shown while waiting for the confirming click, such as "Confirm delete?"
     * @param compact     {@code true} for the smaller button
     */
    public HudConfirmButton(String text, String confirmText, boolean compact) {
        super(text, false, compact);
        this.text = text;
        this.confirmText = confirmText;
        resetTimer = new Timer(CONFIRM_WINDOW_MILLIS, event -> reset());
        resetTimer.setRepeats(false);
        super.addActionListener(this::onClick);
    }

    /**
     * Adds a listener that is told only when the player confirms the action with a second click.
     *
     * @param listener the listener to add
     */
    @Override
    public void addActionListener(ActionListener listener) {
        confirmedListeners.add(listener);
    }

    private void onClick(ActionEvent event) {
        if (!awaitingConfirmation) {
            awaitingConfirmation = true;
            setText(confirmText);
            resetTimer.restart();
            return;
        }
        reset();
        for (ActionListener listener : confirmedListeners) {
            listener.actionPerformed(event);
        }
    }

    /**
     * Cancels a pending confirmation, restoring the normal label.
     */
    public void reset() {
        resetTimer.stop();
        awaitingConfirmation = false;
        setText(text);
    }
}
