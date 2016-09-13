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
        //$(this).css("backgroundColor", "#B46D64");
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
        
    });

    $("#newChatForm").submit(function(e) {
        e.preventDefault();
        var toListAsString = $("#newChatUsers").val();
        curChannel.toList = toListAsString.split(",");
        curChannel.toList.push(curUser.user + "@" + curUser.domain);
        $("#chat-title span").text("[" + curChannel.toList.toString() + "]");
    });

    $("#createForm").submit(function(e) {
        e.preventDefault();
        var fullUsername = $("#createUser").val();
        var password = $("#createPass").val();
        //is this safe????

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

            //create user
            var package = {
               username: userName,
               password: password
            };
            //$.post("https://" + connectURL + ":4567/users/", package);
            $.support.cors = true;
            $.ajax({
                url: "https://" + connectURL + ":4567/users/",
                type: "POST",
                data:package,
                contentType:"text/plain; charset=utf-8",
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
        //is this safe????

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

            //connect
            loginToProvider(userName, password, connectURL);

            $("#chat-title span").text("connected, no chat opened");
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

    //var package = {
    //    username: "yacklebeam",
    //    password: "password"
    //};
    //$.post("https://" + URL + ":" + port + '/users/', package);

    // connect the websocket connection for messages?
    //curUser.userid = "yacklebeam";
    //curUser.passwd = "password"
    //loginToProvider();

    //load dummy messages
    /*for(i = 0; i < 120; ++i)
    {
        addChatMessage("test-johnsmiths", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc eget rutrum tellus. Etiam tempor, justo ac fermentum sodales, dolor felis condimentum ligula, vel molestie lacus nibh non magna.");
    }*/
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
        username: user + "@" + domain,
        password: pass
    };
    $.post("https://" + domain + ':4567/token/', package, function(data) {
        curUser.token = data.access_token;
    });

    var messageSocket = new WebSocket("wss://"+ curUser.domain + ":4567/messages/");
    messageSocket.onmessage = recieveMessage(event);

}

function loadLines()
{
    document.getElementById("chat-window").style.backgroundColor = "#ffffff";
}

function addChatMessage(sender, msg)
{
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
        //send the message to the provider
        var curDate = new Date();
        var curDateTime = curDate.toISOString();
        var package = {
            to: curChannel.toList,
            from: curUser.userid + "@" + curUser.domain,
            sentAt: curDateTime,
            message: msg,
            messageFormatted: msg,
            format: "text/markdown"
        };

        $.ajax({
            url: "https://" + curUser.domain + ":4567/messages/",
            type: "POST",
            data: package,
            headers: {
                Authorization: curUser.token
            },
            dataType: 'json'
        });
    }
}

function recieveMessage(event)
{
    try {
        var decoded = JSON.parse(event.data);
        addChatMessage(decoded.from, decoded.message);    
    }
    catch(err) {
        console.log(err);
    }

}

$(document).ready(initPage);