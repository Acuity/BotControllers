package com.acuity.botcontrol.clients.dreambot;

import com.acuity.db.domain.vertex.impl.scripts.conditions.evaluators.ItemCountEvaluator;

/**
 * Created by Zachary Herridge on 8/29/2017.
 */
public class DreambotEvaluator {

    private DreambotControlScript controlScript;

    public DreambotEvaluator(DreambotControlScript controlScript) {
        this.controlScript = controlScript;
    }

    public boolean evaluate(Object evaluator) {
        if (evaluator instanceof ItemCountEvaluator){
            int itemID = ((ItemCountEvaluator) evaluator).getItemID();
            int amount = ((ItemCountEvaluator) evaluator).getAmount();

            int count = controlScript.getInventory().count(itemID) + controlScript.getInventory().count(itemID + 1);
            if (controlScript.getBank().isOpen()){
                count += controlScript.getBank().count(itemID) + controlScript.getBank().count(itemID + 1);
            }

            return count >= amount;
        }
        return true;
    }
}
