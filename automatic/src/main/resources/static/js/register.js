// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    // 获取DOM元素
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const registerBtn = document.getElementById('registerBtn');
    const messageBox = document.getElementById('message');

    // 注册核心函数
    function register() {
        // 获取输入值并去除首尾空格
        const username = usernameInput.value.trim();
        const password = passwordInput.value.trim();

        // 表单验证
        if (!username) {
            showMessage('请输入用户名', 'error');
            return;
        }
        if (!password) {
            showMessage('请输入密码', 'error');
            return;
        }
        if (password.length < 6) {
            showMessage('密码长度不能少于6位', 'error');
            return;
        }

        // 构建请求数据
        const registerData = JSON.stringify({ username, password });
        const apiPath = "http://localhost:8080/register";

        // 发送注册请求
        fetch(apiPath, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: registerData
        })
        .then(res => {
            // 检查响应状态是否正常
            if (!res.ok) {
                throw new Error('请求失败，状态码：' + res.status);
            }
            return res.json();
        })
        .then(data => {
            console.log('注册响应数据:', data);
            if (data.success) {
                // 注册成功
                showMessage('注册成功！', 'success');
                localStorage.setItem('token', data.token);
                // 可选：注册成功后跳转到登录页/主页
                // window.location.href = './login.html';
            } else {
                // 注册失败
                showMessage('注册失败：' + (data.message || '未知错误'), 'error');
            }
        })
        .catch(err => {
            console.error('注册请求出错:', err);
            showMessage('注册请求出错，请检查服务是否正常', 'error');
        });
    }

    // 提示信息展示函数
    function showMessage(text, type) {
        messageBox.textContent = text;
        messageBox.className = 'message ' + type;
        // 3秒后清空提示
        setTimeout(() => {
            messageBox.textContent = '';
            messageBox.className = 'message';
        }, 3000);
    }

    // 绑定注册按钮点击事件
    registerBtn.addEventListener('click', register);
});