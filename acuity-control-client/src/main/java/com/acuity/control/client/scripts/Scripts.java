package com.acuity.control.client.scripts;

import com.acuity.common.util.AcuityDir;
import com.acuity.control.client.util.Downloader;
import com.acuity.db.domain.vertex.impl.scripts.ScriptStartupConfig;

import java.io.File;
import java.io.IOException;

/**
 * Created by Zach on 8/12/2017.
 */
public class Scripts {

    public static File downloadScript(String scriptKey, int clientType, int scriptRev, String downloadLink) throws IOException {
        return Downloader.downloadIfNotPresent(new File(AcuityDir.getScripts(), scriptKey + "/" + clientType + "/jar/"), "scriptRev" + scriptRev + ".jar", downloadLink, true);
    }

    public static ScriptInstance loadScript(String key, String title, int clientType, int rev, String jarURL) throws IOException {
        return new ScriptInstance(key, title, downloadScript(key, clientType, rev, jarURL));
    }

    public static ScriptInstance loadScript(ScriptStartupConfig script) throws IOException {
        if (script == null) return null;
        return loadScript(script.getScript().getKey(), script.getScript().getTitle(), script.getScriptVersion().getClientID(), script.getScriptVersion().getRevision(), script.getScriptVersion().getJarURL());
    }
}
