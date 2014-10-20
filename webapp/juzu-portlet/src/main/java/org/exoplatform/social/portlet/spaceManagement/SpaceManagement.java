package org.exoplatform.social.portlet.spaceManagement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.inject.Inject;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.View;
import juzu.impl.common.JSON;
import juzu.request.RenderContext;
import juzu.template.Template;

import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.space.GroupPrefs;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webui.application.WebuiRequestContext;

public class SpaceManagement {
  private static final Log LOG = ExoLogger.getLogger(SpaceManagement.class);

  @Inject
  @Path("index.gtmpl") Template index;

  @Inject
  @Path("uiPopupGroupSelector.gtmpl") Template uiPopupGroup;
  
  @Inject
  ResourceBundle bundle;  
  
  @Inject
  OrganizationService organizationService;

  @Inject
  GroupPrefs groupPrefs;

  @Inject
  SpaceService spaceService;
  
  
  private String currentSelected = "/";
  private Locale locale = Locale.ENGLISH;
  private LinkedList<String> listMemberhip;

  @View
  public void index(RenderContext renderContext) {
    
    if (renderContext != null) {
      locale = renderContext.getUserContext().getLocale();
    }
    if (bundle == null) {
      bundle = renderContext.getApplicationContext().resolveBundle(locale);
    }
    
    Map<String, Object> parameters = new HashMap<String, Object>();

    //
    ContextMapper context = new ContextMapper(bundle);
    parameters.put("_ctx", context);
    parameters.put("isRestricted", groupPrefs.isOnRestricted());
    parameters.put("restrictedNodes", GroupPrefs.getRestrictedNodes());

    index.render(parameters);
  }
  
  @Ajax
  @Resource
  public Response saveRestricted(String restricted) {
    JSON data = new JSON();
    //restricted
    try {
      boolean isOnRestricted = Boolean.valueOf(restricted);
      //
      groupPrefs.setOnRestricted(isOnRestricted);
      //
      data.set("ok", "true");
      data.set("restricted", String.valueOf(isOnRestricted));
    } catch (Exception e) {
      data.set("ok", "false");
      data.set("status", e.toString());
    }
    return Response.ok(data.toString()).withMimeType("application/json");
  }

  @Ajax
  @Resource
  public Response saveRestrictedMembership(String membershipType) {
    JSON data = new JSON();
    //restricted
    try {
      //
      String membership = membershipType + ":" + currentSelected;
      LOG.info("Save membership: " + membership);
      groupPrefs.addRestrictedMemberships(membership);
      //
      data.set("ok", "true");
    } catch (Exception e) {
      data.set("ok", "false");
      data.set("status", e.toString());
    }
    return Response.ok(data.toString()).withMimeType("application/json");
  }

  @Ajax
  @Resource
  public Response removeRestrictedMembership(String membership) {
    JSON data = new JSON();
    //restricted
    try {
      //
      LOG.info("Remove membership: " + membership);
      groupPrefs.removeRestrictedMemberships(membership);
      //
      data.set("ok", "true");
    } catch (Exception e) {
      data.set("ok", "false");
      data.set("status", e.toString());
    }
    return Response.ok(data.toString()).withMimeType("application/json");
  }

  @Ajax
  @Resource
  public Response setSelectedGroup(String groupId, String hashChild) {
    JSON data = new JSON();
    //restricted
    try {
      //
      currentSelected = groupId;
      data.set("ok", "true");
      Map<String, String> breadcumbs = getBreadcumbs(groupId);
      data.map("breadcumbs", breadcumbs);
    } catch (Exception e) {
      data.set("ok", "false");
      data.set("status", e.toString());
    }
    return Response.ok(data.toString()).withMimeType("application/json");
  }
  
  @Ajax
  @Resource
  public Response openGroupSelector(String groupId, String hashChild) {
    this.currentSelected = groupId;
    Map<String, Object> parameters = new HashMap<String, Object>();
    //
    ContextMapper context = new ContextMapper(bundle);
    parameters.put("_ctx", context);
    parameters.put("allGroups", groupPrefs.getGroups());
    parameters.put("isRootNode", true);
    parameters.put("currentSelected", currentSelected);
    parameters.put("breadcumbs", getBreadcumbs(groupId));
    parameters.put("listMemberhip", (groupId == null || groupId == "/") ? new ArrayList<String>() : getMembershipTypes());
    //
    return uiPopupGroup.ok(parameters).withMimeType("text/html");
  }
  
  private Map<String, String> getBreadcumbs(String groupId) {
    LinkedHashMap<String, String> breadcumbs = new LinkedHashMap<String, String>();
    try {
      GroupHandler handler = organizationService.getGroupHandler();
      Group current = handler.findGroupById(groupId);
      while (current.getParentId() != null) {
        breadcumbs.put(current.getId(), current.getLabel());
        //
        current = handler.findGroupById(current.getParentId());
      }
    } catch (Exception e) {
      LOG.warn("Build breadcumbs unsuccessfully.", e);
    }
    //
    return breadcumbs;
  }
  
/*
  @Inject @Path("main.gtmpl") main main;
  @Inject @Path("groupSelector.gtmpl") groupSelector groupSelector;
  //@Inject @Path("moreGroup.gtmpl") moreGroup moreGroup;
  @Inject @Path("item.gtmpl") item item;
  @Inject @Path("restrictedGroups.gtmpl") restrictedGroups restrictedGroups;
  
  @Inject GroupPrefs groupPrefs;
  
  List<String> moreRestrictedGroups = new ArrayList<String>();

  @View
  public void index() throws Exception {
    main.with()
        .isRestricted(groupPrefs.isOnRestricted())
        .restrictedGroups(groupPrefs.getRestrictedGroups())
        .render();
  }
  
  @Ajax
  @Resource
  public void doAddGroup() throws Exception {
    groupSelector.with()
                 .allGroups(groupPrefs.getGroups())
                 .isRootNode(!groupPrefs.hasParent(null))
                 .render();
  }
  
  
  @Ajax
  @Resource
  public void removeGroup(String groupId) throws Exception {
    groupPrefs.removeRestrictedGroups(groupId);
    
    restrictedGroups.with()
                    .restrictedGroups(groupPrefs.getRestrictedGroups())
                    .render();
  }
  
  @Ajax
  @Resource
  public void doSelectGroup(String groupId, String groupName) throws Exception {
    // temp process, return if group is selected
    if (groupPrefs.getRestrictedGroups().hasNode(groupId)) return;
    
    // store selected group
    groupPrefs.addRestrictedGroups(groupId);
    
    //
    item.with()
             .groupName(groupName)
             .render();
  }
  
  @Ajax
  @Resource
  public void doAccessChildGroup(String groupId) throws Exception {
    groupPrefs.downLevel(groupId, groupPrefs.getGroups());
    groupSelector.with()
                 .allGroups(groupPrefs.getGroups())
                 .isRootNode(!groupPrefs.hasParent(groupId))
                 .render();
  }
  
  @Ajax
  @Resource
  public void backToParentGroup() throws Exception {
    GroupTree newTree = groupPrefs.upLevel(groupPrefs.getGroups());
    groupPrefs.setGroups(newTree);
    
    groupSelector.with()
                 .allGroups(groupPrefs.getGroups())
                 .isRootNode(!groupPrefs.hasParent(null))
                 .render();
  }
  
  @Ajax
  @Resource
  public void switchMode() throws Exception {
    // change status
    groupPrefs.setOnRestricted(!groupPrefs.isOnRestricted());
  }
  */
  
  
  private List<String> getMembershipTypes() {
    if (listMemberhip == null) {
      try {
        List<MembershipType> memberships;
        memberships = (List<MembershipType>) organizationService.getMembershipTypeHandler().findMembershipTypes();
        Collections.sort(memberships, new Comparator<MembershipType>() {
          @Override
          public int compare(MembershipType o1, MembershipType o2) {
            return o1.getName().compareTo(o2.getName());
          }
        });
        listMemberhip = new LinkedList<String>();
        boolean containWildcard = false;
        for (MembershipType mt : memberships) {
          listMemberhip.add(mt.getName());
          if ("*".equals(mt.getName())) {
            containWildcard = true;
          }
        }
        if (!containWildcard) {
          listMemberhip.addFirst("*");
        }
      } catch (Exception e) {
        LOG.warn("Get memberships type unsuccessfully.");
      }
    }
    return listMemberhip;
  }

  public class ContextMapper {
    ResourceBundle bundle;

    public ContextMapper(ResourceBundle bundle) {
      this.bundle = bundle;
    }

    public String appRes(String key) {
      try {
        return bundle.getString(key).replaceAll("'", "&#39;").replaceAll("\"", "&#34;");
      } catch (java.util.MissingResourceException e) {
        LOG.warn("Can't find resource for bundle key " + key);
      } catch (Exception e) {
        LOG.debug("Error when get resource bundle key " + key, e);
      }
      return key;
    }
    
    public String appRes(String key, String ...args) {
      return String.format(appRes(key), args);
    }
    
    public WebuiRequestContext getRequestContext() {
      return WebuiRequestContext.getCurrentInstance();
    }
  }
  
  
}
