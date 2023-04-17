package app.krista.extensions.cyber_security.iam.azureAD.impl.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Constants {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String STATE = "state";
    public static final String REQUIRED_SCOPE = "openid offline_access User.ReadWrite.All UserAuthenticationMethod.ReadWrite.All";
    public static final String CALLBACK_URI = "/azuread/callback";

    private Constants() {
    }
}
