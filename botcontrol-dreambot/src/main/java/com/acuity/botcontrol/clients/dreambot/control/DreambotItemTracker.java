package com.acuity.botcontrol.clients.dreambot.control;

import com.acuity.botcontrol.clients.dreambot.DreambotControlScript;
import com.acuity.db.domain.common.RSItem;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import org.dreambot.api.wrappers.items.Item;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 9/12/2017.
 */
public class DreambotItemTracker {

    private DreambotControlScript controlScript;

    public DreambotItemTracker(DreambotControlScript controlScript) {
        this.controlScript = controlScript;
    }

    public void onChange(Item item) {

    }

    private long lastSend = 0;
    public void onUpdate() {
        long time = System.currentTimeMillis();
        if (time - lastSend < TimeUnit.SECONDS.toMillis(10)) return;

        Set<RSItem> inv = controlScript.getInventory().stream().parallel()
                .filter(Objects::nonNull)
                .map(item -> new RSItem(item.getID(), item.getName(), item.getAmount(), item.isNoted()))
                .distinct()
                .map(rsItem -> {
                    int count = 0;

                    for (Item item : controlScript.getInventory()) {
                        if (item != null && item.getID() == rsItem.getId()) {
                            count += item.getAmount();
                        }
                    }

                    rsItem.setQuantity(count);

                    return rsItem;
                })
                .collect(Collectors.toSet());

        controlScript.getBotControl().getRemote().send(new MessagePackage(MessagePackage.Type.RS_ITEM_UPDATE, MessagePackage.SERVER)
                .setBody(0, inv)
                .setBody(1, 0)
                .setBody(2, controlScript.getClient().getUsername())
        );

        lastSend = time;
    }
}
