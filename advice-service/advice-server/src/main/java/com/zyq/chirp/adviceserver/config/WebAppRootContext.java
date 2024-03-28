package com.zyq.chirp.adviceserver.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.web.util.WebAppRootListener;

public class WebAppRootContext implements ServletContextInitializer {
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        servletContext.addListener(WebAppRootListener.class);
        //设置websocket传输消息的最大长度为1M
        int maxLength = 1024 * 1024;
        servletContext.setInitParameter("org.apache.tomcat.websocket.textBufferSize", Integer.toString(maxLength));
    }
}
