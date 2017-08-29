package com.acuity.control.client.security;

import java.lang.reflect.ReflectPermission;
import java.security.Permission;

/**
 * Created by Zachary Herridge on 8/29/2017.
 */
public class AcuitySecurityManager {

    public static void init(){
        SecurityManager securityManager = new SecurityManager(){
            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof ReflectPermission){
                    if (perm.getName().equals("suppressAccessChecks")){

                    }
                }

                if (perm instanceof RuntimePermission){
                    if (perm.getName().equals("decryptString")){

                    }
                }
            }
        };
        System.setSecurityManager(securityManager);
    }
}
