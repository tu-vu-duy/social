/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.notification.plugin;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;

public class ActivityMentionPlugin extends AbstractNotificationPlugin {
  
  public static final String ID = "ActivityMentionPlugin";
  
  public ActivityMentionPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public NotificationInfo makeNotification(NotificationContext ctx) {
    ExoSocialActivity activity = ctx.value(SocialNotificationUtils.ACTIVITY);
    
    Set<String> receivers = new HashSet<String>();
    Utils.sendToMentioners(receivers, activity.getMentionedIds(), activity.getPosterId());

    return NotificationInfo.instance().key(getKey())
           .to(new ArrayList<String>(receivers))
           .with("poster", Utils.getUserId(activity.getPosterId()))
           .with(SocialNotificationUtils.ACTIVITY_ID.getKey(), activity.getId())
           .end();
  }

  @Override
  public MessageInfo makeMessage(NotificationContext ctx) {
    MessageInfo messageInfo = new MessageInfo();
    
    NotificationInfo notification = ctx.getNotificationInfo();
    String language = getLanguage(notification);

    TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
    SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);
    
    String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
    ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
    Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);

    templateContext.put("USER", identity.getProfile().getFullName());
    String subject = TemplateUtils.processSubject(templateContext);
    
    // In case of mention on a comment, we need provide the id of the activity, not of the comment
    if (activity.isComment()) {
      ExoSocialActivity parentActivity = Utils.getActivityManager().getParentActivity(activity);
      activityId = parentActivity.getId();
      templateContext.put("REPLY_ACTION_URL", LinkProviderUtils.getRedirectUrl("reply_activity_highlight_comment", activityId + "-" + activity.getId()));
      templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity_highlight_comment", activityId + "-" + activity.getId()));
    } else {
      templateContext.put("REPLY_ACTION_URL", LinkProviderUtils.getRedirectUrl("reply_activity", activityId));
      templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity", activityId));
    }
    
    templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(identity.getProfile()));
    templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
    templateContext.put("ACTIVITY", Utils.processMentions(activity.getTitle()));
    String body = TemplateUtils.processGroovy(templateContext);
    
    return messageInfo.subject(subject).body(body).end();
  }

  @Override
  public boolean makeDigest(NotificationContext ctx, Writer writer) {
    List<NotificationInfo> notifications = ctx.getNotificationInfos();
    NotificationInfo first = notifications.get(0);

    String language = getLanguage(first);
    TemplateContext templateContext = new TemplateContext(first.getKey().getId(), language);
    
    Map<String, List<String>> receiverMap = new LinkedHashMap<String, List<String>>();
    try {
      for (NotificationInfo notification : notifications) {
        String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
        ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
        Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
        
        if (activity.isComment()) {
          activity = Utils.getActivityManager().getParentActivity(activity);
        }

        //make the list receivers who will send mail to them.
        SocialNotificationUtils.processInforSendTo(receiverMap, activity.getId(), identity.getRemoteId());
      }
      writer.append(SocialNotificationUtils.getMessageByIds(receiverMap, templateContext));
    } catch (IOException e) {
      ctx.setException(e);
      return false;
    }
    
    return true;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    ExoSocialActivity activity = ctx.value(SocialNotificationUtils.ACTIVITY);
    return activity.getMentionedIds().length > 0; 
  }
}
