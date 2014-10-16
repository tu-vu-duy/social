(function(sUtils, $) {

  var SpaceManager = {
    portletId : "UISpaceManagementPortlet",
    isRestricted : true,
    init : function(portletId, isRestricted, iphoneLabel_) {
      var iphoneLabel = {YES:'YES', NO:'NO'};
      iphoneLabel = $.extend(true, {}, iphoneLabel, ((iphoneLabel_ == null) ? {} : iphoneLabel_));
      //
      SpaceManager.isRestricted = isRestricted;
      SpaceManager.portletId = '#' + portletId;
      //
      var portlet = $(SpaceManager.portletId);
      portlet.find('input:checkbox.yesno').iphoneStyle({
        checkedLabel: iphoneLabel.YES,
        uncheckedLabel: iphoneLabel.NO,
        onChange : SpaceManager.iphoneSwitch
      });
      portlet.find('#AddGroupButton').on('click', SpaceManager.openGroupSelector);
      
    },
    iphoneSwitch : function(elm) {
      var input = $(elm);
      if(input.data('error') === 'true') {
        input.data('error', '');
        return;
      }
      //
      var ajax = $(SpaceManager.portletId).jzAjax({
        url : "SpaceManagement.saveRestricted()",
        data : {
          "restricted" : (input[0].checked)
        },
        success : function(data) {
          var checkbox = $(SpaceManager.portletId).find('input:checkbox.yesno');
          if(data.ok === 'true') {
            checkbox.data('check', data.restricted);
            if(data.restricted === 'true') {
              $(SpaceManager.portletId).find('div.uilist-groups:first').removeClass('hidden');
            } else {
              $(SpaceManager.portletId).find('div.uilist-groups:first').removeClass('show').addClass('hidden');
            }
          } else {
            sUtils.feedbackMessageInline(SpaceManager.portletId, "Change the status error");
            checkbox.data('error', 'true').click();
          }
        }
      });
      if(ajax) {
        ajax.fail(function(jqXHR, textStatus) {
          alert( "Request failed: " + textStatus + ". "+jqXHR);
        });
      }
    },
    openGroupSelector : function(evt) {
      var groupId = $(this).data('id');
      var hashChild = $(this).data('child');
      groupId = (groupId) ? groupId : '/';
      hashChild = (hashChild) ? hashChild : 'false';
      //
      $(SpaceManager.portletId).jzAjax({
        url : "SpaceManagement.openGroupSelector()",
        data : {
          "groupId" : (groupId) ? groupId : '/',
          "hashChild" : (hashChild) ? hashChild : 'false'
        },
        success : function(data) {
          if(data && data.length > 0) {
            sUtils.setCookies('currentConfirm', 'AddGroupButton', 300);
            var popup = $('<div></div>').html(data);
            var pwindow = popup.find('div.UIPopupWindow:first');
            pwindow.find('.uiIconClose:first').on('click', sUtils.PopupConfirmation.hiden);
            pwindow.find('.parentContainer:first').find('> li.node').on('click', SpaceManager.showChild);
            //
            sUtils.PopupConfirmation.show(pwindow.css('min-width', '600px').attr('id', 'UISocialPopupConfirmation'));
          } else {
            sUtils.feedbackMessageInline(SpaceManager.portletId, "Carn not open the Group selector");
          }
        }
      });
    },
    showChild : function() {
      var groupId = $(this).data('id');
      var hashChild = $(this).data('child');
      groupId = (groupId) ? groupId : '/';
      hashChild = (hashChild) ? hashChild : 'false';
      console.log('groupId ' + groupId + ' hashChild ' + hashChild);
      //
      
    },
    addGroup : function(selectedElId, groupName) {
      
    },
    initTree : function() {
  
    }
  };
  return SpaceManager;
})(socialUtil, jq);
