package com.acuity.control.client.managers;

import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;

import java.awt.image.BufferedImage;

/**
 * Created by Zachary Herridge on 10/5/2017.
 */
public abstract class ClientManager {

    private BotControl botControl;

    public abstract void sendClientState();

    public abstract Object createInstanceOfScript(ScriptNode scriptRunConfig);

    public abstract void destroyInstanceOfScript(Object scriptInstance);

    public abstract boolean evaluate(Object evaluator);

    public abstract boolean isSignedIn(RSAccount rsAccount);

    public abstract void sendInGameMessage(String messagePackageBodyAs);

    public abstract Integer getCurrentWorld();

    public abstract void hopToWorld(int world);

    public abstract BufferedImage getScreenCapture();

    public abstract boolean executeLoginHandler();

    public boolean isSignedIn() {
        RSAccount rsAccount = botControl.getRsAccountManager().getRsAccount();
        return rsAccount != null && isSignedIn(rsAccount);
    }

    public BotControl getBotControl() {
        return botControl;
    }

    public ClientManager setBotControl(BotControl botControl) {
        this.botControl = botControl;
        return this;
    }
}
