package com.zergatul.cheatutils.configs;

import com.zergatul.cheatutils.utils.MathUtils;

public class FlyHackConfig extends ModuleConfig implements ValidatableConfig {

    public boolean overrideFlyingSpeed;
    public float flyingSpeed;
    public boolean onGroundFlag;

    // Anti-Kick settings
    public boolean antiKickEnabled;
    public int antiKickInterval;
    public double antiKickDistance;

    public FlyHackConfig() {
        enabled = false;
        overrideFlyingSpeed = false;
        flyingSpeed = 0.05f;
        onGroundFlag = false; // Default to false, can be true for NoFall-like behavior

        antiKickEnabled = false;
        antiKickInterval = 30; // Ticks, e.g., Wurst default
        antiKickDistance = 0.07; // Blocks, e.g., Wurst default
    }

    @Override
    public void validate() {
        flyingSpeed = MathUtils.clamp(flyingSpeed, 0.001f, 10f);
        antiKickInterval = MathUtils.clamp(antiKickInterval, 5, 80); // Example range
        antiKickDistance = MathUtils.clamp(antiKickDistance, 0.01, 0.2); // Example range
    }
}