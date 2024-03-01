package app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors;

import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

public class RestClient {

    public static final String BEARER_ = "Bearer ";
    public static final String BAD_REQUEST = "Bad Request";
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    public static final String REQUEST_IS_ACCEPTED = "Request is Accepted";

    public static final String REQUEST_IS_SUCCESS = "Request is Success";

    public static final String CONFLICT_OCCURS = "Conflict Occurs";

    public static final String ONE_OR_MORE_INPUT_PARAMETER_IS_INVALID = "One or more input parameter is invalid";
    public static final String ONE_OR_MORE_FIELD_PROVIDED_ARE_INVALID = "One or more field provided are invalid";
    public static final String GRAPH_API_BASE_URI = "https://graph.microsoft.com/v1.0/me";

    public static final String GRAPH_API_DRIVES_BASE_URI = "https://graph.microsoft.com/v1.0";

    String accessToken;
    String driveId;
    String instanceUrl;

    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String YOU_ARE_NOT_AUTHORIZED = "You are not authorized";
    public static final String APPLICATION_JSON = "application/json";
    public static final String STATUS_CODE_IS = "Status code is ";


    public RestClient(String accessToken, String driveId) {
        this.accessToken = accessToken;
        this.driveId = driveId;
        this.instanceUrl = getProperInstanceUrl(driveId);
    }

    private String getProperInstanceUrl(String driveId) {
        return driveId != null ? GRAPH_API_DRIVES_BASE_URI + "/drives/" + driveId + "/items/"
                : GRAPH_API_BASE_URI + "/drive/items/";
    }

    public Response get(String endPoint) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(endPoint)
                    .get()
                    .build();
            return handleResponse(client.newCall(request).execute());
        } catch (IOException e) {
            return null;
        }
    }

    public Response executeHttpMethods(String endpoint, String action, RequestBody payload) throws IOException {
        switch (action) {
            case Constants.POST_METHOD:
                return executeRequest(payload, Constants.POST_METHOD, endpoint);
            case Constants.GET_REQUEST:
                return executeRequest(null, Constants.GET_REQUEST, endpoint);
            case Constants.PATCH_METHOD:
                return executeRequest(payload, Constants.PATCH_METHOD, endpoint);
            default:
                break;
        }
        return null;
    }

    private static Request prepareRequest(String methodType, RequestBody requestBody, String endPointUrl,
                                          String accessToken) {

        Request.Builder requestBuilder = new Request.Builder()
                .url(endPointUrl)
                .method(methodType, requestBody);

        if (!Constants.GET_REQUEST.equalsIgnoreCase(methodType)) {
            requestBuilder
                    .addHeader(AUTHORIZATION, BEARER_ + accessToken)
                    .addHeader(CONTENT_TYPE, APPLICATION_JSON);
        }

        return requestBuilder.build();
    }

    private Response executeRequest(RequestBody payload, String methodType, String endPointUrl) throws
            IOException {
        Request request = prepareRequest(methodType, payload, instanceUrl + endPointUrl, accessToken);
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        try (Response response = client.newCall(request).execute()) {
            return handleResponse(Objects.requireNonNull(response));
        }
    }

    public Response handleResponse(Response response) {
        int statusCode = response.code();

        if (statusCode == Constants.OK || statusCode == Constants.CREATED_RES || statusCode == Constants.NO_CONTENT) {
            System.out.println(STATUS_CODE_IS + response.code() + REQUEST_IS_SUCCESS);
            return response;
        } else if (statusCode == Constants.ACCEPTED) {
            System.out.println(STATUS_CODE_IS + response.code() + REQUEST_IS_ACCEPTED);
            return response;
        } else if (statusCode == Constants.CONFLICT) {
            System.out.println(STATUS_CODE_IS + response.code() + CONFLICT_OCCURS);
            return response;
        } else if (statusCode == Constants.UNAUTHORIZED) {
            System.out.println(STATUS_CODE_IS + response.code() + YOU_ARE_NOT_AUTHORIZED);
            throw new IllegalArgumentException(YOU_ARE_NOT_AUTHORIZED);
        } else if (statusCode == Constants.BAD_REQUEST) {
            System.out.println(STATUS_CODE_IS + response.code() + BAD_REQUEST);
            throw new IllegalArgumentException(ONE_OR_MORE_INPUT_PARAMETER_IS_INVALID);
        } else if (statusCode == Constants.INTERNAL_SERVER_ERROR) {
            System.out.println(STATUS_CODE_IS + response.code() + INTERNAL_SERVER_ERROR);
            throw new IllegalArgumentException(ONE_OR_MORE_FIELD_PROVIDED_ARE_INVALID);
        }
        return null;
    }

}
