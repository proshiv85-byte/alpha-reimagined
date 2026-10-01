import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Starts the survival-patched Minecraft Alpha 1.1.2_01 client.
 *
 * Expects the system property "alphar.home" to point at the unpacked bundle
 * (lib/, natives/, assets/). Shows a small dialog for the username and an
 * optional server to join, remembers the answers, then hands over to the
 * RetroMCP launchwrapper in the same JVM.
 *
 * Command-line options skip the dialog:
 *   --username NAME [--server HOST[:PORT]]
 */
public class AlpharLauncher {
	private static final int DEFAULT_PORT = 25565;

	public static void main(String[] args) throws Exception {
		File home = new File(System.getProperty("alphar.home", ".")).getAbsoluteFile();
		File gameDir = gameDirectory();
		gameDir.mkdirs();
		File settingsFile = new File(gameDir, "launcher.properties");
		Properties settings = new Properties();
		if(settingsFile.isFile()) {
			try(InputStream in = new FileInputStream(settingsFile)) {
				settings.load(in);
			}
		}

		String username = null;
		String server = null;
		for(int i = 0; i + 1 < args.length; i++) {
			if(args[i].equals("--username")) {
				username = args[++i];
			} else if(args[i].equals("--server")) {
				server = args[++i];
			}
		}

		if(username == null) {
			String[] answer = askUser(settings.getProperty("username", ""), settings.getProperty("server", ""));
			if(answer == null) {
				System.exit(0);
			}

			username = answer[0];
			server = answer[1];
			settings.setProperty("username", username);
			settings.setProperty("server", server);
			try(OutputStream out = new FileOutputStream(settingsFile)) {
				settings.store(out, "Alpha survival launcher");
			}
		}

		String natives = new File(home, "natives").getPath();
		System.setProperty("org.lwjgl.librarypath", natives);
		System.setProperty("net.java.games.input.librarypath", natives);

		List<String> launch = new ArrayList<String>();
		add(launch, "--username", username);
		add(launch, "--session", "-");
		add(launch, "--version", "a1.1.2_01");
		add(launch, "--gameDir", gameDir.getPath());
		add(launch, "--assetsDir", new File(home, "assets").getPath());
		add(launch, "--assetIndex", "a1.0.14");
		if(server != null && server.trim().length() > 0) {
			String host = server.trim();
			int port = DEFAULT_PORT;
			int colon = host.lastIndexOf(':');
			if(colon > 0) {
				try {
					port = Integer.parseInt(host.substring(colon + 1));
					host = host.substring(0, colon);
				} catch(NumberFormatException e) {
					// not a port, treat the whole thing as the host name
				}
			}

			add(launch, "--server", host);
			add(launch, "--port", Integer.toString(port));
		}

		org.mcphackers.launchwrapper.Launch.main(launch.toArray(new String[0]));
	}

	private static void add(List<String> list, String key, String value) {
		list.add(key);
		list.add(value);
	}

	static File gameDirectory() {
		String os = System.getProperty("os.name").toLowerCase();
		String appData = System.getenv("APPDATA");
		if(os.contains("win") && appData != null) {
			return new File(appData, ".alphar");
		}

		return new File(System.getProperty("user.home"), ".alphar");
	}

	private static String[] askUser(final String lastName, final String lastServer) throws Exception {
		final String[][] result = new String[1][];
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch(Exception e) {
					// default look and feel is fine
				}

				String title = "Minecraft Alpha 1.1.2_01 - Survival";
				if(System.getProperty("alphar.debug") != null) {
					title = title + " (TEST: N = noclip, needs op)";
				}

				final JDialog dialog = new JDialog((java.awt.Frame)null, title, true);
				final JTextField name = new JTextField(lastName, 18);
				final JTextField server = new JTextField(lastServer, 18);
				final JButton play = new JButton("Play");
				final AtomicBoolean accepted = new AtomicBoolean(false);

				JPanel form = new JPanel(new GridBagLayout());
				form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
				GridBagConstraints c = new GridBagConstraints();
				c.insets = new Insets(4, 4, 4, 4);
				c.anchor = GridBagConstraints.WEST;
				c.gridx = 0; c.gridy = 0;
				form.add(new JLabel("Username:"), c);
				c.gridx = 1;
				form.add(name, c);
				c.gridx = 0; c.gridy = 1;
				form.add(new JLabel("Server (optional):"), c);
				c.gridx = 1;
				form.add(server, c);
				c.gridx = 1; c.gridy = 2;
				JLabel hint = new JLabel("host or host:port - leave empty for the main menu");
				hint.setFont(hint.getFont().deriveFont(hint.getFont().getSize2D() - 1.0F));
				form.add(hint, c);

				JPanel buttons = new JPanel();
				buttons.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
				buttons.add(play);

				play.addActionListener(e -> {
					String n = name.getText().trim();
					if(!n.matches("[A-Za-z0-9_]{1,16}")) {
						JOptionPane.showMessageDialog(dialog, "Username must be 1-16 letters, digits or underscores.", "Invalid username", JOptionPane.WARNING_MESSAGE);
						return;
					}

					accepted.set(true);
					dialog.dispose();
				});

				dialog.getRootPane().setDefaultButton(play);
				dialog.getContentPane().add(form, BorderLayout.CENTER);
				dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
				dialog.pack();
				dialog.setResizable(false);
				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);

				if(accepted.get()) {
					result[0] = new String[]{name.getText().trim(), server.getText().trim()};
				}
			}
		});
		return result[0];
	}
}
