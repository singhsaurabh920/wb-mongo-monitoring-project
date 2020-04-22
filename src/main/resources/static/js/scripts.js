$("#btnCancelRegister").click(function () {
    window.location.replace("/");
});

$("#btnCancelLogin").click(function () {
    window.location.replace("/");
});

$("#btnRegisterDone").click(function () {
    window.location.replace("/");
});

$("#btnLogin").click(function () {
    $('#msgLoginFailed').hide();
    $.post("/user/login/" + $("#username").val() + "/" + $("#password").val(), function (data, status) {
        if (data == 'AUTHENTICATED') {
            window.location.replace("secured.html");
        } else if (data == "REQUIRE_TOKEN_CHECK") {
            $("#modalLoginCheckToken").modal('show');
        } else {
            $('#msgLoginFailed').show();
        }
    }).fail(function(){
        $('#msgLoginFailed').show();
    });
});

$("#btnRegister").click(function () {
    $.post("/user/register/" + $("#username").val() + "/" + $("#password").val(), function (data, status) {
        if (status == 'success') {
            $("#tokenQr").attr("src", "https://zxing.org/w/chart?cht=qr&chs=250x250&chld=M&choe=UTF-8&chl=otpauth://totp/worldbuild.org?secret=" + data + "&issuer=worldbuild");
            $("#tokenValue").text(data);
            $("#modalRegister").modal('show');
        }
    });
});

$("#btnLogout").click(function () {
    $.post("/user/logout", function (data, status) {
        window.location.replace("/")
    });
});

$("#btnTokenVerify").click(function () {
    $('#msgTokenCheckFailed').hide();
    $.post("/user/authenticate/token/"+$("#username").val() + "/" + $("#loginToken").val(), function (data, status) {
        if (data == 'AUTHENTICATED') {
            window.location.replace("secured.html");
        } else {
            $('#msgTokenCheckFailed').show();
        }
    }).fail(function(){
        $('#msgTokenCheckFailed').show();
    });
});