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
package mekhq.campaign.force;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Set;

import mekhq.campaign.Campaign;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a {@link Formation} keeps its identity across the StratCon deploy -> battle ->
 * resolve cycle: its campaign {@code id} and its position in the Table of Equipment
 * ({@code parentFormation}) are unchanged, and only the transient {@code scenarioId} marker moves.
 *
 * <p>Also guards {@link Formation#getFullMMName()} against the force-id collision regression: it must
 * emit each formation's own campaign-unique id, not a derived {@code 17 * id + ...} value, because
 * colliding ids made the MegaMek server merge distinct formations.</p>
 */
class FormationScenarioRetentionTest {

    /**
     * Builds a Regiment &gt; Battalion &gt; Company &gt; Lance ToE with distinct campaign ids and
     * returns the four formations in depth order {regiment, battalion, company, lance}.
     */
    private static Formation[] buildToE() {
        Formation regiment = new Formation("Test Regiment");
        regiment.setId(1);
        Formation battalion = new Formation("First Battalion");
        battalion.setId(2);
        Formation company = new Formation("A Company");
        company.setId(3);
        Formation lance = new Formation("Command Lance");
        lance.setId(4);

        company.addSubFormation(lance, true);
        battalion.addSubFormation(company, true);
        regiment.addSubFormation(battalion, true);
        return new Formation[] { regiment, battalion, company, lance };
    }

    @Test
    void deployAndReleasePreservesFormationIdAndToEPosition() {
        Campaign campaign = mock(Campaign.class);
        Formation[] toe = buildToE();
        Formation regiment = toe[0];
        Formation lance = toe[3];

        int idBefore = lance.getId();
        Formation parentBefore = lance.getParentFormation();
        assertEquals(Formation.NO_ASSIGNED_SCENARIO, lance.getScenarioId(),
              "a fresh formation should not be deployed to a scenario");

        // Deploy: StratCon marks the whole formation subtree with the scenario id.
        final int scenarioId = 77;
        regiment.setScenarioId(scenarioId, campaign);
        assertEquals(scenarioId, lance.getScenarioId(), "deploy should set the scenario marker");
        assertEquals(idBefore, lance.getId(), "deploy must not change the formation id");
        assertSame(parentBefore, lance.getParentFormation(),
              "deploy must not change the formation's ToE position");

        // Resolve: scenario completion clears the marker; structure must be untouched.
        regiment.clearScenarioIds(campaign);
        assertEquals(Formation.NO_ASSIGNED_SCENARIO, lance.getScenarioId(),
              "resolving the scenario should clear the scenario marker");
        assertEquals(idBefore, lance.getId(), "resolution must not change the formation id");
        assertSame(parentBefore, lance.getParentFormation(),
              "resolution must not change the formation's ToE position");
    }

    @Test
    void getFullMMNameEmitsEachFormationsOwnUniqueId() {
        Formation[] toe = buildToE();
        Set<Integer> seenIds = new HashSet<>();
        for (Formation formation : toe) {
            String mmName = formation.getFullMMName();
            int lastId = lastSegmentId(mmName);
            assertEquals(formation.getId(), lastId,
                  "getFullMMName must emit the formation's own campaign id, not a derived value: " + mmName);
            assertTrue(seenIds.add(lastId),
                  "two formations produced the same force-string id (collision): " + mmName);
        }
    }

    @Test
    void getFullMMNameRendersTheFullToeChain() {
        Formation lance = buildToE()[3];
        assertEquals("Test Regiment|1||First Battalion|2||A Company|3||Command Lance|4||",
              lance.getFullMMName(),
              "the force string should be the top-down chain of ancestor formations, each as name|id");
    }

    /**
     * Returns the id of the last {@code name|id} segment of a MegaMek force string.
     */
    private static int lastSegmentId(String forceString) {
        String[] segments = forceString.split("\\|\\|");
        String[] fields = segments[segments.length - 1].split("\\|");
        return Integer.parseInt(fields[1]);
    }
}
