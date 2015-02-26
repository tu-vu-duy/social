/***************************************************************************
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 ***************************************************************************/
package org.exoplatform.social.user.portlet;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.webui.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

@ComponentConfig(
  lifecycle = UIApplicationLifecycle.class,
  template = "app:/groovy/social/portlet/user/UIMiniConnectionsPortlet.gtmpl"
)
public class UIMiniConnectionsPortlet extends UIAbstractUserPortlet {
  protected final static int MAX_DISPLAY = 6;
  private int allSize = 0;

  public UIMiniConnectionsPortlet() throws Exception {
  }

  protected List<ProfileBean> loadPeoples() throws Exception {
    ListAccess<Identity> listAccess = Utils.getRelationshipManager().getConnectionsByFilter(currentProfile.getIdentity(), new ProfileFilter());
    Identity[] identities = listAccess.load(0, MAX_DISPLAY);
    allSize = listAccess.getSize();
    List<ProfileBean> profileBeans = new ArrayList<ProfileBean>();
    for (int i = 0; i < identities.length; i++) {
      profileBeans.add(new ProfileBean(identities[i]));
    }
    return profileBeans;
  }
  
  protected int getAllSize() {
    return allSize;
  }
  
  protected class ProfileBean {
    private final String avatarURL;
    private final String displayName;
    private final String profileURL;
    private final String userId;

    public ProfileBean(Identity identity) {
      this.userId = identity.getRemoteId();
      //
      Profile profile = identity.getProfile();
      this.displayName = profile.getFullName();
      this.profileURL = profile.getUrl();
      String avatarURL = profile.getAvatarUrl();
      if (UserProfileHelper.isEmpty(avatarURL) || avatarURL.equalsIgnoreCase("null")) {
        avatarURL = "/social-resources/skin/images/ShareImages/UserAvtDefault.png";
      }
      this.avatarURL = avatarURL;
    }
    public String getUserId() {
      return userId;
    }
    public String getAvatarURL() {
      return avatarURL;
    }
    public String getDisplayName() {
      return displayName;
    }
    public String getProfileURL() {
      return profileURL;
    }
  }
}
