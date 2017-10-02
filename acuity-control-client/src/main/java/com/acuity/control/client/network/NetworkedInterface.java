package com.acuity.control.client.network;

import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;

/**
 * Created by Zach on 10/1/2017.
 */
public interface NetworkedInterface {

    void onMessagePackage(MessagePackage messagePackage);

}
