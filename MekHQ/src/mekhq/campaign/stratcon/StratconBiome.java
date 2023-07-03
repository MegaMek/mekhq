package mekhq.campaign.stratcon;

import java.util.List;

public class StratconBiome {
    // biomes will be sorted into buckets based on this field
    public String biomeCategory;
    
    // lower bound temperature, in degrees kelvin
    public int allowedTemperatureLowerBound;

    // upper bound temperature, in degrees kelvin
    public int allowedTemperatureUpperBound;

    public List<String> allowedTerrainTypes;
}
