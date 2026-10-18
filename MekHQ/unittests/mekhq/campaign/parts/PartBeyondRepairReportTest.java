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

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.protomeks.ProtoMekLocation;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A failed repair can leave a part beyond repair, and no tech can work on it again. The report only said the repair
 * failed, so the player never learned the part had to be scrapped and replaced (issue #9579). Parts that can never be
 * scrapped, such as a Mek's centre torso, a ProtoMek's torso or a vehicle location, are reported as unreplaceable.
 */
class PartBeyondRepairReportTest {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    private static MekLocation locationOf(Unit unit, int location) {
        return PartsScenario.unitParts(unit, MekLocation.class).stream()
                     .filter(part -> part.getLoc() == location)
                     .findFirst()
                     .orElseThrow();
    }

    private static TankLocation vehicleLocationOf(Unit unit, int location) {
        return PartsScenario.unitParts(unit, TankLocation.class).stream()
                     .filter(part -> part.getLocation() == location)
                     .findFirst()
                     .orElseThrow();
    }

    private static ProtoMekLocation protoMekLocationOf(Unit unit, int location) {
        return PartsScenario.unitParts(unit, ProtoMekLocation.class).stream()
                     .filter(part -> part.getLocation() == location)
                     .findFirst()
                     .orElseThrow();
    }

    private static Unit wolverine() {
        return PartsScenario.create().withUnit(UnitFixture.WOLVERINE_WVR_6R);
    }

    @Test
    void aFailureThatLeavesAPartBeyondRepairSaysToReplaceIt() {
        MekLocation leftArm = locationOf(wolverine(), Mek.LOC_LEFT_ARM);

        String report = leftArm.fail(SkillType.EXP_LEGENDARY);

        assertTrue(leftArm.isDamagedBeyondRepair());
        assertTrue(report.contains(getTextAt(RESOURCE_BUNDLE, "Part.fail.beyondRepair")), report);
    }

    @Test
    void aCentreTorsoBeyondRepairIsReportedAsUnreplaceable() {
        MekLocation centreTorso = locationOf(wolverine(), Mek.LOC_CENTER_TORSO);

        String report = centreTorso.fail(SkillType.EXP_LEGENDARY);

        assertTrue(report.contains(getTextAt(RESOURCE_BUNDLE, "Part.fail.beyondRepairNotReplaceable")), report);
    }

    @Test
    void aVehicleLocationBeyondRepairIsReportedAsUnreplaceable() {
        Unit vedette = PartsScenario.create().withUnit(UnitFixture.VEDETTE_MEDIUM_TANK);
        TankLocation front = vehicleLocationOf(vedette, Tank.LOC_FRONT);

        String report = front.fail(SkillType.EXP_LEGENDARY);

        assertTrue(report.contains(getTextAt(RESOURCE_BUNDLE, "Part.fail.beyondRepairNotReplaceable")), report);
    }

    @Test
    void aProtoMekTorsoBeyondRepairIsReportedAsUnreplaceable() {
        Unit minotaur = PartsScenario.create().withUnit(UnitFixture.MINOTAUR_PROTOMEK);
        ProtoMekLocation torso = protoMekLocationOf(minotaur, ProtoMek.LOC_TORSO);

        String report = torso.fail(SkillType.EXP_LEGENDARY);

        assertTrue(report.contains(getTextAt(RESOURCE_BUNDLE, "Part.fail.beyondRepairNotReplaceable")), report);
    }

    @Test
    void aProtoMekArmBeyondRepairSaysToReplaceIt() {
        Unit minotaur = PartsScenario.create().withUnit(UnitFixture.MINOTAUR_PROTOMEK);
        ProtoMekLocation leftArm = protoMekLocationOf(minotaur, ProtoMek.LOC_LEFT_ARM);

        String report = leftArm.fail(SkillType.EXP_LEGENDARY);

        assertTrue(report.contains(getTextAt(RESOURCE_BUNDLE, "Part.fail.beyondRepair")), report);
    }

    @Test
    void aFailureThatCanStillBeRetriedSaysNothingMore() {
        MekLocation leftArm = locationOf(wolverine(), Mek.LOC_LEFT_ARM);

        String report = leftArm.fail(SkillType.EXP_ELITE);

        assertFalse(leftArm.isDamagedBeyondRepair());
        assertFalse(report.contains(getTextAt(RESOURCE_BUNDLE, "Part.fail.beyondRepair")), report);
    }
}
