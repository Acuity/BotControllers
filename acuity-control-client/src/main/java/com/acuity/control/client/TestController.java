package com.acuity.control.client;

import com.acuity.control.client.machine.MachineUtil;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClient;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.message_package.data.RemoteScript;
import com.acuity.db.domain.vertex.impl.scripts.*;
import com.acuity.db.domain.vertex.impl.scripts.conditions.EndCondition;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class TestController {

    BotControl botControl = new BotControl("localhost", ClientType.DREAMBOT) {
        @Override
        public void sendClientState() {
            BotClientState clientState = new BotClientState();
            clientState.setCpuUsage(MachineUtil.getCPUUsage());
            clientState.setGameState(0);
            send(new MessagePackage(MessagePackage.Type.CLIENT_STATE_UPDATE, MessagePackage.SERVER).setBody(clientState));
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
            return true;
        }
    };

    private void runTest(){
       /* Optional<ScriptStartupConfig> scriptRunConfig = botControl.requestScriptRunConfig("Script/1857691", "ScriptVersion/2:1:1857691");
        for (BotClient botClient : botControl.requestBotClients()) {
            scriptRunConfig.ifPresent(config -> {
                config.setPullAccountsFromTagID("Tag/3087498");
                ScriptExecutionConfig executionConfig = new ScriptExecutionConfig(new EndCondition(), config);
                RemoteScript.StartRequest request = new RemoteScript.StartRequest(executionConfig, true);
                RemoteScript.StartResponse b = botControl.requestRemoteScriptStart(botClient.getKey(), request);
                System.out.println(b);
            });
        }*/
    }

    public static void main(String[] args) {
        TestController testController = new TestController();

        new Thread(() -> {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    String input = br.readLine();
                    if (input.equals("kill-script")) {
                        testController.botControl.getScriptManager().onScriptEnded(testController.botControl.getScriptManager().getScriptInstance());
                    }
                }
            }
            catch (Throwable e){
                e.printStackTrace();
            }
        }).start();

        while (true){
            testController.botControl.onLoop();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
