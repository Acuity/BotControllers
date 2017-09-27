package com.acuity.control.client.network.websockets.response;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 8/15/2017.
 */
public class ResponseTracker {

    private Cache<String, MessageResponse> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    public Cache<String, MessageResponse> getCache() {
        return cache;
    }
}
