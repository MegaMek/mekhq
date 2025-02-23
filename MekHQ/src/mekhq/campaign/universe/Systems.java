/*
 * Copyright (c) 2011-2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import megamek.common.EquipmentType;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.Utilities;
import mekhq.campaign.universe.enums.HiringHallLevel;
import mekhq.campaign.universe.enums.HPGRating;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This will eventually replace the Planets object as our source of
 * system/planetary information using the new XML format that places planets
 * within systems
 *
 * @author Taharqa
 */

public class Systems {
    private static final MMLogger logger = MMLogger.create(Systems.class);

    private static Systems systems;

    private final int HPG_RADIUS_A_STATION = 50;
    private final int HPG_RADIUS_B_STATION = 30;

    public static Systems getInstance() {
        if (systems == null) {
            systems = new Systems();
        }

        return systems;
    }

    public static void setInstance(Systems instance) {
        systems = instance;
    }

    private ConcurrentMap<String, PlanetarySystem> systemList = new ConcurrentHashMap<>();
    /**
     * Organizes systems into a grid of 30lyx30ly squares so we can find nearby
     * systems without iterating through the entire planet list.
     */
    private HashMap<Integer, Map<Integer, Set<PlanetarySystem>>> systemGrid = new HashMap<>();

    // HPG Network cache (to not recalculate all the damn time)
    private Collection<Systems.HPGLink> hpgNetworkCache = null;
    private LocalDate hpgNetworkCacheDate = null;

    private Systems() {
    }

    private Set<PlanetarySystem> getSystemGrid(int x, int y) {
        if (!systemGrid.containsKey(x)) {
            return null;
        }

        return systemGrid.get(x).get(y);
    }

    /** Return the planet by given name at a given time point */
    public PlanetarySystem getSystemByName(String name, LocalDate when) {
        if (null == name) {
            return null;
        }

        name = name.toLowerCase(Locale.ROOT);
        for (PlanetarySystem system : systemList.values()) {
            if (null != system) {
                String systemName = system.getName(when);

                if ((null != systemName) && systemName.toLowerCase(Locale.ROOT).equals(name)) {
                    return system;
                }
            }
        }

        return null;
    }

    public List<PlanetarySystem> getNearbySystems(final double centerX, final double centerY, int distance) {
        List<PlanetarySystem> neighbors = new ArrayList<>();

        visitNearbySystems(centerX, centerY, distance, neighbors::add);

        neighbors.sort(Comparator.comparingDouble(o -> o.getDistanceTo(centerX, centerY)));
        return neighbors;
    }

    public List<PlanetarySystem> getNearbySystems(final PlanetarySystem system, int distance) {
        return getNearbySystems(system.getX(), system.getY(), distance);
    }

    public ConcurrentMap<String, PlanetarySystem> getSystems() {
        return systemList;
    }

    public PlanetarySystem getSystemById(String id) {
        return (null != id ? systemList.get(id) : null);
    }

    /**
     * Get a list of planetary systems within a certain jump radius (30ly per jump)
     * that
     * you can shop on, sorted by number of jumps and in system transit time
     *
     * @param system - current <code>PlanetarySystem</code>
     * @param jumps  - number of jumps out to look as an integer
     * @return a list of planets where you can go shopping
     */
    public List<PlanetarySystem> getShoppingSystems(final PlanetarySystem system, int jumps, LocalDate when) {
        List<PlanetarySystem> shoppingSystems = getNearbySystems(system, jumps * 30);

        // remove dead planets
        Iterator<PlanetarySystem> iter = shoppingSystems.iterator();
        while (iter.hasNext()) {
            PlanetarySystem s = iter.next();
            if (null == s.getPrimaryPlanet() || s.getPrimaryPlanet().isEmpty(when)) {
                iter.remove();
            }
        }

        shoppingSystems.sort((p1, p2) -> {
            // sort first on number of jumps required
            int jump1 = (int) Math.ceil(p1.getDistanceTo(system) / 30.0);
            int jump2 = (int) Math.ceil(p2.getDistanceTo(system) / 30.0);
            int sComp = Integer.compare(jump1, jump2);

            if (sComp != 0) {
                return sComp;
            }

            // if number of jumps the same then sort on in system transit time
            return Double.compare(p1.getTimeToJumpPoint(1.0), p2.getTimeToJumpPoint(1.0));
        });

        return shoppingSystems;
    }

    public List<NewsItem> getPlanetaryNews(LocalDate when) {
        List<NewsItem> news = new ArrayList<>();
        for (PlanetarySystem system : systemList.values()) {
            if (null != system) {
                Planet.PlanetaryEvent event;
                for (Planet p : system.getPlanets()) {
                    event = p.getEvent(when);
                    if ((null != event) && (null != event.message)) {
                        NewsItem item = new NewsItem();
                        item.setHeadline(event.message);
                        item.setDate(event.date);
                        item.setLocation(p.getPrintableName(when));
                        news.add(item);
                    }
                }
            }
        }
        return news;
    }

    public Collection<HPGLink> getHPGNetwork(LocalDate when) {
        if ((null != when) && when.equals(hpgNetworkCacheDate)) {
            return hpgNetworkCache;
        }

        Set<HPGLink> result = new HashSet<>();
        for (PlanetarySystem system : systemList.values()) {
            HPGRating hpg = system.getHPG(when);
            if (hpg != null) {
                int distance = 0;
                if (hpg == HPGRating.A) {
                    distance = HPG_RADIUS_A_STATION;
                }

                if (hpg == HPGRating.B) {
                    distance = HPG_RADIUS_B_STATION;
                }

                Collection<PlanetarySystem> neighbors = getNearbySystems(system, distance);

                if (distance > 0) {
                    for (PlanetarySystem neighbor : neighbors) {
                        hpg = neighbor.getHPG(when);
                        if (null != hpg) {
                            HPGLink link = new HPGLink(system, neighbor, hpg);
                            result.add(link);
                        }
                    }
                }
            }
        }
        hpgNetworkCache = result;
        hpgNetworkCacheDate = when;
        return result;
    }

    // Data loading methods

    /**
     * Loads the default planetary system data. This includes all *.yml files in
     * data/universe/planetary_systems and subfolders. It also loads a player's
     * custom planets in their custom user directory, if it exists.
     *
     * @throws DOMException
     * @throws IOException
     */
    public static Systems loadDefault() throws DOMException, IOException {
        logger.info("Starting load of system data from XML...");
        long currentTime = System.currentTimeMillis();

        Systems systems = new Systems();

        // load default systems
        systems.load(MHQConstants.PLANETARY_SYSTEM_DIRECTORY_PATH);

        // load user directory systems
        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        systems.load(new File(userDir, MHQConstants.PLANETARY_SYSTEM_DIRECTORY_PATH).toString());

        // a bit of post loading clean up
        systems.cleanupSystems();

        // logging
        logger.info(String.format(Locale.ROOT, "Loaded a total of %d systems in %.3fs.",
                systems.systemList.size(), (System.currentTimeMillis() - currentTime) / 1000.0));
        systems.logVeryCloseSystems();

        return systems;
    }

    /**
     * Loads Systems data from files.
     *
     * @param planetsPath     The path to the folder containing planetary XML files.
     *
     * @throws DOMException
     * @throws IOException
     */
    public void load(String planetsPath) throws DOMException, IOException {
        // set up mapper
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        // add custom deserializer for any complex objects that need to be read from Strings, etc.
        SimpleModule module = new SimpleModule();
        module.addDeserializer(SocioIndustrialData.class, new SocioIndustrialData.SocioIndustrialDataDeserializer());
        module.addDeserializer(StarType.class, new StarType.StarTypeDeserializer());
        module.addDeserializer(SourceableValue.class, new SourceableValue.SourceableValueDeserializer());
        mapper.registerModule(module);
        // this will allow the mapper to deserialize LocalDate objects
        mapper.registerModule(new JavaTimeModule());

        // Now we can Load all the yml files in the planetsPath and subdirectories
        parsePlanetarySystemFiles(planetsPath, mapper);
    }

    /**
     * loop through all files in the directory and subdirectories and load any
     * *.yml files found.
     * @param dirName the name of the directory from which to load files
     * @param mapper the Jackson mapper used to load the data from yaml
     */
    private void parsePlanetarySystemFiles(String dirName, ObjectMapper mapper) {
        if ((null == dirName)) {
            throw new NullPointerException();
        }

        File dir = new File(dirName);
        if (dir.isDirectory()) {
            File[] files = dir.listFiles((dir1, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
            if ((null != files) && (files.length > 0)) {
                // Case-insensitive sorting. Yes, even on Windows. Deal with it.
                Arrays.sort(files, Comparator.comparing(File::getPath));
                // Try parsing and updating the main list, one by one
                for (File file : files) {
                    if (file.isFile()) {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            loadPlanetarySystem(fis, mapper);
                        } catch (Exception ex) {
                            // Ignore this file then
                            logger.error(
                                String.format("Exception trying to parse %s - ignoring.", file.getPath()),
                                ex);
                        }
                    }
                }
            }

            File[] zipFiles = dir.listFiles((dir1, name) -> name.toLowerCase(Locale.ROOT).endsWith(".zip"));
            for (File zipFile : zipFiles) {
                try (ZipFile zip = new ZipFile(zipFile.getPath())) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        // Check if entry is a directory
                        if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                            try (InputStream inputStream = zip.getInputStream(entry)) {
                                loadPlanetarySystem(inputStream, mapper);
                            } catch (Exception ex) {
                                // Ignore this file then
                                logger.error(
                                    String.format("Exception trying to parse zip  entry %s - ignoring.", entry.getName()),
                                    ex);
                            }
                        }
                    }
                } catch (Exception ex) {
                    logger.error(
                        String.format("Exception trying to read the zip file %s -ignoring.", zipFile.getName()),
                        ex);
                }
            }

            // Get subdirectories too
            File[] dirs = dir.listFiles();
            if (null != dirs && dirs.length > 0) {
                Arrays.sort(dirs, Comparator.comparing(File::getPath));
                for (File subDirectory : dirs) {
                    if (subDirectory.isDirectory()) {
                        parsePlanetarySystemFiles(subDirectory.getPath(), mapper);
                    }
                }
            }
        }
    }

    private void loadPlanetarySystem(InputStream source, ObjectMapper mapper) throws IOException {

        PlanetarySystem system = mapper.readValue(source, PlanetarySystem.class);
        systemList.put(system.getId(), system);

    }

    private void cleanupSystems() {
        List<PlanetarySystem> toRemove = new ArrayList<>();
        for (PlanetarySystem system : systemList.values()) {
            if ((null == system.getX()) || (null == system.getY())) {
                logger.error(String.format("System \"%s\" is missing coordinates", system.getId()));
                toRemove.add(system);
                continue;
            }

            if(null == system.getStar()) {
                logger.error(String.format("System \"%s\" is missing a star", system.getId()));
                toRemove.add(system);
                continue;
            }

            // make sure the primary slot is not larger than the number of planets
            if (system.getPrimaryPlanetPosition() > system.getPlanets().size()) {
                logger.error(String
                        .format("System \"%s\" has a primary slot greater than the number of planets", system.getId()));
                toRemove.add(system);
                continue;
            }
            int x = (int) (system.getX() / 30.0);
            int y = (int) (system.getY() / 30.0);
            systemGrid.computeIfAbsent(x, k -> new HashMap<>());
            systemGrid.get(x).computeIfAbsent(y, k -> new HashSet<>());
            systemGrid.get(x).get(y).add(system);
        }
        for (PlanetarySystem system : toRemove) {
            systemList.remove(system.getId());
        }
    }

    private void logVeryCloseSystems() {
        // Planetary sanity check time!
        for (PlanetarySystem system : systemList.values()) {
            List<PlanetarySystem> veryCloseSystems = getNearbySystems(system, 1);
            if (veryCloseSystems.size() > 1) {
                for (PlanetarySystem closeSystem : veryCloseSystems) {
                    if (!system.getId().equals(closeSystem.getId())) {
                        logger.warn(String.format(Locale.ROOT,
                                "Extremely close systems detected. Data error? %s <-> %s: %.3f ly",
                                system.getId(), closeSystem.getId(), system.getDistanceTo(closeSystem)));
                    }
                }
            }
        }
    }

    /** A data class representing a HPG link between two planets */
    public static final class HPGLink {
        /**
         * In case of HPG-A to HPG-B networks, <code>primary</code> holds the HPG-A
         * node. Else the order doesn't matter.
         */
        public final PlanetarySystem primary;
        public final PlanetarySystem secondary;
        public final HPGRating rating;

        public HPGLink(PlanetarySystem primary, PlanetarySystem secondary, HPGRating rating) {
            this.primary = primary;
            this.secondary = secondary;
            this.rating = rating;
        }

        @Override
        public int hashCode() {
            return Objects.hash(primary, secondary, rating);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if ((null == obj) || (getClass() != obj.getClass())) {
                return false;
            }
            final HPGLink other = (HPGLink) obj;
            return Objects.equals(primary, other.primary) && Objects.equals(secondary, other.secondary)
                    && (rating == other.rating);
        }
    }

    public void visitNearbySystems(final double centerX, final double centerY, final int distance,
            Consumer<PlanetarySystem> visitor) {
        int gridRadius = (int) Math.ceil(distance / 30.0);
        int gridX = (int) (centerX / 30.0);
        int gridY = (int) (centerY / 30.0);

        for (int x = gridX - gridRadius; x <= gridX + gridRadius; x++) {
            for (int y = gridY - gridRadius; y <= gridY + gridRadius; y++) {
                Set<PlanetarySystem> grid = getSystemGrid(x, y);

                if (null != grid) {
                    for (PlanetarySystem p : grid) {
                        if (p.getDistanceTo(centerX, centerY) <= distance) {
                            visitor.accept(p);
                        }
                    }
                }
            }
        }
    }

    public void visitNearbySystems(final PlanetarySystem system, final int distance,
            Consumer<PlanetarySystem> visitor) {
        visitNearbySystems(system.getX(), system.getY(), distance, visitor);
    }
}
