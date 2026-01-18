// admin-common.js
const CTX = "";

//  页面加载时立即检查登录状态
(function checkAdminLoginOnLoad() {
    // 如果当前页面是登录页，不需要检查
    if (window.location.pathname.endsWith('/adminlogin.html')) {
        return;
    }

    // 检查是否是后台页面
    if (window.location.pathname.includes('/admin/')) {
        fetch(CTX + "/admin-api/auth/check", {
            method: 'GET',
            credentials: 'include'
        })
            .then(response => response.json())
            .then(data => {
                if (!data.success) {
                    // 未登录，立即跳转到登录页
                    window.location.replace(CTX + "/admin/adminlogin.html");
                }
            })
            .catch(() => {
                // 请求失败，跳转到登录页
                window.location.replace(CTX + "/admin/adminlogin.html");
            });
    }
})();

// 退出登录
function logoutAdmin(event) {
    if(event) event.preventDefault();

    if (window.jQuery) {
        $.post(CTX + "/admin-api/auth/logout", function () {
            // 清除浏览器历史记录，使用 replace 跳转
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
    var uid = $("#userid").val();
    var pwd = $("#e_passwd").val();
    var repwd = $("#e_repasswd").val();
    var orgid1 = "";
    if (pwd == null || pwd == "") {
        setTimeout("$('#updateRes').modal('hide')", 100);
        alert("密码未修改！");
        return;
    }
    if (pwd != null || pwd != "") {
        if (pwd.length > 16) {
            alert("密码不能大于16位！");
            return;
        } else if (pwd.length < 6 && pwd.length > 0) {
            alert("密码不能小于6位！");
            return;
        } else if (pwd != repwd) {
            alert("两次密码不一致！");
            return;
        }
    }
    pwd = window.encodeURIComponent(encodeURI(pwd));
    var data = "&uid=" + uid + "&passwd=" + pwd + "&orgid=" + orgid1;

    $.ajax({
        type: "post",
        url: CTX + "/userCtrl/update_userInfo.do?" + data,
        cache: false,
        async: false,
        dataType: 'json',
        success: function (res) {
            alert(res.msg);
            if (res.success == true) {
                // 密码修改成功后，使用 replace 跳转
                window.location.replace(CTX + "/admin/adminlogin.html");
            }
        },
        error: function () {
            alert("修改数据失败！");
        }
    });
}

function closeRes() {
    setTimeout("$('#updateRes').modal('hide')", 100);
    var form = document.getElementById("upfm");
    if (form) form.reset();
}

// 页面加载后同步账户信息到模态框
document.addEventListener('DOMContentLoaded', function() {
    const name = document.getElementById("LoginAccount")?.textContent || "admin";
    const mirror = document.getElementById("LoginAccountMirror");
    if (mirror) mirror.textContent = name;
});

//  禁止浏览器缓存页面
window.addEventListener('pageshow', function(event) {
    // 如果页面是从缓存中加载的（用户按了后退按钮）
    if (event.persisted) {
        // 重新检查登录状态
        if (!window.location.pathname.endsWith('/adminlogin.html')) {
            window.location.reload();
        }
    }
});