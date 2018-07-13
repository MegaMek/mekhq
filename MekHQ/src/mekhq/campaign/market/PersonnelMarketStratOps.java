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

package mekhq.campaign.market;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Node;

import megamek.common.Compute;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.module.api.PersonnelMarketMethod;

/**
 * Method for personnel market generation given in the repair and maintenance section of Strategic Operations
 *
 */
public class PersonnelMarketStratOps implements PersonnelMarketMethod {
    
    private int daysSinceRolled = 0;

    @Override
    public String getModuleName() {
        return "Strat Ops";
    }

    @Override
    public List<Person> generatePersonnelForDay(Campaign c) {
        if (daysSinceRolled == c.getCampaignOptions().getMaintenanceCycleDays()) {
            Person p;
            int roll = Compute.d6(2);
            if (roll == 2) { // Medical
                p = c.newPerson(Person.T_DOCTOR);
            } else if (roll == 3) { // ASF or Proto Pilot
                if (c.getFaction().isClan() && c.getCalendar().after(new GregorianCalendar(3059, 1, 1)) && Compute.d6(2) < 6) {
                    p = c.newPerson(Person.T_PROTO_PILOT);
                } else {
                    p = c.newPerson(Person.T_AERO_PILOT);
                }
            } else if (roll == 4 || roll == 10) { // MW
                p = c.newPerson(Person.T_MECHWARRIOR);
            } else if (roll == 5 || roll == 9) { // Vehicle Crews
                if (Compute.d6() < 3) {
                    p = c.newPerson(Person.T_GVEE_DRIVER);
                } else {
                    p = c.newPerson(Person.T_VEE_GUNNER);
                }
            } else if (roll == 6 || roll == 8) { // Infantry
                if (c.getFaction().isClan() && Compute.d6(2) > 3) {
                    p = c.newPerson(Person.T_BA);
                } else {
                    p = c.newPerson(Person.T_INFANTRY);
                }
            } else if (roll == 11) { // Tech
                int tr = Compute.randomInt(Person.T_ASTECH);
                while (tr < Person.T_MECH_TECH) {
                    tr = Compute.randomInt(Person.T_ASTECH);
                }
                p = c.newPerson(tr);
            } else if (roll == 12) { // Vessel Crew
                int tr = Compute.randomInt(Person.T_SPACE_GUNNER);
                while (tr < Person.T_SPACE_PILOT) {
                    tr = Compute.randomInt(Person.T_SPACE_GUNNER);
                }
                p = c.newPerson(tr);
            } else {
                p = c.newPerson(Person.T_NONE);
            }
            UUID id = UUID.randomUUID();
            p.setId(id);
            daysSinceRolled = 0;
            return Collections.singletonList(p);
        } else {
            daysSinceRolled++;
            return null;
        }
    }

    @Override
    public List<Person> removePersonnelForDay(Campaign c, List<Person> current) {
        if (daysSinceRolled == c.getCampaignOptions().getMaintenanceCycleDays()) {
            return current;
        }
        return null;
    }
    
    @Override
    public void loadFieldsFromXml(Node node) {
        if (node.getNodeName().equals("daysSinceRolled")) {
            daysSinceRolled = Integer.parseInt(node.getTextContent());
        }
    }
    
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "daysSinceRolled", daysSinceRolled);
    }

}
