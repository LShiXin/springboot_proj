Vue.createApp({
    data() {
        return {
            isLoading: false,
            records: [],
            filteredRecords: [],
            filterStatus: '',
            filterTaskName: '',
            filterDateRange: '',
            expandedRecordId: null,
            showDetailModal: false,
            selectedRecord: {},
            cancellingId: null,
            retryingId: null,
            
            // 分页相关
            currentPage: 1,
            pageSize: 10,
            totalRecords: 0,
            totalPages: 1
        };
    },
    computed: {
        // 计算过滤后的记录
        computedFilteredRecords() {
            let filtered = this.records;
            
            // 按状态过滤
            if (this.filterStatus) {
                filtered = filtered.filter(record => record.status === this.filterStatus);
            }
            
            // 按任务名称过滤
            if (this.filterTaskName) {
                const keyword = this.filterTaskName.toLowerCase();
                filtered = filtered.filter(record => 
                    record.taskName && record.taskName.toLowerCase().includes(keyword)
                );
            }
            
            // 按日期过滤
            if (this.filterDateRange) {
                const filterDate = new Date(this.filterDateRange);
                filtered = filtered.filter(record => {
                    const recordDate = new Date(record.startTime);
                    return recordDate.toDateString() === filterDate.toDateString();
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
        filterStatus() {
            this.currentPage = 1;
            this.updateFilteredRecords();
        },
        filterTaskName() {
            this.currentPage = 1;
            this.updateFilteredRecords();
        },
        filterDateRange() {
            this.currentPage = 1;
            this.updateFilteredRecords();
        },
        pageSize() {
            this.currentPage = 1;
            this.updateFilteredRecords();
        },
        currentPage() {
            this.updateFilteredRecords();
        }
    },
    methods: {
        // 初始化数据
        async initData() {
            this.isLoading = true;
            try {
                // 这里应该调用后端API获取执行记录
                // 暂时使用模拟数据
                await this.fetchExecutionRecords();
                this.updateFilteredRecords();
            } catch (error) {
                console.error('初始化数据失败:', error);
                this.showError('加载执行记录失败');
            } finally {
                this.isLoading = false;
            }
        },
        
        // 获取执行记录（模拟数据）
        async fetchExecutionRecords() {
            // 模拟API调用延迟
            await new Promise(resolve => setTimeout(resolve, 500));
            
            // 模拟数据
            this.records = [
                {
                    id: 1,
                    taskId: 'task-001',
                    taskName: '数据同步任务',
                    startTime: '2024-01-15T10:30:00',
                    endTime: '2024-01-15T10:35:30',
                    duration: 330000, // 5分30秒，单位毫秒
                    status: 'SUCCESS',
                    result: '同步完成，共处理1000条记录',
                    executor: 'system',
                    triggerType: 'SCHEDULED',
                    retryCount: 0,
                    errorMessage: null,
                    logs: [
                        { timestamp: '2024-01-15T10:30:05', level: 'INFO', message: '开始执行数据同步' },
                        { timestamp: '2024-01-15T10:32:15', level: 'INFO', message: '正在处理数据，进度：50%' },
                        { timestamp: '2024-01-15T10:34:45', level: 'INFO', message: '数据同步完成' }
                    ],
                    resultData: { processed: 1000, success: 1000, failed: 0 }
                },
                {
                    id: 2,
                    taskId: 'task-002',
                    taskName: '报表生成任务',
                    startTime: '2024-01-15T09:00:00',
                    endTime: '2024-01-15T09:15:20',
                    duration: 920000, // 15分20秒
                    status: 'FAILED',
                    result: '生成报表时发生错误',
                    executor: 'admin',
                    triggerType: 'MANUAL',
                    retryCount: 2,
                    errorMessage: '数据库连接超时，请检查数据库服务状态',
                    logs: [
                        { timestamp: '2024-01-15T09:00:10', level: 'INFO', message: '开始生成日报表' },
                        { timestamp: '2024-01-15T09:05:30', level: 'WARN', message: '数据库查询较慢' },
                        { timestamp: '2024-01-15T09:10:15', level: 'ERROR', message: '数据库连接超时' }
                    ],
                    resultData: null
                },
                {
                    id: 3,
                    taskId: 'task-003',
                    taskName: '数据备份任务',
                    startTime: '2024-01-15T08:00:00',
                    endTime: null,
                    duration: null,
                    status: 'RUNNING',
                    result: null,
                    executor: 'system',
                    triggerType: 'SCHEDULED',
                    retryCount: 0,
                    errorMessage: null,
                    logs: [
                        { timestamp: '2024-01-15T08:00:05', level: 'INFO', message: '开始数据备份' },
                        { timestamp: '2024-01-15T08:30:00', level: 'INFO', message: '备份进度：30%' }
                    ],
                    resultData: null
                },
                {
                    id: 4,
                    taskId: 'task-004',
                    taskName: '日志清理任务',
                    startTime: '2024-01-14T23:00:00',
                    endTime: '2024-01-14T23:05:15',
                    duration: 315000, // 5分15秒
                    status: 'SUCCESS',
                    result: '清理完成，删除旧日志文件15个',
                    executor: 'system',
                    triggerType: 'SCHEDULED',
                    retryCount: 0,
                    errorMessage: null,
                    logs: [
                        { timestamp: '2024-01-14T23:00:10', level: 'INFO', message: '开始清理过期日志' },
                        { timestamp: '2024-01-14T23:04:50', level: 'INFO', message: '清理完成' }
                    ],
                    resultData: { deletedFiles: 15, freedSpace: '2.3GB' }
                },
                {
                    id: 5,
                    taskId: 'task-005',
                    taskName: '邮件发送任务',
                    startTime: '2024-01-14T14:30:00',
                    endTime: '2024-01-14T14:30:45',
                    duration: 45000, // 45秒
                    status: 'CANCELLED',
                    result: '任务被用户取消',
                    executor: 'user123',
                    triggerType: 'MANUAL',
                    retryCount: 0,
                    errorMessage: '用户手动取消了任务执行',
                    logs: [
                        { timestamp: '2024-01-14T14:30:05', level: 'INFO', message: '开始发送邮件' },
                        { timestamp: '2024-01-14T14:30:30', level: 'WARN', message: '用户取消了任务' }
                    ],
                    resultData: null
                }
            ];
            
            // 实际项目中应该调用API：
            // const response = await fetch('/api/execution-records', {
            //     headers: {
            //         'Authorization': `Bearer ${localStorage.getItem('token')}`
            //     }
            // });
            // if (!response.ok) throw new Error('获取执行记录失败');
            // this.records = await response.json();
        },
        
        // 更新过滤后的记录
        updateFilteredRecords() {
            this.filteredRecords = this.computedFilteredRecords;
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
        
        // 格式化时间
        formatTime(dateTime) {
            if (!dateTime) return '-';
            const date = new Date(dateTime);
            return date.toLocaleTimeString('zh-CN', {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });
        },
        
        // 格式化持续时间
        formatDuration(durationMs) {
            if (!durationMs) return '-';
            
            const seconds = Math.floor(durationMs / 1000);
            const minutes = Math.floor(seconds / 60);
            const hours = Math.floor(minutes / 60);
            
            if (hours > 0) {
                return `${hours}小时${minutes % 60}分${seconds % 60}秒`;
            } else if (minutes > 0) {
                return `${minutes}分${seconds % 60}秒`;
            } else {
                return `${seconds}秒`;
            }
        },
        
        // 获取状态样式类
        getStatusClass(status) {
            switch (status) {
                case 'SUCCESS': return 'success';
                case 'FAILED': return 'error';
                case 'RUNNING': return 'running';
                case 'CANCELLED': return 'warning';
                default: return 'info';
            }
        },
        
        // 获取状态文本
        getStatusText(status) {
            switch (status) {
                case 'SUCCESS': return '成功';
                case 'FAILED': return '失败';
                case 'RUNNING': return '运行中';
                case 'CANCELLED': return '已取消';
                default: return status;
            }
        },
        
        // 截断文本
        truncateText(text, maxLength) {
            if (!text) return '';
            if (text.length <= maxLength) return text;
            return text.substring(0, maxLength) + '...';
        },
        
        // 查看详情
        viewDetails(record) {
            this.selectedRecord = record;
            this.showDetailModal = true;
        },
        
        // 关闭详情弹窗
        closeDetailModal() {
            this.showDetailModal = false;
            this.selectedRecord = {};
        },
        
        // 切换展开行
        toggleExpand(recordId) {
            this.expandedRecordId = this.expandedRecordId === recordId ? null : recordId;
        },
        
        // 取消执行
        async cancelExecution(record) {
            if (!confirm('确定要取消这个任务的执行吗？')) return;
            
            this.cancellingId = record.id;
            try {
                // 调用API取消任务
                // await fetch(`/api/execution-records/${record.id}/cancel`, {
                //     method: 'POST',
                //     headers: {
                //         'Authorization': `Bearer ${localStorage.getItem('token')}`
                //     }
                // });
                
                // 模拟API调用
                await new Promise(resolve => setTimeout(resolve, 1000));
                
                // 更新本地状态
                const index = this.records.findIndex(r => r.id === record.id);
                if (index !== -1) {
                    this.records[index].status = 'CANCELLED';
                    this.records[index].endTime = new Date().toISOString();
                    this.updateFilteredRecords();
                }
                
                this.showSuccess('任务已取消');
            } catch (error) {
                console.error('取消任务失败:', error);
                this.showError('取消任务失败');
            } finally {
                this.cancellingId = null;
            }
        },
        
        // 重试执行
        async retryExecution(record) {
            if (!confirm('确定要重试这个任务吗？')) return;
            
            this.retryingId = record.id;
            try {
                // 调用API重试任务
                // await fetch(`/api/execution-records/${record.id}/retry`, {
                //     method: 'POST',
                //     headers: {
                //         'Authorization': `Bearer ${localStorage.getItem('token')}`
                //     }
                // });
                
                // 模拟API调用
                await new Promise(resolve => setTimeout(resolve, 1000));
                
                // 创建新的执行记录
                const newRecord = {
                    ...record,
                    id: Date.now(),
                    startTime: new Date().toISOString(),
                    endTime: null,
                    duration: null,
                    status: 'RUNNING',
                    result: null,
                    errorMessage: null,
                    retryCount: (record.retryCount || 0) + 1
                };
                
                this.records.unshift(newRecord);
                this.updateFilteredRecords();
                
                this.showSuccess('任务已开始重试');
            } catch (error) {
                console.error('重试任务失败:', error);
                this.showError('重试任务失败');
            } finally {
                this.retryingId = null;
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
            alert(message); // 实际项目中可以使用更优雅的通知组件
        },
        
        // 显示错误消息
        showError(message) {
            alert('错误: ' + message); // 实际项目中可以使用更优雅的通知组件
        }
    },
    mounted() {
        this.initData();
        
        // 设置自动刷新（每30秒刷新一次）
        this.refreshInterval = setInterval(() => {
            this.refreshData();
        }, 30000);
    },
    beforeUnmount() {
        // 清除定时器
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
        }
    }
}).mount('#executionRecordApp');