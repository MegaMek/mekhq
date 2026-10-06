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
package mekhq.campaign.personnel.quartermaster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Shared plumbing for the two kit catalogs, {@link ArmorKitCatalog} and {@link EquipmentKitCatalog}.
 *
 * <p>The two catalogs answer very different questions — armor kits are grouped by who wears them and gated by year,
 * equipment kits by which skill they improve — so most of their logic is deliberately kept apart. What they share is
 * the shape of the choices they feed the campaign-options dropdowns: a "no kit" sentinel first, then the kit internal
 * names. That builder lives here; each subclass keeps its own domain logic (including its own {@code canBeIssuedKit},
 * which the two answer differently).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public abstract class AbstractKitCatalog {
    protected AbstractKitCatalog() {
    }

    /**
     * Builds a default-kit dropdown's choices: the "no kit" sentinel first, then the given kit internal names in the
     * order supplied.
     *
     * @param sentinel the value meaning "issue no kit" (coveralls for armor, the empty string for equipment)
     * @param kitNames the kit internal names to offer, already in display order
     *
     * @return the option values, sentinel first
     */
    protected static List<String> optionKitNames(String sentinel, Collection<String> kitNames) {
        List<String> names = new ArrayList<>();
        names.add(sentinel);
        names.addAll(kitNames);
        return names;
    }
}
