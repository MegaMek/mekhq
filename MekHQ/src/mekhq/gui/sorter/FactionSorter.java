package mekhq.gui.sorter;

import java.util.Comparator;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;

public class FactionSorter implements Comparator<Faction> {
    private Campaign campaign;

    public FactionSorter(Campaign c) {
        campaign = c;
    }

    @Override
    public int compare(Faction a, Faction b) {
        int year = campaign.getGameYear();

        return String.CASE_INSENSITIVE_ORDER.compare(a.getFullName(year), b.getFullName(year));
    }
}
