package org.exoplatform.social.portlet.spaceManagement;

import java.text.MessageFormat;
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
  @Path("uiGroupBreadcrumbs.gtmpl") Template uiBreadcumbs;
 
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
  public Response reloadPortlet() {
    Map<String, Object> parameters = new HashMap<String, Object>();
    //
    ContextMapper context = new ContextMapper(bundle);
    parameters.put("_ctx", context);
    parameters.put("isRestricted", groupPrefs.isOnRestricted());
    parameters.put("restrictedNodes", GroupPrefs.getRestrictedNodes());
    return index.ok(parameters).withMimeType("text/html");
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
    //restricted
    if(!groupPrefs.isOnRestricted()) {
      return responseError("The restricted not active.");
    }
    try {
      //
      String membership = membershipType + ":" + getCurrentSelected();
      groupPrefs.addRestrictedMemberships(membership);
      LOG.info("Save membership: " + membership);
      //
    } catch (Exception e) {
      return responseError(e.toString());
    }
    return reloadPortlet();
  }

  @Ajax
  @Resource
  public Response removeRestrictedMembership(String membership) {
    //restricted
    try {
      if(!groupPrefs.isOnRestricted()) {
        return responseError("The restricted not active.");
      }
      //
      LOG.info("Remove membership: " + membership);
      groupPrefs.removeRestrictedMemberships(membership);
      //
    } catch (Exception e) {
      return responseError(e.toString());
    }
    return reloadPortlet();
  }

  @Ajax
  @Resource
  public Response setSelectedGroup(String groupId, String hashChild) {
    try {
      //
      setCurrentSelected(groupId);
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("breadcumbs", getBreadcumbs(groupId));
      return uiBreadcumbs.ok(parameters).withMimeType("text/html");
    } catch (Exception e) {
      return responseError(e.toString());
    }
  }

  @Ajax
  @Resource
  public Response openGroupSelector(String groupId, String hashChild) {
    if(!groupPrefs.isOnRestricted()) {
      return Response.ok("").withMimeType("text/html");
    }
    setCurrentSelected(groupId);
    LOG.info("openGroupSelector: " + getCurrentSelected());
    //
    Map<String, Object> parameters = new HashMap<String, Object>();
    ContextMapper context = new ContextMapper(bundle);
    parameters.put("_ctx", context);
    parameters.put("allGroups", groupPrefs.getGroups());
    parameters.put("isRootNode", true);
    parameters.put("currentSelected", getCurrentSelected());
    parameters.put("breadcumbs", getBreadcumbs(groupId));
    parameters.put("listMemberhip", (groupId == null || groupId == "/") ? new ArrayList<String>() : getMembershipTypes());
    //
    return uiPopupGroup.ok(parameters).withMimeType("text/html");
  }
  
  private Map<String, String> getBreadcumbs(String groupId) {
    Map<String, String> breadcumbs = new LinkedHashMap<String, String>();
    try {
      if(groupId != null && !groupId.isEmpty() && !groupId.equals("/")) {
        GroupHandler handler = organizationService.getGroupHandler();
        Group current = handler.findGroupById(groupId);
        LOG.info("  "+ current.getId()+" : "+ current.getLabel());
        breadcumbs.put(current.getId(), current.getLabel());
        while (current.getParentId() != null) {
          current = handler.findGroupById(current.getParentId());
          breadcumbs.put(current.getId(), current.getLabel());
        }
      }
    } catch (Exception e) {
      LOG.warn("Build breadcumbs unsuccessfully.", e);
    }
    //
    LOG.info("getBreadcumbs: " + breadcumbs.toString());
    return breadcumbs;
  }
  
  private Response responseError(String message) {
    JSON data = new JSON();
    data.set("ok", "false");
    data.set("message", message);
    return Response.ok(data.toString()).withMimeType("application/json");
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
  
  
  public String getCurrentSelected() {
    return currentSelected;
  }

  public void setCurrentSelected(String currentSelected) {
    LOG.info("setCurrentSelected " + currentSelected);
    this.currentSelected = currentSelected;
  }

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
      return MessageFormat.format(appRes(key), args);
    }
    
    public WebuiRequestContext getRequestContext() {
      return WebuiRequestContext.getCurrentInstance();
    }
  }
  
  
}
