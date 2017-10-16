package com.acuity.botcontrol.clients.dreambot.control;

import com.acuity.botcontrol.clients.dreambot.DreambotControlScript;
import com.acuity.db.domain.common.RSItem;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import org.dreambot.api.wrappers.items.Item;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 9/12/2017.
 */
public class DreambotItemTracker {

    private DreambotControlScript controlScript;
    private Instant lastSend = Instant.MIN;

    public DreambotItemTracker(DreambotControlScript controlScript) {
        this.controlScript = controlScript;
    }

    public void onChange(Item item) {

    }

    private int count(int itemID) {
        return controlScript.getInventory().stream()
                .filter(Objects::nonNull)
                .filter(item -> itemID == item.getID())
                .mapToInt(Item::getAmount).sum();
    }


    public void onUpdate() {
        Instant now = Instant.now();
        if (lastSend.isBefore(now.minusSeconds(10))) return;

        Set<RSItem> inv = controlScript.getInventory().stream().parallel()
                .filter(Objects::nonNull)
                .map(item -> new RSItem(item.getID(), item.getName(), item.getAmount(), item.isNoted()))
                .distinct()
                .map(rsItem -> {
                    rsItem.setQuantity(count(rsItem.getId()));
                    return rsItem;
                })
                .collect(Collectors.toSet());

        controlScript.getBotControl().getRemote().send(new MessagePackage(MessagePackage.Type.RS_ITEM_UPDATE, MessagePackage.SERVER)
                .setBody(0, inv)
                .setBody(1, 0)
                .setBody(2, controlScript.getClient().getUsername())
        );

        lastSend = now;
    }
}
