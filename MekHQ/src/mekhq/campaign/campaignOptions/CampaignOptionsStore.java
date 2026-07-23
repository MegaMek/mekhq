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
package mekhq.campaign.campaignOptions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nonnull;

/**
 * A heterogeneous, type-safe store for campaign option values keyed by {@link CampaignOption}.
 *
 * <p>Because each {@link CampaignOption} key carries its own value type, {@link #get(CampaignOption)} and
 * {@link #set(CampaignOption, Object)} are fully compile-time-checked: the compiler binds {@code T} from the key, so
 * mismatched values fail to compile and reads need no cast or type token at the call site.</p>
 */
final class CampaignOptionsStore {
    private final Map<CampaignOption<?>, Object> values = new HashMap<>();

    /**
     * Creates a store seeded with each managed option's declared default value.
     *
     * @param managed the options this store holds values for
     */
    CampaignOptionsStore(final Collection<CampaignOption<?>> managed) {
        for (final CampaignOption<?> option : managed) {
            values.put(option, option.defaultValue());
        }
    }

    /**
     * @param option the option to read
     *
     * @return the stored value for the given option
     */
    <T> @Nonnull T get(final @Nonnull CampaignOption<T> option) {
        return option.type().cast(values.get(option));
    }

    /**
     * @param option the option to write
     * @param value  the value to store; its type is enforced by the compiler against the key
     */
    <T> void set(final CampaignOption<T> option, @Nonnull T value) {
        values.put(option, value);
    }
}
