package com.acuity.control.client.scripts;

import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.conditions.Evaluators;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptEvaluator;
import com.acuity.db.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Zach on 8/20/2017.
 */
public class ScriptConditionEvaluator {

    private static Logger logger = LoggerFactory.getLogger(ScriptConditionEvaluator.class);

    public static boolean evaluate(BotControl botControl, List<ScriptEvaluator> evaluators){
        if (evaluators == null) return true;

        for (ScriptEvaluator evaluator : evaluators) {
            if (!evaluate(botControl, evaluator)) return false;
        }
        return true;
    }

    private static boolean evaluate(BotControl botControl, ScriptEvaluator evaluator){
        String evaluatorKey = evaluator.getKey();
        String evaluatorJSON = evaluator.getJson();
        if (evaluatorKey != null && evaluatorJSON != null){
            Class aClass = Evaluators.fromKey(evaluatorKey);
            if (aClass != null){
                Object instance = Json.GSON.fromJson(evaluatorJSON, aClass);
                try {
                    boolean evaluate = botControl.evaluate(instance);
                    logger.debug("Evaluated - {}, {}, result={}.", evaluatorKey, evaluatorJSON, evaluate);
                    return evaluate;
                }
                catch (Throwable e){
                    logger.error("Error during evaluating.", e);
                }
            }
        }
        return false;
    }

}
