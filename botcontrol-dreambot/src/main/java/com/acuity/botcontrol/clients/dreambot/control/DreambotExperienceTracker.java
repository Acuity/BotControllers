package com.acuity.botcontrol.clients.dreambot.control;

import com.acuity.botcontrol.clients.dreambot.DreambotControlScript;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.SkillTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Zachary Herridge on 10/9/2017.
 */
public class DreambotExperienceTracker {

    private static Logger logger = LoggerFactory.getLogger(DreambotExperienceTracker.class);

    private DreambotControlScript controlScript;
    private SkillTracker skillTracker;

    public DreambotExperienceTracker(DreambotControlScript controlScript) {
        this.controlScript = controlScript;
    }

    public void execute(){
        if (!controlScript.getBotControl().getClientInterface().isSignedIn() ||
                controlScript.getBotControl().getRsAccountManager().getLastNotSignedIn().isAfter(Instant.now().minusSeconds(15))){
            skillTracker = null;
            return;
        }

        if (skillTracker == null){
            skillTracker = new SkillTracker(controlScript.getClient());
            skillTracker.start();
            return;
        }

        for (Skill skill : Skill.values()) {
            long gainedExperience = skillTracker.getGainedExperience(skill);
            if (gainedExperience != 0 && skillTracker.getStartExperience(skill) != 0){
                logger.debug("Sending skill xp event {}, {}.", skill.getName(), gainedExperience);
                Map<String, Object> event = new HashMap<>();
                event.put("skill", skill.getName());
                event.put("xpGained", gainedExperience);

                controlScript.getBotControl().getRemote().send(new MessagePackage(MessagePackage.Type.ADD_KEENIO_EVENT, MessagePackage.SERVER)
                        .setBody(0, "skills.gainedXP")
                        .setBody(1,  event));
            }
        }

        skillTracker.start();
    }
}
