package exception;

public class LicenseKeyException extends Exception {
//

    public LicenseKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public LicenseKeyException(Throwable cause) {
        super(cause);
    }
}
