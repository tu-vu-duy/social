package org.exoplatform.social.core.entity;

import javax.persistence.*;

/**
 * Created by bdechateauvieux on 3/26/15.
 */
@Entity
@Table(name = "SOC_STREAM_ITEMS")
public class StreamItem {
  @Id
  @GeneratedValue
  @Column(name = "STREAM_ITEM_ID")
  private Long       id;

  @OneToOne
  @JoinColumn(name = "ACTIVITY_ID")
  private Activity   activity;

  @Column(length = 36)
  private String     viewerId;

  @Enumerated
  private StreamType viewerStreamType;

  public void setActivity(Activity activity) {
    this.activity = activity;
  }

  public Activity getActivity() {
    return activity;
  }
}
