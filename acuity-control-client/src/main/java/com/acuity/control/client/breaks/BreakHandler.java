package com.acuity.control.client.breaks;

import com.acuity.control.client.AbstractBotController;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.profiles.BreakProfile;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Zachary Herridge on 8/15/2017.
 */
public class BreakHandler {

    private AbstractBotController controller;

    private BreakProfile profile;

    private long nextBreak;
    private long nextBeakDuration;
    private long nextBreakEndTime;

    public BreakHandler(AbstractBotController controller) {
        this.controller = controller;
    }

    public int loop(){
        if (profile == null) return -1;

        long time = System.currentTimeMillis();
        if (time > nextBreak){
            if (time < nextBreakEndTime){
                return 1000;
            }
            else {
                generateBreak();
            }
        }
        return -1;
    }

    private void generateBreak(){
        long interval = ThreadLocalRandom.current().nextLong(profile.getMinInterval(), profile.getMaxInterval());
        nextBreak = System.currentTimeMillis() + interval;
        nextBeakDuration = ThreadLocalRandom.current().nextLong(profile.getMinLength(), profile.getMaxLength());
        nextBreakEndTime = nextBreak + nextBeakDuration;
        controller.send(new MessagePackage(MessagePackage.Type.BREAK_UPDATE, MessagePackage.SERVER)
                .setBody(0, nextBreak)
                .setBody(1, nextBeakDuration)
        );
    }

    private void clearBreak(){
        profile = null;
        nextBreak = -1;
        nextBeakDuration = -1;
        nextBreakEndTime = -1;
    }

    public void setProfile(BreakProfile profile) {
        clearBreak();
        this.profile = profile;
        if (profile != null) generateBreak();
    }

    public BreakProfile getProfile() {
        return profile;
    }
}
