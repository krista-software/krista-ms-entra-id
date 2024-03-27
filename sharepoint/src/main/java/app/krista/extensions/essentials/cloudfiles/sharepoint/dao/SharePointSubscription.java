package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.Subscription;
import com.microsoft.graph.requests.SubscriptionCollectionPage;
import com.microsoft.graph.serializer.OffsetDateTimeSerializer;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@Service
public class SharePointSubscription {

    private static final Logger lOGGER = LoggerFactory.getLogger(SharePointSubscription.class);

    private SharePointSubscription() {
    }

    /**
     * This method will create a mail alert subscription
     *
     * @param routingUrl routing url to get the mail alert
     * @param provider   {@link GraphServiceClientProvider} object to get the subscriptions from microsoft
     * @param siteId
     * @param listId
     */

    public static Subscription createSubscription(String routingUrl, GraphServiceClientProvider provider, String siteId, String listId) {
        try {
            Subscription subscription = setSubscriptionParameters(routingUrl.contains("https://extension.local.eng.krista.app") ? routingUrl.replace("https://extension.local.eng.krista.app", "https://bdf4-2409-40c2-101b-f156-5cb4-a5d9-3d22-8550.ngrok-free.app") : routingUrl, siteId, listId);
            return provider.getGraphServiceClientForAdmin().subscriptions().buildRequest().post(subscription);
        } catch (ClientException | ParseException cause) {
            lOGGER.error("Error occurred while creating subscription : {}", cause.getMessage());
            return null;
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
            throw new IllegalStateException(cause);
        }
    }

    /**
     * This method is used to delete the mail alert subscription
     *
     * @param provider       {@link GraphServiceClientProvider} object to get the subscriptions
     * @param subscriptionId url to remove the subscription alert
     */
    public static void deleteSubscription(GraphServiceClientProvider provider, String subscriptionId) {
        try {
            SubscriptionCollectionPage collectionPage = provider.getGraphServiceClientForAdmin().subscriptions().buildRequest().get();
            if (collectionPage != null && !collectionPage.getCurrentPage().isEmpty()) {
                for (Subscription subscription : collectionPage.getCurrentPage()) {
                    if (subscription.id != null && subscription.id.equals(subscriptionId)) {
                        provider.getGraphServiceClientForAdmin().subscriptions(Objects.requireNonNull(subscription.id)).buildRequest().delete();
                    }
                }
            }
        } catch (ClientException cause) {
            lOGGER.error("Error while deleting subscription : {}", cause.getMessage());
        }
    }

}
