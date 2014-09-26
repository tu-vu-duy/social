package org.exoplatform.social.extras.injection;

import java.util.HashMap;

import org.apache.commons.math3.util.Precision;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.extras.injection.utils.LoremIpsum4J;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
public class ActivityInjector extends AbstractSocialInjector {

  /** . */
  private static final String NUMBER = "number";

  /** . */
  private static final String FROM_USER = "fromUser";

  /** . */
  private static final String TO_USER = "toUser";

  /** . */
  private static final String TYPE = "type";

  /** . */
  private static final String USER_PREFIX = "userPrefix";

  /** . */
  private static final String SPACE_PREFIX = "spacePrefix";
  
  public ActivityInjector(PatternInjectorConfig pattern) {
    super(pattern);
  }

  @Override
  public void inject(HashMap<String, String> params) throws Exception {
    
    //
    int number = param(params, NUMBER);
    int from = param(params, FROM_USER);
    int to = param(params, TO_USER);
    String type = params.get(TYPE);
    String userPrefix = params.get(USER_PREFIX);
    String spacePrefix = params.get(SPACE_PREFIX);
    init(userPrefix, spacePrefix, userSuffixValue, spaceSuffixValue);

    if (!"space".equals(type) && !"user".equals(type)) {
      getLog().info("'" + type + "' is a wrong value for type parameter. Please set it to 'user' or 'space'. Aborting injection ..." );
      return;
    }

    // Init provider and base name
    String provider = null;
    if ("space".equals(type)) {
      provider = SpaceIdentityProvider.NAME;
    }
    else if ("user".equals(type)) {
      provider = OrganizationIdentityProvider.NAME;
    }

    String fromUser;
    long timeCreateActivities = 0;
    long timeLoadActivities = 0;
    long numberCreatedAndLoadActivities = 0;
    long allTime = 0;
    try {
      for(int i = from; i <= to; ++i) {
        //
        if (provider.equalsIgnoreCase(OrganizationIdentityProvider.NAME)) {
          fromUser = this.userNameSuffixPattern(i);
        } else {
          fromUser = this.spaceNameSuffixPattern(i);
          fromUser = fromUser.replace(".", "");
        }
        
        Identity identity = identityManager.getOrCreateIdentity(provider, fromUser, false);
        RealtimeListAccess<ExoSocialActivity> listAccess = activityManager.getActivityFeedWithListAccess(identity);

        for (int j = 0; j < number; ++j) {
          //
          long startCreate = System.currentTimeMillis();
          ExoSocialActivity activity = new ExoSocialActivityImpl();
          lorem = new LoremIpsum4J();
          activity.setBody(lorem.getWords(10));
          activity.setTitle(lorem.getParagraphs());
          activityManager.saveActivity(identity, "DEFAULT_ACTIVITY", activity.getTitle()); //
          getLog().info("Activity for " + fromUser + " generated");
          //
          long doneCreate = System.currentTimeMillis();
          timeCreateActivities += (doneCreate - startCreate);
          //
          getLog().info("Loading 20 activities .....");
          listAccess.load(0, 20);
          //
          timeLoadActivities += (System.currentTimeMillis() - doneCreate);
          //
          allTime += (System.currentTimeMillis() - startCreate);
          //
          ++numberCreatedAndLoadActivities;
        }
      }
    } catch (Exception e) {
    } finally {
      getLog().info("\n\n======================= RESULTS =======================\n\n");
      getLog().info("All activities generated             : " + numberCreatedAndLoadActivities);
      getLog().info("All time to generated activities     : " + timeCreateActivities  + "ms");
      getLog().info("All time to loaded activities        : " + timeLoadActivities  + "ms");
      getLog().info("All time to load and generated       : " + allTime  + "ms");
      //
      if(numberCreatedAndLoadActivities > 0) {
        getLog().info("Average time to generate one activity: " + (timeCreateActivities/numberCreatedAndLoadActivities)  + "ms");
        getLog().info("Average time to load 20 activities   : " + (timeLoadActivities/numberCreatedAndLoadActivities)  + "ms");
      }
      if(numberCreatedAndLoadActivities <= 0) numberCreatedAndLoadActivities = 1;
      getLog().info("\n\n======================= RESULTS TABLE =======================\n\n");
      getLog().info(String.format("\n|  Activities  |  Time to generated  |  Time to loaded  |  Time to load + generated  |" +
      		"  Average time to generate  |  Average time to load 20  |\n|%s%s%s%s%s%s\n",
      		buildInfo(11, String.valueOf(numberCreatedAndLoadActivities), ""),
          buildInfo(18, String.valueOf(timeCreateActivities), ""),
          buildInfo(15, String.valueOf(timeLoadActivities), ""),
          buildInfo(25, String.valueOf(allTime), ""),
          buildInfo(25, String.valueOf(Precision.round(timeCreateActivities*1f/numberCreatedAndLoadActivities, 3)), "ms"),
          buildInfo(24, String.valueOf(Precision.round(timeLoadActivities*1f/numberCreatedAndLoadActivities, 3)), "ms")));
      
      getLog().info("\n=======================================================\n\n");
    }

  }
  private String sp = "                                            ";

  private String buildInfo(int length, String val, String type) {
    int l = (val + type).length();
    return "   " + val + type + ((length - l > 0) ? sp.substring(0, length - l) : "") + "|";
  }
  
}
