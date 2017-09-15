package com.acuity.control.client;

import com.acuity.control.client.machine.MachineUtil;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClient;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.message_package.data.RemoteScriptTask;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.Script;
import com.acuity.db.domain.vertex.impl.scripts.ScriptExecutionConfig;
import com.acuity.db.domain.vertex.impl.scripts.ScriptStartupConfig;
import com.acuity.db.domain.vertex.impl.scripts.ScriptVersion;
import com.acuity.db.domain.vertex.impl.scripts.conditions.EndCondition;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class TestController {

    BotControl botControl = new BotControl("localhost", ClientType.DREAMBOT) {
        @Override
        public void sendClientState() {
           /* BotClientState clientState = new BotClientState();
            clientState.setCpuUsage(ThreadLocalRandom.current().nextDouble(1, 100));
            clientState.setGameState(0);
            send(new MessagePackage(MessagePackage.Type.CLIENT_STATE_UPDATE, MessagePackage.SERVER).setBody(clientState));*/
        }

        @Override
        public Object createInstanceOfScript(ScriptStartupConfig scriptRunConfig) {
            return new Object();
        }

        @Override
        public void destroyInstanceOfScript(Object scriptInstance) {
        }

        @Override
        public boolean evaluate(Object evaluator) {
            return false;
        }
        @Override
        public boolean isSignedIn(RSAccount rsAccount) {
            return true;
        }

        @Override
        public void sendInGameMessage(String messagePackageBodyAs) {

        }

        @Override
        public BufferedImage getScreenCapture() {
            try {
                return new Robot().createScreenCapture(new Rectangle(800, 800));
            } catch (AWTException e) {
                e.printStackTrace();
            }
            return null;
        }
    };

    public static void main(String[] args) {
        TestController testController = new TestController();


        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testController.botControl.getConnection().sendScreenCapture(0);

      /*  new Thread(() -> {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    String input = br.readLine();
                    if (input.equals("kill-script")) {
                        testController.botControl.getScriptManager().onScriptEnded(testController.botControl.getScriptManager().getScriptInstance().orElse(null));
                    }

                    if (input.equals("task-request")){
                        for (BotClient botClient : testController.botControl.requestBotClients()) {
                            ScriptStartupConfig scriptStartupConfig = new ScriptStartupConfig();
                            scriptStartupConfig.setScriptID("Script/1857691");
                            scriptStartupConfig.setScriptVersionID("ScriptVersion/2:1:1857691");
                            scriptStartupConfig.setPullAccountsFromTagID("Tag/3087498");
                            scriptStartupConfig.setEndTime(LocalDateTime.now().plus(10, ChronoUnit.MINUTES));
                            scriptStartupConfig.setQuickStartArgs(Collections.emptyList());
                            ScriptExecutionConfig executionConfig = new ScriptExecutionConfig(new EndCondition(), scriptStartupConfig);
                            executionConfig.setRemoveOnEnd(true);
                            RemoteScriptTask.StartRequest startRequest = new RemoteScriptTask.StartRequest(executionConfig, true);

                            RemoteScriptTask.StartResponse startResponse = testController.botControl.requestRemoteTaskStart(botClient.getKey(), startRequest);
                            if (startResponse != null){
                                System.out.println(startResponse);
                                break;
                            }
                        }
                    }

                }
            }
            catch (Throwable e){
                e.printStackTrace();
            }
        }).start();*/

        while (true){
            try {
                testController.botControl.onLoop();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
