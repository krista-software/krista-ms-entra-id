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

    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String TENANT_ID = "tenantId";
    public static final String EMAIL = "email";
    public static final String ALLOW_GROUP_ALERT = "allowGroupAlert";

    private final String routingUrl;
    private String clientId;
    private String clientSecret;
    private String mailId;
    private Boolean allowGroupAlert;
    private String tenantId;
    private OAuth20Service oAuth20Service;

    @Inject
    public SharePointAttributes(Invoker invoker) {
        this(invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE),
                invoker.getAttributes());
    }

    public SharePointAttributes(String routingUrl, Map<String, Object> attributes) {
        this(routingUrl, InvokerAttributes.getStringOrNull(attributes, CLIENT_ID),
                InvokerAttributes.getStringOrNull(attributes, CLIENT_SECRET),
                InvokerAttributes.getStringOrNull(attributes, EMAIL),
                InvokerAttributes.getBooleanOrDefault(attributes, ALLOW_GROUP_ALERT, false),
                InvokerAttributes.getStringOrNull(attributes, TENANT_ID));
    }

    public SharePointAttributes(String routingUrl, String clientId, String clientSecret, String mailId,
                                boolean allowGroupAlert, String tenantId) {
        this.routingUrl = routingUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.mailId = mailId;
        this.allowGroupAlert = allowGroupAlert;
        this.tenantId = tenantId;
    }

    public static SharePointAttributes create(String routingUrl, Map<String, Object> attributes) {
        return new SharePointAttributes(routingUrl, attributes);
    }

    public void update(Map<String, Object> newAttributes) {
        clientId = InvokerAttributes.getStringOrNull(newAttributes, CLIENT_ID);
        clientSecret = InvokerAttributes.getStringOrNull(newAttributes, CLIENT_SECRET);
        mailId = InvokerAttributes.getStringOrNull(newAttributes, EMAIL);
        allowGroupAlert = InvokerAttributes.getBooleanOrDefault(newAttributes, ALLOW_GROUP_ALERT, false);
        tenantId = InvokerAttributes.getStringOrNull(newAttributes, TENANT_ID);
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

    public boolean getAllowGroupAlert() {
        return allowGroupAlert != null && allowGroupAlert;
    }

    public String getCallbackUrl() {
        //return "http://localhost:8765/extension/api/bVEgszmxVA_skmHPEtI1Pjg_e_e/rest/sharepoint/callback";
        //return "http://localhost:8765/extension/api/d9x_syuTvCPaY1Iq7OQUBHQ_e_e/test/rest/sharepoint/callback";
        //return "http://localhost:8765/extension/api/d9x_syuTvCPaY1Iq7OQUBHQ_e_e/draft/rest/sharepoint/callback";
        //return "http://localhost:8765/extension/api/d9x_syuTvCPaY1Iq7OQUBHQ_e_e/rest/sharepoint/callback";
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
