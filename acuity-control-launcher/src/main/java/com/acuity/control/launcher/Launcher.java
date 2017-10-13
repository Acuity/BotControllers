package com.acuity.control.launcher;

import com.acuity.common.util.AcuityDir;
import com.acuity.common.util.ControlUtil;
import com.acuity.common.util.DownloadUtil;
import com.google.gson.internal.LinkedTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 10/13/2017.
 */
public class Launcher {

    private static Logger logger = LoggerFactory.getLogger(Launcher.class);

    @SuppressWarnings("unchecked")
    public static void updateVersion(String globalKey, String propertiesKey, File file) throws IOException {
        HashMap<String, Object> globalInfoDoc = ControlUtil.getGlobalInfoDoc();

        LinkedTreeMap<String, Object> versionInfo = (LinkedTreeMap<String, Object>) globalInfoDoc.get(globalKey);

        String globalVersion = String.valueOf(versionInfo.get("version"));
        String propertyVersion = AcuityDir.getProperties().getProperty(propertiesKey);

        logger.info("Comparing versions. remote={}, local={}", globalVersion, propertyVersion);

        if (propertyVersion == null || !file.exists() || !propertyVersion.equalsIgnoreCase(globalVersion)){
            String url = String.valueOf(versionInfo.get("url"));
            logger.info("Downloading new version. {}", url);
            DownloadUtil.download(url, file.getPath());
            AcuityDir.setProperty(propertiesKey, globalVersion);
        }
    }

    public static void launch(File file, String classpath, String[] quickstart){
        String command = "java -classpath " + file.getPath() + " " + classpath + " " + Arrays.stream(quickstart).collect(Collectors.joining(" "));

        logger.debug("Runtime command. {}", command);

        try {
            Process exec = Runtime.getRuntime().exec(command);
            try (InputStream stream = exec.getInputStream()){
                byte[] buffer = new byte[1024];
                int len;
                while ((len = stream.read(buffer)) != -1) {
                    System.out.write(buffer, 0, len);
                }
            }
        } catch (IOException e) {
            logger.error("Error during executing Runtime command.", e);
        }
    }
}
