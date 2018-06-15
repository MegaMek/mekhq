/*
 * Copyright (c) 2018  - The MegaMek Team
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
package mekhq.module;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.module.api.MekHQModule;

/**
 * Common functionality for MekHQ module service managers.
 * 
 * @author Neoancient
 *
 */
abstract public class AbstractServiceManager<T extends MekHQModule> {

    private final ServiceLoader<T> loader;
    private final Map<String, T> methods;
    
    protected AbstractServiceManager(Class<T> clazz) {
        loader = ServiceLoader.load(clazz);
        methods = new HashMap<>();
        loadMethods();
    }
    
    abstract protected Class<?> getServiceClass();
    
    private void loadMethods() {
        try {
            for (Iterator<T> iter = loader.iterator(); iter.hasNext(); ) {
                final T method = iter.next();
                methods.put(method.getModuleName(), method);
            }
        } catch (ServiceConfigurationError err) {
            MekHQ.getLogger().log(getClass(), "loadMethods()", err); //$NON-NLS-1$
        }
    }
    
    /**
     * Retrieves a specific instance of the service
     * @param key The name of the method, returned by the service's getMethodName method.
     * @return    The service associated with the key, or null if there is no such service.
     */
    public @Nullable T getMethod(String key) {
        return methods.get(key);
    }
    
    /**
     * @return An unmodifiable 
     */
    public Collection<T> getAllMethods() {
        return Collections.unmodifiableCollection(methods.values());
    }
}
