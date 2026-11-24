package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;
import evernight.client.settings.types.NumberSetting;
import evernight.client.settings.types.StringSetting;
import evernight.client.utils.chat.ChatUtils;
import net.minecraft.client.multiplayer.ClientLevel;

/** Sends a login command once per world join. Ported from {@code AutoLoginPlus}. */
public final class AutoLoginModule extends MikuModule {
    private final Setting<String> command;
    private final Setting<Integer> delay;
    private final Setting<Boolean> onlyOnce;

    private ClientLevel lastLevel;
    private int ticks;
    private boolean pending;
    private boolean sent;

    public AutoLoginModule() {
        super("AutoLoginPlus", "Sends /login with your password after joining a server.",
                MikuCategories.MIKU, "mikulogin");
        this.command = this.add(new StringSetting("Command", "/login {password}"));
        this.delay = this.add(new NumberSetting<>("Delay", 40, 0, 200));
        this.onlyOnce = this.add(new BooleanSetting("OnlyOnce", true));
    }

    @Override
    public void onEnable() {
        this.lastLevel = null;
        this.pending = false;
        this.sent = false;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            this.lastLevel = null;
            return;
        }
        if (mc().level != this.lastLevel) {
            this.lastLevel = mc().level;
            this.pending = !(this.onlyOnce.getValue() && this.sent);
            this.ticks = 0;
        }
        if (!this.pending) {
            return;
        }
        if (++this.ticks < this.delay.getValue()) {
            return;
        }
        String text = this.command.getValue();
        if (text == null || text.isBlank()) {
            this.pending = false;
            return;
        }
        ChatUtils.sendChatOrCommand(text);
        this.pending = false;
        this.sent = true;
    }
}
