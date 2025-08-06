/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.io;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class FileTypeTest {

    /**
     * Ensures we didn't forget to specify the extensions in some file type.
     */
    @Test
    public void testExtensionsProvided() {
        for (FileType ft : FileType.values()) {
            assertFalse(ft.getExtensions().isEmpty());
        }
    }

    @Test
    public void testFileNamefilter() {
        Arrays.asList(
              "file.cpnx", "file.CPNX",
              "file.xml", "file.XML",
              "file.cpnx.gz", "file.CPNX.GZ", "file.CPNX.gz",
              "some/dir/file.xml"
        ).forEach(fn -> assertTrue(FileType.CPNX.getNameFilter().test(fn), fn + " was not accepted"));

        Arrays.asList(
              "file.abc",
              "file.xml.abc",
              "file.xmlabc",
              "file.abcxml"
        ).forEach(fn -> assertFalse(FileType.CPNX.getNameFilter().test(fn), fn + " was not refused"));
    }
}
