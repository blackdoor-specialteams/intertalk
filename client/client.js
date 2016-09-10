function initPage()
{
    $("#submit-form").submit(function(e) {
        e.preventDefault();
        submitMessage();
    });
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
    var msg = $("#message-line").val();
    if(msg.trim() == "") 
    {   
        $("#message-line").val("")
        return false;
    }
    else
    {
        $("#message-line").val("")
        addChatMessage("yacklebeam", msg);
    }
}

$(document).ready(initPage);