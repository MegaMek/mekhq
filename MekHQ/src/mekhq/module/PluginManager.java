/**
 * 
 */
package mekhq.module;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.logging.LogLevel;
import mekhq.MekHQ;

/**
 * Tracks which plugins are installed and which are active. Provides class loader for use by ServiceLoader.
 * 
 * @author Neoancient
 *
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
        MekHQ.getLogger().log(getClass(), "<init>(File)",
                LogLevel.DEBUG, "Initializing plugin manager.");

        scriptFiles = new ArrayList<>();
        File dir = new File(PLUGIN_DIR);
        if (!dir.exists()) {
            MekHQ.getLogger().log(getClass(), "<init>()", LogLevel.WARNING, //$NON-NLS-1$
                    "Could not find plugin directory"); //$NON-NLS-1$
        }
        URL[] urls = new URL[0];
        if (dir.exists() && dir.isDirectory()) {
            List<URL> plugins = getPluginsFromDir(dir);
            urls = plugins.toArray(urls);
        } else {
            MekHQ.getLogger().log(getClass(), "<init>", LogLevel.WARNING, //$NON-NLS-1$
                    "Could not find plugin directory."); //$NON-NLS-1$
        }
        MekHQ.getLogger().log(getClass(), "<init>(File)",
                LogLevel.DEBUG, "Found " + urls.length + " plugins");
        classLoader = new URLClassLoader(urls);
    }
    
    /**
     * Recursively checks the plugin directory for jar files and adds them to the list.
     * @param dir  The directory to check
     * @return     A list of all jar files in the directory and subdirectories
     */
    private List<URL> getPluginsFromDir(File dir) {
        MekHQ.getLogger().log(getClass(), "getPluginsFromDir(File)",
                LogLevel.DEBUG, "Now checking directory " + dir.getName());
        final List<URL> retVal = new ArrayList<>();
        for (File f : dir.listFiles()) {
            if (f.getName().startsWith(".")) {
                continue;
            }
            if (f.isDirectory()) {
                retVal.addAll(getPluginsFromDir(f));
            }
            if (f.getName().toLowerCase().endsWith(".jar")) {
                MekHQ.getLogger().log(getClass(), "getPluginsFromDir(File)",
                        LogLevel.DEBUG, "Now adding plugin " + f.getName() + " to class loader.");
                try {
                    retVal.add(f.toURI().toURL());
                } catch (MalformedURLException e) {
                    // Should not happen
                }
            } else {
                scriptFiles.add(f);
            }
        }
        return retVal;
    }
    
    public List<File> getScripts() {
        return Collections.unmodifiableList(scriptFiles);
    }
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }

}
