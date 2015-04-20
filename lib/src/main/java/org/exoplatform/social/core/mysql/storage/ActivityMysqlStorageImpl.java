/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.mysql.storage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.chromattic.api.ChromatticException;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.activity.filter.ActivityFilter;
import org.exoplatform.social.core.activity.filter.ActivityUpdateFilter;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ActivityStreamImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.mysql.MysqlDBConnect;
import org.exoplatform.social.core.mysql.model.ActivityEntity;
import org.exoplatform.social.core.mysql.model.StreamItem;
import org.exoplatform.social.core.mysql.model.StreamItemImpl;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.relationship.model.Relationship.Type;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.ActivityStreamStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.exception.NodeNotFoundException;
import org.exoplatform.social.core.storage.impl.ActivityBuilderWhere;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.core.storage.impl.StorageUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Huy Quang
 *          quangnh2@exoplatform.com
 * Dec 12, 2013  
 */
public class ActivityMysqlStorageImpl extends ActivityStorageImpl {

  private static final Pattern MENTION_PATTERN = Pattern.compile("@([^\\s]+)|@([^\\s]+)$");
  public static final Pattern USER_NAME_VALIDATOR_REGEX = Pattern.compile("^[\\p{L}][\\p{L}._\\-\\d]+$");
  
  private static final String TIME = "time";
  private static final String POSTED_TIME = "postedTime";
  
  public enum ViewerType {
    SPACE("SPACE"), POSTER("POSTER"), LIKER("LIKER"), COMMENTER("COMMENTER"), MENTIONER("MENTIONER"), SPACE_MEMBER("SPACE_MEMBER");

    private final String type;

    public String getType() {
      return type;
    }

    ViewerType(String type) {
      this.type = type;
    }
  }
  
  private final SortedSet<ActivityProcessor> activityProcessors;
  private final RelationshipStorage relationshipStorage;
  private final IdentityStorage identityStorage;
  private final SpaceStorage spaceStorage;
  private ActivityStorage activityStorage;
  private MysqlDBConnect dbConnect;
  

  private static final Log LOG = ExoLogger.getLogger(ActivityMysqlStorageImpl.class);
  
  public ActivityMysqlStorageImpl(final RelationshipStorage relationshipStorage,
                                  final IdentityStorage identityStorage,
                                  final SpaceStorage spaceStorage,
                                  final ActivityStreamStorage streamStorage,
                                  MysqlDBConnect dbConnect) {

    super(relationshipStorage, identityStorage, spaceStorage, streamStorage);
    this.relationshipStorage = relationshipStorage;
    this.identityStorage = identityStorage;
    this.spaceStorage = spaceStorage;
    this.dbConnect = dbConnect;
    this.activityProcessors = new TreeSet<ActivityProcessor>(processorComparator());
  }
  
  @Override
	public void setInjectStreams(boolean mustInject) {

	}

	@Override
	public ExoSocialActivity getActivity(String activityId) throws ActivityStorageException {

    //
    // ActivityEntity activityEntity = _findById(ActivityEntity.class,
    // activityId);
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;

    StringBuilder getActivitySQL = new StringBuilder();
    getActivitySQL.append("select ")
                  .append("_id, title, titleId, body, bodyId, postedTime, lastUpdated, posterId, ownerId, ownerIdentityId,")
                  .append("permaLink, appId, externalId, priority, hidable, lockable, likers, mentioners, commenters, commentIds, metadata, templateParams, activityType")
                  .append(" from activity where _id = ?");

    ExoSocialActivity activity = new ActivityEntity();

    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(getActivitySQL.toString());
      preparedStatement.setString(1, activityId);

      rs = preparedStatement.executeQuery();

      while (rs.next()) {
        fillActivityFromResultSet(rs, activity);
      }

      processActivity(activity);
      //activity.setTitle("test abc");

      LOG.debug("activity found");

      return activity;

    } catch (SQLException e) {

      LOG.error("error in activity look up:", e.getMessage());
      return null;

    } finally {
      try {
        if (rs != null) {
          rs.close();
        }

        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e);
      }
    }
  }

  private void fillActivityFromResultSet(ResultSet rs, ExoSocialActivity activity) throws SQLException{

    activity.setId(rs.getString("_id"));
    activity.setTitle(rs.getString("title"));
    activity.setTitleId(rs.getString("titleId"));
    activity.setBody(rs.getString("body"));
    activity.setBodyId(rs.getString("bodyId"));
    activity.setUserId(rs.getString("posterId"));
    activity.setPostedTime(rs.getLong("postedTime"));
    activity.setUpdated(rs.getLong("lastUpdated"));
    //activity.setType(activityEntity.getType());
    activity.setAppId(rs.getString("appId"));
    activity.setExternalId(rs.getString("externalId"));
    //activity.setUrl(activityEntity.getUrl());
    activity.setPriority(rs.getFloat("priority"));
    if (rs.wasNull()) {
      activity.setPriority(null);
    }
    activity.setPosterId(rs.getString("posterId"));
    //
    byte[] st = (byte[]) rs.getObject("templateParams");
    try {
      ByteArrayInputStream baip = new ByteArrayInputStream(st);
      ObjectInputStream ois = new ObjectInputStream(baip);
      activity.setTemplateParams((Map<String, String>) ois.readObject());
    } catch (Exception e) {
      LOG.debug("Failed to get templateParams of activity from database");
    }

    String[] commentIds = StringUtils.split(rs.getString("commentIds"), ",");
    activity.setReplyToId(commentIds);
    
    String lks = rs.getString("likers");
    String[] likes = StringUtils.split(lks, ",");
    if (likes != null) {
      activity.setLikeIdentityIds(likes);
    }
    
    String[] mentioners = StringUtils.split(rs.getString("mentioners"), ",");
    if (mentioners != null) {
      activity.setMentionedIds(mentioners);
    }
    
    String[] commenters = StringUtils.split(rs.getString("commenters"), ",");
    if (commenters != null) {
      activity.setCommentedIds(commenters);
    }
    
    //mentioners and commenters are moved to StreamItem
    
    //
    activity.isLocked(rs.getBoolean("lockable"));
    activity.isHidden(rs.getBoolean("hidable"));
    activity.setType(rs.getString("activityType"));
    
    
    ActivityStream stream = new ActivityStreamImpl();
    String ownerIdentityId = rs.getString("ownerIdentityId");
    Identity owner = identityStorage.findIdentityById(ownerIdentityId);
    stream.setType(owner.getProviderId());
    stream.setPrettyId(owner.getRemoteId());
    stream.setId(owner.getId());

    //
    activity.setActivityStream(stream);
    activity.setStreamOwner(owner.getRemoteId());
    activity.setStreamId(ownerIdentityId);
  }
  
  
  private void processActivity(ExoSocialActivity existingActivity) {
    Iterator<ActivityProcessor> it = activityProcessors.iterator();
    while (it.hasNext()) {
      try {
        it.next().processActivity(existingActivity);
      } catch (Exception e) {
        LOG.warn("activity processing failed ");
      }
    }
  }
  
	@Override
	public List<ExoSocialActivity> getUserActivities(Identity owner) throws ActivityStorageException {
		return getUserActivities(owner, 0, -1);
	}

	@Override
	public List<ExoSocialActivity> getUserActivities(Identity owner, long offset, long limit) throws ActivityStorageException {
	  return getUserActivitiesForUpgrade(owner, offset, limit);
	}

	@Override
	public List<ExoSocialActivity> getUserActivitiesForUpgrade(Identity owner, long offset, long limit) throws ActivityStorageException {
	  return getUserActivities(owner, -1, false, offset, limit);
	}
	
	private String buildSQLQueryByTime(String timeField, long time, boolean isNewer) {
	  if (time < 0) return "";
	  StringBuilder sb = new StringBuilder();
	  if (isNewer) {
	    sb.append(" and ").append(timeField).append(" > '").append(time).append("'");
	  } else {
	    sb.append(" and ").append(timeField).append(" < '").append(time).append("'");
	  }
	  return sb.toString();
	}
	
	private List<ExoSocialActivity> getUserActivities(Identity owner, long time, boolean isNewer, long offset, long limit) throws ActivityStorageException {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;

    StringBuilder getActivitySQL = new StringBuilder();
    getActivitySQL.append("select distinct activityId")
                  .append(" from stream_item where (((viewerId = ? and not viewerType like '%SPACE%')")
                  .append(" or (posterId = ? and viewerType is null)) and hidable='0'")
                  .append(buildSQLQueryByTime(TIME, time, isNewer))
                  .append(")")
                  .append(" order by time desc")
                  .append(limit > 0 ? " LIMIT " + limit : "").append(offset >= 0 ? " OFFSET " + offset : "");

    List<ExoSocialActivity> list = new ArrayList<ExoSocialActivity>();
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(getActivitySQL.toString());
      preparedStatement.setString(1, owner.getId());
      preparedStatement.setString(2, owner.getId());
      rs = preparedStatement.executeQuery();
      //
      while (rs.next()) {
        ExoSocialActivity activity = getStorage().getActivity(rs.getString("activityId"));
        list.add(activity);
      }
      LOG.debug("activities found");
      return list;

    } catch (SQLException e) {
      LOG.error("error in stream items look up:", e.getMessage());
      return new ArrayList<ExoSocialActivity>();
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
    
  }

  @Override
  public List<ExoSocialActivity> getActivities(Identity owner,
                                               Identity viewer,
                                               long offset,
                                               long limit) throws ActivityStorageException {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;

    String[] identityIds = getIdentities(owner, viewer);

    StringBuilder getActivitySQL = new StringBuilder();
    getActivitySQL.append("select distinct activityId")
                  .append(" from stream_item where (posterId in ('")
                  .append(StringUtils.join(identityIds, "','"))
                  .append("') ")
                  .append(" and not viewerType like '%SPACE%' and hidable='0')")
                  .append(" order by time desc")
                  .append(limit > 0 ? " LIMIT " + limit : "").append(offset >= 0 ? " OFFSET " + offset : "");

    List<ExoSocialActivity> list = new ArrayList<ExoSocialActivity>();
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(getActivitySQL.toString());

      rs = preparedStatement.executeQuery();

      while (rs.next()) {
        ExoSocialActivity activity = getStorage().getActivity(rs.getString("activityId"));
        list.add(activity);
      }

      LOG.debug("activities found");

      return list;

    } catch (SQLException e) {

      LOG.error("error in activities look up:", e.getMessage());
      return new ArrayList<ExoSocialActivity>();

    } finally {
      try {
        if (rs != null) {
          rs.close();
        }

        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }

  }

  private String[] getIdentities(Identity owner, Identity viewer) {
    List<String> posterIdentities = new ArrayList<String>();
    posterIdentities.add(owner.getId());

    if (viewer != null && owner.getId().equals(viewer.getId()) == false) {
      Relationship rel = relationshipStorage.getRelationship(owner, viewer);

      boolean hasRelationship = false;
      if (rel != null && rel.getStatus() == Type.CONFIRMED) {
        hasRelationship = true;
      }

      if (hasRelationship) {
        posterIdentities.add(viewer.getId());
      }
    }
    
    return posterIdentities.toArray(new String[0]);
  }
  
  @Override
  public void saveComment(ExoSocialActivity activity, ExoSocialActivity comment) throws ActivityStorageException {

    LOG.debug("begin to create comment");

    // insert to mysql comment table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;

    StringBuilder insertTableSQL = new StringBuilder();
    insertTableSQL.append("INSERT INTO comment")
                  .append("(_id, activityId, title, titleId, body, bodyId, postedTime, lastUpdated, posterId, mentioners, ")
                  .append("hidable, lockable, templateParams)")
                  .append("VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");

    StringBuilder updateActivitySQL = new StringBuilder();
    updateActivitySQL.append("update activity set lastUpdated = ?, mentioners = ?, commenters = ?, commentIds = ? where _id = ?");

    long currentMillis = System.currentTimeMillis();
    long commentMillis = (comment.getPostedTime() != null ? comment.getPostedTime() : currentMillis);
    List<String> mentioners = new ArrayList<String>();
    activity.setMentionedIds(processMentions(activity.getMentionedIds(), comment.getTitle(), mentioners, true));
    List<String> commenters = new ArrayList<String>(Arrays.asList(activity.getCommentedIds()));
    if (comment.getUserId() != null && ! commenters.contains(comment.getUserId())) {
      commenters.add(comment.getUserId());
    }
    activity.setCommentedIds(commenters.toArray(new String[0]));
    
    comment.setMentionedIds(processMentions(comment.getTitle()));
    try {
      dbConnection = dbConnect.getDBConnection();

      // insert comment
      preparedStatement = dbConnection.prepareStatement(insertTableSQL.toString());

      comment.setId(UUID.randomUUID().toString());
      preparedStatement.setString(1, comment.getId());
      preparedStatement.setString(2, activity.getId());
      preparedStatement.setString(3, comment.getTitle());
      preparedStatement.setString(4, comment.getTitleId());
      preparedStatement.setString(5, comment.getBody());
      preparedStatement.setString(6, comment.getBodyId());
      preparedStatement.setLong(7, commentMillis);
      preparedStatement.setLong(8, commentMillis);
      preparedStatement.setString(9, comment.getUserId());
      preparedStatement.setString(10, StringUtils.join(comment.getMentionedIds(),","));
      preparedStatement.setBoolean(11, activity.isHidden());
      preparedStatement.setBoolean(12, activity.isLocked());
      //
      if (comment.getTemplateParams() != null) {
        try {
          ByteArrayOutputStream b = new ByteArrayOutputStream();
          ObjectOutputStream output = new ObjectOutputStream(b);
          output.writeObject(comment.getTemplateParams());
          preparedStatement.setBinaryStream(13, new ByteArrayInputStream(b.toByteArray()));
        } catch (IOException e) {
          LOG.debug("Failed to save templateParams of activity into database");
        }
      } else {
        preparedStatement.setNull(13, Types.BLOB);
      }

      preparedStatement.executeUpdate();

      LOG.debug("new comment created");

      //
      List<String> commentIds = new ArrayList(Arrays.asList(activity.getReplyToId()));
      commentIds.add(comment.getId());
      
      // update activity
      preparedStatement = dbConnection.prepareStatement(updateActivitySQL.toString());
      preparedStatement.setLong(1, commentMillis);
      preparedStatement.setString(2, StringUtils.join(activity.getMentionedIds(),","));
      preparedStatement.setString(3, StringUtils.join(activity.getCommentedIds(),","));
      preparedStatement.setString(4, StringUtils.join(commentIds,","));
      preparedStatement.setString(5, activity.getId());

      preparedStatement.executeUpdate();

      LOG.debug("activity updated");

      activity.setReplyToId(commentIds.toArray(new String[commentIds.size()]));
      activity.setUpdated(currentMillis);
    } catch (SQLException e) {

      LOG.error("error in comment creation:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }

    comment.setUpdated(commentMillis);

    Identity poster = new Identity(activity.getPosterId());
    poster.setRemoteId(activity.getStreamOwner());

    commenter(poster, activity, comment);

    updateMentioner(poster, activity, comment);

  }

  /**
   * Creates StreamItem for each user who commented on the activity
   * 
   * @param poster poster of activity
   * @param activity
   * @param comment
   * @throws MongoException
   */
  private void commenter(Identity poster, ExoSocialActivity activity, ExoSocialActivity comment) {
    StreamItem o = getStreamItem(activity.getId(), comment.getUserId());
    
    if (o == null) {
      // create new stream item for COMMENTER
      createStreamItem(activity.getId(),
                       poster.getRemoteId(),
                       activity.getUserId() != null ? activity.getUserId() : poster.getId(),
                       comment.getUserId(),
                       ViewerType.COMMENTER.getType(),
                       activity.isHidden(),
                       activity.isLocked(),
                       comment.getUpdated().getTime());
      
    } else {
      //update COMMENTER
      if (StringUtils.isBlank(o.getViewerType())) {
        //add new commenter on this stream item
        updateStreamItem(o.getId(), ViewerType.COMMENTER.getType(), o.getViewerId(), 1, o.getMentioner(), comment.getUpdated().getTime());
      } else {
        String[] viewTypes = o.getViewerType().split(",");
        
        if(ArrayUtils.contains(viewTypes, ViewerType.COMMENTER.getType())){
          //increment only number of commenter
          updateStreamItem(o.getId(), o.getViewerType(), o.getViewerId(), o.getCommenter() + 1, o.getMentioner(), comment.getUpdated().getTime());
        } else {
          //add new COMMENTER element to viewerTypes field
          updateStreamItem(o.getId(), o.getViewerType() + "," + ViewerType.COMMENTER.getType(), o.getViewerId(), 1, o.getMentioner(), comment.getUpdated().getTime());
        }
      }
    }
    
  }
  
  private void updateMentioner(Identity poster,
                               ExoSocialActivity activity,
                               ExoSocialActivity comment) {

    String[] mentionIds = processMentions(comment.getTitle());

    for (String mentioner : mentionIds) {
      //
      StreamItem entity = getStreamItem(activity.getId(), mentioner);
      if (entity == null) {
        createStreamItem(activity.getId(),
                         poster.getRemoteId(),
                         activity.getUserId() != null ? activity.getUserId() : poster.getId(),
                         poster.getId(),
                         ViewerType.MENTIONER.getType(),
                         activity.isHidden(),
                         activity.isLocked(),
                         activity.getPostedTime());
      } else {
        // update mention
        updateMention(entity, mentioner, comment);
      }
    }

  }
  
  private void updateMention(StreamItem entity, String mentionId, ExoSocialActivity comment) {
    //
    String mentionType = ViewerType.MENTIONER.getType();
    int mentionNum = 1;
    String viewerTypes = null;
    String viewerId = null;
    if (StringUtils.isBlank(entity.getViewerType())) {
      //viewerType = MENTIONER + commenter = 1 + viewerId = mentionId
      viewerTypes = ViewerType.MENTIONER.getType();
      viewerId = mentionId;
    } else {
      viewerId = entity.getViewerId();
      
      String[] arrViewTypes = entity.getViewerType().split(",");

      if (ArrayUtils.contains(arrViewTypes, mentionType)) {
        // increase number by 1
        mentionNum = entity.getMentioner() + 1;
        viewerTypes = entity.getViewerType();
      } else {
        // add new type MENTIONER to arrViewTypes
        arrViewTypes = (String[]) ArrayUtils.add(arrViewTypes, mentionType);
        viewerTypes = StringUtils.join(arrViewTypes, ",");
      }

      // update mentioner
    }
    
    //update time comment.getUpdated().getTime()
    updateStreamItem(entity.getId(), viewerTypes, viewerId, entity.getCommenter(), mentionNum, comment.getUpdated().getTime());
  }
  
  /**
   * update stream item's comment info
   */
  private void updateStreamItem(String id, String viewerTypes, String viewerId, Integer commenterNum, Integer mentionerNum, Long time) {
    //insert to mysql stream_item table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
 
    StringBuilder insertTableSQL = new StringBuilder();
    insertTableSQL.append("update stream_item")
                  .append(" set viewerType = ?, viewerId = ?, commenter =?, mentioner = ?, time = ?")
                  .append(" where _id = ?");
    
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(insertTableSQL.toString());
      preparedStatement.setString(1, viewerTypes);
      preparedStatement.setString(2, viewerId);
      preparedStatement.setInt(3, commenterNum);
      preparedStatement.setInt(4, mentionerNum);
      preparedStatement.setLong(5, time);
      preparedStatement.setString(6, id);
      
      preparedStatement.executeUpdate();
 
      LOG.debug("stream item updated");
 
    } catch (SQLException e) {
 
      LOG.error("error in stream item update:", e.getMessage());
 
    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }
  
  /**
   * get a stream item by activity, viewer and type
   */
  private StreamItem getStreamItem(String activityId, String viewerId) {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;

    StringBuilder getActivitySQL = new StringBuilder();
    getActivitySQL.append("select ")
                  .append("_id, activityId, ownerId, posterId, viewerId, viewerType, hidable, lockable, time, mentioner, commenter")
                  .append(" from stream_item where activityId = ? and viewerId = ? and hidable='0'");

    StreamItem item = null;

    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(getActivitySQL.toString());
      preparedStatement.setString(1, activityId);
      preparedStatement.setString(2, viewerId);

      rs = preparedStatement.executeQuery();

      while (rs.next()) {
        item = fillStreamItemFromResultSet(rs);
      }

      LOG.debug("stream item found");

      return item;

    } catch (SQLException e) {

      LOG.error("error in stream item look up:", e.getMessage());
      return null;

    } finally {
      try {
        if (rs != null) {
          rs.close();
        }

        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }
  
  /**
   * fill in StreamItem object from ResultSet
   */
  private StreamItem fillStreamItemFromResultSet(ResultSet rs) throws SQLException{
    StreamItem item = new StreamItemImpl();
    item.setId(rs.getString("_id"));
    item.setActivityId(rs.getString("activityId"));
    item.setOwnerId(rs.getString("ownerId"));
    item.setPosterId(rs.getString("posterId"));
    item.setViewerId(rs.getString("viewerId"));
    item.setViewerType(rs.getString("viewerType"));
    item.setHidable(rs.getBoolean("hidable"));
    item.setLockable(rs.getBoolean("lockable"));
    item.setTime(rs.getLong("time"));
    item.setMentioner(rs.getInt("mentioner"));
    item.setCommenter(rs.getInt("commenter"));
    return item;
  }
  
	@Override
	public ExoSocialActivity saveActivity(Identity owner,
			ExoSocialActivity activity) throws ActivityStorageException {

    try {
      Validate.notNull(owner, "owner must not be null.");
      Validate.notNull(activity, "activity must not be null.");
      Validate.notNull(activity.getUpdated(), "Activity.getUpdated() must not be null.");
      Validate.notNull(activity.getPostedTime(), "Activity.getPostedTime() must not be null.");
      Validate.notNull(activity.getTitle(), "Activity.getTitle() must not be null.");
    } catch (IllegalArgumentException e) {
      throw new ActivityStorageException(ActivityStorageException.Type.ILLEGAL_ARGUMENTS, e.getMessage(), e);
    }

    try {
      if (activity.getId() == null) {
        _createActivity(owner, activity);
      } else {
        _saveActivity(activity);
      }

      LOG.debug(String.format(
          "Activity %s by %s (%s) saved",
          activity.getTitle(),
          activity.getUserId(),
          activity.getId()
      ));

      return activity;

    }
    catch (NodeNotFoundException e) {
      throw new ActivityStorageException(ActivityStorageException.Type.FAILED_TO_SAVE_ACTIVITY, e.getMessage(), e);
    } catch (ChromatticException ex) {
      Throwable throwable = ex.getCause();
      if (throwable instanceof ItemExistsException || 
          throwable instanceof InvalidItemStateException) {
        LOG.warn("Probably was inserted activity by another session");
        LOG.debug(ex.getMessage(), ex);
        return activity;
      } else {
        throw new ActivityStorageException(ActivityStorageException.Type.FAILED_TO_SAVE_ACTIVITY, ex.getMessage());
      }
    }
	}

  protected String[] _createActivity(Identity owner, ExoSocialActivity activity) throws NodeNotFoundException {

    LOG.debug("begin to create activity");
    
    // Create activity
    long currentMillis = System.currentTimeMillis();
    long activityMillis = (activity.getPostedTime() != null ? activity.getPostedTime() : currentMillis);
    activity.setPostedTime(activityMillis);
    activity.setUpdated(activityMillis);

    //records activity for mention case.
    activity.setMentionedIds(processMentions(activity.getTitle()));
    
    if(owner != null){
      String remoter = owner.getRemoteId();
      activity.setPosterId(activity.getUserId() != null ? activity.getUserId() : owner.getId());
      activity.setStreamOwner(remoter);
      activity.setStreamId(owner.getId());
      //
      ActivityStream stream = new ActivityStreamImpl();
      stream.setId(owner.getId());
      stream.setPrettyId(remoter);
      stream.setType(owner.getProviderId());
      activity.setActivityStream(stream);
      
    }
    activity.setReplyToId(new String[]{});
    
    //insert to mysql activity table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
 
    StringBuilder insertTableSQL = new StringBuilder();
    insertTableSQL.append("INSERT INTO activity")
                  .append("(_id, title, titleId, body, bodyId, postedTime, lastUpdated, posterId, ownerId, ownerIdentityId,")
                  .append("permaLink, appId, externalId, priority, hidable, lockable, likers, mentioners, commenters, commentIds, metadata, templateParams, activityType)")
                  .append("VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
    
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(insertTableSQL.toString());
 
      activity.setId(UUID.randomUUID().toString());
      preparedStatement.setString(1, activity.getId());
      fillPreparedStatementFromActivity(owner, activity, preparedStatement, 2);
      
      preparedStatement.executeUpdate();
 
      LOG.debug("new activity created");
 
    } catch (SQLException e) {
 
      LOG.error("error in activity creation:", e.getMessage());
 
    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
    //end of insertion

    //fillStream(null, activity);
    newStreamItemForNewActivity(owner, activity);
    
    return null;
  }
  
  private int fillPreparedStatementFromActivity(Identity owner,
                                                 ExoSocialActivity activity,
                                                 PreparedStatement preparedStatement,
                                                 int index) throws SQLException {
    preparedStatement.setString(index++, activity.getTitle());
    preparedStatement.setString(index++, activity.getTitleId());
    preparedStatement.setString(index++, activity.getBody());
    preparedStatement.setString(index++, activity.getBodyId());
    preparedStatement.setLong(index++, activity.getPostedTime());
    preparedStatement.setLong(index++, activity.getUpdated().getTime());
    preparedStatement.setString(index++, activity.getPosterId());
    preparedStatement.setString(index++, owner.getRemoteId());
    preparedStatement.setString(index++, owner.getId());
    preparedStatement.setString(index++, activity.getPermaLink());
    preparedStatement.setString(index++, activity.getAppId());
    preparedStatement.setString(index++, activity.getExternalId());
    if(activity.getPriority() == null){
      preparedStatement.setNull(index++, Types.FLOAT);
    }else{
      preparedStatement.setFloat(index++, activity.getPriority());
    }
    preparedStatement.setBoolean(index++, activity.isHidden());
    preparedStatement.setBoolean(index++, activity.isLocked());
    preparedStatement.setString(index++, StringUtils.join(activity.getLikeIdentityIds(),","));
    preparedStatement.setString(index++, StringUtils.join(activity.getMentionedIds(),","));
    preparedStatement.setString(index++, StringUtils.join(activity.getCommentedIds(),","));
    preparedStatement.setString(index++, StringUtils.join(activity.getReplyToId(),","));
    preparedStatement.setString(index++, null);
    //
    if (activity.getTemplateParams() != null) {
      try {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(b);
        output.writeObject(activity.getTemplateParams());
        preparedStatement.setBinaryStream(index++, new ByteArrayInputStream(b.toByteArray()));
      } catch (IOException e) {
        LOG.debug("Failed to save templateParams of activity into database");
      }
    } else {
      preparedStatement.setNull(index++, Types.BLOB);
    }
    preparedStatement.setString(index++, activity.getType());
    
    return index;
  }
  
  private void newStreamItemForNewActivity(Identity poster, ExoSocialActivity activity) {
    //create StreamItem
    if (OrganizationIdentityProvider.NAME.equals(poster.getProviderId())) {
      //poster
      poster(poster, activity);
      //connection
      //connection(poster, activity);
      //mention
      mention(poster, activity);
    } else {
      //for SPACE
      spaceMembers(poster, activity);
    }
  }
  
  private void poster(Identity poster, ExoSocialActivity activity) {
    createStreamItem(activity.getId(),
                     poster.getRemoteId(),
                     activity.getUserId() != null ? activity.getUserId() : poster.getId(),
                     poster.getId(),
                     ViewerType.POSTER.getType(),
                     activity.isHidden(),
                     activity.isLocked(),
                     activity.getPostedTime());
  }
        
  /**
   * Creates StreamItem for each user who has mentioned on the activity
   * 
   * @param poster
   * @param activity
   * @throws MongoException
   */
  private void mention(Identity poster, ExoSocialActivity activity) {
    for (String mentioner : activity.getMentionedIds()) {
      createStreamItem(activity.getId(),
                       poster.getRemoteId(),
                       activity.getUserId() != null ? activity.getUserId() : poster.getId(),
                       mentioner,
                       ViewerType.MENTIONER.getType(),
                       activity.isHidden(),
                       activity.isLocked(),
                       activity.getPostedTime());
    }
  }

  private void spaceMembers(Identity poster, ExoSocialActivity activity) {
    Space space = spaceStorage.getSpaceByPrettyName(poster.getRemoteId());
    
    if (space == null) return;

    createStreamItem(activity.getId(),
                     poster.getRemoteId(),
                     activity.getUserId() != null ? activity.getUserId() : poster.getId(),
                     poster.getId(),
                     null,
                     activity.isHidden(),
                     activity.isLocked(),
                     activity.getPostedTime());
  }

  
  private void createStreamItem(String activityId, String ownerId, String posterId, String viewerId, 
                                String viewerType, Boolean hidable, Boolean lockable, Long time){
    //insert to mysql stream_item table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
 
    StringBuilder insertTableSQL = new StringBuilder();
    insertTableSQL.append("INSERT INTO stream_item")
                  .append("(_id, activityId, ownerId, posterId, viewerId, viewerType,")
                  .append("hidable, lockable, time)")
                  .append("VALUES (?,?,?,?,?,?,?,?,?)");
    
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(insertTableSQL.toString());
      preparedStatement.setString(1, UUID.randomUUID().toString());
      preparedStatement.setString(2, activityId);
      preparedStatement.setString(3, ownerId);
      preparedStatement.setString(4, posterId);
      preparedStatement.setString(5, viewerId);
      preparedStatement.setString(6, viewerType);
      preparedStatement.setBoolean(7, hidable);
      preparedStatement.setBoolean(8, lockable);
      preparedStatement.setLong(9, time);
      
      preparedStatement.executeUpdate();
 
      LOG.debug("new stream item created");
 
    } catch (SQLException e) {
 
      LOG.error("error in stream item creation:", e.getMessage());
 
    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
    
  }
  
  protected void _saveActivity(ExoSocialActivity activity) {
    LOG.debug("begin to update activity");
    
    // Update activity
    ExoSocialActivity dbActivity = getActivity(activity.getId());
    String[] orginLikers = dbActivity.getLikeIdentityIds();
    
    long currentMillis = System.currentTimeMillis();
    if (activity.getTitle() == null) {
      activity.setTitle(dbActivity.getTitle());
    }
    if (activity.getBody() == null) {
      activity.setBody(dbActivity.getBody());
    }
    if (activity.getTemplateParams() == null) {
      activity.setTemplateParams(dbActivity.getTemplateParams());
    }
    activity.setUpdated(currentMillis);
    //insert to mysql activity table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
 
    StringBuilder sql = new StringBuilder();
    sql.append("update activity ")
                  .append("set title=?, titleId=?, body=?, bodyId=?, postedTime=?, lastUpdated=?, posterId=?, ownerId=?, ownerIdentityId=?,")
                  .append("permaLink=?, appId=?, externalId=?, priority=?, hidable=?, lockable=?, likers=?, mentioners=?, commenters=?, commentIds=?, metadata=?, templateParams=?, activityType=?")
                  .append(" where _id = ?");
    
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(sql.toString());
 
      Identity owner = identityStorage.findIdentityById(activity.getStreamId());
      int index = fillPreparedStatementFromActivity(owner, activity, preparedStatement, 1);
      preparedStatement.setString(index, activity.getId());
      
      preparedStatement.executeUpdate();
 
      LOG.debug("activity updated");
 
    } catch (SQLException e) {
 
      LOG.error("error in activity update:", e.getMessage());
 
    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
      //
    }
    
    updateStreamItemTime(activity.getId(), dbActivity.getUpdated().getTime(), activity.isHidden());
    
    //update likers
    String[] removedLikes = StorageUtils.sub(orginLikers, activity.getLikeIdentityIds());
    String[] addedLikes = StorageUtils.sub(activity.getLikeIdentityIds(), orginLikers);
    if (removedLikes.length > 0 || addedLikes.length > 0) {
      manageActivityLikes(addedLikes, removedLikes, activity);
    }
  }
  
  private void manageActivityLikes(String[] addedLikes,
                                   String[] removedLikes,
                                   ExoSocialActivity activity) {
    if (addedLikes != null) {
      for (String liker : addedLikes) {
        like(activity, liker);
      }
    }

    if (removedLikes != null) {
      for (String liker : removedLikes) {
        unLike(activity, liker);
      }
    }
  }
  
  private void like(ExoSocialActivity activity, String userId) throws ActivityStorageException {
    //
    String likeType = ViewerType.LIKER.getType();
    StreamItem o = getStreamItem(activity.getId(), userId);

    if (o == null) {
      // create new stream item for LIKER
      createStreamItem(activity.getId(),
                       activity.getStreamOwner(),
                       activity.getUserId() != null ? activity.getUserId() : activity.getPosterId(),
                       userId,
                       ViewerType.LIKER.getType(),
                       activity.isHidden(),
                       activity.isLocked(),
                       activity.getUpdated().getTime());
    } else {
      // update LIKER
      String[] viewTypes = o.getViewerType().split(",");

      if (ArrayUtils.contains(viewTypes, likeType)) {
        updateStreamItem(o.getId(),
                         o.getViewerType(),
                         o.getViewerId(),
                         o.getCommenter(),
                         o.getMentioner(),
                         activity.getUpdated().getTime());
      } else {
        String newViewTypes = StringUtils.join(ArrayUtils.add(viewTypes, likeType), ",");
        updateStreamItem(o.getId(),
                         newViewTypes,
                         o.getViewerId(),
                         o.getCommenter(),
                         o.getMentioner(),
                         activity.getUpdated().getTime());
      }
    }
  }
  
  private void unLike(ExoSocialActivity activity, String userId) throws ActivityStorageException {
    StreamItem o = getStreamItem(activity.getId(), userId);

    if (o != null) {
      // update LIKER
      String[] viewTypes = o.getViewerType().split(",");
      String[] newViewTypes = (String[]) ArrayUtils.removeElement(viewTypes, ViewerType.LIKER.name());
      boolean removeable = userId.equals(o.getPosterId()) ? false : true;
      
      if (newViewTypes.length == 0 && removeable) {
        deleteStreamItem(o.getId());
      } else {
        updateStreamItem(o.getId(),
                         StringUtils.join(newViewTypes, ","),
                         o.getViewerId(),
                         o.getCommenter(),
                         o.getMentioner(),
                         activity.getUpdated().getTime());
      }
    }
  }
  
  /**
   * update stream item's comment info
   */
  private void updateStreamItemTime(String activityId, Long time, boolean isHidden) {
    //insert to mysql stream_item table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
 
    StringBuilder sql = new StringBuilder();
    sql.append("update stream_item")
                  .append(" set time = ?, hidable = ?")
                  .append(" where activityId = ?");
    
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setLong(1, time);
      preparedStatement.setBoolean(2, isHidden);
      preparedStatement.setString(3, activityId);
      
      preparedStatement.executeUpdate();
 
      LOG.debug("stream item updated");
 
    } catch (SQLException e) {
 
      LOG.error("error in stream item update:", e.getMessage());
 
    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }

	@Override
	public ExoSocialActivity getParentActivity(ExoSocialActivity comment)
			throws ActivityStorageException {
		// This method is not used anymore
		return null;
	}

	@Override
	public void deleteActivity(String activityId)
			throws ActivityStorageException {
	  LOG.debug("begin to delete activity");

    // insert to mysql comment table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;

    StringBuilder sql = new StringBuilder("delete from activity where _id = ?");

    try {
      dbConnection = dbConnect.getDBConnection();

      // insert comment
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, activityId);
      preparedStatement.executeUpdate();

      deleteCommentByActivity(activityId);
      deleteStreamItemByActivity(activityId);
      
      LOG.debug("activity deleted");

    } catch (SQLException e) {

      LOG.error("error in activity deletion:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }

	}

  private void deleteCommentByActivity(String activityId) throws ActivityStorageException {
    LOG.debug("begin to delete comments");

    // insert to mysql comment table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;

    StringBuilder sql = new StringBuilder("delete from comment where activityId = ?");

    try {
      dbConnection = dbConnect.getDBConnection();

      // insert comment
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, activityId);
      preparedStatement.executeUpdate();

      LOG.debug("comments deleted");

    } catch (SQLException e) {

      LOG.error("error in comment deletion:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }

  }
	 
  private void deleteStreamItemByActivity(String activityId) throws ActivityStorageException {
    LOG.debug("begin to delete stream items");

    // insert to mysql comment table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;

    StringBuilder sql = new StringBuilder("delete from stream_item where activityId = ?");

    try {
      dbConnection = dbConnect.getDBConnection();

      // insert comment
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, activityId);
      preparedStatement.executeUpdate();

      LOG.debug("stream items deleted");

    } catch (SQLException e) {

      LOG.error("error in stream items deletion:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }

  }
  
	@Override
	public void deleteComment(String activityId, String commentId)
			throws ActivityStorageException {
	  //
	  updateParentActivity(activityId, commentId);
	  //
    deleteComment(commentId);
	}

  private void updateParentActivity(String activityId, String commentId) {
    ExoSocialActivity comment = getComment(commentId);
    ExoSocialActivity activity = getActivity(activityId);
    String[] mentioners = activity.getMentionedIds();
    String[] commenters = activity.getCommentedIds();
    List<String> commentIds = new ArrayList<String>(Arrays.asList(activity.getReplyToId()));
    mentioners = processMentions(mentioners, comment.getTitle(), new ArrayList<String>(), false);
    commenters = processCommenters(commenters, comment.getPosterId(), new ArrayList<String>(), false);
    commentIds.remove(commentId);
    
    String[] mentionIds = processMentions(comment.getTitle());
    //update activities refs for mentioner
    removeMentioner(activityId, mentionIds);
    
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    dbConnection = dbConnect.getDBConnection();
    long currentMillis = System.currentTimeMillis();
    long commentMillis = (comment.getPostedTime() != null ? comment.getPostedTime() : currentMillis);
    StringBuilder insertTableSQL = new StringBuilder();
    try {
      // insert comment
      preparedStatement = dbConnection.prepareStatement(insertTableSQL.toString());
      
      StringBuilder updateActivitySQL = new StringBuilder();
      updateActivitySQL.append("update activity set lastUpdated = ?,mentioners = ?, commenters = ?, commentIds = ? where _id = ?");
  
      List<String> removedMentioners = new ArrayList<String>(Arrays.asList(mentionIds));
      
      // update activity
      preparedStatement = dbConnection.prepareStatement(updateActivitySQL.toString());
      preparedStatement.setLong(1, commentMillis);
      preparedStatement.setString(2, StringUtils.join(processMentions(activity.getMentionedIds(), comment.getTitle(), removedMentioners, false),","));
      preparedStatement.setString(3, StringUtils.join(activity.getCommentedIds(),","));
      preparedStatement.setString(4, StringUtils.join(commentIds,","));
      preparedStatement.setString(5, activity.getId());
  
      preparedStatement.executeUpdate();
    } catch (SQLException e) {

      LOG.error("error in updating activity:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }

  private void removeMentioner(String activityId, String[] mentionIds) {
    if(ArrayUtils.isEmpty(mentionIds)){
      return;
    }
    
    List<StreamItem> items = getStreamItem(activityId, mentionIds);
    if(CollectionUtils.isEmpty(items)){
      return;
    }
    
    for(StreamItem it:items){
      //update
      if (StringUtils.isNotBlank(it.getViewerType())) {
        String[] viewTypes = it.getViewerType().split(",");
        
        //if MENTIONER is Poster, don't remove stream item
        boolean removeable = ArrayUtils.contains(mentionIds, it.getPosterId()) ? false : true;
        
        if (it.getMentioner() > 0) {
          int number = it.getMentioner() - 1;
          if (number == 0) {
            //remove Mentioner
            String[] newViewTypes = (String[]) ArrayUtils.removeElement(viewTypes, ViewerType.MENTIONER.name());
            if (newViewTypes.length == 0 && removeable) {
              //delete stream item
              deleteStreamItem(it.getId());
            }else{
              //update number + viewType
              updateStreamItem(it.getId(), StringUtils.join(newViewTypes,","), it.getViewerId(), it.getCommenter(), number, it.getTime());
            }
          } else {
            //update number
            updateStreamItem(it.getId(), it.getViewerType(), it.getViewerId(), it.getCommenter(), number, it.getTime());
          }
        }
      }
    }
    
  }
  
  private String[] processMentions(String[] mentionerIds, String title, List<String> addedOrRemovedIds, boolean isAdded) {
    if (title == null || title.length() == 0) {
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }
    
    Matcher matcher = MENTION_PATTERN.matcher(title);
    while (matcher.find()) {
      String remoteId = matcher.group().substring(1);
      if (!USER_NAME_VALIDATOR_REGEX.matcher(remoteId).matches()) {
        continue;
      }
      Identity identity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, remoteId);
      // if not the right mention then ignore
      if (identity != null) { 
        String mentionStr = identity.getId() + MENTION_CHAR; // identityId@
        mentionerIds = isAdded ? add(mentionerIds, mentionStr, addedOrRemovedIds) : remove(mentionerIds, mentionStr, addedOrRemovedIds);
      }
    }
    return mentionerIds;
  }
  
  private String[] processCommenters(String[] commenters, String commenter, List<String> addedOrRemovedIds, boolean isAdded) {
    if (commenter == null || commenter.length() == 0) {
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }
    
    String newCommenter = commenter + MENTION_CHAR; 
    commenters = isAdded ? add(commenters, newCommenter, addedOrRemovedIds) : remove(commenters, newCommenter, addedOrRemovedIds);
    
    return commenters;
  }
  
  private String[] add(String[] mentionerIds, String mentionStr, List<String> addedOrRemovedIds) {
    if (ArrayUtils.toString(mentionerIds).indexOf(mentionStr) == -1) { // the first mention
      addedOrRemovedIds.add(mentionStr.replace(MENTION_CHAR, ""));
      return (String[]) ArrayUtils.add(mentionerIds, mentionStr + 1);
    }
    
    String storedId = null;
    for (String mentionerId : mentionerIds) {
      if (mentionerId.indexOf(mentionStr) != -1) {
        mentionerIds = (String[]) ArrayUtils.removeElement(mentionerIds, mentionerId);
        storedId = mentionStr + (Integer.parseInt(mentionerId.split(MENTION_CHAR)[1]) + 1);
        break;
      }
    }
    
    addedOrRemovedIds.add(mentionStr.replace(MENTION_CHAR, ""));
    mentionerIds = (String[]) ArrayUtils.add(mentionerIds, storedId);
    return mentionerIds;
  }
  
  private String[] remove(String[] mentionerIds, String mentionStr, List<String> addedOrRemovedIds) {
    for (String mentionerId : mentionerIds) {
      if (mentionerId.indexOf(mentionStr) != -1) {
        int numStored = Integer.parseInt(mentionerId.split(MENTION_CHAR)[1]) - 1;
        
        if (numStored == 0) {
          addedOrRemovedIds.add(mentionStr.replace(MENTION_CHAR, ""));
          return (String[]) ArrayUtils.removeElement(mentionerIds, mentionerId);
        }

        mentionerIds = (String[]) ArrayUtils.removeElement(mentionerIds, mentionerId);
        mentionerIds = (String[]) ArrayUtils.add(mentionerIds, mentionStr + numStored);
        break;
      }
    }
    return mentionerIds;
  }
    
  private void deleteStreamItem(String id) throws ActivityStorageException {
    LOG.debug("begin to delete stream item");

    // insert to mysql comment table
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;

    StringBuilder sql = new StringBuilder("delete from stream_item where _id = ?");

    try {
      dbConnection = dbConnect.getDBConnection();

      // insert comment
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, id);
      preparedStatement.executeUpdate();

      LOG.debug("stream item deleted");

    } catch (SQLException e) {

      LOG.error("error in stream item deletion:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }

  }
  
  /**
   * get a stream item by activity, viewer and type
   */
  private List<StreamItem> getStreamItem(String activityId, String[] mentionIds) {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;

    StringBuilder getActivitySQL = new StringBuilder();
    getActivitySQL.append("select ")
                  .append("_id, activityId, ownerId, posterId, viewerId, viewerType, hidable, lockable, time, mentioner, commenter")
                  .append(" from stream_item where activityId = ? and viewerId in ('")
                  .append(StringUtils.join(mentionIds, "','")).append("') and hidable='0'");

    List<StreamItem> list = new ArrayList<StreamItem>();
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(getActivitySQL.toString());
      preparedStatement.setString(1, activityId);

      rs = preparedStatement.executeQuery();

      while (rs.next()) {
        StreamItem item = fillStreamItemFromResultSet(rs);
        list.add(item);
      }

      LOG.debug("stream items found");

      return list;

    } catch (SQLException e) {

      LOG.error("error in stream items look up:", e.getMessage());
      return new ArrayList<StreamItem>();

    } finally {
      try {
        if (rs != null) {
          rs.close();
        }

        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }
  
  /**
   * Processes Mentioners who has been mentioned via the Activity.
   * 
   * @param title
   */
  private String[] processMentions(String title) {
    String[] mentionerIds = new String[0];
    if (title == null || title.length() == 0) {
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    Matcher matcher = MENTION_PATTERN.matcher(title);
    while (matcher.find()) {
      String remoteId = matcher.group().substring(1);
      if (!USER_NAME_VALIDATOR_REGEX.matcher(remoteId).matches()) {
        continue;
      }
      Identity identity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, remoteId);
      // if not the right mention then ignore
      if (identity != null) {
        mentionerIds = (String[]) ArrayUtils.add(mentionerIds, identity.getId());
      }
    }
    return mentionerIds;
  }

  
	public ExoSocialActivity getComment(String id){
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;

    StringBuilder sql = new StringBuilder();
    sql.append("select ")
                  .append("_id, activityId, title, titleId, body, bodyId, postedTime,")
                  .append("lastUpdated, mentioners, posterId, ownerId, permaLink, hidable, lockable, templateParams")
                  .append(" from comment where _id = ?");

    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, id);

      rs = preparedStatement.executeQuery();
      ExoSocialActivity comment = null;
      
      while (rs.next()) {
        comment = fillCommentFromResultSet(rs);
      }

      LOG.debug("comment found");

      return comment;

    } catch (SQLException e) {

      LOG.error("error in comment look up:", e.getMessage());
      return null;

    } finally {
      try {
        if (rs != null) {
          rs.close();
        }

        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    } 
	}
	
	private void deleteComment(String id){
	  LOG.debug("begin to delete comment");

    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;

    StringBuilder sql = new StringBuilder("delete from comment where _id = ?");

    try {
      dbConnection = dbConnect.getDBConnection();

      // insert comment
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, id);
      preparedStatement.executeUpdate();

      LOG.debug("comment deleted");

    } catch (SQLException e) {

      LOG.error("error in comment deletion:", e.getMessage());

    } finally {
      try {
        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
	}
	
	@Override
	public List<ExoSocialActivity> getActivitiesOfIdentities(
			List<Identity> connectionList, long offset, long limit)
			throws ActivityStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getActivitiesOfIdentities(
			List<Identity> connectionList, TimestampType type, long offset,
			long limit) throws ActivityStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfUserActivities(Identity owner)
			throws ActivityStorageException {
	  return getNumberOfUserActivitiesForUpgrade(owner);
	}

  @Override
  public int getNumberOfUserActivitiesForUpgrade(Identity owner) throws ActivityStorageException {
    return getNumberOfUserActivities(owner, -1, false);
  }
  
  private int getNumberOfUserActivities(Identity owner, long time, boolean isNewer) {
    StringBuilder sql = new StringBuilder();
    sql.append("select count(distinct activityId) as count ")
       .append(" from stream_item where (((viewerId = ? and not viewerType like '%SPACE%')")
       .append(" or (posterId = ? and viewerType is null)) and hidable='0'")
       .append(buildSQLQueryByTime(TIME, time, isNewer))
       .append(")");

    return getCount(sql.toString(), owner.getId(), owner.getId());
  }

	@Override
	public int getNumberOfNewerOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
	  return getNumberOfUserActivities(ownerIdentity, baseActivity.getPostedTime(), true);
	}

	@Override
	public List<ExoSocialActivity> getNewerOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		return getUserActivities(ownerIdentity, baseActivity.getPostedTime(), true, 0, limit);
	}

	@Override
	public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
	  return getNumberOfUserActivities(ownerIdentity, baseActivity.getPostedTime(), false);
	}

	@Override
	public List<ExoSocialActivity> getOlderOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		return getUserActivities(ownerIdentity, baseActivity.getPostedTime(), false, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getActivityFeed(Identity ownerIdentity,
			int offset, int limit) {
	  return getActivityFeedForUpgrade(ownerIdentity, offset, limit);
	}
	
	private String getFeedActivitySQLQuery(Identity ownerIdentity, long time, boolean isNewer, int limit, int offset, boolean isCount) {
    List<Identity> relationships = relationshipStorage.getConnections(ownerIdentity);
    Set<String> relationshipIds = new HashSet<String>();
    for (Identity identity : relationships) {
      relationshipIds.add(identity.getId());
    }
    // get spaces where user is member
    List<Space> spaces = spaceStorage.getMemberSpaces(ownerIdentity.getRemoteId());
    String[] spaceIds = new String[0];
    for (Space space : spaces) {
      spaceIds = (String[]) ArrayUtils.add(spaceIds, space.getPrettyName());
    }
    
    StringBuilder sql = new StringBuilder();
    sql.append(isCount ? "select count(distinct activityId)" : "select distinct activityId")
       .append(" from stream_item where ")
       .append(" ((viewerId ='").append(ownerIdentity.getId()).append("'");
    
    if(CollectionUtils.isNotEmpty(spaces)){
      sql.append(" or ownerId in ('").append(StringUtils.join(spaceIds, "','")).append("') ");
    }
    
    if(CollectionUtils.isNotEmpty(relationships)){
      sql.append(" or (posterId in ('").append(StringUtils.join(relationshipIds, "','")).append("') ")
         .append("and not viewerType like '%SPACE%')");
    }
    sql.append(") and hidable='0'")
       .append(buildSQLQueryByTime(TIME, time, isNewer))
       .append(")");
    if (! isCount) {
      sql.append(" order by time desc")
         .append(limit > 0 ? " LIMIT " + limit : "").append(offset > 0 ? " OFFSET " + offset : "");
    }
    //
    return sql.toString();
  }

	@Override
	public List<ExoSocialActivity> getActivityFeedForUpgrade(Identity ownerIdentity, int offset, int limit) {
	  return getActivityFeed(ownerIdentity, -1, false, offset, limit);
	}
	
  private List<ExoSocialActivity> getActivityFeed(Identity ownerIdentity, long time, boolean isNewer, int offset, int limit) {

    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(getFeedActivitySQLQuery(ownerIdentity, time, isNewer, limit, offset, false));
      rs = preparedStatement.executeQuery();
      //
      while (rs.next()) {
        ExoSocialActivity activity = getStorage().getActivity(rs.getString("activityId"));
        result.add(activity);
      }
      LOG.debug("getActivityFeed size = " + result.size());
      return result;
    } catch (SQLException e) {
      LOG.error("error in activity look up:", e.getMessage());
      return new LinkedList<ExoSocialActivity>();
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }

  @Override
  public int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity) {
    return getNumberOfActivitesOnActivityFeedForUpgrade(ownerIdentity);
  }

  @Override
  public int getNumberOfActivitesOnActivityFeedForUpgrade(Identity ownerIdentity) {
    return getNumberOfActivityFeed(ownerIdentity, -1, false);
  }
  
  private int getNumberOfActivityFeed(Identity ownerIdentity, long time, boolean isNewer) {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(getFeedActivitySQLQuery(ownerIdentity, time, isNewer, 0, -1, true));
      rs = preparedStatement.executeQuery();
      while (rs.next()) {
        return rs.getInt(1);
      }
      return 0;
    } catch (SQLException e) {
      return 0;
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }

	@Override
	public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity) {
		return getNumberOfActivityFeed(ownerIdentity, baseActivity.getPostedTime(), true);
	}

	@Override
	public List<ExoSocialActivity> getNewerOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		return getActivityFeed(ownerIdentity, baseActivity.getPostedTime(), true, 0, limit);
	}

	@Override
	public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity) {
	  return getNumberOfActivityFeed(ownerIdentity, baseActivity.getPostedTime(), false);
	}

	@Override
	public List<ExoSocialActivity> getOlderOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
	  return getActivityFeed(ownerIdentity, baseActivity.getPostedTime(), false, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getActivitiesOfConnections(
			Identity ownerIdentity, int offset, int limit) {
	  return getActivitiesOfConnectionsForUpgrade(ownerIdentity, offset, limit);
	}
	
	private String getActivitiesOfConnectionsQuery(Identity ownerIdentity, long time, boolean isNewer, int offset, int limit, boolean isCount) {
	  List<Identity> relationships = relationshipStorage.getConnections(ownerIdentity);
    Set<String> relationshipIds = new HashSet<String>();
    for (Identity identity : relationships) {
      relationshipIds.add(identity.getId());
    }
    StringBuilder getActivitySQL = new StringBuilder();
    getActivitySQL.append(isCount ? "select count(distinct activityId)" : "select distinct activityId")
                  .append(" from stream_item where (posterId in ('")
                  .append(StringUtils.join(relationshipIds, "','"))
                  .append("') and not viewerType like '%SPACE%' and hidable='0'")
                  .append(buildSQLQueryByTime(TIME, time, isNewer))
                  .append(")");
    if (! isCount) {
      getActivitySQL.append(" order by time desc")
                    .append(limit > 0 ? " LIMIT " + limit : "").append(offset > 0 ? " OFFSET " + offset : "");
    }
    return getActivitySQL.toString();
	}
	
  @Override
  public List<ExoSocialActivity> getActivitiesOfConnectionsForUpgrade(Identity ownerIdentity, int offset, int limit) {
    return getActivitiesOfConnections(ownerIdentity, -1, false, offset, limit);
  }
  
  private List<ExoSocialActivity> getActivitiesOfConnections(Identity ownerIdentity, long time, boolean isNewer, int offset, int limit) {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;
    List<ExoSocialActivity> list = new ArrayList<ExoSocialActivity>();
    //
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(getActivitiesOfConnectionsQuery(ownerIdentity, time, isNewer, offset, limit, false));
      rs = preparedStatement.executeQuery();
      while (rs.next()) {
        ExoSocialActivity activity = getStorage().getActivity(rs.getString("activityId"));
        list.add(activity);
      }
    } catch (Exception e) {
      return list;
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
    //
    return list;
  }

  @Override
  public int getNumberOfActivitiesOfConnections(Identity ownerIdentity) {
    return getNumberOfActivitiesOfConnectionsForUpgrade(ownerIdentity);
  }

  private int getNumberActivitiesOfConnections(Identity ownerIdentity, long time, boolean isNewer) {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(getActivitiesOfConnectionsQuery(ownerIdentity, time, isNewer, 0, -1, true));
      rs = preparedStatement.executeQuery();
      while (rs.next()) {
        return rs.getInt(1);
      }
      return 0;
    } catch (SQLException e) {
      return 0;
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }
  
	@Override
	public int getNumberOfActivitiesOfConnectionsForUpgrade(Identity ownerIdentity) {
		return getNumberActivitiesOfConnections(ownerIdentity, -1, false);
	}

	@Override
	public List<ExoSocialActivity> getActivitiesOfIdentity(Identity ownerIdentity, long offset, long limit) {
		return getUserActivities(ownerIdentity, offset, limit);
	}

	@Override
	public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity) {
		return getNumberActivitiesOfConnections(ownerIdentity, baseActivity.getPostedTime(), true);
	}

	@Override
	public List<ExoSocialActivity> getNewerOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity, long limit) {
		return getActivitiesOfConnections(ownerIdentity, baseActivity.getPostedTime(), true, 0, (int) limit);
	}

	@Override
	public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity) {
	  return getNumberActivitiesOfConnections(ownerIdentity, baseActivity.getPostedTime(), false);
	}

	@Override
	public List<ExoSocialActivity> getOlderOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
	  return getActivitiesOfConnections(ownerIdentity, baseActivity.getPostedTime(), false, 0, limit);
	}
	
	private String getUserSpaceActivitiesQuery(Identity ownerIdentity, long time, boolean isNewer, int offset, int limit, boolean isCount) {
	  List<Space> spaces = spaceStorage.getMemberSpaces(ownerIdentity.getRemoteId());
    String[] spaceIds = new String[0];
    for (Space space : spaces) {
      spaceIds = (String[]) ArrayUtils.add(spaceIds, space.getPrettyName());
    }
    
    StringBuilder getActivitySQL = new StringBuilder();
    getActivitySQL.append(isCount ? "select count(distinct activityId)" : "select distinct activityId")
                  .append(" from stream_item where (ownerId in ('")
                  .append(StringUtils.join(spaceIds, "','")).append("') and hidable='0'")
                  .append(buildSQLQueryByTime(TIME, time, isNewer))
                  .append(")");
    if (! isCount) {
      getActivitySQL.append(" order by time desc")
                    .append(limit > 0 ? " LIMIT " + limit : "").append(offset > 0 ? " OFFSET " + offset : "");
    }
    
    return getActivitySQL.toString();
	}

	@Override
	public List<ExoSocialActivity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit) {
    return getUserSpacesActivitiesForUpgrade(ownerIdentity, offset, limit);
	}

	@Override
	public List<ExoSocialActivity> getUserSpacesActivitiesForUpgrade(Identity ownerIdentity, int offset, int limit) {
	  return getUserSpacesActivities(ownerIdentity, -1, false, offset, limit);
	}
	
	private List<ExoSocialActivity> getUserSpacesActivities(Identity ownerIdentity, long time, boolean isNewer, int offset, int limit) {
	  Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;
    List<ExoSocialActivity> list = new ArrayList<ExoSocialActivity>();
    //
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(getUserSpaceActivitiesQuery(ownerIdentity, time, isNewer, offset, limit, false));
      rs = preparedStatement.executeQuery();
      //
      while (rs.next()) {
        ExoSocialActivity activity = getStorage().getActivity(rs.getString("activityId"));
        list.add(activity);
      }
      LOG.debug("activities found");
      //
      return list;
    } catch (SQLException e) {
      LOG.error("error in stream items look up:", e.getMessage());
      return new ArrayList<ExoSocialActivity>();
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
	}

	@Override
	public int getNumberOfUserSpacesActivities(Identity ownerIdentity) {
	  return getNumberOfUserSpacesActivitiesForUpgrade(ownerIdentity);
	}
	
	private int getNumberOfUserSpacesActivities(Identity ownerIdentity, long time, boolean isNewer) {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(getUserSpaceActivitiesQuery(ownerIdentity, time, isNewer, 0, -1, true));
      rs = preparedStatement.executeQuery();
      while (rs.next()) {
        return rs.getInt(1);
      }
      return 0;
    } catch (SQLException e) {
      return 0;
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }

	@Override
	public int getNumberOfUserSpacesActivitiesForUpgrade(Identity ownerIdentity) {
	  return getNumberOfUserSpacesActivities(ownerIdentity, -1, false);
	}

	@Override
	public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
		return getNumberOfUserSpacesActivities(ownerIdentity, baseActivity.getPostedTime(), true);
	}

	@Override
	public List<ExoSocialActivity> getNewerOnUserSpacesActivities(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		return getUserSpacesActivities(ownerIdentity, baseActivity.getPostedTime(), true, 0, limit);
	}

	@Override
	public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
	  return getNumberOfUserSpacesActivities(ownerIdentity, baseActivity.getPostedTime(), false);
	}

	@Override
	public List<ExoSocialActivity> getOlderOnUserSpacesActivities(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
	  return getUserSpacesActivities(ownerIdentity, baseActivity.getPostedTime(), false, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getComments(ExoSocialActivity existingActivity, int offset, int limit) {
	  return getComments(existingActivity, -1, false, offset, limit);
	}
	
	private List<ExoSocialActivity> getComments(ExoSocialActivity existingActivity, long time, boolean isNewer, int offset, int limit) {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;
    //
    StringBuilder sql = new StringBuilder();
    sql.append("select _id from comment where activityId ='").append(existingActivity.getId()).append("'")
       .append(buildSQLQueryByTime(POSTED_TIME, time, isNewer))
       .append(" order by postedTime asc")
       .append(limit > 0 ? " LIMIT " + limit : "").append(offset > 0 ? " OFFSET " + offset : "");
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      rs = preparedStatement.executeQuery();
      List<ExoSocialActivity> result = new ArrayList<ExoSocialActivity>();
      while (rs.next()) {
        ExoSocialActivity comment = getStorage().getComment(rs.getString("_id"));
        processActivity(comment);
        result.add(comment);
      }
      return result;
    } catch (SQLException e) {
      LOG.error("error in comments look up:", e.getMessage());
      return new ArrayList<ExoSocialActivity>();
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }

  private ExoSocialActivity fillCommentFromResultSet(ResultSet rs) throws SQLException{
    ExoSocialActivity comment = new ActivityEntity();
    
    comment.setId(rs.getString("_id"));
    comment.setTitle(rs.getString("title"));
    comment.setTitleId(rs.getString("titleId"));
    comment.setBody(rs.getString("body"));
    comment.setBodyId(rs.getString("bodyId"));
    comment.setUserId(rs.getString("posterId"));
    comment.setPostedTime(rs.getLong("postedTime"));
    comment.setUpdated(rs.getLong("lastUpdated"));
    comment.setPosterId(rs.getString("posterId"));
    comment.setParentId(rs.getString("activityId"));

    comment.isLocked(rs.getBoolean("lockable"));
    comment.isHidden(rs.getBoolean("hidable"));
    //
    byte[] st = (byte[]) rs.getObject("templateParams");
    try {
      ByteArrayInputStream baip = new ByteArrayInputStream(st);
      ObjectInputStream ois = new ObjectInputStream(baip);
      comment.setTemplateParams((Map<String, String>) ois.readObject());
    } catch (Exception e) {
      LOG.debug("Failed to get templateParams of comment from database");
    }

    String[] mentioners = StringUtils.split(rs.getString("mentioners"), ",");
    if (mentioners != null) {
      comment.setMentionedIds(mentioners);
    }
    
    comment.setStreamOwner(rs.getString("ownerId"));
    comment.isComment(true);
    return comment;
  }
  
	@Override
	public int getNumberOfComments(ExoSocialActivity existingActivity) {
    return getNumberOfComments(existingActivity, -1, false);
	}
	
	private int getNumberOfComments(ExoSocialActivity existingActivity, long time, boolean isNewer) {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;
    //
    StringBuilder sql = new StringBuilder();
    sql.append("select count(*) numberOfComment ")
       .append(" from comment where activityId = ?")
       .append(buildSQLQueryByTime(POSTED_TIME, time, isNewer));
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(sql.toString());
      preparedStatement.setString(1, existingActivity.getId());
      rs = preparedStatement.executeQuery();
      while (rs.next()) {
        return rs.getInt(1);
      }
    } catch (SQLException e) {
      LOG.error("error in comments look up:", e.getMessage());
      return 0;
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
    return 0;
  }

	@Override
	public int getNumberOfNewerComments(ExoSocialActivity existingActivity, ExoSocialActivity baseComment) {
	  return getNumberOfComments(existingActivity, baseComment.getPostedTime(), true);
	}

	@Override
	public List<ExoSocialActivity> getNewerComments(ExoSocialActivity existingActivity, ExoSocialActivity baseComment, int limit) {
		return getComments(existingActivity, baseComment.getPostedTime(), true, 0, limit);
	}

	@Override
	public int getNumberOfOlderComments(ExoSocialActivity existingActivity, ExoSocialActivity baseComment) {
	  return getNumberOfComments(existingActivity, baseComment.getPostedTime(), false);
	}

	@Override
	public List<ExoSocialActivity> getOlderComments(ExoSocialActivity existingActivity, ExoSocialActivity baseComment, int limit) {
	  return getComments(existingActivity, baseComment.getPostedTime(), false, 0, limit);
	}

	@Override
	public SortedSet<ActivityProcessor> getActivityProcessors() {
	  return activityProcessors;
	}

	@Override
	public void updateActivity(ExoSocialActivity existingActivity)
			throws ActivityStorageException {
	  _saveActivity(existingActivity);
	}

	@Override
	public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
		return getNumberOfActivityFeed(ownerIdentity, sinceTime, true);
	}

	@Override
	public int getNumberOfNewerOnUserActivities(Identity ownerIdentity, Long sinceTime) {
		return getNumberOfUserActivities(ownerIdentity, sinceTime, true);
	}

	@Override
	public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, Long sinceTime) {
		return getNumberActivitiesOfConnections(ownerIdentity, sinceTime, true);
	}

	@Override
	public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, Long sinceTime) {
		return getNumberOfSpaceActivities(ownerIdentity, sinceTime, true);
	}

	@Override
	public List<ExoSocialActivity> getActivitiesOfIdentities(
			ActivityBuilderWhere where, ActivityFilter filter, long offset,
			long limit) throws ActivityStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfSpaceActivities(Identity spaceIdentity) {
	  return getNumberOfSpaceActivitiesForUpgrade(spaceIdentity);
	}

	@Override
	public int getNumberOfSpaceActivitiesForUpgrade(Identity spaceIdentity) {
	  return getNumberOfSpaceActivities(spaceIdentity, -1, false);
	}
	
	private int getNumberOfSpaceActivities(Identity spaceIdentity, long time, boolean isNewer) {
    StringBuilder sql = new StringBuilder();
    sql.append("select count(distinct activityId) as count from stream_item where ownerId = ? and hidable='0'")
       .append(buildSQLQueryByTime(TIME, time, isNewer));
    return getCount(sql.toString(), spaceIdentity.getRemoteId());
  }
	
	private int getCount(String sql, Object... params){
	  Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;
    int count = 0;
    
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(sql);
      
      int index = 1;
      for(Object p: params){
        if(p instanceof String){
          preparedStatement.setString(index++,(String)p);
        }else if(p instanceof Integer){
          preparedStatement.setInt(index++,(Integer)p);
        }else if(p instanceof Long){
          preparedStatement.setLong(index++,(Long)p);
        }
      }
      
      rs = preparedStatement.executeQuery();

      while (rs.next()) {
        count = rs.getInt("count");
      }

      LOG.debug("activities found");

      return count;

    } catch (SQLException e) {

      LOG.error("error in stream items look up:", e.getMessage());
      return 0;

    } finally {
      try {
        if (rs != null) {
          rs.close();
        }

        if (preparedStatement != null) {
          preparedStatement.close();
        }

        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
	}

	@Override
	public List<ExoSocialActivity> getSpaceActivities(Identity spaceIdentity, int index, int limit) {
	  return getSpaceActivitiesForUpgrade(spaceIdentity, index, limit);
	}

	@Override
	public List<ExoSocialActivity> getSpaceActivitiesForUpgrade(Identity spaceIdentity, int index, int limit) {
    return getSpaceActivities(spaceIdentity, -1, false, index, limit);
	}
	
	private List<ExoSocialActivity> getSpaceActivities(Identity spaceIdentity, long time, boolean isNewer, int index, int limit) {
    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    ResultSet rs = null;

    StringBuilder getActivitySQL = new StringBuilder();
    getActivitySQL.append("select distinct activityId")
                  .append(" from stream_item where ownerId = ? and hidable='0'")
                  .append(buildSQLQueryByTime(TIME, time, isNewer))
                  .append(" order by time desc")
                  .append(limit > 0 ? " LIMIT " + limit : "").append(index >= 0 ? " OFFSET " + index : "");
    
    List<ExoSocialActivity> list = new ArrayList<ExoSocialActivity>();
    try {
      dbConnection = dbConnect.getDBConnection();
      preparedStatement = dbConnection.prepareStatement(getActivitySQL.toString());
      preparedStatement.setString(1, spaceIdentity.getRemoteId());
      rs = preparedStatement.executeQuery();
      //
      while (rs.next()) {
        ExoSocialActivity activity = getStorage().getActivity(rs.getString("activityId"));
        list.add(activity);
      }
      return list;
    } catch (SQLException e) {
      LOG.error("error in stream items look up:", e.getMessage());
      return new ArrayList<ExoSocialActivity>();
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        if (dbConnection != null) {
          dbConnection.close();
        }
      } catch (SQLException e) {
        LOG.error("Cannot close statement or connection:", e.getMessage());
      }
    }
  }

	@Override
	public List<ExoSocialActivity> getActivitiesByPoster(
			Identity posterIdentity, int offset, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExoSocialActivity> getActivitiesByPoster(
			Identity posterIdentity, int offset, int limit,
			String... activityTypes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfActivitiesByPoster(Identity posterIdentity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfActivitiesByPoster(Identity ownerIdentity,
			Identity viewerIdentity) {
    String[] identityIds = getIdentities(ownerIdentity, viewerIdentity);
    StringBuilder sql = new StringBuilder();
    sql.append("select count(distinct activityId) as count from stream_item where posterId in('")
       .append(StringUtils.join(identityIds, "','"))
       .append("') and hidable='0'");
    return getCount(sql.toString(), new Object());
	}

	@Override
	public List<ExoSocialActivity> getNewerOnSpaceActivities(Identity spaceIdentity, ExoSocialActivity baseActivity, int limit) {
		return getSpaceActivities(spaceIdentity, baseActivity.getPostedTime(), true, 0, limit);
	}

	@Override
	public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, ExoSocialActivity baseActivity) {
		return getNumberOfSpaceActivities(spaceIdentity, baseActivity.getPostedTime(), true);
	}

	@Override
	public List<ExoSocialActivity> getOlderOnSpaceActivities(Identity spaceIdentity, ExoSocialActivity baseActivity, int limit) {
	  return getSpaceActivities(spaceIdentity, baseActivity.getPostedTime(), false, 0, limit);
	}

	@Override
	public int getNumberOfOlderOnSpaceActivities(Identity spaceIdentity, ExoSocialActivity baseActivity) {
	  return getNumberOfSpaceActivities(spaceIdentity, baseActivity.getPostedTime(), false);
	}

	@Override
	public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, Long sinceTime) {
	  return getNumberOfSpaceActivities(spaceIdentity, sinceTime, true);
	}

	@Override
	public int getNumberOfUpdatedOnActivityFeed(Identity owner,
			ActivityUpdateFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfUpdatedOnUserActivities(Identity owner,
			ActivityUpdateFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfUpdatedOnActivitiesOfConnections(Identity owner,
			ActivityUpdateFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfUpdatedOnUserSpacesActivities(Identity owner,
			ActivityUpdateFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfUpdatedOnSpaceActivities(Identity owner,
			ActivityUpdateFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfMultiUpdated(Identity owner,
			Map<String, Long> sinceTimes) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getNewerFeedActivities(Identity owner, Long sinceTime, int limit) {
		return getActivityFeed(owner, sinceTime, true, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getNewerUserActivities(Identity owner, Long sinceTime, int limit) {
		return getUserActivities(owner, sinceTime, true, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getNewerUserSpacesActivities(Identity owner, Long sinceTime, int limit) {
		return getUserSpacesActivities(owner, sinceTime, true, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getNewerActivitiesOfConnections(Identity owner, Long sinceTime, int limit) {
		return getActivitiesOfConnections(owner, sinceTime, true, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getNewerSpaceActivities(Identity owner, Long sinceTime, int limit) {
		return getSpaceActivities(owner, sinceTime, true, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getOlderFeedActivities(Identity owner, Long sinceTime, int limit) {
	  return getActivityFeed(owner, sinceTime, false, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getOlderUserActivities(Identity owner, Long sinceTime, int limit) {
	  return getUserActivities(owner, sinceTime, false, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getOlderUserSpacesActivities(Identity owner, Long sinceTime, int limit) {
	  return getUserSpacesActivities(owner, sinceTime, false, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getOlderActivitiesOfConnections(Identity owner, Long sinceTime, int limit) {
	  return getActivitiesOfConnections(owner, sinceTime, false, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getOlderSpaceActivities(Identity owner, Long sinceTime, int limit) {
	  return getSpaceActivities(owner, sinceTime, false, 0, limit);
	}

	@Override
	public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
		return getNumberOfActivityFeed(ownerIdentity, sinceTime, false);
	}

	@Override
	public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, Long sinceTime) {
		return getNumberOfUserActivities(ownerIdentity, sinceTime, false);
	}

	@Override
	public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, Long sinceTime) {
		return getNumberActivitiesOfConnections(ownerIdentity, sinceTime, false);
	}

	@Override
	public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, Long sinceTime) {
		return getNumberOfUserSpacesActivities(ownerIdentity, sinceTime, false);
	}

	@Override
	public int getNumberOfOlderOnSpaceActivities(Identity ownerIdentity, Long sinceTime) {
	  return getNumberOfSpaceActivities(ownerIdentity, sinceTime, false);
	}

	@Override
	public List<ExoSocialActivity> getNewerComments(ExoSocialActivity existingActivity, Long sinceTime, int limit) {
		return getComments(existingActivity, sinceTime, true, 0, limit);
	}

	@Override
	public List<ExoSocialActivity> getOlderComments(ExoSocialActivity existingActivity, Long sinceTime, int limit) {
	  return getComments(existingActivity, sinceTime, false, 0, limit);
	}

	@Override
	public int getNumberOfNewerComments(ExoSocialActivity existingActivity, Long sinceTime) {
		return getNumberOfComments(existingActivity, sinceTime, true);
	}

	@Override
	public int getNumberOfOlderComments(ExoSocialActivity existingActivity, Long sinceTime) {
	  return getNumberOfComments(existingActivity, sinceTime, false);
	}

  private ActivityStorage getStorage() {
    if (activityStorage == null) {
      activityStorage = (ActivityStorage) PortalContainer.getInstance().getComponentInstanceOfType(ActivityStorage.class);
    }
    
    return activityStorage;
  }
  
  public void setStorage(final ActivityStorage storage) {
    this.activityStorage = storage;
  }
  
  private static Comparator<ActivityProcessor> processorComparator() {
    return new Comparator<ActivityProcessor>() {

      public int compare(ActivityProcessor p1, ActivityProcessor p2) {
        if (p1 == null || p2 == null) {
          throw new IllegalArgumentException("Cannot compare null ActivityProcessor");
        }
        return p1.getPriority() - p2.getPriority();
      }
    };
  }
  
}
