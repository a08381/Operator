package cn.windmourn.operator.utils;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class ConfigManager {

    private Configuration config;
    private File configFile;

    private boolean opsOwnerOnStart;
    private boolean registerExtraCommands;
    private boolean creeperHealMode;
    private boolean noFallenOnFarmland;

    private ConfigManager(File file) {
        configFile = file;
        config = new Configuration(file);
        init();
        config.save();
    }

    public static ConfigManager loadConfig(File file) {
        return new ConfigManager(file);
    }

    public Configuration getConfig() {
        return config;
    }

    public void reloadConfig() {
        config = new Configuration(configFile);
        init();
        config.save();
    }

    private void init() {
        opsOwnerOnStart = config.get(Configuration.CATEGORY_GENERAL, "OpsOwnerOnStart", true, "Make yourself be a Operator every server start. Default is true.").getBoolean();
        registerExtraCommands = config.get(Configuration.CATEGORY_GENERAL, "RegisterExtraCommands", true, "Rigister some commands like fly, vanish, etc. Default is true.").getBoolean();
        creeperHealMode = config.get(Configuration.CATEGORY_GENERAL, "CreeperHealMode", false, "Explosion will NEVER break a block. now is unusable. Default is false.").getBoolean();
        noFallenOnFarmland = config.get(Configuration.CATEGORY_GENERAL, "NoFallenOnFarmland", true, "Fallen on farmland will not make it turn back to dirt. Default is true.").getBoolean();
    }

    public void set(String key, String value) {
        config.get(Configuration.CATEGORY_GENERAL, key, value, "netease.");
    }

    public void save() {
        config.save();
    }

    public boolean isOpsOwnerOnStart() {
        return opsOwnerOnStart;
    }

    public boolean isRegisterExtraCommands() {
        return registerExtraCommands;
    }

    public boolean isCreeperHealMode() {
        return creeperHealMode;
    }

    public boolean isNoFallenOnFarmland() {
        return noFallenOnFarmland;
    }

}
