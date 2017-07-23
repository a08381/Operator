package cn.windmourn.operator.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Created by lenovo on 2017/7/19.
 */
public class CommandFly extends CommandBase {

    @Override
    public String getName() {
        return "fly";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.fly.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (args.length >= 1) {
            player = getEntity(server, sender, args[0], EntityPlayerMP.class);
            player.capabilities.allowFlying = !player.capabilities.allowFlying;
            player.sendPlayerAbilities();
            notifyCommandListener(sender, this, "commands.fly.success", player.getName(), new TextComponentTranslation(player.isInvisible() ? "commands.fly.flyable" : "commands.fly.unflyable"));
        } else {
            player.capabilities.allowFlying = !player.capabilities.allowFlying;
            player.sendPlayerAbilities();
            notifyCommandListener(sender, this, "commands.fly.success", player.getName(), new TextComponentTranslation(player.isInvisible() ? "commands.fly.flyable" : "commands.fly.unflyable"));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return args.length >= 1 ? getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()) : Collections.<String>emptyList();
    }

}
