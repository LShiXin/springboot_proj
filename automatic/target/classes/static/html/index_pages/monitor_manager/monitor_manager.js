Vue.createApp({
    data() {
        return {
            tasks: [], // 任务列表
            isLoading: false,
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
            showLinks: {}, // 控制显示链接子表的状态
            newLink: {
                url: '',
                enabled: true,
                remark: ''
            }
        };
    },
    mounted() {
        this.loadUserTasks();
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
        }
    },
    methods: {
        handleAddTask() {
            this.addingTask = true;
            this.newTask = { name: '', keywords: '', startTime: '', endTime: '', intervalMinutes: '', enabled: true, links: [] };
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
                const res = await fetch('http://localhost:8080/monitottask/getbyuser', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    }
                });
                if (!res.ok) throw new Error('获取用户任务失败');
                const result = await res.json();
                thit.tasks = result.data; // 直接覆盖任务列表
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
                const res = await fetch('http://localhost:8080/monitottask/add', {
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
        handleToggleLinks(taskId) {
            // 切换指定任务的链接子表显示状态
            this.showLinks = { ...this.showLinks, [taskId]: !this.showLinks[taskId] };
            
            // 为测试目的，给任务添加一些固定的链接数据
            const task = this.tasks.find(t => t.id === taskId);
            if (task && !task.links) {
                // 添加固定数据用于测试
                task.links = [
                    { id: 1, url: 'https://www.example.com', enabled: true, remark: '示例网站' },
                    { id: 2, url: 'https://www.test.com', enabled: false, remark: '测试网站' },
                    { id: 3, url: 'https://www.demo.com', enabled: true, remark: '演示网站' }
                ];
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
            
            fetch('http://localhost:8080/monitottask/handleActivate', {
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
                const res = await fetch('http://localhost:8080/monitottask/update', {
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
                const res = await fetch('http://localhost:8080/monitottask/handleDelete', {
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
        handleAddLink(task) {
            // 确保子表是展开的
            this.showLinks = { ...this.showLinks, [task.id]: true };
            
            // 为测试目的，给任务添加一些固定的链接数据（如果还没有的话）
            if (!task.links) {
                task.links = [
                    { id: 1, url: 'https://www.example.com', enabled: true, remark: '示例网站' },
                    { id: 2, url: 'https://www.test.com', enabled: false, remark: '测试网站' },
                    { id: 3, url: 'https://www.demo.com', enabled: true, remark: '演示网站' }
                ];
            }
        },
        handleEditLink(task, linkIdx) {
            alert('编辑扫描链接功能开发中');
        },
        handleDeleteLink(task, linkIdx) {
            if (confirm('确定要删除该扫描链接吗？')) {
                task.links.splice(linkIdx, 1);
            }
        },
        addNewLink(task) {
            if (!this.newLink.url) {
                alert('请输入监控链接');
                return;
            }
            
            // 为新链接生成一个临时ID（实际应用中应该是后端返回的ID）
            const newLinkId = (task.links.length > 0 ? Math.max(...task.links.map(l => l.id)) + 1 : 1);
            
            // 添加新链接到任务的链接列表
            task.links.push({
                id: newLinkId,
                url: this.newLink.url,
                enabled: this.newLink.enabled,
                remark: this.newLink.remark
            });
            
            // 清空输入框
            this.newLink = {
                url: '',
                enabled: true,
                remark: ''
            };
        },
        toggleLinkStatus(link) {
            link.enabled = !link.enabled;
        },
        closeSubTable(taskId) {
            // 关闭指定任务的子表
            this.showLinks = { ...this.showLinks, [taskId]: false };
        }
    }
}).mount('#monitorManagerApp');
