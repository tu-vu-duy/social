/**
 * 
 */
package org.exoplatform.social.notification.template;

import java.util.Locale;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.AbstractChannelTemplateHandler;
import org.exoplatform.commons.api.notification.channel.ChannelConfigs;
import org.exoplatform.commons.api.notification.channel.TemplateConfig;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;
import org.exoplatform.social.notification.plugin.ActivityCommentPlugin;
import org.exoplatform.social.notification.plugin.ActivityMentionPlugin;
import org.exoplatform.social.notification.plugin.LikePlugin;
import org.exoplatform.social.notification.plugin.NewUserPlugin;
import org.exoplatform.social.notification.plugin.PostActivityPlugin;
import org.exoplatform.social.notification.plugin.PostActivitySpaceStreamPlugin;
import org.exoplatform.social.notification.plugin.RelationshipReceivedRequestPlugin;
import org.exoplatform.social.notification.plugin.RequestJoinSpacePlugin;
import org.exoplatform.social.notification.plugin.SocialNotificationUtils;
import org.exoplatform.social.notification.plugin.SpaceInvitationPlugin;
import org.exoplatform.webui.utils.TimeConvertUtils;

@ChannelConfigs (
   id = "intranet",
   templates = {
       @TemplateConfig( pluginId=ActivityCommentPlugin.ID, path="war:/intranet-notification/templates/ActivityCommentPlugin.gtmpl"),
       @TemplateConfig( pluginId=ActivityMentionPlugin.ID, path="war:/intranet-notification/templates/ActivityMentionPlugin.gtmpl"),
       @TemplateConfig( pluginId=LikePlugin.ID, path="war:/intranet-notification/templates/LikePlugin.gtmpl"),
       @TemplateConfig( pluginId=NewUserPlugin.ID, path="war:/intranet-notification/templates/NewUserPlugin.gtmpl"),
       @TemplateConfig( pluginId=PostActivityPlugin.ID, path="war:/intranet-notification/templates/PostActivityPlugin.gtmpl"),
       @TemplateConfig( pluginId=PostActivitySpaceStreamPlugin.ID, path="war:/intranet-notification/templates/PostActivitySpaceStreamPlugin.gtmpl"),
       @TemplateConfig( pluginId=RelationshipReceivedRequestPlugin.ID, path="war:/intranet-notification/templates/RelationshipReceivedRequestPlugin.gtmpl"),
       @TemplateConfig( pluginId=RequestJoinSpacePlugin.ID, path="war:/intranet-notification/templates/RequestJoinSpacePlugin.gtmpl"),
       @TemplateConfig( pluginId=SpaceInvitationPlugin.ID, path="war:/intranet-notification/templates/SpaceInvitationPlugin.gtmpl")
   }
)
public class ActivityIntranetTemplateHandler extends AbstractChannelTemplateHandler {

  /**
   * 
   */
  public ActivityIntranetTemplateHandler() {
    super();
  }

  @Override
  public MessageInfo makeMessage(NotificationContext ctx) {
    NotificationInfo notification = ctx.getNotificationInfo();
    MakeMessageInfo builder = MakeMessageInfo.get(notification.getKey().getId());
    if(builder != null) {
      return builder.buildMessage(ctx, channelId);
    }
    return null;
  }
  
  private enum MakeMessageInfo {
    ActivityCommentPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());

        String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
        ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
        ExoSocialActivity parentActivity = Utils.getActivityManager().getParentActivity(activity);
        Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
        
        TemplateContext templateContext = TemplateContext.newChannelInstance(channelId, notification.getKey().getId(), language);
        templateContext.put("READ", (notification.isHasRead()) ? "read" : "unread");
        templateContext.put("NOTIFICATION_ID", notification.getId());
        templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgo(notification.getLastModifiedDate().getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
        templateContext.put("USER", identity.getProfile().getFullName());
        templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(identity.getProfile()));
        templateContext.put("ACTIVITY", NotificationUtils.removeLinkTitle(parentActivity.getTitle()));
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity_highlight_comment", parentActivity.getId() + "-" + activity.getId()));

        String body = TemplateUtils.processGroovy(templateContext);
        
        return messageInfo.to(notification.getTo()).body(body).end();
      }
    },
    ActivityMentionPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = TemplateContext.newChannelInstance(channelId, notification.getKey().getId(), language);
        
        String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
        ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
        Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
        templateContext.put("READ", (notification.isHasRead()) ? "read" : "unread");
        templateContext.put("NOTIFICATION_ID", notification.getId());
        templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgo(notification.getLastModifiedDate().getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
        templateContext.put("USER", identity.getProfile().getFullName());
        templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(identity.getProfile()));
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("ACTIVITY", NotificationUtils.removeLinkTitle(activity.getTitle()));
        
        // In case of mention on a comment, we need provide the id of the activity, not of the comment
        if (activity.isComment()) {
          ExoSocialActivity parentActivity = Utils.getActivityManager().getParentActivity(activity);
          activityId = parentActivity.getId();
          templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity_highlight_comment", activityId + "-" + activity.getId()));
        } else {
          templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity", activityId));
        }
        String body = TemplateUtils.processGroovy(templateContext);
        return messageInfo.to(notification.getTo()).body(body).end();
      }
    },
    LikePlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = TemplateContext.newChannelInstance(channelId, notification.getKey().getId(), language);
        String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
        ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
        Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getValueOwnerParameter("likersId"), true);
        templateContext.put("READ", (notification.isHasRead()) ? "read" : "unread");
        templateContext.put("NOTIFICATION_ID", notification.getId());
        templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgo(notification.getLastModifiedDate().getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
        templateContext.put("USER", identity.getProfile().getFullName());
        templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(identity.getProfile()));
        templateContext.put("ACTIVITY", NotificationUtils.removeLinkTitle(activity.getTitle()));
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity", activity.getId()));

        String body = TemplateUtils.processGroovy(templateContext);
        return messageInfo.to(notification.getTo()).body(body).end();
      }
    },
    NewUserPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = TemplateContext.newChannelInstance(channelId, notification.getKey().getId(), language);
        String remoteId = notification.getValueOwnerParameter(SocialNotificationUtils.REMOTE_ID.getKey());
        Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteId, true);
        Profile userProfile = identity.getProfile();
        templateContext.put("READ", (notification.isHasRead()) ? "read" : "unread");
        templateContext.put("NOTIFICATION_ID", notification.getId());
        templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgo(TimeConvertUtils.getGreenwichMeanTime().getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
        templateContext.put("USER", userProfile.getFullName());
        templateContext.put("PORTAL_NAME", NotificationPluginUtils.getBrandingPortalName());
        //templateContext.put("PORTAL_HOME", NotificationUtils.getPortalHome(NotificationPluginUtils.getBrandingPortalName()));
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(userProfile));

        String body = TemplateUtils.processGroovy(templateContext);
        return messageInfo.to(notification.getTo()).body(body).end();
      }
    },
    PostActivityPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = TemplateContext.newChannelInstance(channelId, notification.getKey().getId(), language);

        String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
        ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
        Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
        templateContext.put("READ", (notification.isHasRead()) ? "read" : "unread");
        templateContext.put("NOTIFICATION_ID", notification.getId());
        templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgo(notification.getLastModifiedDate().getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
        templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(identity.getProfile()));
        templateContext.put("ACTIVITY", NotificationUtils.removeLinkTitle(activity.getTitle()));
        templateContext.put("USER", identity.getProfile().getFullName());
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity", activity.getId()));
        
        String body = TemplateUtils.processGroovy(templateContext);
        return messageInfo.to(notification.getTo()).body(body).end();
      }
    },
    PostActivitySpaceStreamPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = TemplateContext.newChannelInstance(channelId, notification.getKey().getId(), language);
        String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
        ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
        Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
        
        Identity spaceIdentity = Utils.getIdentityManager().getOrCreateIdentity(SpaceIdentityProvider.NAME, activity.getStreamOwner(), true);
        Space space = Utils.getSpaceService().getSpaceByPrettyName(spaceIdentity.getRemoteId());
        templateContext.put("READ", (notification.isHasRead()) ? "read" : "unread");
        templateContext.put("NOTIFICATION_ID", notification.getId());
        templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgo(notification.getLastModifiedDate().getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
        templateContext.put("USER", identity.getProfile().getFullName());
        templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(identity.getProfile()));
        templateContext.put("ACTIVITY", NotificationUtils.removeLinkTitle(activity.getTitle()));
        templateContext.put("SPACE", spaceIdentity.getProfile().getFullName());
        templateContext.put("SPACE_URL", LinkProviderUtils.getRedirectUrl("space", space.getId()));
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity", activity.getId()));

        String body = TemplateUtils.processGroovy(templateContext);
        return messageInfo.to(notification.getTo()).body(body).end();
      }
    },
    RelationshipReceivedRequestPlugin() {
      private static final String ACCEPT_INVITATION_TO_CONNECT = "social/intranet-notification/confirmInvitationToConnect";
      private static final String REFUSE_INVITATION_TO_CONNECT = "social/intranet-notification/ignoreInvitationToConnect";

      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = TemplateContext.newChannelInstance(channelId, notification.getKey().getId(), language);

        String sender = notification.getValueOwnerParameter("sender");
        String status = notification.getValueOwnerParameter("status");
        String toUser = notification.getTo();
        Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, sender, true);
        Profile userProfile = identity.getProfile();
        templateContext.put("READ", (notification.isHasRead()) ? "read" : "unread");
        templateContext.put("NOTIFICATION_ID", notification.getId());
        templateContext.put("STATUS", status != null && status.equals("accepted") ? "ACCEPTED" : "PENDING");
        templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgo(notification.getLastModifiedDate().getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
        templateContext.put("USER", userProfile.getFullName());
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(userProfile));
        templateContext.put("ACCEPT_CONNECTION_REQUEST_ACTION_URL", LinkProviderUtils.getRestUrl(ACCEPT_INVITATION_TO_CONNECT, sender, toUser));
        templateContext.put("REFUSE_CONNECTION_REQUEST_ACTION_URL", LinkProviderUtils.getRestUrl(REFUSE_INVITATION_TO_CONNECT, sender, toUser));

        String body = TemplateUtils.processGroovy(templateContext);
        return messageInfo.to(notification.getTo()).body(body).end();
      }
    },
    RequestJoinSpacePlugin() {
      private static final String VALIDATE_SPACE_REQUEST = "social/intranet-notification/validateRequestToJoinSpace";
      private static final String REFUSE_SPACE_REQUEST = "social/intranet-notification/refuseRequestToJoinSpace";

      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = TemplateContext.newChannelInstance(channelId, notification.getKey().getId(), language);
        String status = notification.getValueOwnerParameter("status");
        String spaceId = notification.getValueOwnerParameter(SocialNotificationUtils.SPACE_ID.getKey());
        Space space = Utils.getSpaceService().getSpaceById(spaceId);
        Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getValueOwnerParameter("request_from"), true);
        Profile userProfile = identity.getProfile();
        
        templateContext.put("READ", (notification.isHasRead()) ? "read" : "unread");
        templateContext.put("STATUS", status != null && status.equals("accepted") ? "ACCEPTED" : "PENDING");
        templateContext.put("NOTIFICATION_ID", notification.getId());
        templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgo(notification.getLastModifiedDate().getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
        templateContext.put("SPACE", space.getDisplayName());
        templateContext.put("USER", userProfile.getFullName());
        templateContext.put("SPACE_URL", LinkProviderUtils.getRedirectUrl("space_members", space.getId()));
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(userProfile));
        templateContext.put("VALIDATE_SPACE_REQUEST_ACTION_URL", LinkProviderUtils.getRestUrl(VALIDATE_SPACE_REQUEST, space.getId(), identity.getRemoteId()));
        templateContext.put("REFUSE_SPACE_REQUEST_ACTION_URL", LinkProviderUtils.getRestUrl(REFUSE_SPACE_REQUEST, space.getId(), identity.getRemoteId()));

        String body = TemplateUtils.processGroovy(templateContext);
        return messageInfo.to(notification.getTo()).body(body).end();
      }
    },
    SpaceInvitationPlugin() {
      private static final String ACCEPT_SPACE_INVITATION = "social/intranet-notification/acceptInvitationToJoinSpace";
      private static final String REFUSE_SPACE_INVITATION = "social/intranet-notification/ignoreInvitationToJoinSpace";

      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = TemplateContext.newChannelInstance(channelId, notification.getKey().getId(), language);

        String status = notification.getValueOwnerParameter("status");
        String spaceId = notification.getValueOwnerParameter(SocialNotificationUtils.SPACE_ID.getKey());
        Space space = Utils.getSpaceService().getSpaceById(spaceId);
        templateContext.put("READ", (notification.isHasRead()) ? "read" : "unread");
        templateContext.put("STATUS", status != null && status.equals("accepted") ? "ACCEPTED" : "PENDING");
        templateContext.put("NOTIFICATION_ID", notification.getId());
        templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgo(notification.getLastModifiedDate().getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
        templateContext.put("SPACE", space.getDisplayName());
        templateContext.put("SPACE_URL", LinkProviderUtils.getRedirectUrl("space", space.getId()));
        templateContext.put("SPACE_AVATAR", LinkProviderUtils.getSpaceAvatarUrl(space));
        templateContext.put("ACCEPT_SPACE_INVITATION_ACTION_URL", LinkProviderUtils.getRestUrl(ACCEPT_SPACE_INVITATION, space.getId(), notification.getTo()));
        templateContext.put("REFUSE_SPACE_INVITATION_ACTION_URL", LinkProviderUtils.getRestUrl(REFUSE_SPACE_INVITATION, space.getId(), notification.getTo()));
     
        String body = TemplateUtils.processGroovy(templateContext);
        return messageInfo.to(notification.getTo()).body(body).end();
      }
    };
    
    public static MakeMessageInfo get(String id) {
      for (MakeMessageInfo item : values()) {
        if(item.name().equalsIgnoreCase(id)) {
          return item;
        }
      }
      return null;
    }
    protected abstract MessageInfo buildMessage(NotificationContext ctx, String channelId);
  }

}
