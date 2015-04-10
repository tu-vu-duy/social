/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.social.rest.impl.space;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.core.space.SpaceFilter;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.social.rest.api.RestProperties;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.social.rest.api.SpaceRestResources;
import org.exoplatform.social.rest.entity.ActivityEntity;
import org.exoplatform.social.rest.entity.BaseEntity;
import org.exoplatform.social.rest.entity.CollectionEntity;
import org.exoplatform.social.rest.entity.DataEntity;
import org.exoplatform.social.rest.entity.SpaceEntity;
import org.exoplatform.social.service.rest.api.VersionResources;
import org.exoplatform.social.service.rest.api.models.ActivityRestIn;

@Path(VersionResources.VERSION_ONE + "/social/spaces")
public class SpaceRestResourcesV1 implements SpaceRestResources {

  public SpaceRestResourcesV1() {
  }
  
  /**
   * {@inheritDoc}
   */
  @RolesAllowed("users")
  public Response getSpaces(@Context UriInfo uriInfo) throws Exception {
    String q = RestUtils.getQueryParam(uriInfo, "q");
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    int limit = RestUtils.getLimit(uriInfo);
    int offset = RestUtils.getOffset(uriInfo);
    
    ListAccess<Space> listAccess = null;
    SpaceFilter spaceFilter = null;
    if (q != null) {
      spaceFilter = new SpaceFilter();
      spaceFilter.setSpaceNameSearchCondition(q);
    }
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    if (RestUtils.isMemberOfAdminGroup()) {
      listAccess = spaceService.getAllSpacesByFilter(spaceFilter);
    } else {
      listAccess = spaceService.getAccessibleSpacesByFilter(authenticatedUser, spaceFilter);
    }
    List<DataEntity> spaceInfos = new ArrayList<DataEntity>();
    for (Space space : listAccess.load(offset, limit)) {
      SpaceEntity spaceInfo = EntityBuilder.buildEntityFromSpace(space, authenticatedUser, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand"));
      //
      spaceInfos.add(spaceInfo.getDataEntity()); 
    }
    CollectionEntity collectionSpace = new CollectionEntity(spaceInfos, EntityBuilder.SPACES_TYPE, offset, limit);
    if (RestUtils.isReturnSize(uriInfo)) {
      collectionSpace.setSize( listAccess.getSize());
    }
    
    return EntityBuilder.getResponse(collectionSpace, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  /**
   * {@inheritDoc}
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response createSpace(@Context UriInfo uriInfo,
                               SpaceEntity model) throws Exception {
    if (model == null || model.getDisplayName() == null || model.getDisplayName().length() == 0) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    // validate the display name
    if (spaceService.getSpaceByDisplayName(model.getDisplayName()) != null) {
      throw new SpaceException(SpaceException.Code.SPACE_ALREADY_EXIST);
    }

    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    //
    Space space = new Space();
    fillSpaceFromModel(space, model);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId("/space/space" + space.getPrettyName());
    space.setType(DefaultSpaceApplicationHandler.NAME);
    String[] managers = new String[] {authenticatedUser};
    String[] members = new String[] {authenticatedUser};
    space.setManagers(managers);
    space.setMembers(members);
    //
    spaceService.createSpace(space, authenticatedUser);

    return EntityBuilder.getResponse(EntityBuilder.buildEntityFromSpace(space, authenticatedUser, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  /**
   * {@inheritDoc}
   */
  @GET
  @Path("{id}")
  @RolesAllowed("users")
  public Response getSpaceById(@Context UriInfo uriInfo) throws Exception {
    String id = RestUtils.getPathParam(uriInfo, "id");
    
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    Space space = spaceService.getSpaceById(id);
    if (space == null || (Space.HIDDEN.equals(space.getVisibility()) && ! spaceService.isMember(space, authenticatedUser) && ! RestUtils.isMemberOfAdminGroup())) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    return EntityBuilder.getResponse(EntityBuilder.buildEntityFromSpace(space, authenticatedUser, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  /**
   * {@inheritDoc}
   */
  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response updateSpaceById(@Context UriInfo uriInfo,
                                  SpaceEntity model) throws Exception {
    
    if (model == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    //
    String id = RestUtils.getPathParam(uriInfo, "id");
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    Space space = spaceService.getSpaceById(id);
    if (space == null || (! spaceService.isManager(space, authenticatedUser) && ! RestUtils.isMemberOfAdminGroup())) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    fillSpaceFromModel(space, model);
    spaceService.updateSpace(space);
    
    return EntityBuilder.getResponse(EntityBuilder.buildEntityFromSpace(space, authenticatedUser, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  /**
   * {@inheritDoc}
   */
  @DELETE
  @Path("{id}")
  @RolesAllowed("users")
  public Response deleteSpaceById(@Context UriInfo uriInfo) throws Exception {
    String id = RestUtils.getPathParam(uriInfo, "id");
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    //
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    Space space = spaceService.getSpaceById(id);
    if (space == null || (! spaceService.isManager(space, authenticatedUser) && ! RestUtils.isMemberOfAdminGroup())) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    spaceService.deleteSpace(space);
    
    return EntityBuilder.getResponse(EntityBuilder.buildEntityFromSpace(space, authenticatedUser, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  /**
   * {@inheritDoc}
   */
  @GET
  @Path("{id}/users")
  @RolesAllowed("users")
  public Response getSpaceMembers(@Context UriInfo uriInfo) throws Exception {
    String id = RestUtils.getPathParam(uriInfo, "id");
    String role = RestUtils.getQueryParam(uriInfo, "role");
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    //
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    Space space = spaceService.getSpaceById(id);
    if (space == null || (! spaceService.isMember(space, authenticatedUser) && ! RestUtils.isMemberOfAdminGroup())) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    int limit = RestUtils.getLimit(uriInfo);
    int offset = RestUtils.getOffset(uriInfo);
    
    String[] users = (role != null && role.equals("manager")) ? space.getManagers() : space.getMembers();
    int size = users.length;
    //
    users = Arrays.copyOfRange(users, offset > size - 1 ? size - 1 : offset, (offset + limit > size) ? size : (offset + limit));
    List<DataEntity> profileInfos = EntityBuilder.buildEntityProfiles(users, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand"));
    CollectionEntity collectionUser = new CollectionEntity(profileInfos, EntityBuilder.USERS_TYPE, offset, limit);
    if (RestUtils.isReturnSize(uriInfo)) {
      collectionUser.setSize(size);
    }    
    return EntityBuilder.getResponse(collectionUser, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  /**
   * {@inheritDoc}
   */
  @GET
  @Path("{id}/activities")
  @RolesAllowed("users")
  public Response getSpaceActivitiesById(@Context UriInfo uriInfo) throws Exception {
    
    String id = RestUtils.getPathParam(uriInfo, "id");
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    //
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    Space space = spaceService.getSpaceById(id);
    if (space == null || ! spaceService.isMember(space, authenticatedUser)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    int limit = RestUtils.getLimit(uriInfo);
    int offset = RestUtils.getOffset(uriInfo);
    
    Identity spaceIdentity = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
    RealtimeListAccess<ExoSocialActivity> listAccess = CommonsUtils.getService(ActivityManager.class).getActivitiesOfSpaceWithListAccess(spaceIdentity);
    Long after = RestUtils.getLongValue(uriInfo, "after"); 
    Long before = RestUtils.getLongValue(uriInfo, "before");
    List<ExoSocialActivity> activities = null;
    if (after != null) {
      activities = listAccess.loadNewer(after.longValue(), limit);
    } else if (before != null) {
      activities = listAccess.loadOlder(before.longValue(), limit);
    } else {
      activities = listAccess.loadAsList(offset, limit);
    }
    List<DataEntity> activityEntities = new ArrayList<DataEntity>();
    //
    BaseEntity as = new BaseEntity(spaceIdentity.getRemoteId());
    as.setProperty(RestProperties.TYPE, EntityBuilder.SPACE_ACTIVITY_TYPE);
    //
    for (ExoSocialActivity activity : activities) {
      ActivityEntity activityInfo = EntityBuilder.buildEntityFromActivity(activity, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand"));
      activityInfo.setActivityStream(as.getDataEntity());
      //
      activityEntities.add(activityInfo.getDataEntity());
    }
    CollectionEntity collectionActivity = new CollectionEntity(activityEntities, EntityBuilder.ACTIVITIES_TYPE,  offset, limit);
    if(RestUtils.isReturnSize(uriInfo)) {
      collectionActivity.setSize(listAccess.getSize());
    }
    return EntityBuilder.getResponse(collectionActivity, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  /**
   * {@inheritDoc}
   */
  @POST
  @Path("{id}/activities")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response postActivityOnSpace(@Context UriInfo uriInfo,
                                       ActivityRestIn model) throws Exception {
    if (model == null || model.getTitle() == null || model.getTitle().length() == 0) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    String id = RestUtils.getPathParam(uriInfo, "id");
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    //
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    Space space = spaceService.getSpaceById(id);
    if (space == null || ! spaceService.isMember(space, authenticatedUser)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    Identity spaceIdentity = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
    Identity poster = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser, false);
    
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle(model.getTitle());
    activity.setUserId(poster.getId());
    CommonsUtils.getService(ActivityManager.class).saveActivityNoReturn(spaceIdentity, activity);
    
    return EntityBuilder.getResponse(EntityBuilder.buildEntityFromActivity(activity, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }

  private void fillSpaceFromModel(Space space, SpaceEntity model) {
    space.setDisplayName(model.getDisplayName());
    if (model.getDescription() != null && model.getDescription().length() > 0) {
      space.setDescription(StringEscapeUtils.escapeHtml(model.getDescription()));
    }
    if (space.getGroupId() == null) {
      String groupId = model.getDisplayName();
      if (model.getGroupId() != null && model.getGroupId().length() > 0) {
        groupId = model.getGroupId();
        if (groupId.indexOf("/") >= 0) {
          groupId = groupId.substring(groupId.lastIndexOf("/") + 1);
        }
        if (groupId == "") {
          groupId = model.getDisplayName();
        }
      }
      space.setPrettyName(groupId);
    }
    
    if (model.getVisibility() != null && (model.getVisibility().equals(Space.HIDDEN) 
        || model.getVisibility().equals(Space.PRIVATE))) {
      space.setVisibility(model.getVisibility());
    } else if (space.getVisibility() == null || space.getVisibility().length() == 0) {
      space.setVisibility(Space.PRIVATE);
    }
    
    if (Space.OPEN.equals(model.getSubscription()) || Space.CLOSE.equals(model.getSubscription())) {
      space.setRegistration(model.getSubscription());
    } else if (space.getRegistration() == null || space.getRegistration().length() == 0) {
      space.setRegistration(Space.VALIDATION);
    }
  }
}
