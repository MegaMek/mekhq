/**
 * 
 */
package mekhq.campaign.event;

import java.util.Collections;
import java.util.List;

import megamek.common.event.MMEvent;
import mekhq.campaign.personnel.Person;

/**
 * Triggered when new potential recruits are available on the personnel market
 * 
 * @author Neoancient
 *
 */
public class MarketNewPersonnelEvent extends MMEvent {
    
    private final List<Person> newPersonnel;
    
    public MarketNewPersonnelEvent(List<Person> newPersonnel) {
        this.newPersonnel = Collections.unmodifiableList(newPersonnel);
    }
    
    /**
     * @return An unmodifiable list of new recruits available
     */
    public List<Person> getPersonnel() {
        return newPersonnel;
    }

}
