/*
 * Copyright (c) 2018 The MegaMek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

@SuppressWarnings({"javadoc","nls"})
public class FileTypeTest {

    /**
     * Ensures we didn't forget to specify the extensions in some file type.
     */
    @Test
    public void testExtensionsProvided() {
        for (FileType ft : FileType.values()) {
            assertTrue(!ft.getExtensions().isEmpty());
        }
    }

    @Test
    public void testFileNamefilter() {

        Arrays.asList(
            "file.cpnx", "file.CPNX",
            "file.xml",  "file.XML",
            "some/dir/file.xml"
        ).forEach(fn -> assertTrue(fn + " was not accepted",FileType.CPNX.getNameFilter().test(fn)));

        Arrays.asList(
            "file.abc",
            "file.xml.abc",
            "file.xmlabc",
            "file.abcxml"
        ).forEach(fn -> assertFalse(fn + " was not refused",FileType.CPNX.getNameFilter().test(fn)));

    }

}
