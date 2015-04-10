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
package org.exoplatform.social.rest.impl.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.rest.api.AbstractSocialRestService;
import org.exoplatform.social.rest.api.ActivityRestResources;
import org.exoplatform.social.rest.entity.ActivitiesCollections;
import org.exoplatform.social.rest.entity.CommentsCollections;
import org.exoplatform.social.service.rest.RestProperties;
import org.exoplatform.social.service.rest.RestUtils;
import org.exoplatform.social.service.rest.Util;
import org.exoplatform.social.service.rest.api.models.ActivityRestIn;
import org.exoplatform.social.service.rest.api.models.CommentRestIn;

@Path("v1/social/activities")
public class ActivityRestResourcesV1 extends AbstractSocialRestService implements ActivityRestResources {
  
  @GET
  @RolesAllowed("users")
  public Response getActivitiesOfCurrentUser(@Context UriInfo uriInfo) throws Exception {
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    
    Identity currentUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser, true);
    
    int limit = getQueryValueLimit();
    int offset = getQueryValueOffset();
    
    ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
    RealtimeListAccess<ExoSocialActivity> listAccess = activityManager.getAllActivitiesWithListAccess();
    List<ExoSocialActivity> activities = listAccess.loadAsList(offset, limit);
    
    List<Map<String, Object>> activitiesInfo = new ArrayList<Map<String, Object>>();
    for (ExoSocialActivity activity : activities) {
      Map<String, String> as = RestUtils.getActivityStream(activity, currentUser);
      if (as == null) continue;
      Map<String, Object> activityInfo = RestUtils.buildEntityFromActivity(activity, uriInfo.getPath(), getQueryParam("expand"));
      activityInfo.put(RestProperties.ACTIVITY_STREAM, as);
      //
      activitiesInfo.add(activityInfo); 
    }
    
    ActivitiesCollections activitiesCollections = new ActivitiesCollections(getQueryValueReturnSize() ? listAccess.getSize() : -1, offset, limit);
    activitiesCollections.setActivities(activitiesInfo);
    
    return Util.getResponse(activitiesCollections, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @GET
  @Path("{id}")
  @RolesAllowed("users")
  public Response getActivityById(@Context UriInfo uriInfo) throws Exception {
    String id = getPathParam("id");
    
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    Identity currentUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser, true);
    
    ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
    ExoSocialActivity activity = activityManager.getActivity(id);
    if (activity == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    Map<String, String> as = RestUtils.getActivityStream(activity.isComment() ? activityManager.getParentActivity(activity) : activity, currentUser);
    if (as == null) { //current user doesn't have permission to view activity
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    Map<String, Object> activityInfo = RestUtils.buildEntityFromActivity(activity, uriInfo.getPath(), getQueryParam("expand"));
    if (! activity.isComment()) {
      activityInfo.put(RestProperties.ACTIVITY_STREAM, as);
    }
    
    return Util.getResponse(activityInfo, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response updateActivityById(@Context UriInfo uriInfo,
                                      ActivityRestIn model) throws Exception {
    if (model == null || model.getTitle() == null || model.getTitle().length() == 0) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    //
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    Identity currentUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser, true);
    
    String id = getPathParam("id");
    ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
    ExoSocialActivity activity = activityManager.getActivity(id);
    if (activity == null || ! activity.getPosterId().equals(currentUser.getId())) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    //update activity's title
    activity.setTitle(model.getTitle());
    activityManager.updateActivity(activity);
    
    Map<String, String> as = RestUtils.getActivityStream(activity, currentUser);
    Map<String, Object> activityInfo = RestUtils.buildEntityFromActivity(activity, uriInfo.getPath(), getQueryParam("expand"));
    activityInfo.put(RestProperties.ACTIVITY_STREAM, as);
    
    return Util.getResponse(activityInfo, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @DELETE
  @Path("{id}")
  @RolesAllowed("users")
  public Response deleteActivityById(@Context UriInfo uriInfo) throws Exception {
    String id = getPathParam("id");
    
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    Identity currentUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser, true);
    
    ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
    ExoSocialActivity activity = activityManager.getActivity(id);
    if (activity == null || ! activity.getPosterId().equals(currentUser.getId()) || ! activity.getStreamOwner().equals(currentUser.getRemoteId())) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    Map<String, String> as = RestUtils.getActivityStream(activity, currentUser);
    Map<String, Object> activityInfo = RestUtils.buildEntityFromActivity(activity, uriInfo.getPath(), getQueryParam("expand"));
    activityInfo.put(RestProperties.ACTIVITY_STREAM, as);
    
    activityManager.deleteActivity(activity);
    
    return Util.getResponse(activityInfo, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @GET
  @Path("{id}/comments")
  @RolesAllowed("users")
  public Response getCommentsOfActivity(@Context UriInfo uriInfo) throws Exception {
    String id = getPathParam("id");
    
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    Identity currentUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser, true);
    
    int limit = getQueryValueLimit();
    int offset = getQueryValueOffset();
    
    ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
    ExoSocialActivity activity = activityManager.getActivity(id);
    if (activity == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    if (RestUtils.getActivityStream(activity, currentUser) == null) { //current user doesn't have permission to view activity
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    RealtimeListAccess<ExoSocialActivity> listAccess = activityManager.getCommentsWithListAccess(activity);
    List<ExoSocialActivity> comments = listAccess.loadAsList(offset, limit);
    
    List<Map<String, Object>> commentsInfo = new ArrayList<Map<String, Object>>();
    for (ExoSocialActivity comment : comments) {
      Map<String, Object> commentInfo = RestUtils.buildEntityFromActivity(comment, uriInfo.getPath(), getQueryParam("expand"));
      //
      commentsInfo.add(commentInfo);
    }
    
    CommentsCollections commentsCollections = new CommentsCollections(getQueryValueReturnSize() ? listAccess.getSize() : -1, offset, limit);
    commentsCollections.setComments(commentsInfo);
    
    return Util.getResponse(commentsCollections, uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
  
  @POST
  @Path("{id}/comments")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response postComment(@Context UriInfo uriInfo,
                               CommentRestIn model) throws Exception {
    if (model == null || model.getText() == null || model.getText().length() == 0) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    String id = getPathParam("id");
    
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    Identity currentUser = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser, true);
    
    ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
    ExoSocialActivity activity = activityManager.getActivity(id);
    if (activity == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    if (RestUtils.getActivityStream(activity, currentUser) == null) { //current user doesn't have permission to view activity
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    
    ExoSocialActivity comment = new ExoSocialActivityImpl();
    comment.setTitle(model.getText());
    comment.setUserId(currentUser.getId());
    activityManager.saveComment(activity, comment);
    
    return Util.getResponse(RestUtils.buildEntityFromActivity(activityManager.getActivity(comment.getId()), uriInfo.getPath(), getQueryParam("expand")), uriInfo, RestUtils.getJsonMediaType(), Response.Status.OK);
  }
}
