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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.logging.MMLogger;

/**
 * Tracks which plugins are installed and which are active. Provides class loader for use by ServiceLoader.
 *
 * @author Neoancient
 */
public class PluginManager {
    private static final MMLogger logger = MMLogger.create(PluginManager.class);

    private static PluginManager instance;
    private static final String PLUGIN_DIR = "./plugins";

    private final ClassLoader classLoader;
    private final List<File> scriptFiles;

    public synchronized static PluginManager getInstance() {
        if (null == instance) {
            instance = new PluginManager();
        }
        return instance;
    }

    private PluginManager() {
        logger.debug("Initializing plugin manager.");

        scriptFiles = new ArrayList<>();
        File dir = new File(PLUGIN_DIR);
        if (!dir.exists()) {
            logger.warn("Could not find plugin directory");
        }
        URL[] urls = new URL[0];
        if (dir.exists() && dir.isDirectory()) {
            List<URL> plugins = getPluginsFromDir(dir);
            urls = plugins.toArray(urls);
        } else {
            logger.warn("Could not find plugin directory.");
        }
        logger.debug("Found " + urls.length + " plugins");
        classLoader = new URLClassLoader(urls);
    }

    /**
     * Recursively checks the plugin directory for jar files and adds them to the list.
     *
     * @param origin The origin file to check
     *
     * @return A list of all jar files in the directory and subdirectories
     */
    private List<URL> getPluginsFromDir(final File origin) {
        if (!origin.isDirectory()) {
            return new ArrayList<>();
        }
        final File[] files = origin.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }

        logger.debug("Now checking directory " + origin.getName());
        final List<URL> plugins = new ArrayList<>();
        for (final File file : files) {
            if (file.getName().startsWith(".")) {
                continue;
            }

            plugins.addAll(getPluginsFromDir(file));

            if (file.getName().toLowerCase().endsWith(".jar")) {
                logger.debug("Now adding plugin " + file.getName() + " to class loader.");
                try {
                    plugins.add(file.toURI().toURL());
                } catch (MalformedURLException ignored) {
                    // Should not happen
                }
            } else {
                scriptFiles.add(file);
            }
        }
        return plugins;
    }

    public List<File> getScripts() {
        return Collections.unmodifiableList(scriptFiles);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
