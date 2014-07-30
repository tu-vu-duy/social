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
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.notification.AbstractPluginTest;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          thanhvc@exoplatform.com
 * Aug 20, 2013  
 */
public class SpaceInvitationPluginTest extends AbstractPluginTest {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  @Override
  public AbstractNotificationPlugin getPlugin() {
    return pluginService.getPlugin(NotificationKey.key(SpaceInvitationPlugin.ID));
  }

  public void testSimpleCase() throws Exception {
    //
    Space space = getSpaceInstance(1);
    
    //Invite user to join space
    spaceService.addInvitedUser(space, maryIdentity.getRemoteId());
    List<NotificationInfo> list = assertMadeNotifications(1);
    
    //assert Message Info
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.setNotificationInfo(list.get(0).setTo(rootIdentity.getRemoteId()));
    MessageInfo message = buildMessageInfo(ctx);
    
    assertSubject(message, "You've been invited to join " + space.getDisplayName() + " space");
    assertBody(message, "You've received an invitation to join");
    notificationService.clearAll();
  }
  
  public void testPluginOFF() throws Exception {
    //
    Space space = getSpaceInstance(1);
    
    //Invite user to join space
    spaceService.addInvitedUser(space, rootIdentity.getRemoteId());
    List<NotificationInfo> list = assertMadeNotifications(1);
    
    //assert Message Info
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.setNotificationInfo(list.get(0).setTo(rootIdentity.getRemoteId()));
    MessageInfo message = buildMessageInfo(ctx);
    
    assertSubject(message, "You've been invited to join " + space.getDisplayName() + " space");
    assertBody(message, "You've received an invitation to join");
    notificationService.clearAll();
    
    //OFF
    turnOFF(getPlugin());
    
    //Make invite
    spaceService.addInvitedUser(space, demoIdentity.getRemoteId());
    assertMadeNotifications(0);
    
    //check other plugin
    makeRelationship(johnIdentity, demoIdentity);
    assertMadeNotifications(1);
    
    notificationService.clearAll();
    //
    turnON(getPlugin());
  }
  
  public void testPluginON() throws Exception {
    //OFF
    turnOFF(getPlugin());
    //
    Space space = getSpaceInstance(1);
    //Invite user to join space
    spaceService.addInvitedUser(space, rootIdentity.getRemoteId());
    assertMadeNotifications(0);
    
    //ON
    turnON(getPlugin());
    
    //Make more invitations
    Space space2 = getSpaceInstance(2);
    spaceService.addInvitedUser(space2, rootIdentity.getRemoteId());
    
    List<NotificationInfo> list = assertMadeNotifications(1);
    //assert Message Info
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.setNotificationInfo(list.get(0).setTo(rootIdentity.getRemoteId()));
    MessageInfo message = buildMessageInfo(ctx);
    
    assertSubject(message, "You've been invited to join " + space2.getDisplayName() + " space");
    assertBody(message, "You've received an invitation to join");
    notificationService.clearAll();
  }
  
  public void testDigestWithPluginOFF() throws Exception {
    //Make more invitations
    Space space1 = getSpaceInstance(1);
    Space space2 = getSpaceInstance(2);
    spaceService.addInvitedUser(space1, rootIdentity.getRemoteId());
    spaceService.addInvitedUser(space2, rootIdentity.getRemoteId());
    
    //assert Digest message
    List<NotificationInfo> ntfs = assertMadeNotifications(2);
    List<NotificationInfo> messages = new ArrayList<NotificationInfo>();
    for (NotificationInfo m : ntfs) {
      m.setTo(rootIdentity.getRemoteId());
      messages.add(m);
    }
    Writer writer = new StringWriter();
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.setNotificationInfos(messages);
    getPlugin().buildDigest(ctx, writer);
    
    assertDigest(writer, "You have been asked to joing the following spaces: my space 1, my space 2.");
    notificationService.clearAll();
    
    //OFF
    turnOFF(getPlugin());
    //
    Space space3 = getSpaceInstance(3);
    spaceService.addInvitedUser(space3, rootIdentity.getRemoteId());
    assertMadeNotifications(0);
    
    //other plugin
    makeRelationship(johnIdentity, demoIdentity);
    assertMadeNotifications(1);
    
    notificationService.clearAll();
    //
    turnON(getPlugin());
  }
  
  public void testDigestWithPluginON() throws Exception {
    //OFF
    turnOFF(getPlugin());
    //
    Space space = getSpaceInstance(1);
    //Invite user to join space
    spaceService.addInvitedUser(space, rootIdentity.getRemoteId());
    assertMadeNotifications(0);
    
    //ON
    turnON(getPlugin());
    
    //Make more invitations
    Space space2 = getSpaceInstance(2);
    Space space3 = getSpaceInstance(3);
    spaceService.addInvitedUser(space2, rootIdentity.getRemoteId());
    spaceService.addInvitedUser(space3, rootIdentity.getRemoteId());
    
    //assert Digest message
    List<NotificationInfo> ntfs = assertMadeNotifications(2);
    List<NotificationInfo> messages = new ArrayList<NotificationInfo>();
    for (NotificationInfo m : ntfs) {
      m.setTo(rootIdentity.getRemoteId());
      messages.add(m);
    }
    Writer writer = new StringWriter();
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.setNotificationInfos(messages);
    getPlugin().buildDigest(ctx, writer);
    
    assertDigest(writer, "You have been asked to joing the following spaces: my space 2, my space 3.");
    notificationService.clearAll();
    
  }
  
  public void testDigestWithFeatureOFF() throws Exception {
    //Make more invitations
    Space space1 = getSpaceInstance(1);
    Space space2 = getSpaceInstance(2);
    spaceService.addInvitedUser(space1, rootIdentity.getRemoteId());
    spaceService.addInvitedUser(space2, rootIdentity.getRemoteId());
    
    //assert Digest message
    List<NotificationInfo> ntfs = assertMadeNotifications(2);
    List<NotificationInfo> messages = new ArrayList<NotificationInfo>();
    for (NotificationInfo m : ntfs) {
      m.setTo(rootIdentity.getRemoteId());
      messages.add(m);
    }
    Writer writer = new StringWriter();
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.setNotificationInfos(messages);
    getPlugin().buildDigest(ctx, writer);
    
    assertDigest(writer, "You have been asked to joing the following spaces: my space 1, my space 2.");
    notificationService.clearAll();
    
    //OFF Feature
    turnFeatureOff();
    //
    Space space3 = getSpaceInstance(3);
    spaceService.addInvitedUser(space3, rootIdentity.getRemoteId());
    makeRelationship(johnIdentity, demoIdentity);
    assertMadeNotifications(0);
    
    //
    turnFeatureOn();
  }
  
  public void testDigestWithFeatureON() throws Exception {
    //
    turnFeatureOff();
    
    //Make invitation
    Space space1 = getSpaceInstance(1);
    spaceService.addInvitedUser(space1, rootIdentity.getRemoteId());
    assertMadeNotifications(0);
    
    //ON
    turnFeatureOn();
    
    Space space2 = getSpaceInstance(2);
    Space space3 = getSpaceInstance(3);
    spaceService.addInvitedUser(space2, rootIdentity.getRemoteId());
    spaceService.addInvitedUser(space3, rootIdentity.getRemoteId());
    //assert Digest message
    List<NotificationInfo> ntfs = assertMadeNotifications(2);
    List<NotificationInfo> messages = new ArrayList<NotificationInfo>();
    for (NotificationInfo m : ntfs) {
      m.setTo(rootIdentity.getRemoteId());
      messages.add(m);
    }
    Writer writer = new StringWriter();
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.setNotificationInfos(messages);
    getPlugin().buildDigest(ctx, writer);
    
    assertDigest(writer, "You have been asked to joing the following spaces: my space 2, my space 3.");
    notificationService.clearAll();
    
  }
  public void testDigestCancelInvitation() throws Exception {
    //Make more invitations
    Space space1 = getSpaceInstance(1);
    Space space2 = getSpaceInstance(2);
    Space space3 = getSpaceInstance(3);
    Space space4 = getSpaceInstance(4);
    Space space5 = getSpaceInstance(5);
    spaceService.addInvitedUser(space1, demoIdentity.getRemoteId());
    spaceService.addInvitedUser(space2, demoIdentity.getRemoteId());
    spaceService.addInvitedUser(space3, demoIdentity.getRemoteId());
    spaceService.addInvitedUser(space4, demoIdentity.getRemoteId());
    spaceService.addInvitedUser(space5, demoIdentity.getRemoteId());
    
    //assert Digest message
    List<NotificationInfo> ntfs = assertMadeNotifications(5);
    List<NotificationInfo> messages = new ArrayList<NotificationInfo>();
    for (NotificationInfo m : ntfs) {
      m.setTo(demoIdentity.getRemoteId());
      messages.add(m);
    }
    
    //space2 cancel invitation to demo
    spaceService.removeInvitedUser(space2, demoIdentity.getRemoteId());
    
    Writer writer = new StringWriter();
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.setNotificationInfos(messages);
    getPlugin().buildDigest(ctx, writer);
    
    assertDigest(writer, "You have been asked to joing the following spaces: my space 1, my space 3, my space 4 and 1 others.");
    notificationService.clearAll();
    
  }  
}
