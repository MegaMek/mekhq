/**
 * 
 */
package mekhq.campaign.event;

import megamek.common.event.MMEvent;
import mekhq.campaign.CurrentLocation;

/**
 * Event for a change of location (planetary system) for the campaign.
 * 
 * @author Neoancient
 *
 */
public class LocationChangedEvent extends MMEvent {
    
    private final CurrentLocation location;
    private final boolean kfJump;
    
    /**
     * An event that is triggered when the campaign location changes to a different planetary system.
     * 
     * @param location The campaign location object.
     * @param kfJump   Whether the jump occurred as a result of moving to the next location in a jump
     *                 path (as opposed to GM set location)
     */
    public LocationChangedEvent(CurrentLocation location, boolean kfJump) {
        this.location = location;
        this.kfJump = kfJump;
    }
    
    /**
     * @return The campaign's location object.
     */
    public CurrentLocation getLocation() {
        return location;
    }
    
    /**
     * @return true if the location change is the result of moving to the next location in a jump
     *         path as part of the campaign new day process (as opposed changing the location using
     *         GM mode).
     */
    public boolean isKFJump() {
        return kfJump;
    }

}
