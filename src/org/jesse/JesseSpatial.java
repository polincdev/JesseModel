package org.jesse;

import java.util.ArrayList;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class JesseSpatial extends Node {
    public enum JesseAnimations {
        Walk, WalkBack, WalkStrafeLeft, WalkStrafeRight, JumpStart, JumpMid, JumpEnd, JumpForward, Run, RunStrafeLeft, RunStrafeRight, Stand, Crouch, CrouchToStand, StandToCrouch, TurnLeft, TurnRight, Stop
    }

    private AnimControl animControl;
    private ArrayList<AnimChannel> animChannels = new ArrayList<AnimChannel>();

    public JesseSpatial(Spatial sp) {
        super("Jesse");
        attachChild(sp);
        animControl = sp.getControl(AnimControl.class);
        playAnim(0, JesseAnimations.Stand, true);
    }

    public AnimChannel getChannel(int i) {
        while (animChannels.size() <= i) animChannels.add(animControl.createChannel());
        return animChannels.get(i);
    }

    public void playAnim(int channel, JesseAnimations anim, boolean loop) {
        AnimChannel animChannel = getChannel(channel);
        animChannel.setAnim(anim.name(), 0.3f);
        animChannel.setLoopMode(loop ? LoopMode.Loop : LoopMode.DontLoop);
    }

}
