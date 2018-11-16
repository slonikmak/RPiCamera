package com.oceanos.RPiCamera.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @autor slonikmak on 15.11.2018.
 */
@WebSocket
public class WebcamWebSocketHandler {

    static {
        Webcam.setDriver(new V4l4jDriver());
    }

    static private Logger LOG = Logger.getLogger(WebcamWebSocketHandler.class.getName());

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Session session;

    private void teardown() {
        try {
            session.close();
            session = null;
        } finally {
            //WebcamCache.unsubscribe(this);
        }
    }

    private void setup(Session session) {
        this.session = session;

        Map<String, Object> message = new HashMap<String, Object>();
        message.put("type", "list");
        message.put("webcams", Arrays.asList("/dev/video0"));

        send(message);


       /* Webcam webcam = Webcam.getWebcamByName("/dev/video1");
        webcam.setViewSize(new Dimension(640, 480));
        webcam.open();*/

        /*new Thread(()->{
            while (true){
                try {
                    BufferedImage bufferedImage = webcam.getImage();
                    if (bufferedImage != null){
                        newImage(bufferedImage);
                        //ImageIO.write(bufferedImage, "JPG", new File("/home/pi/new.jpg"));
                        bufferedImage.flush();
                        System.out.println("get");

                        Thread.sleep(40);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();*/

        RPiWebcam rPiWebcam = new RPiWebcam(1280, 720);
        rPiWebcam.activate((d,l)-> {
            System.out.println("recived "+l);
            newImage(d);
        });

    }
    @OnWebSocketClose
    public void onClose(int status, String reason) {
        LOG.info("WebSocket closed, status = {}, reason = {} " + status + " "+ reason);
        teardown();
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        LOG.log(Level.WARNING, "WebSocket error "+t);
        teardown();
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        LOG.info("WebSocket connect, from = "+session.getRemoteAddress().getAddress());
        setup(session);
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        LOG.info("WebSocket message, text = " + message);
    }

    public void newImage(BufferedImage image) throws IOException {

        //LOG.info("New image from " + webcam);

        //image = ImageIO.read(new File("/home/pi/new.jpg"));


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "JPG", baos);
        } catch (IOException e) {
            LOG.warning(e.getMessage());
        }

        String base64 = null;
        try {
            base64 = new String(Base64.getEncoder().encode(baos.toByteArray()), "UTF8");
        } catch (UnsupportedEncodingException e) {
            LOG.warning(e.getMessage());
        }

        Map<String, Object> message = new HashMap<String, Object>();
        message.put("type", "image");
        message.put("webcam", "default");
        message.put("image", base64);

        send(message);
    }

    private void send(String message) {
        if (session.isOpen()) {
            try {
                session.getRemote().sendStringByFuture(message);
            } catch (Exception e) {
                //LOG.warning("Exception when sending string", e);
                e.printStackTrace();
            }
        }
    }

    private void send(Object object) {
        try {
            send(MAPPER.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            //LOG.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }
}
