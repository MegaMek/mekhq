/*
 * Action.java
 *
 * Copyright (c) 2019 Vicente Cartas Espinel <vicente.cartas at outlook.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq;

/**
 * Represents an operation that accepts no parameters and returns no
 * result.
 *
 * Inspired by java.util.function.Consumer and C# System.Action
 */
public interface Action {
    /*
     * Performs this operation.
     */
    void invoke();

    /**
     * Returns a composed {@code Action} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code Action} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default Action andThen(Action after) {
        assert after != null;
        return () -> { invoke(); after.invoke(); };
    }
}
