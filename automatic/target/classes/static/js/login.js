// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    // 获取DOM元素
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const loginBtn = document.getElementById('loginBtn');
    const statusBtn = document.getElementById('statusBtn');
    const messageBox = document.getElementById('message');
    
    // 弹窗相关元素
    const welcomeModal = document.getElementById('welcomeModal');
    const closeModalBtn = document.getElementById('closeModalBtn');
    const confirmModalBtn = document.getElementById('confirmModalBtn');
    const dontShowAgainBtn = document.getElementById('dontShowAgainBtn');
    
    // 检查是否已经显示过弹窗
    const hasSeenWelcome = localStorage.getItem('hasSeenWelcome');
    
    // 如果没有显示过，显示弹窗
    if (!hasSeenWelcome) {
        setTimeout(() => {
            welcomeModal.style.display = 'flex';
        }, 500); // 延迟500ms显示，让页面先加载完成
    }
    
    // 关闭弹窗函数
    function closeWelcomeModal() {
        welcomeModal.style.display = 'none';
    }
    
    // 设置不再显示
    function setDontShowAgain() {
        localStorage.setItem('hasSeenWelcome', 'true');
        closeWelcomeModal();
    }
    
    // 事件监听
    closeModalBtn.addEventListener('click', closeWelcomeModal);
    confirmModalBtn.addEventListener('click', closeWelcomeModal);
    dontShowAgainBtn.addEventListener('click', setDontShowAgain);
    
    // 点击弹窗外部关闭
    welcomeModal.addEventListener('click', function(event) {
        if (event.target === welcomeModal) {
            closeWelcomeModal();
        }
    });
    
    // 按ESC键关闭弹窗
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' && welcomeModal.style.display === 'flex') {
            closeWelcomeModal();
        }
    });

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
        const apiPath = "/api/login";

        // 发送登录请求
        fetch(apiPath, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: loginData
        })
        .then(res => {
            // 即使返回401状态码，我们也需要解析响应体
            return res.json().then(data => {
                return { status: res.status, data: data };
            });
        })
        .then(result => {
            console.log('登录响应状态:', result.status, '数据:', result.data);
            
            if (result.status === 200 && result.data.code === 200 && result.data.data && result.data.data.token) {
                // 登录成功
                showMessage('登录成功！', 'success');
                localStorage.setItem('token', result.data.data.token);
                console.log('Token已保存到localStorage:', result.data.data.token);
                // 登录成功后跳转
                window.location.href = './index.html';
            } else if (result.status === 401 || (result.data.data && result.data.data.success === false)) {
                // 登录失败 - 用户名或密码错误
                showMessage('登录失败：用户名或密码错误', 'error');
            } else {
                // 其他错误
                showMessage('登录失败：' + (result.data.message || '未知错误'), 'error');
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
        fetch('/api/service/status', {
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