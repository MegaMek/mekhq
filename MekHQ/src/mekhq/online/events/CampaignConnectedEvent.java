package mekhq.online.events;

import java.util.UUID;

import megamek.common.event.MMEvent;

public class CampaignConnectedEvent extends MMEvent {
    private final UUID id;

    public CampaignConnectedEvent(UUID id) {
        this.id = id;
    }

    public UUID getCampaignId() {
        return id;
    }
}
