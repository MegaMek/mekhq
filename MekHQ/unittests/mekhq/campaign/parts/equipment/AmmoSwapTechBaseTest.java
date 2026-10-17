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
package mekhq.campaign.parts.equipment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.TechConstants;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import mekhq.Utilities;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;
import testUtilities.parts.PartsScenario;

/**
 * A bin can be switched to any munition with the tech base of the weapon it feeds (issue #6050). The Doloire DLR-OC is
 * an Inner Sphere chassis with a Clan LRM-20 and Artemis V. Fielded by an opposing force, it could carry Inner Sphere
 * Artemis-capable ammunition, and once salvaged its bin could not be switched to the Clan Artemis V round it needs,
 * because the choices followed the tech base of the ammunition in the bin. The Doloire is a pinned copy of
 * {@code meks/3145/Republic/Doloire DLR-OC.mtf} from mm-data.
 */
class AmmoSwapTechBaseTest {
    private final PartsScenario scenario = PartsScenario.create();

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    private AmmoBin longRangeMissileBin(Unit doloire) {
        for (AmmoBin ammoBin : PartsScenario.unitParts(doloire, AmmoBin.class)) {
            if (ammoBin.getType().getAmmoType() == AmmoType.AmmoTypeEnum.LRM) {
                return ammoBin;
            }
        }
        throw new IllegalStateException("The Doloire DLR-OC has no LRM bin");
    }

    /** The Inner Sphere Artemis-capable LRM-20 round an opposing force could load. */
    private static AmmoType innerSphereArtemisRound(Entity entity) {
        for (AmmoType ammoType : AmmoType.getMunitionsFor(AmmoType.AmmoTypeEnum.LRM)) {
            boolean isInnerSphere = !TechConstants.isClan(ammoType.getTechLevel(entity.getTechLevelYear()));
            boolean isArtemisCapable = ammoType.getMunitionType().contains(Munitions.M_ARTEMIS_CAPABLE);
            if ((ammoType.getRackSize() == 20) && isInnerSphere && isArtemisCapable) {
                return ammoType;
            }
        }
        throw new IllegalStateException("No Inner Sphere Artemis-capable LRM-20 round");
    }

    private Unit doloire() {
        Entity doloire = MHQTestUtilities.getEntityForUnitTesting("Doloire DLR-OC", false);
        assertNotNull(doloire);
        return scenario.getCampaign().addNewUnit(doloire, false, 0, PartQuality.QUALITY_D);
    }

    @Test
    void theBinFeedsAClanLauncher() {
        Unit doloire = doloire();
        EquipmentType launcher = longRangeMissileBin(doloire).getLauncherType();

        assertNotNull(launcher);
        assertTrue(TechConstants.isClan(launcher.getTechLevel(doloire.getEntity().getTechLevelYear())),
              launcher.getName());
    }

    @Test
    void aBinHoldingInnerSphereAmmoCanSwitchToArtemisV() {
        Unit doloire = doloire();
        AmmoBin ammoBin = longRangeMissileBin(doloire);
        AmmoType wrongRound = innerSphereArtemisRound(doloire.getEntity());

        List<AmmoType> choices = Utilities.getMunitionsFor(doloire.getEntity(), ammoBin.getLauncherType(), wrongRound,
              CampaignOptions.TECH_UNOFFICIAL);

        boolean isOfferingArtemisV = choices.stream()
                                           .anyMatch(choice -> choice.getMunitionType()
                                                                     .contains(Munitions.M_ARTEMIS_V_CAPABLE));
        assertTrue(isOfferingArtemisV, choices.toString());
    }

    @Test
    void withoutALauncherTheChoicesFollowTheAmmunition() {
        Unit doloire = doloire();
        AmmoType wrongRound = innerSphereArtemisRound(doloire.getEntity());

        List<AmmoType> choices = Utilities.getMunitionsFor(doloire.getEntity(), null, wrongRound,
              CampaignOptions.TECH_UNOFFICIAL);

        boolean isOfferingAClanRound = choices.stream()
                                             .anyMatch(choice -> TechConstants.isClan(choice.getTechLevel(
                                                   doloire.getEntity().getTechLevelYear())));
        assertFalse(isOfferingAClanRound, choices.toString());
    }
}
