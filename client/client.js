var URL = "csubj.io"
var port = "4567";

var curUser = {
    userid: "yacklebeam",
    passwd: "password",
    token: ""
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

    $("#message-text").keypress(function(evt)
    {
        if(evt.which == 13)
        {
            var txt = $("#message-text");

            if(evt.shiftKey)
            {
                var curHeight = txt.height();
                txt.css("height", (curHeight + 16).toString() + "px");
                $("#message-box").css("height", (curHeight + 24).toString() + "px");
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
    });

    var package = {
        username: "yacklebeam",
        password: "password"
    };
    $.post("https://" + URL + ":" + port + '/users/', package);

    // connect the websocket connection for messages?
    curUser.userid = "yacklebeam";
    curUser.passwd = "password"
    loginToProvider();

    //load dummy messages
    /*for(i = 0; i < 120; ++i)
    {
        addChatMessage("test-johnsmiths", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc eget rutrum tellus. Etiam tempor, justo ac fermentum sodales, dolor felis condimentum ligula, vel molestie lacus nibh non magna.");
    }*/
}

function loginToProvider()
{
    var package = {
        grant_type: "password",
        username: curUser.userid + "@" + URL,
        password: curUser.passwd
    };
    $.post("https://" + URL + ":" + port + '/token/', package, function(data) {
        //do something with that login shit
        curUser.token = data.access_token;
    });

    var messageSocket = new WebSocket("wss://"+ URL + ":" + port + "/messages/");
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
    return [curUser.userid + "@" + URL,];
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
            to: getToIDs(),
            from: curUser.userid + "@" + URL,
            sentAt: curDateTime,
            message: msg,
            messageFormatted: msg,
            format: "text/markdown"
        };

        $.ajax({
            url: "https://" + URL + ":" + port + "/messages/",
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
    try {
        var decoded = JSON.parse(event.data);
        addChatMessage(decoded.from, decoded.message);    

    }
    catch(err) {
        
    }

}

$(document).ready(initPage);