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
package mekhq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CampaignPresetTest {
    private String originalUserDirectory;

    @BeforeEach
    void rememberUserDirectory() {
        originalUserDirectory = PreferenceManager.getClientPreferences().getUserDir();
    }

    @AfterEach
    void restoreUserDirectory() {
        PreferenceManager.getClientPreferences().setUserDir(originalUserDirectory == null ? "" : originalUserDirectory);
    }

    @Test
    void userCampaignPresetDirectoryFallsBackToLegacyDirectory() {
        PreferenceManager.getClientPreferences().setUserDir("");

        assertEquals(Path.of(MHQConstants.USER_CAMPAIGN_PRESET_DIRECTORY).normalize(),
              CampaignPreset.getUserCampaignPresetDirectory().toPath().normalize());
    }

    @Test
    void userCampaignPresetDirectoryUsesConfiguredUserDirectory(final @TempDir Path temporaryDirectory) {
        final Path userDirectory = temporaryDirectory.resolve("Custom Files (BattleTech)");
        PreferenceManager.getClientPreferences().setUserDir(userDirectory.toString());

        assertEquals(userDirectory.resolve(MHQConstants.CAMPAIGN_PRESET_DIRECTORY).normalize(),
              CampaignPreset.getUserCampaignPresetDirectory().toPath().normalize());
    }

    @Test
    void presetPickerFindsMetadataInConfiguredUserDirectory(final @TempDir Path temporaryDirectory) throws IOException {
        final Path userDirectory = temporaryDirectory.resolve("Custom Files (BattleTech)");
        final Path presetDirectory = userDirectory.resolve(MHQConstants.CAMPAIGN_PRESET_DIRECTORY);
        final Path presetFile = presetDirectory.resolve("Custom User Preset.xml");
        Files.createDirectories(presetDirectory);
        Files.writeString(presetFile, """
              <?xml version="1.0" encoding="UTF-8"?>
              <campaignPreset version="0.50.10">
                  <title>Custom User Preset</title>
                  <description>Loaded from the configured user directory.</description>
              </campaignPreset>
              """, StandardCharsets.UTF_8);
        PreferenceManager.getClientPreferences().setUserDir(userDirectory.toString());

        assertTrue(CampaignPreset.getCampaignPresetsMetadata()
                         .stream()
                         .anyMatch(preset -> presetFile.toFile().equals(preset.getPresetFile())));
    }

    @Test
    void writeToFileReportsFailureForInvalidDestination(final @TempDir Path temporaryDirectory) throws IOException {
        final Path parentFile = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(parentFile, "blocking file", StandardCharsets.UTF_8);
        final Path presetFile = parentFile.resolve("Campaign Preset.xml");

        assertFalse(new CampaignPreset().writeToFile(null, presetFile.toFile()));
        assertFalse(Files.exists(presetFile));
    }

    @Test
    void writeToFileReportsSuccess(final @TempDir Path temporaryDirectory) {
        final Path presetFile = temporaryDirectory.resolve("Campaign Preset.xml");

        assertTrue(new CampaignPreset().writeToFile(null, presetFile.toFile()));
        assertTrue(Files.isRegularFile(presetFile));
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    void createDetachedCopy_keepsTheValues() {
        final CampaignPreset source = createPopulatedPreset();

        final CampaignPreset copy = CampaignPreset.createDetachedCopy(source);

        assertNotNull(copy);
        assertEquals("Detached Copy Test", copy.getTitle());
        assertFalse(copy.isGM());
        assertEquals(73, copy.getCampaignOptions().get(CampaignOption.PERCENT_FEMALE));
        assertEquals(42, copy.getRandomSkillPreferences().getOverallRecruitBonus());
        assertEquals(source.getGameOptions().getOption(OptionsConstants.ALLOWED_TECH_LEVEL).stringValue(),
              copy.getGameOptions().getOption(OptionsConstants.ALLOWED_TECH_LEVEL).stringValue());
        final String skillName = SkillType.skillList[0];
        assertEquals(source.getSkills().get(skillName).getTarget(), copy.getSkills().get(skillName).getTarget());
    }

    /**
     * Saving a preset writes the dialog's values into the copy, so editing it must never reach the source (which, when
     * saving, is the running campaign).
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    void createDetachedCopy_sharesNoObjectsWithTheSource() {
        final CampaignPreset source = createPopulatedPreset();
        final String skillName = SkillType.skillList[0];
        final int sourceTarget = source.getSkills().get(skillName).getTarget();

        final CampaignPreset copy = CampaignPreset.createDetachedCopy(source);
        assertNotNull(copy);
        copy.getCampaignOptions().set(CampaignOption.PERCENT_FEMALE, 5);
        copy.getRandomSkillPreferences().setOverallRecruitBonus(1);
        copy.getSkills().get(skillName).setTarget(sourceTarget + 1);

        assertNotSame(source.getCampaignOptions(), copy.getCampaignOptions());
        assertNotSame(source.getGameOptions(), copy.getGameOptions());
        assertNotSame(source.getRandomSkillPreferences(), copy.getRandomSkillPreferences());
        assertNotSame(source.getSkills(), copy.getSkills());
        assertEquals(73, source.getCampaignOptions().get(CampaignOption.PERCENT_FEMALE));
        assertEquals(42, source.getRandomSkillPreferences().getOverallRecruitBonus());
        assertEquals(sourceTarget, source.getSkills().get(skillName).getTarget());
    }

    private static CampaignPreset createPopulatedPreset() {
        final CampaignPreset preset = new CampaignPreset();
        preset.setTitle("Detached Copy Test");
        preset.setGM(false);
        preset.setGameOptions(new GameOptions());
        final CampaignOptions campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PERCENT_FEMALE, 73);
        preset.setCampaignOptions(campaignOptions);
        final RandomSkillPreferences randomSkillPreferences = new RandomSkillPreferences();
        randomSkillPreferences.setOverallRecruitBonus(42);
        preset.setRandomSkillPreferences(randomSkillPreferences);
        preset.setSkills(SkillType.createDefaultTypes());
        return preset;
    }
}
