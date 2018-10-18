/*
 * Copyright (C) 2011-2016 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.joda.time.DateTime;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import megamek.common.EquipmentType;
import megamek.common.logging.LogLevel;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.universe.Planet.PlanetaryEvent;

/** 
 * This will eventually replace the Planets object as our source of system/planetary information
 * using the new XML format that places planets within systems
 * @author Taharqa
 */

public class Systems {
    private final static Object LOADING_LOCK = new Object[0];
    private static Systems systems;
    
    //private static ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.Planets", new EncodeControl()); //$NON-NLS-1$
    
 // Marshaller / unmarshaller instances
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;
    static {
        try {
            JAXBContext context = JAXBContext.newInstance(LocalSystemList.class, Planet.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            unmarshaller = context.createUnmarshaller();
            // For debugging only!
            // unmarshaller.setEventHandler(new javax.xml.bind.helpers.DefaultValidationEventHandler());
        } catch(JAXBException e) {
            MekHQ.getLogger().error(Planets.class, "<init>", e); //$NON-NLS-1$
        }
    }
    
    public static Systems getInstance() {
        if(systems == null) {
            systems = new Systems();
        }
        if(!systems.initialized && !systems.initializing) {
            systems.initializing = true;
            systems.loader = new Thread(new Runnable() {
                @Override
                public void run() {
                    systems.initialize();
                }
            }, "Planet Loader");
            systems.loader.setPriority(Thread.NORM_PRIORITY - 1);
            systems.loader.start();
        }
        return systems;
    }
    
    private ConcurrentMap<String, PlanetarySystem> systemList = new ConcurrentHashMap<>();
    /* organizes systems into a grid of 30lyx30ly squares so we can find
     * nearby systems without iterating through the entire planet list. */
    private HashMap<Integer, Map<Integer, Set<PlanetarySystem>>> systemGrid = new HashMap<>();
    
    // HPG Network cache (to not recalculate all the damn time)
    private Collection<Planets.HPGLink> hpgNetworkCache = null;
    private DateTime hpgNetworkCacheDate = null;
    
    private Thread loader;
    private boolean initialized = false;
    private boolean initializing = false;
    
    private Systems() {}
    
    private Set<PlanetarySystem> getSystemGrid(int x, int y) {
        if( !systemGrid.containsKey(x) ) {
            return null;
        }
        return systemGrid.get(x).get(y);
    }
    
    public List<PlanetarySystem> getNearbySystems(final double centerX, final double centerY, int distance) {
        List<PlanetarySystem> neighbors = new ArrayList<>();
        int gridRadius = (int)Math.ceil(distance / 30.0);
        int gridX = (int)(centerX / 30.0);
        int gridY = (int)(centerY / 30.0);
        for (int x = gridX - gridRadius; x <= gridX + gridRadius; x++) {
            for (int y = gridY - gridRadius; y <= gridY + gridRadius; y++) {
                Set<PlanetarySystem> grid = getSystemGrid(x, y);
                if(null != grid) {
                    for(PlanetarySystem s : grid) {
                        if(s.getDistanceTo(centerX, centerY) <= distance) {
                            neighbors.add(s);
                        }
                    }
                }
            }
        }
        Collections.sort(neighbors, new Comparator<PlanetarySystem>() {
            @Override
            public int compare(PlanetarySystem o1, PlanetarySystem o2) {
                return Double.compare(o1.getDistanceTo(centerX, centerY), o2.getDistanceTo(centerX, centerY));
            }
        });
        return neighbors;
    }
    
    public List<PlanetarySystem> getNearbySystems(final PlanetarySystem system, int distance) {
        return getNearbySystems(system.getX(), system.getY(), distance);
    }
    
    public ConcurrentMap<String, PlanetarySystem> getSystems() {
        return systemList;
    }
    
    public PlanetarySystem getSystemById(String id) {
        return( null != id ? systemList.get(id) : null);
    }
    
// Data loading methods
    
    private void initialize() {
        try {
            generateSystems();
        } catch (ParseException e) {
            MekHQ.getLogger().error(getClass(), "initialize()", e); //$NON-NLS-1$
        }
    }
    
    private void done() {
        initialized = true;
        initializing = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void updateSystems(FileInputStream source) {
        final String METHOD_NAME = "updateSystems(FileInputStream)"; //$NON-NLS-1$
        // JAXB unmarshaller closes the stream it doesn't own. Bad JAXB. BAD.
        try(InputStream is = new FilterInputStream(source) {
                @Override
                public void close() {  }
            }) {
            // Reset the file stream
            source.getChannel().position(0);

            LocalSystemList systems = unmarshaller.unmarshal(
                    MekHqXmlUtil.createSafeXmlSource(is), LocalSystemList.class).getValue();

            // Run through the list again, this time creating and updating systems as we go
            for( PlanetarySystem system : systems.list ) {
                PlanetarySystem oldSystem = systemList.get(system.getId());
                if( null == oldSystem ) {
                    systemList.put(system.getId(), system);
                } else {
                    // Update with new data
                    //TODO: deal with updating
                    //oldSystem.copyDataFrom(system);
                    //system = oldSystem;
                }
            }
            
            // Process system deletions
            for( String systemId : systems.toDelete ) {
                if( null != systemId ) {
                    systemList.remove(systemId);
                }
            }
        } catch (JAXBException e) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME, e);
        } catch(IOException e) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME, e);
        }
    }
    
    private void generateSystems() throws DOMException, ParseException {
        generateSystems("data/universe/planets", "data/universe/systems.xml");
    }
    
    private void generateSystems(String planetsPath, String defaultFilePath) throws DOMException, ParseException {
        final String METHOD_NAME = "generateSystems()"; //NON-NLS-1$
        
        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
                "Starting load of system data from XML..."); //$NON-NLS-1$
        long currentTime = System.currentTimeMillis();
        synchronized (LOADING_LOCK) {
            // Step 1: Initialize variables.
            if( null == systemList ) {
                systemList = new ConcurrentHashMap<>();
            }
            systemList.clear();
            if( null == systemGrid ) {
                systemGrid = new HashMap<>();
            }
            // Be nice to the garbage collector
            for( Map.Entry<Integer, Map<Integer, Set<PlanetarySystem>>> systemGridColumn : systemGrid.entrySet() ) {
                for( Map.Entry<Integer, Set<PlanetarySystem>> systemGridElement : systemGridColumn.getValue().entrySet() ) {
                    if( null != systemGridElement.getValue() ) {
                        systemGridElement.getValue().clear();
                    }
                }
                if( null != systemGridColumn.getValue() ) {
                    systemGridColumn.getValue().clear();
                }
            }
            systemGrid.clear();
            
            // Step 2: Read the default file
            try(FileInputStream fis = new FileInputStream(defaultFilePath)) { //$NON-NLS-1$
                updateSystems(fis);
            } catch (Exception ex) {
                MekHQ.getLogger().error(getClass(), METHOD_NAME, ex);
            }
            
            // Step 3: Load all the xml files within the planets subdirectory, if it exists
            //TODO: Not sure we even want to allow this as an option for systems
            //Utilities.parseXMLFiles(planetsPath, this::updatePlanets);
            
            List<PlanetarySystem> toRemove = new ArrayList<>();
            for (PlanetarySystem system : systemList.values()) {
                if((null == system.getX()) || (null == system.getY())) {
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                            String.format("System \"%s\" is missing coordinates", system.getId())); //$NON-NLS-1$
                    toRemove.add(system);
                    continue;
                }
                int x = (int)(system.getX()/30.0);
                int y = (int)(system.getY()/30.0);
                if (systemGrid.get(x) == null) {
                    systemGrid.put(x, new HashMap<Integer, Set<PlanetarySystem>>());
                }
                if (systemGrid.get(x).get(y) == null) {
                    systemGrid.get(x).put(y, new HashSet<PlanetarySystem>());
                }
                if( !systemGrid.get(x).get(y).contains(system) ) {
                    systemGrid.get(x).get(y).add(system);
                }
            }
            for(PlanetarySystem system : toRemove) {
                systemList.remove(system.getId());
            }
            done();
        }
        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
                String.format(Locale.ROOT,
                        "Loaded a total of %d systems in %.3fs.", //$NON-NLS-1$
                        systemList.size(), (System.currentTimeMillis() - currentTime) / 1000.0));
        // Planetary sanity check time!
        for(PlanetarySystem system : systemList.values()) {
            List<PlanetarySystem> veryCloseSystems = getNearbySystems(system, 1);
            if(veryCloseSystems.size() > 1) {
                for(PlanetarySystem closeSystem : veryCloseSystems) {
                    if(!system.getId().equals(closeSystem.getId())) {
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.WARNING,
                                String.format(Locale.ROOT,
                                        "Extremly close systems detected. Data error? %s <-> %s: %.3f ly", //$NON-NLS-1$
                                        system.getId(), closeSystem.getId(), system.getDistanceTo(closeSystem)));
                    }
                }
            }
        }
    }
    
    @XmlRootElement(name="systems")
    private static final class LocalSystemList {
        @XmlElement(name="system")
        public List<PlanetarySystem> list;
        
        @XmlTransient
        public List<String> toDelete;
        
        @SuppressWarnings("unused")
        private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
            toDelete = new ArrayList<String>();
            if( null == list ) {
                list = new ArrayList<PlanetarySystem>();
            } else {
                // Fill in the "toDelete" list
                List<PlanetarySystem> filteredList = new ArrayList<PlanetarySystem>(list.size());
                for( PlanetarySystem system : list ) {
                    if( null != system.delete && system.delete && null != system.getId() ) {
                        toDelete.add(system.getId());
                    } else {
                        filteredList.add(system);
                    }
                }
                list = filteredList;
            }
        }
    }
}
