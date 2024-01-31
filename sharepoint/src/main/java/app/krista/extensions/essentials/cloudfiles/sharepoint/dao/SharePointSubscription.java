package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.List;
import com.microsoft.graph.models.Site;
import com.microsoft.graph.models.Subscription;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.ListCollectionPage;
import com.microsoft.graph.requests.SiteCollectionPage;
import com.microsoft.graph.requests.SubscriptionCollectionPage;
import com.microsoft.graph.requests.SubscriptionCollectionRequestBuilder;
import com.microsoft.graph.serializer.OffsetDateTimeSerializer;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;

@Service
public class SharePointSubscription {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharePointSubscription.class);

    private SharePointSubscription() {
    }

    public static void createSharePointSubscriptionForAllSites(String routingUrl, GraphServiceClientProvider provider) {
        try {
            LinkedList<QueryOption> requestOptions = new LinkedList<>();
            requestOptions.add(new QueryOption("search", "*"));
            SiteCollectionPage sites = provider.getGraphServiceClientForAdmin().sites().buildRequest(requestOptions).get();

            for (Site site : sites.getCurrentPage()) {
                try {
                    provider.getGraphServiceClientForAdmin().sites(site.id).buildRequest().get();    // To check if site exist or not
                    ListCollectionPage listCollectionPage = provider.getGraphServiceClientForAdmin().sites(site.id).lists().buildRequest().get();
                    if (listCollectionPage != null) {
                        for (List list : listCollectionPage.getCurrentPage()) {
                            createSubscription(routingUrl, provider, site.id, list.id);
                        }
                    }
                } catch (Exception cause) {
                    LOGGER.info(" Error Description : " + cause.getMessage() + " Site Name : " + site.displayName);
                }
            }
        } catch (Exception cause) {
            System.out.println(" Error Description : " + cause.getMessage());
        }
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
            Subscription subscription = setSubscriptionParameters(routingUrl, siteId, listId);

            SubscriptionCollectionPage collectionPage = provider.getGraphServiceClientForAdmin().subscriptions().buildRequest().get();
            while (collectionPage != null && !collectionPage.getCurrentPage().isEmpty()) {
                for (Subscription oldSubscription : collectionPage.getCurrentPage()) {
                    if (Objects.requireNonNull(oldSubscription.resource).equals(subscription.resource)) {
                        return;
                    }
                }
                SubscriptionCollectionRequestBuilder nextPage = collectionPage.getNextPage();
                if (nextPage != null) {
                    collectionPage = nextPage.buildRequest().get();
                } else {
                    break;
                }
            }
            provider.getGraphServiceClientForAdmin().subscriptions().buildRequest().post(subscription);
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
        subscription.resource = Constants.SITE_RESOURCE + siteId + Constants.LIST_RESOURCE + listId;
        long threeDaysInMillis = System.currentTimeMillis() + (86000 * 1000 * 28L);
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
            long threeDaysInMillis = System.currentTimeMillis() + (86000 * 1000 * 28L);
            Date date = new Date(threeDaysInMillis);
            DateFormat dateFormat = new SimpleDateFormat(Constants.YYYY_MM_DD_T_HH_MM_SS_SSSSSSS_Z);
            subscription.expirationDateTime = OffsetDateTimeSerializer.deserialize(dateFormat.format(date));
            provider.getGraphServiceClientForAdmin().subscriptions(subscriptionId).buildRequest().patch(subscription);
        } catch (ClientException | ParseException cause) {
            throw new RuntimeException(cause);
        }
    }


}
