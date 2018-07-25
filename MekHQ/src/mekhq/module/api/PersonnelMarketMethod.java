/*
 * Copyright (c) 2018  - The MegaMek Team
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
package mekhq.module.api;

import java.io.PrintWriter;
import java.util.List;

import org.w3c.dom.Node;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

/**
 * Interface to be implemented by methods for generating and removing personnel market entries.
 * 
 * Implementations of this interface need to be registered by adding the fully qualified class name
 * to src/META-INF/services/mekhq.module.api.PersonnelMarketMethod.
 * 
 * @author Neoancient
 *
 */
public interface PersonnelMarketMethod extends MekHQModule {
    
    List<Person> generatePersonnelForDay(Campaign c);
    List<Person> removePersonnelForDay(Campaign c, List<Person> current);

    @Override
    default void initPlugin(Campaign c) {};
    @Override
    default void loadFieldsFromXml(Node node) {}
    @Override
    default void writeToXml(PrintWriter pw1, int indent) {}

}
