/*
 * Copyright (c) 2018-2020 - The MegaMek Team. All Rights Reserved.
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mekhq.MekHQ;

/**
 * Tracks which plugins are installed and which are active. Provides class loader for use by ServiceLoader.
 *
 * @author Neoancient
 */
public class PluginManager {
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
        MekHQ.getLogger().debug("Initializing plugin manager.");

        scriptFiles = new ArrayList<>();
        File dir = new File(PLUGIN_DIR);
        if (!dir.exists()) {
            MekHQ.getLogger().warning("Could not find plugin directory");
        }
        URL[] urls = new URL[0];
        if (dir.exists() && dir.isDirectory()) {
            List<URL> plugins = getPluginsFromDir(dir);
            urls = plugins.toArray(urls);
        } else {
            MekHQ.getLogger().warning("Could not find plugin directory.");
        }
        MekHQ.getLogger().debug("Found " + urls.length + " plugins");
        classLoader = new URLClassLoader(urls);
    }

    /**
     * Recursively checks the plugin directory for jar files and adds them to the list.
     * @param origin The origin file to check
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

        MekHQ.getLogger().debug("Now checking directory " + origin.getName());
        final List<URL> plugins = new ArrayList<>();
        for (final File file : files) {
            if (file.getName().startsWith(".")) {
                continue;
            }

            plugins.addAll(getPluginsFromDir(file));

            if (file.getName().toLowerCase().endsWith(".jar")) {
                MekHQ.getLogger().debug("Now adding plugin " + file.getName() + " to class loader.");
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
