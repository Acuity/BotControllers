package com.acuity.control.client.util;

import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class RemotePrintStream extends PrintStream {

    private BotControl botControl;

    public RemotePrintStream(BotControl botControl, OutputStream out) {
        super(out, true);
        this.botControl = botControl;
    }

    @Override
    public void print(String logMessage) {
        if (botControl.getConnection().isConnected()) botControl.send(new MessagePackage(MessagePackage.Type.LOG, MessagePackage.SERVER).setBody(logMessage));
    }

    public void setBotControl(BotControl botControl) {
        this.botControl = botControl;
    }
}