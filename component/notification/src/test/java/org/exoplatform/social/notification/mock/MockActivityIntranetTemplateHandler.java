package org.exoplatform.social.notification.mock;

import org.exoplatform.commons.api.notification.channel.ChannelConfigs;
import org.exoplatform.commons.api.notification.channel.TemplateConfig;
import org.exoplatform.social.notification.plugin.ActivityCommentPlugin;
import org.exoplatform.social.notification.plugin.ActivityMentionPlugin;
import org.exoplatform.social.notification.plugin.LikePlugin;
import org.exoplatform.social.notification.plugin.NewUserPlugin;
import org.exoplatform.social.notification.plugin.PostActivityPlugin;
import org.exoplatform.social.notification.plugin.PostActivitySpaceStreamPlugin;
import org.exoplatform.social.notification.plugin.RelationshipReceivedRequestPlugin;
import org.exoplatform.social.notification.plugin.RequestJoinSpacePlugin;
import org.exoplatform.social.notification.plugin.SpaceInvitationPlugin;
import org.exoplatform.social.notification.template.ActivityIntranetTemplateHandler;
@ChannelConfigs (
   id = "intranet",
   templates = {
       @TemplateConfig( pluginId=ActivityCommentPlugin.ID, path="classpath:/notification/templates/ActivityCommentPlugin.gtmpl"),
       @TemplateConfig( pluginId=ActivityMentionPlugin.ID, path="classpath:/notification/templates/ActivityMentionPlugin.gtmpl"),
       @TemplateConfig( pluginId=LikePlugin.ID, path="classpath:/notification/templates/LikePlugin.gtmpl"),
       @TemplateConfig( pluginId=NewUserPlugin.ID, path="classpath:/notification/templates/NewUserPlugin.gtmpl"),
       @TemplateConfig( pluginId=PostActivityPlugin.ID, path="classpath:/notification/templates/PostActivityPlugin.gtmpl"),
       @TemplateConfig( pluginId=PostActivitySpaceStreamPlugin.ID, path="classpath:/notification/templates/PostActivitySpaceStreamPlugin.gtmpl"),
       @TemplateConfig( pluginId=RelationshipReceivedRequestPlugin.ID, path="classpath:/notification/templates/RelationshipReceivedRequestPlugin.gtmpl"),
       @TemplateConfig( pluginId=RequestJoinSpacePlugin.ID, path="classpath:/notification/templates/RequestJoinSpacePlugin.gtmpl"),
       @TemplateConfig( pluginId=SpaceInvitationPlugin.ID, path="classpath:/notification/templates/SpaceInvitationPlugin.gtmpl")
   }
)
public class MockActivityIntranetTemplateHandler extends ActivityIntranetTemplateHandler {

  public MockActivityIntranetTemplateHandler() {
    super();
  }

}
