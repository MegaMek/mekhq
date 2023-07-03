package mekhq.campaign.stratcon;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import mekhq.MHQConstants;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;

import javax.xml.transform.Source;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class StratconBiomeManifest {
    public static final String FOG_OF_WAR = "FogOfWar";
    
    public static class MapTypeList {
        public List<String> mapTypes = new ArrayList<>();
    }

    public List<StratconBiome> biomes = new ArrayList<>();
    public TreeMap<Integer, StratconBiome> biomeMap = new TreeMap<>();
    public Map<String, MapTypeList> biomeMapTypes = new HashMap<>();

    public Map<String, String> biomeImages = new HashMap<>();

    public String getBiomeImage(String biomeType) {
        if (biomeImages.containsKey(biomeType)) {
            return biomeImages.get(biomeType);
        }

        return null;
    }

    private static StratconBiomeManifest instance;

    /**
     * Gets the singleton biome manifest instance
     */
    public static StratconBiomeManifest getInstance() {
        if (instance == null) {
            instance = load();
        }

        return instance;
    }

    private static StratconBiomeManifest load() {
        StratconBiomeManifest resultingManifest = null;
        File inputFile = new File(MHQConstants.STRATCON_BIOME_MANIFEST_PATH);
        if (!inputFile.exists()) {
            LogManager.getLogger().warn(String.format("Specified file %s does not exist", MHQConstants.STRATCON_BIOME_MANIFEST_PATH));
            return null;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(StratconBiomeManifest.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MHQXMLUtility.createSafeXmlSource(fileStream);
                JAXBElement<StratconBiomeManifest> manifestElement = um.unmarshal(inputSource, StratconBiomeManifest.class);
                resultingManifest = manifestElement.getValue();
            }
        } catch (Exception e) {
            LogManager.getLogger().error("Error Deserializing Facility Manifest", e);
        }

        for (StratconBiome biome : resultingManifest.biomes) {
            resultingManifest.biomeMap.put(biome.allowedTemperatureLowerBound, biome);
        }

        return resultingManifest;
    }
}