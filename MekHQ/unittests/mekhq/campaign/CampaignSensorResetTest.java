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
 * of The Topps Company Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import megamek.common.equipment.Sensor;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies that readying a unit for a game no longer discards a sensor the player chose.
 *
 * <p>Before this, the campaign handed every unit back its active probe, or radar when it had no probe, every time a
 * unit was refreshed. That undid anything set in the MegaMek lobby and anything saved against the chassis, so a
 * sensor preference could never reach a campaign game.</p>
 */
class CampaignSensorResetTest {

    private static final int MEK_RADAR_INDEX = 0;
    private static final int MEK_SEISMIC_INDEX = 3;

    /** Builds a Mek with the four sensors the unit file parser gives every Mek, starting on radar. */
    private static Mek mekWithStandardSensors() {
        Mek mek = new BipedMek();
        mek.getSensors().add(new Sensor(Sensor.TYPE_MEK_RADAR));
        mek.getSensors().add(new Sensor(Sensor.TYPE_MEK_IR));
        mek.getSensors().add(new Sensor(Sensor.TYPE_MEK_MAG_SCAN));
        mek.getSensors().add(new Sensor(Sensor.TYPE_MEK_SEISMIC));
        mek.setNextSensor(mek.getSensors().firstElement());
        return mek;
    }

    @Test
    @DisplayName("A unit with no chosen sensor is still reset the way it always was")
    void aUnitWithNoChosenSensorIsStillReset() {
        Mek mek = mekWithStandardSensors();
        mek.setNextSensor(mek.getSensors().elementAt(MEK_SEISMIC_INDEX));

        Campaign.resetSensorChoice(mek);

        assertNotNull(mek.getNextSensor());
        assertEquals(Sensor.TYPE_MEK_RADAR, mek.getNextSensor().type(),
              "A unit nobody configured must go back to radar, exactly as it did before this change");
    }

    @Test
    @DisplayName("A sensor the player chose survives being readied for a game")
    void aChosenSensorSurvives() {
        Mek mek = mekWithStandardSensors();
        mek.setNextSensor(mek.getSensors().elementAt(MEK_SEISMIC_INDEX));
        mek.setCustomSensorChoice(true);

        Campaign.resetSensorChoice(mek);

        assertEquals(Sensor.TYPE_MEK_SEISMIC, mek.getNextSensor().type(),
              "The campaign must not discard a sensor chosen in the lobby or saved against the chassis");
    }

    @Test
    @DisplayName("A unit with no sensors is left alone")
    void aUnitWithNoSensorsIsLeftAlone() {
        Mek mek = new BipedMek();

        Campaign.resetSensorChoice(mek);

        assertNull(mek.getNextSensor(), "A unit carrying no sensors has nothing to reset");
    }

    @Test
    @DisplayName("Resetting twice changes nothing further")
    void resettingTwiceChangesNothingFurther() {
        Mek mek = mekWithStandardSensors();

        Campaign.resetSensorChoice(mek);
        Campaign.resetSensorChoice(mek);

        assertEquals(mek.getSensors().elementAt(MEK_RADAR_INDEX).type(), mek.getNextSensor().type(),
              "Readying a unit repeatedly, which the campaign does, must stay stable");
    }
}
