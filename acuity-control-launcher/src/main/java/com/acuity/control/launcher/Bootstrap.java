package com.acuity.control.launcher;

import com.acuity.common.util.AcuityDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Zachary Herridge on 10/13/2017.
 */
public class Bootstrap {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) {
        args = new String[]{"Dreambot"};

        String clientType = args[0];

        logger.info("Launching Acuity. '{}'.", clientType);

        String[] quickstart = Arrays.copyOfRange(args, 0, args.length - 1);
        logger.debug("Quickstart args. {}", Arrays.toString(quickstart));

        if (clientType.equalsIgnoreCase("Dreambot")){
            start("dreambotController", "dreambotContollerVersion", new File(AcuityDir.getHome(), "dreambotController.jar"), quickstart);
        }
    }

    private static void start(String globalKey, String propertiesKey, File jar, String[] quickstart){
        try {
            Launcher.updateVersion(globalKey, propertiesKey, jar);
            Launcher.launch(jar, quickstart);
        } catch (IOException e) {
            logger.error("Error during launching controller.", e);
        }
    }
}
