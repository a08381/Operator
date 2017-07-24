package cn.windmourn.operator.utils;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class ConfigManager {

    private Configuration config;
    private File configFile;

    private boolean opsOwnerOnStart;
    private boolean registerExtraCommands;
    private boolean neverExplosion;
    private boolean explosionNoBreak;

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
        neverExplosion = config.get(Configuration.CATEGORY_GENERAL, "NeverExplosion", false, "ALL entities explosion will be disabled. Default is false.").getBoolean();
        explosionNoBreak = config.get(Configuration.CATEGORY_GENERAL, "ExplosionNoBreak", true, "Explosion will NEVER break a block. Default is true.").getBoolean();
    }

    public boolean isOpsOwnerOnStart() {
        return opsOwnerOnStart;
    }

    public boolean isRegisterExtraCommands() {
        return registerExtraCommands;
    }

    public boolean isNeverExplosion() {
        return neverExplosion;
    }

    public boolean isExplosionNoBreak() {
        return explosionNoBreak;
    }

}
