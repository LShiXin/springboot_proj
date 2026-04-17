Vue.createApp({
    data() {
        return {
            isLoading: false,
            notifications: [],
            filteredNotifications: [],
            filterReadStatus: '',
            filterTaskName: '',
            filterDateRange: '',
            
            // 工具提示相关
            showUrlTooltip: false,
            currentTooltipUrl: '',
            tooltipStyle: {
                top: '0px',
                left: '0px'
            },
            
            // 标记已读状态
            markingAllAsRead: false,
            
            // 分页相关
            currentPage: 1,
            pageSize: 10,
            totalRecords: 0,
            totalPages: 1
        };
    },
    computed: {
        // 计算过滤后的通知
        computedFilteredNotifications() {
            let filtered = this.notifications;
            
            // 按阅读状态过滤
            if (this.filterReadStatus === 'unread') {
                filtered = filtered.filter(notification => !notification.read);
            } else if (this.filterReadStatus === 'read') {
                filtered = filtered.filter(notification => notification.read);
            }
            
            // 按任务名称过滤
            if (this.filterTaskName) {
                const keyword = this.filterTaskName.toLowerCase();
                filtered = filtered.filter(notification => 
                    notification.taskName && notification.taskName.toLowerCase().includes(keyword)
                );
            }
            
            // 按日期过滤
            if (this.filterDateRange) {
                const filterDate = new Date(this.filterDateRange);
                filtered = filtered.filter(notification => {
                    const notificationDate = new Date(notification.notificationTime);
                    return notificationDate.toDateString() === filterDate.toDateString();
                });
            }
            
            // 更新分页信息
            this.totalRecords = filtered.length;
            this.totalPages = Math.ceil(this.totalRecords / this.pageSize);
            
            // 分页处理
            const startIndex = (this.currentPage - 1) * this.pageSize;
            const endIndex = startIndex + this.pageSize;
            
            return filtered.slice(startIndex, endIndex);
        }
    },
    watch: {
        // 监听过滤条件变化，重置到第一页
        filterReadStatus() {
            this.currentPage = 1;
            this.updateFilteredNotifications();
        },
        filterTaskName() {
            this.currentPage = 1;
            this.updateFilteredNotifications();
        },
        filterDateRange() {
            this.currentPage = 1;
            this.updateFilteredNotifications();
        },
        pageSize() {
            this.currentPage = 1;
            this.updateFilteredNotifications();
        },
        currentPage() {
            this.updateFilteredNotifications();
        }
    },
    methods: {
        // 初始化数据
        async initData() {
            this.isLoading = true;
            try {
                // 这里应该调用后端API获取通知记录
                await this.fetchNotifications();
                this.updateFilteredNotifications();
            } catch (error) {
                console.error('初始化数据失败:', error);
                this.showError('加载通知记录失败');
            } finally {
                this.isLoading = false;
            }
        },
        
        // 获取当前用户ID
        async getCurrentUserId() {
            const token = localStorage.getItem('token');
            if (!token) {
                console.error('用户未登录');
                return null;
            }
            
            try {
                const response = await fetch('/api/user/refresh', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    }
                });
                
                if (!response.ok) {
                    throw new Error(`获取用户信息失败: ${response.status}`);
                }
                
                const result = await response.json();
                if (result.code === 200) {
                    return result.data.id;
                } else {
                    console.error('获取用户ID失败:', result.message);
                    return null;
                }
            } catch (error) {
                console.error('获取用户ID时发生错误:', error);
                return null;
            }
        },
        
        // 获取通知记录（从数据库获取真实数据）
        async fetchNotifications() {
            try {
                // 获取当前用户ID
                const userId = await this.getCurrentUserId();
                if (!userId) {
                    this.showError('无法获取用户信息，请重新登录');
                    this.notifications = [];
                    return;
                }
                
                // 调用API获取通知记录
                const token = localStorage.getItem('token');
                const response = await fetch(`/api/crawler/notifications/user/${userId}`, {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });
                
                if (!response.ok) {
                    throw new Error(`获取通知记录失败: ${response.status}`);
                }
                
                const result = await response.json();
                if (result.code === 200) {
                    // 处理API返回的数据（NotificationWithTaskNameDTO格式）
                    this.notifications = result.data.map(notification => {
                        // 确保数据格式正确
                        return {
                            id: notification.id,
                            userId: notification.userId,
                            taskId: notification.taskId,
                            taskName: notification.taskName || `任务${notification.taskId}`, // 使用API返回的任务名称
                            title: notification.title,
                            url: notification.url,
                            notificationTime: notification.notificationTime,
                            originalContent: notification.originalContent,
                            processedContent: notification.processedContent || notification.originalContent,
                            matchedKeywords: notification.matchedKeywords,
                            createdAt: notification.createdAt,
                            read: notification.read || false
                        };
                    });
                    
                    // 按通知时间降序排序（最新的在前）
                    this.notifications.sort((a, b) => {
                        return new Date(b.notificationTime) - new Date(a.notificationTime);
                    });
                    
                    console.log(`成功加载 ${this.notifications.length} 条通知记录`);
                    // console.log(this.notifications);
                } else {
                    throw new Error(result.message || '获取通知记录失败');
                }
            } catch (error) {
                console.error('获取通知记录失败:', error);
                this.showError('加载通知记录失败: ' + error.message);
                // 如果API调用失败，使用空数组
                this.notifications = [];
            }
        },
        
        // 更新过滤后的通知
        updateFilteredNotifications() {
            this.filteredNotifications = this.computedFilteredNotifications;
        },
        
        // 刷新数据
        refreshData() {
            this.initData();
        },
        
        // 格式化日期时间
        formatDateTime(dateTime) {
            if (!dateTime) return '-';
            const date = new Date(dateTime);
            return date.toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });
        },
        


        // 打开通知链接
        openNotificationUrl(notification) {
            if (notification.url) {
                window.open(notification.url, '_blank');
                // 自动标记为已读
                this.markAsRead(notification);
            }
        },
        
        // 标记通知为已读
        async markAsRead(notification) {
            if (notification.read) return;
            
            try {
                // 调用API标记为已读
                const token = localStorage.getItem('token');
                const response = await fetch(`/api/crawler/notifications/${notification.id}/read`, {
                    method: 'PUT',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });
                
                if (!response.ok) {
                    throw new Error(`标记已读失败: ${response.status}`);
                }
                
                const result = await response.json();
                if (result.code === 200) {
                    // 更新本地状态
                    const index = this.notifications.findIndex(n => n.id === notification.id);
                    if (index !== -1) {
                        this.notifications[index].read = true;
                        this.updateFilteredNotifications();
                    }
                    
                    this.showSuccess('已标记为已读');
                } else {
                    throw new Error(result.message || '标记已读失败');
                }
            } catch (error) {
                console.error('标记已读失败:', error);
                this.showError('标记已读失败: ' + error.message);
            }
        },
        
        // 标记所有通知为已读
        async markAllAsRead() {
            if (!confirm('确定要标记所有通知为已读吗？')) return;
            
            this.markingAllAsRead = true;
            try {
                // 获取当前用户ID
                const userId = await this.getCurrentUserId();
                if (!userId) {
                    throw new Error('无法获取用户信息');
                }
                
                // 调用API标记所有为已读
                const token = localStorage.getItem('token');
                const response = await fetch(`/api/crawler/notifications/user/${userId}/read-all`, {
                    method: 'PUT',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });
                
                if (!response.ok) {
                    throw new Error(`标记所有已读失败: ${response.status}`);
                }
                
                const result = await response.json();
                if (result.code === 200) {
                    // 更新本地状态
                    this.notifications.forEach(notification => {
                        notification.read = true;
                    });
                    this.updateFilteredNotifications();
                    
                    this.showSuccess(`所有通知已标记为已读 (共${result.data.markedCount || this.notifications.length}条)`);
                } else {
                    throw new Error(result.message || '标记所有已读失败');
                }
            } catch (error) {
                console.error('标记所有已读失败:', error);
                this.showError('标记所有已读失败: ' + error.message);
            } finally {
                this.markingAllAsRead = false;
            }
        },
        
        // 复制链接到剪贴板
        async copyUrl(notification) {
            try {
                await navigator.clipboard.writeText(notification.url);
                this.showSuccess('链接已复制到剪贴板');
            } catch (error) {
                console.error('复制链接失败:', error);
                // 降级方案
                const textArea = document.createElement('textarea');
                textArea.value = notification.url;
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
                this.showSuccess('链接已复制到剪贴板');
            }
        },

        // 删除通知
        async deleteNotification(notification) {
            if (!confirm(`确定要删除这条通知吗？\n\n标题: ${notification.title || '无标题'}\n任务: ${notification.taskName || '未知任务'}`)) {
                return;
            }

            try {
                // 调用API删除通知
                const token = localStorage.getItem('token');
                const response = await fetch(`/api/crawler/notifications/${notification.id}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (!response.ok) {
                    throw new Error(`删除通知失败: ${response.status}`);
                }

                const result = await response.json();
                if (result.code === 200) {
                    // 从本地数组中移除通知
                    const index = this.notifications.findIndex(n => n.id === notification.id);
                    if (index !== -1) {
                        this.notifications.splice(index, 1);
                        this.updateFilteredNotifications();
                    }
                    
                    this.showSuccess('通知已删除');
                } else {
                    throw new Error(result.message || '删除通知失败');
                }
            } catch (error) {
                console.error('删除通知失败:', error);
                this.showError('删除通知失败: ' + error.message);
            }
        },
        
        // 分页方法
        prevPage() {
            if (this.currentPage > 1) {
                this.currentPage--;
            }
        },
        
        nextPage() {
            if (this.currentPage < this.totalPages) {
                this.currentPage++;
            }
        },
        
        changePageSize() {
            this.currentPage = 1;
        },
        
        // 显示成功消息
        showSuccess(message) {
            // 实际项目中可以使用更优雅的通知组件
            const toast = document.createElement('div');
            toast.className = 'success-toast';
            toast.textContent = message;
            toast.style.cssText = `
                position: fixed;
                top: 20px;
                right: 20px;
                background-color: #67c23a;
                color: white;
                padding: 12px 20px;
                border-radius: 4px;
                box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
                z-index: 9999;
                animation: fadeInOut 3s ease-in-out;
            `;
            
            document.body.appendChild(toast);
            setTimeout(() => {
                if (toast.parentNode) {
                    document.body.removeChild(toast);
                }
            }, 3000);
        },
        
        // 显示错误消息
        showError(message) {
            // 实际项目中可以使用更优雅的通知组件
            const toast = document.createElement('div');
            toast.className = 'error-toast';
            toast.textContent = message;
            toast.style.cssText = `
                position: fixed;
                top: 20px;
                right: 20px;
                background-color: #f56c6c;
                color: white;
                padding: 12px 20px;
                border-radius: 4px;
                box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
                z-index: 9999;
                animation: fadeInOut 3s ease-in-out;
            `;
            
            document.body.appendChild(toast);
            setTimeout(() => {
                if (toast.parentNode) {
                    document.body.removeChild(toast);
                }
            }, 3000);
        }
    },
    mounted() {
        this.initData();
        
        // 设置自动刷新（每60秒刷新一次）
        this.refreshInterval = setInterval(() => {
            this.refreshData();
        }, 60000);
        
        // 添加CSS动画
        const style = document.createElement('style');
        style.textContent = `
            @keyframes fadeInOut {
                0% { opacity: 0; transform: translateY(-20px); }
                10% { opacity: 1; transform: translateY(0); }
                90% { opacity: 1; transform: translateY(0); }
                100% { opacity: 0; transform: translateY(-20px); }
            }
        `;
        document.head.appendChild(style);
    },
    beforeUnmount() {
        // 清除定时器
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
        }
    }
}).mount('#notificationRecordApp');
