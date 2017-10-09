package com.acuity.botcontrol.clients.dreambot.control;

import com.acuity.botcontrol.clients.dreambot.DreambotControlScript;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.SkillTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        if (!controlScript.getBotControl().getClientInterface().isSignedIn()){
            skillTracker = null;
            return;
        }

        if (skillTracker == null){
            skillTracker = new SkillTracker(controlScript.getClient());
            skillTracker.resetAll();
            skillTracker.start();
            return;
        }

        for (Skill skill : Skill.values()) {
            long gainedExperience = skillTracker.getGainedExperience(skill);
            if (gainedExperience != 0){
                logger.info("Gained XP in {}. {}", skill.getName(), gainedExperience);
            }
        }
    }
}
