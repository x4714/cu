package com.zergatul.cheatutils.modules.hacks;

import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.FlyHackConfig;
import com.zergatul.cheatutils.controllers.NetworkPacketsController;
import com.zergatul.cheatutils.accessors.ServerboundMovePlayerPacketAccessor;
import com.zergatul.cheatutils.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class FlyHack implements Module {

    public static final FlyHack instance = new FlyHack();
    private static final Minecraft mc = Minecraft.getInstance();

    private int antiKickTickCounter = 0;
    private AntiKickPhase currentAntiKickPhase = AntiKickPhase.NONE;

    private enum AntiKickPhase {
        NONE,
        DIP,      // Tick 0: Apply downward velocity
        RECOVER   // Tick 1: Apply upward/neutral velocity (or let gravity act briefly)
    }

    private FlyHack() {
        NetworkPacketsController.instance.addClientPacketHandler(this::onClientPacket);
        Events.ClientTickStart.add(this::updateAntiKickStateMachine);
        Events.ClientPlayerLoggingOut.add(this::resetAntiKickState);
        Events.DimensionChange.add(this::resetAntiKickState);
    }

    private void resetAntiKickState() {
        antiKickTickCounter = 0;
        currentAntiKickPhase = AntiKickPhase.NONE;
    }

    private void updateAntiKickStateMachine() {
        FlyHackConfig config = ConfigStore.instance.getConfig().flyHackConfig;

        if (!config.enabled || !config.antiKickEnabled || mc.player == null) {
            resetAntiKickState();
            return;
        }

        antiKickTickCounter++;

        if (currentAntiKickPhase == AntiKickPhase.DIP) {
            // Transition from DIP to RECOVER
            currentAntiKickPhase = AntiKickPhase.RECOVER;
        } else if (currentAntiKickPhase == AntiKickPhase.RECOVER) {
            // Transition from RECOVER to NONE
            currentAntiKickPhase = AntiKickPhase.NONE;
        }
        // If NONE, check if it's time to start a new anti-kick cycle
        else if (antiKickTickCounter >= config.antiKickInterval) {
            antiKickTickCounter = 0;
            if (mc.options.keyShift.isDown()) {
                // If sneaking, skip this anti-kick attempt and wait for the next interval
                // Effectively, we just reset the counter and remain in NONE phase
            } else {
                currentAntiKickPhase = AntiKickPhase.DIP;
            }
        }
    }

    // Called by MixinLocalPlayer.onBeforeAiStep
    // Returns true if FlyHack's normal creative-like flight should be active
    public boolean shouldApplyNormalFlyLogic(LocalPlayer player, FlyHackConfig config) {
        if (!config.enabled) return false; // FlyHack disabled, let vanilla handle
        if (config.antiKickEnabled && currentAntiKickPhase != AntiKickPhase.NONE) {
            return false; // Anti-kick is active, it will control abilities.flying
        }
        return true; // Normal FlyHack operation
    }

    // Called by MixinLocalPlayer.onBeforeAiStep when shouldApplyNormalFlyLogic is false
    public void applyAntiKickMotionAndAbilities(LocalPlayer player, FlyHackConfig config) {
        player.getAbilities().flying = false; // Crucial: ensure flying is OFF for anti-kick

        Vec3 currentVel = player.getDeltaMovement();
        if (currentAntiKickPhase == AntiKickPhase.DIP) {
            player.setDeltaMovement(currentVel.x, -config.antiKickDistance, currentVel.z);
        } else if (currentAntiKickPhase == AntiKickPhase.RECOVER) {
            // For Wurst-like recovery, apply a small upward or neutral impulse
            // Or simply let gravity pull down for a tick if that's preferred for "landing"
            // Here, we apply a slight upward push to counteract the dip.
            player.setDeltaMovement(currentVel.x, config.antiKickDistance * 0.5, currentVel.z); // Smaller recovery
            // Alternatively, to let gravity act more naturally for a "touch ground" feel:
            // player.setDeltaMovement(currentVel.x, 0, currentVel.z); // Then rely on onGround=true packet
        }
    }

    private void onClientPacket(NetworkPacketsController.ClientPacketArgs args) {
        if (args.packet instanceof ServerboundMovePlayerPacket packet) {
            FlyHackConfig config = ConfigStore.instance.getConfig().flyHackConfig;
            if (config.enabled) {
                if (config.antiKickEnabled) {
                    if (currentAntiKickPhase == AntiKickPhase.DIP) {
                        ((ServerboundMovePlayerPacketAccessor) packet).setOnGround_CU(false);
                        return;
                    } else if (currentAntiKickPhase == AntiKickPhase.RECOVER) {
                        ((ServerboundMovePlayerPacketAccessor) packet).setOnGround_CU(true); // Simulate landing
                        return;
                    }
                }
                ((ServerboundMovePlayerPacketAccessor) packet).setOnGround_CU(config.onGroundFlag);
            }
        }
    }
}