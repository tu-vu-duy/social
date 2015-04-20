package org.exoplatform.social.core.mysql.model;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.social.core.activity.model.ActivityStreamImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;

@Entity
@Table(name="activity")
public class ActivityEntity extends Activity implements ExoSocialActivity {
  @Id
  private String id;
  
  private String appId;
  private String body;
  private String bodyId;
  private String externalId;
  private Date updated;
  private Long postedTime;
  private Float priority;
  private String title;
  private String titleId;
  private String url;
  private String userId;
  private boolean isAComment = false;
  private String type;
  private boolean isHiddenActivity = false;
  private boolean isLockedActivity = false;
  private String name;
  private String summary;
  private transient String permaLink;
  private String parentId;
  private String posterId;

  private String[] likeIdentityIds;
  private String[] replyToId;
  private String[] mentionedIds;
  private String[] commentedIds;

//  private Map<String, String> templateParams;
  @Column(name="templateParams")
  private String[] templateParameters;
  
  public ActivityEntity() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getBodyId() {
    return bodyId;
  }

  public void setBodyId(String bodyId) {
    this.bodyId = bodyId;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public Date getUpdated() {
    return (updated == null) ? new Date() : updated;
  }

  public void setUpdated(Date updated) {
    this.updated = updated;
  }

  public Long getPostedTime() {
    return (postedTime == null) ? 0l : postedTime;
  }

  public void setPostedTime(Long postedTime) {
    this.postedTime = postedTime;
  }

  public Float getPriority() {
    return priority;
  }

  public void setPriority(Float priority) {
    this.priority = priority;
  }

  public String[] getTemplateParameters() {
    return templateParameters;
  }

  public void setTemplateParameters(String[] templateParams) {
    this.templateParameters = templateParams;
  }

  public Map<String, String> getTemplateParams() {
    Map<String, String> template = new HashMap<String, String>();
    if (templateParameters != null) {
      for (int i = 0; i < templateParameters.length; i++) {
        String[] values = templateParameters[i].split("||");
        template.put(values[0], values[1]);
      }
    }
    return template;
  }

  public void setTemplateParams(Map<String, String> templateParams) {
    if (templateParams != null) {
      this.templateParameters = new String[] {};
      int i = 0;
      for (String key : templateParams.keySet()) {
        this.templateParameters[i] = key + "||" + templateParams.get(key);
      }
    }
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitleId() {
    return titleId;
  }

  public void setTitleId(String titleId) {
    this.titleId = titleId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public final void setActivityStream(final org.exoplatform.social.core.activity.model.ActivityStream providedAS) {
    activityStream = providedAS;
  }

  public final boolean isComment() {
    return isAComment;
  }

  public final void isComment(final boolean isCommentOrNot) {
    isAComment = isCommentOrNot;
  }

  public final String getType() {
    return type;
  }

  public final void setType(final String activityType) {
    this.type = activityType;
  }

  public String[] getReplyToId() {
    return replyToId;
  }

  public final void setReplyToId(String[] replyToIdentityId) {
    this.replyToId = replyToIdentityId;
  }

  public final boolean isHidden() {
    return isHiddenActivity;
  }

  public final void isHidden(final boolean isHiddenOrNot) {
    isHiddenActivity = isHiddenOrNot;
  }

  public final boolean isLocked() {
    return isLockedActivity;
  }

  public final void isLocked(final boolean isLockedOrNot) {
    isLockedActivity = isLockedOrNot;
  }

  public final String[] getLikeIdentityIds() {
    if (likeIdentityIds != null) {
      return Arrays.copyOf(likeIdentityIds, likeIdentityIds.length);
    }
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  @Override
  public int getNumberOfLikes() {
    return likeIdentityIds == null ? 0 : likeIdentityIds.length;
  }

  public final void setLikeIdentityIds(final String[] identityIds) {
    likeIdentityIds = Arrays.copyOf(identityIds, identityIds.length);
  }

  public final String getStreamOwner() {
    return getActivityStream().getPrettyId();
  }

  public final void setStreamOwner(final String activitySO) {
    getActivityStream().setPrettyId(activitySO);
  }

  public final String getStreamId() {
    return getActivityStream().getId();
  }

  public final void setStreamId(final String sId) {
    getActivityStream().setId(sId);
  }

  public final String getName() {
    return name;
  }

  public final void setName(final String activityName) {
    name = activityName;
  }

  public final String getSummary() {
    return summary;
  }

  public final void setSummary(final String activitySummary) {
    summary = activitySummary;
  }

  public final String getPermaLink() {
    return permaLink;
  }

  public final void setPermanLink(final String activityPermaLink) {
    permaLink = activityPermaLink;
  }

  public final String getStreamFaviconUrl() {
    return getActivityStream().getFaviconUrl();
  }

  public final String getStreamSourceUrl() {
    return getActivityStream().getPermaLink();
  }

  public final String getStreamTitle() {
    return getActivityStream().getTitle();
  }

  public final String getStreamUrl() {
    return getActivityStream().getPermaLink();
  }

  public final String[] getMentionedIds() {
    if (mentionedIds != null) {
      return Arrays.copyOf(mentionedIds, mentionedIds.length);
    }
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  public final void setMentionedIds(final String[] identityIds) {
    mentionedIds = Arrays.copyOf(identityIds, identityIds.length);
  }

  public final String[] getCommentedIds() {
    if (commentedIds != null) {
      return Arrays.copyOf(commentedIds, commentedIds.length);
    }
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  public final void setCommentedIds(final String[] identityIds) {
    commentedIds = Arrays.copyOf(identityIds, identityIds.length);
  }

  public void setUpdated(Long updated) {
    if (updated != null) {
      setUpdated(new Date(updated));
    } else {
      setUpdated(getPostedTime());
    }
  }

  @Override
  public org.exoplatform.social.core.activity.model.ActivityStream getActivityStream() {
    if (activityStream == null) {
      activityStream = new ActivityStreamImpl();
    }
    return activityStream;
  }

  @Override
  public String getPosterId() {
    return posterId;
  }

  @Override
  public void setPosterId(String posterId) {
    this.posterId = posterId;
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  @Override
  public void setParentId(String parentId) {
    this.parentId = parentId;
  }
  
  @Override
  public String toString() {
    return "ActivityEntity[id = " + getId() + ",title=" + getTitle() + ",lastModified= " + getUpdated().getTime() + " ]";
  }
  
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ExoSocialActivityImpl)) {
      return false;
    }

    ExoSocialActivityImpl that = (ExoSocialActivityImpl) o;

    if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (getId() != null ? getId().hashCode() : 0);
    return result;
  }
}
