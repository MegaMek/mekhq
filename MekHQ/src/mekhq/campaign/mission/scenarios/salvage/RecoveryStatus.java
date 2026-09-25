package mekhq.campaign.mission.scenarios.salvage;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import jakarta.annotation.Nullable;

/**
 * Whether the units assigned to a wreck can recover it, and if not, why not.
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum RecoveryStatus {
    /** No recovery units have been assigned. */
    UNASSIGNED(false, null),
    /** Without salvage operations, every wreck is recovered automatically. */
    RECOVERED_AUTOMATICALLY(true, "recovered"),
    /** The assigned units can recover the wreck. */
    RECOVERED(true, "valid"),
    /** A single unit carries the wreck in cargo space it may share with other wrecks. */
    CARRIED_IN_CARGO(true, "valid.cargo"),
    /** A single unit carries the wreck in one of its bays; its other bays may hold other wrecks. */
    CARRIED_IN_BAY(true, "valid.bay"),
    /** The assigned units drag or tug the wreck, which commits them to it alone. */
    COMMITTED(true, "valid.committed"),
    /** A DropShip or larger needs a unit with a working naval tug adaptor. */
    NO_NAVAL_TUG(false, "noTug"),
    /** A fighter in space needs a working fighter or small craft bay; a small craft, a working small craft bay. */
    NO_SUITABLE_BAY_EQUIPMENT(false, "noVesselWithSuitableBayEquipment"),
    /** No assigned unit has enough cargo space for the wreck. */
    NO_CARGO_CAPACITY(false, "noCapacity.cargo"),
    /** The assigned units can't drag the wreck between them. */
    NO_TOW_CAPACITY(false, "noCapacity.tow"),
    /** The carrier's cargo space is already full of other wrecks. */
    CARGO_FULL(false, "cargoFull"),
    /** The carrier has no free bay with working doors. */
    NO_FREE_BAY(false, "noFreeBay"),
    /** An assigned unit is committed to another wreck. */
    UNIT_IN_USE(false, "unitInUse");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.CamOpsSalvage";

    private final boolean isRecovered;
    /** The resource key suffix of the status's label, or {@code null} if there is nothing to show. */
    private final @Nullable String labelKey;

    RecoveryStatus(boolean isRecovered, @Nullable String labelKey) {
        this.isRecovered = isRecovered;
        this.labelKey = labelKey;
    }

    /**
     * @return {@code true} if the wreck will be recovered
     */
    public boolean isRecovered() {
        return isRecovered;
    }

    /**
     * @return {@code true} if units were assigned to the wreck, but they can't recover it
     */
    public boolean isProblem() {
        return !isRecovered && (this != UNASSIGNED);
    }

    /**
     * @return a short description of the status, or an empty string if there is nothing to show
     */
    public String getLabel() {
        return (labelKey == null) ? "" :
                     getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation." + labelKey);
    }
}
