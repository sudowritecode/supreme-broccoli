package za.hungu.plinth.messaging;

public interface MessagePublisher {

    void publish(EncryptedMessageEvent event);
}
