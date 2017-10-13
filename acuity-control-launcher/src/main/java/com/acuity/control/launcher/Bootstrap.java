package com.acuity.control.launcher;

import com.acuity.common.util.AcuityDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by Zachary Herridge on 10/13/2017.
 */
public class Bootstrap {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) {
        String clientType = args[0];
        String count = args[1];

        logger.info("Launching Acuity with args. {}, {}.", clientType, count);

        String[] quickstart = Arrays.copyOfRange(args, 1, args.length - 1);
        logger.debug("Quickstart args. {}", Arrays.toString(quickstart));

        if (clientType.equalsIgnoreCase("Dreambot")){
            try {
                Launcher.updateVersion("dreambotController", "dreambotContollerVersion", new File(AcuityDir.getHome(), "dreambotController"));
            } catch (Throwable e) {
                logger.error("Error during updating dreambot controller.", e);
            }
        }

    }
}
