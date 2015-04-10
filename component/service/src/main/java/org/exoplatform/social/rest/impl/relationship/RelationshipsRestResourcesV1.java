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
package org.exoplatform.social.rest.impl.relationship;

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
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.social.rest.api.RelationshipsRestResources;
import org.exoplatform.social.rest.api.RestProperties;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.social.rest.entity.CollectionEntity;
import org.exoplatform.social.rest.entity.DataEntity;
import org.exoplatform.social.rest.entity.RelationshipEntity;
import org.exoplatform.social.service.rest.api.VersionResources;

@Path(VersionResources.VERSION_ONE + "/social/relationships")
public class RelationshipsRestResourcesV1 implements RelationshipsRestResources {

  public RelationshipsRestResourcesV1() {
  }
  
  @GET
  @RolesAllowed("users")
  public Response getRelationships(@Context UriInfo uriInfo) throws Exception {
    String status = RestUtils.getQueryParam(uriInfo, "status");
    int limit = RestUtils.getLimit(uriInfo);
    int offset = RestUtils.getOffset(uriInfo);
    //
    RelationshipManager relationshipManager = CommonsUtils.getService(RelationshipManager.class);
    Relationship.Type type;
    try {
      type = Relationship.Type.valueOf(status.toUpperCase());
    } catch (Exception e) {
      type = Relationship.Type.ALL;
    }
    
    List<Relationship> relationships = new ArrayList<Relationship>();
    int size = 0;
    if (RestUtils.isMemberOfAdminGroup()) {
      relationships = relationshipManager.getRelationshipsByStatus(null, type, offset, limit);
      size = relationshipManager.getRelationshipsCountByStatus(null, type);
    } else {
      String currentUser = ConversationState.getCurrent().getIdentity().getUserId();
      Identity authenticatedUser = CommonsUtils.getService(IdentityManager.class)
                                               .getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentUser , true);
      relationships = relationshipManager.getRelationshipsByStatus(authenticatedUser, type, offset, limit);
      size = relationshipManager.getRelationshipsCountByStatus(authenticatedUser, type);
    }
    List<DataEntity> relationshipEntities = EntityBuilder.buildRelationshipEntities(relationships, uriInfo);
    CollectionEntity collectionRelationship = new CollectionEntity(relationshipEntities, RestProperties.RELATIONSHIPS, offset, limit);
    if (RestUtils.isReturnSize(uriInfo)) {
      collectionRelationship.setSize(size);
    }
    //
    return EntityBuilder.getResponse(collectionRelationship, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response createRelationship(@Context UriInfo uriInfo,
                                     RelationshipEntity model) throws Exception {
    String senderRemoteId, receiverRemoteId;
    if (model == null || (senderRemoteId = model.getSender()) == null || senderRemoteId.isEmpty()
                      || (receiverRemoteId = model.getReceiver()) == null || receiverRemoteId.isEmpty()) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    if (! RestUtils.isMemberOfAdminGroup() && ! authenticatedUser.equals(senderRemoteId) && ! authenticatedUser.equals(receiverRemoteId)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    Relationship.Type type;
    try {
      type = Relationship.Type.valueOf(model.getStatus().toUpperCase());
    } catch (Exception e) {
      type = Relationship.Type.ALL;
    }
    RelationshipManager relationshipManager = CommonsUtils.getService(RelationshipManager.class);
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    Identity receiver = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, receiverRemoteId, true);
    Identity sender = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, senderRemoteId, true);

    Relationship relationship = relationshipManager.get(sender, receiver);
    if (relationship != null) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    relationship = new Relationship(sender, receiver, type);
    relationshipManager.update(relationship);

    return EntityBuilder.getResponse(EntityBuilder.buildEntityRelationship(relationship, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand"), true), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @GET
  @Path("{id}")
  @RolesAllowed("users")
  public Response getRelationshipById(@Context UriInfo uriInfo) throws Exception {
    String id = RestUtils.getPathParam(uriInfo, "id");
    
    Identity authenticatedUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, ConversationState.getCurrent().getIdentity().getUserId(), true);
    RelationshipManager relationshipManager = CommonsUtils.getService(RelationshipManager.class);
    Relationship relationship = relationshipManager.get(id);
    if (relationship == null || ! hasPermissionOnRelationship(authenticatedUser, relationship)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    return EntityBuilder.getResponse(EntityBuilder.buildEntityRelationship(relationship, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand"), true), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response updateRelationshipById(@Context UriInfo uriInfo,
                                         RelationshipEntity model) throws Exception {
    
    if(model == null || model.getStatus() == null || model.getStatus().length() == 0) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    String id = RestUtils.getPathParam(uriInfo, "id");
    Identity authenticatedUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, ConversationState.getCurrent().getIdentity().getUserId(), true);
    RelationshipManager relationshipManager = CommonsUtils.getService(RelationshipManager.class);
    Relationship relationship = relationshipManager.get(id);
    if (relationship == null || ! hasPermissionOnRelationship(authenticatedUser, relationship)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    Relationship.Type type;
    try {
      type = Relationship.Type.valueOf(model.getStatus().toUpperCase());
    } catch (Exception e) {
      type = null;
    }
    
    if (type != null && ! type.equals(Relationship.Type.ALL)) {
      if (type.equals(Relationship.Type.IGNORED)) {
        relationshipManager.delete(relationship);
      } else {
        relationship.setStatus(type);
        relationshipManager.update(relationship);
      }
    }
    
    return EntityBuilder.getResponse(EntityBuilder.buildEntityRelationship(relationship, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand"), false), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @DELETE
  @Path("{id}")
  @RolesAllowed("users")
  public Response deleteRelationshipById(@Context UriInfo uriInfo) throws Exception {
    String id = RestUtils.getPathParam(uriInfo, "id");
    
    Identity authenticatedUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, ConversationState.getCurrent().getIdentity().getUserId(), true);
    RelationshipManager relationshipManager = CommonsUtils.getService(RelationshipManager.class);
    Relationship relationship = relationshipManager.get(id);
    if (relationship == null || ! hasPermissionOnRelationship(authenticatedUser, relationship)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //delete the relationship
    relationshipManager.delete(relationship);
    
    return EntityBuilder.getResponse(EntityBuilder.buildEntityRelationship(relationship, uriInfo.getPath(), RestUtils.getQueryParam(uriInfo, "expand"), false), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }

  /**
   * Check if the viewer is an administrator or the receiver or the sender of relationship
   * 
   * @param authenticatedUser
   * @param relationship
   * @return
   */
  private boolean hasPermissionOnRelationship(Identity authenticatedUser, Relationship relationship) {
    if (RestUtils.isMemberOfAdminGroup()) return true;
    if (authenticatedUser.getId().equals(relationship.getSender().getId())
        || authenticatedUser.getId().equals(relationship.getReceiver().getId())) {
      return true;
    }
    return false;
  }
}
