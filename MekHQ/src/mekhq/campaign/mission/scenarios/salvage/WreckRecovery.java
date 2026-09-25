package mekhq.campaign.mission.scenarios.salvage;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
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
    private @Nullable Unit firstUnit;
    /** The second recovery unit, or {@code null}. */
    private @Nullable Unit secondUnit;
    /** How the player would like a single unit to recover the wreck, or {@code null} for no preference. */
    private @Nullable RecoveryMethod preferredRecoveryMethod;

    // Worked out by SalvageRecoveryPlan
    RecoveryStatus status = RecoveryStatus.UNASSIGNED;
    /** How a single unit recovers the wreck on the ground, or {@code null} if that doesn't apply. */
    @Nullable RecoveryMethod recoveryMethod;
    boolean isRecoveryMethodChoosable;
    /** The unit the recovery method was chosen for, so the choice resets when the unit changes. */
    @Nullable Unit recoveryMethodUnit;
    /** What this wreck takes up in its carrier's shared cargo space or bays, or {@code null} if not shared. */
    @Nullable SalvageRecoveryPlan.CarryLoad carryLoad;

    WreckRecovery(TestUnit wreck) {
        this.wreck = wreck;
    }

    public TestUnit getWreck() {
        return wreck;
    }

    /**
     * Assigns the units that will recover the wreck.
     *
     * @param firstUnit  the first recovery unit, or {@code null}
     * @param secondUnit the second recovery unit, or {@code null}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setRecoveryUnits(@Nullable Unit firstUnit, @Nullable Unit secondUnit) {
        this.firstUnit = firstUnit;
        this.secondUnit = secondUnit;
    }

    /**
     * Sets how the player would like a single unit to recover the wreck, where it could either carry or drag it.
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
     * @return how a single unit recovers the wreck on the ground, or {@code null} if that doesn't apply
     */
    public @Nullable RecoveryMethod getRecoveryMethod() {
        return recoveryMethod;
    }

    /**
     * @return {@code true} if the assigned unit could either carry or drag the wreck, so the player may choose
     */
    public boolean isRecoveryMethodChoosable() {
        return isRecoveryMethodChoosable;
    }
}
