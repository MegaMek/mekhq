package mekhq.campaign.mission;

import megamek.common.*;
import megamek.common.enums.Gender;
import megamek.common.options.IBasicOptionGroup;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import mekhq.campaign.personnel.SpecialAbility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CrewSkillUpgraderTest {

    @BeforeAll
    static void setUpAll() {
        EquipmentType.initializeTypes();
        SpecialAbility.initializeSPA();
    }

    boolean allSPAsFalse(Crew c) {
        Enumeration<IOptionGroup> g = c.getOptions().getGroups();
        Enumeration<IOption> o = c.getOptions(PilotOptions.LVL3_ADVANTAGES);
        while(o.hasMoreElements()) {
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
                BipedMech.class,
                VTOL.class,
                Tank.class,
                TripodMech.class,
                AeroSpaceFighter.class,
                BattleArmor.class,
                Infantry.class,
                QuadMech.class,
                Jumpship.class
        ));
        ArrayList<CrewType> crewTypes = new ArrayList<>(List.of(
                CrewType.SINGLE,
                CrewType.DUAL,
                CrewType.CREW,
                CrewType.INFANTRY_CREW,
                CrewType.TRIPOD,
                CrewType.VESSEL,
                CrewType.QUADVEE,
                CrewType.COMMAND_CONSOLE,
                CrewType.SUPERHEAVY_TRIPOD
        ));

        for (int i = 0; i < 1000; i++) {
            Entity e = (Entity) eClasses.get(i % eClasses.size()).getDeclaredConstructor().newInstance();
            CrewType t = crewTypes.get(i % crewTypes.size());
            Crew c = new Crew(t, "Pilot #" + String.valueOf(i), t.getCrewSlots(), 2, 3, Gender.RANDOMIZE, i % 2==0, null);
            assertTrue(allSPAsFalse(c));
            e.setCrew(c);
            entities.add(e);
        }

        // Upgrade each entity and confirm SPA is assigned
        for (Entity ent: entities) {
            csu.upgradeCrew(ent);
            assertFalse(allSPAsFalse(ent.getCrew()));
        }

    }

    @Test
    void testUpgradeCrewConfirmSPAAdded() {
        CrewSkillUpgrader csu = new CrewSkillUpgrader(4);
        Entity e = new BipedMech();
        Crew c = new Crew(CrewType.SINGLE, "Joanne Q. Publique", 1, 3, 4, Gender.FEMALE, false, null);
        e.setCrew(c);

        assertTrue(allSPAsFalse(c));

        csu.upgradeCrew(e);

        assertFalse(allSPAsFalse(c));
    }
}