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
package mekhq.campaign.mission;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import megamek.common.*;
import megamek.common.enums.Gender;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import mekhq.campaign.personnel.SpecialAbility;

class CrewSkillUpgraderTest {

    @BeforeAll
    static void setUpAll() {
        EquipmentType.initializeTypes();
        SpecialAbility.initializeSPA();
    }

    boolean allSPAsFalse(Crew c) {
        Enumeration<IOptionGroup> g = c.getOptions().getGroups();
        Enumeration<IOption> o = c.getOptions(PilotOptions.LVL3_ADVANTAGES);
        while (o.hasMoreElements()) {
            IOption spa = o.nextElement();
            if (spa.getValue() instanceof Vector) {
                if (!((Vector<?>) spa.getValue()).isEmpty()) {
                    return false;
                }
            } else {
                if (spa.booleanValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    void testUpgradeThousandCrew() throws Exception {
        CrewSkillUpgrader csu = new CrewSkillUpgrader(4);
        ArrayList<Entity> entities = new ArrayList<>();
        // Iterate over these to make units
        ArrayList<Class> eClasses = new ArrayList<>(List.of(
                BipedMek.class,
                VTOL.class,
                Tank.class,
                TripodMek.class,
                AeroSpaceFighter.class,
                BattleArmor.class,
                Infantry.class,
                QuadMek.class,
                Jumpship.class));
        ArrayList<CrewType> crewTypes = new ArrayList<>(List.of(
                CrewType.SINGLE,
                CrewType.DUAL,
                CrewType.CREW,
                CrewType.INFANTRY_CREW,
                CrewType.TRIPOD,
                CrewType.VESSEL,
                CrewType.QUADVEE,
                CrewType.COMMAND_CONSOLE,
                CrewType.SUPERHEAVY_TRIPOD));

        for (int i = 0; i < 1000; i++) {
            Entity e = (Entity) eClasses.get(i % eClasses.size()).getDeclaredConstructor().newInstance();
            CrewType t = crewTypes.get(i % crewTypes.size());
            Crew c = new Crew(t, "Pilot #" + String.valueOf(i), t.getCrewSlots(), 2, 3, Gender.RANDOMIZE, i % 2 == 0,
                    null);
            assertTrue(allSPAsFalse(c));
            e.setCrew(c);
            entities.add(e);
        }

        // Upgrade each entity and confirm SPA is assigned
        for (Entity ent : entities) {
            csu.upgradeCrew(ent);
            assertFalse(allSPAsFalse(ent.getCrew()));
        }

    }

    @Test
    void testUpgradeCrewConfirmSPAAdded() {
        CrewSkillUpgrader csu = new CrewSkillUpgrader(4);
        Entity e = new BipedMek();
        Crew c = new Crew(CrewType.SINGLE, "Joanne Q. Publique", 1, 3, 4, Gender.FEMALE, false, null);
        e.setCrew(c);

        assertTrue(allSPAsFalse(c));

        csu.upgradeCrew(e);

        assertFalse(allSPAsFalse(c));
    }
}
