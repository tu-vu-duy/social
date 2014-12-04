/**
 * 
 */
package org.exoplatform.social.notification.template;

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

@ChannelConfigs (
   id = "email",
   templates = {
       @TemplateConfig( pluginId=ActivityCommentPlugin.ID, path="war:/notification/templates/ActivityCommentPlugin.gtmpl"),
       @TemplateConfig( pluginId=ActivityMentionPlugin.ID, path="war:/notification/templates/ActivityMentionPlugin.gtmpl"),
       @TemplateConfig( pluginId=LikePlugin.ID, path="war:/notification/templates/LikePlugin.gtmpl"),
       @TemplateConfig( pluginId=NewUserPlugin.ID, path="war:/notification/templates/NewUserPlugin.gtmpl"),
       @TemplateConfig( pluginId=PostActivityPlugin.ID, path="war:/notification/templates/PostActivityPlugin.gtmpl"),
       @TemplateConfig( pluginId=PostActivitySpaceStreamPlugin.ID, path="war:/notification/templates/PostActivitySpaceStreamPlugin.gtmpl"),
       @TemplateConfig( pluginId=RelationshipReceivedRequestPlugin.ID, path="war:/notification/templates/RelationshipReceivedRequestPlugin.gtmpl"),
       @TemplateConfig( pluginId=RequestJoinSpacePlugin.ID, path="war:/notification/templates/RequestJoinSpacePlugin.gtmpl"),
       @TemplateConfig( pluginId=SpaceInvitationPlugin.ID, path="war:/notification/templates/SpaceInvitationPlugin.gtmpl")
   }
)
public class ActivityEmailTemplateHandler extends AbstractChannelTemplateHandler {

  /**
   * 
   */
  public ActivityEmailTemplateHandler() {
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
        
        TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
        templateContext.put("USER", identity.getProfile().getFullName());
        String subject = TemplateUtils.processSubject(templateContext);
        
        SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("COMMENT", NotificationUtils.processLinkTitle(activity.getTitle()));
        templateContext.put("REPLY_ACTION_URL", LinkProviderUtils.getRedirectUrl("reply_activity_highlight_comment", parentActivity.getId() + "-" + activity.getId()));
        templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity_highlight_comment", parentActivity.getId() + "-" + activity.getId()));

        String body = SocialNotificationUtils.getBody(ctx, templateContext, parentActivity);
        
        return messageInfo.subject(subject).body(body).end();
      }
    },
    ActivityMentionPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());

        TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
        SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);
        
        String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
        ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
        Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);

        templateContext.put("USER", identity.getProfile().getFullName());
        String subject = TemplateUtils.processSubject(templateContext);
        
        templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(identity.getProfile()));
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        String body = "";
        
        // In case of mention on a comment, we need provide the id of the activity, not of the comment
        if (activity.isComment()) {
          ExoSocialActivity parentActivity = Utils.getActivityManager().getParentActivity(activity);
          activityId = parentActivity.getId();
          templateContext.put("REPLY_ACTION_URL", LinkProviderUtils.getRedirectUrl("reply_activity_highlight_comment", activityId + "-" + activity.getId()));
          templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity_highlight_comment", activityId + "-" + activity.getId()));
          templateContext.put("ACTIVITY", NotificationUtils.processLinkTitle(activity.getTitle()));
          body = TemplateUtils.processGroovy(templateContext);
        } else {
          templateContext.put("REPLY_ACTION_URL", LinkProviderUtils.getRedirectUrl("reply_activity", activityId));
          templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity", activityId));
          body = SocialNotificationUtils.getBody(ctx, templateContext, activity);
        }
        
        return messageInfo.subject(subject).body(body).end();
      }
    },
    LikePlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
        SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);
        
        String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
        ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
        Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getValueOwnerParameter("likersId"), true);
        
        templateContext.put("USER", identity.getProfile().getFullName());
        templateContext.put("SUBJECT", activity.getTitle());
        String subject = TemplateUtils.processSubject(templateContext);

        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("REPLY_ACTION_URL", LinkProviderUtils.getRedirectUrl("reply_activity", activity.getId()));
        templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity", activity.getId()));

        String body = SocialNotificationUtils.getBody(ctx, templateContext, activity);
        
        return messageInfo.subject(subject).body(body).end();
      }
    },
    NewUserPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
        SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);

        String remoteId = notification.getValueOwnerParameter(SocialNotificationUtils.REMOTE_ID.getKey());
        Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteId, true);
        Profile userProfile = identity.getProfile();
        
        templateContext.put("USER", userProfile.getFullName());
        templateContext.put("PORTAL_NAME", NotificationPluginUtils.getBrandingPortalName());
        templateContext.put("PORTAL_HOME", NotificationUtils.getPortalHome(NotificationPluginUtils.getBrandingPortalName()));
        String subject = TemplateUtils.processSubject(templateContext);
        
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(userProfile));
        templateContext.put("CONNECT_ACTION_URL", LinkProviderUtils.getInviteToConnectUrl(identity.getRemoteId(), notification.getTo()));
        String body = TemplateUtils.processGroovy(templateContext);
        
        return messageInfo.subject(subject).body(body).end();
      }
    },
    PostActivityPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
        SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);

        String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
        ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
        Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
        
        
        templateContext.put("USER", identity.getProfile().getFullName());
        templateContext.put("SUBJECT", activity.getTitle());
        String subject = TemplateUtils.processSubject(templateContext);
        
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("REPLY_ACTION_URL", LinkProviderUtils.getRedirectUrl("reply_activity", activity.getId()));
        templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity", activity.getId()));
        
        String body = SocialNotificationUtils.getBody(ctx, templateContext, activity);
        
        return messageInfo.subject(subject).body(body).end();
      }
    },
    PostActivitySpaceStreamPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
        SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);

        String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
        ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
        Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
        
        Identity spaceIdentity = Utils.getIdentityManager().getOrCreateIdentity(SpaceIdentityProvider.NAME, activity.getStreamOwner(), true);
        
        templateContext.put("USER", identity.getProfile().getFullName());
        templateContext.put("SPACE", spaceIdentity.getProfile().getFullName());
        templateContext.put("SUBJECT", activity.getTitle());
        String subject = TemplateUtils.processSubject(templateContext);
        
        Space space = Utils.getSpaceService().getSpaceByPrettyName(spaceIdentity.getRemoteId());
        templateContext.put("SPACE_URL", LinkProviderUtils.getRedirectUrl("space", space.getId()));
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("REPLY_ACTION_URL", LinkProviderUtils.getRedirectUrl("reply_activity", activity.getId()));
        templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity", activity.getId()));

        String body = SocialNotificationUtils.getBody(ctx, templateContext, activity);
        
        return messageInfo.subject(subject).body(body).end();
      }
    },
    RelationshipReceivedRequestPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);

        String sender = notification.getValueOwnerParameter("sender");
        String toUser = notification.getTo();
        SocialNotificationUtils.addFooterAndFirstName(toUser, templateContext);
        Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, sender, true);
        Profile userProfile = identity.getProfile();
        
        templateContext.put("PORTAL_NAME", System.getProperty("exo.notifications.portalname", "eXo"));
        templateContext.put("USER", userProfile.getFullName());
        String subject = TemplateUtils.processSubject(templateContext);
        
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(userProfile));
        templateContext.put("ACCEPT_CONNECTION_REQUEST_ACTION_URL", LinkProviderUtils.getConfirmInvitationToConnectUrl(sender, toUser));
        templateContext.put("REFUSE_CONNECTION_REQUEST_ACTION_URL", LinkProviderUtils.getIgnoreInvitationToConnectUrl(sender, toUser));
        String body = TemplateUtils.processGroovy(templateContext);

        return messageInfo.subject(subject).body(body).end();
      }
    },
    RequestJoinSpacePlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
        SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);

        String spaceId = notification.getValueOwnerParameter(SocialNotificationUtils.SPACE_ID.getKey());
        Space space = Utils.getSpaceService().getSpaceById(spaceId);
        Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getValueOwnerParameter("request_from"), true);
        Profile userProfile = identity.getProfile();
        
        templateContext.put("SPACE", space.getDisplayName());
        templateContext.put("USER", userProfile.getFullName());
        String subject = TemplateUtils.processSubject(templateContext);
        
        templateContext.put("SPACE_URL", LinkProviderUtils.getRedirectUrl("space_members", space.getId()));
        templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
        templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(userProfile));
        templateContext.put("VALIDATE_SPACE_REQUEST_ACTION_URL", LinkProviderUtils.getValidateRequestToJoinSpaceUrl(space.getId(), identity.getRemoteId()));
        templateContext.put("REFUSE_SPACE_REQUEST_ACTION_URL", LinkProviderUtils.getRefuseRequestToJoinSpaceUrl(space.getId(), identity.getRemoteId()));
        String body = TemplateUtils.processGroovy(templateContext);
        
        return messageInfo.subject(subject).body(body).end();
      }
    },
    SpaceInvitationPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx, String channelId) {
        MessageInfo messageInfo = new MessageInfo();
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
        SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);

        String spaceId = notification.getValueOwnerParameter(SocialNotificationUtils.SPACE_ID.getKey());
        Space space = Utils.getSpaceService().getSpaceById(spaceId);
        
        templateContext.put("SPACE", space.getDisplayName());
        templateContext.put("SPACE_URL", LinkProviderUtils.getRedirectUrl("space", space.getId()));
        String subject = TemplateUtils.processSubject(templateContext);
        
        templateContext.put("SPACE_AVATAR", LinkProviderUtils.getSpaceAvatarUrl(space));
        templateContext.put("ACCEPT_SPACE_INVITATION_ACTION_URL", LinkProviderUtils.getAcceptInvitationToJoinSpaceUrl(space.getId(), notification.getTo()));
        templateContext.put("REFUSE_SPACE_INVITATION_ACTION_URL", LinkProviderUtils.getIgnoreInvitationToJoinSpaceUrl(space.getId(), notification.getTo()));
        String body = TemplateUtils.processGroovy(templateContext);
        
        return messageInfo.subject(subject).body(body).end();
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
