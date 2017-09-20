package com.acuity.control.client.breaks;

import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.profiles.BreakProfile;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class BreakManager {

    private BotControl botControl;

    private BreakProfile profile;

    private long nextBreak;
    private long nextBeakDuration;
    private long nextBreakEndTime;

    public BreakManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public int onLoop(){
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
        botControl.send(new MessagePackage(MessagePackage.Type.BREAK_UPDATE, MessagePackage.SERVER)
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

    public void onBotClientConfigUpdate(BotClientConfig config) {
        Integer hashCode = profile != null ? profile.hashCode() : null;
        Integer otherHashCode = config.getBreakProfile().map(BreakProfile::hashCode).orElse(null);
        if (!Objects.equals(hashCode, otherHashCode)) setProfile(config.getBreakProfile().orElse(null));
    }
}
