package com.jarvis.addon;

import com.jarvis.addon.modules.JarvisAlerts;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class JarvisAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Jarvis");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Jarvis Alerts");
        Modules.get().add(new JarvisAlerts());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    // Must match the root package of the addon, otherwise @EventHandler methods won't work.
    @Override
    public String getPackage() {
        return "com.jarvis.addon";
    }
}
