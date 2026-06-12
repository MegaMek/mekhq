package mekhq.campaign.events;

import megamek.common.event.MMEvent;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignNewDayManager;

/**
 * Event fired when something requests that any ongoing Advance Multiple Days process be interrupted after the current
 * day completes.
 *
 * <p>Listeners that wish to halt multi-day advancement should fire this event via {@code MekHQ.triggerEvent(new
 * InterruptAdvanceDayEvent(campaign))}. {@link CampaignNewDayManager} subscribes to this event and sets
 * {@code startDayWithNoInterruptions = false} upon receipt.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class InterruptAdvanceMultipleDaysEvent extends MMEvent {
    private final Campaign campaign;

    public InterruptAdvanceMultipleDaysEvent(Campaign campaign) {
        this.campaign = campaign;
    }

    public Campaign getCampaign() {
        return campaign;
    }
}
