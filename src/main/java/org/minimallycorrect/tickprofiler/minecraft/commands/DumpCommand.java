package org.minimallycorrect.tickprofiler.minecraft.commands;

import java.lang.reflect.*;
import java.util.*;

import org.minimallycorrect.tickprofiler.Log;
import org.minimallycorrect.tickprofiler.minecraft.TickProfiler;
import org.minimallycorrect.tickprofiler.util.TableFormatter;

import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class DumpCommand extends Command {
	public static String name = "dump";

	public static TableFormatter dump(TableFormatter tf, World world, BlockPos pos, int maxLen) {
		@SuppressWarnings("MismatchedQueryAndUpdateOfStringBuilder")
		StringBuilder sb = tf.sb;
		IBlockState block = world.getBlockState(pos);
		//noinspection ConstantConditions
		if (block == null || block.getBlock() == Blocks.AIR) {
			sb.append("No block at ").append(Log.name(world)).append(" ").append(Log.toString(pos)).append('\n');
		} else {
			sb.append(block.getBlock()).append(':').append(block.getProperties()).append('\n');
		}
		sb.append("World time: ").append(world.getWorldTime()).append('\n');
		TileEntity toDump = world.getTileEntity(pos);
		if (toDump == null) {
			sb.append("No tile entity at ").append(Log.name(world)).append(" ").append(Log.toString(pos)).append('\n');
			return tf;
		}
		dump(tf, toDump, maxLen);
		return tf;
	}

	private static void dump(TableFormatter tf, Object toDump, int maxLen) {
		tf.sb.append(toDump.getClass().getName()).append('\n');
		tf
			.heading("Field")
			.heading("Value");
		Class<?> clazz = toDump.getClass();
		do {
			for (Field field : clazz.getDeclaredFields()) {
				if ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
					continue;
				}
				field.setAccessible(true);
				tf.row(field.getName());
				try {
					String value = String.valueOf(field.get(toDump));
					tf.row(value.substring(0, Math.min(value.length(), maxLen)));
				} catch (IllegalAccessException e) {
					tf.row(e.getMessage());
				}
			}
		} while ((clazz = clazz.getSuperclass()) != Object.class);
		tf.finishTable();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean requireOp() {
		return TickProfiler.instance.requireOpForDumpCommand;
	}

	@Override
	public void processCommand(final ICommandSender commandSender, List<String> arguments) {
		World world = DimensionManager.getWorld(0);
		int x = 0;
		int y = 0;
		int z = 0;
		try {
			if (commandSender instanceof Entity) {
				world = ((Entity) commandSender).world;
			}
			x = Integer.parseInt(arguments.remove(0));
			y = Integer.parseInt(arguments.remove(0));
			z = Integer.parseInt(arguments.remove(0));
			if (!arguments.isEmpty()) {
				world = DimensionManager.getWorld(Integer.parseInt(arguments.remove(0)));
			}
		} catch (Exception e) {
			world = null;
		}
		if (world == null) {
			sendChat(commandSender, "Usage: /dump x y z [world=currentworld]");
		} else {
			sendChat(commandSender, dump(new TableFormatter(commandSender), world, new BlockPos(x, y, z), commandSender instanceof Entity ? 35 : 70).toString());
		}
	}

	@Override
	public String getUsage(ICommandSender icommandsender) {
		return "Usage: /dump x y z [world=currentworld]";
	}
}
