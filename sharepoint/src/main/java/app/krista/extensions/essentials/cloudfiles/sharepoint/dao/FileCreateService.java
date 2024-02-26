package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.GroupImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for creating groups using Microsoft Graph API.
 * Provides methods to validate group data and perform group creation.
 *
 * @author Vaibhav Choudhary
 */

public class FileCreateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCreateService.class);

    private final GraphServiceClient<Request> graphClient;

    public FileCreateService(GraphServiceClient<Request> graphClient) {
        this.graphClient = graphClient;
    }

    /**
     * Creates a new group using the provided groupData map, validates the input, and performs the group creation.
     *
     * @param groupName    A map containing properties for group creation.
     * @param mailNickName A map containing properties for group creation.
     * @return A GroupImpl object representing the newly created group, or null if creation fails.
     * @see GroupImpl
     * @see GraphServiceException
     */
    public Map<String, Object> createGroup(String groupName, String mailNickName) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (!validateGroupData(groupName, mailNickName)) {
                LOGGER.error("Invalid input: Missing group data or properties");
                throw new IllegalArgumentException("Invalid input: Missing group data or properties");
            }
            Group groupToCreate = buildGroup(groupName, mailNickName);
            Group createdGroup = graphClient.groups().buildRequest().post(groupToCreate);

            if (createdGroup.id != null) {
                payload.put(Constants.FILE, File.fromFile(new GroupImpl(createdGroup, null)));
                payload.put(Constants.SUCCESS, true);
                payload.put(Constants.MESSAGE, Constants.GROUP_CREATED);
            } else {
                payload.put(Constants.SUCCESS, false);
                payload.put(Constants.MESSAGE, Constants.FAILED_LIST_CREATION);
            }
            return payload;
        } catch (GraphServiceException cause) {
            LOGGER.error("Error occurred while creating the group. Details: {}", cause.getMessage());
            return ResponseUtil.nonSuccessResponce(payload);
        }
    }

    private boolean validateGroupData(String groupName, String mailNickName) {
        return groupName != null && !groupName.trim().isEmpty()
                && mailNickName != null && !mailNickName.trim().isEmpty()
                && groupName.matches("[a-zA-Z0-9 ]+")  // Allow only letters, numbers, and spaces
                && mailNickName.matches("[a-zA-Z0-9 ]+")
                && !mailNickName.contains(" ");
    }

    private Group buildGroup(String groupName, String mailNickName) {
        Group groupToCreate = new Group();
        groupToCreate.displayName = groupName;
        groupToCreate.mailNickname = mailNickName;
        groupToCreate.groupTypes = Collections.singletonList("Unified");
        groupToCreate.mailEnabled = true;
        groupToCreate.securityEnabled = false;
        return groupToCreate;
    }

}