package mekhq.campaign.universe.WarriorsAlmanac;

import java.util.List;
import java.util.function.Function;

public enum AlmanacTechAdvancementPhase {
    EXTINCT("WarriorsAlmanacDialog.extinct", WarriorsAlmanacData::extinctionDate),
    COMMON("WarriorsAlmanacDialog.common", WarriorsAlmanacData::commonDate),
    PRODUCTION("WarriorsAlmanacDialog.production", WarriorsAlmanacData::productionDate),
    PROTOTYPE("WarriorsAlmanacDialog.prototype", WarriorsAlmanacData::prototypeDate);

    private final String headerKey;
    private final Function<WarriorsAlmanacData, List<String>> names;

    AlmanacTechAdvancementPhase(String headerKey, Function<WarriorsAlmanacData, List<String>> names) {
        this.headerKey = headerKey;
        this.names = names;
    }

    public String getHeaderKey() {
        return headerKey;
    }

    public Function<WarriorsAlmanacData, List<String>> getNames() {
        return names;
    }
}
