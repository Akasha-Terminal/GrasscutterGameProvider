package io.github.pseudodistant.provider.services;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.entrypoint.EntrypointUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class MiniHooks {
    public static final String INTERNAL_NAME = MiniHooks.class.getName().replace('.', '/');
    public static void init() {
        Path runDir = Paths.get(".");

        FabricLoaderImpl.INSTANCE.prepareModInit(runDir, FabricLoaderImpl.INSTANCE.getGameInstance());
        EntrypointUtils.invoke("main", ModInitializer.class, ModInitializer::onInitialize);
        EntrypointUtils.invoke("client", ClientModInitializer.class, ClientModInitializer::onInitializeClient);
        EntrypointUtils.invoke("server", DedicatedServerModInitializer.class, DedicatedServerModInitializer::onInitializeServer);
    }
}
