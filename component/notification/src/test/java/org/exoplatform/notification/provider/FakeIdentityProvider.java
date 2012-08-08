package org.exoplatform.notification.provider;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.social.core.identity.IdentityProvider;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;

public class FakeIdentityProvider extends IdentityProvider<Application> {

  /** The Constant NAME. */
  public final static String  NAME = "apps";


  private static Map<String,Application> appsByUrl = new HashMap<String,Application>();

  @Override
  public Application findByRemoteId(String remoteId) {
    return appsByUrl.get(remoteId);
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Identity createIdentity(Application app) {
    Identity identity = new Identity(NAME, app.getId());
    return identity;
  }


  public void addApplication(Application app) {
    appsByUrl.put(app.getId(), app);
  }

  @Override
  public void populateProfile(Profile profile, Application app) {
    profile.setProperty(Profile.USERNAME, app.getName());
    profile.setProperty(Profile.FIRST_NAME, app.getName());
    profile.setAvatarUrl(app.getIcon());
    profile.setUrl(app.getUrl());
  }

}
