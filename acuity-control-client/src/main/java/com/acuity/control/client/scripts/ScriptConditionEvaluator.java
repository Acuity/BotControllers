package com.acuity.control.client.scripts;

import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.scripts.conditions.EndCondition;
import com.acuity.db.domain.vertex.impl.scripts.conditions.Evaluators;
import com.acuity.db.util.Json;

/**
 * Created by Zach on 8/20/2017.
 */
public class ScriptConditionEvaluator {

    public static boolean evaluate(BotControl botControl, EndCondition condition){
        String evaluatorKey = condition.getEvaluatorKey();
        String evaluatorJSON = condition.getEvaluatorJSON();
        if (evaluatorKey != null && evaluatorJSON != null){
            Class aClass = Evaluators.fromKey(evaluatorKey);
            if (aClass != null){
                Object evaluator = Json.GSON.fromJson(evaluatorJSON, aClass);
                return botControl.evaluate(evaluator);
            }
        }
        return true;
    }

}
