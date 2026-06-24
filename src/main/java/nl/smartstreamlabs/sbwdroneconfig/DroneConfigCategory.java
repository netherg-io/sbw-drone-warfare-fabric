package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.network.chat.Component;

public enum DroneConfigCategory {
    DRONE("screen.sbwdroneconfig.config.category.drone", "screen.sbwdroneconfig.config.description.drone"),
    LUCAS_FLIGHT("screen.sbwdroneconfig.config.category.lucas_flight", "screen.sbwdroneconfig.config.description.lucas_flight"),
    FUEL("screen.sbwdroneconfig.config.category.fuel", "screen.sbwdroneconfig.config.description.fuel"),
    BATTERY("screen.sbwdroneconfig.config.category.battery", "screen.sbwdroneconfig.config.description.battery"),
    JAMMER("screen.sbwdroneconfig.config.category.jammer", "screen.sbwdroneconfig.config.description.jammer"),
    WEATHER("screen.sbwdroneconfig.config.category.weather", "screen.sbwdroneconfig.config.description.weather"),
    SPOTLIGHT("screen.sbwdroneconfig.config.category.spotlight", "screen.sbwdroneconfig.config.description.spotlight"),
    FPV_DRONE("screen.sbwdroneconfig.config.category.fpv_drone", "screen.sbwdroneconfig.config.description.fpv_drone");

    private final String titleKey;
    private final String descriptionKey;

    DroneConfigCategory(String titleKey, String descriptionKey) {
        this.titleKey = titleKey;
        this.descriptionKey = descriptionKey;
    }

    public Component title() {
        return Component.translatable(titleKey);
    }

    public Component description() {
        return Component.translatable(descriptionKey);
    }
}
