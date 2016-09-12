var URL = "csubj.io"
var port = "4567";

var curUser = {
    userid: "yacklebeam",
    passwd: "password",
    token: ""
};

function initPage()
{
    $("#submit-form").submit(function(e) {
        e.preventDefault();
        submitMessage();
    });

    var package = {
        username: "yacklebeam",
        password: "password"
    };
    $.post(URL + ":" + port + '/users', package);

    // connect the websocket connection for messages?
    curUser.userid = "yacklebeam";
    curUser.passwd = "password"
    loginToProvider();
}

function loginToProvider()
{
    var package = {
        grant_type: "password",
        username: curUser.userid + "@" + URL,
        password: curUser.passwd
    };
    $.post(URL + ":" + port + '/token', package, function(data) {
        //do something with that login shit
        curUser.token = data.access_token;
    });

    var messageSocket = new WebSocket("ws://"+ URL + ":" + port + "/messages");
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

function getToIDs()
{
    return [curUserID];
}

function submitMessage()
{
    var msg = $("#message-line").val();
    if(msg.trim() == "") 
    {   
        $("#message-line").val("")
        return false;
    }
    else
    {
        $("#message-line").val("")
        //send the message to the provider
        var curDate = new Date();
        var curDateTime = curDate.toISOString();
        var package = {
            to: getToIDs(),
            from: curUserID,
            sentAt: curDateTime,
            message: msg,
            messageFormatted: msg,
            format: "text/markdown"
        };

        $.ajax({
            url: URL + ":" + port + "/messages",
            type: "POST",
            data: package,
            headers: {
                Authorization: curUser.token
            },
            dataType: 'json'
        })
    }
}

function recieveMessage(event)
{
    var decoded = JSON.parse(event.data);

    addChatMessage(decoded.from, decoded.message);    
}

$(document).ready(initPage);