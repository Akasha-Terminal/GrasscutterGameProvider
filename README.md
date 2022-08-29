# GrasscutterGameProvider

Fabric Loader (yes, the Minecraft mod loader) GameProvider for Grasscutter, based on [ExampleGameProvider](https://github.com/PseudoDistant/ExampleGameProvider) by PseudoDistant.

# Why?

I know Grasscutter has its own plugin system, but by using Fabric Loader we can use Mixins to modify GC internals for whatever reason.

# How?

Build with shadow, then run `java -cp "grasscutter.jar;gameprovider.jar" net.fabricmc.loader.impl.launch.knot.KnotServer` instead of `java -jar grasscutter.jar`

If you want to use regular Grasscutter plugins too, you will need to build PluginFix and put it in the mods folder.