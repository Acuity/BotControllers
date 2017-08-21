package com.acuity.botcontrol.clients.dreambot;

import org.dreambot.api.methods.container.impl.bank.BankMode;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.interactive.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Zach on 5/28/2017.
 */
@ScriptManifest(name = "TestScript", author = "AcuityBotting", category = Category.MISC, description = "", version = 0)
public class TempMuleCode extends AbstractScript{

    private String partnerName;
    private int world;
    private Tile location;
    private Map<Integer, Integer> itemsSet = new HashMap<>();

    private boolean banked = false;
    private boolean tradeOpen = false;

    @Override
    public void onStart(String... strings) {
        if (strings.length < 3) return;
        partnerName = strings[0];

        world = Integer.parseInt(strings[1]);

        String[] tileArgs = strings[2].split(",");
        location = new Tile(Integer.parseInt(tileArgs[0]), Integer.parseInt(tileArgs[1]), Integer.parseInt(tileArgs[2]));


        for (int i = 3; i < strings.length; i++) {
            String[] itemArgs = strings[i].split(",");
            itemsSet.put(Integer.parseInt(itemArgs[0]), Integer.parseInt(itemArgs[1]));
        }
    }

    @Override
    public int onLoop() {
        if (tradeOpen && !getTrade().isOpen()){
            return -1;
        }
        if (world != getClient().getCurrentWorld()){
            getWorldHopper().hopWorld(world);
        }
        else if (location.distance() > 10){
            getWalking().walk(location);
        }
        else if (!banked){
            if (getBank().isOpen()){
                getBank().setWithdrawMode(BankMode.NOTE);
                boolean change = false;
                for (Map.Entry<Integer, Integer> item : itemsSet.entrySet()) {
                    if (getInventory().count(item.getKey() + 1) < item.getValue() && getBank().contains(item.getKey())){
                        getBank().withdraw(item.getKey(), item.getValue());
                        sleep(1000, 2000);
                        change = true;
                    }
                }
                if (!change) banked = true;
            }
            else {
                getBank().openClosest();
            }
        }
        else if (getTrade().isOpen()){
            tradeOpen = true;
            boolean contains = false;
            for (Map.Entry<Integer, Integer> item : itemsSet.entrySet()) {
                if (getInventory().contains(item.getKey() + 1)){
                    getTrade().addItem(item.getKey() + 1, 9999999);
                    contains = true;
                }
            }
            if (!contains){
                getTrade().acceptTrade();
            }
        }
        else {
            if (getBank().isOpen()) getBank().close();
            Player closest = getPlayers().closest(partnerName);
            if (closest != null) {
                closest.interact("Trade with");
                sleepUntil(() -> getTrade().isOpen(), 5000);
            }
        }
        return 300;
    }
}
