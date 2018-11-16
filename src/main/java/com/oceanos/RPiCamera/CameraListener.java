package com.oceanos.RPiCamera;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @autor slonikmak on 15.11.2018.
 */
public interface CameraListener {
    void process(BufferedImage image, int length) throws IOException;
}
