/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 */

package mekhq.gui;

import javax.swing.Timer;

/**
 * Schedules a future execution of a method after a specified delay. Additional scheduling reset
 * the timer. This is designed for cases when an action produces multiple events that would cause
 * part of the gui to refresh. As long as the events are received within the delay period, only the
 * final one will cause a refresh. This also causes the refresh to occur on the Swing event dispatching
 * thread regardless of which thread it is scheduled from.
 *
 * @author Neoancient
 *
 */
public class ActionScheduler {

    @FunctionalInterface
    public interface Action {
        void act();
    }

    public static final int DELAY = 50;

    private final Timer timer;

    public ActionScheduler(Action action) {
        timer = new Timer(DELAY, ev -> action.act());
        timer.setRepeats(false);
    }

    public ActionScheduler(Action action, int delay) {
        timer = new Timer(delay, ev -> action.act());
        timer.setRepeats(false);
    }

    public void schedule() {
        timer.restart();
    }

}
