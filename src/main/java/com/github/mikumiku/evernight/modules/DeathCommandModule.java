package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.NumberSetting;
import evernight.client.settings.types.StringSetting;
import evernight.client.utils.chat.ChatUtils;

/** Runs a chat command after dying. Ported from {@code DeathAutoCommand}. */
public final class DeathCommandModule extends MikuModule {
    private final Setting<String> command;
    private final Setting<Integer> delay;

    private boolean dead;
    private long deathTime;

    public DeathCommandModule() {
        super("DeathCommand", "Sends a chat command after you die.", MikuCategories.MIKU, "mikudeathcmd");
        this.command = this.add(new StringSetting("Command", "/spawn"));
        this.delay = this.add(new NumberSetting<>("Delay", 1500, 0, 10000));
    }

    @Override
    public void onEnable() {
        this.dead = false;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        boolean dying = mc().player.isDeadOrDying() || mc().player.getHealth() <= 0.0F;
        if (dying && !this.dead) {
            this.dead = true;
            this.deathTime = System.currentTimeMillis();
            return;
        }
        if (!this.dead) {
            return;
        }
        if (System.currentTimeMillis() - this.deathTime < this.delay.getValue()) {
            return;
        }
        String text = this.command.getValue();
        if (text != null && !text.isBlank()) {
            ChatUtils.sendChatOrCommand(text);
        }
        this.dead = false;
    }
}
