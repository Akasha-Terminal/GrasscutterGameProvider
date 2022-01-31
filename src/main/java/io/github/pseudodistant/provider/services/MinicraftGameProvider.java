package io.github.pseudodistant.provider.services;

import io.github.pseudodistant.provider.patch.MiniEntrypointPatch;
import io.github.pseudodistant.provider.patch.MinicraftPatch;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.version.StringVersion;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipFile;

public class MinicraftGameProvider implements GameProvider {

	private static final String[] ENTRYPOINTS = new String[]{"com.mojang.ld22.Game", "minicraft.core.Game"};
	private static final HashSet<String> SENSITIVE_ARGS = new HashSet<String>(Arrays.asList(new String[] {}));
	
	private Arguments arguments;
	private String entrypoint;
	private Path launchDir;
	private Path libDir;
	private Path gameJar;
	private boolean development = false;
	private boolean isPlus = false;
	private final List<Path> miscGameLibraries = new ArrayList<>();
	
	private static final GameTransformer TRANSFORMER = new GameTransformer(
			new MinicraftPatch(),
			new MiniEntrypointPatch());
	
	@Override
	public String getGameId() {
		return "minicraft";
	}

	@Override
	public String getGameName() {
		return "Minicraft";
	}

	@Override
	public String getRawGameVersion() {
		return getGameVersion().getFriendlyString();
	}

	@Override
	public String getNormalizedGameVersion() {
		return getRawGameVersion();
	}

	@Override
	public Collection<BuiltinMod> getBuiltinMods() {
		
		HashMap<String, String> minicraftContactInformation = new HashMap<>();
		minicraftContactInformation.put("homepage", "https://PLACEHOLDER");
		minicraftContactInformation.put("issues", "https://discord.gg/MsQdqe52yT");
		
		BuiltinModMetadata.Builder minicraftMetaData =
				new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
				.setName(getGameName())
				.addAuthor("Notch", minicraftContactInformation)
				.setContact(new ContactInformationImpl(minicraftContactInformation))
				.setDescription("A 2D top-down action game designed and programmed by Markus Persson, the creator of Minecraft, for a Ludum Dare, a 48-hour game programming competition.");
		
		return Collections.singletonList(new BuiltinMod(gameJar, minicraftMetaData.build()));
	}

	@Override
	public String getEntrypoint() {
		return entrypoint;
	}

	@Override
	public Path getLaunchDirectory() {
		if (arguments == null) {
			return Paths.get(".");
		}
		
		return getLaunchDirectory(arguments);
	}

	@Override
	public boolean isObfuscated() {
		return false;
	}

	@Override
	public boolean requiresUrlClassLoader() {
		return false;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean locateGame(FabricLauncher launcher, String[] args) {
		this.arguments = new Arguments();
		arguments.parse(args);
		
		Map<Path, ZipFile> zipFiles = new HashMap<>();
		List<Path> lookupPaths = new ArrayList<>();
		
		if(System.getProperty(SystemProperties.DEVELOPMENT) == "true") {
			development = true;
		}
		
		try {
			String gameJarProperty = System.getProperty(SystemProperties.GAME_JAR_PATH);
			GameProviderHelper.FindResult result = null;
			if(gameJarProperty == null) {
				gameJarProperty = "./jars/minicraft.jar";
			}
			if(gameJarProperty != null) {
				Path path = Paths.get(gameJarProperty);
				if (!Files.exists(path)) {
					throw new RuntimeException("Game jar configured through " + SystemProperties.GAME_JAR_PATH + " system property doesn't exist");
				}
				lookupPaths.add(path);

				result = GameProviderHelper.findFirst(Collections.singletonList(path), zipFiles, true, ENTRYPOINTS);
			}
			
			if(result == null) {
				return false;
			}
			
			entrypoint = result.name;
			gameJar = result.path;
			
		}
		finally {
			for(ZipFile f : zipFiles.values()) {
				try {
					f.close();
				}
				catch(IOException e) {
					//ignore
				}
			}
		}
		
		processArgumentMap(arguments);
		
		//locateFilesystemDependencies();
		
		return true;
		
	}

	@Override
	public void initialize(FabricLauncher launcher) {
		TRANSFORMER.locateEntrypoints(launcher, gameJar);
	}

	@Override
	public GameTransformer getEntrypointTransformer() {
		return TRANSFORMER;
	}

	@Override
	public void unlockClassPath(FabricLauncher launcher) {
		launcher.addToClassPath(gameJar);
		
		for(Path lib : miscGameLibraries) {
			launcher.addToClassPath(lib);
		}
	}

	@Override
	public void launch(ClassLoader loader) {
		String targetClass = entrypoint;
		
		try {
			Class<?> c = loader.loadClass(targetClass);
			Method m = c.getMethod("main", String[].class);
			m.invoke(null, (Object) arguments.toArray());
		}
		catch(InvocationTargetException e) {
			throw new FormattedException("Minicraft has crashed!", e.getCause());
		}
		catch(ReflectiveOperationException e) {
			throw new FormattedException("Failed to start Minicraft", e);
		}
	}

	@Override
	public Arguments getArguments() {
		return arguments;
	}

	@Override
	public String[] getLaunchArguments(boolean sanitize) {
		if (arguments == null) return new String[0];

		String[] ret = arguments.toArray();
		if (!sanitize) return ret;

		int writeIdx = 0;

		for (int i = 0; i < ret.length; i++) {
			String arg = ret[i];

			if (i + 1 < ret.length
					&& arg.startsWith("--")
					&& SENSITIVE_ARGS.contains(arg.substring(2).toLowerCase(Locale.ENGLISH))) {
				i++; // skip value
			} else {
				ret[writeIdx++] = arg;
			}
		}

		if (writeIdx < ret.length) ret = Arrays.copyOf(ret, writeIdx);

		return ret;
	}
	
	private void processArgumentMap(Arguments arguments) {
		if (!arguments.containsKey("gameDir")) {
			arguments.put("gameDir", getLaunchDirectory(arguments).toAbsolutePath().normalize().toString());
		}
		
		launchDir = Path.of(arguments.get("gameDir"));
		System.out.println("Launch directory is " + launchDir);
		libDir = launchDir.resolve(Path.of("./lib"));
	}
	
	private static Path getLaunchDirectory(Arguments arguments) {
		return Paths.get(arguments.getOrDefault("gameDir", "."));
	}
	
	private StringVersion getGameVersion() {
		return new StringVersion("1.0.0");
	}

}
