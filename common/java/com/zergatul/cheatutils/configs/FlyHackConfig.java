package com.zergatul.cheatutils.configs;

import com.zergatul.cheatutils.utils.MathUtils;

public class FlyHackConfig extends ModuleConfig implements ValidatableConfig {

    public boolean overrideFlyingSpeed;
    public float flyingSpeed;
    public boolean onGroundFlag;

    public boolean antiKick;
    public int antiKickInterval;
    public float antiKickDistance;

    public FlyHackConfig() {
        enabled = false;
        overrideFlyingSpeed = false;
        flyingSpeed = 0.05f;

        antiKick = false;
        antiKickInterval = 30;
        antiKickDistance = 0.07f;
    }

    @Override
    public void validate() {
        flyingSpeed = MathUtils.clamp(flyingSpeed, 0.001f, 10f);
        antiKickInterval = MathUtils.clamp(antiKickInterval, 5, 80);
        antiKickDistance = MathUtils.clamp(antiKickDistance, 0.01f, 0.2f);
    }
}