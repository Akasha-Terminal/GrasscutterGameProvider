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
        if (!entrypoint.startsWith("com.mojang.")) {
            return;
        }
        ClassNode mainClass = readClass(classSource.apply(entrypoint));
        MethodNode initMethod = findMethod(mainClass, (method) -> method.name.equals("init") && method.desc.equals("()V"));

        if (initMethod == null) {
            throw new RuntimeException("Could not find init method in " + entrypoint + "!");
        }
        Log.debug(LogCategory.GAME_PATCH, "Found init method: %s -> %s", entrypoint, mainClass.name);
        Log.debug(LogCategory.GAME_PATCH, "Patching init method %s%s", initMethod.name, initMethod.desc);
        ListIterator<AbstractInsnNode> it = initMethod.instructions.iterator();
        it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MiniHooks.INTERNAL_NAME, initMethod.name, initMethod.desc, false));

        classEmitter.accept(mainClass);
    }
}