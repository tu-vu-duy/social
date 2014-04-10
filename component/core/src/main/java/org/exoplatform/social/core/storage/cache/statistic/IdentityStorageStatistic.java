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
import org.exoplatform.social.core.storage.cache.CachedIdentityStorage;


@Managed
@ManagedDescription("Byte Static Manager")
@NameTemplate({ 
  @Property(key = "service", value = "social"), 
  @Property(key = "view", value = "identitystorage")
})
public class IdentityStorageStatistic extends ByteStaticManager {

  public IdentityStorageStatistic(CachedIdentityStorage viewBean) {
    super(viewBean);
  }

  //identity, identityIndex, profile, identityCount, identities

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoIdentityData() throws Exception {
    return getInfoCache(TYPE.identity);
  }

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoIdentityCountData() throws Exception {
    return getInfoCache(TYPE.identityCount);
  }

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoIdentityIndexData() throws Exception {
    return getInfoCache(TYPE.identityIndex);
  }

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoIdentitiesData() throws Exception {
    return getInfoCache(TYPE.identities);
  }

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoProfileData() throws Exception {
    return getInfoCache(TYPE.profile);
  }

  
  
}
