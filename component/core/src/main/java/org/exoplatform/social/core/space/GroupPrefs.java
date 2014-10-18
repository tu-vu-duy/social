/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.social.core.space;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.common.utils.GroupNode;
import org.exoplatform.social.common.utils.GroupTree;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 24, 2012  
 */
public class GroupPrefs {
  private static String ON_RESTRICTED_KEY = "SOCIAL_SPACE_ADMIN_ON_RESTRICTED_KEY";
  private static String GROUP_RESTRICTED_KEY = "SOCIAL_SPACE_ADMIN_GROUPS_RESTRICTED_KEY";
  
  private SettingService settingService = null;
  private OrganizationService orgSrv = null;
  
  private static final Log LOG = ExoLogger.getLogger(GroupPrefs.class);
  private static GroupTree treeAllGroups = GroupTree.createInstance();
  private static List<String> restrictedMemberships = new CopyOnWriteArrayList<String>();
  private static List<GroupNode> restrictedNodes = new ArrayList<GroupNode>();
  private boolean isOnRestricted;
  
  public GroupPrefs(SettingService settingService) {
    this.settingService = settingService;
    this.orgSrv = SpaceUtils.getOrganizationService();
    loadSetting();
  }
  
  private void loadSetting() {
    try {
      SettingValue<?> value = this.settingService.get(Context.GLOBAL, Scope.PORTAL, ON_RESTRICTED_KEY);

      // isRestricted value
      if (value != null) {
        Boolean boolValue = (Boolean) value.getValue();
        isOnRestricted = boolValue.booleanValue();
      }

      // restricted memberships
      value = this.settingService.get(Context.GLOBAL, Scope.PORTAL, GROUP_RESTRICTED_KEY);

      if (value != null) {
        String[] memberships = ((String) value.getValue()).split(",");
        try {
          for (String membership : memberships) {
            restrictedMemberships.add(membership);
            restrictedNodes.add(buildGroupNode(membership));
          }
        } catch (Exception e) {
          LOG.warn("Cannot get all restricted memberships.", e);
        }
      }
      
      // all groups
      GroupHandler handler = orgSrv.getGroupHandler();
      Collection<?> allGroups = null;
      try {
        allGroups = handler.findGroups(null);

        for (Object group : allGroups) {
          if (group instanceof Group) {
            Group grp = (Group) group;
            Group parentGroup = grp.getParentId() != null ? handler.findGroupById(grp.getParentId()) : null;
            Collection<?> children = handler.findGroups(grp);
            treeAllGroups.addSibilings(buildGroupNode(parentGroup, grp, children));
          }
        }

      } catch (Exception e) {
        LOG.warn("Cannot get all groups.", e);
      }
    } catch (Exception e) {
      LOG.warn("Load setting is unsuccessfully", e);
    }
  }
  
  private GroupNode buildGroupNode(String membership) {
    String groupId = membership.substring(membership.indexOf(":"));
    String membershipType = membership.substring(0, membership.indexOf(":"));
    String groupLabel = groupId;
    try {
      groupLabel = orgSrv.getGroupHandler().findGroupById(groupId).getLabel();
    } catch (Exception e) {
      LOG.warn("Group " + groupId + " not exitsting");
    }
    return GroupNode.createInstance(groupId, groupLabel).setMembershipType(membershipType);
  }
  
  private GroupNode buildGroupNode(Group parentGroup, Group currentGroup, Collection children) {
    GroupNode currentNode = null;
    try {
      if (currentGroup != null) {
        currentNode = GroupNode.createInstance(currentGroup.getId(), currentGroup.getLabel());
        //
        if (parentGroup != null)
          currentNode.setParent(GroupNode.createInstance(parentGroup.getId(), parentGroup.getLabel()));
        //
        if (children != null) {
          GroupHandler handler = orgSrv.getGroupHandler();
          for (Object g : children) {
            if (g instanceof Group) {
              Group grp = (Group) g;
              Collection<?> subChildren = handler.findGroups(grp);
              GroupNode child = GroupNode.createInstance(grp.getId(), grp.getLabel());
              child.setHasChildren(subChildren.size() > 0);
              currentNode.addChildren(child);
            }
          }
        }
        
      }
    } catch (Exception e) {
      // 
      LOG.warn("Cannot build group node.");
    }
    return currentNode;
  }
  
  public GroupTree getGroups() {
    return treeAllGroups;  
  }

  public List<String> getMembershipTypes() {
    return listMemberhip;  
  }
  
  public void setGroups(GroupTree tree) {
    treeAllGroups = tree;
  }
  
  public static List<String> getRestrictedMemberships() {
    return Collections.unmodifiableList(new ArrayList<String>(restrictedMemberships));
  }

  public static List<GroupNode> getRestrictedNodes() {
    return Collections.unmodifiableList(restrictedNodes);
  }
  
  public void addRestrictedMemberships(String membership) {
    try {
      if (!restrictedMemberships.contains(membership)) {
        restrictedMemberships.add(membership);
        //
        this.settingService.set(Context.GLOBAL, Scope.PORTAL, GROUP_RESTRICTED_KEY, SettingValue.create(getValueRestrictedMemberships()));
        //
        restrictedNodes.add(buildGroupNode(membership));
      }
    } catch (Exception e) {
      LOG.warn("Cannot add restricted membership: " + membership, e);
    }
  }
  
  private static String getValueRestrictedMemberships() {
    Iterator<String> i = restrictedMemberships.iterator();
    if (!i.hasNext()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (;;) {
      sb.append(i.next());
      if (!i.hasNext()) {
        return sb.toString();
      }
      sb.append(",");
    }
  }
  
  public void removeRestrictedGroups(String membership) {
    try {
      if (restrictedMemberships.contains(membership)) {
        restrictedMemberships.remove(membership);
        //
        settingService.set(Context.GLOBAL, Scope.PORTAL, GROUP_RESTRICTED_KEY, SettingValue.create(getValueRestrictedMemberships()));
        //
        restrictedNodes.remove(buildGroupNode(membership));
      }
    } catch (Exception e) {
      LOG.warn("Cannot remove restricted membership: " + membership, e);
    }
  }
  
  public boolean isOnRestricted() {
    return isOnRestricted;
  }

  public void setOnRestricted(boolean isOnRestricted) {
    this.isOnRestricted = isOnRestricted;
    this.settingService.set(Context.GLOBAL, Scope.PORTAL, ON_RESTRICTED_KEY, SettingValue.create(isOnRestricted));
  }
  
  public boolean hasParent(String groupKey) {
    try {
      if (groupKey == null) {
        GroupNode curGroup = getGroups().getSibilings().get(0);
        return curGroup.hasParent();
      } else {
        Group selectedGroup = orgSrv.getGroupHandler().findGroupById(groupKey);
        return selectedGroup.getParentId() != null;
      }  
    } catch (Exception e) {
      return false;
    }
  }
  
  public void downLevel(String groupKey, GroupTree tree) {
    
    try {
      GroupHandler handler = orgSrv.getGroupHandler();
      Group selectedGroup = handler.findGroupById(groupKey);
      if (selectedGroup != null) {
        Group parentGroup = handler.findGroupById(selectedGroup.getParentId());
        Collection<?> parentChildren = handler.findGroups(parentGroup);

        if (parentChildren.size() > 0) {
          tree.clear();
        }
        
        for (Object group : parentChildren) {
          if (group instanceof Group) {
            Group grp = (Group) group;
            Group newParentGroup = grp.getParentId() != null ? handler.findGroupById(grp.getParentId()) : null;
            Collection<?> newchildren = handler.findGroups(grp);
            tree.addSibilings(buildGroupNode(newParentGroup, grp, newchildren));
          }
        }
        
      }
    } catch (Exception e) {
      // 
      LOG.warn("Cannot down level of node.");
    }
  }

  
  public GroupTree upLevel(GroupTree tree) {
    GroupNode groupNode = tree.getSibilings().size() > 0 ? tree.getSibilings().get(0) : null;
    GroupTree newTree = GroupTree.createInstance();
    try {

      if (groupNode != null) {
        GroupNode myParent = groupNode.getParent();
        if (myParent != null) {
          // get parent
          GroupHandler handler = orgSrv.getGroupHandler();
          Group parentGroup = handler.findGroupById(myParent.getId());
          Collection<?> children = null;
          if (parentGroup.getParentId() != null) {
            Group ancestorGroup = handler.findGroupById(parentGroup.getParentId());
            children = handler.findGroups(ancestorGroup);
          } else {
            children = handler.findGroups(null);
          }

          // get children of ancestor, then push into tree
          for (Object group : children) {
            if (group instanceof Group) {
              Group grp = (Group) group;
              Group newParentGroup = grp.getParentId() != null ? handler.findGroupById(grp.getParentId()) : null;
              Collection<?> newchildren = handler.findGroups(grp);
              newTree.addSibilings(buildGroupNode(newParentGroup, grp, newchildren));
            }
          }

        }

      }
      return newTree;
    } catch (Exception e) {
      //
      LOG.warn("Cannot down level of node.");
      return newTree;
    }
  }
  
}
