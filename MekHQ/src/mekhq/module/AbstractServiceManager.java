/*
 * Copyright (c) 2018 - The MegaMek Team. All Rights Reserved.
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
package mekhq.module;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.module.api.MekHQModule;

/**
 * Common functionality for MekHQ module service managers.
 *
 * @author Neoancient
 */
abstract public class AbstractServiceManager<T extends MekHQModule> {

    private final ServiceLoader<T> loader;
    private final Map<String, T> services;

    protected AbstractServiceManager(Class<T> clazz) {
        loader = ServiceLoader.load(clazz, PluginManager.getInstance().getClassLoader());
        services = new HashMap<>();
        loadServices();
        loadScripts(clazz);
    }

    private void loadServices() {
        try {
            for (Iterator<T> iter = loader.iterator(); iter.hasNext(); ) {
                final T service = iter.next();
                MekHQ.getLogger().debug("Found service " + service.getModuleName());

                services.put(service.getModuleName(), service);
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
    }

    @SuppressWarnings(value = "unchecked")
    private void loadScripts(Class<T> clazz) {
        for (MekHQModule module : ScriptPluginManager.getInstance().getModules()) {
            if (clazz.isInstance(module)) {
                services.put(module.getModuleName(), (T) module);
            }
        }
    }

    /**
     * Retrieves a specific instance of the service
     * @param key The name of the method, returned by the service's getMethodName method.
     * @return    The service associated with the key, or null if there is no such service.
     */
    public @Nullable T getService(String key) {
        return services.get(key);
    }

    /**
     * @return An unmodifiable collection of the services
     */
    public Collection<T> getAllServices() {
        return getAllServices(false);
    }

    /**
     * Retrieve a collection of all available services
     *
     * @param sort Whether to sort the collection by the service name.
     * @return An unmodifiable collection of the services
     */
    public Collection<T> getAllServices(boolean sort) {
        if (sort) {
            return Collections.unmodifiableCollection(services.values().stream()
                    .sorted(Comparator.comparing(MekHQModule::getModuleName)).collect(Collectors.toList()));
        }
        return Collections.unmodifiableCollection(services.values());
    }
}
