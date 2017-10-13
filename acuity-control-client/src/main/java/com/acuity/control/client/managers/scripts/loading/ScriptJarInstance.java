package com.acuity.control.client.managers.scripts.loading;

import java.io.File;
import java.io.IOException;

/**
 * Created by Zach on 8/12/2017.
 */
public class ScriptJarInstance {

    private final String key;
    private final String title;
    private final File jarLocation;
    private ScriptClassLoader scriptClassLoader;

    public ScriptJarInstance(String key, String title, File jarLocation) {
        this.key = key;
        this.title = title;
        this.jarLocation = jarLocation;
        this.scriptClassLoader = new ScriptClassLoader();
    }

    public ScriptClassLoader getScriptClassLoader() {
        return scriptClassLoader;
    }

    public void loadJar() throws IOException {
        scriptClassLoader.loadJar(jarLocation);
    }
}
