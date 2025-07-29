package app.krista.extensions.cyber_security.iam.azureAD;

import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.*;
import app.krista.extensions.cyber_security.iam.azureAD.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.cyber_security.iam.azureAD.impl.stores.AzureADAttributesStore;
import app.krista.ksdk.context.AuthorizationContext;

import javax.inject.Inject;
import java.util.Map;


@Java(version = Java.Version.JAVA_21)
@Extension(version = "2.0.6", name = "AzureAD")
@StaticResource(path = "docs", file = "docs")
public class AzureADExtension {


    private final AzureADRequestAuthenticator requestAuthenticator;
    private final GraphServiceClientProviderFactory providerFactory;

    @Inject
    public AzureADExtension(Invoker invoker, AzureADAttributesStore attributeStore, GraphServiceClientProviderFactory providerFactory, AuthorizationContext authorizationContext) {
        this.requestAuthenticator = new AzureADRequestAuthenticator(attributeStore, invoker.getInvokerId(), authorizationContext);
        this.providerFactory = providerFactory;
    }

    @InvokerRequest(InvokerRequest.Type.AUTHENTICATOR)
    public RequestAuthenticator getRequestAuthenticator() {
        return requestAuthenticator;
    }

    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of("Authentication", "rest/azuread/docs/", "Documentation", "static/docs");
    }
}

