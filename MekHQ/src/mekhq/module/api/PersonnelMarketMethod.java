/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.module.api;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import org.w3c.dom.Node;

import java.io.PrintWriter;
import java.util.List;

/**
 * Interface to be implemented by methods for generating and removing personnel market entries.
 *
 * Implementations of this interface need to be registered by adding the fully qualified class name
 * to src/META-INF/services/mekhq.module.api.PersonnelMarketMethod.
 *
 * @author Neoancient
 */
public interface PersonnelMarketMethod extends MekHQModule {
    List<Person> generatePersonnelForDay(Campaign c);
    List<Person> removePersonnelForDay(Campaign c, List<Person> current);

    @Override
    default void initPlugin(Campaign c) {

    }

    @Override
    default void loadFieldsFromXml(Node node) {

    }

    @Override
    default void writeToXML(final PrintWriter pw, int indent) {

    }
}
