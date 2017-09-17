package cn.windmourn.operator.commands;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public class CommandsManager {

    public static void registerServerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandOp());
        event.registerServerCommand(new CommandDeOp());
        event.registerServerCommand(new CommandBan());
        event.registerServerCommand(new CommandBanIP());
        event.registerServerCommand(new CommandUnBan());
        event.registerServerCommand(new CommandUnBanIP());
    }

    public static void registerExtraCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandVanish());
        event.registerServerCommand(new CommandFly());
        event.registerServerCommand(new CommandSuper());
    }

}
