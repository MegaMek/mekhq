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

import mekhq.campaign.DetachmentLocationManager;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.LocalPersonnel;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.market.RequestedStockLevels;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;

/**
 * The single-detachment seam for an {@link AbstractForce}.
 *
 * <p>Everything here is convenience that <em>only makes sense when a force has exactly one
 * {@link Detachment}</em> — "the" hangar, "the" warehouse, "the" personnel roster. A force implements this interface
 * only while it is single-detachment; the sole method it must supply is {@link #getForceDetachment()}, and every other
 * accessor is a default passthrough to that detachment.</p>
 *
 * <p><b>This is deliberately a distinct type so the single-detachment assumption is greppable and
 * compiler-checked.</b> When multi-detachment forces arrive they simply will not implement
 * {@code SingleDetachmentForce}, and the compiler will flag every call site that assumed a single detachment. Prefer
 * {@link AbstractForce#getDetachments()} (and the aggregate helpers on {@link AbstractForce}) for any logic that can be
 * made detachment-count-agnostic; reach for these accessors only where a caller genuinely has not been taught about
 * multiple detachments yet.</p>
 */
public interface SingleDetachmentForce {

    /** The single detachment this force tracks. */
    Detachment getForceDetachment();

    default LocalHangar getHangar() {
        return getForceDetachment().getHangar();
    }

    default LocalWarehouse getWarehouse() {
        return getForceDetachment().getWarehouse();
    }

    default void setWarehouse(LocalWarehouse warehouse) {
        getForceDetachment().setWarehouse(warehouse);
    }

    default LocalPersonnel getPersonnel() {
        return getForceDetachment().getPersonnel();
    }

    default RequestedStockLevels getRequestedStockLevels() {
        return getForceDetachment().getRequestedStockLevels();
    }

    default PartInventory getPartInventory(Part part) {
        return getForceDetachment().getPartInventory(part);
    }

    default DetachmentLocationManager getDetachmentLocationManager() {
        return getForceDetachment().getDetachmentLocationManager();
    }
}
