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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.enums.TechBase;
import megamek.common.equipment.EquipmentType;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * The "allow purchasing of Clan parts" option limits buying only (issue #3575). An Inner Sphere force refitting a
 * Wolverine WVR-6R with a Clan ER Medium Laser can use one it already owns, and is warned when it would have to buy
 * one.
 */
class RefitPurchaseCheckTest {
    private static final String CLAN_ER_MEDIUM_LASER = "CLERMediumLaser";

    private PartsScenario scenario;
    private Campaign campaign;

    @BeforeAll
    static void initializeSkills() {
        SkillType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.ALLOW_CLAN_PURCHASES, false);
        assertFalse(campaign.getPlayerForce().getFaction().isClan(), "The test force is Inner Sphere");
    }

    /** A Wolverine WVR-6R design that adds a Clan ER Medium Laser in the centre torso. */
    private static Entity wolverineWithAClanLaser() {
        Entity design = UnitFixture.WOLVERINE_WVR_6R.loadEntity();
        design.setMixedTech(true);
        try {
            design.addEquipment(EquipmentType.get(CLAN_ER_MEDIUM_LASER), Mek.LOC_CENTER_TORSO);
        } catch (Exception exception) {
            throw new IllegalStateException("The WVR-6R has room for one more laser", exception);
        }
        return design;
    }

    private Refit planTheRefit() {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        return new Refit(wolverine, wolverineWithAClanLaser(), true, false, false);
    }

    /** The same refit as a kit, the way "Acquire and Use Refit Kit" plans it. */
    private Refit planTheRefitKit() {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        return new Refit(wolverine, wolverineWithAClanLaser(), false, false, false);
    }

    private boolean isImpossibleToAcquire(Refit refitKit) {
        return campaign.checkAcquisition(refitKit, null, false).getTargetNumber().getValue() == TargetRoll.IMPOSSIBLE;
    }

    private void stockOneClanLaser() {
        Unit donor = scenario.withUnit(UnitFixture.HOPLITE_C);
        for (EquipmentPart part : PartsScenario.unitParts(donor, EquipmentPart.class)) {
            if (part.getName().startsWith("ER Medium Laser")) {
                scenario.withSpare(part.clone(), 1);
                break;
            }
        }
        campaign.getPlayerForce().getHangar().removeUnit(donor.getId());
    }

    @Test
    void aClanLaserAlreadyOwnedIsFittedAndNothingIsWarnedAbout() throws Exception {
        stockOneClanLaser();
        Refit refit = planTheRefit();

        assertEquals(List.of(), RefitPurchaseCheck.findPartsThatCannotBeBought(campaign, refit));
        refit.begin();
        assertTrue(refit.acquireParts(), "The refit takes the laser from stock");
    }

    @Test
    void theRefitListShowsEverythingInStock() {
        stockOneClanLaser();

        RefitPartsStatus partsStatus = RefitPartsStatus.of(campaign, planTheRefit());

        assertEquals(RefitPartsStatus.State.IN_STOCK, partsStatus.state());
        assertTrue(partsStatus.describe().contains("Nothing needs to be ordered"), partsStatus.describe());
    }

    @Test
    void theRefitListShowsPartsToOrder() {
        campaign.getCampaignOptions().set(CampaignOption.ALLOW_CLAN_PURCHASES, true);

        RefitPartsStatus partsStatus = RefitPartsStatus.of(campaign, planTheRefit());

        assertEquals(RefitPartsStatus.State.TO_ORDER, partsStatus.state());
        assertTrue(partsStatus.describe().contains("ER Medium Laser"), partsStatus.describe());
    }

    @Test
    void theRefitListShowsPartsThatCannotBeBought() {
        RefitPartsStatus partsStatus = RefitPartsStatus.of(campaign, planTheRefit());

        assertEquals(RefitPartsStatus.State.CANNOT_BUY, partsStatus.state());
        assertEquals(List.of("ER Medium Laser"), partsStatus.parts());
        assertTrue(partsStatus.describe().contains("campaign's purchasing options"), partsStatus.describe());
    }

    @Test
    void aClanLaserThatWouldHaveToBeBoughtIsNamedInTheWarning() {
        Refit refit = planTheRefit();

        assertEquals(List.of("ER Medium Laser"), RefitPurchaseCheck.findPartsThatCannotBeBought(campaign, refit));
    }

    @Test
    void noWarningWhenClanPurchasesAreAllowed() {
        campaign.getCampaignOptions().set(CampaignOption.ALLOW_CLAN_PURCHASES, true);
        Refit refit = planTheRefit();

        assertEquals(List.of(), RefitPurchaseCheck.findPartsThatCannotBeBought(campaign, refit));
    }

    /** A kit arrives with every part it needs, so buying one would bring in the Clan laser the force may not buy. */
    @Test
    void aRefitKitHoldingAClanLaserCannotBeBought() {
        assertTrue(isImpossibleToAcquire(planTheRefitKit()));
    }

    @Test
    void aRefitKitHoldingAClanLaserCanBeBoughtWhenClanPurchasesAreAllowed() {
        campaign.getCampaignOptions().set(CampaignOption.ALLOW_CLAN_PURCHASES, true);

        assertFalse(isImpossibleToAcquire(planTheRefitKit()));
    }

    @Test
    void aRefitKitOfInnerSpherePartsCanBeBought() {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        Entity design = UnitFixture.WOLVERINE_WVR_6R.loadEntity();
        try {
            design.addEquipment(EquipmentType.get("ISMediumLaser"), Mek.LOC_CENTER_TORSO);
        } catch (Exception exception) {
            throw new IllegalStateException("The WVR-6R has room for one more laser", exception);
        }

        assertFalse(isImpossibleToAcquire(new Refit(wolverine, design, false, false, false)));
    }

    @Test
    void automaticAcquisitionsIgnoreTheTechBaseOptions() {
        campaign.getCampaignOptions().set(CampaignOption.ACQUISITIONS_TYPE, AcquisitionsType.AUTOMATIC);

        assertTrue(RefitPurchaseCheck.canBuyTechBase(campaign.getCampaignOptions(), TechBase.CLAN));
    }

    @Test
    void refitVariantsMayUseClanEquipmentWhenTheCampaignMayNotBuyIt() {
        EquipmentType clanLaser = EquipmentType.get(CLAN_ER_MEDIUM_LASER);
        RefitTechManager refitTechManager = new RefitTechManager(campaign);

        assertFalse(campaign.isLegal(clanLaser), "The campaign may not buy it in year " + campaign.getGameYear());
        assertTrue(refitTechManager.isLegal(clanLaser), "A refit design may still use it");
    }
}
