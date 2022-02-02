package io.github.pseudodistant.provider.patch;

import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Consumer;
import java.util.function.Function;

public class MinicraftPatch extends GamePatch {
    @Override
    public void process(FabricLauncher launcher, Function<String, ClassReader> classSource, Consumer<ClassNode> classEmitter) {

    }
}
