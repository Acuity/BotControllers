package com.acuity.botcontrol.clients.dreambot;

import org.dreambot.api.wrappers.items.Item;

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
}
