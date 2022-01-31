package io.github.pseudodistant.provider.patch;

import io.github.pseudodistant.provider.services.MiniHooks;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class MiniEntrypointPatch extends GamePatch {
    @Override
    public void process(FabricLauncher launcher, Function<String, ClassReader> classSource, Consumer<ClassNode> classEmitter) {
        String entrypoint = launcher.getEntrypoint();
        if (!entrypoint.startsWith("com.mojang.") && !entrypoint.startsWith("minicraft.core.")) {
            return;
        }
        ClassNode mainClass = readClass(classSource.apply(entrypoint));
        ClassNode plusInitializer = readClass(classSource.apply("minicraft.core.Initializer"));
        MethodNode vanillaInitMethod = findMethod(mainClass, (method) -> method.name.equals("init") && method.desc.equals("()V"));
        MethodNode plusInitMethod = findMethod(plusInitializer, (method) -> method.name.equals("run") && method.desc.equals("()V"));
        MethodNode initMethod = vanillaInitMethod != null ? vanillaInitMethod : plusInitMethod;

        if (initMethod == null) {
            throw new RuntimeException("Could not find init method in " + entrypoint + "!");
        }
        if (!(initMethod == plusInitMethod)) {
            Log.debug(LogCategory.GAME_PATCH, "Found init method: %s -> %s", entrypoint, mainClass.name);
        } else {
            Log.debug(LogCategory.GAME_PATCH, "Found init method: %s -> %s", entrypoint, plusInitializer.name);
        }
        Log.debug(LogCategory.GAME_PATCH, "Patching init method %s%s", initMethod.name, initMethod.desc);
        ListIterator<AbstractInsnNode> it = initMethod.instructions.iterator();
        if (initMethod == vanillaInitMethod) {
            it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MiniHooks.INTERNAL_NAME, initMethod.name, initMethod.desc, false));
            classEmitter.accept(mainClass);
        } else {
            it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MiniHooks.INTERNAL_NAME, initMethod.name, initMethod.desc, false));
            classEmitter.accept(plusInitializer);
        }
    }
}