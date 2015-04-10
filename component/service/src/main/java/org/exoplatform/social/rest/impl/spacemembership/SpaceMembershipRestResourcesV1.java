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
package org.exoplatform.social.rest.impl.spacemembership;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.rest.api.AbstractSocialRestService;
import org.exoplatform.social.rest.api.SpaceMembershipRestResources;
import org.exoplatform.social.rest.entity.SpaceMembershipRestIn;
import org.exoplatform.social.rest.entity.SpaceMembershipsCollections;
import org.exoplatform.social.service.rest.RestUtils;
import org.exoplatform.social.service.rest.Util;

@Path("v1/social/spacesMemberships")
public class SpaceMembershipRestResourcesV1 extends AbstractSocialRestService implements SpaceMembershipRestResources {
  
  private static final String SPACE_PREFIX = "/space/";
  
  private enum MembershipType {
    ALL, PENDING, APPROVED
  }
  
  public SpaceMembershipRestResourcesV1(){
  }

  @GET
  @RolesAllowed("users")
  public Response getSpacesMemberships(@Context UriInfo uriInfo) throws Exception {
    //
    int limit = getQueryValueLimit();
    int offset = getQueryValueOffset();

    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    Space givenSpace = null;
    String space= getQueryParam("space");
    if (space != null) {
      givenSpace = spaceService.getSpaceByDisplayName(space);
      if (givenSpace == null) {
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
      }
    }
    
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    Identity identity = null;
    String user = getQueryParam("user");
    if (user != null) {
      identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, user, true);
      if (identity == null) {
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
      }
    }
    
    int size;
    String status = getQueryParam("status");
    ListAccess<Space> listAccess = null;
    List<Space> spaces = new ArrayList<Space>();
    if (givenSpace == null) {
      listAccess = (identity == null) ? spaceService.getAllSpacesWithListAccess() : spaceService.getMemberSpaces(user);
      spaces = Arrays.asList(listAccess.load(offset, limit));
      size = getQueryValueReturnSize() ? listAccess.getSize() : -1;
    } else {
      spaces.add(givenSpace);
      size = getQueryValueReturnSize() ? spaces.size() : -1;
    }
    
    List<Map<String, Object>> spaceMemberships = new ArrayList<Map<String, Object>>();
    setSpaceMemberships(spaceMemberships, spaces, user, uriInfo);
    
    SpaceMembershipsCollections membershipsCollections = new SpaceMembershipsCollections(size, offset, limit);
    membershipsCollections.setSpaceMemberships(spaceMemberships);
    
    return Util.getResponse(membershipsCollections, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @POST
  @RolesAllowed("users")
  public Response addSpacesMemberships(@Context UriInfo uriInfo,
                                        SpaceMembershipRestIn model) throws Exception {
    if (model == null || model.getUser() == null || model.getSpace() == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    String user = model.getUser();
    String space = model.getSpace();
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    //
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    if (space == null || spaceService.getSpaceByDisplayName(space) == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    Space givenSpace = spaceService.getSpaceByDisplayName(space);
    
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    if (user == null || identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, user, true) == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    if (RestUtils.isMemberOfAdminGroup() || spaceService.isManager(givenSpace, authenticatedUser)
        || (authenticatedUser.equals(user) && givenSpace.getRegistration().equals(Space.OPEN))) {
      spaceService.addMember(givenSpace, user);
      if ("manager".equals(model.getRole())) {
        spaceService.setManager(givenSpace, user, true);
      }
    } else {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    return Util.getResponse(RestUtils.buildEntityFromSpaceMembership(givenSpace, user, "", uriInfo.getPath(), getQueryParam("expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @GET
  @Path("{id}") //id must have this format spaceName:userName:type
  @RolesAllowed("users")
  public Response getSpaceMembershipById(@Context UriInfo uriInfo) throws Exception {
    String[] idParams = getPathParam("id").split(":");
    if (idParams.length != 3) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    if (CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, idParams[1], true) == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    String spaceGroupId = SPACE_PREFIX + idParams[0];
    Space space = spaceService.getSpaceByGroupId(spaceGroupId);
    if (space == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    if (! authenticatedUser.equals(idParams[1]) && ! RestUtils.isMemberOfAdminGroup() && ! spaceService.isManager(space, authenticatedUser)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    return Util.getResponse(RestUtils.buildEntityFromSpaceMembership(space, idParams[1], idParams[2], uriInfo.getPath(), getQueryParam("expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @PUT
  @Path("{id}")
  @RolesAllowed("users")
  public Response updateSpaceMembershipById(@Context UriInfo uriInfo,
                                             SpaceMembershipRestIn model) throws Exception {
    String[] idParams = getPathParam("id").split(":");
    if (idParams.length != 3) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    String targetUser = idParams[1];
    if (CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, targetUser, true) == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    String spacePrettyName = idParams[0];
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    String spaceGroupId = SPACE_PREFIX + spacePrettyName;
    Space space = spaceService.getSpaceByGroupId(spaceGroupId);
    if (space == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    if (! RestUtils.isMemberOfAdminGroup() && ! spaceService.isManager(space, authenticatedUser)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    space.setEditor(authenticatedUser);
    if (model.getRole() != null && model.getRole().equals("manager") && ! spaceService.isManager(space, targetUser)) {
      spaceService.setManager(space, targetUser, true);
    }
    if (model.getRole() != null && model.getRole().equals("member") && spaceService.isManager(space, targetUser)) {
      spaceService.setManager(space, targetUser, false);
    }
    //
    String role = idParams[2];
    return Util.getResponse(RestUtils.buildEntityFromSpaceMembership(space, targetUser, role, uriInfo.getPath(), getQueryParam("expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @DELETE
  @Path("{id}")
  @RolesAllowed("users")
  public Response deleteSpaceMembershipById(@Context UriInfo uriInfo) throws Exception {
    String[] idParams = getPathParam("id").split(":");
    if (idParams.length != 3) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    String targetUser = idParams[1];
    if (CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, targetUser, true) == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    String spacePrettyName = idParams[0];
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    String spaceGroupId = SPACE_PREFIX + spacePrettyName;
    Space space = spaceService.getSpaceByGroupId(spaceGroupId);
    if (space == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    if (! authenticatedUser.equals(targetUser) && ! RestUtils.isMemberOfAdminGroup() && ! spaceService.isManager(space, authenticatedUser)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    String role = idParams[2];
    space.setEditor(authenticatedUser);
    if (role != null && role.equals("manager")) {
      spaceService.setManager(space, targetUser, false);
    }
    if (role != null && role.equals("member")) {
      if (spaceService.isManager(space, targetUser)) {
        spaceService.setManager(space, targetUser, false);
      }
      spaceService.removeMember(space, targetUser);
    }
    //
    return Util.getResponse(RestUtils.buildEntityFromSpaceMembership(space, targetUser, role, uriInfo.getPath(), getQueryParam("expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  private void setSpaceMemberships(List<Map<String, Object>> spaceMemberships, List<Space> spaces, 
      String userId, UriInfo uriInfo) {
    for (Space space : spaces) {
      if (userId != null) {
        if (ArrayUtils.contains(space.getMembers(), userId)) {
          spaceMemberships.add(RestUtils.buildEntityFromSpaceMembership(space, userId, "member", uriInfo.getPath(), getQueryParam("expand")));
        }
        if (ArrayUtils.contains(space.getManagers(), userId)) {
          spaceMemberships.add(RestUtils.buildEntityFromSpaceMembership(space, userId, "manager", uriInfo.getPath(), getQueryParam("expand")));
        }
      } else {
        for (String user : space.getMembers()) {
          spaceMemberships.add(RestUtils.buildEntityFromSpaceMembership(space, user, "member", uriInfo.getPath(), getQueryParam("expand")));
        }
        for (String user : space.getManagers()) {
          spaceMemberships.add(RestUtils.buildEntityFromSpaceMembership(space, user, "manager", uriInfo.getPath(), getQueryParam("expand")));
        }
      }
    }
  }
  
  private MembershipType getMembershipType(String status) {
    try {
      return MembershipType.valueOf(status);
    } catch (Exception e) {
      return MembershipType.ALL;
    }
  }
  
}
