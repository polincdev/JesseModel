
package org.jesse;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.VideoRecorderAppState;
import com.jme3.asset.TextureKey;

import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;

import com.jme3.input.KeyInput;

import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;

import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;

import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;
import java.io.File;

/** Showcase of Jesse model with basic animations - TPP mode - chase camera */
public class JesseModelTest extends SimpleApplication implements AnimEventListener {

  public static void main(String[] args) {
    JesseModelTest app = new JesseModelTest();
    app.start();
  }

  private Node modelsNode;
  private AnimChannel channel;
  private AnimControl control;
  BulletAppState bulletAppState;
  Node player;
  Node playerNode = new Node();

  BitmapText hintText;
  BitmapText debugText;

  // Models of the scene
  Spatial jasse;
  Spatial terrain;
  Spatial objModel;

  // Controler
  boolean simpleControl = true;
  CharacterControl characterCtrl;
  BetterCharacterControl betterCharacterCtrl;

  // Walking variables
  float walkSpeed = 0.1f;
  float runSpeed = 0.35f;
  float rotSpeed = FastMath.PI / 2;
  float blendTime = 0.25f;
  // Shift from capsule
  float modelShiftFromBody = -0.1f;

  // Array of all aniamtions
  String[] animTypes=new String[]  {
   "Walk",
   "WalkBack",
   "WalkStrafeLeft",
   "WalkStrafeRight",
   "JumpStart",
   "JumpMid",
   "JumpEnd",
   "JumpForward",
   "Run",
   "RunStrafeLeft",
   "RunStrafeRight",
   "Stand",
   "Crouch",
   "CrouchToStand",
   "StandToCrouch",
   "TurnLeft",
   "TurnRight",
   "Stop"
  };
  
  // Variables for chase camera
  private Vector3f walkDirection = new Vector3f();
  private Vector3f viewDirection = new Vector3f(0, 0, 1);

  private boolean left = false, right = false, up = false, down = false, jump = false, run = false;
  private boolean crouch = false, leftRot = false, rightRot = false;
  private long leftTime = 0, rightTime = 0, upTime = 0, downTime = 0;
  private int runMarginInMilis = 250;
  private Vector3f camDir = new Vector3f();
  private Vector3f camLeft = new Vector3f();

  @Override
  public void simpleInitApp() {
    this.setDisplayStatView(false);
    flyCam.setMoveSpeed(5f);
    flyCam.setEnabled(false);
    cam.setLocation(new Vector3f(0, 5, 22));
    // Basic input
    initKeys();
    // Main node containing models and lights. Models are added to Scene imported
    // node
    modelsNode = new Node("ModelsNode");
    rootNode.attachChild(modelsNode);
    // Add scene and character
    modelsNode.attachChild(makeSceneAndCharacter());
    // Gui hints
    makeHints();
    // Skybox
    makeSky();
    // PBR light, Shadows, lights, filters
    makeEffects();
    // Physics or models
    makeWorld();
    // Chase cam
    makeCam();
  }

  void makeCam() {
    // create the camera Node
    CameraNode camNode = new CameraNode("Camera Node", cam);
    // This mode means that camera copies the movements of the target:
    camNode.setControlDir(ControlDirection.SpatialToCamera);
    // Attach the camNode to the target:
    player.attachChild(camNode);
    // Move camNode, e.g. behind and above the target:
    camNode.setLocalTranslation(new Vector3f(0, 1.5f, -10));
    // Rotate the camNode to look at the target:
    camNode.lookAt(player.getLocalTranslation(), Vector3f.UNIT_Y);

    // Enable a chase cam for this target (typically the player).
    ChaseCamera chaseCam = new ChaseCamera(cam, playerNode, inputManager);
    // rubber cam
    chaseCam.setSmoothMotion(true);
    chaseCam.setTrailingEnabled(true);
    chaseCam.setChasingSensitivity(9);
    chaseCam.setTrailingRotationInertia(0.5f);
  }

  float rotation = 0;

  @Override
  public void simpleUpdate(float tpf) {
    camDir.set(cam.getDirection()).multLocal(run ? runSpeed : walkSpeed);
    camLeft.set(cam.getLeft()).multLocal(run ? runSpeed * 0.8f : walkSpeed * 0.8f);
    walkDirection.set(0, 0, 0);
    if (left) {
      walkDirection.addLocal(camLeft);
    }
    if (right) {
      walkDirection.addLocal(camLeft.negate());
    }
    if (up) {
      walkDirection.addLocal(camDir);
    }
    if (down) {
      camDir.set(cam.getDirection()).multLocal(walkSpeed / 2f);
      walkDirection.addLocal(camDir.negate());
    }
    if (simpleControl) {
      characterCtrl.setWalkDirection(walkDirection);
      // model follows body - with a shift
      playerNode.setLocalTranslation(characterCtrl.getPhysicsLocation().add(0, modelShiftFromBody, 0));
    } else {

      // BetterCharacter's value must be amplified
      walkDirection.multLocal(60f);
      walkDirection.y = 0;
      betterCharacterCtrl.setWalkDirection(walkDirection);
      // model follows body - with a shift
      playerNode.setLocalTranslation(playerNode.getLocalTranslation().add(walkDirection));
    }

    // Rotation
    if (leftRot) {

      if (simpleControl) {
        Quaternion rotateL = new Quaternion().fromAngleAxis(rotSpeed * tpf, Vector3f.UNIT_Y);
        rotateL.multLocal(viewDirection);
        characterCtrl.setViewDirection(viewDirection);
        // player.rotate(rotateL);
      } else {
        Quaternion rotateL = new Quaternion().fromAngleAxis(rotSpeed * tpf, Vector3f.UNIT_Y);
        rotateL.multLocal(viewDirection);
        betterCharacterCtrl.setViewDirection(viewDirection);
      }
    } else if (rightRot) {
      if (simpleControl) {
        Quaternion rotateL = new Quaternion().fromAngleAxis(-rotSpeed * tpf, Vector3f.UNIT_Y);
        rotateL.multLocal(viewDirection);
        characterCtrl.setViewDirection(viewDirection);
        // player.rotate(rotateL);
      } else {
        Quaternion rotateL = new Quaternion().fromAngleAxis(-rotSpeed * tpf, Vector3f.UNIT_Y);
        rotateL.multLocal(viewDirection);
        betterCharacterCtrl.setViewDirection(viewDirection);

      }
    }

  }

  void makeEffects() {

    // PBR
    Node probeNode = (Node) assetManager.loadModel("Models/Scene/market.j3o");
    LightProbe probe = (LightProbe) probeNode.getLocalLightList().iterator().next();
    rootNode.addLight(probe);

    FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
    FXAAFilter fXAAFilter = new FXAAFilter();
    fXAAFilter.setReduceMul(0.0f);
    fXAAFilter.setSubPixelShift(0.0f);
    fpp.addFilter(fXAAFilter);

    // We must add a light to make the model visible
    // Light
    DirectionalLight sun = new DirectionalLight();
    sun.setColor(ColorRGBA.White.mult(0.75f));
    sun.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal());
    rootNode.addLight(sun);

    // Shadows
    DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, 1024, 4);
    dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
    dlsf.setShadowIntensity(0.6f);
    // dlsf.setLambda(0.1f);
    dlsf.setLight(sun);
    dlsf.setEnabled(true);
    fpp.addFilter(dlsf);

    // Important - parent node should get CastAndReceive
    ((Spatial) modelsNode.getChild("Scene")).setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

    for (Spatial child : ((Node) modelsNode.getChild("Scene")).getChildren()) child.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    //
    SSAOFilter ssaoFilter = new SSAOFilter(2.9299974f, 32.920483f, 5.8100376f, 0.091000035f);
    
    ssaoFilter.setApproximateNormals(false);
    fpp.addFilter(ssaoFilter);

    //
    getViewPort().addProcessor(fpp);

  }

  void makeHints() {
    // Text
    BitmapFont font = getAssetManager().loadFont("Interface/Fonts/Default.fnt");
    // Hint
    hintText = new BitmapText(font);
    hintText.setSize(font.getCharSet().getRenderedSize() * 1.5f);
    hintText.setColor(ColorRGBA.Red);
    hintText.setText("Move:WASD Strafe:QE Crouch:Z");
    hintText.setLocalTranslation(0, this.getCamera().getHeight() - 10, 1.0f);
    hintText.updateGeometricState();
    guiNode.attachChild(hintText);
    // Info
    debugText = hintText.clone();
    debugText.setColor(ColorRGBA.White);
    debugText.setText("AnimType:Stand");
    debugText.setLocalTranslation(0, hintText.getLocalTranslation().y - 30, 1.0f);
    debugText.updateGeometricState();
    guiNode.attachChild(debugText);

  }

  /** Custom Keybinding: Map named actions to inputs. */
  private void initKeys() {
    inputManager.addMapping("Rec", new KeyTrigger(KeyInput.KEY_P));
    inputManager.addListener(actionListener, new String[] { "Rec" });

  }

  private ActionListener actionListener = new ActionListener() {
    public void onAction(String name, boolean keyPressed, float tpf) {

      if (!keyPressed) return;
      if (name.equals("Rec")) {
        switchRecord();
      }

    }

  };

  ActionListener actionListener2 = new ActionListener() {

    @Override
    public void onAction(String binding, boolean value, float tpf) {
      //
      if (binding.equals("Left")) {
        if (value) {

          left = true;
          if (System.currentTimeMillis() - leftTime < runMarginInMilis) doRunStrafeLeft();
          else doWalkStrafeLeft();
          leftTime = System.currentTimeMillis();
        } else {
          left = false;
        }

      } else if (binding.equals("Right")) {
        if (value) {
          right = true;
          if (System.currentTimeMillis() - rightTime < runMarginInMilis) doRunStrafeRight();
          else doWalkStrafeRight();
          rightTime = System.currentTimeMillis();

        } else {
          right = false;
        }
      }
      //
      if (binding.equals("LeftRot")) {
        if (value) {

          leftRot = true;
          doTurnLeft();

        } else {
          leftRot = false;
        }

      } else if (binding.equals("RightRot")) {
        if (value) {
          rightRot = true;
          doTurnRight();

        } else {
          rightRot = false;
        }
      } else if (binding.equals("Up")) {
        if (value) {
          up = true;
          if (System.currentTimeMillis() - upTime < runMarginInMilis) doRun();
          else doWalk();
          upTime = System.currentTimeMillis();
        } else {
          up = false;
        }
      } else if (binding.equals("Down") && !jump) {
        if (value) {
          down = true;
          doWalkBack();
        } else {
          down = false;
        }
      } else if (binding.equals("Jump")) {

        // only once
        if (jump) return;
        jump = true;
        // doJumpStart provides full jump animation. doJumpMid stats midway and is just
        // faster
        doJumpStart();
        // doJumpMid();

      } else if (binding.equals("Crouch")) {
        if (value) {
          crouch = true;
          doStandToCrouch();
        } else {
          crouch = false;
          doCrouchToStand();
        }
      }
      // Default
      if (!up && !left && !right && !down && !jump && !crouch && !leftRot && !rightRot) {
        doStand();
      }

    }
  };

  void doTurnLeft() {

    if (!left && !right && !up && !down && !jump && !crouch) setAnimLoop("TurnLeft", 1.5f, blendTime);
  }

  void doTurnRight() {

    if (!left && !right && !up && !down && !jump && !crouch) setAnimLoop("TurnRight", 1.5f, blendTime);
  }

  void doStand() {
    run = false;
    setAnimLoop("Stand", 1.0f, blendTime * 2);
  }

  void doCrouchToStand() {
    run = false;
    setAnimOnce("CrouchToStand", 2.0f, blendTime / 2f);

    if (!simpleControl) betterCharacterCtrl.setDucked(false);
  }

  void doStandToCrouch() {
    run = false;

    setAnimOnce("StandToCrouch", 1.0f, blendTime);

    if (!simpleControl) betterCharacterCtrl.setDucked(true);
  }

  void doCrouch() {
    run = false;
    setAnimLoop("Crouch", 1.0f, blendTime);

  }

  void doRunStrafeLeft() {
    run = true;
    if (!jump) setAnimLoop("RunStrafeLeft", 1.0f, blendTime);
  }

  void doRunStrafeRight() {
    run = true;
    if (!jump) setAnimLoop("RunStrafeRight", 1.0f, blendTime);
  }

  void doRun() {
    run = true;
    if (!jump) setAnimLoop("Run", 0.9f, blendTime);
  }

  void doStop() {
    run = false;
    if (!jump) setAnimOnce("Stop", 1.0f, blendTime * 4);
  }

  void doWalkStrafeLeft() {
    run = false;
    if (!jump) setAnimLoop("WalkStrafeLeft", 1.0f, blendTime);
  }

  void doWalkStrafeRight() {
    run = false;
    if (!jump) setAnimLoop("WalkStrafeRight", 1.0f, blendTime);
  }

  void doWalk() {
    run = false;
    if (!jump) setAnimLoop("Walk", 1.0f, blendTime);
  }

  void doWalkBack() {
    run = false;
    if (!jump) setAnimLoop("WalkBack", 1.0f, blendTime);
  }

  void doJumpStart() {

    // Disables everything except for move forward
    left = false;
    right = false;
    down = false;
    crouch = false;
    leftRot = false;
    rightRot = false;

    // If waking/running - one step jump forward. Otherwise 3 step jump up
    if (up) {
      setAnimOnce("JumpForward", 1.0f, blendTime);
      // jump right away - dont wait for mid action as it is one step animation
      if (simpleControl) characterCtrl.jump(new Vector3f(0, 7, 0));
      else betterCharacterCtrl.jump();
    } else setAnimOnce("JumpStart", 1.7f, blendTime);
  }

  void doJumpMid() {
    setAnimOnce("JumpMid", 1.0f, blendTime);
    if (simpleControl) characterCtrl.jump(new Vector3f(0, 7, 0));
    else betterCharacterCtrl.jump();
  }

  void doJumpEnd() {
    setAnimOnce("JumpEnd", 1.0f, 0.1f);

  }

  public void setAnimOnce(String animName, float speed, float blendTime) {
    // System.out.println("ANIM once move="+ animName);
    channel.setAnim(animName, blendTime);
    channel.setLoopMode(LoopMode.DontLoop);
    channel.setSpeed(speed);
    refreshDisplay(animName);
  }

  public void setAnimLoop(String animName, float speed, float blendTime) {
    // System.out.println("ANIM loop move="+ animName);
    channel.setAnim(animName, blendTime);
    channel.setLoopMode(LoopMode.Loop);
    channel.setSpeed(speed);
    refreshDisplay(animName);

  }

  void makeWorld() {
    /** Set up Physics */
    bulletAppState = new BulletAppState();
    stateManager.attach(bulletAppState);
    // bulletAppState.setDebugEnabled(true);

    /** Add physics: */
    // We set up collision detection for the scene by creating a
    // compound collision shape and a static RigidBodyControl with mass zero.*/
    // Scene
    CollisionShape terrainShape = CollisionShapeFactory.createMeshShape(modelsNode.getChild("Plane"));
    RigidBodyControl landscape = new RigidBodyControl(terrainShape, 0);
    terrain.addControl(landscape);
    // Objects
    CollisionShape objectShape = CollisionShapeFactory.createMeshShape(modelsNode.getChild("Cube"));
    RigidBodyControl objCtrl = new RigidBodyControl(objectShape, 0);
    objModel.addControl(objCtrl);

    // We set up collision detection for the player by creating
    // a capsule collision shape and a CharacterControl.
    // The CharacterControl offers extra settings for
    // size, stepheight, jumping, falling, and gravity.
    // We also put the player in its starting position.
    CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.0f, 4.5f, 1);
    if (simpleControl) {
      characterCtrl = new CharacterControl(capsuleShape, 2.0f);
      characterCtrl.setJumpSpeed(20);
      characterCtrl.setFallSpeed(20);
      characterCtrl.setGravity(new Vector3f(0f, -10f, 0f));
      characterCtrl.setPhysicsLocation(new Vector3f(0, 5.5f, 0));
      jasse.addControl(characterCtrl);
      ((Node) modelsNode.getChild("Scene")).attachChild(jasse);
    } else {
      betterCharacterCtrl = new BetterCharacterControl(1.0f, 6.5f, 1.2f);
      betterCharacterCtrl.setJumpForce(new Vector3f(0, 15f, 0));
      betterCharacterCtrl.setGravity(new Vector3f(0f, -35f, 0f));
      betterCharacterCtrl.setDuckedFactor(0.6f);
      betterCharacterCtrl.warp(new Vector3f(0, 7.5f, 0));
      playerNode.attachChild(jasse);
      jasse.move(0, modelShiftFromBody, 0);
      playerNode.addControl(betterCharacterCtrl);
      bulletAppState.getPhysicsSpace().addAll(playerNode);
      ((Node) modelsNode.getChild("Scene")).attachChild(playerNode);
    }
    // We attach the scene and the player to the rootnode and the physics space,
    // to make them appear in the game world.
    bulletAppState.getPhysicsSpace().add(terrain);
    bulletAppState.getPhysicsSpace().add(objModel);
    if (simpleControl) bulletAppState.getPhysicsSpace().add(characterCtrl);
    else {
      // add to physics state
      bulletAppState.getPhysicsSpace().add(betterCharacterCtrl);
      bulletAppState.getPhysicsSpace().addAll(jasse);
    }

    registerInput();
  }

  public void registerInput() {
    inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_Q));
    inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_E));
    inputManager.addMapping("LeftRot", new KeyTrigger(KeyInput.KEY_A));
    inputManager.addMapping("RightRot", new KeyTrigger(KeyInput.KEY_D));
    inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
    inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
    inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
    inputManager.addMapping("Crouch", new KeyTrigger(KeyInput.KEY_Z));
    inputManager.addListener(actionListener2, "Left");
    inputManager.addListener(actionListener2, "Right");
    inputManager.addListener(actionListener2, "LeftRot");
    inputManager.addListener(actionListener2, "RightRot");
    inputManager.addListener(actionListener2, "Up");
    inputManager.addListener(actionListener2, "Down");
    inputManager.addListener(actionListener2, "Jump");
    inputManager.addListener(actionListener2, "Crouch");
  }

  void makeSky() {
    // Skybox
    Texture west, east, north, south, up, down;
    west = assetManager.loadTexture("Models/Scene/Skybox/posx.jpg");// "Models/Scene/grid32BlueBlackPBR.png"
    east = assetManager.loadTexture("Models/Scene/Skybox/negx.jpg");
    north = assetManager.loadTexture("Models/Scene/Skybox/negz.jpg");
    south = assetManager.loadTexture("Models/Scene/Skybox/posz.jpg");
    up = assetManager.loadTexture("Models/Scene/Skybox/posy.jpg");
    down = assetManager.loadTexture("Models/Scene/Skybox/negy.jpg");
    Spatial skybox = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
    modelsNode.attachChild(skybox);
  }

  protected Spatial makeCharacter() {

    // load a character
    Spatial scene = assetManager.loadModel("Models/Jesse/Jesse.j3o");
    Node sceneAsNode = ((Node) scene);
    jasse = ((Node) scene).getChild("Jesse");
    jasse.scale(1f);
    // Move by 50% asssuming that the root bone is half of its body.
    // Since Jaime's proportions are not humanoid these numbers are different
    // BoundingBox box = (BoundingBox)jamie.getWorldBound();
    jasse.setLocalTranslation(-.0f, 3.15f, -0f);

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
    // Temp settings
    mat_basic.setFloat("Metallic", 0.18f);
    mat_basic.setFloat("Roughness", 0.251f);

    jasse.setMaterial(mat_basic);

    // Anim
    player = (Node) jasse;
    control = player.getControl(AnimControl.class);
    control.addListener(this);
    channel = control.createChannel();
    channel.setAnim("Stand");// animTypes[currentType]

    // List animations
    // for(int a=0;a< control.getAnimationNames().size();a++)
    // System.out.println("\""+control.getAnimationNames().toArray()[a]+"\",");

    return jasse;
  }

  Spatial makeSceneAndCharacter() {
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
    Spatial scene = assetManager.loadModel("Models/Scene/testScene.gltf");
    Node sceneAsNode = ((Node) scene);

    // Tiling
    ((Geometry) sceneAsNode.getChild("Plane")).getMesh().scaleTextureCoordinates(new Vector2f(4, 4));
    mat_basic.getTextureParam("BaseColorMap").getTextureValue().setWrap(WrapMode.Repeat);
    mat_basic.getTextureParam("NormalMap").getTextureValue().setWrap(WrapMode.Repeat);
    // Tex for scene
    sceneAsNode.getChild("Plane").setMaterial(mat_basic);
    sceneAsNode.getChild("Cube").setMaterial(mat_basic2);
    // Physics
    terrain = sceneAsNode.getChild("Plane");
    objModel = sceneAsNode.getChild("Cube");
    // Configure character - it is added via PlayerNode in makeWorld
    makeCharacter();

    return sceneAsNode;
  }

  public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {

    // Start just animation - time for real jump
    if (animName.equals("JumpStart")) {
      doJumpMid();
    }
    // Real jump complete lets fnish it
    else if (animName.equals("JumpMid")) {
      doJumpEnd();
    } else if (animName.equals("JumpEnd") || animName.equals("JumpForward")) {

      jump = false;
      // ressurects pre jump action
      if (up) if (run) doRun();
      else doWalk();
    }
    // If running and stoping
    else if (animName.equals("Stop")) {
      doStand();
    }

    // Init crouching
    else if (animName.equals("StandToCrouch")) {
      doCrouch();
    }

  }

  public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
    // unused
  }

  void refreshDisplay(String animType) {
    debugText.setText("AnimType:" + animType);

  }

  VideoRecorderAppState videoRecorderAppState = null;
  boolean recordActive = false;

  public void startRecord() {
    videoRecorderAppState = new VideoRecorderAppState(new File("d:\\snap_" + System.currentTimeMillis() + ".avi"));
    stateManager.attach(videoRecorderAppState);
    recordActive = true;
  }

  public void stopRecord() {
    stateManager.detach(videoRecorderAppState);
    recordActive = false;
  }

  public void switchRecord() {
    if (recordActive) stopRecord();
    else startRecord();
  }
}