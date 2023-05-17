package mekhq.campaign.stratcon;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import megamek.common.Compute;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class StratconTerrainPlacer {
    public static void InitializeTrackTerrain(StratconTrackState track) {
        // 1. get the correct biome list according to track temperature
        // 2. pick random biome to be "base terrain"; apply it to all track hexes
        // 3. "stripe" the other biomes:
        //      striping is "starting coordinate" (random track coord),
        //          "length" (1-10), "width" (1-3) and "direction" (1-6)
        //      stripe ends when center goes off board
        StratconBiome biome = StratconBiomeManifest.GetInstance().biomeMap.ceilingEntry(track.getTemperature()).getValue();

        int baseTerrainIndex = Compute.randomInt(biome.allowedTerrainTypes.size());

        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                track.setTerrainTile(new StratconCoords(x, y), biome.allowedTerrainTypes.get(baseTerrainIndex));
            }
        }
    }
}
