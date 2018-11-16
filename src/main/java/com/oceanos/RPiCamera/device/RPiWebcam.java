/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.oceanos.RPiCamera.device;


import com.oceanos.RPiCamera.CameraListener;

/**
 *
 * @author POS
 */
public class RPiWebcam {

    private int width;
    private int height;
    private String name;
    private WebcamCapture webcamCapture;

    public RPiWebcam(int width, int height){
        this(width, height, "/dev/video1");
    }

    public RPiWebcam(int width, int height, String name) {
        this.width = width;
        this.height = height;
        this.name = name;
    }

    public void activate() {
        webcamCapture = new WebcamCapture(width, height, name);
    }

    public void activate(CameraListener listener) {
        webcamCapture = new WebcamCapture(width, height, name);
        webcamCapture.setListener(listener);
    }
}
