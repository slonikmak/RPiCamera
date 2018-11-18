package com.oceanos.RPiCamera.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;
import com.pi4j.io.serial.*;
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

    private final Serial serial = SerialFactory.createInstance();

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

        initSerial();

        Map<String, Object> message = new HashMap<String, Object>();
        message.put("type", "list");
        message.put("webcams", Arrays.asList("/dev/video0"));

        send(message);


        /*Webcam webcam = Webcam.getWebcamByName("/dev/video0");
        webcam.setViewSize(new Dimension(640, 480));
        webcam.open();

        new Thread(()->{
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
        }).start();
*/
        RPiWebcam rPiWebcam = new RPiWebcam(320, 240, "/dev/video0");
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
        try {
            serial.writeln(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private void initSerial(){


        ///////////SERIAL INIT
        serial.addListener(new SerialDataEventListener() {
            @Override
            public void dataReceived(SerialDataEvent event) {

                // NOTE! - It is extremely important to read the data received from the
                // serial port.  If it does not get read from the receive buffer, the
                // buffer will continue to grow and consume memory.

                // print out the data received to the console
                try {
                    //System.out.println("[HEX DATA]   " + event.getHexByteString());
                    System.out.println("[ASCII DATA] " + event.getAsciiString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        ////////////

        try {
            // create serial config object
            SerialConfig config = new SerialConfig();

            // set default serial settings (device, baud rate, flow control, etc)
            //
            // by default, use the DEFAULT com port on the Raspberry Pi (exposed on GPIO header)
            // NOTE: this utility method will determine the default serial port for the
            //       detected platform and board/model.  For all Raspberry Pi models
            //       except the 3B, it will return "/dev/ttyAMA0".  For Raspberry Pi
            //       model 3B may return "/dev/ttyS0" or "/dev/ttyAMA0" depending on
            //       environment configuration.
            config.device("/dev/ttyUSB0")
                    .baud(Baud._9600)
                    .dataBits(DataBits._8)
                    .parity(Parity.NONE)
                    .stopBits(StopBits._1)
                    .flowControl(FlowControl.NONE);

            // parse optional command argument options to override the default serial settings.

            // display connection details
            System.out.println(" Connecting to: " + config.toString());


            // open the default serial device/port with the configuration settings
            serial.open(config);


        }
        catch(IOException ex) {
            System.out.println(" ==>> SERIAL SETUP FAILED : " + ex.getMessage());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            System.out.println("close serial");
            try {
                serial.flush();
                //serial.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }
}
