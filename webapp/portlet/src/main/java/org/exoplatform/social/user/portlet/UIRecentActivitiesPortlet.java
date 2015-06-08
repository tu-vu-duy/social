/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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
package org.exoplatform.social.user.portlet;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.webui.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

@ComponentConfig(
  lifecycle = UIApplicationLifecycle.class,
  template = "app:/groovy/social/portlet/user/UIRecentActivitiesPortlet.gtmpl"
)
public class UIRecentActivitiesPortlet extends UIAbstractUserPortlet {
  private static int LATEST_ACTIVITIES_NUM = 5;
  private static int ACTIVITIES_NUM_TO_CHECK = 6;
  protected boolean hasActivityBottomIcon = false;

  public UIRecentActivitiesPortlet() throws Exception {
  }

  public void setHasActivityBottomIcon(boolean hasActivityBottomIcon) {
    this.hasActivityBottomIcon = hasActivityBottomIcon;
  }

  protected List<ExoSocialActivity> getRecentActivities() {
    List<ExoSocialActivity> results = new ArrayList<ExoSocialActivity>();
    RealtimeListAccess<ExoSocialActivity> activitiesListAccess = null;
    if (currentProfile.getIdentity().getId().equals(Utils.getViewerIdentity().getId())) {
      activitiesListAccess = Utils.getActivityManager().getActivitiesWithListAccess(currentProfile.getIdentity());
    } else {
      activitiesListAccess = Utils.getActivityManager().getActivitiesWithListAccess(currentProfile.getIdentity(), Utils.getViewerIdentity());
    }
    
    results = activitiesListAccess.loadAsList(0, LATEST_ACTIVITIES_NUM);
    setHasActivityBottomIcon(activitiesListAccess.loadAsList(0, ACTIVITIES_NUM_TO_CHECK).size() <= LATEST_ACTIVITIES_NUM);
    return results; 
  }

  protected Identity getOwnerActivity(ExoSocialActivity activity) {
    return Utils.getIdentityManager().getIdentity(activity.getUserId(), true);
  }

}
