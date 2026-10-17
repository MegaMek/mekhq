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
package mekhq.campaign.unit;

import static mekhq.campaign.unit.Unit.SITE_FACILITY_BASIC;
import static mekhq.campaign.unit.Unit.SITE_FACILITY_MAINTENANCE;
import static mekhq.campaign.unit.Unit.SITE_FACTORY_CONDITIONS;
import static mekhq.campaign.unit.Unit.SITE_FIELD_WORKSHOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * A unit joining the force goes where the rest of the force is worked on (MekHQ issue 6541). A player who rented
 * Factory Conditions for the whole force saw every salvaged or bought unit start at a basic facility.
 */
class NewUnitRepairSiteTest {
    private static final int TRANSIT_DAYS = 3;

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    private static Unit addLocust(Campaign campaign, int days) {
        return campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, days, PartQuality.QUALITY_D);
    }

    private static Campaign campaignWithForceAt(int site) {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        addLocust(campaign, 0).setSite(site);
        addLocust(campaign, 0).setSite(site);
        return campaign;
    }

    private static Unit unitAt(int site, boolean isPresent, boolean isMothballed) {
        Unit unit = mock(Unit.class);
        when(unit.getSite()).thenReturn(site);
        when(unit.isPresent()).thenReturn(isPresent);
        when(unit.isMothballed()).thenReturn(isMothballed);
        return unit;
    }

    @Test
    void newUnitJoinsTheForceAtItsRentedFactory() {
        Campaign campaign = campaignWithForceAt(SITE_FACTORY_CONDITIONS);

        assertEquals(SITE_FACTORY_CONDITIONS, addLocust(campaign, 0).getSite());
    }

    @Test
    void firstUnitGetsTheContractSite() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();

        assertEquals(SITE_FACILITY_BASIC, addLocust(campaign, 0).getSite());
    }

    /** The site is picked when the unit arrives, so a unit still on its way keeps the default until then. */
    @Test
    void unitStillInTransitIsPlacedOnArrival() {
        Campaign campaign = campaignWithForceAt(SITE_FACTORY_CONDITIONS);

        assertEquals(SITE_FACILITY_BASIC, addLocust(campaign, TRANSIT_DAYS).getSite());
        assertEquals(SITE_FACTORY_CONDITIONS, NewUnitRepairSite.getSite(campaign));
    }

    @Test
    void rentedSiteIsNotUsedWhereBaysCannotBeRented() {
        Campaign campaign = campaignWithForceAt(SITE_FACTORY_CONDITIONS);
        campaign.setLocation(new CurrentLocation(campaign.getCurrentSystem(), TRANSIT_DAYS));

        assertEquals(SITE_FACILITY_BASIC, NewUnitRepairSite.getSite(campaign));
    }

    @Test
    void freeSiteIsUsedEvenWhereBaysCannotBeRented() {
        Campaign campaign = campaignWithForceAt(SITE_FIELD_WORKSHOP);
        campaign.setLocation(new CurrentLocation(campaign.getCurrentSystem(), TRANSIT_DAYS));

        assertEquals(SITE_FIELD_WORKSHOP, NewUnitRepairSite.getSite(campaign));
    }

    @Test
    void theSiteMostUnitsUseWins() {
        List<Unit> units = List.of(unitAt(SITE_FIELD_WORKSHOP, true, false),
              unitAt(SITE_FIELD_WORKSHOP, true, false),
              unitAt(SITE_FACTORY_CONDITIONS, true, false));

        assertEquals(SITE_FIELD_WORKSHOP, NewUnitRepairSite.getMostUsedSite(units));
    }

    @Test
    void aTieGoesToTheBetterSite() {
        List<Unit> units = List.of(unitAt(SITE_FACILITY_MAINTENANCE, true, false),
              unitAt(SITE_FIELD_WORKSHOP, true, false));

        assertEquals(SITE_FACILITY_MAINTENANCE, NewUnitRepairSite.getMostUsedSite(units));
    }

    @Test
    void mothballedAndUndeliveredUnitsDoNotCount() {
        List<Unit> units = List.of(unitAt(SITE_FACTORY_CONDITIONS, true, true),
              unitAt(SITE_FACTORY_CONDITIONS, false, false));

        assertNull(NewUnitRepairSite.getMostUsedSite(units));
    }
}
