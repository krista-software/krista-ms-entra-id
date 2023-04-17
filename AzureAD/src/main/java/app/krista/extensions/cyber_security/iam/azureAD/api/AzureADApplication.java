package app.krista.extensions.cyber_security.iam.azureAD.api;

import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

@Service
@ApplicationPath("/azuread")
@ContractsProvided(Application.class)
public class AzureADApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(AuthenticationResource.class);
    }

}
