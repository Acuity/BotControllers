package com.acuity.botcontrol.clients.dreambot;

import com.acuity.common.security.PasswordStore;
import com.acuity.control.client.AbstractBotController;
import com.acuity.control.client.machine.MachineUtil;
import com.acuity.control.client.scripts.ScriptConditionEvaluator;
import com.acuity.control.client.scripts.ScriptInstance;
import com.acuity.control.client.scripts.Scripts;

import com.acuity.control.client.util.ProxyUtil;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.proxy.Proxy;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccountState;
import com.acuity.db.domain.vertex.impl.scripts.*;

import org.dreambot.api.methods.input.Keyboard;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.script.AbstractScript;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 8/14/2017.
 */
public class DreambotController extends AbstractBotController {

    private DreambotControlScript controlScript;

    private ScriptControlProfile scriptControlProfile;
    private ScriptRunConfig scriptRunConfig;
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
        updateProxy(botClientConfig);
        updateBreakProfile(botClientConfig);
        updateControlProfile(botClientConfig);
        updateScript(botClientConfig);
    }

    private void updateControlProfile(BotClientConfig config){
        ScriptControlProfile scriptControlProfile = config.getScriptControlProfile().orElse(null);
        if (scriptControlProfile == null) this.scriptControlProfile = null;
        else if (this.scriptControlProfile == null || this.scriptControlProfile.hashCode() != scriptControlProfile.hashCode()){
            this.scriptControlProfile = scriptControlProfile;
        }
    }

    private void updateBreakProfile(BotClientConfig botClientConfig) {
        controlScript.getBreakHandler().setProfile(botClientConfig.getBreakProfile().orElse(null));
    }

    private void updateProxy(BotClientConfig botClientConfig){
        if (!Objects.equals(botClientConfig.getProxy().orElse(null), proxy)){
            proxy = botClientConfig.getProxy().orElse(null);
            ProxyUtil.setSocksProxy(proxy, this);
            try {
                controlScript.getClient().getSocketWrapper().getSocket().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateScript(BotClientConfig botClientConfig){
        ScriptRunConfig scriptRunConfig = botClientConfig.getScriptRunConfig().orElse(null);
        if (scriptRunConfig != null && (this.scriptRunConfig == null || this.scriptRunConfig.hashCode() != scriptRunConfig.hashCode())){
            Script script = scriptRunConfig.getScript();

            String s = Optional.ofNullable(this.scriptRunConfig).map(ScriptRunConfig::getScriptID).orElse(null);

            if (script == null || !script.getID().equals(s)){
                String[] args = scriptRunConfig.getQuickStartArgs() == null ? new String[0] : scriptRunConfig.getQuickStartArgs().toArray(new String[scriptRunConfig.getQuickStartArgs().size()]);
                if (scriptRunConfig.getScriptVersion().getType() == ScriptVersion.Type.ACUITY_REPO){
                    try {
                        ScriptInstance scriptInstance = Scripts.loadScript(scriptRunConfig);
                        if (scriptInstance == null) return;
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
                                abstractScript.onStart(args);
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
                    Map<String, Class<? extends AbstractScript>> repoScripts = DreambotControlScript.getRepoScripts();
                    Class<? extends AbstractScript> aClass = repoScripts.get(scriptRunConfig.getScript().getTitle());
                    if (aClass != null){
                        try {
                            AbstractScript abstractScript = aClass.newInstance();
                            abstractScript.registerMethodContext(controlScript.getClient());
                            abstractScript.registerContext(controlScript.getClient());
                            abstractScript.onStart(args);
                            controlScript.setDreambotScript(abstractScript);
                        } catch (InstantiationException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        else {
            this.scriptRunConfig = null;
            AbstractScript abstractScript = controlScript.getDreambotScript();
            controlScript.setDreambotScript(null);
            if (abstractScript != null) abstractScript.onExit();
        }
    }


    public void onLoop(){
        sendAccountUpdate();
        sendClientUpdate();

        if (scriptControlProfile != null){
            for (Map.Entry<ScriptRunCondition, ScriptRunConfig> entry : scriptControlProfile.getConditionalScriptMap().entrySet()) {
                if (entry.getKey().getConditions().stream().allMatch(ScriptConditionEvaluator::evaluate)){
                    ScriptRunConfig value = entry.getValue();
                    if (this.scriptRunConfig == null || this.scriptRunConfig.hashCode() != value.hashCode()){
                        getBotControl().requestScript(value);
                    }
                    break;
                }
            }
        }

    }

    private long lastClientStateSend = 0;
    private void sendClientUpdate(){
        long timeMillis = System.currentTimeMillis();
        if (timeMillis - lastClientStateSend > TimeUnit.SECONDS.toMillis(5)){
            BotClientState state = new BotClientState();
            state.setGameState(controlScript.getClient().getGameStateID());
            state.setCpuUsage(MachineUtil.getCPUUsage());
            state.setCaptureTimestamp(LocalDateTime.now());
            send(new MessagePackage(MessagePackage.Type.CLIENT_STATE_UPDATE, MessagePackage.SERVER).setBody(state));
            lastClientStateSend = timeMillis;
        }
    }

    private long lastRSAccountStateSend = 0;
    private void sendAccountUpdate(){
        long timeMillis = System.currentTimeMillis();
        if (timeMillis - lastRSAccountStateSend > TimeUnit.SECONDS.toMillis(5)){
            RSAccountState rsAccountState = new RSAccountState();
            rsAccountState.setSkillExperience(new HashMap<>());
            if (controlScript.getClient().isLoggedIn() && account != null){
                for (Skill skill : Skill.values()) {
                    rsAccountState.getSkillExperience().put(skill.getName(), controlScript.getSkills().getExperience(skill));
                }
                rsAccountState.setHpPercent(controlScript.getCombat().getHealthPercent());
                rsAccountState.setPrayerPoints(controlScript.getSkills().getBoostedLevels(Skill.PRAYER));
                rsAccountState.setRunEnergy(controlScript.getWalking().getRunEnergy());

                Tile tile = controlScript.getLocalPlayer().getTile();
                if (tile != null) rsAccountState.setTile(tile.getX() + "," + tile.getY() + "," + tile.getZ());

                send(new MessagePackage(MessagePackage.Type.ACCOUNT_STATE_UPDATE, MessagePackage.SERVER).setBody(rsAccountState));
            }
            lastRSAccountStateSend = timeMillis;
        }
    }


    @Override
    public void handleMessage(MessagePackage messagePackage) {
        Object result = messagePackage.getBodyAs(Object.class);
        if (result != null){
            if ("kill-bot".equals(result)){
                System.exit(0);
            }
        }
    }

    public RSAccount getAccount() {
        return account;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public static void main(String[] args) {
        DreambotController dreambotController = new DreambotController(new DreambotControlScript());
        try {
            dreambotController.start(PasswordStore.getAcuityEmail(), PasswordStore.getAcuityPassword());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
