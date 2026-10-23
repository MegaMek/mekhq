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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.units.Entity;
import mekhq.campaign.finances.Money;
import mekhq.campaign.work.IAcquisitionWork;
import org.junit.jupiter.api.Test;

/**
 * Tests how {@link PartInUse} decides that two entries in the parts in use report are the same item, and that it only
 * works out an item's price when the price is asked for.
 */
class PartInUseTest {
    private static final String MEDIUM_LASER = "Medium Laser";

    @Test
    void entriesForTheSameItemAreEqualAndShareAHash() {
        Part comparisonPart = sameTypeComparisonPart();
        PartInUse firstEntry = new PartInUse(partBuying(acquisitionWork(MEDIUM_LASER, TechBase.IS, comparisonPart)));
        PartInUse secondEntry = new PartInUse(partBuying(acquisitionWork(MEDIUM_LASER, TechBase.IS, comparisonPart)));

        assertEquals(firstEntry, secondEntry);
        assertEquals(firstEntry.hashCode(), secondEntry.hashCode());
    }

    @Test
    void clanAndInnerSphereVersionsAreNotEqual() {
        Part comparisonPart = sameTypeComparisonPart();
        PartInUse innerSphereEntry = new PartInUse(partBuying(acquisitionWork(MEDIUM_LASER, TechBase.IS,
              comparisonPart)));
        PartInUse clanEntry = new PartInUse(partBuying(acquisitionWork(MEDIUM_LASER, TechBase.CLAN,
              comparisonPart)));

        assertNotEquals(innerSphereEntry, clanEntry);
    }

    @Test
    void differentPartTypesWithTheSameNameAreNotEqual() {
        Part comparisonPart = mock(Part.class);
        when(comparisonPart.isSamePartType(any())).thenReturn(false);
        PartInUse firstEntry = new PartInUse(partBuying(acquisitionWork(MEDIUM_LASER, TechBase.IS, comparisonPart)));
        PartInUse secondEntry = new PartInUse(partBuying(acquisitionWork(MEDIUM_LASER, TechBase.IS, comparisonPart)));

        assertNotEquals(firstEntry, secondEntry);
    }

    @Test
    void differentNamesAreNotEqualWithoutBuildingParts() {
        IAcquisitionWork mediumLaserWork = acquisitionWork(MEDIUM_LASER, TechBase.IS, sameTypeComparisonPart());
        IAcquisitionWork largeLaserWork = acquisitionWork("Large Laser", TechBase.IS, sameTypeComparisonPart());
        PartInUse mediumLaserEntry = new PartInUse(partBuying(mediumLaserWork));
        PartInUse largeLaserEntry = new PartInUse(partBuying(largeLaserWork));

        assertNotEquals(mediumLaserEntry, largeLaserEntry);
        verify(mediumLaserWork, never()).getAcquisitionPart();
        verify(largeLaserWork, never()).getAcquisitionPart();
    }

    @Test
    void repeatedComparisonsBuildEachComparisonPartOnce() {
        IAcquisitionWork storedWork = acquisitionWork(MEDIUM_LASER, TechBase.IS, sameTypeComparisonPart());
        IAcquisitionWork incomingWork = acquisitionWork(MEDIUM_LASER, TechBase.IS, sameTypeComparisonPart());
        PartInUse storedEntry = new PartInUse(partBuying(storedWork));
        PartInUse incomingEntry = new PartInUse(partBuying(incomingWork));

        // The report looks each warehouse part up in a map like this, more than once per part
        Map<PartInUse, PartInUse> report = new HashMap<>();
        report.put(storedEntry, storedEntry);
        assertTrue(report.containsKey(incomingEntry));
        assertEquals(storedEntry, report.get(incomingEntry));
        assertEquals(storedEntry, incomingEntry);

        verify(storedWork, times(1)).getAcquisitionPart();
        verify(incomingWork, times(1)).getAcquisitionPart();
    }

    @Test
    void buildingAnEntryDoesNotWorkOutItsPrice() {
        IAcquisitionWork acquisitionWork = acquisitionWork(MEDIUM_LASER, TechBase.IS, sameTypeComparisonPart());

        new PartInUse(partBuying(acquisitionWork));

        verify(acquisitionWork, never()).getBuyCost();
    }

    @Test
    void priceIsWorkedOutOnceWhenFirstAskedFor() {
        IAcquisitionWork acquisitionWork = acquisitionWork(MEDIUM_LASER, TechBase.IS, sameTypeComparisonPart());
        Money price = Money.of(40000);
        when(acquisitionWork.getBuyCost()).thenReturn(price);
        PartInUse entry = new PartInUse(partBuying(acquisitionWork));

        assertEquals(price, entry.getCost());
        assertEquals(price, entry.getCost());
        verify(acquisitionWork, times(1)).getBuyCost();
    }

    @Test
    void armorThatDoesNotExistAtItsTechRatingWeighsNothing() {
        // BAR 10 armor only exists from tech rating D, so it has no points per ton at C
        SVArmor armor = new SVArmor(10, TechRating.C, 0, Entity.LOC_NONE, null);

        PartInUse entry = new PartInUse(armor);

        assertEquals(0.0, entry.getTonnagePerItem());
    }

    /**
     * @return a part that reports every other part as the same type
     */
    private static Part sameTypeComparisonPart() {
        Part comparisonPart = mock(Part.class);
        when(comparisonPart.isSamePartType(any())).thenReturn(true);
        return comparisonPart;
    }

    /**
     * @param acquisitionName the name the report shows for the item
     * @param techBase        the item's tech base
     * @param comparisonPart  the part returned whenever the item's acquisition part is built
     *
     * @return the purchase a parts in use entry is built around
     */
    private static IAcquisitionWork acquisitionWork(String acquisitionName, TechBase techBase,
          Part comparisonPart) {
        IAcquisitionWork acquisitionWork = mock(IAcquisitionWork.class);
        when(acquisitionWork.getAcquisitionName()).thenReturn(acquisitionName);
        when(acquisitionWork.getTechBase()).thenReturn(techBase);
        when(acquisitionWork.getBuyCost()).thenReturn(Money.zero());
        when(acquisitionWork.getAcquisitionPart()).thenReturn(comparisonPart);
        return acquisitionWork;
    }

    /**
     * @param acquisitionWork the purchase the part leads to
     *
     * @return a warehouse part whose replacement is bought through {@code acquisitionWork}
     */
    private static Part partBuying(IAcquisitionWork acquisitionWork) {
        Part part = mock(Part.class);
        String partName = acquisitionWork.getAcquisitionName();
        when(part.getName()).thenReturn(partName);
        when(part.getDetails(false)).thenReturn("");
        when(part.getAcquisitionWork()).thenReturn(acquisitionWork);
        return part;
    }
}
