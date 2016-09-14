var curUser = {
    user: "",
    pass: "",
    token: "",
    domain: ""
};

var chatTabList = [];

var curChannel = {
    toList: [],
};

function initPage()
{
    $(".chat-tab").click(function () {
        $("#chat-tabs").children().each(function() {
            $(this).css("backgroundColor", "#0f0f0f");
            $(this).css("color", "#b48c64");
        });

        $(this).css("backgroundColor", "#b48c64");
        $(this).css("color", "#0f0f0f");
    });

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
        if(toListAsString != "") curChannel.toList = toListAsString.split(",");
        curChannel.toList.push(curUser.user + "@" + curUser.domain);
        $("#chat-title span").text("[" + curChannel.toList.toString() + "]");
        $("#status-bar span").text("status: chatting with [" + curChannel.toList.toString() + "]");
        ///// END CREATE CHANNEL
        $("#new-chat-button").css("backgroundColor", "#b48c64");
        $("#input-box-new-chat").css("display", "none");
    });

    $("#createForm").submit(function(e) {
        e.preventDefault();
        var fullUsername = $("#createUser").val();
        var password = $("#createPass").val();

        if(fullUsername == "" || password == "")
        {
            $("#createUser").val("");
            $("#createPass").val("");
            return;
        }        

        var indexOfAt = fullUsername.indexOf("@");
        if(indexOfAt > 0)
        {
            var userName = fullUsername.substring(0, indexOfAt);
            var connectURL = fullUsername.substring(indexOfAt + 1);

            $("#createUser").val("");
            $("#createPass").val("");

            $("#connect-button").css("backgroundColor", "#b48c64");
            $("#input-box-connect").css("display", "none");

            var package = '{"username":"'+userName+'","password":"'+password+'"}';
            $.support.cors = true;
            $.ajax({
                url: "http://" + connectURL + ":4567/users",
                type: "POST",
                data:package,
                contentType:"application/json; charset=utf-8",
                success: function(data) {
                    $("#status-bar span").text("status: account created. click [connect] to login");
                },
                error: function(data) {
                    $("#status-bar span").text("status: account creation failed");
                },
                beforeSend: function() {
                    $("#status-bar span").text("status: creating account...please wait");
                }
            });
        }
        else
        {
            $("#createUser").val("TRY <username>@<provider domain>");
            $("#createPass").val("");
        }
    });

    $("#connectForm").submit(function(e){
        e.preventDefault();
        var fullUsername = $("#connectUser").val();
        var password = $("#connectPass").val();

        if(fullUsername == "" || password == "")
        {
            $("#connectUser").val("");
            $("#connectPass").val("");
            return;
        }        

        var indexOfAt = fullUsername.indexOf("@");
        if(indexOfAt > 0)
        {
            var userName = fullUsername.substring(0, indexOfAt);
            var connectURL = fullUsername.substring(indexOfAt + 1);

            $("#connectUser").val("");
            $("#connectPass").val("");

            $("#connect-button").css("backgroundColor", "#b48c64");
            $("#input-box-connect").css("display", "none");

            loginToProvider(userName, password, connectURL);
        }
        else
        {
            $("#connectUser").val("TRY <username>@<provider domain>");
            $("#connectPass").val("");
        }
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

function addChat(toarray)
{
    $('#chat-tabs').append("<div class='chat-tab'>"+ toarray +"</div>");

    $("#chat-tabs").scrollTop($("#chat-tabs")[0].scrollHeight);

    $(".chat-tab").click(function () {
        $("#chat-tabs").children().each(function() {
            $(this).css("backgroundColor", "#0f0f0f");
            $(this).css("color", "#b48c64");
        });

        $(this).css("backgroundColor", "#b48c64");
        $(this).css("color", "#0f0f0f");
    });
}

function loginToProvider(user, pass, domain)
{
    curUser.user = user;
    curUser.domain = domain;
    curUser.pass = pass;

    var package = {
        grant_type: "password",
        username: user,
        password: pass
    };
    $.support.cors = true;

    $.ajax({
        url: "http://" + domain + ":4567/token",
        type: "POST",
        data:package,
        contentType:"application/x-www-form-urlencoded",
        success:function(data) {
            $("#status-bar span").text("status: logged in. click [new chat] to set chat list");
            curUser.token = data.access_token;
            var messageSocket = new WebSocket("ws://"+ curUser.domain + ":4567/messageStream");
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
    var indexOfAt = senderFull.indexOf("@");
    var sender = senderFull.substring(0, indexOfAt);

    $('#chat-window').append("<div class='chat-message'><div class='sender-name'>"+"[ "+sender+" ]"+"</div><div class='message'>"+msg+"</div></div>");

    $("#chat-window").scrollTop($("#chat-window")[0].scrollHeight);
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
            to: curChannel.toList,
            from: curUser.user + "@" + curUser.domain,
            sentAt: curDateTime,
            message: msg,
            messageFormatted: msg,
            format: "text/markdown"
        };

        var jPackage = JSON.stringify(package);

        $.ajax({
            url: "http://" + curUser.domain + ":4567/messages",
            type: "POST",
            data: jPackage,
            headers: {
                Authorization: curUser.token
            },
            contentType:"application/json; charset=utf-8"
        });
    }
}

$(document).ready(initPage);