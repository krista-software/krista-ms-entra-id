package app.krista.extensions.cyber_security.iam.microsoft_entra_id;

import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.*;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.stores.MicrosoftEntraIDAttributesStore;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants;
import app.krista.ksdk.context.AuthorizationContext;

import javax.inject.Inject;
import java.util.Map;


@Java(version = Java.Version.JAVA_21)
@Field.Boolean(value = Constants.ENABLE_ERROR_HANDLING,required = false)
@Extension(version = "3.0.1", name = "Microsoft Entra ID")
@StaticResource(path = "docs", file = "docs")
public class MicrosoftEntraIDExtension {


    private final MicrosoftEntraIDRequestAuthenticator requestAuthenticator;
    private final GraphServiceClientProviderFactory providerFactory;

    @Inject
    public MicrosoftEntraIDExtension(Invoker invoker, MicrosoftEntraIDAttributesStore attributeStore, GraphServiceClientProviderFactory providerFactory, AuthorizationContext authorizationContext) {
        this.requestAuthenticator = new MicrosoftEntraIDRequestAuthenticator(attributeStore, invoker.getInvokerId(), authorizationContext);
        this.providerFactory = providerFactory;
    }

    @InvokerRequest(InvokerRequest.Type.AUTHENTICATOR)
    public RequestAuthenticator getRequestAuthenticator() {
        return requestAuthenticator;
    }

    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of("Authentication", "rest/microsoftentraid/docs/", "Documentation", "static/docs");
    }
}

