/*
 * Copyright 2016 Andres Almiray
 *
 * This file is part of Java Trove Examples
 *
 * Java Trove Examples is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java Trove Examples is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Trove Examples. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kordamp.javatrove.chat04.client.controller;

import com.google.inject.Injector;
import org.jdeferred.DeferredManager;
import org.kordamp.javatrove.chat04.client.ChatClient;
import org.kordamp.javatrove.chat04.client.model.AppModel;
import org.kordamp.javatrove.chat04.client.util.ApplicationEventBus;
import org.kordamp.javatrove.chat04.client.util.ThrowableEvent;

import javax.inject.Inject;
import java.util.Optional;

/**
 * @author Andres Almiray
 */
public class AppController {
    @Inject private AppModel model;
    @Inject private DeferredManager deferredManager;
    @Inject private ApplicationEventBus eventBus;

    @Inject
    private Injector injector;

    public void login() {
        deferredManager.when(() -> {
            ChatClient client = injector.getInstance(ChatClient.class);
            model.setClient(client);
            client.login(model.getServer(), model.getPort(), model.getName());
        }).fail(this::handleException)
            .then((Void result) -> model.setConnected(true));
    }

    public void logout() {
        deferredManager.when(() -> {
            Optional<ChatClient> client = model.getClient();
            client.ifPresent(c -> c.logout(model.getName()));
        }).fail(this::handleException)
            .always((state, result, rejected) -> disconnect());
    }

    public void send() {
        deferredManager.when(() -> {
            String message = model.getMessage();
            model.setMessage("");
            model.getClient().ifPresent(c -> c.send(model.getName(), message));
        }).fail(throwable -> disconnect());
    }

    private void handleException(Throwable throwable) {
        model.setClient(null);
        eventBus.publishAsync(new ThrowableEvent(throwable));
    }

    private void disconnect() {
        model.setClient(null);
        model.setConnected(false);
        model.getMessages().clear();
    }
}
