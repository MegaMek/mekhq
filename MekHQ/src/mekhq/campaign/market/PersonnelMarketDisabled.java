/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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

package mekhq.campaign.market;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.module.api.PersonnelMarketMethod;

import java.util.ArrayList;
import java.util.List;

public class PersonnelMarketDisabled implements PersonnelMarketMethod {


    @Override
    public String getModuleName() {
        return "Disabled";
    }

    @Override
    public List<Person> generatePersonnelForDay(Campaign c) {
        return new ArrayList<Person>();
    }

    @Override
    public List<Person> removePersonnelForDay(Campaign c, List<Person> current) {
        return new ArrayList<Person>();
    }
}
