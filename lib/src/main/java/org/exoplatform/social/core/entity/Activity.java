package org.exoplatform.social.core.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * Created by bdechateauvieux on 3/24/15.
 */
@Entity
@Table(name = "SOC_ACTIVITIES")
@EntityListeners({Activity.ActivityEntityListener.class})
@NamedQueries({
  @NamedQuery(
    name = "getActivitiesByLikerId",
    query = "select a from Activity a join a.likerIds likers where likers = :likerId"
  ),
  @NamedQuery(
    name = "getUserActivities",
    query = "select a from Activity a where a.ownerId = :ownerId"
  )
})
public class Activity extends BaseActivity {
    @Id
//    @GeneratedValue
//    @GeneratedValue(generator="system-uuid")
//    @GenericGenerator(name="system-uuid", strategy = "uuid2")
    @Column(name="ACTIVITY_ID", length=36)
//    private Long id;
    private String id;

    private String type;

    @ElementCollection
    @CollectionTable(
            name = "SOC_ACTIVITY_LIKERS",
            joinColumns=@JoinColumn(name = "ACTIVITY_ID")
    )
    @Column(name="LIKER_ID")
    private Set<String> likerIds = new HashSet<String>();

    @ElementCollection
    @JoinTable(
            name = "SOC_ACTIVITY_TEMPLATE_PARAMS",
            joinColumns=@JoinColumn(name = "ACTIVITY_ID")
    )
    @MapKeyColumn(name="TEMPLATE_PARAM_KEY")
    @Column(name="TEMPLATE_PARAM_VALUE")
    private Map<String, String> templateParams;

    @OneToMany
    @JoinTable(
            name = "SOC_ACTIVITY_COMMENTS",
            joinColumns = @JoinColumn(name = "ACTIVITY_ID"),
            inverseJoinColumns = @JoinColumn(name = "COMMENT_ID")
    )
    private List<Comment> comments;

    @OneToOne
    private ActivityStreamEntity activityStream;

    public void setId(String id) {
        this.id = id;
    }

    public void addLiker(String likerId) {
        this.likerIds.add(likerId);
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public Set<String> getLikerIds() {
      return likerIds;
    }

    public void setLikerIds(Set<String> likerIds) {
      this.likerIds = likerIds;
    }

    public Map<String, String> getTemplateParams() {
      return templateParams;
    }

    public void setTemplateParams(Map<String, String> templateParams) {
      this.templateParams = templateParams;
    }

    public List<Comment> getComments() {
      return comments;
    }

    public void setComments(List<Comment> comments) {
      this.comments = comments;
    }

    public String getId() {
      return id;
    }


  public static class ActivityEntityListener {
    @PrePersist
    public void onPrePersist(Activity activity) {
      activity.setId(UUID.randomUUID().toString());
    }
  }

  public void setActivityStream(ActivityStreamEntity stream) {
    this.activityStream = stream;
  }

  public ActivityStreamEntity getActivityStream() {
    return this.activityStream;
  }
}
