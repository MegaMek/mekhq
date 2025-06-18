/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.utilities.glossary;

import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DocumentationEntryTest {
    @ParameterizedTest
    @EnumSource(DocumentationEntry.class)
    void testAllEntriesHaveValidTitle(DocumentationEntry entry) {
        String title = entry.getTitle();
        assertTrue(isResourceKeyValid(title), "Title for " + entry.name() + " is invalid: " + title);
    }

    @Test
    void testTitlesAreSortedAlphabeticallyByLookUpNamesOrder() {
        List<String> lookUpNames = DocumentationEntry.getLookUpNamesSortedByTitle();

        List<String> titles = lookUpNames.stream()
                                    .map(name -> Objects.requireNonNull(DocumentationEntry.getDocumentationEntryFromLookUpName(
                                          name)).getTitle())
                                    .toList();

        List<String> sorted = new java.util.ArrayList<>(titles);
        sorted.sort(String::compareTo);

        assertEquals(sorted, titles, "Titles are not sorted alphabetically.");
    }

    @Test
    void testTitlesAreUnique() {
        List<String> lookUpNames = DocumentationEntry.getLookUpNamesSortedByTitle();
        List<String> uniqueTitles = lookUpNames.stream()
                                          .map(name -> Objects.requireNonNull(DocumentationEntry.getDocumentationEntryFromLookUpName(
                                                name)).getTitle())
                                          .distinct()
                                          .toList();

        assertEquals(lookUpNames.size(), uniqueTitles.size(), "Titles are not unique.");
    }

    @ParameterizedTest
    @EnumSource(DocumentationEntry.class)
    void testGetFileAddressReturnsExistingFile(DocumentationEntry documentationEntry) {
        String filePath = documentationEntry.getFileAddress();
        Path path = Path.of(filePath);

        assertTrue(Files.exists(path), "File does not exist at: " + filePath);
        assertTrue(Files.isRegularFile(path), "Path is not a regular file: " + filePath);
        assertTrue(Files.isReadable(path), "File is not readable: " + filePath);
    }

    @ParameterizedTest
    @EnumSource(DocumentationEntry.class)
    void testAllDocumentationEntriesCanBeOpenedAsPdf(DocumentationEntry entry) {
        String fileAddress = entry.getFileAddress();
        File file = new File(fileAddress);
        assertTrue(file.exists(), "File does not exist: " + fileAddress);

        try (PDDocument pdf = Loader.loadPDF(file)) {
            assertNotNull(pdf, "Could not load PDF document: " + fileAddress);
            assertTrue(pdf.getNumberOfPages() > 0, "PDF has no pages: " + fileAddress);
        } catch (IOException e) {
            fail("File '" + fileAddress + "' could not be opened as a PDF. Exception: " + e.getMessage());
        }
    }
}
