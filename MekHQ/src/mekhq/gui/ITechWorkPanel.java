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

import mekhq.campaign.personnel.Person;
import mekhq.campaign.work.IPartWork;

/**
 * Interface implemented by any panel that manages tech work. Needed to provide selectedTask
 * to the TechTableModel and the selectedTech to the TaskTableModel.
 *
 */
public interface ITechWorkPanel {

    public IPartWork getSelectedTask();
    
    public Person getSelectedTech();
}
