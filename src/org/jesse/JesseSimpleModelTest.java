package org.jesse;

import com.jme3.app.SimpleApplication;

public class JesseSimpleModelTest extends SimpleApplication {

    @Override
    public void simpleInitApp() {
        Jesse.setupDefaultCamera(cam, flyCam);
        Jesse.buildAndAttachScene(assetManager, rootNode);
    }

    public static void main(String[] args) {
        new JesseSimpleModelTest().start();
    }

}