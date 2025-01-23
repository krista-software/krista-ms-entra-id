package app.krista.extensions.cyber_security.iam.azureAD;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extension.util.InvokerAttributes;
import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Map;

@Service
public final class AzureADAttributes {

    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String TENANT_ID = "tenantId";

    public static final String EMAIL = "email";
    private final String routingUrl;
    private String mail;
    private String clientId;
    private String clientSecret;
    private String tenantId;
    private OAuth20Service oAuth20Service;

    @Inject
    public AzureADAttributes(Invoker invoker) {
        this(invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE),
                invoker.getAttributes());
    }

    public AzureADAttributes(String routingUrl, Map<String, Object> attributes) {
        this(routingUrl, InvokerAttributes.getStringOrNull(attributes, CLIENT_ID),
                InvokerAttributes.getStringOrNull(attributes, CLIENT_SECRET),
                InvokerAttributes.getStringOrNull(attributes, TENANT_ID),
                InvokerAttributes.getStringOrNull(attributes, EMAIL));
    }

    public AzureADAttributes(String routingUrl, String clientId, String clientSecret, String tenantId, String email) {
        this.routingUrl = routingUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tenantId = tenantId;
        this.mail = email;
    }

    public static AzureADAttributes create(Invoker invoker, Map<String, Object> attributes) {
        return new AzureADAttributes(
                invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE),
                attributes);
    }

    /**
     * @param newAttributes This function updates the attributes provided by user
     */
    public void update(Map<String, Object> newAttributes) {
        clientId = InvokerAttributes.getStringOrNull(newAttributes, CLIENT_ID);
        clientSecret = InvokerAttributes.getStringOrNull(newAttributes, CLIENT_SECRET);
        tenantId = InvokerAttributes.getStringOrNull(newAttributes, TENANT_ID);
        mail = InvokerAttributes.getStringOrNull(newAttributes, EMAIL);
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTenantId() {
        return tenantId;
    }


    public String getEmail() {
        return mail;
    }

    public String getCallbackUrl() {
        return routingUrl + "/rest/azuread/callback";
    }

    public synchronized OAuth20Service getOAuth20Service() {
        if (oAuth20Service == null) {
            oAuth20Service = new ServiceBuilder(clientId)
                    .apiSecret(clientSecret)
                    .callback(getCallbackUrl())
                    .defaultScope("openid offline_access User.Read Group.ReadWrite.All UserAuthenticationMethod.ReadWrite.All User.ReadWrite.All")
                    .build(MicrosoftAzureActiveDirectory20Api.custom(tenantId));
        }
        return oAuth20Service;
    }
}
