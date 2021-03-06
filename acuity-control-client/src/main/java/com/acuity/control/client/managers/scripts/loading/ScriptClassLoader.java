package com.acuity.control.client.managers.scripts.loading;

import com.acuity.common.util.AcuityDir;
import com.acuity.control.client.util.Downloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by Zach on 8/12/2017.
 */
public class ScriptClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(ScriptClassLoader.class);

    private static final String CLASS_EXTENSION = ".class";

    private JarFile jarFile;
    private ClassLoader loader;
    private Map<String, Class> loadedClasses = new HashMap<>();

    public void loadJar(File jarLocation) throws IOException {
        if (loadedClasses.size() > 0) {
            logger.info("{} already loaded.", jarLocation);
            return;
        }

        logger.info("Loading {}.", jarLocation);
        if (!jarLocation.exists()) throw new IllegalStateException("Missing script jar file.");

        jarFile = new JarFile(jarLocation);
        loader = URLClassLoader.newInstance(new URL[]{jarLocation.toURI().toURL()});

        final Enumeration entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) entries.nextElement();
            if (jarEntry.isDirectory() || !jarEntry.getName().endsWith(CLASS_EXTENSION)) continue;

            String className = jarEntry.getName().substring(0, jarEntry.getName().length() - CLASS_EXTENSION.length());
            className = className.replace('/', '.');
            try {
                Class clazz = loader.loadClass(className);
                loadedClasses.put(className, clazz);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                logger.trace("Failed to find class", e);
            }
            logger.trace("Loaded class {}.", className);
        }
        logger.debug("Loading {} complete.", jarLocation);
    }

    public Map<String, Class> getLoadedClasses() {
        return loadedClasses;
    }

    public static ScriptJarInstance loadScript(String key, String title, int clientType, int rev, String jarURL) throws IOException {
        File file = Downloader.downloadIfNotPresent(new File(AcuityDir.getScripts(), key + File.separatorChar + clientType + File.separatorChar + "jar" + File.separatorChar), "scriptRev" + rev + ".jar", jarURL, true);
        return new ScriptJarInstance(key, title, file);
    }
}
