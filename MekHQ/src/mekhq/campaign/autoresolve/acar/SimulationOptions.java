/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.autoresolve.acar;

import megamek.common.options.AbstractOptions;
import megamek.common.options.AbstractOptionsInfo;

/**
 * @author Luana Coppio
 */
public class SimulationOptions extends AbstractOptions  {

    public static final SimulationOptions empty = new SimulationOptions(null);

    public SimulationOptions(AbstractOptions abstractOptions) {

    }

    @Override
    protected void initialize() {
        // do nothing
    }

    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        throw new UnsupportedOperationException("Not supported in this class.");
    }
}
