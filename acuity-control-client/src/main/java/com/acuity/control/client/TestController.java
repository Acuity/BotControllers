package com.acuity.control.client;

import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class TestController {

    BotControl botControl = new BotControl("localhost", ClientType.DREAMBOT) {
        @Override
        public void sendClientState() {
            BotClientState clientState = new BotClientState();
            clientState.setCpuUsage(ThreadLocalRandom.current().nextDouble(1, 100));
            clientState.setGameState(0);
            send(new MessagePackage(MessagePackage.Type.UPDATE_CLIENT_STATE, MessagePackage.SERVER).setBody(clientState));
        }

        @Override
        public Object createInstanceOfScript(ScriptNode scriptRunConfig) {
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
        public Integer getCurrentWorld() {
            return 308;
        }

        @Override
        public void hopToWorld(int world) {

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
        while (true){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
