(function(sUtils, $) {

  var SpaceManager = {
    portletId : "UISpaceManagementPortlet",
    isRestricted : true,
    iphoneLabel : {YES:'YES', NO:'NO'},
    init : function(portletId, isRestricted, iphoneLabel_) {
      SpaceManager.iphoneLabel = $.extend(true, {}, SpaceManager.iphoneLabel, ((iphoneLabel_ == null) ? {} : iphoneLabel_));
      //
      SpaceManager.isRestricted = isRestricted || SpaceManager.isRestricted;
      SpaceManager.portletId = '#' + portletId;
      //
      SpaceManager.registerAction();
    },
    registerAction : function() {
      var portlet = $(SpaceManager.portletId);
      portlet.find('input:checkbox.yesno').iphoneStyle({
        checkedLabel: SpaceManager.iphoneLabel.YES,
        uncheckedLabel: SpaceManager.iphoneLabel.NO,
        onChange : SpaceManager.iphoneSwitch
      });
      portlet.find('#AddGroupButton').on('click', SpaceManager.openGroupSelector);
      //removeMembership
      portlet.find('#GroupList').find('a.removeMembership').on('click', SpaceManager.removeMembership);
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
              SpaceManager.isRestricted = true;
              $(SpaceManager.portletId).find('div.uilist-groups:first').removeClass('hidden');
            } else {
              SpaceManager.isRestricted = false;
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
      if($(this).parents('.uilist-groups:first').hasClass('hidden')
          || SpaceManager.isRestricted == false) {
        sUtils.setCookies('currentConfirm', '', -300);
        return;
      }
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
            pwindow.find('.parentContainer:first').find('> li.node').find('a:first').on('click', SpaceManager.showChild)
                   .find('ul.childrenContainer').find('> li.node').find('a:first').on('click', SpaceManager.selectChild);
            //saveMembership
            pwindow.find('#MembershipTypes').find('a.membershipType').on('click', SpaceManager.saveMembership);
      
            sUtils.PopupConfirmation.show(pwindow.css('min-width', '600px').attr('id', 'UISocialPopupConfirmation'));
          } else {
            sUtils.feedbackMessageInline(SpaceManager.portletId, "Carn not open the Group selector");
          }
        }
      });
    },
    showChild : function() {
      var current = $(this).parent();
      var groupId = current.data('id');
      var hashChild = current.data('child');
      groupId = (groupId) ? groupId : '/';
      hashChild = (hashChild) ? hashChild : 'false';
      console.log('groupId ' + groupId + ' hashChild ' + hashChild);
      //
      if(hashChild) {
        current.parents('ul.parentContainer:first').find('.childrenContainer').hide();
        current.parents('ul.parentContainer:first').find('.expandIcon').removeClass('expandIcon').removeClass('nodeSelected');
        current.find('> a.uiIconNode:first').addClass('expandIcon nodeSelected');
        current.find('.childrenContainer:first').show();
        //
        $(SpaceManager.portletId).jzAjax({
          url : "SpaceManagement.setSelectedGroup()",
          data : {
            "groupId" : (groupId) ? groupId : '/',
            "hashChild" : (hashChild) ? hashChild : 'false'
          },
          success : function(data) {
            if(typeof data === 'object') {
              console.log(data);
            } else {
              var content = $('<div></div>').html(data);
              current.parents('div.UIPopupWindow:first')
                     .find('#uiBreadcrumb').html(content.html())
            }
          }
        });
      }
    },
    selectChild : function() {
    
    },
    saveMembership : function() {
      var jelm = $(this);
      var membershipType = jelm.data('membership-type');
      console.log('save membershipType: ' + membershipType);
      //
      $(SpaceManager.portletId).jzAjax({
        url : "SpaceManagement.saveRestrictedMembership()",
        data : {
          "membershipType" : membershipType
        },
        success : function(data) {
          jelm.parents('div.UIPopupWindow:first').find('.uiIconClose:first').trigger('click');
          SpaceManager.loadPortlet(data);
        }
      });
    },
    removeMembership : function(evt) {
      var membership = $(this).data('membership');
      //
      $(SpaceManager.portletId).jzAjax({
        url : "SpaceManagement.removeRestrictedMembership()",
        data : {
          "membership" : membership
        },
        success : function(data) {
          SpaceManager.loadPortlet(data);
        }
      });
    },
    updatePortlet : function() {
      $(SpaceManager.portletId).jzAjax({
        url : "SpaceManagement.reloadPortlet()",
        success : function(data) {
          SpaceManager.loadPortlet(data);
        }
      });
    },
    loadPortlet :function(data) {
      var content = $('<div></div>').html(data);
      $(SpaceManager.portletId).html(content.find(SpaceManager.portletId).html())
      SpaceManager.registerAction();
    }
  };
  return SpaceManager;
})(socialUtil, jq);