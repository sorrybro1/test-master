const CTX = "";

//确保元素存在后再更新
function updateUserDisplay(userInfo) {
    console.log("准备更新用户信息：", userInfo);

    // 尝试更新，如果元素不存在则等待
    const tryUpdate = () => {
        const loginAccount = document.getElementById("LoginAccount");
        const loginAccountMirror = document.getElementById("LoginAccountMirror");
        const useridInput = document.getElementById("userid");

        // 检查关键元素是否已加载
        if (!loginAccount || !loginAccountMirror) {
            console.log("元素尚未加载，100ms后重试...");
            setTimeout(tryUpdate, 100); // 100ms后重试
            return;
        }

        // 更新导航栏的用户名
        if (userInfo.username) {
            loginAccount.textContent = userInfo.username;
            console.log(" 已更新 LoginAccount：", userInfo.username);
        }

        // 更新模态框中的用户名
        if (userInfo.username) {
            loginAccountMirror.textContent = userInfo.username;
            console.log(" 已更新 LoginAccountMirror：", userInfo.username);
        }

        // 更新隐藏的用户ID
        if (useridInput && userInfo.uid) {
            useridInput.value = userInfo.uid;
            console.log(" 已更新 userid：", userInfo.uid);
        }
    };

    tryUpdate();
}

// 页面加载时获取并显示用户信息
(function loadUserInfo() {
    if (window.location.pathname.endsWith('/adminlogin.html')) {
        return;
    }

    const protectedPages = [
        '/admin/index.html',
        '/admin/org.html',
        '/admin/stType.html',
        '/admin/stContent.html',
        '/admin/role.html'
    ];

    const currentPath = window.location.pathname;
    const isProtectedPage = protectedPages.some(page => currentPath.endsWith(page));

    if (isProtectedPage) {
        fetch(CTX + "/admin-api/auth/check", {
            method: 'GET',
            credentials: 'include'
        })
            .then(response => response.json())
            .then(data => {
                if (!data.success) {
                    window.location.replace(CTX + "/admin/adminlogin.html");
                } else {
                    if (data.userInfo) {
                        updateUserDisplay(data.userInfo);
                    }
                }
            })
            .catch(() => {
                window.location.replace(CTX + "/admin/adminlogin.html");
            });
    }
})();

// 退出登录
function logoutAdmin(event) {
    if(event) event.preventDefault();

    if (window.jQuery) {
        $.post(CTX + "/admin-api/auth/logout", function () {
            window.location.replace(CTX + "/admin/adminlogin.html");
        }).fail(function () {
            window.location.replace(CTX + "/admin/adminlogin.html");
        });
    } else {
        window.location.replace(CTX + "/admin/adminlogin.html");
    }
}

function getUser() {
    setTimeout("$('#updateRes').modal('show')", 100);
}

function updateReson() {
    var pwd = $("#e_passwd").val();
    var repwd = $("#e_repasswd").val();

    // 验证
    if (pwd == null || pwd == "") {
        alert("请输入新密码！");
        return;
    }

    if (pwd.length > 16) {
        alert("密码不能大于16位！");
        return;
    }

    if (pwd.length < 6) {
        alert("密码不能小于6位！");
        return;
    }

    if (pwd != repwd) {
        alert("两次密码不一致！");
        return;
    }

    // 调用接口
    $.ajax({
        type: "POST",
        url: CTX + "/admin-api/auth/updatePassword",
        contentType: "application/json;charset=UTF-8",
        data: JSON.stringify({
            newPassword: pwd
        }),
        dataType: 'json',
        success: function (res) {
            alert(res.message);
            if (res.success == true) {
                // 关闭模态框
                $('#updateRes').modal('hide');
                // 跳转到登录页
                window.location.replace(CTX + "/admin/adminlogin.html");
            }
        },
        error: function (xhr) {
            alert("修改密码失败：" + (xhr.responseText || xhr.status));
        }
    });
}

function closeRes() {
    setTimeout("$('#updateRes').modal('hide')", 100);
    var form = document.getElementById("upfm");
    if (form) form.reset();
}

// 禁止浏览器缓存页面
window.addEventListener('pageshow', function(event) {
    if (event.persisted) {
        if (!window.location.pathname.endsWith('/adminlogin.html')) {
            window.location.reload();
        }
    }
});