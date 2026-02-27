// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    // 获取DOM元素
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const loginBtn = document.getElementById('loginBtn');
    const statusBtn = document.getElementById('statusBtn');
    const messageBox = document.getElementById('message');

    // 登录函数
    function login() {
        // 获取输入值并去除首尾空格
        const username = usernameInput.value.trim();
        const password = passwordInput.value.trim();

        // 简单表单验证
        if (!username) {
            showMessage('请输入用户名', 'error');
            return;
        }
        if (!password) {
            showMessage('请输入密码', 'error');
            return;
        }

        // 构建请求数据
        const loginData = JSON.stringify({ username, password });
        const apiPath = "http://localhost:8080/login";

        // 发送登录请求
        fetch(apiPath, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: loginData
        })
        .then(res => {
            // 检查响应状态是否正常
            if (!res.ok) {
                throw new Error('请求失败，状态码：' + res.status);
            }
            return res.json();
        })
        .then(data => {
            console.log('登录响应数据:', data);
            if (data.code === 200 && data.data && data.data.token) {
                // 登录成功
                showMessage('登录成功！', 'success');
                localStorage.setItem('token', data.data.token);
                // 可选：登录成功后跳转
                // window.location.href = './index.html';
            } else {
                // 登录失败
                showMessage('登录失败：' + (data.message || '未知错误'), 'error');
            }
        })
        .catch(err => {
            console.error('登录请求出错:', err);
            showMessage('登录请求出错，请检查服务是否正常', 'error');
        });
    }

    // 获取服务状态函数
    function getServiceStatus() {
        // 获取本地存储的token
        const token = localStorage.getItem('token');
        if (!token) {
            showMessage('请先登录获取token', 'error');
            return;
        }

        // 发送状态请求
        fetch('http://localhost:8080/service/status', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + token
            }
        })
        .then(res => {
            if (res.status === 401) {
                showMessage('未授权，token无效或过期', 'error');
                // 清除无效token
                localStorage.removeItem('token');
                return null;
            }
            if (!res.ok) {
                throw new Error('获取状态失败，状态码：' + res.status);
            }
            return res.text();
        })
        .then(text => {
            if (text !== null) {
                showMessage('服务状态：' + text, 'success');
                // 也可以用alert展示，二选一
                // alert('服务状态: ' + text);
            }
        })
        .catch(err => {
            console.error('获取状态请求出错:', err);
            showMessage('获取服务状态出错', 'error');
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

    // 绑定按钮点击事件
    loginBtn.addEventListener('click', login);
    statusBtn.addEventListener('click', getServiceStatus);
});