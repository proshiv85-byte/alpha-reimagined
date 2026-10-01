package net.minecraft.src;

import net.minecraft.server.MinecraftServer;

public final class ThreadServerApplication extends Thread {
	final MinecraftServer mcServer;

	public ThreadServerApplication(String var1, MinecraftServer var2) {
		// World generation lights terrain recursively; with the default thread
		// stack some worlds overflow it and the server silently exits.
		super((ThreadGroup)null, (Runnable)null, var1, 64L * 1024L * 1024L);
		this.mcServer = var2;
	}

	public void run() {
		this.mcServer.run();
	}
}
