package de.tp971.splitscreen;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;

import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Version;

public class Main {

	public static void main(String[] args) throws IOException {
		initGst(args);

		var p = new ProcessBuilder(
				Main.findExecutable("splitscreen-cli").toString(),
				"list-encoders"
			)
			.redirectInput(Redirect.PIPE)
			.redirectOutput(Redirect.PIPE)
			.redirectError(Redirect.INHERIT)
			.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

		var encoders = new ArrayList<Encoder>();
		String line;
		while((line = reader.readLine()) != null) {
			var split = line.split("\t");
			var name = split[0];
			var desc = split[1];
			var def = split.length == 3 && split[2].equals("default");
			encoders.add(new Encoder(name, desc, def));
		}

		SwingUtilities.invokeLater(() -> {
			FlatDarkLaf.setup();
			new MainWindow(encoders).setVisible(true);
		});
	}
	
	public static Path findExecutable(String name) {
		var execPath = getExecPath();
		List<Path> paths = List.of(
			execPath.resolve(name),
			execPath.resolve(name + ".exe"),
        	Path.of(".", name),
        	Path.of(".", name + ".exe")
		);
		for(Path path : paths)
			if(path.toFile().canExecute())
				return path;
		return Path.of(name);
	}
	
	private static void initGst(String[] args) {
        if (Platform.isWindows()) {
            String gstPath = System.getProperty("gstreamer.path");

            if(gstPath == null) {
				Path path = getExecPath()
					.resolve("gstreamer")
					.resolve("bin");
				if(path.toFile().exists())
					gstPath = path.toString();
            }

			if(gstPath == null && Platform.is64Bit()) {
				List<String> vars = List.of(
						"GSTREAMER_1_0_ROOT_MSVC_X86_64",
						"GSTREAMER_1_0_ROOT_MINGW_X86_64",
						"GSTREAMER_1_0_ROOT_X86_64");
				for(String next : vars) {
					String val = System.getenv(next);
					if(val != null) {
						gstPath = val.endsWith("\\") ? val + "bin\\" : val + "\\bin\\";
						break;
					}
				}
			}

            if(gstPath != null) {
                String systemPath = System.getenv("PATH");
				Kernel32.INSTANCE.SetEnvironmentVariable("PATH",
					systemPath == null || systemPath.isEmpty() ?
						gstPath : gstPath + File.pathSeparator + systemPath);
            }

        } else if (Platform.isMac()) {
            String gstPath = System.getProperty("gstreamer.path",
				"/Library/Frameworks/GStreamer.framework/Libraries/");
			String jnaPath = System.getProperty("jna.library.path", "").trim();
			System.setProperty("jna.library.path",
				jnaPath.isEmpty() ? gstPath : jnaPath + File.pathSeparator + gstPath);
        }

		Gst.init(Version.BASELINE, "SplitScreen", args);
	}
	
	private static Path getExecPath() {
		try {
			return Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().getParent();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

}
