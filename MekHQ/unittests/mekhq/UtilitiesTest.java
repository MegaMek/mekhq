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

import static mekhq.campaign.personnel.skills.SkillType.EXP_ELITE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.gui.enums.PersonnelTableModelColumn.BLOODNAME;
import static mekhq.gui.enums.PersonnelTableModelColumn.FIRST_NAME;
import static mekhq.gui.enums.PersonnelTableModelColumn.LAST_NAME;
import static mekhq.gui.enums.PersonnelTableModelColumn.PERSONNEL_STATUS;
import static mekhq.gui.enums.PersonnelTableModelColumn.PERSON_GRAPHICAL;
import static mekhq.gui.enums.PersonnelTableModelColumn.RANK;
import static mekhq.gui.enums.PersonnelTableModelColumn.SKILL_LEVEL;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.baseComponents.tables.MHQTableModel;
import mekhq.gui.enums.PersonnelTableModelColumn;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for personnel skill adjustments and CSV table exports.
 */
class UtilitiesTest {
    private Campaign mockCampaign;

    @BeforeEach
    void setUp() {
        SkillType.initializeTypes();
        mockCampaign = mock(Campaign.class);
        // Person construction reads campaign.getPlayerForce().getRankSystem(); the force just needs to be non-null.
        when(mockCampaign.getPlayerForce()).thenReturn(mock(PlayerForce.class));
    }

    private Person newPersonWithSkills(String... skillNames) {
        Person person = new Person(mockCampaign, "MERC");
        for (String skillName : skillNames) {
            person.addSkill(skillName, EXP_REGULAR, 0);
        }
        return person;
    }

    @Test
    void appliesBonusToAllMappedSkillsForMekWarriorPhenotype() {
        Person person = newPersonWithSkills(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK);

        Utilities.applyPhenotypeSkillBonus(person, Phenotype.MEKWARRIOR);

        assertEquals(1, person.getSkill(SkillType.S_GUN_MEK).getBonus());
        assertEquals(1, person.getSkill(SkillType.S_PILOT_MEK).getBonus());
    }

    @Test
    void appliesBonusToAllMappedSkillsForNavalPhenotype() {
        Person person = newPersonWithSkills(SkillType.S_TECH_VESSEL,
              SkillType.S_GUN_SPACE,
              SkillType.S_PILOT_SPACE,
              SkillType.S_NAVIGATION);

        Utilities.applyPhenotypeSkillBonus(person, Phenotype.NAVAL);

        assertEquals(1, person.getSkill(SkillType.S_TECH_VESSEL).getBonus());
        assertEquals(1, person.getSkill(SkillType.S_GUN_SPACE).getBonus());
        assertEquals(1, person.getSkill(SkillType.S_PILOT_SPACE).getBonus());
        assertEquals(1, person.getSkill(SkillType.S_NAVIGATION).getBonus());
    }

    @Test
    void doesNotTouchSkillsOutsideThePhenotypeMapping() {
        Person person = newPersonWithSkills(SkillType.S_GUN_MEK, SkillType.S_SMALL_ARMS);

        Utilities.applyPhenotypeSkillBonus(person, Phenotype.MEKWARRIOR);

        assertEquals(1, person.getSkill(SkillType.S_GUN_MEK).getBonus());
        assertEquals(0, person.getSkill(SkillType.S_SMALL_ARMS).getBonus());
    }

    @Test
    void addsToAnyExistingBonusRatherThanOverwriting() {
        Person person = new Person(mockCampaign, "MERC");
        person.addSkill(SkillType.S_GUN_MEK, EXP_REGULAR, 2);

        Utilities.applyPhenotypeSkillBonus(person, Phenotype.MEKWARRIOR);

        assertEquals(3, person.getSkill(SkillType.S_GUN_MEK).getBonus());
    }

    @Test
    void ignoresMappedSkillsThePersonDoesNotHave() {
        // A MekWarrior phenotype maps to both gunnery and piloting; a person with only one should not gain the other.
        Person person = newPersonWithSkills(SkillType.S_GUN_MEK);

        Utilities.applyPhenotypeSkillBonus(person, Phenotype.MEKWARRIOR);

        assertEquals(1, person.getSkill(SkillType.S_GUN_MEK).getBonus());
        assertFalse(person.hasSkill(SkillType.S_PILOT_MEK));
    }

    @Test
    void internalPhenotypesApplyNoBonus() {
        Person person = newPersonWithSkills(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK);

        Utilities.applyPhenotypeSkillBonus(person, Phenotype.NONE);
        Utilities.applyPhenotypeSkillBonus(person, Phenotype.GENERAL);

        assertEquals(0, person.getSkill(SkillType.S_GUN_MEK).getBonus());
        assertEquals(0, person.getSkill(SkillType.S_PILOT_MEK).getBonus());
    }

    @Nested
    class CsvExport {
        @TempDir
        Path directory;

        @Test
        void exportsPersonnelColumnTextInsteadOfRawModelObjects() throws IOException {
            when(mockCampaign.getCampaignOptions()).thenReturn(mock(CampaignOptions.class));
            when(mockCampaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));
            Person person = mock(Person.class);
            String fullName = "Fiona \"Tango\" Bignal";
            when(person.toString()).thenReturn(fullName);
            when(person.getFullDesc(mockCampaign))
                  .thenReturn("<b>Captain " + fullName + "</b><br/>Elite MekWarrior");
            when(person.getRankName()).thenReturn("Captain");
            when(person.getFirstName()).thenReturn("Fiona");
            when(person.getLastName()).thenReturn("Bignal");
            when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
            when(person.getExperienceLevel(mockCampaign.getCampaignOptions(), false, mockCampaign.getLocalDate(),
                  false, true)).thenReturn(EXP_ELITE);

            MHQTableModel<Person, PersonnelTableModelColumn> model = new MHQTableModel<>(
                  List.of(PERSON_GRAPHICAL, RANK, FIRST_NAME, LAST_NAME, SKILL_LEVEL, PERSONNEL_STATUS, BLOODNAME)) {
                @Override
                protected Object getCellValue(Person row, PersonnelTableModelColumn column) {
                    return column.getCellValue(mockCampaign, row);
                }

                @Override
                protected TableCellRenderer getRenderer() {
                    return new Renderer();
                }
            };
            model.setData(List.of(person));

            CSVRecord record = exportAndRead(new JTable(model)).getFirst();

            assertEquals("Captain " + fullName + "\nElite MekWarrior", record.get(PERSON_GRAPHICAL.toString()));
            assertEquals("Captain", record.get(RANK.toString()));
            assertEquals("Fiona", record.get(FIRST_NAME.toString()));
            assertEquals("Bignal", record.get(LAST_NAME.toString()));
            assertEquals("Elite", record.get(SKILL_LEVEL.toString()));
            assertEquals(PersonnelStatus.ACTIVE.getLabel(), record.get(PERSONNEL_STATUS.toString()));
            assertEquals("", record.get(BLOODNAME.toString()));
        }

        @ParameterizedTest
        @ValueSource(strings = { "<br>", "<br/>", "<br />", "<BR>" })
        void preservesHtmlLineBreaksInsideCsvFields(String lineBreak) throws IOException {
            JTable table = new JTable(new DefaultTableModel(
                  new Object[][] { { "<html><b>Captain Fiona Bignal</b>" + lineBreak + "Elite MekWarrior</html>" } },
                  new String[] { "Person" }));

            CSVRecord record = exportAndRead(table).getFirst();

            assertEquals("Captain Fiona Bignal\nElite MekWarrior", record.get("Person"));
        }

        @Test
        void preservesPlainTableValuesAndCsvEscaping() throws IOException {
            String name = "R\u00e9my, \"Ace\"";
            JTable table = new JTable(new DefaultTableModel(
                  new Object[][] { { name, "<html><span>Active</span></html>", "First line\nSecond line", 42, null } },
                  new String[] { "Name", "Status", "Notes", "Number", "Empty" }));

            CSVRecord record = exportAndRead(table).getFirst();

            assertEquals(name, record.get("Name"));
            assertEquals("Active", record.get("Status"));
            assertEquals("First line\nSecond line", record.get("Notes"));
            assertEquals("42", record.get("Number"));
            assertEquals("", record.get("Empty"));
        }

        private List<CSVRecord> exportAndRead(JTable table) throws IOException {
            Path file = directory.resolve("personnel.csv");
            String report = Utilities.exportTableToCSV(table, file.toFile());
            assertEquals(table.getModel().getRowCount() + " " + getTextAt("mekhq.resources.Utilities", "RowsWritten.text"),
                  report);

            try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get()
                  .parse(Files.newBufferedReader(file))) {
                int columnCount = table.getModel().getColumnCount();
                assertEquals(columnCount, parser.getHeaderNames().size());
                for (int column = 0; column < columnCount; column++) {
                    assertEquals(table.getModel().getColumnName(column), parser.getHeaderNames().get(column));
                }
                List<CSVRecord> records = parser.getRecords();
                assertEquals(table.getModel().getRowCount(), records.size());
                for (CSVRecord record : records) {
                    assertEquals(columnCount, record.size());
                }
                return records;
            }
        }
    }
}
