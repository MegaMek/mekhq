/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.finances.financialInstitutions;

import megamek.common.Compute;
import mekhq.MHQConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;

public class FinancialInstitutionsTest {

    @BeforeEach
    public void beforeEach() {
        FinancialInstitutions.getFinancialInstitutions().clear();
    }

    @Test
    public void testRandomFinancialInstitution() {
        final FinancialInstitution institution1 = new FinancialInstitution();
        institution1.setShutterDate(LocalDate.of(3036, 1, 1));
        FinancialInstitutions.getFinancialInstitutions().add(institution1);

        final FinancialInstitution institution2 = new FinancialInstitution();
        institution2.setFoundationDate(LocalDate.of(3000, 1, 1));
        FinancialInstitutions.getFinancialInstitutions().add(institution2);

        final FinancialInstitution institution3 = new FinancialInstitution();
        institution3.setFoundationDate(LocalDate.of(3000, 1, 1));
        institution3.setShutterDate(LocalDate.of(3036, 1, 1));
        FinancialInstitutions.getFinancialInstitutions().add(institution3);

        final FinancialInstitution institution4 = new FinancialInstitution();
        institution4.setFoundationDate(LocalDate.of(3045, 1, 1));
        FinancialInstitutions.getFinancialInstitutions().add(institution4);

        final FinancialInstitution institution5 = new FinancialInstitution();
        institution5.setFoundationDate(LocalDate.of(3000, 1, 1));
        institution5.setShutterDate(LocalDate.of(3024, 1, 1));
        FinancialInstitutions.getFinancialInstitutions().add(institution5);

        final FinancialInstitution institution6 = new FinancialInstitution();
        FinancialInstitutions.getFinancialInstitutions().add(institution6);

        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);
            assertEquals(institution1, FinancialInstitutions.randomFinancialInstitution(LocalDate.of(3025, 1, 1)));

            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(1);
            assertEquals(institution2, FinancialInstitutions.randomFinancialInstitution(LocalDate.of(3025, 1, 1)));

            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(3);
            assertEquals(institution6, FinancialInstitutions.randomFinancialInstitution(LocalDate.of(3025, 1, 1)));
        }
    }

    //region File I/O
    @Test
    public void testInitializeFinancialInstitutions() {
        final List<FinancialInstitution> canonFinancialInstitutions = new ArrayList<>();
        final FinancialInstitution institution1 = new FinancialInstitution();
        canonFinancialInstitutions.add(institution1);
        final FinancialInstitution institution2 = new FinancialInstitution();
        canonFinancialInstitutions.add(institution2);

        final List<FinancialInstitution> userdataFinancialInstitutions = new ArrayList<>();
        final FinancialInstitution financialInstitution3 = new FinancialInstitution();
        userdataFinancialInstitutions.add(financialInstitution3);


        try (MockedStatic<FinancialInstitutions> financialInstitutions = Mockito.mockStatic(FinancialInstitutions.class)) {
            financialInstitutions.when(FinancialInstitutions::getFinancialInstitutions).thenCallRealMethod();
            financialInstitutions.when(FinancialInstitutions::initializeFinancialInstitutions).thenCallRealMethod();

            financialInstitutions.when(() -> FinancialInstitutions.loadFinancialInstitutionsFromFile(new File(MHQConstants.FINANCIAL_INSTITUTIONS_FILE_PATH))).thenReturn(canonFinancialInstitutions);
            financialInstitutions.when(() -> FinancialInstitutions.loadFinancialInstitutionsFromFile(new File(MHQConstants.USER_FINANCIAL_INSTITUTIONS_FILE_PATH))).thenReturn(new ArrayList<>());
            FinancialInstitutions.initializeFinancialInstitutions();
            assertEquals(2, FinancialInstitutions.getFinancialInstitutions().size());

            financialInstitutions.when(() -> FinancialInstitutions.loadFinancialInstitutionsFromFile(new File(MHQConstants.USER_FINANCIAL_INSTITUTIONS_FILE_PATH))).thenReturn(userdataFinancialInstitutions);
            FinancialInstitutions.initializeFinancialInstitutions();
            assertEquals(3, FinancialInstitutions.getFinancialInstitutions().size());
        }
    }

    @Test
    public void testLoadFinancialInstitutionsFromFile(final @TempDir Path temporaryDirectory) throws IOException {
        final Path path = temporaryDirectory.resolve("financialInstitutions.xml");
        final List<String> inputText = Arrays.asList("<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<financialInstitutions>",
                "<institution>",
                "<name>Bank of Oriente</name>",
                "</institution>",
                "<institution>",
                "<name>ComStar Green</name>",
                "<shutterDate>20-20-20</shutterDate>",
                "</institution>",
                "</financialInstitutions>");
        Files.write(path, inputText);

        assertEquals(1, FinancialInstitutions.loadFinancialInstitutionsFromFile(path.toFile()).size());
    }

    @Test
    public void testLoadFinancialInstitutionsFromFileErrorCases() {
        // Null File Returns An Empty Array
        assertTrue(FinancialInstitutions.loadFinancialInstitutionsFromFile(null).isEmpty());

        // Illegal File Path Returns An Empty Array
        assertTrue(FinancialInstitutions.loadFinancialInstitutionsFromFile(new File("")).isEmpty());
    }
    //endregion File I/O
}
