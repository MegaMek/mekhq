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

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

/**
 * The player's plan for recovering a single wreck: which units are assigned to it and, where they have the choice,
 * whether it is carried or dragged.
 *
 * <p>The player's choices are set here; whether they work is worked out by {@link SalvageRecoveryPlan#revalidate()},
 * as a wreck may share its carrier with other wrecks.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class WreckRecovery {
    private final TestUnit wreck;
    /** The first recovery unit, or {@code null}. */
    private Unit firstUnit;
    /** The second recovery unit, or {@code null}. */
    private Unit secondUnit;
    /** How the player would like the assigned units to recover the wreck, or {@code null} for no preference. */
    private RecoveryMethod preferredRecoveryMethod;

    // Worked out by SalvageRecoveryPlan
    RecoveryStatus status = RecoveryStatus.UNASSIGNED;
    /** How the assigned units recover the wreck on the ground, or {@code null} if that doesn't apply. */
    RecoveryMethod recoveryMethod;
    /** What this wreck takes up in its carrier's shared cargo space or bays, or {@code null} if not shared. */
    SalvageRecoveryPlan.CarryLoad carryLoad;

    WreckRecovery(TestUnit wreck) {
        this.wreck = wreck;
    }

    public TestUnit getWreck() {
        return wreck;
    }

    /**
     * Assigns the units that will recover the wreck. Changing the units forgets the player's choice of recovery
     * method, so the new units start from whichever method suits them.
     *
     * @param firstUnit  the first recovery unit, or {@code null}
     * @param secondUnit the second recovery unit, or {@code null}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setRecoveryUnits(@Nullable Unit firstUnit, @Nullable Unit secondUnit) {
        if ((firstUnit != this.firstUnit) || (secondUnit != this.secondUnit)) {
            preferredRecoveryMethod = null;
        }
        this.firstUnit = firstUnit;
        this.secondUnit = secondUnit;
    }

    /**
     * Sets how the player would like the assigned units to recover the wreck. The choice is kept even if the units
     * can't recover the wreck that way, in which case the wreck isn't recovered.
     *
     * @param preferredRecoveryMethod the player's choice, or {@code null} for no preference
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setPreferredRecoveryMethod(@Nullable RecoveryMethod preferredRecoveryMethod) {
        this.preferredRecoveryMethod = preferredRecoveryMethod;
    }

    @Nullable RecoveryMethod getPreferredRecoveryMethod() {
        return preferredRecoveryMethod;
    }

    public @Nullable Unit getFirstUnit() {
        return firstUnit;
    }

    public @Nullable Unit getSecondUnit() {
        return secondUnit;
    }

    /**
     * @return the units assigned to the wreck
     */
    public List<Unit> getRecoveryUnits() {
        List<Unit> recoveryUnits = new ArrayList<>();
        if (firstUnit != null) {
            recoveryUnits.add(firstUnit);
        }
        if (secondUnit != null) {
            recoveryUnits.add(secondUnit);
        }
        return recoveryUnits;
    }

    /**
     * @return {@code true} if any units are assigned to the wreck
     */
    public boolean hasRecoveryUnits() {
        return (firstUnit != null) || (secondUnit != null);
    }

    /**
     * @return whether the wreck will be recovered, and if not, why not
     */
    public RecoveryStatus getStatus() {
        return status;
    }

    /**
     * @return {@code true} if the wreck will be recovered
     */
    public boolean isRecovered() {
        return status.isRecovered();
    }

    /**
     * @return how the assigned units recover the wreck on the ground, or {@code null} if that doesn't apply
     */
    public @Nullable RecoveryMethod getRecoveryMethod() {
        return recoveryMethod;
    }
}
