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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.QuickTrain.QuickTrainOptions;
import mekhq.campaign.personnel.training.AutoTrainSettings.Order;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

/**
 * Tests {@link AutoTrainSettings}: Quick Train's defaults, the builder, and saving presets to and reading them from a
 * campaign file.
 */
class AutoTrainSettingsTest {
    private final CampaignOptions options = new CampaignOptions();

    /** @return settings with every value changed from its default */
    private static AutoTrainSettings everythingChanged() {
        return new AutoTrainSettings.Builder()
                     .setName("Veterans")
                     .setTargetLevel(7)
                     .setContinuous(true)
                     .setLevelArtillery(true)
                     .setLevelScouting(true)
                     .setLevelEscape(true)
                     .setLevelLeadership(true)
                     .setLevelTraining(true)
                     .setLevelOtherCommand(true)
                     .setSpecificSkill(S_GUN_MEK)
                     .setReserveXp(250)
                     .setSkipTrainingFormations(true)
                     .setRespectExclusions(true)
                     .setSkipFeeImprovements(true)
                     .setOrder(Order.CHEAPEST_FIRST)
                     .setRunMonthly(true)
                     .build();
    }

    private static String write(AutoTrainSettings settings) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        settings.writeToXML(writer, 0);
        writer.flush();
        return stringWriter.toString();
    }

    private AutoTrainSettings read(String xml) throws Exception {
        Node node = MHQXMLUtility.newSafeDocumentBuilder()
                          .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                          .getDocumentElement();
        return AutoTrainSettings.generateInstanceFromXML(node, options);
    }

    @Nested
    class Defaults {
        @Test
        void trainAsQuickTrainDid() {
            AutoTrainSettings settings = AutoTrainSettings.defaults("Default", options);
            QuickTrainOptions quickTrain = QuickTrainOptions.buildQuickTrainOptions(options);

            assertEquals("Default", settings.name());
            assertEquals(AutoTrainSettings.DEFAULT_TARGET_LEVEL, settings.targetLevel());
            assertFalse(settings.isContinuous());
            assertNull(settings.specificSkill());
            assertEquals(0, settings.reserveXp());
            assertTrue(settings.isSkipTrainingFormations());
            assertTrue(settings.isRespectExclusions());
            assertFalse(settings.isSkipFeeImprovements());
            assertEquals(Order.WEAKEST_FIRST, settings.order());
            assertFalse(settings.isRunMonthly());
            assertEquals(quickTrain.isLevelArtillery(), settings.isLevelArtillery());
            assertEquals(quickTrain.isLevelScoutingSkills(), settings.isLevelScouting());
            assertEquals(quickTrain.isLevelEscapeSkills(), settings.isLevelEscape());
            assertEquals(quickTrain.isLevelLeadership(), settings.isLevelLeadership());
            assertEquals(quickTrain.isLevelTraining(), settings.isLevelTraining());
            assertEquals(quickTrain.isLevelOtherCommandSkills(), settings.isLevelOtherCommand());
        }

        @Test
        void aNewBuilderStartsWithEverythingOff() {
            AutoTrainSettings settings = new AutoTrainSettings.Builder().build();

            assertEquals("", settings.name());
            assertEquals(0, settings.targetLevel());
            assertFalse(settings.isContinuous());
            assertFalse(settings.isLevelArtillery());
            assertFalse(settings.isSkipTrainingFormations());
            assertFalse(settings.isRespectExclusions());
            assertNull(settings.specificSkill());
            assertEquals(Order.WEAKEST_FIRST, settings.order());
            assertFalse(settings.isRunMonthly());
        }
    }

    @Nested
    class Conversions {
        @Test
        void aBuilderFromSettingsBuildsTheSameSettings() {
            AutoTrainSettings settings = everythingChanged();

            assertEquals(settings, settings.toBuilder().build());
        }

        @Test
        void quickTrainOptionsCarryTheOptionalSkills() {
            AutoTrainSettings settings = new AutoTrainSettings.Builder()
                                               .setLevelArtillery(true)
                                               .setLevelEscape(true)
                                               .setLevelTraining(true)
                                               .setSkipTrainingFormations(true)
                                               .build();

            assertEquals(new QuickTrainOptions(true, false, true, false, true, false, true),
                  settings.toQuickTrainOptions());
        }
    }

    @Nested
    class Xml {
        @Test
        void everyValueIsSavedAndRead() throws Exception {
            AutoTrainSettings settings = everythingChanged();

            assertEquals(settings, read(write(settings)));
        }

        @Test
        void noSpecificSkillIsReadAsNone() throws Exception {
            AutoTrainSettings settings = everythingChanged().toBuilder().setSpecificSkill(null).build();

            String xml = write(settings);

            assertFalse(xml.contains("specificSkill"));
            assertNull(read(xml).specificSkill());
        }

        @Test
        void missingValuesKeepQuickTrainsDefaults() throws Exception {
            AutoTrainSettings settings = read("<preset><name>Only a name</name></preset>");

            assertEquals(AutoTrainSettings.defaults("Only a name", options), settings);
        }

        @Test
        void unreadableValuesKeepTheirDefaultsAndTheRestIsRead() throws Exception {
            AutoTrainSettings settings = read("<preset><name>Odd</name><targetLevel>high</targetLevel>"
                                                    + "<order>SIDEWAYS</order><reserveXp>40</reserveXp>"
                                                    + "<unknown>ignored</unknown></preset>");

            assertEquals("Odd", settings.name());
            assertEquals(AutoTrainSettings.DEFAULT_TARGET_LEVEL, settings.targetLevel());
            assertEquals(Order.WEAKEST_FIRST, settings.order());
            assertEquals(40, settings.reserveXp());
        }

        @Test
        void aBlankSpecificSkillIsReadAsNone() throws Exception {
            assertNull(read("<preset><name>Blank</name><specificSkill> </specificSkill></preset>").specificSkill());
        }
    }
}
