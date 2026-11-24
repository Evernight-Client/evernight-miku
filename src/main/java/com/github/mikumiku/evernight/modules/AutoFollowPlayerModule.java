package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import evernight.client.ClientContext;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;
import evernight.client.settings.types.NumberSetting;
import evernight.client.settings.types.StringSetting;
import net.minecraft.world.entity.player.Player;

/** Walks towards another player. Ported from {@code AutoFollowPlayer}. */
public final class AutoFollowPlayerModule extends MikuModule {
    private final Setting<String> playerName;
    private final Setting<Double> distance;
    private final Setting<Boolean> sprint;

    public AutoFollowPlayerModule() {
        super("AutoFollowPlayer", "Follows a player automatically.", MikuCategories.TWEAK, "mikufollow");
        this.playerName = this.add(new StringSetting("Player", ""));
        this.distance = this.add(new NumberSetting<>("Distance", 3.0, 1.0, 10.0));
        this.sprint = this.add(new BooleanSetting("Sprint", true));
    }

    @Override
    public void onDisable() {
        if (ClientContext.inputOverrides != null) {
            ClientContext.inputOverrides.requestReset();
        }
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame() || ClientContext.inputOverrides == null) {
            return;
        }
        Player target = this.findTarget();
        if (target == null) {
            ClientContext.inputOverrides.requestReset();
            return;
        }
        double dx = target.getX() - mc().player.getX();
        double dz = target.getZ() - mc().player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal <= this.distance.getValue()) {
            ClientContext.inputOverrides.requestReset();
            return;
        }
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        mc().player.setYRot(targetYaw);
        mc().player.setYHeadRot(targetYaw);
        ClientContext.inputOverrides.requestForward(true);
        if (this.sprint.getValue()) {
            ClientContext.inputOverrides.requestSprint(true);
        }
    }

    private Player findTarget() {
        String name = this.playerName.getValue();
        if (name == null || name.isBlank()) {
            return null;
        }
        for (Player player : mc().level.players()) {
            if (player != mc().player && player.isAlive()
                    && player.getName().getString().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }
}
