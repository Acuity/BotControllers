package com.acuity.botcontrol.clients.dreambot;

import com.acuity.db.domain.vertex.impl.scripts.conditions.evaluators.ItemCountEvaluator;
import com.acuity.db.domain.vertex.impl.scripts.conditions.evaluators.VarpEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Zachary Herridge on 8/29/2017.
 */
public class DreambotEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(DreambotEvaluator.class);

    private DreambotControlScript controlScript;

    public DreambotEvaluator(DreambotControlScript controlScript) {
        this.controlScript = controlScript;
    }

    public boolean evaluate(Object evaluator) {
        if (evaluator instanceof ItemCountEvaluator){
            if (!controlScript.getBotControl().getClientManager().isSignedIn()) return false;

            int itemID = ((ItemCountEvaluator) evaluator).getItemID();
            int amount = ((ItemCountEvaluator) evaluator).getAmount();

            int count = controlScript.getInventory().count(itemID) + controlScript.getInventory().count(itemID + 1);
            if (controlScript.getBank().isOpen()){
                count += controlScript.getBank().count(itemID) + controlScript.getBank().count(itemID + 1);
            }

            boolean result = count >= amount;
            logger.trace("Item {} evaluated to {} vs {}, result={}.", itemID, count, amount, result);
            return result;
        }

        if (evaluator instanceof VarpEvaluator){
            if (!controlScript.getBotControl().getClientManager().isSignedIn()) return false;

            int varpID = ((VarpEvaluator) evaluator).getVarpID();
            int config = controlScript.getPlayerSettings().getConfig(varpID);
            boolean result = config != ((VarpEvaluator) evaluator).getVarp();
            logger.trace("Varp {} evaluated to {}, result={}.", varpID, config, result);
            return result;
        }

        return false;
    }
}
