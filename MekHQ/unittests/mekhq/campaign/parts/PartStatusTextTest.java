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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;
import mekhq.gui.sorter.WarehouseStatusSorter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * The status the Warehouse tab shows for a part comes from the resource bundle, and an empty OmniPod reads as empty
 * (issue #10321). The warehouse sorts parts in transit by the number of days in the text, so that shape is kept.
 */
class PartStatusTextTest {
    private Campaign campaign;
    private Part spare;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        spare = PartsScenario.unitParts(locust, EquipmentPart.class).getFirst().clone();
        spare.setCampaign(campaign);
    }

    private String statusArrivingIn(int days) {
        Part arriving = spare.clone();
        arriving.setDaysToArrival(days);
        return arriving.getStatus();
    }

    @Test
    void anEmptyOmniPodReadsAsEmpty() {
        OmniPod pod = new OmniPod(spare, campaign);

        assertEquals("Empty", pod.getStatus());
    }

    @Test
    void aSparePartReadsAsFunctionalOrDamaged() {
        assertEquals("Functional", spare.getStatus());

        spare.setHits(1);

        assertEquals("Damaged", spare.getStatus());
    }

    @Test
    void aPartInTransitCountsItsDays() {
        assertEquals("In transit (1 day)", statusArrivingIn(1));
        assertEquals("In transit (5 days)", statusArrivingIn(5));
        assertEquals("In transit (1000 days)", statusArrivingIn(1000), "No thousands separator, so it still sorts");
    }

    @Test
    void theWarehouseStillSortsPartsInTransitByTheirDays() {
        List<String> statuses = new ArrayList<>(List.of(statusArrivingIn(1000), statusArrivingIn(5),
              spare.getStatus(), statusArrivingIn(1)));

        statuses.sort(new WarehouseStatusSorter());

        assertEquals(List.of("Functional", "In transit (1 day)", "In transit (5 days)", "In transit (1000 days)"),
              statuses);
    }

    @Test
    void aTranslatedTransitStatusStillSortsByItsDays() {
        List<String> statuses = new ArrayList<>(List.of("Unterwegs (12 Tage)", "Funktionsfaehig", "Unterwegs (3 Tage)",
              "Unterwegs (1 Tag)"));

        statuses.sort(new WarehouseStatusSorter());

        assertEquals(List.of("Funktionsfaehig", "Unterwegs (1 Tag)", "Unterwegs (3 Tage)", "Unterwegs (12 Tage)"),
              statuses, "12 days sorts after 3 days, not before it as text would");
    }
}
