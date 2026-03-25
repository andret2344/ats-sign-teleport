/*
 * Copyright Andret Tools System (c) 2026. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.signteleport;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SignTeleportCommandTest {
	@Mock
	private SignTeleportPlugin plugin;
	@Mock
	private SignTeleportService service;
	@Mock
	private CommandSender sender;
	@Mock
	private Command command;

	@Test
	void reloadCommand() {
		// given
		final SignTeleportCommand cmd = new SignTeleportCommand(plugin, service);

		// when
		final boolean result = cmd.onCommand(sender, command, "signteleport", new String[]{"reload"});

		// then
		assertThat(result).isTrue();
		verify(plugin).reloadConfig();
		verify(service).updateSigns();
		verify(sender).sendMessage("SignTeleport configuration reloaded.");
	}

	@Test
	void unknownSubcommand() {
		// given
		final SignTeleportCommand cmd = new SignTeleportCommand(plugin, service);

		// when
		final boolean result = cmd.onCommand(sender, command, "signteleport", new String[]{"unknown"});

		// then
		assertThat(result).isFalse();
		verify(plugin, never()).reloadConfig();
		verify(service, never()).updateSigns();
	}

	@Test
	void noArguments() {
		// given
		final SignTeleportCommand cmd = new SignTeleportCommand(plugin, service);

		// when
		final boolean result = cmd.onCommand(sender, command, "signteleport", new String[]{});

		// then
		assertThat(result).isFalse();
		verify(plugin, never()).reloadConfig();
	}

	@Test
	void reloadCaseInsensitive() {
		// given
		final SignTeleportCommand cmd = new SignTeleportCommand(plugin, service);

		// when
		final boolean result = cmd.onCommand(sender, command, "signteleport", new String[]{"RELOAD"});

		// then
		assertThat(result).isTrue();
		verify(plugin).reloadConfig();
		verify(service).updateSigns();
	}
}
