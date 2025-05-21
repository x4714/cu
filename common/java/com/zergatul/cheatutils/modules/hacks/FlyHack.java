package com.zergatul.cheatutils.modules.hacks;

import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.FlyHackConfig;
import com.zergatul.cheatutils.controllers.NetworkPacketsController;
import com.zergatul.cheatutils.accessors.ServerboundMovePlayerPacketAccessor;
import com.zergatul.cheatutils.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class FlyHack implements Module {

    public static final FlyHack instance = new FlyHack();
    
    private int tickCounter = 0;

    private FlyHack() {
        NetworkPacketsController.instance.addClientPacketHandler(this::onClientPacket);
    }

    private void onClientPacket(NetworkPacketsController.ClientPacketArgs args) {
        if (args.packet instanceof ServerboundMovePlayerPacket packet) {
            FlyHackConfig config = ConfigStore.instance.getConfig().flyHackConfig;
            if (config.enabled) {
                ((ServerboundMovePlayerPacketAccessor) packet).setOnGround_CU(config.onGroundFlag);
            }
        }
    }
    
    public void onTick() {
        FlyHackConfig config = ConfigStore.instance.getConfig().flyHackConfig;
        if (!config.enabled || !config.antiKick) {
            tickCounter = 0;
            return;
        }
        
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        doAntiKick(config);
    }
    
    private void doAntiKick(FlyHackConfig config) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        // Increment counter and reset if past interval
        tickCounter++;
        if (tickCounter > config.antiKickInterval + 1) {
            tickCounter = 0;
        }
        
        // Two-stage anti-kick: first down, then up
        switch (tickCounter) {
            case 0 -> {
                // Don't do anti-kick if player is sneaking
                if (Minecraft.getInstance().options.keyShift.isDown()) {
                    tickCounter = 2; // Skip this cycle
                } else {
                    // Send down movement
                    NetworkPacketsController.instance.sendPacket(
                        new ServerboundMovePlayerPacket.Pos(
                            player.getX(),
                            player.getY() - config.antiKickDistance,
                            player.getZ(),
                            config.onGroundFlag
                        )
                    );
                }
            }
            case 1 -> {
                // Send up movement to counteract the down movement
                NetworkPacketsController.instance.sendPacket(
                    new ServerboundMovePlayerPacket.Pos(
                        player.getX(),
                        player.getY() + config.antiKickDistance,
                        player.getZ(),
                        config.onGroundFlag
                    )
                );
            }
        }
    }
}