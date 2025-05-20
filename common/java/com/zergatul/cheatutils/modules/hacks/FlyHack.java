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
    private boolean shouldFall = false;
    private boolean isFalling = false;
    private double originalY;

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
            shouldFall = false;
            isFalling = false;
            return;
        }
        
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        if (isFalling) {
            player.setPos(player.getX(), originalY, player.getZ());
            isFalling = false;
            return;
        }
        
        tickCounter++;
        if (tickCounter >= config.antiKickInterval) {
            tickCounter = 0;
            applyAntiKick(config);
        }
    }
    
    private void applyAntiKick(FlyHackConfig config) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        originalY = player.getY();
        player.setPos(player.getX(), originalY - config.antiKickDistance, player.getZ());
        isFalling = true;
    }
}