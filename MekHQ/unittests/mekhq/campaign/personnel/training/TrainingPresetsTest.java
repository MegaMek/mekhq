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
package mekhq.campaign.personnel.training;

import static mekhq.campaign.personnel.skills.SkillType.S_GUN_MEK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

/**
 * Tests {@link TrainingPresets}: saving, finding and removing presets by name, keeping them in the campaign file, and
 * running the monthly ones.
 */
class TrainingPresetsTest {
    private static final int START_LEVEL = 2;

    private static AutoTrainSettings preset(String name) {
        return new AutoTrainSettings.Builder().setName(name).setTargetLevel(START_LEVEL + 1)
                     .setSpecificSkill(S_GUN_MEK).build();
    }

    private static String write(TrainingPresets presets) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        presets.writeToXML(writer, 0);
        writer.flush();
        return stringWriter.toString();
    }

    private static TrainingPresets read(String xml) throws Exception {
        Node node = MHQXMLUtility.newSafeDocumentBuilder()
                          .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                          .getDocumentElement();
        return TrainingPresets.generateInstanceFromXML(node, new CampaignOptions());
    }

    @Nested
    class Names {
        @Test
        void savingANameAgainReplacesThatPresetInPlace() {
            TrainingPresets presets = new TrainingPresets();
            presets.save(preset("Gunners"));
            presets.save(preset("Pilots"));
            AutoTrainSettings replacement = preset("GUNNERS").toBuilder().setContinuous(true).build();

            presets.save(replacement);

            assertEquals(2, presets.getPresets().size());
            assertSame(replacement, presets.getPresets().get(0));
        }

        @Test
        void presetsAreFoundIgnoringCase() {
            TrainingPresets presets = new TrainingPresets();
            AutoTrainSettings gunners = preset("Gunners");
            presets.save(gunners);

            assertSame(gunners, presets.find("gUnNeRs"));
            assertNull(presets.find("Pilots"));
        }

        @Test
        void presetsAreRemovedIgnoringCase() {
            TrainingPresets presets = new TrainingPresets();
            presets.save(preset("Gunners"));

            assertFalse(presets.remove("Pilots"));
            assertTrue(presets.remove("GUNNERS"));
            assertTrue(presets.getPresets().isEmpty());
        }

        @Test
        void theListCantBeChangedFromOutside() {
            TrainingPresets presets = new TrainingPresets();
            List<AutoTrainSettings> list = presets.getPresets();

            assertThrows(UnsupportedOperationException.class, () -> list.add(preset("Sneaky")));
        }
    }

    @Nested
    class Xml {
        @Test
        void noPresetsWriteNothing() {
            assertEquals("", write(new TrainingPresets()));
        }

        @Test
        void presetsAreSavedAndReadInOrder() throws Exception {
            TrainingPresets presets = new TrainingPresets();
            presets.save(preset("Gunners"));
            presets.save(preset("Pilots").toBuilder().setRunMonthly(true).build());

            TrainingPresets loaded = read(write(presets));

            assertEquals(presets.getPresets(), loaded.getPresets());
        }

        @Test
        void presetsWithoutANameAreSkipped() throws Exception {
            TrainingPresets loaded = read("<trainingPresets><preset><name> </name></preset>"
                                                + "<preset><name>Kept</name></preset></trainingPresets>");

            assertEquals(1, loaded.getPresets().size());
            assertEquals("Kept", loaded.getPresets().get(0).name());
        }
    }

    @Nested
    class Monthly {
        private final TrainingFixture fixture = new TrainingFixture();

        private Person gunner() {
            Person person = fixture.livePerson(100_000);
            TrainingFixture.giveSkill(person, S_GUN_MEK, START_LEVEL);
            when(fixture.campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false))
                  .thenReturn(List.of(person));
            return person;
        }

        @Test
        void monthlyPresetsTrainTheActivePersonnel() {
            Person person = gunner();
            TrainingPresets presets = new TrainingPresets();
            presets.save(preset("Gunners").toBuilder().setRunMonthly(true).build());

            presets.runMonthly(fixture.campaign);

            assertEquals(START_LEVEL + 1, person.getSkill(S_GUN_MEK).getLevel());
            assertEquals(100_000 - TrainingFixture.improvementCost(S_GUN_MEK, START_LEVEL), person.getXP());
        }

        @Test
        void otherPresetsDontRun() {
            Person person = gunner();
            TrainingPresets presets = new TrainingPresets();
            presets.save(preset("Gunners"));

            presets.runMonthly(fixture.campaign);

            assertEquals(START_LEVEL, person.getSkill(S_GUN_MEK).getLevel());
            assertEquals(100_000, person.getXP());
            verify(fixture.finances, never()).debit(any(), any(LocalDate.class), any(), anyString());
        }
    }
}
