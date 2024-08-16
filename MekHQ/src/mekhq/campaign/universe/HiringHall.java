package mekhq.campaign.universe;

import mekhq.campaign.universe.enums.HiringHallLevel;

import java.time.LocalDate;

public class HiringHall {
    private HiringHallLevel level;
    private LocalDate startDate;
    private LocalDate endDate;
    private String planetName;

    public HiringHall(HiringHallLevel level, LocalDate startDate, LocalDate endDate, String planetName) {
        this.level = level;
        this.startDate = startDate;
        this.endDate = endDate;
        this.planetName = planetName;
    }

    public boolean isActive(LocalDate date) {
        return ((startDate == null) || !startDate.isAfter(date))
            && ((endDate == null) || !endDate.isBefore(date));
    }

    public String getPlanetName() {
        return planetName;
    }

    public HiringHallLevel getLevel() {
        return level;
    }

    public void setLevel(HiringHallLevel level) {
        this.level = level;
    }
}
