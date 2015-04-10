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
package org.exoplatform.social.rest.impl.user;

import java.util.ArrayList;
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

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.social.rest.api.UserRestResources;
import org.exoplatform.social.rest.entity.ActivityEntity;
import org.exoplatform.social.rest.entity.CollectionEntity;
import org.exoplatform.social.rest.entity.DataEntity;
import org.exoplatform.social.rest.entity.SpaceEntity;
import org.exoplatform.social.rest.entity.UserEntity;
import org.exoplatform.social.service.rest.api.VersionResources;

/**
 * 
 * Provides REST Services for manipulating jobs related to users.
 * 
 * @anchor UsersRestService
 */

@Path(VersionResources.VERSION_ONE + "/social/users")
public class UserRestResourcesV1 implements UserRestResources {
  
  public static enum ACTIVITY_STREAM_TYPE {
    all, owner, connections, spaces
  }
  
  public UserRestResourcesV1() {
    
  }
  
  @GET
  @RolesAllowed("users")
  public Response getUsers(@Context UriInfo uriInfo) throws Exception {
    String q = RestUtils.getQueryParam(uriInfo, "q");
    
    int limit = RestUtils.getLimit(uriInfo);
    int offset = RestUtils.getOffset(uriInfo);
    
    ProfileFilter filter = new ProfileFilter();
    filter.setName(q == null || q.isEmpty() ? "" : q);
    
    ListAccess<Identity> list = CommonsUtils.getService(IdentityManager.class).getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME, filter, false);

    Identity[] identities = list.load(offset, limit);
    List<DataEntity> profileInfos = new ArrayList<DataEntity>();
    for (Identity identity : identities) {
      UserEntity profileInfo = EntityBuilder.buildEntityProfile(identity.getProfile(), uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand"));
      //
      profileInfos.add(profileInfo.getDataEntity());
    }
    CollectionEntity collectionUser = new CollectionEntity(profileInfos, EntityBuilder.USERS_TYPE, offset, limit);
    if(RestUtils.isReturnSize(uriInfo)) {
      collectionUser.setSize(list.getSize());
    }
    return EntityBuilder.getResponse(collectionUser, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response addUser(@Context UriInfo uriInfo,
                          UserEntity model) throws Exception {
    if (model.isNotValid()) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    //Check permission of current user
    if (!RestUtils.isMemberOfAdminGroup()) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
    
    //check if the user is already exist
    Identity identity = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, model.getUsername(), true);
    if (identity != null) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    
    //Create new user
    UserHandler userHandler = CommonsUtils.getService(OrganizationService.class).getUserHandler();
    User user = userHandler.createUserInstance(model.getUsername());
    user.setFirstName(model.getFirstname());
    user.setLastName(model.getLastname());
    user.setEmail(model.getEmail());
    user.setPassword(model.getPassword() == null || model.getPassword().isEmpty() ? "exo" : model.getPassword());
    userHandler.createUser(user, true);
    //
    return EntityBuilder.getResponse(EntityBuilder.buildEntityProfile(model.getUsername(), uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @GET
  @Path("{id}")
  @RolesAllowed("users")
  public Response getUserById(@Context UriInfo uriInfo) throws Exception {
    String id = RestUtils.getPathParam(uriInfo, "id");
    Identity identity = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, id, true);
    //
    if (identity == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    return EntityBuilder.getResponse(EntityBuilder.buildEntityProfile(identity.getProfile(), uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @DELETE
  @Path("{id}")
  @RolesAllowed("users")
  public Response deleteUserById(@Context UriInfo uriInfo) throws Exception {
    //Check permission of current user
    if (!RestUtils.isMemberOfAdminGroup()) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
    
    String id = RestUtils.getPathParam(uriInfo, "id");
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, id, true);
    if (identity == null) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    identityManager.hardDeleteIdentity(identity);
    identity.setDeleted(true);
    //
    return EntityBuilder.getResponse(EntityBuilder.buildEntityProfile(identity.getProfile(), uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response updateUserById(@Context UriInfo uriInfo,
                                 UserEntity model) throws Exception {
    String id = RestUtils.getPathParam(uriInfo, "id");
    UserHandler userHandler = CommonsUtils.getService(OrganizationService.class).getUserHandler();
    User user = userHandler.findUserByName(id);
    if (user == null) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    //Check if the current user is the authenticated user
    if (!ConversationState.getCurrent().getIdentity().getUserId().equals(id)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
    
    fillUserFromModel(user, model);
    userHandler.saveUser(user, true);
    //
    return EntityBuilder.getResponse(EntityBuilder.buildEntityProfile(id, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @GET
  @Path("{id}/connections")
  @RolesAllowed("users")
  public Response getConnectionOfUser(@Context UriInfo uriInfo) throws Exception {
    String id = RestUtils.getPathParam(uriInfo, "id");
    Identity target = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, id, true);
    if (target == null) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    
    int limit = RestUtils.getLimit(uriInfo);
    int offset = RestUtils.getOffset(uriInfo);
    
    List<DataEntity> profileInfos = new ArrayList<DataEntity>();
    ListAccess<Identity> listAccess = CommonsUtils.getService(RelationshipManager.class).getConnectionsByFilter(target, new ProfileFilter());
    Identity []identities = listAccess.load(offset, limit);
    for (Identity identity : identities) {
      UserEntity profileInfo = EntityBuilder.buildEntityProfile(identity.getProfile(), uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand"));
      //
      profileInfos.add(profileInfo.getDataEntity());
    }
    CollectionEntity collectionUser = new CollectionEntity(profileInfos, EntityBuilder.USERS_TYPE, offset, limit);
    if(RestUtils.isReturnSize(uriInfo)) {
      collectionUser.setSize(listAccess.getSize());
    }
    return EntityBuilder.getResponse(collectionUser, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @GET
  @Path("{id}/spaces")
  @RolesAllowed("users")
  public Response getSpacesOfUser(@Context UriInfo uriInfo) throws Exception {
    String id = RestUtils.getPathParam(uriInfo, "id");
    Identity target = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, id, true);
    //Check if the given user exists
    if (target == null) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    //Check permission of authenticated user : he must be an admin or he is the given user
    if (!RestUtils.isMemberOfAdminGroup() && !ConversationState.getCurrent().getIdentity().getUserId().equals(id) ) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
    
    int limit = RestUtils.getLimit(uriInfo);
    int offset = RestUtils.getOffset(uriInfo);
    
    List<DataEntity> spaceInfos = new ArrayList<DataEntity>();
    ListAccess<Space> listAccess = CommonsUtils.getService(SpaceService.class).getMemberSpaces(id);
    
    for (Space space : listAccess.load(offset, limit)) {
      SpaceEntity spaceInfo = EntityBuilder.buildEntityFromSpace(space, id, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand"));
      //
      spaceInfos.add(spaceInfo.getDataEntity()); 
    }
    CollectionEntity collectionSpace = new CollectionEntity(spaceInfos, EntityBuilder.SPACES_TYPE, offset, limit);
    if (RestUtils.isReturnSize(uriInfo)) {
      collectionSpace.setSize( listAccess.getSize());
    }
    
    return EntityBuilder.getResponse(collectionSpace, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @GET
  @Path("{id}/activities")
  @RolesAllowed("users")
  public Response getActivitiesOfUser(@Context UriInfo uriInfo) throws Exception {
    String id = RestUtils.getPathParam(uriInfo, "id");
    
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    //Check if the given user doesn't exist
    Identity target = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, id, true);
    if (target == null) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    String type = RestUtils.getQueryParam(uriInfo, "type");
    ACTIVITY_STREAM_TYPE streamType;
    try {
      streamType = ACTIVITY_STREAM_TYPE.valueOf(type);
    } catch (Exception e) {
      streamType = ACTIVITY_STREAM_TYPE.all;
    }

    ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
    RealtimeListAccess<ExoSocialActivity> listAccess = null;
    List<ExoSocialActivity> activities = null;
    switch (streamType) {
      case all: {
        listAccess = activityManager.getActivityFeedWithListAccess(target);
        break;
      }
      case owner: {
        listAccess = activityManager.getActivitiesWithListAccess(target);
        break;
      }
      case connections: {
        listAccess = activityManager.getActivitiesOfConnectionsWithListAccess(target);
        break;
      }
      case spaces: {
        listAccess = activityManager.getActivitiesOfUserSpacesWithListAccess(target);
        break;
      }
      default:
        break;
    }
    //
    int limit = RestUtils.getLimit(uriInfo);
    int offset = RestUtils.getOffset(uriInfo);
    Long after = RestUtils.getLongValue(uriInfo, "after"); 
    Long before = RestUtils.getLongValue(uriInfo, "before");
    if (after != null) {
      activities = listAccess.loadNewer(after, limit);
    } else if (before != null) {
      activities = listAccess.loadOlder(before, limit);
    } else {
      activities = listAccess.loadAsList(offset, limit);
    }
    Identity currentUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser, true);
    List<DataEntity> activityEntities = new ArrayList<DataEntity>();
    for (ExoSocialActivity activity : activities) {
      DataEntity as = EntityBuilder.getActivityStream(activity, currentUser);
      if (as == null) continue;
      ActivityEntity activityEntity = EntityBuilder.buildEntityFromActivity(activity, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand"));
      activityEntity.setActivityStream(as);
      //
      activityEntities.add(activityEntity.getDataEntity()); 
    }
    CollectionEntity collectionActivity = new CollectionEntity(activityEntities, EntityBuilder.ACTIVITIES_TYPE,  offset, limit);
    if(RestUtils.isReturnSize(uriInfo)) {
      collectionActivity.setSize(listAccess.getSize());
    }
    return EntityBuilder.getResponse(collectionActivity, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @POST
  @Path("{id}/activities")
  @RolesAllowed("users")
  public Response addActivityByUser(@Context UriInfo uriInfo,
                                    ActivityEntity model) throws Exception {
    if (model == null || model.getTitle() == null || model.getTitle().length() ==0) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    String id = RestUtils.getPathParam(uriInfo, "id");
    //Check if the given user doesn't exist
    Identity target = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, id, true);
    if (target == null || !ConversationState.getCurrent().getIdentity().getUserId().equals(id)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle(model.getTitle());
    CommonsUtils.getService(ActivityManager.class).saveActivityNoReturn(target, activity);
    
    return EntityBuilder.getResponse(EntityBuilder.buildEntityFromActivity(activity, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  private void fillUserFromModel(User user, UserEntity model) {
    if (model.getFirstname() != null && !model.getFirstname().isEmpty()) {
      user.setFirstName(model.getFirstname());
    }
    if (model.getLastname() != null && !model.getLastname().isEmpty()) {
      user.setLastName(model.getLastname());
    }
    if (model.getEmail() != null && !model.getEmail().isEmpty()) {
      user.setEmail(model.getEmail());
    }
    if (model.getPassword() != null && !model.getPassword().isEmpty()) {
      user.setPassword(model.getPassword());
    }
  }
}
