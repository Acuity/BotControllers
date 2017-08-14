package com.acuity.botcontrol.clients.dreambot;

import com.acuity.control.client.AbstractBotController;
import com.acuity.control.client.scripts.ScriptInstance;
import com.acuity.control.client.scripts.Scripts;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.proxy.Proxy;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccountState;
import com.acuity.db.domain.vertex.impl.scripts.Script;
import com.acuity.security.PasswordStore;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.script.AbstractScript;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 8/14/2017.
 */
public class DreambotController extends AbstractBotController {


    private DreambotControlScript controlScript;

    private Script script;
    private RSAccount account;
    private Proxy proxy;

    public DreambotController(DreambotControlScript controlScript) {
        super("localhost", ClientType.DREAMBOT.getID());
        this.controlScript = controlScript;
    }

    @Override
    public void onGoodLogin() {

    }

    @Override
    public void onBadLogin() {

    }

    @Override
    public void updateAccount(RSAccount rsAccount) {
        account = rsAccount;
    }

    @Override
    public void updateConfig(BotClientConfig botClientConfig) {
        proxy = botClientConfig.getProxy();
        script = botClientConfig.getScript();

        if (script != null){
            try {
                ScriptInstance scriptInstance = Scripts.loadScript(script);
                scriptInstance.loadJar();
                Class result = scriptInstance.getScriptLoader().getLoadedClasses().values().stream().filter(aClass -> {
                    Class superclass = aClass.getSuperclass();
                    if (superclass != null && superclass.equals(AbstractScript.class)){
                        return true;
                    }
                    return false;
                }).findAny().orElse(null);

                if (result != null){
                    try {
                        AbstractScript abstractScript = (AbstractScript) result.newInstance();
                        abstractScript.registerMethodContext(controlScript.getClient());
                        abstractScript.registerContext(controlScript.getClient());
                        abstractScript.onStart();
                        controlScript.setDreambotScript(abstractScript);
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            AbstractScript abstractScript = controlScript.getDreambotScript();
            controlScript.setDreambotScript(null);
            if (abstractScript != null) abstractScript.onExit();
        }
    }

    public void onLoop(){
        sendAccountUpdate();
    }

    private long lastSend = 0;
    private void sendAccountUpdate(){
        long timeMillis = System.currentTimeMillis();
        if (timeMillis - lastSend > TimeUnit.SECONDS.toMillis(5)){
            RSAccountState rsAccountState = new RSAccountState();

            if (controlScript.getClient().isLoggedIn() && account != null){
                for (Skill skill : Skill.values()) {
                    rsAccountState.getSkillExperience().put(skill.getName(), controlScript.getSkills().getExperience(skill));
                }
                rsAccountState.setHpPercent(controlScript.getCombat().getHealthPercent());
                rsAccountState.setPrayerPoints(controlScript.getSkills().getBoostedLevels(Skill.PRAYER));
                rsAccountState.setRunEnergy(controlScript.getWalking().getRunEnergy());

                send(new MessagePackage(MessagePackage.Type.ACCOUNT_STATE_UPDATE, MessagePackage.SERVER).setBody(rsAccountState));
            }

            lastSend = timeMillis;
        }
    }


    @Override
    public void handleMessage(MessagePackage messagePackage) {
        Object result = messagePackage.getBodyAsType();
        if (result != null){
            if ("kill-bot".equals(result)){
                System.exit(0);
            }
        }
    }

    public Script getScript() {
        return script;
    }

    public RSAccount getAccount() {
        return account;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public static void main(String[] args) {
        DreambotController dreambotController = new DreambotController(null);
        try {
            dreambotController.start(PasswordStore.getAcuityEmail(), PasswordStore.getAcuityPassword());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
