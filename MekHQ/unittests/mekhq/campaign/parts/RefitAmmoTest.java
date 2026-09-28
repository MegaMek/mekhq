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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import megamek.common.equipment.AmmoType;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A refit handles ammunition once: bins the unit keeps hold on to the ammo they carry, and only the bins the refit
 * adds are filled, from stock or from what the refit buys, never for free (issue #10194).
 *
 * <p>The Wolverine WVR-6R carries an AC/5 and an SRM 6; the WVR-6M drops the AC/5 and keeps the SRM 6 and its bin.
 * Refitting the 6R into the 6M keeps a bin, and refitting the 6M into the 6R adds one.</p>
 */
class RefitAmmoTest {
    private static final int SRM_BIN_SHOTS_LEFT = 7;

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

    @Test
    void aKeptBinHoldsOnToItsAmmoAndStockIsUntouched() throws Exception {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        AmmoBin srmBin = ammoBin(wolverine, "SRM 6");
        srmBin.setShotsNeeded(srmBin.getFullShots() - SRM_BIN_SHOTS_LEFT);
        srmBin.updateConditionFromPart();
        campaign.getQuartermaster().addAmmo(srmBin.getType(), 30);

        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6M.loadEntity(), false, false, false);
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());
        refit.succeed();

        assertEquals(SRM_BIN_SHOTS_LEFT, shotsIn(ammoBin(wolverine, "SRM 6")), "The SRM 6 bin keeps its own rounds");
        assertEquals(30, campaign.getQuartermaster().getAmmoAvailable(srmBin.getType()), "Stock is not drawn on");
    }

    @Test
    void aCustomRefitOrdersTheAmmoForANewBinInsteadOfGivingItAway() throws Exception {
        Unit wolverine = beginCustomRefitIntoTheSixR();
        Refit refit = wolverine.getRefit();

        assertEquals(Map.of("AC/5 Ammo", 1), procurementList(), "One ton for the new bin, none for the full SRM bin");
        assertTrue(refit.acquireParts());
        refit.succeed();

        assertEquals(0, shotsIn(ammoBin(wolverine, "AC/5")), "The ammo is still on order, so the new bin is empty");
        assertEquals(ammoBin(wolverine, "SRM 6").getFullShots(), shotsIn(ammoBin(wolverine, "SRM 6")));
    }

    @Test
    void aCustomRefitLoadsANewBinFromStockOnce() throws Exception {
        AmmoType autocannonAmmo = ammoBin(scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R), "AC/5").getType();
        campaign.getQuartermaster().addAmmo(autocannonAmmo, 20);
        Unit wolverine = beginCustomRefitIntoTheSixR();
        Refit refit = wolverine.getRefit();

        assertEquals(Map.of(), procurementList(), "Stock covers the new bin, so nothing is ordered");
        assertTrue(refit.acquireParts());
        refit.succeed();

        assertEquals(20, shotsIn(ammoBin(wolverine, "AC/5")));
        assertEquals(0, campaign.getQuartermaster().getAmmoAvailable(autocannonAmmo), "The 20 rounds were used once");
    }

    @Test
    void aKitRefitFillsANewBinFromTheKit() throws Exception {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6M);
        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6R.loadEntity(), false, false, false);
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());

        refit.succeed();

        AmmoBin autocannonBin = ammoBin(wolverine, "AC/5");
        assertEquals(autocannonBin.getFullShots(), shotsIn(autocannonBin));
        assertEquals(0, campaign.getQuartermaster().getAmmoAvailable(autocannonBin.getType()),
              "The kit's ammo went into the bin and nowhere else");
    }

    /**
     * Starts a custom refit of a Wolverine WVR-6M into the WVR-6R, with the AC/5 it needs already in the warehouse.
     *
     * @return the unit being refitted
     */
    private Unit beginCustomRefitIntoTheSixR() throws Exception {
        Unit donor = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        for (Part part : new ArrayList<>(donor.getParts())) {
            if ("AC/5".equals(part.getName())) {
                scenario.withSpare(part.clone(), 1);
            }
        }
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6M);
        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6R.loadEntity(), true, false, false);
        refit.begin();
        return wolverine;
    }

    private static AmmoBin ammoBin(Unit unit, String weaponName) {
        List<AmmoBin> ammoBins = PartsScenario.unitParts(unit, AmmoBin.class);
        for (AmmoBin ammoBin : ammoBins) {
            if (ammoBin.getName().startsWith(weaponName + " Ammo")) {
                return ammoBin;
            }
        }
        throw new IllegalStateException("No " + weaponName + " ammo bin on " + unit.getName());
    }

    private static int shotsIn(AmmoBin ammoBin) {
        return ammoBin.getFullShots() - ammoBin.getShotsNeeded();
    }

    /** Counts the procurement list by acquisition name, adding up each order's quantity. */
    private SortedMap<String, Integer> procurementList() {
        SortedMap<String, Integer> census = new TreeMap<>();
        for (IAcquisitionWork order : campaign.getPlayerForce().getShoppingList().getShoppingList()) {
            census.merge(order.getAcquisitionName(), order.getQuantity(), Integer::sum);
        }
        return census;
    }
}
