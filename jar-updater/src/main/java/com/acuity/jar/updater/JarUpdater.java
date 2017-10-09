package com.acuity.jar.updater;

import com.acuity.control.client.util.MachineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;

/**
 * Created by Zachary Herridge on 10/9/2017.
 */
public class JarUpdater {

    private static final Logger logger = LoggerFactory.getLogger(JarUpdater.class);

    public static void main(String[] args) {
        if (args.length < 3){
            logger.warn("Incorrect argument length.");
            return;
        }

        String downloadURL = args[0];
        String downloadPath = args[1];
        boolean executeOnComplete = Boolean.parseBoolean(args[2]);

        try {
            logger.info("Starting download. {}, {}", downloadURL, downloadPath);
            MachineUtil.download(downloadURL, downloadPath);

            logger.info("Download complete.");

            if (executeOnComplete){
                logger.info("Executing file.");
                Desktop.getDesktop().open(new File(downloadPath));
            }
        } catch (Throwable e) {
            logger.error("Error during download.", e);
        }
    }
}
