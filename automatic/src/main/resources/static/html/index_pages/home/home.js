Vue.createApp({
    data() {
        return {
            // 用户统计数据
            userStats: {
                userTaskCount: 0,
                userTaskExecutions: 0,
                userSuccessExecutions: 0,
                userFailedExecutions: 0,
                userRecentExecutions: 0,
                userTotalNotifications: 0,
                userTotalNewNotifications: 0,
                todayNewNotifications: 0,
                userSuccessRate: 0
            },
            // 加载状态
            isLoading: false,
            // 错误信息
            errorMessage: '',
            // 刷新按钮状态
            isRefreshing: false,
            // 自动刷新定时器
            refreshInterval: null
        };
    },
    computed: {
        // 格式化后的用户成功率
        formattedUserSuccessRate() {
            return this.userStats.userSuccessRate.toFixed(1) + '%';
        },
        // 格式化后的用户任务数量
        formattedUserTaskCount() {
            return this.formatNumber(this.userStats.userTaskCount);
        },
        // 格式化后的用户任务执行次数
        formattedUserTaskExecutions() {
            return this.formatNumber(this.userStats.userTaskExecutions);
        },
        // 格式化后的用户成功执行次数
        formattedUserSuccessExecutions() {
            return this.formatNumber(this.userStats.userSuccessExecutions);
        },
        // 格式化后的用户失败执行次数
        formattedUserFailedExecutions() {
            return this.formatNumber(this.userStats.userFailedExecutions);
        },
        // 格式化后的用户24小时执行次数
        formattedUserRecentExecutions() {
            return this.formatNumber(this.userStats.userRecentExecutions);
        },
        // 格式化后的用户总通知数
        formattedUserTotalNotifications() {
            return this.formatNumber(this.userStats.userTotalNotifications);
        },
        // 格式化后的用户新通知总数
        formattedUserTotalNewNotifications() {
            return this.formatNumber(this.userStats.userTotalNewNotifications);
        },
        // 格式化后的今日新通知数
        formattedTodayNewNotifications() {
            return this.formatNumber(this.userStats.todayNewNotifications);
        }
    },
    methods: {
        // 加载用户定时任务执行统计数据
        async loadUserTaskExecutionStats() {
            this.isLoading = true;
            this.errorMessage = '';
            
            try {
                const token = localStorage.getItem('token');
                if (!token) {
                    throw new Error('未找到登录token，请先登录');
                }
                
                const response = await fetch('/api/task-execution/user-stats', {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    }
                });
                
                if (!response.ok) {
                    if (response.status === 401) {
                        throw new Error('登录状态失效，请重新登录');
                    }
                    throw new Error('网络响应不正常');
                }
                
                const data = await response.json();
                console.log('获取到的用户统计数据:', data);
                
                // 更新用户统计数据
                this.userStats = {
                    userTaskCount: data.userTaskCount || 0,
                    userTaskExecutions: data.userTaskExecutions || 0,
                    userSuccessExecutions: data.userSuccessExecutions || 0,
                    userFailedExecutions: data.userFailedExecutions || 0,
                    userRecentExecutions: data.userRecentExecutions || 0,
                    userTotalNotifications: data.userTotalNotifications || 0,
                    userTotalNewNotifications: data.userTotalNewNotifications || 0,
                    todayNewNotifications: data.todayNewNotifications || 0,
                    userSuccessRate: data.userSuccessRate || 0
                };
                
                console.log('用户定时任务统计数据加载完成');
            } catch (error) {
                console.error('加载用户定时任务统计数据失败:', error);
                this.errorMessage = '数据加载失败: ' + error.message;
                this.showErrorMessage(this.errorMessage);
            } finally {
                this.isLoading = false;
            }
        },
        
        // 手动刷新数据
        async refreshData() {
            this.isRefreshing = true;
            await this.loadUserTaskExecutionStats();
            
            // 1秒后重置刷新按钮状态
            setTimeout(() => {
                this.isRefreshing = false;
            }, 1000);
        },
        
        // 显示错误消息
        showErrorMessage(message) {
            console.error('错误消息:', message);
            
            // 创建一个简单的错误提示
            const errorDiv = document.getElementById('error-message');
            if (!errorDiv) {
                const newErrorDiv = document.createElement('div');
                newErrorDiv.id = 'error-message';
                newErrorDiv.style.cssText = `
                    position: fixed;
                    top: 20px;
                    right: 20px;
                    background: #f44336;
                    color: white;
                    padding: 15px;
                    border-radius: 5px;
                    z-index: 1000;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.2);
                `;
                newErrorDiv.textContent = message;
                document.body.appendChild(newErrorDiv);
                
                // 5秒后自动移除
                setTimeout(() => {
                    if (newErrorDiv.parentNode) {
                        newErrorDiv.parentNode.removeChild(newErrorDiv);
                    }
                }, 5000);
            }
        },
        
        // 格式化数字（添加千位分隔符）
        formatNumber(num) {
            if (num === null || num === undefined) return '0';
            return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        },
        
        // 格式化日期时间
        formatDateTime(date) {
            if (!date) return '-';
            if (!(date instanceof Date)) {
                date = new Date(date);
            }
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');
            const seconds = String(date.getSeconds()).padStart(2, '0');
            return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
        }
    },
    mounted() {
        console.log('首页Vue组件加载完成');
        
        // 加载用户定时任务统计数据
        this.loadUserTaskExecutionStats();
        
        // 每30秒刷新一次数据
        this.refreshInterval = setInterval(() => {
            this.loadUserTaskExecutionStats();
        }, 30000);
    },
    beforeUnmount() {
        // 清除定时器
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
        }
    }
}).mount('#homeApp');