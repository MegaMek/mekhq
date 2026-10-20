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
package mekhq.campaign.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * The jobs a tech is in the middle of, which mark them as busy in the personnel table (issue #5343).
 */
class TechJobsTest {
    private final PartsScenario scenario = PartsScenario.create();

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    private static EquipmentPart mediumLaserOf(Unit unit) {
        for (EquipmentPart part : PartsScenario.unitParts(unit, EquipmentPart.class)) {
            if ("Medium Laser".equals(part.getName())) {
                return part;
            }
        }
        throw new IllegalStateException("No Medium Laser on " + unit.getName());
    }

    @Test
    void aTechWithNoJobIsNotBusy() {
        Person tech = scenario.withTech(SkillType.EXP_REGULAR);

        assertEquals(List.of(), TechJobs.find(scenario.getCampaign()).getJobs(tech));
    }

    @Test
    void aRepairUnderWayNamesThePartTheUnitAndTheTimeLeft() {
        Person tech = scenario.withTech(SkillType.EXP_REGULAR);
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        Part laser = mediumLaserOf(locust);
        laser.setHits(1);
        laser.setTech(tech);

        List<String> jobs = TechJobs.find(scenario.getCampaign()).getJobs(tech);

        assertEquals(1, jobs.size(), jobs.toString());
        String job = jobs.getFirst();
        assertTrue(job.startsWith("Repairing Medium Laser on " + locust.getName() + " ("), job);
        assertTrue(job.endsWith(" left)"), job);
    }

    @Test
    void aRefitUnderWayIsAJob() throws Exception {
        Person tech = scenario.withTech(SkillType.EXP_REGULAR);
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        Refit refit = new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), true, false, false);
        refit.setTech(tech);
        locust.setRefit(refit);

        List<String> jobs = TechJobs.find(scenario.getCampaign()).getJobs(tech);

        assertEquals(List.of("Refitting " + locust.getName()), jobs.stream()
                                                                      .map(job -> job.substring(0, job.indexOf(" (")))
                                                                      .toList());
    }

    @Test
    void maintenanceStretchesTheTimeLeft() {
        Person tech = scenario.withTech(SkillType.EXP_REGULAR);
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        Part laser = mediumLaserOf(locust);
        laser.setHits(1);
        laser.setTech(tech);
        String withoutMaintenance = TechJobs.find(scenario.getCampaign()).getJobs(tech).getFirst();

        // The same tech now keeps four other units maintained, leaving far less of the day for the repair
        for (int count = 0; count < 4; count++) {
            tech.addTechUnit(scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R));
        }
        String withMaintenance = TechJobs.find(scenario.getCampaign()).getJobs(tech).getFirst();

        assertTrue(!withMaintenance.equals(withoutMaintenance),
              "Less free time means more days left: " + withoutMaintenance + " / " + withMaintenance);
    }
}
