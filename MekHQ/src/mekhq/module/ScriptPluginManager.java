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
import java.io.FileReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import megamek.logging.MMLogger;
import mekhq.module.api.MekHQModule;

/**
 * Manages plugins requiring interpretation by a scripting engine. As scripts are encountered while parsing the plugins
 * directory they are converted to instances of the MekHQModule interface for use by the various specific module
 * managers.
 *
 * @author Neoancient
 */
public class ScriptPluginManager {
    private static final MMLogger logger = MMLogger.create(ScriptPluginManager.class);

    private static ScriptPluginManager instance;

    private final List<MekHQModule> modules;

    public synchronized static ScriptPluginManager getInstance() {
        if (null == instance) {
            instance = new ScriptPluginManager();
        }
        return instance;
    }

    private final ScriptEngineManager scriptEngineManager;

    private ScriptPluginManager() {
        scriptEngineManager = new ScriptEngineManager(PluginManager.getInstance().getClassLoader());
        modules = new CopyOnWriteArrayList<>();
        loadModules();
    }

    public List<MekHQModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    private void loadModules() {
        List<File> scriptFiles = PluginManager.getInstance().getScripts();
        for (File f : scriptFiles) {
            int extStart = f.getName().lastIndexOf('.');
            if ((extStart > 0) && (f.getName().length() > extStart)) {
                addModule(f, f.getName().substring(extStart + 1));
            }
        }
    }

    private void addModule(File script, String extension) {
        ScriptEngine engine = scriptEngineManager.getEngineByExtension(extension);
        if (null == engine) {
            logger.warn("Could not find script engine for extension " + extension);
            return;
        }
        try (Reader fileReader = new FileReader(script)) {
            engine.eval(fileReader);
            Iterable<?> plugins = (Iterable<?>) engine.eval("getPlugins()");
            for (Object p : plugins) {
                if (p instanceof MekHQModule) {
                    modules.add((MekHQModule) p);
                }
            }
        } catch (Exception e) {
            logger.error("While parsing script " + script.getName(), e);
        }
    }

    @SuppressWarnings("unused")
    private static void listEngines() {
        ScriptEngineManager mgr = new ScriptEngineManager(PluginManager.getInstance().getClassLoader());
        for (ScriptEngineFactory engine : mgr.getEngineFactories()) {
            logger.info("Engine: " + engine.getEngineName());
            logger.info("\tVersion: " + engine.getEngineVersion());
            logger.info("\tAlias: " + engine.getNames());
            logger.info("\tLanguage name: " + engine.getLanguageName() + "\n");
        }
    }
}
