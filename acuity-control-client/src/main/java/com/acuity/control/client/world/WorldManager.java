package com.acuity.control.client.world;

import com.acuity.control.client.BotControl;

/**
 * Created by Zachary Herridge on 9/25/2017.
 */
public class WorldManager {

    private BotControl botControl;

    public WorldManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public boolean onLoop(){


        return false;
    }

}
