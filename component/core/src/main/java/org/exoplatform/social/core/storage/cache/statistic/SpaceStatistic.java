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
import org.exoplatform.social.core.storage.cache.CachedSpaceStorage;

@Managed
@ManagedDescription("Byte Static Manager")
@NameTemplate({ 
  @Property(key = "service", value = "social"), 
  @Property(key = "view", value = "spaceStorage") 
})
public class SpaceStatistic extends ByteStaticManager {

  public SpaceStatistic(CachedSpaceStorage viewBean) {
    super(viewBean);
  }

  // space, spaceSimple, refSpace, spacesCount, spaces
  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoSpaceData() throws Exception {
    return getInfoCache(TYPE.space);
  }

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoSpaceSimpleData() throws Exception {
    return getInfoCache(TYPE.spaceSimple);
  }

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoRefSpaceData() throws Exception {
    return getInfoCache(TYPE.refSpace);
  }

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoSpacesCountData() throws Exception {
    return getInfoCache(TYPE.spacesCount);
  }

  @Managed
  @ManagedDescription("Turn on the infomation.")
  @Impact(ImpactType.READ)
  public String getInfoSpacesData() throws Exception {
    return getInfoCache(TYPE.spaces);
  }

}
