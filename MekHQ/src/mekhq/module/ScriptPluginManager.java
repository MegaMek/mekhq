/*
 * Copyright (c) 2018, 2020 The MegaMek Team. All rights reserved.
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

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.module.api.MekHQModule;

/**
 * Manages plugins requiring interpretation by a scripting engine. As scripts are encountered while
 * parsing the plugins directory they are converted to instances of the MekHQModule interface for
 * use by the various specific module managers.
 *
 * @author Neoancient
 *
 */
public class ScriptPluginManager {

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
        final String METHOD_NAME = "addModule(File)"; //$NON-NLS-1$

        ScriptEngine engine = scriptEngineManager.getEngineByExtension(extension);
        if (null == engine) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.WARNING,
                    "Could not find script engine for extension " + extension);
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
        } catch (IOException | ScriptException e) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                    "While parsing script " + script.getName());
            MekHQ.getLogger().error(getClass(), METHOD_NAME, e);
        }
    }

    @SuppressWarnings("unused")
    private static void listEngines() {
        ScriptEngineManager mgr = new ScriptEngineManager(PluginManager.getInstance().getClassLoader());
        for (ScriptEngineFactory engine : mgr.getEngineFactories()) {
            System.out.println("Engine: " + engine.getEngineName());
            System.out.println("\tVersion: " + engine.getEngineVersion());
            System.out.println("\tAlias: " + engine.getNames());
            System.out.println("\tLanguage name: " + engine.getLanguageName());
            System.out.println();
        }
    }
}
