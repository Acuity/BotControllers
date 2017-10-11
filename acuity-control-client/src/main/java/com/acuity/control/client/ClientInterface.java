package com.acuity.control.client;

import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;

import java.awt.image.BufferedImage;

/**
 * Created by Zachary Herridge on 10/5/2017.
 */
public abstract class ClientInterface {

    private BotControl botControl;

    public abstract void updateClientState(BotClientState botClientState);

    public abstract Object createInstanceOfScript(ScriptNode scriptRunConfig);

    public abstract void destroyInstanceOfScript(Object scriptInstance);

    public abstract boolean evaluate(Object evaluator);

    public abstract boolean isSignedIn(RSAccount rsAccount);

    public abstract void sendInGameMessage(String messagePackageBodyAs);

    public abstract Integer getCurrentWorld();

    public abstract void hopToWorld(int world);

    public abstract BufferedImage getScreenCapture();

    public abstract boolean executeLoginHandler();

    public abstract int getGameState();

    public abstract void logout();

    public abstract String getEmail();

    public boolean isSignedIn() {
        return isSignedIn(botControl.getRsAccountManager().getRsAccount());
    }

    public BotControl getBotControl() {
        return botControl;
    }

    public ClientInterface setBotControl(BotControl botControl) {
        this.botControl = botControl;
        return this;
    }

    public boolean isLoggedIn() {
        return getGameState() >= 30;
    }
}
