package com.acuity.botcontrol.clients.dreambot;

import com.acuity.control.client.BotControl;
import com.acuity.control.client.managers.scripts.ScriptLocation;
import com.acuity.control.client.managers.scripts.Scripts;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.scripts.Script;
import com.acuity.db.domain.vertex.impl.scripts.ScriptVersion;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import com.acuity.db.util.ArangoDBUtil;
import org.dreambot.api.Client;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.loader.NetworkLoader;
import org.dreambot.server.net.datatype.ScriptData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Zachary Herridge on 9/25/2017.
 */
public class DreambotScriptManager {

    private static final Logger logger = LoggerFactory.getLogger(DreambotScriptManager.class);

    @SuppressWarnings("unchecked")
    public static Map<String, Class<? extends AbstractScript>> getRepoScripts() {
        Map<String, Class<? extends AbstractScript>> results = new HashMap<>();
        try {
            Method getAllFreeScripts = NetworkLoader.class.getDeclaredMethod("getAllFreeScripts");
            List list = (List) getAllFreeScripts.invoke(null);
            Method getAllPremiumScripts = NetworkLoader.class.getDeclaredMethod("getAllPremiumScripts");
            list.addAll((List) getAllPremiumScripts.invoke(null));

            for (Object testObject : list) {
                try {
                    Field scriptDataField = Arrays.stream(testObject.getClass().getDeclaredFields())
                            .filter(field -> field.getType().equals(ScriptData.class))
                            .findAny().orElse(null);

                    if (scriptDataField != null) {
                        scriptDataField.setAccessible(true);
                        ScriptData scriptData = (ScriptData) scriptDataField.get(testObject);

                        Class<? extends AbstractScript> remoteClass = NetworkLoader.getRemoteClass(scriptData);
                        if (remoteClass != null) results.put(scriptData.name, remoteClass);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    public static AbstractScript initDreambotScript(BotControl botControl, Client client, ScriptNode runConfig) {
        if (runConfig != null) {
            logger.debug("initDreambotScript - initing off ScriptStartupConfig. {}", runConfig);
            ScriptVersion scriptVersion = botControl.requestScriptVersion(runConfig.getScriptID(), runConfig.getScriptVersionID()).orElse(null);
            if (scriptVersion != null) {
                String[] args = runConfig.getScriptArguments() == null ? new String[0] : runConfig.getScriptArguments().toArray(new String[runConfig.getScriptArguments().size()]);
                if (scriptVersion.getType() == ScriptVersion.Type.ACUITY_REPO) {
                    logger.trace("initDreambotScript - loading version off Acuity-Repo.", scriptVersion);
                    try {
                        ScriptLocation scriptInstance = Scripts.loadScript(
                                ArangoDBUtil.keyFromID(runConfig.getScriptID()),
                                ArangoDBUtil.keyFromID(runConfig.getScriptID()),
                                ClientType.DREAMBOT.getID(),
                                scriptVersion.getRevision(),
                                scriptVersion.getJarURL()
                        );
                        scriptInstance.loadJar();
                        Class result = scriptInstance.getScriptLoader().getLoadedClasses().values().stream().filter(AbstractScript.class::isAssignableFrom).findAny().orElse(null);
                        if (result != null) {
                            return startScript(botControl, client, result, args);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Script script = botControl.requestScript(runConfig.getScriptID()).orElse(null);
                    if (script != null){
                        logger.trace("initDreambotScript - loading version off Dreambot-Repo.", script);
                        Map<String, Class<? extends AbstractScript>> repoScripts = getRepoScripts();
                        Class<? extends AbstractScript> aClass = repoScripts.get(script.getTitle());
                        if (aClass != null) {
                            return startScript(botControl, client, aClass, args);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static void setBotControl(BotControl botControl, Class clazz, Object object) {
        Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.getType().equals(BotControl.class)).forEach(field -> {
            boolean accessible = field.isAccessible();
            if (!accessible) field.setAccessible(true);
            try {
                field.set(object, botControl);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (!accessible) field.setAccessible(false);
        });
    }

    private static AbstractScript startScript(BotControl botControl, Client client, Class clazz, String[] args) {
        try {
            AbstractScript abstractScript = (AbstractScript) clazz.newInstance();
            setBotControl(botControl, clazz, abstractScript);
            setBotControl(botControl, clazz.getSuperclass(), abstractScript);
            abstractScript.registerMethodContext(client);
            abstractScript.registerContext(client);
            if (args != null && args.length > 0) {
                logger.debug("Starting script with args. {}, {}", clazz, Arrays.toString(args));
                abstractScript.onStart(args);
            }
            else {
                logger.debug("Starting script without args. {}", clazz);
                abstractScript.onStart();
            }
            return abstractScript;
        } catch (Throwable e) {
            logger.error("Error during script startup.", e);
        }
        return null;
    }

}
