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

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang.SerializationUtils;
import org.exoplatform.management.ManagementAware;
import org.exoplatform.management.ManagementContext;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.social.core.storage.cache.model.key.ScopeCacheKey;

public abstract class ByteStaticManager implements ManagementAware {
  
  protected Map<TYPE, HashMap<ScopeCacheKey, Object>> data;
  protected Map<TYPE, ExoCache> dataCache;
  
  protected ManagementContext context;
  
  public enum TYPE {
    activity, activities, activityCount,
    identity, identityIndex, profile, identityCount, identities,
    relationship, relationshipByIdentity, relationshipCount, relationships, suggestion,
    space, spaceSimple, refSpace, spacesCount, spaces
  }
  
  public ByteStaticManager(ViewBean viewBean) {
    viewBean.setViewBean(this);
    //
    data = new HashMap<ByteStaticManager.TYPE, HashMap<ScopeCacheKey, Object>>();
    dataCache = new HashMap<ByteStaticManager.TYPE, ExoCache>();
  }
  
  public void put(TYPE type, Object obj, ScopeCacheKey key) {
    HashMap<ScopeCacheKey, Object> ol = data.get(type);

    if (ol == null) {
      ol = new HashMap<ScopeCacheKey, Object>();
    }
    //
    ol.put(key, obj);
    //
    data.put(type, ol);
  }
  
  public void put(TYPE type, ExoCache cache) {
    dataCache.put(type, cache);
  }
  
  public void registerManager(Object o) {
    if (context != null) {
      context.register(o);
    }
  }

  @Override
  public void setContext(ManagementContext context) {
    this.context = context;
  }


  protected byte[] getbyteObject(Serializable o) {
    return SerializationUtils.serialize(o);
  }

  protected String getInfoCache(TYPE type) throws Exception {
    ExoCache cache = dataCache.get(type);
    if(cache == null || cache.getCacheSize() == 0) {
      return String.valueOf(type) + " { Size: 0, capacity: 0}";
    }
    byte[] b = getbyteObject((LinkedList)cache.getCachedObjects());
    
    return String.valueOf(type) + " { Size: " + String.valueOf(cache.getCacheSize()) + ", capacity: " + b.length + "}";
  }

  protected String getInfo(HashMap<ScopeCacheKey, Object> os) {
    if (os == null || os.isEmpty()) {
      return "{ Size: 0, capacity: 0}";
    }
    byte[] b = getbyteObject(os);
    //
    return "{ Size: " + String.valueOf(os.size()) + ", capacity: " + b.length + "}";
  }
}
