package com.acuity.control.client.network.endpoints.config_updates;

import com.acuity.control.client.network.BotControlConnection;
import com.acuity.control.client.network.ControlEndpoint;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptSelector;

/**
 * Created by Zachary Herridge on 10/4/2017.
 */
public class SetSelectorEP extends ControlEndpoint {
    @Override
    public boolean isEndpointOf(int i) {
        return MessagePackage.Type.SET_CONFIG_SELECTOR == i;
    }

    @Override
    public void handle(BotControlConnection botControlConnection, MessagePackage messagePackage) {
        ScriptSelector selector = messagePackage.getBodyAs(ScriptSelector.class);
        botControlConnection.getBotControl().getClientConfigManager().getCurrentConfig().setScriptSelector(selector);
    }
}
