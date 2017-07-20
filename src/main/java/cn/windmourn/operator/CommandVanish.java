package cn.windmourn.operator;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Created by lenovo on 2017/7/18.
 */
public class CommandVanish extends CommandBase {

    @Override
    public String getName() {
        return "vanish";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.vanish.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (args.length >= 1) {
            player = getEntity(server, sender, args[0], EntityPlayerMP.class);
            player.setInvisible(!player.isInvisible());
            notifyCommandListener(sender, this, "commands.vanish.success", player.getName(), new TextComponentTranslation(player.isInvisible() ? "commands.vanish.invisible" : "commands.vanish.visible"));
        } else {
            player.setInvisible(!player.isInvisible());
            notifyCommandListener(sender, this, "commands.vanish.success", player.getName(), new TextComponentTranslation(player.isInvisible() ? "commands.vanish.invisible" : "commands.vanish.visible"));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return args.length >= 1 ? getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()) : Collections.<String>emptyList();
    }

}
