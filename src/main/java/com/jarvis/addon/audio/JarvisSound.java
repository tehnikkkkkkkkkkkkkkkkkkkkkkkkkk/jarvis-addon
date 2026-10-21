package com.jarvis.addon.audio;

/** Each constant maps to a WAV file inside the jar: assets/jarvis/sounds/<file>.wav */
public enum JarvisSound {
    TOTEM_POP("totem_pop"),
    HELMET_BREAK("helmet_break"),
    CHESTPLATE_BREAK("chestplate_break"),
    LEGGINGS_BREAK("leggings_break"),
    BOOTS_BREAK("boots_break"),
    LOW_HP("low_hp");

    public final String file;

    JarvisSound(String file) {
        this.file = file;
    }

    public String path() {
        return "/assets/jarvis/sounds/" + file + ".wav";
    }
}
