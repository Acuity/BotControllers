package com.acuity.control.client.managers;

import com.acuity.common.world_data_parser.WorldDataResult;
import com.acuity.control.client.BotControl;
import com.acuity.control.client.network.response.MessageResponse;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClient;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.message_package.data.RemoteScriptTask;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.Script;
import com.acuity.db.domain.vertex.impl.scripts.ScriptVersion;
import com.acuity.db.domain.vertex.impl.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 10/5/2017.
 */
public class RemoteManager {

    private static final Logger logger = LoggerFactory.getLogger(RemoteManager.class);
    private static final int TIMEOUT_SECONDS = 6;

    private BotControl botControl;

    public RemoteManager(BotControl botControl) {
        this.botControl = botControl;
    }

    @SuppressWarnings("unchecked")
    public WorldDataResult requestWorldData(){
        try{
            return send(new MessagePackage(MessagePackage.Type.REQUEST_WORLD_DATA, MessagePackage.SERVER))
                    .waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getResponse()
                    .map(messagePackage -> messagePackage.getBodyAs(WorldDataResult.class))
                    .orElse(null);
        }
        catch (Throwable e){
            e.printStackTrace();
        }
        return null;
    }

    public Optional<MessagePackage> confirmState(){
        RSAccount rsAccount = botControl.getRsAccountManager().getRsAccount();

        Optional<MessagePackage> response = send(new MessagePackage(MessagePackage.Type.CONFIRM_CLIENT_STATE, MessagePackage.SERVER)
                .setBody(rsAccount != null ? rsAccount.getID() : null)
        ).waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS).getResponse();

        Boolean rsAccountConfirmed = response.map(messagePackage -> messagePackage.getBodyAs(0, Boolean.class)).orElse(false);
        if (!rsAccountConfirmed) logger.warn("RSAccount failed to be confirmed.");

        logger.trace("Confirmed state. {}, {}", rsAccountConfirmed, rsAccount);

        return response;
    }

    public void updateClientStateNoResponse(BotClientState botClientState, boolean serializeNull) {
        send(new MessagePackage(MessagePackage.Type.UPDATE_CLIENT_STATE, MessagePackage.SERVER)
                .setBody(0, botClientState)
                .setBody(1, serializeNull)
        );
    }

    public boolean updateClientState(BotClientState botClientState, boolean serializeNull) {
        return send(new MessagePackage(MessagePackage.Type.UPDATE_CLIENT_STATE, MessagePackage.SERVER)
                .setBody(0, botClientState)
                .setBody(1, serializeNull)

        )
                .waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getResponse().map(messagePackage -> messagePackage.getBodyAs(boolean.class)).orElse(false);
    }

    @SuppressWarnings("unchecked")
    public List<RSAccount> requestRSAccounts(boolean filterUnassignable) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_ACCOUNTS, MessagePackage.SERVER).setBody(filterUnassignable))
                .waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> Arrays.asList(messagePackage.getBodyAs(RSAccount[].class)))
                .orElse(Collections.EMPTY_LIST);
    }

    @SuppressWarnings("unchecked")
    public List<Tag> requestTags(String title) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_TAGS, MessagePackage.SERVER).setBody(title))
                .waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> Arrays.asList(messagePackage.getBodyAs(Tag[].class)))
                .orElse(Collections.EMPTY_LIST);
    }

    public boolean requestTagAccount(RSAccount account, Tag tag) {
        return send(new MessagePackage(MessagePackage.Type.ADD_RS_ACCOUNT_TAG, MessagePackage.SERVER)
                .setBody(0, true)
                .setBody(1, account)
                .setBody(2, tag)
        ).waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS).getResponse().map(messagePackage -> messagePackage.getBodyAs(boolean.class)).orElse(false);
    }

    public boolean requestAccountAssignment(RSAccount account, boolean force) {
        return send(
                new MessagePackage(MessagePackage.Type.REQUEST_ACCOUNT_ASSIGNMENT, MessagePackage.SERVER)
                        .setBody(0, account == null ? null : account.getID())
                        .setBody(1, force))
                .waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(boolean.class))
                .orElse(false);
    }

    public boolean isAccountAssigned(RSAccount rsAccount) {
        return send(new MessagePackage(MessagePackage.Type.CHECK_ACCOUNT_ASSIGNMENT, MessagePackage.SERVER).setBody(rsAccount.getID()))
                .waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(boolean.class))
                .orElse(true);
    }

    public List<BotClient> requestBotClients() {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_BOT_CLIENTS, MessagePackage.SERVER))
                .waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> Arrays.asList(messagePackage.getBodyAs(BotClient[].class)))
                .orElse(Collections.emptyList());
    }

    public RemoteScriptTask.StartResponse requestRemoteTaskStart(String destinationKey, RemoteScriptTask.StartRequest startRequest) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_REMOTE_TASK_START, destinationKey).setBody(startRequest))
                .waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(RemoteScriptTask.StartResponse.class))
                .orElse(null);
    }

    public Optional<Script> requestScript(String scriptID) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_SCRIPT, MessagePackage.SERVER).setBody(scriptID))
                .waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(Script.class));
    }

    public Optional<Tag> requestTag(String tagID) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_TAG, MessagePackage.SERVER).setBody(tagID))
                .waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getResponse().map(messagePackage -> messagePackage.getBodyAs(Tag.class));
    }

    public void requestStatusSet(String key, String status) {
        send(new MessagePackage(MessagePackage.Type.REQUEST_STATUS_SET, MessagePackage.SERVER)
                .setBody(0, key)
                .setBody(1, status));
    }

    public void requestStatusClear() {
        send(new MessagePackage(MessagePackage.Type.REQUEST_CLEAR_STATUS, MessagePackage.SERVER));
    }

    public Optional<ScriptVersion> requestScriptVersion(String scriptID, String scriptVersionID) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_SCRIPT_VERSION, MessagePackage.SERVER)
                .setBody(0, scriptID)
                .setBody(1, scriptVersionID)
        )
                .waitForResponse(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(ScriptVersion.class));
    }

    public MessageResponse send(MessagePackage messagePackage) {
        return botControl.getConnection().send(messagePackage);
    }

    public void respond(MessagePackage init, MessagePackage response) {
        response.setResponseToKey(init.getResponseKey());
        send(response);
    }
}
