package mekhq.online.events;

import java.util.UUID;

import megamek.common.event.MMEvent;

public class CampaignDisconnectedEvent extends MMEvent {
    private final UUID id;

    public CampaignDisconnectedEvent(UUID id) {
        this.id = id;
    }

    public UUID getCampaignId() {
        return id;
    }
}
