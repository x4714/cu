package com.zergatul.cheatutils.modules.hacks;

import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.FlyHackConfig;
import com.zergatul.cheatutils.controllers.NetworkPacketsController;
import com.zergatul.cheatutils.accessors.ServerboundMovePlayerPacketAccessor;
import com.zergatul.cheatutils.modules.Module;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class FlyHack implements Module {

    public static final FlyHack instance = new FlyHack();

    private int antiKickTickCounter = 0;
    private boolean isPerformingAntiKickDip = false;    // True during the downward motion tick
    private boolean isAfterAntiKickDip = false;        // True for the tick immediately after the dip

    private FlyHack() {
        NetworkPacketsController.instance.addClientPacketHandler(this::onClientPacket);
        Events.ClientTickStart.add(this::updateAntiKickState);
        Events.ClientPlayerLoggingOut.add(this::resetAntiKickState);
        Events.DimensionChange.add(this::resetAntiKickState);
    }

    private void resetAntiKickState() {
        antiKickTickCounter = 0;
        isPerformingAntiKickDip = false;
        isAfterAntiKickDip = false;
    }

    private void updateAntiKickState() {
        FlyHackConfig config = ConfigStore.instance.getConfig().flyHackConfig;

        // Reset flags from previous tick
        if (isAfterAntiKickDip) {
            isAfterAntiKickDip = false;
        }
        if (isPerformingAntiKickDip) {
            isPerformingAntiKickDip = false; // Dip only lasts one tick
            isAfterAntiKickDip = true;       // Next tick will be "after dip"
        }

        if (!config.enabled || !config.antiKickEnabled) {
            antiKickTickCounter = 0;
            return;
        }

        antiKickTickCounter++;

        if (antiKickTickCounter >= config.antiKickInterval) {
            isPerformingAntiKickDip = true; // Schedule dip for the current tick
            antiKickTickCounter = 0;
        }
    }

    // Called by MixinLocalPlayer.onBeforeAiStep
    public boolean shouldBypassVanillaFlightLogic(LocalPlayer player, FlyHackConfig config) {
        // If we are performing the dip or in the immediate recovery tick, bypass vanilla flight
        return isPerformingAntiKickDip || isAfterAntiKickDip;
    }
    
    public void applyAntiKickMotion(LocalPlayer player, FlyHackConfig config) {
        if (isPerformingAntiKickDip) {
            Vec3 currentVel = player.getDeltaMovement();
            player.setDeltaMovement(currentVel.x, -config.antiKickDistance, currentVel.z);
        }
        // For isAfterAntiKickDip, no specific motion is applied here;
        // vanilla gravity acts because abilities.flying is false.
        // The onGround=true packet will be sent.
    }

    private void onClientPacket(NetworkPacketsController.ClientPacketArgs args) {
        if (args.packet instanceof ServerboundMovePlayerPacket packet) {
            FlyHackConfig config = ConfigStore.instance.getConfig().flyHackConfig;
            if (config.enabled) { // Only if FlyHack (the parent feature) is on
                if (config.antiKickEnabled) { // And anti-kick is on
                    if (isPerformingAntiKickDip) {
                        // During the downward dip of anti-kick, set onGround to false.
                        ((ServerboundMovePlayerPacketAccessor) packet).setOnGround_CU(false);
                        return; // Anti-kick takes precedence for onGround flag this tick
                    } else if (isAfterAntiKickDip) {
                        // Tick immediately after the dip, simulate landing.
                        ((ServerboundMovePlayerPacketAccessor) packet).setOnGround_CU(true);
                        return; // Anti-kick takes precedence for onGround flag this tick
                    }
                }
                // If not in an anti-kick maneuver, or anti-kick is off, use the general onGroundFlag.
                ((ServerboundMovePlayerPacketAccessor) packet).setOnGround_CU(config.onGroundFlag);
            }
        }
    }
}