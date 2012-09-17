$("#composerInput")
    .livequery(function(){
         $("#composerInput").tokenInput([{
                "id": "@root",
                "first_name": "Root",
                "last_name": "Root",
                "username": "root",
                "url": "http://localhost:8080/social-resources/skin/ShareImages/Avatar.gif"
            },
            {
                "id": "@john",
                "first_name": "John",
                "last_name": "Anthony",
                "username": "john",
                "url": "http://localhost:8080/social-resources/skin/ShareImages/Avatar.gif"
            },
            {
                "id": "@mary",
                "first_name": "Mary",
                "last_name": "Kelly",
                "username": "mary",
                "url": "http://localhost:8080/social-resources/skin/ShareImages/Avatar.gif"
            },
            {
                "id": "@demo",
                "first_name": "Demo",
                "last_name": "Gtn",
                "username": "demo",
                "url": "http://localhost:8080/social-resources/skin/ShareImages/Avatar.gif"
            }
         ], {
            theme: "facebook",
            completionTrigger: "@",
            propertyToSearch: "first_name",
            tokenDelimiter: " ",
            resultsFormatter: function(item){ return "<li>" + "<img src='" + item.url + "' title='" + item.first_name + " " + item.last_name + "' height='20px' width='20px' />" + "<div style=' display: inline-block; padding-left: 10px;'>" + item.first_name + " " + item.last_name + " (" + item.username + ")</div></li>" },
            tokenFormatter: function(item) { return "<li><p>" + item.first_name + " " + item.last_name + "</p></li>" }
        });
    });