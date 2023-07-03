package mekhq.campaign.stratcon;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import mekhq.MHQConstants;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;

import javax.xml.transform.Source;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@XmlAccessorType(XmlAccessType.NONE)
public class StratconBiomeManifest {
    public static final String FOG_OF_WAR = "FogOfWar";
    
    public static class MapTypeList {
        public List<String> mapTypes = new ArrayList<>();
    }

    @XmlElement(name="biomes")
    private List<StratconBiome> biomes = new ArrayList<>();
    @XmlElement(name="biomeMapTypes")
    private Map<String, MapTypeList> biomeMapTypes = new HashMap<>();
    @XmlElement(name="biomeImages")
    private Map<String, String> biomeImages = new HashMap<>();
    
    // derived fields, populated at load time
    private Map<String, TreeMap<Integer, StratconBiome>> biomeTempMap = new HashMap<>();
    private Map<String, List<StratconBiome>> biomeCategoryMap = new HashMap<>();

    public TreeMap<Integer, StratconBiome> getTempMap(String category) {
        return biomeTempMap.get(category);
    }
    
    public Map<String, MapTypeList> getBiomeMapTypes() {
        return biomeMapTypes;
    }
    
    /**
     * Get the file path for the hex image corresponding to the given terrain type
     */
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
            // initialize mapping of biome category to temp map
            if (!resultingManifest.biomeTempMap.containsKey(biome.biomeCategory)) {
                resultingManifest.biomeTempMap.put(biome.biomeCategory, new TreeMap<>());
            }
            
            resultingManifest.biomeTempMap.get(biome.biomeCategory).put(biome.allowedTemperatureLowerBound, biome);
            
            // initialize mapping of biome category to list of biomes
            if (!resultingManifest.biomeCategoryMap.containsKey(biome.biomeCategory)) {
                resultingManifest.biomeCategoryMap.put(biome.biomeCategory, new ArrayList<>());
            }
            
            resultingManifest.biomeCategoryMap.get(biome.biomeCategory).add(biome);
        }

        return resultingManifest;
    }
}