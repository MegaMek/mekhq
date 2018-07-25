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
package mekhq.module;

import mekhq.module.api.PersonnelMarketMethod;

/**
 * Manager for services that provide methods for generating and removing potential recruits to and
 * from the personnel market
 * 
 * @author Neoancient
 *
 */
public class PersonnelMarketServiceManager extends AbstractServiceManager<PersonnelMarketMethod> {
    
    private static PersonnelMarketServiceManager instance;
    
    private PersonnelMarketServiceManager() {
        super(PersonnelMarketMethod.class);
    }
    
    public static PersonnelMarketServiceManager getInstance() {
        if (null == instance) {
            instance = new PersonnelMarketServiceManager();
        }
        return instance;
    }
}
