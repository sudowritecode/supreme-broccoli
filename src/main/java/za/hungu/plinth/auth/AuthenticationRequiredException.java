package za.hungu.plinth.auth;

public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException() {
        super("A valid device token is required.");
    }

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
