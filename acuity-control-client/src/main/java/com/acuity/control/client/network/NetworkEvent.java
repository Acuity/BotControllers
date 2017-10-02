package com.acuity.control.client.network;

import com.google.common.eventbus.SubscriberExceptionContext;

/**
 * Created by Zach on 8/5/2017.
 */
public class NetworkEvent {
    private NetworkEvent() {
    }

    public static class Opened {

    }

    public static class LoginComplete {

    }

    public static class Closed {

    }

    public static class Error {
        private Throwable throwable;
        private SubscriberExceptionContext subscriberExceptionContext;

        public Error(Throwable throwable, SubscriberExceptionContext subscriberExceptionContext) {
            this.throwable = throwable;
            this.subscriberExceptionContext = subscriberExceptionContext;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public SubscriberExceptionContext getSubscriberExceptionContext() {
            return subscriberExceptionContext;
        }
    }
}
