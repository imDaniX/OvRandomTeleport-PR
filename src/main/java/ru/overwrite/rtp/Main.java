package ru.overwrite.rtp;

import org.bstats.bukkit.Metrics;

import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

import lombok.Getter;
import ru.overwrite.rtp.utils.Config;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.logging.BukkitLogger;
import ru.overwrite.rtp.utils.logging.PaperLogger;

import java.lang.reflect.Constructor;

public class Main extends JavaPlugin {

	private final Server server = getServer();

	private final Logger logger = Utils.FOLIA ? new PaperLogger(this) : new BukkitLogger(this);
	
	@Getter
	private final Config pluginConfig = new Config();
	
	@Getter 
	private RtpManager rtpManager = new RtpManager(this);
	
	@Getter
	private Economy economy;

	@Override
	public void onEnable() {
		if (!isPaper()) {
			return;
		}
		saveDefaultConfig();
		PluginManager pluginManager = server.getPluginManager();
		FileConfiguration config = getConfig();
		pluginConfig.setupMessages(config);
		ConfigurationSection mainSettings = config.getConfigurationSection("main_settings");
		registerCommand(pluginManager, mainSettings);
		if (mainSettings.getBoolean("enable_metrics")) {
			new Metrics(this, 22021);
		}
		setupEconomy(pluginManager);
		pluginManager.registerEvents(new RtpListener(this), this);
		getServer().getScheduler().runTaskAsynchronously(this, () -> rtpManager.setupChannels(config, pluginManager));
	}
	
	public boolean isPaper() {
		if (server.getName().equals("CraftBukkit")) {
			loggerInfo(" ");
			loggerInfo("§6============= §6! WARNING ! §c=============");
			loggerInfo("§eЭтот плагин работает только на Paper и его форках!");
			loggerInfo("§eАвтор плагина §cкатегорически §eвыступает за отказ от использования устаревшего и уязвимого софта!");
			loggerInfo("§eСкачать Paper: §ahttps://papermc.io/downloads/all");
			loggerInfo("§6============= §6! WARNING ! §c=============");
			loggerInfo(" ");
			this.setEnabled(false);
			return false;
		}
		return true;
	}
	
	private void setupEconomy(PluginManager pluginManager) {
        if (!pluginManager.isPluginEnabled("Vault")) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        economy = rsp.getProvider();
    }

	private void registerCommand(PluginManager pluginManager, ConfigurationSection mainSettings) {
		try {
			CommandMap commandMap = server.getCommandMap();
			Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class,
					Plugin.class);
			constructor.setAccessible(true);
			PluginCommand command = constructor.newInstance(mainSettings.getString("rtp_command"), this);
			command.setExecutor(new RtpCommand(this));
			command.setTabCompleter(new RtpCommand(this));
			commandMap.register(getDescription().getName(), command);
		} catch (Exception e) {
			logger.info("Unable to register password command!");
			e.printStackTrace();
			pluginManager.disablePlugin(this);
		}
	}

	public void loggerInfo(String logMessage) {
		logger.info(logMessage);
	}

}
