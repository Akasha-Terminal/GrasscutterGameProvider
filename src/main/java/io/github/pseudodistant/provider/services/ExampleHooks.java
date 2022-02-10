package io.github.pseudodistant.provider.services;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.entrypoint.EntrypointUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ExampleHooks {
    public static final String INTERNAL_NAME = ExampleHooks.class.getName().replace('.', '/');
    /** This hook runs Fabric's ModInitializer.onInitialize() from where it is called.
     *  It's recommended that you call them from as late into the game's execution as you can while still being before the game loop,
     *  to allow ModInitializer to allow as many game alterations as possible.
     */
    public static void init() {
        Path runDir = Paths.get(".");

        FabricLoaderImpl.INSTANCE.prepareModInit(runDir, FabricLoaderImpl.INSTANCE.getGameInstance());
        EntrypointUtils.invoke("main", ModInitializer.class, ModInitializer::onInitialize);
        if (FabricLoaderImpl.INSTANCE.getEnvironmentType() == EnvType.CLIENT) {
            EntrypointUtils.invoke("client", ClientModInitializer.class, ClientModInitializer::onInitializeClient);
        } else {
            EntrypointUtils.invoke("server", DedicatedServerModInitializer.class, DedicatedServerModInitializer::onInitializeServer);
        }
    }
}
