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
package mekhq.campaign.mission.scenarios.salvage;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.function.Supplier;

/**
 * The salvage rulesets a campaign can use to resolve post-scenario salvage.
 *
 * <p>Each system is backed by an {@link AbstractSalvage} implementation, which defines how that ruleset behaves.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum SalvageSystem {
    /** The original MekHQ salvage method: salvage is claimed directly in the resolve scenario wizard. */
    LEGACY("LEGACY", LegacySalvage::new),
    /** Salvage as written in Campaign Operations. */
    CAM_OPS_STRICT("CAM_OPS_STRICT", CamOpsStrictSalvage::new),
    /** Campaign Operations salvage with MekHQ revisions. */
    CAM_OPS_REVISED("CAM_OPS_REVISED", CamOpsRevisedSalvage::new),
    /** A simplified version of Campaign Operations salvage. */
    CHAOS_CAMPAIGN("CHAOS_CAMPAIGN", ChaosCampaignSalvage::new),
    /** An expanded version of Campaign Operations salvage. */
    MEKHQ("MEKHQ", MekHQSalvage::new);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.SalvageSystem";

    private final String lookupName;
    private final String label;
    private final String tooltip;
    private final AbstractSalvage salvage;

    SalvageSystem(String lookupName, Supplier<AbstractSalvage> salvageSupplier) {
        this.lookupName = lookupName;
        this.label = getTextAt(RESOURCE_BUNDLE, "SalvageSystem." + lookupName + ".label");
        this.tooltip = getTextAt(RESOURCE_BUNDLE, "SalvageSystem." + lookupName + ".tooltip");
        this.salvage = salvageSupplier.get();
    }

    public String getLookupName() {
        return lookupName;
    }

    public String getLabel() {
        return label;
    }

    public String getTooltip() {
        return tooltip;
    }

    /**
     * Returns the implementation of this salvage system's rules.
     *
     * @return the salvage rules for this system
     *
     * @author Illiani
     * @since 0.51.01
     */
    public AbstractSalvage getSalvage() {
        return salvage;
    }

    /**
     * Finds the salvage system with the given lookup name.
     *
     * @param lookupName the lookup name, as stored in campaign files
     *
     * @return the matching salvage system, or {@link #CAM_OPS_STRICT} if none matches
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static SalvageSystem fromLookupName(String lookupName) {
        for (SalvageSystem salvageSystem : values()) {
            if (salvageSystem.lookupName.equals(lookupName)) {
                return salvageSystem;
            }
        }

        return CAM_OPS_STRICT;
    }

    @Override
    public String toString() {
        return label;
    }
}
