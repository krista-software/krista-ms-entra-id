/*
 * Ms Entra Id Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.cyber_security.iam.microsoft_entra_id;

import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.*;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.stores.MicrosoftEntraIDAttributesStore;
import app.krista.ksdk.context.AuthorizationContext;

import javax.inject.Inject;
import java.util.Map;


@Java(version = Java.Version.JAVA_21)
@Extension(version = "3.0.3", name = "Microsoft Entra ID")
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

