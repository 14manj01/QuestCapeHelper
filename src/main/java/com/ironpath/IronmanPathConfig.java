package com.ironpath;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ironmanpath")
public interface IronmanPathConfig extends Config
{
    @ConfigItem(
            keyName = "showActiveStepOverlay",
            name = "Show active step overlay",
            description = "Show the current active step (next unfinished route step) as an overlay in the top-right of the game view."
    )
    default boolean showActiveStepOverlay()
    {
        return false;
    }
}
