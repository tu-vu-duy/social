/*
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
 */
package org.exoplatform.social.core.storage.cache.statistic;

import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.social.core.storage.cache.CachedRelationshipStorage;

@Managed
@ManagedDescription("Byte Static Manager")
@NameTemplate({ 
  @Property(key = "service", value = "social"), 
  @Property(key = "view", value = "relationshipStorage")
})
public class RelationshipStatistic extends ByteStaticManager {

  public RelationshipStatistic(CachedRelationshipStorage viewBean) {
    super(viewBean);
  }
  //relationship, relationshipByIdentity, relationshipCount, relationships, suggestion,
  
  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoRelationshipData() throws Exception {
    return getInfoCache(TYPE.relationship);
  }

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoRelationshipByIdentityData() throws Exception {
    return getInfoCache(TYPE.relationshipByIdentity);
  }

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoRelationshipsCountData() throws Exception {
    return getInfoCache(TYPE.relationshipCount);
  }

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoRelationshipsData() throws Exception {
    return getInfoCache(TYPE.relationships);
  }

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoSuggestionData() throws Exception {
    return getInfoCache(TYPE.suggestion);
  }

}
