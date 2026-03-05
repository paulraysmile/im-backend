package com.bx.implatform.session;

/*
 * @Description
 * @Author Blue
 * @Date 2022/10/21
 */
public class SessionContext {


    private static ThreadLocal<UserSession> sessionLocal = new ThreadLocal<>();

    public static UserSession getSession() {
        return sessionLocal.get();
    }

    public static void setSession(UserSession session){
        sessionLocal.set(session);
    }
}
