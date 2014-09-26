package org.exoplatform.social.extras.injection;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.relationship.model.Relationship;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
public class RelationshipInjector extends AbstractSocialInjector {

  /** . */
  private static final String NUMBER = "number";

  /** . */
  private static final String FROM_USER = "fromUser";

  /** . */
  private static final String TO_USER = "toUser";

  /** . */
  private static final String PREFIX = "prefix";

  private static final String IS_BY_USER = "byUser";

  private static final String SRC_USER = "srcUser";
  
  public RelationshipInjector(PatternInjectorConfig pattern) {
    super(pattern);
  }

  
  @Override
  public void inject(HashMap<String, String> params) throws Exception {
    
    //
    int number = param(params, NUMBER);
    int from = param(params, FROM_USER);
    int to = param(params, TO_USER);
    String prefix = params.get(PREFIX);
    boolean byNumber = false;
    if(params.containsKey(IS_BY_USER)) {
      byNumber = Boolean.valueOf(params.get(IS_BY_USER));
    }
    
    init(prefix, null, userSuffixValue, spaceSuffixValue);
    //
    String srcUser = "";
    if(params.containsKey(SRC_USER)) {
      srcUser = params.get(SRC_USER);
    }
    //
    if (number <= 0 && !srcUser.isEmpty()) {
      getLog().error("Number have to be positive. Value '" + number + "' incorrect. Aborting injection ...");
      return;
    }
    if (from > 0 && to > 0 && to < from) {
      getLog().error("Number form must be less than to. Aborting injection ...");
      return;
    }
    //
    if (!srcUser.isEmpty()) {
      if (from < 0) {
        from = 0;
      }
      if (from > 0 && to > 0) {
        number = to;
      } else if (from > 0 && to == 0) {
        number += from;
      }
      //
      number = Math.min(number, userNumber);
      for (int i = from; i < number; i++) {
        addRelationship(srcUser, (prefix + i));
      }
      return;
    } else if (byNumber) {
      //
      number = Math.min(number, userNumber);
      boolean hasContinue = false;
      for (int i = from; i <= to; i++) {
        String fromUser = prefix + i;
        for (int j = 0; j < number; j++) {
          if (i == j) {
            hasContinue = true;
            continue;
          }

          if (!hasContinue && i == (to - 1)) {
            continue;
          }
          String toUser = prefix + j;
          //
          addRelationship(fromUser, toUser);
        }
      }
      return;
    }

    // Check if possible and adjust number if needed.
    Map<Integer, Integer> computed = compute(from, to, number);
    getLog().info("About to inject relationships :");
    for (Map.Entry<Integer, Integer> e : computed.entrySet()) {
      getLog().info("" + e.getKey() + " user(s) with " + e.getValue() + " connection(s)");

    }

    int floor = from;
    for (Map.Entry<Integer, Integer> e : computed.entrySet()) {
      generate(e, floor);
      floor += e.getKey();
    }

  }

  private void generate(Map.Entry<Integer, Integer> e, int floor) {
    for (int i = floor; i < floor + e.getKey(); ++i) {
      for (int j = floor; j < floor + e.getKey(); ++j) {
        //
        String fromUser = this.userNameSuffixPattern(i);
        String toUser = this.userNameSuffixPattern(j);
        //
        if (i > j) {
          getLog().info("Relationship between " + fromUser + " and " + toUser + " already exists");
        } else if (i == j) {
          continue;
        } else {
          //
          addRelationship(fromUser, toUser);
        }
      }
    }
  }
  
  
  private void addRelationship(String srcUser, String destUser) {
    //
    Identity identity1 = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, srcUser, false);
    Identity identity2 = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, destUser, false);

    //
    Relationship r = new Relationship(identity1, identity2, Relationship.Type.CONFIRMED);
    relationshipManager.update(r);

    //
    getLog().info("Relationship between " + srcUser + " and " + destUser + " generated");
  }

  /**
   * @param a begin range
   * @param b end range
   * @param c number
   */
  public boolean isPossible(int a, int b, int c) {

    return (b - a + 1F) % (c + 1F) == 0;

  }

  /**
   * @param a begin range
   * @param b end range
   * @param c number
   */
  public Map<Integer, Integer> compute(int a, int b, int c) {

    Map<Integer, Integer> result = new LinkedHashMap<Integer, Integer>();

    // number too big, set maximum
    if (c > b - a) {
      result.put(b - a + 1, b - a);
    }
    // exact is possible
    else if (isPossible(a, b, c)) {
      result.put(b - a + 1, c);
    }
    // compute result
    else {
      int group = (int) ((b - a + 1F) / (c + 1F));
      int exact = group * (c + 1);
      int remaining = (b - a + 1) - exact;
      result.put(exact, c);
      result.put(remaining, remaining - 1);
    }

    return result;
    
  }

}
