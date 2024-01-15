package app.krista.extensions.essentials.cloudfiles.sharepoint;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extension.util.InvokerAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Map;

@Service
public class SharePointAttributes {
    private final String routingUrl;
    private String clientId;
    private String clientSecret;
    private String mailId;
    private String tenantId;
    private OAuth20Service oAuth20Service;

    @Inject
    public SharePointAttributes(Invoker invoker) {
        this(invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE),
                invoker.getAttributes());
    }

    public SharePointAttributes(String routingUrl, Map<String, Object> attributes) {
        this(routingUrl, InvokerAttributes.getStringOrNull(attributes, Constants.CLIENT_ID),
                InvokerAttributes.getStringOrNull(attributes, Constants.CLIENT_SECRET),
                InvokerAttributes.getStringOrNull(attributes, Constants.EMAIL),
                InvokerAttributes.getStringOrNull(attributes, Constants.TENANT_ID));
    }

    public SharePointAttributes(String routingUrl, String clientId, String clientSecret, String mailId,
                                String tenantId) {
        this.routingUrl = routingUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.mailId = mailId;
        this.tenantId = tenantId;
    }

    public static SharePointAttributes create(String routingUrl, Map<String, Object> attributes) {
        return new SharePointAttributes(routingUrl, attributes);
    }

    public void update(Map<String, Object> newAttributes) {
        clientId = InvokerAttributes.getStringOrNull(newAttributes, Constants.CLIENT_ID);
        clientSecret = InvokerAttributes.getStringOrNull(newAttributes, Constants.CLIENT_SECRET);
        mailId = InvokerAttributes.getStringOrNull(newAttributes, Constants.EMAIL);
        tenantId = InvokerAttributes.getStringOrNull(newAttributes, Constants.TENANT_ID);
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

    public String getMailId() {
        return mailId;
    }

    public String getCallbackUrl() {
        return routingUrl + Constants.REST_SHAREPOINT_CALLBACK;
    }

    public synchronized OAuth20Service getOAuth20Service() {
        if (oAuth20Service == null) {
            oAuth20Service = new ServiceBuilder(clientId)
                    .apiSecret(clientSecret)
                    .defaultScope(Constants.REQUIRED_SCOPE)
                    .callback(getCallbackUrl())
                    .build(MicrosoftAzureActiveDirectory20Api.custom(tenantId));
        }
        return oAuth20Service;
    }

}
