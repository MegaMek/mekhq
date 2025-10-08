/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.module;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.module.api.MekHQModule;

/**
 * Common functionality for MekHQ module service managers.
 *
 * @author Neoancient
 */
abstract public class AbstractServiceManager<T extends MekHQModule> {
    private static final MMLogger LOGGER = MMLogger.create(AbstractServiceManager.class);

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
            for (final T service : loader) {
                LOGGER.debug("Found service {}", service.getModuleName());

                services.put(service.getModuleName(), service);
            }
        } catch (Exception e) {
            LOGGER.error("", e);
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
     *
     * @param key The name of the method, returned by the service's getMethodName method.
     *
     * @return The service associated with the key, or null if there is no such service.
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
     *
     * @return An unmodifiable collection of the services
     */
    public Collection<T> getAllServices(boolean sort) {
        if (sort) {
            return services.values()
                         .stream()
                         .sorted(Comparator.comparing(MekHQModule::getModuleName))
                         .collect(Collectors.toUnmodifiableList());
        }
        return Collections.unmodifiableCollection(services.values());
    }
}
