package com.oceanos.RPiCamera;

import com.oceanos.RPiCamera.device.RPiWebcam;
import com.oceanos.RPiCamera.device.WebcamWebSocketHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * @autor slonikmak on 15.11.2018.
 */
public class Main {
    public static void main(String[] args) throws Exception {

        Server server = new Server(8123);
        WebSocketHandler wsHandler = new WebSocketHandler() {

            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.register(WebcamWebSocketHandler.class);
            }
        };

        server.setHandler(wsHandler);
        server.start();
        server.join();

    }
}
