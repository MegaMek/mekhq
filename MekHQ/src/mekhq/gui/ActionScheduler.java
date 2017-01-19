/*
 * Copyright (c) 2017 The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui;

import javax.swing.Timer;

/**
 * Schedules a future execution of a method after a specified delay. Additional schedulings reset
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
