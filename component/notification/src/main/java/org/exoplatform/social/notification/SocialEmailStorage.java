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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.notification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.exoplatform.commons.api.notification.EmailMessage;


public class SocialEmailStorage {
  final Queue<SocialMessage> pendingMessagesQueue = new ConcurrentLinkedQueue<SocialMessage>();

  public void addEmailNotification(SocialMessage message) {
    pendingMessagesQueue.add(message);
  }

  public Collection<EmailMessage> getEmailNotification() {
    Collection<EmailMessage> pending = new ArrayList<EmailMessage>(pendingMessagesQueue);
    pendingMessagesQueue.clear();
    return pending;
  }
}
