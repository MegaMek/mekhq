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
package mekhq.campaign.force;

/**
 * A single typed force-scoped option holding a current {@code value} and the {@code defaultValue} it
 * falls back to.
 *
 * <p>This is the {@link mekhq.campaign.force.ForceOptions} building block. It intentionally mirrors the role that
 * {@code megamek.common.options.Option}/{@code IOption} plays for game options, but is generic and type-safe rather than
 * {@link Object}-based, so callers get a concrete {@code T} without casting.</p>
 *
 * @param <T> the type of value this option holds
 */
public class ForceOption<T> {
    private final String name;
    private T value;
    private final T defaultValue;

    /**
     * Creates a new option whose current value starts equal to {@code defaultValue}.
     *
     * @param name         the option's identifier
     * @param defaultValue the value this option holds until changed, and reverts to on
     *                     {@link #resetToDefault()}
     */
    public ForceOption(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getDefault() {
        return defaultValue;
    }

    public void resetToDefault() {
        this.value = defaultValue;
    }
}
