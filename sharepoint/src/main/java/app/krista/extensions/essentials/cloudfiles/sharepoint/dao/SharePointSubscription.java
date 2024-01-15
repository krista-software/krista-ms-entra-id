package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.List;
import com.microsoft.graph.models.Site;
import com.microsoft.graph.models.Subscription;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.*;
import com.microsoft.graph.serializer.OffsetDateTimeSerializer;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SharePointSubscription {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharePointSubscription.class);

    private SharePointSubscription() {
    }

    public static Map<String, Object> createSharePointSubscriptionForAllSites(String routingUrl, GraphServiceClientProvider provider) {
        Map<String, Object> payload = new HashMap<>();
        try {
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(null, null);
            LinkedList<QueryOption> requestOptions = new LinkedList<>();
            requestOptions.add(new QueryOption("search", "*"));
            SiteCollectionPage sites = graphClient.sites().buildRequest(requestOptions).get();

            for (Site site : sites.getCurrentPage()) {
                try {
                    graphClient.sites(site.id).buildRequest().get();    // To check if site exist or not
                    ListCollectionPage listCollectionPage = graphClient.sites(site.id).lists().buildRequest().get();
                    if (listCollectionPage != null) {
                        for (List list : listCollectionPage.getCurrentPage()) {
                            createSubscription(routingUrl, provider, site.id , list.id);
                        }
                    }
                } catch (Exception cause) {
                    continue;
                }
            }
        } catch (Exception cause) {
            ResponseUtil.successResponse(false, Constants.FAILED_TO_CREATE_SUBSCRIPTION, payload);
        }
        return payload;
    }

    /**
     * This method will create a mail alert subscription
     *
     * @param routingUrl routing url to get the mail alert
     * @param provider   {@link GraphServiceClientProvider} object to get the subscriptions from microsoft
     * @param siteId
     * @param listId
     */
    public static void createSubscription(String routingUrl, GraphServiceClientProvider provider, String siteId, String listId) {
        try {
            //Subscription subscription = setSubscriptionParameters(routingUrl.contains("https://extension.local.eng.krista.app") ? routingUrl.replace("https://extension.local.eng.krista.app", "https://d8d1-103-97-240-165.ngrok-free.app") : routingUrl);
            Subscription subscription = setSubscriptionParameters(routingUrl, siteId, listId);
            System.out.print(" Subscription " + subscription.notificationUrl + "-" + subscription.lifecycleNotificationUrl + "-" + subscription.resource);

            SubscriptionCollectionPage collectionPage = provider.getGraphServiceClientForAdmin().subscriptions().buildRequest().get();
            while (collectionPage != null && !collectionPage.getCurrentPage().isEmpty()) {
                for (Subscription oldSubscription : collectionPage.getCurrentPage()) {
                    System.out.println(" old notificationUrl " + Objects.requireNonNull(oldSubscription.notificationUrl));
                    System.out.println(" subscription notificationUrl " + subscription.notificationUrl);
                    if (Objects.requireNonNull(oldSubscription.notificationUrl).equals(subscription.notificationUrl)) {
                        System.out.println(" Came inside ");
                        return;
                    }
                }
                SubscriptionCollectionRequestBuilder nextPage = collectionPage.getNextPage();
                if (nextPage != null) {
                    System.out.println(" nextPage " + nextPage);
                    collectionPage = nextPage.buildRequest().get();
                } else {
                    break;
                }
            }
            provider.getGraphServiceClientForAdmin().subscriptions().buildRequest().post(subscription);
            System.out.println(" Group Subscription Created ");
        } catch (ClientException | ParseException cause) {
            throw new RuntimeException(cause);
        }
    }

    /**
     * This method is used to set subscription parameters for creating a mail alert subscription
     *
     * @param routingUrl routing url to get the mail alert
     * @param siteId
     * @param listId
     * @return {@link Subscription} object with subscription parameters
     */
    @NotNull
    private static Subscription setSubscriptionParameters(String routingUrl, String siteId, String listId) throws ParseException {
        Subscription subscription = new Subscription();
        subscription.changeType = Constants.CHANGETYPE;
        subscription.notificationUrl = routingUrl + Constants.REST_SHAREPOINT_MAIL_NOTIFICATION;
        subscription.lifecycleNotificationUrl = routingUrl + Constants.REST_SHAREPOINT_LIFECYCLE_NOTIFICATION;
        subscription.resource = Constants.SITE_RESOURCE+siteId+Constants.LIST_RESOURCE+listId;
        long threeDaysInMillis = System.currentTimeMillis() + (86000 * 1000 * 28);
        Date date = new Date(threeDaysInMillis);
        DateFormat dateFormat = new SimpleDateFormat(Constants.YYYY_MM_DD_T_HH_MM_SS_SSSSSSS_Z);
        subscription.expirationDateTime = OffsetDateTimeSerializer.deserialize(dateFormat.format(date));
        return subscription;
    }

    /**
     * This method is used to set the expiry date for the existing subscription
     *
     * @param provider       {@link GraphServiceClientProvider} object to get the subscriptions list
     * @param subscriptionId id of the existing subscription
     */
    public static void renewSubscription(GraphServiceClientProvider provider, String subscriptionId) {
        try {
            Subscription subscription = new Subscription();
            long threeDaysInMillis = System.currentTimeMillis() + (86000 * 1000 * 28);
            Date date = new Date(threeDaysInMillis);
            DateFormat dateFormat = new SimpleDateFormat(Constants.YYYY_MM_DD_T_HH_MM_SS_SSSSSSS_Z);
            subscription.expirationDateTime = OffsetDateTimeSerializer.deserialize(dateFormat.format(date));
            provider.getGraphServiceClientForAdmin().subscriptions(subscriptionId).buildRequest().patch(subscription);
        } catch (ClientException | ParseException cause) {
            throw new RuntimeException(cause);
        }
    }


}
