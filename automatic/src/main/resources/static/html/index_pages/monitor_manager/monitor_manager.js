Vue.createApp({
    data() {
        return {
            tasks: [], // 任务列表
            // isLoading: false,
            addingTask: false,
            editingTaskId: null,
            activatingTaskId: null, // 正在激活的任务ID
            editingTask: {
                id: null,
                name: '',
                keywords: '',
                startTime: '',
                endTime: '',
                intervalMinutes: ''
            },
            originalEditingTask: null, // 原始编辑任务数据
            newTask: {
                name: '',
                keywords: '',
                startTime: '',
                endTime: '',
                intervalMinutes: '',
                enabled: false,
                links: []
            },
            linksByTaskId: {},             // 以任务ID为键的链接对象
            showLinks: {}, // 控制显示链接子表的状态


            allOptionalUrls: [],
            showAddLinkModal: false,  // 控制添加链接弹窗显示
            selectedUrlId: '',        // 选中的链接ID
            currentAddLinkTask: null,  // 当前要添加链接的任务
            
            // 鼠标悬停相关
            hoverTimeout: null,        // 悬停计时器
            hoverTaskId: null,         // 当前悬停的任务ID
            hoverColumnIndex: null     // 当前悬停的列索引
        };
    },
    mounted() {
        // 加载该用户下的定时任务
        this.loadUserTasks();

        // 加载所有可选的用户链接
        this.loadOptionalUrls(); 
        
    },
    computed: {
        // 计算属性：判断编辑的任务是否有修改
        isEditingTaskModified() {
            if (!this.originalEditingTask) return false;
            return (
                this.editingTask.name !== this.originalEditingTask.name ||
                this.editingTask.keywords !== this.originalEditingTask.keywords ||
                this.editingTask.startTime !== this.originalEditingTask.startTime ||
                this.editingTask.endTime !== this.originalEditingTask.endTime ||
                this.editingTask.intervalMinutes !== this.originalEditingTask.intervalMinutes
            );
        },
        // 计算属性：获取选中的链接详情
        selectedLinkDetail() {
            if (!this.selectedUrlId || !this.allOptionalUrls || this.allOptionalUrls.length === 0) {
                return null;
            }
            
            // 将selectedUrlId转换为字符串进行比较，确保类型一致
            const selectedIdStr = String(this.selectedUrlId);
            const foundItem = this.allOptionalUrls.find(item => {
                // 确保item和item.url_id存在
                if (!item || item.url_id === undefined || item.url_id === null) {
                    return false;
                }
                // 将url_id转换为字符串进行比较
                return String(item.url_id) === selectedIdStr;
            });
            
            console.log('selectedLinkDetail计算属性执行:');
            console.log('  selectedUrlId:', this.selectedUrlId);
            console.log('  selectedIdStr:', selectedIdStr);
            console.log('  allOptionalUrls长度:', this.allOptionalUrls.length);
            console.log('  找到的项:', foundItem);
            
            return foundItem || null;
        },
        // 计算属性：获取按钮提示信息
        buttonTooltip() {
            if (this.allOptionalUrls.length === 0) {
                return '没有可用的链接，请先添加基础链接';
            } else if (!this.selectedUrlId) {
                return '请从下拉列表中选择一个链接';
            } else {
                return '点击确认添加选中的链接';
            }
        },
        // 计算属性：获取当前任务已添加的链接ID集合
        currentTaskAddedLinkIds() {
            if (!this.currentAddLinkTask || !this.currentAddLinkTask.links) {
                return new Set();
            }
            // 获取当前任务已添加链接的URL集合，用于去重检查
            return new Set(this.currentAddLinkTask.links.map(link => link.url));
        },
        // 计算属性：过滤可选链接，标记已添加的链接
        filteredOptionalUrls() {
            if (!this.allOptionalUrls || this.allOptionalUrls.length === 0) {
                return [];
            }
            
            // 如果没有当前任务或当前任务没有链接，返回所有可选链接
            if (!this.currentAddLinkTask || !this.currentAddLinkTask.links || this.currentAddLinkTask.links.length === 0) {
                return this.allOptionalUrls.map(item => ({
                    ...item,
                    isAdded: false
                }));
            }
            
            // 获取当前任务已添加链接的URL集合
            const addedUrls = new Set(this.currentAddLinkTask.links.map(link => link.url));
            
            // 标记哪些链接已经添加
            return this.allOptionalUrls.map(item => ({
                ...item,
                isAdded: addedUrls.has(item.url)
            }));
        }
    },
    methods: {
        handleAddTask() {
            this.addingTask = true;
            this.newTask = { name: '', keywords: '', startTime: '', endTime: '', intervalMinutes: '', enabled: true };
        },
        async loadOptionalUrls(){
            var thit=this
            var token = localStorage.getItem('token');
            if (!token) {
                console.warn('未找到token，无法加载可选链接');
                thit.allOptionalUrls = [];
                return;
            }
            var path="/api/monitottask/loadOptionalUrls"
            try {
                const res = await fetch(path, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    }
                });
                
                if (!res.ok) {
                    if (res.status === 401) {
                        console.warn('Token无效或已过期，请重新登录');
                        thit.allOptionalUrls = [];
                        return;
                    }
                    throw new Error(`HTTP错误: ${res.status}`);
                }
                
                const result = await res.json();
                if (result.code === 200 && result.data) {
                    thit.allOptionalUrls = result.data;
                    console.log('成功加载可选链接:', thit.allOptionalUrls.length, '个');
                    console.log('可选链接数据:', thit.allOptionalUrls);
                } else {
                    console.warn('加载可选链接失败:', result.message || '未知错误');
                    thit.allOptionalUrls = [];
                }
            } catch (err) {
                console.error('获取可选链接失败：' + err.message);
                thit.allOptionalUrls = [];
            }

        },
        async loadUserTasks() {
            var thit = this;
            // 判断 tasks 是否已存在有效任务（如有 id 字段的对象）
            if (this.tasks.length > 0 && this.tasks.some(t => t && t.id)) {
                return; // 如果已有任务，则不重新加载
            }
            var token = localStorage.getItem('token');
            if (!token) {
                alert('登录状态失效，请先登录');
                return;
            }
            try {
                const res = await fetch('/api/monitottask/getbyuser', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    }
                });
                if (!res.ok) throw new Error('获取用户任务失败');
                const result = await res.json();
                thit.tasks = result.data; // 直接覆盖任务列表
                console.log('加载用户任务:', result.data);
            } catch (err) {
                alert('获取用户任务失败：' + err.message);
            }
        },
        async confirmAddTask() {
            if (!this.newTask.name) {
                alert('请填写任务名称');
                return;
            }
            if (!this.newTask.keywords) {
                alert('请填写关键字');
                return;
            }
            if (!this.newTask.startTime) {
                alert('请选择开始时间');
                return;
            }
            if (!this.newTask.endTime) {
                alert('请选择结束时间');
                return;
            }
            if (!this.newTask.intervalMinutes || this.newTask.intervalMinutes < 1) {
                alert('请填写执行间隔（分钟，正整数）');
                return;
            }
            await this.confirmAddTaskApi(this.newTask);
        },

        async confirmAddTaskApi(taskData) {
            // 构造后端需要的任务对象
            var thit = this
            var token = localStorage.getItem('token');
            if (!token) {
                alert('登录状态失效，请先登录');
                return;
            }
            const payload = {
                name: taskData.name,
                keywords: taskData.keywords,
                enabled: taskData.enabled,
                // 定时任务相关字段
                scheduleConfig: {
                    startTime: taskData.startTime,
                    endTime: taskData.endTime,
                    intervalMillis: taskData.intervalMinutes * 60 * 1000 // 转为毫秒
                }
            };
            try {
                const res = await fetch('/api/monitottask/add', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify(payload)
                });

                if (!res.ok) throw new Error('添加任务失败');
                const result = await res.json();
                // 添加到本地列表
                console.log('添加任务响应:', result);
                var new_task = {
                    id: result.data.id,
                    name: result.data.name,
                    keywords: result.data.keywords,
                    startTime: result.data.startTime,
                    endTime: result.data.endTime,
                    intervalMinutes: result.data.intervalMinutes,
                    enabled: result.data.enabled,
                }
                console.log('当前任务列表:', thit.tasks);
                thit.tasks.unshift(new_task);
                thit.addingTask = false;
                alert('任务添加成功');
            } catch (err) {
                alert('任务添加失败：' + err.message);
            }
        },
        cancelAddTask() {
            this.addingTask = false;
        },

        //点击显示当前任务的子表
        handleToggleLinks(taskId) {
            // 切换指定任务的链接子表显示状态
            this.showLinks = { ...this.showLinks, [taskId]: !this.showLinks[taskId] };

            // 如果链接子表被展开且任务没有链接数据，则从后端获取
            if (this.showLinks[taskId]) {
                this.getTaskUrls(taskId);
            }
        },
        // 获取对应taskID下的全部数据
        async getTaskUrls(taskID) {
            const token = localStorage.getItem('token');
            if (!token) {
                alert('登录状态失效，请先登录');
                return;
            }

            try {
                const res = await fetch('/api/monitottask/getTaskUrlsByTaskId', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify(taskID)
                });

                if (!res.ok) {
                    throw new Error('获取任务链接失败');
                }

                const result = await res.json();
                if (result.code === 200) {
                    // 找到对应的任务并更新链接数据
                    const task = this.tasks.find(t => t.id === taskID);
                    if (task) {
                        task.links = result.data.map(item => ({
                            id: item.id,
                            taskId: item.taskId,
                            url: item.url,
                            enabled: item.enabled,
                            remark: item.remark
                        }));
                    }
                } else {
                    alert('获取任务链接失败：' + (result.message || '未知错误'));
                }
            } catch (err) {
                alert('获取任务链接失败：' + err.message);
            }
        },
        handleActivate(task) {
            const token = localStorage.getItem('token');
            if (!token) {
                alert('登录状态失效，请先登录');
                return;
            }

            // 设置正在激活状态
            this.activatingTaskId = task.id;

            fetch('/api/monitottask/handleActivate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token
                },
                body: task.id
            })
                .then(async (res) => {
                    if (!res.ok) {
                        const errorData = await res.json().catch(() => ({}));
                        throw new Error(errorData.message || '激活任务失败');
                    }
                    return res.json();
                })
                .then((result) => {
                    if (result.code === 200) {
                        // 切换任务状态
                        task.enabled = !task.enabled;
                        alert('任务状态已切换');
                    } else {
                        alert('激活失败：' + (result.message || '未知错误'));
                    }
                })
                .catch((err) => {
                    alert('激活任务失败：' + err.message);
                })
                .finally(() => {
                    // 清除正在激活状态
                    this.activatingTaskId = null;
                });
        },
        handleEdit(task) {
            if (this.addingTask) {
                return;
            }
            this.editingTaskId = task.id;
            this.editingTask = {
                id: task.id,
                name: task.name || '',
                keywords: task.keywords || '',
                startTime: this.toDateTimeLocal(task.startTime),
                endTime: this.toDateTimeLocal(task.endTime),
                intervalMinutes: task.intervalMinutes
            };
            // 保存原始数据用于比较
            this.originalEditingTask = JSON.parse(JSON.stringify(this.editingTask));
        },
        cancelEditTask() {
            this.editingTaskId = null;
            this.editingTask = { id: null, name: '', keywords: '', startTime: '', endTime: '', intervalMinutes: '' };
            this.originalEditingTask = null;
        },
        async saveEditTask(task) {
            if (!this.editingTask.name) {
                alert('请填写任务名称');
                return;
            }
            if (!this.editingTask.keywords) {
                alert('请填写关键字');
                return;
            }
            if (!this.editingTask.startTime) {
                alert('请选择开始时间');
                return;
            }
            if (!this.editingTask.endTime) {
                alert('请选择结束时间');
                return;
            }
            if (!this.editingTask.intervalMinutes || this.editingTask.intervalMinutes < 1) {
                alert('请填写执行间隔（分钟，正整数）');
                return;
            }
            const token = localStorage.getItem('token');
            if (!token) {
                alert('登录状态失效，请先登录');
                return;
            }
            const payload = {
                id: this.editingTask.id,
                name: this.editingTask.name,
                keywords: this.editingTask.keywords,
                scheduleConfig: {
                    startTime: this.editingTask.startTime,
                    endTime: this.editingTask.endTime,
                    intervalMillis: this.editingTask.intervalMinutes * 60 * 1000
                }
            };
            try {
                const res = await fetch('/api/monitottask/update', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify(payload)
                });
                const result = await res.json();
                if (!res.ok || result.code !== 200) {
                    throw new Error(result.message || '保存失败');
                }
                task.name = result.data.name;
                task.keywords = result.data.keywords;
                task.startTime = result.data.startTime;
                task.endTime = result.data.endTime;
                task.intervalMinutes = result.data.intervalMinutes;
                task.enabled = result.data.enabled;
                this.cancelEditTask();
                alert('任务更新成功');
            } catch (err) {
                alert('任务更新失败：' + err.message);
            }
        },
        toDateTimeLocal(value) {
            if (!value) return '';
            return String(value).replace(' ', 'T').slice(0, 16);
        },
        async handleDelete(taskId) {
            var thit = this;
            if (!confirm('确定要删除该任务吗？(删除定时任务会删除所有相关的扫描链接)')) {
                return;
            }

            var token = localStorage.getItem('token');
            console.log('删除任务，使用 token:', token);
            if (!token) {
                alert('登录状态失效，请先登录');
                return;
            }

            try {
                const res = await fetch('/api/monitottask/handleDelete', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify(taskId)
                });

                if (!res.ok) {
                    const errorData = await res.json();
                    throw new Error(errorData.message || '删除任务失败');
                }

                const result = await res.json();
                if (result.code === 200 && result.data === true) {
                    // 从前端列表中移除已删除的任务
                    thit.tasks = thit.tasks.filter(t => t.id !== taskId);
                    alert('任务删除成功');
                } else {
                    alert('删除失败：' + (result.message || '未知错误'));
                }
            } catch (err) {
                alert('删除任务失败：' + err.message);
            }
        },

        addNewLink(task) {
            var thit = this
            var token = localStorage.getItem('token');
            console.log(task)
        },
        // 点击添加链接，显示当前所有可选择链接 allOptionalUrls
        async handleAddLink(task){
            console.log('当前任务:', task);
            console.log('可选链接列表:', this.allOptionalUrls);
            this.currentAddLinkTask = task;
            
            // 总是清空选择，让用户手动选择
            this.selectedUrlId = '';
            
            // 如果当前任务还没有加载links数据，先加载
            if (!task.links || task.links.length === 0) {
                console.log('当前任务links未加载，先获取links数据');
                await this.getTaskUrls(task.id);
            }
            
            this.showAddLinkModal = true;
        },

        // 下拉框变化处理
        onUrlSelectChange(event) {
            
            // 获取选中的option
            if (event?.target?.options) {
                const selectedOption = event.target.options[event.target.selectedIndex];
            }
            
            // 尝试多种方式获取值
            let selectedValue = null;
            if (event?.target?.value !== undefined) {
                selectedValue = event.target.value;
                console.log('14. 从event.target.value获取值:', selectedValue);
            }
            
            // 也检查selectedIndex
            if (event?.target?.selectedIndex !== undefined && event.target.selectedIndex >= 0) {
                const option = event.target.options[event.target.selectedIndex];
                if (option && option.value !== undefined) {
                    console.log('15. 从selectedIndex获取option值:', option.value);
                    if (!selectedValue) {
                        selectedValue = option.value;
                    }
                }
            }

        },

        // 关闭添加链接弹窗
        closeAddLinkModal() {
            this.showAddLinkModal = false;
            this.selectedUrlId = '';
            this.currentAddLinkTask = null;
        },

        // 确认添加链接
        async confirmAddLink() {
            
            if (!this.selectedUrlId) {
                console.error('错误：没有选中的链接ID');
                alert('请选择要添加的链接');
                return;
            }

            if (!this.currentAddLinkTask) {
                console.error('错误：没有当前任务');
                alert('未找到对应的任务');
                return;
            }

            const selectedLink = this.selectedLinkDetail;
            if (!selectedLink) {
                alert('未找到选中的链接信息');
                return;
            }

            // 检查链接是否已经添加到当前任务中
            if (this.currentAddLinkTask.links && this.currentAddLinkTask.links.length > 0) {
                const isAlreadyAdded = this.currentAddLinkTask.links.some(link => 
                    link.url === selectedLink.url
                );
                
                if (isAlreadyAdded) {
                    alert('该链接已经添加到当前任务中，不能重复添加');
                    return;
                }
            }

            const token = localStorage.getItem('token');
            if (!token) {
                alert('登录状态失效，请先登录');
                return;
            }

            try {
                // 构造请求数据
                const payload = {
                    taskId: this.currentAddLinkTask.id,
                    url: selectedLink.url,
                    remark: selectedLink.remark,
                    enabled: true,
                    classId: selectedLink.class_id,
                    methodId: selectedLink.method_id,
                    urlId: selectedLink.url_id
                };
                
                const res = await fetch('/api/monitottask/addLink', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify(payload)
                });

                if (!res.ok) {
                    const errorData = await res.json();
                    throw new Error(errorData.message || '添加链接失败');
                }

                const result = await res.json();
                console.log('8. 后端响应:', result);
                
                if (result.code === 200) {
                    alert('链接添加成功');
                    
                    // 如果当前任务正在显示链接子表，则刷新链接列表
                    if (this.showLinks[this.currentAddLinkTask.id]) {
                        // 这里可以调用刷新链接列表的方法，或者直接添加到本地列表
                        if (!this.currentAddLinkTask.links) {
                            this.currentAddLinkTask.links = [];
                        }
                        this.currentAddLinkTask.links.push({
                            id: result.data.id,
                            taskId: this.currentAddLinkTask.id,
                            url: selectedLink.url,
                            enabled: true,
                            remark: selectedLink.remark
                        });
                    }
                    
                    this.closeAddLinkModal();
                } else {
                    alert('添加失败：' + (result.message || '未知错误'));
                }
            } catch (err) {
                alert('添加链接失败：' + err.message);
            }
        },
        
        // 切换链接状态
        async toggleLinkStatus(link) {
            if (!confirm(`确定要${link.enabled ? '关闭' : '启用'}该链接吗？`)) {
                return;
            }
            
            const token = localStorage.getItem('token');
            if (!token) {
                alert('登录状态失效，请先登录');
                return;
            }
            
            try {
                // 先保存原始状态，以便在失败时回滚
                const originalEnabled = link.enabled;
                
                // 调用后端接口切换链接状态
                const res = await fetch('/api/monitottask/toggleLinkStatus', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify(link.id)
                });
                
                if (!res.ok) {
                    const errorData = await res.json();
                    throw new Error(errorData.message || '切换链接状态失败');
                }
                
                const result = await res.json();
                if (result.code === 200) {
                    // 更新链接状态为后端返回的新状态
                    link.enabled = result.data.enabled;
                    alert(`链接已${link.enabled ? '启用' : '关闭'}`);
                } else {
                    alert('切换失败：' + (result.message || '未知错误'));
                    // 如果后端失败，回滚状态
                    link.enabled = originalEnabled;
                }
            } catch (err) {
                alert('切换链接状态失败：' + err.message);
                // 如果出错，回滚状态
                link.enabled = !link.enabled;
            }
        },
        
        // 删除链接
        async handleDeleteLink(task, idx) {
            if (!confirm('确定要删除该链接吗？')) {
                return;
            }
            
            const token = localStorage.getItem('token');
            if (!token) {
                alert('登录状态失效，请先登录');
                return;
            }
            
            const link = task.links[idx];
            if (!link || !link.id) {
                alert('链接信息不完整，无法删除');
                return;
            }
            
            try {
                const res = await fetch('/api/monitottask/deleteLink', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify(link.id)
                });
                
                if (!res.ok) {
                    const errorData = await res.json();
                    throw new Error(errorData.message || '删除链接失败');
                }
                
                const result = await res.json();
                if (result.code === 200 && result.data === true) {
                    // 从前端列表中移除已删除的链接
                    task.links.splice(idx, 1);
                    alert('链接删除成功');
                } else {
                    alert('删除失败：' + (result.message || '未知错误'));
                }
            } catch (err) {
                alert('删除链接失败：' + err.message);
            }
        },
        
        // 鼠标悬停相关方法
        
        // 处理鼠标进入数据列
        handleMouseEnter(taskId, columnIndex, event) {
            // 清除之前的计时器
            if (this.hoverTimeout) {
                clearTimeout(this.hoverTimeout);
                this.hoverTimeout = null;
            }
            
            // 设置新的计时器，500毫秒（半秒）后显示提示
            this.hoverTimeout = setTimeout(() => {
                // console.log(`触发显示下次执行时间提示，taskId: ${taskId}, columnIndex: ${columnIndex}`);
                this.showNextExecutionTime(taskId, columnIndex, event);
            }, 500);
            
            // 保存当前悬停的任务和列信息
            this.hoverTaskId = taskId;
            this.hoverColumnIndex = columnIndex;
        },
        
        // 处理鼠标离开数据列
        handleMouseLeave() {
            // 清除计时器
            if (this.hoverTimeout) {
                clearTimeout(this.hoverTimeout);
                this.hoverTimeout = null;
            }
            
            // 清除悬停信息
            this.hoverTaskId = null;
            this.hoverColumnIndex = null;
            
            // 移除工具提示
            this.removeTooltip();
        },
        
        // 显示下次执行时间
        showNextExecutionTime(taskId, columnIndex, event) {
            // 找到对应的任务
            const task = this.tasks.find(t => t.id === taskId);
            if (!task) return;
            
            // 获取下次执行时间
            const nextExecutionTime = task.nextExecutionTime;
            const lastExecutionTime = task.lastExecutionTime;
            
            // 构建提示文本
            let tooltipText = '';
            
            if (nextExecutionTime) {
                tooltipText = `下次执行时间：${nextExecutionTime}`;
            } else {
                tooltipText = '下次执行时间：未设置';
            }
            
            // if (lastExecutionTime) {
            //     tooltipText += `\n上次执行时间：${lastExecutionTime}`;
            // }
            
            // 显示工具提示
            this.showTooltip(event.target, tooltipText);
        },
        
        
        // 格式化时间显示
        formatDateTime(dateTimeStr) {
            if (!dateTimeStr) return '未设置';
            
            try {
                const date = new Date(dateTimeStr);
                return date.toLocaleString('zh-CN', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                });
            } catch (e) {
                return dateTimeStr;
            }
        }
        
    }
}).mount('#monitorManagerApp');
