package org.exoplatform.social.core.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.activity.filter.ActivityFilter;
import org.exoplatform.social.core.activity.filter.ActivityUpdateFilter;
import org.exoplatform.social.core.entity.Activity;
import org.exoplatform.social.core.entity.ActivityStreamEntity;
import org.exoplatform.social.core.entity.Comment;
import org.exoplatform.social.core.entity.StreamItem;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage.TimestampType;
import org.exoplatform.social.core.storage.impl.ActivityBuilderWhere;

/**
 * Created by bdechateauvieux on 4/18/15.
 */
public class ActivityDao {

  private final EntityManagerFactory FACTORY;

  public ActivityDao() {
    FACTORY = Persistence.createEntityManagerFactory("org.exoplatform.social.hibernate-activity");
  }

  private void saveEntity(Object entity) {
    try {
      EntityManager entityManager = FACTORY.createEntityManager();
      entityManager.getTransaction().begin();
      entityManager.persist(entity);
      entityManager.getTransaction().commit();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void updateEntity(Object entity) {
    try {
      EntityManager entityManager = FACTORY.createEntityManager();
      entityManager.getTransaction().begin();
      entityManager.merge(entity);
      entityManager.getTransaction().commit();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void removeEntity(Object entity) {
    EntityManager entityManager = FACTORY.createEntityManager();
    entityManager.getTransaction().begin();
    entityManager.remove(entity);
    entityManager.getTransaction().commit();
  }

  public List<Activity> getActivityByLikerId(String likerId) {
    // TODO One activityManager per session
    EntityManager entityManager = FACTORY.createEntityManager();
    TypedQuery<Activity> query = entityManager.createNamedQuery("getActivitiesByLikerId", Activity.class);
    query.setParameter("likerId", "1");
    return query.getResultList();
  }

  public Activity getActivity(String activityId) throws ActivityStorageException {
    EntityManager entityManager = FACTORY.createEntityManager();
    return entityManager.find(Activity.class, activityId);
  }

  public List<Activity> getUserActivities(Identity owner) throws ActivityStorageException {
    return getUserActivities(owner, 0, -1);
  }

  public List<Activity> getUserActivities(Identity owner, long offset, long limit) throws ActivityStorageException {
    EntityManager entityManager = FACTORY.createEntityManager();
    TypedQuery<Activity> query = entityManager.createNamedQuery("getUserActivities", Activity.class);
    query.setParameter("ownerId", owner.getId());
    if (limit > 0) {
      query.setFirstResult((int) offset);
      query.setMaxResults((int) limit);
    }
    return query.getResultList();
  }

  public List<Activity> getActivities(Identity owner, Identity viewer, long offset, long limit) throws ActivityStorageException {
    StringBuilder strQuery = new StringBuilder();
    strQuery.append("select new ")
            .append(StreamItem.class.getName())
            .append("(activityId) from StreamItem as s join a.activityId Activity a where (s.viewerId = '")
            .append(viewer.getId())
            .append("') and (a.ownerId ='")
            .append(owner.getId())
            .append("')");

    EntityManager entityManager = FACTORY.createEntityManager();
    TypedQuery<StreamItem> query = entityManager.createNamedQuery(strQuery.toString(), StreamItem.class);
    if (limit > 0) {
      query.setFirstResult((int) offset);
      query.setMaxResults((int) limit);
    }

    List<Activity> activities = new ArrayList<Activity>();
    List<StreamItem> streamItems = query.getResultList();
    for (StreamItem streamItem : streamItems) {
      activities.add(streamItem.getActivity());
    }
    return activities;
  }

  public void saveComment(Activity activity, Comment comment) throws ActivityStorageException {
  }

  public Activity saveActivity(Identity owner, Activity activity) throws ActivityStorageException {
    String remoter = owner.getRemoteId();
    activity.setPosterId(activity.getOwnerId() != null ? activity.getOwnerId() : owner.getId());
    //
    ActivityStreamEntity stream = new ActivityStreamEntity();
    stream.setId(owner.getId());
    stream.setPrettyId(remoter);
    stream.setType(owner.getProviderId());
    //
    activity.setActivityStream(stream);
    //
    saveEntity(activity);
    //
    return activity;
  }

  public Activity getActivityByComment(Comment comment) throws ActivityStorageException {
    return null;
  }

  public void deleteActivity(Activity activity) throws ActivityStorageException {
    removeEntity(activity);
  }

  public void deleteComment(Comment comment) throws ActivityStorageException {
    removeEntity(comment);
  }

  public List<Activity> getActivitiesOfIdentities(List<Identity> connectionList, long offset, long limit) throws ActivityStorageException {
    return null;
  }

  public List<Activity> getActivitiesOfIdentities(List<Identity> connectionList, TimestampType type, long offset, long limit) throws ActivityStorageException {
    return null;
  }

  public int getNumberOfUserActivities(Identity owner) throws ActivityStorageException {
    return 0;
  }

  public int getNumberOfUserActivitiesForUpgrade(Identity owner) throws ActivityStorageException {
    return 0;
  }

  public int getNumberOfNewerOnUserActivities(Identity ownerIdentity, Activity baseActivity) {
    return 0;
  }

  public List<Activity> getNewerOnUserActivities(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getOlderOnUserActivities(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getActivityFeed(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getActivityFeedForUpgrade(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfActivitesOnActivityFeedForUpgrade(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerOnActivityFeed(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getOlderOnActivityFeed(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getActivitiesOfConnections(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getActivitiesOfConnectionsForUpgrade(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfActivitiesOfConnections(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfActivitiesOfConnectionsForUpgrade(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getActivitiesOfIdentity(Identity ownerIdentity, long offset, long limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerOnActivitiesOfConnections(Identity ownerIdentity, Activity baseActivity, long limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getOlderOnActivitiesOfConnections(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getUserSpacesActivitiesForUpgrade(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfUserSpacesActivities(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfUserSpacesActivitiesForUpgrade(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerOnUserSpacesActivities(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getOlderOnUserSpacesActivities(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getComments(Activity existingActivity) {
    return getComments(existingActivity, 0, -1);
  }

  public List<Activity> getComments(Activity existingActivity, int offset, int limit) {
    return null;
  }

  public int getNumberOfComments(Activity existingActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerComments(Activity existingActivity, Activity baseComment) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerComments(Activity existingActivity, Activity baseComment, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderComments(Activity existingActivity, Activity baseComment) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getOlderComments(Activity existingActivity, Activity baseComment, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public SortedSet<ActivityProcessor> getActivityProcessors() {
    // TODO Auto-generated method stub
    return null;
  }

  public void updateActivity(Activity existingActivity) throws ActivityStorageException {
    updateEntity(existingActivity);
  }

  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerOnUserActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getActivitiesOfIdentities(ActivityBuilderWhere where, ActivityFilter filter, long offset, long limit) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfSpaceActivities(Identity spaceIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfSpaceActivitiesForUpgrade(Identity spaceIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getSpaceActivities(Identity spaceIdentity, int index, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getSpaceActivitiesForUpgrade(Identity spaceIdentity, int index, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getActivitiesByPoster(Identity posterIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getActivitiesByPoster(Identity posterIdentity, int offset, int limit, String... activityTypes) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfActivitiesByPoster(Identity posterIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfActivitiesByPoster(Identity ownerIdentity, Identity viewerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerOnSpaceActivities(Identity spaceIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getOlderOnSpaceActivities(Identity spaceIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderOnSpaceActivities(Identity spaceIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfUpdatedOnActivityFeed(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfUpdatedOnUserActivities(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfUpdatedOnActivitiesOfConnections(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfUpdatedOnUserSpacesActivities(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfUpdatedOnSpaceActivities(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfMultiUpdated(Identity owner, Map<String, Long> sinceTimes) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerFeedActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getNewerUserActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getNewerUserSpacesActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getNewerActivitiesOfConnections(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getNewerSpaceActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getOlderFeedActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getOlderUserActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getOlderUserSpacesActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getOlderActivitiesOfConnections(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getOlderSpaceActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfOlderOnSpaceActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerComments(Activity existingActivity, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getOlderComments(Activity existingActivity, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfNewerComments(Activity existingActivity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfOlderComments(Activity existingActivity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public Activity getComment(String commentId) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }
}
