/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.social.portlet.userNotification;


import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.View;
import juzu.impl.common.JSON;
import juzu.impl.request.Request;
import juzu.io.AppendableStream;
import juzu.request.RenderContext;
import juzu.request.RequestContext;
import juzu.template.Template;

import org.exoplatform.commons.api.notification.model.GroupProvider;
import org.exoplatform.commons.api.notification.model.PluginInfo;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.model.UserSetting.FREQUENCY;
import org.exoplatform.commons.api.notification.plugin.config.PluginConfig;
import org.exoplatform.commons.api.notification.service.setting.ChannelManager;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.commons.notification.impl.DigestDailyPlugin;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webui.application.WebuiRequestContext;


public class UserNotificationSetting {
  private static final Log LOG = ExoLogger.getLogger(UserNotificationSetting.class);

  @Inject
  @Path("index.gtmpl") Template index;

  @Inject
  @Path("UIChannelContainer.gtmpl") Template channelContainer;
  
  @Inject
  ResourceBundle bundle;  
  
  @Inject
  PluginSettingService pluginSettingService;

  @Inject
  ChannelManager channelManager;

  @Inject
  UserSettingService     userSettingService;
  
  private static final String DAILY                   = "Daily";

  private static final String WEEKLY                  = "Weekly";

  private static final String NEVER                   = "Never";

  private final static String SELECT_BOX_PREFIX       = "SelectBox";

  private Locale locale = Locale.ENGLISH;

  @View
  public void index(RenderContext renderContext) {
    //Redirect to the home's page if the feature is off
    if(CommonsUtils.isFeatureActive(NotificationUtils.FEATURE_NAME) == false) {
      redirectToHomePage();
      return;
    }
    if (renderContext != null) {
      locale = renderContext.getUserContext().getLocale();
    }
    if (bundle == null) {
      bundle = renderContext.getApplicationContext().resolveBundle(locale);
    }
    index.render(parameters());
  }

  @Ajax
  @POST
  @Resource
  public Response saveSetting(String pluginId, String channels, String digest) {
    JSON data = new JSON();
    try {
      UserSetting setting = userSettingService.get(getRemoteUser());
      // digest
      if (WEEKLY.equals(digest)) {
        setting.addPlugin(pluginId, FREQUENCY.WEEKLY);
      }else
      if (DAILY.equals(digest)) {
        setting.addPlugin(pluginId, FREQUENCY.DAILY);
      } else {
        setting.removePlugin(pluginId, FREQUENCY.WEEKLY);
        setting.removePlugin(pluginId, FREQUENCY.DAILY);
      }
      //channels
      Map<String, String> datas = parserParams(channels);
      for (String channelId : datas.keySet()) {
        if (Boolean.valueOf(datas.get(channelId))) {
          if (UserSetting.EMAIL_CHANNEL.equals(channelId)) {
            setting.addPlugin(pluginId, FREQUENCY.INSTANTLY);
          } else {
            setting.addChannelPlugin(channelId, pluginId);
          }
        } else {
          if (UserSetting.EMAIL_CHANNEL.equals(channelId)) {
            setting.removePlugin(pluginId, FREQUENCY.INSTANTLY);
          } else {
            setting.removeChannelPlugin(channelId, pluginId);
          }
        }
      }
      userSettingService.save(setting);
      data.set("ok", "true");
      data.set("status", "true");
      data.set("context", getChannelContainer(setting, pluginId));
    } catch (Exception e) {
      LOG.error("Failed to save user settings: ", e);
      data.set("ok", "false");
      data.set("status", e.toString());
    }
    return Response.ok(data.toString()).withMimeType("application/json");
  }

  private String getChannelContainer(UserSetting setting, String pluginId) {
    List<String> channels = channelManager.getChannelIds();
    Map<String, Object> parameters = new HashMap<String, Object>();

    //
    Context context = new Context(bundle);
    parameters.put("_ctx", context);
    //
    Map<String, String> options = buildOptions(context);
    Map<String, Boolean> channelStatus = new HashMap<String, Boolean>();
    Map<String, CheckBoxInput> channelCheckBoxList = new HashMap<String, CheckBoxInput>();
    parameters.put("emailSelectBox", null);
    parameters.put("emailCheckBox", null);
    for (String channelId : channels) {
      boolean isChannelActive = setting.isChannelActive(channelId);
      channelStatus.put(channelId, isChannelActive);
      if(UserSetting.EMAIL_CHANNEL.equals(channelId)) {
        parameters.put("emailSelectBox", new SelectBoxInput(channelId + pluginId, options, getValue(setting, pluginId), isChannelActive));
        parameters.put("emailCheckBox", new CheckBoxInput(channelId, pluginId, setting.isInInstantly(pluginId), isChannelActive));
      } else {
        channelCheckBoxList.put(channelId, new CheckBoxInput(channelId, pluginId, setting.isInChannel(channelId, pluginId), isChannelActive));
      }
    }
    parameters.put("channels", channels);
    parameters.put("channelCheckBoxList", channelCheckBoxList);
    parameters.put("channelStatus", channelStatus);
    parameters.put("emailChannel", UserSetting.EMAIL_CHANNEL);
    parameters.put("pluginId", pluginId);
    //
    StringBuilder buffer = new StringBuilder();
    channelContainer.renderTo(new AppendableStream(buffer), parameters);
    return buffer.toString();
  }

  @Ajax
  @Resource
  public Response saveActiveStatus(String channelId, String enable) {
    JSON data = new JSON();
    try {
      UserSetting setting = userSettingService.get(getRemoteUser());
      if (enable.equals("true") || enable.equals("false")) {
        if(enable.equals("true")) {
          setting.setChannelActive(channelId);
        } else {
          setting.removeChannelActive(channelId);
        }
        userSettingService.save(setting);
      }
      data.set("ok", "true");
      data.set("type", channelId);
      data.set("enable", enable);
      //
      StringBuilder buffer = new StringBuilder();
      index.renderTo(new AppendableStream(buffer), parameters());
      data.set("context", buffer.toString());
    } catch (Exception e) {
      data.set("ok", "false");
      data.set("status", e.toString());
      return new Response.Error("Exception in switching stat of provider " + channelId + ". " + e.toString());
    }
    return Response.ok(data.toString()).withMimeType("application/json");
  }
  
  @Ajax
  @Resource
  public Response resetSetting(String params) {
    try {
      UserSetting setting = UserSetting.getDefaultInstance();
      //
      userSettingService.save(setting.setUserId(getRemoteUser()));
    } catch (Exception e) {
      return new Response.Error("Error to save default notification user setting" + e.toString());
    }
    
    return index.ok(parameters()).withMimeType("text/html");
  }
  
  private String getRemoteUser() {
    RequestContext requestContext = Request.getCurrent().getContext();
    return requestContext.getSecurityContext().getRemoteUser();
  }

  private Map<String, Object> parameters() {
    
    Map<String, Object> parameters = new HashMap<String, Object>();

    //
    Context context = new Context(bundle);
    parameters.put("_ctx", context);

    UserSetting setting = userSettingService.get(getRemoteUser());
    //
    List<GroupProvider> groups = pluginSettingService.getGroupPlugins();
    parameters.put("groups", groups);
    //
    boolean hasActivePlugin = false;
    Map<String, SelectBoxInput> emailSelectBoxList = new HashMap<String, SelectBoxInput>();
    Map<String, CheckBoxInput> emailCheckBoxList = new HashMap<String, CheckBoxInput>();
    //
    Map<String, CheckBoxInput> channelCheckBoxList = new HashMap<String, CheckBoxInput>();
    Map<String, String> options = buildOptions(context);
    List<String> channels = channelManager.getChannelIds();
    for (GroupProvider groupProvider : pluginSettingService.getGroupPlugins()) {
      for (PluginInfo info : groupProvider.getPluginInfos()) {
        String pluginId = info.getType();
        for (String channelId : channels) {
          if(info.isChannelActive(channelId)) {
            if(UserSetting.EMAIL_CHANNEL.equals(channelId)) {
              emailSelectBoxList.put(pluginId, new SelectBoxInput(channelId + pluginId, options, getValue(setting, pluginId), setting.isChannelActive(channelId)));
              emailCheckBoxList.put(pluginId, new CheckBoxInput(channelId, pluginId, setting.isInInstantly(pluginId), setting.isChannelActive(channelId)));
              hasActivePlugin = true;
            } else {
              channelCheckBoxList.put(channelId + pluginId, new CheckBoxInput(channelId, pluginId, setting.isInChannel(channelId, pluginId), setting.isChannelActive(channelId)));
              hasActivePlugin = true;
            }
          }
        }
      }
    }
    parameters.put("hasActivePlugin", hasActivePlugin);
    parameters.put("emailCheckBoxList", emailCheckBoxList);
    parameters.put("emailSelectBoxList", emailSelectBoxList);
    //
    parameters.put("channelCheckBoxList", channelCheckBoxList);
    //
    Map<String, Boolean> channelStatus = new HashMap<String, Boolean>();
    for (String channelId : channels) {
      channelStatus.put(channelId, setting.isChannelActive(channelId));
    }
    parameters.put("channelStatus", channelStatus);
    parameters.put("channels", channels);
    parameters.put("emailChannel", UserSetting.EMAIL_CHANNEL);

    return parameters;
  }
  
  private String makeSelectBoxId(String pluginId) {
    return new StringBuffer(pluginId).append(SELECT_BOX_PREFIX).toString();
  }
  
  private String getValue(UserSetting setting, String pluginId) {
    if (setting.isInWeekly(pluginId)) {
      return WEEKLY;
    } else if (setting.isInDaily(pluginId)) {
      return DAILY;
    } else {
      return NEVER;
    }
  }
  
  private Map<String, String> buildOptions(Context context) {
    Map<String, String> options = new HashMap<String, String>();
    options.put(SelectBoxInput.DAILY, context.appRes("UINotification.label.Daily"));
    options.put(SelectBoxInput.WEEKLY, context.appRes("UINotification.label.Weekly"));
    options.put(SelectBoxInput.NEVER, context.appRes("UINotification.label.Never"));
    return options;
  }
  
  private Map<String, String> parserParams(String params) {
    Map<String, String> datas = new HashMap<String, String>();
    String[] arrays = params.split("&");
    for (int i = 0; i < arrays.length; i++) {
      String[] data = arrays[i].split("=");
      datas.put(data[0], data[1]);
    }
    return datas;
  }
  
  private void redirectToHomePage() {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    HttpServletRequest currentServletRequest = portalRequestContext.getRequest();
    StringBuilder sb = new StringBuilder();
    sb.append(currentServletRequest.getScheme()).append("://")
      .append(currentServletRequest.getServerName())
      .append(":").append(currentServletRequest.getServerPort())
      .append("/").append(PortalContainer.getCurrentPortalContainerName())
      .append("/").append(portalRequestContext.getPortalOwner());
    
    WebuiRequestContext ctx = WebuiRequestContext.getCurrentInstance();
    JavascriptManager jsManager = ctx.getJavascriptManager();
    jsManager.addJavascript("try { window.location.href='" + sb.toString() + "' } catch(e) {" +
            "window.location.href('" + sb.toString() + "') }");
  }

  public class Context {
    ResourceBundle rs;

    public Context(ResourceBundle rs) {
      this.rs = rs;
    }

    public String appRes(String key) {
      try {
        return rs.getString(key).replaceAll("'", "&#39;").replaceAll("\"", "&#34;");
      } catch (java.util.MissingResourceException e) {
        LOG.warn("Can't find resource for bundle key " + key);
      } catch (Exception e) {
        LOG.debug("Error when get resource bundle key " + key, e);
      }
      return key;
    }
    
    private String getBundlePath(String id) {
      PluginConfig pluginConfig = pluginSettingService.getPluginConfig(id);
      if (pluginConfig != null) {
        return pluginConfig.getBundlePath();
      }
      //
      if (GroupProvider.defaultGroupIds.contains(id)) {
        return pluginSettingService.getPluginConfig(DigestDailyPlugin.ID).getBundlePath();
      }
      //
      List<GroupProvider> groups = pluginSettingService.getGroupPlugins();
      for (GroupProvider groupProvider : groups) {
        if (groupProvider.getGroupId().equals(id)) {
          return groupProvider.getPluginInfos().get(0).getBundlePath();
        }
      }
      
      return "";
    }

    public String pluginRes(String key, String id) {
      String path = getBundlePath(id);
      return TemplateUtils.getResourceBundle(key, locale, path);
    }
  }
  
  public class CheckBoxInput {
    private final String  pluginId;
    private final String  channelId;
    private final boolean isChecked;
    private final boolean isActive;

    public CheckBoxInput(String channelId, String pluginId, boolean isChecked, boolean isActive) {
      this.pluginId = pluginId;
      this.channelId = channelId;
      this.isActive = isActive;
      this.isChecked = isChecked;
    }
    public boolean isChecked() {
      return isChecked;
    }
    public boolean isActive() {
      return isActive;
    }
    public String render(String label) {
      String name = channelId + pluginId;
      StringBuffer buffer = new StringBuffer("<label class=\"uiCheckbox\" for=\"").append(name).append("\">");
      buffer.append("<input type=\"checkbox\" class=\"checkbox\" ")
            .append((isChecked == true) ? "checked=\"checked\" " : "")
            .append((isActive == false) ? "disabled " : "")
            .append("name=\"").append(name).append("\" id=\"").append(name).append("\" ")
            .append("data-channel=\"").append(channelId).append("\" />")
            .append("<span>").append(label).append("</span></label>");
      return buffer.toString();
    }
  }
  
  public class SelectBoxInput {
    public static final String NEVER = "Never";
    public static final String DAILY = "Daily";
    public static final String WEEKLY = "Weekly";
    private final String name;
    private final Map<String, String> options;
    private final String selectedId;
    private final boolean isActive;
    public SelectBoxInput(String name, Map<String, String> options, String selectedId, boolean isActive) {
      this.name = name;
      this.options = options;
      this.selectedId = selectedId;
      this.isActive = isActive;
    }
    public String getName() {
      return name;
    }
    public String getSelectedId() {
      return selectedId;
    }
    public String getValueLabel() {
      if (options.containsKey(selectedId)) {
        return options.get(selectedId);
      }
      return selectedId;
    }
    public boolean isActive() {
      return isActive;
    }
    public boolean isActiveSend() {
      return !NEVER.equals(selectedId);
    }
    public String render() {
      String selected = "";
      String id = makeSelectBoxId(name);
      StringBuffer buffer = new StringBuffer("<span class=\"uiSelectbox\">");
      buffer.append("<select name=\"").append(id).append("\" id=\"").append(id).append("\"").append((isActive == false) ? " disabled " : "").append(" class=\"selectbox input-small\">");
      for (String key : options.keySet()) {
        selected = (key.equals(selectedId) == true) ? " selected=\"selected\" " : "";
        buffer.append("<option value=\"").append(key).append("\" class=\"option\"").append(selected).append(">").append(options.get(key)).append("</option>");
      }
      buffer.append("</select>").append("</span>");
      return buffer.toString();
    }
  }
     
}
