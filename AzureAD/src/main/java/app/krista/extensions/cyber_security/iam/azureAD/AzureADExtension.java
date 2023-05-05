package app.krista.extensions.cyber_security.iam.azureAD;

import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Extension;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.InvokerRequest;
import app.krista.extension.impl.anno.StaticResource;
import app.krista.extensions.cyber_security.iam.azureAD.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.cyber_security.iam.azureAD.impl.stores.AzureADAttributesStore;

import javax.inject.Inject;
import java.util.Map;


@Field.Text(value = AzureADAttributes.CLIENT_ID)
@Field.Text(value = AzureADAttributes.CLIENT_SECRET, isSecured = true)
@Field.Text(value = AzureADAttributes.TENANT_ID, isSecured = true)
@Field.Text(value = AzureADAttributes.EMAIL)
@Extension(version = "1.0.0-rc3")
@StaticResource(path = "docs", file = "docs")
public final
class AzureADExtension {


    private final Invoker invoker;
    private final AzureADAttributes azureADAttributes;
    private final GraphServiceClientProviderFactory clientProviderFactory;
    private final AzureADRequestAuthenticator requestAuthenticator;

    @Inject
    public AzureADExtension(Invoker invoker, AzureADAttributes azureADAttributes, GraphServiceClientProviderFactory clientProviderFactory, AzureADAttributesStore azureADAttributesStore) {
        this(invoker, azureADAttributes,
                new AzureADRequestAuthenticator(azureADAttributesStore, azureADAttributes), clientProviderFactory);
    }

    public AzureADExtension(Invoker invoker, AzureADAttributes azureADAttributes, AzureADRequestAuthenticator requestAuthenticator, GraphServiceClientProviderFactory clientProviderFactory) {
        this.invoker = invoker;
        this.azureADAttributes = azureADAttributes;
        this.requestAuthenticator = requestAuthenticator;
        this.clientProviderFactory = clientProviderFactory;
    }

    @InvokerRequest(InvokerRequest.Type.AUTHENTICATOR)
    public RequestAuthenticator getRequestAuthenticator() {
        return requestAuthenticator;
    }

    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of("Documentation", "static/docs");
    }

    @InvokerRequest(InvokerRequest.Type.VALIDATE_ATTRIBUTES)
    public void validateConnection(Map<String, Object> connectionAttributes) {
        AzureADAttributes localAzureADAttributes = AzureADAttributes.create(invoker, connectionAttributes);
        validateAndTestConnection(localAzureADAttributes);
    }

    @InvokerRequest(InvokerRequest.Type.TEST_CONNECTION)
    public void testConnection() {
        validateAndTestConnection(azureADAttributes);
    }

    /**
     * @param azureADAttributes This function creates a connection with GraphServiceClient to validate the given credentials.
     */
    private void validateAndTestConnection(AzureADAttributes azureADAttributes) {
        if (azureADAttributes.getEmail() != null && !azureADAttributes.getEmail().isBlank()) {
            clientProviderFactory.create(azureADAttributes)
                    .getGraphServiceClientForAdmin()
                    .me().buildRequest().get();
        }
    }

    @InvokerRequest(InvokerRequest.Type.INVOKER_UPDATED)
    public void attributesUpdated(Map<String, Object> oldAttributes, Map<String, Object> newAttributes) {
        azureADAttributes.update(newAttributes);
    }
}

