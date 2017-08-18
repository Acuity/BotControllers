package com.acuity.control.client.scripts;

import com.acuity.control.client.util.Downloader;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.scripts.Script;
import com.acuity.db.domain.vertex.impl.scripts.ScriptVersion;
import com.acuity.util.AcuityDir;

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

    public static ScriptInstance loadScript(Script script, ClientType clientType) throws IOException {
        ScriptVersion scriptVersion = script.getScriptVersions().get(clientType.getID());
        if (scriptVersion == null) return null;
        return loadScript(script.getKey(), script.getTitle(), clientType.getID(), scriptVersion.getRevision(), scriptVersion.getJarURL());
    }
}
