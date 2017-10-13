package com.acuity.botcontrol.clients.dreambot;

import com.acuity.control.client.BotControl;
import com.acuity.control.client.managers.scripts.task.TaskBlocking;
import com.acuity.control.client.network.NetworkedInterface;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import org.dreambot.api.script.AbstractScript;

/**
 * Created by Zachary Herridge on 8/28/2017.
 */
public abstract class AcuityAbstractScript extends AbstractScript implements TaskBlocking, NetworkedInterface {

    private BotControl botControl;

    public BotControl getBotControl() {
        return botControl;
    }

    @Override
    public boolean isAcceptingTasks(){
        return true;
    }

    @Override
    public abstract void onMessagePackage(MessagePackage messagePackage);
}
