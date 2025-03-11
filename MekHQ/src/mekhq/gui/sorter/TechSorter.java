/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
