package mekhq.campaign;

import java.util.UUID;

import org.joda.time.DateTime;

import mekhq.campaign.universe.PlanetarySystem;

public class RemoteCampaign {

    private final UUID id;
    private final String name;
    private final DateTime date;
    private final PlanetarySystem location;

    public RemoteCampaign(UUID id, String name, DateTime date, PlanetarySystem location) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.location = location;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public DateTime getDate() {
		return date;
    }

    public PlanetarySystem getLocation() {
        return location;
    }
}
