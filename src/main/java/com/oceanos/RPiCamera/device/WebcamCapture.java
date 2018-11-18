/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oceanos.RPiCamera.device;

import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.StateException;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;
import com.oceanos.RPiCamera.CameraListener;

import java.io.IOException;

/**
 *
 * @author POS
 */
public class WebcamCapture implements CaptureCallback {

   
    private static int std = V4L4JConstants.MAX_JPEG_QUALITY, channel = 0, q = 60;

    private CameraListener listener;

    private VideoDevice videoDevice;
    private FrameGrabber frameGrabber;
    int width;
    int height;
    String name;

    public WebcamCapture(int width, int height, String name) {
        this.width = width;
        this.height = height;
        this.name = name;
        Webcam.setDriver(new V4l4jDriver());
        try {
            initFrameGrabber();
        } catch (V4L4JException ex) {
           ex.printStackTrace();
            cleanupCapture();
            return;
        }

        // start capture
        try {
            frameGrabber.startCapture();
        } catch (V4L4JException ex) {
           ex.printStackTrace();
        }


    }

    private void initFrameGrabber() throws V4L4JException {
        System.out.println("camera : "+name);
        videoDevice = new VideoDevice(name);
        frameGrabber = videoDevice.getJPEGFrameGrabber(width, height, channel, std, q);
        frameGrabber.setCaptureCallback(this);

        width = frameGrabber.getWidth();
        height = frameGrabber.getHeight();

        System.out.println("start capture");

    }

    private void cleanupCapture() {
        try {
            frameGrabber.stopCapture();
        } catch (StateException ex) {
            // the frame grabber may be already stopped, so we just ignore
            // any exception and simply continue.
           ex.printStackTrace();
        }

        // release the frame grabber and video device
        videoDevice.releaseFrameGrabber();
        videoDevice.release();
    }

    public void close() {

        cleanupCapture();
    }

    @Override
    public void exceptionReceived(V4L4JException ex) {
        // This method is called by v4l4j if an exception
        // occurs while waiting for a new frame to be ready.
        // The exception is available through e.getCause()
       ex.printStackTrace();
    }

    // long time = System.nanoTime();
    @Override
    public void nextFrame(VideoFrame frame) {

        //  System.out.println(TimeUnit.MILLISECONDS.convert(System.nanoTime() - time, TimeUnit.NANOSECONDS) + " ms" + " Size: " + ((float) frame.getFrameLength() / (1024 * 1024)) + " mb");
        //  time = System.nanoTime();
        //connection.send(frame.getBytes(), frame.getFrameLength());
        try {

            listener.process(frame.getBufferedImage(), frame.getFrameLength());
        } catch (IOException e) {
            e.printStackTrace();
        }
        frame.recycle();

    }

    public void setListener(CameraListener listener) {
        this.listener = listener;
    }
}
