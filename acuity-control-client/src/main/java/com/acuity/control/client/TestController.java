package com.acuity.control.client;

import com.acuity.db.domain.common.ClientType;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class TestController {

    BotControl botControl = new BotControl("localhost", ClientType.DREAMBOT);


    public static void main(String[] args) {
        TestController testController = new TestController();
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
