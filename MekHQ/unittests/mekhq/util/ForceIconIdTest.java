/*
 * MekHQ - Copyright (C) 2018 - The MekHQ Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package mekhq.util;

import static org.junit.Assert.*;

import org.junit.Test;

@SuppressWarnings({"javadoc","nls"})
public class ForceIconIdTest {

    @Test
    public void testDuplicates() {
        ImageId i1 = ImageId.of("a", "b");
        ImageId i2 = ImageId.of("a", "b");
        assertEquals(1, ForceIconId.EMPTY.withAddedLayers(i1, i2).layers().count());
    }

}
