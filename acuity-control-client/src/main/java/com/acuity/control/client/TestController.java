package com.acuity.control.client;

import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.scripts.ScriptRunConfig;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class TestController {

    BotControl botControl = new BotControl("localhost", ClientType.DREAMBOT) {
        @Override
        public Object createInstanceOfScript(ScriptRunConfig scriptRunConfig) {
            return new Object();
        }

        @Override
        public void destroyInstanceOfScript(Object scriptInstance) {
        }
    };

    public static void main(String[] args) {
        TestController testController = new TestController();
        while (true){
            testController.botControl.onLoop();
            System.out.println(testController.botControl.getRsAccountManager().getRsAccount());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
