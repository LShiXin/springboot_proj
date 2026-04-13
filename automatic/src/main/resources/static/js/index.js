const { createApp } = Vue;

createApp({
    data() {
        return {
            username: localStorage.getItem('username') || (localStorage.getItem('token') ? '用户' : '游客'),
            activeMenu: '',
            // isLoading: false, // 加载状态
            menuList: [

                { title: '首页', icon: 'icon-home', path: './index_pages/home/home.html' },
                { title: '任务管理', icon: 'icon-user', path: './index_pages/monitor_manager/monitor_manager.html' },
                { title: '通知记录', icon: 'icon-history', path: './index_pages/notification_record/notification_record.html' },
                { title: '执行记录', icon: 'icon-task', path: './index_pages/building/building.html' },
                { title: '数据统计', icon: 'icon-chart', path: './index_pages/building/building.html' },
                { title: '系统设置', icon: 'icon-setting', path: './index_pages/building/building.html' },
                { title: '日志管理', icon: 'icon-log', path: './index_pages/building/building.html' },
                { title: '帮助中心', icon: 'icon-help', path: './index_pages/building/building.html' }
            ]
        };
    },
    methods: {
        // 退出登录
        handleLogout() {
            if (confirm('确定要退出登录吗？')) {
                localStorage.removeItem('token');
                localStorage.removeItem('username');
                this.username = '游客';
                window.location.href = './login.html';
            }
        },
        // 加载右侧页面核心方法
        async loadPage(pagePath, menuTitle) {
            // 如果点击的是当前激活菜单，则不重复加载页面
            console.log(this.activeMenu);
            if (this.activeMenu === menuTitle) {
                return;
            }
            // 1. 更新激活菜单
            this.activeMenu = menuTitle;
            // 2. 显示加载状态
            // this.isLoading = true;
            const container = document.getElementById('pageContainer');

            try {
                // 3. 发送请求加载页面内容
                console.log(`正在加载页面: ${pagePath}`);
                const res = await fetch(pagePath);
                if (!res.ok) throw new Error(`加载失败：${res.status}`);
                const html = await res.text();
                // 4. 渲染到容器中
                container.innerHTML = html;

                // 5. 执行子页面中的脚本（如需）
                this.executePageScripts(container);
            } catch (err) {
                console.error('页面加载错误:', err);
                container.innerHTML = `<div class="page-card"><div class="page-title">加载失败</div><p style="color:#f56c6c;">${err.message}</p></div>`;
            } finally {
                // 6. 隐藏加载状态
                // this.isLoading = false;
            }
        },
        // 执行子页面中的脚本（可选）
        executePageScripts(container) {
            const scripts = container.querySelectorAll('script');
            scripts.forEach(oldScript => {
                const newScript = document.createElement('script');
                // 复制属性
                Array.from(oldScript.attributes).forEach(attr => {
                    newScript.setAttribute(attr.name, attr.value);
                });
                // 复制内容
                newScript.textContent = oldScript.textContent;
                newScript.async = false; // 同步执行,避免发生顺序执行问题
                // 替换并执行
                oldScript.parentNode.replaceChild(newScript, oldScript);
            });
        },
        // 获取用户信息（可选）
        async getUserInfo() {
            const token = localStorage.getItem('token');
            console.log('获取用户信息，token:', token);
            if (!token) return;
            console.log(token);
            var apiPath = "/api/user/refresh";
            try {
                const res = await fetch(apiPath, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    }
                });
                if (!res.ok) throw new Error(`请求失败：${res.status}`);
                const data = await res.json();
                console.log('用户信息响应:', data);
                if (data.code === 200) {
                    user={
                        username: data.data.username,
                        email: data.data.email,
                        phone: data.data.phone,
                        enabled: data.data.enabled
                    }
                    this.username = data.data.username || '用户';
                    console.log('用户信息获取成功:', data.data);
                } else {
                    console.warn('用户信息获取失败:', data.message);
                    localStorage.removeItem('token');
                    localStorage.removeItem('username');
                    this.username = '游客';
                }
            } catch (err) {
                console.error('获取用户信息错误:', err);
                localStorage.removeItem('token');
                localStorage.removeItem('username');
                this.username = '游客';
            }
        },
        },
        // 页面挂载后默认加载首页
        mounted() {
            this.getUserInfo(); // 可选
            this.loadPage('./index_pages/home/home.html', '首页');// 默认加载首页
            console.log(this.activeMenu)
        }
    
}).mount('#app');