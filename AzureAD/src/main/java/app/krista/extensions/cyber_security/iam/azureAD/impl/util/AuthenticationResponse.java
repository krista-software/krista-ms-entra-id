package app.krista.extensions.cyber_security.iam.azureAD.impl.util;

public class AuthenticationResponse {
    private final boolean isSuccess;
    private final String errorMessage;
    private final String url;

    public AuthenticationResponse(boolean isSuccess, String errorMessage, String url) {
        this.isSuccess = isSuccess;
        this.errorMessage = errorMessage;
        this.url = url;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getUrl() {
        return url;
    }
}
