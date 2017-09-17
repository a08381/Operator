package cn.windmourn.operator.commands;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandSuper extends CommandBase {

    @Override
    public String getName() {
        return "super";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.super.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String s = args.length > 1 ? args[1] : "";
        String s1 = args.length > 2 ? buildString(args, 2).toLowerCase() : "";
        if (args.length > 1) {
            if (args[0].equalsIgnoreCase("gamerule")) {
                if (args.length == 2) {
                    if (!sender.getEntityWorld().getGameRules().hasRule(s)) {
                        throw new CommandException("commands.gamerule.norule", s);
                    }
                    for (WorldServer world : server.worlds) {
                        String s2 = world.getGameRules().getString(s);
                        sender.sendMessage((new TextComponentString(server.getEntityWorld().equals(world) ? (server instanceof DedicatedServer ? ((DedicatedServer) server).getStringProperty("level-name", "world") : "world") : world.provider.getSaveFolder()))
                                .appendText(": ").appendText(s).appendText(" = ").appendText(s2));
                        sender.setCommandStat(CommandResultStats.Type.QUERY_RESULT, world.getGameRules().getInt(s));
                    }
                } else {
                    for (World world : server.worlds) {
                        if (world.getGameRules().areSameType(s, GameRules.ValueType.BOOLEAN_VALUE) && !"true".equals(s1) && !"false".equals(s1)) {
                            throw new CommandException("commands.generic.boolean.invalid", s1);
                        }
                        world.getGameRules().setOrCreateGameRule(s, s1);
                        CommandGameRule.notifyGameRuleChange(world.getGameRules(), s, server);
                        notifyCommandListener(sender, this, "commands.gamerule.success", s, s1);
                    }
                }
            } else if (args[0].equalsIgnoreCase("tp")) {
                WorldServer newWorld = null;
                for (WorldServer worldIn : server.worlds) {
                    if (worldIn.getWorldInfo().getWorldName().equals(s)) {
                        newWorld = worldIn;
                    }
                }
                if (newWorld == null) {
                    throw new CommandException("commands.tp.notSameDimension");
                }
                BlockPos pos = newWorld.getSpawnPoint();
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();
                int dimension = newWorld.provider.getDimension();
                if (args.length == 2) {
                    EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                    player.changeDimension(dimension);
                    player.attemptTeleport(x, y, z);
                } else {
                    EntityPlayerMP player = getPlayer(server, sender, s1);
                    player.changeDimension(dimension);
                    player.attemptTeleport(x, y, z);
                }
            }
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        switch (args.length) {
            case 1: {
                return getListOfStringsMatchingLastWord(args, Arrays.asList("gamerule", "tp"));
            }
            case 2: {
                switch (args[0].toLowerCase()) {
                    case "gamerule": {
                        return getListOfStringsMatchingLastWord(args, server.getEntityWorld().getGameRules().getRules());
                    }
                    case "tp": {
                        List<String> list = new ArrayList<>();
                        for (WorldServer world : server.worlds) {
                            list.add(world.getWorldInfo().getWorldName());
                        }
                        return getListOfStringsMatchingLastWord(args, list);
                    }
                }
                break;
            }
            case 3: {
                switch (args[0].toLowerCase()) {
                    case "gamerule": {
                        return getListOfStringsMatchingLastWord(args, sender.getEntityWorld().getGameRules().getString(args[1]));
                    }
                    case "tp": {
                        return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
                    }
                }
                break;
            }
        }
        return Collections.emptyList();
    }

}
