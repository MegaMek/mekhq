package mekhq.campaign.stratcon;

import java.util.List;

public class StratconBiome {
    // lower bound temperature, in degrees kelvin
    public int allowedTemperatureLowerBound;

    // upper bound temperature, in degrees kelvin
    public int allowedTemperatureUpperBound;

    public List<String> allowedTerrainTypes;
}
