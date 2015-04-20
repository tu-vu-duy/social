package org.exoplatform.social.core.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Created by bdechateauvieux on 3/25/15.
 */
@MappedSuperclass
public abstract class BaseActivity {
  @Column(length = 2000)
  private String title;
  @Column(length = 36)
  private String titleId;
  @Temporal(TemporalType.TIMESTAMP)
  private Date posted;
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdated;
  @Column(length = 36)
  private String posterId;
  @Column(length = 36)
  private String ownerId;
  @Column(length = 255)
  private String permaLink;
  @Column(length = 36)
  private String appId;
  @Column(length = 36)
  private String externalId;
  private Boolean locked;
  private Boolean hidden;

  @Deprecated
  @Column(length = 2000)
  private String body;
  @Deprecated
  @Column(length = 36)
  private String bodyId;
  @Deprecated
  private float priority;

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
  public Date getPosted() {
    return posted;
  }
  public void setPosted(Date posted) {
    this.posted = posted;
  }
  public Date getLastUpdated() {
    return lastUpdated;
  }
  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
  public String getPosterId() {
    return posterId;
  }
  public void setPosterId(String posterId) {
    this.posterId = posterId;
  }
  public String getOwnerId() {
    return ownerId;
  }
  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }
  public String getPermaLink() {
    return permaLink;
  }
  public void setPermaLink(String permaLink) {
    this.permaLink = permaLink;
  }
  public String getAppId() {
    return appId;
  }
  public void setAppId(String appId) {
    this.appId = appId;
  }
  public String getExternalId() {
    return externalId;
  }
  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }
  public Boolean getLocked() {
    return locked;
  }
  public void setLocked(Boolean locked) {
    this.locked = locked;
  }
  public Boolean getHidden() {
    return hidden;
  }
  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }
  public String getBody() {
    return body;
  }
  public void setBody(String body) {
    this.body = body;
  }
  public float getPriority() {
    return priority;
  }
  public void setPriority(float priority) {
    this.priority = priority;
  }
}
