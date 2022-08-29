package me.cael.pluginfix.mixin;

import emu.grasscutter.plugin.PluginManager;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.List;

@Mixin(PluginManager.class)
public class PluginManagerMixin {

    @Redirect(
            method = "loadPlugins",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/net/URLClassLoader;loadClass(Ljava/lang/String;)Ljava/lang/Class;"
            )
    )
    private Class<?> pluginfix_loadClass(URLClassLoader instance, String s) throws ClassNotFoundException {
        return this.getClass().getClassLoader().loadClass(s);
    }

    @Inject(
            method = "loadPlugins",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/net/URLClassLoader;<init>([Ljava/net/URL;)V",
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void pluginfix_loadPlugins(CallbackInfo ci, File pluginsDir, File[] files, List<File> plugins, URL[] pluginNames) throws URISyntaxException {
        for (URL url: pluginNames) {
            FabricLauncherBase.getLauncher().addToClassPath(Paths.get(url.toURI()));
        }
    }

}
