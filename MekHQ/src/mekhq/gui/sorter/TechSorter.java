/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.sorter;

import java.util.Comparator;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.work.IPartWork;

/**
 * A comparator that sorts techs by skill level
 * 
 * @author Jay Lawson
 */
public class TechSorter implements Comparator<Person> {
    private IPartWork partWork;

    public TechSorter() {
        this(null);
    }

    public TechSorter(IPartWork p) {
        partWork = p;
    }

    @Override
    public int compare(Person p0, Person p1) {
        if (partWork != null && partWork.getUnit() != null) {
            if (p0.getTechUnits().contains(partWork.getUnit())) {
                return -1;
            }
            if (p1.getTechUnits().contains(partWork.getUnit())) {
                return 1;
            }
        }
        return ((Comparable<Integer>) p0.getBestTechLevel()).compareTo(p1.getBestTechLevel());
    }

    public void setPart(IPartWork p) {
        partWork = p;
    }

    public void clearPart() {
        partWork = null;
    }
}
