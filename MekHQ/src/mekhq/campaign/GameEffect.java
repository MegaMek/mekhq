/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

import megamek.common.compute.Compute;

/**
 * A game effect consists for two parts: A (human-readable) description of what it does, and a function (consumer) to be
 * called when it should do it. The function expects a pseudo-random integer number generator as its only argument.
 * <p>
 * Effects are transient structures, used to implement effect generation at a different spot than effect application as
 * well as implementing player choice with useful information about what effect the choice will potentially have.
 */
public record GameEffect(String desc, Consumer<IntUnaryOperator> action) {
    private static final IntUnaryOperator DEFAULT_RND = Compute::randomInt;

    /** "No operation" effect (for reporting) */
    public GameEffect(String desc) {
        this(desc, rnd -> {});
    }

    public GameEffect(String desc, Consumer<IntUnaryOperator> action) {
        this.desc = desc;
        this.action = Objects.requireNonNull(action);
    }

    /**
     * Helper method, applying the action with the default randomization source
     */
    public void apply() {
        action.accept(DEFAULT_RND);
    }

    @Override
    public String toString() {
        return desc;
    }
}
