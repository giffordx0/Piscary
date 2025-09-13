package com.chunksmith.piscary;

import com.chunksmith.piscary.commands.FishCommand;
import com.chunksmith.piscary.commands.FtCommand;
import com.chunksmith.piscary.integrations.PiscaryPlaceholders;
import com.chunksmith.piscary.listeners.FishingListener;
import com.chunksmith.piscary.listeners.TotemListener;
import com.chunksmith.piscary.listeners.CrabListener;
import com.chunksmith.piscary.services.*;
import com.chunksmith.piscary.util.Msg;
import org.bukkit.plugin.java.JavaPlugin;

public class Piscary extends JavaPlugin {

    private ConfigService configService;
    private LangService langService;
    private FishService fishService;
    private EconomyService economyService;
    private LevelService levelService;
    private EntropyService entropyService;
    private BaitService baitService;
    private ScalesService scalesService;
    private DeliveriesService deliveriesService;
    private TournamentService tournamentService;
    private AugmentService augmentService;
    private BagService bagService;
    private TotemService totemService;
    private CrabsService crabsService;
    private SeasonalService seasonalService;
    private MenuService menuService;

    @Override
    public void onEnable() {
        // Seed defaults
        saveDefault("config.yml");
        saveDefault("messages.yml");
        saveDefault("lang/en.yml");
        saveDefault("menus/main.yml");
        saveDefault("menus/shop.yml");
        saveDefault("menus/bag.yml");
        saveDefault("menus/gut.yml");
        saveDefault("menus/scales.yml");
        saveDefault("menus/deliveries.yml");
        saveDefault("menus/augment.yml");
        saveDefault("menus/augments.yml");
        saveDefault("menus/totems.yml");
        saveDefault("menus/themes/dark-neon.yml");
        saveDefault("fish/index.yml");
        saveDefault("fish/sets/ocean.yml");
        saveDefault("baits.yml");
        saveDefault("deliveries.yml");
        saveDefault("tournaments.yml");
        saveDefault("totems.yml");
        saveDefault("crabs.yml");
        saveDefault("seasons.yml"); // NEW

        // Services
        this.configService = new ConfigService(this);
        this.langService = new LangService(this);
        this.fishService = new FishService(this);
        this.economyService = new EconomyService(this);
        this.levelService = new LevelService(this);      // UPDATED (includes Seasonal & Totem multipliers)
        this.entropyService = new EntropyService(this);
        this.baitService = new BaitService(this);
        this.scalesService = new ScalesService(this);
        this.deliveriesService = new DeliveriesService(this);
        this.tournamentService = new TournamentService(this);
        this.augmentService = new AugmentService(this);
        this.bagService = new BagService(this);
        this.totemService = new TotemService(this);
        this.crabsService = new CrabsService(this);
        this.seasonalService = new SeasonalService(this); // NEW
        this.menuService = new MenuService(this);

        Msg.init(this, langService);

        // Listeners
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        getServer().getPluginManager().registerEvents(new TotemListener(this), this);
        getServer().getPluginManager().registerEvents(new CrabListener(this), this);
        getServer().getPluginManager().registerEvents(menuService, this);

        // Commands
        FishCommand fishCmd = new FishCommand(this);
        getCommand("fish").setExecutor(fishCmd);
        getCommand("fish").setTabCompleter(fishCmd);
        getCommand("ft").setExecutor(new FtCommand(this));

        // PlaceholderAPI
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PiscaryPlaceholders(this).register();
            getLogger().info("PlaceholderAPI: %piscary_*% registered.");
        }

        getLogger().info("Piscary enabled.");
    }

    @Override
    public void onDisable() {
        if (tournamentService != null) tournamentService.shutdown();
        if (totemService != null) totemService.saveAll();
        if (crabsService != null) crabsService.shutdown();
        if (seasonalService != null) seasonalService.shutdown();
        getLogger().info("Piscary disabled.");
    }

    public ConfigService config() { return configService; }
    public LangService lang() { return langService; }
    public FishService fish() { return fishService; }
    public EconomyService eco() { return economyService; }
    public LevelService level() { return levelService; }
    public EntropyService entropy() { return entropyService; }
    public BaitService bait() { return baitService; }
    public ScalesService scales() { return scalesService; }
    public DeliveriesService deliveries() { return deliveriesService; }
    public TournamentService tournaments() { return tournamentService; }
    public AugmentService augments() { return augmentService; }
    public BagService bag() { return bagService; }
    public TotemService totems() { return totemService; }
    public CrabsService crabs() { return crabsService; }
    public SeasonalService seasons() { return seasonalService; } // NEW
    public MenuService menus() { return menuService; }

    private void saveDefault(String path) {
        if (getResource(path) != null) saveResource(path, false);
    }
}
