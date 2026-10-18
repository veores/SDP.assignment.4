package com.example.orderfacade.subsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationService {

    public record Message(String to, String subject, String body) {
    }

    private final List<Message> outbox = new ArrayList<>();

    public void send(String recipientEmail, String subject, String body) {
        if (recipientEmail == null || !recipientEmail.contains("@")) {
            throw new NotificationException("Invalid e-mail address: " + recipientEmail);
        }
        outbox.add(new Message(recipientEmail, subject, body));
    }

    public List<Message> sentMessages() {
        return Collections.unmodifiableList(outbox);
    }
}
