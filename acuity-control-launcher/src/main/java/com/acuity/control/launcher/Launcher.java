package com.acuity.control.launcher;

import com.acuity.common.util.AcuityDir;
import com.acuity.common.util.ControlUtil;
import com.acuity.common.util.DownloadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Zachary Herridge on 10/13/2017.
 */
public class Launcher {

    private static Logger logger = LoggerFactory.getLogger(Launcher.class);

    @SuppressWarnings("unchecked")
    public static void updateVersion(String globalKey, String propertiesKey, File file) throws IOException {
        HashMap<String, Object> globalInfoDoc = ControlUtil.getGlobalInfoDoc();

        HashMap<String, Object> versionInfo = (HashMap<String, Object>) globalInfoDoc.get(globalKey);

        String globalVersion = String.valueOf(versionInfo.get("version"));
        String propertyVersion = AcuityDir.getProperties().getProperty(propertiesKey);

        logger.info("Comparing versions. remote={}, local={}", globalVersion, propertyVersion);

        if (propertyVersion == null || !propertyVersion.equalsIgnoreCase(globalVersion)){
            String url = String.valueOf(globalInfoDoc.get("url"));
            DownloadUtil.download(url, file.getPath());
        }
    }

    public static void launch(File file, String count){

    }
}
