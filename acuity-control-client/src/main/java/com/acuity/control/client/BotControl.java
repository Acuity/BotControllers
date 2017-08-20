package com.acuity.control.client;

import com.acuity.control.client.websockets.response.MessageResponse;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.scripts.ScriptRunConfig;

/**
 * Created by Zach on 8/20/2017.
 */
public class BotControl {

    private AbstractBotController controller;


    public BotControl(AbstractBotController abstractBotController) {
        this.controller = abstractBotController;
    }

    public MessageResponse requestScript(ScriptRunConfig runConfig){
        return controller.send(new MessagePackage(MessagePackage.Type.REQUEST_SCRIPT_CHANGE, MessagePackage.SERVER).setBody(runConfig));
    }
}
