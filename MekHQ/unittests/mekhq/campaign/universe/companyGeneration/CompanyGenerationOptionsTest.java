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
package mekhq.campaign.universe.companyGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import megamek.common.enums.SkillLevel;
import mekhq.MHQConstants;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Constructor-default and XML-round-trip coverage for the support-personnel coverage / skill /
 * astech / medic fields added for the RULESET_BASED pipeline. Faction-dependent paths and the
 * Random Origin sub-block are out of scope — those work today via the same mechanism and are
 * tested indirectly by the existing presets that load on startup.
 */
class CompanyGenerationOptionsTest {

    /**
     * Loggers that emit expected ERROR-level noise in this test class. They're silenced for the
     * whole class run so {@code MekHQ\logs\mekhq.log} (the shared production log) doesn't get
     * polluted with stack traces that aren't a real problem.
     *
     * <p>{@code RandomOriginOptions}: its constructor logs at ERROR with a stack trace when it
     * can't find the default planet. That happens unconditionally in unit-test context (no
     * universe data is loaded) and fires on every {@code new CompanyGenerationOptions(...)}.</p>
     */
    private static final String[] SILENCED_LOGGERS = { "mekhq.campaign.RandomOriginOptions" };
    private static final Level[] PREVIOUS_LEVELS = new Level[SILENCED_LOGGERS.length];

    @BeforeAll
    static void silenceExpectedErrorLoggers() {
        for (int i = 0; i < SILENCED_LOGGERS.length; i++) {
            PREVIOUS_LEVELS[i] = LogManager.getLogger(SILENCED_LOGGERS[i]).getLevel();
            Configurator.setLevel(SILENCED_LOGGERS[i], Level.OFF);
        }
    }

    @AfterAll
    static void restoreLoggers() {
        for (int i = 0; i < SILENCED_LOGGERS.length; i++) {
            Configurator.setLevel(SILENCED_LOGGERS[i], PREVIOUS_LEVELS[i]);
        }
    }

    // ===== Constructor defaults =====

    @Test
    void constructor_seedsCoveragePercentsAtOneHundred_forEverySupportRole() {
        CompanyGenerationOptions options = new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);

        assertNotNull(options.getSupportPersonnelCoveragePercents());
        for (PersonnelRole role : new PersonnelRole[] {
                PersonnelRole.MEK_TECH, PersonnelRole.MECHANIC, PersonnelRole.AERO_TEK, PersonnelRole.BA_TECH,
                PersonnelRole.DOCTOR,
                PersonnelRole.ADMINISTRATOR_COMMAND, PersonnelRole.ADMINISTRATOR_LOGISTICS,
                PersonnelRole.ADMINISTRATOR_TRANSPORT, PersonnelRole.ADMINISTRATOR_HR
        }) {
            assertEquals(100, options.getSupportPersonnelCoveragePercents().get(role),
                  "Default coverage for " + role.name() + " should be 100");
        }
    }

    @Test
    void constructor_seedsSkillLevelsAtRegular_forEverySupportRole() {
        CompanyGenerationOptions options = new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);

        assertNotNull(options.getSupportPersonnelSkillLevels());
        for (PersonnelRole role : new PersonnelRole[] {
                PersonnelRole.MEK_TECH, PersonnelRole.MECHANIC, PersonnelRole.AERO_TEK, PersonnelRole.BA_TECH,
                PersonnelRole.DOCTOR,
                PersonnelRole.ADMINISTRATOR_COMMAND, PersonnelRole.ADMINISTRATOR_LOGISTICS,
                PersonnelRole.ADMINISTRATOR_TRANSPORT, PersonnelRole.ADMINISTRATOR_HR
        }) {
            assertEquals(SkillLevel.REGULAR, options.getSupportPersonnelSkillLevels().get(role),
                  "Default skill level for " + role.name() + " should be REGULAR");
        }
    }

    @Test
    void constructor_assistantDefaults_matchLegacyPoolAssistantsBehavior() {
        CompanyGenerationOptions options = new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);

        assertTrue(options.isGenerateAstechs(), "Astechs generated by default");
        assertFalse(options.isAstechsAsPersonnel(), "Pool mode by default");
        assertEquals(SkillLevel.REGULAR, options.getAstechSkillLevel());

        assertTrue(options.isGenerateMedics());
        assertFalse(options.isMedicsAsPersonnel());
        assertEquals(SkillLevel.REGULAR, options.getMedicSkillLevel());
    }

    @Test
    void constructor_defaultsConsistentAcrossMethods() {
        // The new ratgen-specific fields are method-independent. AtB and Windchild presets get the
        // same defaults so switching method later doesn't lose configuration.
        CompanyGenerationOptions windchild = new CompanyGenerationOptions(CompanyGenerationMethod.WINDCHILD);
        CompanyGenerationOptions atb = new CompanyGenerationOptions(CompanyGenerationMethod.AGAINST_THE_BOT);
        CompanyGenerationOptions ruleset = new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);

        assertEquals(100, windchild.getSupportPersonnelCoveragePercents().get(PersonnelRole.MEK_TECH));
        assertEquals(100, atb.getSupportPersonnelCoveragePercents().get(PersonnelRole.MEK_TECH));
        assertEquals(100, ruleset.getSupportPersonnelCoveragePercents().get(PersonnelRole.MEK_TECH));
    }

    // ===== Writer: verify the new tags appear =====
    //
    // We can't easily call CompanyGenerationOptions.writeToXML(...) end-to-end in a test because
    // its RandomOriginOptions sub-block walks a Planet/PlanetarySystem graph that needs the full
    // universe loaded. We split the verification: writer tests below capture the partial output
    // up to (but not including) the random-origin section by catching the NPE, and parser tests
    // further down construct synthetic XML and verify the field-by-field parse path.

    @Test
    void writer_emitsCoveragePercentsBlock_withPerRoleEntries() throws Exception {
        CompanyGenerationOptions options = new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);
        options.getSupportPersonnelCoveragePercents().put(PersonnelRole.MEK_TECH, 150);
        options.getSupportPersonnelCoveragePercents().put(PersonnelRole.DOCTOR, 50);

        String xml = capturePartialWrite(options);

        assertTrue(xml.contains("<supportPersonnelCoveragePercents>"));
        assertTrue(xml.contains("<MEK_TECH>150</MEK_TECH>"));
        assertTrue(xml.contains("<DOCTOR>50</DOCTOR>"));
        assertTrue(xml.contains("</supportPersonnelCoveragePercents>"));
    }

    @Test
    void writer_emitsSkillLevelsBlock_withPerRoleEntries() throws Exception {
        CompanyGenerationOptions options = new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);
        options.getSupportPersonnelSkillLevels().put(PersonnelRole.MEK_TECH, SkillLevel.ELITE);
        options.getSupportPersonnelSkillLevels().put(PersonnelRole.AERO_TEK, SkillLevel.GREEN);

        String xml = capturePartialWrite(options);

        assertTrue(xml.contains("<supportPersonnelSkillLevels>"));
        assertTrue(xml.contains("<MEK_TECH>ELITE</MEK_TECH>"));
        assertTrue(xml.contains("<AERO_TEK>GREEN</AERO_TEK>"));
        assertTrue(xml.contains("</supportPersonnelSkillLevels>"));
    }

    @Test
    void writer_emitsAstechAndMedicTags() throws Exception {
        CompanyGenerationOptions options = new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);
        options.setGenerateAstechs(true);
        options.setAstechsAsPersonnel(true);
        options.setAstechSkillLevel(SkillLevel.VETERAN);
        options.setGenerateMedics(false);
        options.setMedicsAsPersonnel(false);
        options.setMedicSkillLevel(SkillLevel.ULTRA_GREEN);

        String xml = capturePartialWrite(options);

        assertTrue(xml.contains("<generateAstechs>true</generateAstechs>"));
        assertTrue(xml.contains("<astechsAsPersonnel>true</astechsAsPersonnel>"));
        assertTrue(xml.contains("<astechSkillLevel>VETERAN</astechSkillLevel>"));
        assertTrue(xml.contains("<generateMedics>false</generateMedics>"));
        assertTrue(xml.contains("<medicsAsPersonnel>false</medicsAsPersonnel>"));
        assertTrue(xml.contains("<medicSkillLevel>ULTRA_GREEN</medicSkillLevel>"));
    }

    @Test
    void writer_emitsAstechFieldsAfterPoolAssistants_forLegacyMirrorOrdering() throws Exception {
        // The legacy migration in parseFromXML mirrors poolAssistants onto generateAstechs /
        // generateMedics. For new presets, the explicit tags must overwrite that fallback —
        // which only works if the writer emits them AFTER poolAssistants.
        CompanyGenerationOptions options = new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);

        String xml = capturePartialWrite(options);

        int poolIndex = xml.indexOf("<poolAssistants>");
        int astechIndex = xml.indexOf("<generateAstechs>");
        int medicIndex = xml.indexOf("<generateMedics>");

        assertTrue(poolIndex > 0, "poolAssistants tag present");
        assertTrue(astechIndex > poolIndex, "generateAstechs must come after poolAssistants");
        assertTrue(medicIndex > poolIndex, "generateMedics must come after poolAssistants");
    }

    // ===== Parser: hand-rolled XML with the new tags =====

    @Test
    void parser_readsCoveragePercents_perRole() throws Exception {
        String xml = """
              <companyGenerationOptions version="0.50.10">
                  <method>RULESET_BASED</method>
                  <specifiedFaction>FS</specifiedFaction>
                  <supportPersonnelCoveragePercents>
                      <MEK_TECH>150</MEK_TECH>
                      <DOCTOR>50</DOCTOR>
                  </supportPersonnelCoveragePercents>
              </companyGenerationOptions>
              """;

        CompanyGenerationOptions parsed = parseXml(xml);

        assertNotNull(parsed);
        assertEquals(150, parsed.getSupportPersonnelCoveragePercents().get(PersonnelRole.MEK_TECH));
        assertEquals(50, parsed.getSupportPersonnelCoveragePercents().get(PersonnelRole.DOCTOR));
    }

    @Test
    void parser_readsSkillLevels_perRole() throws Exception {
        String xml = """
              <companyGenerationOptions version="0.50.10">
                  <method>RULESET_BASED</method>
                  <specifiedFaction>FS</specifiedFaction>
                  <supportPersonnelSkillLevels>
                      <MEK_TECH>ELITE</MEK_TECH>
                      <AERO_TEK>GREEN</AERO_TEK>
                  </supportPersonnelSkillLevels>
              </companyGenerationOptions>
              """;

        CompanyGenerationOptions parsed = parseXml(xml);

        assertNotNull(parsed);
        assertEquals(SkillLevel.ELITE, parsed.getSupportPersonnelSkillLevels().get(PersonnelRole.MEK_TECH));
        assertEquals(SkillLevel.GREEN, parsed.getSupportPersonnelSkillLevels().get(PersonnelRole.AERO_TEK));
    }

    @Test
    void parser_readsAstechFields() throws Exception {
        String xml = """
              <companyGenerationOptions version="0.50.10">
                  <method>RULESET_BASED</method>
                  <specifiedFaction>FS</specifiedFaction>
                  <generateAstechs>true</generateAstechs>
                  <astechsAsPersonnel>true</astechsAsPersonnel>
                  <astechSkillLevel>VETERAN</astechSkillLevel>
              </companyGenerationOptions>
              """;

        CompanyGenerationOptions parsed = parseXml(xml);

        assertNotNull(parsed);
        assertTrue(parsed.isGenerateAstechs());
        assertTrue(parsed.isAstechsAsPersonnel());
        assertEquals(SkillLevel.VETERAN, parsed.getAstechSkillLevel());
    }

    @Test
    void parser_readsMedicFields() throws Exception {
        String xml = """
              <companyGenerationOptions version="0.50.10">
                  <method>RULESET_BASED</method>
                  <specifiedFaction>FS</specifiedFaction>
                  <generateMedics>true</generateMedics>
                  <medicsAsPersonnel>true</medicsAsPersonnel>
                  <medicSkillLevel>ULTRA_GREEN</medicSkillLevel>
              </companyGenerationOptions>
              """;

        CompanyGenerationOptions parsed = parseXml(xml);

        assertNotNull(parsed);
        assertTrue(parsed.isGenerateMedics());
        assertTrue(parsed.isMedicsAsPersonnel());
        assertEquals(SkillLevel.ULTRA_GREEN, parsed.getMedicSkillLevel());
    }

    // ===== Legacy migration =====

    @Test
    void legacyPreset_poolAssistantsOnly_mirrorsOntoGenerateAstechsAndMedics() throws Exception {
        // Simulates loading an old preset that predates the astech/medic split: only the
        // `<poolAssistants>` tag is present. Both new flags should pick up the legacy value.
        String legacyXml = """
              <companyGenerationOptions version="0.50.10">
                  <method>RULESET_BASED</method>
                  <specifiedFaction>FS</specifiedFaction>
                  <poolAssistants>true</poolAssistants>
              </companyGenerationOptions>
              """;

        CompanyGenerationOptions parsed = parseXml(legacyXml);

        assertNotNull(parsed);
        assertTrue(parsed.isPoolAssistants());
        assertTrue(parsed.isGenerateAstechs(),
              "poolAssistants=true should migrate onto generateAstechs in old presets");
        assertTrue(parsed.isGenerateMedics(),
              "poolAssistants=true should migrate onto generateMedics in old presets");
    }

    @Test
    void legacyPreset_poolAssistantsOff_mirrorsOff() throws Exception {
        String legacyXml = """
              <companyGenerationOptions version="0.50.10">
                  <method>RULESET_BASED</method>
                  <specifiedFaction>FS</specifiedFaction>
                  <poolAssistants>false</poolAssistants>
              </companyGenerationOptions>
              """;

        CompanyGenerationOptions parsed = parseXml(legacyXml);

        assertNotNull(parsed);
        assertFalse(parsed.isPoolAssistants());
        assertFalse(parsed.isGenerateAstechs());
        assertFalse(parsed.isGenerateMedics());
    }

    @Test
    void newPreset_explicitFieldsOverrideLegacyMirror() throws Exception {
        // poolAssistants is true but generateAstechs/generateMedics are explicitly false. The
        // explicit values must win — they appear after poolAssistants in writeToXML, so the
        // legacy mirror fires first and is then overwritten.
        String xml = """
              <companyGenerationOptions version="0.50.10">
                  <method>RULESET_BASED</method>
                  <specifiedFaction>FS</specifiedFaction>
                  <poolAssistants>true</poolAssistants>
                  <generateAstechs>false</generateAstechs>
                  <generateMedics>false</generateMedics>
              </companyGenerationOptions>
              """;

        CompanyGenerationOptions parsed = parseXml(xml);

        assertNotNull(parsed);
        assertTrue(parsed.isPoolAssistants(), "Legacy flag still parsed for legacy paths");
        assertFalse(parsed.isGenerateAstechs(), "Explicit value overrides the legacy mirror");
        assertFalse(parsed.isGenerateMedics(), "Explicit value overrides the legacy mirror");
    }

    @Test
    void invalidSkillLevelString_fallsBackToRegular() throws Exception {
        String xml = """
              <companyGenerationOptions version="0.50.10">
                  <method>RULESET_BASED</method>
                  <specifiedFaction>FS</specifiedFaction>
                  <astechSkillLevel>NOT_A_REAL_SKILL_LEVEL</astechSkillLevel>
                  <medicSkillLevel>ALSO_NOT_REAL</medicSkillLevel>
              </companyGenerationOptions>
              """;

        CompanyGenerationOptions parsed = parseXml(xml);

        assertNotNull(parsed);
        assertEquals(SkillLevel.REGULAR, parsed.getAstechSkillLevel(),
              "Unparseable skill-level string falls back to Regular");
        assertEquals(SkillLevel.REGULAR, parsed.getMedicSkillLevel(),
              "Unparseable skill-level string falls back to Regular");
    }

    // ===== Helpers =====

    /**
     * Captures whatever {@code writeToXML} produces before it hits its downstream
     * {@code RandomOriginOptions} block (which needs the full universe loaded). The Personnel
     * region — including all the new support-personnel tags — is written well before that block,
     * so the captured string contains everything we need to verify writer output.
     *
     * <p>The downstream NPE is expected in unit-test context and silenced at the class level via
     * {@link #silenceExpectedErrorLoggers()}; we catch it here so {@code writeToXML} can finish
     * flushing the writer before the exception propagates.</p>
     */
    private static String capturePartialWrite(CompanyGenerationOptions options) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            try {
                options.writeToXML(pw, 0, MHQConstants.VERSION);
            } catch (NullPointerException ignored) {
                // RandomOriginOptions.writeToXML NPEs without loaded planet data; everything we
                // care about has already been flushed to the writer by then.
            }
        }
        return sw.toString();
    }

    private static CompanyGenerationOptions parseXml(String xml) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Element root = builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
        root.normalize();
        Version version = new Version(root.getAttribute("version"));
        return CompanyGenerationOptions.parseFromXML(root.getChildNodes(), version);
    }
}
