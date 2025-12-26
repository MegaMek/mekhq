package mekhq.campaign;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.events.persons.PersonEvent;
import mekhq.campaign.personnel.Person;

/**
 * For processing events that should trigger for any kind of campaign, AtB or otherwise.
 * @param campaign
 */
public record CampaignEventProcessor(Campaign campaign) {

    public CampaignEventProcessor(Campaign campaign) {
        this.campaign = campaign;
        MekHQ.registerHandler(this);
    }

    /**
     * Handles updates to personnel records.
     *
     * <p>Clears cached values</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param personEvent the event containing updates related to a person in the campaign
     */
    @Subscribe
    public void handlePersonUpdate(PersonEvent personEvent) {
        campaign().invalidateActivePersonnelCache();
        Person person = personEvent.getPerson();
        person.invalidateAdvancedAsTechContribution();
    }
}
