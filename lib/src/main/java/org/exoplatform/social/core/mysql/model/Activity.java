package org.exoplatform.social.core.mysql.model;

import java.util.List;

import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.exoplatform.social.core.activity.model.ActivityStream;

public abstract class Activity extends ActivityImpl {

  protected List<MediaItem> mediaItems;
  
  protected ActivityStream activityStream;
  public Activity() {
  }
  

  public List<MediaItem> getMediaItems() {
    return mediaItems;
  }

  public void setMediaItems(List<MediaItem> mediaItems) {
    this.mediaItems = mediaItems;
  }

}
