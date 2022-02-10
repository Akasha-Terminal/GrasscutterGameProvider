package io.github.pseudodistant.provider.services;

import io.github.pseudodistant.provider.patch.ExampleEntrypointPatch;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipFile;

public class ExampleGameProvider implements GameProvider {

	/* Define our entrypoint classes, we will be using these to allow ModInitializer to work, as well as to start the game.
	 * (At least one of these should have the main method, so that the game can start.)
	 */
	private static final String[] ENTRYPOINTS = new String[]{"com.mojang.mario.FullScreenFrameLauncher"};
	// Set our game's arguments (This variable isn't necessary, but makes the process a lot easier).
	private static final Set<String> SENSITIVE_ARGS = new HashSet<>(Arrays.asList(
			// List of all of our arguments, all lowercase, and without --
			"list",
			"of",
			"game",
			"arguments"));
	
	private Arguments arguments;
	private String entrypoint;
	private Path launchDir;
	private Path libDir;
	private Path gameJar;
	private boolean development = false;
	private final List<Path> miscGameLibraries = new ArrayList<>();
	private static final StringVersion gameVersion = new StringVersion("1.0.0");

	// Apply our patches, for the sake of incorporating ModInitializer hooks, or to patch branding.
	private static final GameTransformer TRANSFORMER = new GameTransformer(
			new ExampleEntrypointPatch());
	
	@Override
	// Fabric GameProvider method for setting the modid for the game (For Minecraft, this is `minecraft`).
	public String getGameId() {
		return "example-game";
	}

	@Override
	// Fabric GameProvider method for setting the pretty name for the game (The ones that ModMenu likes to use).
	public String getGameName() {
		return "Example Game";
	}

	@Override
	// Set the version string of the game, simple as that.
	public String getRawGameVersion() {
		return gameVersion.getFriendlyString();
	}

	@Override
	// Set a SemVer-compliant string so that mods can see if they're compatible with the version being loaded.
	public String getNormalizedGameVersion() {
		return getRawGameVersion();
	}

	@Override
	/* This is where we actually set the game's metadata, including the modid, the version, the author, and any
	 * other relevant metadata to the game.
	 */
	public Collection<BuiltinMod> getBuiltinMods() {
		HashMap<String, String> exampleContactInformation = new HashMap<>();
		exampleContactInformation.put("homepage", "https://insert.website.here/");
		exampleContactInformation.put("wiki", "https://insert.website.here/wiki/");
		exampleContactInformation.put("issues", "idk some issue link");

		BuiltinModMetadata.Builder exampleMetadata =
				new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
				.setName(getGameName())
				.addAuthor("ExampleAuthor", exampleContactInformation)
				.setContact(new ContactInformationImpl(exampleContactInformation))
				.setDescription("A very brief, yet informative, description of the game.");


		return Collections.singletonList(new BuiltinMod(Collections.singletonList(gameJar), exampleMetadata.build()));
	}

	@Override
	// Getter for entrypoint.
	public String getEntrypoint() {
		return entrypoint;
	}

	@Override
	/* Get the game's launch directory. This is especially useful if the game has a launcher that
	 * launches it from a specific directory, like Minecraft.
	 * For any game that's run with a `java -jar` command, we can usually just set it to the current working
	 * directory, which can be called with "." or just a filename like "game.jar"
	 */
	public Path getLaunchDirectory() {
		if (arguments == null) {
			return Paths.get(".");
		}
		return getLaunchDirectory(arguments);
	}

	@Override
	// Is the game obfuscated, as in does it need an intermediary?
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
	/* Where is the game's Jar file?
	 * This is needed because instead of launching the game, you're actually launching Fabric (Knot, specifically).
	 * Fabric needs to know where the game is so Fabric can actually start it.
	 */
	public boolean locateGame(FabricLauncher launcher, String[] args) {
		this.arguments = new Arguments();
		arguments.parse(args);
		
		Map<Path, ZipFile> zipFiles = new HashMap<>();
		
		if(Objects.equals(System.getProperty(SystemProperties.DEVELOPMENT), "true")) {
			development = true;
		}
		
		try {
			String gameJarProperty = System.getProperty(SystemProperties.GAME_JAR_PATH);
			GameProviderHelper.FindResult result = null;
			if(gameJarProperty == null) {
				gameJarProperty = "./game.jar";
			}
			if(gameJarProperty != null) {
				Path path = Paths.get(gameJarProperty);
				if (!Files.exists(path)) {
					throw new RuntimeException("Game jar configured through " + SystemProperties.GAME_JAR_PATH + " system property doesn't exist");
				}

				result = GameProviderHelper.findFirst(Collections.singletonList(path), zipFiles, true, ENTRYPOINTS);
			}
			
			if(result == null) {
				return false;
			}
			
			entrypoint = result.name;
			gameJar = result.path;

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		processArgumentMap(arguments);

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
	// Add the game to the classpath, as well as any of the game's dependencies.
	public void unlockClassPath(FabricLauncher launcher) {
		launcher.addToClassPath(gameJar);
		
		for(Path lib : miscGameLibraries) {
			launcher.addToClassPath(lib);
		}
	}

	@Override
	// Start the game using Fabric Loader.
	public void launch(ClassLoader loader) {
		String targetClass = entrypoint;
		
		try {
			Class<?> c = loader.loadClass(targetClass);
			Method m = c.getMethod("main", String[].class);
			m.invoke(null, (Object) arguments.toArray());
		}
		catch(InvocationTargetException e) {
			throw new FormattedException("The game has crashed!", e.getCause());
		}
		catch(ReflectiveOperationException e) {
			throw new FormattedException("Failed to start the game", e);
		}
	}

	@Override
	// Getter for arguments.
	public Arguments getArguments() {
		return arguments;
	}

	@Override
	/* Gets the arguments being passed to Fabric Loader, so for
	 * ... net.fabricmc.loader.launch.knot.KnotClient --debug true
	 * the state of --debug can be called from here.
	 */
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
}
