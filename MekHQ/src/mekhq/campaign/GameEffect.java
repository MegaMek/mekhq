/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

import megamek.common.Compute;

/**
 * A game effect consists for two parts: A (human-readable) description of what it does, and a
 * function (consumer) to be called when it should do it. The function expects a pseudo-random integer
 * number generator as its only argument.
 * <p>
 * Effects are transient structures, used to implement effect generation at a different spot than
 * effect application as well as implementing player choice with useful information about what
 * effect the choice will potentially have.
 */
public class GameEffect {
    private static final IntUnaryOperator DEFAULT_RND = Compute::randomInt;

    public final String desc;
    public final Consumer<IntUnaryOperator> action;

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
