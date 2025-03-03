/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.cleanup;

import java.util.Objects;

import megamek.common.annotations.Nullable;
import mekhq.campaign.unit.Unit;

public class EquipmentUnscramblerResult {
    //region Variable Declarations
    private final Unit unit;
    private boolean succeeded;
    private String message;
    //endregion Variable Declarations

    //region Constructors
    public EquipmentUnscramblerResult(final Unit unit) {
        this.unit = Objects.requireNonNull(unit);
    }
    //endregion Constructors

    //region Getters/Setters
    public Unit getUnit() {
        return unit;
    }

    public boolean succeeded() {
        return succeeded;
    }

    public void setSucceeded(final boolean succeeded) {
        this.succeeded = succeeded;
    }

    public @Nullable String getMessage() {
        return message;
    }

    public void setMessage(final @Nullable String message) {
        this.message = message;
    }
    //endregion Getters/Setters
}
