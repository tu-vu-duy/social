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

import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.notification.AbstractPluginTest;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          thanhvc@exoplatform.com
 * Aug 20, 2013  
 */
public class PostActivityPluginTest extends AbstractPluginTest {
  
  public void testSimpleCase() throws Exception {
    //STEP 1 post activity
    makeActivity(demoIdentity, "demo post activity on activity stream of root");
    
    List<NotificationInfo> list = assertMadeNotifications(1);
    NotificationInfo postActivityNotification = list.get(0);
    
    //STEP 2 assert Message info
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.setNotificationInfo(postActivityNotification.setTo("root"));
    MessageInfo info = buildMessageInfo(ctx);
    
    assertSubject(info, "Demo gtn posted on your activity stream");
    assertBody(info, "New post on your activity stream");
    assertBody(info, "demo post activity on activity stream of root");
  }
  
  public void testPluginONOFF() throws Exception {
    //by default the plugin is on
    makeActivity(demoIdentity, "demo post activity on activity stream of root");
    assertMadeNotifications(1);
    notificationService.clearAll();
    
    //turn off the plugin
    turnOFF(getPlugin());
    
    makeActivity(johnIdentity, "john post activity on activity stream of root");
    assertMadeNotifications(0);
    
    //turn on the plugin
    turnON(getPlugin());
    makeActivity(demoIdentity, "demo post activity on activity stream of root");
    assertMadeNotifications(1);
    notificationService.clearAll();
  }
  
  public void testFeatureONOFF() throws Exception {
    //by default the feature is ON
    makeActivity(demoIdentity, "demo post activity on activity stream of root");
    assertMadeNotifications(1);
    notificationService.clearAll();
    
    //turn off the feature
    turnFeatureOff();
    makeActivity(maryIdentity, "mary post activity on activity stream of root");
    assertMadeNotifications(0);
    
    //turn on the feature
    turnFeatureOn();
    makeActivity(johnIdentity, "john post activity on activity stream of root");
    assertMadeNotifications(1);
    notificationService.clearAll();
  }
  
  public void testDigest() throws Exception {
    //config setting of root to receive notification daily
    UserSetting rootSetting = new UserSetting();
    rootSetting.setUserId(rootIdentity.getRemoteId());
    rootSetting.setDailyProviders(Arrays.asList(getPlugin().getId()));
    userSettingService.save(rootSetting);
    
    //create new user
    Identity ghostIdentity = identityManager.getOrCreateIdentity("organization", "ghost", true);
    notificationService.clearAll();
    
    makeActivity(demoIdentity, "demo post activity on activity stream of root");
    makeActivity(maryIdentity, "mary post activity on activity stream of root");
    makeActivity(johnIdentity, "john post activity on activity stream of root");
    makeActivity(ghostIdentity, "ghost post activity on activity stream of root");
    
    //Digest
    List<NotificationInfo> list = assertMadeNotifications(4);
    
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    list.set(0, list.get(0).setTo(rootIdentity.getRemoteId()));
    ctx.setNotificationInfos(list);
    Writer writer = new StringWriter();
    getPlugin().buildDigest(ctx, writer);
    assertDigest(writer, "Demo gtn, Mary Kelly, John Anthony and 1 others posted on your activity stream.");
    
    tearDownIdentityList.add(ghostIdentity);
  }
  
  public void testDigestWithSameUser() throws Exception {
    makeActivity(demoIdentity, "demo post activity on activity stream of root");
    makeActivity(demoIdentity, "mary post activity on activity stream of root");
    makeActivity(johnIdentity, "john post activity on activity stream of root");
    
    //Digest
    List<NotificationInfo> list = assertMadeNotifications(3);
    
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    list.set(0, list.get(0).setTo(rootIdentity.getRemoteId()));
    ctx.setNotificationInfos(list);
    Writer writer = new StringWriter();
    getPlugin().buildDigest(ctx, writer);
    assertDigest(writer, "Demo gtn, John Anthony posted on your activity stream.");
  }
  
  public void testDigestWithDeletedActivity() throws Exception {
    ExoSocialActivity demoActivity = makeActivity(demoIdentity, "demo post activity on activity stream of root");
    makeActivity(johnIdentity, "john post activity on activity stream of root");
    
    //Digest
    List<NotificationInfo> list = assertMadeNotifications(2);
    
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    list.set(0, list.get(0).setTo(rootIdentity.getRemoteId()));
    ctx.setNotificationInfos(list);
    
    //demo delete his activity
    activityManager.deleteActivity(demoActivity);
    tearDownActivityList.remove(demoActivity);
    
    Writer writer = new StringWriter();
    getPlugin().buildDigest(ctx, writer);
    assertDigest(writer, "John Anthony posted on your activity stream.");
  }
  
  @Override
  public AbstractNotificationPlugin getPlugin() {
    return pluginService.getPlugin(NotificationKey.key(PostActivityPlugin.ID));
  }

}
