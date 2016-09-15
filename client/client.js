var curUser = {
    user: "",
    pass: "",
    token: "",
    domain: ""
};

var chatContexts = [];

var curChannel = {
    toList: [],
};

Array.prototype.compare = function(testArr) {
    if (this.length != testArr.length) return false;
    for (var i = 0; i < testArr.length; i++) {
        if (this[i].compare) { //To test values in nested arrays
            if (!this[i].compare(testArr[i])) return false;
        }
        else if (this[i] !== testArr[i]) return false;
    }
    return true;
}

function initPage()
{
    $("#hide-left-sidebar").click(function() {
        $("#left-sidebar").css("display", "none");
        $("#hidden-left-sidebar").css("display", "block");
    });

    $("#hidden-left-sidebar").click(function() {
        $("#left-sidebar").css("display", "flex");
        $("#hidden-left-sidebar").css("display", "none");
    });

    $("#connect-button").click(function() {
        var curView = $("#input-box-connect").css("display");
        if(curView == "inline-block") {
            $("#connect-button").css("backgroundColor", "#b48c64");
            $("#input-box-connect").css("display", "none");
        }
        if(curView == "none") {
            $("#connect-button").css("backgroundColor", "#B46D64");
            $("#input-box-connect").css("display", "inline-block");
            
            $("#new-chat-button").css("backgroundColor", "#b48c64");
            $("#input-box-new-chat").css("display", "none");
        }
    });

    $("#new-chat-button").click(function() {
        var curView = $("#input-box-new-chat").css("display");
        if(curView == "inline-block") {
            $("#new-chat-button").css("backgroundColor", "#b48c64");
            $("#input-box-new-chat").css("display", "none");
        }
        if(curView == "none") {
            $("#new-chat-button").css("backgroundColor", "#B46D64");
            $("#input-box-new-chat").css("display", "inline-block");
            
            $("#connect-button").css("backgroundColor", "#b48c64");
            $("#input-box-connect").css("display", "none");        
        }
    });

    $("#create-button").click(function() {
        var curView = $("#input-box-create").css("display");
        if(curView == "inline-block") {
            $("#create-button").css("backgroundColor", "#b48c64");
            $("#input-box-create").css("display", "none");
        }
        if(curView == "none") {
            $("#create-button").css("backgroundColor", "#B46D64");
            $("#input-box-create").css("display", "inline-block");       
        }
    });

    $("#disconnect-button").click(function() {
        //close websocket i guess?
        //clear session data like user and channels?
        //add checks to stop sending and such if not logged in
        $("#chat-title span").text("");
        $("#status-bar span").text("status: disconnected.  click [create] to make account or [connect] to login");
    });

    $("#newChatForm").submit(function(e) {
        e.preventDefault();
        ////// CREATE CHANNEL
        var toListAsString = $("#newChatUsers").val();
        var channelName = $("#newChatName").val();
        if(toListAsString != "") curChannel.toList = toListAsString.split(",");
        curChannel.toList.push(curUser.user + "@" + curUser.domain);
        curChannel.name = channelName;
        $("#chat-title span").text(channelName + ":[" + curChannel.toList.toString() + "]");
        $("#status-bar span").text("status: chatting on "+channelName+" with [" + curChannel.toList.toString() + "]");

        addChat(channelName, curChannel.toList);
        ///// END CREATE CHANNEL
        $("#new-chat-button").css("backgroundColor", "#b48c64");
        $("#input-box-new-chat").css("display", "none");
        $("#newChatName").val("");
        $("#newChatUsers").val("");
    });

    $("#createForm").submit(function(e) {
        e.preventDefault();
        var fullUsername = $("#createUser").val();
        var password = $("#createPass").val();
        var cDomain = $("#createProvider").val();

        if(fullUsername == "" || password == "" || cDomain == "")
        {
            $("#createUser").val("");
            $("#createPass").val("");
            $("#createProvider").val("");
            return;
        }         

        $("#createUser").val("");
        $("#createPass").val("");
        $("#createProvider").val("");

        $("#create-button").css("backgroundColor", "#b48c64");
        $("#input-box-create").css("display", "none");

        var package = '{"username":"'+fullUsername+'","password":"'+password+'"}';
        $.support.cors = true;
        $.ajax({
            url: "https://" + cDomain + "/users",
            type: "POST",
            data:package,
            contentType:"application/json; charset=utf-8",
            success: function(data) {
                $("#status-bar span").text("status: account created. click [connect] to login");
            },
            error: function(data) {
                $("#status-bar span").text("status: account creation might have failed (try to connect?)");
            },
            beforeSend: function() {
                $("#status-bar span").text("status: creating account...please wait");
            }
        });
    });

    $("#connectForm").submit(function(e){
        e.preventDefault();
        var fullUsername = $("#connectUser").val();
        var password = $("#connectPass").val();
        var cDomainAndPort = $("#connectProvider").val();

        if(fullUsername == "" || password == "" || cDomainAndPort == "")
        {
            $("#connectUser").val("");
            $("#connectPass").val("");
            $("#connectProvider").val("");
            return;
        }        

        $("#connectUser").val("");
        $("#connectPass").val("");
        $("#connectProvider").val("");

        $("#connect-button").css("backgroundColor", "#b48c64");
        $("#input-box-connect").css("display", "none");

        var indexOfColon = cDomainAndPort.indexOf(":");
        var cDomain = cDomainAndPort.substring(0, indexOfColon);
        var portNum = cDomainAndPort.substring(indexOfColon+1);

        loginToProvider(fullUsername, password, cDomain, portNum);
    });

    $("#message-text").keydown(function(evt)
    {
        if(evt.which == 13)
        {
            var txt = $("#message-text");

            if(evt.shiftKey)
            {
                var curHeight = txt.height();
                txt.css("height", (curHeight + 15).toString() + "px");
                $("#message-box").css("height", (curHeight + 23).toString() + "px");
            }
            else
            {
                evt.preventDefault();
                txt.css("height", "16px");
                $("#message-box").css("height", "24px");
                //submit the message now
                submitMessage();
            }
        }
        else if(evt.which == 8)
        {
            var txt = $("#message-text").val();
            var count = (txt.match(/\n/g) || []).length;
        }
    });
}

function getCurrentChatContextIndex(toList)
{

}

function addChat(name, toarray)
{
    $('#chat-tabs').append("<div class='chat-tab'><span>"+ name +"</span></div>");

    $("#chat-tabs").scrollTop($("#chat-tabs")[0].scrollHeight);

    $(".chat-tab").click(function () {
        $("#chat-tabs").children().each(function() {
            $(this).css("backgroundColor", "#0f0f0f");
            $(this).css("color", "#b48c64");
        });

        $(this).css("backgroundColor", "#b48c64");
        $(this).css("color", "#0f0f0f");
        
        var chatName = $(this).text();
        var chatToList = getChatListFromName(chatName);

        curChannel.toList = chatToList.split(",");
        $("#chat-title span").text(chatName + ":[" + curChannel.toList.toString() + "]");
        $("#status-bar span").text("status: chatting on "+chatName+" with [" + curChannel.toList.toString() + "]");
    });

    var newChat = {};
    newChat.toList = toarray;
    newChat.name = name;
    newChat.messages = [];

    chatContexts.push(newChat);
}

function getChatListFromName(search)
{
    for(var i = 0; i < chatContexts.length; ++i)
    {
        if(chatContexts[i].name == search)
        {
            return chatContexts[i].toList.toString();
        }
    }
    return "";
}

function loginToProvider(user, pass, domainIn, portIn)
{
    curUser.user = user;
    curUser.domain = domainIn;
    curUser.pass = pass;
    curUser.port = portIn;

    var package = {
        grant_type: "password",
        username: user,
        password: pass
    };
    $.support.cors = true;

    $.ajax({
        url: "https://" + curUser.domain + ":" + curUser.port + "/token",
        type: "POST",
        data:package,
        contentType:"application/x-www-form-urlencoded",
        success:function(data) {
            $("#status-bar span").text("status: logged in. click [new chat] to set chat list");
            curUser.token = data.access_token;
            var messageSocket = new WebSocket("wss://"+ curUser.domain + ":" + curUser.port + "/messageStream");
            messageSocket.onmessage= function(event) {
                try {
                    var decoded = JSON.parse(event.data);
                    addChatMessage(decoded.from, decoded.message);    
                }
                catch(err) {
                }
            }
            messageSocket.onopen = function(event) {
                messageSocket.send(curUser.token);
            }
            messageSocket.onclose = function(event) {
                //we should recconnect here is the ws was dropped by the server
                //else give an error
                console.log("websocket closed (" + event.code + ") " + event.reason);
            }
        },
        error: function(data) {
            $("#status-bar span").text("status: login failed");
        },
        beforeSend: function() {
            $("#status-bar span").text("status: logging in...please wait");
        }
    });
}

function addChatMessage(senderFull, msg)
{
    //save message to context message list


    var indexOfAt = senderFull.indexOf("@");
    var sender = senderFull.substring(0, indexOfAt);

    $('#chat-window').append("<div class='chat-message'><div class='sender-name'>"+"[ "+sender+" ]"+"</div><div class='message'>"+msg+"</div></div>");

    $("#chat-window").scrollTop($("#chat-window")[0].scrollHeight);
}

function addDomains(toList)
{
    newToList = [];
    for(var i = 0; i < toList.length; ++i)
    {
        var user = toList[i];
        if(user.indexOf("@") == -1) user += "@" + curUser.domain;

        newToList.push(user);
    }
    return newToList;
}

function submitMessage()
{
    var msg = $("#message-text").val();
    if(msg.trim() == "") 
    {   
        $("#message-text").val("")
        return false;
    }
    else
    {
        $("#message-text").val("")
        var curDate = new Date();
        var curDateTime = curDate.toISOString();
        var package = {
            to: addDomains(curChannel.toList),
            from: curUser.user + "@" + curUser.domain,
            sentAt: curDateTime,
            message: msg,
            messageFormatted: msg,
            format: "text/markdown"
        };

        var jPackage = JSON.stringify(package);

        $.support.cors = true;
        $.ajax({
            url: "https://" + curUser.domain + ":" + curUser.port + "/messages",
            type: "POST",
            data: jPackage,
            contentType:"application/json; charset=utf-8",
            beforeSend: function(xhr) {
                xhr.setRequestHeader('Authorization', curUser.token);
            }
        });
    }
}

$(document).ready(initPage);