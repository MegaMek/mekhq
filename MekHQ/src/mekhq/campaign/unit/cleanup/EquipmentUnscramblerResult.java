package mekhq.campaign.unit.cleanup;

import java.util.Objects;

import megamek.common.annotations.Nullable;
import mekhq.campaign.unit.Unit;

public class EquipmentUnscramblerResult {
    private final Unit unit;
    
	private boolean succeeded;
    private String message;

    public EquipmentUnscramblerResult(Unit unit) {
        this.unit = Objects.requireNonNull(unit);
    }

    public Unit getUnit() {
        return unit;
    }

    public boolean succeeded() {
        return succeeded;
    }

    public void setSucceeded(boolean success) {
        succeeded = success;
    }

    public @Nullable String getMessage() {
        return message;
    }

    public void setMessage(@Nullable String message) {
        this.message = message;
    }
}
