package org.exoplatform.social.rest.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.social.service.rest.api.models.ActivityRestIn;
import org.exoplatform.social.service.rest.api.models.CommentRestIn;

public interface ActivityRestResources extends SocialRest {

  /**
   * Process to return all activities in json format
   * 
   * @param uriInfo
   * @return
   * @throws Exception
   */
  @GET
  public Response getActivitiesOfCurrentUser(@Context UriInfo uriInfo) throws Exception;
  
  /**
   * Process to return an activity by id in json format
   * 
   * @param uriInfo
   * @return
   * @throws Exception
   */
  @GET
  @Path("{id}")
  public Response getActivityById(@Context UriInfo uriInfo) throws Exception;
  
  /**
   * Process to update the title of an activity by id
   * 
   * @param uriInfo
   * @return
   * @throws Exception
   */
  @PUT
  @Path("{id}")
  public Response updateActivityById(@Context UriInfo uriInfo,
                                      ActivityRestIn model) throws Exception;
  
  /**
   * Process to delete an activity by id
   * 
   * @param uriInfo
   * @return
   * @throws Exception
   */
  @DELETE
  @Path("{id}")
  public Response deleteActivityById(@Context UriInfo uriInfo) throws Exception;
  
  /**
   * Process to return all comments of an activity in json format
   * 
   * @param uriInfo
   * @return
   * @throws Exception
   */
  @GET
  @Path("{id}/comments")
  public Response getCommentsOfActivity(@Context UriInfo uriInfo) throws Exception;
  
  /**
   * Process to create new comment
   * 
   * @param uriInfo
   * @return
   * @throws Exception
   */
  @POST
  @Path("{id}/comments")
  public Response postComment(@Context UriInfo uriInfo,
                               CommentRestIn model) throws Exception;
  
}
