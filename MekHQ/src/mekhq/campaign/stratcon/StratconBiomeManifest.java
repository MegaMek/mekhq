package mekhq.campaign.stratcon;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class StratconBiomeManifest {

    public static class MapTypeList {
        public List<String> mapTypes = new ArrayList<>();
    }

    public List<StratconBiome> biomes = new ArrayList<>();
    public Map<String, MapTypeList> biomeMapTypes = new HashMap<>();

    public StratconBiomeManifest() {
        // each biome contains a list of terrain types.
        // e.g. SnowField: FlatSnow, BullshitSnow, SnowWithLake, SnowWithTown etc

        StratconBiome frozen = new StratconBiome();
        frozen.allowedTemperatureUpperBound = 267;
        frozen.allowedTerrainTypes = new ArrayList<>();
        frozen.allowedTerrainTypes.add("SnowField");
        frozen.allowedTerrainTypes.add("Glacier");
        frozen.allowedTerrainTypes.add("ArcticDesert");
        biomes.add(frozen);

        StratconBiome cold = new StratconBiome();
        cold.allowedTemperatureLowerBound = 268;
        cold.allowedTemperatureUpperBound = 277;
        cold.allowedTerrainTypes = new ArrayList<>();
        cold.allowedTerrainTypes.add("Tundra");
        cold.allowedTerrainTypes.add("Steppe");
        cold.allowedTerrainTypes.add("EvergreenForest");
        cold.allowedTerrainTypes.add("Swamp");
        cold.allowedTerrainTypes.add("ArcticDesert");
        biomes.add(cold);

        MapTypeList snowFieldMaps = new MapTypeList();
        snowFieldMaps.mapTypes.add("Snowfield");
        snowFieldMaps.mapTypes.add("SnowRiver");
        snowFieldMaps.mapTypes.add("SnowOutpost");
        snowFieldMaps.mapTypes.add("SnowCoast");
        snowFieldMaps.mapTypes.add("SnowHill");
        biomeMapTypes.put("SnowField", snowFieldMaps);

        try {
            JAXBContext context = JAXBContext.newInstance(StratconBiomeManifest.class);
            JAXBElement<StratconBiomeManifest> templateElement = new JAXBElement<>(new QName("StratconBiomeManifest"), StratconBiomeManifest.class, this);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            File file = new File("./data/stratconbiomedefinitions/StratconBiomeManifest.xml");
            m.marshal(templateElement, file);
        } catch (Exception e) {
            // temp code so we swallow it
            int alpha = 1;
        }
    }
}