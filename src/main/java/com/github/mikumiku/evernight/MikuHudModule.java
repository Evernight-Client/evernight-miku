package com.github.mikumiku.evernight;

import evernight.client.modules.HudWidgetModule;
import evernight.client.render.text.Translations;
import evernight.client.settings.core.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Minecraft;

/**
 * Base class for Miku HUD widgets.
 *
 * <p>{@link HudWidgetModule} cannot extend {@link evernight.addon.AddonModule} (Java has no multiple
 * inheritance), so this class repeats the addon namespacing: display name, description and setting
 * labels are all resolved from {@code addons.mikumiku.*} instead of the client namespace.
 */
public abstract class MikuHudModule extends HudWidgetModule {
    protected MikuHudModule(String name, String... aliases) {
        super(name, aliases);
    }

    protected static Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public String getDisplayName() {
        return Translations.get("addons.mikumiku.names.modules." + this.getName());
    }

    @Override
    public String getDescription() {
        return Translations.get("addons.mikumiku.descriptions.modules." + this.getName());
    }

    @Override
    public <S extends Setting<?>> S add(S setting) {
        setting.setDisplayNameKey("addons.mikumiku.names.settings." + setting.getName());
        return super.add(setting);
    }

    @Override
    public <S extends Setting<?>> S add(S setting, Setting<?> precedingSetting) {
        setting.setDisplayNameKey("addons.mikumiku.names.settings." + setting.getName());
        return super.add(setting, precedingSetting);
    }
}
