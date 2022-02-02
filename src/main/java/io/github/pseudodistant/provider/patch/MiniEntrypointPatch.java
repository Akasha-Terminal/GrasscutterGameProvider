package io.github.pseudodistant.provider.patch;

import io.github.pseudodistant.provider.services.MiniHooks;
import io.github.pseudodistant.provider.services.MinicraftGameProvider;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.version.StringVersion;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class MiniEntrypointPatch extends GamePatch {
    @Override
    public void process(FabricLauncher launcher, Function<String, ClassReader> classSource, Consumer<ClassNode> classEmitter) {
        String entrypoint = launcher.getEntrypoint();
        if (!entrypoint.startsWith("com.mojang.") && !entrypoint.startsWith("minicraft.")) {
            return;
        }
        ClassNode mainClass = readClass(classSource.apply(entrypoint));
        ClassNode plusInitializer = readClass(classSource.apply("minicraft.core.Initializer"));

        MethodNode initMethod;

        if (plusInitializer == null) {
            if (!try202Version(mainClass)) {
                ClassNode titleMenuNode = readClass(classSource.apply("minicraft.screen.TitleMenu"));
                ClassNode oldTitleMenuNode = readClass(classSource.apply("com.mojang.ld22.screen.TitleMenu"));
                try193Version(titleMenuNode == null ? oldTitleMenuNode : titleMenuNode);
            }
            initMethod = findMethod(mainClass, (method) -> method.name.equals("init") && method.desc.equals("()V"));
        } else {
            tryLatestVersion(mainClass);
            initMethod = findMethod(plusInitializer, (method) -> method.name.equals("run") && method.desc.equals("()V"));
        }

        if (initMethod == null) {
            throw new RuntimeException("Could not find init method in " + entrypoint + "!");
        }
        Log.debug(LogCategory.GAME_PATCH, "Found init method: %s -> %s", entrypoint, plusInitializer == null ? mainClass.name : plusInitializer.name);

        Log.debug(LogCategory.GAME_PATCH, "Patching init method %s%s", initMethod.name, initMethod.desc);
        ListIterator<AbstractInsnNode> it = initMethod.instructions.iterator();
        it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MiniHooks.INTERNAL_NAME, "init", "()V", false));
        if (plusInitializer == null) {
            classEmitter.accept(mainClass);
        } else {
            classEmitter.accept(plusInitializer);

        }
    }

    private boolean tryLatestVersion(ClassNode mainClass) {
        for (MethodNode method : mainClass.methods) {
            if (method.name.equals("<clinit>")) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof LdcInsnNode ldcInsnNode) {
                        Object value = ldcInsnNode.cst;
                        if (value instanceof String) {
                            if (((String) value).contains(".")) {
                                MinicraftGameProvider.setGameVersion(new StringVersion((String)value));
                                MinicraftGameProvider.setIsPlus();
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean try202Version(ClassNode mainClass) {
        for (MethodNode method : mainClass.methods) {
            if (method.name.equals("<clinit>")) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof LdcInsnNode ldcInsnNode) {
                        Object value = ldcInsnNode.cst;
                        if ((value.toString()).contains("VER")) {
                            MinicraftGameProvider.setGameVersion(new StringVersion(((String) value).replace("VERSION ", "")));
                            MinicraftGameProvider.setIsPlus();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean try193Version(ClassNode mainClass) {
        for (MethodNode method : mainClass.methods) {
            if (method.name.equals("render")) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof LdcInsnNode ldcInsnNode) {
                        Object value = ldcInsnNode.cst;
                        if ((value.toString()).contains("Ver")) {
                            String version = ((String) value).replace("Version ", "");
                            if (version.contains("2.0.0-dev4")) {
                                version = version.replace("-dev4", "");
                            }
                            MinicraftGameProvider.setGameVersion(new StringVersion(version));
                            MinicraftGameProvider.setIsPlus();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
