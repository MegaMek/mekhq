/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.work;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import testUtilities.parts.PartsScenario;

/**
 * The abilities the Repair Bay shows for each tech, so a player can see at a glance which tech to save for which job
 * (issues #3622 and #3443).
 */
class TechRepairAbilitiesTest {
    private final PartsScenario scenario = PartsScenario.create();

    @BeforeAll
    static void loadAbilities() {
        SpecialAbility.initializeSPA(true);
    }

    /**
     * Reads the game's own ability file rather than the shared ability table, which other tests load with the test
     * copy; that copy predates the fabrication abilities.
     */
    @Test
    void everyRepairAbilityIsDefinedInTheGamesAbilityFile() throws Exception {
        String abilityFile = Files.readString(Path.of("data/universe/defaultspa.yml"));

        for (String abilityName : TechRepairAbilities.REPAIR_ABILITIES) {
            Pattern entry = Pattern.compile("^\\s*- lookupName: " + Pattern.quote(abilityName) + "\\s*$",
                  Pattern.MULTILINE);
            assertTrue(entry.matcher(abilityFile).find(), abilityName + " is defined");
        }
    }

    @Test
    void aTechsAbilitiesAreNamedInOrderAndExplained() {
        Person tech = scenario.withTech(SkillType.EXP_REGULAR);
        tech.getOptions().getOption(PersonnelOptions.FLAW_GREMLINS).setValue(true);
        tech.getOptions().getOption(PersonnelOptions.TECH_WEAPON_SPECIALIST).setValue(true);
        String weaponSpecialist = SpecialAbility.getAbility(PersonnelOptions.TECH_WEAPON_SPECIALIST).getDisplayName();
        String gremlins = SpecialAbility.getAbility(PersonnelOptions.FLAW_GREMLINS).getDisplayName();

        assertEquals("Abilities: " + weaponSpecialist + ", " + gremlins, TechRepairAbilities.describeNames(tech));
        String effects = TechRepairAbilities.describeEffects(tech);
        assertNotNull(effects);
        assertEquals(2, effects.split("<br>").length, effects);
        assertTrue(effects.contains(SpecialAbility.getAbility(PersonnelOptions.FLAW_GREMLINS).getDescription()),
              effects);
    }

    @Test
    void aTechWithNoRepairAbilityShowsNothing() {
        Person tech = scenario.withTech(SkillType.EXP_REGULAR);

        assertEquals("", TechRepairAbilities.describeNames(tech));
        assertNull(TechRepairAbilities.describeEffects(tech));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Part.modifier.easyToMaintain", "Part.modifier.difficultToMaintain",
                             "Part.modifier.prototypeTsm", "Part.modifier.weaponSpecialist",
                             "Part.modifier.armorSpecialist", "Part.modifier.internalSpecialist",
                             "Part.modifier.maintainer", "Part.modifier.fixer" })
    void everyRepairModifierLabelIsInTheBundle(String key) {
        String label = getTextAt("mekhq.resources.Parts", key);

        assertFalse(label.startsWith("!"), key + " is missing from the bundle");
    }
}
