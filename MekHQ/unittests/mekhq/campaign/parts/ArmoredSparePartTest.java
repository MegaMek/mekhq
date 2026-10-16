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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * An armored component stays armored when it comes off its unit, and an armored slot is filled with an armored spare
 * and a plain slot with a plain one (issues #8641 and #8845). The Hunchback HBK-4G here has its left arm Medium Laser
 * armored, which adds 150,000 C-bills for the laser's one slot; its right arm Medium Laser is plain.
 */
class ArmoredSparePartTest {
    private static final Money ARMORING_ONE_SLOT = Money.of(150000);

    private final PartsScenario scenario = PartsScenario.create();
    private Unit hunchback;
    private EquipmentPart armoredLaser;
    private EquipmentPart plainLaser;

    @BeforeEach
    void setUp() {
        Entity entity = UnitFixture.HUNCHBACK_HBK_4G.loadEntity();
        Mounted<?> leftArmLaser = mediumLaserIn(entity, "LA");
        leftArmLaser.setArmored(true);
        hunchback = scenario.getCampaign().addNewUnit(entity, false, 0, PartQuality.QUALITY_D);
        armoredLaser = partFor(hunchback, leftArmLaser);
        plainLaser = partFor(hunchback, mediumLaserIn(entity, "RA"));
    }

    private static Mounted<?> mediumLaserIn(Entity entity, String locationAbbreviation) {
        for (Mounted<?> mounted : entity.getEquipment()) {
            boolean isMediumLaser = mounted.getType().getName().equals("Medium Laser");
            boolean isInLocation = entity.getLocationAbbr(mounted.getLocation()).equals(locationAbbreviation);
            if (isMediumLaser && isInLocation) {
                return mounted;
            }
        }
        throw new AssertionError("No Medium Laser in " + locationAbbreviation);
    }

    private static EquipmentPart partFor(Unit unit, Mounted<?> mounted) {
        int equipmentNum = unit.getEntity().getEquipmentNum(mounted);
        for (EquipmentPart part : PartsScenario.unitParts(unit, EquipmentPart.class)) {
            if (part.getEquipmentNum() == equipmentNum) {
                return part;
            }
        }
        throw new AssertionError("No part for " + mounted.getName());
    }

    private MissingEquipmentPart missingPartAt(int equipmentNum) {
        for (MissingEquipmentPart missingPart : PartsScenario.unitParts(hunchback, MissingEquipmentPart.class)) {
            if (missingPart.getEquipmentNum() == equipmentNum) {
                return missingPart;
            }
        }
        throw new AssertionError("No missing part at equipment " + equipmentNum);
    }

    /**
     * Salvages a laser off the Hunchback.
     *
     * @return the spare it leaves in the warehouse
     */
    private EquipmentPart salvage(EquipmentPart laser) {
        laser.remove(true);
        assertTrue(laser.getUnit() == null, "The salvaged laser is a spare");
        return laser;
    }

    @Test
    void anInstalledArmoredComponentCostsTheArmoring() {
        assertTrue(armoredLaser.isArmored());
        assertFalse(plainLaser.isArmored());
        assertEquals(plainLaser.getStickerPrice().plus(ARMORING_ONE_SLOT), armoredLaser.getStickerPrice());
    }

    @Test
    void aSalvagedArmoredComponentStaysArmoredAtItsPrice() {
        Money installedPrice = armoredLaser.getStickerPrice();

        EquipmentPart spare = salvage(armoredLaser);

        assertTrue(spare.isArmored());
        assertEquals(installedPrice, spare.getStickerPrice());
    }

    @Test
    void anArmoredSlotTakesAnArmoredSpareButNotAPlainOne() {
        int armoredSlot = armoredLaser.getEquipmentNum();
        EquipmentPart armoredSpare = salvage(armoredLaser);
        EquipmentPart plainSpare = salvage(plainLaser);

        MissingEquipmentPart missingArmoredLaser = missingPartAt(armoredSlot);

        assertTrue(missingArmoredLaser.isAcceptableReplacement(armoredSpare, false));
        assertFalse(missingArmoredLaser.isAcceptableReplacement(plainSpare, false),
              "An armored slot needs an armored laser");
    }

    @Test
    void aPlainSlotDoesNotTakeAnArmoredSpare() {
        int plainSlot = plainLaser.getEquipmentNum();
        EquipmentPart armoredSpare = salvage(armoredLaser);
        salvage(plainLaser);

        MissingEquipmentPart missingPlainLaser = missingPartAt(plainSlot);

        assertFalse(missingPlainLaser.isAcceptableReplacement(armoredSpare, false));
    }

    @Test
    void aReplacementBoughtForAnArmoredSlotIsArmored() {
        Money installedPrice = armoredLaser.getStickerPrice();
        int armoredSlot = armoredLaser.getEquipmentNum();
        salvage(armoredLaser);

        EquipmentPart replacement = missingPartAt(armoredSlot).getNewPart();

        assertTrue(replacement.isArmored());
        assertEquals(installedPrice, replacement.getStickerPrice(), "It is bought at the armored price");
    }

    @Test
    void armoredAndPlainSparesAreKeptApart() {
        EquipmentPart armoredSpare = salvage(armoredLaser);
        EquipmentPart plainSpare = salvage(plainLaser);

        assertFalse(armoredSpare.isSamePartType(plainSpare));
        assertTrue(armoredSpare.isSamePartType(armoredSpare.clone()));
    }

    @Test
    void anArmoredSpareStaysArmoredThroughASave() throws Exception {
        EquipmentPart armoredSpare = salvage(armoredLaser);

        Part loaded = PartXmlRoundTripTest.saveAndLoad(armoredSpare, scenario.getCampaign());

        EquipmentPart loadedSpare = assertInstanceOf(EquipmentPart.class, loaded);
        assertNotNull(loadedSpare);
        assertTrue(loadedSpare.isArmored());
        assertEquals(armoredSpare.getStickerPrice(), loadedSpare.getStickerPrice());
    }
}
