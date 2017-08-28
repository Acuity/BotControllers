package com.acuity.botcontrol.clients.dreambot;

import com.acuity.control.client.BotControl;
import org.dreambot.api.methods.MethodContext;
import org.dreambot.api.script.AbstractScript;

/**
 * Created by Zachary Herridge on 8/28/2017.
 */
public abstract class AcuityAbstractScript extends AbstractScript {

    private BotControl botControl;

    public BotControl getBotControl() {
        return botControl;
    }
}
