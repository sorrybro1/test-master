// student-common.js
const CTX = "";

//  页面加载时立即检查登录状态
(function checkStudentLoginOnLoad() {
    // 如果当前页面是登录页，不需要检查
    if (window.location.pathname.endsWith('/login.html')) {
        return;
    }

    // 检查是否是学生/教师页面
    if (window.location.pathname.includes('/pages/')) {
        fetch(CTX + "/api/auth/check", {
            method: 'GET',
            credentials: 'include'
        })
            .then(response => response.json())
            .then(data => {
                if (!data.success) {
                    // 未登录，立即跳转到登录页
                    window.location.replace(CTX + "/pages/login.html");
                }
            })
            .catch(() => {
                // 请求失败，跳转到登录页
                window.location.replace(CTX + "/pages/login.html");
            });
    }
})();

//  学生/教师退出登录
function logout(event) {
    if(event) event.preventDefault();

    if (window.jQuery) {
        $.post(CTX + "/api/auth/logout", function () {
            // 清除浏览器历史记录，使用 replace 跳转
            window.location.replace(CTX + "/pages/login.html");
        }).fail(function () {
            window.location.replace(CTX + "/pages/login.html");
        });
    } else {
        window.location.replace(CTX + "/pages/login.html");
    }
}

//  禁止浏览器缓存页面
window.addEventListener('pageshow', function(event) {
    // 如果页面是从缓存中加载的（用户按了后退按钮）
    if (event.persisted) {
        // 重新检查登录状态
        if (!window.location.pathname.endsWith('/login.html')) {
            window.location.reload();
        }
    }
});