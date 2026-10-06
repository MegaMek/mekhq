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
package mekhq.campaign.personnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;

import megamek.common.universe.Bloodnames2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies looking a Bloodname back up from the name a person carries.
 *
 * <p>{@link Person} stores only the name string, so the Bloodname tab has to find the record again to
 * say anything about the house behind it. These tests pin that lookup.</p>
 *
 * <p>The data now comes from {@code data/universe/bloodnames} rather than the retired
 * {@code bloodnames.xml}, and is loaded through {@link Bloodnames2} so the tests do not depend on the
 * data having been staged into this project.</p>
 */
class BloodnameLookupTest {

    /** A Bloodname that exists in the shipped data, with its founder. */
    private static final String KNOWN_BLOODNAME = "Blackburn";
    private static final String KNOWN_FOUNDER = "Annie";

    /** Where the Bloodname data lives, tried in the staged location and then in mm-data itself. */
    private static final String[] CANDIDATE_DIRECTORIES = {
          "data/universe/bloodnames",
          "../mm-data/data/universe/bloodnames",
          "D:/MegaMek Projects/mm-data/data/universe/bloodnames",
    };

    private static boolean dataLoaded;

    @BeforeAll
    static void loadBloodnames() {
        // The data is resolved relative to the working directory, which differs between environments,
        // so the lookup tests skip rather than fail spuriously when it cannot be found. The
        // null-safety tests below hold either way.
        for (String directory : CANDIDATE_DIRECTORIES) {
            if (new File(directory).isDirectory()) {
                Bloodnames2.setInstance(new Bloodnames2(directory));
                dataLoaded = true;
                break;
            }
        }
        if (dataLoaded) {
            Bloodname.loadBloodnameData();
        }
    }

    @Test
    void aBlankOrMissingNameFindsNothing() {
        // A person with no Bloodname is the ordinary case among Clan warriors, not an error.
        assertNull(Bloodname.getBloodname(null));
        assertNull(Bloodname.getBloodname(""));
        assertNull(Bloodname.getBloodname("   "));
    }

    @Test
    void aNameTheDataDoesNotKnowFindsNothing() {
        // The field is free text and can hold a hand-typed name, so the lookup has to miss cleanly.
        assertNull(Bloodname.getBloodname("NotARealBloodname"));
    }

    @Test
    void aKnownNameFindsItsHouse() {
        assumeTrue(dataLoaded, "Bloodname data not reachable from this working directory");

        Bloodname bloodname = Bloodname.getBloodname(KNOWN_BLOODNAME);
        assertNotNull(bloodname, KNOWN_BLOODNAME + " should exist in the shipped data");
        assertEquals(KNOWN_BLOODNAME, bloodname.getName());
        assertEquals(KNOWN_FOUNDER, bloodname.getFounder(),
              "the founder is what identifies the house behind the name");
    }

    @Test
    void theLookupIgnoresCase() {
        // Nothing normalises the stored string, so a name that differs only in case must still match.
        assumeTrue(dataLoaded, "Bloodname data not reachable from this working directory");

        assertNotNull(Bloodname.getBloodname(KNOWN_BLOODNAME.toUpperCase()));
        assertNotNull(Bloodname.getBloodname(KNOWN_BLOODNAME.toLowerCase()));
    }
}
