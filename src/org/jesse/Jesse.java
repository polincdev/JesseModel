package org.jesse;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.input.FlyByCamera;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;

public class Jesse {
    public static Node buildWorld(AssetManager assetManager) {
        // Must be false - otherwise uv gets messed up.
        TextureKey keyFloor = new TextureKey("Models/Scene/wall_diffuse_1.jpg", false);// pave_color.jpg
        Texture texFloor = assetManager.loadTexture(keyFloor);

        TextureKey keyNorm = new TextureKey("Models/Scene/wall_normal.jpg", false);// pave_norm.jpg
        Texture texNormalPlane = assetManager.loadTexture(keyNorm);

        Material mat_basic = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat_basic.setTexture("BaseColorMap", texFloor);
        mat_basic.setTexture("NormalMap", texNormalPlane);
        mat_basic.setFloat("Metallic", 0.1f);
        mat_basic.setFloat("Roughness", 0.00f);
        mat_basic.setFloat("Glossiness", 0.5f);

        // Must be false - otherwise uv gets messed up.
        TextureKey keyFloor2 = new TextureKey("Models/Scene/deck.jpg", false);
        Texture texFloor2 = assetManager.loadTexture(keyFloor2);
        TextureKey keyNorm2 = new TextureKey("Models/Scene/deck_normal.jpg", false);
        Texture texNormalPlane2 = assetManager.loadTexture(keyNorm2);

        Material mat_basic2 = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat_basic2.setTexture("BaseColorMap", texFloor2);
        mat_basic2.setFloat("Metallic", 0.1f);
        mat_basic2.setFloat("Roughness", 0.1f);
        mat_basic2.setTexture("NormalMap", texNormalPlane2);
        // Scene
        Node scene = (Node) assetManager.loadModel("Models/Scene/testScene.gltf");

        // Tiling
        ((Geometry) scene.getChild("Plane")).getMesh().scaleTextureCoordinates(new Vector2f(4, 4));
        mat_basic.getTextureParam("BaseColorMap").getTextureValue().setWrap(WrapMode.Repeat);
        mat_basic.getTextureParam("NormalMap").getTextureValue().setWrap(WrapMode.Repeat);
        // Tex for scene
        scene.getChild("Plane").setMaterial(mat_basic);
        scene.getChild("Cube").setMaterial(mat_basic2);

        Node probeNode = (Node) assetManager.loadModel("Models/Scene/market.j3o");
        LightProbe probe = (LightProbe) probeNode.getLocalLightList().iterator().next();
        scene.addLight(probe);

        // We must add a light to make the model visible
        // Light
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White.mult(0.75f));
        sun.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal());
        scene.addLight(sun);

        return scene;
    }

    public static JesseSpatial buildJesse(AssetManager assetManager) {

        Node scene = (Node) assetManager.loadModel("Models/Jesse/Jesse.j3o");
        Spatial jesse = scene.getChild("Jesse");

        // Must be false - otherwise uv gets messed up.
        TextureKey keyBase = new TextureKey("Models/Jesse/JesseBaseColor.png", false);
        Texture texBaseJesse = assetManager.loadTexture(keyBase);
        TextureKey keyMetal = new TextureKey("Models/Jesse/JesseMetallic.png", false);
        Texture texMetalJesse = assetManager.loadTexture(keyMetal);
        TextureKey keyRough = new TextureKey("Models/Jesse/JesseRoughness.png", false);
        Texture texRoughJesse = assetManager.loadTexture(keyRough);
        TextureKey keyNorm = new TextureKey("Models/Jesse/JesseNormal.png", false);
        Texture texNormalJesse = assetManager.loadTexture(keyNorm);

        Material mat_basic = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat_basic.setTexture("BaseColorMap", texBaseJesse);
        mat_basic.setTexture("MetallicMap", texMetalJesse);
        mat_basic.setTexture("RoughnessMap", texRoughJesse);
        mat_basic.setTexture("NormalMap", texNormalJesse);

        jesse.setMaterial(mat_basic);

        return new JesseSpatial(jesse);
    }

    public static Spatial buildSky(AssetManager assetManager) {
        // Skybox
        Texture west, east, north, south, up, down;
        west = assetManager.loadTexture("Models/Scene/Skybox/posx.jpg");// "Models/Scene/grid32BlueBlackPBR.png"
        east = assetManager.loadTexture("Models/Scene/Skybox/negx.jpg");
        north = assetManager.loadTexture("Models/Scene/Skybox/negz.jpg");
        south = assetManager.loadTexture("Models/Scene/Skybox/posz.jpg");
        up = assetManager.loadTexture("Models/Scene/Skybox/posy.jpg");
        down = assetManager.loadTexture("Models/Scene/Skybox/negy.jpg");
        return SkyFactory.createSky(assetManager, west, east, north, south, up, down);
    }

    public static JesseSpatial buildAndAttachScene(AssetManager assetManager, Node rootNode) {
        JesseSpatial jesse = buildJesse(assetManager);
        Node world = buildWorld(assetManager);
        Spatial sky = buildSky(assetManager);

        rootNode.attachChild(world);
        world.attachChild(jesse);
        world.attachChild(sky);

        jesse.setLocalTranslation(-.0f, 4.2f, -0f);
        return jesse;
    }

    public static void setupDefaultCamera(Camera cam, FlyByCamera flyCam) {
        cam.setLocation(new Vector3f(0, 10, 20));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(100);
        flyCam.setDragToRotate(true);
    }

}