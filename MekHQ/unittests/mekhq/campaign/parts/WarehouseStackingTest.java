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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Warehouse stacks hold only parts that really are alike, and only a spare can be sold (issue #10219).
 *
 * <p>The Wolverine WVR-6R carries an AC/5 and its ammunition bin; refitting it into the WVR-6M takes both out.</p>
 */
class WarehouseStackingTest {
    private static final String AUTOCANNON = "AC/5";

    private PartsScenario scenario;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.PAY_FOR_PARTS, true);
        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.STARTING_CAPITAL, campaign.getLocalDate(), Money.of(10000000), "Test funds");
    }

    private static EquipmentPart findAutocannon(Unit unit) {
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(unit, EquipmentPart.class)) {
            boolean isAutocannon = equipmentPart.getName().startsWith(AUTOCANNON) && !(equipmentPart instanceof AmmoBin);
            if (isAutocannon) {
                return equipmentPart;
            }
        }
        return null;
    }

    private void refitIntoTheSixM(Unit wolverine) throws Exception {
        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6M.loadEntity(), false, false, false);
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());
        refit.succeed();
    }

    @Test
    void aUsedPartTakenOutByARefitDoesNotJoinABrandNewStack() throws Exception {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        EquipmentPart installedAutocannon = findAutocannon(wolverine);
        assertNotNull(installedAutocannon, "The WVR-6R carries an AC/5");
        Part brandNewAutocannon = installedAutocannon.clone();
        campaign.getQuartermaster().addPart(brandNewAutocannon, 0, true);

        refitIntoTheSixM(wolverine);

        int brandNewCount = 0;
        int usedCount = 0;
        for (Part spare : campaign.getPlayerForce().getWarehouse().getSpareParts()) {
            boolean isAutocannon = spare.getName().startsWith(AUTOCANNON) && !(spare instanceof AmmoStorage);
            if (isAutocannon) {
                if (spare.isBrandNew()) {
                    brandNewCount += spare.getQuantity();
                } else {
                    usedCount += spare.getQuantity();
                }
            }
        }
        assertEquals(1, brandNewCount, "The bought autocannon stays brand new");
        assertEquals(1, usedCount, "The autocannon taken out of the Wolverine is a used one");
    }

    @Test
    void anEmptyAmmoBinTakenOutByARefitIsNotKept() throws Exception {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);

        refitIntoTheSixM(wolverine);

        int looseAmmoBins = 0;
        for (Part part : campaign.getPlayerForce().getWarehouse().getParts()) {
            boolean isLooseBin = (part instanceof AmmoBin) && (part.getUnit() == null);
            if (isLooseBin && !part.isReservedForRefit()) {
                looseAmmoBins++;
            }
        }
        assertEquals(0, looseAmmoBins);
    }

    @Test
    void armorOfDifferentQualityStaysInSeparateStacks() {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        Armor installedArmor = PartsScenario.unitParts(wolverine, Armor.class).getFirst();
        Armor goodArmor = installedArmor.clone();
        goodArmor.setAmount(16);
        goodArmor.setQuality(PartQuality.QUALITY_F);
        Armor poorArmor = installedArmor.clone();
        poorArmor.setAmount(16);
        poorArmor.setQuality(PartQuality.QUALITY_A);

        campaign.getQuartermaster().addPart(goodArmor, 0, false);
        campaign.getQuartermaster().addPart(poorArmor, 0, false);

        int armorStacks = 0;
        for (Part spare : campaign.getPlayerForce().getWarehouse().getSpareParts()) {
            if ((spare instanceof Armor armor) && armor.isSameType(installedArmor)) {
                armorStacks++;
            }
        }
        assertEquals(2, armorStacks, "Quality F and quality A armor are not one stack");
    }

    @Test
    void aPartOnAUnitCannotBeSold() {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        EquipmentPart installedAutocannon = findAutocannon(wolverine);
        assertNotNull(installedAutocannon);
        Money balanceBefore = campaign.getPlayerForce().getFinances().getBalance();

        campaign.getQuartermaster().sellPart(installedAutocannon, 1);

        assertEquals(balanceBefore, campaign.getPlayerForce().getFinances().getBalance(), "Nothing is paid");
        assertSame(wolverine, installedAutocannon.getUnit(), "The autocannon stays on the Wolverine");
    }

    @Test
    void armorOnAUnitCannotBeSold() {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        Armor installedArmor = PartsScenario.unitParts(wolverine, Armor.class).getFirst();
        int pointsBefore = installedArmor.getAmount();
        Money balanceBefore = campaign.getPlayerForce().getFinances().getBalance();

        campaign.getQuartermaster().sellArmor(installedArmor, pointsBefore);

        assertEquals(balanceBefore, campaign.getPlayerForce().getFinances().getBalance(), "Nothing is paid");
        assertSame(wolverine, installedArmor.getUnit(), "The armor stays on the Wolverine");
        assertEquals(pointsBefore, installedArmor.getAmount());
    }
}
