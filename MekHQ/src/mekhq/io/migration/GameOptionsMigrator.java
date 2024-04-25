/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.io.migration;

import megamek.Version;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;

public class GameOptionsMigrator {
    public static void migrate(final Version version, final GameOptions gameOptions) {
        // commented out as an example of how to do
        //if (version.isLowerThan("0.49.11")) {
        //    final IOption turnTimerOption = gameOptions.getOption(OptionsConstants.BASE_TURN_TIMER);
        //    turnTimerOption.setValue(turnTimerOption.intValue() * 60);
        //}
    }
}
