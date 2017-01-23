/*
 * This file is part of IPLog, licensed under the MIT License.
 *
 * Copyright (c) 2017 Meronat <http://meronat.com>
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.meronat.iplog.commands;

import com.meronat.iplog.IPLog;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class HistoryCommand implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

        Optional<User> optionalUser = args.getOne("player");

        Optional<InetAddress> optionalAddress = args.getOne("ip");

        if (optionalUser.isPresent() && optionalAddress.isPresent()) {

            throw new CommandException(Text.of(TextColors.RED, "You must specify either an IP address or player, but not both."));

        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME.withLocale(src.getLocale());

        if (optionalAddress.isPresent()) {

            Sponge.getScheduler().createAsyncExecutor(IPLog.getPlugin()).execute(() -> {

                Map<UUID, LocalDateTime> users = IPLog.getPlugin().getStorage().getPlayersAndTime(optionalAddress.get());

                if (users.isEmpty()) {

                    src.sendMessage(Text.of(TextColors.RED, "There are no players associated with the specified IP address."));

                    return;

                }

                Sponge.getScheduler().createSyncExecutor(IPLog.getPlugin()).execute(() -> {

                    UserStorageService userStorageService = Sponge.getServiceManager().provide(UserStorageService.class).get();

                    List<Text> contents = new ArrayList<>();

                    for (Map.Entry<UUID, LocalDateTime> e : users.entrySet()) {

                        userStorageService.get(e.getKey()).ifPresent(user -> contents.add(Text.of(TextColors.DARK_GREEN, user.getName(),
                            TextColors.GRAY, "    ", timeFormatter.format(e.getValue()))));

                    }

                    Sponge.getServiceManager().provide(PaginationService.class).ifPresent(p -> p.builder()
                        .title(Text.of(TextColors.DARK_GREEN, "User Logins Associated With ", TextColors.GREEN, optionalAddress.get().getHostAddress()))
                        .contents(contents)
                        .linesPerPage(14)
                        .padding(Text.of(TextColors.GRAY, "="))
                        .sendTo(src));

                });

            });

        } else if (optionalUser.isPresent()) {

            Sponge.getScheduler().createAsyncExecutor(IPLog.getPlugin()).execute(() -> {

                Map<String, LocalDateTime> addresses = IPLog.getPlugin().getStorage().getAddressesAndTime(optionalUser.get().getUniqueId());

                if (addresses.isEmpty()) {

                    src.sendMessage(Text.of(TextColors.RED, "There are no IP addresses associated with the specified user."));

                    return;

                }

                Sponge.getScheduler().createSyncExecutor(IPLog.getPlugin()).execute(() -> {

                    List<Text> contents = new ArrayList<>();

                    for (Map.Entry<String, LocalDateTime> e : addresses.entrySet()) {

                        contents.add(Text.of(TextColors.DARK_GREEN, e.getKey(), "    ", timeFormatter.format(e.getValue())));

                    }

                    Sponge.getServiceManager().provide(PaginationService.class).ifPresent(p -> p.builder()
                        .title(Text.of(TextColors.DARK_GREEN, "IP Logins Associated With ", TextColors.GREEN, optionalUser.get().getName()))
                        .contents(contents)
                        .linesPerPage(14)
                        .padding(Text.of(TextColors.GRAY, "="))
                        .sendTo(src));

                });

            });

        } else {

            throw new CommandException(Text.of(TextColors.RED, "You must specify either an IP address or a player."));

        }

        return CommandResult.success();

    }

}