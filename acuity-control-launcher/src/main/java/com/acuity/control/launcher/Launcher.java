package com.acuity.control.launcher;

import com.acuity.common.util.AcuityDir;
import com.acuity.common.util.ControlUtil;
import com.acuity.common.util.DownloadUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Zachary Herridge on 10/13/2017.
 */
public class Launcher {

    @SuppressWarnings("unchecked")
    public static void updateVersion(String globalKey, String propertiesKey, File file) throws IOException {
        HashMap<String, Object> globalInfoDoc = ControlUtil.getGlobalInfoDoc();

        HashMap<String, Object> versionInfo = (HashMap<String, Object>) globalInfoDoc.get(globalKey);

        String globalVersion = String.valueOf(versionInfo.get("version"));
        String propertyVersion = AcuityDir.getProperties().getProperty(propertiesKey, "0.0.0");

        if (!propertyVersion.equalsIgnoreCase(globalVersion)){
            String url = String.valueOf(globalInfoDoc.get("url"));
            DownloadUtil.download(url, file.getPath());
        }
    }

    public static void launch(String file, String count){

    }
}
