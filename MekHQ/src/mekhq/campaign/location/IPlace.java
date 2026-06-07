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

package mekhq.campaign.location;

import java.util.Collection;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Hangar;
import mekhq.campaign.Warehouse;
import mekhq.campaign.personnel.Person;

/**
 * A sub-interface of {@link ILocation} that marks a node in the {@link LocationNode} tree as a
 * "place" — an anchor that owns campaign resources such as a {@link Hangar}, {@link Warehouse},
 * and personnel roster.
 *
 * <p>
 * {@link ILocation#getHangar()}, {@link ILocation#getWarehouse()}, and
 * {@link ILocation#getPersonnel()} walk <em>up</em> the {@link LocationNode} tree until they reach
 * an {@code IPlace}. {@code IPlace} terminates that traversal by providing its own overrides.
 * Concrete implementations such as {@link mekhq.campaign.Campaign} override each method to return
 * the resource they own directly; implementations that do not own a given resource inherit these
 * defaults and return {@code null}.
 * </p>
 */
public interface IPlace extends ILocation {

    /**
     * Returns the {@link Hangar} owned by this place, or {@code null} if this place does not own
     * one.
     *
     * <p>Overrides {@link ILocation#getHangar()} to stop the upward tree traversal.</p>
     */
    @Override
    @Nullable
    default Hangar getHangar() {
        return null;
    }

    /**
     * Returns the {@link Warehouse} owned by this place, or {@code null} if this place does not
     * own one.
     *
     * <p>Overrides {@link ILocation#getWarehouse()} to stop the upward tree traversal.</p>
     */
    @Override
    @Nullable
    default Warehouse getWarehouse() {
        return null;
    }

    /**
     * Returns the personnel roster owned by this place, or {@code null} if this place does not own
     * one.
     *
     * <p>Overrides {@link ILocation#getPersonnel()} to stop the upward tree traversal.</p>
     */
    @Override
    @Nullable
    default Collection<Person> getPersonnel() {
        return null;
    }
}
