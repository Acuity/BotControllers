package com.acuity.control.launcher;

import com.acuity.common.util.AcuityDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

/**
 * Created by Zachary Herridge on 10/13/2017.
 */
public class Bootstrap {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) {
        args = new String[]{"Dreambot", "1"};

        String clientType = args[0];
        String count = args[1];

        logger.info("Launching Acuity with args. '{}', '{}'.", clientType, count);

        String[] quickstart = Arrays.copyOfRange(args, 1, args.length - 1);
        logger.debug("Quickstart args. {}", Arrays.toString(quickstart));

        if (clientType.equalsIgnoreCase("Dreambot")){
            File controllerJar = new File(AcuityDir.getHome(), "dreambotController.jar");
            try {
                Launcher.updateVersion("dreambotController", "dreambotContollerVersion", controllerJar);
                Launcher.launch(controllerJar, Integer.parseInt(count));
            } catch (Throwable e) {
                logger.error("Error during updating dreambot controller.", e);
            }
        }

        while (true){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
