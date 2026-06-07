package com.pumpworks;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(PumpworksMod.ID)
public class PumpworksMod {

    public static final String ID = "pumpworks";
    public static final String NAME = "Create Pumpworks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PumpworksMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing {}", NAME);
    }
}
